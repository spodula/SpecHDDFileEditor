package hddEditor.ui.partitionPages.dialogs;
/*
 * New partition dialog
 */

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.PLUSIDEDOS;

public class NewPartitionDialog {
	private Display display = null;
	private Shell shell = null;
	private Combo PartitionTypeDropdown = null;

	//Created list of Combo entry numbers vs IDS and descriptions
	private byte[] PartTypesIDs = null;
	private String[] PartTypeDesc = null;
	
	//List of existing partitions. 
	private String[] ExistingParts;

	/*
	 * Return values. 
	 */
	public byte PartType = 0x00;
	public int SizeMB = 0;
	public String PartTypeSelected = "";
	public String Name = "";
	private boolean result = false;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public NewPartitionDialog(Display display) {
		this.display = display;
	}

	/**
	 * Show the form
	 * 
	 * @param ExistingPartitions
	 * @return
	 */
	public boolean Show(String ExistingPartitions[]) {
		ExistingParts = ExistingPartitions;
		for (int i = 0; i < ExistingPartitions.length; i++) {
			String s = ExistingPartitions[i].toUpperCase().trim();
			ExistingParts[i] = s;
		}

		Createform();
		loop();
		return (result);
	}

	/**
	 * Dialog loop, open and wait until closed.
	 */
	public void loop() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	/**
	 * Create form.
	 */
	private void Createform() {
		shell = new Shell(display);
		shell.setSize(400, 200);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;
		gridLayout.makeColumnsEqualWidth = true;
		shell.setLayout(gridLayout);

		/*
		 * Create a list of partition type IDs and names vs Index in the dropdown.
		 */
		ArrayList<PLUSIDEDOS.PARTSTRING> ItemList = new ArrayList<PLUSIDEDOS.PARTSTRING>();
		for (PLUSIDEDOS.PARTSTRING ps : PLUSIDEDOS.PartTypes) {
			if ((ps.flags & PLUSIDEDOS.PART_ALLOCATABLE) != 0) {
				ItemList.add(ps);
			}
		}
		PartTypesIDs = new byte[ItemList.size()];
		PartTypeDesc = new String[ItemList.size()];

		//get the text of the default +3DOS partition type
		int i = 0;
		String defaultText = "";
		for (PLUSIDEDOS.PARTSTRING ps : ItemList) {
			PartTypesIDs[i] = ps.PartID;
			PartTypeDesc[i++] = ps.Name;
			if (ps.PartID == PLUSIDEDOS.PARTITION_PLUS3DOS) {
				defaultText = ps.Name;
			}
		}

		Label l = new Label(shell, SWT.SHADOW_NONE);
		l.setText("Create a new partition.");
		FontData fontData = l.getFont().getFontData()[0];
		Font boldFont = new Font(shell.getDisplay(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		l.setFont(boldFont);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		l.setLayoutData(gridData);

		l = new Label(shell, SWT.SHADOW_NONE);
		l.setText("Partition type:");
		l.setFont(boldFont);
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 1;
		l.setLayoutData(gridData);

		// Create a dropdown Combo
		PartitionTypeDropdown = new Combo(shell, SWT.DROP_DOWN);
		PartitionTypeDropdown.setItems(PartTypeDesc);
		PartitionTypeDropdown.setText(defaultText);

		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		PartitionTypeDropdown.setLayoutData(gridData);

		l = new Label(shell, SWT.SHADOW_NONE);
		l.setText("Partition name:");
		l.setFont(boldFont);
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 1;
		l.setLayoutData(gridData);

		Text PartName = new Text(shell, SWT.NONE);
		PartName.setText("NewPartition");
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 1;
		PartName.setLayoutData(gridData);
		Color defaultBGColour = PartName.getBackground();
		Color defaultPenColour = PartName.getForeground();
		Color WarningBGColour = new Color(255, 0, 0);
		Color WarningPenColour = new Color(255, 255, 255);

		KeyAdapter PartitionKeyAdapter = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				String txt = PartName.getText();
				if (e != null) {
					e.doit = false;
					char c = e.character;
					//check characers are 0-9. a-z and "_"
					if ((c >= '0' && c <= '9') || (c < 0x20) || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
							|| c == '_') {
						e.doit = true;
						//check we have reached max length
						if (((txt.length() > 14) && (c > 0x1f)) && PartName.getSelectionText().length()==0) {
							e.doit = false;
						}
					}
					if( (e.keyCode == 'c' || e.keyCode == 'v')
			                && ( e.stateMask & SWT.MODIFIER_MASK ) == SWT.CTRL ) {
			            e.doit = true;
			        }
					
				}
			}
			public void keyReleased(KeyEvent e) {
				boolean StringFound = false;
				String txt = PartName.getText();
				txt = txt.toUpperCase().trim();
				for (String s : ExistingParts) {
					if (s.equals(txt))
						StringFound = true;
				}
				if (StringFound) {
					PartName.setBackground(WarningBGColour);
					PartName.setForeground(WarningPenColour);
				} else {
					PartName.setBackground(defaultBGColour);
					PartName.setForeground(defaultPenColour);
				}
			}
		};

		PartName.addKeyListener(PartitionKeyAdapter);

		PartitionKeyAdapter.keyReleased(null);

		l = new Label(shell, SWT.SHADOW_NONE);
		l.setText("Size (Mb) (Max 16Mb):");
		l.setFont(boldFont);
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 1;
		l.setLayoutData(gridData);

		Text PartSize = new Text(shell, SWT.NONE);
		PartSize.setText("16");
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 1;
		PartSize.setLayoutData(gridData);
		
		KeyAdapter PartSizeKeyAdapter = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				e.doit = false;
				char c = e.character;
				if ((c >= '0' && c <= '9') || (c < 0x20)) {
					e.doit = true;
				}
				if (((PartSize.getText().length() > 1) && (c > 0x1f)) && PartSize.getSelectionText().length()==0) {
					e.doit = false;
				}
				if( (e.keyCode == 'c' || e.keyCode == 'v')
		                && ( e.stateMask & SWT.MODIFIER_MASK ) == SWT.CTRL ) {
		            e.doit = true;
		        }
				
			}
			public void keyReleased(KeyEvent e) {
				boolean IsValid = true;
				String PartSizeData = PartSize.getText().trim();
				if (PartSizeData.isBlank() || (Integer.valueOf(PartSizeData) >16) || (Integer.valueOf(PartSizeData) <1 ))
					IsValid=false;

				if (!IsValid) {
					PartSize.setBackground(WarningBGColour);
					PartSize.setForeground(WarningPenColour);
				} else {
					PartSize.setBackground(defaultBGColour);
					PartSize.setForeground(defaultPenColour);
				}
			}
		};
		
		PartSize.addKeyListener(PartSizeKeyAdapter);
		PartitionKeyAdapter.keyReleased(null);
		
		Button CancelButton = new Button(shell, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;
		CancelButton.setText("Cancel");
		CancelButton.setLayoutData(gd);
		CancelButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				result = false;
				shell.dispose();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Button CreateButton = new Button(shell, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;
		CreateButton.setText("Create Partition");
		CreateButton.setLayoutData(gd);
		CreateButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// Decode the partition type into IDs
				String s = PartitionTypeDropdown.getText();
				for (int i = 0; i < PartTypeDesc.length; i++) {
					if (s.equals(PartTypeDesc[i])) {
						PartType = PartTypesIDs[i];
						PartTypeSelected = PartTypeDesc[i];
					}
				}
				// Now extract the size.
				s = PartSize.getText();
				SizeMB = Integer.valueOf(s);
				Name = PartName.getText();

				// and return;
				result = true;
				shell.dispose();

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		shell.pack();
	}

	public void close() {
		result = false;
		if (!shell.isDisposed())
			shell.dispose();
	}
}

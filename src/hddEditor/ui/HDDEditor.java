package hddEditor.ui;

/**
 * Main UI.
 */

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import hddEditor.libs.HtmlHelp;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FDD.AMSDiskFile;
import hddEditor.libs.disks.FDD.BadDiskFileException;
import hddEditor.libs.disks.HDD.IDEDosDisk;
import hddEditor.libs.disks.HDD.RS_IDEDosDisk;
import hddEditor.libs.handlers.IDEDosHandler;
import hddEditor.libs.handlers.NonPartitionedDiskHandler;
import hddEditor.libs.handlers.OSHandler;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.ui.partitionPages.FloppyBootTrackPage;
import hddEditor.ui.partitionPages.FloppyGenericPage;
import hddEditor.ui.partitionPages.GenericPage;
import hddEditor.ui.partitionPages.PlusThreePartPage;
import hddEditor.ui.partitionPages.SystemPartPage;

public class HDDEditor {
	public Disk CurrentDisk = null;
	public OSHandler CurrentHandler = null;

	private static String DefaultDropDownText = "<No Disk loaded>";
	// SWT display object
	private Display display = null;

	// SWT shell object
	private Shell shell = null;

	// partition combo.
	private Combo PartitionDropdown = null;

	// Main page containing the partition information.
	private Composite MainPage = null;

	private FileConversionForm fileConvForm = null;
	private FileNewForm fileNewForm = null;
	
	private String helpcontext="Main";

	/**
	 * Make the menus
	 */
	private void MakeMenus() {
		Label label = new Label(shell, SWT.CENTER);
		label.setBounds(shell.getClientArea());

		Menu menuBar = new Menu(shell, SWT.BAR);
		MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuHeader.setText("&File");

		Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
		fileMenuHeader.setMenu(fileMenu);

		MenuItem FileNewItem = new MenuItem(fileMenu, SWT.PUSH);
		FileNewItem.setText("&New");
		FileNewItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				doNewFile();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		MenuItem fileLoadItem = new MenuItem(fileMenu, SWT.PUSH);
		fileLoadItem.setText("&Load");
		fileLoadItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				FileDialog fd = new FileDialog(shell, SWT.OPEN);
				fd.setText("Open");
				String[] filterExt = { "*", "*.img", "*.hdf" };
				fd.setFilterExtensions(filterExt);
				String selected = fd.open();
				if (selected != null) {
					LoadFile(selected);
				}
			}
		});

		MenuItem fileConvertItem = new MenuItem(fileMenu, SWT.PUSH);
		fileConvertItem.setText("&Convert between Raw and HDF");
		fileConvertItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				ShowConvertForm();
			}
		});

		MenuItem fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
		fileExitItem.setText("E&xit");
		fileExitItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				if (CurrentDisk != null) {
					CurrentDisk.close();
				}
				shell.close();
			}
		});

		MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		helpMenuHeader.setText("&Help");

		Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
		helpMenuHeader.setMenu(helpMenu);

		MenuItem helpGetHelpItem = new MenuItem(helpMenu, SWT.PUSH);
		helpGetHelpItem.setText("&Get Help");
		helpGetHelpItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				HtmlHelp.DoHelp(helpcontext);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		shell.setMenuBar(menuBar);
	}

	/**
	 * Make and populate the partition dropdown
	 * 
	 */
	private void MakeDropdown() {
		// Strings to use as list items
		String[] ITEMS = { DefaultDropDownText };

		// Create a dropdown Combo
		PartitionDropdown = new Combo(shell, SWT.DROP_DOWN);
		PartitionDropdown.setItems(ITEMS);
		PartitionDropdown.setText(DefaultDropDownText);
		PartitionDropdown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ComboChanged();
			}
		});
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		PartitionDropdown.setLayoutData(gridData);
	}

	/**
	 * Make and populate the form.
	 * 
	 */

	public void MakeForm() {
		display = new Display();
		shell = new Shell(display);
		shell.setSize(920, 830);
		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (fileConvForm != null) {
					fileConvForm.close();
					fileConvForm = null;
				}
				if (fileNewForm != null) {
					fileNewForm.close();
					fileNewForm = null;
				}
			}
		});

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;
		shell.setLayout(gridLayout);

		MakeMenus();
		MakeDropdown();

		MainPage = new Composite(shell, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.makeColumnsEqualWidth = true;
		MainPage.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, true));
		MainPage.setLayout(gridLayout);

		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				event.doit = true;
			}
		});
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
		display.dispose();
	}

	/**
	 * Main function.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		HDDEditor hdi = new HDDEditor();
		hdi.MakeForm();
		if (args.length>0) {
			hdi.LoadFile(args[0]);
		}
		hdi.loop();
	}

	/**
	 * Load a named file.
	 * 
	 * @param selected
	 */
	public void LoadFile(String selected) {
		System.out.println("Loading " + selected);
		try {
			if (CurrentDisk != null) {
				CurrentDisk.close();
			}
			CurrentDisk = GetCorrectDiskFromFile(selected);
			if (CurrentDisk != null) {
				if (CurrentDisk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_HDD) {				
					CurrentHandler = new IDEDosHandler(CurrentDisk);
				} else if (CurrentDisk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_FDD) {				
					CurrentHandler = new NonPartitionedDiskHandler(CurrentDisk);
				} else {
					System.out.println("Loading failed. - Unable to find OS");
				}
					
				UpdateDropdown();
				shell.setText(selected);
			}
		} catch (IOException e) {
			System.out.println("Loading failed.");
		}
	}

	/**
	 * Try to identify the Disk format
	 * 
	 * @param selected
	 * @return
	 * @throws BadDiskFileException 
	 */
	private Disk GetCorrectDiskFromFile(String selected) {
		Disk result = null;
		try {
			if (new IDEDosDisk().IsMyFileType(new File(selected))) {
				result = new IDEDosDisk(selected);
			} else if (new RS_IDEDosDisk().IsMyFileType(new File(selected))) {
				result = new RS_IDEDosDisk(selected);
			} else if (new AMSDiskFile().IsMyFileType(new File(selected))) {
				result = new AMSDiskFile(selected);
			} else {
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
				messageBox.setMessage("File " + selected + " is not a Raw HD image or RS HDF drive image.");
				messageBox.setText("File " + selected + " is not a Raw HD image or RS HDF drive image.");
				messageBox.open();
			}
/*			System.out.println("Cylinders " + result.GetNumCylinders());
			System.out.println("Heads " + result.GetNumHeads());
			System.out.println("SPT " + result.GetNumSectors()); */
			System.out.println("Using "+result.getClass().getName());
		} catch (Exception e) {
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("Error openning file " + selected + " " + e.getMessage());
			messageBox.setText("Error openning file " + selected + " " + e.getMessage());
			messageBox.open();
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Update the combo box from the partition list.
	 * 
	 */
	public void UpdateDropdown() {
		String entries[] = null;
		if ((CurrentDisk == null) || !CurrentDisk.IsOpen() || CurrentHandler.SystemPart == null) {
			entries = new String[] { DefaultDropDownText };
		} else {
			ArrayList<String> al = new ArrayList<String>();
			for (IDEDosPartition part : CurrentHandler.SystemPart.partitions) {
				if (part.GetPartType() != 0) {
					String s = String.format("%-20s - %-16s %s", part.GetName(), part.GetTypeAsString(),
							GeneralUtils.GetSizeAsString(part.GetSizeK() * 1024));
					al.add(s);
				}
			}
			entries = al.toArray(new String[0]);
		}
		PartitionDropdown.setItems(entries);
		PartitionDropdown.setText(entries[0]);
		ComboChanged();
	}

	/**
	 * Called when the combo box changes
	 * 
	 */
	private void ComboChanged() {
		String current = PartitionDropdown.getText();
		if (current != null) {
			current = current + "                     ";
			String s = current.substring(0, 20).trim();
			GotoPartitionByName(s);
		}
	}

	/**
	 * Go to a named partition. Not case sensitive.
	 *
	 * @param name
	 */
	public void GotoPartitionByName(String name) {
		String searchsstring = name.toUpperCase() + "    ";
		if (!PartitionDropdown.getText().toUpperCase().startsWith(searchsstring)) {
			// find matching item in the drop down list.
			String result = name;
			for (String s : PartitionDropdown.getItems()) {
				if (s.toUpperCase().startsWith(searchsstring)) {
					result = s;
				}
			}
			PartitionDropdown.setText(result);
		}
		IDEDosPartition part = CurrentHandler.GetPartitionByName(name);
		helpcontext = "partition_type_"+String.valueOf( part.GetPartType() );
		PopulatePartitionScreen(part);
	}

	/**
	 * Populate the main form with the appropriate partition type.
	 * 
	 * @param part
	 */
	private void PopulatePartitionScreen(IDEDosPartition part) {
		switch (part.GetPartType()) {
		case PLUSIDEDOS.PARTITION_SYSTEM:
			new SystemPartPage(this, MainPage, part);
			break;
		case PLUSIDEDOS.PARTITION_PLUS3DOS:
			new PlusThreePartPage(this, MainPage, part);
			break;
		case PLUSIDEDOS.PARTITION_BOOT:
			new FloppyBootTrackPage(this, MainPage, part);
			break;
		case PLUSIDEDOS.PARTITION_UNKNOWN:
			new FloppyGenericPage(null, MainPage, part);
			break;
		default:
			new GenericPage(this, MainPage, part);
		}
	}

	/**
	 * Show the conversion form
	 */
	protected void ShowConvertForm() {
		fileConvForm = new FileConversionForm(display);
		fileConvForm.Show();
		fileConvForm = null;
	}

	/**
	 * New file form
	 */
	protected void doNewFile() {
		fileNewForm = new FileNewForm(display);
		String newfile = fileNewForm.Show();
		fileNewForm = null;
		if (newfile != null)
			LoadFile(newfile);
	}

}

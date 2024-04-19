package hddEditor.ui;

/**
 * New file form...
 */

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.disks.HDD.RS_IDEDosDisk;
import hddEditor.libs.disks.HDD.RawHDDFile;
import hddEditor.ui.partitionPages.dialogs.ProgesssForm;

public class FileNewHDDForm {
	// Form components
	private Display display = null;
	private Shell shell = null;

	private Text Targetfile = null;
	private Combo TargetFileType = null;
	private Button SelectTargetFileBtn = null;
	private Button CloseBtn = null;
	private Button CreateBtn = null;

	private Text Cyls = null;
	private Text Heads = null;
	private Text Spt = null;
	private Text SizeMB = null;

	// Result to return.
	private String result = null;

	// Flag to prevent an endless loop when the boxes are being edited.
	private boolean ModInProgress = false;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public FileNewHDDForm(Display display) {
		this.display = display;
	}

	/**
	 * Show the dialog
	 * 
	 * @param title
	 * @param p3d
	 */
	public String Show() {
		Createform();
		return (loop());
	}

	/**
	 * Create the components on the form.
	 * 
	 * @param title
	 */
	private void Createform() {
		shell = new Shell(display);
		shell.setSize(900, 810);

		GridLayout gridLayout = new GridLayout(4, true);
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;

		shell.setLayout(gridLayout);
		shell.setText("Create new HDF file.");

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;

		Targetfile = new Text(shell, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 3;
		gd.widthHint = 600;
		Targetfile.setLayoutData(gd);
		Targetfile.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

		SelectTargetFileBtn = new Button(shell, SWT.BORDER);
		SelectTargetFileBtn.setText("Select Target file");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		SelectTargetFileBtn.setLayoutData(gd);
		SelectTargetFileBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				FileDialog fd = new FileDialog(shell, SWT.SAVE);
				fd.setText("Select Target file");
				fd.setFileName("newfile.hdf");
				String[] filterExt = { "*", "*.img", "*.hdf" };
				fd.setFilterExtensions(filterExt);
				String selected = fd.open();
				if (selected != null) {
					Targetfile.setText(selected);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Label lbl = new Label(shell, SWT.NONE);
		lbl.setText("Target file type:");

		TargetFileType = new Combo(shell, SWT.CHECK);
		String entries[] = { "HDF file (8 bit)", "HDF file (16 bit)", "Raw IMG file (8 bit)", "Raw IMG file (16 bit)" };
		TargetFileType.setItems(entries);
		TargetFileType.setText(entries[0]);

		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("Cyls:");
		Cyls = new Text(shell, SWT.BORDER);
		Cyls.setText("xxxxxxxxxxxxxxxxxxxx");
		Cyls.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				UpdateSizeMBField();
			}
		});
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("Heads:");
		Heads = new Text(shell, SWT.BORDER);
		Heads.setText("xxxxxxxxxxxxxxxxxxxx");
		Heads.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				UpdateSizeMBField();
			}
		});

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("Disk Size Mb:");
		SizeMB = new Text(shell, SWT.BORDER);
		SizeMB.setText("xxxxxxxxxxxxxxxxxxxx");
		SizeMB.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				UpdateCyls();
			}
		});

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("Sectors Per track:");
		Spt = new Text(shell, SWT.BORDER);
		Spt.setText("xxxxxxxxxxxxxxxxxxxx");
		Spt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				UpdateSizeMBField();
			}
		});
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		CreateBtn = new Button(shell, SWT.BORDER);
		CreateBtn.setText("Create new");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		CreateBtn.setLayoutData(gd);
		CreateBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String s = TargetFileType.getText();
				int cyl = Integer.parseInt(Cyls.getText());
				int head = Integer.parseInt(Heads.getText());
				int spt = Integer.parseInt(Spt.getText());

				if (DoCreateFile(s.contains("HDF"), s.contains("8 bit"), Targetfile.getText(), cyl, head, spt)) {
					shell.close();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		CloseBtn = new Button(shell, SWT.BORDER);
		CloseBtn.setText("Close");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		CloseBtn.setLayoutData(gd);
		CloseBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				shell.close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		shell.pack();
		Targetfile.setText("");
		Cyls.setText("16383");
		Heads.setText("16");
		Spt.setText("63");
		SizeMB.setText("8192");
		UpdateCyls();
	}

	/**
	 * THis updates the CYLs text box when the MB size text box is modified
	 */
	protected void UpdateCyls() {
		if (!ModInProgress && !SizeMB.isDisposed())
			try {
				ModInProgress = true;
				try {
					int MBSize = Integer.parseInt(SizeMB.getText());
					MBSize = MBSize * 1024;
					int head = Integer.parseInt(Heads.getText());
					int spt = Integer.parseInt(Spt.getText());
					MBSize = MBSize / head;
					MBSize = MBSize * 1024;
					MBSize = MBSize / spt;
					int sectorsz = 512;
					if (TargetFileType.getText().contains("8 bit")) {
						sectorsz = 256;
					}
					MBSize = MBSize / sectorsz;
					Cyls.setText(String.valueOf(MBSize));
				} finally {
					ModInProgress = false;
				}
			} catch (NumberFormatException e) {
			}

	}

	/**
	 * This updates the size MB fields when either Cyls, Heads, or SPT are modified.
	 */
	protected void UpdateSizeMBField() {
		if (!ModInProgress && !SizeMB.isDisposed())
			try {
				ModInProgress = true;
				try {
					int cyl = Integer.parseInt(Cyls.getText());
					int head = Integer.parseInt(Heads.getText());
					int spt = Integer.parseInt(Spt.getText());
					int sectorsz = 512;
					if (TargetFileType.getText().contains("8 bit")) {
						sectorsz = 256;
					}
					long size = cyl * head * (spt * sectorsz / 1024) / 1024;
					SizeMB.setText(String.valueOf(size));
				} finally {
					ModInProgress = false;
				}
			} catch (NumberFormatException e) {
			}
	}

	/**
	 * Actually create the file.DoCreateFile NOTE: Most of this wants moving to the Disk
	 * objects.
	 * 
	 * @param IsTargetHDF  - TRUE = HDF, FALSE = Raw IMG
	 * @param IsTarget8Bit - TRUE = 8 bit, FALSE = 16 bit
	 * @param targFile
	 * @return TRUE if file successfully created.
	 */
	public boolean DoCreateFile(boolean IsTargetHDF, boolean IsTarget8Bit, String targFile, int cyl, int head,
			int spt) {
		boolean SuccessfullyCreated = false;
		ProgesssForm pf = new ProgesssForm(display);
		try {
			String bitstring = "8-bit";
			if (!IsTarget8Bit) {
				bitstring = "16-bit";
			}
			String hdfstring = "Raw file";
			if (IsTargetHDF) {
				hdfstring = "HDF file";
			}
			
			System.out.println("Openning " + targFile + " for writing... as "+bitstring+" "+hdfstring);
			if (IsTargetHDF) {
				RS_IDEDosDisk rs = new RS_IDEDosDisk();
				SuccessfullyCreated= rs.CreateBlankHDFDisk(new File(targFile), cyl, head, spt, IsTarget8Bit, pf); 
			} else {
				RawHDDFile rs = new RawHDDFile();
				SuccessfullyCreated = rs.CreateBlankRawDisk(new File(targFile), cyl, head, spt, IsTarget8Bit, pf);
			}
		} finally {
			pf.close();
		}
		return (SuccessfullyCreated);
	}

	/**
	 * Dialog loop, open and wait until closed.
	 */
	public String loop() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		shell.dispose();
		return (result);
	}

	/**
	 * Function so the parent form can force-close the form.
	 */
	public void close() {
		shell.close();
		if (!shell.isDisposed()) {
			shell.dispose();
		}
	}

}

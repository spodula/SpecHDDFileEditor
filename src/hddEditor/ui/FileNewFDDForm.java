package hddEditor.ui;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.disks.FDD.AMSDiskFile;
import hddEditor.libs.disks.FDD.SCLDiskFile;
import hddEditor.libs.disks.FDD.TrDosDiskFile;
import hddEditor.libs.disks.LINEAR.MDFMicrodriveFile;
import hddEditor.libs.disks.LINEAR.TAPFile;
import hddEditor.libs.disks.LINEAR.TZXFile;

public class FileNewFDDForm {
	// Form components
	private Display display = null;
	private Shell shell = null;

	private Text Targetfile = null;
	private Combo TargetFileType = null;
	private Button SelectTargetFileBtn = null;
	private Button CloseBtn = null;
	private Button CreateBtn = null;
	private Button AmstradExtendedCB = null;
	private Button trdCompressed = null;
	private Combo TrDosDiskFormat = null;
	private Text DiskLabel = null;

	// Result to return.
	private String result = null;

	// Flag to prevent an endless loop when the boxes are being edited.
	// private boolean ModInProgress = false;

	private FileSelectDialog fsd = null;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public FileNewFDDForm(Display display, FileSelectDialog fsd) {
		this.display = display;
		this.fsd = fsd;
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
		shell.setText("Create new Floppy/Tape file.");

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
				File f = fsd.AskForSingleFileSave(FileSelectDialog.FILETYPE_DRIVE , "Select target file");
				if (f != null) {
					Targetfile.setText(f.getAbsolutePath());
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
		String entries[] = { "Amstrad +3 DSK file", "128K Microdrive cart", "TR-DOS file (TRD/SCL)","TAP file","TZX file" };
		TargetFileType.setItems(entries);
		TargetFileType.setText(entries[0]);
		TargetFileType.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				SetButtonsEnabled();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("TR-DOS disk size (TRD):");
		TrDosDiskFormat = new Combo(shell, SWT.CHECK);
		String trdEntries[] = { "40 Tracks, 1 head (160K)", "40 Tracks, 2 heads (320k)", "80 Tracks, 1 head (316k)",
				"80 Tracks, 2 heads (632k )" };
		TrDosDiskFormat.setItems(trdEntries);
		TrDosDiskFormat.setText(trdEntries[0]);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("Disk/Cart label:");
		DiskLabel = new Text(shell, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		DiskLabel.setLayoutData(gd);
		DiskLabel.setText("XXXXXXXXXXXXXXXXXXXX");
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		new Label(shell, SWT.NONE);
		AmstradExtendedCB = new Button(shell, SWT.CHECK);
		AmstradExtendedCB.setText("Amstrad file: Extended");
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		new Label(shell, SWT.NONE);
		trdCompressed = new Button(shell, SWT.CHECK);
		trdCompressed.setText("TR-DOS: Compressed (SCL)");
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
				if (DoCreateFile(Targetfile.getText(), TargetFileType.getText(), DiskLabel.getText(),
						AmstradExtendedCB.getSelection(), TrDosDiskFormat.getText(),trdCompressed.getSelection())) {
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
		DiskLabel.setText("Untitled");
		SetButtonsEnabled();
	}

	/**
	 * Enable or disable the appropriate buttons.
	 * 
	 */
	private void SetButtonsEnabled() {
		String tftString = TargetFileType.getText().toUpperCase();
		boolean IsAmstradDisk = tftString.contains("AMSTRAD");
		boolean IsTRDOSDisk = tftString.contains("TR-DOS");
		boolean IsMicrodriveCart = tftString.contains("MICRODRIVE");
		
		AmstradExtendedCB.setEnabled(IsAmstradDisk);
		trdCompressed.setEnabled(IsTRDOSDisk);
		TrDosDiskFormat.setEnabled(IsTRDOSDisk);
		DiskLabel.setEnabled(IsTRDOSDisk || IsMicrodriveCart);
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

	/**
	 * Actually create the file.
	 * 
	 * @return TRUE if file successfully created.
	 */
	protected boolean DoCreateFile(String Filename, String Filetype, String Label, boolean AmstradExtended, String TRDosFormat, boolean IsSCLFile) {
		Filetype = Filetype.toUpperCase();
		boolean IsAmstradDisk = Filetype.contains("AMSTRAD");
		boolean IsTRDOSDisk = Filetype.contains("TR-DOS");
		boolean IsMicrodriveCart = Filetype.contains("MICRODRIVE");
		boolean IsTAPFile = Filetype.contains("TAP");
		boolean IsTZXFile = Filetype.contains("TZX");

		boolean createResult = false;
		if (IsAmstradDisk) {
			createResult = CreateAmstradDisk(Filename, AmstradExtended);
		} else if (IsTRDOSDisk) {
			if (IsSCLFile) {
				createResult = CreateSCLDisk(Filename, Label);
			} else {
				String TrDosDiskFormatText = TRDosFormat.toUpperCase();
				int tracks = 40;
				int heads = 1;

				if (TrDosDiskFormatText.contains("80 TRACKS")) {
					tracks = 80;
				}

				if (TrDosDiskFormatText.contains("2 HEADS")) {
					heads = 2;
				}

				createResult = CreateTRDDisk(Filename, tracks, heads, Label);
			}
		} else if (IsMicrodriveCart) {
			createResult = CreateMDFFile(Filename, Label);
		} else if (IsTAPFile) {
			createResult = CreateTAPFile(Filename);
		} else if (IsTZXFile) {
			createResult = CreateTZXFile(Filename);
		}

		if (createResult) {
			this.result = Filename;
		}
		return (createResult);
	}

	/**
	 * Create a blank microdrive cart
	 * 
	 * @param filename
	 * @return
	 */
	private boolean CreateMDFFile(String filename, String Label) {
		boolean result = false;
		try {
			MDFMicrodriveFile mdf = new MDFMicrodriveFile();
			mdf.CreateBlankMicrodriveCart(new File(filename), Label);
			result = true;
		} catch (Exception E) {
			System.out.println("Error creating cart:" + E.getMessage());
			E.printStackTrace();
		}

		return (result);
	}

	/**
	 * Create a blank TAP file.
	 * 
	 * @param filename
	 * @return
	 */
	private boolean CreateTAPFile(String filename) {
		boolean result = false;
		try {
			TAPFile tap = new TAPFile();
			tap.CreateEmptyTapeFile(new File(filename));
			result = true;
		} catch (Exception E) {
			System.out.println("Error creating tape:" + E.getMessage());
			E.printStackTrace();
		}

		return (result);
	}

	/**
	 * Create a blank TZX file.
	 * 
	 * @param filename
	 * @return
	 */
	private boolean CreateTZXFile(String filename) {
		boolean result = false;
		try {
			TZXFile tzx = new TZXFile();
			tzx.CreateEmptyTapeFile(new File(filename));
			result = true;
		} catch (Exception E) {
			System.out.println("Error creating TZX tape:" + E.getMessage());
			E.printStackTrace();
		}

		return (result);
	}

	/**
	 * Create a TR-DOS disk (Uncompressed, .TRD)
	 * 
	 * @param filename
	 * @param tracks
	 * @param heads
	 * @return
	 */
	private boolean CreateTRDDisk(String filename, int tracks, int heads, String Label) {
		boolean result = false;
		try {
			TrDosDiskFile trd = new TrDosDiskFile();
			trd.CreateBlankTRDOSDisk(new File(filename), tracks, heads, Label);
			result = true;
		} catch (Exception E) {
			System.out.println("Error creating Compressed TR-DOS disk:" + E.getMessage());
			E.printStackTrace();
		}

		return (result);

	}

	/**
	 * Create a TR-DOS disk (compressed, .SCL)
	 * 
	 * @param filename
	 * @return
	 */
	private boolean CreateSCLDisk(String filename, String Label) {
		boolean result = false;
		try {
			SCLDiskFile scl = new SCLDiskFile();
			scl.CreateBlankSCLDisk(new File(filename));
			result = true;
		} catch (Exception E) {
			System.out.println("Error creating Compressed TR-DOS disk:" + E.getMessage());
			E.printStackTrace();
		}

		return (result);
	}

	/**
	 * Create an Amstrad .DSK disk
	 * 
	 * @param filename
	 * @param selection
	 * @return
	 */
	private boolean CreateAmstradDisk(String filename, boolean IsExtended) {
		boolean result = false;
		try {
			AMSDiskFile ams = new AMSDiskFile();
			ams.CreateBlankAMSDisk(new File(filename), IsExtended);
//			System.out.println(ams);
			result = true;
		} catch (Exception E) {
			System.out.println("Error creating Amstrad disk:" + E.getMessage());
			E.printStackTrace();
		}

		return (result);
	}

}

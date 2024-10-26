package hddEditor.ui;
/**
 * Convert between file types
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.HDFUtils;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.HDD.IDEDosDisk;
import hddEditor.libs.disks.HDD.RS_IDEDosDisk;
import hddEditor.ui.partitionPages.dialogs.ProgesssForm;

public class FileConversionForm {
	//Form details
	private Display display = null;
	private Shell shell = null;

	private Text Sourcefile = null;
	private Text Targetfile = null;
	private Combo TargetFileType = null;
	private Button SelectSourceFileBtn = null;
	private Button SelectTargetFileBtn = null;
	private Button CloseBtn = null;
	private Button ConvertBtn = null;

	//reset when the conversion is running. Set by the Cancel button 
	private boolean cancelled = false;

	//Set so the form cant close when the conversion is running. 
	private boolean running = false;

	private FileSelectDialog fsd = null;
	
	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public FileConversionForm(Display display, FileSelectDialog fsd) {
		this.display = display;
		this.fsd = fsd;
	}

	/**
	 * Show the dialog
	 * 
	 * @param title
	 * @param p3d
	 */
	public void Show() {
		Createform();
		loop();
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
		shell.setText("Convert Between Hard drive image formats.");

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;

		Sourcefile = new Text(shell, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 3;
		gd.widthHint = 600;
		Sourcefile.setLayoutData(gd);
		Sourcefile.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

		SelectSourceFileBtn = new Button(shell, SWT.BORDER);
		SelectSourceFileBtn.setText("Select Source file");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		SelectSourceFileBtn.setLayoutData(gd);
		SelectSourceFileBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				File Selected = fsd.AskForSingleFileOpen(FileSelectDialog.FILETYPE_DRIVE,"Select source file", new String[] { "*", "*.img", "*.hdf" },"");
				if (Selected != null) {
					Sourcefile.setText(Selected.getAbsolutePath());
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

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
				File Selected = fsd.AskForSingleFileSave(FileSelectDialog.FILETYPE_DRIVE,"Select Target file",new String[] { "*", "*.img", "*.hdf" },"");
				if (Selected != null) {
					Targetfile.setText(Selected.getAbsolutePath());
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


		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		ConvertBtn = new Button(shell, SWT.BORDER);
		ConvertBtn.setText("Convert");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		ConvertBtn.setLayoutData(gd);
		ConvertBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoConvert();
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
				if (!running) {
					shell.close();
				} 
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		shell.pack();
		Sourcefile.setText("");
		Targetfile.setText("");
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
		shell.dispose();
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
	 * Try to identify the Disk format or NULL
	 * 
	 * @param Filename
	 * @return
	 */
	private Disk GetCorrectDiskFromFile(String Filename) {
		File f = new File(Filename);
		Disk result = null;
		try {
			if (new IDEDosDisk().IsMyFileType(f)) {
				result = new IDEDosDisk(f,0,0,0);
			} else if (new RS_IDEDosDisk().IsMyFileType(f)) {
				result = new RS_IDEDosDisk(f);
			} else {
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
				messageBox.setMessage("File " + Filename + " is not a Raw HD image or RS HDF drive image.");
				messageBox.setText("File " + Filename + " is not a Raw HD image or RS HDF drive image.");
				messageBox.open();
			}
		} catch (IOException e) {
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("Error openning file " + Filename + " " + e.getMessage());
			messageBox.setText("Error openning file " + Filename + " " + e.getMessage());
			messageBox.open();
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Actually convert the file
	 */
	protected void DoConvert() {
		ProgesssForm pf = new ProgesssForm(display);
		try {
			//Disable all the buttons
			ConvertBtn.setEnabled(false);
			Sourcefile.setEnabled(false);
			Targetfile.setEnabled(false);
			TargetFileType.setEnabled(false);
			SelectSourceFileBtn.setEnabled(false);
			SelectTargetFileBtn.setEnabled(false);
			CloseBtn.setEnabled(false);
			
			//Set running flags. 
			running = true;
			cancelled = false;
			display.readAndDispatch();

			//Filenames
			String srcfile = Sourcefile.getText();
			String targFile = Targetfile.getText();
			pf.Show("Converting...", "Converting "+new File(srcfile).getName()+" to "+new File(targFile).getName());

			// Open the disk.
			System.out.println("Loading " + srcfile);
			Disk SourceDisk = GetCorrectDiskFromFile(srcfile);

			//File flags
			boolean IsTarget8Bit = TargetFileType.getText().contains("8 bit");
			boolean IsTargetHDF = TargetFileType.getText().contains("HDF");

			System.out.println("Openning " + targFile + " for writing...");
			try {
				//GDS 30 Apr: Bug #1: Converted to use LONGs for sector numbers.
				long ProgressScaleNum = 1;   //Scale value.
				FileOutputStream TargetFile = new FileOutputStream(targFile);
				try {
					//Write HDF file if needed
					if (IsTargetHDF) {
						HDFUtils.WriteHDFFileHeader(SourceDisk, TargetFile, IsTarget8Bit);
					}
					// Write each sector in turn.
					long Numsectors = SourceDisk.GetNumLogicalSectors();
					
					//Bug #1: Calculate the scale for the progress display sector number.
					long ScaledNumSectors = Numsectors;
					while (ScaledNumSectors > 100000) {
						ScaledNumSectors = ScaledNumSectors / 10;
						ProgressScaleNum = ProgressScaleNum * 10; 
					}
					pf.SetMax((int)ScaledNumSectors);
					
					System.out.println("Copying " + Numsectors + " sectors.");
					int SectorSz = SourceDisk.GetSectorSize();
					for (long sectorNum = 0; (sectorNum < Numsectors) && !cancelled; sectorNum++) {
						byte sector[] = SourceDisk.GetBytesStartingFromSector(sectorNum, SectorSz);
						if (IsTarget8Bit & !IsTargetHDF) {
							sector = PLUSIDEDOS.DoubleSector(sector);
						}
						TargetFile.write(sector);
						//update progress
						if (sectorNum % 100000 == 0) {
							System.out.print("\n" + sectorNum + " ");
						}
						if (sectorNum % 2000 == 0) {
							System.out.print(".");
							//Bug #1: Scale the sector number for the progress display
							pf.SetValue((int) (sectorNum / ProgressScaleNum));
							cancelled = pf.IsCancelled();
							display.readAndDispatch();
						}
					}
					System.out.println();
					if (cancelled) {
						System.out.println("Cancelled");
					} else {
						System.out.println("Copied "+Numsectors+" sectors of "+SectorSz+" bytes");
					}
				} finally {
					TargetFile.close();
				}
				System.out.println("Conversion finished.");
			} catch (FileNotFoundException e) {
				System.out.println("Cannot open file " + targFile + " for writing.");
				System.out.println(e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Cannot write to file file " + targFile);
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		} finally {
			//re-enable all buttons and close file.
			ConvertBtn.setEnabled(true);
			CloseBtn.setEnabled(true);
			Sourcefile.setEnabled(true);
			Targetfile.setEnabled(true);
			TargetFileType.setEnabled(true);
			SelectSourceFileBtn.setEnabled(true);
			SelectTargetFileBtn.setEnabled(true);
			running = false;
			pf.close();
		}
	}
}

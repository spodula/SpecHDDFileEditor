package hddEditor.ui;
//TODO: write some help

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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

import hddEditor.libs.HDFUtils;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.ui.partitionPages.dialogs.ProgesssForm;

public class FileNewForm {
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
	
	private String result = null;

	private boolean ModInProgress = false;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public FileNewForm(Display display) {
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
				if (DoCreateFile()) {
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
	 * 
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
	 * 
	 * @return TRUE if file successfully created.
	 */
	protected boolean DoCreateFile() {
		result = null;
		boolean SuccessfullyCreated = false;
		ProgesssForm pf = new ProgesssForm(display);
		try {
			String targFile = Targetfile.getText();
			boolean IsTarget8Bit = TargetFileType.getText().contains("8 bit");
			boolean IsTargetHDF = TargetFileType.getText().contains("HDF");

			System.out.println("Openning " + targFile + " for writing...");
			String s = "8-bit";
			if (!IsTarget8Bit) {
				s = "16-bit";
			}
			if (IsTargetHDF) {
				s = s + " Ramsoft HDF file";
			} else { 
				s = s + " Raw disk image";
			}
			pf.Show("Creating file...", "Creating "+s+" \""+new File(targFile).getName()+"\"");
			try {
				int cyl = Integer.parseInt(Cyls.getText());
				int head = Integer.parseInt(Heads.getText());
				int spt = Integer.parseInt(Spt.getText());

				int sectorSz = 512;
				if (IsTarget8Bit) {
					sectorSz = 256;
				}

				FileOutputStream TargetFile = new FileOutputStream(targFile);
				try {
					if (IsTargetHDF) {
						HDFUtils.WriteHDFFileHeader(TargetFile, IsTarget8Bit, cyl, head, spt);
					}
					// write out an IDEDOS header
					byte SysPart[] = new byte[0x40];
					for (int i = 0; i < SysPart.length; i++) {
						SysPart[i] = 0x00;
					}

					// 0123456789ABCDEF
					byte NameStr[] = "PLUSIDEDOS      ".getBytes();
					System.arraycopy(NameStr, 0, SysPart, 0, 16);
					SysPart[0x10] = PLUSIDEDOS.PARTITION_SYSTEM;

					SysPart[0x11] = 0; // \
					SysPart[0x12] = 0; // /starting cyl=0
					SysPart[0x13] = 0; // starting head=0

					SysPart[0x14] = 0; // \
					SysPart[0x15] = 0; // / End Cylinder
					SysPart[0x16] = 0; // End head

					// Largest logical sector.

					int LastSector = spt;
					SysPart[0x17] = (byte) (LastSector & 0xff); // = 30 sectors
					SysPart[0x18] = 0;
					SysPart[0x19] = 0;
					SysPart[0x1A] = 0;

					/*
					 * System partition specific information
					 */
					// Disk parameters
					SysPart[0x20] = (byte) ((cyl / 0x100) & 0xff);
					SysPart[0x21] = (byte) (cyl & 0xff);
					SysPart[0x22] = (byte) (head & 0xff);
					SysPart[0x23] = (byte) (spt & 0xff);
					int SPC = spt * head;
					SysPart[0x24] = (byte) ((SPC / 0x100) & 0xff);
					SysPart[0x25] = (byte) (SPC & 0xff);

					// Max partitions - Limit to 63 partitons
					int MaxPartitions = (LastSector * sectorSz / 0x40) - 1;
					if (MaxPartitions > 63) {
						MaxPartitions = 63;
					}
					SysPart[0x26] = (byte) ((MaxPartitions / 0x100) & 0xff);
					SysPart[0x27] = (byte) (SPC & 0xff);

					// Colour attribute byte (Paper white, Ink black 0011 1000)
					SysPart[0x27] = (byte) 0x38;

					// Basic attribute byte (Paper white, Ink black 0011 1000)
					SysPart[0x28] = (byte) 0x38;

					// UA, UB, UM, M0 M1 MR, DD left at 0

					if (IsTarget8Bit && !IsTargetHDF) {
						SysPart = DoubleSector(SysPart);
					}

					TargetFile.write(SysPart);
					// Write a blank file for the rest.
					int NumLogicalSectors = (cyl * head * spt) - spt + 1;

					byte oneSector[] = new byte[512];
					if (IsTarget8Bit && !IsTargetHDF) {
						oneSector = new byte[256];
					}
					pf.SetMax(NumLogicalSectors);
					display.readAndDispatch();

					boolean ActionCancelled = false;
					for (int i = 0; (i < NumLogicalSectors) && !ActionCancelled; i++) {
						TargetFile.write(oneSector);
						if (i % 2000 == 0) {
							pf.SetValue(i);
						}
						ActionCancelled = pf.IsCancelled();
					}

					System.out.println();
					if (ActionCancelled) {
						System.out.println("Cancelled");
						pf.setMessage("Cancelled - Flushing work already done...");
						result = null;
					} else {
						result = targFile;
						SuccessfullyCreated = true;
					}
				} finally {
					TargetFile.close();
				}
				System.out.println("Conversion finished.");
			} catch (FileNotFoundException e) {
				System.out.println("Cannot open file " + targFile + " for writing.");
				System.out.println(e.getMessage());
				result = null;
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Cannot write to file file " + targFile);
				System.out.println(e.getMessage());
				result = null;
				e.printStackTrace();
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

	/**
	 * process the sector to expand it to 16 bits
	 * 
	 * @param Sector
	 * @return
	 */
	private byte[] DoubleSector(byte Sector[]) {
		byte result[] = new byte[Sector.length * 2];

		int ptr = 0;
		for (int i = 0; i < Sector.length; i++) {
			result[ptr++] = Sector[i];
			result[ptr++] = 0;
		}
		return (result);
	}

}

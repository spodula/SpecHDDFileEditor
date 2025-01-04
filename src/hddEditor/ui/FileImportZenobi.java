package hddEditor.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.DiskUtils;
import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.Speccy;
import hddEditor.libs.SpeccyFileEncoders;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.handlers.OSHandler;
import hddEditor.libs.partitions.IDEDosPartition;

public class FileImportZenobi {
	public class ZenobiDetails {
		public FileEntry binfile[] = null;
		public int startaddress = 0;
		public int loaderpoke = 0;
		public int clear = 0;

		public FileEntry Font = null;

		public FileEntry screen = null;
		public boolean ScreenUncompress = false;
		public int ScreenUncompressAddress = 0;
		public String errors = "";

		/**
		 * return details as a string.
		 */
		private String nameornull(FileEntry f) {
			if (f == null) {
				return ("null");
			} else {
				return f.GetFilename();
			}
		}

		@Override
		public String toString() {
			String result = "Bin files: " + nameornull(binfile[0]) + "," + nameornull(binfile[1]) + ","
					+ nameornull(binfile[2]) + "  Start: " + startaddress + " Loaderpoke: " + loaderpoke + " Font:"
					+ nameornull(Font) + " Screen:" + nameornull(screen) + " Compressed:" + ScreenUncompress
					+ " Address:" + ScreenUncompressAddress + " Clear:" + clear + " Errors:" + errors;
			return (result);
		}

		public boolean CanImport() {
			boolean result = (binfile[0] != null) && (binfile[1] != null) && (binfile[2] != null) && (startaddress > 0)
					&& (loaderpoke > 0) && (errors.isBlank());

			return (result);
		}

		public ZenobiDetails() {
			binfile = new FileEntry[3];
			errors = "";
		}

	}

	private static String FormTitle = "Import Zenobi Tape";

	private ZenobiDetails zDets;

	// Form details
	private Display display = null;
	private Shell shell = null;

	// Source file to import
	private Label Sourcefile = null;

	// Button to set this
	private Button SelectSourceFileBtn = null;

	// Source partition within the file.
	private Combo SourcePartition = null;

	// Close and import buttons
	private Button CloseBtn = null;
	private Button ImportBtn = null;

	// Target filename
	private Text TargFilename = null;

	// Error text field.
	private Text ErrorText = null;

	// table of files to import
	private Table SourceList = null;

	// Source disk
	private Disk CurrentSourceDisk = null;

	// Source Handler
	private OSHandler CurrentSourceHandler = null;

	// Target handler
	private OSHandler CurrentTargetHandler;

	// Target partition
	private Combo TargetPartition = null;

	private FileSelectDialog fsd = null;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public FileImportZenobi(Display display, OSHandler handler, FileSelectDialog fsd) {
		this.display = display;
		this.CurrentTargetHandler = handler;
		this.fsd = fsd;
	}

	/**
	 * Show the dialog
	 * 
	 * @param title
	 * @param p3d
	 */
	public void Show(String defaultPart) {
		Createform(defaultPart);
		loop();
	}

	/**
	 * Create the components on the form.
	 * 
	 * @param title
	 */
	private void Createform(String defaultPartition) {
		shell = new Shell(display);
		shell.setSize(900, 810);

		GridLayout gridLayout = new GridLayout(4, true);
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;

		shell.setLayout(gridLayout);
		shell.setText(FormTitle);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;

		Sourcefile = new Label(shell, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 3;
		gd.widthHint = 600;
		Sourcefile.setLayoutData(gd);
		Sourcefile.setAlignment(SWT.CENTER);
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
				File selected = fsd.AskForSingleFileOpen(FileSelectDialog.FILETYPE_IMPORTDRIVE,
						"Select file to import.", HDDEditor.SUPPORTEDFILETYPES, "");
				if (selected != null) {
					Sourcefile.setText(selected.getAbsolutePath());
					DoLoadFile(selected);
				}
				shell.forceActive();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Label lbl = new Label(shell, SWT.NONE);
		lbl.setText("Source partition:");

		SourcePartition = new Combo(shell, SWT.CHECK);
		String entries[] = { "" };
		SourcePartition.setItems(entries);
		SourcePartition.setText(entries[0]);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 3;
		SourcePartition.setLayoutData(gd);
		SourcePartition.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DoPopulateFileList();
			}
		});

		Button btn = new Button(shell, SWT.BORDER);
		btn.setText("Select all");
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (SourceList != null) {
					for (TableItem itm : SourceList.getItems()) {
						itm.setChecked(true);
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		btn = new Button(shell, SWT.BORDER);
		btn.setText("Select None");
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (SourceList != null) {
					for (TableItem itm : SourceList.getItems()) {
						itm.setChecked(false);
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		btn = new Button(shell, SWT.BORDER);
		btn.setText("Invert selection");
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (SourceList != null) {
					for (TableItem itm : SourceList.getItems()) {
						itm.setChecked(!itm.getChecked());
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		lbl = new Label(shell, SWT.NONE);

		SourceList = new Table(shell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.heightHint = 200;
		gd.verticalSpan = 2;
		SourceList.setLayoutData(gd);
		SourceList.setHeaderVisible(true);

		TableColumn tc1 = new TableColumn(SourceList, SWT.LEFT);
		TableColumn tc2 = new TableColumn(SourceList, SWT.FILL);
		tc1.setText("Filename");
		tc2.setText("Notes");
		tc1.setWidth(250);
		tc2.setWidth(250);

		Label txtlbl = new Label(shell, SWT.NONE);
		txtlbl.setText("Base filename:");

		TargFilename = new Text(shell, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		TargFilename.setLayoutData(gd);

		txtlbl = new Label(shell, SWT.NONE);
		txtlbl.setText("Target partition:");
		TargetPartition = new Combo(shell, SWT.CHECK);
		String itms[] = new String[CurrentTargetHandler.SystemPart.partitions.length];
		int itmptr = 0;
		for (IDEDosPartition part : CurrentTargetHandler.SystemPart.partitions) {
			itms[itmptr++] = part.GetName();
		}

		TargetPartition.setItems(itms);
		TargetPartition.setText(defaultPartition);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		TargetPartition.setLayoutData(gd);

		ErrorText = null;

		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		ImportBtn = new Button(shell, SWT.BORDER);
		ImportBtn.setText("Import");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		ImportBtn.setLayoutData(gd);
		ImportBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoImport();
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
		Sourcefile.setText("");
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
	 * 
	 * @param selected
	 */
	protected void DoLoadFile(File selected) {
		try {
			if (CurrentSourceDisk != null) {
				CurrentSourceDisk.close();
			}
			CurrentSourceDisk = DiskUtils.GetCorrectDiskFromFile(selected);
			CurrentSourceHandler = DiskUtils.GetHandlerForDisk(CurrentSourceDisk);
			String entries[] = null;

			ArrayList<String> al = new ArrayList<String>();
			for (IDEDosPartition part : CurrentSourceHandler.SystemPart.partitions) {
				if (part.GetPartType() != 0) {
					String s = String.format("%-20s - %-16s %s", part.GetName(),
							PLUSIDEDOS.GetTypeAsString(part.GetPartType()),
							GeneralUtils.GetSizeAsString(((long) part.GetSizeK()) * (long) 1024));
					al.add(s);
				}
			}
			entries = al.toArray(new String[0]);
			SourcePartition.setItems(entries);

			// QOL improvement, GDS 11 Jan: if we are NOT dealing with a hard drive, default
			// to probably the only real partition.
			if (CurrentSourceDisk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_HDD) {
				SourcePartition.setText(entries[0]);
			} else {
				SourcePartition.setText(entries[entries.length - 1]);
			}
			DoPopulateFileList();

		} catch (IOException e) {
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
			messageBox.setMessage(e.getMessage());
			messageBox.setText(e.getMessage());
			messageBox.open();
			System.out.println("Loading failed. " + e.getMessage());
		}
	}

	private void DoPopulateFileList() {
		SourceList.clearAll();
		SourceList.removeAll();

		String PossName = "UNNAMED";
		String current = SourcePartition.getText();
		if (current != null) {
			current = current + "                     ";
			String s = current.substring(0, 20).trim();
			IDEDosPartition part = CurrentSourceHandler.GetPartitionByName(s);
			FileEntry fl[] = part.GetFileList();
			String[] notes = GetNotes(fl);
			int i = 0;
			if (fl != null) {
				for (FileEntry f : fl) {
					if (notes[i].startsWith("Bin part")) {
						PossName = (f.GetFilename() + "          ").substring(0, 8);
					}
					TableItem itm = new TableItem(SourceList, SWT.NONE);
					itm.setChecked(true);
					String cols[] = { f.GetFilename() + " - " + f.GetFileTypeString() + " - "
							+ String.valueOf(f.GetFileSize()) + " bytes", notes[i++] };
					itm.setData(f);
					itm.setText(cols);
				}
			}
		}
		TargFilename.setText(PossName.trim().toUpperCase());
		ImportBtn.setEnabled(zDets.CanImport());

		if (!zDets.CanImport()) {
			ErrorText = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 4;
			gd.heightHint = 50;
			ErrorText.setLayoutData(gd);
			ErrorText.setVisible(true);
			if (!zDets.errors.isBlank()) {
				ErrorText.setText(zDets.errors);
			} else {
				String s = "File does not seem to be a Zenobi PAWS file...";
				ErrorText.setText(s);
			}

			shell.pack();
		} else {
			if (ErrorText != null) {
				ErrorText.dispose();
				ErrorText = null;
				shell.pack();
			}
		}

	}

	/**
	 * 
	 * 
	 */
	protected void DoImport() {
		try {
			int targetpartnum = -1;
			String content = TargetPartition.getText().trim();
			String indexes[] = TargetPartition.getItems();
			for (int cnt = 0; cnt < indexes.length; cnt++) {
				if (indexes[cnt].equals(content)) {
					targetpartnum = cnt;
				}
			}

			if (targetpartnum != -1) {
				IDEDosPartition TargetPartition = CurrentTargetHandler.SystemPart.partitions[targetpartnum];
				String basefilename = TargFilename.getText().toUpperCase().trim();

				// Assemble and save BIN file
				byte ram[] = new byte[0x10000];
				int startaddress = 0xffff;
				for (int i = 0; i < zDets.binfile.length; i++) {
					FileEntry currentfile = zDets.binfile[i];
					SpeccyBasicDetails dets = currentfile.GetSpeccyBasicDetails();
					System.arraycopy(currentfile.GetFileData(), 0, ram, dets.LoadAddress, currentfile.GetFileSize());
					if (startaddress > dets.LoadAddress) {
						startaddress = dets.LoadAddress;
					}
					System.out.println(
							"Bin file " + i + " start:" + dets.LoadAddress + " fs: " + currentfile.GetFileSize());
				}
				int newlen = 0x10000 - startaddress;
				byte ram2[] = new byte[newlen];
				System.arraycopy(ram, startaddress, ram2, 0, newlen);
				TargetPartition.AddCodeFile(basefilename + ".BIN", startaddress, ram2);

				// if screen valid, add screen
				if (zDets.screen != null) {
					startaddress = 0x4000;
					String ext = ".SCR";
					if (zDets.ScreenUncompress) {
						ext = ".SCP";
						startaddress = zDets.screen.GetSpeccyBasicDetails().LoadAddress;
					}

					TargetPartition.AddCodeFile(basefilename + ext, startaddress, zDets.screen.GetFileData());
				}

				// create basic loader.
				ArrayList<String> newbasic = new ArrayList<String>();
				// CLEAR
				newbasic.add("10 PAPER 0:BORDER 0:CLEAR " + zDets.clear);
				// SCREEN if required
				if (zDets.screen != null) {
					if (zDets.ScreenUncompress) {
						newbasic.add("20 LOAD \"" + basefilename + ".SCP\" CODE " + zDets.ScreenUncompressAddress);
						newbasic.add("30 RANDOMISE USR " + zDets.ScreenUncompressAddress);

					} else {
						newbasic.add("20 LOAD \"" + basefilename + ".SCR\" SCREEN$");
					}
				}
				// Actual binary data...
				newbasic.add("40 LOAD \"" + basefilename + ".BIN\" CODE");
				// POKES to bypass the loader
				newbasic.add("50 POKE " + zDets.loaderpoke + ",0");
				newbasic.add("60 POKE " + (zDets.loaderpoke + 1) + ",0");
				newbasic.add("70 POKE " + (zDets.loaderpoke + 2) + ",0");
				// randomize usr
				newbasic.add("80 RANDOMISE USR " + zDets.startaddress);

				// Encode basic:
				byte BasicAsBytes[] = new byte[0xffff];
				int targetPtr = 0;

				for (String line : newbasic) {
					targetPtr = Speccy.DecodeBasicLine(line, BasicAsBytes, targetPtr);
					System.out.println(line);
				}
				// Copy to an array of the correct size.
				byte data[] = new byte[targetPtr];
				System.arraycopy(BasicAsBytes, 0, data, 0, targetPtr);

				TargetPartition.AddBasicFile(basefilename + ".BAS", data, 10, data.length);

				shell.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
			messageBox.setMessage(e.getMessage());
			messageBox.setText(e.getMessage());
			messageBox.open();
			System.out.println("Copy failed. " + e.getMessage());
		}
	}

	private String[] GetNotes(FileEntry[] directory) {
		zDets = new ZenobiDetails();

		String result[] = new String[directory.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = "";
		}

		// basic run...
		int lastBasic = -1;
		for (int dirIndex = 0; dirIndex < directory.length; dirIndex++) {
			FileEntry f = directory[dirIndex];
			if (f.GetSpeccyBasicDetails().BasicType == -1) {
				result[dirIndex] = "Invalid file.";
			} else if (f.GetSpeccyBasicDetails().BasicType == Speccy.BASIC_BASIC) {
				lastBasic = dirIndex;
			} else
				try {
					String filename = f.GetFilename();
					if (f.GetFileData() != null && f.GetFileData().length == 6912) {
						result[dirIndex] = "Uncompressed screen";
						zDets.screen = f;
					} else if (f.GetFileData() != null && f.GetFileData().length == 768) {
						result[dirIndex] = "Font file";
						zDets.Font = f;
					} else if (filename.endsWith("@")) {
						zDets.binfile[0] = f;

						// Now to search for loader hack...
						int baseaddress = f.GetSpeccyBasicDetails().LoadAddress;
						byte data[] = f.GetFileData();
						// search for the sequence: DD E5 CD 08 07 DD E1
						int foundaddress = locate_bytestream(data,
								new int[] { 0xDD, 0xE5, 0xCD, 0x08, 0x07, 0xDD, 0xE1 }, 0);
						if (foundaddress == -1) {
							result[dirIndex] = "Bin part 1 (Cant find loader)";
						} else {
							result[dirIndex] = "Bin part 1 (Loader poke:" + (foundaddress + 2 + baseaddress) + ")";
							zDets.loaderpoke = foundaddress + 2 + baseaddress;
						}

					} else if (filename.endsWith("A")) {
						result[dirIndex] = "Bin part 2";
						zDets.binfile[1] = f;
					} else if (filename.endsWith("B")) {
						result[dirIndex] = "Bin part 3";
						zDets.binfile[2] = f;
					} else if (filename.endsWith("C")) {
						result[dirIndex] = "128K file";
						zDets.errors = zDets.errors + "Looks like a 128K PAWS file. not currently supported.";
					} else {
						// unidentified BIN file...
						result[dirIndex] = "Compressed screen? (uses basic: " + directory[lastBasic].GetFilename()
								+ ")";
						zDets.screen = f;
						zDets.ScreenUncompress = true;

					}
				} catch (IOException E) {
					zDets.errors = zDets.errors + E.getMessage() + System.lineSeparator();
				}
		}
		result[lastBasic] = "Real loader";
		// Next parse out the information from the real loader... Find all instances of
		// RANDOMIZE USR
		try {
			byte basicdata[] = directory[lastBasic].GetFileData();
			int address = 0;
			double finalru = -1;
			double firstru = -1;
			int numru = 0;
			while (address > -1) {
				address = locate_bytestream(basicdata, new int[] { 0xF9, 0xC0 }, address + 1);
				if (address != -1) {
					// Decode the following number
					while (basicdata[address] != 0x0e) {
						address++;
					}
					finalru = Speccy.GetNumberAtByte(basicdata, address + 1);
					if (numru == 0) {
						firstru = finalru;
					}
					numru++;
					if (numru > 2) {
						zDets.errors = zDets.errors
								+ "More than 2 RANDOMIZE USR statements found. May meed manual intervention."
								+ System.lineSeparator();
					}
				}
			}
			// Updates the notes screen.
			result[lastBasic] = "Real loader - Start address:" + finalru;
			zDets.startaddress = (int) Math.floor(finalru);
			if (numru > 1) {
				for (int i = 0; i < result.length; i++) {
					if (result[i].contains("Compressed screen")) {
						result[i] = result[i] + " (Run address: " + firstru + ")";
						zDets.ScreenUncompressAddress = (int) Math.floor(firstru);
					}
				}
			}
			// locate CLEAR
			address = 0;
			while (address < basicdata.length) {
				if (ByteComp(basicdata[address], 0xFD)) {
					// expecting 5 digits, 0x0E, 5 bytes
					byte possnum[] = new byte[11];
					System.arraycopy(basicdata, address + 1, possnum, 0,
							Math.min(possnum.length, basicdata.length - address));
					// find the 0x0e
					int numstartloc = 0;
					while (numstartloc < 11) {
						byte b = possnum[numstartloc++];
						// If we have found our start of number marker.
						if (b == 0x0E) {
							break;
						}
						// check if the bytes leading up to the 0x0e are numeric...
						if ((b < 0x30) || (b > 0x39)) {
							// if not, fail out
							numstartloc = 11;
						}
					}
					if (numstartloc != 11) {
						zDets.clear = (int) Speccy.GetNumberAtByte(possnum, numstartloc);
						result[lastBasic] = result[lastBasic] + " Clear:" + zDets.clear;
					}
				}
				address++;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	private boolean ByteComp(byte b, int i) {
		int data = b;
		if (data < 0) {
			data = data + 256;
		}
		return (data == i);
	}

	private int locate_bytestream(byte[] source, int[] target, int start) {
		int foundaddress = -1;
		int addr = start;
		for (; addr < source.length - target.length; addr++) {
			boolean matched = true;
			for (int arrind = 0; arrind < target.length; arrind++) {
				if (!ByteComp(source[addr + arrind], target[arrind])) {
					matched = false;
					break;
				}
			}
			if (matched) {
				foundaddress = addr;
				break;
			}
		}
		return (foundaddress);
	}
}

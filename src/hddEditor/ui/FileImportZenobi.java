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
import hddEditor.libs.Languages;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.handlers.OSHandler;
import hddEditor.libs.partitions.IDEDosPartition;

public class FileImportZenobi {
	public class ZenobiDetails {
		// Files containing the main binary data (usually three of them)
		public FileEntry binfile[] = null;

		// Start address decoded from the BASIC loader
		public int startaddress = 0;

		// Address of the bytes identified as the main loader to 0807 to be poked out.
		public int loaderpoke = 0;

		// Value of the CLEAR...
		public int clear = 0;

		// Font file is any
		public FileEntry Font = null;

		// Screen file if any
		public FileEntry screen = null;

		// if TRUE, the screen is compressed and need to put in a RANDOMIZE USR to
		// decompress it.
		public boolean ScreenUncompress = false;

		// Address of the Randomize usr to decompress the screen
		public int ScreenUncompressAddress = 0;

		// Any errors when decoding tape file.
		public String errors = "";

		// Some versions use EXTVEL+xx to start the program. This stores it if required
		public int extvec = 0;

		// any EXTVEC lines
		public String excveclines[] = null;

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

		/**
		 * Do we have enough information to create a loader?
		 * 
		 * @return
		 */
		public boolean CanImport() {
			boolean result = (binfile[0] != null) && (binfile[1] != null) && (binfile[2] != null) && (loaderpoke > 0);

			return (result);
		}

		/**
		 * Constructor
		 */
		public ZenobiDetails() {
			binfile = new FileEntry[3];
			errors = "";
		}

	}

	// The base form title.
	private static String FormTitle = "Import Zenobi Tape";

	// The details of the currently loaded file.
	private ZenobiDetails zDets[];

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

	// Default file select dialog;.
	private FileSelectDialog fsd = null;

	private Languages lang;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public FileImportZenobi(Display display, OSHandler handler, FileSelectDialog fsd, Languages lang) {
		this.display = display;
		this.CurrentTargetHandler = handler;
		this.fsd = fsd;
		this.lang = lang;
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
		SelectSourceFileBtn.setText(lang.Msg(Languages.MSG_SELSOURCE));
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		SelectSourceFileBtn.setLayoutData(gd);
		SelectSourceFileBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				File selected = fsd.AskForSingleFileOpen(FileSelectDialog.FILETYPE_IMPORTDRIVE,
						lang.Msg(Languages.MSG_SELFILEIMP) + ".", HDDEditor.SUPPORTEDFILETYPES, "");
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
		lbl.setText(lang.Msg(Languages.MSG_SOURCEPART) + ":");

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
		btn.setText(lang.Msg(Languages.MSG_SELECTALL));
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
		btn.setText(lang.Msg(Languages.MSG_SELECTNONE));
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
		btn.setText(lang.Msg(Languages.MSG_INVSEL));
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
		tc1.setText(lang.Msg(Languages.MSG_FILENAME));
		tc2.setText(lang.Msg(Languages.MSG_NOTES));
		tc1.setWidth(250);
		tc2.setWidth(250);

		Label txtlbl = new Label(shell, SWT.NONE);
		txtlbl.setText(lang.Msg(Languages.MSG_NOTES) + ":");

		TargFilename = new Text(shell, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		TargFilename.setLayoutData(gd);

		txtlbl = new Label(shell, SWT.NONE);
		txtlbl.setText(lang.Msg(Languages.MSG_TARGETPART) + ":");
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
		ImportBtn.setText(lang.Msg(Languages.MSG_IMPORT));
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
		CloseBtn.setText(lang.Msg(Languages.MSG_CLOSE));
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
	 * Load the selected file and parse out its details.
	 * 
	 * @param selected
	 */
	protected void DoLoadFile(File selected) {
		try {
			// Extract the current disk handler for the file to be imported.
			if (CurrentSourceDisk != null) {
				CurrentSourceDisk.close();
			}
			CurrentSourceDisk = DiskUtils.GetCorrectDiskFromFile(selected);
			CurrentSourceHandler = DiskUtils.GetHandlerForDisk(CurrentSourceDisk);
			String entries[] = null;

			// Create a list of all partitions. Note this only really applies in the case of
			// hard disks.
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
			System.out.println(lang.Msg(Languages.MSG_CANTLOAD) + ". " + e.getMessage());
		}
	}

	/**
	 * Populate the form from the currently selected source disk and partition.
	 */
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
					if (notes[i].contains("Bin part")) {
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
		ImportBtn.setEnabled(zDets[0].CanImport());

		if (!zDets[0].CanImport()) {
			if (ErrorText == null) {
				ErrorText = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			}
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 4;
			gd.heightHint = 50;
			ErrorText.setLayoutData(gd);
			ErrorText.setVisible(true);
			for (ZenobiDetails z : zDets) {
				if (!zDets[0].errors.isBlank()) {
					ErrorText.setText(z.errors);
				} else {
					String s = lang.Msg(Languages.MSG_ZENOBI_INCORRECT) + "...";
					if (zDets[0].loaderpoke == 0) {
						s = s + "\n" + lang.Msg(Languages.MSG_ZENOBI_CANTFINDLOADER) + ".";
					}
					if (zDets[0].startaddress == -1) {
						s = s + "\n" + lang.Msg(Languages.MSG_ZENOBI_CANTFINDSTART);
					}

					ErrorText.setText(s);
				}
			}

			shell.pack();
		} else {
			if (ErrorText != null) {
				ErrorText.setText("");
				shell.pack();
			}
		}

	}

	/**
	 * Actually perform the conversion.
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

				int entrynum = 1;
				for (ZenobiDetails z : zDets) {
					String basefilename = TargFilename.getText().toUpperCase().trim();
					if ((zDets.length > 0) && (entrynum > 1)) {
						basefilename = basefilename + String.valueOf(entrynum++);
					} else {
						entrynum++;
					}

					// Assemble and save BIN file
					byte ram[] = new byte[0x10000];
					int startaddress = 0xffff;
					for (int i = 0; i < z.binfile.length; i++) {
						FileEntry currentfile = z.binfile[i];
						SpeccyBasicDetails dets = currentfile.GetSpeccyBasicDetails();
						System.arraycopy(currentfile.GetFileData(), 0, ram, dets.LoadAddress,
								currentfile.GetFileSize());
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
					if (z.screen != null) {
						startaddress = 0x4000;
						String ext = ".SCR";
						if (z.ScreenUncompress) {
							ext = ".SCP";
							startaddress = z.screen.GetSpeccyBasicDetails().LoadAddress;
						}

						TargetPartition.AddCodeFile(basefilename + ext, startaddress, z.screen.GetFileData());
					}

					// create basic loader.
					ArrayList<String> newbasic = new ArrayList<String>();
					// CLEAR
					newbasic.add("10 PAPER 0:BORDER 0:CLEAR " + z.clear);
					// SCREEN if required
					if (z.screen != null) {
						if (z.ScreenUncompress) {
							newbasic.add("20 LOAD \"" + basefilename + ".SCP\" CODE " + z.ScreenUncompressAddress);
							newbasic.add("30 RANDOMISE USR " + z.ScreenUncompressAddress);

						} else {
							newbasic.add("20 LOAD \"" + basefilename + ".SCR\" SCREEN$");
						}
					}
					// Actual binary data...
					newbasic.add("40 LOAD \"" + basefilename + ".BIN\" CODE");
					// POKES to bypass the loader
					newbasic.add("50 POKE " + z.loaderpoke + ",0");
					newbasic.add("60 POKE " + (z.loaderpoke + 1) + ",0");
					newbasic.add("70 POKE " + (z.loaderpoke + 2) + ",0");

					// If we have found any EXTVEC sections, use those instead of the last randomize
					// usr.
					if (z.excveclines != null) {
						int lineNo = 80;
						for (String line : z.excveclines) {
							newbasic.add(lineNo + " " + line);
							lineNo = lineNo + 10;
						}
					} else {
						// randomize usr
						newbasic.add("80 RANDOMISE USR " + z.startaddress);
					}

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
				}

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

	/**
	 * Try to figure out the file details, create the notes for the form and try to
	 * parse into one or more zDets object.
	 * 
	 * @param directory
	 * @return
	 */
	private String[] GetNotes(FileEntry[] directory) {
		ArrayList<ZenobiDetails> zdl = new ArrayList<ZenobiDetails>();

		ZenobiDetails currentzd = new ZenobiDetails();
		int entrynum = 1;

		String result[] = new String[directory.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = "";
		}

		// basic run...
		int lastBasic = -1;
		int StartOfBlock = 0;
		for (int dirIndex = 0; dirIndex < directory.length; dirIndex++) {
			FileEntry f = directory[dirIndex];
			if (f.GetSpeccyBasicDetails().BasicType == -1) {
				result[dirIndex] = lang.Msg(Languages.MSG_INVALIDFILE) + ".";
			} else if (f.GetSpeccyBasicDetails().BasicType == Speccy.BASIC_BASIC) {
				lastBasic = dirIndex;
			} else
				try {
					String filename = f.GetFilename();
					if (f.GetFileData() != null && f.GetFileData().length == 6912) {
						// uncompressed screens will always override an attempt at a compressed
						result[dirIndex] = entrynum + "." + lang.Msg(Languages.MSG_UNCOMPRESSEDSCREEN);
						currentzd.screen = f;
						currentzd.ScreenUncompress = false;
					} else if (f.GetFileData() != null && f.GetFileData().length == 768) {
						result[dirIndex] = entrynum + "." + lang.Msg(Languages.MSG_FONTFILE);
						currentzd.Font = f;
					} else if (filename.endsWith("@")) {
						currentzd.binfile[0] = f;
						// Now to search for loader hack...
						int baseaddress = f.GetSpeccyBasicDetails().LoadAddress;
						byte data[] = f.GetFileData();
						// search for the sequence: DD E5 CD 08 07 DD E1
						int foundaddress = locate_bytestream(data,
								new int[] { 0xDD, 0xE5, 0xCD, 0x08, 0x07, 0xDD, 0xE1 }, 0);
						if (foundaddress == -1) {
							result[dirIndex] = entrynum + ".Bin part 1 ("
									+ lang.Msg(Languages.MSG_ZENOBI_CANTFINDLOADER) + ")";
						} else {
							result[dirIndex] = entrynum + ".Bin part 1 (" + lang.Msg(Languages.MSG_LOADERPOKE) + ":"
									+ (foundaddress + 2 + baseaddress) + ")";
							currentzd.loaderpoke = foundaddress + 2 + baseaddress;
						}

					} else if (filename.endsWith("A")) {
						result[dirIndex] = entrynum + ".Bin part 2";
						currentzd.binfile[1] = f;
					} else if (filename.endsWith("B")) {
						result[dirIndex] = entrynum + ".Bin part 3";
						currentzd.binfile[2] = f;
						// as this will be the end of the section, restart with a new one.
						try {
							result = ParseBasicFile(currentzd, directory[lastBasic].GetFileData(), result, lastBasic,
									StartOfBlock, entrynum++);
							zdl.add(currentzd);
							currentzd = new ZenobiDetails();
							StartOfBlock = dirIndex;
						} catch (IOException e) {
							e.printStackTrace();
						}

					} else if (filename.endsWith("C")) {
						result[dirIndex] = lang.Msg(Languages.MSG_128KFILE);
						currentzd.errors = currentzd.errors + lang.Msg(Languages.MSG_128KPAWS) + ".";
					} else {
						// unidentified BIN file... If we havent found a screen, see if this is it.
						if (currentzd.screen == null) {
							result[dirIndex] = entrynum + ". " + lang.Msg(Languages.MSG_COMPRESSEDSCREEN)
									+ "? (uses basic: " + directory[lastBasic].GetFilename() + ")";
							currentzd.screen = f;
							currentzd.ScreenUncompress = true;
						} else {
							result[dirIndex] = lang.Msg(Languages.MSG_UNKNOWNFILE);
						}

					}
				} catch (IOException E) {
					currentzd.errors = currentzd.errors + E.getMessage() + System.lineSeparator();
				}
		}

		zDets = zdl.toArray(new ZenobiDetails[zdl.size()]);

		return result;
	}

	/**
	 * This parses the identified BASIC loader, Fills out Compression RANDOMIZE USR
	 * and the game start RANDOMIZE USR. and the notes associated with the files.
	 * 
	 * @param currentzd    Current ZenobiDetails object being populated
	 * @param basicdata    Raw file data of the BASIC file.
	 * @param currentnotes Array of the current notes for the current file
	 * @param lastbasic    index of the BASIC loader in the Notes
	 * @param blockstart   index of the start of the current block within the file.
	 *                     Note, this is not necessarily the loader being parsed
	 *                     some files have two basic files, the first one loading
	 *                     the screen.
	 * @return new notes
	 */
	private String[] ParseBasicFile(ZenobiDetails currentzd, byte basicdata[], String currentnotes[], int lastbasic,
			int blockstart, int entrynum) {
		currentnotes[lastbasic] = entrynum + "." + lang.Msg(Languages.MSG_REALLOADER);
		int address = 0;
		double finalru = -1;
		double firstru = -1;
		int numru = 0;
		while ((address > -1) && (address < basicdata.length)) {
			address = locate_bytestream(basicdata, new int[] { 0xF9, 0xC0 }, address + 1);
			if ((address != -1) && (address < basicdata.length)) {
				// Decode the following number
				while ((address < basicdata.length) && (basicdata[address] != 0x0e)) {
					address++;
				}
				if (address < basicdata.length) {
					finalru = Speccy.GetNumberAtByte(basicdata, address + 1);
					if (numru == 0) {
						firstru = finalru;
					}
					numru++;
					if (numru > 2) {
						currentzd.errors = currentzd.errors + entrynum + "." + lang.Msg(Languages.MSG_CONFUSINGLOADER)
								+ "." + System.lineSeparator();
					}
				}
			}
		}
		// Updates the notes screen.
		currentnotes[lastbasic] = entrynum + "." + lang.Msg(Languages.MSG_REALLOADER) + " - "
				+ lang.Msg(Languages.MSG_STARTADDRESS) + ":" + finalru;
		currentzd.startaddress = (int) Math.floor(finalru);
		if (numru > 1) {
			for (int i = blockstart; i < currentnotes.length; i++) {
				if (currentnotes[i].contains(lang.Msg(Languages.MSG_COMPRESSEDSCREEN))) {
					currentnotes[i] = currentnotes[i] + " (" + lang.Msg(Languages.MSG_RUNADDRESS) + ": " + firstru
							+ ")";
					currentzd.ScreenUncompressAddress = (int) Math.floor(firstru);
				}
			}
		}
		// locate CLEAR.
		// This should be 0xFD <up to 5 bytes between '0' and '9'> 0x0E <5 bytes FP
		// number>
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
					currentzd.clear = (int) Speccy.GetNumberAtByte(possnum, numstartloc);
					currentnotes[lastbasic] = currentnotes[lastbasic] + " CLEAR:" + currentzd.clear;
				}
			}
			address++;
		}
		// check for the extvec.
		if (currentzd.startaddress < 0x4000) {
			// locate LET extvec=
			// This should be 0xFD <up to 5 bytes between '0' and '9'> 0x0E <5 bytes FP
			// number>
			address = locate_bytestream(basicdata, new int[] { 0xF1, 0x65, 0x78, 0x74, 0x76, 0x65, 0x63, 0x3D }, 0);
			if (address != -1) {
				// Decode the following number
				while (basicdata[address] != 0x0e) {
					address++;
				}
				currentzd.extvec = (int) Speccy.GetNumberAtByte(basicdata, address + 1);
			}
			// if we have found the "let extvec=xxxx", update the RU.
			if (currentzd.extvec != 0) {
				currentzd.startaddress = currentzd.startaddress + currentzd.extvec;
				// extract any POKEs. Usually something like poke extvec+12,20
				// Convert the file into text...
				StringBuilder sb = new StringBuilder();
				Speccy.DecodeBasicFromLoadedFile(basicdata, sb, basicdata.length, true, false);
				String basiclines[] = sb.toString().split("\n");
				ArrayList<String> newlines = new ArrayList<String>();
				for (String s : basiclines) {
					if (s.contains("extvec")) {
						int i = s.indexOf(' ');
						if (i > 0) {
							s = s.substring(i).trim();
						}

						newlines.add(s);
					}
				}
				currentzd.excveclines = newlines.toArray(new String[newlines.size()]);
			}

		}

		return (currentnotes);
	}

	/**
	 * This is a helper function because comparing two bytes is annoying because of
	 * the lack of an unsigned byte class in java.
	 * 
	 * @param b
	 * @param i
	 * @return
	 */
	private boolean ByteComp(byte b, int i) {
		int data = b;
		if (data < 0) {
			data = data + 256;
		}
		return (data == i);
	}

	/**
	 * Try to find a given byte stream in a given byte array.
	 * 
	 * @param source
	 * @param target
	 * @param start
	 * @return
	 */
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

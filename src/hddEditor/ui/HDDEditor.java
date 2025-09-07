package hddEditor.ui;
/**
 * ZX Spectrum +3 Hard disk/floppy disk image editor. 
 * https://github.com/spodula/SpecHDDFileEditor
 * 
 * Main UI.
 */

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import hddEditor.libs.HtmlHelp;
import hddEditor.libs.Languages;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.DiskUtils;
import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.HDD.RawHDDFile;
import hddEditor.libs.handlers.OSHandler;
import hddEditor.libs.partitions.CPMPartition;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.cpm.CPMDirectoryEntry;
import hddEditor.ui.partitionPages.FloppyBootTrackPage;
import hddEditor.ui.partitionPages.FloppyGenericPage;
import hddEditor.ui.partitionPages.GenericPage;
import hddEditor.ui.partitionPages.MGTDosPartitionPage;
import hddEditor.ui.partitionPages.MicrodrivePartitionPage;
import hddEditor.ui.partitionPages.PlusThreePartPage;
import hddEditor.ui.partitionPages.RawFloppyPage;
import hddEditor.ui.partitionPages.SystemPartPage;
import hddEditor.ui.partitionPages.TAPPartitionPage;
import hddEditor.ui.partitionPages.TZXPartitionPage;
import hddEditor.ui.partitionPages.TrDosPartitionPage;

public class HDDEditor {
	public static String[] SUPPORTEDFILETYPES = { "*.*", "*.img", "*.hdf", "*.mgt", "*.trd", "*.scl", "*.mdr", "*.mgt",
			"*.tap", "*.tzx" };

	public static int DISKCHECKPERIOD = 2000;

	public Disk CurrentDisk = null;
	public OSHandler CurrentHandler = null;
	public IDEDosPartition CurrentSelectedPartition = null;

	// Language
	public Languages lang = null;
	
	// SWT display object
	public Display display = null;

	public FileSelectDialog filesel = null;

	// SWT shell object
	private Shell shell = null;

	// partition combo.
	private Combo PartitionDropdown = null;

	// Main page containing the partition information.

	private Composite MainPage = null;

	// Sub-forms. These are recorded so they can get
	// forcibly closed if the main form closed.
	private FileConversionForm fileConvForm = null;
	private FileNewHDDForm fileNewHDDForm = null;
	private FileNewFDDForm fileNewFDDForm = null;
	private FileImportForm fileImportForm = null;
	private FileImportZenobi fileImportZenobi = null;

	private String helpcontext = "Main";

	// Items for the drag menu.
	public int dragindex = 0;
	String dragtypes[] = { "TYPE", "RAW", "HEX" };

	// Drag target types for when dragging off the form.
	public static int DRAG_TYPE = 0; // Basic->text, code->hex, screens->PNG, arrays->csv
	public static int DRAG_RAW = 1; // Raw data exactly as on disk.
	public static int DRAG_HEX = 2; // Always hex dump

	// Used to stop the file change check pestering the user when the have cancelled
	// the dialog
	public boolean DontAskReload = false;

	/**
	 * Make the menus
	 */
	private void MakeMenus() {
		lang = new Languages();
		
		Label label = new Label(shell, SWT.CENTER);
		label.setBounds(shell.getClientArea());

		Menu menuBar = new Menu(shell, SWT.BAR);
		MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuHeader.setText("&"+lang.Msg(Languages.MENU_FILE));

		Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
		fileMenuHeader.setMenu(fileMenu);

		MenuItem FileNewHDDItem = new MenuItem(fileMenu, SWT.PUSH);
		FileNewHDDItem.setText(lang.Msg(Languages.MENU_NEWHDD));
		FileNewHDDItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				doNewHDDFile();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		MenuItem FileNewFDDItem = new MenuItem(fileMenu, SWT.PUSH);
		FileNewFDDItem.setText("&"+lang.Msg(Languages.MENU_NEWFDD));
		FileNewFDDItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				doNewFDDFile();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		MenuItem fileLoadItem = new MenuItem(fileMenu, SWT.PUSH);
		fileLoadItem.setText("&"+lang.Msg(Languages.MENU_LOAD));
		fileLoadItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				String defaultdisk = "";
				if (CurrentDisk != null && CurrentDisk.IsOpen()) {
					defaultdisk = new File(CurrentDisk.GetFilename()).getName();
				}

				File f = filesel.AskForSingleFileOpen(FileSelectDialog.FILETYPE_DRIVE, lang.Msg(Languages.MENU_SELFILE),
						HDDEditor.SUPPORTEDFILETYPES, defaultdisk);

				if (f != null) {
					LoadFile(f, false);
				}
			}
		});

		if (GeneralUtils.IsLinuxRoot() || GeneralUtils.IsWindowsAdministrator()) {
			MenuItem deviceLoadItem = new MenuItem(fileMenu, SWT.PUSH);
			deviceLoadItem.setText("&"+lang.Msg(Languages.MENU_SELDISK));
			deviceLoadItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					widgetDefaultSelected(arg0);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					fileSelectDevice fdd = new fileSelectDevice(display, lang);
					File f = fdd.Show();
					if (f != null) {
						int blocksz = fdd.blocksize;
						LoadFile(f, false, blocksz);
					}
				}
			});
		}

		MenuItem fileReLoadItem = new MenuItem(fileMenu, SWT.PUSH);
		fileReLoadItem.setText("&"+lang.Msg(Languages.MENU_RELOAD));
		fileReLoadItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				ReloadCurrentFile();
			}
		});

		MenuItem fileConvertItem = new MenuItem(fileMenu, SWT.PUSH);
		fileConvertItem.setText("&"+lang.Msg(Languages.MSG_CONVRAWHDF));
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

		MenuItem fileCopyItem = new MenuItem(fileMenu, SWT.PUSH);
		fileCopyItem.setText("&"+lang.Msg(Languages.MENU_IMPPART));
		fileCopyItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				ShowCopyForm();
			}
		});

		MenuItem fileImportZenobiItem = new MenuItem(fileMenu, SWT.PUSH);
		fileImportZenobiItem.setText("&"+lang.Msg(Languages.MENU_IMPPAWS));
		fileImportZenobiItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				ShowOpenZenobi();
			}
		});

		MenuItem fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
		fileExitItem.setText(lang.Msg(Languages.MENU_EXIT));
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

		MenuItem OptMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		OptMenuHeader.setText("&"+lang.Msg(Languages.MENU_DRAGDEF));

		Menu OptMenu = new Menu(shell, SWT.DROP_DOWN);
		OptMenuHeader.setMenu(OptMenu);

		for (int i = 0; i < dragtypes.length; i++) {
			MenuItem DefaultDragTypeItem = new MenuItem(OptMenu, SWT.RADIO);
			DefaultDragTypeItem.setText("&"+lang.Msg(Languages.MENU_DRAGDEF)+": " + dragtypes[i]);
			DefaultDragTypeItem.setData(i);
			DefaultDragTypeItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					dragindex = (int) arg0.widget.getData();
					System.out.println(dragindex);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
			if (i == 0) {
				DefaultDragTypeItem.setSelection(true);
			}

		}

		MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		helpMenuHeader.setText("&"+lang.Msg(Languages.MENU_HELP));

		Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
		helpMenuHeader.setMenu(helpMenu);

		MenuItem helpGetHelpItem = new MenuItem(helpMenu, SWT.PUSH);
		helpGetHelpItem.setText("&"+lang.Msg(Languages.MENU_HELPDESC));
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

	protected void ShowOpenZenobi() {
		String current = PartitionDropdown.getText();
		if (current != null) {
			current = current + "                     ";
			current = current.substring(0, 20).trim();
		}
		fileImportZenobi = new FileImportZenobi(display, CurrentHandler, filesel, lang);
		try {
			fileImportZenobi.Show(current);
			// force a refresh
			ComboChanged();
		} finally {
			fileImportZenobi = null;
		}
	}

	/**
	 * Re-load the currently loaded file preserving the selected partition.
	 */
	protected void ReloadCurrentFile() {
		if (CurrentDisk != null && CurrentDisk.IsOpen()) {
			// get current partition
			String currentPartName = PartitionDropdown.getText();
			// reload file
			LoadFile(new File(CurrentDisk.GetFilename()), false);
			// set partition
			PartitionDropdown.setText(currentPartName);
			ComboChanged();
			DontAskReload = false;
		}
	}

	/**
	 * Make and populate the partition dropdown
	 * 
	 */
	private void MakeDropdown() {
		// Strings to use as list items
		String[] ITEMS = { lang.Msg(Languages.MSG_NODISK) };

		// Create a dropdown Combo
		PartitionDropdown = new Combo(shell, SWT.DROP_DOWN);
		PartitionDropdown.setItems(ITEMS);
		PartitionDropdown.setText(lang.Msg(Languages.MSG_NODISK));
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
		filesel = new FileSelectDialog(shell);
		shell.setSize(920, 864);
		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (fileConvForm != null) {
					fileConvForm.close();
					fileConvForm = null;
				}
				if (fileNewHDDForm != null) {
					fileNewHDDForm.close();
					fileNewHDDForm = null;
				}
				if (fileNewFDDForm != null) {
					fileNewFDDForm.close();
					fileNewFDDForm = null;
				}
			}
		});

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;
		gridLayout.marginBottom = 20;
		shell.setLayout(gridLayout);

		MakeMenus();
		MakeDropdown();

		ScrolledComposite MainPage1 = new ScrolledComposite(shell, SWT.NONE);
		MainPage1.setExpandHorizontal(true);
		MainPage1.setExpandVertical(true);
		MainPage1.setAlwaysShowScrollBars(true);

		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 4;
		MainPage1.setLayoutData(gd);

		MainPage = new Composite(MainPage1, SWT.NONE);
		MainPage1.setContent(MainPage);

		gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.makeColumnsEqualWidth = true;
		MainPage.setLayout(gridLayout);

		MainPage1.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent arg0) {
				MainPage1.setMinSize(MainPage.computeSize(MainPage1.getClientArea().width, SWT.DEFAULT));
			}

			@Override
			public void controlMoved(ControlEvent arg0) {
			}
		});

		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				event.doit = true;
			}
		});

		DiskCheckTask dct = new DiskCheckTask();
		dct.rootpage = this;
		Display.getDefault().timerExec(DISKCHECKPERIOD, dct);
	}

	/**
	 * Dialog loop, open and wait until closed.
	 */
	public void loop() {
		shell.open();
		while (!shell.isDisposed()) {
			try {
				if (!display.readAndDispatch())
					display.sleep();
			} catch (Exception E) {
				E.printStackTrace();
			}
		}
		if (filesel != null) {
			filesel.SaveDefaults();
		}
		display.dispose();
	}

	/**
	 * Load a named file.
	 * 
	 * @param selected       - File to load
	 * @param suppressdialog - if TRUE, output errors on the command line only.
	 */
	public void LoadFile(File selected, boolean suppressdialog) {
		LoadFile(selected, suppressdialog, 0);
	}

	/**
	 * Same as above, except force block size
	 * 
	 * @param selected       - File to load
	 * @param suppressdialog - if TRUE, output errors on the command line only.
	 * @param ForceBlockSize - If >0, update the block load size for Hard disks.
	 *                       (Only useful for devices)
	 */
	public void LoadFile(File selected, boolean suppressdialog, int ForceBlockSize) {
		System.out.println(String.format(lang.Msg(Languages.MSG_LOADING),selected.getAbsolutePath()));
		try {
			if (CurrentDisk != null) {
				CurrentDisk.close();
			}
			CurrentDisk = DiskUtils.GetCorrectDiskFromFile(selected);
			if (CurrentDisk != null) {
				if (RawHDDFile.class.isAssignableFrom(CurrentDisk.getClass()) && (ForceBlockSize != 0)) {
					((RawHDDFile) CurrentDisk).DiskBlockSize = ForceBlockSize;
					System.out.println(String.format(lang.Msg(Languages.MSG_BLOCSZOVERRIDE),ForceBlockSize));
				}
				CurrentHandler = DiskUtils.GetHandlerForDisk(CurrentDisk);
				UpdateDropdown();
				shell.setText(selected.getName());
				filesel.SetDefaultFolderForType(FileSelectDialog.FILETYPE_DRIVE, selected);
			}
		} catch (IOException e) {
			if (!suppressdialog) {
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
				messageBox.setMessage(lang.Msg(Languages.MSG_CANTLOAD));
				messageBox.setText(e.getMessage());
				messageBox.open();
			}
			System.out.println(lang.Msg(Languages.MSG_CANTLOAD)+". " + e.getMessage());
		}
	}

	/**
	 * Update the combo box from the partition list.
	 * 
	 */
	public void UpdateDropdown() {
		String entries[] = null;
		if ((CurrentDisk == null) || !CurrentDisk.IsOpen() || CurrentHandler.SystemPart == null) {
			entries = new String[] { lang.Msg(Languages.MSG_NODISK) };
		} else {
			ArrayList<String> al = new ArrayList<String>();
			for (IDEDosPartition part : CurrentHandler.SystemPart.partitions) {
				if (part.GetPartType() != 0) {
					String s = String.format("%-20s - %-16s %s", part.GetName(),
							PLUSIDEDOS.GetTypeAsString(part.GetPartType()),
							GeneralUtils.GetSizeAsString(part.GetSizeK() * 1024));
					al.add(s);
				}
			}
			entries = al.toArray(new String[0]);
		}
		PartitionDropdown.setItems(entries);

		// GDS 11 Jan: if we are NOT deali ng with a hard drive, default
		// to probably the only real partition.
		if (CurrentDisk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_HDD) {
			PartitionDropdown.setText(entries[0]);
		} else {
			PartitionDropdown.setText(entries[entries.length - 1]);
		}
		ComboChanged();
	}

	/**
	 * Called when the combo box changes
	 * 
	 */
	private void ComboChanged() {
		if (!PartitionDropdown.isDisposed()) {
			String current = PartitionDropdown.getText();
			if (current != null) {
				current = current + "                     ";
				String s = current.substring(0, 20).trim();
				GotoPartitionByName(s);
			}
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
		helpcontext = "partition_type_" + String.valueOf(part.GetPartType());
		PopulatePartitionScreen(part);
	}

	/**
	 * Populate the main form with the appropriate partition type.
	 * 
	 * @param part
	 */
	private void PopulatePartitionScreen(IDEDosPartition part) {
		CurrentSelectedPartition = part;
		switch (part.GetPartType()) {
		case PLUSIDEDOS.PARTITION_SYSTEM:
			new SystemPartPage(this, MainPage, part, filesel, lang);
			break;
		case PLUSIDEDOS.PARTITION_PLUS3DOS:
			new PlusThreePartPage(this, MainPage, part, filesel, lang);
			break;
		case PLUSIDEDOS.PARTITION_BOOT:
			new FloppyBootTrackPage(this, MainPage, part, filesel, lang);
			break;
		case PLUSIDEDOS.PARTITION_DISK_TRDOS:
			new TrDosPartitionPage(this, MainPage, part, filesel, lang);
			break;
		case PLUSIDEDOS.PARTITION_TAPE_SINCLAIRMICRODRIVE:
			new MicrodrivePartitionPage(this, MainPage, part, filesel, lang);
			break;
		case PLUSIDEDOS.PARTITION_TAPE_TAP:
			new TAPPartitionPage(this, MainPage, part, filesel, lang);
			break;
		case PLUSIDEDOS.PARTITION_TAPE_TZX:
			new TZXPartitionPage(this, MainPage, part, filesel, lang);
			break;
		case PLUSIDEDOS.PARTITION_DISK_PLUSD:
			new MGTDosPartitionPage(this, MainPage, part, filesel, lang);
			break;
		case PLUSIDEDOS.PARTITION_UNKNOWN:
			new FloppyGenericPage(this, MainPage, part, filesel, lang);
			break;
		case PLUSIDEDOS.PARTITION_RAWFDD:
			new RawFloppyPage(this, MainPage, part, filesel, lang);
			break;
		default:
			new GenericPage(this, MainPage, part, filesel, lang);
		}
	}

	/**
	 * Show the conversion form
	 */
	protected void ShowConvertForm() {
		fileConvForm = new FileConversionForm(display, filesel, lang);
		fileConvForm.Show();
		fileConvForm = null;
	}

	/**
	 * New hard disk file form
	 */
	protected void doNewHDDFile() {
		fileNewHDDForm = new FileNewHDDForm(display, filesel, lang);
		String newfile = fileNewHDDForm.Show();
		fileNewHDDForm = null;
		if (newfile != null)
			LoadFile(new File(newfile), false);
	}

	/**
	 * New floppy disk file
	 */
	protected void doNewFDDFile() {
		fileNewFDDForm = new FileNewFDDForm(display, filesel, lang);
		String newfile = fileNewFDDForm.Show();
		fileNewFDDForm = null;
		if (newfile != null)
			LoadFile(new File(newfile), false);
	}

	/**
	 * Show the "Copy from another disk" form.
	 */
	protected void ShowCopyForm() {
		String current = PartitionDropdown.getText();
		if (current != null) {
			current = current + "                     ";
			current = current.substring(0, 20).trim();
		}
		fileImportForm = new FileImportForm(display, CurrentHandler, filesel, lang);
		try {
			fileImportForm.Show(current);
			// force a refresh
			ComboChanged();
		} finally {
			fileImportForm = null;
		}
	}

	/**
	 * Called when the currently loaded disk detected as out of date
	 */
	public void OnDiskOutOfDate() {
		if (!DontAskReload) {
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			messageBox.setMessage(lang.Msg(Languages.MSG_FILECHANGED));
			messageBox.setText(lang.Msg(Languages.MSG_FILECHANGED)+".\n"+lang.Msg(Languages.MENU_RELOAD)+"?");
			if (messageBox.open() == SWT.YES) {
				ReloadCurrentFile();
			} else {
				DontAskReload = true;
			}
		}
	}

	/**
	 * Main function.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			if (args[0].toLowerCase().startsWith("cat")) {
				File folder = new File(args[1]);
				System.out.println("Folder" + folder.getAbsolutePath());

				File f[] = folder.listFiles();
				Arrays.sort(f);

				for (File file : f) {
					try {
						System.out.println("===============================================================");
						System.out.println("Processing: " + file.getName());
						System.out.println("===============================================================");
						OSHandler handler = DiskUtils.LoadDiskDetails(file);
						if (handler != null) {
							for (IDEDosPartition partition : handler.SystemPart.partitions) {
								if (partition.GetPartType() == PLUSIDEDOS.PARTITION_CPM
										|| partition.GetPartType() == PLUSIDEDOS.PARTITION_PLUS3DOS) {
									CPMPartition sp = (CPMPartition) partition;
									for (CPMDirectoryEntry de : sp.DirectoryEntries) {
										System.out.println("   " + de.GetFilename() + " " + de.GetFileTypeString() + " "
												+ de.GetFileSize());
									}
								}
							}
						}
					} catch (Exception E) {
						System.out.println(E.getMessage());
					}
				}
			} else if (args[0].toLowerCase().startsWith("script=")) {
				ScriptRunner sr = new ScriptRunner();
				String splitParam[] = args[0].split("=");
				sr.RunScript(splitParam[1], new Languages());
			} else {
				HDDEditor hdi = new HDDEditor();
				hdi.MakeForm();
				hdi.LoadFile(new File(args[0]), false);
				hdi.loop();
			}
		} else {
			HDDEditor hdi = new HDDEditor();
			hdi.MakeForm();
			hdi.loop();
		}
	}

}

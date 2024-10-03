package hddEditor.ui;

/**
 * Main UI.
 */

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;

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
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.DiskUtils;
import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.disks.Disk;
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
	public static String[] SUPPORTEDFILETYPES = { "*", "*.img", "*.hdf", "*.mgt", "*.trd", "*.scl", "*.mdr", "*.mgt",
			"*.tap", "*.tzx" };

	public static int DISKCHECKPERIOD = 2000;

	public Disk CurrentDisk = null;
	public OSHandler CurrentHandler = null;
	public IDEDosPartition CurrentSelectedPartition = null;

	private static String DefaultDropDownText = "<No Disk loaded>";

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
		Label label = new Label(shell, SWT.CENTER);
		label.setBounds(shell.getClientArea());

		Menu menuBar = new Menu(shell, SWT.BAR);
		MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuHeader.setText("&File");

		Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
		fileMenuHeader.setMenu(fileMenu);

		MenuItem FileNewHDDItem = new MenuItem(fileMenu, SWT.PUSH);
		FileNewHDDItem.setText("&New Hard disk file");
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
		FileNewFDDItem.setText("&New Floppy/cart/Tape file");
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
		fileLoadItem.setText("&Load");
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

				File f = filesel.AskForSingleFileOpen(FileSelectDialog.FILETYPE_DRIVE, "Select file to open.",
						HDDEditor.SUPPORTEDFILETYPES, defaultdisk);

				if (f != null) {
					LoadFile(f, false);
				}
			}
		});

		String os = System.getProperty("os.name");

		if (os.toUpperCase().contains("LINUX") && GeneralUtils.IsLinuxRoot()) {
			MenuItem deviceLoadItem = new MenuItem(fileMenu, SWT.PUSH);
			deviceLoadItem.setText("&Select Physical disk");
			deviceLoadItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					widgetDefaultSelected(arg0);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					fileSelectDeviceLinux fdd = new fileSelectDeviceLinux(display);
					File f = fdd.Show();
					if (f != null) {
						LoadFile(f, false);
					}
				}
			});
		}

		MenuItem fileReLoadItem = new MenuItem(fileMenu, SWT.PUSH);
		fileReLoadItem.setText("&Reload");
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

		MenuItem fileCopyItem = new MenuItem(fileMenu, SWT.PUSH);
		fileCopyItem.setText("&Import another partition/disk");
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

		MenuItem OptMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		OptMenuHeader.setText("&Drag out default");

		Menu OptMenu = new Menu(shell, SWT.DROP_DOWN);
		OptMenuHeader.setMenu(OptMenu);

		for (int i = 0; i < dragtypes.length; i++) {
			MenuItem DefaultDragTypeItem = new MenuItem(OptMenu, SWT.RADIO);
			DefaultDragTypeItem.setText("&Drag out default: " + dragtypes[i]);
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
	 * @param selected
	 */
	public void LoadFile(File selected, boolean suppressdialog) {
		System.out.println("Loading " + selected.getAbsolutePath());
		try {
			if (CurrentDisk != null) {
				CurrentDisk.close();
			}
			CurrentDisk = DiskUtils.GetCorrectDiskFromFile(selected);
			if (CurrentDisk != null) {
				CurrentHandler = DiskUtils.GetHandlerForDisk(CurrentDisk);
				UpdateDropdown();
				shell.setText(selected.getName());
				filesel.SetDefaultFolderForType(FileSelectDialog.FILETYPE_DRIVE, selected);
			}
		} catch (IOException e) {
			if (!suppressdialog) {
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
				messageBox.setMessage("Cannot load file.");
				messageBox.setText(e.getMessage());
				messageBox.open();
			}
			System.out.println("Loading failed. " + e.getMessage());
		}
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
					String s = String.format("%-20s - %-16s %s", part.GetName(),
							PLUSIDEDOS.GetTypeAsString(part.GetPartType()),
							GeneralUtils.GetSizeAsString(part.GetSizeK() * 1024));
					al.add(s);
				}
			}
			entries = al.toArray(new String[0]);
		}
		PartitionDropdown.setItems(entries);

		// QOL improvement, GDS 11 Jan: if we are NOT dealing with a hard drive, default
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
			new SystemPartPage(this, MainPage, part, filesel);
			break;
		case PLUSIDEDOS.PARTITION_PLUS3DOS:
			new PlusThreePartPage(this, MainPage, part, filesel);
			break;
		case PLUSIDEDOS.PARTITION_BOOT:
			new FloppyBootTrackPage(this, MainPage, part, filesel);
			break;
		case PLUSIDEDOS.PARTITION_DISK_TRDOS:
			new TrDosPartitionPage(this, MainPage, part, filesel);
			break;
		case PLUSIDEDOS.PARTITION_TAPE_SINCLAIRMICRODRIVE:
			new MicrodrivePartitionPage(this, MainPage, part, filesel);
			break;
		case PLUSIDEDOS.PARTITION_TAPE_TAP:
			new TAPPartitionPage(this, MainPage, part, filesel);
			break;
		case PLUSIDEDOS.PARTITION_TAPE_TZX:
			new TZXPartitionPage(this, MainPage, part, filesel);
			break;
		case PLUSIDEDOS.PARTITION_DISK_PLUSD:
			new MGTDosPartitionPage(this, MainPage, part, filesel);
			break;
		case PLUSIDEDOS.PARTITION_UNKNOWN:
			new FloppyGenericPage(this, MainPage, part, filesel);
			break;
		case PLUSIDEDOS.PARTITION_RAWFDD:
			new RawFloppyPage(this, MainPage, part, filesel);
			break;
		default:
			new GenericPage(this, MainPage, part, filesel);
		}
	}

	/**
	 * Show the conversion form
	 */
	protected void ShowConvertForm() {
		fileConvForm = new FileConversionForm(display, filesel);
		fileConvForm.Show();
		fileConvForm = null;
	}

	/**
	 * New hard disk file form
	 */
	protected void doNewHDDFile() {
		fileNewHDDForm = new FileNewHDDForm(display, filesel);
		String newfile = fileNewHDDForm.Show();
		fileNewHDDForm = null;
		if (newfile != null)
			LoadFile(new File(newfile), false);
	}

	/**
	 * New floppy disk file
	 */
	protected void doNewFDDFile() {
		fileNewFDDForm = new FileNewFDDForm(display, filesel);
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
		fileImportForm = new FileImportForm(display, CurrentHandler, filesel);
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
			messageBox.setMessage("Current file has been updated on disk");
			messageBox.setText("The current file has been updated on disk. \nReload?");
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
				for (File file : folder.listFiles()) {
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
				sr.RunScript(splitParam[1]);
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

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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import hddEditor.libs.HtmlHelp;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.DiskUtils;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.handlers.OSHandler;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.ui.partitionPages.FloppyBootTrackPage;
import hddEditor.ui.partitionPages.FloppyGenericPage;
import hddEditor.ui.partitionPages.GenericPage;
import hddEditor.ui.partitionPages.MGTDosPartitionPage;
import hddEditor.ui.partitionPages.MicrodrivePartitionPage;
import hddEditor.ui.partitionPages.PlusThreePartPage;
import hddEditor.ui.partitionPages.SystemPartPage;
import hddEditor.ui.partitionPages.TAPPartitionPage;
import hddEditor.ui.partitionPages.TrDosPartitionPage;

public class HDDEditor { 
	public static String[] SUPPORTEDFILETYPES = { "*", "*.img", "*.hdf", "*.mgt", "*.trd", "*.scl", "*.mdr", "*.mgt",
			"*.tap" };

	public Disk CurrentDisk = null;
	public OSHandler CurrentHandler = null;
	public IDEDosPartition CurrentSelectedPartition = null;

	private static String DefaultDropDownText = "<No Disk loaded>";
	// SWT display object
	public Display display = null;

	// SWT shell object
	private Shell shell = null;

	// partition combo.
	private Combo PartitionDropdown = null;

	// Main page containing the partition information.

	private Composite MainPage = null;

	private FileConversionForm fileConvForm = null;
	private FileNewHDDForm fileNewHDDForm = null;
	private FileNewFDDForm fileNewFDDForm = null;
	private FileImportForm fileImportForm = null;

	private String helpcontext = "Main";

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
				FileDialog fd = new FileDialog(shell, SWT.OPEN);
				fd.setText("Open a media file...");
				fd.setFilterExtensions(HDDEditor.SUPPORTEDFILETYPES);

				if (CurrentDisk.IsOpen()) {
					File f = new File(CurrentDisk.GetFilename());
					fd.setFilterPath(f.getPath());
				}

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
		if (args.length > 0) {
			if (args[0].toLowerCase().startsWith("script=")) {
				ScriptRunner sr = new ScriptRunner();
				String splitParam[] = args[0].split("=");
				sr.RunScript(splitParam[1]);
			} else {
				HDDEditor hdi = new HDDEditor();
				hdi.MakeForm();
				hdi.LoadFile(args[0]);
				hdi.loop();
			}
		} else {
			HDDEditor hdi = new HDDEditor();
			hdi.MakeForm();
			hdi.loop();
		}
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
			CurrentDisk = DiskUtils.GetCorrectDiskFromFile(new File(selected));
			if (CurrentDisk != null) {
				CurrentHandler = DiskUtils.GetHandlerForDisk(CurrentDisk);
				UpdateDropdown();
				shell.setText(selected);
			}
		} catch (IOException e) {
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
			messageBox.setMessage(e.getMessage());
			messageBox.setText(e.getMessage());
			messageBox.open();
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
			new SystemPartPage(this, MainPage, part);
			break;
		case PLUSIDEDOS.PARTITION_PLUS3DOS:
			new PlusThreePartPage(this, MainPage, part);
			break;
		case PLUSIDEDOS.PARTITION_BOOT:
			new FloppyBootTrackPage(this, MainPage, part);
			break;
		case PLUSIDEDOS.PARTITION_DISK_TRDOS:
			new TrDosPartitionPage(this, MainPage, part);
			break;
		case PLUSIDEDOS.PARTITION_TAPE_SINCLAIRMICRODRIVE:
			new MicrodrivePartitionPage(null, MainPage, part);
			break;
		case PLUSIDEDOS.PARTITION_TAPE_TAP:
			new TAPPartitionPage(null, MainPage, part);
			break;
		case PLUSIDEDOS.PARTITION_DISK_PLUSD:
			new MGTDosPartitionPage(null, MainPage, part);
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
	 * New hard disk file form
	 */
	protected void doNewHDDFile() {
		fileNewHDDForm = new FileNewHDDForm(display);
		String newfile = fileNewHDDForm.Show();
		fileNewHDDForm = null;
		if (newfile != null)
			LoadFile(newfile);
	}

	/**
	 * New floppy disk file
	 */
	protected void doNewFDDFile() {
		fileNewFDDForm = new FileNewFDDForm(display);
		String newfile = fileNewFDDForm.Show();
		fileNewFDDForm = null;
		if (newfile != null)
			LoadFile(newfile);
	}

	protected void ShowCopyForm() {
		String current = PartitionDropdown.getText();
		if (current != null) {
			current = current + "                     ";
			current = current.substring(0, 20).trim();
		}
		
		fileImportForm = new FileImportForm(display, CurrentHandler);
		
		fileImportForm.Show(current);
		fileImportForm = null;
		// force a reload of the current partition.
		ComboChanged();
	}
}

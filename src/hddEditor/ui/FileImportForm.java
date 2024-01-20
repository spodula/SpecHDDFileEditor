package hddEditor.ui;
//TODO: viewer for sprites
//TODO: support for TZX file

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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import hddEditor.libs.DiskUtils;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.handlers.OSHandler;
import hddEditor.libs.partitions.IDEDosPartition;

public class FileImportForm {
	// Form details
	private Display display = null;
	private Shell shell = null;

	private Label Sourcefile = null;

	private Combo SourcePartition = null;
	private Button SelectSourceFileBtn = null;
	private Button CloseBtn = null;
	private Button ImportBtn = null;
	private Table SourceList = null;

	private Disk CurrentSourceDisk = null;
	private OSHandler CurrentSourceHandler = null;
	private OSHandler CurrentTargetHandler;

	private Combo TargetPartition = null;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public FileImportForm(Display display, OSHandler handler) {
		this.display = display;
		this.CurrentTargetHandler = handler;
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
		shell.setText("Import another disk.");

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
				FileDialog fd = new FileDialog(shell, SWT.OPEN);
				fd.setText("Select source file");
				fd.setFilterExtensions(HDDEditor.SUPPORTEDFILETYPES);
				
				String selected = fd.open();
				if (selected != null) {
					Sourcefile.setText(selected);
					DoLoadFile(selected);
				}
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

		SourceList = new Table(shell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = 200;
		gd.verticalSpan = 2;
		SourceList.setLayoutData(gd);
		SourceList.setHeaderVisible(true);

		Label txtlbl = new Label(shell, SWT.NONE);
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
		gd.horizontalSpan = 3;
		TargetPartition.setLayoutData(gd);

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
	protected void DoLoadFile(String selected) {
		try {
			if (CurrentSourceDisk != null) {
				CurrentSourceDisk.close();
			}
			CurrentSourceDisk = DiskUtils.GetCorrectDiskFromFile(new File(selected));
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

		String current = SourcePartition.getText();
		if (current != null) {
			current = current + "                     ";
			String s = current.substring(0, 20).trim();
			IDEDosPartition part = CurrentSourceHandler.GetPartitionByName(s);
			FileEntry fl[] = part.GetFileList();
			if (fl != null) {
				for (FileEntry f : fl) {
					TableItem itm = new TableItem(SourceList, SWT.NONE);
					itm.setChecked(true);
					String cols = f.GetFilename() + " - " + f.GetFileTypeString() + " - "
							+ String.valueOf(f.GetFileSize()) + " bytes";
					itm.setData(f);
					itm.setText(cols);
				}
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

				for (TableItem itm : SourceList.getItems()) {
					if (itm.getChecked()) {
						FileEntry fe = (FileEntry) itm.getData();
						SpeccyBasicDetails sbd = fe.GetSpeccyBasicDetails();
						String filename = fe.GetFilename();

						filename =  TargetPartition.UniqueifyFileNameIfRequired(filename);
						switch (sbd.BasicType) {
						case Speccy.BASIC_BASIC:
							TargetPartition.AddBasicFile(filename, fe.GetFileData(), sbd.LineStart,
									sbd.VarStart);
							break;
						case Speccy.BASIC_NUMARRAY:
							TargetPartition.AddNumericArray(filename, fe.GetFileData(), sbd.VarName + "");
							break;
						case Speccy.BASIC_CHRARRAY:
							TargetPartition.AddCharArray(filename, fe.GetFileData(), sbd.VarName + "");
							break;
						case Speccy.BASIC_CODE:
							TargetPartition.AddCodeFile(filename, sbd.LoadAddress, fe.GetFileData());
							break;
						default:
							TargetPartition.AddCodeFile(filename, 0, fe.GetFileData());
							break;
						}
					}
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
}

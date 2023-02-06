package hddEditor.ui;

import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.ProgressCallback;
import hddEditor.libs.partitions.SystemPartition;
import hddEditor.ui.partitionPages.dialogs.PartitionExportProgress;

public class FileExportAllPartitionsForm {
	// Form components
	private Display display = null;
	private Shell shell = null;
	private Text Targetfile = null;
	private Button SelectTargetFileBtn = null;
	private Button CloseBtn = null;
	private Button ExportBtn = null;

	private Combo BasicTargetFileType = null;
	private Combo CodeTargetFileType = null;
	private Combo ArrayTargetFileType = null;
	private Combo ScreenTargetFileType = null;
	private Combo UnknownTargetFileType = null;
	private Combo SwapTargetFileType = null;

	// Result to return.
	private String result = null;

	// Entries for the comboboxes.
	private String BasicEntries[] = { "Text", "Raw", "Raw+Header", "Hex" };
	private String CodeEntries[] = { "Hex", "Raw", "Raw+Header", "Assembly" };
	private String ArrayEntries[] = { "CSV", "Raw", "Raw+Header", "Hex" };
	private String ScreenEntries[] = { "PNG", "GIF", "JPEG", "Raw", "Raw+Header", "Hex", "Assembly" };
	private String UnknownEntries[] = { "Hex", "Raw", "Assembly" };
	private String SwapEntries[] = { "Hex","Raw" };

	// Disk
	private IDEDosPartition ThisDisk;

	// Mode
	private Boolean SinglePartitionMode;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public FileExportAllPartitionsForm(Display display) {
		this.display = display;
	}

	/**
	 * Show the dialog
	 * 
	 * @param partition
	 * 
	 */
	public String Show(IDEDosPartition partition) {
		this.ThisDisk = partition;
		Createform(false);
		return (loop());
	}

	/**
	 * 
	 * @param partition
	 * @return
	 */
	public String ShowSinglePartition(IDEDosPartition partition) {
		this.ThisDisk = partition;
		Createform(true);
		return (loop());
	}

	/**
	 * Create the components on the form.
	 * 
	 * @param title
	 */
	private void Createform(boolean IsSinglePartition) {
		SinglePartitionMode = IsSinglePartition;
		shell = new Shell(display);
		shell.setSize(900, 810);

		GridLayout gridLayout = new GridLayout(4, true);
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;

		shell.setLayout(gridLayout);
		if (IsSinglePartition) {
			shell.setText("Export partition to folder.");
		} else {
			shell.setText("Export entire disk to folder.");
		}

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;

		Targetfile = new Text(shell, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 3;
		gd.widthHint = 600;
		Targetfile.setLayoutData(gd);
		Targetfile.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

		SelectTargetFileBtn = new Button(shell, SWT.BORDER);
		SelectTargetFileBtn.setText("Select Target folder");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		SelectTargetFileBtn.setLayoutData(gd);
		SelectTargetFileBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DirectoryDialog dialog = new DirectoryDialog(shell);
				dialog.setText("Select folder for export to");
				String result = dialog.open();
				if (result != null) {
					Targetfile.setText(result);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Label lbl = new Label(shell, SWT.NONE);
		lbl.setText("BASIC file:");

		BasicTargetFileType = new Combo(shell, SWT.CHECK);
		BasicTargetFileType.setItems(BasicEntries);
		BasicTargetFileType.setText(BasicEntries[0]);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("CODE file:");
		CodeTargetFileType = new Combo(shell, SWT.CHECK);
		CodeTargetFileType.setItems(CodeEntries);
		CodeTargetFileType.setText(CodeEntries[0]);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("Array file:");
		ArrayTargetFileType = new Combo(shell, SWT.CHECK);
		ArrayTargetFileType.setItems(ArrayEntries);
		ArrayTargetFileType.setText(ArrayEntries[0]);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("SCREEN$ file:");
		ScreenTargetFileType = new Combo(shell, SWT.CHECK);
		ScreenTargetFileType.setItems(ScreenEntries);
		ScreenTargetFileType.setText(ScreenEntries[0]);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("Unidentified file:");
		UnknownTargetFileType = new Combo(shell, SWT.CHECK);
		UnknownTargetFileType.setItems(UnknownEntries);
		UnknownTargetFileType.setText(UnknownEntries[0]);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("Swap partition:");
		SwapTargetFileType = new Combo(shell, SWT.CHECK);
		SwapTargetFileType.setItems(SwapEntries);
		SwapTargetFileType.setText(SwapEntries[0]);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		ExportBtn = new Button(shell, SWT.BORDER);
		ExportBtn.setText("Export partitions");
		if (SinglePartitionMode) {
			ExportBtn.setText("Export partition");
		}
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		ExportBtn.setLayoutData(gd);
		ExportBtn.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (SinglePartitionMode) {
					if (DoExportSingle()) {
						shell.close();
					}
				} else {
					if (DoExport()) {
						shell.close();
					}
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
	 * This is a hack to get around the Combo.getSelectionIndex returning
	 * inconsistent values. No idea why, sometimes it just returns -1 even if the
	 * text is valid.
	 * 
	 * @param s   - Text to find
	 * @param arr - Array of items
	 * @return - Index of item in the array.
	 */
	private int TextToIndex(String s, String[] arr) {
		int result = -1;
		for (int i = 0; i < arr.length; i++) {
			if (s.equals(arr[i])) {
				result = i;
			}
		}
		return (result);
	}

	/**
	 * 
	 * @return
	 */
	protected boolean DoExportSingle() {

		int BasicType = TextToIndex(BasicTargetFileType.getText(), GeneralUtils.MasterList);
		int CodeType = TextToIndex(CodeTargetFileType.getText(), GeneralUtils.MasterList);
		int ArrayType = TextToIndex(ArrayTargetFileType.getText(), GeneralUtils.MasterList);
		int ScreenType = TextToIndex(ScreenTargetFileType.getText(), GeneralUtils.MasterList);
		int UnknownType = TextToIndex(UnknownTargetFileType.getText(), GeneralUtils.MasterList);
		int SwapType = TextToIndex(SwapTargetFileType.getText(), GeneralUtils.MasterList);

		System.out.println("Basic type: " + BasicType + " " + GeneralUtils.MasterList[BasicType]);
		System.out.println("Code type: " + CodeType + " " + GeneralUtils.MasterList[CodeType]);
		System.out.println("Array type: " + ArrayType + " " + GeneralUtils.MasterList[ArrayType]);
		System.out.println("Screen type: " + ScreenType + " " + GeneralUtils.MasterList[ScreenType]);
		System.out.println("Unknown type: " + UnknownType + " " + GeneralUtils.MasterList[UnknownType]);
		System.out.println("Swap partition: " + SwapType + " " + GeneralUtils.MasterList[SwapType]);

		PartitionExportProgress pep = new PartitionExportProgress(shell.getDisplay());
		try {
			pep.Show("Exporting Partition "+ThisDisk.GetName(), "Partition:", "File:");
			pep.SetMax1(1);
			pep.SetMax2(1);
			pep.SetValue1(0);
			pep.SetValue2(0);
			int PartNum = 0;
			File directory = new File(Targetfile.getText());
			pep.setMessage1("Exporting partition " + ThisDisk.GetName() + " ("
					+ PLUSIDEDOS.GetTypeAsString(ThisDisk.GetPartType()) + ")");
			pep.SetValue1(PartNum++);
			if (ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_FREE
					&& ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_UNUSED
					&& ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_UNKNOWN
					&& ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_BAD) {
				try {
					long start = System.currentTimeMillis();
					ThisDisk.ExtractPartitiontoFolderAdvanced(directory, BasicType, CodeType, ArrayType, ScreenType,
							UnknownType, SwapType, new ProgressCallback() {
								int lastmax = 0;
								@Override
								public boolean Callback(int max, int value, String text) {
									if (max != lastmax) {
										pep.SetMax2(max);
										max = lastmax;
									}
									pep.SetValue2(value);
									pep.setMessage2(text);
									return (pep.IsCancelled());
								}
							});

					long finish = System.currentTimeMillis();
					System.out.println(String.valueOf(finish - start) + "ms");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} finally {
			pep.close();
		}
		return true;
	}

	protected boolean DoExport() {

		int BasicType = TextToIndex(BasicTargetFileType.getText(), GeneralUtils.MasterList);
		int CodeType = TextToIndex(CodeTargetFileType.getText(), GeneralUtils.MasterList);
		int ArrayType = TextToIndex(ArrayTargetFileType.getText(), GeneralUtils.MasterList);
		int ScreenType = TextToIndex(ScreenTargetFileType.getText(), GeneralUtils.MasterList);
		int UnknownType = TextToIndex(UnknownTargetFileType.getText(), GeneralUtils.MasterList);
		int SwapType = TextToIndex(SwapTargetFileType.getText(), GeneralUtils.MasterList);

		System.out.println("Basic type: " + BasicType + " " + GeneralUtils.MasterList[BasicType]);
		System.out.println("Code type: " + CodeType + " " + GeneralUtils.MasterList[CodeType]);
		System.out.println("Array type: " + ArrayType + " " + GeneralUtils.MasterList[ArrayType]);
		System.out.println("Screen type: " + ScreenType + " " + GeneralUtils.MasterList[ScreenType]);
		System.out.println("Unknown type: " + UnknownType + " " + GeneralUtils.MasterList[UnknownType]);
		System.out.println("Swap partition: " + SwapType + " " + GeneralUtils.MasterList[SwapType]);

		SystemPartition sp = (SystemPartition) ThisDisk;

		PartitionExportProgress pep = new PartitionExportProgress(shell.getDisplay());
		try {
			pep.Show("Exporting all partitions", "Partition:", "File:");
			pep.SetMax1(sp.partitions.length);
			pep.SetMax2(1);
			pep.SetValue1(0);
			pep.SetValue2(0);
			int PartNum = 0;
			File directory = new File(Targetfile.getText());
			for (IDEDosPartition partition : sp.partitions) {
				pep.setMessage1("Exporting partition " + partition.GetName() + " ("
						+ PLUSIDEDOS.GetTypeAsString(partition.GetPartType()) + ")");
				pep.SetValue1(PartNum++);
				if (partition.GetPartType() != PLUSIDEDOS.PARTITION_FREE
						&& partition.GetPartType() != PLUSIDEDOS.PARTITION_UNUSED
						&& partition.GetPartType() != PLUSIDEDOS.PARTITION_UNKNOWN
						&& partition.GetPartType() != PLUSIDEDOS.PARTITION_BAD) {

					File BaseFolder = new File(directory, partition.GetName());
					System.out.print("Extracting partition" + BaseFolder + " - ");
					if (!BaseFolder.exists()) {
						BaseFolder.mkdir();
					}
					try {
						long start = System.currentTimeMillis();
						partition.ExtractPartitiontoFolderAdvanced(BaseFolder, BasicType, CodeType, ArrayType,
								ScreenType, UnknownType, SwapType, new ProgressCallback() {
									int lastmax = 0;

									@Override
									public boolean Callback(int max, int value, String text) {
										if (max != lastmax) {
											pep.SetMax2(max);
											max = lastmax;
										}
										pep.SetValue2(value);
										pep.setMessage2(text);
										return (pep.IsCancelled());
									}
								});

						long finish = System.currentTimeMillis();
						System.out.println(String.valueOf(finish - start) + "ms");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} finally {
			pep.close();
		}
		return true;
	}

}

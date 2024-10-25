package hddEditor.ui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

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
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.ProgressCallback;
import hddEditor.libs.partitions.SystemPartition;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;
import hddEditor.libs.partitions.tap.TapDirectoryEntry;
import hddEditor.ui.partitionPages.dialogs.PartitionExportProgress;

public class FileExportAllPartitionsForm {
	// Form components
	private Display display = null;
	private Shell shell = null;
	private Text Targetfile = null;
	private Button SelectTargetFileBtn = null;
	private Button CloseBtn = null;
	private Button ExportBtn = null;
	private Button IncludeDeleted = null;
	private Button SpecialExport = null;
	private Button SpecialExportFBN = null;

	private Combo BasicTargetFileType = null;
	private Combo CodeTargetFileType = null;
	private Combo ArrayTargetFileType = null;
	private Combo ScreenTargetFileType = null;
	private Combo UnknownTargetFileType = null;
	private Combo SwapTargetFileType = null;

	// Result to return.
	private String result = null;

	// Entries for the comboboxes.
	private String BasicEntries[] = { "Text", "Raw", "Raw+Header", "Hex", "Assembly" };
	private String CodeEntries[] = { "Hex", "Raw", "Raw+Header", "Assembly" };
	private String ArrayEntries[] = { "CSV", "Raw", "Raw+Header", "Hex" };
	private String ScreenEntries[] = { "PNG", "GIF", "JPEG", "Raw", "Raw+Header", "Hex", "Assembly" };
	private String UnknownEntries[] = { "Hex", "Raw", "Assembly" };
	private String SwapEntries[] = { "Hex", "Raw" };

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

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("Include deleted files?:");
		IncludeDeleted = new Button(shell, SWT.CHECK);
		IncludeDeleted.setSelection(false);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("Special export mode?:");
		SpecialExport = new Button(shell, SWT.CHECK);
		SpecialExport.setSelection(false);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("folder by name:");
		SpecialExportFBN = new Button(shell, SWT.CHECK);
		SpecialExportFBN.setSelection(false);

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
					if (SpecialExport.getSelection()) {
						if (DoSpecialExport()) {
							shell.close();
						}
					} else {
						if (DoExportSingle()) {
							shell.close();
						}
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

	/*
	 * This is my special export which creates a folder off different file types.
	 * This is basically a utility for me.
	 */

	protected boolean DoSpecialExport() {
		String TargetFolder = Targetfile.getText();
		if (!SpecialExportFBN.getSelection()) {
			DoExportFlags(GeneralUtils.EXPORT_TYPE_TXT, GeneralUtils.EXPORT_TYPE_HEX, GeneralUtils.EXPORT_TYPE_CSV,
					GeneralUtils.EXPORT_TYPE_PNG, GeneralUtils.EXPORT_TYPE_HEX, GeneralUtils.EXPORT_TYPE_HEX,
					TargetFolder);
			File asm = new File(TargetFolder, "ASM");
			asm.mkdir();
			DoExportFlags(GeneralUtils.EXPORT_TYPE_ASM, GeneralUtils.EXPORT_TYPE_ASM, GeneralUtils.EXPORT_TYPE_ASM,
					GeneralUtils.EXPORT_TYPE_ASM, GeneralUtils.EXPORT_TYPE_ASM, GeneralUtils.EXPORT_TYPE_ASM,
					asm.getAbsolutePath());
			File cpm = new File(TargetFolder, "CPM");
			cpm.mkdir();
			DoExportFlags(GeneralUtils.EXPORT_TYPE_RAWANDHEADER, GeneralUtils.EXPORT_TYPE_RAWANDHEADER,
					GeneralUtils.EXPORT_TYPE_RAWANDHEADER, GeneralUtils.EXPORT_TYPE_RAWANDHEADER,
					GeneralUtils.EXPORT_TYPE_RAWANDHEADER, GeneralUtils.EXPORT_TYPE_RAWANDHEADER,
					cpm.getAbsolutePath());
			File hex = new File(TargetFolder, "HEX");
			hex.mkdir();
			DoExportFlags(GeneralUtils.EXPORT_TYPE_HEX, GeneralUtils.EXPORT_TYPE_HEX, GeneralUtils.EXPORT_TYPE_HEX,
					GeneralUtils.EXPORT_TYPE_HEX, GeneralUtils.EXPORT_TYPE_HEX, GeneralUtils.EXPORT_TYPE_HEX,
					hex.getAbsolutePath());
			File raw = new File(TargetFolder, "RAW");
			raw.mkdir();
			DoExportFlags(GeneralUtils.EXPORT_TYPE_RAW, GeneralUtils.EXPORT_TYPE_RAW, GeneralUtils.EXPORT_TYPE_RAW,
					GeneralUtils.EXPORT_TYPE_RAW, GeneralUtils.EXPORT_TYPE_RAW, GeneralUtils.EXPORT_TYPE_RAW,
					raw.getAbsolutePath());
			return true;
		} else {
			int BasicType = TextToIndex(BasicTargetFileType.getText(), GeneralUtils.MasterList);
			int CodeType = TextToIndex(CodeTargetFileType.getText(), GeneralUtils.MasterList);
			int ArrayType = TextToIndex(ArrayTargetFileType.getText(), GeneralUtils.MasterList);
			int ScreenType = TextToIndex(ScreenTargetFileType.getText(), GeneralUtils.MasterList);
			int UnknownType = TextToIndex(UnknownTargetFileType.getText(), GeneralUtils.MasterList);

			PartitionExportProgress pep = new PartitionExportProgress(shell.getDisplay());
			try {
				pep.Show("Exporting Partition " + ThisDisk.GetName(), "Partition:", "File:");
				pep.SetMax1(1);
				pep.SetMax2(1);
				pep.SetValue1(0);
				pep.SetValue2(0);
				int PartNum = 0;
				File directory = new File(TargetFolder);
				pep.setMessage1("Exporting partition " + ThisDisk.GetName() + " ("
						+ PLUSIDEDOS.GetTypeAsString(ThisDisk.GetPartType()) + ")");
				pep.SetValue1(PartNum++);
				System.out.println(ThisDisk.getClass().getCanonicalName());
				String PartClass = ThisDisk.getClass().getCanonicalName();
				boolean HasDeletedFiles = PartClass.contains("PLUS3DOSPartition");

				if ((ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_FREE
						&& ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_UNUSED
						&& ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_UNKNOWN
						&& ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_BAD)) {

					pep.SetMax2(ThisDisk.GetFileList().length);
					int filenum = 0;
					for (FileEntry file : ThisDisk.GetFileList()) {
						boolean deleted = false;
						if (HasDeletedFiles) {
							deleted = ((hddEditor.libs.partitions.cpm.CPMDirectoryEntry) file).IsDeleted;
						}
						if (!deleted || IncludeDeleted.getSelection()) {
							String fn = file.GetFilename();
							if (fn.contains(".")) {
								int dotloc = fn.lastIndexOf(".");
								fn = fn.substring(0, dotloc);
							}
							File filefolder = new File(directory, fn.toUpperCase());
							pep.setMessage2(file.GetFilename());
							pep.SetValue2(filenum++);

							// create blank sysconfig file.
							FileWriter SysConfig;
							try {
								SysConfig = new FileWriter(new File(filefolder, "partition.index"), false);
								SysConfig.close();
							} catch (IOException e) {
								e.printStackTrace();
							}

							File asm = new File(filefolder, "ASM");
							File cpm = new File(filefolder, "CPM");
							File hex = new File(filefolder, "HEX");
							File raw = new File(filefolder, "RAW");
							if (!asm.exists())
								asm.mkdirs();
							if (!cpm.exists())
								cpm.mkdirs();
							if (!hex.exists())
								hex.mkdirs();
							if (!raw.exists())
								raw.mkdirs();
							try {
								SpeccyBasicDetails sbd = file.GetSpeccyBasicDetails();
								byte data[] = file.GetFileData();
								byte cpmdata[] = file.GetFileRawData();
								Speccy.SaveFileToDiskAdvanced(new File(asm, file.GetFilename()), data, cpmdata,
										data.length, sbd.BasicType, sbd.LineStart, sbd.VarStart, sbd.LoadAddress, "",
										GeneralUtils.EXPORT_TYPE_ASM);
								Speccy.SaveFileToDiskAdvanced(new File(cpm, file.GetFilename()), data, cpmdata,
										data.length, sbd.BasicType, sbd.LineStart, sbd.VarStart, sbd.LoadAddress, "",
										GeneralUtils.EXPORT_TYPE_RAWANDHEADER);
								Speccy.SaveFileToDiskAdvanced(new File(hex, file.GetFilename()), data, cpmdata,
										data.length, sbd.BasicType, sbd.LineStart, sbd.VarStart, sbd.LoadAddress, "",
										GeneralUtils.EXPORT_TYPE_HEX);
								Speccy.SaveFileToDiskAdvanced(new File(raw, file.GetFilename()), data, cpmdata,
										data.length, sbd.BasicType, sbd.LineStart, sbd.VarStart, sbd.LoadAddress, "",
										GeneralUtils.EXPORT_TYPE_RAW);

								Plus3DosFileHeader p3d = null;
								if (PartClass.contains("PLUS3DOSPartition")) {
									p3d = ((hddEditor.libs.partitions.cpm.CPMDirectoryEntry) file).GetPlus3DosHeader();
								}
								boolean isUnknown = (sbd.BasicType < 0) || (sbd.BasicType > 3);

								int SpeccyFileType = 0;
								int basicLine = 0;
								int basicVarsOffset = data.length;
								int codeLoadAddress = 0;
								int filelength = 0;
								String arrayVarName = "";
								if (p3d == null || !p3d.IsPlusThreeDosFile) {
									SpeccyFileType = Speccy.BASIC_CODE;
									codeLoadAddress = 0x10000 - data.length;
									filelength = data.length;
								} else {
									filelength = p3d.filelength;
									SpeccyFileType = p3d.filetype;
									basicLine = p3d.line;
									basicVarsOffset = p3d.VariablesOffset;
									codeLoadAddress = p3d.loadAddr;
									arrayVarName = p3d.VarName;
								}
								try {
									int actiontype = GeneralUtils.EXPORT_TYPE_RAW;
									if (isUnknown) { // Options are: "Raw", "Hex", "Assembly"
										actiontype = UnknownType;
									} else {
										// Identifed BASIC File type
										if (SpeccyFileType == Speccy.BASIC_BASIC) { // Options are: "Text", "Raw",
																					// "Raw+Header",
																					// "Hex"
											actiontype = BasicType;
										} else if ((SpeccyFileType == Speccy.BASIC_NUMARRAY)
												&& (SpeccyFileType == Speccy.BASIC_CHRARRAY)) {
											actiontype = ArrayType;
										} else if ((filelength == 6912)) { // { "PNG", "GIF", "JPEG", "Raw",
																			// "Raw+Header", "Hex", "Assembly" };
											actiontype = ScreenType;
										} else { // CODE Options: { "Raw", "Raw+Header", "Assembly", "Hex" };
											actiontype = CodeType;
										}
									}

									Speccy.SaveFileToDiskAdvanced(new File(filefolder, file.GetFilename()), data,
											cpmdata, filelength, SpeccyFileType, basicLine, basicVarsOffset,
											codeLoadAddress, arrayVarName, actiontype);
									SysConfig = new FileWriter(new File(filefolder, "partition.index"), true);
									try {
										PrintWriter SysConfigp = new PrintWriter(SysConfig);
										try {
											SysConfigp.println("<file>");
											SysConfigp.println(
													"   <filename>" + file.GetFilename().trim() + "</filename>");
											SysConfigp
													.println("   <filelength>" + file.GetFileSize() + "</filelength>");

											if (p3d == null || !p3d.IsPlusThreeDosFile) {
												// Treat CPM files as raw files.
												SysConfigp.println("   <origfiletype>TAP</origfiletype>");
												SysConfigp.println("   <specbasicinfo>");
												SysConfigp.println("       <filetype>3</filetype>");
												SysConfigp.println("       <filetypename>" + Speccy.FileTypeAsString(3)
														+ "</filetypename>");
												SysConfigp.println("       <codeloadaddr>32768</codeloadaddr>");
												SysConfigp.println("   </specbasicinfo>");
											} else {
												SysConfigp.println("   <origfiletype>TAP</origfiletype>");
												SysConfigp.println("   <specbasicinfo>");
												SysConfigp.println("       <filetype>" + p3d.filetype + "</filetype>");
												SysConfigp.println("       <filetypename>"
														+ Speccy.FileTypeAsString(p3d.filetype) + "</filetypename>");
												SysConfigp.println(
														"       <basicsize>" + p3d.filelength + "</basicsize>");
												SysConfigp.println(
														"       <basicstartline>" + p3d.line + "</basicstartline>");
												SysConfigp.println(
														"       <codeloadaddr>" + p3d.loadAddr + "</codeloadaddr>");
												SysConfigp.println("       <basicvarsoffset>" + p3d.VariablesOffset
														+ "</basicvarsoffset>");
												SysConfigp.println(
														"       <arrayvarname>" + p3d.VarName + "</arrayvarname>");
												SysConfigp.println("   </specbasicinfo>");
											}
											if (file.getClass().getName().endsWith("TapDirectoryEntry")) {
												SysConfigp.println("   <tap>");
												SysConfigp.println("       <srcfile>"
														+ ThisDisk.CurrentDisk.GetFilename() + "</srcfile>");
												TapDirectoryEntry tf = (TapDirectoryEntry) file;
												SysConfigp.println("       <datablocknum>" + tf.DataBlock.blocknum
														+ "</datablocknum>");
												if (tf.HeaderBlock != null) {
													SysConfigp.println("       <headerblocknum>"
															+ tf.HeaderBlock.blocknum + "</headerblocknum>");
												}
												SysConfigp.println("   </tap>");
											}

											SysConfigp.println("</file>");
										} finally {
											SysConfigp.close();
										}
									} finally {
										SysConfig.close();
									}

								} catch (Exception E) {
									System.out.println("\nError extracting " + file.GetFilename() + "For folder: "
											+ filefolder + " - " + E.getMessage());
									E.printStackTrace();
								}
							} catch (Exception E) {
								System.out.println("Error exporting " + file.GetFilename());
								System.out.println(E.getMessage());
							}
						}

					}
				}
			} finally {
				pep.close();
			}
			return true;
		}
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
		String TargetFolder = Targetfile.getText();
		return (DoExportFlags(BasicType, CodeType, ArrayType, ScreenType, UnknownType, SwapType, TargetFolder));
	}

	/**
	 * 
	 * @param BasicType
	 * @param CodeType
	 * @param ArrayType
	 * @param ScreenType
	 * @param UnknownType
	 * @param SwapType
	 * @param TargetFolder
	 * @return
	 */
	protected boolean DoExportFlags(int BasicType, int CodeType, int ArrayType, int ScreenType, int UnknownType,
			int SwapType, String TargetFolder) {
		PartitionExportProgress pep = new PartitionExportProgress(shell.getDisplay());
		try {
			pep.Show("Exporting Partition " + ThisDisk.GetName(), "Partition:", "File:");
			pep.SetMax1(1);
			pep.SetMax2(1);
			pep.SetValue1(0);
			pep.SetValue2(0);
			int PartNum = 0;
			File directory = new File(TargetFolder);
			pep.setMessage1("Exporting partition " + ThisDisk.GetName() + " ("
					+ PLUSIDEDOS.GetTypeAsString(ThisDisk.GetPartType()) + ")");
			pep.SetValue1(PartNum++);
			if (ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_FREE
					&& ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_UNUSED
					&& ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_UNKNOWN
					&& ThisDisk.GetPartType() != PLUSIDEDOS.PARTITION_BAD) {
				try {
//					long start = System.currentTimeMillis();
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
							}, IncludeDeleted.getSelection());

//					long finish = System.currentTimeMillis();
//					System.out.println(String.valueOf(finish - start) + "ms");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} finally {
			pep.close();
		}
		return true;
	}

	/**
	 * 
	 * @return
	 */
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
				if (!partition.CanExport) {
					System.out.print("Skipping partition " + partition.GetName());
				} else {

					String strDirName = partition.GetName().replace("+", "Plus");
					strDirName = strDirName.replace("/", "").replace("*", "");

					File BaseFolder = new File(directory, strDirName);
					System.out.print("Extracting partition " + BaseFolder + " - ");

					if (!BaseFolder.exists()) {
						BaseFolder.mkdir();
					}
					try {
//						long start = System.currentTimeMillis();
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
								}, IncludeDeleted.getSelection());

//						long finish = System.currentTimeMillis();
//						System.out.println(String.valueOf(finish - start) + "ms");
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

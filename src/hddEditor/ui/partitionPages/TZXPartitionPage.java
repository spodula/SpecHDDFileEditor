package hddEditor.ui.partitionPages;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.Languages;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.disks.LINEAR.TZXFile;
import hddEditor.libs.disks.LINEAR.tzxblocks.TZXBlock;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.TZXPartition;
import hddEditor.libs.partitions.tzx.TzxDirectoryEntry;
import hddEditor.ui.FileExportAllPartitionsForm;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.dialogs.HexEditDialog;
import hddEditor.ui.partitionPages.dialogs.RenameFileDialog;
import hddEditor.ui.partitionPages.dialogs.AddFiles.AddFilesToTZXPartition;
import hddEditor.ui.partitionPages.dialogs.drop.DropFilesToTapePartition;
import hddEditor.ui.partitionPages.dialogs.edit.TzxFileEditDialog;

public class TZXPartitionPage extends GenericPage {
	Table DirectoryListing = null;
	HexEditDialog HxEditDialog = null;
	RenameFileDialog RenFileDialog = null;
	TzxFileEditDialog SpecFileEditDialog = null;
	AddFilesToTZXPartition AddFilesDialog = null;

	/**
	 * 
	 * 
	 * @param root
	 * @param parent
	 * @param partition
	 */
	public TZXPartitionPage(HDDEditor root, Composite parent, IDEDosPartition partition, FileSelectDialog filesel, Languages lang) {
		super(root, parent, partition, filesel,lang);
		AddComponents();
	}

	/**
	 * 
	 */
	private void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			TZXPartition tzx = (TZXPartition) partition;
			label(lang.Msg(Languages.FILET_TZXFILE) , 4);
			label(lang.Msg(Languages.MSG_DATABLOCKS)+": " + tzx.DirectoryEntries.length, 1);
			label("", 1);
			label("", 1);

			// directory listing
			DirectoryListing = new Table(ParentComp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
			DirectoryListing.setLinesVisible(true);

			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.horizontalSpan = 4;
			gd.heightHint = 400;
			DirectoryListing.setLayoutData(gd);

			TableColumn tc1 = new TableColumn(DirectoryListing, SWT.LEFT);
			TableColumn tc2 = new TableColumn(DirectoryListing, SWT.LEFT);
			TableColumn tc3 = new TableColumn(DirectoryListing, SWT.LEFT);
			TableColumn tc4 = new TableColumn(DirectoryListing, SWT.LEFT);
			TableColumn tc5 = new TableColumn(DirectoryListing, SWT.FILL);
			tc1.setText(lang.Msg(Languages.MSG_FILENAME));
			tc2.setText(lang.Msg(Languages.MSG_BLOCKS));
			tc3.setText(lang.Msg(Languages.MSG_FILETYPE));
			tc4.setText(lang.Msg(Languages.MSG_LENGTH));
			tc5.setText(lang.Msg(Languages.MSG_NOTES));
			tc1.setWidth(150);
			tc2.setWidth(60);
			tc3.setWidth(150);
			tc4.setWidth(100);
			tc5.setWidth(100);
			DirectoryListing.setHeaderVisible(true);

			/***********************************************************************************/

			// Create the drop target
			DropTarget target = new DropTarget(DirectoryListing, DND.DROP_LINK | DND.DROP_COPY | DND.DROP_DEFAULT);
			Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
			target.setTransfer(types);
			target.addDropListener(new DropTargetAdapter() {
				public void dragEnter(DropTargetEvent event) {
					if (event.detail == DND.DROP_DEFAULT) {
						event.detail = (event.operations & DND.DROP_COPY) != 0 ? DND.DROP_COPY : DND.DROP_NONE;
					}

					// Allow dropping text only
					for (int i = 0, n = event.dataTypes.length; i < n; i++) {
						if (TextTransfer.getInstance().isSupportedType(event.dataTypes[i])) {
							event.currentDataType = event.dataTypes[i];
						}
					}
				}

				public void dragOver(DropTargetEvent event) {
					event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
				}

				public void drop(DropTargetEvent event) {
					if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
						String[] filenames = null;
						if (event.data instanceof String[]) {
							filenames = (String[]) event.data;
						} else if (event.data instanceof String) {
							filenames = ((String) event.data).split("\n");
						}
						if (filenames != null) {
							DoDropFile(filenames);
						}
					}
				}
			});
			/***********************************************************************************/
			DragSource source = new DragSource(DirectoryListing, DND.DROP_MOVE | DND.DROP_COPY);
			source.setTransfer(new Transfer[] { FileTransfer.getInstance() });
			source.addDragListener(new DragSourceListener() {
				File tempfiles[];

				public void dragStart(DragSourceEvent event) {
					event.doit = DirectoryListing.getSelectionCount() != 0;
					if (event.doit) {
						TableItem ItemsToDrag[] = DirectoryListing.getSelection();
						tempfiles = new File[ItemsToDrag.length];
						int i = 0;
						for (TableItem item : ItemsToDrag) {
							try {
								File f = File.createTempFile("YYYY", "xxx");
								int exporttype = RootPage.dragindex;

								FileEntry entry = (FileEntry) item.getData();
								if (exporttype == HDDEditor.DRAG_TYPE) {
									SpeccyBasicDetails sd = entry.GetSpeccyBasicDetails();
									int actiontype = GeneralUtils.EXPORT_TYPE_HEX;
									switch (sd.BasicType) {
									case (Speccy.BASIC_BASIC):
										actiontype = GeneralUtils.EXPORT_TYPE_TXT;
										break;
									case (Speccy.BASIC_NUMARRAY):
										actiontype = GeneralUtils.EXPORT_TYPE_CSV;
										break;
									case (Speccy.BASIC_CHRARRAY):
										actiontype = GeneralUtils.EXPORT_TYPE_CSV;
										break;
									case (Speccy.BASIC_CODE):
										if (entry.GetFileSize() == 0x1b00) {
											actiontype = GeneralUtils.EXPORT_TYPE_PNG;
										} else {
											actiontype = GeneralUtils.EXPORT_TYPE_HEX;
										}
										break;
									default:
										actiontype = GeneralUtils.EXPORT_TYPE_HEX;
									}

									Speccy.SaveFileToDiskAdvanced(f, entry.GetFileData(), entry.GetFileData(),
											entry.GetFileData().length, sd.BasicType, sd.LineStart, sd.VarStart,
											sd.LoadAddress, sd.VarName + "", actiontype);
								} else if (exporttype == HDDEditor.DRAG_RAW) {
									GeneralUtils.WriteBlockToDisk(entry.GetFileData(), f);
								} else {
									SpeccyBasicDetails sd = entry.GetSpeccyBasicDetails();
									Speccy.SaveFileToDiskAdvanced(f, entry.GetFileData(), entry.GetFileData(),
											entry.GetFileData().length, sd.BasicType, sd.LineStart, sd.VarStart,
											sd.LoadAddress, sd.VarName + "", GeneralUtils.EXPORT_TYPE_HEX);
								}
								File f1 = new File(f.getParent(), entry.GetFilename());
								f.renameTo(f1);
								f.delete();
								f1.deleteOnExit();
								tempfiles[i] = f1;
								i++;
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}

				public void dragSetData(DragSourceEvent event) {
					// Provide the data of the requested type.
					if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
						if (tempfiles != null && tempfiles.length > 0) {
							String data[] = new String[tempfiles.length];
							for (int i = 0; i < tempfiles.length; i++) {
								File fle = tempfiles[i];
								data[i] = fle.getAbsolutePath();
							}
							event.data = data;
						}
					}
				}

				public void dragFinished(DragSourceEvent event) {
					if (event.detail == DND.DROP_MOVE) {

					}
				}
			});
			/***********************************************************************************/

			UpdateDirectoryEntryList();

			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.widthHint = 200;

			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.widthHint = 200;

			Button Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText(lang.Msg(Languages.MSG_FILEPROPERTIES));
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoEditFile();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});

			Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText(lang.Msg(Languages.MSG_DELETEFILE));
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoDeleteFile();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});

			Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText(lang.Msg(Languages.MSG_ADDGFILES));
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoAddFiles();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});

			Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText(lang.Msg(Languages.MSG_EXTRACTALLFILES));
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoExtractAllFiles();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
			Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText(lang.Msg(Languages.MSG_RENAMEFILE));
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoRenameFile();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
			ParentComp.pack();
			Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText(lang.Msg(Languages.MSG_MOVEFUP));
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoMoveUp();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
			Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText(lang.Msg(Languages.MSG_MOVEFDOWN));
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoMoveDown();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
			ParentComp.pack();
		}
	}

	/**
	 * 
	 * @param filenames
	 */
	protected void DoDropFile(String[] filenames) {
		File fFiles[] = new File[filenames.length];
		int i = 0;
		for (String file : filenames) {
			try {
				URI uri = new URI(file);
				file = uri.getPath();
			} catch (URISyntaxException e) {
				System.out.println(String.format(lang.Msg(Languages.MSG_CANNOTPARSE), file));
			}
			System.out.println(file);
			fFiles[i++] = new File(file);
		}

		DropFilesToTapePartition DropFilesDialog = new DropFilesToTapePartition(ParentComp.getDisplay(), lang);
		DropFilesDialog.Show(lang.Msg(Languages.MSG_ADDGFILES), partition, fFiles);
		DropFilesDialog = null;
		if (!ParentComp.isDisposed()) {
			AddComponents();
		}
	}

	/**
	 * Update the directory listing
	 */
	private void UpdateDirectoryEntryList() {
		if (!DirectoryListing.isDisposed()) {
			DirectoryListing.removeAll();
			TZXPartition tzx = (TZXPartition) partition;
			for (TzxDirectoryEntry entry : tzx.DirectoryEntries) {
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String content[] = new String[5];

				content[0] = entry.GetFilename();
				String blocknum = "";
				if (entry.HeaderBlock != null) {
					blocknum = String.valueOf(entry.HeaderBlock.BlockNumber);
				}
				if (entry.DataBlock != null) {
					if (!blocknum.isBlank()) {
						blocknum = blocknum + ", ";
					}

					blocknum = blocknum + String.valueOf(entry.DataBlock.BlockNumber);
				}
				content[1] = blocknum;

				content[2] = entry.GetFileTypeString();
				content[3] = String.valueOf(entry.GetFileSize());

				String notes = "";
				SpeccyBasicDetails spd = entry.GetSpeccyBasicDetails();
				if (spd.BasicType != -1) {
					notes = spd.GetSpecificDetails().replace("\n", ",");
				} else {
					notes = entry.DataBlock.toString();
					int i = notes.indexOf(')');
					if (i > 4) {
						notes = notes.substring(i + 1).trim();
					}

				}

				content[4] = notes.trim();
				item2.setText(content);
				item2.setData(entry);
			}
		}
	}

	/**
	 * Implementation of DoEditFile
	 */
	protected void DoEditFile() {
		boolean DoAgain = true;

		while (DoAgain == true) {
			DoAgain = false;
			TableItem itms[] = DirectoryListing.getSelection();
			if ((itms != null) && (itms.length != 0)) {
				TzxDirectoryEntry entry = (TzxDirectoryEntry) itms[0].getData();
				SpecFileEditDialog = new TzxFileEditDialog(ParentComp.getDisplay(), fsd, partition, lang);

				byte data[];
				try {
					data = entry.GetFileRawData();

					if (SpecFileEditDialog.Show(data, String.format(lang.Msg(Languages.MSG_EDITINGX),entry.GetFilename()), entry)) {
						TZXFile tap = (TZXFile) partition.CurrentDisk;
						if (entry.HeaderBlock != null) {
							byte headerData[] = entry.HeaderBlock.data;
							headerData[11] = (byte) (data.length & 0xff);
							headerData[12] = (byte) ((data.length / 0x100) & 0xff);
							entry.HeaderBlock.UpdateBlockData(headerData);
						}

						entry.DataBlock.UpdateBlockData(data);
						tap.RewriteFile();
						tap.ParseTZXFile();
						((TZXPartition) partition).LoadPartitionSpecificInformation();
						// refresh the screen.
						AddComponents();
					} else {
						// There are two cases for SHOW returning false,
						// 1: Just closed, no changes
						// 2: File type change
						if (SpecFileEditDialog.FileTypeHasChanged) {
							SpeccyBasicDetails sbd = entry.GetSpeccyBasicDetails();
							if (sbd != null && sbd.IsValidFileType()) {
								TZXBlock header = entry.HeaderBlock;
								if (header != null) {
									System.out.print(lang.Msg(Languages.MSG_FILETYPE)+ ": " + sbd.BasicType + "("
											+ Speccy.SpecFileTypeToString(sbd.BasicType) + ") -> ");
									sbd.BasicType = SpecFileEditDialog.NewFileType;

									header.SetHeader(sbd);
									System.out.println(
											sbd.BasicType + "(" + Speccy.SpecFileTypeToString(sbd.BasicType) + ")");

									TZXPartition TzxPart = (TZXPartition) partition;
									TZXFile tzxfile = (TZXFile) TzxPart.CurrentDisk;
									try {
										tzxfile.RewriteFile();
										TzxPart.LoadPartitionSpecificInformation();
										DoAgain = true;
									} catch (IOException e) {
										e.printStackTrace();
									}
								} else {
									System.err.println(lang.Msg(Languages.MSG_UPDATEIGNORED));
								}
							} else {
								System.err.println(lang.Msg(Languages.MSG_UPDATEIGNORED));
							}
						}
					}

					SpecFileEditDialog = null;
					UpdateDirectoryEntryList();

				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * Force any dialogs that are still open to close. This avoids exceptions on
	 * exit.
	 */
	protected void CloseDialogs() {
		if (SpecFileEditDialog != null) {
			SpecFileEditDialog.close();
		}

		if (HxEditDialog != null) {
			HxEditDialog.close();
		}
		if (RenFileDialog != null) {
			RenFileDialog.close();
		}

		if (AddFilesDialog != null) {
			AddFilesDialog.close();
		}
	}

	/**
	 * Rename the selected file.
	 * 
	 */
	protected void DoRenameFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			TzxDirectoryEntry entry = (TzxDirectoryEntry) itms[0].getData();
			RenFileDialog = new RenameFileDialog(ParentComp.getDisplay(), lang);
			if (RenFileDialog.Show(entry.GetFilename())) {
				try {
					TZXPartition smp = (TZXPartition) partition;
					smp.RenameFile(entry, RenFileDialog.NewName);

					// refresh the screen.
					UpdateDirectoryEntryList();
				} catch (IOException e) {
					MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_ERROR | SWT.CLOSE);
					String s = String.format(lang.Msg(Languages.MSG_ERRORRENAME), entry.GetFilename()) + ": " + e.getMessage();
					messageBox.setMessage(s);
					messageBox.setText(s);
					messageBox.open();
					e.printStackTrace();
				}
			}
			RenFileDialog = null;
		}
	}

	/**
	 * Extract all files on this cartridge to a given folder
	 * 
	 */
	protected void DoExtractAllFiles() {
		FileExportAllPartitionsForm ExportAllPartsForm = new FileExportAllPartitionsForm(ParentComp.getDisplay(), lang);
		try {
			ExportAllPartsForm.ShowSinglePartition(partition);
		} finally {
			ExportAllPartsForm = null;
		}
	}

	/**
	 * Delete the selected file(s).
	 * 
	 */
	protected void DoDeleteFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			TzxDirectoryEntry entry = (TzxDirectoryEntry) itms[0].getData();
			String filename = entry.GetFilename();
			try {
				if (itms.length > 1) {
					filename = lang.Msg(Languages.MSG_THESELECTEDFILES);
				}
				MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
				String s = String.format(lang.Msg(Languages.MSG_AREYOUSUREDEL), filename);
				messageBox.setMessage(s);
				messageBox.setText(s);

				if (messageBox.open() == SWT.OK) {
					TZXPartition TZX = (TZXPartition) partition;
					for (TableItem itm : itms) {
						entry = (TzxDirectoryEntry) itm.getData();
						TZX.DeleteFile(entry, false);
					}
					AddComponents();
				}
			} catch (IOException e) {
				String s = String.format(lang.Msg(Languages.MSG_ERRDELFILE),filename);
				ErrorBox(s + ": "+ e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Show the Add files screen.
	 */
	protected void DoAddFiles() {
		AddFilesDialog = new AddFilesToTZXPartition(ParentComp.getDisplay(), fsd, lang);
		AddFilesDialog.Show(lang.Msg(Languages.MSG_ADDGFILES), (TZXPartition) partition);
		AddFilesDialog = null;
		UpdateDirectoryEntryList();
		if (!ParentComp.isDisposed()) {
			AddComponents();
		}
	}

	/**
	 * Move the selected files down the list.
	 */
	protected void DoMoveDown() {
		TZXPartition tapp = (TZXPartition) partition;
		if (DirectoryListing.getSelectionCount() > 0) {
			TableItem Selected[] = DirectoryListing.getSelection();
			for (int idx = Selected.length - 1; idx > -1; idx--) {
				TzxDirectoryEntry SelItm = (TzxDirectoryEntry) Selected[idx].getData();
				try {
					tapp.MoveDirectoryEntryDown(SelItm);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			AddComponents();
		}
	}

	/**
	 * Move the selected files up the list.
	 */
	protected void DoMoveUp() {
		TZXPartition tapp = (TZXPartition) partition;
		if (DirectoryListing.getSelectionCount() > 0) {
			TableItem Selected[] = DirectoryListing.getSelection();
			for (TableItem sel : Selected) {
				TzxDirectoryEntry SelItm = (TzxDirectoryEntry) sel.getData();
				try {
					tapp.MoveDirectoryEntryUp(SelItm);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			AddComponents();
		}
	}

}

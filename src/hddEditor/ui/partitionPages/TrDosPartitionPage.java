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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
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
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.TrDosPartition;
import hddEditor.libs.partitions.trdos.TrdDirectoryEntry;
import hddEditor.ui.FileExportAllPartitionsForm;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.dialogs.AddressNote;
import hddEditor.ui.partitionPages.dialogs.HexEditDialog;
import hddEditor.ui.partitionPages.dialogs.RenameFileDialog;
import hddEditor.ui.partitionPages.dialogs.AddFiles.AddFilesToTrDosPartition;
import hddEditor.ui.partitionPages.dialogs.drop.DropFilesToTapePartition;
import hddEditor.ui.partitionPages.dialogs.edit.TrDosFileEditDialog;

public class TrDosPartitionPage extends GenericPage {
	Table DirectoryListing = null;
	HexEditDialog HxEditDialog = null;
	TrDosFileEditDialog SpecFileEditDialog = null;

	RenameFileDialog RenFileDialog = null;
	AddFilesToTrDosPartition AddFilesDialog = null;

	/**
	 * 
	 * @param root
	 * @param parent
	 * @param partition+
	 */
	public TrDosPartitionPage(HDDEditor root, Composite parent, IDEDosPartition partition, FileSelectDialog filesel, Languages lang) {
		super(root, parent, partition, filesel,lang);
		AddComponents();
	}

	/**
	 * 
	 */
	private void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			super.AddBasicDetails();
			TrDosPartition fbc = (TrDosPartition) partition;
			
			label(lang.Msg(Languages.MSG_SIZE)+": " + (fbc.CurrentDisk.GetFileSize() / 1024) + "k", 1);
			label("", 4);
			label(lang.Msg(Languages.MSG_DISKLABEL)+": " + fbc.Disklabel, 1);
			label(lang.Msg(Languages.MSG_DISKLABEL)+": " + fbc.NumFreeSectors, 1);
			label(lang.Msg(Languages.MSG_FREESPACE)+": " + (fbc.NumFreeSectors * fbc.CurrentDisk.GetSectorSize() / 1024) + "k", 1);
			label(lang.Msg(Languages.MSG_FILES)+": " + fbc.NumFiles, 1);
			label(lang.Msg(Languages.MSG_DELETEDFILES)+": " + fbc.NumDeletedFiles, 1);
			label(lang.Msg(Languages.MSG_LOGICALDISKTYP)+": " + fbc.LogicalDiskType + "(" + fbc.GetDiskTypeAsString() + ")", 1);
			label(lang.Msg(Languages.MSG_FIRSTFREESECT)+": (C/S) " + fbc.FirstFreeSectorT + "/" + fbc.FirstFreeSectorS, 1);
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
			TableColumn tc5 = new TableColumn(DirectoryListing, SWT.LEFT);
			tc1.setText(lang.Msg(Languages.MSG_FILENAME));
			tc2.setText(lang.Msg(Languages.MSG_FILETYPE));
			tc3.setText(lang.Msg(Languages.MSG_START));
			tc4.setText(lang.Msg(Languages.MSG_LENGTH));
			tc5.setText(lang.Msg(Languages.MSG_SECTORS));
			tc1.setWidth(150);
			tc2.setWidth(150);
			tc3.setWidth(150);
			tc4.setWidth(150);
			tc5.setWidth(100);
			DirectoryListing.setHeaderVisible(true);

			/***********************************************************************************/
			Listener sortListener = new Listener() {
				public void handleEvent(Event e) {
					TableColumn column = (TableColumn) e.widget;
					TrDosPartition p = (TrDosPartition) partition;
					if (column == tc1)
						p.SortDirectoryEntries(IDEDosPartition.SORTTYPE_NAME);
					if (column == tc2)
						p.SortDirectoryEntries(IDEDosPartition.SORTTYPE_TYPE);
					if (column == tc4)
						p.SortDirectoryEntries(IDEDosPartition.SORTTYPE_SIZE);
					if (column == tc5)
						p.SortDirectoryEntries(IDEDosPartition.SORTTYPE_SIZE);
					DirectoryListing.setSortColumn(column);
					UpdateDirectoryEntryList();
				}
			};
			tc1.addListener(SWT.Selection, sortListener);
			tc2.addListener(SWT.Selection, sortListener);
			tc3.addListener(SWT.Selection, sortListener);
			tc4.addListener(SWT.Selection, sortListener);

			/***********************************************************************************/

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
											sd.LoadAddress, sd.VarName + "", actiontype, lang);
								} else if (exporttype == HDDEditor.DRAG_RAW) {
									GeneralUtils.WriteBlockToDisk(entry.GetFileData(), f);
								} else {
									SpeccyBasicDetails sd = entry.GetSpeccyBasicDetails();
									Speccy.SaveFileToDiskAdvanced(f, entry.GetFileData(), entry.GetFileData(),
											entry.GetFileData().length, sd.BasicType, sd.LineStart, sd.VarStart,
											sd.LoadAddress, sd.VarName + "", GeneralUtils.EXPORT_TYPE_HEX, lang);
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
			Btn.setText(lang.Msg(Languages.MSG_EDITRAWFILE));
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoEditRawFile();
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

			Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText(lang.Msg(Languages.MSG_DEFRAGDISK));
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoPackDisk();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
			ParentComp.pack();
		}
	}

	protected void DoAddFiles() {
		AddFilesDialog = new AddFilesToTrDosPartition(ParentComp.getDisplay(), fsd, lang);
		AddFilesDialog.Show(lang.Msg(Languages.MSG_ADDGFILES), (TrDosPartition) partition);
		UpdateDirectoryEntryList();
		AddFilesDialog = null;
		if (!ParentComp.isDisposed()) {
			AddComponents();
		}
	}

	protected void DoDropFile(String[] filenames) {
		File fFiles[] = new File[filenames.length];
		int i = 0;
		for (String file : filenames) {
			try {
				URI uri = new URI(file);
				file = uri.getPath();
			} catch (URISyntaxException e) {
				System.out.println(String.format(lang.Msg(Languages.MSG_CANNOTPARSE),file));
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
	 * 
	 */
	protected void DoRenameFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			TrdDirectoryEntry entry = (TrdDirectoryEntry) itms[0].getData();
			RenFileDialog = new RenameFileDialog(ParentComp.getDisplay(), lang);
			if (RenFileDialog.Show(entry.GetFilename())) {
				try {
					TrDosPartition fbc = (TrDosPartition) partition;
					fbc.RenameFile(entry.GetFilename(), entry.GetFileType(), RenFileDialog.NewName);

					// refresh the screen.
					UpdateDirectoryEntryList();
				} catch (IOException e) {
					MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_ERROR | SWT.CLOSE);
					String s = String.format(lang.Msg(Languages.MSG_ERRORRENAME), entry.GetFilename()) + ": "+e.getMessage();
					messageBox.setMessage(s);
					messageBox.setText(s);
					messageBox.open();
					e.printStackTrace();
				}
			}
			RenFileDialog = null;
		}
	}

	protected void DoExtractAllFiles() {
		FileExportAllPartitionsForm ExportAllPartsForm = new FileExportAllPartitionsForm(ParentComp.getDisplay(), lang);
		try {
			ExportAllPartsForm.ShowSinglePartition(partition);
		} finally {
			ExportAllPartsForm = null;
		}
	}

	/**
	 * 
	 */
	private void UpdateDirectoryEntryList() {
		if (!DirectoryListing.isDisposed()) {
			DirectoryListing.removeAll();
			TrDosPartition fbc = (TrDosPartition) partition;
			for (TrdDirectoryEntry entry : fbc.DirectoryEntries) {
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String content[] = new String[5];
				content[0] = entry.GetFilename();
				content[1] = entry.GetFileType() + " (" + entry.GetFileTypeString() + ")";
				content[2] = "Ch:" + entry.GetStartTrack() + " S:" + entry.GetStartSector();
				content[3] = String.valueOf(entry.GetFileSize());
				content[4] = String.valueOf(entry.GetFileLengthSectors());
				item2.setText(content);
				item2.setData(entry);
			}
		}
	}

	/**
	 * 
	 */
	protected void DoPackDisk() {
		TrDosPartition fbc = (TrDosPartition) partition;
		try {
			fbc.Pack();
			UpdateDirectoryEntryList();
		} catch (IOException e) {
			ErrorBox(lang.Msg(Languages.MSG_ERRIODEL)+"." + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * @throws IOException
	 * 
	 */
	protected void DoDeleteFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			TrdDirectoryEntry entry = (TrdDirectoryEntry) itms[0].getData();
			String filename = entry.GetFilename();
			if (itms.length > 1) {
				filename = lang.Msg(Languages.MSG_THESELECTEDFILES);
			}
			MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
			String s = String.format(lang.Msg(Languages.MSG_AREYOUSUREDEL), filename);
			messageBox.setMessage(s);
			messageBox.setText(s);

			if (messageBox.open() == SWT.YES) {
				TrDosPartition fbc = (TrDosPartition) partition;
				try {
					for (TableItem itm : itms) {
						entry = (TrdDirectoryEntry) itm.getData();
						filename = entry.GetFilename();
						if (entry.GetFileType() != ' ') {
							filename = filename + "." + entry.GetFileType();
						}
						fbc.DeleteFile(filename);
					}
					UpdateDirectoryEntryList();
				} catch (IOException e) {
					s = String.format(lang.Msg(Languages.MSG_ERRDELFILE),filename);
					ErrorBox(s + ": "+ e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 
	 */
	protected void DoEditRawFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			TrdDirectoryEntry entry = (TrdDirectoryEntry) itms[0].getData();
			// Create the hex edit dialog and start it.
			HxEditDialog = new HexEditDialog(ParentComp.getDisplay(), lang);

			byte data[];
			try {
				data = entry.GetFileData();

				AddressNote NewAddressNote = new AddressNote(0, data.length, 0, lang.Msg(Languages.MSG_FILE)+": " + entry.GetFilename());
				AddressNote ANArray[] = { NewAddressNote };

				boolean WriteBackData = HxEditDialog.Show(data,String.format(lang.Msg(Languages.MSG_EDITINGX),entry.GetFilename()), ANArray, fsd);
				if (WriteBackData) {
					TrDosPartition fbc = (TrDosPartition) partition;
					fbc.UpdateFile(entry, data);
				}
			} catch (IOException e) {
				ErrorBox(String.format(lang.Msg(Languages.MSG_ERROREDITING))+": " + e.getMessage());
				e.printStackTrace();
			}

			HxEditDialog = null;
			UpdateDirectoryEntryList();
		}
	}

	/**
	 * The EDIT FILE button has been pressed.
	 */
	private void DoEditFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			TrdDirectoryEntry entry = (TrdDirectoryEntry) itms[0].getData();
			try {
				SpecFileEditDialog = new TrDosFileEditDialog(ParentComp.getDisplay(), fsd, partition,lang);

				byte[] data = entry.GetFileData();
				
				GeneralUtils.HexDump(data, 0, data.length, 0);
				
				if (SpecFileEditDialog.Show(data, String.format(lang.Msg(Languages.MSG_EDITINGX),entry.GetFilename()), entry)) {
					// entry.SetDeleted(true);

					// refresh the screen.
					AddComponents();
				} else {
					// There are two cases for SHOW returning false,
					// 1: Just closed, no changes
					// 2: File type change
					if (SpecFileEditDialog.FileTypeHasChanged) {
						System.out.print(lang.Msg(Languages.MSG_FILETYPE)+": " + entry.GetFileType() + " -> ");
						entry.SetFileType(SpecFileEditDialog.NewFileType.charAt(0));
						System.out.println(entry.GetFileType());

						TrDosPartition part = (TrDosPartition) partition;
						part.UpdateDirentsOnDisk();
					}
				}

				SpecFileEditDialog = null;
				UpdateDirectoryEntryList();
			} catch (IOException e) {
				ErrorBox(lang.Msg(Languages.MSG_ERRORREADINGPART)+ ": " + e.getMessage());
				e.printStackTrace();
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
		if (RenFileDialog != null) {
			RenFileDialog.close();
		}
		if (HxEditDialog != null) {
			HxEditDialog.close();
		}
		if (AddFilesDialog != null) {
			AddFilesDialog.close();
		}
	}
}

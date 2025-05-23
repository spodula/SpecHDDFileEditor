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
	public TrDosPartitionPage(HDDEditor root, Composite parent, IDEDosPartition partition, FileSelectDialog filesel) {
		super(root, parent, partition, filesel);
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
			label("Size: " + (fbc.CurrentDisk.GetFileSize() / 1024) + "k", 1);
			label("", 4);
			label("Label: " + fbc.Disklabel, 1);
			label("Free sectors: " + fbc.NumFreeSectors, 1);
			label("Free Space: " + (fbc.NumFreeSectors * fbc.CurrentDisk.GetSectorSize() / 1024) + "k", 1);
			label("Files: " + fbc.NumFiles, 1);
			label("Deleted files: " + fbc.NumDeletedFiles, 1);
			label("Logical disk type: " + fbc.LogicalDiskType + "(" + fbc.GetDiskTypeAsString() + ")", 1);
			label("First free sector: (C/S) " + fbc.FirstFreeSectorT + "/" + fbc.FirstFreeSectorS, 1);
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
			tc1.setText("Filename");
			tc2.setText("Type");
			tc3.setText("Start");
			tc4.setText("Length");
			tc5.setText("Sectors");
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
								System.out.println("Exporttype:" + exporttype);
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
										System.out.println("CODE: " + entry.GetFileSize());
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

			Button Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText("File Properties");
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
			Btn.setText("Edit Raw file");
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
			Btn.setText("Delete file");
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
			Btn.setText("Add File(s)");
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
			Btn.setText("Extract all Files");
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
			Btn.setText("Rename file");
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
			Btn.setText("Defrag disk");
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
		AddFilesDialog = new AddFilesToTrDosPartition(ParentComp.getDisplay(), fsd);
		AddFilesDialog.Show("Add files", (TrDosPartition) partition);
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
				System.out.println("Cannot parse " + file);
			}
			System.out.println(file);
			fFiles[i++] = new File(file);
		}

		DropFilesToTapePartition DropFilesDialog = new DropFilesToTapePartition(ParentComp.getDisplay());
		DropFilesDialog.Show("Add files", partition, fFiles);
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
			RenFileDialog = new RenameFileDialog(ParentComp.getDisplay());
			if (RenFileDialog.Show(entry.GetFilename())) {
				try {
					TrDosPartition fbc = (TrDosPartition) partition;
					fbc.RenameFile(entry.GetFilename(), entry.GetFileType(), RenFileDialog.NewName);

					// refresh the screen.
					UpdateDirectoryEntryList();
				} catch (IOException e) {
					MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_ERROR | SWT.CLOSE);
					messageBox.setMessage("Error Renaming " + entry.GetFilename() + ": " + e.getMessage());
					messageBox.setText("Error Renaming " + entry.GetFilename() + ": " + e.getMessage());
					messageBox.open();
					e.printStackTrace();
				}
			}
			RenFileDialog = null;
		}
	}

	protected void DoExtractAllFiles() {
		FileExportAllPartitionsForm ExportAllPartsForm = new FileExportAllPartitionsForm(ParentComp.getDisplay());
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
			ErrorBox("IO Error deleting file." + e.getMessage());
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
				filename = "the selected files";
			}
			MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
			messageBox.setMessage("Are you sure you want to delete " + filename + " ?");
			messageBox.setText("Are you sure you want to delete " + filename + " ?");
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
					ErrorBox("IO Error deleting file." + e.getMessage());
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
			HxEditDialog = new HexEditDialog(ParentComp.getDisplay());

			byte data[];
			try {
				data = entry.GetFileData();

				AddressNote NewAddressNote = new AddressNote(0, data.length, 0, "File: " + entry.GetFilename());
				AddressNote ANArray[] = { NewAddressNote };

				boolean WriteBackData = HxEditDialog.Show(data, "Editing " + entry.GetFilename(), ANArray, fsd);
				if (WriteBackData) {
					TrDosPartition fbc = (TrDosPartition) partition;
					fbc.UpdateFile(entry, data);
				}
			} catch (IOException e) {
				ErrorBox("Error editing partition: " + e.getMessage());
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
				SpecFileEditDialog = new TrDosFileEditDialog(ParentComp.getDisplay(), fsd, partition);

				byte[] data = entry.GetFileData();
				
				GeneralUtils.HexDump(data, 0, data.length, 0);
				
				if (SpecFileEditDialog.Show(data, "Editing " + entry.GetFilename(), entry)) {
					// entry.SetDeleted(true);

					// refresh the screen.
					AddComponents();
				} else {
					// There are two cases for SHOW returning false,
					// 1: Just closed, no changes
					// 2: File type change
					if (SpecFileEditDialog.FileTypeHasChanged) {
						System.out.print("File type: " + entry.GetFileType() + " -> ");
						entry.SetFileType(SpecFileEditDialog.NewFileType.charAt(0));
						System.out.println(entry.GetFileType());

						TrDosPartition part = (TrDosPartition) partition;
						part.UpdateDirentsOnDisk();
					}
				}

				SpecFileEditDialog = null;
				UpdateDirectoryEntryList();
			} catch (IOException e) {
				ErrorBox("Error reading partition: " + e.getMessage());
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

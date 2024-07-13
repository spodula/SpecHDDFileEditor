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
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.disks.LINEAR.TAPFile;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.TAPPartition;
import hddEditor.libs.partitions.tap.TapDirectoryEntry;
import hddEditor.ui.FileExportAllPartitionsForm;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.dialogs.HexEditDialog;
import hddEditor.ui.partitionPages.dialogs.RenameFileDialog;
import hddEditor.ui.partitionPages.dialogs.AddFiles.AddFilesToTAPPartition;
import hddEditor.ui.partitionPages.dialogs.drop.DropFilesToTapePartition;
import hddEditor.ui.partitionPages.dialogs.edit.TapFileEditDialog;

public class TAPPartitionPage extends GenericPage {
	Table DirectoryListing = null;
	HexEditDialog HxEditDialog = null;
	RenameFileDialog RenFileDialog = null;
	TapFileEditDialog SpecFileEditDialog = null;
	AddFilesToTAPPartition AddFilesDialog = null;

	/**
	 * 
	 * 
	 * @param root
	 * @param parent
	 * @param partition
	 */
	public TAPPartitionPage(HDDEditor root, Composite parent, IDEDosPartition partition, FileSelectDialog filesel) {
		super(root, parent, partition, filesel);
		AddComponents();
	}

	/**
	 * 
	 */
	private void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			TAPPartition tap = (TAPPartition) partition;
			label("TAP file", 4);
			label("Files: " + tap.DirectoryEntries.length, 1);
			label("Data Blocks: " + ((TAPFile) tap.CurrentDisk).Blocks.length, 1);
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
			tc1.setText("Fileame");
			tc2.setText("Type");
			tc3.setText("Length");
			tc4.setText("Notes");
			tc1.setWidth(150);
			tc2.setWidth(150);
			tc3.setWidth(150);
			tc4.setWidth(100);
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
			ParentComp.pack();
			Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText("Move file up");
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
			Btn.setText("Move file Down");
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
	 * Update the directory listing
	 */
	private void UpdateDirectoryEntryList() {
		if (!DirectoryListing.isDisposed()) {
			DirectoryListing.removeAll();
			TAPPartition tap = (TAPPartition) partition;
			for (TapDirectoryEntry entry : tap.DirectoryEntries) {
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String content[] = new String[5];

				content[0] = entry.GetFilename();
				content[1] = entry.GetFileTypeString();
				content[2] = String.valueOf(entry.GetFileSize());

				String notes = "";
				SpeccyBasicDetails spd = entry.GetSpeccyBasicDetails();
				if (spd.BasicType != -1) {
					notes = spd.GetSpecificDetails().replace("\n", ",");
				}

				content[3] = notes.trim();
				item2.setText(content);
				item2.setData(entry);
			}
		}
	}

	/**
	 * Implementation of DoEditFile
	 */
	protected void DoEditFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			TapDirectoryEntry entry = (TapDirectoryEntry) itms[0].getData();
			SpecFileEditDialog = new TapFileEditDialog(ParentComp.getDisplay(), fsd);

			byte data[];
			try {
				data = entry.GetFileRawData();

				if (SpecFileEditDialog.Show(data, "Editing " + entry.GetFilename(), entry)) {
					TAPFile tap = (TAPFile) partition.CurrentDisk;
					if (entry.HeaderBlock != null) {
						byte headerData[] = entry.HeaderBlock.data;
						headerData[11] = (byte) (data.length & 0xff);
						headerData[12] = (byte) ((data.length / 0x100) & 0xff);
						entry.HeaderBlock.UpdateBlockData(headerData);
					}

					entry.DataBlock.UpdateBlockData(data);
					tap.RewriteFile();
					tap.ParseTAPFile();
					((TAPPartition) partition).LoadPartitionSpecificInformation();
					// refresh the screen.
					AddComponents();
				}
				SpecFileEditDialog = null;
			} catch (IOException e1) {
				e1.printStackTrace();
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
			TapDirectoryEntry entry = (TapDirectoryEntry) itms[0].getData();
			RenFileDialog = new RenameFileDialog(ParentComp.getDisplay());
			if (RenFileDialog.Show(entry.GetFilename())) {
				try {
					TAPPartition smp = (TAPPartition) partition;
					smp.RenameFile(entry, RenFileDialog.NewName);

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

	/**
	 * Extract all files on this cartridge to a given folder
	 * 
	 */
	protected void DoExtractAllFiles() {
		FileExportAllPartitionsForm ExportAllPartsForm = new FileExportAllPartitionsForm(ParentComp.getDisplay());
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
			TapDirectoryEntry entry = (TapDirectoryEntry) itms[0].getData();
			try {
				String filename = entry.GetFilename();
				if (itms.length > 1) {
					filename = "the selected files";
				}
				MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
				messageBox.setMessage("Are you sure you want to delete " + filename + " ?");
				messageBox.setText("Are you sure you want to delete " + filename + " ?");

				if (messageBox.open() == SWT.OK) {
					TAPPartition Tap = (TAPPartition) partition;
					for (TableItem itm : itms) {
						entry = (TapDirectoryEntry) itm.getData();
						Tap.DeleteFile(entry, false);
					}
					AddComponents();
				}
			} catch (IOException e) {
				ErrorBox("Error deleting file: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Show the Add files screen.
	 */
	protected void DoAddFiles() {
		AddFilesDialog = new AddFilesToTAPPartition(ParentComp.getDisplay(), fsd);
		AddFilesDialog.Show("Add files", (TAPPartition) partition);
		AddFilesDialog = null;
		if (!ParentComp.isDisposed()) {
			AddComponents();
		}
	}

	/**
	 * Move the selected files down the list.
	 */
	protected void DoMoveDown() {
		TAPPartition tapp = (TAPPartition) partition;
		if (DirectoryListing.getSelectionCount() > 0) {
			TableItem Selected[] = DirectoryListing.getSelection();
			for (int idx = Selected.length - 1; idx > -1; idx--) {
				TapDirectoryEntry SelItm = (TapDirectoryEntry) Selected[idx].getData();
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
		TAPPartition tapp = (TAPPartition) partition;
		if (DirectoryListing.getSelectionCount() > 0) {
			TableItem Selected[] = DirectoryListing.getSelection();
			for (TableItem sel : Selected) {
				TapDirectoryEntry SelItm = (TapDirectoryEntry) sel.getData();
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

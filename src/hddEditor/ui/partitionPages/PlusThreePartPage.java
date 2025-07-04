package hddEditor.ui.partitionPages;

import java.io.File;

/**
 * implementation of the +3DOS partition specific partition pages 
 */

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
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
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
import hddEditor.libs.partitions.PLUS3DOSPartition;
import hddEditor.libs.partitions.cpm.CPMDirectoryEntry;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;
import hddEditor.libs.partitions.CPMPartition;
import hddEditor.ui.FileExportAllPartitionsForm;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.dialogs.AddressNote;
import hddEditor.ui.partitionPages.dialogs.HexEditDialog;
import hddEditor.ui.partitionPages.dialogs.RenameFileDialog;
import hddEditor.ui.partitionPages.dialogs.AddFiles.AddFilesToPlus3Partition;
import hddEditor.ui.partitionPages.dialogs.drop.DropFilestoPlus3Partition;
import hddEditor.ui.partitionPages.dialogs.edit.Plus3DosFileEditDialog;

public class PlusThreePartPage extends GenericPage {
	Table DirectoryListing;

	/*
	 * Dialog pointers so we can forcibly close them if necessary.
	 */
	Plus3DosFileEditDialog SpecFileEditDialog = null;
	RenameFileDialog RenFileDialog = null;
	HexEditDialog HxEditDialog = null;
	AddFilesToPlus3Partition AddFilesDialog = null;
	DropFilestoPlus3Partition DropFilesDialog = null;

	/**
	 * Open a +3 Partition page
	 * 
	 * @param root      - The root object. used for global variables.
	 * @param parent    - Parent shell for the form.
	 * @param partition - Current partition.
	 * @param filesel   - File selection dialog object.
	 */
	public PlusThreePartPage(HDDEditor root, Composite parent, IDEDosPartition partition, FileSelectDialog filesel) {
		super(root, parent, partition, filesel);
		AddComponents();
	}

	/**
	 * Add all the components to the form
	 * 
	 */
	public void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			PLUS3DOSPartition pdp = (PLUS3DOSPartition) partition;
			super.AddBasicDetails();
			ParentComp.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent arg0) {
					CloseDialogs();
				}
			});

			label("", 4);
			Label l = label("Partition Details.", 1);
			FontData fontData = l.getFont().getFontData()[0];
			Font font = new Font(ParentComp.getDisplay(),
					new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
			l.setFont(font);
			label("", 3);

			if (pdp.DirectoryBlocks == 0 || pdp.bam == null) {
				label("Partition does not contain a valid CPM/+3 filesystem.", 4);
			} else {

				label("Partition free space: " + GeneralUtils.GetSizeAsString(pdp.freeSpace * 1024), 1);
				label("Drive Mapping: " + pdp.DriveLetter, 1);
				label("Reserved Tracks: " + pdp.ReservedTracks, 1);
				label("Disk size: " + GeneralUtils.GetSizeAsString(pdp.diskSize * 1024), 1);

				label("Blocksize: " + pdp.BlockSize, 1);
				label("Max Block: " + pdp.MaxBlock, 1);
				label("Used blocks: " + pdp.usedblocks, 1);
				label("Free space: " + GeneralUtils.GetSizeAsString(pdp.freeSpace * 1024), 1);

				label("Max dirent: " + pdp.MaxDirent, 1);
				label("Used dirents: " + pdp.usedDirEnts, 1);

				label("", 2);

				label("", 4);
				// directory listing
				DirectoryListing = new Table(ParentComp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
				DirectoryListing.setLinesVisible(true);

				GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				gd.horizontalSpan = 4;
				gd.heightHint = 100;
				DirectoryListing.setLayoutData(gd);

				TableColumn tc1 = new TableColumn(DirectoryListing, SWT.LEFT);
				TableColumn tc2 = new TableColumn(DirectoryListing, SWT.LEFT);
				TableColumn tc3 = new TableColumn(DirectoryListing, SWT.LEFT);
				TableColumn tc4 = new TableColumn(DirectoryListing, SWT.LEFT);
				TableColumn tc5 = new TableColumn(DirectoryListing, SWT.LEFT);
				tc1.setText("Filename");
				tc2.setText("Type");
				tc3.setText("CPM Length");
				tc4.setText("+3 Length");
				tc5.setText("Flags");
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
						CPMPartition p = (CPMPartition) partition;
						if (column == tc1)
							p.SortDirectoryEntries(IDEDosPartition.SORTTYPE_NAME);
						if (column == tc2)
							p.SortDirectoryEntries(IDEDosPartition.SORTTYPE_TYPE);
						if (column == tc3)
							p.SortDirectoryEntries(IDEDosPartition.SORTTYPE_SIZE);
						if (column == tc4)
							p.SortDirectoryEntries(IDEDosPartition.SORTTYPE_SIZE);
						DirectoryListing.setSortColumn(column);
						PopulateDirectory();
					}
				};
				tc1.addListener(SWT.Selection, sortListener);
				tc2.addListener(SWT.Selection, sortListener);
				tc3.addListener(SWT.Selection, sortListener);
				tc4.addListener(SWT.Selection, sortListener);

				/***********************************************************************************/

				// Create the drop target
				DropTarget target = new DropTarget(DirectoryListing, DND.DROP_LINK | DND.DROP_COPY | DND.DROP_DEFAULT);
				target.setTransfer(new Transfer[] { FileTransfer.getInstance() });
				target.addDropListener(new DropTargetAdapter() {
					public void dragEnter(DropTargetEvent event) {
						if (event.detail == DND.DROP_DEFAULT) {
							event.detail = (event.operations & DND.DROP_COPY) != 0 ? DND.DROP_COPY : DND.DROP_NONE;
						}

						// Allow dropping text only
						for (int i = 0, n = event.dataTypes.length; i < n; i++) {
							if (FileTransfer.getInstance().isSupportedType(event.dataTypes[i])) {
								event.currentDataType = event.dataTypes[i];
							}
						}
					}

					public void dragOver(DropTargetEvent event) {
						event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
					}

					public void drop(DropTargetEvent event) {
						if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
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

				PopulateDirectory();

				gd = new GridData(SWT.FILL, SWT.FILL, true, false);
				gd.widthHint = 200;

				Button Btn = new Button(ParentComp, SWT.PUSH);
				Btn.setText("File Properties");
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
				Btn.setLayoutData(gd);

				Btn = new Button(ParentComp, SWT.PUSH);
				Btn.setText("Edit Raw file");
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
				Btn.setLayoutData(gd);

				Btn = new Button(ParentComp, SWT.PUSH);
				Btn.setText("Delete file");
				Btn.setLayoutData(gd);
				Btn.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent arg0) {
						DoDeleteSelectedFile();
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
				Btn.setText("Undelete file");
				Btn.setLayoutData(gd);
				Btn.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent arg0) {
						DoUndeleteFile();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent arg0) {
						widgetSelected(arg0);
					}
				});
			}
			label("", 1);
			ParentComp.pack();
		}
	}

	/**
	 * Undelete the currently selected file(s)
	 */
	protected void DoUndeleteFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			try {
				for (TableItem en : itms) {
					CPMDirectoryEntry ent = (CPMDirectoryEntry) en.getData();
					if (ent.IsComplete()) {
						ent.SetDeleted(false);
					} else {
						MessageBox messageBox = new MessageBox(ParentComp.getShell(),
								SWT.ICON_QUESTION | SWT.YES | SWT.NO);
						messageBox.setMessage(
								"The file " + ent.GetFilename() + "is incomplete and may not work. Un-delete?");
						messageBox.setText("Incomplete file");
						if (messageBox.open() == SWT.YES) {
							ent.SetDeleted(false);
						}
					}
				}
				((CPMPartition) partition).updateDirentBlocks();
				((CPMPartition) partition).ExtractDirectoryListing();
				AddComponents();
				PopulateDirectory();
			} catch (IOException e) {
				ErrorBox("Error reading partition: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Populate the directory listing.
	 */
	private void PopulateDirectory() {
		if (!DirectoryListing.isDisposed()) {
			DirectoryListing.removeAll();

			PLUS3DOSPartition pdp = (PLUS3DOSPartition) partition;
			for (CPMDirectoryEntry entry : pdp.DirectoryEntries) {
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				item2.setData(entry);
				String content[] = new String[5];
				content[0] = entry.GetFilename();
				Plus3DosFileHeader pfdh = entry.GetPlus3DosHeader();
				if (pfdh.IsPlus3DosFile()) {
					content[1] = pfdh.getTypeDesc();
					content[3] = String.valueOf(pfdh.GetDOSFileSize() - 0x80);
				} else {
					content[1] = "CPM/Invalid +3 Header";
				}
				content[2] = String.valueOf(entry.GetRawFileSize());
				String s = "";
				if (entry.IsDeleted) {
					s = s + ",Deleted";
				}
				if (!entry.IsComplete()) {
					s = s + ",Incomplete";
				} else {
					s = s + ",Complete";
				}
				if (!s.isEmpty()) {
					s = s.substring(1);
				}
				content[4] = s;
				item2.setText(content);
			}
		}

	}

	/**
	 * The EDIT FILE button has been pressed.
	 */
	private void DoEditFile() {
		boolean DoAgain = true;

		while (DoAgain == true) {
			DoAgain = false;
			TableItem itms[] = DirectoryListing.getSelection();
			if ((itms != null) && (itms.length != 0)) {
				CPMDirectoryEntry entry = (CPMDirectoryEntry) itms[0].getData();
				String filename = entry.GetFilename();
				try {
					SpecFileEditDialog = new Plus3DosFileEditDialog(ParentComp.getDisplay(), fsd, partition);

					byte[] data = entry.GetFileRawData();

					if (SpecFileEditDialog.Show(data, "Editing " + entry.GetFilename(), entry)) {
						entry.SetDeleted(true);
						((PLUS3DOSPartition) partition).AddCPMFile(entry.GetFilename(), SpecFileEditDialog.data);
						// refresh the screen.
						AddComponents();
					} else {
						// There are two cases for SHOW returning false,
						// 1: Just closed, no changes
						// 2: File type change
						if (SpecFileEditDialog != null) {
							if (SpecFileEditDialog.FileTypeHasChanged) {
								Plus3DosFileHeader p3d = entry.GetPlus3DosHeader();
								if (p3d != null && p3d.IsPlus3DosFile()) {
									System.out.print("File type: " + p3d.GetFileType() + "("
											+ Speccy.SpecFileTypeToString(p3d.GetFileType()) + ") -> ");
									p3d.SetFileType(SpecFileEditDialog.NewFileType);
									System.out.println(p3d.GetFileType() + "("
											+ Speccy.SpecFileTypeToString(p3d.GetFileType()) + ")");

									PLUS3DOSPartition p3dPart = (PLUS3DOSPartition) partition;
									try {
										entry.SetDeleted(true);
										byte rawdata[] = entry.GetFileRawData();
										System.arraycopy(p3d.RawHeader, 0, rawdata, 0, 0x80);
										p3dPart.AddCPMFile(filename, rawdata);
										DoAgain = true;
									} catch (IOException e) {
										e.printStackTrace();
									}
								} else {
									System.err.println("Update ignored, No +3DOS Basic header to update.");
								}
							}
						}
					}
					PopulateDirectory();
					SpecFileEditDialog = null;
					// Re-select the current file.
					int index = 0;
					for (TableItem ti : DirectoryListing.getItems()) {
						entry = (CPMDirectoryEntry) ti.getData();
						if (entry.GetFilename().trim().equals(filename.trim())) {
							DirectoryListing.select(index);
						}
						index++;
					}
				} catch (IOException e) {
					ErrorBox("Error reading partition: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Add files pressed
	 */
	private void DoAddFiles() {
		AddFilesDialog = new AddFilesToPlus3Partition(ParentComp.getDisplay(), fsd);
		AddFilesDialog.Show("Add files", (PLUS3DOSPartition) partition);
		AddFilesDialog = null;
		if (!ParentComp.isDisposed()) {
			AddComponents();
		}
	}

	/**
	 * Edit raw file button pressed
	 */
	protected void DoEditRawFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			CPMDirectoryEntry entry = (CPMDirectoryEntry) itms[0].getData();
			try {
				// Create the hex edit dialog and start it.
				HxEditDialog = new HexEditDialog(ParentComp.getDisplay());

				byte data[] = entry.GetFileData();

				AddressNote NewAddressNote = new AddressNote(0, data.length, 0, "File: " + entry.GetFilename());
				AddressNote ANArray[] = { NewAddressNote };

				boolean WriteBackData = HxEditDialog.Show(data, "Editing " + entry.GetFilename(), ANArray, fsd);
				if (WriteBackData) {
					entry.SetDeleted(true);
					((PLUS3DOSPartition) partition).AddCPMFile(entry.GetFilename(), HxEditDialog.Data);
					// refresh the screen.
					AddComponents();
				}
				HxEditDialog = null;
			} catch (IOException e) {
				ErrorBox("Error reading File: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Delete selected file button pressed
	 */
	protected void DoDeleteSelectedFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			CPMDirectoryEntry entry = (CPMDirectoryEntry) itms[0].getData();
			try {
				String filename = entry.GetFilename();
				if (itms.length > 1) {
					filename = "the selected files";
				}
				MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
				messageBox.setMessage("Are you sure you want to delete " + filename + " ?");
				messageBox.setText("Are you sure you want to delete " + filename + " ?");
				if (messageBox.open() == SWT.OK) {
					// for +3dos files, deleting deleted files causes a reload of the dirents.
					// This means the existing ones are invalidated. so we set a delayed reload and
					// do it at the end.
					if (DirectoryListing != null && !DirectoryListing.isDisposed()) {
						itms = DirectoryListing.getSelection();
						if (itms != null) {
							for (TableItem en : itms) {
								CPMDirectoryEntry ent = (CPMDirectoryEntry) en.getData();
								if (ent != null) {
									ent.DelayReload = true;
									ent.SetDeleted(true);
									ent.DelayReload = false;
								}
							}
						}
					}
					((CPMPartition) partition).updateDirentBlocks();
					((CPMPartition) partition).ExtractDirectoryListing();
					AddComponents();
				}
				PopulateDirectory();
			} catch (IOException e) {
				ErrorBox("Error reading partition: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Show the "Extract all files" form.
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
	 * Rename the current file.
	 */
	protected void DoRenameFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			CPMDirectoryEntry entry = (CPMDirectoryEntry) itms[0].getData();
			RenFileDialog = new RenameFileDialog(ParentComp.getDisplay());
			if (RenFileDialog.Show(entry.GetFilename())) {
				try {
					if (RenFileDialog != null)
						entry.SetFilename(RenFileDialog.NewName);
					// refresh the screen.
					AddComponents();
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
		if (DropFilesDialog != null) {
			DropFilesDialog.close();
		}
	}

	/**
	 * Handle the drag/drop of files on to this form.
	 * 
	 * @param data
	 */
	protected void DoDropFile(String[] data) {
		File fFiles[] = new File[data.length];
		int i = 0;
		for (String file : data) {
			try {
				URI uri = new URI(file);
				file = uri.getPath();
			} catch (URISyntaxException e) {
				System.out.println("Cannot parse " + file);
			}
			System.out.println(file);
			fFiles[i++] = new File(file);
		}

		DropFilestoPlus3Partition DropFilesDialog = new DropFilestoPlus3Partition(ParentComp.getDisplay());
		DropFilesDialog.Show("Add files", (PLUS3DOSPartition) partition, fFiles);
		DropFilesDialog = null;
		if (!ParentComp.isDisposed()) {
			AddComponents();
		}
	}

}

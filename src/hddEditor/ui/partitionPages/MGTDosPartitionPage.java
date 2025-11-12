package hddEditor.ui.partitionPages;
/*
 * MGT technical information
 * https://k1.spdns.de/Vintage/Sinclair/82/Peripherals/Disc%20Interfaces/DiSCiPLE%20%26%20Plus%20D%20(MGT%2C%20Datel)/disciple-tech_v8.pdf
 */

import java.io.File;
//TODO: Support more MGT file types:
/*
  o ZX microdrive (6)
  o Opentype (10)
  o UNIDOS Subdirectory (12)
  o UNIDOS create?(13)
  o SAM types 16-19, 22,23
  o Sam type 20 (screen) only partially supported.
  o Masterdos subdirectory (21)
  o EDOS (23-26)
  o HDOS (28-31)
  o MGT - Support defrag
*/
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
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
import hddEditor.libs.MGT;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.MGTDosPartition;
import hddEditor.libs.partitions.mgt.MGTDirectoryEntry;
import hddEditor.ui.FileExportAllPartitionsForm;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.dialogs.AddressNote;
import hddEditor.ui.partitionPages.dialogs.HexEditDialog;
import hddEditor.ui.partitionPages.dialogs.RenameFileDialog;
import hddEditor.ui.partitionPages.dialogs.AddFiles.AddFilesToMGTPartition;
import hddEditor.ui.partitionPages.dialogs.drop.DropFilesToTapePartition;
import hddEditor.ui.partitionPages.dialogs.edit.MGTDosFileEditDialog;

public class MGTDosPartitionPage extends GenericPage {
	Table DirectoryListing = null;
	MGTDosFileEditDialog SpecFileEditDialog = null;
	AddFilesToMGTPartition AddFilesDialog = null;

	RenameFileDialog RenFileDialog = null;
	HexEditDialog HxEditDialog = null;

	public MGTDosPartitionPage(HDDEditor root, Composite parent, IDEDosPartition partition, FileSelectDialog filesel, Languages lang) {
		super(root, parent, partition, filesel,lang);
		AddComponents();
	}

	private void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			super.AddBasicDetails();

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
			tc4.setText(lang.Msg(Languages.MSG_LENREPORTED));
			tc5.setText(lang.Msg(Languages.MSG_LEGSECTORS));
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
					MGTDosPartition p = (MGTDosPartition) partition;
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
											sd.LoadAddress, sd.VarName + "", actiontype, lang, ParentComp.getDisplay());
								} else if (exporttype == HDDEditor.DRAG_RAW) {
									GeneralUtils.WriteBlockToDisk(entry.GetFileData(), f);
								} else {
									SpeccyBasicDetails sd = entry.GetSpeccyBasicDetails();
									Speccy.SaveFileToDiskAdvanced(f, entry.GetFileData(), entry.GetFileData(),
											entry.GetFileData().length, sd.BasicType, sd.LineStart, sd.VarStart,
											sd.LoadAddress, sd.VarName + "", GeneralUtils.EXPORT_TYPE_HEX, lang, ParentComp.getDisplay());
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
					DoFileProperties();
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

			ParentComp.getShell().pack();
			((ScrolledComposite) ParentComp.getParent())
					.setMinSize(ParentComp.computeSize(ParentComp.getParent().getClientArea().width + 1, SWT.DEFAULT));
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

	protected void DoRenameFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			MGTDirectoryEntry entry = (MGTDirectoryEntry) itms[0].getData();
			RenFileDialog = new RenameFileDialog(ParentComp.getDisplay(), lang);
			if (RenFileDialog.Show(entry.GetFilename())) {
				try {
					MGTDosPartition fbc = (MGTDosPartition) partition;
					fbc.RenameFile(entry.GetFilename(), RenFileDialog.NewName);

					// refresh the screen.
					UpdateDirectoryEntryList();
				} catch (IOException e) {
					MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_ERROR | SWT.CLOSE);
					String s = String.format(lang.Msg(Languages.MSG_ERRORRENAME), entry.GetFilename());
					messageBox.setMessage(s + ": " + e.getMessage());
					messageBox.setText(s + ": " + e.getMessage());
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

	protected void DoAddFiles() {
		AddFilesDialog = new AddFilesToMGTPartition(ParentComp.getDisplay(), fsd, lang);
		AddFilesDialog.Show(lang.Msg(Languages.MSG_ADDGFILES) , (MGTDosPartition) partition);
		AddFilesDialog = null;
		if (!ParentComp.isDisposed()) {
			AddComponents();
		}
	}

	protected void DoDeleteFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			MGTDirectoryEntry entry = (MGTDirectoryEntry) itms[0].getData();
			String filename = entry.GetFilename();
			if (itms.length > 1) {
				filename = lang.Msg(Languages.MSG_THESELECTEDFILES);
			}
			MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
			String s = String.format(lang.Msg(Languages.MSG_AREYOUSUREDEL), filename);
			messageBox.setMessage(s);
			messageBox.setText(s);

			int response = messageBox.open();
			if (response == SWT.YES) {
				MGTDosPartition fbc = (MGTDosPartition) partition;
				try {
					for (TableItem itm : itms) {
						entry = (MGTDirectoryEntry) itm.getData();
						String fn = entry.GetFilename();
						if (entry.GetFileType() != ' ') {
							filename = filename + "." + entry.GetFileType();
						}
						fbc.DeleteFile(fn);
					}
					UpdateDirectoryEntryList();
				} catch (IOException e) {
					ErrorBox(lang.Msg(Languages.MSG_ERRIODEL) + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	protected void DoEditRawFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			MGTDirectoryEntry entry = (MGTDirectoryEntry) itms[0].getData();
			// Create the hex edit dialog and start it.
			HxEditDialog = new HexEditDialog(ParentComp.getDisplay(), lang);

			byte data[];
			try {
				data = entry.GetFileData();

				AddressNote NewAddressNote = new AddressNote(0, data.length, 0, "File: " + entry.GetFilename());
				AddressNote ANArray[] = { NewAddressNote };

				boolean WriteBackData = HxEditDialog.Show(data,String.format(lang.Msg(Languages.MSG_EDITINGX),entry.GetFilename()), ANArray, fsd);
				if (WriteBackData) {
					MGTDosPartition mbc = (MGTDosPartition) partition;
					mbc.UpdateFile(entry, data);
				}
				UpdateDirectoryEntryList();

			} catch (IOException e) {
				ErrorBox(lang.Msg(Languages.MSG_ERROREDITING)+": " + e.getMessage());
				e.printStackTrace();
			}
			HxEditDialog = null;
		}
	}

	/*
	 * File Properties
	 */
	protected void DoFileProperties() {
		boolean DoAgain = true;

		while (DoAgain == true) {
			DoAgain = false;

			TableItem itms[] = DirectoryListing.getSelection();
			if ((itms != null) && (itms.length != 0)) {
				MGTDirectoryEntry entry = (MGTDirectoryEntry) itms[0].getData();
				SpecFileEditDialog = new MGTDosFileEditDialog(ParentComp.getDisplay(), fsd, partition, lang);

				byte[] data;
				try {
					data = entry.GetFileData();
					if (SpecFileEditDialog.Show(data,String.format(lang.Msg(Languages.MSG_EDITINGX),entry.GetFilename()), entry)) {
						// entry.SetDeleted(true);

						// refresh the screen.
						AddComponents();
					} else {
						// There are two cases for SHOW returning false,
						// 1: Just closed, no changes
						// 2: File type change
						if (SpecFileEditDialog.FileTypeHasChanged) {
							MGTDosPartition part = (MGTDosPartition) partition;
							entry.SetFileType(SpecFileEditDialog.NewFileType);
							part.SaveDirectoryEntry(entry);
						} 
						SpecFileEditDialog = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				UpdateDirectoryEntryList();
			}
		}
	}

	/**
	 * Update the directory listing
	 */
	private void UpdateDirectoryEntryList() {
		if (!DirectoryListing.isDisposed()) {
			DirectoryListing.removeAll();
			MGTDosPartition mdp = (MGTDosPartition) partition;
			for (MGTDirectoryEntry entry : mdp.DirectoryEntries) {
				if (entry.GetFileType() != MGT.MGTFT_ERASED) {
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String content[] = new String[5];
					content[0] = entry.GetFilename();
					content[1] = entry.GetFileType() + " (" + entry.GetFileTypeString() + ")";
					content[2] = "Ch:" + entry.GetStartTrack() + " S:" + entry.GetStartSector();
					content[3] = String.valueOf(entry.GetFileSize());
					content[4] = String.valueOf(entry.GetRawFileSize());
					item2.setText(content);
					item2.setData(entry);
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

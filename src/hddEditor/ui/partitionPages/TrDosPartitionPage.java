package hddEditor.ui.partitionPages;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.TrDosPartition;
import hddEditor.libs.partitions.trdos.TrdDirectoryEntry;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.dialogs.AddFilesToTrDosPartition;
import hddEditor.ui.partitionPages.dialogs.AddressNote;
import hddEditor.ui.partitionPages.dialogs.HexEditDialog;
import hddEditor.ui.partitionPages.dialogs.RenameFileDialog;
import hddEditor.ui.partitionPages.dialogs.TrDosFileEditDialog;

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
	public TrDosPartitionPage(HDDEditor root, Composite parent, IDEDosPartition partition) {
		super(root, parent, partition);
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
			DirectoryListing = new Table(ParentComp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
			DirectoryListing.setLinesVisible(true);

			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 4;
			gd.heightHint = 400;
			DirectoryListing.setLayoutData(gd);

			TableColumn tc1 = new TableColumn(DirectoryListing, SWT.LEFT);
			TableColumn tc2 = new TableColumn(DirectoryListing, SWT.LEFT);
			TableColumn tc3 = new TableColumn(DirectoryListing, SWT.LEFT);
			TableColumn tc4 = new TableColumn(DirectoryListing, SWT.LEFT);
			TableColumn tc5 = new TableColumn(DirectoryListing, SWT.LEFT);
			tc1.setText("Fileame");
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
			Btn.setText("Extract all Files (raw)");
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
		AddFilesDialog = new AddFilesToTrDosPartition(ParentComp.getDisplay());
		AddFilesDialog.Show("Add files", (TrDosPartition) partition);
		UpdateDirectoryEntryList();
		AddFilesDialog = null;
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

	/**
	 * 
	 */
	protected void DoExtractAllFiles() {
		DirectoryDialog dialog = new DirectoryDialog(ParentComp.getShell());
		dialog.setText("Select folder for export to");
		String result = dialog.open();
		if (result != null) {
			File directory = new File(result);
			for (TableItem t : DirectoryListing.getItems()) {
				TrdDirectoryEntry entry = (TrdDirectoryEntry) t.getData();
				String filename = entry.GetFilename().trim();
				switch (entry.GetFileType()) {
				case ('B'):
					filename = filename + ".basic";
					break;
				case ('C'):
					filename = filename + ".code";
					break;
				case ('#'):
					filename = filename + ".stream";
					break;
				case ('D'):
					if (entry.IsCharArray()) {
						filename = filename + ".cdata";
					} else {
						filename = filename + ".ndata";
					}
					break;
				}
				try {
					File TargetFilename = new File(directory, filename);
					byte file[] = entry.GetFileData();
					OutputStream outputStream = new FileOutputStream(TargetFilename);
					try {
						outputStream.write(file);
					} finally {
						outputStream.close();
					}
					System.out.println("Written " + filename);
				} catch (IOException e) {
					System.out.println("Error extracting " + entry.GetFilename() + ": " + e.getMessage());
					e.printStackTrace();
				}
			}
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
				content[1] = entry.GetFileType() + " (" + entry.GetFileTypeName() + ")";
				content[2] = "Ch:" + entry.GetStartTrack() + " S:" + entry.GetStartSector();
				content[3] = String.valueOf(entry.GetFileLength());
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
			MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			messageBox.setMessage("Are you sure you want to delete '" + entry.GetFilename() + "'?");
			messageBox.setText("Are you sure you want to delete '" + entry.GetFilename() + "'?");
			int response = messageBox.open();
			if (response == SWT.YES) {
				TrDosPartition fbc = (TrDosPartition) partition;
				try {
					fbc.DeleteFile(entry.GetFilename(), entry.GetFileType());
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

				boolean WriteBackData = HxEditDialog.Show(data, "Editing " + entry.GetFilename(), ANArray);
				if (WriteBackData) {
					TrDosPartition fbc = (TrDosPartition) partition;
					fbc.UpdateFile(entry, data);
				}
			} catch (IOException e) {
				ErrorBox("Error editing partition: " + e.getMessage());
				e.printStackTrace();
			}

			HxEditDialog = null;

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
				SpecFileEditDialog = new TrDosFileEditDialog(ParentComp.getDisplay());

				byte[] data = entry.GetFileData();
				if (SpecFileEditDialog.Show(data, "Editing " + entry.GetFilename(), entry)) {
					entry.SetDeleted(true);
					// refresh the screen.
					AddComponents();
				}
				SpecFileEditDialog = null;
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

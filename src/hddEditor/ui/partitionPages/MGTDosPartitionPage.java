package hddEditor.ui.partitionPages;
/*
 * MGT technical information
 * https://k1.spdns.de/Vintage/Sinclair/82/Peripherals/Disc%20Interfaces/DiSCiPLE%20%26%20Plus%20D%20(MGT%2C%20Datel)/disciple-tech_v8.pdf
 */
//TODO: Support more MGT file types:

//TODO:   ZX microdrive (6)
//TODO:   ZX 128K snapshot (9)
//TODO:   Opentype (10)
//TODO:   ZX Execute (11)
//TODO:   UNIDOS Subdirectory (12)
//TODO:   UNIDOS create?(13)
//TODO:   SAM types 16-20, 22,23
//TODO:   Masterdos subdirectory (21)
//TODO:   EDOS (23-26)
//TODO:   HDOS (28-31)
//TODO: MGT - Support defrag

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.libs.MGT;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.MGTDosPartition;
import hddEditor.libs.partitions.TrDosPartition;
import hddEditor.libs.partitions.mgt.MGTDirectoryEntry;
import hddEditor.ui.FileExportAllPartitionsForm;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.dialogs.AddFilesToMGTPartition;
import hddEditor.ui.partitionPages.dialogs.AddressNote;
import hddEditor.ui.partitionPages.dialogs.HexEditDialog;
import hddEditor.ui.partitionPages.dialogs.MGTDosFileEditDialog;
import hddEditor.ui.partitionPages.dialogs.RenameFileDialog;

public class MGTDosPartitionPage extends GenericPage {
	Table DirectoryListing = null;
	MGTDosFileEditDialog SpecFileEditDialog = null;
	AddFilesToMGTPartition AddFilesDialog = null; 

	RenameFileDialog RenFileDialog = null;
	HexEditDialog HxEditDialog = null;

	public MGTDosPartitionPage(HDDEditor root, Composite parent, IDEDosPartition partition) {
		super(root, parent, partition);
		AddComponents();
	}

	private void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			super.AddBasicDetails();

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
			tc4.setText("Length (reported)");
			tc5.setText("Length (Sectors)");
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
					DoFileProperties();
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
					DoDefragDisk();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
			ParentComp.getShell().pack();
			((ScrolledComposite)ParentComp.getParent()).setMinSize(ParentComp.computeSize(ParentComp.getParent().getClientArea().width+1, SWT.DEFAULT));
		}

	}

	protected void DoDefragDisk() {
		// TODO Defrag disk

	}

	protected void DoRenameFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			MGTDirectoryEntry entry = (MGTDirectoryEntry) itms[0].getData();
			RenFileDialog = new RenameFileDialog(ParentComp.getDisplay());
			if (RenFileDialog.Show(entry.GetFilename())) {
				try {
					MGTDosPartition fbc = (MGTDosPartition) partition;
					fbc.RenameFile(entry.GetFilename(), RenFileDialog.NewName);

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

	protected void DoAddFiles() {		
		AddFilesDialog = new AddFilesToMGTPartition(ParentComp.getDisplay());
		AddFilesDialog.Show("Add files", (MGTDosPartition) partition);
		AddFilesDialog = null;
		if (!ParentComp.isDisposed()) {
			AddComponents();
		}
	}

	protected void DoDeleteFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			MGTDirectoryEntry entry = (MGTDirectoryEntry) itms[0].getData();
			MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			messageBox.setMessage("Are you sure you want to delete '" + entry.GetFilename() + "'?");
			messageBox.setText("Are you sure you want to delete '" + entry.GetFilename() + "'?");
			int response = messageBox.open();
			if (response == SWT.YES) {
				TrDosPartition fbc = (TrDosPartition) partition;
				try {
					String filename = entry.GetFilename();
					if (entry.GetFileType() != ' ') {
						filename = filename + "." + entry.GetFileType();
					}
					fbc.DeleteFile(filename);
					UpdateDirectoryEntryList();
				} catch (IOException e) {
					ErrorBox("IO Error deleting file." + e.getMessage());
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
			HxEditDialog = new HexEditDialog(ParentComp.getDisplay());

			byte data[];
			try {
				data = entry.GetFileData();

				AddressNote NewAddressNote = new AddressNote(0, data.length, 0, "File: " + entry.GetFilename());
				AddressNote ANArray[] = { NewAddressNote };

				boolean WriteBackData = HxEditDialog.Show(data, "Editing " + entry.GetFilename(), ANArray);
				if (WriteBackData) {
					MGTDosPartition mbc = (MGTDosPartition) partition;
					mbc.UpdateFile(entry, data);
				}
			} catch (IOException e) {
				ErrorBox("Error editing partition: " + e.getMessage());
				e.printStackTrace();
			}
			HxEditDialog = null;
		}
	}

	protected void DoFileProperties() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			MGTDirectoryEntry entry = (MGTDirectoryEntry) itms[0].getData();
			try {
				SpecFileEditDialog = new MGTDosFileEditDialog(ParentComp.getDisplay());

				byte[] data = entry.GetFileData();
				if (SpecFileEditDialog.Show(data, "Editing " + entry.GetFilename(), entry)) {
					// entry.SetDeleted(true);

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

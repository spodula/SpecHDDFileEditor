package hddEditor.ui.partitionPages;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.libs.disks.LINEAR.MDFMicrodriveFile;
import hddEditor.libs.disks.LINEAR.MicrodriveSector;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.SinclairMicrodrivePartition;
import hddEditor.libs.partitions.mdf.MicrodriveDirectoryEntry;
import hddEditor.ui.FileExportAllPartitionsForm;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.dialogs.AddFilesToMDRPartition;
import hddEditor.ui.partitionPages.dialogs.AddressNote;
import hddEditor.ui.partitionPages.dialogs.HexEditDialog;
import hddEditor.ui.partitionPages.dialogs.MicrodriveFileEditDialog;
import hddEditor.ui.partitionPages.dialogs.RenameFileDialog;

public class MicrodrivePartitionPage extends GenericPage {
	Table DirectoryListing = null;
	HexEditDialog HxEditDialog = null;
	RenameFileDialog RenFileDialog = null;
	MicrodriveFileEditDialog SpecFileEditDialog = null;
	AddFilesToMDRPartition AddFilesDialog = null;

	/**
	 * 
	 * 
	 * @param root
	 * @param parent
	 * @param partition
	 */
	public MicrodrivePartitionPage(HDDEditor root, Composite parent, IDEDosPartition partition) {
		super(root, parent, partition);
		AddComponents();
	}

	/**
	 * 
	 */
	private void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			SinclairMicrodrivePartition smp = (SinclairMicrodrivePartition) partition;
			label("Sinclair Spectrum Microdrive cartridge", 4);
			label("Blocks: " + smp.CurrentDisk.GetNumSectors(), 1);
			label("Cart Name: " + smp.GetCartName(), 1);
			label("Free Blocks: " + smp.NumFreeSectors(), 1);
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
			tc3.setText("Length");
			tc4.setText("Sectors");
			tc5.setText("Notes");
			tc1.setWidth(150);
			tc2.setWidth(150);
			tc3.setWidth(150);
			tc4.setWidth(50);
			tc5.setWidth(50);
			DirectoryListing.setHeaderVisible(true);

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
			Btn.setText("Pack Cartridge");
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoPackCart();
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
	 * Update the directory listing
	 */
	private void UpdateDirectoryEntryList() {
		if (!DirectoryListing.isDisposed()) {
			DirectoryListing.removeAll();
			SinclairMicrodrivePartition smp = (SinclairMicrodrivePartition) partition;
			for (MicrodriveDirectoryEntry entry : smp.Files) {
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String content[] = new String[5];
				content[0] = entry.GetFilename();
				content[1] = entry.GetFiletype() + " (" + entry.GetFileTypeName() + ")";
				content[2] = String.valueOf(entry.GetFileSize());
				content[3] = String.valueOf(entry.sectors.length);

				int FileHBad = 0;
				int SectorHBad = 0;
				int DataBad = 0;

				for (MicrodriveSector Sector : entry.sectors) {
					if (!Sector.IsFileChecksumValid())
						DataBad++;
					if (!Sector.IsSectorChecksumValid())
						SectorHBad++;
					if (!Sector.IsHeaderChecksumValid())
						FileHBad++;
				}

				String notes = "";

				if (FileHBad > 0)
					notes = notes + " bad file headers: " + FileHBad;
				if (SectorHBad > 0)
					notes = notes + " bad sector headers: " + SectorHBad;
				if (DataBad > 0)
					notes = notes + " bad DataBad headers: " + DataBad;

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
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {

			MicrodriveDirectoryEntry entry = (MicrodriveDirectoryEntry) itms[0].getData();
			SpecFileEditDialog = new MicrodriveFileEditDialog(ParentComp.getDisplay());

			byte data[] = entry.GetFileRawData();
			byte newdata[] = new byte[data.length - 0x09];
			System.arraycopy(data, 0x09, newdata, 0, newdata.length);

			if (SpecFileEditDialog.Show(newdata, "Editing " + entry.GetFilename(), entry)) {
				byte NewRawData[] = new byte[newdata.length + 0x09];
				System.arraycopy(data, 0, NewRawData, 0, 0x09);
				System.arraycopy(newdata, 0, NewRawData, 0x09, newdata.length);
				NewRawData[1] = (byte) ((newdata.length % 0x100) & 0xff);
				NewRawData[2] = (byte) ((newdata.length / 0x100) & 0xff);
				try {
					entry.SetFileRawData(NewRawData, (MDFMicrodriveFile) partition.CurrentDisk);
				} catch (IOException e) {
					MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_ERROR | SWT.CLOSE);
					messageBox.setMessage("Error Writing back file: " + entry.GetFilename() + ": " + e.getMessage());
					messageBox.setText("Error Writing back file: " + entry.GetFilename() + ": " + e.getMessage());
					messageBox.open();
					e.printStackTrace();
				}
				// refresh the screen.
				AddComponents();
			}
			SpecFileEditDialog = null;
		}
	}

	/**
	 * Edit the raw file.
	 */
	protected void DoEditRawFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			MicrodriveDirectoryEntry entry = (MicrodriveDirectoryEntry) itms[0].getData();
			// Create the hex edit dialog and start it.
			HxEditDialog = new HexEditDialog(ParentComp.getDisplay());

			byte data[];
			data = entry.GetFileRawData();

			AddressNote NewAddressNote = new AddressNote(0, data.length, 0, "File: " + entry.GetFilename());
			AddressNote ANArray[] = { NewAddressNote };

			boolean WriteBackData = HxEditDialog.Show(data, "Editing " + entry.GetFilename(), ANArray);
			if (WriteBackData) {
				try {
					entry.SetFileRawData(HxEditDialog.Data, (MDFMicrodriveFile) partition.CurrentDisk);
				} catch (IOException e) {
					MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_ERROR | SWT.CLOSE);
					messageBox.setMessage("Error Writing back file: " + entry.GetFilename() + ": " + e.getMessage());
					messageBox.setText("Error Writing back file: " + entry.GetFilename() + ": " + e.getMessage());
					messageBox.open();
					e.printStackTrace();
				}
			}
			HxEditDialog = null;
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
			MicrodriveDirectoryEntry entry = (MicrodriveDirectoryEntry) itms[0].getData();
			RenFileDialog = new RenameFileDialog(ParentComp.getDisplay());
			if (RenFileDialog.Show(entry.GetFilename())) {
				try {
					SinclairMicrodrivePartition smp = (SinclairMicrodrivePartition) partition;
					smp.RenameFile(entry.GetFilename(), RenFileDialog.NewName);

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
	 * Pack the current microdrive cart.
	 * 
	 */
	protected void DoPackCart() {
		SinclairMicrodrivePartition smp = (SinclairMicrodrivePartition) partition;
		try {
			smp.Pack();
		} catch (IOException e) {
			MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_ERROR | SWT.CLOSE);
			messageBox.setMessage("Error packing microdrive: " + e.getMessage());
			messageBox.setText("Error packing microdrive: " + e.getMessage());
			messageBox.open();
			e.printStackTrace();
		}
		UpdateDirectoryEntryList();
	}

	/**
	 * Extract all files on this cartridge to a given folder
	 * 
	 */
	protected void DoExtractAllFiles() {
		FileExportAllPartitionsForm ExportAllPartsForm = new FileExportAllPartitionsForm (ParentComp.getDisplay()); 
		try {
			ExportAllPartsForm.ShowSinglePartition(partition);
		} finally {
			ExportAllPartsForm = null;
		}
	}


	/**
	 * Delete the selected file.
	 * 
	 */
	protected void DoDeleteFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			MicrodriveDirectoryEntry entry = (MicrodriveDirectoryEntry) itms[0].getData();
			try {
				MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
				messageBox.setMessage("Are you sure you want to delete " + entry.GetFilename() + " ?");
				messageBox.setText("Are you sure you want to delete " + entry.GetFilename() + " ?");
				if (messageBox.open() == SWT.OK) {
					SinclairMicrodrivePartition smp = (SinclairMicrodrivePartition) partition;
					smp.DeleteMicrodriveFile(entry.GetFilename());
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
		AddFilesDialog = new AddFilesToMDRPartition(ParentComp.getDisplay());
		AddFilesDialog.Show("Add files", (SinclairMicrodrivePartition) partition);
		AddFilesDialog = null;
		if (!ParentComp.isDisposed()) {
			AddComponents();
		}
	}

}

package hddEditor.ui.partitionPages;
/**
 * implementation of the +3DOS partition specific partition pages 
 */


import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.PLUS3DOSPartition;
import hddEditor.libs.partitions.cpm.DirectoryEntry;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.dialogs.AddFilesToPlus3Partition;
import hddEditor.ui.partitionPages.dialogs.AddressNote;
import hddEditor.ui.partitionPages.dialogs.HexEditDialog;
import hddEditor.ui.partitionPages.dialogs.RenameFileDialog;
import hddEditor.ui.partitionPages.dialogs.SpectrumFileEditDialog;

public class PlusThreePartPage extends GenericPage {
	Table DirectoryListing;
	
	/*
	 * Dialog pointers so we can forcibly close them if necessary.
	 */
	SpectrumFileEditDialog SpecFileEditDialog = null;
	RenameFileDialog RenFileDialog = null;
	HexEditDialog HxEditDialog = null;
	AddFilesToPlus3Partition AddFilesDialog = null;
	
	/**
	 * 
	 * @param root
	 * @param parent
	 * @param partition
	 */
	public PlusThreePartPage(HDDEditor root, Composite parent, IDEDosPartition partition) {
		super(root, parent, partition);
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
			tc3.setText("CPM Length");
			tc4.setText("+3 Length");
			tc5.setText("Flags");
			tc1.setWidth(150);
			tc2.setWidth(150);
			tc3.setWidth(150);
			tc4.setWidth(150);
			tc5.setWidth(100);
			DirectoryListing.setHeaderVisible(true);

			for (DirectoryEntry entry : pdp.DirectoryEntries) {
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				item2.setData(entry);
				String content[] = new String[5];
				content[0] = entry.filename();
				Plus3DosFileHeader pfdh = entry.GetPlus3DosHeader();
				if (pfdh.IsPlusThreeDosFile) {
					content[1] = pfdh.getTypeDesc();
					content[3] = String.valueOf(pfdh.fileSize - 0x80);
				} else {
					content[1] = "CPM/Invalid +3 Header";
				}
				content[2] = String.valueOf(entry.GetFileSize());
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
			Btn.setText("Extract all Files (raw)");
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoExtractAllFilesRaw();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
			Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText("Extract all Files (Code=asm)");
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoExtractAllFilesAsm();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
			Btn = new Button(ParentComp, SWT.PUSH);
			Btn.setText("Extract all Files (code=hex)");
			Btn.setLayoutData(gd);
			Btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoExtractAllFilesHex();
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

			label("", 1);
			ParentComp.pack();
		}
	}



	/**
	 * The EDIT FILE button has been pressed.
	 */
	private void DoEditFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			DirectoryEntry entry = (DirectoryEntry) itms[0].getData();
			try {
				SpecFileEditDialog = new SpectrumFileEditDialog(ParentComp.getDisplay());

				byte[] data = entry.GetFileData();
				if (SpecFileEditDialog.Show(data, "Editing " + entry.filename(), entry)) {
					entry.SetDeleted(true);
					((PLUS3DOSPartition) partition).AddCPMFile(entry.filename(), SpecFileEditDialog.data);
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
	 * Add files pressed
	 */
	private void DoAddFiles() {
		AddFilesDialog = new AddFilesToPlus3Partition(ParentComp.getDisplay());
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
			DirectoryEntry entry = (DirectoryEntry) itms[0].getData();
			try {
				// Create the hex edit dialog and start it.
				HxEditDialog = new HexEditDialog(ParentComp.getDisplay());

				byte data[] = entry.GetFileData();

				AddressNote NewAddressNote = new AddressNote(0, data.length, 0, "File: " + entry.filename());
				AddressNote ANArray[] = { NewAddressNote };

				boolean WriteBackData = HxEditDialog.Show(data, "Editing " + entry.filename(), ANArray);
				if (WriteBackData) {
					entry.SetDeleted(true);
					((PLUS3DOSPartition) partition).AddCPMFile(entry.filename(), HxEditDialog.Data);
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
			DirectoryEntry entry = (DirectoryEntry) itms[0].getData();
			try {
				MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
				messageBox.setMessage("Are you sure you want to delete " + entry.filename() + " ?");
				messageBox.setText("Are you sure you want to delete " + entry.filename() + " ?");
				if (messageBox.open() == SWT.OK) {
					entry.SetDeleted(true);
					AddComponents();
				}
			} catch (IOException e) {
				ErrorBox("Error reading partition: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Extract all files in the current partition.
	 */
	protected void DoExtractAllFilesRaw() {
		DirectoryDialog dialog = new DirectoryDialog(ParentComp.getShell());
		dialog.setText("Select folder for export to");
		String result = dialog.open();
		if (result != null) {
			File directory = new File(result);
			try {
				partition.ExtractPartitiontoFolder(directory, true, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	

	protected void DoExtractAllFilesHex() {
		DirectoryDialog dialog = new DirectoryDialog(ParentComp.getShell());
		dialog.setText("Select folder for export to");
		String result = dialog.open();
		if (result != null) {
			File directory = new File(result);
			try {
				partition.ExtractPartitiontoFolder(directory, false, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected void DoExtractAllFilesAsm() {
		DirectoryDialog dialog = new DirectoryDialog(ParentComp.getShell());
		dialog.setText("Select folder for export to");
		String result = dialog.open();
		if (result != null) {
			File directory = new File(result);
			try {
				partition.ExtractPartitiontoFolder(directory, false, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Rename the current file.
	 */
	protected void DoRenameFile() {
		TableItem itms[] = DirectoryListing.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			DirectoryEntry entry = (DirectoryEntry) itms[0].getData();
			RenFileDialog = new RenameFileDialog(ParentComp.getDisplay());
			if (RenFileDialog.Show(entry.filename())) {
				try {
					entry.RenameTo(RenFileDialog.NewName);
					// refresh the screen.
					AddComponents();					
				} catch (IOException e) {
					MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_ERROR | SWT.CLOSE);
					messageBox.setMessage("Error Renaming " + entry.filename() + ": "+e.getMessage());
					messageBox.setText("Error Renaming " + entry.filename() + ": "+e.getMessage());
					messageBox.open();
					e.printStackTrace();
				}
			}
			RenFileDialog = null;
		}
	}

	/**
	 * Force any dialogs that are still open to close. 
	 * This avoids exceptions on exit. 
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

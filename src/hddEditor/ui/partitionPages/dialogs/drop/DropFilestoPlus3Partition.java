package hddEditor.ui.partitionPages.dialogs.drop;

/**
 * Extra bits required to drop to a +3DOS partition
 * Mostly additions to allow dropping +3DOS files with headers
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.CPM;
import hddEditor.libs.partitions.PLUS3DOSPartition;
import hddEditor.libs.partitions.cpm.CPMDirectoryEntry;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;

public class DropFilestoPlus3Partition extends GenericDropForm {

	public DropFilestoPlus3Partition(Display display) {
		super(display);
	}

	//New file type
	private final static int FILETYPE_PLUS3DOS = 10;

	/**
	 * Create a newFileListItem from the given file. 
	 * This is overridden to look for +3DOS headers.
	 * 
	 * @param f
	 * @return
	 */
	@Override
	protected NewFileListItem IdentifyFileType(File f) {
		NewFileListItem nfli = super.IdentifyFileType(f);
		nfli.fileheader = new Plus3DosFileHeader(nfli.data);
		if (nfli.fileheader.IsPlusThreeDosFile) {
			nfli.FileType = FILETYPE_PLUS3DOS;
		} else {
			nfli.fileheader = null;
		}
		return (nfli);
	}

	/**
	 * Overridden to handle files with +3DOS headers.
	 */
	@Override
	protected void FileTypeChanged() {
		if (details != null) {
			if (details.FileType == FILETYPE_PLUS3DOS) {
				/*
				 * Remove the old components
				 */
				for (Control child : MainPage.getChildren()) {
					child.dispose();
				}
				ImageLabel = null;
				MainPage.pack();
				/*
				 * Actually render the file
				 */
				RenderPlus3DosFile(details);
				MainPage.pack();
				shell.pack();
				EnableDisableModifiers();
				shell.layout(true, true);
				final Point newSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

				shell.setSize(newSize);
			} else {
				super.FileTypeChanged();
			}
		}
	}

	/**
	 * Enable or disable the appropriate buttons and sliders depending on the file
	 * type.
	 * Overridden for +3DOS header file type. (As you can't change any of the edit values
	 * if they are already defined in the header)
	 */
	@Override
	protected void EnableDisableModifiers() {
		if (details.FileType != FILETYPE_PLUS3DOS)  {
			super.EnableDisableModifiers();
		} else {
			intensitySlider.setEnabled(false);
			IsBWCheck.setEnabled(false);
			StartLine.setEnabled(false);
			StartAddress.setEnabled(false);
			SelectedFileType.setEnabled(false);
		}
	}



	/**
	 * Render a given file with a +3DOS header.
	 * 
	 * @param details
	 */
	private void RenderPlus3DosFile(NewFileListItem details) {
		String str = "Missing or invalid +3DOS header.";

		if (details != null) {
			if (details.fileheader == null) {
				Plus3DosFileHeader pfd = new Plus3DosFileHeader(details.data);
				if (pfd.IsPlusThreeDosFile) {
					details.fileheader = pfd;
				}
			}

			if (details.fileheader != null) {
				str = "Valid +3Dos Header: \n" + "File type: " + details.fileheader.getTypeDesc() + " ("
						+ details.fileheader.filetype + ")\n" + "+3DOS file length: " + details.fileheader.filelength
						+ "\n" + "Disk file length: " + details.fileheader.fileSize + "\n" + "Dos version: "
						+ details.fileheader.VersionNo + " Dos Issue:" + details.fileheader.IssueNo + "\n";
				switch (details.fileheader.filetype) {
				case 0:
					str = str + "Start line: " + details.fileheader.line + "\n";
					str = str + "Variables offset: " + details.fileheader.VariablesOffset + "\n";
					break;
				case 1:
					str = str + "Variable name: " + details.fileheader.VarName + "\n";
					break;
				case 2:
					str = str + "Variable name: " + details.fileheader.VarName + "\n";
					break;
				case 3:
					str = str + "Load address: " + details.fileheader.loadAddr + "\n";
					break;
				}

			}
		}
		Text t = new Text(MainPage, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 4;
		gd.verticalSpan = 5;
		gd.minimumHeight = 110;
		gd.minimumWidth = 500;
		t.setLayoutData(gd);
		t.setText(str);

		if (details.fileheader != null && details.fileheader.ChecksumValid) {
			switch (details.fileheader.filetype) {
			case 0:
				RenderBasic(details);
				break;
			case 1:
				RenderChrArray(details);
				break;
			case 2:
				RenderNumArray(details);
				break;
			case 3:
				if (details.fileheader.filelength == 6912) {
					RenderScreen(details);
				} else {
					RenderCode(details);
				}
				break;
			}
		}
	}

	/**
	 * Routine to get the file data from a file taking into account the
	 * +3DOS file type header for the appropriate type.
	 * 
	 * @param details
	 * @return
	 */
	@Override
	protected byte[] ExtractData(NewFileListItem details) {
		byte data[] = details.data;
		if (details.FileType == FILETYPE_PLUS3DOS) {
			data = new byte[6912];
			System.arraycopy(details.data, 0x80, data, 0x00, Math.min(6912, details.data.length - 128));
		}
		return (data);
	}


	/**
	 * Modify the given filename so its unique in the current selection. Note, this
	 * has a limitation that it will probably not work properly over >999 files, but
	 * that is more than the default number of dirents (511), so *should* be ok.
	 * 
	 * This is the +3DOS version.
	 * 
	 * @param s
	 * @return
	 */
	@Override
	protected String UniqueifyName(String s) {
		String result = CPM.FixFullName(s);
		PLUS3DOSPartition pfd = (PLUS3DOSPartition) CurrentPartition;

		/*
		 * Extract the filename and default extension from the file.
		 */
		String filename = "";
		String extension = "";
		if (result.contains(".")) {
			int i = result.lastIndexOf(".");
			extension = result.substring(i + 1);
			filename = result.substring(0, i);
		} else {
			filename = result;
		}

		/*
		 * Make a list of the files already added.
		 */
		ArrayList<String> currentlist = new ArrayList<>();
		for (TableItem file : DirectoryListing.getItems()) {
			String fname = file.getText(1);
			currentlist.add(fname);
		}

		/*
		 * Add in the files on the disk..
		 */
		for (CPMDirectoryEntry d : pfd.DirectoryEntries) {
			String fname = d.GetFilename();
			currentlist.add(fname);
		}

		/*
		 * Check the filename against the list, and if found, create a new filename.
		 */
		int num = 1;
		boolean FileFound = true;
		while (FileFound) {
			FileFound = currentlist.indexOf(result) > -1;
			if (FileFound) {
				extension = String.valueOf(num++);
				while (extension.length() < 3) {
					extension = "0" + extension;
				}
				result = filename.trim() + "." + extension.trim();
			}
		}
		/*
		 * Resulting name should be unique.
		 */
		return (result);
	}

	/**
	 * Close and exit, saving files.
	 */
	@Override
	protected void DoAddFiles() {
		PLUS3DOSPartition pfd = (PLUS3DOSPartition) CurrentPartition;
		TableItem files[] = DirectoryListing.getItems();
		for (TableItem file : files) {
			NewFileListItem details = (NewFileListItem) file.getData();
			try {
				if (details.FileType == FILETYPE_PLUS3DOS) {
					pfd.AddCPMFile(details.filename, details.data);
				} else if (details.FileType == FILETYPE_RAW) {
					pfd.AddCPMFile(details.filename, details.data);
				} else {
					SaveFileWithDetails(details);					
				}
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Get the supported file text strings.
	 * Adding in the file with +3DOS header string.
	 * 
	 * @return
	 */
	@Override
	protected String[] GetSupportedFileTypes() {
		String result[] = new String[genericfiletypes.length + 1];
		System.arraycopy(genericfiletypes, 0, result, 0, genericfiletypes.length);
		result[result.length - 1] = "File w' plus3dos header";
		return (result);
	}
	

}

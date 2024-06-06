package hddEditor.ui.partitionPages.dialogs.AddFiles;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.CPM;
import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Speccy;
import hddEditor.libs.SpeccyFileEncoders;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;

public class GenericAddPageDialog {
	protected Display display = null;
	protected Shell shell = null;

	protected IDEDosPartition CurrentPartition;

	/*
	 * File types. 0-3 are the standard basic types. 
	 * Any after that are placeholders for special processing.
	 */
	protected final static int FILETYPE_BASIC = 0;
	protected final static int FILETYPE_NUMARRAY = 1;
	protected final static int FILETYPE_CHRARRAY = 2;
	protected final static int FILETYPE_CODE = 3;
	protected final static int FILETYPE_SCREEN = 4;

	/*
	 * The important components on the form.
	 */
	protected Table DirectoryListing = null;
	protected Slider intensitySlider = null;
	protected Text StartLine = null;
	protected Composite MainPage = null;
	protected Label ImageLabel = null;
	protected Button IsBWCheck = null;
	protected Text StartAddress = null;
	protected Text Filename = null;

	protected FileSelectDialog fsd = null;
	
	/*
	 * This class is used to store the details the files we want to add.
	 */
	public class NewFileListItem {
		// Original filename.
		public File OriginalFilename = null;
		// Filename as converted to CPM.
		public String filename = null;
		// +3DOS file header of the file already has one.
		public Plus3DosFileHeader fileheader = null;
		// File type for all file types except for TR-DOS.
		public int FileType = 0;
		// File type character as used TR-DOS. Not used for any other disk types.
		public char cFileType = FILETYPE_BASIC;
		// If the file is an image file, this contains the original image. Used so the
		// user can edit it.
		public BufferedImage OriginalImage = null;
		// start line
		public int line = 32768;
		// Load address for CODE files
		public int LoadAddress = 32768;
		// Intensity
		public int Intensity = 0;
		// BW
		public boolean IsBlackWhite = false;
		// Raw file data.
		public byte[] data = null;

	}

	/**
	 * Base constructor
	 * 
	 * @param display
	 */
	public GenericAddPageDialog(Display display, FileSelectDialog fsd) {
		this.display = display;
		this.fsd = fsd;
	}

	/**
	 * loop until the dialog is closed.
	 */
	public void loop() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		shell.dispose();
	}

	/**
	 * Close the form.
	 */
	public void close() {
		shell.close();
		if (!shell.isDisposed()) {
			shell.dispose();
		}
	}

	/**
	 * Dispose of any dialogs opened by this partition
	 */
	protected void DisposeSubDialogs() {

	}

	/**
	 * Render a the file as BASIC
	 * 
	 * @param data
	 * @param details
	 */
	protected void RenderBasic(byte data[], NewFileListItem details) {
		Plus3DosFileHeader pfd = details.fileheader;
		if (pfd == null) {
			byte tmpbyte[] = new byte[0x80];
			pfd = new Plus3DosFileHeader(tmpbyte);
			pfd.VariablesOffset = data.length;
		}
		StringBuilder sb = new StringBuilder();
		Speccy.DecodeBasicFromLoadedFile(data, sb, pfd.VariablesOffset, false, false);

		Text t = new Text(MainPage, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 4;
		gd.verticalSpan = 6;
		gd.minimumHeight = 198;
		gd.minimumWidth = 500;
		t.setLayoutData(gd);
		t.setText(sb.toString());
	}

	/**
	 * Render Character array
	 * 
	 * @param data
	 */
	protected void RenderChrArray(byte data[]) {
		int location = 0;

		// Number of dimensions
		int numDimensions = data[location++] & 0xff;

		// LOad the dimension sizes into an array
		int Dimsizes[] = new int[numDimensions];
		for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
			int dimsize = data[location++] & 0xff;
			dimsize = dimsize + (data[location++] & 0xff) * 0x100;
			Dimsizes[dimnum] = dimsize;
		}

		String s = "DIM A$(";
		for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
			if (dimnum > 0)
				s = s + ",";
			s = s + String.valueOf(Dimsizes[dimnum]);
		}
		s = s + ")\n";

		Text ArrayEdit = new Text(MainPage, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.verticalSpan = 6;
		gd.minimumHeight = 198;
		gd.minimumWidth = 500;
		ArrayEdit.setLayoutData(gd);

		// count of what dimensions have been processed.
		int DimCounts[] = new int[numDimensions];
		for (int dimnum = 0; dimnum < numDimensions; dimnum++)
			DimCounts[dimnum] = 0;

		StringBuilder sb = new StringBuilder();
		sb.append(s);

		boolean complete = false;
		while (!complete) {
			for (int cc = 0; cc < Dimsizes[Dimsizes.length - 1]; cc++) {

				if (cc != 0) {
					sb.append(",");
				}
				String chr = Speccy.tokens[data[location++] & 0xff];
				chr = chr.replace("&amp;", "&");
				chr = chr.replace("&gt;", ">");
				chr = chr.replace("&lt;", "<");

				sb.append(chr);
			}
			sb.append("\r\n");
			int diminc = Dimsizes.length - 2;
			boolean doneInc = false;
			while (!doneInc) {
				if (diminc == -1) {
					doneInc = true;
					complete = true;
				} else {
					int x = DimCounts[diminc];
					x++;
					if (x == Dimsizes[diminc]) {
						DimCounts[diminc] = 0;
						diminc--;
					} else {
						DimCounts[diminc] = x;
						doneInc = true;
					}
				}
			}
		}
		ArrayEdit.setText(sb.toString());
	}

	/**
	 * Render numeric array
	 * 
	 * @param data
	 */
	protected void RenderNumArray(byte data[]) {
		int location = 0;

		// Number of dimensions
		int numDimensions = data[location++] & 0xff;

		// Load the dimension sizes into an array
		int Dimsizes[] = new int[numDimensions];
		for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
			int dimsize = data[location++] & 0xff;
			dimsize = dimsize + (data[location++] & 0xff) * 0x100;
			Dimsizes[dimnum] = dimsize;
		}

		String s = "DIM A(";
		for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
			if (dimnum > 0)
				s = s + ",";
			s = s + String.valueOf(Dimsizes[dimnum]);
		}
		s = s + ")\n";

		Text ArrayEdit = new Text(MainPage, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.verticalSpan = 6;
		gd.minimumHeight = 198;
		gd.minimumWidth = 500;
		ArrayEdit.setLayoutData(gd);

		// count of what dimensions have been processed.
		int DimCounts[] = new int[numDimensions];
		for (int dimnum = 0; dimnum < numDimensions; dimnum++)
			DimCounts[dimnum] = 0;

		StringBuilder sb = new StringBuilder();
		sb.append(s);

		boolean complete = false;
		while (!complete) {
			for (int cc = 0; cc < Dimsizes[Dimsizes.length - 1]; cc++) {

				if (cc != 0) {
					sb.append(",");
				}
				double x = Speccy.GetNumberAtByte(data, location);
				// special case anything thats an exact integer because it makes the arrays look
				// less messy when displayed.
				if (x != Math.rint(x)) {
					sb.append(x);
					sb.append(",");
				} else {
					sb.append((int) x);
				}
				location = location + 5;
			}
			sb.append("\r\n");
			int diminc = Dimsizes.length - 2;
			boolean doneInc = false;
			while (!doneInc) {
				if (diminc == -1) {
					doneInc = true;
					complete = true;
				} else {
					int x = DimCounts[diminc];
					x++;
					if (x == Dimsizes[diminc]) {
						DimCounts[diminc] = 0;
						diminc--;
					} else {
						DimCounts[diminc] = x;
						doneInc = true;
					}
				}
			}

		}
		ArrayEdit.setText(sb.toString());
	}

	/**
	 * Render the selected file as CODE
	 * 
	 * @param data
	 */
	protected void RenderCode(NewFileListItem details) {
		byte data[] = details.data;

		int AddressLength = String.format("%X", data.length - 1).length();

		Table HexTable = new Table(MainPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		HexTable.setLinesVisible(true);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = 400;
		HexTable.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(HexTable, SWT.LEFT);
		tc1.setText("Address");
		tc1.setWidth(80);
		for (int i = 0; i < 16; i++) {
			TableColumn tcx = new TableColumn(HexTable, SWT.LEFT);
			tcx.setText(String.format("%02X", i));
			tcx.setWidth(30);
		}
		TableColumn tc2 = new TableColumn(HexTable, SWT.LEFT);
		tc2.setText("Ascii");
		tc2.setWidth(160);

		HexTable.setHeaderVisible(true);

		int ptr = 0;
		int numrows = data.length / 16;
		if (data.length % 16 != 0) {
			numrows++;
		}
		int Address = details.LoadAddress;

		Font mono = new Font(MainPage.getDisplay(), "Monospace", 10, SWT.NONE);
		for (int rownum = 0; rownum < numrows; rownum++) {
			TableItem Row = new TableItem(HexTable, SWT.NONE);

			String asciiLine = "";
			String content[] = new String[18];
			String addr = String.format("%X", Address);
			Address = Address + 16;
			while (addr.length() < AddressLength) {
				addr = "0" + addr;
			}
			content[0] = addr;
			for (int i = 1; i < 17; i++) {
				byte b = 0;
				if (ptr < data.length) {
					b = data[ptr++];
					content[i] = String.format("%02X", (b & 0xff));
				} else {
					content[i] = "--";
				}
				if (b >= 32 && b <= 127) {
					asciiLine = asciiLine + (char) b;
				} else {
					asciiLine = asciiLine + ".";
				}
			}
			content[17] = asciiLine;
			Row.setText(content);
			Row.setFont(mono);
		}
	}

	/**
	 * Render the currently selected file as a screen.
	 * 
	 * @param data
	 */
	protected void RenderScreen(byte data[], NewFileListItem details) {
		ImageData image = Speccy.GetImageFromFileArray(data, 0x00);
		Image img = new Image(MainPage.getDisplay(), image);
		ImageLabel = new Label(MainPage, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumHeight = 192;
		gd.minimumWidth = 256;
		gd.horizontalSpan = 2;
		ImageLabel.setLayoutData(gd);
		ImageLabel.setImage(img);
		IsBWCheck.setSelection(details.IsBlackWhite);
		intensitySlider.setSelection(details.Intensity);
	}

	/**
	 * Re-render the selected image.
	 */
	protected void ReRenderImage() {
		// If selected file is an image and is selected...
		if (ImageLabel != null) {
			if (DirectoryListing.getSelectionCount() > 0) {
				// Get the image details.
				TableItem SelectedFile = DirectoryListing.getSelection()[0];
				NewFileListItem details = (NewFileListItem) SelectedFile.getData();
				// Render the image
				byte buffer[] = SpeccyFileEncoders.ScaleImage(shell.getDisplay(), intensitySlider.getSelection(),
						details.OriginalImage, IsBWCheck.getSelection());
				// write it back to the buffer and the listbox.
				details.data = buffer;
				details.Intensity = intensitySlider.getSelection();
				details.IsBlackWhite = IsBWCheck.getSelection();
				SelectedFile.setData(details);

				// Now, re-render to the displayed image
				ImageData image = Speccy.GetImageFromFileArray(buffer, 0x00);
				Image img = new Image(MainPage.getDisplay(), image);

				ImageLabel.setImage(img);
				MainPage.pack();
				shell.pack();
			}
		}
	}

	/**
	 * Get a unique name
	 * 
	 * @param s
	 * @return
	 */
	protected String UniqueifyName(String s) {
		System.out.println("UniqueifyName needs to be implemented");
		return ("ERR");
	}

	/**
	 * Add a character array, Note, this is just really a text file.
	 */
	protected void DoAddCharacterFiles() {
		int filelimit = 16384;
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open CSV file");
		if ((Selected != null) && (Selected.length > 0)) {
			for (File filename : Selected) {
				/*
				 * Iterate all the selected files.
				 */
				try {
					byte ArrayAsBytes[] = SpeccyFileEncoders.EncodeCharacterArray(filename, filelimit);
					/*
					 * Create the text strings for the row.
					 */
					String DosFileName = UniqueifyName(CPM.FixFullName(filename.getName()));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filename.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "Character Array";
					values[3] = String.valueOf(ArrayAsBytes.length);
					values[4] = "";

					/*
					 * Create the data object
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filename;
					listitem.filename = DosFileName;
					listitem.fileheader = null;
					listitem.FileType = FILETYPE_CHRARRAY;
					listitem.data = ArrayAsBytes;

					/*
					 * Add the row
					 */
					item2.setText(values);
					item2.setData(listitem);
				} catch (IOException e) {
					System.out.println("Failed to add " + filename.getAbsolutePath() + " " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Add a csv file as a numeric array
	 */
	protected void DoAddNumericArrays() {
		int filelimit = 16384;
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open CSV file");
		if ((Selected != null) && (Selected.length > 0)) {
			for (File filename : Selected) {
				try {
					byte ArrayAsBytes[] = SpeccyFileEncoders.EncodeNumericArray(filename, filelimit);

					/*
					 * Create the row text items
					 */
					String DosFileName = UniqueifyName(CPM.FixFullName(filename.getName()));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filename.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "Number Array";
					values[3] = String.valueOf(ArrayAsBytes.length);
					values[4] = "";

					/*
					 * Populate the data object
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filename;
					listitem.filename = DosFileName;
					listitem.FileType = FILETYPE_NUMARRAY;
					listitem.fileheader = null;
					listitem.data = ArrayAsBytes;

					/*
					 * Add the row
					 */
					item2.setText(values);
					item2.setData(listitem);
				} catch (IOException e) {
					System.out.println("Failed to add " + filename.getAbsolutePath() + " " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Convert and Add image files as SCREEN$ errors Supports all image types
	 * ImageIO supports (PNG, GIF, JPEG, BMP, WEBMP)
	 */
	protected void DoAddImageFiles() {
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open Image file");
		if ((Selected != null) && (Selected.length > 0)) {
			for (File filename : Selected) {
				BufferedImage RawImage;
				try {
					/*
					 * Load the image
					 */
					RawImage = ImageIO.read(filename);

					/*
					 * Convert and scale the image
					 */
					int bwslider = intensitySlider.getSelection();
					byte buffer[] = SpeccyFileEncoders.ScaleImage(shell.getDisplay(), bwslider, RawImage,
							IsBWCheck.getSelection());

					/*
					 * Create the row texts.
					 */
					String DosFileName = UniqueifyName(CPM.FixFullName(filename.getName()));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filename.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "SCREEN$";
					values[3] = String.valueOf(buffer.length);
					values[4] = "";

					/*
					 * Create the data storage object. Note, we store the original image as well as
					 * the buffer
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filename;
					listitem.filename = DosFileName;
					listitem.fileheader = null;
					listitem.FileType = FILETYPE_SCREEN;
					listitem.data = buffer;
					listitem.OriginalImage = RawImage;
					listitem.Intensity = bwslider;
					listitem.IsBlackWhite = IsBWCheck.getSelection();
					listitem.LoadAddress = 0x4000;

					/*
					 * Add the row.
					 */
					item2.setText(values);
					item2.setData(listitem);
				} catch (IOException e) {
					System.out.println("Failed to add " + filename.getAbsolutePath() + " " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Add BINARY file(s) as a CODE file.
	 */
	protected void DoAddBinaryFiles() {
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open raw binary file");
		if ((Selected != null) && (Selected.length > 0)) {
			for (File filename : Selected) {
				
				byte buffer[] = new byte[(int) filename.length()];
				FileInputStream is = null;
				try {
					try {
						is = new FileInputStream(filename);
						is.read(buffer);
					} finally {
						if (is != null)
							is.close();
					}
				} catch (IOException e) {
					System.out.println("Error loading file!");
				}

				/*
				 * Create the texts for the Row
				 */
				String DosFileName = UniqueifyName(CPM.FixFullName(filename.getName()));
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String values[] = new String[5];
				values[0] = filename.getAbsolutePath();
				values[1] = DosFileName;
				values[2] = "Code (Raw Manual)";
				values[3] = String.valueOf(buffer.length);
				values[4] = "";

				/*
				 * Create the data storage object
				 */
				NewFileListItem listitem = new NewFileListItem();
				listitem.OriginalFilename = filename;
				listitem.filename = DosFileName;
				listitem.fileheader = null;
				listitem.FileType = FILETYPE_CODE;
				listitem.LoadAddress = Integer.valueOf(StartAddress.getText());
				listitem.data = buffer;

				/*
				 * Add the row
				 */
				item2.setText(values);
				item2.setData(listitem);
			}
		}
	}

	/**
	 * Add a text file as a BASIC file.
	 */
	protected void DoAddTextBasicFiles() {
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open BASIC text file");
		if ((Selected != null) && (Selected.length > 0)) {
			for (File filename : Selected) {
				
				byte data[] = SpeccyFileEncoders.EncodeTextFileToBASIC(filename);

				/*
				 * Make the values required for the table item.
				 */
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String values[] = new String[5];
				values[0] = filename.getAbsolutePath();
				values[1] = UniqueifyName(CPM.FixFullName(filename.getName()));
				values[2] = "Basic (Manual)";
				values[3] = String.valueOf(data.length);
				values[4] = "";

				/*
				 * Populate the storage array details.
				 */
				NewFileListItem listitem = new NewFileListItem();
				listitem.OriginalFilename = filename;
				listitem.filename = values[1];
				listitem.fileheader = null;
				listitem.FileType = FILETYPE_BASIC;
				listitem.data = data;

				/*
				 * Add to the table
				 */
				item2.setText(values);
				item2.setData(listitem);
			}
		}
	}

	/**
	 * Add pre-converted basic files.
	 */
	protected void DoAddBinaryBasicFiles() {
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open encoded BASIC file");
		if ((Selected != null) && (Selected.length > 0)) {
			for (File filename : Selected) {
				/*
				 * Load the file
				 */
				byte buffer[] = new byte[(int) filename.length()];
				FileInputStream is = null;
				try {
					try {
						is = new FileInputStream(filename);
						is.read(buffer);
					} finally {
						if (is != null)
							is.close();
					}
				} catch (IOException e) {
					System.out.println("Error loading file!");
				}

				/*
				 * Create the texts for the table row
				 */
				String DosFileName = UniqueifyName(CPM.FixFullName(filename.getName()));
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String values[] = new String[5];
				values[0] = filename.getAbsolutePath();
				values[1] = DosFileName;
				values[2] = "Basic (Raw Manual)";
				values[3] = String.valueOf(buffer.length);
				values[4] = "";

				/*
				 * Create the storage object and add it to the row
				 */
				NewFileListItem listitem = new NewFileListItem();
				listitem.OriginalFilename = filename;
				listitem.filename = DosFileName;
				listitem.fileheader = null;
				listitem.FileType = FILETYPE_BASIC;
				listitem.data = buffer;

				/*
				 * Create the table row
				 */
				item2.setText(values);
				item2.setData(listitem);
			}
		}
	}

}

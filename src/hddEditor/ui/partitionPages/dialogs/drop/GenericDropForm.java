package hddEditor.ui.partitionPages.dialogs.drop;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.Speccy;
import hddEditor.libs.SpeccyFileEncoders;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;

public class GenericDropForm {

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
		// File type as defined above.
		public int FileType = FILETYPE_RAW;
		// If the file is an image file, this contains the original image. Used so the
		// user can edit it.
		public BufferedImage OriginalImage = null;
		// Load address for CODE files
		public int LoadAddress = 32768;
		// Start line for basic files
		public int StartLine = 32768;
		// Intensity
		public int Intensity = 0;
		// BW
		public boolean IsBlackWhite = false;
		// Raw file data.
		public byte[] data = null;

		public String GetFileTypeAsName() {
			String result = "Unknown";
			String s[] = GetSupportedFileTypes();
			if (FileType > -1 && FileType < s.length) {
				result = s[FileType];
			}
			return (result);
		}
	}

	protected Display display = null;
	protected Shell shell = null;

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
	protected Label filenameLabel = null;
	protected Combo SelectedFileType = null;

	protected NewFileListItem details = null;

	protected String[] genericfiletypes = new String[] { "Basic (text)", "Basic (Encoded)", "Code", "Code (screen$)",
			"Character Array (text)", "Character Array (encoded)", "Numeric Array (text)", "Numeric Array (encoded)",
			"image (PNG, gif or jpg)", "Raw" };

	/*
	 * Current partition.
	 */
	protected IDEDosPartition CurrentPartition = null;

	/**
	 * Basic file types
	 */
	protected final static int FILETYPE_BASIC_TEXT = 0;
	protected final static int FILETYPE_BASIC_ENC = 1;
	protected final static int FILETYPE_CODE = 2;
	protected final static int FILETYPE_CODE_SCREEN = 3;
	protected final static int FILETYPE_CHRARRAY_TEXT = 4;
	protected final static int FILETYPE_CHRARRAY_ENC = 5;
	protected final static int FILETYPE_NUMARRAY_TEXT = 6;
	protected final static int FILETYPE_NUMARRAY_ENC = 7;
	protected final static int FILETYPE_IMAGE = 8;
	protected final static int FILETYPE_RAW = 9;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public GenericDropForm(Display display) {
		this.display = display;
	}

	/**
	 * Dialog loop, open and wait until closed.
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
		DisposeSubDialogs();
	}

	/**
	 * Dispose of any dialogs opened by this form
	 */
	protected void DisposeSubDialogs() {

	}

	/**
	 * 
	 * @return
	 */
	protected String[] GetSupportedFileTypes() {
		return (genericfiletypes);
	}

	/**
	 * Show the dialog
	 * 
	 * @param title
	 * @param p3d
	 */
	public void Show(String title, IDEDosPartition p3d, File files[]) {
		CurrentPartition = p3d;
		Createform(title, files);
		loop();
	}

	/**
	 * Create the components on the form.
	 * 
	 * @param title
	 */
	protected void Createform(String title, File files[]) {
		shell = new Shell(display);
		shell.setSize(900, 810);
		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				DisposeSubDialogs();
			}
		});

		GridLayout gridLayout = new GridLayout(4, true);
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;

		shell.setLayout(gridLayout);
		shell.setText(title);
		DirectoryListing = new Table(shell, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		DirectoryListing.setLinesVisible(true);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = 100;
		DirectoryListing.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(DirectoryListing, SWT.LEFT);
		TableColumn tc2 = new TableColumn(DirectoryListing, SWT.LEFT);
		TableColumn tc3 = new TableColumn(DirectoryListing, SWT.LEFT);
		TableColumn tc4 = new TableColumn(DirectoryListing, SWT.LEFT);
		TableColumn tc5 = new TableColumn(DirectoryListing, SWT.LEFT);
		tc1.setText("Filename");
		tc2.setText("+3 Filename");
		tc3.setText("Type");
		tc4.setText("Length");
		tc5.setText("Flags");
		tc1.setWidth(250);
		tc2.setWidth(150);
		tc3.setWidth(150);
		tc4.setWidth(150);
		tc5.setWidth(100);
		DirectoryListing.setHeaderVisible(true);
		DirectoryListing.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSelectedFileChange();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		filenameLabel = new Label(shell, SWT.LEFT);
		filenameLabel.setText("File: <none selected>");
		FontData fontData = filenameLabel.getFont().getFontData()[0];
		Font font = new Font(shell.getDisplay(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		filenameLabel.setFont(font);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 3;
		filenameLabel.setLayoutData(gd);

		SelectedFileType = new Combo(shell, SWT.DROP_DOWN);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		SelectedFileType.setLayoutData(gd);
		SelectedFileType.setItems(GetSupportedFileTypes());
		SelectedFileType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String s = SelectedFileType.getText();
				int i = -1;
				int cnt = 0;
				for (String ftype : GetSupportedFileTypes()) {
					if (ftype.equals(s)) {
						i = cnt;
					}
					cnt++;
				}
				if ((details != null) && (i != -1)) {
					details.FileType = i;
					FileTypeChanged();
					UpdateFlagsFromDetails();
				}
			}
		});

		Label l = new Label(shell, SWT.LEFT);
		l.setText("BASIC files:");
		l.setFont(font);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		l.setLayoutData(gd);
		new Label(shell, SWT.NONE);

		l = new Label(shell, SWT.LEFT);
		l.setText("Line (32788 = none):");

		StartLine = new Text(shell, SWT.BORDER);
		StartLine.setText("32768");
		new Label(shell, SWT.LEFT);
		new Label(shell, SWT.LEFT);
		StartLine.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
				TableItem SelectedFiles[] = DirectoryListing.getSelection();
				if (SelectedFiles != null && SelectedFiles.length > 0) {
					NewFileListItem details = (NewFileListItem) SelectedFiles[0].getData();
					if (details != null) {
						details.StartLine = Integer.valueOf(StartLine.getText());
						UpdateFlagsFromDetails();
					}
				}
			}

			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});

		l = new Label(shell, SWT.LEFT);
		l.setText("Image files:");
		l.setFont(font);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		l.setLayoutData(gd);

		new Label(shell, SWT.NONE);

		IsBWCheck = new Button(shell, SWT.CHECK);
		IsBWCheck.setText("Monochrome");
		IsBWCheck.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ReRenderImage();
				UpdateFlagsFromDetails();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Label IntensityLabel = new Label(shell, SWT.LEFT);
		IntensityLabel.setText("Cutoff: 100%");

		intensitySlider = new Slider(shell, SWT.HORIZONTAL | SWT.BORDER);
		intensitySlider.setBounds(0, 0, 150, 40);
		intensitySlider.setMaximum(104);
		intensitySlider.setMinimum(0);
		intensitySlider.setIncrement(1);
		intensitySlider.setSelection(50);
		intensitySlider.setPageIncrement(10);
		intensitySlider.setThumb(4);

		intensitySlider.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				int perspectiveValue = intensitySlider.getMaximum() - intensitySlider.getSelection()
						+ intensitySlider.getMinimum() - intensitySlider.getThumb();
				IntensityLabel.setText("Cutoff: " + perspectiveValue + "%");
				ReRenderImage();
				UpdateFlagsFromDetails();
			}
		});
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		intensitySlider.setLayoutData(gd);

		l = new Label(shell, SWT.NONE);

		l.setText("CODE files:");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		l.setLayoutData(gd);
		l.setFont(font);

		new Label(shell, SWT.NONE);
		l = new Label(shell, SWT.NONE);
		l.setText("CODE load address:");
		StartAddress = new Text(shell, SWT.NONE);
		StartAddress.setText("32768");
		StartAddress.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
				TableItem SelectedFiles[] = DirectoryListing.getSelection();
				if (SelectedFiles != null && SelectedFiles.length > 0) {
					NewFileListItem details = (NewFileListItem) SelectedFiles[0].getData();
					if (details != null) {
						details.LoadAddress = Integer.valueOf(StartAddress.getText());
						System.out.println("Reset load address");
					}
					UpdateFlagsFromDetails();
				}
			}

			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});

		MainPage = new Composite(shell, SWT.BORDER);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.makeColumnsEqualWidth = true;

		gd = new GridData(SWT.FILL, SWT.NONE, true, true);
		gd.heightHint = 200;
		gd.horizontalSpan = 4;
		MainPage.setLayoutData(gd);
		MainPage.setLayout(gridLayout);

		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;
		Button Btn = new Button(shell, SWT.PUSH);
		Btn.setText("Add files");
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoAddFiles();
				shell.close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;
		Btn = new Button(shell, SWT.PUSH);
		Btn.setText("Cancel");
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				shell.close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		IntensityLabel.setText("Cutoff: 50%");

		PopulateFiles(files);
		shell.pack();
	}


	/**
	 * Re-render the image after the image slider is moved or the bw/colour button checked.
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
	 * Update the flags text on the given selected items.
	 */
	protected void UpdateFlagsFromDetails() {
		TableItem selected[] = DirectoryListing.getSelection();
		if (selected != null) {
			for (TableItem item : selected) {
				NewFileListItem nfli = (NewFileListItem) item.getData();
				if (nfli != null) {
					String content[] = new String[5];
					content[0] = nfli.OriginalFilename.getName();
					content[1] = nfli.filename;
					content[2] = nfli.GetFileTypeAsName();
					content[3] = String.format("%d", nfli.OriginalFilename.length());
					content[4] = GetFlagsFromDetails(nfli);
					item.setText(content);
				}
			}
		}

	}

	/**
	 * Get the flags text from the given NewFileListItem
	 * 
	 * @param Details
	 * @return
	 */
	protected String GetFlagsFromDetails(NewFileListItem Details) {
		String flags = "";
		if (Details != null) {
			if (Details.fileheader != null) {
				switch (Details.fileheader.filetype) {
				case 0:
					flags = "BASIC line " + Details.fileheader.line;
				case 3:
					flags = "Code " + Details.fileheader.loadAddr + "," + Details.fileheader.filelength;
				}
			} else {
				switch (Details.FileType) {
				case FILETYPE_BASIC_TEXT:
				case FILETYPE_BASIC_ENC:
					flags = "BASIC line " + Details.StartLine;
					break;
				case FILETYPE_CODE:
				case FILETYPE_CODE_SCREEN:
					flags = "Code " + Details.LoadAddress + "," + Details.OriginalFilename.length();
					break;
				case FILETYPE_IMAGE:
					flags = "Code 16384,6912";
					break;
				}
			}
		}
		return (flags);
	}

	protected void FileTypeChanged() {
		if (details != null) {
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

			switch (details.FileType) {
			case FILETYPE_BASIC_TEXT:
				RenderText(details);
				break;
			case FILETYPE_BASIC_ENC:
				RenderBasic(details);
				break;
			case FILETYPE_CODE:
				RenderCode(details);
				break;
			case FILETYPE_CODE_SCREEN:
				RenderScreen(details);
				break;
			case FILETYPE_CHRARRAY_TEXT:
				RenderText(details);
				break;
			case FILETYPE_CHRARRAY_ENC:
				RenderChrArray(details);
				break;
			case FILETYPE_NUMARRAY_TEXT:
				RenderText(details);
				break;
			case FILETYPE_NUMARRAY_ENC:
				RenderNumArray(details);
				break;
			case FILETYPE_IMAGE:
				RenderScreen(details);
				break;
			case FILETYPE_RAW:
				RenderCode(details);
				break;
			}

			MainPage.pack();
			shell.pack();
			EnableDisableModifiers();
			shell.layout(true, true);
			final Point newSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

			shell.setSize(newSize);
		}

	}

	protected void DoSelectedFileChange() {
		if (DirectoryListing.getSelectionCount() > 0) {
			/*
			 * Get the first selected file
			 */
			TableItem SelectedFile = DirectoryListing.getSelection()[0];
			details = (NewFileListItem) SelectedFile.getData();
			filenameLabel.setText(details.OriginalFilename.getName());

			/*
			 * Get the file type details. and set the file type
			 */
			SelectedFileType.setText(details.GetFileTypeAsName());
			StartLine.setText(String.format("%d", details.StartLine));
			IsBWCheck.setSelection(details.IsBlackWhite);
			intensitySlider.setSelection(details.Intensity);
			StartAddress.setText(String.format("%d", details.LoadAddress));
			FileTypeChanged();
			EnableDisableModifiers();
			shell.pack();
		}	}

	/**
	 * Render the data as a generic text file.
	 * 
	 * @param details
	 */
	private void RenderText(NewFileListItem details) {
		byte data[] = ExtractData(details);
		String content = new String(data);

		Text t = new Text(MainPage, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 4;
		gd.verticalSpan = 6;
		gd.minimumHeight = 198;
		gd.minimumWidth = 500;
		t.setLayoutData(gd);
		t.setText(content);
	}

	/**
	 * This gets the actual file data to be added to the disk.
	 * Normally you don't have to override this, however its provided
	 * so files like +3DOS and Microdrive files can remove the header.
	 * 
	 * @param details
	 * @return
	 */
	protected byte[] ExtractData(NewFileListItem details) {
		return (details.data);
	}

	/**
	 * Render a the file as BASIC
	 * 
	 * @param details
	 */
	protected void RenderBasic(NewFileListItem details) {
		Plus3DosFileHeader pfd = details.fileheader;
		if (pfd == null) {
			byte tmpbyte[] = new byte[0x80];
			pfd = new Plus3DosFileHeader(tmpbyte);
			pfd.VariablesOffset = details.data.length;
		}
		byte data[] = ExtractData(details);
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
	 * @param details
	 */
	protected void RenderChrArray(NewFileListItem details) {
		int location = 0;

		byte data[] = ExtractData(details);

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
	 * @param details
	 */
	protected void RenderNumArray(NewFileListItem details) {
		int location = 0;
		byte data[] = ExtractData(details);

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
	 * @param details
	 */
	protected void RenderCode(NewFileListItem details) {
		byte data[] = ExtractData(details);
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
	protected void RenderScreen(NewFileListItem details) {
		byte data[] = ExtractData(details);
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
	 * Enable or disable the appropriate buttons and sliders depending on the file
	 * type.
	 */
	protected void EnableDisableModifiers() {
		boolean ImageType = false;
		boolean BasicType = false;
		boolean CodeType = false;
		switch (details.FileType) {
		case FILETYPE_BASIC_TEXT:
			BasicType = true;
			break;
		case FILETYPE_BASIC_ENC:
			BasicType = true;
			break;
		case FILETYPE_CODE:
			CodeType = true;
			break;
		case FILETYPE_CODE_SCREEN:
			CodeType = true;
			break;
		case FILETYPE_CHRARRAY_TEXT:
			break;
		case FILETYPE_CHRARRAY_ENC:
			break;
		case FILETYPE_NUMARRAY_TEXT:
			break;
		case FILETYPE_NUMARRAY_ENC:
			break;
		case FILETYPE_IMAGE:
			ImageType = true;
			break;
		case FILETYPE_RAW:
			CodeType = true;
			break;
		}

		intensitySlider.setEnabled(ImageType);
		IsBWCheck.setEnabled(ImageType);
		StartLine.setEnabled(BasicType);
		StartAddress.setEnabled(CodeType);
	}

	/**
	 * Populate the form with the given dropped files.
	 * 
	 * @param files
	 */
	protected void PopulateFiles(File files[]) {
		System.out.println("Dropped " + files.length + " files.");
		for (File f : files) {
			if (f != null) {
				TableItem item = new TableItem(DirectoryListing, SWT.NONE);
				String content[] = new String[5];
				NewFileListItem nfli = IdentifyFileType(f);

				content[0] = nfli.OriginalFilename.getName();
				content[1] = nfli.filename;
				content[2] = nfli.GetFileTypeAsName();
				content[3] = String.format("%d", f.length());
				content[4] = GetFlagsFromDetails(nfli);
				item.setText(content);
				item.setData(nfli);
			}
		}
	}

	/**
	 * Fill out a NewFileListItem from a given file.
	 * 
	 * @param f
	 * @return
	 */
	protected NewFileListItem IdentifyFileType(File f) {
		NewFileListItem nfli = new NewFileListItem();
		nfli.FileType = FILETYPE_CODE;
		nfli.LoadAddress = 0x8000;
		if (f.length() == 0x1b00) {
			nfli.LoadAddress = 0x4000;
			nfli.FileType = FILETYPE_CODE_SCREEN;
		}
		nfli.filename = UniqueifyName(f.getName());
		nfli.OriginalFilename = f;
		nfli.data = GeneralUtils.ReadFileIntoArray(f.getAbsolutePath());

		nfli.fileheader = null;

		// Check for image types
		byte d[] = nfli.data;
		if ((d[0] & 0xff) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') { // png
			nfli.FileType = FILETYPE_IMAGE;
		} else if ((d[0] & 0xff) == 0xFF && (d[1] & 0xff) == 0xd8) { // jpg
			nfli.FileType = FILETYPE_IMAGE;
		} else if (d[0] == 'G' && d[1] == 'I' && d[2] == 'F') { // gif
			nfli.FileType = FILETYPE_IMAGE;
		}
		if (f.canRead()) {
			if (nfli.FileType == FILETYPE_IMAGE) {
				BufferedImage RawImage;
				try {
					RawImage = ImageIO.read(f);
					nfli.OriginalImage = RawImage;

					byte buffer[] = SpeccyFileEncoders.ScaleImage(shell.getDisplay(), intensitySlider.getSelection(),
							nfli.OriginalImage, IsBWCheck.getSelection());
					// write it back to the buffer and the listbox.
					nfli.data = buffer;
					nfli.LoadAddress = 16384;
				} catch (Exception E) {

				}
			}
		}
		return (nfli);
	}

	/**
	 * Return a unique and valid filename This should be overridden
	 * 
	 * @param name
	 * @return
	 */
	protected String UniqueifyName(String name) {
		System.out.println("UniqueifyName is not implemented!");
		return name;
	}
	
	/**
	 * Add one file.
	 * 
	 * @param details
	 * @throws IOException
	 */
	protected void SaveFileWithDetails(NewFileListItem details) throws IOException {
		switch (details.FileType) {
		case FILETYPE_BASIC_TEXT:
			byte data[] = SpeccyFileEncoders.EncodeTextFileToBASIC(details.OriginalFilename.getAbsoluteFile());
			CurrentPartition.AddBasicFile(details.filename, data, details.StartLine, data.length);
			break;
		case FILETYPE_BASIC_ENC:
			CurrentPartition.AddBasicFile(details.filename, details.data, details.StartLine,
					details.data.length);
			break;
		case FILETYPE_CODE:
			CurrentPartition.AddCodeFile(details.filename, details.LoadAddress, details.data);
			break;
		case FILETYPE_CODE_SCREEN:
			CurrentPartition.AddCodeFile(details.filename, details.LoadAddress, details.data);
			break;
		case FILETYPE_CHRARRAY_TEXT:
			byte chrdata[] = SpeccyFileEncoders.EncodeCharacterArray(details.OriginalFilename.getAbsoluteFile(),
					512);
			CurrentPartition.AddCharArray(details.filename, chrdata, "");
			break;
		case FILETYPE_CHRARRAY_ENC:
			CurrentPartition.AddCharArray(details.filename, details.data, "");
			break;
		case FILETYPE_NUMARRAY_TEXT:
			byte numdata[] = SpeccyFileEncoders.EncodeNumericArray(details.OriginalFilename.getAbsoluteFile(),
					512);
			CurrentPartition.AddNumericArray(details.filename, numdata, "");
			break;
		case FILETYPE_NUMARRAY_ENC:
			CurrentPartition.AddNumericArray(details.filename, details.data, "");
			break;
		case FILETYPE_IMAGE:
			CurrentPartition.AddCodeFile(details.filename, details.LoadAddress, details.data);
			break;
		case FILETYPE_RAW:
			CurrentPartition.AddCodeFile(details.filename, 0, details.data);
			break;
		}
	}

	/**
	 * Add the files. This is the generic method.  Override it to add additional file types.
	 */
	protected void DoAddFiles() {
		TableItem files[] = DirectoryListing.getItems();
		for (TableItem file : files) {
			NewFileListItem details = (NewFileListItem) file.getData();
			try {
				SaveFileWithDetails(details);					
			} catch (IOException e) {
			}
		}
	}


}

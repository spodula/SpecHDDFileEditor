package hddEditor.ui.partitionPages.dialogs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.Speccy;
import hddEditor.libs.TRDOS;
import hddEditor.libs.partitions.TrDosPartition;
import hddEditor.libs.partitions.trdos.TrdDirectoryEntry;

public class AddFilesToTrDosPartition {
	private Display display = null;
	private Shell shell = null;

	/*
	 * The important components on the form.
	 */
	private Table DirectoryListing = null;
	private Slider intensitySlider = null;
	private Text StartLine = null;
	private Composite MainPage = null;
	private Label ImageLabel = null;
	private Button IsBWCheck = null;
	private Text GenericFileType = null;

	/*
	 * Current disk.
	 */
	private TrDosPartition CurrentPartition = null;

	private final static char FILETYPE_BASIC = 'B';
	private final static char FILETYPE_NUMARRAY = 'D';
	private final static char FILETYPE_CHRARRAY = 'E';
	private final static char FILETYPE_CODE = 'C';
	private final static char FILETYPE_STREAM = '#';
	private final static char FILETYPE_SCREEN = 'S';

	/*
	 * This class is used to store the details the files we want to add.
	 */
	public class NewFileListItem {
		// Original filename.
		public File OriginalFilename = null;
		// Filename as converted to TR-DOS.
		public String filename = null;
		// File type as defined above.
		public char FileType = FILETYPE_BASIC;
		// If the file is an image file, this contains the original image. Used so the
		// user can edit it.
		public BufferedImage OriginalImage = null;
		// Intensity
		public int Intensity = 0;
		// BW
		public boolean IsBlackWhite = false;
		// Raw file data.
		public byte[] data = null;
		// BASIC line
		public int line = 32768;

	}

	/**
	 * 
	 */
	public void close() {
		shell.close();
		if (!shell.isDisposed()) {
			shell.dispose();
		}
	}

	/**
	 * Dispose of any dialogs openned by this partition
	 */
	protected void DisposeSubDialogs() {

	}

	public AddFilesToTrDosPartition(Display display) {
		this.display = display;
	}

	public void Show(String Title, TrDosPartition partition) {
		CurrentPartition = partition;
		Createform(Title);
		loop();
	}

	public void loop() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		shell.dispose();
	}

	private void Createform(String title) {
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

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;

		Button Btn = new Button(shell, SWT.PUSH);
		Btn.setText("Select Text BASIC file");
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoAddTextBasicFiles();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Btn = new Button(shell, SWT.PUSH);
		Btn.setText("Select binary BASIC file");
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoAddBinaryBasicFiles();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Btn = new Button(shell, SWT.PUSH);
		Btn.setText("Select Code file");
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoAddCodeFiles();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Btn = new Button(shell, SWT.PUSH);
		Btn.setText("Select image as screen$");
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoAddImageFiles();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Btn = new Button(shell, SWT.PUSH);
		Btn.setText("Select Numeric array");
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoAddNumericArrays();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Btn = new Button(shell, SWT.PUSH);
		Btn.setText("Select Character array");
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoAddCharacterFiles();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Btn = new Button(shell, SWT.PUSH);
		Btn.setText("Add Generic type ->");
		Btn.setLayoutData(gd);
		GenericFileType = new Text(shell, SWT.BORDER);
		GenericFileType.setText("#");
		
		

		DirectoryListing = new Table(shell, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		DirectoryListing.setLinesVisible(true);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = 100;
		DirectoryListing.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(DirectoryListing, SWT.LEFT);
		TableColumn tc2 = new TableColumn(DirectoryListing, SWT.LEFT);
		TableColumn tc3 = new TableColumn(DirectoryListing, SWT.LEFT);
		TableColumn tc4 = new TableColumn(DirectoryListing, SWT.LEFT);
		TableColumn tc5 = new TableColumn(DirectoryListing, SWT.LEFT);
		tc1.setText("Filename");
		tc2.setText("TR-Dos Filename");
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

		Label l = new Label(shell, SWT.LEFT);
		l.setText("Defaults.");
		FontData fontData = l.getFont().getFontData()[0];
		Font font = new Font(shell.getDisplay(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		l.setFont(font);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		l.setLayoutData(gd);

		l = new Label(shell, SWT.LEFT);
		l.setText("BASIC files:");
		l.setFont(font);

		l = new Label(shell, SWT.LEFT);
		l.setText("Line (32788 = none):");

		StartLine = new Text(shell, SWT.BORDER);
		StartLine.setText("32768");

		new Label(shell, SWT.NONE);
		l = new Label(shell, SWT.LEFT);
		l.setText("Image files:");
		l.setFont(font);

		IsBWCheck = new Button(shell, SWT.CHECK);
		IsBWCheck.setText("Monochrome");
		IsBWCheck.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ReRenderImage();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

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
			}
		});
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		intensitySlider.setLayoutData(gd);

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
		Btn = new Button(shell, SWT.PUSH);
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

		shell.pack();
		IntensityLabel.setText("Cutoff: 50%");
	}

	/**
	 * Modify the given filename so its unique in the current selection. Note, this
	 * has a limitation that it will probably not work properly over >999 files, but
	 * that is more than the default number of dirents (511), so *should* be ok.
	 * 
	 * @param s
	 * @return
	 */
	private String UniqueifyName(String s) {
		String result = s;

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
		for (TrdDirectoryEntry d : CurrentPartition.DirectoryEntries) {
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

	protected void ReRenderImage() {
		// If selected file is an image and is selected...
		if (ImageLabel != null) {
			if (DirectoryListing.getSelectionCount() > 0) {
				// Get the image details.
				TableItem SelectedFile = DirectoryListing.getSelection()[0];
				NewFileListItem details = (NewFileListItem) SelectedFile.getData();
				// Render the image
				byte buffer[] = ScaleImage(shell.getDisplay(), intensitySlider.getSelection(), details.OriginalImage,
						IsBWCheck.getSelection());
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
	 * Scale the loaded image to 256x192 into a new image, run the speccy display
	 * conversion, then return it as a SWT compatable image.
	 * 
	 * @param selected
	 * @return
	 */
	public byte[] ScaleImage(Display display, int IntensityCutoff, BufferedImage RawImage, boolean isBW) {
		BufferedImage TargetImg = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
		byte result[] = null;
		if (RawImage != null) {
			// Draw the loaded image to the new buffer
			Graphics2D graphics2D = TargetImg.createGraphics();
			graphics2D.drawImage(RawImage, 0, 0, 256, 192, null);
			graphics2D.dispose();
			// process it.

			if (isBW) {
				result = RenderBW(TargetImg, IntensityCutoff);
			} else {
				result = RenderColour(TargetImg, IntensityCutoff);
			}
		}
		return result;
	}

	/**
	 * Render the currently loaded image into a Spectrum compatible Coloured
	 * bufferedimage. The conversion works, but is not very good. There are better
	 * libraries available. will use a decent one later.
	 * 
	 * @param SourceImage  - Image to convert
	 * @param CutoffSlider - Colour cutoff (0->255)
	 * @return - Screen$
	 */
	private byte[] RenderColour(BufferedImage SourceImage, int CutoffSlider) {
		byte Screen[] = new byte[6912];
		int compval = (CutoffSlider * 256) / 100;
		// scale to 8 colours
		for (int x = 0; x < 256; x++) {
			for (int y = 0; y < 192; y++) {
				int col = SourceImage.getRGB(x, y);
				int red = (col & 0xff0000) >> 16;
				int green = (col & 0xff00) >> 8;
				int blue = col & 0xff;

				if (red > compval) {
					red = 0xff;
				} else {
					red = 0x00;
				}
				if (green > compval) {
					green = 0xff;
				} else {
					green = 0x00;
				}
				if (blue > compval) {
					blue = 0xff;
				} else {
					blue = 0x00;
				}

				col = (red << 16) + (green << 8) + blue;
				SourceImage.setRGB(x, y, col);
			}
		}
		// Group into attributes
		int attriblocation = 0x1800;
		int colours[] = new int[8];
		for (int y = 0; y < 24; y++) {
			for (int x = 0; x < 32; x++) {
				// Blank the colour indexes
				for (int i = 0; i < 8; i++) {
					colours[i] = 0;
				}
				// base positions.
				int basex = x * 8;
				int basey = y * 8;
				// get the square
				for (int a = 0; a < 7; a++) {
					for (int b = 0; b < 7; b++) {
						// col = 00000000 RRRRRRRR GGGGGGGG BBBBBBBB
						// Speccy = 00000GRB
						int col = SourceImage.getRGB(basex + a, basey + b);
						int red = (col >> 16) & 0x02;
						int green = (col >> 8) & 0x04;
						int blue = (col & 0x01);

						col = red + green + blue;
						colours[col]++;
					}
				}
				// find the max and max-1
				int ink = 0;
				int paper = 0;

				int maxnum = 0;
				for (int i = 0; i < 8; i++) {
					if (colours[i] > maxnum) {
						ink = i;
						maxnum = colours[i];
					}
				}

				colours[ink] = 0;
				maxnum = 0;
				for (int i = 0; i < 8; i++) {
					if (colours[i] > maxnum) {
						paper = i;
						maxnum = colours[i];
					}
				}
				if (maxnum == 0) {
					paper = ink;
				}
				// make an array of colours
				int newcolours[] = new int[8];
				for (int i = 0; i < 8; i++) {
					newcolours[i] = Speccy.colours[ink];
				}
				newcolours[paper] = Speccy.colours[paper];

				// rewrite the square
				Screen[attriblocation++] = (byte) (ink + (paper * 8));
				for (int a = 0; a < 8; a++) {
					int byt = 0;
					for (int b = 0; b < 8; b++) {
						int col = SourceImage.getRGB(basex + b, basey + a);
						int red = (col >> 16) & 0x02;
						int green = (col >> 8) & 0x04;
						int blue = (col & 0x01);
						col = red + green + blue;

						int newcol = newcolours[col];

						SourceImage.setRGB(basex + b, basey + a, newcol);
						// calculate if we are ink or paper.
						byt = byt << 1;
						if (newcol == Speccy.colours[ink]) {
							byt = byt + 1;
						}
					}
					// calculate the pixel data location [ 000 aabbb cccxxxxx ] where yptn =
					// [aacccbbb]
					int yptn = basey + a;
					int y1 = yptn & 0x07;
					int y2 = (yptn & 0x38) >> 3;
					int y3 = (yptn & 0xc0) >> 6;
					int address = (y3 << 11) + (y1 << 8) + (y2 << 5) + (basex >> 3);
					// write the pixel data
					Screen[address] = (byte) (byt & 0xff);

				}
			}
		}
		return (Screen);

	}

	/**
	 * Render the currently loaded images as a black and white image. Note RGB->lum
	 * values are from ITU BT.601.
	 * 
	 * @param SourceImage - Image to render
	 * @param bwSlider    - Intensity cutoff (0-255)
	 * @return - Screen$
	 */
	private byte[] RenderBW(BufferedImage SourceImage, int bwSlider) {
		byte Screen[] = new byte[6912];
		// store for pixel data
		boolean pixels[] = new boolean[49152];
		int pxIdx = 0;

		// loop every pixel.
		for (int y = 0; y < 192; y++) {
			for (int x = 0; x < 256; x++) {
				// get the RGB values
				int col = SourceImage.getRGB(x, y);
				int red = (col & 0xff0000) >> 16;
				int green = (col & 0xff00) >> 8;
				int blue = col & 0xff;

				// convert into a luminance (Greyscale) value.
				double lum = (0.299 * red) + (0.587 * green) + (0.114 * blue);
				int iLum = (int) Math.round(lum * 100 / 256);

				// See if the Luminance crosses the value, if so set the local image.
				if (iLum > bwSlider) {
					iLum = 0xffffff;
					pixels[pxIdx++] = false;
				} else {
					iLum = 0x00;
					pixels[pxIdx++] = true;
				}

				SourceImage.setRGB(x, y, iLum);
			}
		}

		pxIdx = 0;
		for (int y = 0; y < 192; y++) {
			// calculate the pixel data location [ 000 aabbb cccxxxxx ] where yptn =
			// [aacccbbb]
			int y1 = y & 0x07;
			int y2 = (y & 0x38) >> 3;
			int y3 = (y & 0xc0) >> 6;
			int baseYAddress = (y3 << 11) + (y1 << 8) + (y2 << 5);

			// write the line
			for (int x = 0; x < 32; x++) {
				int byt = 0;
				for (int b = 0; b < 8; b++) {
					boolean px = pixels[pxIdx++];
					int col = 0;
					if (px) {
						col = 1;
					}
					byt = (byt << 1) + col;
				}
				int address = baseYAddress + x;
				// write the pixel data
				Screen[address] = (byte) (byt & 0xff);
			}
		}
		// make the entire attribute area black on white.
		byte wob = Speccy.ToAttribute(Speccy.COLOUR_BLACK, Speccy.COLOUR_WHITE, false, false);
		for (int i = 0x1800; i < 0x1b00; i++) {
			Screen[i] = wob;
		}

		return (Screen);
	}

	protected void DoAddImageFiles() {
		FileDialog fd = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		fd.setText("Open Image file");
		String[] filterExt = { "*" };
		fd.setFilterExtensions(filterExt);
		if ((fd.open() != null) && (fd.getFileNames().length > 0)) {
			for (String filename : fd.getFileNames()) {
				File FilePath = new File(fd.getFilterPath());
				File filedets = new File(FilePath, filename);

				BufferedImage RawImage;
				try {
					/*
					 * Load the image
					 */
					RawImage = ImageIO.read(filedets);

					/*
					 * Convert and scale the image
					 */
					int bwslider = intensitySlider.getSelection();
					byte buffer[] = ScaleImage(shell.getDisplay(), bwslider, RawImage, IsBWCheck.getSelection());

					/*
					 * Create the row texts.
					 */
					String DosFileName = UniqueifyName(TRDOS.FixFullName(filename));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filedets.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "C (scr)";
					values[3] = String.valueOf(buffer.length);
					values[4] = "";

					/*
					 * Create the data storage object. Note, we store the original image as well as
					 * the buffer
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filedets;
					listitem.filename = DosFileName;
					listitem.FileType = FILETYPE_SCREEN;
					listitem.data = buffer;
					listitem.OriginalImage = RawImage;
					listitem.Intensity = bwslider;
					listitem.IsBlackWhite = IsBWCheck.getSelection();

					/*
					 * Add the row.
					 */
					item2.setText(values);
					item2.setData(listitem);
				} catch (IOException e) {
					System.out.println("Failed to add " + filedets.getAbsolutePath() + " " + e.getMessage());
				}
			}
		}
	}

	protected void DoSelectedFileChange() {
		if (DirectoryListing.getSelectionCount() > 0) {
			/*
			 * Get the first selected file
			 */
			TableItem SelectedFile = DirectoryListing.getSelection()[0];
			NewFileListItem details = (NewFileListItem) SelectedFile.getData();
			/*
			 * Remove the old components
			 */
			for (Control child : MainPage.getChildren()) {
				child.dispose();
			}
			ImageLabel = null;
			MainPage.pack();

			/*
			 * Get the file type details.
			 */
			char treatAs = details.FileType;
			byte data[] = details.data;

			/*
			 * Actually render the file
			 */
			switch (treatAs) {
			case FILETYPE_BASIC:
				RenderBasic(data, details);
				break;
			case FILETYPE_CHRARRAY:
				RenderChrArray(data);
				break;
			case FILETYPE_NUMARRAY:
				RenderNumArray(data);
				break;
			case FILETYPE_CODE:
				RenderCode(data);
				break;
			case FILETYPE_SCREEN:
				RenderScreen(data, details);
				break;
			}

			MainPage.pack();
			shell.pack();
		}
	}

	/**
	 * Render the currently selected file as a screen.
	 * 
	 * @param data
	 */
	private void RenderScreen(byte data[], NewFileListItem details) {
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
	 * 
	 * @param data
	 */
	private void RenderCode(byte[] data) {
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
		int Address = 0;

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

	private void RenderBasic(byte data[], NewFileListItem details) {
		StringBuilder sb = new StringBuilder();
		Speccy.DecodeBasicFromLoadedFile(data, sb, data.length, false, false);

		Text t = new Text(MainPage, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 4;
		gd.verticalSpan = 6;
		gd.minimumHeight = 198;
		gd.minimumWidth = 500;
		t.setLayoutData(gd);
		t.setText(sb.toString());
	}

	protected void DoAddCodeFiles() {
		FileDialog fd = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		fd.setText("Open CODE file");
		String[] filterExt = { "*" };
		fd.setFilterExtensions(filterExt);
		if ((fd.open() != null) && (fd.getFileNames().length > 0)) {
			/*
			 * Iterate all the files selected.
			 */
			for (String filename : fd.getFileNames()) {
				/*
				 * Load the file
				 */
				File FilePath = new File(fd.getFilterPath());
				File filedets = new File(FilePath, filename);

				byte buffer[] = new byte[(int) filedets.length()];
				FileInputStream is = null;
				try {
					try {
						is = new FileInputStream(filedets);
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
				String DosFileName = UniqueifyName(TRDOS.FixFullName(filename));
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String values[] = new String[5];
				values[0] = filedets.getAbsolutePath();
				values[1] = DosFileName;
				values[2] = "Code (Raw Manual)";
				values[3] = String.valueOf(buffer.length);
				values[4] = "";

				/*
				 * Create the data storage object
				 */
				NewFileListItem listitem = new NewFileListItem();
				listitem.OriginalFilename = filedets;
				listitem.filename = DosFileName;
				listitem.FileType = FILETYPE_CODE;
				listitem.data = buffer;

				/*
				 * Add the row
				 */
				item2.setText(values);
				item2.setData(listitem);
			}
		}

	}

	protected void DoAddCharacterFiles() {
		int filelimit = 16384;
		FileDialog fd = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		fd.setText("Open CSV file");
		String[] filterExt = { "*" };
		fd.setFilterExtensions(filterExt);
		if ((fd.open() != null) && (fd.getFileNames().length > 0)) {
			for (String filename : fd.getFileNames()) {
				/*
				 * Iterate all the selected files.
				 */
				File FilePath = new File(fd.getFilterPath());
				File filedets = new File(FilePath, filename);
				ArrayList<String> lines = new ArrayList<String>();
				try {
					int numlines = 0;
					String CSVline;
					BufferedReader br = new BufferedReader(new FileReader(filedets));
					try {
						while (((CSVline = br.readLine()) != null) && numlines < filelimit) {
							lines.add(CSVline);
							numlines++;
						}
					} finally {
						br.close();
					}
					if (numlines == filelimit) {
						System.out.println("Load stopped at " + filelimit + " lines. Too large");
					} else {
						System.out.println("Loaded " + numlines + " lines.");
					}

					// get second dimension from the file.
					int maxdim2 = 1;
					for (String line : lines) {
						String columns[] = SplitLine(line, ", \t", 1);
						if (columns.length > maxdim2)
							maxdim2 = columns.length;
					}
					System.out.println("Number of columns is: " + maxdim2);

					// number of diumensions.
					int dimensions = 1;
					if (maxdim2 > 1) {
						dimensions = 2;
					}

					// dimensions. ((X * Y) Data) + 2 bytes for each dimension + 1 for dimension no.
					int arraysize = (maxdim2 * lines.size()) + (dimensions * 2) + 1;
					System.out.println("Calcsize:  " + arraysize);

					byte ArrayAsBytes[] = new byte[arraysize];

					// dimensions.

					int ptr = 1;
					// Each dimension.
					ArrayAsBytes[ptr++] = (byte) (lines.size() & 0xff);
					ArrayAsBytes[ptr++] = (byte) (lines.size() / 0x100);
					ArrayAsBytes[0] = 1;
					if (maxdim2 > 1) {
						ArrayAsBytes[0] = 2;
						ArrayAsBytes[ptr++] = (byte) (maxdim2 & 0xff);
						ArrayAsBytes[ptr++] = (byte) (maxdim2 / 0x100);
					}

					// for each item.
					for (int dim1 = 0; dim1 < lines.size(); dim1++) {
						// pad line to at least 255 characters
						String line = lines.get(dim1)
								+ "                                                                "
								+ "                                                                "
								+ "                                                                "
								+ "                                                                ";
						// write string to the array
						for (int dim2 = 0; dim2 < maxdim2; dim2++) {
							char c = line.charAt(dim2);
							ArrayAsBytes[ptr++] = (byte) c;
						}
					}

					/*
					 * Create the text strings for the row.
					 */
					String DosFileName = UniqueifyName(TRDOS.FixFullName(filename));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filedets.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "D (Character Array)";
					values[3] = String.valueOf(ArrayAsBytes.length);
					values[4] = "";

					/*
					 * Create the data object
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filedets;
					listitem.filename = DosFileName;
					listitem.FileType = FILETYPE_CHRARRAY;
					listitem.data = ArrayAsBytes;

					/*
					 * Add the row
					 */
					item2.setText(values);
					item2.setData(listitem);
				} catch (IOException e) {
					System.out.println("Failed to add " + filedets.getAbsolutePath() + " " + e.getMessage());
				}
			}
		}

	}

	protected void DoAddNumericArrays() {
		int filelimit = 16384;
		FileDialog fd = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		fd.setText("Open CSV file");
		String[] filterExt = { "*" };
		fd.setFilterExtensions(filterExt);
		if ((fd.open() != null) && (fd.getFileNames().length > 0)) {
			/*
			 * Iterate all the returned files.
			 */
			for (String filename : fd.getFileNames()) {
				File FilePath = new File(fd.getFilterPath());
				File filedets = new File(FilePath, filename);
				ArrayList<String> lines = new ArrayList<String>();
				try {
					/*
					 * Load the file into an array of lines.
					 */
					int numlines = 0;
					String CSVline;
					BufferedReader br = new BufferedReader(new FileReader(filedets));
					try {
						while (((CSVline = br.readLine()) != null) && numlines < filelimit) {
							lines.add(CSVline);
							numlines++;
						}
					} finally {
						br.close();
					}
					if (numlines == filelimit) {
						System.out.println("Load stopped at " + filelimit + " lines. Too large");
					} else {
						System.out.println("Loaded " + numlines + " lines.");
					}

					// get second dimension from the file.
					int maxdim2 = 1;
					for (String line : lines) {
						String columns[] = SplitLine(line, ", \t", 1);
						if (columns.length > maxdim2)
							maxdim2 = columns.length;
					}
					System.out.println("Number of columns is: " + maxdim2);

					// number of diumensions.
					int dimensions = 1;
					if (maxdim2 > 1) {
						dimensions = 2;
					}

					int arraysize = (maxdim2 * lines.size() * 5) + (dimensions * 2) + 1;
					System.out.println("Calcsize:  " + arraysize);

					byte ArrayAsBytes[] = new byte[arraysize];

					// dimensions.

					int ptr = 1;
					// Each dimension.
					ArrayAsBytes[ptr++] = (byte) (lines.size() & 0xff);
					ArrayAsBytes[ptr++] = (byte) (lines.size() / 0x100);
					ArrayAsBytes[0] = 1;
					if (maxdim2 > 1) {
						ArrayAsBytes[0] = 2;
						ArrayAsBytes[ptr++] = (byte) (maxdim2 & 0xff);
						ArrayAsBytes[ptr++] = (byte) (maxdim2 / 0x100);
					}

					// for each item.
					for (int dim1 = 0; dim1 < lines.size(); dim1++) {
						String line = lines.get(dim1);
						String numbers[] = SplitLine(line, ", \t", maxdim2);
						for (int dim2 = 0; dim2 < maxdim2; dim2++) {
							String sNumber = numbers[dim2];
							if (!isNumeric(sNumber)) {
								sNumber = "0";
							}
							Double number = Double.valueOf(sNumber);
							byte[] newnum = Speccy.EncodeValue(number, true);
							for (int i = 1; i < newnum.length; i++) {
								ArrayAsBytes[ptr++] = newnum[i];
							}
						}
					}
					System.out.println("final ptr: " + ptr);

					/*
					 * Create the row text items
					 */
					String DosFileName = UniqueifyName(TRDOS.FixFullName(filename));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filedets.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "(D) Number Array";
					values[3] = String.valueOf(ArrayAsBytes.length);
					values[4] = "";

					/*
					 * Populate the data object
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filedets;
					listitem.filename = DosFileName;
					listitem.FileType = FILETYPE_NUMARRAY;
					listitem.data = ArrayAsBytes;

					/*
					 * Add the row
					 */
					item2.setText(values);
					item2.setData(listitem);
				} catch (IOException e) {
					System.out.println("Failed to add " + filedets.getAbsolutePath() + " " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Check to see if a given string is numeric.
	 * 
	 * @param strNum
	 * @return
	 */
	public static boolean isNumeric(String strNum) {
		if (strNum == null) {
			return false;
		}
		try {
			Double.parseDouble(strNum);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	/**
	 * very basic parser. Will take into account quotes.
	 *
	 * @param line    - Line to split
	 * @param splitby - list of characters to use as delimiters
	 * @return - Array of strings.
	 */
	private String[] SplitLine(String line, String splitby, int padto) {
		ArrayList<String> al = new ArrayList<String>();
		String curritem = "";
		boolean InQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (InQuotes) {
				if (c == '"') {
					InQuotes = false;
					if (!curritem.isEmpty()) {
						al.add(curritem);
						curritem = "";
					}
				} else {
					curritem = curritem + c;
				}
			} else {
				if (c == '"') {
					InQuotes = true;
					if (!curritem.isEmpty()) {
						al.add(curritem);
						curritem = "";
					}
				} else {
					if (splitby.indexOf(c) > -1) {
						if (!curritem.isEmpty()) {
							al.add(curritem);
							curritem = "";
						}
					} else {
						curritem = curritem + c;
					}

				}
			}
		}

		while (al.size() < padto) {
			al.add("");
		}
		String result[] = al.toArray(new String[0]);

		return result;
	}

	protected void DoAddBinaryBasicFiles() {
		FileDialog fd = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		fd.setText("Open");
		String[] filterExt = { "*" };
		fd.setFilterExtensions(filterExt);
		if ((fd.open() != null) && (fd.getFileNames().length > 0)) {
			for (String filename : fd.getFileNames()) {
				File FilePath = new File(fd.getFilterPath());
				File filedets = new File(FilePath, filename);

				/*
				 * Load the file
				 */
				byte buffer[] = new byte[(int) filedets.length()];
				FileInputStream is = null;
				try {
					try {
						is = new FileInputStream(filedets);
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
				String DosFileName = UniqueifyName(TRDOS.FixFullName(filename));
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String values[] = new String[5];
				values[0] = filedets.getAbsolutePath();
				values[1] = DosFileName;
				values[2] = "Basic (Raw Manual)";
				values[3] = String.valueOf(buffer.length);
				values[4] = "";

				/*
				 * Create the storage object and add it to the row
				 */
				NewFileListItem listitem = new NewFileListItem();
				listitem.OriginalFilename = filedets;
				listitem.filename = DosFileName;
				listitem.FileType = FILETYPE_BASIC;
				listitem.data = buffer;
				listitem.line = Integer.valueOf(StartLine.getText());

				/*
				 * Create the table row
				 */
				item2.setText(values);
				item2.setData(listitem);
			}
		}
	}

	protected void DoAddTextBasicFiles() {
		FileDialog fd = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		fd.setText("Open");
		String[] filterExt = { "*" };
		fd.setFilterExtensions(filterExt);
		if ((fd.open() != null) && (fd.getFileNames().length > 0)) {
			/*
			 * Iterate all files selected...
			 */
			for (String filename : fd.getFileNames()) {
				File FilePath = new File(fd.getFilterPath());
				File filedets = new File(FilePath, filename);
				BufferedReader br;
				/*
				 * Read the file and tokenise it. Note, no syntax checking is done, so if your
				 * basic is invalid, it will still be added
				 */
				byte BasicAsBytes[] = new byte[0xffff];
				int targetPtr = 0;
				try {
					br = new BufferedReader(new FileReader(filedets));
					try {
						String line;
						while ((line = br.readLine()) != null) {
							targetPtr = Speccy.DecodeBasicLine(line, BasicAsBytes, targetPtr);
						}
					} finally {
						br.close();
					}
				} catch (FileNotFoundException e) {
					System.out.println("File " + filedets.getAbsolutePath() + " cannot be opened.");
				} catch (IOException e) {
					System.out.println("File " + filedets.getAbsolutePath() + " IO Error: " + e.getMessage());
				}

				// Copy to an array of the correct size.
				byte data[] = new byte[targetPtr];
				System.arraycopy(BasicAsBytes, 0, data, 0, targetPtr);

				/*
				 * Make the values required for the table item.
				 */
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String values[] = new String[5];
				values[0] = filedets.getAbsolutePath();
				values[1] = UniqueifyName(TRDOS.FixFullName(filename));
				values[2] = "Basic (Manual)";
				values[3] = String.valueOf(targetPtr);
				values[4] = "";

				/*
				 * Populate the storage array details.
				 */
				NewFileListItem listitem = new NewFileListItem();
				listitem.OriginalFilename = filedets;
				listitem.filename = values[1];
				listitem.FileType = FILETYPE_BASIC;
				listitem.data = data;
				listitem.line = Integer.valueOf(StartLine.getText());

				/*
				 * Add to the table
				 */
				item2.setText(values);
				item2.setData(listitem);
			}
		}

	}

	protected void DoAddFiles() {
		TableItem files[] = DirectoryListing.getItems();
		for (TableItem file : files) {
			NewFileListItem details = (NewFileListItem) file.getData();
			try {
				// Default variable name for arrays
				switch (details.FileType) {
				case FILETYPE_BASIC:
					CurrentPartition.AddBasicFile(details.filename, details.data, details.line, details.data.length);
					break;
				case FILETYPE_CHRARRAY:
					CurrentPartition.AddCharArray(details.filename, details.data);
					break;
				case FILETYPE_NUMARRAY:
					CurrentPartition.AddNumericArray(details.filename, details.data);
					break;
				case FILETYPE_CODE:
					// for CODE files, put at the top of memory
					int startaddress = 0x10000 - details.data.length;
					CurrentPartition.AddCodeFile(details.filename, startaddress, details.data);
					break;
				case FILETYPE_SCREEN:
					// For Screen$ files, these start at 16384 (0x4000)
					CurrentPartition.AddCodeFile(details.filename, 0x4000, details.data);
					break;
				case FILETYPE_STREAM:
					CurrentPartition.AddCodeFile(details.filename, 0x0000, details.data);
					break;
				}
			} catch (Exception e) {
				System.out.println(
						"Error adding " + details.OriginalFilename + " as " + details.filename + " " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Render Character array
	 * 
	 * @param data
	 */
	private void RenderChrArray(byte data[]) {
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
	private void RenderNumArray(byte data[]) {
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

}

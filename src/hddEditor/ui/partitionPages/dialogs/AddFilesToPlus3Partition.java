package hddEditor.ui.partitionPages.dialogs;

/**
 * Add files to the +3DOS partition
 */

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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

import hddEditor.libs.CPM;
import hddEditor.libs.Speccy;
import hddEditor.libs.SpeccyFileEncoders;
import hddEditor.libs.partitions.PLUS3DOSPartition;
import hddEditor.libs.partitions.cpm.DirectoryEntry;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;

public class AddFilesToPlus3Partition {
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
	private Text StartAddress = null;

	/*
	 * Current disk.
	 */
	private PLUS3DOSPartition CurrentPartition = null;

	/*
	 * File types. Note, 0-3 correspond to the normal speccy file types, 4 and 5 are
	 * types used to denote additional processing.
	 */
	private final static int FILETYPE_BASIC = 0;
	private final static int FILETYPE_NUMARRAY = 1;
	private final static int FILETYPE_CHRARRAY = 2;
	private final static int FILETYPE_CODE = 3;
	private final static int FILETYPE_SCREEN = 4;
	private final static int FILETYPE_CPM = 5;

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
		public int FileType = FILETYPE_CPM;
		// If the file is an image file, this contains the original image. Used so the
		// user can edit it.
		public BufferedImage OriginalImage = null;
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
	 * Constructor
	 * 
	 * @param display
	 */
	public AddFilesToPlus3Partition(Display display) {
		this.display = display;
	}

	/**
	 * Show the dialog
	 * 
	 * @param title
	 * @param p3d
	 */
	public void Show(String title, PLUS3DOSPartition p3d) {
		CurrentPartition = p3d;
		Createform(title);
		loop();
	}

	/**
	 * Create the components on the form.
	 * 
	 * @param title
	 */
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
		Btn.setText("Select +3 Files with headers");
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoAddPlus3Files();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Btn = new Button(shell, SWT.PUSH);
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
				DoAddBinaryFiles();
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
		Btn.setText("Select CPM file");
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoAddCPMFiles();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

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
				}
			}

			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});

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
	 * Select files to be added. The files should have the 128 byte Spectrum +3DOS
	 * header.
	 */
	protected void DoAddPlus3Files() {
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
				byte HeaderBuffer[] = new byte[0x80];
				byte data[] = null;
				InputStream is = null;
				try {
					/*
					 * Open the file and load it.
					 */
					Plus3DosFileHeader p3d = null;
					File filedets = new File(FilePath, filename);
					try {
						is = new FileInputStream(filedets);
						int numRead = is.read(HeaderBuffer);
						if (numRead < HeaderBuffer.length) {
							byte newbuffer[] = new byte[numRead];
							System.arraycopy(HeaderBuffer, 0, newbuffer, 0, numRead);
							HeaderBuffer = newbuffer;
						}
						p3d = new Plus3DosFileHeader(HeaderBuffer);
						data = new byte[p3d.filelength];
						numRead = is.read(data);
						if (numRead < data.length) {
							System.out.println("File terminated before +3DOS header says it should.");
						}
					} finally {
						if (is != null)
							is.close();
					}
					/*
					 * Try to identify the file type.
					 */
					// Check for a +3DOS header
					if (p3d.IsPlusThreeDosFile) {
						String DosFileName = UniqueifyName(CPM.FixFullName(filename));
						String filetypeName = p3d.getTypeDesc() + "(+3Dos Header)";

						TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
						String values[] = new String[5];
						values[0] = filedets.getAbsolutePath();
						values[1] = DosFileName;
						values[2] = filetypeName;
						values[3] = String.valueOf(p3d.filelength);
						values[4] = "";

						byte newdata[] = new byte[data.length + 0x80];

						System.arraycopy(HeaderBuffer, 0, newdata, 0, 0x80);
						System.arraycopy(data, 0, newdata, 0x80, data.length);

						NewFileListItem listitem = new NewFileListItem();
						listitem.OriginalFilename = filedets;
						listitem.filename = DosFileName;
						listitem.fileheader = p3d;
						listitem.FileType = FILETYPE_CPM;
						listitem.LoadAddress = p3d.loadAddr;
						listitem.data = newdata;

						item2.setText(values);
						item2.setData(listitem);
					} else {
						System.out.println("File " + filename + " does not have a +3DOS header.");
					}
				} catch (FileNotFoundException E) {
					System.out.println("Error reading " + filename + " File not found.");
				} catch (IOException E) {
					System.out.println("Error reading " + filename + " " + E.getMessage());
				}
			}
		}
	}

	/**
	 * Add a text file as a BASIC file.
	 */
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

				byte data[] = SpeccyFileEncoders.EncodeTextFileToBASIC(filedets);

				/*
				 * Make the values required for the table item.
				 */
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String values[] = new String[5];
				values[0] = filedets.getAbsolutePath();
				values[1] = UniqueifyName(CPM.FixFullName(filename));
				;
				values[2] = "Basic (Manual)";
				values[3] = String.valueOf(data.length);
				values[4] = "";

				/*
				 * Populate the storage array details.
				 */
				NewFileListItem listitem = new NewFileListItem();
				listitem.OriginalFilename = filedets;
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
				String DosFileName = UniqueifyName(CPM.FixFullName(filename));
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

	/**
	 * Add BINARY file(s) as a CODE file.
	 */
	protected void DoAddBinaryFiles() {

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
				String DosFileName = UniqueifyName(CPM.FixFullName(filename));
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
	 * Add file(s) as raw CPM files (EG, Headerless).
	 */
	protected void DoAddCPMFiles() {
		FileDialog fd = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		fd.setText("Open CPM file");
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
				 * Create the Row texts
				 */
				String DosFileName = UniqueifyName(CPM.FixFullName(filename));
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String values[] = new String[5];
				values[0] = filedets.getAbsolutePath();
				values[1] = DosFileName;
				values[2] = "CPM";
				values[3] = String.valueOf(buffer.length);
				values[4] = "";

				/*
				 * Create the data storage object
				 */
				NewFileListItem listitem = new NewFileListItem();
				listitem.OriginalFilename = filedets;
				listitem.filename = DosFileName;
				listitem.fileheader = null;
				listitem.FileType = FILETYPE_CPM;
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
	 * Convert and Add image files as SCREEN$ errors Supports all image types
	 * ImageIO supports (PNG, GIF, JPEG, BMP, WEBMP)
	 */
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
					byte buffer[] = SpeccyFileEncoders.ScaleImage(shell.getDisplay(), bwslider, RawImage,
							IsBWCheck.getSelection());

					/*
					 * Create the row texts.
					 */
					String DosFileName = UniqueifyName(CPM.FixFullName(filename));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filedets.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "SCREEN$";
					values[3] = String.valueOf(buffer.length);
					values[4] = "";

					/*
					 * Create the data storage object. Note, we store the original image as well as
					 * the buffer
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filedets;
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
					System.out.println("Failed to add " + filedets.getAbsolutePath() + " " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Add a csv file as a numeric array
	 */
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
				try {
					byte ArrayAsBytes[] = SpeccyFileEncoders.EncodeNumericArray(filedets, filelimit);

					/*
					 * Create the row text items
					 */
					String DosFileName = UniqueifyName(CPM.FixFullName(filename));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filedets.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "Number Array";
					values[3] = String.valueOf(ArrayAsBytes.length);
					values[4] = "";

					/*
					 * Populate the data object
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filedets;
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
					System.out.println("Failed to add " + filedets.getAbsolutePath() + " " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Add a character array, Note, this is just really a text file.
	 */
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
				try {
					byte ArrayAsBytes[] = SpeccyFileEncoders.EncodeCharacterArray(filedets, filelimit);
					/*
					 * Create the text strings for the row.
					 */
					String DosFileName = UniqueifyName(CPM.FixFullName(filename));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filedets.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "Character Array";
					values[3] = String.valueOf(ArrayAsBytes.length);
					values[4] = "";

					/*
					 * Create the data object
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filedets;
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
					System.out.println("Failed to add " + filedets.getAbsolutePath() + " " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Add the selected files to the partition and return This is used when OK is
	 * pressed.
	 */
	protected void DoAddFiles() {
		TableItem files[] = DirectoryListing.getItems();
		for (TableItem file : files) {
			NewFileListItem details = (NewFileListItem) file.getData();
			try {
				// Default variable name for arrays
				char varname = 'A';
				switch (details.FileType) {
				case FILETYPE_CPM:
					CurrentPartition.AddCPMFile(details.filename, details.data);
					break;
				case FILETYPE_BASIC:
					String Startline = StartLine.getText();
					int line = Integer.valueOf(Startline);
					CurrentPartition.AddBasicFile(details.filename, details.data, line, details.data.length);
					break;
				case FILETYPE_CHRARRAY:
					CurrentPartition.AddPlusThreeFile(details.filename, details.data, varname * 0x100, 0,
							Speccy.BASIC_CHRARRAY);
					break;
				case FILETYPE_NUMARRAY:
					CurrentPartition.AddPlusThreeFile(details.filename, details.data, varname * 0x100, 0,
							Speccy.BASIC_NUMARRAY);
					break;
				case FILETYPE_CODE:
					// for CODE files, put at the top of memory
					CurrentPartition.AddCodeFile(details.filename, details.LoadAddress, details.data);
					break;
				case FILETYPE_SCREEN:
					// For Screen$ files, these start at 16384 (0x4000)
					CurrentPartition.AddCodeFile(details.filename, 0x4000, details.data);
					break;
				}
			} catch (IOException e) {
				System.out.println(
						"Error adding " + details.OriginalFilename + " as " + details.filename + " " + e.getMessage());
				e.printStackTrace();
			}
		}

	}

	/**
	 * Update the lower page with the selected file.
	 * 
	 */
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
			int treatAs = details.FileType;
			byte data[] = details.data;

			/*
			 * For files that already have a +3DOS header, Convert them into the proper type
			 * for rendering.
			 */
			if (details.FileType == FILETYPE_CPM) {
				Plus3DosFileHeader pfd = details.fileheader;
				if (pfd != null) {
					// Remove the +3DOS for the purposes of rendering the file.
					byte newdata[] = new byte[data.length - 0x80];
					System.arraycopy(data, 0x80, newdata, 0, newdata.length);
					data = newdata;

					// We will treat it as the type of file in the +3DOS header
					treatAs = pfd.filetype;

					// If the file is CODE, and length 6912, treat as a screen.
					if ((pfd.filetype == FILETYPE_CODE) && (pfd.filelength == 6912)) {
						treatAs = FILETYPE_SCREEN;
					}
				}
			}

			if (treatAs == FILETYPE_CODE || treatAs == FILETYPE_SCREEN)
				StartAddress.setText(String.valueOf(details.LoadAddress));

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
			case FILETYPE_CPM:
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
	 * Render a the file as BASIC
	 * 
	 * @param data
	 * @param details
	 */
	private void RenderBasic(byte data[], NewFileListItem details) {
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

	/**
	 * Render the selected file as CODE
	 * 
	 * @param data
	 */
	private void RenderCode(byte data[]) {
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
		for (DirectoryEntry d : CurrentPartition.DirectoryEntries) {
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

}

package hddEditor.ui.partitionPages.dialogs.AddFiles;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.SpeccyFileEncoders;
import hddEditor.libs.TRDOS;
import hddEditor.libs.partitions.TrDosPartition;
import hddEditor.libs.partitions.trdos.TrdDirectoryEntry;

public class AddFilesToTrDosPartition extends GenericAddPageDialog {

	/*
	 * The important components on the form.
	 */
	private Text GenericFileType = null;

	/*
	 * Unlike all other types, for TR-DOS partitions, the
	 * file types are stored as characters rather than integers.
	 */
	private final static char FILETYPE_BASIC = 'B';
	private final static char FILETYPE_NUMARRAY = 'D';
	private final static char FILETYPE_CHRARRAY = 'E';
	private final static char FILETYPE_CODE = 'C';
	private final static char FILETYPE_STREAM = '#';
	private final static char FILETYPE_SCREEN = 'S';


	public AddFilesToTrDosPartition(Display display, FileSelectDialog fsd) {
		super(display, fsd);
	}

	public void Show(String Title, TrDosPartition partition) {
		CurrentPartition = partition;
		Createform(Title);
		loop();
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
		gd.horizontalSpan = 2;
		l.setLayoutData(gd);
		
		l = new Label(shell, SWT.LEFT);
		l.setText("Filename:");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		l.setLayoutData(gd);
		
		Filename = new Text(shell, SWT.LEFT);
		Filename.setText("________.____");
		Filename.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
				TableItem SelectedFiles[] = DirectoryListing.getSelection();
				if (SelectedFiles != null && SelectedFiles.length > 0) {
					NewFileListItem details = (NewFileListItem) SelectedFiles[0].getData();
					if (details != null) {
						details.filename = UniqueifyName(Filename.getText());
						SelectedFiles[0].setText(1, details.filename);
					}
				}
			}

			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});

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
	@Override
	protected String UniqueifyName(String s) {
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
		for (TrdDirectoryEntry d : ((TrDosPartition) CurrentPartition).DirectoryEntries) {
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

	@Override
	protected void DoAddImageFiles() {
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open image file",new String[] {"*"});
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
					byte buffer[] = SpeccyFileEncoders.ScaleImage(shell.getDisplay(), bwslider, RawImage, IsBWCheck.getSelection());

					/*
					 * Create the row texts.
					 */
					String DosFileName = UniqueifyName(TRDOS.FixFullName(filename.getName()));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filename.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "C (scr)";
					values[3] = String.valueOf(buffer.length);
					values[4] = "";

					/*
					 * Create the data storage object. Note, we store the original image as well as
					 * the buffer
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filename;
					listitem.filename = DosFileName;
					listitem.cFileType = FILETYPE_SCREEN;
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
					System.out.println("Failed to add " + filename.getAbsolutePath() + " " + e.getMessage());
				}
			}
		}
		shell.moveAbove(null);
	}

	protected void DoSelectedFileChange() {
		if (DirectoryListing.getSelectionCount() > 0) {
			/*
			 * Get the first selected file
			 */
			TableItem SelectedFile = DirectoryListing.getSelection()[0];
			NewFileListItem details = (NewFileListItem) SelectedFile.getData();
			Filename.setText(details.filename);
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
			char treatAs = details.cFileType;
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
				RenderCode(details);
				break;
			case FILETYPE_SCREEN:
				RenderScreen(data, details);
				break;
			}

			MainPage.pack();
			shell.pack();
			shell.layout(true,true);
			final Point newSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);  
			shell.setSize(newSize);
		}
	}

	protected void DoAddCodeFiles() {
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open CODE file",new String[] {"*"});
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
				 * Create the texts for the Row
				 */
				String DosFileName = UniqueifyName(TRDOS.FixFullName(filename.getName()));
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
				listitem.cFileType = FILETYPE_CODE;
				listitem.data = buffer;

				/*
				 * Add the row
				 */
				item2.setText(values);
				item2.setData(listitem);
			}
		}
		shell.moveAbove(null);
	}

	@Override
	protected void DoAddCharacterFiles() {
		int filelimit = 16384;
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open CODE file",new String[] {"*"});
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
					String DosFileName = UniqueifyName(TRDOS.FixFullName(filename.getName()));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filename.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "D (Character Array)";
					values[3] = String.valueOf(ArrayAsBytes.length);
					values[4] = "";

					/*
					 * Create the data object
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filename;
					listitem.filename = DosFileName;
					listitem.cFileType = FILETYPE_CHRARRAY;
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
		shell.moveAbove(null);
	}

	@Override
	protected void DoAddNumericArrays() {
		int filelimit = 16384;
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open CODE file",new String[] {"*"});
		if ((Selected != null) && (Selected.length > 0)) {
			for (File filename : Selected) {
				try {
					byte ArrayAsBytes[] = SpeccyFileEncoders.EncodeNumericArray(filename, filelimit);

					/*
					 * Create the row text items
					 */
					String DosFileName = UniqueifyName(TRDOS.FixFullName(filename.getName()));
					TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
					String values[] = new String[5];
					values[0] = filename.getAbsolutePath();
					values[1] = DosFileName;
					values[2] = "(D) Number Array";
					values[3] = String.valueOf(ArrayAsBytes.length);
					values[4] = "";

					/*
					 * Populate the data object
					 */
					NewFileListItem listitem = new NewFileListItem();
					listitem.OriginalFilename = filename;
					listitem.filename = DosFileName;
					listitem.cFileType = FILETYPE_NUMARRAY;
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
		shell.moveAbove(null);
	}
	
	@Override
	protected void DoAddBinaryBasicFiles() {
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open CODE file",new String[] {"*"});
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
				String DosFileName = UniqueifyName(TRDOS.FixFullName(filename.getName()));
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
				listitem.cFileType = FILETYPE_BASIC;
				listitem.data = buffer;
				listitem.line = Integer.valueOf(StartLine.getText());

				/*
				 * Create the table row
				 */
				item2.setText(values);
				item2.setData(listitem);
			}
		}
		shell.moveAbove(null);
	}

	@Override
	protected void DoAddTextBasicFiles() {
		File Selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, "Open CODE file",new String[] {"*"});
		if ((Selected != null) && (Selected.length > 0)) {
			for (File filename : Selected) {

				byte data[] = SpeccyFileEncoders.EncodeTextFileToBASIC(filename);

				/*
				 * Make the values required for the table item.
				 */
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String values[] = new String[5];
				values[0] = filename.getAbsolutePath();
				values[1] = UniqueifyName(TRDOS.FixFullName(filename.getName()));
				values[2] = "Basic (Manual)";
				values[3] = String.valueOf(data.length);
				values[4] = "";

				/*
				 * Populate the storage array details.
				 */
				NewFileListItem listitem = new NewFileListItem();
				listitem.OriginalFilename = filename;
				listitem.filename = values[1];
				listitem.cFileType = FILETYPE_BASIC;
				listitem.data = data;
				listitem.line = Integer.valueOf(StartLine.getText());

				/*
				 * Add to the table
				 */
				item2.setText(values);
				item2.setData(listitem);
			}
		}
		shell.moveAbove(null);
	}

	protected void DoAddFiles() {
		TableItem files[] = DirectoryListing.getItems();
		for (TableItem file : files) {
			NewFileListItem details = (NewFileListItem) file.getData();
			try {
				// Default variable name for arrays
				switch (details.cFileType) {
				case FILETYPE_BASIC:
					CurrentPartition.AddBasicFile(details.filename, details.data, details.line, details.data.length);
					break;
				case FILETYPE_CHRARRAY:
					CurrentPartition.AddCharArray(details.filename, details.data,"A");
					break;
				case FILETYPE_NUMARRAY:
					CurrentPartition.AddNumericArray(details.filename, details.data,"A");
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
}

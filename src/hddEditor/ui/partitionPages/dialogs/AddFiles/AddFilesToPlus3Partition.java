package hddEditor.ui.partitionPages.dialogs.AddFiles;

/**
 * Add files to the +3DOS partition
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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

import hddEditor.libs.CPM;
import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Languages;
import hddEditor.libs.Speccy;
import hddEditor.libs.partitions.PLUS3DOSPartition;
import hddEditor.libs.partitions.cpm.CPMDirectoryEntry;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;

public class AddFilesToPlus3Partition extends GenericAddPageDialog {

	/*
	 * Extra File type. Note, 0-3 correspond to the normal speccy file types, 4 is
	 * IMAGE which is a special case of CODE.
	 */
	private final static int FILETYPE_CPM = 5;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public AddFilesToPlus3Partition(Display display, FileSelectDialog fsd, Languages lang) {
		super(display, fsd, lang);
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
		Btn.setText(lang.Msg(Languages.MSG_SELPLUS3FILES));
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
		Btn.setText(lang.Msg(Languages.MSG_SELECTTXTBASICFILE));
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
		Btn.setText(lang.Msg(Languages.MSG_SELECTBINBASICFILE));
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
		Btn.setText(lang.Msg(Languages.MSG_SELECTCODEFILE));
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
		Btn.setText(lang.Msg(Languages.MSG_SELECTIMAGEFILE));
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
		Btn.setText(lang.Msg(Languages.MSG_SELECTNUMFILE));
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
		Btn.setText(lang.Msg(Languages.MSG_SELECTCHARFILE));
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
		Btn.setText(lang.Msg(Languages.MSG_SELCPMFILE));
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
		tc1.setText(lang.Msg(Languages.MSG_FILENAME));
		tc2.setText(lang.Msg(Languages.MSG_PLUS3FILES));
		tc3.setText(lang.Msg(Languages.MSG_FILETYPE));
		tc4.setText(lang.Msg(Languages.MSG_LENGTH));
		tc5.setText(lang.Msg(Languages.MSG_FLAGS));
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
		l.setText(lang.Msg(Languages.MSG_DEFAULTS) + ".");
		FontData fontData = l.getFont().getFontData()[0];
		Font font = new Font(shell.getDisplay(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		l.setFont(font);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		l.setLayoutData(gd);

		l = new Label(shell, SWT.LEFT);
		l.setText(lang.Msg(Languages.MSG_FILENAME) + ":");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		l.setLayoutData(gd);

		Filename = new Text(shell, SWT.LEFT);
		Filename.setText("_________");
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
		l.setText(lang.Msg(Languages.MSG_BASICFILES) + ":");
		l.setFont(font);

		l = new Label(shell, SWT.LEFT);
		l.setText(lang.Msg(Languages.MSG_STARTLINE) + " (32788 = none):");

		StartLine = new Text(shell, SWT.BORDER);
		StartLine.setText("32768");
		StartLine.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
				TableItem SelectedFiles[] = DirectoryListing.getSelection();
				if (SelectedFiles != null && SelectedFiles.length > 0) {
					NewFileListItem details = (NewFileListItem) SelectedFiles[0].getData();
					if (details != null) {
						try {
							details.line = Integer.valueOf(StartLine.getText());
						} catch (NumberFormatException E) {
						}
					}
				}
			}

			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});

		new Label(shell, SWT.NONE);
		l = new Label(shell, SWT.LEFT);
		l.setText(lang.Msg(Languages.MSG_IMAGEFILES) + ":");
		l.setFont(font);

		IsBWCheck = new Button(shell, SWT.CHECK);
		IsBWCheck.setText(lang.Msg(Languages.MSG_MONOCHROME));
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
		l.setText(lang.Msg(Languages.MSG_CODELOADADD) + ": ");
		StartAddress = new Text(shell, SWT.NONE);
		StartAddress.setText("32768");
		StartAddress.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
				TableItem SelectedFiles[] = DirectoryListing.getSelection();
				if (SelectedFiles != null && SelectedFiles.length > 0) {
					NewFileListItem details = (NewFileListItem) SelectedFiles[0].getData();
					if (details != null) {
						try {
							details.LoadAddress = Integer.valueOf(StartAddress.getText());
						} catch (NumberFormatException E) {
						}
					}
				}
			}

			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});

		new Label(shell, SWT.NONE);

		Label IntensityLabel = new Label(shell, SWT.LEFT);
		IntensityLabel.setText(lang.Msg(Languages.MSG_CUTOFF) + ": 100%");

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
				IntensityLabel.setText(lang.Msg(Languages.MSG_CUTOFF) + ": " + perspectiveValue + "%");
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
		Btn.setText(lang.Msg(Languages.MSG_ADDGFILES));
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
		Btn.setText(lang.Msg(Languages.MSG_CANCEL));
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
		IntensityLabel.setText(lang.Msg(Languages.MSG_CUTOFF) + ": 50%");
		Filename.setText("");
	}

	/**
	 * Select files to be added. The files should have the 128 byte Spectrum +3DOS
	 * header.
	 */
	protected void DoAddPlus3Files() {
		File selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES,lang.Msg(Languages.MSG_SELFILEIMP),
				new String[] { "*" });
		if ((selected != null) && (selected.length > 0)) {
			/*
			 * Iterate all files selected...
			 */
			for (File filename : selected) {
				byte HeaderBuffer[] = new byte[0x80];
				byte data[] = null;
				InputStream is = null;
				try {
					/*
					 * Open the file and load it.
					 */
					Plus3DosFileHeader p3d = null;
					try {
						is = new FileInputStream(filename);
						int numRead = is.read(HeaderBuffer);
						if (numRead < HeaderBuffer.length) {
							byte newbuffer[] = new byte[numRead];
							System.arraycopy(HeaderBuffer, 0, newbuffer, 0, numRead);
							HeaderBuffer = newbuffer;
						}
						p3d = new Plus3DosFileHeader(HeaderBuffer);
						data = new byte[p3d.GetBasicFileLength()];
						numRead = is.read(data);
						if (numRead < data.length) {
							System.out.println(lang.Msg(Languages.MSG_PLUS3FILEBAD));
						}
					} finally {
						if (is != null)
							is.close();
					}
					/*
					 * Try to identify the file type.
					 */
					// Check for a +3DOS header
					if (p3d.IsPlus3DosFile()) {
						String DosFileName = UniqueifyName(filename.getName());
						String filetypeName = p3d.getTypeDesc() + "("+lang.Msg(Languages.MSG_PLUS3HEADER)+")";

						TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
						String values[] = new String[5];
						values[0] = filename.getAbsolutePath();
						values[1] = DosFileName;
						values[2] = filetypeName;
						values[3] = String.valueOf(p3d.GetBasicFileLength());
						values[4] = "";

						byte newdata[] = new byte[data.length + 0x80];

						System.arraycopy(HeaderBuffer, 0, newdata, 0, 0x80);
						System.arraycopy(data, 0, newdata, 0x80, data.length);

						NewFileListItem listitem = new NewFileListItem();
						listitem.OriginalFilename = filename;
						listitem.filename = DosFileName;
						listitem.fileheader = p3d;
						listitem.FileType = FILETYPE_CPM;
						listitem.LoadAddress = p3d.GetLoadAddress();
						listitem.data = newdata;

						item2.setText(values);
						item2.setData(listitem);
					} else {
						System.out.println(String.format(lang.Msg(Languages.MSG_NOPLUS3HEADER), filename ));
					}
				} catch (FileNotFoundException E) {
					System.out.println(String.format(lang.Msg(Languages.MSG_ERRREADFILEXFNF), filename ));
				} catch (IOException E) {
					System.out.println(String.format(lang.Msg(Languages.MSG_ERRREADFILEX), filename ) + " " + E.getMessage());
				}
			}
		}
		shell.moveAbove(null);
	}

	/**
	 * Add file(s) as raw CPM files (EG, Headerless).
	 */
	protected void DoAddCPMFiles() {
		File selected[] = fsd.AskForMultipleFileOpen(FileSelectDialog.FILETYPE_FILES, lang.Msg(Languages.MSG_SELFILESTOADD),
				new String[] { "*.*" });
		if ((selected != null) && (selected.length > 0)) {
			for (File filename : selected) {

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
					System.out.println(String.format(lang.Msg(Languages.MSG_ERRORLOADING), filename.getName()));
				}

				/*
				 * Create the Row texts
				 */
				String DosFileName = UniqueifyName(CPM.FixFullName(filename.getName()));
				TableItem item2 = new TableItem(DirectoryListing, SWT.NONE);
				String values[] = new String[5];
				values[0] = filename.getAbsolutePath();
				values[1] = DosFileName;
				values[2] = "CPM";
				values[3] = String.valueOf(buffer.length);
				values[4] = "";

				/*
				 * Create the data storage object
				 */
				NewFileListItem listitem = new NewFileListItem();
				listitem.OriginalFilename = filename;
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
		shell.moveAbove(null);
	}

	/**
	 * Add the selected files to the partition and return This is used when OK is
	 * pressed.
	 */
	protected void DoAddFiles() {
		TableItem files[] = DirectoryListing.getItems();
		PLUS3DOSPartition p3dp = (PLUS3DOSPartition) CurrentPartition;
		for (TableItem file : files) {
			NewFileListItem details = (NewFileListItem) file.getData();
			try {
				// Default variable name for arrays
				char varname = 'A';
				switch (details.FileType) {
				case FILETYPE_CPM:
					p3dp.AddCPMFile(details.filename, details.data);
					break;
				case FILETYPE_BASIC:
					String Startline = StartLine.getText();
					int line = Integer.valueOf(Startline);
					p3dp.AddBasicFile(details.filename, details.data, line, details.data.length);
					break;
				case FILETYPE_CHRARRAY:
					p3dp.AddPlusThreeFile(details.filename, details.data, varname * 0x100, 0, Speccy.BASIC_CHRARRAY);
					break;
				case FILETYPE_NUMARRAY:
					p3dp.AddPlusThreeFile(details.filename, details.data, varname * 0x100, 0, Speccy.BASIC_NUMARRAY);
					break;
				case FILETYPE_CODE:
					// for CODE files, put at the top of memory
					p3dp.AddCodeFile(details.filename, details.LoadAddress, details.data);
					break;
				case FILETYPE_SCREEN:
					// For Screen$ files, these start at 16384 (0x4000)
					p3dp.AddCodeFile(details.filename, 0x4000, details.data);
					break;
				}
			} catch (IOException e) {
				System.out.println(String.format(lang.Msg(Languages.MSG_ERRORADDING),details.OriginalFilename, 
						details.filename) + " " + e.getMessage());
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
					treatAs = pfd.GetFileType();

					// If the file is CODE, and length 6912, treat as a screen.
					if ((pfd.GetFileType() == FILETYPE_CODE) && (pfd.GetBasicFileLength() == 6912)) {
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
				RenderCode(details);
				break;
			case FILETYPE_SCREEN:
				RenderScreen(data, details);
				break;
			}

			MainPage.pack();
			shell.pack();
			shell.layout(true, true);
			final Point newSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			shell.setSize(newSize);
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
	@Override
	protected String UniqueifyName(String s) {
		s = CPM.FixFullName(s);

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
		for (CPMDirectoryEntry d : ((PLUS3DOSPartition) CurrentPartition).DirectoryEntries) {
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

}

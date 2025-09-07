package hddEditor.ui.partitionPages.dialogs.AddFiles;

import java.io.IOException;
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

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Languages;
import hddEditor.libs.partitions.SinclairMicrodrivePartition;
import hddEditor.libs.partitions.mdf.MicrodriveDirectoryEntry;

public class AddFilesToMDRPartition extends GenericAddPageDialog {

	/**
	 * 
	 * @param display
	 */
	public AddFilesToMDRPartition(Display display, FileSelectDialog fsd, Languages lang) {
		super(display, fsd, lang);
	}

	/**
	 * Show the dialog
	 * 
	 * @param title
	 * @param p3d
	 */
	public void Show(String title, SinclairMicrodrivePartition smp) {
		CurrentPartition = smp;
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
		tc2.setText(lang.Msg(Languages.MSG_MDRFILE));
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

		l = new Label(shell, SWT.LEFT);
		l.setText(lang.Msg(Languages.MSG_CODELOADADD) + ": ");

		StartAddress = new Text(shell, SWT.BORDER);
		StartAddress.setText("16384");

		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
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
				byte data[] = details.data;

				// Default variable name for arrays
				switch (details.FileType) {
				case FILETYPE_BASIC:
					String Startline = StartLine.getText();
					int line = Integer.valueOf(Startline);
					CurrentPartition.AddBasicFile(details.filename, data, line, data.length - 0x80);
					break;
				case FILETYPE_CHRARRAY:
					CurrentPartition.AddCharArray(details.filename, data, "A");
					break;
				case FILETYPE_NUMARRAY:
					CurrentPartition.AddNumericArray(details.filename, data, "A");
					break;
				case FILETYPE_CODE:
					// for CODE files, put at the top of memory
					CurrentPartition.AddCodeFile(details.filename, details.LoadAddress, data);
					break;
				case FILETYPE_SCREEN:
					// For Screen$ files, these start at 16384 (0x4000)
					CurrentPartition.AddCodeFile(details.filename, 0x4000, data);
					break;
				}
			} catch (IOException e) {
				System.out.println(
						String.format(lang.Msg(Languages.MSG_ERRORADDING), details.OriginalFilename, details.filename)
								+ " " + e.getMessage());
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
			shell.layout(true, true);
			final Point newSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			shell.setSize(newSize);
		}
	}

	/**
	 * Modify the given filename so its unique in the current selection.
	 * 
	 * @param s
	 * @return
	 */
	@Override
	protected String UniqueifyName(String s) {
		String result = s.toLowerCase();
		/*
		 * Extract the filename and default extension from the file.
		 */
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
		for (MicrodriveDirectoryEntry d : ((SinclairMicrodrivePartition) CurrentPartition).DirectoryEntries) {
			String fname = d.GetFilename();
			currentlist.add(fname);
		}

		/*
		 * Check the filename against the list, and if found, create a new filename.
		 */
		int num = 1;
		boolean FileFound = true;
		String origfile = result;
		while (FileFound) {
			FileFound = currentlist.indexOf(result) > -1;
			if (FileFound) {
				String numstr = String.valueOf(num++);
				if (numstr.length() + origfile.length() > 10) {
					origfile = origfile.substring(0, 10 - numstr.length());
				}
				result = origfile + numstr;
			}
		}
		/*
		 * Resulting name should be unique.
		 */
		return (result);
	}

}

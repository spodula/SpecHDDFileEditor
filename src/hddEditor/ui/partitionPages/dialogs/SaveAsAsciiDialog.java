package hddEditor.ui.partitionPages.dialogs;
/**
 * Save As ASCII dialog used for the Hex edit. 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Languages;

public class SaveAsAsciiDialog {
	//Form components
	private Display display = null;
	private Shell shell = null;

	private Text startEdit = null;
	private Text lengthEdit = null;
	private Text FileNameEdit = null;
	private Text editSeperator = null;

	private Button AddressCB = null;
	private Button ASCIICB = null;
	private Button SWzeroCB = null;
	private Button DecHexCb = null;
	private Button SeperatorCB = null;

	public Label ExampleLabel = null;

	private int DataLength = 0;

	private FileSelectDialog filesel = null;
	
	private String defaultfilename = "";
	
	private Languages lang;
	
	/**
	 * Constructor
	 * @param display
	 */
	public SaveAsAsciiDialog(Display display, FileSelectDialog filesel,Languages lang) {
		this.display = display;
		this.filesel = filesel;
		this.lang = lang;
	}

	/**
	 * Show the dialog
	 *
	 * @param data
	 * @param title
	 */
	public void Show(byte[] data, String title, String defaultfilename) {
		this.DataLength = data.length;
		this.defaultfilename = defaultfilename;
		Createform(data, title);
		loop();
	}

	/**
	 * This listener is added to a lot of the components to update the example label
	 * when formatting has changed
	 */
	SelectionListener ExampleListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent arg0) {
			byte data[] = { 'T', 'E', 'S', 'T', 'I', 'N', 'G', '-', '1', '2', '3', '4', '5', '6', '&', '7' };

			int address = 0;
			try {
				address = Integer.valueOf(startEdit.getText());
			} catch (NumberFormatException E) {
			}
			if (SWzeroCB.getSelection()) {
				address = 0;
			}
			String seperator = "";
			if (SeperatorCB.getSelection()) {
				seperator = editSeperator.getText();
			}

			String result = FormatLine(data, 0, address, DecHexCb.getSelection(), AddressCB.getSelection(), seperator,
					ASCIICB.getSelection());
			ExampleLabel.setText(result);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent arg0) {
			widgetSelected(arg0);
		}
	};

	/**
	 * Format the text as per the parameters set on the form.
	 * 
	 * @param data - Data being formatted
	 * @param ptr - Pointer within the data
	 * @param Address - displayed start address
	 * @param IsHex - Hex or Dec?
	 * @param ShowAddress - Show start address at the start of the line?
	 * @param seperator - Seperator between values
	 * @param ShowAscii - Show ascii table at the end
	 * @return - formatted String
	 */
	private String FormatLine(byte[] data, int ptr, int Address, boolean IsHex, boolean ShowAddress, String seperator,
			boolean ShowAscii) {
		String DisplayFormat = "%Ad";
		if (IsHex) {
			DisplayFormat = "%AX";
		}
		String result = "";
		if (ShowAddress) {
			// Work out the maximum address length.
			int i = String.format(DisplayFormat.replace("A", ""), DataLength - 1).length();
			String addressformat = DisplayFormat.replace("A", "0" + String.valueOf(i));
			result = String.format(addressformat, Address);
			result = result + "\t";
		}
		// Add the data at the end.
		String dataformat = DisplayFormat.replace("A", "02");
		for (int i = 0; i < 16; i++) {
			char c = 0x00;
			if ((ptr+i) < data.length) {
				c = (char) data[ptr + i];
				result = result + String.format(dataformat, (int) (c & 0xff));
			} else {
				result = result + "--";
			}
			if (i != 15)
				result = result + seperator;
		}
		//Add in the ASCII bit at the end if required.
		if (ShowAscii) {
			result = result + "\t";
			for (int i = 0; i < 16; i++) {
				char c = 0x00;
				if ((ptr+i) < data.length) {
					c = (char) data[ptr + i];
				}
				if (c > 0x1f && c < 0x80) {
					result = result + c;
				} else {
					result = result + ".";
				}
			}
		}
		return (result);
	}

	/**
	 * create the form
	 * 
	 * @param data
	 * @param title
	 */
	private void Createform(byte[] data, String title) {
		shell = new Shell(display);
		shell.setSize(900, 810);
		GridLayout gridLayout = new GridLayout(4, true);
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;

		shell.setLayout(gridLayout);
		shell.setText(title);

		Label lbl = new Label(shell, SWT.NONE);
		lbl.setText(title);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setFont(boldFont);
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_FILENAME) + ":");
		lbl.setFont(boldFont);

		FileNameEdit = new Text(shell, SWT.BORDER);
		FileNameEdit.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		FileNameEdit.setLayoutData(gd);
		FileNameEdit.setSize(200, FileNameEdit.getSize().y);
		Button Selbtn = new Button(shell, SWT.NONE);
		Selbtn.setText(lang.Msg(Languages.MSG_SELECT) );
		Selbtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				SelectFile();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		GridData SingleColFillGridData = new GridData();
		SingleColFillGridData.grabExcessHorizontalSpace = true;
		SingleColFillGridData.horizontalSpan = 1;
		SingleColFillGridData.minimumWidth = 100;
		SingleColFillGridData.widthHint = 200;
		Selbtn.setLayoutData(SingleColFillGridData);


		lbl = new Label(shell, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_RANGESTART) + ":" );
		lbl.setFont(boldFont);

		startEdit = new Text(shell, SWT.BORDER);
		startEdit.setText("00000000000000000000000000000000");
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 3;
		startEdit.setLayoutData(gd);
		startEdit.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				// make sure the user can only enter 0-9, A-F (And control keys)
				ExampleListener.widgetSelected(null);
			}
		});
		// restrict the editor to numbers and control keys.
		startEdit.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				e.doit = false;
				char c = e.character;
				if ((c >= '0' && c <= '9') || (c < 0x20)) {
					e.doit = true;
				}
			}
		});

		lbl = new Label(shell, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_LENGTH) + ":" );

		lengthEdit = new Text(shell, SWT.BORDER);
		lengthEdit.setText("00000000000000000000000000000000");
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 3;
		lengthEdit.setLayoutData(gd);

		AddressCB = new Button(shell, SWT.CHECK);
		AddressCB.setText(lang.Msg(Languages.MSG_ADDRESS));
		AddressCB.addSelectionListener(ExampleListener);
		AddressCB.setSelection(true);

		ASCIICB = new Button(shell, SWT.CHECK);
		ASCIICB.setText(lang.Msg(Languages.MSG_ASCIISECTT));
		ASCIICB.addSelectionListener(ExampleListener);
		ASCIICB.setSelection(true);
		SWzeroCB = new Button(shell, SWT.CHECK);
		SWzeroCB.setText(lang.Msg(Languages.MSG_START0T));
		SWzeroCB.addSelectionListener(ExampleListener);
		SWzeroCB.setSelection(true);
		DecHexCb = new Button(shell, SWT.CHECK);
		DecHexCb.setText(lang.Msg(Languages.MSG_DECHEXT));
		DecHexCb.addSelectionListener(ExampleListener);
		DecHexCb.setSelection(true);
		SeperatorCB = new Button(shell, SWT.CHECK);
		SeperatorCB.setText(lang.Msg(Languages.MSG_SEPERATORT));
		SeperatorCB.addSelectionListener(ExampleListener);
		SeperatorCB.setSelection(true);
		editSeperator = new Text(shell, SWT.BORDER);
		editSeperator.setText("XXX");
		editSeperator.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				ExampleListener.widgetSelected(null);
			}
		});

		lbl = new Label(shell, SWT.NONE);
		lbl.setText("");
		lbl = new Label(shell, SWT.NONE);
		lbl.setText("");

		lbl = new Label(shell, SWT.NONE);
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 4;
		lbl.setText(lang.Msg(Languages.MSG_SAMPLE));
		lbl.setFont(boldFont);
		lbl.setLayoutData(gd);

		ExampleLabel = new Label(shell, SWT.NONE);
		ExampleLabel.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxXX");
		ExampleLabel.setLayoutData(gd);
		fontData = ExampleLabel.getFont().getFontData()[0];
		Font MonoFont = new Font(display, new FontData("Monospace", 12, SWT.BOLD));
		ExampleLabel.setFont(MonoFont);

		lbl = new Label(shell, SWT.NONE);
		lbl = new Label(shell, SWT.NONE);

		Button Cancelbtn = new Button(shell, SWT.NONE);
		Cancelbtn.setText(lang.Msg(Languages.MSG_CANCEL));
		Cancelbtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				shell.close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		Cancelbtn.setLayoutData(SingleColFillGridData);

		Button OKbtn = new Button(shell, SWT.NONE);
		OKbtn.setText(lang.Msg(Languages.MSG_SAVE));
		OKbtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFile(data);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		OKbtn.setLayoutData(SingleColFillGridData);

		shell.pack();
		FileNameEdit.setText("");
		startEdit.setText("0");
		lengthEdit.setText(String.valueOf(data.length));
		editSeperator.setText(" ");
		ExampleListener.widgetSelected(null);
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
	 * Select a file to save as
	 */
	public void SelectFile() {
		File Selected = filesel.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES, lang.Msg(Languages.MSG_SAVEFILEAS)+":", new String[] {"*.txt"}, defaultfilename);
		if (Selected != null) {
			FileNameEdit.setText(Selected.getAbsolutePath());
		}
		shell.moveAbove(null);
	}

	/**
	 * Actually save the file.
	 * @param data
	 */
	public void DoSaveFile(byte[] data) {
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(FileNameEdit.getText());
			try {
				int start = Integer.valueOf(startEdit.getText());
				int length = Integer.valueOf(lengthEdit.getText());
				length = Math.min(length, data.length - start);
				byte newdata[] = new byte[length];
				System.arraycopy(data, start, newdata, 0, length);

				int address = 0;
				try {
					address = Integer.valueOf(startEdit.getText());
				} catch (NumberFormatException E) {
				}
				if (SWzeroCB.getSelection()) {
					address = 0;
				}
				String seperator = "";
				if (SeperatorCB.getSelection()) {
					seperator = editSeperator.getText();
				}
				int ptr = 0;
				boolean IsHex = DecHexCb.getSelection();
				boolean ShowAddress = AddressCB.getSelection();
				boolean ShowAscii = ASCIICB.getSelection();
				
				while (ptr < newdata.length) {
					String s = FormatLine(newdata, ptr, address, IsHex, ShowAddress, seperator, ShowAscii)+System.lineSeparator();
					ptr = ptr + 16;
					address = address + 16;
					outputStream.write(s.getBytes(StandardCharsets.UTF_8));
				}

				shell.close();
			} finally {
				outputStream.close();
			}
		} catch (FileNotFoundException e) {
			MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			dialog.setText(lang.Msg(Languages.MSG_ERRSAVING));
			dialog.setMessage(lang.Msg(Languages.MSG_DIRNOTFOUND));
			dialog.open();
			e.printStackTrace();
		} catch (IOException e) {
			MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			dialog.setText(lang.Msg(Languages.MSG_ERRSAVING));
			dialog.setMessage(lang.Msg(Languages.MSG_IOERROR));
			dialog.open();
			e.printStackTrace();
		}
	}
	
	/**
	 * Callback from the HexEditDialog to force this form to close when the HexEdit
	 * dialog closes.
	 */
	public void close() {
		shell.setVisible(false);
		shell.dispose();
	}

}

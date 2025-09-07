package hddEditor.ui.partitionPages.dialogs;
/**
 * Implementation of the Hexedits Search/replace dialog
 */


import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.Languages;

public class SearchReplaceDialog {
	//Form components
	private Display display = null;
	public Shell shell = null;
	private Button binAscii = null;
	private Label HexAsciiLabel = null;
	private Text replaceEdit = null;
	private Text searchEdit = null;
	private Table ResultList = null;
	private Label InfoLabel = null;

	//result
	private boolean result = false;

	//List of what the bytes of actually are.
	private AddressNote Notes[] = null;
	
	//Data to search
	public byte[] Searchdata = null;

	private static String FormatHex = "Format: XX XX XX XX";
	private static String FormatAsc = "Format: ABCDEFGH";

	public Languages lang;
	
	/**
	 * Constructor
	 * 
	 * @param display
	 * @param src
	 * @param notes
	 */
	public SearchReplaceDialog(Display display, byte src[], AddressNote notes[], Languages lang) {
		this.display = display;
		Searchdata = new byte[src.length];
		System.arraycopy(src, 0, Searchdata, 0, src.length);
		Notes = notes;
		this.lang = lang;
	}

	/**
	 * Show the form
	 * @return
	 */
	public boolean Show() {
		Createform();
		loop();
		return (result);
	}

	/**
	 * Dialog loop, open and wait until closed.
	 */
	public void loop() {
		shell.open();
		while (!shell.isDisposed() && shell.isVisible()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	/**
	 * Create box.
	 */
	private void Createform() {
		shell = new Shell(display);
		shell.setSize(900, 810);
		GridLayout gridLayout = new GridLayout(4, true);
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;
		gridLayout.marginBottom = 20;

		shell.setLayout(gridLayout);
		shell.setText(lang.Msg(Languages.MSG_SEARCHREPLACE));

		Label lbl = new Label(shell, SWT.NONE);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setText(lang.Msg(Languages.MENU_SEARCH)+":");
		lbl.setFont(boldFont);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		lbl.setLayoutData(gd);

		searchEdit = new Text(shell, SWT.BORDER);
		searchEdit.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxxxxxxxxXXXXXXXXXXXx");
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 3;
		searchEdit.setLayoutData(gd);
		searchEdit.setSize(200, searchEdit.getSize().y);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_REPLACE)+":");
		lbl.setFont(boldFont);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		lbl.setLayoutData(gd);

		replaceEdit = new Text(shell, SWT.BORDER);
		replaceEdit.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxxxxxxxxXXXXXXXXx");
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 3;
		replaceEdit.setLayoutData(gd);
		replaceEdit.setSize(200, searchEdit.getSize().y);

		InfoLabel = new Label(shell, SWT.NONE);
		InfoLabel.setText("xxxxxxxxxxxxxxxx");
		InfoLabel.setFont(boldFont);

		binAscii = new Button(shell, SWT.BORDER | SWT.CHECK);
		binAscii.setText(lang.Msg(Languages.MSG_HEXASCII));
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 1;
		binAscii.setLayoutData(gd);
		binAscii.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoHexAsciiChange();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		HexAsciiLabel = new Label(shell, SWT.NONE);
		HexAsciiLabel.setText(FormatHex);
		HexAsciiLabel.setFont(boldFont);
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		HexAsciiLabel.setLayoutData(gd);

		ResultList = new Table(shell, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		ResultList.setLinesVisible(true);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = 100;
		ResultList.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(ResultList, SWT.LEFT);
		TableColumn tc2 = new TableColumn(ResultList, SWT.FILL);
		tc1.setText(lang.Msg(Languages.MSG_ADDRESS));
		tc2.setText(lang.Msg(Languages.MSG_NOTES));
		tc1.setWidth(150);
		tc2.setWidth(350);
		ResultList.setHeaderVisible(true);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 160;
		Button Btn = new Button(shell, SWT.PUSH);
		Btn.setText(lang.Msg(Languages.MENU_SEARCH));
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSearch();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Btn = new Button(shell, SWT.PUSH);
		Btn.setText(lang.Msg(Languages.MSG_REPLACE));
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSearch();
				DoReplace();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		Btn = new Button(shell, SWT.PUSH);
		Btn.setText(lang.Msg(Languages.MSG_CANCEL));
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				shell.dispose();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		Btn = new Button(shell, SWT.PUSH);
		Btn.setText(lang.Msg(Languages.MSG_SAVECLOSE));
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				result = true;
				shell.dispose();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		shell.pack();
		searchEdit.setText("");
		replaceEdit.setText("");
		InfoLabel.setText("");
	}

	/**
	 * Handler for the Hex/ascii checbox.
	 */
	protected void DoHexAsciiChange() {
		boolean issel = binAscii.getSelection();
		if (issel) {
			HexAsciiLabel.setText(FormatAsc);
		} else {
			HexAsciiLabel.setText(FormatHex);
		}
	}

	/**
	 * Quick and dirty IsHexNum
	 * 
	 * @param c
	 * @return
	 */
	protected boolean IsHexNum(char c) {
		return ("ABCDEFabcdef0123456789".indexOf(c) != -1);
	}

	/**
	 * Actually do the search.
	 */
	protected void DoSearch() {
		ResultList.removeAll();
		String txt = searchEdit.getText();
		byte searchBytes[] = ConvertSearchToSearchArray(txt);

		/*
		 * Search and add results to the list.
		 */
		int numresults = 0;
		for (int ptr = 0; ptr < (Searchdata.length - searchBytes.length); ptr++) {
			boolean found = true;
			if (searchBytes[0] == Searchdata[ptr]) {
				for (int i = 1; i < searchBytes.length; i++) {
					if (searchBytes[i] != Searchdata[ptr + i]) {
						found = false;
					}
				}
			} else
				found = false;
			if (found) {
				String line[] = new String[3];
				String s = Integer.toHexString(ptr);
				while (s.length() < 8)
					s = "0" + s;
				line[0] = s;
				line[1] = "";
				if (Notes != null) {
					for (AddressNote an : Notes) {
						if (an.DoesNoteApply(ptr)) {
							int addressInFile = ptr - an.StartAddress + an.NoteDisp;
							line[1] = an.note + " byte: " + addressInFile + " ($" + Integer.toHexString(addressInFile)
									+ ")";
						}
					}
				}

				TableItem tbl = new TableItem(ResultList, SWT.BORDER);
				tbl.setText(line);
				numresults++;
			}
		}
		String s = "";
		if (numresults > 1)
			s = "s";
		InfoLabel.setText(String.format(lang.Msg(Languages.MSG_XITEMSFOUND), numresults,s));
		ResultList.redraw();
	}

	/**
	 * Convert the given text from the edit box to an array of bytes.
	 * 
	 * @param txt
	 * @return
	 */
	private byte[] ConvertSearchToSearchArray(String txt) {
		/*
		 * Convert text to a byte stream
		 */
		byte searchBytes[] = new byte[txt.length()];
		int srcPtr = 0;
		int tarPtr = 0;
		boolean IsSecondByte = false;
		boolean AsciiMode = binAscii.getSelection();
		int currentnum = 0;
		// iterate the text
		while (srcPtr < txt.length()) {
			char c = txt.charAt(srcPtr++);
			/*
			 * In hex mode, parse the text as 2 digit Hex numbers
			 */
			if (!AsciiMode) {
				// If character is hex, try to add it.
				if (IsHexNum(c)) {
					if (!IsSecondByte) {
						// First hex character, store it for later
						currentnum = Integer.parseInt(c + "", 16);
						IsSecondByte = true;
					} else {
						// Take the second digit, add it, and reset for the next digit.
						currentnum = currentnum * 16;
						currentnum = currentnum + Integer.parseInt(c + "", 16);
						searchBytes[tarPtr++] = (byte) (currentnum & 0xff);
						currentnum = -1;
						IsSecondByte = false;
					}
				} else {
					// If we didnt get a number last time, we need to ignore it, else add it.
					if (currentnum != -1) {
						searchBytes[tarPtr++] = (byte) (currentnum & 0xff);
					}
					currentnum = -1;
					IsSecondByte = false;
				}
			} else {
				// For ascii mode, just add to the search bytes
				searchBytes[tarPtr++] = (byte) (c & 0xff);
			}
		}
		// If we have a partial number, add what we have.
		if ((currentnum != -1) && !AsciiMode) {
			if (IsSecondByte) {
				searchBytes[tarPtr++] = (byte) (currentnum & 0xff);
			}
		}
		byte result[] = new byte[tarPtr];
		System.arraycopy(searchBytes, 0, result, 0, tarPtr);
		return (result);
	}

	/**
	 * Callback from the HexEditDialog to force this form to close when the HexEdit
	 * dialog closes.
	 */
	public void close() {
		shell.setVisible(false);
		shell.dispose();
	}

	/**
	 * Force this form to the front.
	 */
	public void ForceFront() {
		shell.forceActive();
		shell.forceFocus();
	}

	/**
	 * Implement the replace.
	 */
	protected void DoReplace() {
		String txt = searchEdit.getText();
		byte searchBytes[] = ConvertSearchToSearchArray(txt);

		txt = replaceEdit.getText();
		byte ReplaceBytes[] = ConvertSearchToSearchArray(txt);
		int NumReplace = 0;

		if (searchBytes.length != ReplaceBytes.length) {
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage(lang.Msg(Languages.MSG_SRSAMELEN));
			messageBox.setText(lang.Msg(Languages.MSG_SRSAMELEN));
			messageBox.open();
		} else {
			DoSearch();
			for (TableItem tbl : ResultList.getItems()) {
				String value = tbl.getText(0).trim();
				int address = Integer.parseInt(value, 16);
				for (int i = 0; i < ReplaceBytes.length; i++) {
					Searchdata[address + i] = ReplaceBytes[i];
				}
				NumReplace++;
			}
		}
		String s = "";
		if (NumReplace > 1)
			s = "s";
		InfoLabel.setText(String.format(lang.Msg(Languages.MSG_REPLACEDXITEMS), NumReplace, s));
	}
}

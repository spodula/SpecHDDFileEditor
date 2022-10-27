package hddEditor.ui.partitionPages.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.TestUtils;

public class HexEditDialog {
	private boolean result = false;
	private boolean xModified = false;
	private Display display = null;
	private Shell shell = null;

	public byte[] Data = null;

	private String Title = "";

	private Menu menuBar, fileMenu, helpMenu, editMenu;

	private MenuItem fileMenuHeader, helpMenuHeader, editMenuHeader;
	private MenuItem fileExitItem, fileSaveAsciiItem, fileSaveItem;
	private MenuItem helpGetHelpItem;
	private MenuItem editSearchItem;

	private Table HexTable = null;
	private Label InfoLabel = null;
	private boolean FirstChar = false;

	private AddressNote[] notes = null;

	private SearchReplaceDialog SearchReplaceDlg = null;
	private SaveAsAsciiDialog SaveAsAsciiDlg = null;
	private SaveAsDialog SaveAsDlg = null;

	private String EDIT_LABEL = "Press ENTER to edit byte";
	private String EDITING_LABEL = "Press ENTER to set byte or ESCAPE to cancel";

	/**
	 * 
	 * @param Modified
	 */
	private void SetModified(boolean Modified) {
		xModified = Modified;
		String s = Title;
		if (Modified) {
			s = s + " (Modified)";
		}
		shell.setText(s);
	}

	/**
	 * 
	 * @param display
	 */
	public HexEditDialog(Display display) {
		this.display = display;
	}

	/**
	 * 
	 * @param data
	 * @param title
	 * @param notes
	 * @return
	 */
	public boolean Show(byte[] data, String title, AddressNote notes[]) {
		result = false;
		xModified = false;
		this.Title = title;
		this.Data = data;
		this.notes = notes;
		Createform();
		SetModified(false);
		loop();
		return (result);
	}

	/**
	 * 
	 */
	private void Createform() {
		shell = new Shell(display);
		shell.setSize(900, 810);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.makeColumnsEqualWidth = true;
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;
		shell.setLayout(gridLayout);
		MakeMenus();

		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				CloseSubWindows();
			}
		});

		Label lbl = new Label(shell, SWT.NONE);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setText(String.format("Length: %d bytes (%X)", Data.length, Data.length));
		lbl.setFont(boldFont);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		int AddressLength = String.format("%X", Data.length - 1).length();

		int numrows = Data.length / 16;
		if (Data.length % 16 != 0) {
			numrows++;
		}

		Font mono = new Font(display, "Monospace", 10, SWT.NONE);
		HexTable = new Table(shell, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL);
		HexTable.setLinesVisible(true);
		HexTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				TableItem Row = (TableItem) event.item;
				int index = HexTable.indexOf(Row);
				int address = index * 16;

				String asciiLine = "";
				String content[] = new String[18];
				String addr = String.format("%X", address);
				while (addr.length() < AddressLength) {
					addr = "0" + addr;
				}
				content[0] = addr;

				for (int i = 1; i < 17; i++) {
					byte b = 0;
					if (address < Data.length) {
						b = Data[address++];
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
		});

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = 600;
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
		HexTable.setItemCount(numrows);

		// create a TableCursor to navigate around the table
		final TableCursor cursor = new TableCursor(HexTable, SWT.None);

		// create an editor to edit the cell when the user hits "ENTER"
		// while over a cell in the table
		final ControlEditor editor = new ControlEditor(cursor);
		editor.grabHorizontal = true;
		editor.grabVertical = true;

		cursor.addSelectionListener(new SelectionAdapter() { // when the TableEditor is over a cell, select the
																// corresponding row in
			// the table
			public void widgetSelected(SelectionEvent e) {
				HexTable.setSelection(new TableItem[] { cursor.getRow() });
			}

			// when the user hits "ENTER" in the TableCursor, pop up a text editor so that
			// they can change the text of the cell
			public void widgetDefaultSelected(SelectionEvent e) {
				TableItem row = cursor.getRow();
				int column = cursor.getColumn();

				if (column != 0 && column != 17) {
					final Text text = new Text(cursor, SWT.NONE);
					text.setText(row.getText(column));
					FirstChar = true;
					text.addKeyListener(new KeyAdapter() {
						public void keyPressed(KeyEvent e) { // close the text editor and copy the data over
							// when the user hits "ENTER"
							if (e.character == SWT.CR) {
								TableItem row = cursor.getRow();
								int column = cursor.getColumn();
								String s = (text.getText().toUpperCase() + "  ").substring(0, 2);
								row.setText(column, s);
								text.dispose(); // update the character display.
								int value = Integer.parseInt(s, 16);
								char chr = (char) value;
								byte asc[] = row.getText(17).getBytes();
								asc[column - 1] = (byte) chr;
								row.setText(17, new String(asc));
								int address = Integer.valueOf(row.getText(0), 16) + column - 1;
								System.out.println("Updating " + address + " (" + String.format("%04X", address)
										+ ") with " + value);

								Data[address] = (byte) (value & 0xff);

								InfoLabel.setText(EDIT_LABEL);

								SetModified(true);
								e.doit = true;
							} else if (e.character == SWT.ESC) {
								// close the text editor when the user hits "ESC"
								text.dispose();
								InfoLabel.setText(EDIT_LABEL);
								e.doit = true;
							} else {
								// make sure the user can only enter 0-9, A-F (And control keys)
								e.doit = false;
								char c = e.character;
								if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f')
										|| (c < 0x20)) {
									if (c < 0x20) {
										e.doit = true;
									} else {
										e.doit = (text.getText().length() < 2) || FirstChar;
										if (FirstChar) {
											FirstChar = false;
											text.setText("");
										}
									}
								}
							}
						}
					});
					editor.setEditor(text);
					text.setFocus();
					InfoLabel.setText(EDITING_LABEL);
					InfoLabel.setSize(800, 20);
				}
			}
		});

		// Hide the TableCursor when the user hits the "MOD1" or "MOD2" key.
		// This allows the user to select multiple items in the table.

		cursor.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.MOD1 || e.keyCode == SWT.MOD2 || (e.stateMask & SWT.MOD1) != 0
						|| (e.stateMask & SWT.MOD2) != 0) {
					cursor.setVisible(false);
				}
			}
		});

		// Show the TableCursor when the user releases the "MOD2" or "MOD1" key.
		// This signals the end of the multiple selection task.
		HexTable.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.MOD1 && (e.stateMask & SWT.MOD2) != 0)
					return;
				if (e.keyCode == SWT.MOD2 && (e.stateMask & SWT.MOD1) != 0)
					return;
				if (e.keyCode != SWT.MOD1 && (e.stateMask & SWT.MOD1) != 0)
					return;
				if (e.keyCode != SWT.MOD2 && (e.stateMask & SWT.MOD2) != 0)
					return;

				TableItem[] selection = HexTable.getSelection();
				TableItem row = (selection.length == 0) ? HexTable.getItem(HexTable.getTopIndex()) : selection[0];
				HexTable.showItem(row);
				cursor.setSelection(row, 0);
				cursor.setVisible(true);
				cursor.setFocus();
			}
		});

		InfoLabel = new Label(shell, SWT.NONE);
		InfoLabel.setText(EDIT_LABEL);
		InfoLabel.setFont(boldFont);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		InfoLabel.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		gd.grabExcessHorizontalSpace = true;
		new Label(shell, SWT.NONE).setLayoutData(gd);

		Button OKBtn = new Button(shell, SWT.NONE);
		OKBtn.setText("OK");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.grabExcessHorizontalSpace = true;

		OKBtn.setLayoutData(gd);
		OKBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				result = xModified;
				shell.close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}
		});

		Button CancelBtn = new Button(shell, SWT.NONE);
		CancelBtn.setText("Cancel");
		CancelBtn.setLayoutData(gd);
		CancelBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoCloseQuestion();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}
		});
		shell.pack();
	}

	/**
	 * 
	 */
	private void MakeMenus() {
		Label label = new Label(shell, SWT.CENTER);
		label.setBounds(shell.getClientArea());

		menuBar = new Menu(shell, SWT.BAR);
		/**
		 * File menu
		 */
		fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuHeader.setText("&File");

		fileMenu = new Menu(shell, SWT.DROP_DOWN);
		fileMenuHeader.setMenu(fileMenu);

		fileSaveItem = new MenuItem(fileMenu, SWT.PUSH);
		fileSaveItem.setText("&Save As");
		fileSaveItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoFileSaveItem(false);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		fileSaveAsciiItem = new MenuItem(fileMenu, SWT.PUSH);
		fileSaveAsciiItem.setText("&Save As ascii");
		fileSaveAsciiItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoFileSaveItem(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
		fileExitItem.setText("E&xit");
		fileExitItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				DoCloseQuestion();
			}
		});
		/**
		 * Edit menu
		 */
		editMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		editMenuHeader.setText("&Edit");

		editMenu = new Menu(shell, SWT.DROP_DOWN);
		editMenuHeader.setMenu(editMenu);

		editSearchItem = new MenuItem(editMenu, SWT.PUSH);
		editSearchItem.setText("&Search");
		editSearchItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				DoSearchDialog();
			}
		});

		/**
		 * Help menu
		 */
		helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		helpMenuHeader.setText("&Help");

		helpMenu = new Menu(shell, SWT.DROP_DOWN);
		helpMenuHeader.setMenu(helpMenu);

		helpGetHelpItem = new MenuItem(helpMenu, SWT.PUSH);
		helpGetHelpItem.setText("&Get Help");

		shell.setMenuBar(menuBar);
	}

	/**
	 * 
	 */
	protected void DoCloseQuestion() {
		boolean DoClose = true;
		if (xModified) {
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			messageBox.setMessage("Do you really want to close without saving?");
			messageBox.setText("Close");
			int response = messageBox.open();

			DoClose = (response == SWT.YES);
		}
		if (DoClose) {
			result = false;
			shell.close();
		}
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
		shell.dispose();
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Display display = new Display();
		byte data[] = TestUtils.ReadFileIntoArray("/home/graham/2gtest.img");
		HexEditDialog testf = new HexEditDialog(display);
		System.out.println(testf.Show(data, "CPM BAM", null));
	}

	/**
	 * 
	 * @param SaveAscii
	 */
	protected void DoFileSaveItem(boolean SaveAscii) {
		if (SaveAscii) {
			SaveAsAsciiDlg = new SaveAsAsciiDialog(display);
			SaveAsAsciiDlg.Show(Data, "Save file as ascii");
			SaveAsAsciiDlg = null;
		} else {
			SaveAsDlg = new SaveAsDialog(display);
			SaveAsDlg.Show(Data, "Save file (Binary)");
			SaveAsDlg = null;
		}
	}

	/**
	 * 
	 */
	protected void DoSearchDialog() {
		if (SearchReplaceDlg != null) {
			SearchReplaceDlg.ForceFront();
		} else {
			SearchReplaceDlg = new SearchReplaceDialog(display, Data, notes);
			if (SearchReplaceDlg.Show()) {
				if (!shell.isDisposed()) {
					System.arraycopy(SearchReplaceDlg.Searchdata, 0, Data, 0, Data.length);
					SetModified(true);
				}
			}
			SearchReplaceDlg = null;
			// Force the virtual table to refresh.
			if (!HexTable.isDisposed()) {
				HexTable.clearAll();
			}
		}
	}

	/**
	 * Provided to close any sub-windows (Ie, the search/replace dialog)
	 */
	protected void CloseSubWindows() {
		// Close the search/replace dialog if its still open;
		if (SearchReplaceDlg != null) {
			SearchReplaceDlg.close();
			SearchReplaceDlg = null;
		}
		if (SaveAsAsciiDlg != null) {
			SaveAsAsciiDlg.close();
			SaveAsAsciiDlg = null;
		}
		if (SaveAsDlg != null) {
			SaveAsDlg.close();
			SaveAsDlg = null;
		}
	}
	
	/**
	 * 
	 */
	public void close() {
		CloseSubWindows();
		shell.close();
		if (!shell.isDisposed()) {
			shell.dispose();
		}
	}

}

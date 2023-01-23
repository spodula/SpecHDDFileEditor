package hddEditor.ui.partitionPages.FileRenderers;

/**
 * Render a CODE file
 */

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.ASMLib;
import hddEditor.libs.ASMLib.DecodedASM;
import hddEditor.libs.Speccy;

public class CodeRenderer extends FileRenderer {
	// components
	private Text StartAddress = null;
	private Combo CodeTypeDropDown = null;
	private Table HexTable = null;
	private Label[] ImageLabel = null;

	// Rendering options
	private String[] CODETYPES = { "Binary", "Screen", "Assembly" };

	/**
	 * 
	 * @param mainPage
	 * @param data
	 * @param header
	 * @param Filename
	 * @param fileSize
	 * @param loadAddr
	 */
	public void RenderCode(Composite mainPage, byte data[], byte header[], String Filename, int fileSize,
			int loadAddr) {
		this.filename = Filename;
		this.MainPage = mainPage;
		this.data = data;
		Label lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("CODE file: ");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("+3DOS File length: ");
		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(String.format("%d (%X)", fileSize, fileSize));

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Start Address: ");

		StartAddress = new Text(mainPage, SWT.NONE);
		StartAddress.setText(String.valueOf(loadAddr));
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		StartAddress.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);

		Button btn = new Button(mainPage, SWT.NONE);
		btn.setText("Extract file as Hex");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsHex(data, mainPage, 0, data.length, filename);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		btn = new Button(mainPage, SWT.NONE);
		btn.setText("Extract file as Binary");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsBin(data, mainPage,Filename);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		if (header != null) {
			btn = new Button(mainPage, SWT.NONE);
			btn.setText("Extract file as Binary Inc Header");
			btn.setLayoutData(gd);
			btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					byte newdata[] = new byte[data.length + header.length];
					System.arraycopy(header, 0, newdata, 0, header.length);
					System.arraycopy(data, 0, newdata, header.length, data.length);

					DoSaveFileAsBin(data, mainPage,Filename);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
		} else {
			new Label(mainPage, SWT.NONE);
		}

		btn = new Button(mainPage, SWT.NONE);
		btn.setText("Extract file as Picture");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsPic(data, mainPage);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		btn = new Button(mainPage, SWT.NONE);
		btn.setText("Extract file as Asm");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsAsm(data, mainPage, loadAddr);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		CodeTypeDropDown = new Combo(mainPage, SWT.NONE);
		CodeTypeDropDown.setItems(CODETYPES);
		CodeTypeDropDown.setText(CODETYPES[0]);
		if (data.length == 6912) {
			CodeTypeDropDown.setText(CODETYPES[1]);
		}
		CodeTypeDropDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CodeTypeComboChanged(data, loadAddr);
			}
		});

		lbl = new Label(mainPage, SWT.NONE);
		lbl = new Label(mainPage, SWT.NONE);
		lbl = new Label(mainPage, SWT.NONE);
		CodeTypeComboChanged(data, loadAddr);

		mainPage.pack();
	}

	/**
	 * Add the BIN (hex) option
	 * 
	 * @param data
	 * @param loadAddr
	 */
	private void AddBin(byte data[], int loadAddr) {
		int AddressLength = String.format("%X", data.length - 1).length();

		HexTable = new Table(MainPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
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
		int Address = loadAddr;

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
	 * Render the data as a Screen
	 * 
	 * @param data
	 */
	private void AddScreen(byte data[]) {
		int base = 0;
		ArrayList<Label> al = new ArrayList<Label>();
		while (base < data.length) {
			byte screen[] = new byte[0x1b00];
			for (int i = 0; i < 0x1800; i++) {
				screen[i] = 0;
			}
			byte wob = Speccy.ToAttribute(Speccy.COLOUR_BLACK, Speccy.COLOUR_WHITE, false, false);
			for (int i = 0x1800; i < 0x1b00; i++) {
				screen[i] = wob;
			}
			System.arraycopy(data, base, screen, 0, Math.min(0x1b00, data.length - base));

			ImageData image = Speccy.GetImageFromFileArray(screen, 0);
			Image img = new Image(MainPage.getDisplay(), image);
			Label lbl = new Label(MainPage, SWT.NONE);
			lbl.setImage(img);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.minimumHeight = 192;
			gd.minimumWidth = 256;
			gd.horizontalSpan = 2;
			lbl.setLayoutData(gd);
			al.add(lbl);
			base = base + 0x1b00;
		}
		ImageLabel = al.toArray(new Label[0]);
		MainPage.pack();
	}

	/**
	 * Render the code file as selected in the combo.
	 * 
	 * @param data
	 * @param loadAddr
	 */
	private void CodeTypeComboChanged(byte data[], int loadAddr) {
		String s = CodeTypeDropDown.getText().trim();
		DoChangeCodeType(s, data, loadAddr);
	}

	/**
	 * Actually render
	 * 
	 * @param s
	 * @param data
	 * @param loadAddr
	 */
	private void DoChangeCodeType(String s, byte data[], int loadAddr) {
		// Dispose of any items that are already on the form
		if (HexTable != null) {
			HexTable.dispose();
			HexTable = null;
		}

		if (ImageLabel != null) {
			for (Label l : ImageLabel) {
				l.dispose();
			}
			ImageLabel = null;
		}

		// Render the appropriate type
		if (s.equals(CODETYPES[1])) {
			AddScreen(data);
		} else if (s.equals(CODETYPES[2])) {
			AddAsm(data, loadAddr);
		} else {
			AddBin(data, loadAddr);
		}
		MainPage.pack();
	}

	/**
	 * Render the code as ASM.
	 * 
	 * @param data
	 * @param startaddress
	 */
	private void AddAsm(byte data[], int startaddress) {
		HexTable = new Table(MainPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		HexTable.setLinesVisible(true);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = 400;
		HexTable.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(HexTable, SWT.LEFT);
		tc1.setText("Address");
		tc1.setWidth(80);
		TableColumn tc2 = new TableColumn(HexTable, SWT.LEFT);
		tc2.setText("Hex");
		tc2.setWidth(160);
		TableColumn tc3 = new TableColumn(HexTable, SWT.LEFT);
		tc3.setText("Asm");
		tc3.setWidth(160);
		TableColumn tc4 = new TableColumn(HexTable, SWT.LEFT);
		tc4.setText("Chr");
		tc4.setWidth(160);
		HexTable.setHeaderVisible(true);

		ASMLib asm = new ASMLib();
		int loadedaddress = startaddress;
		int realaddress = 0x0000;
		int asmData[] = new int[5];
		try {
			while (realaddress < data.length) {
				String chrdata = "";
				for (int i = 0; i < 5; i++) {
					int d = 0;
					if (realaddress + i < data.length) {
						d = (int) data[realaddress + i] & 0xff;
					}
					asmData[i] = d;

					if ((d > 0x1F) && (d < 0x7f)) {
						chrdata = chrdata + (char) d;
					} else {
						chrdata = chrdata + "?";
					}
				}
				// decode instruction
				DecodedASM Instruction = asm.decode(asmData, loadedaddress);
				// output it. - First, assemble a list of hex bytes, but pad out to 12 chars
				// (4x3)
				String hex = "";
				for (int j = 0; j < Instruction.length; j++) {
					hex = hex + String.format("%02X", asmData[j]) + " ";
				}

				TableItem Row = new TableItem(HexTable, SWT.NONE);
				String dta[] = new String[4];
				dta[0] = String.format("%04X", loadedaddress);
				dta[1] = hex;
				dta[2] = Instruction.instruction;
				dta[3] = chrdata.substring(0, Instruction.length);

				Row.setText(dta);

				realaddress = realaddress + Instruction.length;
				loadedaddress = loadedaddress + Instruction.length;

			} // while
		} catch (Exception E) {
			System.out.println("Error at: " + realaddress + "(" + loadedaddress + ")");
			System.out.println(E.getMessage());
			E.printStackTrace();
		}

	}

	protected void DoSaveFileAsAsm(byte[] data, Composite mainPage2, int loadAddr) {
		FileDialog fd = new FileDialog(MainPage.getShell(), SWT.SAVE);
		fd.setText("Save Assembly file as");
		String[] filterExt = { "*.*" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			try {
				Speccy.DoSaveFileAsAsm(data, selected, loadAddr);
			} catch (FileNotFoundException e) {
				MessageBox dialog = new MessageBox(MainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Directory not found!");
				dialog.open();

				e.printStackTrace();
			} catch (IOException e) {
				MessageBox dialog = new MessageBox(MainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("IO error: " + e.getMessage());
				dialog.open();

				e.printStackTrace();
			}
		}		
	}


	/**
	 * Save the file as an image file. (Note, this will be 256x192 of whatever
	 * format is selected)
	 * 
	 * @param data
	 * @param mainPage
	 */
	protected void DoSaveFileAsPic(byte[] data, Composite mainPage) {
		FileDialog fd = new FileDialog(MainPage.getShell(), SWT.SAVE);
		fd.setText("Save Assembly file as");
		String[] filterExt = { "*.jpg", "*.gif", "*.png", "*.bmp", "*.svg", "*.tiff", "*.ico" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			FileOutputStream file;
			try {
				file = new FileOutputStream(selected);
				try {
					ImageData image = Speccy.GetImageFromFileArray(data, 0x80);
					ImageLoader imageLoader = new ImageLoader();
					imageLoader.data = new ImageData[] { image };
					int filetyp = SWT.IMAGE_JPEG;
					if (selected.toLowerCase().endsWith(".gif")) {
						filetyp = SWT.IMAGE_GIF;
					} else if (selected.toLowerCase().endsWith(".png")) {
						filetyp = SWT.IMAGE_PNG;
					} else if (selected.toLowerCase().endsWith(".bmp")) {
						filetyp = SWT.IMAGE_BMP;
					} else if (selected.toLowerCase().endsWith(".svg")) {
						filetyp = SWT.IMAGE_SVG;
					} else if (selected.toLowerCase().endsWith(".tiff")) {
						filetyp = SWT.IMAGE_TIFF;
					} else if (selected.toLowerCase().endsWith(".ico")) {
						filetyp = SWT.IMAGE_ICO;
					}

					imageLoader.save(selected, filetyp);
				} finally {
					file.close();
				}
			} catch (FileNotFoundException e) {
				MessageBox dialog = new MessageBox(MainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Directory not found!");
				dialog.open();

				e.printStackTrace();
			} catch (IOException e) {
				MessageBox dialog = new MessageBox(MainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("IO error: " + e.getMessage());
				dialog.open();
				e.printStackTrace();
			}
		}
	}

}

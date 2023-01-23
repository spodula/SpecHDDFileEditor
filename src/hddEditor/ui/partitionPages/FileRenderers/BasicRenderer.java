package hddEditor.ui.partitionPages.FileRenderers;
//BUGFIX: GDS 22/01/2023 - Fixed error deciding when REM statements need to be disassembled. Was calculating with wrong values.
//BUGFIX: GDS 23/01/2023 - Now handles bad basic files a bit better when the +3Size> CPMsize 
//QOLFIX: GDS 22/01/2023 - DoSaveFileAsText: Now defaults to the current filename and puts in title bar.

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.ASMLib;
import hddEditor.libs.Speccy;
import hddEditor.libs.ASMLib.DecodedASM;

public class BasicRenderer extends FileRenderer {
	// Components
	private Text StartLineEdit = null;
	private Text VariableStartEdit = null;
	private Table Listing = null;
	private Table Variables = null;

	/**
	 * Version of Render that doesn't rely on the +3DOS header
	 * 
	 * @param mainPage
	 * @param data
	 * @param Filename
	 * @param filelength
	 * @param loadaddr
	 * @param fileSize
	 * @param VariablesOffset
	 * @param Startline
	 */
	public void RenderBasic(Composite mainPage, byte data[], byte header[], String Filename, int filelength,
			int VariablesOffset, int Startline) {
		this.filename = Filename;
		this.MainPage = mainPage;
		this.data = data;

		Label lbl = new Label(this.MainPage, SWT.NONE);
		lbl.setText("BASIC program: ");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		lbl = new Label(this.MainPage, SWT.NONE);
		lbl.setText("Start line: ");

		StartLineEdit = new Text(this.MainPage, SWT.NONE);
		StartLineEdit.setText("");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		StartLineEdit.setLayoutData(gd);

		lbl = new Label(this.MainPage, SWT.NONE);
		lbl.setText("Variable start: ");

		VariableStartEdit = new Text(this.MainPage, SWT.NONE);
		VariableStartEdit.setText("");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		VariableStartEdit.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;

		Button btn = new Button(this.MainPage, SWT.NONE);
		btn.setText("Extract file as text");
		btn.setLayoutData(gd);
		Composite mainpage = this.MainPage;
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsText(mainpage);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		btn = new Button(this.MainPage, SWT.NONE);
		btn.setText("Extract file as Binary");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsBin(data, mainpage, Filename);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);

			}
		});

		if (header != null) {
			btn = new Button(this.MainPage, SWT.NONE);
			btn.setText("Extract file as Binary Inc Header");
			btn.setLayoutData(gd);
			btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					byte newdata[] = new byte[data.length + header.length];
					System.arraycopy(header, 0, newdata, 0, header.length);
					System.arraycopy(data, 0, newdata, header.length, data.length);
					DoSaveFileAsBin(newdata, mainpage, Filename);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
		}

		btn = new Button(this.MainPage, SWT.NONE);
		btn.setText("Extract file as Hex");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsHex(data, mainpage, 0, filelength, Filename);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Listing = new Table(this.MainPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		Listing.setLinesVisible(true);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.heightHint = 300;
		gd.widthHint = mainpage.getSize().x;
		Listing.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(Listing, SWT.LEFT);
		tc1.setText("Line number");
		tc1.setWidth(80);
		TableColumn tc2 = new TableColumn(Listing, SWT.FILL);
		tc2.setText("Line");
		tc2.setWidth(600);

		AddBasicFile(filelength, VariablesOffset);

		StartLineEdit.setText(String.valueOf(Startline));
		VariableStartEdit.setText(String.valueOf(VariablesOffset));

		lbl = new Label(this.MainPage, SWT.NONE);
		lbl.setText("Variables: ");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		Variables = new Table(this.MainPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		Variables.setLinesVisible(true);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.heightHint = 100;
		gd.widthHint = this.MainPage.getSize().x;
		Variables.setLayoutData(gd);

		TableColumn vc1 = new TableColumn(Variables, SWT.LEFT);
		vc1.setText("Variable");
		vc1.setWidth(80);
		TableColumn vc2 = new TableColumn(Variables, SWT.FILL);
		vc2.setText("Type");
		vc2.setWidth(100);
		TableColumn vc3 = new TableColumn(Variables, SWT.FILL);
		vc3.setText("Content");
		vc3.setWidth(580);

		int varlen = Math.min(filelength,data.length) - VariablesOffset;
		if (varlen > 0) {
			byte variables[] = new byte[varlen];
			System.arraycopy(data, VariablesOffset, variables, 0, varlen);
			DecodeVariables(this.MainPage, variables);
		}
		this.MainPage.pack();

	}

	/**
	 * Add the file as BASIC.
	 * 
	 * @param filelength
	 * @param VariablesOffset
	 */
	private void AddBasicFile(int filelength, int VariablesOffset) {
		class RemDetails {
			public int locationAddr;
			public int size;
			public byte data[];
			public boolean valid = false;
			public int line;
		}

		Font mono = new Font(MainPage.getDisplay(), "Monospace", 10, SWT.NONE);

		ArrayList<RemDetails> RemLocations = new ArrayList<RemDetails>();

		int ptr = 0;
		int EndOfBasicArea = Math.min(filelength, VariablesOffset);
		while (ptr < EndOfBasicArea) {
			int linenum = -1;
			int linelen = 0;
			int OrigLineLen = 0;
			try {
				linenum = ((data[ptr++] & 0xff) * 256);
				linenum = linenum + (data[ptr++] & 0xff);
				linelen = (int) data[ptr++] & 0xff;
				linelen = linelen + ((int) (data[ptr++] & 0xff) * 256);
				// Record original line length for REM purposes.
				OrigLineLen = linelen;
				// fiddles bad line lengths
				linelen = Math.min(filelength - ptr + 4, linelen);
			} catch (Exception E) {
				System.out.println("Basic parsing error, bad linenum.");
				String details[] = new String[2];
				details[0] = "Invalid";
				details[1] = "Bad line number encountered.";
				ptr = 99999999;

				TableItem Row = new TableItem(Listing, SWT.NONE);
				Row.setText(details);
			}

			if ((ptr >= VariablesOffset) || (linenum < 0)) {
				// now into the variables area. Ignoring for the moment.
				ptr = 99999999;
			} else {
				String sixdigit = String.valueOf(linenum);
				while (sixdigit.length() < 6) {
					sixdigit = sixdigit + " ";
				}
				StringBuilder sb = new StringBuilder();
				try {
					RemDetails rd = new RemDetails();
					byte line[] = new byte[linelen];
					for (int i = 0; i < linelen; i++) {
						line[i] = data[ptr + i];
						// get the rem details
						if ((line[i] & 0xff) == (0xEA & 0xff)) {
							if (((i == 0) || (line[i - 1] == (byte) ':')) && !rd.valid) {
								rd.locationAddr = ptr + i + 1;
								int RestOfLine = OrigLineLen - i - 2; // Length = rest of line - terminating CR and the
																		// original REM statement
								int RestOfData = data.length - rd.locationAddr;
								RestOfLine = Math.min(RestOfLine, RestOfData);
								rd.size = RestOfLine;
								byte remdata[] = new byte[RestOfLine];
								System.arraycopy(data, rd.locationAddr, remdata, 0, rd.size);
								rd.data = remdata;
								rd.line = linenum;
								// Check to see if the REM is actually a REM.
								rd.valid = false;
								for (int RData = 0; RData < remdata.length; RData++) {
									if ((remdata[RData] < ' ') || ((remdata[RData] & 0xff) > 127)) {
										rd.valid = true;
									}
								}
							}
						}
					}
					if (rd.valid) {
						RemLocations.add(rd);
					}
					Speccy.DecodeBasicLine(sb, line, 0, linelen, false);
				} catch (Exception E) {
					sb.append("Bad line: " + E.getMessage());
					ptr = 99999999;
				}
				// point to next line.
				ptr = ptr + linelen;

				String details[] = new String[2];
				details[0] = sixdigit;
				details[1] = sb.toString();

				TableItem Row = new TableItem(Listing, SWT.NONE);
				Row.setText(details);
				Row.setFont(mono);
			}
		}
		if (RemLocations.size() > 0) {
			String details[] = new String[2];
			details[0] = "";
			details[1] = "Code in Rem statements:";
			TableItem Row = new TableItem(Listing, SWT.NONE);
			Row.setText(details);

			int baseaddress = 23867;

			for (RemDetails rd : RemLocations) {
				details = new String[2];
				details[0] = "Line: " + String.valueOf(rd.line);
				details[1] = rd.size + " bytes";
				Row = new TableItem(Listing, SWT.NONE);
				Row.setText(details);

				ASMLib asm = new ASMLib();
				int loadedaddress = baseaddress + rd.locationAddr;
				int realaddress = 0x0000;
				int asmData[] = new int[5];
				try {
					while (realaddress < rd.data.length) {
						String chrdata = "";
						for (int i = 0; i < 5; i++) {
							int d = 0;
							if ((realaddress + i) < rd.data.length) {
								d = (int) rd.data[realaddress + i] & 0xff;
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

						details = new String[2];
						while (hex.length() < 10) {
							hex = hex + " ";
						}
						details[0] = String.format("%04X", loadedaddress);
						String textline = hex + " " + Instruction.instruction;
						while (textline.length() < 40) {
							textline = textline + " ";
						}
						details[1] = textline + " " + chrdata.substring(0, Instruction.length);

						Row = new TableItem(Listing, SWT.NONE);
						Row.setText(details);
						Row.setFont(mono);

						realaddress = realaddress + Instruction.length;
						loadedaddress = loadedaddress + Instruction.length;

					} // while
				} catch (Exception E) {
					System.out.println("Error at: " + realaddress + "(" + loadedaddress + ")");
					System.out.println(E.getMessage());
					E.printStackTrace();
				}
			}

		}
	}

	/**
	 * Save file as text.
	 *
	 * @param mainPage
	 */
	protected void DoSaveFileAsText(Composite mainPage) {
		FileDialog fd = new FileDialog(mainPage.getShell(), SWT.SAVE);
		fd.setText("Save " + filename + " as text file");
		String[] filterExt = { "*.*" };
		fd.setFilterExtensions(filterExt);
		fd.setFileName(filename);
		String selected = fd.open();
		if (selected != null) {
			PrintWriter file;
			try {
				file = new PrintWriter(selected, "UTF-8");
				try {
					for (int line = 0; line < Listing.getItemCount(); line++) {
						TableItem itm = Listing.getItem(line);
						String lineno = itm.getText(0);
						String content = itm.getText(1);
						file.write(lineno.trim() + " ");
						file.write(content.trim() + System.lineSeparator());
					}
				} finally {
					file.close();
				}
			} catch (FileNotFoundException e) {
				MessageBox dialog = new MessageBox(mainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Directory not found!");
				dialog.open();

				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				MessageBox dialog = new MessageBox(mainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Internal error, cannot write UTF-8?");
				dialog.open();
				e.printStackTrace();
			}

		}
	}

	/**
	 * Decode the variables
	 * 
	 * @param mainPage
	 * @param VarData
	 */
	private void DecodeVariables(Composite mainPage, byte[] VarData) {
		int ptr = 0x00;
		if (ptr >= (VarData.length)) {
			TableItem Row = new TableItem(Variables, SWT.NONE);
			Row.setText(new String[] { "No Variables", "", "" });
		} else {
			while (ptr < VarData.length) {
				int var = (int) (VarData[ptr++] & 0xff);
				int vartype = var / 0x20;
				// char c = (char) ((var & 0x1f) + 0x60);

				if (vartype == 0x00) {
					// anything after this marker is junk so just skip it.
					TableItem Row = new TableItem(Variables, SWT.NONE);
					Row.setText(new String[] { "End of variables", "", "" });
					ptr = VarData.length;
				} else if (vartype == 1) {
					TableItem Row = new TableItem(Variables, SWT.NONE);
					Row.setText(new String[] { "Unknown type", "", "" });
					ptr = VarData.length;
				} else if (vartype == 2) { // string
					ptr = VariableType2(ptr, var, VarData);
				} else if (vartype == 3) { // number (1 letter)
					ptr = VariableType3(ptr, var, VarData);
				} else if (vartype == 4) { // Array of numbers
					ptr = VariableType4(ptr, var, VarData);
				} else if (vartype == 5) { // Number who's name is longer than 1 letter
					ptr = VariableType5(ptr, var, VarData);
				} else if (vartype == 6) { // array of characters
					ptr = VariableType6(ptr, var, VarData);
				} else if (vartype == 7) { // for/next control variable
					ptr = VariableType7(ptr, var, VarData);
				} else {
					System.out.print("UNKNOWN! $" + Integer.toHexString(var) + " at " + ptr);
				}
			}
		}

	}

	/**
	 * Handler for type 7 variables (FOR/NEXT variables) Variable name is parsed
	 * from the original marker (first 5 bytes) + 0x60 format is: byte: [0-4]
	 * Current Value of variable (Speccy FP representation) [5-9] TO value (Speccy
	 * FP representation) [10-14] STEP value (Speccy FP representation) [15]
	 * statement within FOR line to start looping from. (integer)
	 * 
	 * Like the previous ones, this is similar to the ZX81 8k Rom equivelent, except
	 * the addition of byte 15 (As the zx81 does not allow multiple statements on a
	 * line)
	 * 
	 * @param Address
	 * @param chr
	 * @param file
	 * @return
	 */
	private int VariableType7(int Address, int chr, byte[] file) {
		int varname = (chr & 0x1f);
		varname = varname + 0x40;
		String txt = "Value=" + String.valueOf(Speccy.GetNumberAtByte(file, Address));
		Address = Address + 5;
		txt = txt + " Limit=" + String.valueOf(Speccy.GetNumberAtByte(file, Address));
		Address = Address + 5;
		txt = txt + " Step=" + String.valueOf(Speccy.GetNumberAtByte(file, Address));
		Address = Address + 5;

		int lsb = (file[Address++] & 0xff);
		int msb = (file[Address++] & 0xff);
		int line = (msb * 256) + lsb;
		txt = txt + " Loop line=" + String.valueOf(line);

		int statement = (file[Address++] & 0xff);

		txt = txt + " Next Statement in line: " + String.valueOf(statement);

		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = (char) varname + "";
		row[1] = "For/Next variable";
		row[2] = txt;
		Row.setText(row);

		return (Address);
	}

	/**
	 * Handler for type 6 variables (Character arrays) Variable name is parsed from
	 * the original marker (first 5 bytes) + 0x60 format is: [0-1] data length (Used
	 * to quickly skip over variable when searching) [2] Number of dimensions
	 * (1-255) [3.4] First dimension size.(1-65535) .. [xx.yy] last dimension size
	 * [z] char for (1,0) [z] char for (2,0) ... [z] char for (<sz>,0) [z] char for
	 * (1,1) and so on.
	 * 
	 * @param Address
	 * @param chr
	 * @param file
	 * @return
	 */
	private int VariableType6(int Address, int chr, byte[] file) {
		int varname = (chr & 0x1f);
		varname = varname + 0x40;

		String txt = "(";
		Address = Address + 2;
		int dimensions = (file[Address++] & 0xff);

		int dims[] = new int[dimensions];
		int dimcounts[] = new int[dimensions];
		int numentries = 1;
		for (int i = 0; i < dimensions; i++) {
			int lsb = (file[Address++] & 0xff);
			int msb = (file[Address++] & 0xff);
			dims[i] = (msb * 256) + lsb;
			if (i > 0) {
				txt = txt + ",";
			}
			txt = txt + String.valueOf(dims[i]);
			numentries = numentries * dims[i];
			dimcounts[i] = 1;
		}
		txt = txt + ")";

		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = (char) varname + "$";
		row[1] = "Character array";
		row[2] = txt;
		Row.setText(row);

		return (Address + numentries);
	}

	/**
	 * Handler for type 5 variables (Number with a name > 1) Format: (Original char)
	 * [101XXXXX] where XXXXX = char of name - 0x60 [0] [000XXXXX] where XXXXX =
	 * char of name - 0x40 ... [Y] [100XXXXX] where XXXXX = char of name - 0x40 (Bit
	 * 7 is set to terminate string) [N1..N5] Speccy Floating point number
	 * 
	 * @param Address
	 * @param chr
	 * @param file
	 * @return
	 */
	private int VariableType5(int Address, int chr, byte[] file) {
		boolean done = false;
		String vn = "";
		while (!done) {
			int varname = (chr & 0x1f);
			varname = varname + 0x40;
			vn = vn + String.valueOf((char) varname);
			chr = file[Address++];
			done = (chr & 0x80) == 0x80;
		}
		int varname = (chr & 0x3f) + 0x40;
		vn = vn + String.valueOf((char) varname);

		double value = Speccy.GetNumberAtByte(file, Address);

		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = vn;
		row[1] = "Number (#5)";
		row[2] = String.valueOf(value);
		Row.setText(row);

		Address = Address + 5;
		return (Address);
	}

	/**
	 * Handler for type 4 variables (Numeric arrays) Variable name is parsed from
	 * the original marker (first 5 bytes) + 0x40 format is: [0-1] data length (Used
	 * to quickly skip over variable when searching) [2] Number of dimensions
	 * (1-255) [3.4] First dimension size. (1-65535) .. [xx.yy] last dimension size
	 * [ZZZZZ] Speccy FP representation of (1[,1[,1]]) [ZZZZZ] Speccy FP
	 * representation of (2[,1[,1]]) ... [ZZZZZ] Speccy FP representation of
	 * (xx[,1[,1]]) [ZZZZZ] Speccy FP representation of (1[,2[,1]]) and so on.
	 * 
	 * @param Address
	 * @param chr
	 * @param file
	 * @return
	 */
	private int VariableType4(int Address, int chr, byte[] file) {
		int numValues = 1;
		try {
			int varname = (chr & 0x1f);
			varname = varname + 0x40;
			String txt = "(";
			Address = Address + 2;
			int dimensions = (file[Address++] & 0xff);

			int dims[] = new int[dimensions];
			int dimcounts[] = new int[dimensions];
			for (int i = 0; i < dimensions; i++) {
				int lsb = (file[Address++] & 0xff);
				int msb = (file[Address++] & 0xff);
				dims[i] = (msb * 256) + lsb;
				if (i > 0) {
					txt = txt + ",";
				}
				numValues = numValues * dims[i];
				txt = txt + String.valueOf(dims[i]);
				dimcounts[i] = 1;
			}
			txt = txt + ")";

			TableItem Row = new TableItem(Variables, SWT.NONE);
			String row[] = new String[3];
			row[0] = (char) varname + "";
			row[1] = "Numeric Array";
			row[2] = txt;
			Row.setText(row);

		} catch (Exception E) {
			E.printStackTrace();
			System.out.println(E.getMessage());
		}

		return (Address + (numValues * 5));
	}

	/**
	 * Handler for type 3 variables (Numbers with a one character name) Variable
	 * name is parsed from the original marker (first 5 bits) + 0x40 [12345] Speccy
	 * floating point representation of the value.
	 * 
	 * @param Address
	 * @param chr
	 * @param file
	 * @return
	 */
	private int VariableType3(int Address, int chr, byte[] file) {
		int varname = (chr & 0x1f);
		varname = varname + 0x40;
		double value = Speccy.GetNumberAtByte(file, Address);
		String sValue = String.valueOf(value);

		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = (char) varname + "";
		row[1] = "Number (#3)";
		row[2] = sValue;
		Row.setText(row);

		Address = Address + 5;
		return (Address);
	}

	/**
	 * Handler for type 2 variables (Strings) Variable name is parsed from the
	 * original marker (first 5 bits) + 0x40 [1..2] String length lsb first [3..x]
	 * Characters making up the string.
	 * 
	 * @param Address
	 * @param chr
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private int VariableType2(int Address, int chr, byte[] file) {
		int varname = chr & 0x1f;
		varname = varname + 0x40;
		int lsb = (file[Address++] & 0xff);
		int msb = (file[Address++] & 0xff);
		int length = (msb * 256) + lsb;

		String s = "";
		while (length > 0 && Address < file.length) {
			char c = (char) file[Address++];
			s = s + Speccy.DecodeToken("" + c);
			length--;
		}

		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = (char) chr + "$";
		row[1] = "String";
		row[2] = s.trim();

		Row.setText(row);

		return (Address);
	}
}

package hddEditor.ui.partitionPages.FileRenderers.RawRender;

/**
 * Try to decode a given binary file as BASIC.
 * 
 */

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.libs.ASMLib;
import hddEditor.libs.Speccy;
import hddEditor.libs.ASMLib.DecodedASM;
import hddEditor.libs.Languages;

public class BasicRenderer implements Renderer {
	public Table Listing = null;
	public Table Variables = null;
	public Label VarLBL = null;

	@Override
	public void DisposeRenderer() {
		if (Listing != null) {
			Listing.dispose();
		}
		Listing = null;

		if (Variables != null) {
			Variables.dispose();
		}
		Variables = null;

		if (VarLBL != null) {
			VarLBL.dispose();
		}
		VarLBL = null;
	}

	public String GetBasicSummary(Languages lang) {
		String result = "";

		for (TableItem line : Listing.getItems()) {
			String lineno = line.getText(0);
			String content = line.getText(1);
			result = result + lineno + " " + content + "\n";
		}
		result = result + System.lineSeparator() + lang.Msg(Languages.MSG_VARIABLES) + ":" + System.lineSeparator();
		if (Variables != null && Variables.getItems() != null) {
			for (TableItem line : Variables.getItems()) {
				String varname = line.getText(0);
				String type = line.getText(1);
				String content = line.getText(2);
				result = result + varname + " " + type + " - " + content + "\n";
			}
		} else {
			result = result + lang.Msg(Languages.MSG_NOVARS) + ".";
		}
		return (result.trim());
	}

	/**
	 * Add the variables table.
	 * 
	 * @param TargetPage
	 * @param filelength
	 * @param VariablesOffset
	 */
	private void PopulateVariables(Composite TargetPage, byte data[], int filelength, int VariablesOffset,
			Languages lang) {
		Variables.removeAll();
		int varlen = Math.min(filelength, data.length) - VariablesOffset;
		if (varlen > 0) {
			byte variables[] = new byte[varlen];
			System.arraycopy(data, VariablesOffset, variables, 0, varlen);
			DecodeVariables(TargetPage, variables, lang);
		}
	}

	/**
	 * Add the file as BASIC.
	 * 
	 * @param filelength
	 * @param VariablesOffset
	 */
	public void AddBasicFile(Composite TargetPage, byte data[], int filelength, int VariablesOffset,
			boolean IncludeRealValues, Languages lang) {
		if (Listing != null) {
			Listing.dispose();
		}
		if (Variables != null) {
			Variables.dispose();
		}

		Listing = new Table(TargetPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		Listing.setLinesVisible(true);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.heightHint = 300;
		gd.widthHint = TargetPage.getSize().x;
		Listing.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(Listing, SWT.LEFT);
		tc1.setText(lang.Msg(Languages.MSG_LINENUM));
		tc1.setWidth(80);
		TableColumn tc2 = new TableColumn(Listing, SWT.FILL);
		tc2.setText(lang.Msg(Languages.MSG_LINE));
		tc2.setWidth(600);

		VarLBL = new Label(TargetPage, SWT.NONE);
		VarLBL.setText(lang.Msg(Languages.MSG_VARIABLES) + ": ");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		VarLBL.setLayoutData(gd);

		Variables = new Table(TargetPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		Variables.setLinesVisible(true);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.heightHint = 100;
		// gd.widthHint = TargetPage.getSize().x;
		Variables.setLayoutData(gd);

		TableColumn vc1 = new TableColumn(Variables, SWT.LEFT);
		vc1.setText(lang.Msg(Languages.MSG_VARIABLE));
		vc1.setWidth(80);
		TableColumn vc2 = new TableColumn(Variables, SWT.FILL);
		vc2.setText(lang.Msg(Languages.MSG_VARTYPE));
		vc2.setWidth(100);
		TableColumn vc3 = new TableColumn(Variables, SWT.FILL);
		vc3.setText(lang.Msg(Languages.MSG_CONTENTS));
		vc3.setWidth(580);

		PopulateBASIC(data, filelength, VariablesOffset, IncludeRealValues, lang);
		try {
			PopulateVariables(TargetPage, data, filelength, VariablesOffset, lang);
		} catch (Exception E) {

		}
		TargetPage.pack();
	}

	private void PopulateBASIC(byte data[], int filelength, int VariablesOffset, boolean IncludeRealVals,
			Languages lang) {
		class RemDetails {
			public int locationAddr;
			public int size;
			public byte data[];
			public boolean valid = false;
			public int line;
		}
		Listing.clearAll();
		Listing.removeAll();

		Font mono = new Font(Listing.getDisplay(), "Monospace", 10, SWT.NONE);

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
				System.out.println(lang.Msg(Languages.MSG_BASICPARSERR));
				String details[] = new String[2];
				details[0] = lang.Msg(Languages.MSG_INVALID);
				details[1] = lang.Msg(Languages.MSG_BADLINENO);
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
						// if the line length is corrupt, this feeds NULLS until the end of line.
						// Something this happens as part of anti-hack protection.
						if ((ptr + i) >= data.length) {
							line[i] = 0x00;
						} else {
							line[i] = data[ptr + i];
						}
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
					Speccy.DecodeBasicLine(sb, line, 0, linelen, IncludeRealVals);
				} catch (Exception E) {
					sb.append(lang.Msg(Languages.MSG_BADLINENO) + ": " + E.getMessage());
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
			details[1] = lang.Msg(Languages.MSG_REMCODE) + ":";
			TableItem Row = new TableItem(Listing, SWT.NONE);
			Row.setText(details);

			int baseaddress = 23867;

			for (RemDetails rd : RemLocations) {
				details = new String[2];
				details[0] = lang.Msg(Languages.MSG_LINE) + ": " + String.valueOf(rd.line);
				details[1] = rd.size + " " + lang.Msg(Languages.MSG_BYTES);
				Row = new TableItem(Listing, SWT.NONE);
				Row.setText(details);

				ASMLib asm = new ASMLib(lang);
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
					System.out.println(String.format(lang.Msg(Languages.MSG_ERRORATXX), realaddress, loadedaddress));
					System.out.println(E.getMessage());
					E.printStackTrace();
				}
			}
		}
	}

	/**
	 * Decode the variables
	 * 
	 * @param mainPage
	 * @param VarData
	 */
	private void DecodeVariables(Composite mainPage, byte[] VarData, Languages lang) {
		try {
			int ptr = 0x00;
			if (ptr >= (VarData.length)) {
				TableItem Row = new TableItem(Variables, SWT.NONE);
				Row.setText(new String[] { lang.Msg(Languages.MSG_NOVARS), "", "" });
			} else {
				while (ptr < VarData.length) {
					int var = (int) (VarData[ptr++] & 0xff);
					int vartype = var / 0x20;
					// char c = (char) ((var & 0x1f) + 0x60);

					if (vartype == 0x00) {
						// anything after this marker is junk so just skip it.
						TableItem Row = new TableItem(Variables, SWT.NONE);
						Row.setText(new String[] { lang.Msg(Languages.MSG_ENDOFVARS), "", "" });
						ptr = VarData.length;
					} else if (vartype == 1) {
						TableItem Row = new TableItem(Variables, SWT.NONE);
						Row.setText(new String[] { lang.Msg(Languages.MSG_UNKNOWNTYP), "", "" });
						ptr = VarData.length;
					} else if (vartype == 2) { // string
						ptr = VariableType2(ptr, var, VarData, lang);
					} else if (vartype == 3) { // number (1 letter)
						ptr = VariableType3(ptr, var, VarData, lang);
					} else if (vartype == 4) { // Array of numbers
						ptr = VariableType4(ptr, var, VarData, lang);
					} else if (vartype == 5) { // Number who's name is longer than 1 letter
						ptr = VariableType5(ptr, var, VarData, lang);
					} else if (vartype == 6) { // array of characters
						ptr = VariableType6(ptr, var, VarData, lang);
					} else if (vartype == 7) { // for/next control variable
						ptr = VariableType7(ptr, var, VarData, lang);
					} else {
						System.out.print(lang.Msg(Languages.MSG_UNKNOWNTYP).toUpperCase() + "! $"
								+ Integer.toHexString(var) + " at " + ptr);
					}
				}
			}
		} catch (Exception E) {
			TableItem Row = new TableItem(Variables, SWT.NONE);
			Row.setText(new String[] { lang.Msg(Languages.MSG_FAILEDTODECODE), "", "" });
		}
	}

	/*-
	 * Handler for type 7 variables (FOR/NEXT variables) Variable name is parsed
	 * from the original marker (first 5 bytes) + 0x60 
	 * format is: byte: 
	 * [0-4] Current Value of variable (Speccy FP representation) 
	 * [5-9] TO value (Speccy FP representation) 
	 * [10-14] STEP value (Speccy FP representation) 
	 * [15] statement within FOR line to start looping from. (integer)
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
	private int VariableType7(int Address, int chr, byte[] file, Languages lang) {
		int varname = (chr & 0x1f);
		varname = varname + 0x40;
		String txt = lang.Msg(Languages.MSG_VALUE) + "=" + String.valueOf(Speccy.GetNumberAtByte(file, Address));
		Address = Address + 5;
		txt = txt + " " + lang.Msg(Languages.MSG_LIMIT) + "=" + String.valueOf(Speccy.GetNumberAtByte(file, Address));
		Address = Address + 5;
		txt = txt + " " + lang.Msg(Languages.MSG_STEP) + "=" + String.valueOf(Speccy.GetNumberAtByte(file, Address));
		Address = Address + 5;

		int lsb = (file[Address++] & 0xff);
		int msb = (file[Address++] & 0xff);
		int line = (msb * 256) + lsb;
		txt = txt + " " + lang.Msg(Languages.MSG_LOOPLINE) + "=" + String.valueOf(line);

		int statement = (file[Address++] & 0xff);

		txt = txt + " " + lang.Msg(Languages.MSG_NEXTSTATE) + "=" + String.valueOf(statement);

		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = (char) varname + "";
		row[1] = lang.Msg(Languages.MSG_FORNEXT);
		row[2] = txt;
		Row.setText(row);

		return (Address);
	}

	/*-
	 * Handler for type 6 variables (Character arrays) Variable name is parsed from
	 * the original marker (first 5 bytes) + 0x60 
	 * format is: 
	 * [0-1] data length (Used to quickly skip over variable when searching) 
	 * [2] Number of dimensions (1-255) 
	 * [3.4] First dimension size.(1-65535) 
	 * .. 
	 * [xx.yy] last dimension size
	 * [z] char for (1,0) 
	 * [z] char for (2,0) 
	 * ... 
	 * [z] char for (<sz>,0) 
	 * [z] char for (1,1) and so on.
	 * 
	 * @param Address
	 * @param chr
	 * @param file
	 * @return
	 */
	private int VariableType6(int Address, int chr, byte[] file, Languages lang) {
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
		row[1] = lang.Msg(Languages.MSG_CHARARRAY);
		row[2] = txt;
		Row.setText(row);

		return (Address + numentries);
	}

	/*-
	 * Handler for type 5 variables (Number with a name > 1) 
	 * Format: (Original char) [101XXXXX] where XXXXX = char of name - 0x60 
	 * [0] [000XXXXX] where XXXXX = char of name - 0x40 
	 * ... 
	 * [Y] [100XXXXX] where XXXXX = char of name - 0x40 (Bit 7 is set to terminate string) 
	 * [N1..N5] Speccy Floating point number
	 * 
	 * @param Address
	 * @param chr
	 * @param file
	 * @return
	 */
	private int VariableType5(int Address, int chr, byte[] file, Languages lang) {
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
		row[1] = lang.Msg(Languages.MSG_NUMBER) + " (#5)";
		row[2] = String.valueOf(value);
		Row.setText(row);

		Address = Address + 5;
		return (Address);
	}

	/*-
	 * Handler for type 4 variables (Numeric arrays) Variable name is parsed from
	 * the original marker (first 5 bytes) + 0x40 format is: 
	 * [0-1] data length (Used to quickly skip over variable when searching) 
	 * [2] Number of dimensions (1-255) 
	 * [3.4] First dimension size. (1-65535) 
	 * .. 
	 * [xx.yy] last dimension size
	 * [ZZZZZ] Speccy FP representation of (1[,1[,1]]) 
	 * [ZZZZZ] Speccy FP representation of (2[,1[,1]]) 
	 * ... 
	 * [ZZZZZ] Speccy FP representation of (xx[,1[,1]]) 
	 * [ZZZZZ] Speccy FP representation of (1[,2[,1]]) and so on.
	 * 
	 * @param Address
	 * @param chr
	 * @param file
	 * @return
	 */
	private int VariableType4(int Address, int chr, byte[] file, Languages lang) {
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
			row[1] = lang.Msg(Languages.MSG_NUMARRAY); 
			row[2] = txt;
			Row.setText(row);

		} catch (ArrayIndexOutOfBoundsException E) {
			System.out.println(lang.Msg(Languages.MSG_BADNUMARRAY));
			// Ignore
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
	private int VariableType3(int Address, int chr, byte[] file, Languages lang) {
		int varname = (chr & 0x1f);
		varname = varname + 0x40;
		double value = Speccy.GetNumberAtByte(file, Address);
		String sValue = String.valueOf(value);

		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = (char) varname + "";
		row[1] = lang.Msg(Languages.MSG_NUMBER) + " (#3)";
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
	private int VariableType2(int Address, int chr, byte[] file, Languages lang) {
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
		row[1] = lang.Msg(Languages.MSG_STRING); 
		row[2] = s.trim();

		Row.setText(row);

		return (Address);
	}

}

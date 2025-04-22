package hddEditor.libs;

//QOLFIX: GDS 22/01/2023 - Removed the HTML elements from the token tables - This was a hangover from the old disk image reader than rendered using HTML.
//QOLFIX: GDS 22/01/2023 - Changed to use System.lineseperator rather than hardcoding \r\n
//BUGFIX: GDS 23/01/2023 - Now handles bad basic files a bit better when the +3Size> CPMsize 
//QOLFIX: GDS 23/01/2023 - Changed to output XML to write to file
//BUGFIX: GDS 23/01/2023 - Better exception handling, stopping bad variables corrupting whole file.
//BUGFIX: GDS 23/01/2023 - Better handling of arrays with lots of dimensions.
//QOLFIX: GDS 23/01/2023 - DoSaveFileAsAsm: Now uses fixed format strings so data export looks better.
//QOLFIX: GDS 23/01/2023 - Added progress bar for export.
//TODO: parsing still seems to be broken. EG: 
/*
50 let a=32771
60 for b=1 to 16
70 print peek a;" ";peek (a+1);" ";peek (a+2);" ";peek (a+3);" ";peek (a+4)
80 let a=a+12
90 next b
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;

import hddEditor.libs.ASMLib.DecodedASM;

public class Speccy {
	// TAPE block types
	public static int TAPE_HEADER = 0x00;
	public static int TAPE_DATA = 0xff;
	public static int TAPE_HEADER_LEN = 17;

	// Spectrum colours
	public static int COLOUR_BLACK = 0;
	public static int COLOUR_BLUE = 1;
	public static int COLOUR_RED = 2;
	public static int COLOUR_MAGENTA = 3;
	public static int COLOUR_GREEN = 4;
	public static int COLOUR_CYAN = 5;
	public static int COLOUR_YELLOW = 6;
	public static int COLOUR_WHITE = 7;

	// Colour names
	public final static String[] SPECTRUM_COLOURS = { "Black", "Blue", "Red", "Magenta", "Green", "Cyan", "Yellow",
			"White" };

	// Start of the commands in the Spectrum character set. Tokenising starts here.
	public final static int SPECCY_CMD_START = 0xA3;

	// Prefix for numbers in basic files.
	public final static byte NUMSTART = 0x0E;

	// Spectrum file types
	public static final int BASIC_BASIC = 0x00;
	public static final int BASIC_NUMARRAY = 0x01;
	public static final int BASIC_CHRARRAY = 0x02;
	public static final int BASIC_CODE = 0x03;

	public static final String[] filetypeNames = { "Basic", "Numeric array", "Character array", "Code" };

	public static String SpecFileTypeToString(int filetype) {
		String result = "Unknown";
		if (filetype > -1 && filetype < filetypeNames.length) {
			result = filetypeNames[filetype];
		}
		return (result);
	}

	/**
	 * Testing for the number conversion
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// test for numeric encoding and decoding.
		System.out.println("===============Numeric encoding test================");
		System.out.println("The value and the re-encoded value should match within 31 bits ( Approx 9-10)");
		System.out.println("Value         neg    00      01      02      03      04      Re-encoded value");
		outputnum(-3212.0);
		outputnum(4096.0);
		outputnum(1323234.0);
		outputnum(23.0);
		outputnum(14096.1);

		// Test for line tokenisation and encoding
		System.out.println("===============Line tokenisation test================");
		// String line = "10 print at 21,0;PAPER 6;INK 0;\"Loading BAK2SCL2\"";
		String line = "10 if a<=10 then goto 100";
		System.out.println("Line to decode: " + line);
		byte target[] = new byte[1024];
		int last = DecodeBasicLine(line, target, 0);

		System.out.println("Result:");
		for (int i = 0; i < last; i++) {
			String s = String.format("%2d %3d (%2x)", i, target[i], target[i]);
			if (target[i] >= ' ' && target[i] < 0x7f) {
				s = s + " " + String.valueOf((char) target[i]);
			}
			System.out.println(s);
		}
	}

	/**
	 * used for test purposes only. Encode and then re-encode a number to and from a
	 * speccy Floating point number and output it.
	 * 
	 * @param x
	 */
	static void outputnum(Double x) {
		// number
		String s = String.format(" %10.2f ", x);
		// output the encoded value
		byte num[] = EncodeValue(x, false);
		if (num.length != 7) {
			s = s + "        ";
		}
		for (byte n : num) {
			int i = (int) (n & 0xff);
			s = s + String.format("%3d(%02x) ", i, i);
		}
		// re-encode the number and output it. should match within 31 bits ( Approx 9-10
		// decimal places)
		int base = 1;
		if (num.length == 7)
			base = 2;
		s = s + GetNumberAtByte(num, base);
		System.out.println(s);
	}

	// zx spectrum colours in SWT RGB (This differs from HTML RGB)
	public static final int colours[] = { 0x000000, 0xD70000, 0x0000D7, 0xD700D7, 0x00D700, 0xD7D700, 0x00D7D7,
			0xD7D7D7 };
	public static final int coloursBright[] = { 0x000000, 0xFF0000, 0x0000FF, 0xFF00FF, 0x00FF00, 0xFFFF00, 0x00FFFF,
			0xFFFFFF };

	// zx Spectrum tokens from 0 to 255
	public static final String[] tokens = { "", "", "", "", "", "", "<tab>", "", "<left>", "<right>", "", "", "",
			"<nl>", "<num>", "",
			// 10
			"<ink>", "<paper>", "<flash>", "<bright>", "<inverse>", "<over>", "<at>", "<tab>", "", "", "", "", "", "",
			"", "",
			// 20
			" ", "!", "\"", "#", "$", "%", "&;", "'", "(", ")", "*", "+", ",", "-", ".", "/",
			// 30
			"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ":", ";", "<", "=", ">", "?",
			// 40
			"@", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
			// 50
			"P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "[", "\\", "]", "^", "_",
			// 60
			"&", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o",
			// 70
			"p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "{", "|", "}", "~", "" + (char) 0x24B8, // (C)
			// 80
			// block characters:
			// BA
			// DC
			" ", "" + (char) 0x259D, // A
			"" + (char) 0x2598, // B
			"" + (char) 0x2580, // BA
			"" + (char) 0x2597, // C
			"" + (char) 0x2590, // C A
			"" + (char) 0x259a, // CB
			"" + (char) 0x259c, // CBA
			"" + (char) 0x2596, // D
			"" + (char) 0x259E, // D A
			"" + (char) 0x258C, // D B
			"" + (char) 0x258B, // D BA
			"" + (char) 0x2584, // DC
			"" + (char) 0x259F, // DC A
			"" + (char) 0x2599, // DCB
			"" + (char) 0x2588, // DCBA
			// 0x90
			"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
			// 0xA0
			"Q", "R", "S", " SPECTRUM ", " PLAY ", " RND ", " INKEY$ ", " PI ", " FN ", " POINT ", " SCREEN$ ",
			" ATTR ", " AT ", " TAB ", " VAL$ ", " CODE ",
			// 0xB0
			"VAL ", "LEN ", "SIN ", "COS ", "TAN ", "ASN ", "ACS ", "ATN ", "LN ", "EXP ", "INT ", "SQR ", "SGN ",
			"ABS ", "PEEK ", "IN ",
			// 0xC0
			"USR ", "STR$ ", "CHR$ ", "NOT ", "BIN ", " OR ", " AND ", "<=", ">=", "<>", " LINE ", " THEN ", " TO ",
			" STEP ", " DEF FN ", " CAT ",
			// 0xD0
			" FORMAT ", " MOVE ", " ERASE ", " OPEN# ", " CLOSE# ", " MERGE ", " VERIFY ", " BEEP ", " CIRCLE ",
			" INK ", " PAPER ", " FLASH ", " BRIGHT ", " INVERSE ", " OVER ", " OUT ",
			// 0xE0
			" LPRINT ", " LLIST ", " STOP ", " READ ", " DATA ", " RESTORE ", " NEW ", " BORDER ", " CONTINUE ",
			" DIM ", " REM ", " FOR ", " GOTO ", " GOSUB ", " INPUT ", " LOAD ",
			// 0xF0
			" LIST ", " LET ", " PAUSE ", " NEXT ", " POKE ", " PRINT ", " PLOT ", " RUN ", " SAVE ", " RANDOMISE ",
			" IF ", " CLS ", " DRAW ", " CLEAR ", " RETURN ", " COPY " };

	/**
	 * Try to decode the given string into a Speccy token.
	 * 
	 * @param token
	 * @return
	 */
	public static String DecodeToken(String token) {
		String uToken = token.toUpperCase().trim();
		String result = "";
		// Check for a number
		if (isNumeric(token)) {
			Double d = Double.parseDouble(token);
			result = String.valueOf(d);
			// Is an integer? if so, dont put leading zeroes in the text.
			if (d == Math.floor(d)) {
				// Encode as Integer
				int di = d.intValue();
				result = String.valueOf(di);
			}
			// Get the encoded value and add it to the result.
			byte num[] = EncodeValue(d, false);
			for (byte n : num) {
				result = result + (char) n;
			}

		} else {
			// Check for a matching token.
			int tokenNum = 0;
			for (int i = SPECCY_CMD_START; i < Speccy.tokens.length; i++) {
				String possToken = Speccy.tokens[i].trim();
				possToken = possToken.replace("RANDOMIZE", "RANDOMISE");

				if (!possToken.isEmpty() && possToken.equals(uToken)) {
					tokenNum = i;
				}
			}
			// if we find one, return this.
			if (tokenNum != 0) {
				result = result + (char) tokenNum;
			} else {
				// otherwise convert into Speccy ASCII (Mostly the same as real ascii with two
				// exceptions....
				token = token.replace("(C)", "" + 0x7f);

				for (int i = 0; i < token.length(); i++) {
					int chr = token.charAt(i);
					if (chr == '@') {
						chr = 0x40;
					}
					if (chr == '£') {
						chr = 0x60;
					}
					if (chr < ' ') {
						result = result + "\0x" + Integer.toHexString(chr);
					} else {
						result = result + (char) chr;
					}
				}
			}
		}
		return result;
	}

	/**
	 * 
	 * @param data
	 * @param loc
	 * @param value
	 */
	private static void SetCharArray(char data[], int loc, String value) {
		int ptr = 0;
		value = value.trim();
		while ((loc < data.length) && (ptr < value.length())) {
			data[loc++] = value.charAt(ptr++);
		}
	}

	/**
	 * Save the file as ASM
	 * 
	 * @param data
	 * @param mainPage2
	 * @param loadAddr
	 * @throws IOException
	 */
	public static void DoSaveFileAsAsm(byte[] data, File filename, int loadAddr) {
		StringBuilder sb = new StringBuilder();
		try {
			String cr = System.lineSeparator();
			sb.append("File: " + filename + cr);
			sb.append("Org: " + loadAddr + cr);
			sb.append("Length: " + data.length + cr + cr);
			ASMLib asm = new ASMLib();
			int loadedaddress = loadAddr;
			int realaddress = 0x0000;
			try {
				int asmData[] = new int[5];
				while (realaddress < data.length) {
					char chrdata[] = new char[5];
					for (int i = 0; i < 5; i++) {
						int d = 0;
						if (realaddress + i < data.length) {
							d = (int) data[realaddress + i] & 0xff;
						}
						asmData[i] = d;

						if ((d > 0x1F) && (d < 0x7f)) {
							chrdata[i] = (char) d;
						} else {
							chrdata[i] = '?';
						}
					}
					// decode instruction
					DecodedASM Instruction = asm.decode(asmData, loadedaddress);

					char Line[] = new char[80];
					for (int i = 0; i < Line.length; i++) {
						Line[i] = ' ';
					}
					SetCharArray(Line, 0, String.format("%04X\t", loadedaddress));

					// output it. - First, assemble a list of hex bytes, but pad out to 12 chars
					// (4x3)
					int loc = 6;
					for (int j = 0; j < Instruction.length; j++) {
						SetCharArray(Line, loc, String.format("%02X", asmData[j]));
						loc = loc + 3;
					}

					String s = Instruction.instruction;
					SetCharArray(Line, 20, s);
					SetCharArray(Line, 40, new String(chrdata).substring(0, Instruction.length));
					sb.append(new String(Line).trim());
					sb.append(cr);
					// gds 20 Aug 2023, extra CRLF after a RET or a JP
					if (Instruction.length > 0) {
						int firstbyte = asmData[0];
						if ((firstbyte == 195) || (firstbyte == 201)) {
							sb.append(cr);
						}
					}

					realaddress = realaddress + Instruction.length;
					loadedaddress = loadedaddress + Instruction.length;
				} // while
			} catch (Exception E) {
				System.out.println("Error at: " + realaddress + "(" + loadedaddress + ")");
				System.out.println(E.getMessage());
				E.printStackTrace();
			}
		} finally {
			GeneralUtils.WriteBlockToDisk(sb.toString().getBytes(), filename);
		}
	}

	/**
	 * Encode a given number as a Speccy 6 byte floating point representation (as a
	 * special case, 7 byte if a negative integer)
	 * 
	 * FP notation [EEEEEEEE] [Sxxxxxxx] [xxxxxxxx] [xxxxxxxx] [xxxxxxxx] where E is
	 * the exponent -0x80, S is sign, and xxx is 31 bits of the mantissa with bit 32
	 * (Where the sign is) assumed to be 1
	 * 
	 * INT notation [00000000] [00000000] [hhhhhhhh] [llllllll] [00000000] where h
	 * is the high byte and l is the low byte
	 * 
	 * 
	 * @param d number to encode
	 * @return encoded 6 byte array
	 */
	public static byte[] EncodeValue(Double d, boolean FloatInsteadOfNeg) {
		// Get the sign of the number and flip it (Common to both int and float
		// representation)
		byte result[] = new byte[6];
		result[0] = NUMSTART; // number starts with 0x0e
		boolean sign = (d < 0);

		// check to see if we can encode as a 16 bit integer.
		if ((d == Math.floor(d)) && ((d < 65536) && (d > -65536) && !FloatInsteadOfNeg) || ((d > 0) && (d < 65535))) {
			// Encode as Integer
			int di = d.intValue();
			int base = 1;
			// we can only encode positive integers, so we put a negative sign in front for
			// negatives and neg the number.
			if (sign) {
				di = -di;
				result = new byte[7];
				result[0] = '-'; // -
				result[base++] = NUMSTART;
			}
			// fill in the array
			result[base++] = 0; // integer
			result[base++] = 0; //
			result[base++] = (byte) (di & 0xff);
			result[base++] = (byte) (di / 0x100);
			result[base++] = 0;
		} else {
			// encode as float
			// firstly if negative, make positive
			if (sign) {
				d = -d;
			}
			// work out the exponent of the number
			int exp = (int) (Math.log(d) / Math.log(2));
			if ((exp < -129) || (exp > 126)) {
				System.out.println("Number out of range");
			} else {
				// We need to move the number to the highest bits of a 32 bit number.
				Double dv = Math.pow(2, exp); // what we need to divide by to normalise the number (so its 1.xxxxx)
				long divisor = dv.longValue();

				Double dMantissa = d / divisor; // Now multiply it back up again so bit 32 is set. (Do it in two stages
												// because of java)
				dMantissa = dMantissa * 0x8000;
				dMantissa = dMantissa * 0x10000;
				dMantissa = dMantissa + 0.5; // add in the fiddle factor.
				long mantissa = Math.round(dMantissa); // and get it as long integer
				// We now have the value we need to write to the 4 mantissa bytes.

				// byte1 1 = exponent based around 0x80
				result[1] = (byte) ((exp + 0x81) & 0xff);

				// byte2 = highest bits of the mantissa with the final but removed.(It is
				// assumed)
				result[2] = (byte) ((mantissa >> 24) & 0x7f);
				// the highest bit of the mantissa bytes holds the sign
				if (sign)
					result[2] = (byte) (result[2] | 0x80);

				// the rest of the mantissa.
				result[3] = (byte) ((mantissa >> 16) & 0xff);
				result[4] = (byte) ((mantissa >> 8) & 0xff);
				result[5] = (byte) (mantissa & 0xff);
			}
		}

		return (result);
	}

	/**
	 * decode the number at the given byte.
	 * 
	 * @param line
	 * @param location
	 * @return
	 */
	public static double GetNumberAtByte(byte line[], int location) {
		int exponent = ((byte) line[location] & 0xff);
		double fVal = 0;
		// exponent of zero marks this an integer.
		if (exponent != 0) {
			// extract the sign
			boolean sign = (line[location + 1] & 0x80) == 0x80;
			// Extract the 32 bit number, merging "1" in the MSB.
			long value = ((byte) line[location + 1] & 0x7f) + 0x80;
			value = value * 256 + ((byte) line[location + 2] & 0xff);
			value = value * 256 + ((byte) line[location + 3] & 0xff);
			value = value * 256 + ((byte) line[location + 4] & 0xff);

			// Get the the exponent. Note 0x80=2^0
			exponent = exponent - 0x80;

			// output is a floating point value so transfer my long word there.
			fVal = value;

			// note, done like this in stages because Java treats these as 32 bit integers,
			// its too big to do on one go.
			fVal = fVal / 65536;
			fVal = fVal / 65536;
			// shift via the exponent... (2^exponent)
			fVal = fVal * Math.pow(2, exponent);
			// negate if required.
			if (sign) {
				fVal = -fVal;
			}
		} else {
			// Read the number[2,3] as a 16 bit int
			fVal = (line[location + 2] & 0xff) + ((line[location + 3] & 0xff) * 256);
		}
		return (fVal);
	}

	/**
	 * Decode a single basic line
	 * 
	 * @param sb               String builder to add the resultant text to
	 * @param line             the bytes containing the line
	 * @param offset           start of the line (After its line number and length
	 *                         words)
	 * @param linelength       Line length
	 * @param DisplayValueOnly if TRUE wont try to extract the real FP value stored
	 *                         after the text for numbers.
	 */
	public static void DecodeBasicLine(StringBuilder sb, byte line[], int offset, int linelength,
			boolean DisplayValueOnly) {
		int ptr = 0;
		// REMs are treated differently. Just output the characters. Don't try to
		// tokenise them.
		boolean inrem = ((int) (line[0] & 0xff) == 0xEA);
		while (ptr < linelength - 1) {
			int chr = (int) line[ptr] & 0xff;
			if (inrem || (chr != NUMSTART)) {
				String tokenvalue = Speccy.tokens[chr];
				if (DisplayValueOnly) {
					tokenvalue = tokenvalue.replace("GOSUB", "GO SUB");
					tokenvalue = tokenvalue.replace("GOTO", "GO TO");
				}

				sb.append(tokenvalue);
			} else {
				if (!DisplayValueOnly) {
					double fpv = Speccy.GetNumberAtByte(line, ptr + 1);
					sb.append("(");
					sb.append(String.valueOf(fpv));
					sb.append(")");
				}
				ptr = ptr + 5; // skip the number
			}
			ptr++;
		}
	}

	/**
	 * Take the passed in FILE and append a basic listing to the stringbuilder
	 * 
	 * @param file             - data to parse
	 * @param sb               - string buiffer
	 * @param header           - +3 dos header, mainly used to determine the end of
	 *                         the program area.
	 * @param DisplayValueOnly - Dont display the actual value of any numbers after
	 *                         the real one.
	 */
	public static void DecodeBasicFromLoadedFile(byte file[], StringBuilder sb, int VariablesOffset,
			boolean DisplayValueOnly, boolean SkipHeader) {
		int ptr = 128; // skip the file header
		int EndOfBasicArea = Math.min(file.length, VariablesOffset + 128);
		if (!SkipHeader) {
			ptr = 0;
			EndOfBasicArea = Math.min(file.length, VariablesOffset);
		}
		try {
			while (ptr < EndOfBasicArea) {
				int linenum = ((file[ptr++] & 0xff) * 256);
				linenum = linenum + (file[ptr++] & 0xff);
				int linelen = (int) file[ptr++] & 0xff;
				linelen = linelen + ((int) (file[ptr++] & 0xff) * 256);

				if (ptr >= VariablesOffset + 0x80) {
					// now into the variables area. Ignoring for the moment.
					ptr = file.length;
				} else {
					String sixdigit = String.valueOf(linenum);
					while (sixdigit.length() < 6) {
						sixdigit = sixdigit + " ";
					}

					sb.append(sixdigit);

					byte line[] = new byte[linelen];
					for (int i = 0; i < linelen; i++) {
						if (ptr + i < file.length) {
							line[i] = file[ptr + i];
						}
					}

					Speccy.DecodeBasicLine(sb, line, 0, linelen, DisplayValueOnly);

					// point to next line.
					ptr = ptr + linelen;

					sb.append(System.lineSeparator());
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Ran out of data");
		}
	}

	/**
	 * Copied from:
	 * https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
	 * Returns if a given number is a valid number.
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean isHex(String str) {
		try {
			Integer.parseInt(str, 16);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Decode the variables area of a basic program.
	 * 
	 * @param file
	 * @param sb
	 * @param header
	 */
	private static void DecodeVariablesFromLoadedFile(byte[] file, StringBuilder sb, int VariablesOffset,
			int filesize) {
		int ptr = VariablesOffset;
		int varsize = filesize - VariablesOffset;

		sb.append("<variables>" + System.lineSeparator());
		sb.append("<varsize>" + varsize + "</varsize>" + System.lineSeparator());
		sb.append("<offset>" + VariablesOffset + "</offset>" + System.lineSeparator());
		if (file.length < filesize) {
			sb.append(
					"<err>The file is smaller than reported by the basic header. Variables will be incorrect. Reported size: "
							+ filesize + " Actual size: " + file.length + "</err>" + System.lineSeparator());
		}
		int ActualVarSize = Math.min(file.length, filesize) - VariablesOffset;
		sb.append("<rawdata>" + System.lineSeparator());
		if (ActualVarSize > 0) {
			sb.append(GeneralUtils.HexDump(file, VariablesOffset, ActualVarSize, 0));
		}
		sb.append("</rawdata>" + System.lineSeparator());

		ptr = VariablesOffset;
		if (ptr >= file.length) {
			sb.append("No variables");
		} else {
			while (ptr < filesize) {
				int var = (int) (file[ptr++] & 0xff);
				int vartype = var / 0x20;
				try {
					switch (vartype) {
					case 0:
						sb.append("End of variables" + System.lineSeparator());
						ptr = filesize;
						break;
					case 2:
						ptr = VariableType2(sb, ptr, var, file);
						break;
					case 3:
						ptr = VariableType3(sb, ptr, var, file);
						break;
					case 4:
						ptr = VariableType4(sb, ptr, var, file);
						break;
					case 5:
						ptr = VariableType5(sb, ptr, var, file);
						break;
					case 6:
						ptr = VariableType6(sb, ptr, var, file);
						break;
					case 7:
						ptr = VariableType7(sb, ptr, var, file);
						break;
					default:
						String s = "Bad variable - Invalid type 0x" + Integer.toHexString(vartype) + " at " + ptr
								+ System.lineSeparator();
						sb.append(s);
						System.out.print(s);
						break;
					}
				} catch (Exception E) {
					sb.append("<error>" + System.lineSeparator());
					sb.append("  <vartype>" + vartype + "</vartype>" + System.lineSeparator());
					sb.append("  <ptr>" + String.valueOf(ptr - 1) + "</ptr>" + System.lineSeparator());
					sb.append("  <errorstr>" + E.getMessage() + "</errorstr>" + System.lineSeparator());
					sb.append("  <errorname>" + E + "</errorname>" + System.lineSeparator());
					sb.append("  <stacktrace>" + System.lineSeparator());
					StringWriter dummy = new StringWriter();
					PrintWriter pw = new PrintWriter(dummy);
					E.printStackTrace(pw);
					String stacktrace = dummy.toString().replace(System.lineSeparator(),
							System.lineSeparator() + "    ");
					sb.append("    " + stacktrace);
					sb.append("  </stacktrace>" + System.lineSeparator());
					sb.append("</error>" + System.lineSeparator());

					/*
					 * if we have an error, its safe to assume the rest of the variables area is
					 * screwed up not least of which because the pointer to the next variable will
					 * now be bad, so jump out.
					 */
					ptr = filesize;
				}
			}
		}
		sb.append("</variables>" + System.lineSeparator());
	}

	/**
	 * Handler for type 2 variables (Strings) Variable name is parsed from the
	 * original marker (first 5 bytes) + 0x40 [1..2] String length lsb first [3..x]
	 * Characters making up the string.
	 * 
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType2(StringBuilder sb, int Address, int chr, byte[] file) {
		sb.append("<variable>" + System.lineSeparator());
		try {
			sb.append("  <type>2</type>" + System.lineSeparator());
			sb.append("  <typename>String</typename>" + System.lineSeparator());
			int varname = chr & 0x3f;
			varname = varname + 0x40;
			int LenLsb = (file[Address++] & 0xff);
			int LenMsb = (file[Address++] & 0xff);
			int length = (LenMsb * 256) + LenLsb;
			String VarContent = "";
			sb.append("  <varname>" + String.valueOf((char) varname) + "$</varname>" + System.lineSeparator());
			while (length > 0) {
				char c = (char) file[Address++];
				VarContent = VarContent + DecodeToken("" + c);
				length--;
			}
			sb.append("  <content>" + VarContent + "</content>" + System.lineSeparator());
		} finally {
			sb.append("</variable>" + System.lineSeparator());
		}
		return (Address);
	}

	/**
	 * Handler for type 3 variables (Numbers with a one character name) Variable
	 * name is parsed from the original marker (first 5 bytes) + 0x40 [12345] Speccy
	 * floating point representation of the value.
	 * 
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType3(StringBuilder sb, int Address, int chr, byte[] file) {
		sb.append("<variable>" + System.lineSeparator());
		try {
			int varname = chr & 0x3f;
			varname = varname + 0x40;
			double value = GetNumberAtByte(file, Address);

			sb.append("  <type>3</type>" + System.lineSeparator());
			sb.append("  <typename>Number (1 character name)</typename>" + System.lineSeparator());
			sb.append("  <varname>" + String.valueOf((char) varname) + "</varname>" + System.lineSeparator());
			sb.append("  <content>" + String.valueOf(value) + "</content>" + System.lineSeparator());
			Address = Address + 5;
		} finally {
			sb.append("</variable>" + System.lineSeparator());
		}
		return (Address);
	}

	/*-
	 * Handler for type 4 variables (Numeric arrays) Variable name is parsed from
	 * the original marker (first 5 bytes) + 0x40 format is: 
	 * [0-1] data length (Used to quickly skip over variable when searching) 
	 * [2] Number of dimensions (1-255) 
	 * [3-4] First dimension size. (1-65535) 
	 * .. 
	 * [xx.yy] last dimension size
	 * [ZZZZZ] Speccy FP representation of (1[,1[,1]]) 
	 * [ZZZZZ] Speccy FP representation of (2[,1[,1]]) 
	 * ... 
	 * [ZZZZZ] Speccy FP representation of (<sz>[,1[,1]]) 
	 * [ZZZZZ] Speccy FP representation of (1[,2[,1]]) and so on.
	 * 
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType4(StringBuilder sb, int Address, int chr, byte[] file) {
		sb.append("<variable>" + System.lineSeparator());
		try {
			int varname = chr & 0x3f;
			varname = varname + 0x40;
			String cr = System.lineSeparator();
			sb.append("  <type>4</type>" + cr);
			sb.append("  <typename>Numeric array</typename>" + cr);
			sb.append("  <varname>" + String.valueOf((char) varname) + "</varname>" + cr);

			try {
				Address = Address + 2;
				int dimensions = (file[Address++] & 0xff);
				StringBuilder TempSB = new StringBuilder();
				TempSB.append("  <dimensions>" + cr);
				int dims[] = new int[dimensions];
				int dimcounts[] = new int[dimensions];
				TempSB.append("  <numdimensions>" + dimensions + "<numdimensions>" + cr);
				for (int i = 0; i < dimensions; i++) {
					int lsb = (file[Address++] & 0xff);
					int msb = (file[Address++] & 0xff);
					dims[i] = (msb * 256) + lsb;
					TempSB.append("    <dimension>" + dims[i] + "<dimension>" + cr);
					dimcounts[i] = 1;
				}
				TempSB.append("  </dimensions>" + cr);
				sb.append(TempSB.toString());
				sb.append("  <content>" + cr);
				sb.append("{" + cr + "  ");
				boolean first = false;
				boolean done = false;
				while (!done) {
					double val = GetNumberAtByte(file, Address);
					Address = Address + 5;
					TempSB = new StringBuilder();
					TempSB.append("    (");
					if (dimensions > 4) {
						/*
						 * if the dimensions are greater than 4, then chances are, the variable is
						 * broken. This usually caused by using a character array as a location for
						 * code. (See Capitan, Caultron, and many other files in C_C of the workbench hd
						 * image)
						 */
						TempSB.append(String.valueOf(Math.round(dimcounts[0])));
						TempSB.append(",");
						TempSB.append(String.valueOf(Math.round(dimcounts[1])));
						TempSB.append(" ... ");
						TempSB.append(String.valueOf(Math.round(dimcounts[dimensions - 2])));
						TempSB.append(",");
						TempSB.append(String.valueOf(Math.round(dimcounts[dimensions - 1])));
					} else {
						for (int i = 0; i < dimensions; i++) {
							if (i != 0) {
								TempSB.append(",");
							}
							TempSB.append(String.valueOf(Math.round(dimcounts[i])));
						}
					}
					TempSB.append(")=");
					TempSB.append(String.valueOf(val));

					boolean decdone = false;
					int dimid = dimensions - 1;
					while (!decdone) {
						int num = dimcounts[dimid];
						num++;
						if (num > dims[dimid]) {
							dimcounts[dimid] = 1;
							if (dimid == dimensions - 1) {
								TempSB.append(cr);
								TempSB.append("  ");
							} else {
								TempSB.append(cr);
								TempSB.append(cr);
								TempSB.append("  ");
							}
							dimid--;
							if (dimid == -1) {
								decdone = true;
								done = true;
							}
							first = true;
						} else {
							if (!first) {
								TempSB.append(", ");
							}
							first = false;
							dimcounts[dimid] = num;
							decdone = true;
						}
					}
					sb.append(TempSB.toString());
				}
				sb.append(cr + "}" + cr);

				sb.append("  </content>" + cr);
			} catch (Exception E) {
				sb.append("  <err>Bad variable: " + varname + "</err>");
				throw (E);
			}
		} finally {
			sb.append("</variable>" + System.lineSeparator());
		}

		return (Address);
	}

	/**
	 * Handler for type 5 variables (Number with a name > 1) Format: (Original char)
	 * [101XXXXX] where XXXXX = char of name - 0x40 [0] [000XXXXX] where XXXXX =
	 * char of name - 0x40 ... [Y] [100XXXXX] where XXXXX = char of name - 0x40 (Bit
	 * 7 is set to terminate string) [N1..N5] Speccy Floating point number
	 * 
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType5(StringBuilder sb, int Address, int chr, byte[] file) {
		sb.append("<variable>" + System.lineSeparator());
		try {
			boolean done = false;
			String variable = "";
			while (!done) {
				int varname = (chr & 0x3f);
				varname = varname + 0x40;
				variable = variable + String.valueOf((char) varname);
				chr = file[Address++];
				done = (chr & 0x80) == 0x80;
			}
			int varname = (chr & 0x3f) + 0x40;
			variable = variable + String.valueOf((char) varname);

			double value = GetNumberAtByte(file, Address);
			Address = Address + 5;

			sb.append("  <type>5</type>" + System.lineSeparator());
			sb.append("  <typename>Number (multiple character name)</typename>" + System.lineSeparator());
			sb.append("  <varname>" + variable + "</varname>" + System.lineSeparator());
			sb.append("  <content>" + String.valueOf(value) + "</content>" + System.lineSeparator());
		} finally {
			sb.append("</variable>" + System.lineSeparator());
		}
		return (Address);
	}

	/**
	 * Handler for type 6 variables (Character arrays) Variable name is parsed from
	 * the original marker (first 5 bytes) + 0x40 format is: [0-1] data length (Used
	 * to quickly skip over variable when searching) [2] Number of dimensions
	 * (1-255) [3.4] First dimension size.(1-65535) .. [xx.yy] last dimension size
	 * [z] char for (1,0) [z] char for (2,0) ... [z] char for (<sz>,0) [z] char for
	 * (1,1) and so on.
	 * 
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType6(StringBuilder sb, int Address, int chr, byte[] file) {
		String cr = System.lineSeparator();
		sb.append("<variable>" + System.lineSeparator());
		try {
			int varname = (chr & 0x1f);
			varname = varname + 0x40;
			Address = Address + 2; // Ignore the data size
			sb.append("  <type>6</type>" + System.lineSeparator());
			sb.append("  <typename>Character array</typename>" + System.lineSeparator());
			sb.append("  <varname>" + String.valueOf((char) varname) + "$</varname>" + System.lineSeparator());

			/*
			 * Add the dimensions Note, doing it this way so if the variable is screwed up,
			 * it wont add the incomplete XML
			 */
			StringBuilder TempSB = new StringBuilder();
			TempSB.append("  <dimensions>" + cr);
			int dimensions = (file[Address++] & 0xff);
			int dims[] = new int[dimensions];
			int dimcounts[] = new int[dimensions];
			TempSB.append("  <numdimensions>" + dimensions + "<numdimensions>" + cr);
			for (int i = 0; i < dimensions; i++) {
				int lsb = (file[Address++] & 0xff);
				int msb = (file[Address++] & 0xff);
				dims[i] = (msb * 256) + lsb;
				TempSB.append("    <dimension>" + dims[i] + "<dimension>" + cr);
				dimcounts[i] = 1;
			}
			TempSB.append("  </dimensions>" + cr);
			sb.append(TempSB.toString());

			/*
			 * Add the content
			 */
			TempSB = new StringBuilder();
			TempSB.append(cr);
			boolean first = false;
			boolean done = false;
			while (!done && (Address < file.length)) {
				char chracter = (char) (file[Address++] & 0xff);
				TempSB.append("    (");
				if (dimensions > 4) {
					/*
					 * if the dimensions are greater than 4, then chances are, the variable is
					 * broken. This usually caused by using a character array as a location for
					 * code. (See Capitan, Caultron, and many other files in C_C of the workbench hd
					 * image)
					 */
					TempSB.append(String.valueOf(Math.round(dimcounts[0])));
					TempSB.append(",");
					TempSB.append(String.valueOf(Math.round(dimcounts[1])));
					TempSB.append(" ... ");
					TempSB.append(String.valueOf(Math.round(dimcounts[dimensions - 2])));
					TempSB.append(",");
					TempSB.append(String.valueOf(Math.round(dimcounts[dimensions - 1])));
				} else {
					for (int i = 0; i < dimensions; i++) {
						if (i != 0) {
							TempSB.append(",");
						}
						TempSB.append(String.valueOf(Math.round(dimcounts[i])));
					}
				}
				TempSB.append(")=");
				TempSB.append(DecodeToken("" + chracter));

				boolean decdone = false;
				int dimid = dimensions - 1;
				while (!decdone) {
					int num = dimcounts[dimid];
					num++;
					if (num > dims[dimid]) {
						dimcounts[dimid] = 1;
						if (dimid == dimensions - 1) {
							TempSB.append(cr);
							TempSB.append("      ");
						} else {
							TempSB.append(cr);
							TempSB.append(cr);
							TempSB.append("      ");
						}
						dimid--;
						if (dimid == -1) {
							decdone = true;
							done = true;
						}
						first = true;
					} else {
						if (!first) {
							TempSB.append(", ");
						}
						first = false;
						dimcounts[dimid] = num;
						decdone = true;
					}
				}
			}
			TempSB.append(cr + "}" + cr);

			sb.append("  <content>" + cr + TempSB.toString() + "</content>" + cr);

		} finally {
			sb.append("</variable>" + cr);
		}

		return (Address);
	}

	/**
	 * Handler for type 7 variables (FOR/NEXT variables) Variable name is parsed
	 * from the original marker (first 5 bytes) + 0x40 format is: byte: [0-4]
	 * Current Value of variable (Speccy FP representation) [5-9] TO value (Speccy
	 * FP representation) [10-14] STEP value (Speccy FP representation) [15]
	 * statement within FOR line to start looping from. (integer)
	 * 
	 * Like the previous ones, this is similar to the ZX81 8k Rom equivelent, except
	 * the addition of byte 15 (As the zx81 does not allow multiple statements on a
	 * line)
	 * 
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType7(StringBuilder sb, int Address, int chr, byte[] file) {
		sb.append("<variable>" + System.lineSeparator());
		try {
			int varname = (chr & 0x3f);
			varname = varname + 0x40;

			String value = String.valueOf(GetNumberAtByte(file, Address));
			Address = Address + 5;
			String limit = String.valueOf(GetNumberAtByte(file, Address));
			Address = Address + 5;
			String step = String.valueOf(GetNumberAtByte(file, Address));
			Address = Address + 5;

			int looplinelsb = (file[Address++] & 0xff);
			int looplinemsb = (file[Address++] & 0xff);
			int loopline = (looplinemsb * 256) + looplinelsb;

			int looplinestatement = (file[Address++] & 0xff);

			sb.append("  <type>7</type>" + System.lineSeparator());
			sb.append("  <typename>for/next variable</typename>" + System.lineSeparator());
			sb.append("  <varname>" + String.valueOf((char) varname) + "</varname>" + System.lineSeparator());
			sb.append("  <content>" + value + "</content>" + System.lineSeparator());
			sb.append("  <limit>" + limit + "</limit>" + System.lineSeparator());
			sb.append("  <step>" + step + "</step>" + System.lineSeparator());
			sb.append("  <loopline>" + loopline + "</loopline>" + System.lineSeparator());
			sb.append("  <looplinestatement>" + looplinestatement + "</looplinestatement>" + System.lineSeparator());
		} finally {
			sb.append("</variable>" + System.lineSeparator());
		}

		return (Address);
	}

	/**
	 * Convert the file into an image. Image will always be 256x192
	 * 
	 * @param file
	 * @param filebase
	 * @return
	 */
	public static ImageData GetImageFromFileArray(byte file[], int filebase) {
		PaletteData palette = new PaletteData(0xFF, 0xFF00, 0xFF0000);
		ImageData imageData = new ImageData(256, 192, 24, palette);
		// If the file is incomplete, pad to the correct size.
		if (file.length < 6912) {
			byte newFile[] = new byte[6912 + filebase];
			byte wob = Speccy.ToAttribute(Speccy.COLOUR_BLACK, Speccy.COLOUR_WHITE, false, false);
			for (int i = 6144 + filebase; i < 6912 + filebase; i++) {
				newFile[i] = wob;
			}
			// Copy in the file we have
			System.arraycopy(file, 0, newFile, 0, file.length);
			file = newFile;
		}

		// populate the image
		for (int yptn = 0; yptn < 192; yptn++) {
			// screen location in file = 0033111 222XXXXX where y = 33222111
			int y1 = yptn & 0x07;
			int y2 = (yptn & 0x38) / 0x08;
			int y3 = (yptn & 0xc0) / 0x40;

			int lineAddress = (y2 * 0x20) + (y1 * 0x100) + (y3 * 0x800);

			// Attribute address is linear starting from 0x1800
			int attributeAddr = ((yptn / 8) * 32) + 0x1800;

			int pixelX = 0;
			for (int xptn = 0; xptn < 32; xptn++) {
				// get the 8 bits of the current byte
				int pixels = file[lineAddress + xptn + filebase] & 0xff;

				// Get the colours of the pixels in the current block
				int attributes = file[attributeAddr + xptn + filebase] & 0xff;
				int pen = attributes & 0x7;
				int paper = (attributes & 0x38) / 0x08;

				// convert the colours from index to actual RGB values. (Taking into account
				// BRIGHT)
				if ((attributes & 0x40) == 0x40) {
					pen = Speccy.coloursBright[pen];
					paper = Speccy.coloursBright[paper];
				} else {
					pen = Speccy.colours[pen];
					paper = Speccy.colours[paper];
				}

				// plot 8 pixels of a line
				for (int px = 0; px < 8; px++) {
					boolean bit = (pixels & 0x80) == 0x80;
					pixels = pixels << 1;

					if (bit) {
						imageData.setPixel(pixelX++, yptn, pen);
					} else {
						imageData.setPixel(pixelX++, yptn, paper);
					}
				}
			}
		}
		return (imageData);
	}

	/**
	 * Try to tokenise a the given basic line.
	 * 
	 * @param Line         - Line to tokenise
	 * @param BasicAsBytes - Target array
	 * @param TargetPtr    - Target ptr
	 * @return - Next byte in the target array
	 */
	public static int DecodeBasicLine(String Line, byte BasicAsBytes[], int TargetPtr) {
		ArrayList<Byte> NewLine = new ArrayList<Byte>();
		Line = Line.trim();
		String err = "";
		// split line
		ArrayList<String> TokenList = SplitLine(Line);

		// read the line number
		if (TokenList.size() > 0) {
			// get the initial token Should be the line number
			String token = TokenList.get(0);
			int linenum = 0;
			try {
				linenum = Integer.parseInt(token);
			} catch (NumberFormatException nfe) {
				err = "Bad lineno: " + linenum;
			}
			if (err.isBlank()) {
				// Tokenise the rest of the line.
				int tokenptr = 1;
				while (tokenptr < TokenList.size()) {
					token = TokenList.get(tokenptr++);
					String tkn = Speccy.DecodeToken(token);
					for (int i = 0; i < tkn.length(); i++) {
						int c = tkn.charAt(i);
						NewLine.add((byte) c);
					}
				}

			}
			// Add in the EOL chararacter.
			NewLine.add((byte) 0x0d);

			// Add in the line number
			BasicAsBytes[TargetPtr++] = (byte) ((linenum / 0x100) & 0xff);
			BasicAsBytes[TargetPtr++] = (byte) (linenum & 0xff);
			// Add in the line size
			BasicAsBytes[TargetPtr++] = (byte) (NewLine.size() & 0xff);
			BasicAsBytes[TargetPtr++] = (byte) ((NewLine.size() / 0x100) & 0xff);
			// copy line into byte array
			for (byte b : NewLine) {
				BasicAsBytes[TargetPtr++] = b;
			}
		}
		return (TargetPtr);
	}

	/**
	 * Tokenise the given line. Will do this using a state machine.
	 * 
	 * @param line Line to parse
	 * @return Token list.
	 */
	private static ArrayList<String> SplitLine(String line) {
		// values for the state machine.
		int STATE_NONE = 0;
		int STATE_NUMBER = 1;
		int STATE_STRING = 2;
		int STATE_MISC = 3;
		int STATE_OPERATOR = 4;
		int STATE_REM = 5;

		// some preprocessing
		line = CISreplace(line, "GO SUB", "GOSUB");
		line = CISreplace(line, "GO TO", "GOTO");
		line = CISreplace(line, "CLOSE #", "CLOSE#");
		line = CISreplace(line, "OPEN #", "OPEN#");

		int state = STATE_NONE;
		String curritem = "";
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < line.length(); i++) {
			char chr = line.charAt(i);
			if (state == STATE_REM) {
				// if we have found a rem, just add everything from here on.
				// Dont try to switch states going forward.
				curritem = curritem + chr;
			} else if (state == STATE_NONE) {
				// if we are in state_none, switch to another state.
				if (IsNumber(chr)) {
					state = STATE_NUMBER;
					curritem = curritem + chr;
				} else if (IsOperator(chr)) {
					state = STATE_OPERATOR;
					curritem = curritem + chr;
				} else if (chr == '"') {
					state = STATE_STRING;
					curritem = curritem + '"';
				} else if (chr != ' ') {
					state = STATE_MISC;
					curritem = curritem + chr;
				}
			} else if (state == STATE_NUMBER) {
				if (IsNumber(chr)) {
					curritem = curritem + chr;
				} else {
					result.add(curritem);
					curritem = "";
					// ok we are not a number, Lets decide the next state
					if (IsSeperator(chr)) {
						result.add("" + chr);
						state = STATE_MISC;
					} else if (IsOperator(chr)) {
						state = STATE_OPERATOR;
						curritem = curritem + chr;
					} else if (chr == '"') {
						curritem = curritem + '"';
						state = STATE_STRING;
					} else if (chr != ' ') {
						state = STATE_MISC;
						curritem = curritem + chr;
					}
				}
			} else if (state == STATE_STRING) {
				if (chr == '"') {
					state = STATE_NONE;
					curritem = curritem + '"';
					result.add(curritem);
					curritem = "";
				} else {
					curritem = curritem + chr;
				}
			} else if (state == STATE_MISC) {
				if (curritem.toUpperCase().contentEquals("REM")) {
					result.add(curritem);
					curritem = "" + chr;
					state = STATE_REM;
				}
				if (IsNumber(chr)) {
					// are we a continuation of an identifier? If so, dont switch state.
					if (curritem.isEmpty()) {
						state = STATE_NUMBER;
						result.add(curritem);
						curritem = "" + chr;
					} else {
						curritem = curritem + chr;
					}
				} else if (IsOperator(chr)) {
					state = STATE_OPERATOR;
					result.add(curritem);
					curritem = "" + chr;
				} else if (chr == '"') {
					state = STATE_STRING;
					result.add(curritem);
					curritem = "\"";
				} else if (IsSeperator(chr)) {
					result.add(curritem);
					result.add("" + chr);
					curritem = "";

				} else if (chr != ' ') {
					curritem = curritem + chr;
				} else { // is space
					result.add(curritem);
					curritem = "";
				}
			} else if (state == STATE_OPERATOR) {
				if (IsOperator(chr)) {
					curritem = curritem + chr;
				} else {
					result.add(curritem);
					curritem = "";
					// ok we are not a number, Lets decide the next state
					if (IsSeperator(chr)) {
						result.add("" + chr);
					} else if (IsNumber(chr)) {
						state = STATE_NUMBER;
						curritem = curritem + chr;
					} else if (chr == '"') {
						state = STATE_STRING;
						curritem = "\"";
					} else if (chr != ' ') {
						state = STATE_MISC;
						curritem = curritem + chr;
					}
				}
			}
		}
		result.add(curritem);

		// remove the spaces
		ArrayList<String> result2 = new ArrayList<String>();
		for (String sr : result) {
			sr = sr.trim();
			if (!sr.isBlank()) {
				result2.add(sr);
			}
		}
		return (result2);
	}

	/**
	 * Is the character part of a number?
	 * 
	 * @param chr
	 * @return TRUE if number 0-9 or -
	 */
	private static boolean IsNumber(char chr) {
		String numbers = "0123456789.";
		return (numbers.indexOf(chr) > -1);
	}

	/**
	 * Is the character a logical or math operator?
	 * 
	 * @param chr
	 * @return TRUE if an operator character
	 */
	private static boolean IsOperator(char chr) {
		String operators = "()+-/*<>&=";
		return (operators.indexOf(chr) > -1);
	}

	/**
	 * Is the character something used to separate statements?
	 * 
	 * @param chr
	 * @return TRUE if a seperator character
	 */
	private static boolean IsSeperator(char chr) {
		String seperators = ";:, ";
		return (seperators.indexOf(chr) > -1);
	}

	/**
	 * Case insensitive replace from
	 * https://stackoverflow.com/questions/5054995/how-to-replace-case-insensitive-literal-substrings-in-java
	 * 
	 * @param source
	 * @param target
	 * @param replacement
	 */
	private static String CISreplace(String source, String target, String replacement) {
		StringBuilder sbSource = new StringBuilder(source);
		StringBuilder sbSourceLower = new StringBuilder(source.toLowerCase());
		String searchString = target.toLowerCase();

		int idx = 0;
		while ((idx = sbSourceLower.indexOf(searchString, idx)) != -1) {
			sbSource.replace(idx, idx + searchString.length(), replacement);
			sbSourceLower.replace(idx, idx + searchString.length(), replacement);
			idx += replacement.length();
		}
		sbSourceLower.setLength(0);
		sbSourceLower.trimToSize();
		sbSourceLower = null;

		return sbSource.toString();
	}

	/**
	 * Save the array to file
	 * 
	 * @param data     - Data to be saved
	 * @param mainPage - Owning page
	 * @param varname  - Variable name
	 * @throws IOException
	 */
	public static void DoSaveNumericArrayAsText(File targetFilename, byte[] data, String varname) {
		try {
			FileOutputStream file;
			file = new FileOutputStream(targetFilename);
			try {
				file.write(("File: " + targetFilename.getName() + System.lineSeparator()).getBytes());
				int location = 0x80; // skip header

				// Number of dimensions
				int numDimensions = data[location++] & 0xff;

				// LOad the dimension sizes into an array
				int Dimsizes[] = new int[numDimensions];
				for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
					int dimsize = data[location++] & 0xff;
					dimsize = dimsize + (data[location++] & 0xff) * 0x100;
					Dimsizes[dimnum] = dimsize;
				}
				String s = "DIM " + varname + "(";
				for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
					if (dimnum > 0)
						s = s + ",";
					s = s + String.valueOf(Dimsizes[dimnum]);
				}
				s = s + ") = " + System.lineSeparator();
				file.write(s.getBytes());

				// count of what dimensions have been processed.
				int DimCounts[] = new int[numDimensions];
				for (int dimnum = 0; dimnum < numDimensions; dimnum++)
					DimCounts[dimnum] = 0;

				StringBuffer sb = new StringBuffer();
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
					sb.append(System.lineSeparator());
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
				file.write(sb.toString().getBytes());

			} finally {
				file.close();
			}
		} catch (Exception e) {
			System.out.println("Error exporting: " + targetFilename.getName() + " " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Save the character array
	 * 
	 * @param data
	 * @param mainPage
	 * @param p3d
	 * @throws IOException
	 */
	public static void DoSaveCharArrayAsText(File targetFilename, byte[] data, String varname) {
		FileOutputStream file;
		try {
			file = new FileOutputStream(targetFilename);
			try {
				file.write(("File: " + targetFilename.getName() + System.lineSeparator()).getBytes());
				int location = 0x80; // skip header

				// Number of dimensions
				int numDimensions = data[location++] & 0xff;

				// LOad the dimension sizes into an array
				int Dimsizes[] = new int[numDimensions];
				for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
					int dimsize = data[location++] & 0xff;
					dimsize = dimsize + (data[location++] & 0xff) * 0x100;
					Dimsizes[dimnum] = dimsize;
				}
				String s = "DIM " + varname + "(";
				for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
					if (dimnum > 0)
						s = s + ",";
					s = s + String.valueOf(Dimsizes[dimnum]);
				}
				s = s + ") = " + System.lineSeparator();
				file.write(s.getBytes());

				// count of what dimensions have been processed.
				int DimCounts[] = new int[numDimensions];
				for (int dimnum = 0; dimnum < numDimensions; dimnum++)
					DimCounts[dimnum] = 0;

				StringBuffer sb = new StringBuffer();
				boolean complete = false;
				while (!complete) {
					for (int cc = 0; cc < Dimsizes[Dimsizes.length - 1]; cc++) {

						if (cc != 0) {
							sb.append(",");
						}
						String chr = Speccy.tokens[data[location++] & 0xff];
						sb.append(chr);
					}
					sb.append(System.lineSeparator());
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
				file.write(sb.toString().getBytes());

			} finally {
				file.close();
			}
		} catch (Exception e) {
			System.out.println("Error exporting: " + targetFilename.getName() + " " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Get the colour attribute byte
	 * 
	 * @param ink
	 * @param paper
	 * @param bright
	 * @param flash
	 * @return
	 */
	public static byte ToAttribute(int ink, int paper, boolean bright, boolean flash) {
		int result = ink + (paper * 8);
		if (bright) {
			result = result + 0x40;
		}
		if (flash) {
			result = result + 0x80;
		}
		return ((byte) (result & 0xff));
	}

	/**
	 * Get a file type as a string.
	 * 
	 * @param filetype
	 * @return
	 */
	public static String FileTypeAsString(int filetype) {
		String result = "Unknown (#" + filetype + ")";
		if ((filetype > -1) || (filetype < 0x04)) {
			result = filetypeNames[filetype];
		}
		return (result);
	}

	/**
	 * Render a given file type to the given folder.
	 * 
	 * @param targetFilename
	 * @param data
	 * @param filelength
	 * @param speccyFileType
	 * @param basicLine
	 * @param basicVarsOffset
	 * @param codeLoadAddress
	 * @param arrayVarName
	 * @throws IOException
	 */

	public static void SaveFileToDisk(File targetFilename, byte[] data, int filelength, int speccyFileType,
			int basicLine, int basicVarsOffset, int codeLoadAddress, String arrayVarName, boolean codeAsHex)
			throws IOException {

		switch (speccyFileType) {
		case BASIC_BASIC:
			SaveBasicFile(targetFilename, data, basicLine, basicVarsOffset, filelength);
			break;
		case BASIC_NUMARRAY:
			DoSaveNumericArrayAsText(targetFilename, data, arrayVarName);
			break;
		case BASIC_CHRARRAY:
			DoSaveCharArrayAsText(targetFilename, data, arrayVarName);
			break;
		case BASIC_CODE:
			SaveCodeFile(targetFilename, data, codeLoadAddress, filelength, codeAsHex);
			break;
		default:
			SaveCodeFile(targetFilename, data, 0x10000 - filelength, filelength, codeAsHex);
			break;
		}

	}

	private static void SaveCodeFile(File targetFilename, byte[] data, int codeLoadAddress, int filelength,
			boolean OutAsHex) throws IOException {
		if ((filelength == 6912) && (codeLoadAddress == 16384)) {
			ImageData image = Speccy.GetImageFromFileArray(data, 0);
			ImageLoader imageLoader = new ImageLoader();
			imageLoader.data = new ImageData[] { image };
			int filetyp = SWT.IMAGE_PNG;
			FileOutputStream file;
			try {
				file = new FileOutputStream(targetFilename);
				imageLoader.save(file, filetyp);
				file.close();
			} catch (FileNotFoundException e) {
				System.out.println("Cannot save " + targetFilename.getAbsolutePath());
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Error closing " + targetFilename.getAbsolutePath());
				e.printStackTrace();
			}

		} else {
			if (OutAsHex) {
				String hexdata = GeneralUtils.HexDump(data, 0, data.length, 0);
				GeneralUtils.WriteBlockToDisk(hexdata.getBytes(), targetFilename);
			} else {
				DoSaveFileAsAsm(data, targetFilename, codeLoadAddress);
			}
		}
	}

	/**
	 * 
	 * @param targetFilename
	 * @param data
	 * @param basicLine
	 * @param basicVarsOffset
	 * @param filelength
	 * @throws IOException
	 */
	private static void SaveBasicFile(File targetFilename, byte[] data, int basicLine, int basicVarsOffset,
			int filelength) {
		FileOutputStream file;
		try {
			file = new FileOutputStream(targetFilename);
			try {
				int ptr = 0;
				int EndOfBasicArea = Math.min(filelength, basicVarsOffset);
				while (ptr < EndOfBasicArea) {
					int linenum = -1;
					int linelen = 0;
					try {
						linenum = ((data[ptr++] & 0xff) * 256);
						linenum = linenum + (data[ptr++] & 0xff);
						linelen = (int) data[ptr++] & 0xff;
						linelen = linelen + ((int) (data[ptr++] & 0xff) * 256);
						// fiddles bad line lengths
						linelen = Math.min(filelength - ptr + 4, linelen);
					} catch (Exception E) {
						System.out.println("Basic parsing error, bad linenum.");
						file.write("Bad line number encountered.\n".getBytes());
						ptr = 99999999;
						linenum = 0;
					}

					if ((ptr >= basicVarsOffset) || (linenum < 0)) {
						// now into the variables area. Ignoring for the moment.
						ptr = 99999999;
					} else {
						String sixdigit = String.valueOf(linenum);
						while (sixdigit.length() < 6) {
							sixdigit = sixdigit + " ";
						}
						StringBuilder sb = new StringBuilder();
						try {
							byte line[] = new byte[linelen];
							System.arraycopy(data, ptr, line, 0, linelen);
							Speccy.DecodeBasicLine(sb, line, 0, linelen, false);
						} catch (Exception E) {
							sb.append("Bad line: " + E.getMessage());
							ptr = 99999999;
						}

						// point to next line.
						ptr = ptr + linelen;

						file.write(sixdigit.getBytes());
						file.write(" ".getBytes());
						file.write(sb.toString().getBytes());
						file.write("\n".getBytes());
					}
				}
			} finally {
				file.close();
			}
		} catch (Exception E) {
			System.out.println("---------------------------------------------------------");
			System.out.println("Error extracting BASIC file " + targetFilename.getName());
			System.out.println("Error: " + E.getMessage());
			E.printStackTrace();
		}

		/*
		 * output the variables in a seperate file.
		 */
		if (basicVarsOffset < filelength) {
			int varlen = filelength - basicVarsOffset;
			byte variables[] = new byte[varlen];
			try {
				if (data.length - basicVarsOffset < varlen) {
					System.out.println("---------------------------------------------------------");
					System.out.println("Error extracting variables for BASIC file " + targetFilename.getName());
					System.out.println("Error: program Data does not contain enough space for variables. Ignoring");
				} else {

					System.arraycopy(data, basicVarsOffset, variables, 0, varlen);
					StringBuilder sb = new StringBuilder();
					DecodeVariablesFromLoadedFile(variables, sb, 0, varlen);

					// Now for the variables area.
					if (!sb.toString().startsWith("End of variables")) {
						GeneralUtils.WriteBlockToDisk(sb.toString().getBytes(), targetFilename + ".variables");
					}
				}
			} catch (Exception E) {
				System.out.println("---------------------------------------------------------");
				System.out.println("Error extracting variables for BASIC file " + targetFilename.getName());
				System.out.println("Error: " + E.getMessage());
				E.printStackTrace();
				System.out.println("data being parsed:");
				System.out.println(GeneralUtils.HexDump(variables, 0, variables.length, 0));
			}
		}
	}

	/**
	 * Save file using the given types.
	 *
	 * 
	 * @param targetFilename
	 * @param entrydata
	 * @param rawdata
	 * @param filelength
	 * @param speccyFileType
	 * @param basicLine
	 * @param basicVarsOffset
	 * @param codeLoadAddress
	 * @param arrayVarName
	 * @param ActionType
	 */
	public static void SaveFileToDiskAdvanced(File targetFilename, byte[] entrydata, byte[] rawdata, int filelength,
			int speccyFileType, int basicLine, int basicVarsOffset, int codeLoadAddress, String arrayVarName,
			int ActionType) {

		int ImageFileTyp = -1;
		switch (ActionType) {
		case GeneralUtils.EXPORT_TYPE_RAW:
			GeneralUtils.WriteBlockToDisk(entrydata, targetFilename);
			break;
		case GeneralUtils.EXPORT_TYPE_HEX:
			String hexdata = GeneralUtils.HexDump(entrydata, 0, entrydata.length, codeLoadAddress);
			GeneralUtils.WriteBlockToDisk(hexdata.getBytes(), targetFilename);
			break;
		case GeneralUtils.EXPORT_TYPE_ASM:
			DoSaveFileAsAsm(entrydata, targetFilename, codeLoadAddress);
			break;
		case GeneralUtils.EXPORT_TYPE_TXT:
			SaveBasicFile(targetFilename, entrydata, basicLine, basicVarsOffset, filelength);
			break;
		case GeneralUtils.EXPORT_TYPE_RAWANDHEADER:
			GeneralUtils.WriteBlockToDisk(rawdata, targetFilename);
			break;
		case GeneralUtils.EXPORT_TYPE_CSV:
			if (speccyFileType == BASIC_NUMARRAY) {
				DoSaveNumericArrayAsText(targetFilename, entrydata, arrayVarName);
			} else {
				DoSaveCharArrayAsText(targetFilename, entrydata, arrayVarName);
			}
			break;
		case GeneralUtils.EXPORT_TYPE_PNG:
			ImageFileTyp = SWT.IMAGE_PNG;
			break;
		case GeneralUtils.EXPORT_TYPE_GIF:
			ImageFileTyp = SWT.IMAGE_GIF;
			break;
		case GeneralUtils.EXPORT_TYPE_JPG:
			ImageFileTyp = SWT.IMAGE_JPEG;
			break;
		}

		if (ImageFileTyp > -1) {
			ImageData image = Speccy.GetImageFromFileArray(entrydata, 0);
			ImageLoader imageLoader = new ImageLoader();
			imageLoader.data = new ImageData[] { image };
			FileOutputStream file;
			try {
				file = new FileOutputStream(targetFilename);
				imageLoader.save(file, ImageFileTyp);
				file.close();
			} catch (FileNotFoundException e) {
				System.out.println("Cannot save " + targetFilename.getAbsolutePath());
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Error closing " + targetFilename.getAbsolutePath());
				e.printStackTrace();
			}
		}
	}

}

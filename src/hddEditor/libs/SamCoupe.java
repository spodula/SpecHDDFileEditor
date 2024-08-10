package hddEditor.libs;
//TODO: SAMSCREEN: Apply interrupt colour change
//TODO: SAMSCREEN: Encode image.
//TODO: SAMSCREEN: Support Mode 2
//TODO: SAM Files seem to have a 9 byte header. Basic header? 
//TODO: MGT/SAM filenames, trim out spaces.
//TODO: MGT/SAM files: lots of "Warning, active sectors in Sector address map (0) doesnt match Dirent Count (8) for file" errors
//TODO: MGT/SAM files: Sam defines additional file type flags. Bit 7=HIDDEN, bit7=PROTECTED. Implement in MGT

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class SamCoupe {
	/*-
	 * Class to wrap up a raw MGT file to extract the SAMDOS information.
	 * 
	 * SAM file headers (types 16-20) (taken from https://www.mono.org/~unc/Coupe/Tech/dos.html)
	 * 
	 * 	SAMDOS type
	 *	0	File type
	 *	1-2	Modulo length
	 *  3-4	Offset start
	 *	5-6	Unused
	 *	7	Number of pages
	 *	8	Starting page number
	 */
	public class SamFileWrapper {
		public boolean IsValid;
		private byte rawdata[];
		
		/**
		 * parse the data and do some basic sanity checking.
		 * @param data
		 */
		public SamFileWrapper(byte data[]) {
			rawdata = data;
			IsValid = true;
			if (rawdata.length < 9) {
				IsValid = false;
			}
			int ft =getFileType(); 
			if (ft < MGT.MGTFT_SAMBASIC || ft < MGT.MGTFT_SAMSCREEN) {
				IsValid = false;
			}
		}
		
		/**
		 * File type. 
		 * 
		 * @return
		 */
		public int getFileType() {
			return (rawdata[0] & 0xff);
		}
		public void setFileType(int filetype) {
			rawdata[0] = (byte)(filetype & 0xff);
		}
		
		/**
		 * Offset bytes in the final page.
		 * 
		 * @return
		 */
		public int getModuloLength() {
			int value = -1;
			if (IsValid) {
				value = ((rawdata[2] & 0xff) * 0x100) + (rawdata[1] & 0xff);
			}
			return (value);
		}
		public void setModuloLength(int len) {
			int lsb = len & 0xff;
			int msb = len & 0xff00;
			rawdata[1] = (byte) (lsb & 0xff);
			rawdata[2] = (byte) (msb & 0xff);
		}
		
		/**
		 * offset in the first page to start reading the file.
		 * 
		 * @return
		 */
		public int getOffsetStart() {
			int value = -1;
			if (IsValid) {
				value = ((rawdata[4] & 0xff) * 0x100) + (rawdata[3] & 0xff);
			}
			return (value);
		}
		public void setOffsetStart(int offset) {
			int lsb = offset & 0xff;
			int msb = offset & 0xff00;
			rawdata[3] = (byte) (lsb & 0xff);
			rawdata[4] = (byte) (msb & 0xff);
		}

		/**
		 * Number of complete 16k pages used in the file.
		 * @return
		 */
		public int getNumPages() {
			if (IsValid) {
				return (rawdata[7] & 0xff);
			} else {
				return(-1);
			}
		}
		public void setNumPages(int numpages) {
			rawdata[7] = (byte)(numpages & 0xff);
		}

		/**
		 * start page to load the file
		 * @return
		 */
		public int getStartPage() {
			if (IsValid) {
				return (rawdata[7] & 0xff);
			} else {
				return(-1);
			}
		}
		public void setStartPage(int startpage) {
			rawdata[8] = (byte)(startpage & 0xff);
		}
		
		/**
		 * Get the file data minus its header
		 * @return
		 */
		public byte[] getData() {
			if (IsValid) {
				byte data[] = new byte[rawdata.length-9];
				System.arraycopy(rawdata, 9, data, 0, data.length);
				return(data);
			} else {
				return(rawdata);
			}
		}
		
		/**
		 * Calculate the file size.
		 * Details sourced from https://www.mono.org/~unc/Coupe/Tech/dos.html
		 * @return
		 */
		public int getSAMFileSize() {
			int result = (getNumPages() * 0x4000) + getModuloLength(); 
			return(result);
		}
		public void setetSAMFileSize(int filesize) {
			int numpages = filesize / 0x4000;
			int modulo = filesize % 0x4000;
			setNumPages(numpages);
			setModuloLength(modulo);
		}
		
		/**
		 * return details as a string.
		 */
		@Override
		public String toString() {
			String result = "";
			result = result + "Type: "+getFileType();
			result = result + " - Valid: "+IsValid;
			result = result + " - Modulo len: "+getModuloLength();
			result = result + " - Offset: "+getOffsetStart();
			result = result + " - Pages: "+getNumPages();
			result = result + " - StartPage: "+getStartPage();
			result = result + " - RealLen: "+getSAMFileSize();
			result = result + " - Datalen: "+(rawdata.length-9);
			return(result);
		}
	}
	

	/*-
	 * Implementation of the SAM Coupe version of a screen$.
	 * 
	 * This is more complex than the speccy one. Details from:
	 * https://blog.martinfitzpatrick.com/writing-a-sam-coupe-screen-converter-in-python/
	 * https://github.com/mfitzp/scrimage
	 *
	 * File format: 
	 * [0..xx] Byte data (24576 bytes in mode 4,3, 6144 bytes in mode 2) 
	 * Palette A 
	 *   [+16] 16 Bytes selecting palette from fixed SAM 256 colour palette 
	 *   [+4] 0x00,0x11,0x22,0x7f 
	 * OR 
	 *   [+4] 4 Bytes selecting palette from fixed SAM 256 colour palette 
	 *   [+4] 0x00,0x11,0x22,0x7f
	 *    
	 * Palette B [+16] 16 Bytes selecting palette from fixed SAM 256 colour palette 
	 *   [+4] 0x00,0x11,0x22,0x7f
	 * OR 
	 *   [+4] 4 Bytes selecting palette from fixed SAM 256 colour palette 
	 *   [+4] 0x00,0x11,0x22,0x7f 
	 * Interrupts 
	 *   [4 bytes for each interrupts] 
	 *   [1] 0xff.
	 * 
	 * 
	 * Format:
	 * 
	 * Image data: 
	 *   Mode 4: 256x192 Linear 4bpp 16 Colours 24k 
	 *   Mode 3: 512x192 Linear 2bpp 4 Colours 24k 
	 *   Mode 2: 256x192 Linear 1bpp 2 Colours 12K Colour attributes. 
	 *   Mode 1: Same as Speccy.
	 * 
	 * Palette's are terminated with the byte stream: 0x00,0x11,0x22,0x7f This can
	 * probably be used to determine if a file is Mode 3 or Mode 4. Modes 2 and 1
	 * will have to be determined from file size.
	 * 
	 * Interrupts are used to change the palette colours on the fly. They can be
	 * changed on a line by line basis. 
	 * 
	 * Interrupt format: 
	 *   [0] 172-Y position to apply colour change 
	 *   [1] Colour number in the Palette's to change 
	 *   [2] new Colour in Palette A 
	 *   [3] new Colour in Palette B
	 * Terminates with FF
	 */
	public static class SAMScreen {
		public static final int SAMSCREEN_MODE1 = 1;
		public static final int SAMSCREEN_MODE2 = 2;
		public static final int SAMSCREEN_MODE3 = 3;
		public static final int SAMSCREEN_MODE4 = 4;
		public static final int SAMSCREEN_MODE_UNDETERMINED = 0;

		public static byte Seperator[] = { 0x00, 0x11, 0x22, 0x7f };

		public byte rawFrameBuffer[];
		public int height;
		public int width;
		public int mode;

		public int Palette1[];
		public int Palette2[];
		public SamScreenInterrupt Interrupts[];

		/**
		 * Wrapper for a Sam Coupe screen file.
		 * 
		 * @param data  - Raw data to be parsed.
		 * @param IsRaw - Does the data contain the 9 byte header?
		 * @throws Exception
		 */
		public SAMScreen(byte data[], boolean IsRaw) {
			try {
				if (!IsRaw) {
					// remove the file header (first 9 bytes)
					byte newdata[] = new byte[data.length - 9];
					System.arraycopy(data, 9, newdata, 0, newdata.length);
					data = newdata;
				}
				// Try to figure out what screen mode.
				mode = SAMSCREEN_MODE4; // default.

				// If file is < mode2 size, treat as mode 1
				// If file is < mode 3/4 size, treat as mode 2
				if (data.length < 12000) {
					mode = SAMSCREEN_MODE1;
				} else if (data.length < 24576) {
					mode = SAMSCREEN_MODE2;
				}

				// default display parameters
				height = 192;
				width = 256;

				int palettestart = 0;

				// copy the raw frame buffer.
				switch (mode) {
				case SAMSCREEN_MODE1:
					rawFrameBuffer = new byte[6912];
					System.arraycopy(data, 0, rawFrameBuffer, 0, rawFrameBuffer.length);
					break;
				case SAMSCREEN_MODE2:
					throw new Exception("Sam screenmode 2 not supported");
				// havent determined between mode 3 or 4 yet.
				case SAMSCREEN_MODE4:
					rawFrameBuffer = new byte[24576];
					System.arraycopy(data, 0, rawFrameBuffer, 0, rawFrameBuffer.length);
					palettestart = 24576;
					break;
				}

				Palette1 = ReadPalette(data, palettestart);
				palettestart = palettestart + Palette1.length + 4;
				Palette2 = ReadPalette(data, palettestart);
				palettestart = palettestart + Palette2.length + 4;

				if (Palette1.length == 4) {
					// If our palette contains 4 entries, we are mode 3, which has a width of 512.
					mode = SAMSCREEN_MODE3;
					width = 512;
				}

				// Load interrupts until terminated with 0xff
				ArrayList<SamScreenInterrupt> TmpIntList = new ArrayList<SamScreenInterrupt>();

				while ((data[palettestart] & 0xff) != 0xff) {
					SamScreenInterrupt s = new SamScreenInterrupt(data, palettestart);
					TmpIntList.add(s);
					palettestart = palettestart + 4;
				}
				Interrupts = TmpIntList.toArray(new SamScreenInterrupt[TmpIntList.size()]);
			} catch (Exception E) {
				mode = SAMSCREEN_MODE_UNDETERMINED;
				width = 0;
				height = 0;
				Interrupts = new SamScreenInterrupt[0];
				Palette1 = new int[0];
				Palette2 = new int[0];
				rawFrameBuffer = data;
			}
		}

		/**
		 * Get the image as an ImageData object for display. Doesn't yet support Mode 2
		 * screens as i have no documentation about the format. Mode 3 screens haven't
		 * yet been tested, but should work
		 * 
		 * Interrupt colour changes havent been implemented. 
		 * 
		 * @return
		 */
		public ImageData GetImage() {
			if (mode == SAMSCREEN_MODE2) {
				System.out.println("MOde 2 screens not supported");
				return (null);
			} else if (mode == SAMSCREEN_MODE1) { // speccy mode
				ImageData result = Speccy.GetImageFromFileArray(rawFrameBuffer, 0);
				return (result);
			} else if (mode == SAMSCREEN_MODE4 || mode == SAMSCREEN_MODE3) {
				PaletteData palette = new PaletteData(0x0000FF, 0x00FF00, 0xFF0000);
				ImageData imageData = new ImageData(width, height, 24, palette);
				int address = 0;
				int xptn = 0;
				for (int yptn = 0; yptn < height; yptn++) {
					xptn = 0;
					while (xptn < width) {
						byte data = rawFrameBuffer[address++];
						if (mode == SAMSCREEN_MODE4) {
							int b2 = data & 0x0f;
							int b1 = (data & 0xf0) >> 4;
							int col1 = Palette1[b1];
							int col2 = Palette1[b2];

							imageData.setPixel(xptn++, yptn, SamToRGB(col1));
							imageData.setPixel(xptn++, yptn, SamToRGB(col2));

						} else if (mode == SAMSCREEN_MODE3) {
							int b4 = (data & 0x03);
							int b3 = (data & 0x0c) >> 2;
							int b2 = (data & 0x30) >> 4;
							int b1 = (data & 0xc0) >> 6;
							int col1 = Palette1[b1];
							int col2 = Palette1[b2];
							int col3 = Palette1[b3];
							int col4 = Palette1[b4];

							imageData.setPixel(xptn++, yptn, SamToRGB(col1));
							imageData.setPixel(xptn++, yptn, SamToRGB(col2));
							imageData.setPixel(xptn++, yptn, SamToRGB(col3));
							imageData.setPixel(xptn++, yptn, SamToRGB(col4));
						}
					}
				}
				return (imageData);
			} else {
				// If not types 1,2,3,4
				return (null);
			}
		}

		/**
		 * Read a palette until the terminator is found.
		 * 
		 * @param data
		 * @param index
		 * @return
		 */
		private int[] ReadPalette(byte data[], int index) {
			ArrayList<Integer> palette = new ArrayList<Integer>();

			while (index < data.length && !IsEndOfPalette(data, index)) {
				palette.add(Integer.valueOf(data[index++] & 0xff));
			}
			int result[] = new int[palette.size()];
			int ix = 0;

			for (Integer x : palette) {
				result[ix++] = x;
			}

			return (result);
		}

		/**
		 * Check to see if the byte pointed to by the index is the end marker.
		 * 
		 * @param data
		 * @param index
		 * @return
		 */
		private boolean IsEndOfPalette(byte data[], int index) {
			return (data[index] == Seperator[0] && data[index + 1] == Seperator[1] && data[index + 2] == Seperator[2]
					&& data[index + 3] == Seperator[3]);
		}

		/**
		 * Convert a Sam Palette entry into an RGB colour.
		 * 
		 * This is an extremely long-winded way of doing it, but at time of writing i was sure i was doing it
		 * wrong, and i wanted to make it easier to debug.
		 * 
		 * @param samcol - SAM colour
		 * @return rgb
		 */
		public int SamToRGB(int samcol) {
			boolean b0 = (samcol & 0x01) != 0;
			boolean r0 = (samcol & 0x02) != 0;
			boolean g0 = (samcol & 0x04) != 0;
			boolean br = (samcol & 0x08) != 0;
			boolean b1 = (samcol & 0x10) != 0;
			boolean r1 = (samcol & 0x20) != 0;
			boolean g1 = (samcol & 0x40) != 0;

			int blu = convbool(b0, b1, br);
			int red = convbool(r0, r1, br);
			int grn = convbool(g0, g1, br);

			return (red + (grn << 8) | (blu << 16));
		}

		/**
		 * Convert the booleans for lsb,msb and bright into a number.
		 * 
		 * @param a
		 * @param b
		 * @param half
		 * @return
		 */
		private int convbool(boolean a, boolean b, boolean intens) {
			int i = 0;
			if (a)
				i = 0x40;
			if (b)
				i = i | 0x80;
			if (!intens) {
				i = i / 2;
			}
			return (i);
		}

		/**
		 * return details as a string.
		 */
		@Override
		public String toString() {
			String result = "";
			result = result + "Sam Coupe screen file:" + System.lineSeparator();
			result = result + "Mode: " + mode + System.lineSeparator();
			result = result + "Height: " + height + System.lineSeparator();
			result = result + "Width: " + width + System.lineSeparator();
			result = result + "Colour depth: " + Palette1.length + System.lineSeparator();
			result = result + "Palette 1: ";
			for (int col : Palette1) {
				result = result + col + ", ";
			}
			result = result.substring(0, result.length() - 2) + System.lineSeparator();

			result = result + "Palette 2: ";
			for (int col : Palette2) {
				result = result + col + ", ";
			}
			result = result.substring(0, result.length() - 2) + System.lineSeparator();

			result = result + "Colour change interrupts: " + System.lineSeparator();
			for (SamScreenInterrupt i : Interrupts) {
				result = result + "  " + i.toString() + System.lineSeparator();
			}

			return (result);
		}

	}

	/**
	 * Implementation of a Sam Screen interrupt. This is a wrapper around the 4-byte
	 * interrupt.
	 */
	public static class SamScreenInterrupt {
		private byte rawdata[];

		public SamScreenInterrupt(byte[] data, int index) {
			rawdata = new byte[4];
			System.arraycopy(data, index, rawdata, 0, 4);
		}

		public byte[] GetRawData() {
			return (rawdata);
		}

		public int GetYPtn() {
			return (172 - (rawdata[0] & 0xff));
		}

		public void SetYPtn(int y) {
			rawdata[0] = (byte) ((172 - y) & 0xff);
		}

		public int GetColourNum() {
			return (rawdata[1] & 0xff);
		}

		public void SetColourNum(int colnum) {
			rawdata[1] = (byte) (colnum & 0xff);
		}

		public int GetPaletteACol() {
			return (rawdata[2] & 0xff);
		}

		public void SetPaletteACol(int col) {
			rawdata[2] = (byte) (col & 0xff);
		}

		public int GetPaletteBCol() {
			return (rawdata[3] & 0xff);
		}

		public void SetPaletteBCol(int col) {
			rawdata[3] = (byte) (col & 0xff);
		}

		/**
		 * return details as a string.
		 */
		@Override
		public String toString() {
			String result = "";
			result = result + "YPos: " + GetYPtn();
			result = result + " Colour num: " + GetColourNum();
			result = result + " Palette A col:" + GetPaletteACol();
			result = result + " Palette B col:" + GetPaletteBCol();

			return (result);
		}
	}

	/**
	 * Test harness. Parameters: [0] File to load. (will only work on files without
	 * headers) [1] (optional) target for the image to be written for a PNG.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Params: [filename] [output filename]");
		} else {
			byte data[] = GeneralUtils.ReadFileIntoArray(args[0]);
			try {
				SAMScreen sc = new SAMScreen(data, false);
				System.out.println(sc);
				int ptr = 0;
				int part = 0;
				for (int y = 0; y < sc.height; y++) {
					for (int x = 0; x < sc.width; x++) {
						int d = (sc.rawFrameBuffer[ptr] & 0xff);

						for (int i = 0; i < part; i++) {
							d = d / sc.Palette1.length;
						}
						d = d & (sc.Palette1.length - 1);

						System.out.print(String.format("%X", d));
						part--;
						if (sc.mode == SAMScreen.SAMSCREEN_MODE3) {
							if (part == -1) {
								part = 3;
								ptr++;
							}
						} else if (sc.mode == SAMScreen.SAMSCREEN_MODE4) {
							if (part == -1) {
								part = 1;
								ptr++;
							}
						}
					}
					System.out.println();
				}

				ImageData id = sc.GetImage();

				if (args.length > 1) {
					ImageLoader saver = new ImageLoader();
					saver.data = new ImageData[] { id };
					saver.save(args[1], SWT.IMAGE_PNG);
				}

				Display display = new Display();
				Shell shell = new Shell(display);
				Label label = new Label(shell, SWT.BORDER);

				label.setImage(new Image(display, id));

				shell.setSize(300, 200);
				shell.open();
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch())
						display.sleep();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static final String[] tokens = {
			"","","","","","","<PComma>","<edit>","<left>","<right>","<down>","<up>","<del>","<cr>","<num>","",
			"<pen>","<paper>","<flash>","<bright>","<inverse>","<over>","<at>","<tab>","<lWord>","<rWord>","","","","","","",
			"","","","","","","EXIT PROC","EXIT DO","EXIT FOR","LOCN","RESERVED","EQU","TICKS","SHIFT$","SVAL$","USING$",
			"TIME$","DATE$","INP$","DIR$","FSTAT","DSTAT","FPAGES","SCRAD","INARRAY","","","PI","RND","POINT","FREE","LENGTH",

			"ITEM","ATTR","FN","BIN","XMOUSE","YMOUSE","XPEN","YPEN","RAMTOP","","INSTR","INKEY$","SCREEN$","MEM$","","PATH$",
			"STRING$","","","SIN","COS","TAN","ASN","ACS","ATN","LN","EXP","ABS","SGN","SQR","INT","USR",
			"IN","PEEK","DPEEK","DVAR","SVAR","BUTTON","EOF","PTR","XVAR","UDG","NVAL","LEN","CODE","VAL$","VAL","TRUNC$",
			"CHR$","STR$","BIN$","HEX$","USR$","","NOT","","","","MOD","DIV","BOR","","BAND","OR",
			
			"AND","<>","<=",">=","-","USING","WRITE","AT","TAB","OFF","WHILE","UNTIL","LINE","THEN","TO","STEP",
			"DIR","FORMAT","ERASE","MOVE","SAVE","LOAD","MERGE","VERIFY","OPEN","CLOSE","CIRCLE","PLOT","LET","BLITZ","BORDER","CLS",
			"PALETTE","PEN","PAPER","FLASH","BRIGHT","INVERSE","OVER","FATPIX","CSIZE","BLOCKS","MODE","GRAB","PUT","BEEP","SOUND","NEW",
			"RUN","STOP","CONTINUE","CLEAR","GO TO","GO SUB","RETURN","REM","READ","DATA","RESTORE","PRINT","LPRINT","LIST","LLIST","DUMP",

			"FOR","NEXT","PAUSE","DRAW","DEFAULT","DIM","INPUT","RANDOMIZE","DEF FN","DEF KEYCODE","DEF PROC","END PROC","RENUM","DELETE","REF","COPY",
			"","KEYIN","LOCAL","LOOP","IF DO","LOOP","EXIT IF","IF","IF","ELSE","ELSE","END IF","KEY","ON ERROR","ON","GET",
			"OUT","POKE","DPOKE","RENAME","CALL","ROLL","SCROLL","SCREEN","DISPLAY","BOOT","LABEL","FILL","WINDOW","AUTO","POP","RECORD",
			"DEVICE","PROTECT","HIDE","ZAP","POW","BOOM","ZOOM","BACKUP","TIME","DATE","ALTER","SORT","JOIN","EDIT","",""
	};
	public static final String[] characters = {
			"","","","","","","<PComma>","<edit>","<left>","<right>","<down>","<up>","<del>","<cr>","<num>","",
			"<pen>","<paper>","<flash>","<bright>","<inverse>","<over>","<at>","<tab>","<lWord>","<rWord>","","","","","","",
			" ","!","\"","#","$","%","&","'","(",")","*","+",",","-",".","/",
			"0","1","2","3","4","5","6","7","8","9",":",";","<","=",">","?",

			"@","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O",
			"P","Q","R","S","T","U","V","W","X","Y","Z","[","\\","]","^","_",
			"£","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o",
			"p","q","r","s","t","u","v","w","x","y","z","{","|","}","~","" + (char) 0x24B8,

			//TODO: complete Sam characters
			"","","","","","","","","","","","","","","","",
			"","","","","","","","","","","","","","","","",
			"","","","","","","","","","","","","","","","",
			"","","","","","","","","","","","","","","","",

			"","","","","","","","","","","","","","","","",
			"","","","","","","","","","","","","","","","",
			"","","","","","","","","","","","","","","","",
			"","","","","","","","","","","","","","","",""
	};

	
}

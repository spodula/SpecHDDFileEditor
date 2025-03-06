package hddEditor.libs.snapshots.readers.zxStateBlocks;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/*-*
 * Wrapper for the first 8 bytes of a ZX-State block and a few helper functions. 
 * This always contains the ID Type (first 4 bytes) and the length (Second 4 bytes, LSB first)
 */

public class GenericZXStateBlock {
	public static String[][] BlockTypeIdentifiers = { { "ZXSTATASP", "ZXAT", "ATA Ide Hard disk interface" },
			{ "ZXSTATASPRAM", "ATRP", "ZXATASP ram contents" }, { "ZXSTAYBLOCK", "AY", "AY-3-8912 Registers" },
			{ "ZXSTCF", "ZXCF", "ZXCF compact flash interface" }, { "ZXSTCFRAM", "CFRP", "ZXCF Ram" },
			{ "ZXSTCOVOX", "COVX", "State of the COVOX sound system" }, { "ZXSTBETA128", "B128", "Beta 128 state" },
			{ "ZXSTBETADISK", "BDSK", "Beta 128 disk drive" }, { "ZXSTCREATOR", "CRTR", "Creator program" },
			{ "ZXSTDOCK", "DOCK", "Timex Expansion dock" }, { "ZXSTDSKFILE", "DSK", "+3 inserted disk" },
			{ "ZXSTGS", "GS", "Registers and internal state for the General sound interface" },
			{ "ZXSTGSRAMPAGE", "GSRP", "General sound interface ram page" },
			{ "ZXSTKEYBOARD", "KEYB", "Keyboard state and joystick emulation flags" },
			{ "ZXSTIF1", "IF1", "State of the IF/1" }, { "ZXSTIF2ROM", "IF2R", "Interface 2 rom cart" },
			{ "ZXSTJOYSTICK", "JOY", "Joystick setup for 2 players" },
			{ "ZXSTMCART", "MDRV", "Microdrive cartridge (1 for each drive with a cart)" },
			{ "ZXSTMOUSE", "AMXM", "Mouse emulation" }, { "ZXSTMULTIFACE", "MFCE", "State of any multiface connected" },
			{ "ZXSTOPUS", "OPUS", "Discovery disk interface" },
			{ "ZXSTOPUSDISK", "ODSK", "discovery disk drive (1 for each)" },
			{ "ZXSTPLUS3", "+3", "Number of +3 Disk drives and motor state" },
			{ "ZXSTPLUSD", "PLSD", "Plus D interface state" },
			{ "ZXSTPLUSDDISK", "PDSK", "Plus D drive (One for each drive)" },
			{ "ZXSTRAMPAGE", "RAMP", "one 16K Ram page" }, { "ZXSTROM", "ROM", "Custom Rom" },
			{ "ZXSTSCLD", "SCLD", "Screen mode and memory paging status to Timex machines" },
			{ "ZXSTSIDE", "SIDE", "Presence of the Simple IDE interface" },
			{ "ZXSTSPECDRUM", "DRUM", "State of the Specdrum interface" }, { "ZXSTSPECREGS", "SPCR", "ULA status" },
			{ "ZXSTTAPE", "TAPE", "State of the virtual cassette recorder" },
			{ "ZXSTUSPEECH", "USPE", "State of the Currah uSpeech" },
			{ "ZXSTZXPRINTER", "ZXPR", "Presence of the ZX-Printer" },
			{ "ZXSTZ80REGS", "Z80R", "Z80 Registers and internal state" } };

	public String BlockID;
	public int BlockSize;
	public byte raw[];

	/**
	 * 
	 * @param rawdata
	 * @param start
	 */
	public GenericZXStateBlock(byte rawdata[], int start) {
		byte header[] = new byte[8];
		System.arraycopy(rawdata, start, header, 0, 8);

		BlockID = (new String(header)).substring(0, 4).trim();

		BlockSize = (header[4] & 0xff) + ((header[5] & 0xff) * 0x100) + ((header[6] & 0xff) * 0x10000)
				+ ((header[7] & 0xff) * 0x1000000);
		this.raw = new byte[BlockSize + header.length];
		System.arraycopy(rawdata, start, this.raw, 0, this.raw.length);
	}

	/**
	 * 
	 */
	public String toString() {
		String result = BlockID + " (" + GetBlockTypeName() + ") Len: " + String.valueOf(BlockSize);
		return (result);
	}

	/**
	 * 
	 * @param i
	 * @return
	 */
	public int rawbyte(int i) {
		return (raw[i] & 0xff);
	}

	/**
	 * 
	 * @param i
	 * @return
	 */
	public int rawword(int i) {
		return (rawbyte(i) + (rawbyte(i + 1) * 0x100));
	}

	/**
	 * 
	 * @param i
	 * @return
	 */
	public int rawDword(int i) {
		return (rawword(i) + (rawword(i + 2) * 0x10000));
	}

	/**
	 * 
	 * @param i
	 * @return
	 */
	public String Hex(int i) {
		String result = String.format("%02X", i);
		return (result);
	}

	/**
	 * Used to decode flags from a flag array
	 * 
	 * @param arr
	 * @param flags
	 * @param index
	 * @return
	 */
	public String GetFlagsFromArray(String arr[][], int flags, int index) {
		String result = "";
		for (String entry[] : arr) {
			int flag = Integer.parseInt(entry[0]);
			if ((flags & flag) == flag) {
				if (!result.isBlank()) {
					result = result + ", ";
				}
				result = result + entry[index];
			}
		}

		if (result.isEmpty()) {
			result = "<none set>";
		}
		return (result);
	}

	public String GetNameFromArray(String arr[][], String data, int index, int value) {
		String result = "<Undefined>";
		for (String typ[] : arr) {
			if (data.equals(typ[index])) {
				result = typ[value];
			}
		}
		return (result);
	}

	public String GetBlockTypeName() {
		return (GetNameFromArray(BlockTypeIdentifiers, BlockID, 1, 2));
	}

	/**
	 * Zlib decompress a given memory block. 
	 * @param src
	 * @param targetsize
	 * @return
	 */
	protected byte[] zLibDecompressData(byte src[], int targetsize) {
		byte result[] = null;
		Inflater decompresser = new Inflater();
		decompresser.setInput(src, 0, src.length);
		result = new byte[targetsize];
		try {
			int resultLength = decompresser.inflate(result);
			if (resultLength != 0x4000) {
				System.out.println("Warning, was expecting " + targetsize + " bytes, got " + resultLength);
			}
			System.arraycopy(result, 0, result, 0, 0x4000);
		} catch (DataFormatException e) {
			System.out.println("Error decompressing data");
			result = null;
		}
		decompresser.end();
		return(result);
	}

}

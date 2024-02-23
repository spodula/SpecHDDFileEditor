package hddEditor.libs;
/**
 * Useful constants for TZX file decoding.
 * 
 */


public class TZX {
	//TZX block types
	public static final int TZX_STANDARDSPEED_DATABLOCK = 0x10;
	public static final int TZX_TURBOSPEED_DATABLOCK = 0x11;
	public static final int TZX_PURETONE = 0x12;
	public static final int TZX_PULSESEQ = 0x13;
	public static final int TZX_PUREDATA = 0x14;
	public static final int TZX_DIRECTRECORDING = 0x15;
	public static final int TZX_CSWRECORDING = 0x18;
	public static final int TZX_GENERAL = 0x19;
	public static final int TZX_PAUSE = 0x20;
	public static final int TZX_GROUPSTART = 0x21;
	public static final int TZX_GROUPEND = 0x22;
	public static final int TZX_JUMP = 0x23;
	public static final int TZX_LOOPSTART = 0x24;
	public static final int TZX_LOOPEND = 0x25;
	public static final int TZX_CALLSEQ = 0x26;
	public static final int TZX_RETSEQ = 0x27;
	public static final int TZX_SELECTBLOCK = 0x28;
	public static final int TZX_STOP48 = 0x2A;
	public static final int TZX_SETSIGNALLEVEL=0x2B;
	public static final int TZX_TEXTDESC = 0x30;
	public static final int TZX_MESSAGEBLOCK = 0x31;
	public static final int TZX_ARCHIVEINFO = 0x32;
	public static final int TZX_HARDWARETYPE = 0x33;
	public static final int TZX_CUSTOMINFO = 0x35;
	public static final int TZX_GLUE = 0x5A;
	
	// Default time in MS for the delay between blocks
	public static int DEFAULT_STD_DELAY = 954;
	
	//Calculate the checksum for the given datablock.
	public static int CalculateChecksumForBlock(byte block[]) {
		int checksum = 0;
		if (block.length > 0) {
			for (int i = 0; i < block.length - 1; i++) {
				checksum = checksum ^ (block[i] & 0xff);
			}
		}
		int i=(checksum & 0xff);
		return(i);
	}
}

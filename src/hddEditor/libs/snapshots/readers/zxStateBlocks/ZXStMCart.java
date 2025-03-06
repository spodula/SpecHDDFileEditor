package hddEditor.libs.snapshots.readers.zxStateBlocks;
/**
 * Implements the ZXSTMCART block
 * https://www.spectaculator.com/docs/zx-state/microdrive.shtml
 * This contains an active Microdrive cart details.
 * 
 * $00..$03  ID: "MDRV") 		) Decoded by the parent class. 
 * $04..$07  size: LSB..MSB		)
 * ====start of  ZXSTMCART information====
 * $08..$09		Flags
 * $0A			microdrive number
 * $0B			Running?
 * $0C..$0D		drive Position in the file
 * $0E..$0F		Number of Preamble bytes left to skip
 * $10..$13		Uncompressed size
 * $14..End		Data / Filename
 * 
 */

public class ZXStMCart extends GenericZXStateBlock {
	public static int ZXSTMDF_COMPRESSED=1;
	public static int ZXSTMDF_EMBEDDED=2;
	public static String MDRVFlags[][] = {
			{"1"  ,"ZXSTMDF_COMPRESSED","Cart is compressed"},
			{"2"  ,"ZXSTMDF_EMBEDDED","Cart embedded"},  //Note, the docs say this is not yet supported
	};
	
	public int Flags;
	public int DriveNumber;
	boolean DriveRunning;
	public int DrivePos;
	public int Preamble;
	public int UncompressedSize;
	public byte RawMDR[];
	public byte UncompressedMDR[];
	public String Filename;
	

	public ZXStMCart(byte[] rawdata, int start) {
		super(rawdata, start);
		Flags = rawword(0x08);
		DriveNumber = rawbyte(0x0A);
		DriveRunning = rawbyte(0x0B)==1;
		DrivePos = rawword(0x0C);
		Preamble = rawword(0x0E);
		UncompressedSize = rawDword(0x10);
		
		byte data[] = new byte[raw.length-0x14];
		System.arraycopy(raw, 0x0F, data, 0, data.length);
		
		if ((Flags & ZXSTMDF_EMBEDDED) != ZXSTMDF_EMBEDDED) {
			Filename = new String(data).trim();
			RawMDR = null;
			UncompressedMDR = null;
		} else {
			Filename = null;
			RawMDR = data;
			if ((Flags & ZXSTMDF_COMPRESSED) != ZXSTMDF_COMPRESSED) {
				UncompressedMDR = RawMDR;
			} else {
				UncompressedMDR = zLibDecompressData(RawMDR, UncompressedSize);
			}
		}
	}

	
	/**
	 * Add in the ZXSTMCART specific items
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Drive:"+DriveNumber+" Running:"+DriveRunning+" Pos:"+DrivePos+" Preamble:"+Preamble+" Flags:"+GetFlags()+" ("+Flags+")";
		return (result);
	}
	
	public String GetFlags() {
		return(GetFlagsFromArray(MDRVFlags,Flags,1));
	}
	
}

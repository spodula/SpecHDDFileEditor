package hddEditor.libs.snapshots.readers.zxStateBlocks;
/**
 * Implements the ZXSTROM block
 * https://www.spectaculator.com/docs/zx-state/custom_rom.shtml
 * This contains a custom Rom. 
 * Snapshots using the standard roms wont have one of these.
 * 
 * $00..$03  ID: "ROM",$0) 		) Decoded by the parent class. 
 * $04..$07  size: LSB..MSB		)
 * ====start of  ZXSTROM information====
 * $08..$09	 Flags
 * $0A..$0D	 Uncompressed size
 * $0E..end	 Rom file
 */

public class ZXStRom extends GenericZXStateBlock {
	public static int ZXSTRF_COMPRESSED=1;
	public static String RomFlags[][] = {
			{"1"  ,"ZXSTRF_COMPRESSED","Rom is compressed"},
	};
	
	public int Flags;
	public int UncompressedSize;
	public byte RawRom[];
	public byte UncompressedRom[];
	
	public ZXStRom(byte[] rawdata, int start) {
		super(rawdata, start);
		Flags = rawword(0x08);
		UncompressedSize = rawDword(0x0A);
		
		byte data[] = new byte[raw.length-0x0E];
		System.arraycopy(raw, 0x0E, data, 0, data.length);
		
		RawRom = data;
		if ((Flags & ZXSTRF_COMPRESSED) != ZXSTRF_COMPRESSED) {
			UncompressedRom = RawRom;
		} else {
			UncompressedRom = zLibDecompressData(RawRom, UncompressedSize);
		}
	}
	
	
	/**
	 * Add in the ZXSTROM specific items
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result +" Size:"+UncompressedSize+" Flags: "+GetFlags()+" ("+Flags+")";
		
		return (result);
	}
	
	public String GetFlags() {
		return(GetFlagsFromArray(RomFlags,Flags,1));
	}
}

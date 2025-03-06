package hddEditor.libs.snapshots.readers.zxStateBlocks;

/*-*
 * Implements the ZXSTRAMPAGE block
 * https://www.spectaculator.com/docs/zx-state/rampage.shtml
 * This contains one 16K Ram page.
 * 
 * $00..$03 	ID: "RAMP"  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTRAMPAGE information====
 * $08..$09    	Flags (0x01 = Compressed)"
 * $0A 		   	Page number
 * $0B..End    	Memory data
 */

public class ZXStRamPage extends GenericZXStateBlock {
	public static String ramFlags[][] = {
			{"1","ZXSTRF_COMPRESSED","Compressed"}
	};
	
	public int flags;
	public int pagenum;
	public byte MemoryData[];
	public byte UncompressedMemoryData[];

	public ZXStRamPage(byte[] rawdata, int start) {
		super(rawdata, start);
		flags = rawword(0x08);
		pagenum = rawbyte(0x0a);
		int compressdatalength = raw.length - 0x0b;
		MemoryData = new byte[compressdatalength];
		System.arraycopy(raw, 0x0b, MemoryData, 0, compressdatalength);
		if ((flags & 0x01) == 0x01) {
			UncompressedMemoryData = zLibDecompressData(MemoryData,0x4000);
		} else {
			UncompressedMemoryData = MemoryData;
		}
	}
	

	/**
	 * Add in the ZXStRamPage values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Page:"+pagenum+" Flags:"+FlagsAsString()+" Len: C:"+MemoryData.length+" U:"+UncompressedMemoryData.length;
	
		return (result);
	}
	
	public String FlagsAsString() {
		return(GetFlagsFromArray(ramFlags, flags, 2));
	}

}

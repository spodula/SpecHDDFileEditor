package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * UNTESTED
 * Implements the ZXSTDOCK block
 * https://www.spectaculator.com/docs/zx-state/dock.shtml
 * Expansion cart for the Timex series of machines.
 * 
 * $00..$03 	ID: "DOCK"  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTDOCK information====
 * $08..$09		Flags
 * $0A			Page no.
 * $0B..end		Ram page
 */
public class ZXStDock  extends GenericZXStateBlock {
	public static String ramFlags[][] = {
			{"1","ZXSTRF_COMPRESSED","Compressed"},
			{"2","ZXSTDF_RAM","Read=1 read/write=0"},
			{"4","ZXSTDF_EXROMDOCK","1=Dock, 0=EXROM"},
			
	};
	
	public int flags;
	public int pagenum;
	public byte MemoryData[];
	public byte UncompressedMemoryData[];

	public ZXStDock(byte[] rawdata, int start) {
		super(rawdata, start);
		flags = rawword(0x08);
		pagenum = rawbyte(0x0a);
		int compressdatalength = raw.length - 0x0b;
		MemoryData = new byte[compressdatalength];
		System.arraycopy(raw, 0x0b, MemoryData, 0, compressdatalength);
		if ((flags & 0x01) == 0x01) {
			UncompressedMemoryData = zLibDecompressData(MemoryData,0x2000);
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

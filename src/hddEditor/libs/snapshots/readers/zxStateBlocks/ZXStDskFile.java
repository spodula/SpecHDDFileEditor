package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * UNTESTED...
 * Implements the ZXSTDSKFILE block
 * https://www.spectaculator.com/docs/zx-state/dskfile.shtml
 * This contains one +3 Floppy disk drive
 * 
 * $00..$03 	ID: "DSK0"  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTDSKFILE information====
 * $08..$09		Flags
 * $0A			drive number
 * $0B..$0E		Uncompressed size
 * $0F..		Either disk filename or raw disk data.
 * 
 */
public class ZXStDskFile extends GenericZXStateBlock {
	public static int ZXSTDSKF_COMPRESSED=1;
	public static int ZXSTDSKF_EMBEDDED=2;
	public static String DiskFlags[][] = {
			{"1","ZXSTDSKF_COMPRESSED","Embedded disk image is compressed"},
			{"2","ZXSTDSKF_EMBEDDED","Disk image is embedded"},
			{"4","ZXSTDSKF_SIDEB","Disk Side B merged"}
	};
	
	public int Flags;
	public int DriveNum;
	public int UnCompressedSize;
	public String Filename;
	public byte rawDisk[];
	public byte rawDiskUncompressed[];

	public ZXStDskFile(byte[] rawdata, int start) {
		super(rawdata, start);
		Flags = rawword(0x08);
		DriveNum = rawbyte(0x0A);
		UnCompressedSize = rawDword(0x0B);
		
		byte data[] = new byte[raw.length-0x0F];
		System.arraycopy(raw, 0x0F, data, 0, data.length);
		Filename = null;
		rawDisk = null;
		rawDiskUncompressed = null;
		
		if ((Flags & ZXSTDSKF_EMBEDDED) == ZXSTDSKF_EMBEDDED) {
			rawDisk = data;
			if ((Flags & ZXSTDSKF_COMPRESSED) == ZXSTDSKF_COMPRESSED) {
				rawDiskUncompressed = zLibDecompressData(data, UnCompressedSize);
			} else {
				rawDiskUncompressed = rawDisk;
			}
		} else {
			Filename = new String(data).trim();
		}	
	}	
	
	/**
	 * Add in the ZXSTDSKFILE values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " drive:"+DriveNum+" Uncompressedsize:"+UnCompressedSize+" Flags:"+GetFlags()+" ("+Flags+") ";
		if (Filename!=null) {
			result = result + " Filename:"+Filename;
		}
		return (result.trim());
	}
	
	/**
	 * 
	 * @return
	 */
	public String GetFlags() {
		return(GetFlagsFromArray(DiskFlags, Flags,2));
	}
	
}

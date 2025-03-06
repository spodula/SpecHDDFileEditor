package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * UNTESTED
 * Implements the ZXSTBETADISK block
 * https://www.spectaculator.com/docs/zx-state/betadisk.shtml
 * This the status of a single Beta disk drive.
 * 
 * $00..$03 	ID: "BSDK"  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTBETADISK information====
 * $08..$0B		Flags
 * $0C			Drive number
 * $0D 			Cylinder
 * $0E			Disk type
 * $0F..End		Either the disk name or the actual disk image	
 */

public class ZXStBetaDisk extends GenericZXStateBlock {
	public static int ZXSTBDF_EMBEDDED = 1;
	public static int ZXSTBDF_COMPRESSED = 2;
	
	public static String DiskFlags[][] = {
			{"1","ZXSTBDF_EMBEDDED","Disk image is embedded"},
			{"2","ZXSTBDF_COMPRESSED","Embedded disk image is compressed"},
			{"4","ZXSTBDF_WRITEPROTECT","Disk is write protected"}
	};
	
	public static String[][] DriveTypes = {
			{"0","ZXSTBDT_TRD","TRD"},
			{"1","ZXSTBDT_SCL","SCL"},
			{"2","ZXSTBDT_FDI","FDI"},    
			{"3","ZXSTBDT_UDI","UDI"}
	};
	
	public int flags;
	public int drivenum;
	public int cyl;
	public int disktype;
	public String Filename;
	public byte rawDisk[];
	public byte rawDiskUncompressed[];
	
	public ZXStBetaDisk(byte[] rawdata, int start) {
		super(rawdata, start);		
		flags = rawDword(0x08);
		drivenum = rawbyte(0x0C);
		cyl = rawbyte(0x0D);
		disktype = rawbyte(0x0E);
		
		byte data[] = new byte[raw.length-0x0F];
		System.arraycopy(raw, 0x0F, data, 0, data.length);
		Filename = null;
		rawDisk = null;
		rawDiskUncompressed = null;
		
		if ((flags & ZXSTBDF_EMBEDDED) == ZXSTBDF_EMBEDDED) {
			rawDisk = data;
			if ((flags & ZXSTBDF_COMPRESSED) == ZXSTBDF_COMPRESSED) {
				rawDiskUncompressed = zLibDecompressData(data, 0x4000);
			} else {
				rawDiskUncompressed = rawDisk;
			}
		} else {
			Filename = new String(data).trim();
		}
	}
	
	/**
	 * Add in the ZXSTSPECDRUM values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " drive:"+drivenum+" Cyl:"+cyl+" Type:"+GetDiskTypeName()+"("+disktype+") Flags:"+GetFlags()+" ("+flags+") ";
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
		return(GetFlagsFromArray(DiskFlags, flags,2));
	}
	
	/**
	 * 
	 * @return
	 */
	public String GetDiskTypeName() {
		return(GetNameFromArray(DriveTypes, String.valueOf(disktype), 0, 2));
	}

}

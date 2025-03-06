package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * UNTESTED...
 * Implements the ZXSTOPUSDISK block
 * https://www.spectaculator.com/docs/zx-state/opusdisk.shtml
 * This contains one Opus Floppy disk drive
 * 
 * $00..$03 	ID: "ODSK"  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTOPUSDISK information====
 * $08..$0B		Flags
 * $0C			drive number
 * $0D			Current Cylinder
 * $0E			Disk type
 * $0F..		Either disk filename or raw disk data.
 * 
 */

public class ZXStOpusDisk extends GenericZXStateBlock {
	public static int DISK144Size=0x168000;
	
	public static int ZXSTOPDF_EMBEDDED=1;
	public static int ZXSTOPDF_COMPRESSED=2;
	public static String DiskFlags[][] = {
			{"1","ZXSTOPDF_EMBEDDED","Embedded disk image is compressed"},
			{"2","ZXSTOPDF_COMPRESSED","Disk image is embedded"},
			{"4","ZXSTOPDF_WRITEPROTECT","Disk is write protected"}
	};
	public static int ZXSTOPDT_OPD=0;
	public static int ZXSTOPDT_OPU=1;
	
	public static String DiskTypes[][] = {
			{"0","ZXSTOPDT_OPD","Disk image is OPD format"},
			{"1","ZXSTOPDT_OPU","Disk image is OPU format"},
			{"2","ZXSTOPDT_FLOPPY0","Real disk mode:first floppy drive"},
			{"3","ZXSTOPDT_FLOPPY1","Real disk mode:Second floppy drive"}
	};
	
	public int Flags;
	public int DriveNum;
	public int Cylinder;
	public int DiskType;
	public String filename;
	public byte CompressedDisk[];
	public byte UnCompressedDisk[];
	
	public ZXStOpusDisk(byte[] rawdata, int start) {
		super(rawdata, start);
		Flags = rawDword(0x08);
		DriveNum = rawbyte(0x0C);
		Cylinder = rawbyte(0x0D);
		DiskType = rawbyte(0x0E);
		filename = null;
		CompressedDisk = null;
		UnCompressedDisk = null;

		//For Disk image types (OPD and OPU)
		if ((DiskType == ZXSTOPDT_OPD) || (DiskType == ZXSTOPDT_OPU)) {
			byte data[] = new byte[raw.length-0x0F];
			System.arraycopy(raw, 0x0F, data, 0, data.length);
			if ((Flags & ZXSTOPDF_EMBEDDED) != ZXSTOPDF_EMBEDDED) {	//Only filename
				filename = new String(data).trim();
			} else {
				CompressedDisk = data;
				if ((Flags & ZXSTOPDF_COMPRESSED) != ZXSTOPDF_COMPRESSED) {	//Only filename
					UnCompressedDisk = zLibDecompressData(CompressedDisk, DISK144Size); //Note, this will probably produce a warning.
				} else {
					UnCompressedDisk = CompressedDisk;
				}
			}
		}
	}

	/**
	 * Add in the ZXStOpusDisk values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Flags:"+GetFlags()+" Drivenum:"+DriveNum+" Cyl:"+Cylinder+" type:"+DiskType;
		if (filename != null) {
			result = result + " Filename:"+filename;
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
	
	public String GetDiskType() {
		return (GetNameFromArray(DiskTypes, String.valueOf(DiskType), 0, 2));
	}
	
}

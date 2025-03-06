package hddEditor.libs.snapshots.readers.zxStateBlocks;

/*-*
 * UNTESTED...
 * Implements the ZXSTPLUSDDISK block
 * https://www.spectaculator.com/docs/zx-state/plusddisk.shtml
 * This contains one +D Disk drive
 * 
 * $00..$03 	ID: "PDSK"  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTPLUSDDISK information====
 * $08..$0B		Flags
 * $0C			drive number
 * $0D			Cylinder
 * $0E			Disktype
 * $0F..		Either disk filename or raw disk data.
 * 
 */
public class ZXStPlusDDisk extends GenericZXStateBlock {
	public static int ZXSTPDDF_EMBEDDED = 1;
	public static int ZXSTPDDF_COMPRESSED = 2;
	public static String DiskFlags[][] = { { "1", "ZXSTPDDF_EMBEDDED", "Disk image is embedded" },
			{ "2", "ZXSTPDDF_COMPRESSED", "Embedded disk image is compressed" },
			{ "4", "ZXSTPDDF_WRITEPROTECT", "inserted disk is write protected" } };

	public static String DiskTypes[][] = { { "0", "ZXSTPDDT_MGT", "Disk image is a .MGT disk image" },
			{ "1", "ZXSTPDDT_IMG", "Disk image is a .IMG disk image" },
			{ "2", "ZXSTPDDT_FLOPPY0", "Real Floppy drive 0" }, { "3", "ZXSTPDDT_FLOPPY1", "Real Floppy drive 0" }, };

	public int Flags;
	public int DriveNum;
	public int Cylinder;
	public int DiskType;
	public String Filename;
	public byte rawDisk[];
	public byte rawDiskUncompressed[];

	public ZXStPlusDDisk(byte[] rawdata, int start) {
		super(rawdata, start);
		Flags = rawDword(0x08);
		DriveNum = rawbyte(0x0C);
		Cylinder = rawbyte(0x0D);
		DiskType = rawbyte(0x0E);

		byte data[] = new byte[raw.length - 0x0F];
		System.arraycopy(raw, 0x0F, data, 0, data.length);
		Filename = null;
		rawDisk = null;
		rawDiskUncompressed = null;

		if (DiskType > 1) {
			if ((Flags & ZXSTPDDF_EMBEDDED) == ZXSTPDDF_EMBEDDED) {
				rawDisk = data;
				if ((Flags & ZXSTPDDF_COMPRESSED) == ZXSTPDDF_COMPRESSED) { // Note, dummy number
					rawDiskUncompressed = zLibDecompressData(data, 0x1509950);
				} else {
					rawDiskUncompressed = rawDisk;
				}
			} else {
				Filename = new String(data).trim();
			}
		}
	}

	/**
	 * Add in the ZXSTPLUSDDISK values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " drive:" + DriveNum + " Cylinder:" + Cylinder + " Flags:" + GetFlags() + " (" + Flags + ") Type:"+GetDiskType();
		if (Filename != null) {
			result = result + " Filename:" + Filename;
		}
		return (result.trim());
	}

	/**
	 * 
	 * @return
	 */
	public String GetFlags() {
		return (GetFlagsFromArray(DiskFlags, Flags, 2));
	}
	public String GetDiskType() {
		return (GetNameFromArray(DiskTypes, String.valueOf(DiskType), 0, 2));
	}

}

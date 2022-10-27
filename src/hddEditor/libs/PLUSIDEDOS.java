package hddEditor.libs;

public class PLUSIDEDOS {
	public static final int PARTITION_UNUSED = 0x00;
	public static final int PARTITION_SYSTEM = 0x01;
	public static final int PARTITION_SWAP = 0x02;
	public static final int PARTITION_PLUS3DOS = 0x03;
	public static final int PARTITION_CPM = 0x04;
	public static final int PARTITION_BOOT = 0x05;
	public static final int PARTITION_MOVIE = 0x0f;
	public static final int PARTITION_FAT16 = 0x10;
	public static final int PARTITION_UZIX = 0x20;
	public static final int PARTITION_DISK_TRDOS = 0x30;
	public static final int PARTITION_DISK_PLUSD = 0x31;
	public static final int PARTITION_DISK_MB02 = 0x32;
	public static final int PARTITION_DISK_TOSA2 = 0x33;
	public static final int PARTITION_DISK_PLUS3 = 0x40;
	public static final int PARTITION_DISK_ELWRO800 = 0x41;
	public static final int PARTITION_DISK_AMSTRADCPC = 0x48;
	public static final int PARTITION_DISK_AMSTRADPCW = 0x49;
	public static final int PARTITION_BAD = 0xFE;
	public static final int PARTITION_FREE = 0xFF;

	public static class PARTSTRING {
		public String Name;
		public byte PartID;
		public int flags;

		public PARTSTRING(int partID, String Name, int flags) {
			this.Name = Name;
			this.PartID = (byte)partID;
			this.flags = flags;
		}
	}
	public static int PART_ALLOCATABLE = 1;
	public static int PART_PARTITION = 2;
	public static int PART_DISKIMAGE = 4;
	public static int PART_SPECIAL = 8;
	public static int PART_CPM = 16;
	
	public static PARTSTRING[] PartTypes = {
			new PARTSTRING(PARTITION_UNUSED, "Unused", PART_SPECIAL|PART_ALLOCATABLE),
			new PARTSTRING(PARTITION_SYSTEM, "System",PART_SPECIAL),
			new PARTSTRING(PARTITION_SWAP, "Swap", PART_SPECIAL|PART_ALLOCATABLE),
			new PARTSTRING(PARTITION_PLUS3DOS, "+3 Dos",PART_ALLOCATABLE),
			new PARTSTRING(PARTITION_CPM, "CP/M",PART_SPECIAL),
			new PARTSTRING(PARTITION_BOOT, "Boot file",PART_SPECIAL),
			new PARTSTRING(PARTITION_MOVIE, "Movie",PART_SPECIAL),
			new PARTSTRING(PARTITION_FAT16, "FAT 16",PART_SPECIAL),
			new PARTSTRING(PARTITION_UZIX, "UZI(X)",PART_SPECIAL),
			new PARTSTRING(PARTITION_DISK_TRDOS, "TRDos Disk image",PART_SPECIAL|PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_PLUSD, "+D Disk image",PART_SPECIAL|PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_MB02, "MB-02 Disk image",PART_SPECIAL|PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_TOSA2, "TOS A.2 Disk image",PART_SPECIAL|PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_PLUS3, "+3 Disk image",PART_SPECIAL|PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_ELWRO800, "Elwo 800 Jr Disk image",PART_SPECIAL|PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_AMSTRADCPC, "Amstrad CPC disk image",PART_SPECIAL|PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_AMSTRADPCW, "Amstrad PCW disk image",PART_SPECIAL|PART_DISKIMAGE),
			new PARTSTRING(PARTITION_BAD, "Bad disk space",PART_SPECIAL),
			new PARTSTRING(PARTITION_FREE, "Free disk space",PART_SPECIAL),
	};
	

}

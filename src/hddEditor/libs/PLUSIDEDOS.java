package hddEditor.libs;

public class PLUSIDEDOS {
	/*
	 * Media tyoes
	 */
	public static final int MEDIATYPE_HDD = 0x01; //hard drives
	public static final int MEDIATYPE_FDD = 0x02; //Floppy drives
	public static final int MEDIATYPE_LINEAR = 0x03; //Tapes / Microdrives

	/*
	 * Partition types.
	 */
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
	public static final int PARTITION_UNKNOWN = 0x39; // DUMMY put in for FDD support
	public static final int PARTITION_DISK_PLUS3 = 0x40;
	public static final int PARTITION_DISK_ELWRO800 = 0x41;
	public static final int PARTITION_DISK_AMSTRADCPC = 0x48;
	public static final int PARTITION_DISK_AMSTRADPCW = 0x49;
	public static final int PARTITION_BAD = 0xFE;
	public static final int PARTITION_FREE = 0xFF;
	//These are not valid Partition IDs, just used by this program
	public static final int PARTITION_TAPE_SINCLAIRMICRODRIVE = 0x70;
	public static final int PARTITION_TAPE_TAP = 0x71;
	public static final int PARTITION_TAPE_TZX = 0x72;
	public static final int PARTITION_RAWFDD = 0x73;

	/*
	 * Partition flags
	 */
	public static int PART_ALLOCATABLE = 1;
	public static int PART_PARTITION = 2;
	public static int PART_DISKIMAGE = 4;
	public static int PART_SPECIAL = 8;
	public static int PART_CPM = 16;

	/*
	 * Index object
	 */
	public static class PARTSTRING {
		public String Name;
		public byte PartID;
		public int flags;

		public PARTSTRING(int partID, String Name, int flags) {
			this.Name = Name;
			this.PartID = (byte) partID;
			this.flags = flags;
		}
	}

	/*
	 * List of partition types, names and flags.
	 */
	public static PARTSTRING[] PartTypes = {
			new PARTSTRING(PARTITION_UNUSED, "Unused", PART_SPECIAL | PART_ALLOCATABLE),
			new PARTSTRING(PARTITION_SYSTEM, "System", PART_SPECIAL),
			new PARTSTRING(PARTITION_SWAP, "Swap", PART_SPECIAL | PART_ALLOCATABLE),
			new PARTSTRING(PARTITION_PLUS3DOS, "+3 Dos", PART_ALLOCATABLE),
			new PARTSTRING(PARTITION_CPM, "CP/M", PART_SPECIAL),
			new PARTSTRING(PARTITION_BOOT, "Boot file", PART_SPECIAL),
			new PARTSTRING(PARTITION_MOVIE, "Movie", PART_SPECIAL),
			new PARTSTRING(PARTITION_FAT16, "FAT 16", PART_SPECIAL),
			new PARTSTRING(PARTITION_UZIX, "UZI(X)", PART_SPECIAL),
			new PARTSTRING(PARTITION_DISK_TRDOS, "TRDos Disk image", PART_SPECIAL | PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_PLUSD, "+D Disk image", PART_SPECIAL | PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_MB02, "MB-02 Disk image", PART_SPECIAL | PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_TOSA2, "TOS A.2 Disk image", PART_SPECIAL | PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_PLUS3, "+3 Disk image", PART_SPECIAL | PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_ELWRO800, "Elwo 800 Jr Disk image", PART_SPECIAL | PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_AMSTRADCPC, "Amstrad CPC disk image", PART_SPECIAL | PART_DISKIMAGE),
			new PARTSTRING(PARTITION_DISK_AMSTRADPCW, "Amstrad PCW disk image", PART_SPECIAL | PART_DISKIMAGE),
			new PARTSTRING(PARTITION_TAPE_SINCLAIRMICRODRIVE , "Sinclair Microdrive image", PART_SPECIAL ),
			new PARTSTRING(PARTITION_TAPE_TAP,"TAP file", PART_SPECIAL ),
			new PARTSTRING(PARTITION_TAPE_TZX,"TZX file", PART_SPECIAL ),
			new PARTSTRING(PARTITION_RAWFDD,"Microdrive data", PART_SPECIAL ),
			new PARTSTRING(PARTITION_BAD, "Bad disk space", PART_SPECIAL),
			new PARTSTRING(PARTITION_FREE, "Free disk space", PART_SPECIAL) 
	};

	
	/**
	 * Generate the generic bits of an IDEDOS partition (name, type, locations,
	 * sector location)
	 * 
	 * @param name
	 * @param type
	 * @param startCyl
	 * @param startHead
	 * @param endCyl
	 * @param endHead
	 * @param lastSector
	 * @return
	 */
	public static byte[] MakeGenericIDEDOSPartition(String name, int type, int startCyl, int startHead, int endCyl,
			int endHead, long lastSector) {
		byte Part[] = new byte[0x40];
		for (int i = 0; i < Part.length; i++) {
			Part[i] = 0x00;
		}

		// Partition name
		while (name.length() < 0x10)
			name = name + " ";
		byte NameStr[] = name.getBytes();
		System.arraycopy(NameStr, 0, Part, 0, 16);
		Part[0x10] = (byte) (type & 0xff);

		// Start CH
		Part[0x11] = (byte) (startCyl & 0xff); // \
		Part[0x12] = (byte) ((startCyl / 0x100) & 0xff);// /starting cyl=0
		Part[0x13] = (byte) (startHead & 0xff); // starting head

		// End Ch
		Part[0x14] = (byte) (endCyl & 0xff); // \
		Part[0x15] = (byte) ((endCyl / 0x100) & 0xff); // / End Cylinder
		Part[0x16] = (byte) (endHead & 0xff); // End head

		// Largest logical sector.
		Part[0x17] = (byte) (lastSector & 0xff);
		lastSector = lastSector / 0x100;
		Part[0x18] = (byte) (lastSector & 0xff);
		lastSector = lastSector / 0x100;
		Part[0x19] = (byte) (lastSector & 0xff);
		lastSector = lastSector / 0x100;
		Part[0x1A] = (byte) (lastSector & 0xff);
		return (Part);
	}

	/**
	 * Generate the system partition. The first partition entry in the partition
	 * table is always the IDEDOS system partition. There must only be one System
	 * Partition on a hard disk. The partition label must always be the string
	 * "PLUSIDEDOS" followed by six spaces (ascii 0x20) as this string is used to
	 * determine whether the disk was created using an 8 or 16-bit interface. The
	 * starting head and ending head are either zero or one depending on where the
	 * partition table has been written. The drive geometry is written in the type
	 * specific data in the bytes 0x0020 to 0x0027
	 * 
	 * @param cyl
	 * @param head
	 * @param spt
	 * @param sectorSz
	 * @param Write8Bit
	 * @return
	 */
	public static byte[] GetSystemPartition(int DiskCyl, int DiskHead, int DiskSPT, int sectorSz, boolean Write8Bit) {
		byte SysPart[] = MakeGenericIDEDOSPartition("PLUSIDEDOS", PLUSIDEDOS.PARTITION_SYSTEM, 0, 0, 0, 0, DiskSPT);
		/*
		 * System partition specific information
		 */
		// Disk parameters
		SysPart[0x20] = (byte) ((DiskCyl / 0x100) & 0xff);
		SysPart[0x21] = (byte) (DiskCyl & 0xff);
		SysPart[0x22] = (byte) (DiskHead & 0xff);
		SysPart[0x23] = (byte) (DiskSPT & 0xff);

		// Sectors per Cylinder (As opposed by Sectors per track)
		int SPC = DiskSPT * DiskHead;
		SysPart[0x24] = (byte) ((SPC / 0x100) & 0xff);
		SysPart[0x25] = (byte) (SPC & 0xff);

		// Max partitions - Limit to 31 partitions
		// GDS: bugfix. limit to 31 partition
		int MaxPartitions = (DiskSPT * sectorSz / 0x40) - 1;
		if (MaxPartitions > 31) {
			MaxPartitions = 31;
		}
		SysPart[0x26] = (byte) (MaxPartitions & 0xff);
		SysPart[0x27] = (byte) ((MaxPartitions / 0x100) & 0xff);

		// Colour attribute byte (Paper white, Ink black 0011 1000)
		SysPart[0x28] = (byte) 0x38;

		// Basic attribute byte (Paper white, Ink black 0011 1000)
		SysPart[0x29] = (byte) 0x38;

		// UA, UB, UM, M0 M1 MR, DD left at 0

		if (Write8Bit) {
			SysPart = DoubleSector(SysPart);
		}
		return (SysPart);
	}

	/**
	 * Get a free space partition Partition Type 0xFF - Free Disk Space A Type 0xFF
	 * partition entry as created by the +3e ROMs is all blank except for the type
	 * byte and the location and size information. The type specific information is
	 * not used.
	 * 
	 * @param DiskCyl
	 * @param DiskHead
	 * @param DiskSPT
	 * @param sectorSz
	 * @param Write8Bit
	 * @return
	 */
	public static byte[] GetFreeSpacePartition(int StartCyl, int StartHead, int EndCyl, int EndHead, int sectorSz,
			boolean Write8Bit, int DiskHeads, int DiskSPT) {
		int NumTracks = ((EndCyl - StartCyl) * DiskHeads) + EndHead - StartHead;
		long NumSectors = NumTracks * DiskSPT;

		byte FSPart[] = MakeGenericIDEDOSPartition("               ", PLUSIDEDOS.PARTITION_FREE, StartCyl, StartHead,
				EndCyl, EndHead, NumSectors);

		// Blank partition name
		for (int i = 0; i < 0x10; i++) {
			FSPart[i] = 0x00;
		}

		if (Write8Bit) {
			FSPart = DoubleSector(FSPart);
		}

		return (FSPart);
	}

	/**
	 * process the sector to expand it to 16 bits
	 * 
	 * @param Sector
	 * @return
	 */
	public static byte[] DoubleSector(byte Sector[]) {
		byte result[] = new byte[Sector.length * 2];

		int ptr = 0;
		for (int i = 0; i < Sector.length; i++) {
			result[ptr++] = Sector[i];
			result[ptr++] = 0;
		}
		return (result);
	}
	
	/**
	 * Convert the Partition type into a description.
	 * 
	 * @return
	 */
	public static String GetTypeAsString(int partType) {
		String result = "Invalid";
		for (PARTSTRING parttype: PartTypes) {
			if (parttype.PartID == partType) {
				result = parttype.Name;
			}
		}
		return (result);
	}
	
}

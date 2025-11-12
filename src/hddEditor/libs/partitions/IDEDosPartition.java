package hddEditor.libs.partitions;

import java.io.File;

/**
 * Base object for an IDEDOS partition.
 * Basic partition information common to all partition types:
 * 
 * IDEDOS Partition entry - 64 bytes
 * 0x0000	PN PN PN PN PN PN PN PN PN PN PN PN PN PN PN PN	
 * 0x0010	PT SC SC SH EC EC EH LS LS LS LS TD TD TD TD TD 
 * 0x0020	TD TD TD TD TD TD TD TD TD TD TD TD TD TD TD TD  
 * 0x0030	TD TD TD TD TD TD TD TD TD TD TD TD TD TD TD TD  	
 * 
 * Key:
 * PN: non case sensitive partition name. 16 byte un-terminated text string. Space padded.
 * PT: partition type.
 * SC: starting cylinder. 16 bit little endian word.
 * SH: starting head.
 * EC: ending cylinder. 16 bit little endian word.
 * EH: ending head.
 * LS: largest logical sector. 32 bit little endian word.
 * TD: partition type specific data.
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.widgets.Display;

import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.Languages;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.ModifiedEvent;
import hddEditor.ui.partitionPages.dialogs.AddressNote;

public class IDEDosPartition {
	public static final int PARTITION_TYPE_INDEX = 16;
	public static final int SORTTYPE_NAME = 1;
	public static final int SORTTYPE_TYPE = 2;
	public static final int SORTTYPE_SIZE = 3;

	public Disk CurrentDisk = null;
	public int DirentLocation = 0;
	public int DirentNum = -1;

	public byte RawPartition[] = null;

	public boolean CanExport = false;
	
	protected Languages lang;

	/**
	 * Update and set the Partition name
	 * 
	 * @return
	 */
	public String GetName() {
		if (GetPartType() == PLUSIDEDOS.PARTITION_FREE) {
			return ("<Free space>");
		} else {
			String name = new String(RawPartition, StandardCharsets.UTF_8).substring(0, 15).trim();

			return (name);
		}
	}

	public void SetName(String name) {
		// make sure the filename doesn't contain any dubious characters
		name = name.replace(" ", "_");
		// pad to at least 16 bytes
		name = name + "                ";
		// Copy the name to the raw partition
		byte bName[] = name.getBytes();
		for (int i = 0; i < 16; i++) {
			RawPartition[i] = bName[i];
		}
	}

	/**
	 * Update and set the Partition type flag.
	 * 
	 * @param PartType
	 */
	public void SetPartType(int PartType) {
		RawPartition[16] = (byte) (PartType & 0xff);
	}

	public int GetPartType() {
		return (RawPartition[16] & 0xff);
	}

	/**
	 * Update and set the start cylinder
	 */
	public void SetStartCyl(int startcyl) {
		int msb = startcyl / 0x100;
		int lsb = startcyl % 0x100;
		RawPartition[17] = (byte) lsb;
		RawPartition[18] = (byte) msb;
		UpdateEndSector();
	}

	public int GetStartCyl() {
		return ((int) (RawPartition[17] & 0xff) + ((RawPartition[18] & 0xff) * 256));
	}

	/**
	 * Update and set the start head
	 */
	public void SetStartHead(int startHead) {
		RawPartition[19] = (byte) startHead;
		UpdateEndSector();
	}

	public int GetStartHead() {
		return ((int) (RawPartition[19] & 0xff));
	}

	/**
	 * Update and set the end cylinder
	 * 
	 * @param EndCyl
	 */
	public void SetEndCyl(int EndCyl) {
		int msb = EndCyl / 0x100;
		int lsb = EndCyl % 0x100;
		RawPartition[20] = (byte) lsb;
		RawPartition[21] = (byte) msb;
		UpdateEndSector();
	}

	public int GetEndCyl() {
		return ((int) (RawPartition[20] & 0xff) + ((RawPartition[21] & 0xff) * 256));
	}

	/**
	 * Update and set the End head.
	 * 
	 * @param startHead
	 */
	public void SetEndHead(int startHead) {
		RawPartition[22] = (byte) startHead;
		UpdateEndSector();
	}

	public int GetEndHead() {
		return ((int) (RawPartition[22] & 0xff));
	}

	/**
	 * Update the end sector ID.
	 */
	public void UpdateEndSector() {
		byte systemdets[] = null;
		if (CurrentDisk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_HDD) {
			try {
				systemdets = CurrentDisk.GetBytesStartingFromSector(0, 512);
			} catch (IOException e) {
			}
		} else {
			systemdets = RawPartition;
		}
		int HeadsPerTrack = systemdets[0x22] & 0xff;
		int SectorsPerTrack = systemdets[0x23] & 0xff;

		int numCyls = GetEndCyl() - GetStartCyl();
		int numHeads = GetEndHead() - GetStartHead();
		if (numHeads < 0) {
			numHeads = numHeads + HeadsPerTrack;
			numCyls--;
		}
		int numtracks = numCyls * HeadsPerTrack;
		numtracks = numtracks + numHeads;

		long NumSectors = numtracks * SectorsPerTrack;
		// Hack for 8 bit IDE devices.
		if (CurrentDisk.GetSectorSize() == 256) {
			NumSectors = NumSectors / 2;
		}
		SetEndSector((long) NumSectors);
	}

	/**
	 * Get the End sector Note, this is in 512 byte sectors regardless of disk size.
	 * 
	 * @return
	 */
	public long GetEndSector() {
		// endsector is a 4 byte value
		long EndSector = (int) (RawPartition[25] & 0xff) + ((RawPartition[26] & 0xff) * 0x100);
		EndSector = EndSector * 0x10000;
		EndSector = EndSector + (int) (RawPartition[23] & 0xff) + ((RawPartition[24] & 0xff) * 0x100);
		return (EndSector);
	}

	/**
	 * Set the end sector Note this is in 512 byte sectors regardless of disk size
	 * 
	 * @param endsector
	 */
	public void SetEndSector(Long endsector) {
		int lsb = (int) (endsector % 0x10000);
		int msb = (int) (endsector / 0x10000);

		RawPartition[23] = (byte) (lsb % 0x100);
		RawPartition[24] = (byte) (lsb / 0x100);
		RawPartition[25] = (byte) (msb % 0x100);
		RawPartition[26] = (byte) (msb / 0x100);
	}

	/**
	 * get the 5 unused bytes normally at the end of the partition. These are mostly
	 * unused, but are used in SYSTEM partition.
	 * 
	 * @return
	 */
	public byte[] getUnused() {
		byte Unused[] = new byte[5];
		System.arraycopy(RawPartition, 27, Unused, 0, 5);
		return (Unused);

	}

	/**
	 * Set the 5 unused bytes
	 * 
	 * @param unused
	 */
	public void SetUnused(byte unused[]) {
		System.arraycopy(unused, 0, RawPartition, 27, 5);
	}

	/**
	 * Get the second half of the partition data used for partition specific
	 * information.
	 * 
	 * @return
	 */
	public byte[] getExtra() {
		byte extra[] = new byte[32];
		System.arraycopy(RawPartition, 0x20, extra, 0, 0x20);
		return (extra);
	}

	/**
	 * Set the second half of the partition data used for partition specific
	 * information.
	 * 
	 * @return
	 */
	public void SetExtra(byte extra[]) {
		System.arraycopy(extra, 0, RawPartition, 0x20, 0x20);
	}

	// ******************************************
	// Modify tracking and callbacks
	// ******************************************
	// Modify event callback
	public ModifiedEvent OnModify = null;

	// private storage as to the current state
	private boolean DiskModified = false;

	// get modified
	public boolean getModified() {
		return (DiskModified);
	}

	// Update modified.
	public void setModified(boolean Modified) {
		DiskModified = Modified;
		if (OnModify != null) {
			OnModify.ModifiedChanged();
		}
	}

	/**
	 * Get the size in Kbytes of the partition. Note, sectors are treated as 512
	 * bytes regardless of actual disk size.
	 * 
	 * @return
	 */
	public long GetSizeK() {
		return (GetEndSector() / 2);
	}

	/**
	 * Used when creating a partition. Overridden by specific partitions
	 */
	public void SetSensibleDefaults() {

	}

	/**
	 * constructor.
	 * 
	 * @param tag
	 * @param RawDisk
	 * @param RawPartition
	 */
	public IDEDosPartition(int DirentLocation, Disk RawDisk, byte RawPartition[], int DirentNum, boolean Initialise, Languages lang) {
		CanExport = false;
		CurrentDisk = RawDisk;
		this.DirentLocation = DirentLocation;
		this.DirentNum = DirentNum;
		this.RawPartition = RawPartition;
		this.lang = lang;
		PopulateData(RawPartition);
		if (Initialise) {
			SetSensibleDefaults();
		} else {
			try {
				LoadPartitionSpecificInformation();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Populate the common IDEDOS partition information.
	 * 
	 * @param RawPartition
	 */
	private void PopulateData(byte RawPartition[]) {
		this.RawPartition = RawPartition;
		if (RawPartition[16] == 0x00) {
			SetPartType(0);
			SetStartCyl(0);
			SetEndCyl(0);
			SetStartHead(0);
			SetEndHead(0);
			SetEndSector((long) 0);
		}
	}

	/**
	 * Output the basic IDEDOS informaton.
	 */
	@Override
	public String toString() {
		String result = GeneralUtils.PadTo(GetName(), 17);
		result = result + GeneralUtils.PadTo(PLUSIDEDOS.GetTypeAsString(GetPartType()), 7);
		result = result + String.format("%4d/%2d - %4d/%2d + %5d  ", GetStartCyl(), GetStartHead(), GetEndCyl(),
				GetEndHead(), GetEndSector());
		result = result + GeneralUtils.GetSizeAsString(GetSizeK() * 1024);

		return (result);
	}

	/**
	 * Return either a letter or nothing if b=0;
	 * 
	 * @param b
	 * @return
	 */
	public String letterOrBlank(byte b) {
		char c = (char) b;
		if (b != 0) {
			return ("" + (char) c);
		} else {
			return ("");
		}
	}

	/**
	 * 
	 * @param startsector
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public byte[] GetAllDataInPartition() throws IOException {
		long LogicalSectorStartOfPartition = GetStartCyl() * CurrentDisk.GetNumHeads() * (CurrentDisk.GetNumSectors());
		LogicalSectorStartOfPartition = LogicalSectorStartOfPartition + (GetStartHead() * CurrentDisk.GetNumSectors());

		long NumSectors = GetEndSector() * CurrentDisk.GetSectorSize();

		byte result[] = CurrentDisk.GetBytesStartingFromSector(LogicalSectorStartOfPartition, NumSectors);
		return (result);
	}

	/**
	 * 
	 * @param startsector
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public byte[] GetDataInPartition(long startsector, long length) throws IOException {
		int LogicalSectorStartOfPartition = GetStartCyl() * CurrentDisk.GetNumHeads() * (CurrentDisk.GetNumSectors());
		LogicalSectorStartOfPartition = LogicalSectorStartOfPartition + (GetStartHead() * CurrentDisk.GetNumSectors());

		long ActualLogicalSector = LogicalSectorStartOfPartition + startsector;

		byte result[] = CurrentDisk.GetBytesStartingFromSector(ActualLogicalSector, length);
		return (result);
	}

	public void SetAllDataInPartition(byte[] data) throws IOException {
		SetDataInPartition(0, data);
	}

	/**
	 * 
	 * @param startsector
	 * @param data
	 * @throws IOException
	 */
	public void SetDataInPartition(long startsector, byte data[]) throws IOException {
		int LogicalSectorStartOfPartition = GetStartCyl() * CurrentDisk.GetNumHeads() * (CurrentDisk.GetNumSectors());
		LogicalSectorStartOfPartition = LogicalSectorStartOfPartition + (GetStartHead() * CurrentDisk.GetNumSectors());

		long ActualLogicalSector = LogicalSectorStartOfPartition + startsector;

		CurrentDisk.SetLogicalBlockFromSector(ActualLogicalSector, data);
	}

	/**
	 * Get a list of notes associated with various addresses in this partition.
	 * 
	 * @return
	 */
	public AddressNote[] GetAddressNotes() {
		return (null);
	}

	/**
	 * Reload the partition details.
	 */
	public void Reload() {

	}

	/**
	 * Load the partition specific information. Should be overridden to parse out
	 * the information from the second part of the partition
	 */
	protected void LoadPartitionSpecificInformation() throws IOException {

	}

	/**
	 * Extract partition with flags showing what to do with each file type.
	 * 
	 * @param folder
	 * @param BasicAction
	 * @param CodeAction
	 * @param ArrayAction
	 * @param ScreenAction
	 * @param MiscAction
	 * @param progress
	 * @throws IOException
	 */
	public void ExtractPartitiontoFolderAdvanced(File folder, int BasicAction, int CodeAction, int ArrayAction,
			int ScreenAction, int MiscAction, int SwapAction, ProgressCallback progress, boolean IncludeDeleted, Display disp, int FontAction)
			throws IOException {
		System.out
				.println("Class " + this.getClass().getName() + " does not support ExtractPartitiontoFolderAdvanced.");
	}

	/**
	 * 
	 * @param wildcard
	 * @return
	 */
	public FileEntry[] GetFileList(String wildcard) {
		System.out.println("Class " + this.getClass().getName() + " does not support GetFileList.");
		return (null);
	}

	/**
	 * 
	 * @param wildcard
	 * @throws IOException
	 */
	public void DeleteFile(String wildcard) throws IOException {
		System.out.println("Class " + this.getClass().getName() + " does not support Deletefile.");
	}

	/**
	 * Rename a named file.
	 * 
	 * @param filename
	 * @param newName
	 * @throws IOException
	 */
	public void RenameFile(String filename, String newName) throws IOException {
		System.out.println("Class " + this.getClass().getName() + " does not support RenameFile.");
	}

	/**
	 * Save a passed in data with the given filename. as CODE
	 * 
	 * @param filename
	 * @param address
	 * @param data
	 */
	public void AddCodeFile(String filename, int address, byte[] data) throws IOException {
		System.out.println("Class " + this.getClass().getName() + " does not support AddRawCodeFile.");
	}

	/**
	 * 
	 * @param nameOnDisk
	 * @param basicAsBytes
	 * @param line
	 * @param basicoffset
	 * @throws IOException
	 */
	public void AddBasicFile(String nameOnDisk, byte[] basicAsBytes, int line, int basicoffset) throws IOException {
		System.out.println("Class " + this.getClass().getName() + " does not support AddBASICFile.");
	}

	/**
	 * Add an pre-encoded character array to the media
	 * 
	 * @param filename
	 * @param EncodedArray
	 * @param varname
	 * @return
	 * @throws IOException
	 */
	public void AddCharArray(String filename, byte[] EncodedArray, String varname) throws IOException {
		System.out.println("Class " + this.getClass().getName() + " does not support AddCharArray.");
	}

	/**
	 * Add an pre-encoded numeric array to the microdrive.
	 * 
	 * @param filename
	 * @param EncodedArray
	 * @param varname
	 * @return
	 * @throws IOException
	 */
	public void AddNumericArray(String filename, byte[] EncodedArray, String varname) throws IOException {
		System.out.println("Class " + this.getClass().getName() + " does not support AddNumericArray.");
	}

	/**
	 * Get all the files on this partition.
	 * 
	 * @return
	 */
	public FileEntry[] GetFileList() {
		return null;
	}

	/**
	 * Uniquify a filename if required. (IE, a disk)
	 * 
	 * @param filename
	 * @return
	 */
	public String UniqueifyFileNameIfRequired(String filename) {
		return filename;
	}

	/**
	 * Utility functions used for sorting a file listing. This makes more sense on
	 * disks rather than tape, as messing with the order of items on a tape may
	 * result in it being saved incorrectly.
	 */

	// Last sort type.
	private int lastsort = 0;
	// Whether the sort is reversed or not.
	private boolean sortreverse = false;

	/**
	 * Sort the current file list and return it.
	 * 
	 * @param SortType
	 * @return
	 */
	public FileEntry[] SortFileEntry(int SortType) {
		FileEntry fe[] = GetFileList();
		if (SortType == lastsort) {
			sortreverse = !sortreverse;
		}

		switch (SortType) {
		case SORTTYPE_NAME:
			Arrays.sort(fe, SortByName);
			break;
		case SORTTYPE_TYPE:
			Arrays.sort(fe, SortByExt);
			break;
		case SORTTYPE_SIZE:
			Arrays.sort(fe, SortBySize);
			break;
		}
		lastsort = SortType;
		return (fe);

	}

	/**
	 * Used to sort a fileentry list by its filename.
	 */
	public final Comparator<FileEntry> SortByName = new Comparator<FileEntry>() {
		@Override
		public int compare(FileEntry o1, FileEntry o2) {
			if (!sortreverse) {
				return o1.GetFilename().compareTo(o2.GetFilename());
			} else {
				return -o1.GetFilename().compareTo(o2.GetFilename());
			}
		}
	};

	/**
	 * Used to sort a fileentry list by its entry type.
	 */
	public final Comparator<FileEntry> SortByExt = new Comparator<FileEntry>() {
		@Override
		public int compare(FileEntry o1, FileEntry o2) {
			if (!sortreverse) {
				return o1.GetFileTypeString().compareTo(o2.GetFileTypeString());
			} else {
				return -o1.GetFileTypeString().compareTo(o2.GetFileTypeString());
			}
		}
	};

	/**
	 * Used to sort a fileentry list by its size.
	 */
	public final Comparator<FileEntry> SortBySize = new Comparator<FileEntry>() {
		@Override
		public int compare(FileEntry o1, FileEntry o2) {
			int result = 0;
			if (o1.GetFileSize() < o2.GetFileSize())
				result = -1;
			if (o1.GetFileSize() > o2.GetFileSize())
				result = 1;
			if (sortreverse) {
				result = -result;
			}
			return (result);
		}
	};

}

package hddEditor.libs.partitions;

import java.io.File;
import java.io.FileWriter;

/**
 * Handler for CPM partitions.
 * Note, this wont work by itself. it needs to have the param data
 * populated by any inheriting classes.
 */

import java.io.IOException;
import java.util.ArrayList;

import hddEditor.libs.CPM;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.partitions.cpm.DirectoryEntry;
import hddEditor.libs.partitions.cpm.Dirent;
import hddEditor.ui.partitionPages.dialogs.AddressNote;

public class CPMPartition extends IDEDosPartition {
	// This data needs to be populated by an inherited class.
	// It forms the CPM DPB for the disk.
	public int BlockSize;
	public int RecordsPerTrack;
	public int BlockSizeShift;
	public int BLM;
	public int ExtentMask;
	public int MaxBlock;
	public int MaxDirent;
	public int AL0;
	public int AL1;
	public int CheckSumVectorSize;
	public int ReservedTracks;
	public int SectorSizeShift;
	public int PHMSectorSize;
	public int Sidedness;
	public int TracksPerSide;
	public int SectorsPerTrack;
	public int FirstSector;
	public int SectorSize;
	public int GapLengthRW;
	public int GapLengthFmt;
	public int Flags;
	public Dirent[] Dirents;
	public DirectoryEntry[] DirectoryEntries;
	public boolean[] bam;

	// Calculated fields.
	public int DirectoryBlocks; // Number of blocks reserved for the directory
	public int usedblocks; // Blocks that are allocated already
	public int usedDirEnts; // Number of entries in the directory map that are used
	public int diskSize; // Calculated max disk space in Kbytes
	public int freeSpace; // Calculated free space in Kbytes
	public boolean IsValid; //

	/**
	 * Constuctor
	 * 
	 * @param tag
	 * @param RawDisk
	 * @param RawPartition
	 * @param DirentNum
	 * @param Initialise
	 */
	public CPMPartition(int tag, Disk RawDisk, byte[] RawPartition, int DirentNum, boolean Initialise) {
		super(tag, RawDisk, RawPartition, DirentNum, Initialise);
	}

	/**
	 * Get the logical CPM block relative to this partition.
	 * 
	 * @param Record
	 * @return
	 * @throws IOException
	 */
	public byte[] GetLogicalBlock(int BlockID) throws IOException {

		long LogicalSectorInPartition = (BlockID * BlockSize) / CurrentDisk.GetSectorSize();

		long LogicalSectorStartOfPartition = (GetStartCyl() + ReservedTracks) * CurrentDisk.GetNumHeads()
				* (CurrentDisk.GetNumSectors());
		LogicalSectorStartOfPartition = LogicalSectorStartOfPartition + (GetStartHead() * CurrentDisk.GetNumSectors());

		long ActualLogicalSector = LogicalSectorStartOfPartition + LogicalSectorInPartition;

		byte result[] = CurrentDisk.GetBytesStartingFromSector(ActualLogicalSector, BlockSize);

		return (result);
	}

	/**
	 * update the sector containing the BAM
	 * 
	 * @throws IOException
	 */
	public void updateDirentBlocks() throws IOException {
		// update the sectors.

		/*
		 * calculate the start of the Dirent.
		 */
		int bytesRequired = (MaxDirent + 1) * 32;
		byte rawDirents[] = new byte[bytesRequired];
		int bytesLoaded = 0;
		int blockId = 0;

		/*
		 * Load the dirent block
		 */
		while (bytesLoaded < bytesRequired) {
			byte block[] = GetLogicalBlock(blockId);
			System.arraycopy(block, 0, rawDirents, bytesLoaded, block.length);
			blockId++;
			bytesLoaded = bytesLoaded + block.length;
		}

		/*
		 * update the loaded block.
		 */
		int ptr = 0;
		for (Dirent d : Dirents) {
			System.arraycopy(d.rawdirent, 0, rawDirents, ptr, 0x20);
			ptr = ptr + 0x20;
		}

		/*
		 * save the dirent block
		 */
		int bytesSaved = 0;
		blockId = 0;
		byte block[] = new byte[BlockSize];
		while (bytesSaved < bytesRequired) {
			System.arraycopy(rawDirents, bytesSaved, block, 0, BlockSize);
			SetLogicalBlock(blockId, block);
			blockId++;
			bytesSaved = bytesSaved + BlockSize;
		}
	}

	/**
	 * Add a CPM file to the disk.
	 * 
	 * @param filename
	 * @param file
	 * @throws IOException
	 */
	public boolean AddCPMFile(String filename, byte[] file) throws IOException {
		// firstly check to see if the file exists. delete the old one if found.
		DirectoryEntry de = GetDirectoryEntry(filename);
		if (de != null) {
			de.SetDeleted(true);
			updateDirentBlocks();
			System.out.println("AddCPMFile: Deleted old version of " + filename);
		}
		try {
			System.out.println("AddCPMFile: Saving " + filename + " length:" + file.length);
			// do we have enough space on the disk?
			if (file.length > freeSpace * 1024) {
				System.out.println("insufficient free space on disk. (" + String.valueOf(freeSpace * 1024)
						+ " bytes free, " + file.length + " bytes required)");
				return (false);
			}
			// Work out how many blocks the file will need
			int requiredblocks = Math.floorDiv(file.length, BlockSize);
			if (file.length % BlockSize > 0) {
				requiredblocks++;
			}
//			System.out.println("Blocks required: " + requiredblocks);

			// Work out how many DIRENTS this will need
			int blocksPerDirent = 8;
			if (MaxBlock < 0xff) {
				blocksPerDirent = 16;
			}
			int direntsRequired = Math.floorDiv(requiredblocks, blocksPerDirent);
			if (requiredblocks % blocksPerDirent > 0) {
				direntsRequired++;
			}
//			System.out.println("Dirents required: " + direntsRequired);

			// do we have enough dirents?
			int freedirents = MaxDirent - usedDirEnts;
			if (direntsRequired > freedirents) {
				System.out.println("insufficient free directory entries. (" + String.valueOf(direntsRequired)
						+ " entries required, " + freedirents + " entries free)");
				return (false);
			}

			// Get a list of free blocks
			ArrayList<Integer> FreeBlocks = new ArrayList<Integer>();
			for (int i = DirectoryBlocks; i < bam.length; i++) {
				if (!bam[i]) {
					FreeBlocks.add(i);
				}
			}
//			System.out.println("Free blocks: " + FreeBlocks.size());

			// write the blocks in order and allocate BAM entries.
			ArrayList<Integer> NewBlocks = new ArrayList<Integer>();
			byte blk[] = new byte[BlockSize];
			int ptr = 0;
			for (int BlockNum = 0; BlockNum < requiredblocks; BlockNum++) {
				int block = FreeBlocks.get(BlockNum);
				bam[block] = true;
				NewBlocks.add(block);
				int size = file.length - ptr;
				int DataSize = size;
				if (size > BlockSize) {
					DataSize = BlockSize;
				} else {
					for (int i = DataSize; i < BlockSize; i++) {
						blk[i] = (byte) 0xe5;
					}
				}
				System.arraycopy(file, ptr, blk, 0, DataSize);
				SetLogicalBlock(block, blk);
				ptr = ptr + BlockSize;
			}

			// Get a list of free dirents
			ArrayList<Integer> FreeDirents = new ArrayList<Integer>();
			for (int i = 0; i < Dirents.length; i++) {
				int typ = Dirents[i].getType();
				//System.out.println(typ);
				if (typ == Dirent.DIRENT_DELETED || typ == Dirent.DIRENT_UNUSED) {
					FreeDirents.add(i);
				}
			}
			//System.out.println("Free Dirents: " + FreeDirents.size());

			// seperate the filename
			filename = filename.toUpperCase();
			String prefix = "   ";
			if (filename.indexOf('.') > -1) {
				prefix = filename.substring(filename.indexOf('.') + 1) + "   ";
				filename = filename.substring(0, filename.indexOf('.')) + "        ";
			} else {
				filename = filename + "        ";
			}

			// write the dirents
			int block = 0;
			for (int direntNum = 0; direntNum < direntsRequired; direntNum++) {
				int allocatedDirent = FreeDirents.get(direntNum);
				Dirent d = Dirents[allocatedDirent];

				// allocated file
				d.rawdirent[0] = 0;

				// filenames
				for (int j = 0; j < 8; j++) {
					d.rawdirent[j + 1] = (byte) filename.charAt(j);
				}
				for (int j = 0; j < 3; j++) {
					d.rawdirent[j + 9] = (byte) prefix.charAt(j);
				}

				// extent lower 4 bits
//				d.rawdirent[0x0c] = (byte) (direntNum % 0x0f);

				// number of bytes used in the last dirent with 0=128
				d.rawdirent[0x0d] = (byte) (file.length & 0x7f);

				// Lower 5 bits are the upper bits of the extent number
				d.rawdirent[0x0e] = (byte) (direntNum / 0x10);

				// Number of 128 byte records used in the last logical extent. All previous
				// extents are considered to be full.
				int bytesPerDirent = blocksPerDirent * BlockSize;
				int LastDirentBytes = file.length % bytesPerDirent; // extract the bytes in the last dirent.
				int recordsInLastDirent = LastDirentBytes / 0x80;
				if (file.length % 0x80 != 0) {
					recordsInLastDirent++;
				}
				// Updated Dirent for larger files.
				int cdata = (recordsInLastDirent / 128) & ExtentMask;
				d.rawdirent[0x0c] = (byte) (cdata | (direntNum % 0x0f));
				d.rawdirent[0x0f] = (byte) (recordsInLastDirent % 128);

				// copy block numbers to the DIRENTS
				for (int j = 0; j < blocksPerDirent; j++) {
					int blocknum = 0;
					if (block < NewBlocks.size()) {
						blocknum = NewBlocks.get(block++);
					}
					if (blocksPerDirent == 16) {
						d.rawdirent[j + 0x10] = (byte) (blocknum & 0xff);
					} else {
						int index = (j * 2) + 0x10;
						d.rawdirent[index] = (byte) (blocknum & 0xff);
						d.rawdirent[index + 1] = (byte) (blocknum / 0x100);
					}
				}
				int blocks[] = d.getBlocks();
				String s = "";
				for (int bnum : blocks) {
					s = s + ", ";
					s = s + bnum;
				}
//				System.out.println("SaveCPMFile: Dirent: " + d.entrynum + " <- " + d.GetLogicalExtentNum() + " Blocks: "
//						+ s.substring(2));

			}
			// update sectors containing the dirents
			updateDirentBlocks();

			// Add a directory entry.
			ExtractDirectoryListing();

			// set modified
			setModified(true);

			// update free space markers
			usedDirEnts = usedDirEnts + direntsRequired;
			usedblocks = usedblocks + requiredblocks;
			freeSpace = (MaxBlock - usedblocks) * BlockSize / 1024;
			return (true);
		} catch (Exception E) {
			E.printStackTrace();
			return (false);
		}
	}

	/**
	 * Set a logical block within the Partition
	 * 
	 * @param Record
	 * @param Block
	 * @throws IOException
	 */
	public void SetLogicalBlock(int BlockID, byte[] Block) throws IOException {
		long LogicalSectorInPartition = (BlockID * BlockSize) / CurrentDisk.GetSectorSize();

		long LogicalSectorStartOfPartition = (GetStartCyl() + ReservedTracks) * CurrentDisk.GetNumHeads()
				* (CurrentDisk.GetNumSectors());
		LogicalSectorStartOfPartition = LogicalSectorStartOfPartition + (GetStartHead() * CurrentDisk.GetNumSectors());

		long ActualLogicalSector = LogicalSectorStartOfPartition + LogicalSectorInPartition;

		/*
		 * System.out.println("SetLogicalBlock: " + BlockID + " SC:" + GetStartCyl() +
		 * " SH:" + GetStartHead() + " RealSector:" + ActualLogicalSector + " Log:" +
		 * LogicalSectorInPartition + " Startsect:" + LogicalSectorStartOfPartition);
		 */

		CurrentDisk.SetLogicalBlockFromSector(ActualLogicalSector, Block);

	}

	/**
	 * Check to see if a file exists and return its directory entry if found
	 * 
	 * @param filename
	 * @return
	 */
	public DirectoryEntry GetDirectoryEntry(String filename) {
		DirectoryEntry result = null;
		if (filename.endsWith(".")) {
			filename = filename.substring(0, filename.length() - 1);
		}
		for (DirectoryEntry d : DirectoryEntries) {
			if (d.GetFilename().contentEquals(filename)) {
				result = d;
			}
		}

		return (result);
	}

	/**
	 * Load the DIRENTS and parse out the directory entries.
	 * 
	 * @throws IOException
	 */
	public void ExtractDirectoryListing() throws IOException {
		// Load the raw Dirents into a byte block.
		int bytesRequired = (MaxDirent + 1) * 32;
		byte rawDirents[] = new byte[bytesRequired];
		int bytesLoaded = 0;
		int blockId = 0;
		while (bytesLoaded < bytesRequired) {
			byte block[] = GetLogicalBlock(blockId);
			if (block.length > 0) {
				System.arraycopy(block, 0, rawDirents, bytesLoaded,
						Math.min(block.length, bytesRequired - bytesLoaded));
				blockId++;
				bytesLoaded = bytesLoaded + block.length;
			} else {
				bytesLoaded = bytesRequired;
			}
		}
		Dirents = new Dirent[MaxDirent + 1];
		for (int i = 0; i <= MaxDirent; i++) {
			Dirent d = new Dirent(i, ExtentMask);
			d.Is16BitBlockID = (MaxBlock > 254);
			d.LoadDirentFromArray(rawDirents, i * 32);
			Dirents[i] = d;
		}
		RecalculateDirectoryListing();
		int validdirents = 0;
		int invalidDirents = 0;
		for (DirectoryEntry d : DirectoryEntries) {
			boolean valid = d.IsComplete();
			if (d.dirents[0].GetUserNumber() > 16 && d.dirents[0].GetUserNumber() != 0xe5) {
				valid = false;
			}
			if (valid) {
				String s = d.GetFilename();
				for (int i = 0; i < s.length(); i++) {
					char c = s.charAt(i);
					if (!CPM.CharIsCPMValid(c) && (c != ' ') && (c != 0xe5)) {
						valid = false;
					}
				}
			}
			if (valid) {
				validdirents++;
			} else {
				invalidDirents++;
			}
		}
		IsValid = ((validdirents > 1) && (invalidDirents < validdirents)) || ((invalidDirents == 0));
	}

	/**
	 * This function is called to re-calculate the directory listing from the disks
	 * DIRENTS. It is used after the disk is loaded or modified.
	 */
	public void RecalculateDirectoryListing() {
		IsValid = true;
		bam = new boolean[MaxBlock];
		// Convert the DIRENTS into a directory listing.
		DirectoryEntry direntries[] = new DirectoryEntry[Dirents.length]; // number of directory entries cannot be more
		// than the DirEnts

		usedDirEnts = 0;
		int nextdirentry = 0;
		for (Dirent dirent : Dirents) {
			int dType = dirent.getType();
			// only care about files
			if (dType == Dirent.DIRENT_FILE || dType == Dirent.DIRENT_DELETED) {
				if (dType == Dirent.DIRENT_FILE)
					usedDirEnts++;
				// do we have a file called that already?
				DirectoryEntry file = null;
				int Directorynum = nextdirentry;
				for (int j = 0; j < nextdirentry; j++) {
					// if we have found the file, record where it is.
					if (direntries[j].GetFilename().equals(dirent.GetFilename())) {
						file = direntries[j];
						Directorynum = j;
					}
				}
				// If we have not found a file, create a new one.
				if (file == null) {
					file = new DirectoryEntry(this, (dType == Dirent.DIRENT_DELETED), MaxBlock);
					Directorynum = nextdirentry++;
				}
				file.addDirent(dirent);
				direntries[Directorynum] = file;
			}
		}
		// now transfer the array to the object
		DirectoryEntries = new DirectoryEntry[nextdirentry];
		for (int i = 0; i < nextdirentry; i++) {
			DirectoryEntries[i] = direntries[i];
		}

		usedblocks = 0;
		// Recalculate the BAM.
		for (int i = 0; i < bam.length; i++) {
			bam[i] = false;
		}

		// Add in the blocks reserved for the directory (Always start at 0)
		for (int i = 0; i < DirectoryBlocks; i++) {
			bam[i] = true;
			usedblocks++;
		}
		// Fill in the dirents
		for (Dirent d : Dirents) {
			if (d.getType() == Dirent.DIRENT_FILE) {
				int blocks[] = d.getBlocks();
				for (int i : blocks) {
					if (i < bam.length) {
						bam[i] = true;
						usedblocks++;
					}
				}
			}
		}
		// Next, the free space.
		int Freeblocks = MaxBlock - usedblocks;
		freeSpace = (Freeblocks * BlockSize) / 1024;
	}

	/**
	 * ToString overridden to provide CPM specific information
	 * 
	 * @throws IOException
	 */
	public String toString() {
		String result = super.toString() + " ";

		result = result + "\n      \t\tRecords per track: " + RecordsPerTrack;
		result = result + "\tBlock Shift: " + BlockSizeShift;
		result = result + "\t\t\tBlock size: " + BlockSize;
		result = result + "\n  BLM: " + BLM;
		result = result + "\t\t\tExtent mask: " + ExtentMask;
		result = result + "\t\tLast Block #: " + MaxBlock;
		result = result + "\t\tLast Dirent #: " + MaxDirent;
		result = result + "\n  AL0: " + AL0;
		result = result + "\t\t\tAL1: " + AL1;
		result = result + "\t\t\tChecksum vector size: " + CheckSumVectorSize;
		result = result + "\tReserved Tracks: " + ReservedTracks;
		result = result + "\n  Sector Size Shift: " + SectorSizeShift;
		result = result + "\t\tPHMSectorSize: " + PHMSectorSize;
		result = result + "\tSidedness: " + Sidedness;
		result = result + "\t\t\tTracks Per Side: " + TracksPerSide;
		result = result + "\n  Sectors Per Track: " + SectorsPerTrack;
		result = result + "\tFirst Sector: " + FirstSector;
		result = result + "\t\tSectorSize: " + SectorSize + " bytes";
		result = result + "\t\tGap Length RW: " + GapLengthRW;
		result = result + "\n  Gap Length Fmt: " + GapLengthFmt;
		result = result + "\t\tFlags: " + Flags;
		result = result + "\t\tDisk size: " + diskSize + "k";

		result = result + "\n  Used blocks: " + usedblocks;
		result = result + "\t\tUsed Dirents: " + usedDirEnts;
		result = result + "\t\tFree space: " + freeSpace + "k";

		result = result + "\nNon-blank Dirents:\n";
		System.out.println(result);
		if (Dirents == null) {
			System.out.println("NULL DIRENTS!");
		}
		for (Dirent d : Dirents) {
			if (d.getType() == Dirent.DIRENT_FILE)
				result = result + d.toString() + "\n";
		}
		result = result + "\nDirectory entries:\n";
		for (DirectoryEntry de : DirectoryEntries) {
			if (!de.IsDeleted) {
				String fn = de.GetFilename();
				while (fn.length() < 15) {
					fn = fn + " ";
				}
				result = result + fn;
				result = result + "\tCPMLen:" + de.GetFileSize() + "\tDeleted?:" + de.IsDeleted + "\n";
			}
		}

		return (result);
	}

	/**
	 * This used to blank all the dirents to their default state.
	 * 
	 * @throws IOException
	 */
	public void CreateBlankDirectoryArea() throws IOException {
		int DirentSize = 16384; // 2 blocks
		// Read the entire DIRENT
		byte data[] = GetDataInPartition(0, DirentSize);
		// update the data with this partition
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) (0xe5 & 0xff);
		}
		// write it back.
		SetDataInPartition(0, data);
		IsValid = true;
	}

	/**
	 * Get the array of Address/Notes as used by the search function to identify
	 * what a particular address contains
	 * 
	 * @return
	 */
	@Override
	public AddressNote[] GetAddressNotes() {
		ArrayList<AddressNote> resultlist = new ArrayList<AddressNote>();

		int DirBlockSize = DirectoryBlocks * BlockSize;
		AddressNote DirBlocks = new AddressNote(0, DirBlockSize - 1, 0, "Directory blocks");
		resultlist.add(DirBlocks);

		AddressNote an = new AddressNote(DirBlockSize, MaxBlock * BlockSize, 0, "Unallocated space");
		resultlist.add(an);

		for (DirectoryEntry di : DirectoryEntries) {
			int[] blocks = di.getBlocks();
			for (int i = 0; i < blocks.length; i++) {
				an = new AddressNote(blocks[i] * BlockSize, ((blocks[i] + 1) * BlockSize) - 1, i * BlockSize,
						"File: " + di.GetFilename());
				resultlist.add(an);
			}
		}

		AddressNote result[] = resultlist.toArray(new AddressNote[0]);
		return (result);
	}

	/**
	 * Reload the partition details.
	 */
	@Override
	public void Reload() {
		try {
			ExtractDirectoryListing();
		} catch (IOException E) {
		}
	}

	/**
	 * Load the partition specific information
	 */
	@Override
	protected void LoadPartitionSpecificInformation() throws IOException {
		IsValid = true;
		super.LoadPartitionSpecificInformation();
		ExtractDirectoryListing();
	}

	@Override
	public void ExtractPartitiontoFolderAdvanced(File folder, int BasicAction, int CodeAction, int ArrayAction,
			int ScreenAction, int MiscAction, int SwapAction, ProgressCallback progress, boolean IncludeDeleted)
			throws IOException {
		try {
			FileWriter SysConfig = new FileWriter(new File(folder, "partition.index"));
			try {
				SysConfig.write("<speccy>\n".toCharArray());
				int entrynum = 0;
				for (DirectoryEntry entry : DirectoryEntries) {
					if (progress != null) {
						if (progress.Callback(DirectoryEntries.length, entrynum++, "File: " + entry.GetFilename())) {
							break;
						}
					}
					if (!entry.IsDeleted || IncludeDeleted) {
						File TargetFilename = new File(folder, entry.GetFilename().trim());
						byte file[] = entry.GetFileData();
						Speccy.SaveFileToDiskAdvanced(TargetFilename, file, file, file.length, 3, 0, 0, 0, "A",
								MiscAction);

						SysConfig.write("<file>\n".toCharArray());
						SysConfig.write(("  <filename>" + entry.GetFilename() + "</filename>\n").toCharArray());
						SysConfig.write(("  <deleted>" + entry.IsDeleted + "</deleted>\n").toCharArray());
						SysConfig.write(("  <errors>" + entry.Errors + "</errors>\n").toCharArray());
						SysConfig.write(("  <origfiletype>CPM</origfiletype>\n").toCharArray());
						SysConfig.write("   <cpm:blocks>\n".toCharArray());
						int blocks[] = entry.getBlocks();
						for (int i : blocks) {
							SysConfig.write(("       <blockID>" + i + "</blockID>\n").toCharArray());
						}
						SysConfig.write("   </cpm:blocks>\n".toCharArray());
						SysConfig.write("   <cpm:dirents>\n".toCharArray());
						for (Dirent d : entry.dirents) {
							SysConfig.write(("       <dirent>" + d.toString() + "</dirent>\n").toCharArray());
						}
						SysConfig.write("   </cpm:dirents>\n".toCharArray());
						SysConfig.write("</file>\n".toCharArray());
					}
				}

				SysConfig.write("</speccy>\n".toCharArray());
			} finally {
				SysConfig.close();
			}
		} catch (IOException e) {
			System.out.println("Error extracting files: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	@Override
	public FileEntry[] GetFileList(String wildcard) {
		// no wildcard = all files.
		if (wildcard.isEmpty()) {
			wildcard = "*.*";
		}
		ArrayList<FileEntry> results = new ArrayList<FileEntry>();
		for (FileEntry de : DirectoryEntries) {
			if (de.DoesMatch(wildcard)) {
				results.add(de);
			}
		}
		return (results.toArray(new FileEntry[0]));
	}

	@Override
	public void DeleteFile(String wildcard) throws IOException {
		int numdeleted = 0;
		for (DirectoryEntry de : DirectoryEntries) {
			if (de.DoesMatch(wildcard)) {
				de.SetDeleted(true);
				numdeleted++;
			}
		}
		RecalculateDirectoryListing();
		System.out.println("Deleted " + numdeleted + " files.");
	}

	/**
	 * Rename a named file.
	 * 
	 * @param filename
	 * @param newName
	 * @throws IOException
	 */
	@Override
	public void RenameFile(String filename, String newName) throws IOException {
		DirectoryEntry de = GetDirectoryEntry(filename);
		if (de != null) {
			try {
				de.SetFilename(newName);
			} catch (IOException e) {
				System.out.println("Error Renaming file '" + filename + "'. " + e.getMessage());
			}
		} else {
			System.out.println("Rename: File '" + filename + "' not found.");
		}
	}

	/**
	 * Get all the files on this partition.
	 * 
	 * @return
	 */
	@Override	
	public FileEntry[] GetFileList() {
		return DirectoryEntries;
	}
	
	
	/**
	 * Uniquify a filename if required. (IE, a disk)
	 * @param filename
	 * @return
	 */
	@Override
	public String UniqueifyFileNameIfRequired(String filename) {
		String prefix = CPM.FixFullName(filename);
		if (prefix.indexOf(".") > -1) {
			prefix = prefix.substring(0,prefix.indexOf("."));
		}
		
		int index=1;
		FileEntry existingEntries[] = GetFileList(filename);
		while (existingEntries.length > 0) {
			filename = prefix+"."+String.format("%03d", index++);
			existingEntries = GetFileList(filename);			
		}
		return filename;
	}
	
}

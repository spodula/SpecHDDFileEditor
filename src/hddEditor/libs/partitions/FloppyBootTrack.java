package hddEditor.libs.partitions;
/**
 * Implementation of a partition representing the boot track
 * for a CPM or CPM-like disk. Particularly the Amstrad variant.
 */

//TODO: implement export for floppy boot track

import java.io.File;
import java.io.IOException;

import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.FDD.FloppyDisk;
import hddEditor.libs.disks.FDD.Sector;
import hddEditor.libs.disks.FDD.TrackInfo;

public class FloppyBootTrack extends IDEDosPartition {
	public int disktype = 0; // 0=SS SD 3= DSDD
	public int numsectors = 0; // Sectors per track (9)
	public int sectorPow = 0; // Sector size represented by its (power of 2)+7, (usually 2 meaning 512 bytes)
	public int sectorSize = 0; // Calculated sector size from above (512)
	public int reservedTracks = 0; // Reserved tracks (Usually 1)
	public int blockPow = 0; // Block size represented by its (power of 2)+7, (usually 3 meaning 1024 bytes)
	public int blockSize = 0; // Calculated Block size from above (1024)
	public int dirBlocks = 0; // reserved blocks for the directory entries (usually 2)
	public int rwGapLength = 0; // read/write gap length
	public int fmtGapLength = 0; // Format gap length
	public int fiddleByte = 0; // Fiddle byte used to make the checksum match
	public int checksum = 0; // calculated checksum. if this make the checksum add up to 3, its a bootable +3
								// disk
	public String diskformat = "Unknown";

	// Calculated fields.
	public int maxblocks = 0; // Max number of blocks on the disk. (Minus the reserved tracks)
	public int reservedblocks = 0; // Blocks reserved for the Directory (Usually 2)
	public int maxDirEnts = 0; // Max number of entries in the directory
	public int diskSize = 0; // Calculated max disk space in Kbytes
	public int BlockIDWidth = 1; // If a disk has > 256 blocks, DIRENTS are 2 bytes rather than 1.
	
	public String Identifiedby="";
	
	public boolean IsValidCPMFileStructure = true;
	
	public FloppyBootTrack(int DirentLocation, Disk RawDisk, byte[] RawPartition, int DirentNum, boolean Initialise) {
		super(DirentLocation, RawDisk, RawPartition, DirentNum, Initialise);
		SetName("Floppy disk boot track.");
		GetXDPBDetails();
		CanExport = false;
	}	
	
	public void GetXDPBDetails() {
		int reservedtracks=1;
		
		FloppyDisk fdd = (FloppyDisk) CurrentDisk;
		try {
			byte BootSect[] = fdd.GetBytesStartingFromSector(0, (fdd.GetSectorSize() * fdd.diskTracks[0].Sectors.length) * reservedtracks);
			TrackInfo Track0 = fdd.diskTracks[0];
			
			if (Track0.minsectorID == 1) { // first sector=1 PCW/+3
				// if we have an invalid bootsector fiddle the data
				// fix GDS 22 Dec 2021 - Valid values for byte 1 are 0-3 only. This makes the
				// Khobrasoft SP7 disk load (For some reason passed with b0 in sector 1)
				if ((BootSect[0] & 0xff) < 4) {
					// bootsector is valid so use that.
					disktype = BootSect[0] & 0xff;
					numsectors = BootSect[3] & 0xff;
					sectorPow = BootSect[4] & 0xff;
					sectorSize = 128 << sectorPow;
					reservedTracks = BootSect[5] & 0xff;
					blockPow = BootSect[6] & 0xff;
					blockSize = 128 << blockPow;
					dirBlocks = BootSect[7] & 0xff;
					rwGapLength = BootSect[8] & 0xff;
					fmtGapLength = BootSect[9] & 0xff;
					fiddleByte = (int) (BootSect[15] & 0xff);
					Identifiedby = "Amstrad Boot Data in bootsector";
				} else {
					// Get physical values from the AMS disk wrapper and the count of sectors we
					// actually loaded.
					// For the rest, assume we are +3 disk and use the defaults.
					disktype = 0;
					numsectors = Track0.Sectors.length;
					sectorPow = 2;
					sectorSize = 512;
					reservedTracks = 1;
					blockPow = 3;
					blockSize = 1024;
					dirBlocks = 2;
					rwGapLength = 42;
					fmtGapLength = 82;
					fiddleByte = 0;
					Identifiedby = "Format values (Amstrad default #0)";
				}
				diskformat = "PCW/+3";
			} else if (Track0.minsectorID == 0x41) { // CPC system disk
				diskformat = "CPC System";
				disktype = 0;
				numsectors = Track0.Sectors.length;
				sectorPow = 2;
				sectorSize = 512;
				reservedTracks = 2;
				blockPow = 3;
				blockSize = 1024;
				dirBlocks = 2;
				rwGapLength = 0x2a;
				fmtGapLength = 0x52;
				fiddleByte = 0;
				Identifiedby = "Format values (Amstrad default #1)";
			} else if (Track0.minsectorID == 0xC1) { // CPC data disk. (No boot track)
				diskformat = "CPC Data";
				disktype = 0;
				numsectors = Track0.Sectors.length;
				sectorPow = 2;
				sectorSize = 512;
				reservedTracks = 0;
				blockPow = 3;
				blockSize = 1024;
				dirBlocks = 2;
				rwGapLength = 0x2a;
				fmtGapLength = 0x52;
				fiddleByte = 0;
				Identifiedby = "Format values (Amstrad default #2)";
			}
			
			// calculate the checksum
			checksum = 0;
			for (int i = 0; i < BootSect.length; i++) {
				int b = (int) BootSect[i] & 0xff;
				checksum = (int) (checksum + b) & 0xff;
			}
			
			
			IsValidCPMFileStructure = true;
			// +3 disk sectors are always 512. If they are not, something funky is
			// happening so don't try to parse directory entries. So check first 10 or so
			// tracks (To avoid any copy protection on higher tracks)
			// GDS 7 Feb 2022 - Fix for DOuble dragon Side 2 which only has 11 sectors in
			// the file.
			// GDS 7 Feb 2022 - Fixed bubble bobble image where blank tracks are not on the
			// disk.
			// GDS 8 Feb 2022 - Fixed KSFT SP7 image where track 0 contains protection. Should start from valid tracks
			for (int tracknum = reservedTracks; tracknum < Math.min(20, fdd.diskTracks.length); tracknum++) {
				TrackInfo tr = fdd.diskTracks[tracknum];
				if (tr != null) {
					for (Sector s : tr.Sectors) {
						if (s.Sectorsz != 2) {
							IsValidCPMFileStructure = false;
						}
					}
				}
			}
			
			maxblocks = ( fdd.diskTracks.length - reservedTracks) * fdd.NumHeads * numsectors * sectorSize / blockSize;
			reservedblocks = dirBlocks;
			maxDirEnts = dirBlocks * blockSize / 32;
			BlockIDWidth = 1;
			if (maxblocks > 255) {
				BlockIDWidth = 2;
			}
			diskSize = blockSize * (maxblocks - reservedblocks) / 1024;
			
		} catch (IOException e) {
			System.out.println("FloppyBootTrack: cannot load first track...");
		}
	}

	/**
	 * Get all the files on this partition.
	 * 
	 * @return
	 */
	@Override	
	public FileEntry[] GetFileList() {
		return null;
	}

	@Override
	public void ExtractPartitiontoFolderAdvanced(File folder, int BasicAction, int CodeAction, int ArrayAction,
			int ScreenAction, int MiscAction, int SwapAction, ProgressCallback progress, boolean IncludeDeleted)
			throws IOException {
	}
	
	
}

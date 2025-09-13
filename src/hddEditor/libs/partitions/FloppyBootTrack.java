package hddEditor.libs.partitions;

/**
 * Implementation of a partition representing the boot track
 * for a CPM or CPM-like disk. Particularly the Amstrad variant.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import hddEditor.libs.ASMLib;
import hddEditor.libs.Speccy;
import hddEditor.libs.ASMLib.DecodedASM;
import hddEditor.libs.CPM;
import hddEditor.libs.Languages;
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

	public String Identifiedby = "";

	public boolean IsValidCPMFileStructure = true;

	public FloppyBootTrack(int DirentLocation, Disk RawDisk, byte[] RawPartition, int DirentNum, boolean Initialise,
			Languages lang) {
		super(DirentLocation, RawDisk, RawPartition, DirentNum, Initialise, lang);
		SetName("Floppy disk boot track.");
		GetXDPBDetails();
		CanExport = true;
	}

	public void GetXDPBDetails() {
		int reservedtracks = 1;

		FloppyDisk fdd = (FloppyDisk) CurrentDisk;
		try {
			byte BootSect[] = fdd.GetBytesStartingFromSector(0,
					(fdd.GetSectorSize() * fdd.diskTracks[0].Sectors.length) * reservedtracks);
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
			// GDS 8 Feb 2022 - Fixed KSFT SP7 image where track 0 contains protection.
			// Should start from valid tracks
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

			maxblocks = (fdd.diskTracks.length - reservedTracks) * fdd.NumHeads * numsectors * sectorSize / blockSize;
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

	private void wl(FileWriter f, String s) throws IOException {
		f.write(s);
		f.write(System.lineSeparator());
	}

	@Override
	public void ExtractPartitiontoFolderAdvanced(File folder, int BasicAction, int CodeAction, int ArrayAction,
			int ScreenAction, int MiscAction, int SwapAction, ProgressCallback progress, boolean IncludeDeleted)
			throws IOException {

		try {
			FileWriter SysConfig = new FileWriter(new File(folder, "boot.info"));
			if (progress != null) {
				progress.Callback(1, 0, "Floppy boot track...");
			}
			byte data[] = GetAllDataInPartition();
			FloppyDisk Disk = (FloppyDisk) CurrentDisk;
			try {

				String ChecksumStatus = "Not bootable";
				int cs = (checksum + fiddleByte) & 0xff;
				if (cs == 3) {
					ChecksumStatus = "Bootable +3 disk";
				} else if (cs == 1) {
					ChecksumStatus = "Bootable PCW9512 disk";
				} else if (cs == 255) {
					ChecksumStatus = "Bootable PCW8256 disk";
				}

				wl(SysConfig, "Format: " + diskformat + " (" + disktype + ")");
				wl(SysConfig, "Sectors: " + numsectors);
				wl(SysConfig, "Sector size: " + sectorSize + " (" + sectorPow + ")");
				wl(SysConfig, "Reserved Tracks: " + reservedTracks);

				wl(SysConfig, "Block size: " + blockSize + " (" + blockPow + ")");
				wl(SysConfig, "Directory blocks: " + dirBlocks);
				wl(SysConfig, "R/W Gap: " + rwGapLength);
				wl(SysConfig, "Format Gap: " + fmtGapLength);

				wl(SysConfig, "Checksum+fb: " + cs);
				wl(SysConfig, "Bootable?: " + ChecksumStatus);
				wl(SysConfig, "Max CPM Blocks: " + maxblocks);
				wl(SysConfig, "Reserved blocks: " + reservedblocks);

				wl(SysConfig, "Max Dirents: " + maxDirEnts);
				wl(SysConfig, "Bytes per block ID: " + BlockIDWidth);
				wl(SysConfig, "Disk size: " + diskSize + "k");
				wl(SysConfig, "Sector range: " + Disk.diskTracks[0].minsectorID + "-" + Disk.diskTracks[0].maxsectorID);

				wl(SysConfig, "Identification type: " + Identifiedby);

				wl(SysConfig, "\n\nBoot sector code:");

				ASMLib asm = new ASMLib(lang);
				int loadedaddress = 0xfe00;
				int realaddress = 0x0000;
				int asmData[] = new int[5];
				try {
					while (realaddress < data.length) {
						String chrdata = "";
						for (int i = 0; i < 5; i++) {
							int d = 0;
							if (realaddress + i < data.length) {
								d = (int) data[realaddress + i] & 0xff;
							}
							asmData[i] = d;

							if ((d > 0x1F) && (d < 0x7f)) {
								chrdata = chrdata + (char) d;
							} else {
								chrdata = chrdata + "?";
							}
						}

						int decLen = 0;
						String decStr = "";
						if (loadedaddress < 0xfe10) {
							if (loadedaddress < 0xfe0a) {
								decLen = 1;
								String label = lang.Msg(CPM.AMSHDRDescriptions[loadedaddress - 0xfe00]);
								decStr = String.format("defb %d ; %s", asmData[0] & 0xff, label);
							} else if (loadedaddress < 0xfe0f) {
								decLen = 5;
								decStr = "defb ";
								String s = "";
								for (int i : asmData) {
									s = s + ", " + String.valueOf(i);
								}
								decStr = decStr + s.substring(2);
								chrdata = "; " + lang.Msg(Languages.MSG_RESERVED);
							} else {
								decLen = 1;
								decStr = String.format("defb %d", asmData[0] & 0xff);
								chrdata = "; " + lang.Msg(Languages.MSG_CHECKSUM);
							}
						} else {
							// decode instruction
							DecodedASM Instruction = asm.decode(asmData, loadedaddress);
							decLen = Instruction.length;
							decStr = Instruction.instruction;
						}
						// output it. - First, assemble a list of hex bytes, but pad out to 12 chars
						// (4x3)
						String hex = "";
						for (int j = 0; j < decLen; j++) {
							hex = hex + String.format("%02X", asmData[j]) + " ";
						}

						String dta[] = new String[4];
						dta[0] = String.format("%04X", loadedaddress);
						dta[1] = hex;
						dta[2] = decStr;
						if (loadedaddress < 0xfe10) {
							dta[3] = chrdata;
						} else {
							dta[3] = chrdata.substring(0, decLen);
						}

						char dt[] = new char[250];
						for (int i = 0; i < 250; i++) {
							dt[i] = ' ';
						}
						System.arraycopy(dta[0].toCharArray(), 0, dt, 0, dta[0].length());
						System.arraycopy(dta[1].toCharArray(), 0, dt, 6, dta[1].length());
						System.arraycopy(dta[2].toCharArray(), 0, dt, 22, dta[2].length());
						System.arraycopy(dta[3].toCharArray(), 0, dt, 42, Math.min(dta[3].length(), 200));

						if (loadedaddress == 0xfe10) {
							wl(SysConfig, "");
						}

						wl(SysConfig, new String(dt).trim());

						realaddress = realaddress + decLen;
						loadedaddress = loadedaddress + decLen;

					} // while
				} catch (Exception E) {
					System.out.println(String.format(lang.Msg(Languages.MSG_ERRORATXX), realaddress, loadedaddress));
					System.out.println(E.getMessage());
					E.printStackTrace();
				}

			} finally {
				SysConfig.close();
			}

			Speccy.SaveFileToDiskAdvanced(new File(folder, "boot.data"), data, data, BasicAction, CodeAction,
					ArrayAction, ScreenAction, MiscAction, null, SwapAction, lang);
		} catch (IOException e) {
			System.out.println(lang.Msg(Languages.MSG_ERREXTRACTBOOT) + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

}

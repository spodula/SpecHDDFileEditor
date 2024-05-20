package hddEditor.libs.partitions;

import java.io.File;
import java.io.FileWriter;

/**
 * Implementation of the PLUSIDEDOS system partition
 * This partition is unusual as it contains the information for the whole disk as
 * well as some other information used by PLUS3DOS. 
 * 
 * https://sinclair.wiki.zxnet.co.uk/wiki/IDEDOS#Partition_Type_0x01_-_The_IDEDOS_System_Partition
 * 
 * 0x0000	50 4C 55 53 49 44 45 44 4F 53 20 20 20 20 20 20		partition name "PLUSIDEDOS      "
 * 0x0010	01 00 00 SH EC EC EH LS LS LS LS 00 00 00 00 00
 * 0x0020	NC NC NH ST SC SC MP MP EA BA UA UB UM M0 M1 MR	
 * 0x0030	DD 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 * 
 * Key:
 * NC: Number of cylinders available to IDEDOS. 16 bit little endian word.
 * NH: Number of heads.
 * ST: Number of sectors per track.
 * SC: Number of sectors per cylinder (equal to NH * ST). 16 bit little endian word.
 * MP: Maximum partition. 16 bit little endian word.
 * EA: Default +3 Editor colour attribute byte.
 * BA: Default +3 BASIC colour attribute byte.
 * UA: Un-map drive A in non-zero.
 * UB: Un-map drive B in non-zero.
 * UM: Un-map drive M in non-zero.
 * M0: Drive letter to map floppy unit 0.
 * M1: Drive letter to map floppy unit 1.
 * MR: Drive letter to map ramdisk.
 * DD: +3DOS default drive letter.
 */

import java.io.IOException;
import java.util.ArrayList;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.handlers.IDEDosHandler;
import hddEditor.libs.partitions.system.DummyFileEntry;

public class SystemPartition extends IDEDosPartition {
	// Storage for the partitions
	public IDEDosPartition partitions[] = null;

	// Flag set by handlers that create this partition as a dummy for the editor.
	// This flag will stop the partition trying to update the disk if its modified.
	public Boolean DummySystemPartiton = false;

	// Number of Cylinders as described in the XDPB
	private int GetNumCyls() {
		int NumCyls = ((RawPartition[0x20 + 1] & 0xff) * 0x100) + (RawPartition[0x20] & 0xff);
		return (NumCyls);
	}

	public void SetNumCyls(int NumCyls) {
		int msb = NumCyls / 0x100;
		int lsb = NumCyls % 0x100;

		RawPartition[0x21] = (byte) msb;
		RawPartition[0x20] = (byte) lsb;
	}

	// Number of heads as described in the Partition page
	public int GetNumHeads() {
		return (RawPartition[0x22] & 0xff);
	}

	public void SetNumHeads(int NumHeads) {
		RawPartition[0x22] = (byte) NumHeads;
	}

	// Sectors per track as described in the Partition page
	public int GetSPT() {
		return (RawPartition[0x23] & 0xff);
	}

	public void SetSPT(int SPT) {
		RawPartition[0x23] = (byte) SPT;
	}

	// Sectors per Cylinder
	private int GetSectorsPerCyls() {
		int NumCyls = ((RawPartition[0x25] & 0xff) * 0x100) + (RawPartition[0x24] & 0xff);
		return (NumCyls);
	}

	public void SetSectorsPerCyls(int NumSectorsPerCyls) {
		int msb = NumSectorsPerCyls / 0x100;
		int lsb = NumSectorsPerCyls % 0x100;

		RawPartition[0x25] = (byte) msb;
		RawPartition[0x24] = (byte) lsb;
	}

//	Max partitions
	public int GetMaxPartitions() {
		int MaxPartitions = ((RawPartition[0x27] & 0xff) * 0x100) + (RawPartition[0x26] & 0xff);
		return (MaxPartitions);
	}

	public void SetMaxPartitions(int MaxPartitions) {
		int msb = MaxPartitions / 0x100;
		int lsb = MaxPartitions % 0x100;

		RawPartition[0x27] = (byte) msb;
		RawPartition[0x26] = (byte) lsb;
	}

	// Basic editor colour
	public int GetBasicEditColour() {
		return (RawPartition[0x28] & 0xff);
	}

	public void SetBasicEditColour(int BasicEditColour) {
		RawPartition[0x28] = (byte) BasicEditColour;
	}

	// Basic default colour.
	public int GetBasicColour() {
		return (RawPartition[0x29] & 0xff);
	}

	public void SetBasicColour(int BasicColour) {
		RawPartition[0x29] = (byte) BasicColour;
	}

	/*
	 * Unmap A,B,M ; As There is only space for 5 drive mappings, this is sometimes
	 * done to free up more XDPBs so you can have more HDD partitions logged in.
	 * This makes a lot of sense for a +2A setup which doesn't have FDDs
	 */
	public boolean GetUnmapA() {
		return ((RawPartition[0x2a] & 0xff) != 0x00);
	}

	public void SetUnmapA(boolean unmap) {
		RawPartition[0x2a] = BoolTo01(unmap);
	}

	// Unmap B
	public boolean GetUnmapB() {
		return ((RawPartition[0x2b] & 0xff) != 0x00);
	}

	public void SetUnmapB(boolean unmap) {
		RawPartition[0x2b] = BoolTo01(unmap);
	}

	// Unmap M
	public boolean GetUnmapM() {
		return ((RawPartition[0x2c] & 0xff) != 0x00);
	}

	public void SetUnmapM(boolean unmap) {
		RawPartition[0x2c] = BoolTo01(unmap);
	}

	// Drive 0 drive letter
	public String GetUnit0DriveLetter() {
		return (letterOrBlank(RawPartition[0x2d]));
	}

	public void SetUnit0DriveLetter(String DriverLetter) {
		RawPartition[0x2d] = StringOrBlankToByte(DriverLetter);
	}

	// Drive 1 drive letter
	public String GetUnit1DriveLetter() {
		return (letterOrBlank(RawPartition[0x2e]));
	}

	public void SetUnit1DriveLetter(String DriverLetter) {
		RawPartition[0x2e] = StringOrBlankToByte(DriverLetter);
	}

	// Drive 0 drive letter
	public String GetRamdiskDriveLetter() {
		return (letterOrBlank(RawPartition[0x2d]));
	}

	public void SetRamdiskDriveLetter(String DriverLetter) {
		RawPartition[0x2f] = StringOrBlankToByte(DriverLetter);
	}

	// Default +3DOS drive. This is usually "M" by default, but it usually makes
	// more sense to change it to C: (Using a load "C:" ASN command)
	public String GetDefaultDrive() {
		return (letterOrBlank(RawPartition[0x30]));
	}

	public void SetDefaultDrive(String DriverLetter) {
		RawPartition[0x30] = StringOrBlankToByte(DriverLetter);
	}

	/**
	 * 
	 * @param tag
	 * @param disk
	 * @param RawPartition
	 */
	public SystemPartition(int DirentLocation, Disk disk, byte[] RawPartition, int DirentNum, boolean Initialise) {
		super(DirentLocation, disk, RawPartition, DirentNum, Initialise);
		CanExport = true;
	}

	/**
	 * ToString overridden to provide System partition specific information
	 * 
	 * @return
	 */
	public String toString() {
		String result = super.toString();
		result = result + "\n  Allocated Cylinders: " + GetNumCyls();
		result = result + "\tNumber of heads: " + GetNumHeads();
		result = result + "\tSectors Per Track: " + GetSPT();
		result = result + "\tSectors Per Cyl: " + GetSectorsPerCyls();

		result = result + "\n  Max Partitions: " + GetMaxPartitions();
		result = result + "\t\tBasic Editor Colour: " + GetBasicEditColour();
		result = result + "\tBasic Colour: " + GetBasicColour();
		result = result + "\tUnmap drive A: " + GetUnmapA();

		result = result + "\n  Unmap drive B: " + GetUnmapB();
		result = result + "\t\tUnmap drive M: " + GetUnmapM();
		result = result + "\tDisk unit 0 letter: " + GetUnit0DriveLetter();
		result = result + "\tDisk unit 1 letter: " + GetUnit1DriveLetter();

		result = result + "\n  Ramdisk drive letter: " + GetRamdiskDriveLetter();
		result = result + "\tDefault drive: " + GetDefaultDrive();

		return (result);
	}

	/**
	 * Write the system details to the disk. Note, this was originally written
	 * before the partition objects just directly modified the raw dirent. This is
	 * probably a bit pointless.
	 * 
	 * @param BasicEdCol
	 * @param DefaultCol
	 * @param UnmapA
	 * @param UnmapB
	 * @param UnmapM
	 * @param M0
	 * @param M1
	 * @param MR
	 * @param DefaultDrive
	 */
	public void SetDetails(int BasicEdCol, int DefaultCol, boolean UnmapA, boolean UnmapB, boolean UnmapM, String M0,
			String M1, String MR, String DefaultDrive) {
		// Update the dirent
		SetBasicEditColour(BasicEdCol);
		SetBasicColour(DefaultCol);
		SetUnmapA(UnmapA);
		SetUnmapB(UnmapB);
		SetUnmapM(UnmapM);
		SetUnit0DriveLetter(M0);
		SetUnit1DriveLetter(M1);
		SetRamdiskDriveLetter(MR);
		SetDefaultDrive(DefaultDrive);

		UpdateSystemPartitionOnDisk();
	}

	/**
	 * Update this Dirent ONLY from the raw data
	 */
	public void UpdateSystemPartitionOnDisk() {
		if (!DummySystemPartiton)
			try {
				/*
				 * Copy the raw dirent over
				 */
				// Get the number of sectors in the partition
				int DirentLength = GetMaxPartitions() * 0x40;
				// Load the partition
				byte data[] = GetDataInPartition(0, DirentLength);
				// update the data with this partition
				System.arraycopy(RawPartition, 0, data, DirentLocation, 0x40);
				// write it back.

				SetDataInPartition(0, data);
			} catch (IOException e) {
				System.out.println("Error updating System DIRENT: " + e.getMessage());
				e.printStackTrace();
			}
	}

	/**
	 * Update the entire disk partition list.
	 */
	public void UpdatePartitionListOnDisk() {
		if (!DummySystemPartiton)
			try {
				/*
				 * Copy the raw dirent over
				 */
				// Get the number of sectors in the partition
				int DirentSize = GetMaxPartitions() * 0x40;
				// Load the partition
				byte data[] = GetDataInPartition(0, DirentSize);
				// update the data with all the partitions
				for (IDEDosPartition p : partitions) {
					System.arraycopy(p.RawPartition, 0, data, p.DirentLocation, 0x40);
				}
				// write it back.
				SetDataInPartition(0, data);
			} catch (IOException e) {
				System.out.println("Error updating System DIRENT: " + e.getMessage());
				e.printStackTrace();
			}
	}

	/**
	 * Return a byte that contains the uppercase first character of the string or #0
	 * 
	 * @param s
	 * @return
	 */
	private byte StringOrBlankToByte(String s) {
		byte result = 0;
		s = s.trim().toUpperCase();
		if (!s.isEmpty()) {
			char c = s.charAt(0);
			result = (byte) c;
		}
		return (result);
	}

	/**
	 * Convert a boolean to 0 (false) or 1 (true)
	 * 
	 * @param x
	 * @return
	 */
	private byte BoolTo01(boolean x) {
		byte result = 0;
		if (x) {
			result = 1;
		}
		return (result);
	}

	/**
	 * Reload the partition details.
	 */
	@Override
	public void Reload() {
		/*
		 * For the SYstem partition, we need to force a disk reload. This is done by the
		 * parent.
		 */
	}

	@Override
	public void ExtractPartitiontoFolderAdvanced(File folder, int BasicAction, int CodeAction, int ArrayAction,
			int ScreenAction, int MiscAction, int SwapAction, ProgressCallback progress, boolean IncludeDeleted)
			throws IOException {
		FileWriter SysConfig = new FileWriter(new File(folder, "system.config"));
		try {
			SysConfig.write("<plusidedos>\n".toCharArray());
			SysConfig.write(("  <name>" + GetName() + "</name>\n").toCharArray());
			SysConfig.write(("  <dummy>" + DummySystemPartiton + "</dummy>\n").toCharArray());
			SysConfig.write(("  <disk_geometry>\n").toCharArray());
			SysConfig.write(("      <cyl>" + GetNumCyls() + "</cyl>\n").toCharArray());
			SysConfig.write(("      <head>" + GetNumHeads() + "</head>\n").toCharArray());
			SysConfig.write(("      <sectors>" + GetSectorsPerCyls() + "</sectors>\n").toCharArray());
			SysConfig.write(("  </disk_geometry>\n").toCharArray());
			SysConfig.write(("  <basic_colour>" + GetBasicColour() + "</basic_colour>\n").toCharArray());
			SysConfig.write(("  <editor_colour>" + GetBasicEditColour() + "</editor_colour>\n").toCharArray());
			SysConfig.write(("  <unmap_a>" + GetUnmapA() + "</unmap_a>\n").toCharArray());
			SysConfig.write(("  <unmap_b>" + GetUnmapA() + "</unmap_b>\n").toCharArray());
			SysConfig.write(("  <unmap_m>" + GetUnmapA() + "</unmap_m>\n").toCharArray());
			SysConfig.write(("  <default_drive>" + GetDefaultDrive() + "</default_drive>\n").toCharArray());
			SysConfig.write(("  <unit0_drive>" + GetUnit0DriveLetter() + "</unit0_drive>\n").toCharArray());
			SysConfig.write(("  <unit1_drive>" + GetUnit1DriveLetter() + "</unit1_drive>\n").toCharArray());
			SysConfig.write(("  <unitm_drive>" + GetRamdiskDriveLetter() + "</unitm_drive>\n").toCharArray());
			SysConfig.write(("  <maxpart>" + GetMaxPartitions() + "</maxpart>\n").toCharArray());
			SysConfig.write(("  <partitions>\n").toCharArray());

			int entrynum = 0;
			for (IDEDosPartition p : partitions) {
				if (progress != null) {
					if (progress.Callback(partitions.length, entrynum++, "Partition: " + p.GetName())) {
						break;
					}
				}
				if (p.GetPartType() != 0) {
					SysConfig.write(("    <partition>\n").toCharArray());
					SysConfig.write(("        <partname>" + p.GetName() + "</partname>\n").toCharArray());
					SysConfig.write(("        <parttype>" + p.GetPartType() + "</parttype>\n").toCharArray());
					SysConfig.write(("        <parttypename>" + PLUSIDEDOS.GetTypeAsString(p.GetPartType()).trim()
							+ "</parttypename>\n").toCharArray());
					SysConfig.write(
							("        <direntlocation>" + p.DirentLocation + "</direntlocation>\n").toCharArray());
					SysConfig.write(("        <sizek>" + p.GetSizeK() + "</sizek>\n").toCharArray());
					SysConfig.write(("        <part_geometry>\n").toCharArray());
					SysConfig.write(("            <startcyl>" + p.GetStartCyl() + "</startcyl>\n").toCharArray());
					SysConfig.write(("            <starthead>" + p.GetStartHead() + "</starthead>\n").toCharArray());
					SysConfig.write(("            <endcyl>" + p.GetEndCyl() + "</endcyl>\n").toCharArray());
					SysConfig.write(("            <endhead>" + p.GetEndHead() + "</endhead>\n").toCharArray());
					SysConfig.write(("        </part_geometry>\n").toCharArray());
					SysConfig.write(("        <RawDirent>\n").toCharArray());
					String dirent = GeneralUtils.HexDump(p.RawPartition, 0, 0x40, 0).trim();
					dirent = dirent.replace("\n", "\n            ");
					SysConfig.write("            " + dirent);
					SysConfig.write(("\n        </RawDirent>\n").toCharArray());
					SysConfig.write(("    </partition>\n").toCharArray());
				}
			}

			SysConfig.write(("  </partitions>\n").toCharArray());
			SysConfig.write("</plusidedos>\n".toCharArray());
		} finally {
			SysConfig.close();
		}
	}

	/**
	 * Create a named partition on the current disk
	 * 
	 * @param partname
	 * @param Partsize
	 * @param PartType
	 * @param IsScript
	 * @throws PlusIDEDosException
	 */
	public void CreatePartition(String partname, int Partsize, int PartType) throws PlusIDEDosException {
		if (DummySystemPartiton) {
			throw new PlusIDEDosException(partname, "Cannot edit partitions on this media type.");
		} else {
			// check for duplicate name
			boolean IsExisting = false;
			for (IDEDosPartition p : partitions) {
				if (p.GetPartType() != 0) {
					if (p.GetName().equals(partname)) {
						IsExisting = true;
					}
				}
			}
			if (IsExisting) {
				throw new PlusIDEDosException(partname,
						"Error creating partition: Partition " + partname + " already exists.");
			} else {
				/*
				 * Find a free partition with enough space. Note, the flags are for how to deal
				 * with the partition when allocating. If its the last partition, remove the
				 * Free space partition. Otherwise, if its the last allocated partition, but
				 * there are still entries left, Will need to move the Free space partition, and
				 * setup the new one in the old slot.
				 */
				boolean IsLastPartition = false;
				boolean IsLastFreeSpacePartition = false;
				IDEDosPartition FoundPartiton = null;
				for (IDEDosPartition part : partitions) {
					if (part.GetPartType() == PLUSIDEDOS.PARTITION_FREE) {
						long PartitonSizeMb = part.GetSizeK() / 1024;
						if (PartitonSizeMb >= Partsize) {
							FoundPartiton = part;
							if (FoundPartiton.DirentNum == partitions.length - 1) {
								IsLastPartition = true;
							} else {
								if (partitions[FoundPartiton.DirentNum + 1]
										.GetPartType() == PLUSIDEDOS.PARTITION_UNUSED) {
									IsLastFreeSpacePartition = true;
								}
							}
							break;
						}
					}
				}
				if (FoundPartiton == null) {
					throw new PlusIDEDosException(partname,
							"Error creating partition: Unable to find an empty partition.");
				} else {
					// If the partition isnt the last partition, just use it as is. Dont try to
					// defrag the disk.
					if (!IsLastFreeSpacePartition) {
						FoundPartiton.SetPartType(PartType);
						long PartitonSizeMb = FoundPartiton.GetSizeK() / 1024;
						if (PartitonSizeMb != Partsize) {
							PartitonSizeMb = Partsize;
							long NumSectors = PartitonSizeMb * 1024 * 1024 / CurrentDisk.GetSectorSize();

							long NumCyls = NumSectors / CurrentDisk.GetNumSectors();
							if (NumSectors % CurrentDisk.GetNumSectors() != 0) {
								NumCyls++;
							}
							int Tracks = (int) (NumCyls / CurrentDisk.GetNumHeads());
							int Heads = (int) (NumCyls % CurrentDisk.GetNumHeads());

							Heads = Heads + FoundPartiton.GetStartHead();
							if (Heads >= CurrentDisk.GetNumHeads()) {
								Heads = Heads - CurrentDisk.GetNumHeads();
								Tracks++;
							}
							Tracks = Tracks + FoundPartiton.GetStartCyl();

							FoundPartiton.SetEndCyl(Tracks);
							FoundPartiton.SetEndHead(Heads);
							FoundPartiton.UpdateEndSector();

						}
						FoundPartiton = IDEDosHandler.GetNewPartitionByType(PartType, FoundPartiton.DirentLocation,
								FoundPartiton.CurrentDisk, FoundPartiton.RawPartition, FoundPartiton.DirentNum, false);
						partitions[FoundPartiton.DirentNum] = FoundPartiton;
						FoundPartiton.SetName(partname);

					} else {
						int PartitonSizeMb = Partsize;
						int NumSectors = PartitonSizeMb * 1024 * (1024 / CurrentDisk.GetSectorSize());

						// This seems to be hard limit, so fiddle it.
						if (CurrentDisk.GetSectorSize() == 512) {
							if (NumSectors > 32790) {
								NumSectors = 32790;
							}
						} else {
							if (NumSectors > 65580) {
								NumSectors = 65580;
							}
						}

						int NumCyls = NumSectors / CurrentDisk.GetNumSectors();
						if (NumSectors % CurrentDisk.GetNumSectors() != 0) {
							NumCyls++;
						}
						int Tracks = NumCyls / CurrentDisk.GetNumHeads();
						int Heads = NumCyls % CurrentDisk.GetNumHeads();

						IDEDosPartition NewFreePartition = null;

						/*
						 * copy the free space partition. Note, need to duplicate the Raw partition
						 * data, as its passed by reference.
						 */
						if (!IsLastPartition) {
							byte NewRawPartition[] = new byte[FoundPartiton.RawPartition.length];
							System.arraycopy(FoundPartiton.RawPartition, 0, NewRawPartition, 0, NewRawPartition.length);

							NewFreePartition = new FreePartition(FoundPartiton.DirentLocation + 0x40,
									FoundPartiton.CurrentDisk, NewRawPartition, FoundPartiton.DirentNum + 1, false);
							// Update the cylinder
							int NewTrack = FoundPartiton.GetStartCyl() + Tracks;
							int NewHead = FoundPartiton.GetStartHead() + Heads;
							if (NewHead > FoundPartiton.CurrentDisk.GetNumHeads()) {
								NewHead = 0;
								NewTrack++;
							}

							NewFreePartition.SetStartCyl(NewTrack);
							NewFreePartition.SetStartHead(NewHead);
							// Set the new type and add it.
							NewFreePartition.SetPartType(PLUSIDEDOS.PARTITION_FREE);
							NewFreePartition.UpdateEndSector();
							partitions[NewFreePartition.DirentNum] = NewFreePartition;
							// extract the track and head information to use as the end of the new
							// partition.
							Tracks = NewFreePartition.GetStartCyl();
							Heads = NewFreePartition.GetStartHead();
							Heads = Heads - 1;
							if (Heads < 0) {
								Heads = FoundPartiton.CurrentDisk.GetNumHeads() - 1;
								Tracks = Tracks - 1;
							}
						} else {
							// If we havent created a new free space partition, use the old partitions
							// details.
							Tracks = FoundPartiton.GetStartCyl() + Tracks;
							Heads = FoundPartiton.GetStartHead() + Heads;
							if (Heads > FoundPartiton.CurrentDisk.GetNumHeads()) {
								Heads = 0;
								Tracks++;
							}
							// Now, modify the old partition
						}
						FoundPartiton.SetName(partname);
						FoundPartiton.SetPartType(PartType);
						FoundPartiton.SetEndCyl(Tracks);
						FoundPartiton.SetEndHead(Heads);

						// Re-create as the proper type
						FoundPartiton = IDEDosHandler.GetNewPartitionByType(PartType, FoundPartiton.DirentLocation,
								FoundPartiton.CurrentDisk, FoundPartiton.RawPartition, FoundPartiton.DirentNum, true);

						partitions[FoundPartiton.DirentNum] = FoundPartiton;
					}
				}
				UpdatePartitionListOnDisk();
			}
		}
	}

	/**
	 * Implement deletion of selected partition
	 * 
	 * @throws PlusIDEDosException
	 */
	public void DeletePartition(String partname) throws PlusIDEDosException {
		partname = partname.toUpperCase();
		if (DummySystemPartiton) {
			throw new PlusIDEDosException(partname, "Cannot Delete partitions on this media type.");
		} else {
			// Find the partition to delete
			IDEDosPartition part = null;
			for (IDEDosPartition p : partitions) {
				if (p.GetPartType() != 0) {
					if (p.GetName().toUpperCase().equals(partname.toUpperCase())) {
						part = p;
					}
				}
			}
			if (part == null) {
				throw new PlusIDEDosException(partname, "Cannot find the partition '" + partname + "' to delete");
			} else {
				/*
				 * Set the partition to FREE, and reallocate it.
				 */
				part.SetPartType(PLUSIDEDOS.PARTITION_FREE);

				IDEDosPartition NewFreePartition = IDEDosHandler.GetNewPartitionByType(PLUSIDEDOS.PARTITION_FREE,
						part.DirentLocation, part.CurrentDisk, part.RawPartition, part.DirentNum, false);

				NewFreePartition.SetPartType(PLUSIDEDOS.PARTITION_FREE);

				/*
				 * Try to merge together any free partitions.
				 */
				if (NewFreePartition.DirentNum != partitions.length - 1) {
					if (partitions[part.DirentNum + 1].GetPartType() == PLUSIDEDOS.PARTITION_FREE) {
						// Get the partition
						IDEDosPartition OldFreePartiton = partitions[NewFreePartition.DirentNum + 1];
						// Merge the partitions
						NewFreePartition.SetEndCyl(OldFreePartiton.GetEndCyl());
						NewFreePartition.SetEndHead(OldFreePartiton.GetEndHead());
						// reset the partition type
						OldFreePartiton.SetPartType(PLUSIDEDOS.PARTITION_UNUSED);
						// and set it back.
						partitions[OldFreePartiton.DirentNum] = OldFreePartiton;
					}
				}
				/*
				 * Update the partition list and on the disk.
				 */
				partitions[NewFreePartition.DirentNum] = NewFreePartition;
			}
			UpdatePartitionListOnDisk();
		}
	}

	/**
	 * 
	 */
	@Override
	public FileEntry[] GetFileList(String wildcard) {
		ArrayList<FileEntry> results = new ArrayList<FileEntry>();
		for (IDEDosPartition idep : partitions) {
			if (idep.GetPartType() != PLUSIDEDOS.PARTITION_UNUSED) {
				FileEntry fe = new DummyFileEntry(idep.GetName(), (int)( idep.GetSizeK() * 1024),
						PLUSIDEDOS.GetTypeAsString(idep.GetPartType()));
				if (fe.DoesMatch(wildcard)) {
					results.add(fe);
				}
			}
		}
		return (results.toArray(new FileEntry[0]));
	}
	
}

package hddEditor.libs.handlers;

import java.io.File;

/**
 * This object parses the IDEDOS partitions.
 * 
 * 
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import hddEditor.libs.Languages;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.HDD.IDEDosDisk;
import hddEditor.libs.partitions.FreePartition;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.NonCPMDiskImagePartition;
import hddEditor.libs.partitions.PLUS3DOSPartition;
import hddEditor.libs.partitions.SwapPartition;
import hddEditor.libs.partitions.SystemPartition;
import hddEditor.libs.partitions.cpm.CPMDirectoryEntry;

public class IDEDosHandler extends OSHandler {
	/**
	 * Open the disk and decode the IDEDOS partitions.
	 * 
	 * @param disk
	 * @throws IOException
	 */
	public IDEDosHandler(Disk disk,Languages lang) throws IOException {
		super(disk, lang);
		LoadAndDecodePartitions();
	}

	/**
	 * Decode the partitions.
	 * 
	 * @throws IOException
	 */
	private void LoadAndDecodePartitions() throws IOException {
		/*
		 * The first sector is the start of the Partition list. The first item in the
		 * partition list is ALWAYS the system partition. From this we can get details
		 * like the size of the full partition table.
		 */
		byte[] FirstSector = CurrentDisk.GetBytesStartingFromSector(0, 512);

		if (!new String(FirstSector, StandardCharsets.UTF_8).startsWith(IDEDosDisk.IDEDOSHEADER)) {
			System.out.println("File does not contain an IDEDOS partition table.");
		} else {
			// Extract the max number of partitions and then load the appropriate amount of
			// sectors.
			MaxPartitions = (FirstSector[0x26] & 0xff) + ((FirstSector[0x27] & 0xff) * 256);
			System.out.println("Partition table has " + MaxPartitions + " entries.");
			// load the partition table.
			byte[] PartData = CurrentDisk.GetBytesStartingFromSector(0, MaxPartitions * 0x40);

			int ptr = 0;
			int partnum = 0;
			ArrayList<IDEDosPartition> newparts = new ArrayList<IDEDosPartition>();
			while (ptr < PartData.length) {
				byte partitiondata[] = new byte[64];
				System.arraycopy(PartData, ptr, partitiondata, 0, 64);
				int PartType = (partitiondata[0x10] & 0xff);
				IDEDosPartition idp = GetNewPartitionByType(PartType, ptr, CurrentDisk, partitiondata, partnum++,
						false, lang);

				if (PartType == 1) {
					SystemPart = (SystemPartition) idp;
				}
				newparts.add(idp);
				ptr = ptr + 64;
			}
			SystemPart.partitions = newparts.toArray(new IDEDosPartition[0]);
		}

	}

	/**
	 * ToString overridden to show the partitions in the IDEDOS disk
	 */
	@Override
	public String toString() {
		String result = "----------------------------------------------\n";
		for (IDEDosPartition p : SystemPart.partitions) {
			if (p.GetPartType() != 0) {
				result = result + p.toString() + "\n----------------------------------------------\n";
			}
		}
		return (result);
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		IDEDosHandler h;
		try {
			IDEDosDisk disk = new IDEDosDisk(new File("/data1/IDEDOS/2gtest.img"),0,0,0);
			h = new IDEDosHandler(disk,new Languages());
			PLUS3DOSPartition p3d = (PLUS3DOSPartition) h.SystemPart.partitions[1];
			System.out.println("---------------------");
			System.out.println(p3d);
			System.out.println("---------------------");

			String result = "";
			for (CPMDirectoryEntry de : p3d.DirectoryEntries) {
				if (!de.IsDeleted) {
					String fn = de.GetFilename();
					while (fn.length() < 15) {
						fn = fn + " ";
					}
					result = result + fn;
					result = result + "\tCPMLen:" + de.GetFileSize() + "\tDeleted?:" + de.IsDeleted + "\n";
				}
			}
			System.out.println(result);

			disk.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	/**
	 * Get a new partition of the given type
	 * 
	 * @param PartType       - type
	 * @param DirentLocation - Address in dirent block
	 * @param CurrentDisk    - disk
	 * @param partitiondata  - Partition hex data
	 * @param partnum        - Partition number
	 * @param Initialise     - if TRUE, initialise Partition with some default data,
	 *                       if FALSE, just load.
	 * @return
	 */
	public static IDEDosPartition GetNewPartitionByType(int PartType, int DirentLocation, Disk CurrentDisk,
			byte partitiondata[], int partnum, boolean Initialise,Languages lang) {
		IDEDosPartition idp = null;
		try {
			if (PartType == 1) {
				idp = new SystemPartition(DirentLocation, CurrentDisk, partitiondata, partnum, Initialise, lang);
			} else if (PartType == PLUSIDEDOS.PARTITION_SWAP) {
				idp = new SwapPartition(DirentLocation, CurrentDisk, partitiondata, partnum, Initialise, lang);
			} else if (PartType == PLUSIDEDOS.PARTITION_PLUS3DOS) {
				idp = new PLUS3DOSPartition(DirentLocation, CurrentDisk, partitiondata, partnum, Initialise, lang);
			} else if (PartType == PLUSIDEDOS.PARTITION_UNKNOWN)  {
				idp = new NonCPMDiskImagePartition(DirentLocation, CurrentDisk, partitiondata, partnum, Initialise, lang);
			} else if (PartType < 0x40 && PartType > 0x29) {
				idp = new NonCPMDiskImagePartition(DirentLocation, CurrentDisk, partitiondata, partnum, Initialise, lang);
			} else if (PartType == PLUSIDEDOS.PARTITION_FREE) {
				idp = new FreePartition(DirentLocation, CurrentDisk, partitiondata, partnum, Initialise, lang);
			} else {
				// generic partition
				idp = new IDEDosPartition(DirentLocation, CurrentDisk, partitiondata, partnum, Initialise, lang);
			}
		} catch (Exception E) {
			E.printStackTrace();
			System.out.println("Error getting partition of type " + PartType + " :" + E.getMessage());
		}
		return (idp);
	}

}

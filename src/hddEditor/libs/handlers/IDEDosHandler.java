package hddEditor.libs.handlers;
/**
 * This object parses the IDEDOS partitions.
 * 
 * 
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.IDEDosDisk;
import hddEditor.libs.partitions.FreePartition;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.NonCPMDiskImagePartition;
import hddEditor.libs.partitions.PLUS3DOSPartition;
import hddEditor.libs.partitions.SwapPartition;
import hddEditor.libs.partitions.SystemPartition;
import hddEditor.libs.partitions.cpm.DirectoryEntry;

public class IDEDosHandler extends OSHandler {
	//Max partitions as loaded from the disk
	public int MaxPartitions = 0;
	
	public SystemPartition SystemPart = null;
	
	/**
	 * Open the disk and decode the IDEDOS partitions.
	 * 
	 * @param disk
	 * @throws IOException
	 */
	public IDEDosHandler(Disk disk) throws IOException {
		super(disk);
		LoadAndDecodePartitions();	
	}

	/**
	 * Decode the partitions.
	 * 
	 * @throws IOException
	 */
	private void LoadAndDecodePartitions() throws IOException {
		/*
		 * The first sector is the start of the Partition list. 
		 * The first item in the partition list is ALWAYS the system partition.
		 * From this we can get details like the size of the full partition table. 
		 */
		byte[] FirstSector = CurrentDisk.GetBytesStartingFromSector(0,512);
		
		if (!new String(FirstSector, StandardCharsets.UTF_8).startsWith(IDEDosDisk.IDEDOSHEADER)) {
			System.out.println("File does not contain an IDEDOS partition table.");
		} else {
			//Extract the max number of partitions and then load the appropriate amount of sectors.
			MaxPartitions = (FirstSector[0x26] & 0xff) + ((FirstSector[0x27] & 0xff) * 256);
			System.out.println("Partition table has " + MaxPartitions + " entries.");
			//load the partition table.
			byte[] PartData = CurrentDisk.GetBytesStartingFromSector(0, MaxPartitions * 0x40);
			
			int ptr = 0;
			int partnum=0;
			ArrayList<IDEDosPartition> newparts = new ArrayList<IDEDosPartition>();
			while (ptr < PartData.length) {
				byte partitiondata[] = new byte[64];
				System.arraycopy(PartData, ptr, partitiondata, 0, 64);
				int PartType = (partitiondata[0x10] & 0xff);
				IDEDosPartition idp = GetNewPartitionByType(PartType, ptr, CurrentDisk, partitiondata, partnum++, false);

				if (PartType == 1) {
					SystemPart = (SystemPartition)idp;
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
	 * @param args
	 */
	public static void main(String[] args) {
		IDEDosHandler h;
		try {
			IDEDosDisk disk = new IDEDosDisk("/data1/IDEDOS/2gtest.img");
			h = new IDEDosHandler(disk);
			PLUS3DOSPartition p3d = (PLUS3DOSPartition)h.SystemPart.partitions[1];
			System.out.println("---------------------");
			System.out.println(p3d);
			System.out.println("---------------------");
//			Plus3DosFileHeader pdh= p3d.DirectoryEntries[2].GetPlus3DosHeader();
			//System.out.println(p3d.DirectoryEntries[2].filename()+" "+ pdh.getTypeDesc());
//			byte temp[] = TestUtils.ReadFileIntoArray("/home/graham/Pictures/0018_SHE.bin");
//		    if (temp!=null)
//				p3d.AddRawCodeFile("TEST1.CDE", 16384, temp);
			String result = "";
			for(DirectoryEntry de:p3d.DirectoryEntries) {
				if (!de.IsDeleted) {
					String fn=de.filename();
					while(fn.length() < 15) {
						fn = fn + " ";
					}
					result = result + fn; 
					result = result + "\tCPMLen:"+de.GetFileSize() + "\tDeleted?:" +de.IsDeleted +   "\n";
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
	 * Get a partition given the name or NULL
	 * Case insensitive.
	 * 
	 * @param PartName
	 * @return
	 */
	public IDEDosPartition GetPartitionByName(String PartName) {
		IDEDosPartition result = null;
		String searchstring = PartName.trim().toUpperCase();
		for(IDEDosPartition part:SystemPart.partitions) {
			if (part.GetName().toUpperCase().trim().equals(searchstring)) {
				result = part;
			}
		}
		return result;
	}
	
	/**
	 * Get the last partition with the given Partition type
	 * @param PartType
	 * @return
	 */
	public IDEDosPartition GetPartitionByType(int PartType) {
		IDEDosPartition result = null;
		for(IDEDosPartition part:SystemPart.partitions) {
			if (part.GetPartType() == PartType) {
				result = part;
			}
		}
		return result;
	}	
	
	/**
	 * Get a new partition of the given type
	 * 
	 * @param PartType - type
	 * @param DirentLocation - Address in dirent block
	 * @param CurrentDisk - disk
	 * @param partitiondata - Partiton hex data
	 * @param partnum - Partition number
	 * @param Initialise - if TRUE, initialise parititon with data, if FALSE, just load. 
	 * @return
	 */
	public static IDEDosPartition GetNewPartitionByType(int PartType, int DirentLocation, Disk CurrentDisk, byte partitiondata[], int partnum, boolean Initialise) {
		IDEDosPartition idp = null;
		try {
		if (PartType == 1) {
			idp = new SystemPartition(DirentLocation, CurrentDisk, partitiondata,partnum, Initialise);
		} else if (PartType == 2) {
			idp = new SwapPartition(DirentLocation, CurrentDisk, partitiondata,partnum, Initialise);
		} else if (PartType == 3) {
			idp = new PLUS3DOSPartition(DirentLocation, CurrentDisk, partitiondata,partnum, Initialise);
		} else if (PartType < 0x40 && PartType > 0x29) {
			idp = new NonCPMDiskImagePartition(DirentLocation, CurrentDisk, partitiondata,partnum, Initialise);
		} else if (PartType == 0xff) {
			idp = new FreePartition(DirentLocation, CurrentDisk, partitiondata,partnum, Initialise);
		} else {
			//generic partition
			idp = new IDEDosPartition(DirentLocation, CurrentDisk, partitiondata,partnum, Initialise);
		}
		} catch (Exception E) {
			E.printStackTrace();
			System.out.println("Error getting partition of type "+PartType+" :"+E.getMessage());
			
		}
		return(idp);
	}

}

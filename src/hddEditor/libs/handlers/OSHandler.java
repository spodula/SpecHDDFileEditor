package hddEditor.libs.handlers;
/**
 * base object of the OS handler. 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.HDD.IDEDosDisk;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.SystemPartition;

public class OSHandler {
	// Max partitions as loaded from the disk
	public int MaxPartitions = 0;

	// Storage for the System partition
	public SystemPartition SystemPart = null;
	
	//Disk being accesses
	Disk CurrentDisk = null;
	
	/**
	 * Constructor
	 * @param disk
	 */
	public OSHandler(Disk disk) {
		CurrentDisk = disk;
	}

	/**
	 * ToString overridden to show local flags
	 */
	@Override
	public String toString() {
		String result = super.toString();
		return(result);
	}

	/**
	 * Test harness
	 * @param args
	 */
	public static void main(String[] args) {
		OSHandler h;
		try {
			Disk disk = new IDEDosDisk(new File("/data1/idedos.dsk"));
			h = new OSHandler(disk);
			System.out.println(h);
			disk.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Get the last partition with the given Partition type
	 * 
	 * @param PartType
	 * @return
	 */
	public IDEDosPartition GetPartitionByType(int PartType) {
		IDEDosPartition result = null;
		for (IDEDosPartition part : SystemPart.partitions) {
			if (part.GetPartType() == PartType) {
				result = part;
			}
		}
		return result;
	}

	/**
	 * Get a partition given the name or NULL Case insensitive.
	 * 
	 * @param PartName
	 * @return
	 */
	public IDEDosPartition GetPartitionByName(String PartName) {
		IDEDosPartition result = null;
		String searchstring = PartName.trim().toUpperCase();
		for (IDEDosPartition part : SystemPart.partitions) {
			if (part.GetName().toUpperCase().trim().equals(searchstring)) {
				result = part;
			}
		}
		return result;
	}	
}
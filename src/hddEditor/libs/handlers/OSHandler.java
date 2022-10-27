package hddEditor.libs.handlers;
/**
 * base object of the OS handler. 
 */

import java.io.FileNotFoundException;
import java.io.IOException;

import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.IDEDosDisk;

public class OSHandler {
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
			Disk disk = new IDEDosDisk("/data1/idedos.dsk");
			h = new OSHandler(disk);
			System.out.println(h);
			disk.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
}

package hddEditor.libs.disks;
/**
 * This is the interface to a generic disk 
 */

import java.io.File;
import java.io.IOException;

public interface Disk {
	public int GetMediaType();
	
	//Disk filename
	public String GetFilename();
	public void SetFilename(String filename);
	
	//Real Sector size of the disk.
	public int GetSectorSize();
	public void SetSectorSize(int sz);
	
	//Number of cylinders for the disk
	public int GetNumCylinders();
	public void SetNumCylinders(int sz);

	//Get the file size 
	public long GetFileSize();

	//Number of heads for the disk
	public int GetNumHeads();
	public void SetNumHeads(int sz);

	//Number of sectors per track
	public int GetNumSectors();
	public void SetNumSectors(int sz);

	/**
	 * Is the loaded disk out of date compared to the one on disk?
	 * @return TRUE if disk doesnt match loaded disk
	 */
	public boolean DiskOutOfDate();

	/**
	 * Update any internal last modified flags.
	 */
	public void UpdateLastModified();
	
	
	
	/**
	 * Get the number of logical sectors by the disks size. 
	 * 
	 * @return
	 * @throws IOException
	 */
	public long GetNumLogicalSectors();
	
	/**
	 * Close the disk, even if it doesn't want to..
	 */
	public void close();
	
	/**
	 * Returns TRUE if the file opened correctly.  
	 * 
	 * @return
	 */
	public boolean IsOpen();
	
	/**
	 * Set a block starting from the disk logical sector.
	 * 
	 * @param SectorNum
	 * @param result
	 * @throws IOException
	 */
	public void SetLogicalBlockFromSector(long SectorNum, byte result[]) throws IOException;
	
	/**
	 * Get a block of length SZ starting from the disk logical sector.
	 * 
	 * @param SectorNum
	 * @param sz
	 * @return
	 * @throws IOException
	 */
	public byte[] GetBytesStartingFromSector(long SectorNum, long sz) throws IOException;

	/**
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public Boolean IsMyFileType(File filename) throws IOException;
	
}

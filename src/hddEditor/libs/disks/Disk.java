package hddEditor.libs.disks;

import java.io.File;
import java.io.IOException;

public interface Disk {
	public String GetFilename();
	public void SetFilename(String filename);
	
	public int GetSectorSize();
	public void SetSectorSize(int sz);
	
	public int GetNumCylinders();
	public void SetNumCylinders(int sz);

	public long GetFileSize();

	public int GetNumHeads();
	public void SetNumHeads(int sz);

	public int GetNumSectors();
	public void SetNumSectors(int sz);

	/**
	 * Get the number of logical sectors by the disks size. 
	 * 
	 * @return
	 * @throws IOException
	 */
	public int GetNumLogicalSectors();
	
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
	public void SetLogicalBlockFromSector(int SectorNum, byte result[]) throws IOException;
	
	/**
	 * Get a block of length SZ starting from the disk logical sector.
	 * 
	 * @param SectorNum
	 * @param sz
	 * @return
	 * @throws IOException
	 */
	public byte[] GetBytesStartingFromSector(int SectorNum, int sz) throws IOException;

	/**
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public Boolean IsMyFileType(File filename) throws IOException;
	
}

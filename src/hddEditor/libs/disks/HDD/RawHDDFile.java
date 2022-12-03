package hddEditor.libs.disks.HDD;

/**
 * This is a wrapper around a file with no header acting as a disk. It can be any format.
 * 
 * Note, the CHS values are not set at this level usually.
 * It basically provides some low level disk access, which can be overridden by superclasses
 * eg, to handle 8/16 bit access.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

import hddEditor.libs.PLUSIDEDOS;

public class RawHDDFile implements HardDisk {
	// File handle
	private RandomAccessFile inFile = null;
	// filename of the currently open file
	public String filename = "";
	// default sector size
	public int SectorSize = 512;
	// disk size in bytes
	public long FileSize = 0;

	// CHS information for inheriting objects to populate
	public int NumCylinders = 0;
	public int NumHeads = 0;
	public int NumSectors = 0;

	// Simple one sector cache
	private byte[] cache;
	private int cachedSector = -1;

	/**
	 * Get and set the filename
	 */
	public String GetFilename() {
		return (filename);
	}
	public void SetFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Get and set the sector size
	 */
	public int GetSectorSize() {
		return (SectorSize);
	}
	public void SetSectorSize(int sz) {
		this.SectorSize = sz;
	}

	/**
	 * Get and set the number of cylinders
	 */
	public int GetNumCylinders() {
		return (NumCylinders);
	}
	public void SetNumCylinders(int sz) {
		this.NumCylinders = sz;
	}

	/**
	 * Get the file size
	 */
	public long GetFileSize() {
		return (FileSize);
	}

	/**
	 * Get and set the number of heads
	 */
	public int GetNumHeads() {
		return (NumHeads);
	}
	public void SetNumHeads(int sz) {
		this.NumHeads = sz;
	}

	/**
	 * Get and set the number of sectors
	 */
	public int GetNumSectors() {
		return (NumSectors);
	}
	public void SetNumSectors(int sz) {
		this.NumSectors = sz;
	}

	/**
	 * Constructor specifying file to load
	 * 
	 * @param filename
	 * @throws FileNotFoundException
	 */
	public RawHDDFile(String filename) throws FileNotFoundException {
		inFile = new RandomAccessFile(filename, "rw");
		this.filename = filename;
		FileSize = new File(filename).length();
	}
	
	/**
	 * Constructor creating an unopened file object
	 */
	public RawHDDFile() {
		super();
	}

	/**
	 * ToString overridden to provide useful debug information
	 */
	@Override
	public String toString() {
		String result = "Filename: " + filename;
		result = result + "\nLogical sectors: " + GetNumLogicalSectors();
		result = result + "\nCylinders: " + NumCylinders;
		result = result + "\nHeads: " + NumHeads;
		result = result + "\nSectors: " + NumSectors;
		result = result + "\nSector size: " + SectorSize + " bytes";
		result = result + "\nFile size: " + FileSize + " bytes";
		return (result);
	}

	/**
	 * Get the number of logical sectors by the disks size.
	 * 
	 * @return
	 * @throws IOException
	 */
	public int GetNumLogicalSectors() {
		long filesize = 0;
		try {
			filesize = Files.size(Paths.get(filename));
		} catch (IOException e) {
			System.out.println("Failed to get filesize. " + e.getMessage());
		}
		long numsectors = filesize / SectorSize;
		return ((int) numsectors);
	}

	/**
	 * Close the disk, even if it doesn't want to..
	 */
	public void close() {
		if (inFile != null) {
			try {
				inFile.close();
			} catch (IOException e) {
				System.out.println("Failed to close file " + filename + " with error " + e.getMessage());
				e.printStackTrace();
			}
			inFile = null;
		}
	}

	/**
	 * Returns TRUE if the file opened correctly.
	 * 
	 * @return
	 */
	public boolean IsOpen() {
		return (inFile != null);
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		RawHDDFile h;
		try {
			h = new RawHDDFile("/data1/idedos.dsk");
			System.out.println(h);
			h.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set data from the given logical sector with the specified data. 
	 * 
	 * @param SectorNum
	 * @param result
	 * @throws IOException
	 */
	public void SetLogicalBlockFromSector(int SectorNum, byte result[]) throws IOException {
		cachedSector = -1;

		long location = SectorNum * SectorSize;
		inFile.seek(location);
		inFile.write(result);
		cachedSector = -1;

		inFile.seek(location);
		inFile.read(result);
	}

	/**
	 * Get a byte array of the specified size from the given sector number
	 * 
	 * @param SectorNum
	 * @param sz
	 * @return
	 * @throws IOException
	 */
	public byte[] GetBytesStartingFromSector(int SectorNum, int sz) throws IOException {
		if ((cachedSector == SectorNum) && (cache.length == sz)) {
			return (cache);
		}

		byte result[] = new byte[sz];
		long location = SectorNum * SectorSize;

		inFile.seek(location);
		inFile.read(result);

		cache = new byte[result.length];
		System.arraycopy(result, 0, cache, 0, cache.length);
		cachedSector = SectorNum;

		return (result);
	}

	/**
	 * Check to see if i can identify this file as one i can open...
	 * At this level, always false. 
	 * 
	 * @param filename
	 */
	@Override
	public Boolean IsMyFileType(File filename) throws IOException {
		return (false);
	}

	/**
	 * What Media type is this?
	 */
	@Override
	public int GetMediaType() {
		return PLUSIDEDOS.MEDIATYPE_HDD;
	}


}

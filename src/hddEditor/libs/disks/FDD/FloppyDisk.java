package hddEditor.libs.disks.FDD;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.disks.Disk;

public class FloppyDisk implements Disk {
	protected RandomAccessFile inFile;
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
	
	//NUmber of logical sectors
	public int NumLogicalSectors;
	
	
	// Tracks in the file.
	public TrackInfo diskTracks[] = null;
	
	/**
	 * 
	 * @param filename
	 * @throws IOException
	 * @throws BadDiskFileException
	 */
	public FloppyDisk(String filename) throws IOException, BadDiskFileException {
		inFile = new RandomAccessFile(filename, "rw");
		this.filename = filename;
		FileSize = new File(filename).length();
	}

	/**
	 * 
	 */
	public FloppyDisk()  {
		inFile = null;
		this.filename = "";
		FileSize = 0;
	}

	/**
	 * Get and set the filename
	 */
	@Override
	public String GetFilename() {
		return (filename);
	}

	@Override
	public void SetFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Get and set the sector size
	 */
	@Override
	public int GetSectorSize() {
		return (SectorSize);
	}

	@Override
	public void SetSectorSize(int sz) {
		this.SectorSize = sz;
	}

	/**
	 * Get and set the number of cylinders
	 */
	@Override
	public int GetNumCylinders() {
		return (NumCylinders);
	}

	@Override
	public void SetNumCylinders(int sz) {
		this.NumCylinders = sz;
	}

	/**
	 * Get the file size
	 */
	@Override
	public long GetFileSize() {
		return (FileSize);
	}

	/**
	 * Get and set the number of heads
	 */
	@Override
	public int GetNumHeads() {
		return (NumHeads);
	}

	@Override
	public void SetNumHeads(int sz) {
		this.NumHeads = sz;
	}

	/**
	 * Get and set the number of sectors
	 */
	@Override
	public int GetNumSectors() {
		return (NumSectors);
	}

	@Override
	public void SetNumSectors(int sz) {
		this.NumSectors = sz;
	}

	@Override
	public int GetNumLogicalSectors() {
		return NumLogicalSectors;
	}

	/**
	 * Close the disk, even if it doesn't want to..
	 */
	@Override
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
	 * Check if the file is open...
	 */
	@Override
	public boolean IsOpen() {
		return (inFile != null);
	}

	/**
	 * What Media type is this?
	 */
	@Override
	public int GetMediaType() {
		return PLUSIDEDOS.MEDIATYPE_FDD;
	}


	@Override
	public void SetLogicalBlockFromSector(int SectorNum, byte[] result) throws IOException {
		System.out.println("SetLogicalBlockFromSector not implemented for "+getClass().getName());
	}

	@Override
	public byte[] GetBytesStartingFromSector(int SectorNum, int sz) throws IOException {
		System.out.println("GetBytesStartingFromSector not implemented for "+getClass().getName());
		return null;
	}

	@Override
	public Boolean IsMyFileType(File filename) throws IOException {
		System.out.println("IsMyFileType not implemented for "+getClass().getName());
		return false;
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
		result = result + "\n       Tr/H\n";
		for (TrackInfo ti : diskTracks) {
			String trk = Integer.toHexString(ti.tracknum);
			if (trk.length() == 1) {
				trk = "0" + trk;
			}
			result = result + "Track: " + trk + "/" + Integer.toHexString(ti.side) + ": ";
			for (Sector sect : ti.Sectors) {
				result = result + Integer.toHexString(sect.sectorID) + "(" + sect.ActualSize + ") ";
			}
			result = result + "\n";
		}

		return (result);
	}
}

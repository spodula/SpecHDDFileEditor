package hddEditor.libs.disks.HDD;

/**
 * Wrapper from V1.0 and V1.1 HDF hard disks as defined by RealSpectrum by Ramsoft. 
 * 
 * https://sinclair.wiki.zxnet.co.uk/wiki/HDF_format
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import hddEditor.libs.PLUSIDEDOS;

public class RS_IDEFile implements HardDisk {
	public static final String HEADER_ID = "RS-IDE";
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

	// Simple one sector cache
	private byte[] cache;
	private long cachedSector = -1;

	// Raw header data
	private byte RawHeaderData[] = null;

	/**
	 * Constructor
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public RS_IDEFile(String filename) throws IOException {
		inFile = new RandomAccessFile(filename, "rw");
		this.filename = filename;
		FileSize = new File(filename).length();
		ParseDiskInfo();
	}

	public RS_IDEFile() {
		super();
	}

	/**
	 * Parse the RS_IDE disk and check the header
	 * 
	 * @throws IOException
	 */
	private void ParseDiskInfo() throws IOException {
		RawHeaderData = new byte[0x10];
		inFile.seek(0);
		inFile.read(RawHeaderData);

		String header = new String(RawHeaderData, StandardCharsets.UTF_8);
		if (!header.startsWith(HEADER_ID)) {
			System.out.println("File does not have a valid RS-IDE header");
			close();
			throw new IOException("File is not a valid RS-IDE file");
		} else {
			RawHeaderData = new byte[GetHDDataOffset()];
			inFile.seek(0);
			inFile.read(RawHeaderData);
			if (IsSectorHalved()) {
				SectorSize = 256;
			} else {
				SectorSize = 512;
			}
		}
	}

	/**
	 * Get the start of disk data in the file.
	 * 
	 * @return
	 */
	private int GetHDDataOffset() {
		int HDOffset = (RawHeaderData[0x09] & 0xff) + ((RawHeaderData[0x0a] & 0xff) * 0x100);
		return (HDOffset);
	}

	/**
	 * CHeck if the sector is 8 or 16 bit long
	 * 
	 * @return
	 */
	public boolean IsSectorHalved() {
		return (RawHeaderData[0x08] != 0);
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
		result = result + "\nHDD Data offset: " + GetHDDataOffset();
		String rev = Integer.toHexString(GetRevision()).charAt(0) + "." + Integer.toHexString(GetRevision()).charAt(1);
		result = result + "\nFile Revision: v" + rev;
		return (result);
	}

	/**
	 * Get the number of logical sectors by the disks size.
	 * 
	 * @return
	 * @throws IOException
	 */
	public long GetNumLogicalSectors() {
		long filesize = 0;
		try {
			filesize = Files.size(Paths.get(filename));
			filesize = filesize - GetHDDataOffset();
		} catch (IOException e) {
			System.out.println("Failed to get filesize. " + e.getMessage());
		}
		long numsectors = filesize / SectorSize;
		return (numsectors);
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
	 * Returns TRUE if the file openned correctly.
	 * 
	 * @return
	 */
	@Override
	public boolean IsOpen() {
		return (inFile != null);
	}

	/**
	 * Get the File revision.
	 * 
	 * @return
	 */
	public int GetRevision() {
		return (RawHeaderData[0x07] & 0xff);
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		RS_IDEFile h;
		try {
			h = new RS_IDEFile("/data1/IDEDOS/Workbench2.3_4Gb_8Bits.hdf");
			System.out.println(h);
			h.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set the given block of data from the given logical sector.
	 * 
	 * @param SectorNum
	 * @param result
	 */
	public void SetLogicalBlockFromSector(long SectorNum, byte result[]) throws IOException {
		cachedSector = -1;

		long location = SectorNum * SectorSize;
		location = location + GetHDDataOffset();

		inFile.seek(location);
		inFile.write(result);
		cachedSector = -1;

		inFile.seek(location);
		inFile.read(result);
	}

	/**
	 * Get the data starting from the given Logical sector
	 * 
	 * @param SectorNum
	 * @param sz
	 * @return
	 */
	public byte[] GetBytesStartingFromSector(long SectorNum, int sz) throws IOException {
		if ((cachedSector == SectorNum) && (cache.length == sz)) {
			return (cache);
		}

		byte result[] = new byte[sz];
		long location = SectorNum * SectorSize;

		location = location + GetHDDataOffset();

		inFile.seek(location);
		inFile.read(result);

		cache = new byte[result.length];
		System.arraycopy(result, 0, cache, 0, cache.length);
		cachedSector = SectorNum;

		return (result);
	}

	/**
	 * Check to see if the given file has a valid HDF header.
	 * 
	 * @param filename
	 * @return
	 */
	@Override
	public Boolean IsMyFileType(File filename) throws IOException {
		boolean result = false;

		RandomAccessFile inFile = new RandomAccessFile(filename, "rw");
		try {
			byte HeaderData[] = new byte[0x10];
			inFile.seek(0);
			inFile.read(HeaderData);
			String header = new String(HeaderData, StandardCharsets.UTF_8);
			result = header.startsWith(HEADER_ID);
		} finally {
			inFile.close();
		}
		return (result);
	}

	/**
	 * What Media type is this?
	 */
	@Override
	public int GetMediaType() {
		return PLUSIDEDOS.MEDIATYPE_HDD;
	}

}

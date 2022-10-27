package hddEditor.libs.disks;
//https://sinclair.wiki.zxnet.co.uk/wiki/HDF_format

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RS_IDEFile implements Disk {
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

	public String GetFilename() {
		return (filename);
	}

	public void SetFilename(String filename) {
		this.filename = filename;
	}

	public int GetSectorSize() {
		return (SectorSize);
	}

	public void SetSectorSize(int sz) {
		this.SectorSize = sz;
	}

	public int GetNumCylinders() {
		return (NumCylinders);
	}

	public void SetNumCylinders(int sz) {
		this.NumCylinders = sz;
	}

	public long GetFileSize() {
		return (FileSize);
	}

	public int GetNumHeads() {
		return (NumHeads);
	}

	public void SetNumHeads(int sz) {
		this.NumHeads = sz;
	}

	public int GetNumSectors() {
		return (NumSectors);
	}

	public void SetNumSectors(int sz) {
		this.NumSectors = sz;
	}

	// Simple one sector cache
	private byte[] cache;
	private int cachedSector = -1;

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
	 * 
	 * @return
	 */
	private int GetHDDataOffset() {
		int HDOffset = (RawHeaderData[0x09] & 0xff) + ((RawHeaderData[0x0a] & 0xff) * 0x100);
		return (HDOffset);
	}

	/**
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
	public int GetNumLogicalSectors() {
		long filesize = 0;
		try {
			filesize = Files.size(Paths.get(filename));
			filesize = filesize - GetHDDataOffset();
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
	 * Returns TRUE if the file openned correctly.
	 * 
	 * @return
	 */
	public boolean IsOpen() {
		return (inFile != null);
	}

	/**
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
	 * 
	 */
	public void SetLogicalBlockFromSector(int SectorNum, byte result[]) throws IOException {
		cachedSector = -1;

		long location = SectorNum * SectorSize;
		location = location + GetHDDataOffset();

		inFile.seek(location);
		inFile.write(result);
		cachedSector = -1;

//		System.out.print("Writing " + result.length + " bytes to sr:" + SectorNum + " loc:" + location + " :");
		inFile.seek(location);
		inFile.read(result);
//		for (int i = 0; i < 32; i++) {
//			System.out.print(String.format("%02x ", result[i]));
//		}
//		System.out.println();
	}

	/**
	 * 
	 */
	public byte[] GetBytesStartingFromSector(int SectorNum, int sz) throws IOException {
		if ((cachedSector == SectorNum) && (cache.length == sz)) {
			return (cache);
		}

		byte result[] = new byte[sz];
		long location = SectorNum * SectorSize;

		location = location + GetHDDataOffset();

//		System.out.print("Reading " + sz + " bytes from sr:" + SectorNum + " loc:" + location + " :");
		inFile.seek(location);
		inFile.read(result);
//		for (int i = 0; i < 32; i++) {
//			System.out.print(String.format("%02x ", result[i]));
//		}
//		System.out.println();

		cache = new byte[result.length];
		System.arraycopy(result, 0, cache, 0, cache.length);
		cachedSector = SectorNum;

		return (result);
	}

	/**
	 * Check to see if the given file has a valid HDF header.
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

}

package hddEditor.libs.disks.LINEAR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FDD.BadDiskFileException;

public class MDFMicrodriveFile implements Disk {
	protected RandomAccessFile inFile;
	// filename of the currently open file
	public File file;
	// disk size in bytes
	public long FileSize;

	public MicrodriveSector Sectors[];

	public MDFMicrodriveFile(File file) throws IOException, BadDiskFileException {
		inFile = new RandomAccessFile(file, "rw");
		this.file = file;
		FileSize = file.length();
		ParseMDFFile();
	}

	public MDFMicrodriveFile() {
		inFile = null;
		this.file = null;
		FileSize = 0;
	}

	public void ParseMDFFile() throws IOException {
		byte FileData[] = new byte[(int) FileSize];
		inFile.seek(0);
		inFile.read(FileData);

		int ptr = 0;

		ArrayList<MicrodriveSector> MdSectors = new ArrayList<MicrodriveSector>();

		while (ptr < FileData.length - 1) {
			MicrodriveSector msh = new MicrodriveSector(FileData, ptr);
			MdSectors.add(msh);
			ptr = ptr + 0x21f;
		}

		Sectors = MdSectors.toArray(new MicrodriveSector[0]);
	}

	@Override
	public int GetMediaType() {
		return PLUSIDEDOS.MEDIATYPE_LINEAR;
	}

	@Override
	public String GetFilename() {
		return (file.getAbsolutePath());
	}

	@Override
	public void SetFilename(String filename) {
		this.file = new File(filename);
	}

	@Override
	public int GetSectorSize() {
		// Microdrive sectors are 512
		return 512;
	}

	@Override
	public void SetSectorSize(int sz) {
		System.out.println("Attempt to set sector size for a microdrive file.");
	}

	@Override
	public int GetNumCylinders() {
		// Microdrives always have 1 track
		return 1;
	}

	@Override
	public void SetNumCylinders(int sz) {
		System.out.println("Attempt to set Number of Cylinders for a microdrive file.");
	}

	/**
	 * Get the file size
	 */
	@Override
	public long GetFileSize() {
		return (FileSize);
	}

	@Override
	public int GetNumHeads() {
		// Microdrives have 1 head
		return 1;
	}

	@Override
	public void SetNumHeads(int sz) {
		System.out.println("Attempt to set Number of heads for a microdrive file.");
	}

	@Override
	public int GetNumSectors() {
		return Sectors.length;
	}

	@Override
	public void SetNumSectors(int sz) {
		System.out.println("Attempt to set Number of sectors for a microdrive file.");
	}

	@Override
	public long GetNumLogicalSectors() {
		return Sectors.length;
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
				System.out.println("Failed to close file " + file.getName() + " with error " + e.getMessage());
				e.printStackTrace();
			}
			inFile = null;
		}
	}

	/**
	 *
	 * @return
	 */
	@Override
	public String toString() {
		String result = "\n Filename: " + GetFilename();
		result = result + "\n Filesize: " + GetFileSize();
		result = result + "\n Sectors: " + GetNumSectors();
		result = result + "\n Volume name: " + Sectors[0].getVolumeName();
		for (int i = 0xfe; i > 0; i--) {
			MicrodriveSector Sector = GetSectorBySectorNumber(i);
			String hs = Integer.toHexString(i);
			if (hs.length() == 1) {
				hs = "0" + hs;
			}
			result = result + "\n#" + hs + " ";

			if (Sector == null) {
				result = result + "DOesnt exist";
			} else {
				result = result + " Inuse:" + Sector.IsInUse() + " ";

				if ((Sector.GetFlagByte() % 0x04) == 0) {
					result = result + "<free>";
				} else {
					result = result + Sector.getFileName() + " part:" + Sector.getSegmentNumber() + " Flags: "
							+ Sector.getSectorFlagsAsString();
				}
			}
		}

		return (result);
	}

	@Override
	/**
	 * Check if the file is open...
	 * 
	 * @return
	 */
	public boolean IsOpen() {
		return (inFile != null);
	}

	@Override
	/**
	 * This is the base sector update function. This is probably totally useless for
	 * microdrives
	 * 
	 * @param SectorNum
	 * @param result
	 */
	public void SetLogicalBlockFromSector(long SectorNum, byte[] result) throws IOException {
		SetMDLogicalBlockFromSector(SectorNum, result, false, true, "");
	}

	/**
	 * 
	 * 
	 * @param SectorNum
	 * @param result
	 * @param SetFinal
	 * @param InUse
	 * @throws IOException
	 */
	public void SetMDLogicalBlockFromSector(long SectorNum, byte[] SourceFile, boolean SetFinal, boolean InUse,
			String filename) throws IOException {
		int numleft = SourceFile.length;
		int CurrentLoc = 0;
		byte data[] = new byte[0];
		int FileSegmentNumber = 0;
		while ((numleft > 0) && (SectorNum != 0xff) && (data != null)) {
			MicrodriveSector mds = GetSectorBySectorNumber((int) SectorNum);
			SectorNum = SectorNum - 1;
			if (mds != null) {
				data = new byte[513]; // 512 bytes + checksum
				// Copy the data to the sector
				int numbytes = Math.min(numleft, 512);
				System.arraycopy(SourceFile, CurrentLoc, data, 0, numbytes);

				System.out.println("Writing " + numbytes + " to " + SectorNum);

				// Put the sector back and re-calculate the checksum
				mds.SectorData = data;
				mds.SetRecordLength(numbytes);
				mds.UpdateFileChecksum();

				// Calculate the sector flag byte.
				int flag = 0;
				if (InUse) {
					flag = flag + 0x04;
				}
				if (SetFinal && (numleft == numbytes)) {
					flag = flag + 0x02;
				}
				mds.setSectorFlagByte(flag);

				// update the filename
				mds.setFilename(filename);
				mds.setSegmentNumber(FileSegmentNumber++);

				// Re-calcuate the header flag byte as it may have changed.
				mds.UpdateHeaderChecksum();

				// Write the sector to disk.
				mds.UpdateSectorOnDisk(this);

				// Point to the next block of data.
				CurrentLoc = CurrentLoc + numbytes;
				numleft = numleft - numbytes;
				if (SectorNum == 0) {
					SectorNum = 0xff;
				}

			}
		}
	}

	/**
	 * This probably isn't much use on a microdrive as a file is much more likely to
	 * be fragmented, but provided for consistency with other media types.
	 * 
	 * @param SectorNum
	 * @param sz
	 * @return
	 */
	@Override
	public byte[] GetBytesStartingFromSector(long SectorNum, long sz) throws IOException {
		// this is a hack to allow editing of the whole cartridge to work.
		if (SectorNum == 0)
			SectorNum = 0xfe;
		// Note sectors count backwards on Microdrives.
		// As the maximum file length of a microdrive is 128Mb, Just casting to an INT.
		byte result[] = new byte[(int) sz];

		int numleft = (int) sz;
		int ptr = 0;
		byte data[] = new byte[0];
		while ((numleft > 0) && (SectorNum > -1) && (data != null)) {
			MicrodriveSector mds = GetSectorBySectorNumber((int) SectorNum);
			SectorNum--;
			if (mds != null) {
				data = mds.SectorData;
				// make sure we don't run out of sectors.
				if (data != null) {
					int numbytes = Math.min(numleft, data.length);
					System.arraycopy(data, 0, result, ptr, numbytes);
					ptr = ptr + numbytes;
					numleft = numleft - numbytes;
				}
			}
		}
		return result;
	}

	@Override
	/**
	 * Check if the file looks like a microdrive file. Basically, load the first
	 * sector and see if it validates.
	 * 
	 * @param filename
	 * @return
	 */
	public Boolean IsMyFileType(File filename) throws IOException {
		inFile = new RandomAccessFile(filename, "r");
		try {
			byte FileData[] = new byte[0x21f];
			inFile.seek(0);
			inFile.read(FileData);
			MicrodriveSector msh = new MicrodriveSector(FileData, 0);
			if (msh.IsSectorChecksumValid()) {
				return (true);
			} 
		} finally {
			inFile.close();
			inFile = null;
		}
		return (false);
	}

	/**
	 * Get the sector by the sector number.
	 * 
	 * @param SectorNumber
	 * @return
	 */
	public MicrodriveSector GetSectorBySectorNumber(int SectorNumber) {
		for (MicrodriveSector sector : Sectors) {
			if (sector.GetSectorNumber() == SectorNumber) {
				return (sector);
			}
		}
		return (null);
	}

	/**
	 * Test.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String filename = args[0];
			MDFMicrodriveFile mdt = new MDFMicrodriveFile();
			if (mdt.IsMyFileType(new File(filename))) {
				System.out.println("File is a valid microdrive file.");
				mdt = new MDFMicrodriveFile(new File(filename));
				System.out.println(mdt);
			} else {
				System.out.println("File is not a valid microdrive file.");
			}

		} catch (IOException | BadDiskFileException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create and load a blank microdrive cart
	 * 
	 * @param Filename
	 * @param title
	 * @throws IOException
	 */
	public void CreateBlankMicrodriveCart(File file, String VolumeName) throws IOException {
		// b5 to 01
		// Create a file with lots of blank sectors.
		FileOutputStream NewFile = new FileOutputStream(file);
		try {
			for (int SectorNum = 0xb5; SectorNum > 0; SectorNum--) {
				byte SectorData[] = new byte[0x21f];
				// Initialise the sector information
				MicrodriveSector msh = new MicrodriveSector(SectorData, 0);
				msh.setVolumeName(VolumeName);
				msh.setSectorFlagByte(0x00);
				msh.SetSectorNumber(SectorNum);
				msh.setSegmentNumber(0);
				msh.setHeaderChecksum(msh.CalculateHeaderChecksum());
				msh.setSectorChecksum(msh.CalculateSectorChecksum());
				// write our new sector information to disk.
				NewFile.write(msh.SectorHeader);
				NewFile.write(msh.SectorData);
			}
		} finally {
			// Close, forcing flush
			NewFile.close();
			NewFile = null;
		}
		// Load the newly created file.
		this.file = file;
		inFile = new RandomAccessFile(file, "rw");
		FileSize = file.length();
		ParseMDFFile();
	}

}

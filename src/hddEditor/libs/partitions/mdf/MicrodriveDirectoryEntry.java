package hddEditor.libs.partitions.mdf;

import java.io.IOException;

import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.disks.LINEAR.MDFMicrodriveFile;
import hddEditor.libs.disks.LINEAR.MicrodriveSector;

public class MicrodriveDirectoryEntry implements FileEntry {
	// Filename
	private String filename;
	// List of sectors.
	public MicrodriveSector sectors[];

	/**
	 * Create without initialising
	 */
	public MicrodriveDirectoryEntry() {
		sectors = new MicrodriveSector[0];
	}

	/**
	 * Get the filename
	 * 
	 * @return
	 */
	public String GetFilename() {
		return (filename);
	}

	/**
	 * Set the filename
	 * 
	 * @param fn
	 */

	public void SetFilename(String fn) {
		filename = (fn + "          ").substring(0, 10);
	}

	/**
	 * This will get the entire microdrive file including the 9 byte header.
	 * 
	 * @return
	 */
	public byte[] GetFileRawData() {
		int bytesrenamining = GetRawFileSize();
		byte result[] = new byte[bytesrenamining];

		int ptr = 0;
		for (int i = 0; i < sectors.length; i++) {
			MicrodriveSector s0 = GetSectorByFilePartNumber(i);
			int bytes = Math.min(0x200, bytesrenamining);
			System.arraycopy(s0.SectorData, 0, result, ptr, bytes);
			ptr = ptr + bytes;
			bytesrenamining = bytesrenamining - bytes;
		}
		return (result);
	}

	/**
	 * Get the data without the header
	 * 
	 * @return
	 */
	public byte[] GetFileData() {
		byte dat[] = GetFileRawData();
		byte result[] = new byte[dat.length - 9];
		System.arraycopy(dat, 9, result, 0, result.length);

		return (result);

	}

	/**
	 * Add a sector to the sector list for this directory entry.
	 * 
	 * @param sector
	 */
	public void AddSector(MicrodriveSector sector) {
		MicrodriveSector newSectorList[] = new MicrodriveSector[sectors.length + 1];
		for (int i = 0; i < sectors.length; i++) {
			newSectorList[i] = sectors[i];
		}
		newSectorList[sectors.length] = sector;
		sectors = newSectorList;
	}

	/**
	 * Get sector number within the file.
	 * 
	 * @param filepartNumber
	 * @return
	 */
	public MicrodriveSector GetSectorByFilePartNumber(int filepartNumber) {
		for (MicrodriveSector sector : sectors) {
			if (sector.getSegmentNumber() == filepartNumber) {
				return (sector);
			}
		}
		return (null);
	}

	/**
	 * Extract file file length from the basic header including the basic header
	 * 
	 * @return
	 */
	@Override
	public int GetRawFileSize() {
		MicrodriveSector s0 = GetSectorByFilePartNumber(0);
		return ((int) (s0.SectorData[1] & 0xff) + ((s0.SectorData[2] & 0xff) * 0x100) + 0x09);
	}

	/**
	 * 
	 * @return
	 */
	@Override
	public int GetFileSize() {
		MicrodriveSector s0 = GetSectorByFilePartNumber(0);
		return ((int) (s0.SectorData[1] & 0xff) + ((s0.SectorData[2] & 0xff) * 0x100));
	}

	public void SetFileSize(int size) {
		MicrodriveSector s0 = GetSectorByFilePartNumber(0);
		s0.SectorData[1] = (byte) ((size % 0x100) & 0xff);
		s0.SectorData[2] = (byte) ((size / 0x100) & 0xff);
	}

	/**
	 * Get the variable from the basic header.
	 * 
	 * @return
	 */
	public int GetVar2() {
		MicrodriveSector s0 = GetSectorByFilePartNumber(0);
		return ((int) (s0.SectorData[3] & 0xff) + ((s0.SectorData[4] & 0xff) * 0x100));
	}

	public void RenameMicrodriveFile(String to, Disk CurrentDisk) {
		MDFMicrodriveFile mdf = (MDFMicrodriveFile) CurrentDisk;

		for (MicrodriveSector Sector : sectors) {
			Sector.setFilename(to);
			Sector.CalculateHeaderChecksum();
			try {
				Sector.UpdateSectorOnDisk(mdf);
			} catch (IOException e) {
				System.out.println("Cannot Set file on  cartrage. Is it read-only?");
				e.printStackTrace();
			}
		}
		SetFilename(to);
	}

	/**
	 * This will get the entire microdrive file including the 9 byte header.
	 * 
	 * @return
	 * @throws IOException
	 */
	public void SetFileRawData(byte dat[], MDFMicrodriveFile mdf) throws IOException {
		int CurrentSectors = sectors.length;
		int newsectors = dat.length / 512;
		if ((dat.length % 512) != 0) {
			newsectors++;
		}

		// Add extra sectors if needed

		if (newsectors > CurrentSectors) {
			MicrodriveSector LastSector = GetSectorByFilePartNumber(sectors.length - 1);
			int lastsectorid = LastSector.GetSectorNumber();
			for (int i = lastsectorid - 1; (i > 0) && (newsectors != CurrentSectors); i--) {
				MicrodriveSector mds = mdf.GetSectorBySectorNumber(i);
				if (!mds.IsInUse()) {
					// claim the sector.
					mds.setFilename(this.GetFilename());
					mds.setSectorFlagByte(0x06);
					mds.setSegmentNumber(LastSector.getSegmentNumber() + 1);
					mds.UpdateSectorChecksum();
					mds.UpdateHeaderChecksum();
					AddSector(mds);

					// last sector is no longer the final sector.
					LastSector.setSectorFlagByte(0x04);
					LastSector.UpdateSectorChecksum();
					LastSector.UpdateHeaderChecksum();

					LastSector = mds;
					CurrentSectors++;

				}
			}
		}
		// subtract sectors if needed
		while (newsectors < CurrentSectors) {
			MicrodriveSector LastSector = GetSectorByFilePartNumber(sectors.length - 1);
			LastSector.setSectorFlagByte(0x00);
			LastSector.setFilename("");
			LastSector.UpdateSectorChecksum();
			LastSector.UpdateHeaderChecksum();

			MicrodriveSector NewSectorList[] = new MicrodriveSector[sectors.length - 1];
			for (int i = 0; i < NewSectorList.length; i++) {
				NewSectorList[i] = sectors[i];
			}
			sectors = NewSectorList;
		}

		// mark the new last sector.
		MicrodriveSector LastSector = GetSectorByFilePartNumber(sectors.length - 1);
		LastSector.setSectorFlagByte(0x06);
		LastSector.UpdateSectorChecksum();
		LastSector.UpdateHeaderChecksum();

		// Copy data to sectors.
		int bytesleft = dat.length;
		int ptr = 0;
		for (int i = 0; i < sectors.length; i++) {
			MicrodriveSector Sector = GetSectorByFilePartNumber(i);
			byte newdata[] = new byte[513]; // 512 bytes + checksum

			// Copy the data to the sector
			int numbytes = Math.min(bytesleft, 512);
			System.arraycopy(dat, ptr, newdata, 0, numbytes);

			// Put the sector back and re-calculate the checksum
			Sector.SectorData = newdata;
			Sector.SetRecordLength(numbytes);

			Sector.UpdateFileChecksum();

			// Calculate the sector flag byte.
			int flag = 0x04;
			if (bytesleft == numbytes) {
				flag = flag + 0x02;
			}
			Sector.setSectorFlagByte(flag);

			// Re-calcuate the header flag byte as it may have changed.
			Sector.UpdateHeaderChecksum();
			Sector.UpdateFileChecksum();
			// Write the sector to disk.
			Sector.UpdateSectorOnDisk(mdf);

			// Point to the next block of data.
			ptr = ptr + numbytes;
			bytesleft = bytesleft - numbytes;
		}
		// Update the file size.
		int NewFileSize = dat.length - 9;
		SetFileSize(NewFileSize);
		MicrodriveSector Sector = GetSectorByFilePartNumber(0);
		Sector.UpdateFileChecksum();
		Sector.UpdateSectorOnDisk(mdf);

	}

	@Override
	public boolean DoesMatch(String wildcard) {
		String StringToMatch = GetFilename().toUpperCase();
		// convert the wildcard into a search array:
		// Split into filename and extension. pad out with spaces.
		wildcard = wildcard.trim().toUpperCase();
		wildcard = wildcard + "            ";

		// create search array.
		byte comp[] = new byte[10];

		// populate with filename
		boolean foundstar = false;
		for (int i = 0; i < 10; i++) {
			if (foundstar) {
				comp[i] = '?';
			} else {
				char c = wildcard.charAt(i);
				if (c == '*') {
					foundstar = true;
					comp[i] = '?';
				} else {
					comp[i] = (byte) ((int) c & 0xff);
				}
			}
		}

		StringToMatch = (StringToMatch + "          ").substring(0, 10);
		// now search.
		// check the filename
		boolean match = true;
		for (int i = 0; i < 10; i++) {
			byte chr = (byte) StringToMatch.charAt(i);
			byte cchr = comp[i];
			if ((chr != cchr) && (cchr != '?')) {
				match = false;
			}
		}
		return (match);
	}

	/**
	 * 
	 */
	@Override
	public String GetFileTypeString() {
		return(GetSpeccyBasicDetails().BasicTypeString());
	}

	@Override
	public SpeccyBasicDetails GetSpeccyBasicDetails() {
		MicrodriveSector s0 = GetSectorByFilePartNumber(0);
		int ArrayVar = (s0.SectorData[5] & 0x2f) + 'A';
		int VarStart = (int) (s0.SectorData[5] & 0xff) + ((s0.SectorData[6] & 0xff) * 0x100);
		int LineStart =(int) (s0.SectorData[7] & 0xff) + ((s0.SectorData[8] & 0xff) * 0x100);
		int FileType = (int) (s0.SectorData[0] & 0xff);
		SpeccyBasicDetails result = new SpeccyBasicDetails(FileType, VarStart, LineStart, GetVar2(), (char) ArrayVar );
		return (result);
	}
	

	
}

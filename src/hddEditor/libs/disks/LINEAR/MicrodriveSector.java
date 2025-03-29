package hddEditor.libs.disks.LINEAR;

/**
 * From: https://sinclair.wiki.zxnet.co.uk/wiki/ZX_Interface_1
 *
 * A microdrive sector is 543 bytes. (0x21f) Encoding 30 bytes of header, 512 bytes of data and a 1 byte checksum
 *
 * Each sector is laid out in the file as follows:
 *
 * +-------+------------------------------------------------------------------------------+
 * | byte  | Use                                                                          |
 * +-------+------------------------------------------------------------------------------+
 * |000    | Sector flag byte Bit0 = sector header.                                       |
 * |001    | Sector number - Sectors count down from 0xFF                                 |
 * |002    | Unused                                                                       |
 * |003    | Unused                                                                       |
 * |004-00D| Cart name. Note this is repeated for each sector. - ASCII padded with spaces |
 * |00E    | Sector header checksum                                                       | 
 * |00F    | RECFLAG2 - Bit2 = IN USE, Bit1 = Last block in file.                         |
 * |010    | Record segment number.- Sequential integer                                   |
 * |011-012| Record length (LSB first)                                                    |
 * |013-01C| Filename. ASCII padded with spaces                                           |
 * |01D    | File Header checksum                                                         |
 * |01E-21D| 512 bytes sector data                                                        |
 * |21E    | file data checksum                                                           |
 * +-------+------------------------------------------------------------------------------+
**/

import java.io.IOException;

public class MicrodriveSector {
	// This contains the header
	public byte SectorHeader[] = new byte[0x1e];
	// Sector data + 1 byte checksum
	public byte SectorData[] = new byte[0x201];
	// location of the sector in the file.
	public int SectorLocation = 0;

	/**
	 * create a new microdrive sector.
	 * 
	 * @param data
	 * @param ptr
	 */
	public MicrodriveSector(byte data[], int ptr) {
		SectorLocation = ptr;
		System.arraycopy(data, ptr, SectorHeader, 0, 0x1e);
		System.arraycopy(data, ptr + 0x1e, SectorData, 0, 0x201);
	}

	/**
	 * ToString updated for microdrive sectors.
	 */
	@Override
	public String toString() {
		String result = "\n Flag byte: " + GetFlagByte();
		result = result + "\n Sector number: " + GetSectorNumber();
		result = result + "\n Volume name: " + getVolumeName();
		result = result + "\n Header Checksum: " + getHeaderChecksum();
		result = result + "\n Header Checksum valid: " + IsHeaderChecksumValid();
		result = result + "\n Sector Checksum: " + getSectorChecksum();
		result = result + "\n Sector Checksum valid: " + IsSectorChecksumValid();
		result = result + "\n File flag byte: " + getSectorFlagByte();
		result = result + "\n Segment number in file: " + getSegmentNumber();
		result = result + "\n Record length: " + getRecordLength();
		result = result + "\n Filename: " + getFileName();
		result = result + "\n File checksum: " + getFileChecksum();
		result = result + "\n File checksum Valid?: " + IsFileChecksumValid();
		result = result + "\n Sector flags: " + getSectorFlagsAsString();

		return (result);
	}

	/**
	 * Get the sector flag byte
	 * 
	 * @return
	 */
	public byte GetFlagByte() {
		return (SectorHeader[0]);
	}

	/**
	 * Get the raw sector number
	 * 
	 * @return
	 */
	public int GetSectorNumber() {
		return ((int) (SectorHeader[1] & 0xff));
	}

	public void SetSectorNumber(int newnum) {
		SectorHeader[1] = (byte) (newnum & 0xff);
	}

	/**
	 * Get the volume name from the header.
	 * 
	 * @return
	 */
	public String getVolumeName() {
		String result = "";
		result = new String(SectorHeader).substring(4, 14);
		return (result);
	}

	/**
	 * 
	 * @param newname
	 */
	public void setVolumeName(String newname) {
		newname = newname + "           ";
		for (int i = 0; i < 10; i++) {
			byte b = (byte) newname.charAt(i);
			SectorHeader[i + 4] = b;
		}
	}

	/**
	 * re-write a sector to the microdrive file
	 * 
	 * @param cart
	 * @throws IOException
	 */
	public void UpdateSectorOnDisk(MDFMicrodriveFile cart) throws IOException {
		cart.inFile.seek(SectorLocation);
		cart.inFile.write(SectorHeader);
		cart.inFile.write(SectorData);
		cart.UpdateLastModified();
	}

	/**
	 * Get the header checksum byte
	 * 
	 * @return
	 */
	public int getSectorChecksum() {
		return ((int) (SectorHeader[14] & 0xff));
	}

	public void setSectorChecksum(int cs) {
		SectorHeader[14] = (byte) (cs & 0xff);
	}

	public void UpdateSectorChecksum() {
		int headercs = CalculateSectorChecksum();
		setSectorChecksum(headercs);
	}

	/**
	 * Get the sector flag bytes
	 * 
	 * @return
	 */
	public int getSectorFlagByte() {
		return ((int) (SectorHeader[15] & 0xff));
	}

	/**
	 * Set the flag byte. Valid flags are: 0x04 (In use), 0x02 (Final)
	 * 
	 * @param flags
	 */
	public void setSectorFlagByte(int flags) {
		SectorHeader[15] = (byte) (flags & 0xff);
	}

	/**
	 * Get the sector flags as a string.
	 * 
	 * @return
	 */
	public String getSectorFlagsAsString() {
		String result = "";
		int flags = getSectorFlagByte();
		if ((flags & 0x04) == 0) {
			result = "UNUSED";
		} else {
			result = "INUSE";
			if ((flags & 0x02) != 0) {
				result = result + " FINAL";
			}
		}
		return (result);
	}

	/**
	 * Get the "IN USE" flag.
	 * 
	 * @return
	 */
	public boolean IsInUse() {
		int flags = getSectorFlagByte();
		return ((flags & 0x04) != 0);
	}

	/**
	 * Get the number of the sector in the file.
	 * 
	 * @return
	 */
	public int getSegmentNumber() {
		return ((int) (SectorHeader[16] & 0xff));
	}

	public void setSegmentNumber(int num) {
		SectorHeader[16] = (byte) (num & 0xff);
	}

	/**
	 * Get the length of the record.
	 * 
	 * @return
	 */
	public int getRecordLength() {
		return ((int) (SectorHeader[17] & 0xff) + ((SectorHeader[18] & 0xff) * 256));
	}

	/**
	 * Set the microdrive record length
	 * 
	 * @param rl
	 */
	public void SetRecordLength(int rl) {
		SectorHeader[17] = (byte) ((rl % 0x100) & 0xff);
		SectorHeader[18] = (byte) ((rl / 0x100) & 0xff);
	}

	/**
	 * Get the filename from the header
	 * 
	 * @return
	 */
	public String getFileName() {
		String result = "";
		result = new String(SectorHeader).substring(19, 29);
		return (result);
	}

	/**
	 * Set the filename in the header.
	 * 
	 * @param filename
	 */
	public void setFilename(String filename) {
		filename = filename + "          ";
		for (int i = 0; i < 10; i++) {
			byte b = (byte) filename.charAt(i);
			SectorHeader[i + 19] = b;
		}
	}

	/**
	 * Get the checksum byte for the file.
	 * 
	 * @return
	 */
	public int getFileChecksum() {
		return ((int) (SectorData[0x200] & 0xff));
	}

	public void UpdateFileChecksum() {
		int checksum = CalculateFileChecksum();
		SectorData[0x200] = (byte) (checksum & 0xff);
	}

	/**
	 * Get the header checksum byte
	 * 
	 * @return
	 */
	public int getHeaderChecksum() {
		return ((int) (SectorHeader[29] & 0xff));
	}

	public void setHeaderChecksum(int cs) {
		SectorHeader[29] = (byte) (cs & 0xff);
	}

	public void UpdateHeaderChecksum() {
		int headercs = CalculateHeaderChecksum();
		setHeaderChecksum(headercs);
	}

	/**
	 * Calculate the header checksum.
	 * 
	 * @return
	 */
	public int CalculateHeaderChecksum() {
		int csum = 0;
		for (int i = 15; i < 29; i++) {
			csum = csum + (SectorHeader[i] & 0xff);
			if (csum == 0xff) {
				csum = 0;
			}
			if (csum > 0xff) {
				csum = (csum & 0xff) + 1;
			}
		}
		return (csum);
	}

	/**
	 * Calculate what the header checksum should be and return it.
	 * 
	 * @return
	 */
	public int CalculateSectorChecksum() {
		int csum = 0;
		for (int i = 0; i < 14; i++) {
			csum = csum + (SectorHeader[i] & 0xff);
			if (csum == 0xff) {
				csum = 0;
			}
			if (csum > 0xff) {
				csum = (csum & 0xff) + 1;
			}
		}
		return (csum);
	}

	/**
	 * Check the checksum of the header.
	 * 
	 * @return
	 */
	public boolean IsSectorChecksumValid() {
		return (getSectorChecksum() == CalculateSectorChecksum());
	}

	/**
	 * Calculate the checksum of the sector data
	 * 
	 * @return
	 */
	public int CalculateFileChecksum() {
		int csum = 0;
		for (int i = 0; i < 0x200; i++) {
			csum = csum + (SectorData[i] & 0xff);
			if (csum == 0xff) {
				csum = 0;
			}
			if (csum > 0xff) {
				csum = (csum & 0xff) + 1;
			}
		}
		return (csum);
	}

	/**
	 * Check the checksum of the sector data.
	 * 
	 * @return
	 */
	public boolean IsFileChecksumValid() {
		return (getFileChecksum() == CalculateFileChecksum());
	}

	/**
	 * Check the checksum of the header data
	 * 
	 * @return
	 */

	public boolean IsHeaderChecksumValid() {
		return (getHeaderChecksum() == CalculateHeaderChecksum());
	}
}

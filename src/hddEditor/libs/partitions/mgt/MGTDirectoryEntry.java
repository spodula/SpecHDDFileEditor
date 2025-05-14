package hddEditor.libs.partitions.mgt;
/**
 * Directory entry format. (from https://sinclair.wiki.zxnet.co.uk/wiki/MGT_filesystem)
 * Undefined fields should generally be set to $ff.
 *
 * Offset				Description
 * 0       ($00)		Bits 0–4: file type (zero for erased — see below for a list of types), bit 6: protected, bit 7: hidden.
 * 1–10    ($01–$0a)	Filename, padded with spaces. If the first byte of the filename is zero, this may be taken as an end-of-directory marker. Note that '/', '\' and '^' are allowed, but are used for directories under Master DOS.
 * 11–12   ($0b–$0c)	Number of sectors used by the file, in big-endian format (i.e. MSB first).
 * 13–14   ($0d–$0e)	Address of the first sector in the file — track number (0–79, 128–207) followed by sector number (1–10). For a Master DOS subdirectory, the address of the sector holding the directory entry is used.
 * 15–209  ($0f–$d1)	Sector address map (195 bytes) — a bit is set in this map if the corresponding sector is allocated to the file. The lsb of byte 0 corresponds to track 4, sector 1. The msb of byte 0 corresponds to track 4, sector 8. The lsb of byte 1 corresponds to track 4, sector 9. The msb of byte 1 corresponds to track 5, sector 6.
 * 210–219 ($d2–$db)	DISCiPLE / +D file information, or an 10-character Master DOS disk label (in slot 1) — if the first character in the label is '*', this specifies a blank label. SAMDOS sets these bytes to 0 for SAM CODE files.
 * 220–231 ($dc–$e7)	SAM file information. For SAM CODE files, SAMDOS sets these to $20, except for the last byte, which is set to $ff.
 * 220–241 ($dc–$f1)	Snapshot register dump: IY, IX, DE', BC', HL', AF', DE, BC, HL, junk, I, SP. On the stack: F indicating IFF, R, AF, PC. If I holds 0 or 63, IM 1 is set, otherwise, IM 2 is set. On the DISCiPLE, there is a bug (a missing EX AF,AF' instruction) that causes AF' not to be saved.
 * 232–235 ($e8–$eb)	Spare bytes under SAMDOS, set to $ff.
 * 236–244 ($ec–$f4)	SAM file start/length information.
 * 245–249 ($f5–$f9)	Master DOS timestamp — day, month, year, hour and minute. $ff under SAMDOS.
 * 250 	   ($fa)	    ID of Master DOS subdirectory (1–254). $ff under SAMDOS.
 * 251     ($fb)	    $ff (unused) under SAMDOS.
 * 252–253 ($fc–$fd)	Randomly generated Master DOS disks ID (in slot 1). Possibly used under SAMDOS for some purpose.
 * 254     ($fe)	    ID of Master DOS directory containing this entry (0 for root directory). Possibly used under SAMDOS for some purpose.
 * 255     ($ff)	    Number of extra tracks for the root directory (0–35), under Beta DOS and Master DOS (in slot 1). Possibly used under SAMDOS for some purpose. 
 * 
 * DISCiPLE and +D file information
 * Information for ZX Spectrum files is stored at offsets 210–219 of a file's directory entry. For snapshot files, a register dump is stored at offsets 220–241. Bytes 211–219 are also saved as the file header for some file types.
 * 
 * Offset	Description
 * 210     ($d2)	    For opentype files, the number of 64K blocks in the file.
 * 211     ($d3)	    Tape header ID for ZX Spectrum files: 0 for BASIC, 1 for numeric arrays, 2 for string arrays and 3 for code.
 * 212–213 ($d4–$d5)	File length. For opentype files, the length of the last block.
 * 214–215 ($d6–$d7)	Start address.
 * 216–217 ($d8–$d9)	Type-specific.
 * 218–219 ($da–$db)	Autostart line/address.
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import hddEditor.libs.MGT;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.partitions.MGTDosPartition;

public class MGTDirectoryEntry implements FileEntry {
	public byte RawDirectoryEntry[] = null;
	public int DirentNum = 0;
	public int DirentLoc = 0;
	private Disk CurrentDisk = null;

	public MGTDirectoryEntry(Disk CurrentDisk, int DirentNum, byte Dirent[], boolean initialise, int DirentLoc)
			throws IOException {
		super();
		if (initialise) {
			RawDirectoryEntry = new byte[0x100];
		} else {
			RawDirectoryEntry = Dirent;
		}
		this.DirentNum = DirentNum;
		this.DirentLoc = DirentLoc;
		this.CurrentDisk = CurrentDisk;
	}

	@Override
	public String GetFilename() {
		String result = "";
		if (RawDirectoryEntry[0] != MGT.MGTFT_ERASED) {
			result = new String(RawDirectoryEntry, StandardCharsets.UTF_8).substring(1, 11).trim();
		}
		return (result.trim());
	}

	@Override
	public void SetFilename(String filename) throws IOException {
		filename = filename + "               ";
		for (int i = 1; i < 11; i++) {
			RawDirectoryEntry[i] = (byte) filename.charAt(i - 1);
		}
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

	@Override
	public int GetRawFileSize() {
		if (GetFileType() == 0) {
			return (0);
		} else {
			int NumSectors = GetNumSectors();
			return (NumSectors * CurrentDisk.GetSectorSize());
		}
	}

	@Override
	public int GetFileSize() {
		if ((GetFileType() > 7) || (GetFileType() == MGT.MGTFT_ZX48SNA )) {
			return (GetRawFileSize());
		} else {
			int filelen = ((RawDirectoryEntry[213] & 0xff) * 256) + (RawDirectoryEntry[212] & 0xff);
			return filelen;
		}
	}

	public int GetFileType() {
		return RawDirectoryEntry[0] & 0x1f;
	}
	
	public void SetFileType(int ftype) {
		RawDirectoryEntry[0] = (byte) (ftype & 0x1f);
	}

	@Override
	public String GetFileTypeString() {
		String result = "Unknown";

		int filetype = GetFileType();
		if (filetype < 32) {
			result = MGT.MGTFileTypes[filetype];
		}
		return result;
	}

	@Override
	public SpeccyBasicDetails GetSpeccyBasicDetails() {
		int VarStart = GetVar1();
		int LoadAddress = GetLoadAddress();
		int vid = ((LoadAddress / 0x100) & 0x1f) + 0x40;
		char ArrayVar = (char) vid;
		SpeccyBasicDetails result = new SpeccyBasicDetails(GetFileType(), VarStart, GetStartLine(), LoadAddress,
				ArrayVar);
		return (result);
	}

	public int GetVar1() {
		return (RawDirectoryEntry[217] & 0xff) * 256 + (RawDirectoryEntry[216] & 0xff);
	}
	public void SetVar1(int var1) {
		RawDirectoryEntry[217] = (byte) ((var1 & 0xff00) >> 8);
		RawDirectoryEntry[216] = (byte) (var1 & 0xff);		
	}

	public int GetLoadAddress() {
		return (RawDirectoryEntry[215] & 0xff) * 256 + (RawDirectoryEntry[214] & 0xff);
	}
	
	public void SetLoadAddress(int LoadAddress) {
		RawDirectoryEntry[215] = (byte) ((LoadAddress & 0xff00) >> 8);
		RawDirectoryEntry[214] = (byte) (LoadAddress & 0xff);		
	}

	public int GetStartLine() {
		return (RawDirectoryEntry[219] & 0xff) * 256 + (RawDirectoryEntry[218] & 0xff);
	}
	
	public void SetStartLine(int newsl) {
		RawDirectoryEntry[219] = (byte) ((newsl & 0xff00) >> 8);
		RawDirectoryEntry[218] = (byte) (newsl & 0xff);
	}

	public boolean GetProtected() {
		return ((RawDirectoryEntry[0] & 0x40) != 0);
	}

	public boolean GetHidden() {
		return ((RawDirectoryEntry[0] & 0x80) != 0);
	}

	@Override
	public byte[] GetFileData() throws IOException {
		try {
		//note, this may look wierd, but GetFileRawData() returns the sector data, which will be larger than the actual file size.

		byte data[] = new byte[GetFileSize()];
		byte rawdata[] = GetFileRawData();
		//For ZX file types (1-4) strip header
		if ((GetFileType() > 7) || (data.length == rawdata.length)) {
			System.arraycopy(rawdata, 0, data, 0, data.length);
		} else {
			System.arraycopy(rawdata, 9, data, 0, data.length);
		}
		return data;
		} catch (NullPointerException E) {
			System.out.println("No data for filename: "+GetFilename());
			return (null);
		}
	}

	/**
	 * This version of the function generates the list of sectors from the sector
	 * map. it should *ONLY* be used for generating a BAM, as there it no guarantee
	 * the list it provides will be in the correct order. Its just a heck of a lot
	 * quicker when loading a disk.
	 * 
	 * @return
	 * @throws IOException
	 */
	public int[] GetLogicalSectorsFromDirents() throws IOException {
		// get the list of sectors
		int sectors[] = new int[GetNumSectors()];
		int sPtr = 0;
		int CurrentLogicalSector = 4 * CurrentDisk.GetNumSectors();
		for (int BlkUsageMap = 0x0f; BlkUsageMap < 0xd2; BlkUsageMap++) {
			int sectormask = RawDirectoryEntry[BlkUsageMap] & 0xff;
			for (int bit = 0; bit < 8; bit++) {
				if ((sectormask & 0x01) == 0x01) {
					if (sectors.length >= sPtr) {
						sectors[sPtr++] = CurrentLogicalSector;
					}
				}
				sectormask = sectormask / 2;
				CurrentLogicalSector++;
			}
		}
		if (sPtr != GetNumSectors()) {
			System.out.println("Warning, active sectors in Sector address map (" + sPtr
					+ ") doesnt match Dirent Count (" + GetNumSectors() + ") for file:" + GetFilename() + " .");
		}
		return (sectors);
	}

	/**
	 * This function returns a list of logical sectors for a file in the order they
	 * are used.
	 * 
	 * @return
	 * @throws IOException
	 */
	public int[] GetLogicalSectors() throws IOException {
		// get the list of sectors
		int sectors[] = new int[GetNumSectors()];
		int sPtr = 0;
		int NextTrack = (RawDirectoryEntry[0x0d] & 0xff);
		int NextSector = (RawDirectoryEntry[0x0e] & 0xff);
		for (int i = 0; i < GetNumSectors(); i++) {
			int LogicalSector = (NextTrack * CurrentDisk.GetNumSectors()) + NextSector - 1;
			if (LogicalSector == -1) {
				System.out.println("sector of -1 found. Stopping.");
				break;
			}
			sectors[sPtr++] = LogicalSector;
			byte sect[] = CurrentDisk.GetBytesStartingFromSector(LogicalSector, 512);
			if (sect == null) {
				System.out.println("Trying to fetch Logical sector: " + LogicalSector + " returns null");
			}
			NextTrack = (sect[510] & 0xff);
			NextSector = (sect[511] & 0xff);
		}
		return (sectors);
	}

	public int GetNumSectors() {
		return ((RawDirectoryEntry[11] & 0xff) * 256) + (RawDirectoryEntry[12] & 0xff);
	}

	@Override
	public byte[] GetFileRawData() throws IOException {
		if (GetFileType() == 0) {
			return (null);
		}
		int sectors[] = GetLogicalSectors();
		// Now extract the sectors
		byte rawdata[] = new byte[GetNumSectors() * CurrentDisk.GetSectorSize()];
		int ptr = 0;
		for (int LogicalSector : sectors) {
			byte data[] = CurrentDisk.GetBytesStartingFromSector(LogicalSector, CurrentDisk.GetSectorSize());
			System.arraycopy(data, 0, rawdata, ptr, CurrentDisk.GetSectorSize() - 2);
			ptr = ptr + CurrentDisk.GetSectorSize() - 2;
		}

		return rawdata;
	}

	public int GetStartSector() {
		return (int) (RawDirectoryEntry[0x0e] & 0xff);
	}

	public int GetStartTrack() {
		return (int) (RawDirectoryEntry[0x0d] & 0xff);
	}
	
	/**
	 * Update the dirent from the SpeccyBasicDetails.
	 * Note that this will not change filesize
	 * 
	 * @param sbd
	 * @param Part
	 */
	public void SetSpeccyBasicDetails(SpeccyBasicDetails sbd, MGTDosPartition Part) {
		switch (sbd.BasicType) {
		case Speccy.BASIC_BASIC:
			SetVar1(sbd.VarStart);
			SetStartLine(sbd.VarStart);
			break;
		case Speccy.BASIC_NUMARRAY:
			break;
		case Speccy.BASIC_CHRARRAY:
			break;
		case Speccy.BASIC_CODE:
			SetLoadAddress(sbd.LoadAddress);
		default:
			break;
		}
		try {
			Part.SaveDirectoryEntry(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}


package hddEditor.libs.partitions.trdos;

/**
 * Implementation of TR-DOS directory entry.
 * File system entries are 16 bytes long:
 * 
 * 00-07  Filename padded with spaces
 * 08     File type (B,C,D#)
 * 09     Var 1 LSB 
 * 0A     Var 1 MSB
 * 0B     Var 2 LSB
 * 0C     Var 2 MSB
 * 0D     Number of sectors
 * 0E     Start sector
 * 0F     Start (logical) track
 * 
 * Basic (B): 
 * 	 Var 1 = Program+ Vars length
 *   Var 2 = Start of vars area
 * Code (C): 
 *   Var 1 = Start address
 *   Var 2 = Default load address
 * sequential files (#)
 *   Var 1 LSb = Extent no...
 *   Var 1 MSB = 0
 *   Var 2 = length
 * Other files:
 *   Var 1 = reserved
 *   Var 2 = length
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import hddEditor.libs.Speccy;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.partitions.TrDosPartition;

public class TrdDirectoryEntry implements FileEntry {
	public byte DirEntryDescriptor[] = null;
	public int DirentNum = 0;
	public int DirentLoc = 0;
	public int startline = 0;
	private Disk CurrentDisk = null;

	/**
	 * Create a new TR-DOS directory entry
	 * 
	 * @param DirentNum
	 * @param Dirent
	 * @param initialise
	 * @throws IOException
	 */
	public TrdDirectoryEntry(Disk CurrentDisk, int DirentNum, byte Dirent[], boolean initialise, int DirentLoc)
			throws IOException {
		super();
		if (initialise) {
			DirEntryDescriptor = new byte[0x0f];
		} else {
			DirEntryDescriptor = Dirent;
		}
		this.DirentNum = DirentNum;
		this.DirentLoc = DirentLoc;
		this.CurrentDisk = CurrentDisk;
		/**
		 * For BASIC files, extract the line from the file.
		 */
		if ((CurrentDisk != null) && (this.GetFileType() == 'B')) {
			long startsector = (GetStartTrack() * CurrentDisk.GetNumSectors()) + GetStartSector();
			// Want the 10 bytes past the EOF
			int itemlength = GetFileSize() + 0x10;
			// Reduce the amount we have to read. Keep incrementing the sector until there
			// is only one or two sectors left.
			while (itemlength > 512) {
				itemlength = itemlength - CurrentDisk.GetSectorSize();
				startsector++;
			}

			byte result[] = CurrentDisk.GetBytesStartingFromSector(startsector, itemlength);

			int ptr = itemlength - 0x10;
			startline = (result[ptr + 2] & 0xff) + ((result[ptr + 3] & 0xff) * 0x100);
		}

	}

	/**
	 * Get the filename
	 * 
	 * @return
	 */
	public String GetFilename() {
		String result = null;
		if (DirEntryDescriptor[0] > 0x01) {
			result = new String(DirEntryDescriptor, StandardCharsets.UTF_8).substring(0, 8).trim();
		}
		return (result);
	}

	/**
	 * Set the filename
	 * 
	 * @param filename
	 */
	public void SetFilename(String filename) {
		filename = filename.replace(" ", "_");
		filename = filename + "        ";
		for (int i = 0; i < 8; i++) {
			DirEntryDescriptor[i] = (byte) filename.charAt(i);
		}
	}

	/**
	 * Get the file type. The valid file types are: B: Basic C: Code D: Data (array
	 * variable) #: Sequential or Random access file
	 * 
	 * @return
	 */
	public char GetFileType() {
		return ((char) DirEntryDescriptor[0x08]);
	}

	/**
	 * Get a textual representation of the file type
	 * 
	 * @return
	 */
	@Override
	public String GetFileTypeString() {
		String result = "Unknown";
		if (GetDeleted()) {
			result = "Deleted";
		} else {
			switch (GetFileType()) {
			case 'B':
				result = "Basic";
				break;
			case 'C':
				result = "Code";
				break;
			case 'D':
				result = "Data";
				if (IsCharArray()) {
					result = "Data (Char)";
				} else {
					result = "Data (numeric)";
				}
				break;
			case '#':
				result = "Sequential";
				break;
			}
		}
		return (result);
	}

	/**
	 * Set the file type
	 * 
	 * @param ft
	 */
	public void SetFileType(char ft) {
		DirEntryDescriptor[0x08] = (byte) ft;
	}

	/**
	 * Get the file start address
	 * 
	 * @return
	 */
	public int GetVar1() {
		int sa = (DirEntryDescriptor[0x09] & 0xff) + ((DirEntryDescriptor[0x0A] & 0xff) * 0x100);
		return (sa);
	}

	/**
	 * Set the start address
	 * 
	 * @param sa
	 */
	public void SetVar1(int sa) {
		int lsb = sa & 0xff;
		int msb = sa / 0x100;
		DirEntryDescriptor[0x09] = (byte) (lsb & 0xff);
		DirEntryDescriptor[0x0A] = (byte) (msb & 0xff);
	}

	/**
	 * Get the file length.
	 * 
	 * @return
	 */
	public int GetVar2() {
		int fl = (DirEntryDescriptor[0x0b] & 0xff) + ((DirEntryDescriptor[0x0c] & 0xff) * 0x100);
		return (fl);
	}

	/**
	 * set the file length.
	 * 
	 * @param len
	 */
	public void SetVar2(int len) {
		int lsb = len & 0xff;
		int msb = len / 0x100;
		DirEntryDescriptor[0x0B] = (byte) (lsb & 0xff);
		DirEntryDescriptor[0x0C] = (byte) (msb & 0xff);
	}

	/**
	 * Get the sector count of the file
	 * 
	 * @return
	 */
	public int GetFileLengthSectors() {
		return ((int) (DirEntryDescriptor[0x0d] & 0xff));
	}

	/**
	 * Set the sector count of the file.
	 * 
	 * @param len
	 */
	public void SetFileLengthSectors(int len) {
		DirEntryDescriptor[0x0d] = (byte) (len & 0xff);
	}

	/**
	 * Get the start sector
	 * 
	 * @return
	 */
	public int GetStartSector() {
		return ((int) (DirEntryDescriptor[0x0e] & 0xff));
	}

	/**
	 * Set the start sector
	 * 
	 * @param ss
	 */
	public void SetStartSector(int ss) {
		DirEntryDescriptor[0x0e] = (byte) (ss & 0xff);
	}

	/**
	 * Get the start track
	 * 
	 * @return
	 */
	public int GetStartTrack() {
		return ((int) (DirEntryDescriptor[0x0f] & 0xff));
	}

	/**
	 * Set the start track
	 * 
	 * @param st
	 */
	public void SetStartTrack(int st) {
		DirEntryDescriptor[0x0f] = (byte) (st & 0xff);
	}

	/**
	 * Get the deleted flag.
	 * 
	 * @return
	 */
	public boolean GetDeleted() {
		return (DirEntryDescriptor[0] < 0x02);
	}

	/**
	 * Set the deleted flag
	 * 
	 * @param deleted
	 */
	public void SetDeleted(boolean deleted) {
		if (deleted) {
			DirEntryDescriptor[0] = 0x01;
		} else {
			DirEntryDescriptor[0] = 'A';
		}
	}

	/**
	 * Get a textual representation of the directory entry
	 */
	public String toString() {
		String result = "#" + DirentNum + " File: " + PadTo(GetFilename(), 8) + " Typ:"
				+ PadTo(GetFileType() + "(" + GetFileTypeString() + ") ", 9) + " Var1:"
				+ PadTo(String.valueOf(GetVar1()), 5) + " var2:" + PadTo(String.valueOf(GetVar2()), 5) + " sectors: "
				+ PadTo(String.valueOf(GetFileLengthSectors()), 3) + " start:" + GetStartTrack() + "/"
				+ GetStartSector();
		return (result);
	}

	public String PadTo(String s, int num) {
		while (s.length() < num) {
			s = s + " ";
		}
		return (s);
	}

	/**
	 * Test harness.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		byte testdirents[][] = {
				{ 0x5A, 0x2E, 0x53, 0x2E, 0x2D, 0x49, 0x49, 0x20, 0x42, (byte) 0x9B, 0x00, (byte) 0x9B, 0x00,
						(byte) 0xFF, 0x00, 0x01 },
				{ 0x7A, 0x2E, 0x73, 0x2E, 0x2D, 0x69, 0x69, 0x20, 0x43, 0x00, (byte) 0xC0, (byte) 0xFA, 0x34, 0x4D,
						0x0F, 0x10 },
				{ 0x62, 0x6F, 0x6F, 0x74, 0x20, 0x20, 0x20, 0x20, 0x42, 0x20, 0x00, 0x20, 0x00, 0x01, 0x0C, 0x15 } };

		for (int i = 0; i < 3; i++) {
			TrdDirectoryEntry td;
			try {
				td = new TrdDirectoryEntry(null, i, testdirents[i], false, 0);
				System.out.println(td);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get the file data.
	 * 
	 * @return
	 * @throws IOException
	 */
	@Override
	public byte[] GetFileData() throws IOException {
		return (GetFileRawData());
	}

	/**
	 * 
	 */
	@Override
	public byte[] GetFileRawData() throws IOException {
		int startsector = (GetStartTrack() * CurrentDisk.GetNumSectors()) + GetStartSector();

		long length = GetFileSize();
		// check to see if this value is reasonable.
		long FileSizeSectors = GetFileLengthSectors() * 256;
		long diff = Math.abs(FileSizeSectors - length);
		if (diff > 266) { // difference is out of range. (1 sector + 10 BASIC fiddle bytes)
			// fiddle the file length.
			length = FileSizeSectors;
			System.out.println("GetFileData(): File: " + GetFilename() + " bad length. Fiddling.");
		}

		byte result[] = CurrentDisk.GetBytesStartingFromSector(startsector, length);
		return (result);
	}

	/**
	 * 
	 */
	@Override
	public int GetRawFileSize() {
		return (GetFileSize());
	}

	/**
	 * Get the Speccy file length. Note that for BASIC files - this is in the first
	 * byte pair rather than the second one. - This value does not include the bytes
	 * at the end containing the start line.
	 * 
	 * @return
	 */
	@Override
	public int GetFileSize() {
		int length = GetVar2();
		if (GetFileType() == 'B') {
			length = GetVar1();
		}
		return length;
	}

	/**
	 * Return if a given file is a character array. This is provided because the "D"
	 * file type can be either.
	 * 
	 * @return
	 */
	public boolean IsCharArray() {
		boolean result = false;
		if (GetFileType() == 'D') {
			byte dtyp = (byte) (DirEntryDescriptor[0x09] & 0xff);
			result = (dtyp & 0x40) == 0;
		}
		return (result);
	}

	/**
	 * This performs a CPM-style file match except matching is 10.1 rather than 8.3
	 */
	@Override
	public boolean DoesMatch(String wildcard) {
		// convert the wildcard into a search array:
		// Split into filename and extension. pad out with spaces.
		String fname = wildcard.trim().toUpperCase();
		String StringToMatch = GetFilename().toUpperCase();
		String filename = "";
		String extension = "";
		if (fname.contains(".")) {
			int i = fname.lastIndexOf(".");
			extension = fname.substring(i + 1);
			filename = fname.substring(0, i);
		} else {
			filename = fname;
		}
		filename = filename + "            ";
		extension = extension + "  ";

		// create search array.
		byte comp[] = new byte[12];

		// populate with filename
		boolean foundstar = false;
		for (int i = 0; i < 10; i++) {
			if (foundstar) {
				comp[i] = '?';
			} else {
				char c = filename.charAt(i);
				if (c == '*') {
					foundstar = true;
					comp[i] = '?';
				} else {
					comp[i] = (byte) ((int) c & 0xff);
				}
			}
		}

		// populate with extension
		for (int i = 0; i < 1; i++) {
			if (foundstar) {
				comp[i + 10] = '?';
			} else {
				char c = extension.charAt(i);
				if (c == '*') {
					foundstar = true;
					comp[i + 10] = '?';
				} else {
					comp[i + 10] = (byte) ((int) c & 0xff);
				}
			}
		}

		int HasDot = StringToMatch.indexOf('.');
		String preDot = StringToMatch;
		String PostDot = "";
		if (HasDot > -1) {
			preDot = StringToMatch.substring(0, HasDot);
			PostDot = StringToMatch.substring(HasDot + 1);
		}
		preDot = (preDot + "            ").substring(0, 10);
		PostDot = (PostDot + " ").substring(0, 1);

		StringToMatch = preDot + PostDot;
		// now search.
		// check the filename
		boolean match = true;
		for (int i = 0; i < 11; i++) {
			byte chr = (byte) StringToMatch.charAt(i);
			byte cchr = comp[i];
			if ((chr != cchr) && (cchr != '?')) {
				match = false;
			}
		}
		return (match);
	}

	@Override
	public SpeccyBasicDetails GetSpeccyBasicDetails() {
		int VarStart = GetVar1();
		int LoadAddress = GetVar1();
		char ArrayVar = 'A';
		int filetype = Speccy.BASIC_CODE;
		switch (GetFileType()) {
		case 'B':
			filetype = Speccy.BASIC_BASIC;
			break;
		case 'D':
			if (IsCharArray()) {
				filetype = Speccy.BASIC_CHRARRAY;
			} else {
				filetype = Speccy.BASIC_NUMARRAY;
			}
			break;
		}

		SpeccyBasicDetails result = new SpeccyBasicDetails(filetype, VarStart, startline, LoadAddress, ArrayVar);

		return (result);
	}

	public void SetSpeccyBasicDetails(SpeccyBasicDetails sbd, TrDosPartition Part) {
		switch (sbd.BasicType) {
		case Speccy.BASIC_BASIC:
			SetVar2(sbd.VarStart);
			startline = sbd.LineStart;
			SetStartLine();
			break;
		case Speccy.BASIC_NUMARRAY:
			break;
		case Speccy.BASIC_CHRARRAY:
			break;
		case Speccy.BASIC_CODE:
			SetVar1(sbd.LoadAddress);
		default:
			break;
		}
		try {
			Part.UpdateDirentsOnDisk();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void SetStartLine() {
		/**
		 * For BASIC files, extract the line from the file.
		 */
		if ((CurrentDisk != null) && (this.GetFileType() == 'B')) {
			long startsector = (GetStartTrack() * CurrentDisk.GetNumSectors()) + GetStartSector();
			// Want the 10 bytes past the EOF
			int itemlength = GetFileSize() + 0x10;
			// Reduce the amount we have to read. Keep incrementing the sector until there
			// is only one or two sectors left.
			while (itemlength > 512) {
				itemlength = itemlength - CurrentDisk.GetSectorSize();
				startsector++;
			}

			byte result[];
			try {
				result = CurrentDisk.GetBytesStartingFromSector(startsector, itemlength);
				int ptr = itemlength - 0x10;
				int lsb = startline & 0xff;
				int msb = startline / 0x100;

				result[ptr + 2] = (byte) (lsb & 0xff);
				result[ptr + 3] = (byte) (msb & 0xff);
				CurrentDisk.SetLogicalBlockFromSector(startsector, result);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}

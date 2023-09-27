package hddEditor.libs.partitions;

/**
 * This is the implementation of a TR_DOS partition. Note that this actually duplicates
 * some of the code in the TRD disk handler code, due to the fact it requires the same
 * information.
 * 
 * However its possible that you could get a TR-DOS partition inside an ADF file or
 * some other raw disk format, so will re-fetch the disk info block rather than relying on the 
 * underlying disk. 
 * 
 * file format information:
 * https://formats.kaitai.io/tr_dos_image/#:~:text=TR%2DDOS%20flat%2Dfile%20disk%20image%3A%20format%20specification,of%2016%20256%2Dbyte%20sectors.&text=So%2C%20this%20format%20definition%20is,TR%2DDOS%20filesystem%20than%20for%20.
 * https://sinclair.wiki.zxnet.co.uk/wiki/TR-DOS_filesystem
 * 
 * Note, these seem to contradict each other, specifically the use of dirent+0x09
 * I have been allowed to update sinclair wiki version with my experiments as detailed in the Documents section :)
 * Also note the test disk included in the Document folder
 */

import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.util.ArrayList;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.FDD.BadDiskFileException;
import hddEditor.libs.disks.FDD.SCLDiskFile;
import hddEditor.libs.disks.FDD.TrDosDiskFile;
import hddEditor.libs.partitions.trdos.TrdDirectoryEntry;

public class TrDosPartition extends IDEDosPartition {

	public boolean IsValid = false;
	public int FirstFreeSectorS = 0;
	public int FirstFreeSectorT = 0;
	public int LogicalDiskType = 0;
	public int NumFiles = 0;
	public int NumDeletedFiles = 0;
	public int NumFreeSectors = 0;
	public int TRDOSID = 0;
	public String Disklabel = "";

	public TrdDirectoryEntry DirectoryEntries[] = null;

	public TrDosPartition(int DirentLocation, Disk RawDisk, byte[] RawPartition, int DirentNum, boolean Initialise) {
		super(DirentLocation, RawDisk, RawPartition, DirentNum, Initialise);
		IsValid = false;
		try {
			PopulateParameters();
			LoadDirectoryEntries();
		} catch (Exception E) {
			IsValid = false;
			System.out.println("Cannot parse TR-DOS information.");
		}
	}

	/**
	 * 
	 * @return
	 */
	public String GetDiskTypeAsString() {
		String DiskType = "Unknown";
		switch (LogicalDiskType) {
		case 0x16:
			DiskType = "80 tracks, DS";
			break;
		case 0x17:
			DiskType = "40 tracks, DS";
			break;
		case 0x18:
			DiskType = "80 tracks, SS";
			break;
		case 0x19:
			DiskType = "40 tracks, SS";
			break;
		}
		return (DiskType);
	}

	/**
	 * Load the details from the disk info block on the disk (Physical Sector 9,
	 * Head 0, Track 0 = Logical sector 8)
	 * 
	 * @throws IOException
	 */
	private void PopulateParameters() throws IOException {
		byte[] DiskInfoBlock = CurrentDisk.GetBytesStartingFromSector(8, 0x100);

		FirstFreeSectorS = (DiskInfoBlock[0xe1] & 0xff);
		FirstFreeSectorT = (DiskInfoBlock[0xe2] & 0xff);
		LogicalDiskType = (DiskInfoBlock[0xe3] & 0xff);
		NumFiles = (DiskInfoBlock[0xe4] & 0xff);
		NumFreeSectors = ((DiskInfoBlock[0xe5] & 0xff) + ((DiskInfoBlock[0xe6] & 0xff) * 0x100));
		TRDOSID = (DiskInfoBlock[0xe7] & 0xff);
		NumDeletedFiles = (DiskInfoBlock[0xf4] & 0xff);
		Disklabel = "";
		IsValid = true;
		for (int i = 0xf5; i < 0xfc; i++) {
			char c = (char) (DiskInfoBlock[i]);
			Disklabel = Disklabel + c;
			// quick sanity check.
			if (c < ' ' || c > 0x7f) {
				IsValid = false;
			}
		}

		// Check some of the disk parameters for stupid values.
		if ((DiskInfoBlock[0] != 0x00) || (TRDOSID != 0x10) || (DiskInfoBlock[0xff] != 0))
			IsValid = false;
		if ((LogicalDiskType < 0x16) || (LogicalDiskType > 0x19)) {
			IsValid = false;
		}

	}

	/**
	 * return details as a string.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + "\nValid?: " + IsValid;
		result = result + "\nFirst free sector (S/t) " + FirstFreeSectorS + "/" + FirstFreeSectorT;
		result = result + "\nLogical disk type: " + Integer.toHexString(LogicalDiskType);
		result = result + "\nNumFiles: " + NumFiles;
		result = result + "\nNumDeletedFiles: " + NumDeletedFiles;
		result = result + "\nNumFreeSectors: " + NumFreeSectors;
		result = result + "\nTRDOSID: " + TRDOSID;
		result = result + "\nDisklabel: " + Disklabel;
		result = result + "\nDirectory entries: ";
		if (DirectoryEntries != null) {
			for (TrdDirectoryEntry td : DirectoryEntries) {
				result = result + "\n" + td;
			}
		} else {
			result = result + "\n  directory entries not set.";
		}

		return (result);
	}

	/**
	 * Test harness.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		TrDosDiskFile tdf;
		try {
			tdf = new TrDosDiskFile(new File("/home/graham/tmp/ufo.trd"));
			TrDosPartition trp = new TrDosPartition(0, tdf, new byte[64], 1, false);
			System.out.println(trp);
		} catch (IOException | BadDiskFileException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Load the directory entries.
	 * 
	 * @throws IOException
	 */
	private void LoadDirectoryEntries() throws IOException {
		byte dirents[] = CurrentDisk.GetBytesStartingFromSector(0, CurrentDisk.GetSectorSize() * 8);
		ArrayList<TrdDirectoryEntry> newDirents = new ArrayList<TrdDirectoryEntry>();
		int ptr = 0;
		int dNum = 0;
		while (ptr < dirents.length) {
			byte dir[] = new byte[0x10];
			System.arraycopy(dirents, ptr, dir, 0, 0x10);
			TrdDirectoryEntry tde = new TrdDirectoryEntry(CurrentDisk, dNum++, dir, false, ptr);
			ptr = ptr + 0x10;
			if (tde.GetFileType() > 0x01) {
				newDirents.add(tde);
			}
		}
		DirectoryEntries = newDirents.toArray(new TrdDirectoryEntry[0]);
	}

	/**
	 * Add a file to the disk
	 * 
	 * @param filename
	 * @param filetype
	 * @param file
	 * @throws IOException
	 */
	public void AddFile(String filename, char filetype, byte file[], int var1) throws IOException {
		int NumSectorsRequired = file.length / CurrentDisk.GetSectorSize();
		if ((file.length / CurrentDisk.GetSectorSize()) != 0) {
			NumSectorsRequired++;
		}
		/**
		 * Load the directory and disk information block.
		 */
		byte direntAndDib[] = GetDataInPartition(0, 9 * CurrentDisk.GetSectorSize());

		/**
		 * find a blank dirent
		 */
		int i = 0;
		int direntLoc = -1;
		while ((i < 128) && (direntLoc == -1)) {
			if (direntAndDib[0x10 * i] == 0) {
				direntLoc = 0x10 * i;
			}
			i++;
		}
		/*
		 * Populate the Dirent
		 */
		// Filename:
		filename = filename + "          ";
		for (i = 0; i < 8; i++) {
			direntAndDib[direntLoc + i] = (byte) filename.charAt(i);
		}
		// file type
		direntAndDib[direntLoc + 0x08] = (byte) (filetype & 0xff);
		// File start address
		int msb = var1 / 0x100;
		int lsb = var1 % 0x100;
		direntAndDib[direntLoc + 0x09] = (byte) (lsb & 0xff);
		direntAndDib[direntLoc + 0x0A] = (byte) (msb & 0xff);
		// file length
		int flen = file.length;
		// for basic files, fiddle the file size to include the line number.
		if (filetype == 'B') {
			flen = flen - 5;
		}
		msb = flen / 0x100;
		lsb = flen % 0x100;
		direntAndDib[direntLoc + 0x0B] = (byte) (lsb & 0xff);
		direntAndDib[direntLoc + 0x0C] = (byte) (msb & 0xff);
		// file length in sectors
		direntAndDib[direntLoc + 0x0D] = (byte) (NumSectorsRequired & 0xff);
		// start sector
		direntAndDib[direntLoc + 0x0E] = (byte) (FirstFreeSectorS & 0xff);
		// start track
		direntAndDib[direntLoc + 0x0F] = (byte) (FirstFreeSectorT & 0xff);

		/*
		 * Update the PIB
		 */
		// calculate the next free track.
		int numtracks = NumSectorsRequired / CurrentDisk.GetNumSectors();
		int numsectors = NumSectorsRequired % CurrentDisk.GetNumSectors();
		int NextFreeTrack = numtracks + FirstFreeSectorT;
		int NextFreeSector = numsectors + FirstFreeSectorS;
		if (NextFreeSector > CurrentDisk.GetNumSectors()) {
			NextFreeTrack++;
			NextFreeSector = NextFreeSector - CurrentDisk.GetNumSectors();
		}
		direntAndDib[0x8e1] = (byte) (NextFreeSector & 0xff);
		direntAndDib[0x8e2] = (byte) (NextFreeTrack & 0xff);
		// Num files
		int NumFiles = (direntAndDib[0x8e4] & 0xff) + 1;
		direntAndDib[0x8e4] = (byte) (NumFiles & 0xff);

		// Free sectors
		NumFreeSectors = ((direntAndDib[0x8e5] & 0xff) + ((direntAndDib[0x8e6] & 0xff) * 0x100));
		NumFreeSectors = NumFreeSectors - NumSectorsRequired;
		msb = NumFreeSectors / 0x100;
		lsb = NumFreeSectors % 0x100;
		direntAndDib[0x8e5] = (byte) (lsb & 0xff);
		direntAndDib[0x8e6] = (byte) (msb & 0xff);

		/*
		 * write to the disk
		 */
		SetDataInPartition(0, direntAndDib);

		/*
		 * write the file.
		 */
		int LogicalSector = (FirstFreeSectorT * CurrentDisk.GetNumSectors()) + FirstFreeSectorS;
		SetDataInPartition(LogicalSector, file);

		/*
		 * Update local flags
		 */
		FirstFreeSectorT = NextFreeTrack;
		FirstFreeSectorS = NextFreeSector;
		LoadDirectoryEntries();

		// Hack for SCF disks which need to be physically written after all disk mods
		// are done.
		if (CurrentDisk.getClass().getName().equals(SCLDiskFile.class.getName())) {
			SCLDiskFile scf = (SCLDiskFile) CurrentDisk;
			scf.OperationCompleted(DirectoryEntries);
		}
	}

	/**
	 * Pack the disk. This is essential on a disk that has a
	 */
	public void Pack() throws IOException {
		// rebuild the disk....
		/**
		 * Extract all the files.
		 */
		ArrayList<TrdDirectoryEntry> FileDescriptors = new ArrayList<TrdDirectoryEntry>();
		for (TrdDirectoryEntry tde : DirectoryEntries) {
			if (!tde.GetDeleted()) {
				FileDescriptors.add(tde);
			}
		}
		ArrayList<byte[]> FileData = new ArrayList<byte[]>();
		for (TrdDirectoryEntry tde : FileDescriptors) {
			FileData.add(tde.GetFileData());
		}

		// get the old DIB to use as a template, preserving the disk label.
		byte[] DiskInfoBlock = CurrentDisk.GetBytesStartingFromSector(0, 0x900);
		for (int i = 0; i < 0x7ff; i++) {
			DiskInfoBlock[i] = 0x00;
		}

		// Write dirents.
		int NextFreeTrack = 1;
		int NextFreeSector = 0;
		int DirEntPtr = 0;

		for (int filenum = 0; filenum < FileDescriptors.size(); filenum++) {
			// Get the file length details.
			TrdDirectoryEntry entry = FileDescriptors.get(filenum);
			int filedataSectorCount = entry.GetFileLengthSectors();

			// Set the new start of the file and write it back.
			entry.SetStartSector(NextFreeSector);
			entry.SetStartTrack(NextFreeTrack);

			System.out.println(entry.GetFilename() + ": New T/S:" + NextFreeTrack + "/" + NextFreeSector + "  SS:"
					+ entry.GetFileLengthSectors());

			System.arraycopy(entry.DirEntryDescriptor, 0, DiskInfoBlock, DirEntPtr, 0x10);

			// Point to the next dirent on the disk.
			DirEntPtr = DirEntPtr + 0x10;

			// Work out the start of the next file.
			int NumTracks = filedataSectorCount / CurrentDisk.GetNumSectors();
			int NumSectors = filedataSectorCount % CurrentDisk.GetNumSectors();

			NextFreeTrack = NextFreeTrack + NumTracks;
			NextFreeSector = NextFreeSector + NumSectors;

			while (NextFreeSector > CurrentDisk.GetNumSectors()) {
				NextFreeTrack++;
				NextFreeSector = NextFreeSector - CurrentDisk.GetNumSectors();
			}
		}
		// Now finish the DIB.
		DiskInfoBlock[0x8e1] = (byte) (NextFreeSector & 0xff); // next free sector.
		DiskInfoBlock[0x8e2] = (byte) (NextFreeTrack & 0xff); // Next free track
		DiskInfoBlock[0x8e4] = (byte) (FileDescriptors.size() & 0xff); // number of files
		DiskInfoBlock[0x8f4] = 0x00; // number of deleted files.

		// Free sectors remaining...
		int TracksRemaining = (CurrentDisk.GetNumCylinders() * CurrentDisk.GetNumHeads()) - NextFreeTrack;
		int NumFreeSectors = TracksRemaining * CurrentDisk.GetNumSectors();
		NumFreeSectors = NumFreeSectors - (NextFreeSector + 1);

		DiskInfoBlock[0x8e5] = (byte) ((NumFreeSectors % 0x100) & 0xff);
		DiskInfoBlock[0x8e6] = (byte) ((NumFreeSectors / 0x100) & 0xff);

		// Update the local copy of the directory entries.
		DirectoryEntries = FileDescriptors.toArray(new TrdDirectoryEntry[0]);

		// Update the directory entries on disk
		CurrentDisk.SetLogicalBlockFromSector(0, DiskInfoBlock);

		// Now, write the data...
		for (int filenum = 0; filenum < FileDescriptors.size(); filenum++) {
			TrdDirectoryEntry entry = FileDescriptors.get(filenum);
			byte data[] = FileData.get(filenum);

			int startLogicalSector = (entry.GetStartTrack() * CurrentDisk.GetNumSectors()) + entry.GetStartSector();

			CurrentDisk.SetLogicalBlockFromSector(startLogicalSector, data);
		}

		// re-read the parameter block.
		PopulateParameters();
		// Hack for SCF disks which need to be physically written after all disk mods
		// are done.
		if (CurrentDisk.getClass().getName().equals(SCLDiskFile.class.getName())) {
			SCLDiskFile scf = (SCLDiskFile) CurrentDisk;
			scf.OperationCompleted(DirectoryEntries);
		}

	}

	/**
	 * Rename a file.
	 * 
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	public void RenameFile(String from, char type, String to) throws IOException {
		byte dirents[] = CurrentDisk.GetBytesStartingFromSector(0, CurrentDisk.GetSectorSize() * 9);
		boolean renamed = false;

		for (TrdDirectoryEntry trd : DirectoryEntries) {

			if (trd.GetFilename().equals(from) && ((trd.GetFileType() == type) || (type == ' '))) {
				trd.SetFilename(to);
				System.arraycopy(trd.DirEntryDescriptor, 0, dirents, trd.DirentLoc, 0x10);
				NumDeletedFiles = (dirents[0x8f4] & 0xff) + 1;
				dirents[0x8f4] = (byte) (NumDeletedFiles & 0xff);
				renamed = true;
			}
		}
		if (!renamed) {
			System.out.println("File '" + from + "' not found.");
		}
		CurrentDisk.SetLogicalBlockFromSector(0, dirents);
		// Hack for SCF disks which need to be physically written after all disk mods
		// are done.
		if (CurrentDisk.getClass().getName().equals(SCLDiskFile.class.getName())) {
			SCLDiskFile scf = (SCLDiskFile) CurrentDisk;
			scf.OperationCompleted(DirectoryEntries);
		}
	}

	/**
	 * Add a basic file, note we have to hack the LINE variable at the end.
	 * 
	 * @param filename
	 * @param data
	 * @param line
	 * @param StartOfVars
	 * @throws IOException
	 */
	@Override
	public void AddBasicFile(String filename, byte[] data, int line, int StartOfVars) throws IOException {
		byte newdata[] = new byte[data.length + 5];
		System.arraycopy(data, 0, newdata, 0, data.length);
		newdata[data.length] = (byte) (0x80 & 0xff);
		newdata[data.length + 1] = (byte) (0xAA & 0xff);
		int lsb = line % 0x100;
		int msb = line / 0x100;
		newdata[data.length + 2] = (byte) (lsb & 0xff);
		newdata[data.length + 3] = (byte) (msb & 0xff);

		AddFile(filename, 'B', newdata, StartOfVars);
	}

	/**
	 * Update the given entry with a new file.
	 * 
	 * @param entry
	 * @param data
	 * @throws IOException
	 */
	public void UpdateFile(TrdDirectoryEntry entry, byte[] data) throws IOException {
		int filelen = data.length;
		if (entry.GetFileType() == 'B') {
			filelen = filelen + 0x05;
		}
		int NumSectors = filelen / CurrentDisk.GetSectorSize();
		if (filelen % CurrentDisk.GetSectorSize() != 0) {
			NumSectors++;
		}

		/**
		 * If the file is the same size, just overwrite the old one.
		 */
		if (NumSectors == entry.GetFileLengthSectors()) {
			CurrentDisk.SetLogicalBlockFromSector(0, data);
			int LogicalSector = (entry.GetStartTrack() * CurrentDisk.GetNumSectors()) + entry.GetStartSector();
			SetDataInPartition(LogicalSector, data);
		} else {
			// If not, copy the old file descriptor information and re-create it.
			String filename = entry.GetFilename();
			char filetype = entry.GetFileType();
			int line = entry.startline;
			int var1 = entry.GetVar1();

			entry.SetDeleted(true);
			if (filetype == 'B') {
				AddBasicFile(filename, data, line, line);
			} else {
				AddFile(filename, 'B', data, var1);
			}
		}
	}

	/**
	 * Fetch the dirent by name and type
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
	public TrdDirectoryEntry GetDirentByName(String name, char type) {
		TrdDirectoryEntry result = null;
		for (TrdDirectoryEntry entry : DirectoryEntries) {
			if (entry.GetFilename().trim().equals(name.trim()) && entry.GetFileType() == type) {
				result = entry;
			}
		}

		return (result);
	}

	/**
	 * Extract partition with flags showing what to do with each file type.
	 * 
	 * @param folder
	 * @param BasicAction
	 * @param CodeAction
	 * @param ArrayAction
	 * @param ScreenAction
	 * @param MiscAction
	 * @param progress
	 * @throws IOException
	 */
	@Override
	public void ExtractPartitiontoFolderAdvanced(File folder, int BasicAction, int CodeAction, int ArrayAction,
			int ScreenAction, int MiscAction, int SwapAction, ProgressCallback progress, boolean IncludeDeleted)
			throws IOException {
		FileWriter SysConfig;
		try {
			SysConfig = new FileWriter(new File(folder, "partition.index"));
			try {
				SysConfig.write("<speccy>\n".toCharArray());
				int entrynum = 0;
				for (TrdDirectoryEntry entry : DirectoryEntries) {
					if ((entry.GetDeleted() == false) || IncludeDeleted) {
						if (progress != null) {
							if (progress.Callback(DirectoryEntries.length, entrynum++, "File: " + entry.GetFilename()))
								break;
						}
						try {
							File TargetFilename = new File(folder,
									entry.GetFilename().trim() + "." + entry.GetFileType());
							int filelength = entry.GetFileSize();
							int SpeccyFileType = 0;
							int basicLine = 0;
							int basicVarsOffset = entry.GetFileSize();
							int codeLoadAddress = 0;
							String arrayVarName = "A";
							int actiontype = GeneralUtils.EXPORT_TYPE_RAW;
							if ((entry.GetFileType() != 'B') && (entry.GetFileType() != 'C')
									&& (entry.GetFileType() != 'D')) {
								SpeccyFileType = Speccy.BASIC_CODE;
								codeLoadAddress = 0x10000 - entry.GetFileSize();
								actiontype = MiscAction;
							} else {
								switch (entry.GetFileType()) {
								case 'B':
									SpeccyFileType = Speccy.BASIC_BASIC;
									actiontype = BasicAction;
									break;
								case 'D':
									SpeccyFileType = Speccy.BASIC_NUMARRAY;
									if (entry.IsCharArray()) {
										SpeccyFileType = Speccy.BASIC_CHRARRAY;
									}
									actiontype = ArrayAction;
									break;
								case 'C':
									SpeccyFileType = Speccy.BASIC_CODE;
									codeLoadAddress = entry.GetVar1();
									actiontype = CodeAction;
								}
								basicLine = entry.startline;
								basicVarsOffset = entry.GetVar2();

							}

							Speccy.SaveFileToDiskAdvanced(TargetFilename, entry.GetFileData(), entry.GetFileData(),
									filelength, SpeccyFileType, basicLine, basicVarsOffset, codeLoadAddress,
									arrayVarName, actiontype);
							System.out.println("Written " + entry.GetFilename().trim());
						} catch (IOException e) {
							System.out
									.println("Error extracting " + entry.GetFilename().trim() + ": " + e.getMessage());
							e.printStackTrace();
						}
						SysConfig.write(("<file>\n").toCharArray());
						SysConfig.write(("   <filename>" + entry.GetFilename().trim() + "." + entry.GetFileType()
								+ "</filename>\n").toCharArray());
						SysConfig.write(("   <deleted>" + entry.GetDeleted() + "</deleted>\n").toCharArray());
						SysConfig.write(("   <errors></errors>\n").toCharArray());
						SysConfig.write(("   <filelength>" + entry.GetFileSize() + "</filelength>\n").toCharArray());
						SysConfig.write("   <origfiletype>TRDOS</origfiletype>\n".toCharArray());
						SysConfig.write(("   <specbasicinfo>\n".toCharArray()));
						int filetype = Speccy.BASIC_CODE;
						switch (entry.GetFileType()) {
						case 'B':
							filetype = Speccy.BASIC_BASIC;
							break;
						case 'D':
							filetype = Speccy.BASIC_NUMARRAY;
							if (entry.IsCharArray()) {
								filetype = Speccy.BASIC_CHRARRAY;
							}
						}
						SysConfig.write(("       <filetype>" + filetype + "</filetype>\n").toCharArray());
						SysConfig.write(
								("       <filetypename>" + Speccy.FileTypeAsString(filetype) + "</filetypename>\n")
										.toCharArray());
						SysConfig.write(("       <basicsize>" + entry.GetFileSize() + "</basicsize>\n").toCharArray());
						SysConfig.write(
								("       <basicstartline>" + entry.startline + "</basicstartline>\n").toCharArray());
						SysConfig
								.write(("       <codeloadaddr>" + entry.GetVar1() + "</codeloadaddr>\n").toCharArray());
						SysConfig.write(
								("       <basicvarsoffset>" + entry.GetVar2() + "</basicvarsoffset>\n").toCharArray());
						SysConfig.write(("       <arrayvarname>A</arrayvarname>\n").toCharArray());
						SysConfig.write(("   </specbasicinfo>\n".toCharArray()));

						SysConfig.write(("   <trdos>\n".toCharArray()));
						SysConfig.write(("       <filetype>" + entry.GetFileType() + "</filetype>\n").toCharArray());
						SysConfig.write(("       <filetypename>" + entry.GetFileTypeString() + "</filetypename>\n")
								.toCharArray());
						SysConfig.write(("       <direntnum>" + entry.DirentNum + "</direntnum>\n").toCharArray());
						SysConfig.write(
								("       <direntlocation>" + entry.DirentLoc + "</direntlocation>\n").toCharArray());
						SysConfig.write(
								("       <startsector>" + entry.GetStartSector() + "</startsector>\n").toCharArray());
						SysConfig.write(
								("       <starttrack>" + entry.GetStartTrack() + "</starttrack>\n").toCharArray());
						SysConfig.write(("       <numsectors>" + entry.GetFileLengthSectors() + "</numsectors>\n")
								.toCharArray());
						SysConfig.write(("   </trdos>\n".toCharArray()));
						SysConfig.write(("</file>\n").toCharArray());
					}
				}
				SysConfig.write("</speccy>\n".toCharArray());
			} finally {
				SysConfig.close();
			}
		} catch (

		IOException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * 
	 */
	@Override
	public FileEntry[] GetFileList(String wildcard) {
		ArrayList<FileEntry> results = new ArrayList<FileEntry>();
		for (FileEntry de : DirectoryEntries) {
			if (de.DoesMatch(wildcard)) {
				results.add(de);
			}
		}
		return (results.toArray(new FileEntry[0]));
	}

	@Override
	public void DeleteFile(String wildcard) throws IOException {
		byte dirents[] = CurrentDisk.GetBytesStartingFromSector(0, CurrentDisk.GetSectorSize() * 9);
		boolean deleted = false;

		for (TrdDirectoryEntry trd : DirectoryEntries) {
			if (trd.DoesMatch(wildcard)) {
				trd.SetDeleted(true);
				System.arraycopy(trd.DirEntryDescriptor, 0, dirents, trd.DirentLoc, 0x10);
				NumDeletedFiles = (dirents[0x8f4] & 0xff) + 1;
				dirents[0x8f4] = (byte) (NumDeletedFiles & 0xff);
				deleted = true;
			}
		}
		if (!deleted) {
			System.out.println("File '" + wildcard + "' not found.");
		}
		CurrentDisk.SetLogicalBlockFromSector(0, dirents);
		// Hack for SCF disks which need to be physically written after all disk mods
		// are done.
		if (CurrentDisk.getClass().getName().equals(SCLDiskFile.class.getName())) {
			SCLDiskFile scf = (SCLDiskFile) CurrentDisk;
			scf.OperationCompleted(DirectoryEntries);
		}
	}

	/**
	 * Rename a named file. This is the generic version that doesn't include the
	 * type separately.
	 * 
	 * @param filename
	 * @param newName
	 * @throws IOException
	 */
	@Override
	public void RenameFile(String filename, String newName) throws IOException {
		char filetype = ' ';
		if ((filename.length() > 2) && (filename.charAt(filename.length() - 2) == '.')) {
			filetype = filename.charAt(filename.length() - 1);
			filename = filename.substring(0, filename.length() - 2);
		}
		RenameFile(filename, filetype, newName);
	}

	/**
	 * Save a passed in data with the given filename. as CODE
	 * 
	 * @param filename
	 * @param address
	 * @param data
	 * @throws IOException
	 */
	@Override
	public void AddCodeFile(String filename, int address, byte[] data) throws IOException {
		AddFile(filename, 'C', data, address);
	}

	/**
	 * Add an pre-encoded numeric array to the disk.
	 * 
	 * @param filename
	 * @param EncodedArray
	 * @param varname
	 * @return
	 * @throws IOException
	 */
	@Override
	public void AddNumericArray(String filename, byte[] EncodedArray, String varname) throws IOException {
		if (varname.isEmpty()) {
			varname = "A";
		}
		int varbyte = (int) varname.charAt(0) - 0x40;
		varbyte = varbyte + 0xa0; // 101XXXXX
		AddFile(filename, 'D', EncodedArray, varbyte);
	}

	/**
	 * Add a character array
	 * 
	 * @param filename
	 * @param data
	 * @throws IOException
	 */
	@Override
	public void AddCharArray(String filename, byte[] EncodedArray, String varname) throws IOException {
		if (varname.isEmpty()) {
			varname = "A";
		}
		int varbyte = (int) varname.charAt(0) - 0x40;
		varbyte = varbyte + 0xe0; // 111XXXXX
		AddFile(filename, 'D', EncodedArray, varbyte);
	}

	/**
	 * Get all the files on this partition.
	 * 
	 * @return
	 */
	@Override	
	public FileEntry[] GetFileList() {
		return DirectoryEntries;
	}

}

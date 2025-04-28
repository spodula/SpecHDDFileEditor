package hddEditor.libs.partitions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.MGT;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.disks.FDD.BadDiskFileException;
import hddEditor.libs.disks.FDD.MGTDiskFile;
import hddEditor.libs.partitions.mgt.MGTDirectoryEntry;

public class MGTDosPartition extends IDEDosPartition {
	public boolean IsValid = false;
	public MGTDirectoryEntry DirectoryEntries[] = null;
	private int bam[] = null;

	private int BAM_UNUSED = -1;
	private int BAM_DIRECTORY = -2;


	public MGTDosPartition(int DirentLocation, Disk RawDisk, byte[] RawPartition, int DirentNum, boolean Initialise) {
		super(DirentLocation, RawDisk, RawPartition, DirentNum, Initialise);
		CanExport = true;
		IsValid = false;
		try {
			PopulateParameters();
			LoadDirectoryEntries();
		} catch (Exception E) {
			IsValid = false;
			E.printStackTrace();
			System.out.println("Cannot parse PLUSD information.");
		}
	}

	/**
	 * Default parameters for an MGT disk
	 */
	private void PopulateParameters() {
		SetStartCyl(0);
		SetStartHead(0);
		SetEndCyl(CurrentDisk.GetNumCylinders() - 1);
		SetEndHead(CurrentDisk.GetNumHeads() - 1);
	}

	/**
	 * return details as a string.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		if (DirectoryEntries == null) {
			result = result + "\n No entries loaded.";
		} else {

			for (MGTDirectoryEntry entry : DirectoryEntries) {
				result = result + "\n #" + String.format("%02X", entry.DirentNum) + "  "
						+ GeneralUtils.PadTo(entry.GetFilename(), 12)
						+ GeneralUtils.PadTo(entry.GetFileTypeString() + "(" + entry.GetFileType() + ") ", 13) + "Len:"
						+ GeneralUtils.PadTo(Integer.toString(entry.GetFileSize()), 6) + "Start:"
						+ GeneralUtils.PadTo(Integer.toHexString(entry.RawDirectoryEntry[0x0d]) + "/"
								+ Integer.toHexString(entry.RawDirectoryEntry[0x0e]), 6)
						+ "Num Sectors: " + GeneralUtils.PadTo(Integer.toString(entry.GetNumSectors()), 4)
						+ "LoadAddr: "
						+ GeneralUtils.PadTo(Integer.toString(entry.GetSpeccyBasicDetails().LoadAddress), 6)
						+ "StartLine: "
						+ GeneralUtils.PadTo(Integer.toString(entry.GetSpeccyBasicDetails().LineStart), 6)
						+ "VarStart: "
						+ GeneralUtils.PadTo(Integer.toString(entry.GetSpeccyBasicDetails().VarStart), 6);

				if (entry.GetHidden()) {
					result = result + "Hidden ";
				} else {
					result = result + "       ";
				}
				if (entry.GetProtected()) {
					result = result + "Protected ";
				} else {
					result = result + "          ";
				}

			}
		}

		result = result + "\n\nBAM: DD=Directory, FF = unused, other values = Directory# \n";

		String ul = "";
		for (int j = 0; j < 4; j++) {
			result = result + "Ch/H: ";
			ul = ul + "----- ";
			for (int i = 0; i < CurrentDisk.GetNumSectors(); i++) {
				result = result + String.format("%02X ", i + 1);
				ul = ul + "---";
			}
			result = result + "   ";
			ul = ul + "   ";
		}
		result = result + "\n" + ul;
		int byt = 0;
		for (int bamEntry : bam) {
			if (byt % CurrentDisk.GetNumSectors() == 0) {
				result = result + "   ";
				int trk = byt / CurrentDisk.GetNumSectors();
				int head = 0;
				if (trk > 79) {
					head = 1;
					trk = trk - 80;
				}
				if (byt % (CurrentDisk.GetNumSectors() * 4) == 0) {
					result = result + "\n";
				}
				result = result + String.format("%02X/%X: ", trk, head);
			}
			byt++;
			String s = String.format("%02X", bamEntry);
			s = s.substring(s.length() - 2);
			if (s.equals("FE"))
				s = "DD";
			result = result + s + " ";
		}

		return (result);
	}

	/**
	 * Test harness.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		MGTDiskFile tdf;
		try {
			tdf = new MGTDiskFile(new File("/home/graham/Artist2.mgt"));
			MGTDosPartition trp = new MGTDosPartition(0, tdf, new byte[64], 1, false);
			System.out.println(trp);
			byte data[] = new byte[4444];
			trp.AddFile("testfile", 3, data, 1, 22222, 10);
			System.out.println(trp);
			int i[] = trp.DirectoryEntries[0x14].GetLogicalSectors();
			System.out.print("File sectors: ");
			for (int x = 0; x < i.length; x++) {
				System.out.print(i[x] + "  ");
			}

		} catch (IOException | BadDiskFileException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	private void LoadDirectoryEntries() throws IOException {
		/*
		 * Allocate space and preset the BAM.
		 */
		bam = new int[CurrentDisk.GetNumCylinders() * CurrentDisk.GetNumHeads() * CurrentDisk.GetNumSectors()];
		// reserve the directory tracks.
		int reservedSectors = 4 * CurrentDisk.GetNumSectors(); // first 4 tracks
		for (int sect = 0; sect < reservedSectors; sect++) {
			bam[sect] = BAM_DIRECTORY;
		}
		// preset them rest of the BAM locations to unused
		for (int sect = reservedSectors; sect < bam.length; sect++) {
			bam[sect] = BAM_UNUSED;
		}

		/*
		 * Now load the directory entries.
		 */
		byte dirents[] = GetDataInPartition(0, 4 * CurrentDisk.GetNumSectors() * CurrentDisk.GetSectorSize());
		int ptr = 0;
		int Dnum = 0;
		DirectoryEntries = new MGTDirectoryEntry[4 * CurrentDisk.GetNumSectors() * CurrentDisk.GetSectorSize() / 256];
		while (ptr < dirents.length) {
			byte SingleDirent[] = new byte[256];
			System.arraycopy(dirents, ptr, SingleDirent, 0, 256);
			MGTDirectoryEntry newMDE = new MGTDirectoryEntry(CurrentDisk, Dnum, SingleDirent, false, ptr);
			DirectoryEntries[Dnum++] = newMDE;
			ptr = ptr + 256;
			if (newMDE.GetFileType() > MGT.MGTFT_ERASED) {
				int usedsectors[] = newMDE.GetLogicalSectorsFromDirents();
				for (int sector : usedsectors) {
					bam[sector] = newMDE.DirentNum;
				}
			}
		}
	}

	/**
	 * Add a file to the disk. Note, that this is slightly modified for MGT Disks.
	 * The filetype is the MGT filetype - 1 so the filetype will match the normal
	 * Speccy file type while still allowing adding of MGT files.
	 * 
	 * @param filename
	 * @param filetype
	 * @param file
	 * @throws IOException
	 */
	public void AddFile(String filename, int filetype, byte file[], int var1, int StartAddress, int runline)
			throws IOException {
		/*
		 * Check to see if the file exists.
		 */
		if (GetDirentByName(filename) != null) {
			throw new IOException("Cannot add file to " + filename + " - File already exists on disk.");
		}

		/*
		 * Get the number of sectors required
		 */
		int usableSectorData = CurrentDisk.GetSectorSize()-2;
		int NumSectorsRequired = file.length / usableSectorData;
		if ((file.length / usableSectorData) != 0) {
			NumSectorsRequired++;
		}

		/*
		 * find a blank dirent
		 */
		MGTDirectoryEntry dirent = null;
		int direntnum = 0;
		while ((dirent == null) && (direntnum < DirectoryEntries.length)) {
			MGTDirectoryEntry d = DirectoryEntries[direntnum];
			if (d.GetFileType() == MGT.MGTFT_ERASED) {
				dirent = d;
				break;
			}
			direntnum++;
		}
		if (dirent == null)
			throw new IOException("Cannot add file '" + filename + "' Directory full.");

		/*
		 * Find a run of enough sectors
		 */
		int runstart = 0;
		int runlength = 0;
		for (int srNo = 4 * CurrentDisk.GetNumSectors(); srNo < bam.length; srNo++) {
			int SectorContent = bam[srNo];
			if (SectorContent != BAM_UNUSED) {
				runstart = 0;
				runlength = 0;
			} else {
				if (runstart == 0) {
					runstart = srNo;
					runlength = 1;
				} else {
					runlength++;
				}
				if (runlength == NumSectorsRequired)
					break;
			}
		}
		if (runlength != NumSectorsRequired) {
			throw new IOException("Cannot add file '" + filename + "' Cant find a large enough run of sectors.");
		}
		System.out.println("Found blank dirent: " + DirentLocation + " and sectors starting at: " + runstart);

		/*
		 * Update the dirent
		 */

		// filename
		dirent.SetFilename(filename);

		// file type
		dirent.RawDirectoryEntry[0] = (byte) ((filetype + 1) & 0xff);

		// number of sectors
		dirent.RawDirectoryEntry[11] = (byte) ((NumSectorsRequired / 0x100) & 0xff);
		dirent.RawDirectoryEntry[12] = (byte) ((NumSectorsRequired % 0x100) & 0xff);

		// first track/sector in file
		int firstTrack = runstart / CurrentDisk.GetNumSectors();
		if (firstTrack >= CurrentDisk.GetNumCylinders()) {
			firstTrack = firstTrack - CurrentDisk.GetNumCylinders() + 128;
		}
		int SectorNum = ((runstart % CurrentDisk.GetNumSectors()) + 1);
		dirent.RawDirectoryEntry[13] = (byte) (firstTrack & 0xff);
		dirent.RawDirectoryEntry[14] = (byte) (SectorNum & 0xff);

		// Sector map
		int LogicalRunEnd = runstart + runlength;
		int CurrentSector = 4 * CurrentDisk.GetNumSectors();
		for (int sMapPtr = 0x0f; sMapPtr < 0xd2; sMapPtr++) {
			int byt = 0;
			for (int bit = 0; bit < 8; bit++) {
				byt = byt / 2;
				if ((CurrentSector >= runstart) && (CurrentSector < LogicalRunEnd)) {
					byt = byt + 0x80;
				}
				CurrentSector++;
			}
			dirent.RawDirectoryEntry[sMapPtr] = (byte) (byt & 0xff);
		}

		int i[] = dirent.GetLogicalSectors();
		for (int x = 0; x < i.length; x++) {
			System.out.print(i[x] + "  ");
		}

		// Speccy file information
		if (filetype > 3) {
			dirent.RawDirectoryEntry[0xd3] = 0x03; // Code
		} else {
			dirent.RawDirectoryEntry[0xd3] = (byte) (filetype & 0x0f);
		}
		// File length
		dirent.RawDirectoryEntry[0xd4] = (byte) ((file.length % 0x100) & 0xff);
		dirent.RawDirectoryEntry[0xd5] = (byte) ((file.length / 0x100) & 0xff);
		// start address
		dirent.RawDirectoryEntry[0xd6] = (byte) ((StartAddress % 0x100) & 0xff);
		dirent.RawDirectoryEntry[0xd7] = (byte) ((StartAddress / 0x100) & 0xff);
		// type specific information
		dirent.RawDirectoryEntry[0xd8] = (byte) ((var1 % 0x100) & 0xff);
		dirent.RawDirectoryEntry[0xd9] = (byte) ((var1 / 0x100) & 0xff);
		// runline
		dirent.RawDirectoryEntry[0xda] = (byte) ((runline % 0x100) & 0xff);
		dirent.RawDirectoryEntry[0xdb] = (byte) ((runline / 0x100) & 0xff);

		/*
		 * Write the Dirent
		 */
		SaveDirectoryEntry(dirent);
		/*
		 * Update and write the sectors
		 */
		
		//write the file in sector blocks.
		int ptr = 0;
//		System.out.println("Start: T:"+firstTrack+" S:"+SectorNum+" with LB:"+runstart);
		while (SectorNum != 0) {
			SectorNum++;
			if (SectorNum > 10) {
				SectorNum = 1;
				firstTrack++;
			}
			if ((ptr+510) > file.length) {
				SectorNum = 0;
				firstTrack = 0;
			}
			
			byte newdata[] = new byte[CurrentDisk.GetSectorSize()];
			int tocopy = Math.min(newdata.length-2, file.length-ptr);
			System.arraycopy(file, ptr, newdata, 0, tocopy);
			ptr = ptr + tocopy;
			newdata[newdata.length-2] = (byte) (firstTrack & 0xff);
			newdata[newdata.length-1] = (byte) (SectorNum & 0xff);
//			System.out.println("Writing T:"+firstTrack+" S:"+SectorNum+" with LB:"+runstart+ " Ptr:"+ptr+" file.length:"+file.length);
			CurrentDisk.SetLogicalBlockFromSector(runstart, newdata);
			runstart++;
		}
		

		/*
		 * Refresh the directory entry list
		 */
		LoadDirectoryEntries();
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
		AddFile(filename, MGT.MGTFT_ZXBASIC - 1, data, StartOfVars, 23755, line);
	}

	/**
	 * Update the given entry with a new file.
	 * 
	 * @param entry
	 * @param data
	 * @throws IOException
	 */
	public void UpdateFile(MGTDirectoryEntry entry, byte[] data) throws IOException {
		String filename = entry.GetFilename();
		int filetype = entry.GetFileType();
		int var1 = entry.GetVar1();
		int LoadAddress = entry.GetSpeccyBasicDetails().LoadAddress;
		int Runline = entry.GetSpeccyBasicDetails().LineStart;

		// blank out the old dirent.
		for (int i = 0; i < entry.RawDirectoryEntry.length; i++) {
			entry.RawDirectoryEntry[i] = 0;
		}
		DirectoryEntries[entry.DirentNum] = entry;
		/*
		 * Write the dirent to disk
		 */
		SaveDirectoryEntry(entry);

		/*
		 * Add replacement
		 */
		AddFile(filename, filetype, data, var1, LoadAddress, Runline);
	}

	/**
	 * Fetch the dirent by name and type
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
	public MGTDirectoryEntry GetDirentByName(String name) {
		for (MGTDirectoryEntry mde : DirectoryEntries) {
			if (mde.DoesMatch(name)) {
				return (mde);
			}
		}
		return (null);
	}

	/**
	 * Extract disk with flags showing what to do with each file type.
	 * 
	 * @param folder
	 * @param BasicAction
	 * @param CodeAction
	 * @param ArrayAction
	 * @param ScreenAction
	 * @param MiscAction
	 * @param progress
	 * @param IncludeDeleted
	 * @throws IOException
	 */
	@Override
	public void ExtractPartitiontoFolderAdvanced(File folder, int BasicAction, int CodeAction, int ArrayAction,
			int ScreenAction, int MiscAction, int SwapAction, ProgressCallback progress, boolean IncludeDeleted) throws IOException {
		FileWriter SysConfig;
		try {
			SysConfig = new FileWriter(new File(folder, "partition.index"));
			try {
				SysConfig.write("<speccy>\n".toCharArray());
				int entrynum = 0;
				for (MGTDirectoryEntry entry : DirectoryEntries) {
					if (entry.GetFileType() != MGT.MGTFT_ERASED) {
						SpeccyBasicDetails sbd = entry.GetSpeccyBasicDetails();

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

							switch (entry.GetFileType()) {
							case 1:// MGT.MGTFT_ZXBASIC:
								SpeccyFileType = Speccy.BASIC_BASIC;
								actiontype = BasicAction;
								break;
							case 2: // MGT.MGTFT_ZXNUMARRAY:
								SpeccyFileType = Speccy.BASIC_NUMARRAY;
								actiontype = ArrayAction;
								break;
							case 3: // MGT.MGTFT_ZXSTRARRAY:
								SpeccyFileType = Speccy.BASIC_CHRARRAY;
								actiontype = ArrayAction;
								break;
							case 4: // MGT.MGTFT_ZXCODE:
								SpeccyFileType = Speccy.BASIC_CODE;
								actiontype = CodeAction;
								if (filelength==6912) {
									actiontype = ScreenAction;
								}
								break;
							default:
								SpeccyFileType = Speccy.BASIC_CODE;
								actiontype = MiscAction;
							}
							codeLoadAddress = sbd.LoadAddress;
							basicLine = sbd.LineStart;
							basicVarsOffset = sbd.VarStart;

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
						SysConfig.write(("   <deleted>false</deleted>\n").toCharArray());
						SysConfig.write(("   <errors></errors>\n").toCharArray());
						SysConfig.write(("   <filelength>" + entry.GetFileSize() + "</filelength>\n").toCharArray());
						SysConfig.write("   <origfiletype>TRDOS</origfiletype>\n".toCharArray());
						SysConfig.write(("   <specbasicinfo>\n".toCharArray()));

						SysConfig.write(("       <filetype>" + sbd.BasicType + "</filetype>\n").toCharArray());
						SysConfig.write(
								("       <filetypename>" + sbd.BasicTypeString() + "</filetypename>\n").toCharArray());
						SysConfig.write(("       <basicsize>" + entry.GetFileSize() + "</basicsize>\n").toCharArray());
						SysConfig.write(
								("       <basicstartline>" + sbd.LineStart + "</basicstartline>\n").toCharArray());
						SysConfig
								.write(("       <codeloadaddr>" + sbd.LoadAddress + "</codeloadaddr>\n").toCharArray());
						SysConfig.write(
								("       <basicvarsoffset>" + sbd.VarStart + "</basicvarsoffset>\n").toCharArray());
						SysConfig.write(("       <arrayvarname>A</arrayvarname>\n").toCharArray());
						SysConfig.write(("   </specbasicinfo>\n".toCharArray()));

						SysConfig.write(("   <mgt>\n".toCharArray()));
						SysConfig.write(("       <filetype>" + entry.GetFileType() + "</filetype>\n").toCharArray());
						SysConfig.write(("       <hidden>" + entry.GetHidden() + "</hidden>\n").toCharArray());
						SysConfig.write(("       <protected>" + entry.GetProtected() + "</protected>\n").toCharArray());
						SysConfig.write(("       <filetypename>" + entry.GetFileTypeString() + "</filetypename>\n")
								.toCharArray());
						SysConfig.write(("       <direntnum>" + entry.DirentNum + "</direntnum>\n").toCharArray());
						SysConfig.write(
								("       <direntlocation>" + entry.DirentLoc + "</direntlocation>\n").toCharArray());
						SysConfig.write(
								("       <startsector>" + (entry.RawDirectoryEntry[14] & 0xff) + "</startsector>\n")
										.toCharArray());
						SysConfig.write(
								("       <starttrack>" + (entry.RawDirectoryEntry[13] & 0xff) + "</starttrack>\n")
										.toCharArray());
						SysConfig.write(
								("       <numsectors>" + entry.GetNumSectors() + "</numsectors>\n").toCharArray());
						SysConfig.write(("   </mgt>\n".toCharArray()));
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

	/**
	 * Delete the file. Basically this will just blank the Dirent.
	 * 
	 */
	@Override
	public void DeleteFile(String wildcard) throws IOException {
		MGTDirectoryEntry[] files = (MGTDirectoryEntry[]) GetFileList(wildcard);
		if ((files == null) || (files.length == 0)) {
			throw new IOException("Cannot find file '" + wildcard + "' to delete.");
		}

		for (MGTDirectoryEntry currentfile : files) {
			// blank the dirent.
			for (int i = 0; i < currentfile.RawDirectoryEntry.length; i++) {
				currentfile.RawDirectoryEntry[i] = 0;
			}
			DirectoryEntries[currentfile.DirentNum] = currentfile;

			/*
			 * Write the dirent to disk
			 */
			SaveDirectoryEntry(currentfile);
		}
	}

	/**
	 * Write the dirent to disk
	 * 
	 * @param currentfile
	 * @throws IOException
	 */
	public void SaveDirectoryEntry(MGTDirectoryEntry currentfile) throws IOException {
		// locate the sector containing the dirent.
		int direntSector = currentfile.DirentNum * 256 / CurrentDisk.GetSectorSize();
		int LocationInSector = (currentfile.DirentNum * 256) % CurrentDisk.GetSectorSize();

		// Load the sector
		byte sector[] = CurrentDisk.GetBytesStartingFromSector(direntSector, CurrentDisk.GetSectorSize());
		// update the dirent in the sector
		System.arraycopy(currentfile.RawDirectoryEntry, 0, sector, LocationInSector, 256);
		// write it back.
		CurrentDisk.SetLogicalBlockFromSector(direntSector, sector);

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
		MGTDirectoryEntry[] files = (MGTDirectoryEntry[]) GetFileList(filename);
		if ((files == null) || (files.length == 0)) {
			throw new IOException("Cannot find file '" + filename + "' to rename.");
		}
		if (files.length > 1) {
			throw new IOException("filename '" + filename + "' matches more than 1 file.");
		}

		MGTDirectoryEntry currentfile = files[0];
		currentfile.SetFilename(newName);
		DirectoryEntries[currentfile.DirentNum] = currentfile;
		SaveDirectoryEntry(currentfile);
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
		AddFile(filename, MGT.MGTFT_ZXCODE - 1, data, 0xffff, address, 0);
	}

	/**
	 * Add an pre-encoded numeric array to the microdrive.
	 * 
	 * @param filename
	 * @param EncodedArray
	 * @param varname
	 * @return
	 * @throws IOException
	 */
	@Override
	public void AddNumericArray(String filename, byte[] EncodedArray, String varname) throws IOException {
		int variable = (((varname.toUpperCase() + "A").charAt(0) - 0x40) | 0x80) * 256;
		AddFile(filename, MGT.MGTFT_ZXNUMARRAY - 1, EncodedArray, 0xffff, 0, variable);
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
		int variable = (((varname.toUpperCase() + "A").charAt(0) - 0x40) | 0xC0) * 256;
		AddFile(filename, MGT.MGTFT_ZXSTRARRAY - 1, EncodedArray, 0xffff, 0, variable);
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

	/**
	 * Uniquify a filename if required. (IE, a disk)
	 * @param filename
	 * @return
	 */
	@Override
	public String UniqueifyFileNameIfRequired(String filename) {
		String ofilename = filename.trim();
		if (ofilename.length() > 8) {
			ofilename = ofilename.substring(0,8);
		}
		
		int index=1;
		FileEntry existingEntries[] = GetFileList(filename);
		while (existingEntries.length > 0) {
			filename = ofilename + String.format("%02d", index++);
			existingEntries = GetFileList(filename);			
		}
		return filename;
	}

	/**
	 * Sort the directory entries.
	 * 
	 * @param SortType 0 = no sort, 1=name, 2 = file type, 3=file size,
	 */
	public void SortDirectoryEntries(int SortType) {
		DirectoryEntries = (MGTDirectoryEntry[]) SortFileEntry(SortType);
	}	
	
}

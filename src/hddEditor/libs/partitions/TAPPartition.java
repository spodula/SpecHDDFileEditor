package hddEditor.libs.partitions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.disks.FDD.BadDiskFileException;
import hddEditor.libs.disks.LINEAR.TAPFile;
import hddEditor.libs.disks.LINEAR.TAPFile.TAPBlock;
import hddEditor.libs.partitions.tap.TapDirectoryEntry;

public class TAPPartition extends IDEDosPartition {
	//Parsed directory entries.
	public TapDirectoryEntry DirectoryEntries[];

	/**
	 * Constructor for a TAP partition. 
	 * 
	 * @param DirentLocation
	 * @param RawDisk
	 * @param RawPartition
	 * @param DirentNum
	 * @param Initialise
	 */
	public TAPPartition(int DirentLocation, Disk RawDisk, byte[] RawPartition, int DirentNum, boolean Initialise) {
		super(DirentLocation, RawDisk, RawPartition, DirentNum, Initialise);
	}

	/**
	 * get the number of free sectors on the tape. This is basically infinite.
	 * 
	 * @return
	 */
	public int NumFreeSectors() {
		return (99999999);
	}

	/**
	 * Load the partition specific information
	 */
	@Override
	public void LoadPartitionSpecificInformation() {
		RawPartition = new byte[0x40];
		TAPFile tf = (TAPFile) CurrentDisk;

		SetPartType(PLUSIDEDOS.PARTITION_TAPE_TAP);
		SetStartCyl(0);
		SetEndCyl(0);
		SetStartHead(0);
		SetEndHead(0);
		SetEndSector((long) tf.Blocks.length);

		ArrayList<TapDirectoryEntry> dirents = new ArrayList<TapDirectoryEntry>();

		TAPBlock lastblock = null;
		for (TAPBlock tb : tf.Blocks) {
			if (tb.flagbyte == 0x00 && tb.data.length == 17) {
				if (lastblock != null) {
					// create orphan header block.
					TapDirectoryEntry tde = new TapDirectoryEntry(lastblock, null);
					dirents.add(tde);
				}
				lastblock = tb;
			} else {
				if (lastblock != null) {
					// create merged block
					TapDirectoryEntry tde = new TapDirectoryEntry(tb, lastblock);
					dirents.add(tde);

					lastblock = null;
				} else {
					// create orphan data block
					TapDirectoryEntry tde = new TapDirectoryEntry(tb, null);
					dirents.add(tde);
				}
			}
		}
		if (lastblock != null) {
			// create orphan header block.
			TapDirectoryEntry tde = new TapDirectoryEntry(lastblock, null);
			dirents.add(tde);
		}

		DirectoryEntries = dirents.toArray(new TapDirectoryEntry[0]);
	}

	/**
	 * return details as a string.
	 */
	@Override
	public String toString() {
		TAPFile mdf = (TAPFile) CurrentDisk;
		String result = "Filename: " + mdf.GetFilename();
		result = result + "\nNumSectors: " + mdf.GetNumSectors();

		result = result + "\n\nBlocks: ";
		for (TAPFile.TAPBlock mde : mdf.Blocks) {
			result = result + "\n#" + mde.blocknum + ": " + mde;
		}
		result = result + "\n\nDirectory entries: ";
		int i = 0;
		for (TapDirectoryEntry entry : DirectoryEntries) {
			result = result + "\n#" + i + ": " + entry.GetFilename() + ": " + entry.GetFileTypeString();
			i++;
		}

		return (result);

	}

	/**
	 * Get a file by name. (Case insensitive)
	 * 
	 * @param filename
	 * @return
	 */
	public TAPFile.TAPBlock GetFileByName(String filename) {
		TAPFile mdf = (TAPFile) CurrentDisk;
		TAPFile.TAPBlock result = null;
		filename = filename.trim().toUpperCase();
		boolean foundfile = false;

		if (filename.startsWith("BLOCK")) {
			try {
				String num = filename.substring(5);
				int number = Integer.valueOf(num);
				result = mdf.Blocks[number];
				foundfile = true;
			} catch (Exception E) {
			}
		}
		if (!foundfile) {
			for (TAPFile.TAPBlock file : mdf.Blocks) {
				if (file.data.length == 17 && file.flagbyte == 0x00) {
					byte dfn[] = new byte[10];
					System.arraycopy(file.data, 1, dfn, 0, 10);
					String tapeFileName = new String(dfn).trim();
					if (tapeFileName.toUpperCase().equals(filename)) {
						result = file;
						break;
					}
				}
			}
		}

		return (result);
	}

	/**
	 * Add a standard 17 byte Speccy Basic Tape header.
	 * 
	 * @param filename
	 * @param filetype
	 * @param filelength
	 * @param param1
	 * @param param2
	 * @throws IOException
	 */
	public void AddSpeccyHeader(String filename, int filetype, int filelength, int param1, int param2)
			throws IOException {
		byte data[] = new byte[17];
		data[0] = (byte) (filetype & 0xff);
		filename = filename + "           ";
		for (int i = 1; i < 11; i++) {
			data[i] = (byte) filename.charAt(i - 1);
		}
		data[11] = (byte) (filelength & 0xff);
		data[12] = (byte) ((filelength / 0x100) & 0xff);

		data[13] = (byte) (param1 & 0xff);
		data[14] = (byte) ((param1 / 0x100) & 0xff);

		data[15] = (byte) (param2 & 0xff);
		data[16] = (byte) ((param2 / 0x100) & 0xff);

		TAPFile tap = (TAPFile) CurrentDisk;
		tap.AddBlock(data, 0);
	}

	/**
	 * Add a CODE file to the tape.
	 * 
	 * @param filename
	 * @param CodeFile
	 * @param loadAddress
	 * @return
	 * @throws IOException
	 */
	@Override
	public void AddCodeFile(String filename, int loadAddress, byte[] CodeFile) throws IOException {
		AddSpeccyHeader(filename, Speccy.BASIC_CODE, CodeFile.length, loadAddress, 32768);

		TAPFile tap = (TAPFile) CurrentDisk;
		tap.AddBlock(CodeFile, 255);
		LoadPartitionSpecificInformation();
	}

	/**
	 * Add an pre-encoded BASIC file to the tape.
	 * 
	 * @param filename
	 * @param EncodedBASICFile
	 * @param varsStart
	 * @param line
	 * @return
	 * @throws IOException
	 */
	@Override
	public void AddBasicFile(String filename, byte[] EncodedBASICFile, int line, int varsStart) throws IOException {
		AddSpeccyHeader(filename, Speccy.BASIC_BASIC, EncodedBASICFile.length, line, varsStart);

		TAPFile tap = (TAPFile) CurrentDisk;
		tap.AddBlock(EncodedBASICFile, 255);
		LoadPartitionSpecificInformation();
	}

	/**
	 * Add an pre-encoded character array to the tape.
	 * 
	 * @param filename
	 * @param EncodedArray
	 * @param varname
	 * @return
	 * @throws IOException
	 */
	@Override
	public void AddCharArray(String filename, byte[] EncodedArray, String varname) throws IOException {
		int encodedVarName = ((((varname + "A").charAt(0) & 0xff) - 0x60 + 0xC0) & 0xff);

		AddSpeccyHeader(filename, Speccy.BASIC_CHRARRAY, EncodedArray.length, encodedVarName * 0x100, 0);

		TAPFile tap = (TAPFile) CurrentDisk;
		tap.AddBlock(EncodedArray, 255);
		LoadPartitionSpecificInformation();
	}

	/**
	 * Add an pre-encoded numeric array to the tape.
	 * 
	 * @param filename
	 * @param EncodedArray
	 * @param varname
	 * @return
	 * @throws IOException
	 */
	@Override
	public void AddNumericArray(String filename, byte[] EncodedArray, String varname) throws IOException {
		int encodedVarName = ((((varname + "A").charAt(0) & 0xff) - 0x60 + 0x80) & 0xff);

		AddSpeccyHeader(filename, Speccy.BASIC_NUMARRAY, EncodedArray.length, encodedVarName * 0x100, 0);

		TAPFile tap = (TAPFile) CurrentDisk;
		tap.AddBlock(EncodedArray, 255);
		LoadPartitionSpecificInformation();
	}

	/**
	 * Rename a named file.
	 * 
	 * @param filename
	 * @param newName
	 * @throws IOException
	 */
	@Override
	public void RenameFile(String filename, String newName) throws IOException {
		// find the file in the list
		int foundfile = -1;
		filename = filename.trim().toUpperCase();
		for (int i = 0; i < DirectoryEntries.length; i++) {
			if (DirectoryEntries[i].GetFilename().trim().toUpperCase().equals(filename)) {
				foundfile = i;
			}
		}
		if (foundfile == -1) {
			System.out.println("File " + filename + " not found.");
			throw new IOException("File " + filename + " not found.");
		} else {
			RenameFile(DirectoryEntries[foundfile], newName);
		}
	}

	/**
	 * 
	 * @param file
	 * @param newName
	 * @throws IOException
	 */
	public void RenameFile(TapDirectoryEntry file, String newName) throws IOException {
		file.SetFilename(newName);
		((TAPFile) CurrentDisk).RewriteFile();
		LoadPartitionSpecificInformation();
	}

	/**
	 * GetAllDataInPartition overridden.
	 * 
	 * @return
	 * @throws IOException
	 */
	@Override
	public byte[] GetAllDataInPartition() throws IOException {
		return (((TAPFile) CurrentDisk).GetAllData());
	}

	@Override
	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public void SetAllDataInPartition(byte[] data) throws IOException {
		((TAPFile) CurrentDisk).SetAllData(data);
	}

	/**
	 * Get the size in Kbytes of the partition. As usual, this differs for tapes as
	 * the file size is effectively infinate.
	 * 
	 * @return
	 */
	@Override
	public int GetSizeK() {
		return ((int) 99999999);
	}

	/**
	 * Pack the current tape. Not really required as packed when saved.
	 * 
	 * @throws IOException
	 */
	public void Pack() throws IOException {
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
				for (TapDirectoryEntry entry : DirectoryEntries) {
					if (progress != null) {
						if (progress.Callback(DirectoryEntries.length, entrynum++, "File: " + entry.GetFilename())) {
							break;
						}
					}
					SpeccyBasicDetails sd = entry.GetSpeccyBasicDetails();

					File TargetFilename = new File(folder, entry.GetFilename().trim());
					byte entrydata[] = entry.GetFileData();
					byte Rawentrydata[] = entry.GetFileRawData();

					int filelength = entrydata.length;
					int SpeccyFileType = sd.BasicType;
					boolean isUnknown = (SpeccyFileType > 3);
					int basicLine = sd.LineStart;
					int basicVarsOffset = sd.VarStart;
					int codeLoadAddress = sd.LoadAddress;
					String arrayVarName = (sd.VarName + " ").trim();

					try {
						int actiontype = GeneralUtils.EXPORT_TYPE_RAW;
						if (isUnknown) { // Options are: "Raw", "Hex", "Assembly"
							actiontype = MiscAction;
						} else {
							// Identifed BASIC File type
							if (SpeccyFileType == Speccy.BASIC_BASIC) { // Options are: "Text", "Raw", "Raw+Header",
																		// "Hex"
								actiontype = BasicAction;
							} else if ((SpeccyFileType == Speccy.BASIC_NUMARRAY)
									&& (SpeccyFileType == Speccy.BASIC_CHRARRAY)) {
								actiontype = ArrayAction;
							} else if ((filelength == 6912) && (codeLoadAddress == 16384)) { // { "PNG", "GIF", "JPEG",
																								// "Raw",
																								// "Raw+Header", "Hex",
																								// "Assembly" };
								actiontype = ScreenAction;
							} else { // CODE Options: { "Raw", "Raw+Header", "Assembly", "Hex" };
								actiontype = CodeAction;
							}
						}

						Speccy.SaveFileToDiskAdvanced(TargetFilename, entrydata, Rawentrydata, filelength,
								SpeccyFileType, basicLine, basicVarsOffset, codeLoadAddress, arrayVarName, actiontype);
					} catch (Exception E) {
						System.out.println("\nError extracting " + TargetFilename + "For folder: " + folder + " - "
								+ E.getMessage());
						E.printStackTrace();
					}

					System.out.println("Written " + entry.GetFilename().trim());
					SpeccyBasicDetails sbd = entry.GetSpeccyBasicDetails();
					SysConfig.write(("<file>\n").toCharArray());
					SysConfig.write(("   <filename>" + entry.GetFilename().trim() + "</filename>\n").toCharArray());
					SysConfig.write(("   <deleted>false</deleted>\n").toCharArray());
					SysConfig.write(("   <errors></errors>\n").toCharArray());
					SysConfig.write(("   <filelength>" + entry.GetFileSize() + "</filelength>\n").toCharArray());
					SysConfig.write("   <origfiletype>MDF</origfiletype>\n".toCharArray());
					SysConfig.write(("   <specbasicinfo>\n".toCharArray()));
					SysConfig.write(("       <filetype>" + sbd.BasicType + "</filetype>\n").toCharArray());
					SysConfig.write(
							("       <filetypename>" + sbd.BasicTypeString() + "</filetypename>\n").toCharArray());
					SysConfig.write(("       <basicsize>" + entry.GetFileSize() + "</basicsize>\n").toCharArray());
					SysConfig.write(("       <basicstartline>" + sbd.LineStart + "</basicstartline>\n").toCharArray());
					SysConfig.write(("       <codeloadaddr>" + sbd.LoadAddress + "</codeloadaddr>\n").toCharArray());
					SysConfig.write(("       <basicvarsoffset>" + sbd.VarStart + "</basicvarsoffset>\n").toCharArray());
					SysConfig.write(("       <arrayvarname>A</arrayvarname>\n").toCharArray());
					SysConfig.write(("   </specbasicinfo>\n".toCharArray()));
					SysConfig.write(("</file>\n").toCharArray());

				}
				SysConfig.write("</speccy>\n".toCharArray());
			} finally {
				SysConfig.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Get the file list given a wildcard
	 * 
	 * @return 
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
	 * Delete the given file(s)
	 * 
	 * @param filename
	 * @throws IOException
	 */
	@Override
	public void DeleteFile(String wildcard) throws IOException {
		// find the file in the list
		wildcard = wildcard.trim();
		for (TapDirectoryEntry tde : DirectoryEntries) {
			if (tde.DoesMatch(wildcard)) {
				DeleteFile(tde, false);
			}
		}
		LoadPartitionSpecificInformation();
	}

	/**
	 * This is provided as a separate entry because deleting something by filename
	 * Doesn't work very well on tape files as there can be multiple files with the
	 * same name
	 * 
	 * @param filename
	 * @param DontUpdate
	 * @throws IOException
	 */
	public void DeleteFile(TapDirectoryEntry entry, boolean DontUpdate) throws IOException {
		if (entry.HeaderBlock != null) {
			((TAPFile) CurrentDisk).DeleteBlock(entry.HeaderBlock);
		}
		if (entry.DataBlock != null) {
			((TAPFile) CurrentDisk).DeleteBlock(entry.DataBlock);
		}

		if (!DontUpdate) {
			LoadPartitionSpecificInformation();
		}
	}

	/**
	 * Test harness.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		TAPFile tdf;
		try {
			tdf = new TAPFile("/home/graham/a.tap");

			TAPPartition trp = new TAPPartition(0, tdf, new byte[64], 1, false);
			System.out.println(trp);
//			byte xxx[] = new byte[100];

			// trp.AddCodeFile("text.bin", 16384,xxx);
			System.out.println(trp);

		} catch (IOException | BadDiskFileException e) {
			e.printStackTrace();
		}
	}
}

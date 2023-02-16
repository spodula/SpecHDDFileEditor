package hddEditor.libs.partitions;

/**
 * Implementation of a Plus3DOS partition.
 * 
 * 0x0000	PN PN PN PN PN PN PN PN PN PN PN PN PN PN PN PN	
 * 0x0010	03 SC SC SH EC EC EH LS LS LS LS 00 00 00 00 00
 * 0x0020	DP DP DP DP DP DP DP DP DP DP DP DP DP DP DP DP	
 * 0x0030	DP DP DP DP DP DP DP DP DP DP DP DP DL 00 00 00
 * 
 * Key:
 * DP: First 28 bytes of the +3e "extended XDPB"
 * DL: Plus3dos drive letter (or null)
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.partitions.cpm.DirectoryEntry;
import hddEditor.libs.partitions.cpm.Dirent;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;

public class PLUS3DOSPartition extends CPMPartition {
	// +3DOS version information
	public static final int PLUS3ISSUE = 1;
	public static final int PLUS3VERSION = 0;

	// +3DOS file header identifier
	public static final byte stdheader[] = { 'P', 'L', 'U', 'S', '3', 'D', 'O', 'S', 0x1a, PLUS3ISSUE, PLUS3VERSION };

	// Assigned drive letter for the drive
	public String DriveLetter = "";

	// Freeze flag for the XDPB Not really used
	public boolean FreezeFlag = false;

	/**
	 * 
	 * @param tag
	 * @param disk
	 * @param RawPartition
	 * @throws IOException
	 */
	public PLUS3DOSPartition(int tag, Disk disk, byte[] RawPartition, int DirentNum, boolean Initialise)
			throws IOException {
		super(tag, disk, RawPartition, DirentNum, Initialise);
	}

	/**
	 * This function populates the +3 specific information The first 28 bytes of the
	 * extended area is the +3e Extended XDPB, Byte 29 is the +3DOS drive letter
	 * assigned
	 * 
	 * @throws IOException
	 */
	@Override
	protected void LoadPartitionSpecificInformation() throws IOException {
		byte extra[] = getExtra();
		// First, sort out the drive letter
		DriveLetter = letterOrBlank(extra[28]);
		// Now the XDPB
		RecordsPerTrack = ((extra[1] & 0xff) * 0x100) + (extra[0] & 0xff);
		BlockSizeShift = (extra[2] & 0xff);
		BlockSize = 128 << BlockSizeShift;
		BLM = (extra[3] & 0xff);
		ExtentMask = (extra[4] & 0xff);
		MaxBlock = ((extra[6] & 0xff) * 0x100) + (extra[5] & 0xff);
		MaxDirent = ((extra[8] & 0xff) * 0x100) + (extra[7] & 0xff);
		AL0 = (extra[9] & 0xff);
		AL1 = (extra[10] & 0xff);
		CheckSumVectorSize = ((extra[12] & 0xff) * 0x100) + (extra[11] & 0xff);
		ReservedTracks = ((extra[14] & 0xff) * 0x100) + (extra[13] & 0xff);
		SectorSizeShift = (extra[15] & 0xff);
		PHMSectorSize = (extra[16] & 0xff);
		Sidedness = (extra[17] & 0xff);
		TracksPerSide = (extra[18] & 0xff);
		SectorsPerTrack = (extra[19] & 0xff);
		FirstSector = (extra[20] & 0xff);
		SectorSize = ((extra[22] & 0xff) * 0x100) + (extra[21] & 0xff);
		GapLengthRW = (extra[23] & 0xff);
		GapLengthFmt = (extra[24] & 0xff);
		Flags = (extra[25] & 0xff);
		FreezeFlag = (extra[26] != 0);

		diskSize = (MaxBlock * BlockSize) / 1024;

		// add in the reserved blocks
		DirectoryBlocks = (MaxDirent + 1) * 0x20 / BlockSize;
		if (((MaxDirent + 1) * 0x20 % BlockSize) != 0) {
			BlockSize++;
		}

		ExtractDirectoryListing();
	}

	/**
	 * Save a passed in data with the given filename. as CODE
	 * 
	 * @param filename
	 * @param address
	 * @param data
	 */
	public void AddRawCodeFile(String filename, int address, byte[] data) {
		AddPlusThreeFile(filename, data, address, 0, Speccy.BASIC_CODE);
	}

	/**
	 * 
	 * @param nameOnDisk
	 * @param basicAsBytes
	 * @param line
	 * @param basicoffset
	 */
	public void AddBasicFile(String nameOnDisk, byte[] basicAsBytes, int line, int basicoffset) {
		AddPlusThreeFile(nameOnDisk, basicAsBytes, line, basicoffset, Speccy.BASIC_BASIC);
	}

	/**
	 * Add a given file as a +3DOS file.
	 * 
	 * @param nameOnDisk
	 * @param bytes
	 * @param Var1
	 * @param Var2
	 * @param type
	 */
	public void AddPlusThreeFile(String nameOnDisk, byte[] bytes, int Var1, int Var2, int type) {
		try {
			int cpmlen = bytes.length + 0x80;
			// allocate memory for filename and +3Dos header
			byte rawbytes[] = new byte[cpmlen];
			// Load file to memory.
			for (int i = 0; i < bytes.length; i++) {
				rawbytes[i + 0x80] = bytes[i];
			}

			// Make the +3DOS header
			for (int i = 0; i < stdheader.length; i++) {
				rawbytes[i] = stdheader[i];
			}

			// Add in the file size
			for (int i = 0; i < 4; i++) {
				int byt = (cpmlen & 0xff);
				rawbytes[i + 11] = (byte) (byt & 0xff);
				cpmlen = cpmlen / 0x100;
			}
			// Now the +3 basic header
			rawbytes[15] = (byte) (type & 0xff);
			rawbytes[16] = (byte) ((bytes.length % 0x100) & 0xff);
			rawbytes[17] = (byte) ((bytes.length / 0x100) & 0xff);
			rawbytes[18] = (byte) ((Var1 % 0x100) & 0xff);
			rawbytes[19] = (byte) ((Var1 / 0x100) & 0xff);
			rawbytes[20] = (byte) ((Var2 % 0x100) & 0xff);
			rawbytes[21] = (byte) ((Var2 / 0x100) & 0xff);

			// Calculate the checksum.
			int checksum = 0;
			for (int i = 0; i < 127; i++) {
				checksum = checksum + (rawbytes[i] & 0xff);
			}
			rawbytes[127] = (byte) (checksum & 0xff);

			AddCPMFile(nameOnDisk, rawbytes);
		} catch (Exception E) {
			E.printStackTrace();
		}
	}

	/**
	 * Add in some +3 specific stuff.
	 */
	public String toString() {
		String result = "";
		if (DriveLetter.isBlank()) {
			result = result + "\ndrive letter: None";
		} else {
			result = result + "\ndrive letter: " + DriveLetter + ": ";
		}
		result = result + "\tValid: " + IsValid;
		result = result + "\tFreeze Flag: " + FreezeFlag + "\n" + super.toString();

		return (result);
	}

	/**
	 * Intialise the +3 specific information
	 */
	@Override
	public void SetSensibleDefaults() {
		// First, sort out the drive letter
		DriveLetter = "";
		// Now the XDPB
		// Records per track.
		byte extra[] = getExtra();
		extra[0] = 0x00;
		extra[1] = 0x02;

		// Block size shift (8192)
		extra[2] = 0x06;

		// BLM Block size / 128 -1
		extra[3] = 0x3F;

		// Extent mask
		extra[4] = 0x03;

		// MAX DIRENT (0x1ff = 511)
		extra[7] = (byte) (0xff & 0xff);
		extra[8] = 0x01;

		// AL0 (1100 000 = first 2 blocks used for directory
		extra[9] = (byte) (0xC0 & 0xff);

		// AL1
		extra[10] = 0x00;

		// Checksum vector size
		extra[11] = 0x00;
		extra[12] = (byte) (0x80 & 0xff);

		// Reserved tracks
		extra[13] = 0x00;
		extra[14] = 0x00;

		// Sector size shift (2=512)
		extra[15] = 0x02;

		// PHM sector size / 128 -1
		extra[16] = 0x03;

		// Sidedness (single sided)
		extra[17] = 0x00;

		// Tracks per side
		extra[18] = (byte) (CurrentDisk.GetNumHeads() & 0xff);

		// Sectors per track
		extra[19] = (byte) (CurrentDisk.GetNumSectors() & 0xff);

		// First sector
		extra[20] = 0x00;

		// Sector size = 512;
		extra[21] = 0x00;
		extra[22] = 0x02;

		// Gap Lengths (Unused for HD's)
		extra[23] = 0x00;
		extra[24] = 0x00;

		// Flags (Used for the FDC, so not used)
		extra[25] = 0x00;

		// Freeze flags
		extra[26] = 0x00;

		// Extra bytes from +3DOS
		extra[27] = 0x00;
		extra[28] = 0x00; // Mapped drive
		extra[29] = 0x00;
		extra[30] = 0x00;
		extra[31] = 0x00;

		// Max block number (Note, +3Dos assumes sectors are always 512 bytes)
		// Assumptions: block = 8192 bytes.
		MaxBlock = (int) (GetEndSector() * 512) / 8192;
		extra[5] = (byte) ((MaxBlock % 0x100) & 0xff);
		extra[6] = (byte) ((MaxBlock / 0x100) & 0xff);

		SetExtra(extra);

		try {
			// Set the defaults..
			CreateBlankDirectoryArea();

			// Backfill to the local variables.
			LoadPartitionSpecificInformation();
		} catch (IOException e) {
		}
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
			int ScreenAction, int MiscAction, int SwapAction, ProgressCallback progress) throws IOException {
		try {
			FileWriter SysConfig = new FileWriter(new File(folder, "partition.index"));
			try {
				SysConfig.write("<speccy>\n".toCharArray());
				int entrynum = 0;
				for (DirectoryEntry entry : DirectoryEntries) {
					if (progress != null) {
						if (progress.Callback(DirectoryEntries.length, entrynum++, "File: " + entry.GetFilename())) 
							break;
					}
					File TargetFilename = new File(folder, entry.GetFilename().trim());
					Plus3DosFileHeader p3d = entry.GetPlus3DosHeader();
					byte entrydata[] = entry.GetFileData();
					byte Rawentrydata[] = entry.GetFileData();
					boolean isUnknown = true;
					if (p3d.IsPlusThreeDosFile) {
						// strip the +3DOS header
						byte newdata[] = new byte[entrydata.length - 0x80];
						System.arraycopy(entrydata, 0x80, newdata, 0, newdata.length);
						entrydata = newdata;
						isUnknown = false;
					}

					int SpeccyFileType = 0;
					int basicLine = 0;
					int basicVarsOffset = entrydata.length;
					int codeLoadAddress = 0;
					int filelength = 0;
					String arrayVarName = "";
					if (p3d == null || !p3d.IsPlusThreeDosFile) {
						SpeccyFileType = Speccy.BASIC_CODE;
						codeLoadAddress = 0x10000 - entrydata.length;
						filelength = entrydata.length;
					} else {
						filelength = p3d.filelength;
						SpeccyFileType = p3d.filetype;
						basicLine = p3d.line;
						basicVarsOffset = p3d.VariablesOffset;
						codeLoadAddress = p3d.loadAddr;
						arrayVarName = p3d.VarName;
					}
					try {
						int actiontype = GeneralUtils.EXPORT_TYPE_RAW;
						if (isUnknown) { // Options are: "Raw", "Hex", "Assembly"
							actiontype = MiscAction;
						} else {
							// Identifed BASIC File type
							if (SpeccyFileType == Speccy.BASIC_BASIC) { // Options are: "Text", "Raw", "Raw+Header", "Hex"
								actiontype = BasicAction;
							} else if ((SpeccyFileType == Speccy.BASIC_NUMARRAY) && (SpeccyFileType == Speccy.BASIC_CHRARRAY)) {
								actiontype = ArrayAction;
							} else if ((filelength == 6912)) { // { "PNG", "GIF", "JPEG", "Raw",
																								// "Raw+Header", "Hex", "Assembly" };
								actiontype = ScreenAction;
							} else { // CODE Options: { "Raw", "Raw+Header", "Assembly", "Hex" };
								actiontype = CodeAction;
							}
						}
						
						Speccy.SaveFileToDiskAdvanced(TargetFilename, entrydata, Rawentrydata, filelength, SpeccyFileType, basicLine,
								basicVarsOffset, codeLoadAddress, arrayVarName, actiontype);
					} catch (Exception E) {
						System.out.println("\nError extracting " + TargetFilename + "For folder: " + folder + " - "
								+ E.getMessage());
						E.printStackTrace();
					}

					SysConfig.write("<file>\n".toCharArray());
					SysConfig.write(("   <filename>" + entry.GetFilename().trim() + "</filename>\n").toCharArray());
					SysConfig.write(("   <deleted>" + entry.IsDeleted + "</deleted>\n").toCharArray());
					SysConfig.write(("   <errors>" + entry.Errors + "</errors>\n").toCharArray());
					SysConfig.write(("   <filelength>" + entry.GetFileSize() + "</filelength>\n").toCharArray());
					SysConfig.write("   <cpm:blocks>\n".toCharArray());
					int blocks[] = entry.getBlocks();
					for (int i : blocks) {
						SysConfig.write(("       <blockID>" + i + "</blockID>\n").toCharArray());
					}
					SysConfig.write("   </cpm:blocks>\n".toCharArray());
					SysConfig.write("   <cpm:dirents>\n".toCharArray());
					for (Dirent d : entry.dirents) {
						SysConfig.write(("       <dirent>" + d.toString() + "</dirent>\n").toCharArray());
					}
					SysConfig.write("   </cpm:dirents>\n".toCharArray());

					if (p3d == null || !p3d.IsPlusThreeDosFile) {
						// Treat CPM files as raw files.
						SysConfig.write("   <origfiletype>CPM</origfiletype>\n".toCharArray());
						SysConfig.write(("   <specbasicinfo>\n".toCharArray()));
						SysConfig.write(("       <filetype>3</filetype>\n").toCharArray());
						SysConfig.write(("       <filetypename>" + Speccy.FileTypeAsString(3) + "</filetypename>\n")
								.toCharArray());
						SysConfig.write(("       <codeloadaddr>32768</codeloadaddr>\n").toCharArray());
						SysConfig.write(("   </specbasicinfo>\n".toCharArray()));
					} else {
						SysConfig.write("   <origfiletype>PLUS3DOS</origfiletype>\n".toCharArray());
						SysConfig.write(("   <specbasicinfo>\n".toCharArray()));
						SysConfig.write(("       <filetype>" + p3d.filetype + "</filetype>\n").toCharArray());
						SysConfig.write(
								("       <filetypename>" + Speccy.FileTypeAsString(p3d.filetype) + "</filetypename>\n")
										.toCharArray());
						SysConfig.write(("       <basicsize>" + p3d.filelength + "</basicsize>\n").toCharArray());
						SysConfig.write(("       <basicstartline>" + p3d.line + "</basicstartline>\n").toCharArray());
						SysConfig.write(("       <codeloadaddr>" + p3d.loadAddr + "</codeloadaddr>\n").toCharArray());
						SysConfig.write(("       <basicvarsoffset>" + p3d.VariablesOffset + "</basicvarsoffset>\n")
								.toCharArray());
						SysConfig.write(("       <arrayvarname>" + p3d.VarName + "</arrayvarname>\n").toCharArray());
						SysConfig.write(("   </specbasicinfo>\n".toCharArray()));
						SysConfig.write(("   <specplus3>\n".toCharArray()));
						SysConfig.write(("       <signature>" + p3d.Signature + "</signature>\n").toCharArray());
						SysConfig.write(("       <softEOF>" + p3d.SoftEOF + "</softEOF>\n").toCharArray());
						SysConfig.write(("       <iss>" + p3d.IssueNo + "</iss>\n").toCharArray());
						SysConfig.write(("       <ver>" + p3d.VersionNo + "</ver>\n").toCharArray());
						SysConfig.write(("       <rawsize>" + p3d.fileSize + "</rawsize>\n").toCharArray());
						SysConfig.write(("       <checksum>" + p3d.CheckSum + "</checksum>\n").toCharArray());
						SysConfig.write(("   </specplus3>\n".toCharArray()));
					}

					SysConfig.write("</file>\n".toCharArray());
				}

				SysConfig.write("</speccy>\n".toCharArray());
			} finally {
				SysConfig.close();
			}
		} catch (IOException e) {
			System.out.println("Error extracting files: " + e.getMessage());
			e.printStackTrace();
		}
	}

}

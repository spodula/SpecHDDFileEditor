package hddEditor.libs.partitions;

import java.io.IOException;
import java.util.ArrayList;

import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FDD.BadDiskFileException;
import hddEditor.libs.disks.LINEAR.MDFMicrodriveFile;
import hddEditor.libs.disks.LINEAR.MicrodriveSector;
import hddEditor.libs.partitions.mdf.MicrodriveDirectoryEntry;

public class SinclairMicrodrivePartition extends IDEDosPartition {
	public MicrodriveDirectoryEntry Files[];

	/**
	 * Create a microdrive virtual partition.
	 * 
	 * @param DirentLocation
	 * @param RawDisk
	 * @param RawPartition
	 * @param DirentNum
	 * @param Initialise
	 * @throws IOException
	 */
	public SinclairMicrodrivePartition(int DirentLocation, Disk RawDisk, byte[] RawPartition, int DirentNum,
			boolean Initialise) throws IOException {
		super(DirentLocation, RawDisk, RawPartition, DirentNum, false);
	}

	/**
	 * Test harness.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		MDFMicrodriveFile tdf;
		try {
//			tdf = new MDFMicrodriveFile("/home/graham/rothi.mdr");
//			tdf = new MDFMicrodriveFile("/home/graham/DEMO.MDR");
			tdf = new MDFMicrodriveFile("/home/graham/test.mdr");
			SinclairMicrodrivePartition trp = new SinclairMicrodrivePartition(0, tdf, new byte[64], 1, false);
			System.out.println(tdf);
//			trp.Files[0].RenameMicrodriveFile("Test2", tdf);
//			trp.Files[0].RenameMicrodriveFile("demo", tdf);
//			trp.DeleteMicrodriveFile("Demo");
			
			//Should require 14 sectors.
			byte data[] = new byte[6912];
			data[0] = 0x00;

			data[1] = 0x00;
			data[2] = 0x02;
			
			data[3] = 0x05;
			data[4] = 0x5d;

			data[5] = 0x00;
			data[6] = 0x0a;
			
			data[7] = 0x00;
			data[8] = 0x00;
			
//			System.out.println(trp.AddMicrodriveFile("TestMDFFile", data));
			System.out.println(trp);
			System.out.println("------------------------------------------------");
			System.out.println(tdf);

			/*
			 * System.out.println("------------------------------------------------"); for
			 * (MicrodriveDirectoryEntry mdf:trp.Files) { String fn =
			 * (mdf.GetFilename()+"          ").substring(0,10); System.out.print(fn+"  ");
			 * byte data[] = mdf.GetFileRawData();
			 * System.out.print(GeneralUtils.HexDump(data, 0, 0x0a)); }
			 * 
			 * System.out.println("------------------------------------------------");
			 * StringBuilder sb = new StringBuilder(); MicrodriveDirectoryEntry file =
			 * trp.GetFileByName("run"); byte data[] = file.GetFileData();
			 * Speccy.DecodeBasicFromLoadedFile(data,sb, file.GetVarStart(), true, false);
			 * System.out.println(sb);
			 * System.out.println("------------------------------------------------");
			 */

		} catch (IOException | BadDiskFileException e) {
			e.printStackTrace();
		}

	}

	/**
	 * get the number of free sectors on the microdrive.
	 * 
	 * @return
	 */
	public int NumFreeSectors() {
		int result = 0;
		MDFMicrodriveFile mdf = (MDFMicrodriveFile) CurrentDisk;
		for (MicrodriveSector Sector : mdf.Sectors) {
			if (Sector.IsInUse()) {
				result++;
			}
		}
		return (result);
	}

	/**
	 * Load the partition specific information
	 */
	@Override
	public void LoadPartitionSpecificInformation() {
		RawPartition = new byte[0x40];
		MDFMicrodriveFile mdf = (MDFMicrodriveFile) CurrentDisk;

		SetPartType(PLUSIDEDOS.PARTITION_TAPE_SINCLAIRMICRODRIVE);
		SetStartCyl(0);
		SetEndCyl(0);
		SetStartHead(0);
		SetEndHead(0);
		SetEndSector((long) mdf.Sectors.length);

		ArrayList<MicrodriveDirectoryEntry> dirents = new ArrayList<MicrodriveDirectoryEntry>();

		for (MicrodriveSector Sector : mdf.Sectors) {
			if (Sector.IsInUse()) {
				String filename = Sector.getFileName();
				MicrodriveDirectoryEntry mde = null;
				for (MicrodriveDirectoryEntry md : dirents) {
					if (md.GetFilename().equals(filename)) {
						mde = md;
					}
				}

				if (mde == null) {
					mde = new MicrodriveDirectoryEntry();
					mde.SetFilename(filename);
				} else {
					dirents.remove(mde);
				}
				mde.AddSector(Sector);
				dirents.add(mde);
			}
		}

		Files = dirents.toArray(new MicrodriveDirectoryEntry[0]);
	}

	/**
	 * return details as a string.
	 */
	@Override
	public String toString() {
		MDFMicrodriveFile mdf = (MDFMicrodriveFile) CurrentDisk;
		String result = "Filename: " + mdf.GetFilename();
		result = result + "\ncart name: " + mdf.Sectors[0].getVolumeName();
		result = result + "\nNumSectors: " + mdf.GetNumSectors();

		result = result + "\n\nFiles: ";
		for (MicrodriveDirectoryEntry mde : Files) {
			result = result + "\n" + mde.GetFilename() + " sectors:" + mde.sectors.length + " type:";
			result = result + Speccy.FileTypeAsString(mde.GetFiletype());
			result = result + " File length: " + mde.GetFileSize();
			result = result + " Var2: " + mde.GetVar2();
			result = result + " BASIC Variables: " + mde.GetVarStart();
			result = result + " BASIC Line: " + mde.GetLineStart();
		}
		return (result);
	}

	/**
	 * Get a file by name. (Case insensitive)
	 * 
	 * @param filename
	 * @return
	 */
	public MicrodriveDirectoryEntry GetFileByName(String filename) {
		MicrodriveDirectoryEntry result = null;
		filename = filename.trim().toUpperCase();
		for (MicrodriveDirectoryEntry file : Files) {
			if (file.GetFilename().trim().toUpperCase().equals(filename)) {
				result = file;
			}
		}
		return (result);
	}

	/**
	 * Change the cartridge name
	 * 
	 * @param to
	 */
	public void RenameCart(String to) {
		MDFMicrodriveFile mdf = (MDFMicrodriveFile) CurrentDisk;
		for (MicrodriveSector Sector : mdf.Sectors) {
			Sector.setVolumeName(to);
			Sector.CalculateSectorChecksum();
			try {
				Sector.UpdateSectorOnDisk(mdf);
			} catch (IOException e) {
				System.out.println("Cannot rename cartridge. Is it read-only?");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	public String GetCartName() {
		MDFMicrodriveFile mdf = (MDFMicrodriveFile) CurrentDisk;
		return (mdf.Sectors[0].getVolumeName());
	}

	/**
	 * 
	 * @param filename
	 * @throws IOException 
	 */
	public void DeleteMicrodriveFile(String filename) throws IOException {
		// find the file in the list
		int foundfile = -1;
		filename = filename.trim().toUpperCase();
		for (int i = 0; i < Files.length; i++) {
			if (Files[i].GetFilename().trim().toUpperCase().equals(filename)) {
				foundfile = i;
			}
		}
		if (foundfile == -1) {
			System.out.println("File not found.");
		} else {
			// Blank the sectors
			MicrodriveDirectoryEntry mde = Files[foundfile];
			for (MicrodriveSector Sector : mde.sectors) {
				Sector.setSectorFlagByte(0x00);
				Sector.CalculateHeaderChecksum();
				// blank file information.
				for (int i = 19; i < Sector.SectorHeader.length; i++) {
					Sector.SectorHeader[i] = 0x00;
				}
				// blank file.
				for (int i = 0; i < Sector.SectorData.length; i++) {
					Sector.SectorData[i] = 0x00;
				}
				// update sector.
				try {
					Sector.UpdateSectorOnDisk((MDFMicrodriveFile) CurrentDisk);
				} catch (IOException e) {
					System.out.println("Cannot Set file on cartridge. Is it read-only?");
					throw new IOException("Cannot Set file on cartridge. Is it read-only?");
				}
			}
			// remove the filename from the list.
			MicrodriveDirectoryEntry newfiles[] = new MicrodriveDirectoryEntry[Files.length - 1];
			int entryNum = 0;
			for (int i = 0; i < Files.length; i++) {
				if (i != foundfile) {
					newfiles[entryNum++] = Files[i];
				}
			}
			Files = newfiles;
		}
	}

	/**
	 *Add a file to the microdrive, note this does NOT set the basic header
	 *this has to be done by the calling function.
	 * 
	 * @param filename
	 * @param data
	 * @return
	 * @throws IOException 
	 */
	private boolean AddMicrodriveFile(String filename, byte data[]) throws IOException {
		boolean result = false;
		System.out.println("Saving " + filename + " length: " + data.length);
		/*
		 * How many sectors do we need?
		 */
		int NumRequiredSectors = data.length / 0x200;
		if (data.length % 0x200 != 0) {
			NumRequiredSectors++;
		}
		System.out.println("Need " + NumRequiredSectors + " Sectors.");
		/*
		 * Find a run of consecutive sectors
		 */
		MDFMicrodriveFile mdf = (MDFMicrodriveFile) CurrentDisk;
		int SectorRunStart = -1;
		int SectorRunCount = 0;
		int FinalStartSector = -1;
		for (int i = 0xfe; i > -1; i--) {
			MicrodriveSector mds = mdf.GetSectorBySectorNumber(i);
			if (mds == null) {
				SectorRunStart = -1;
			} else {
				if (mds.IsInUse()) {
					SectorRunStart = -1;
				} else {
					if (SectorRunStart == -1) {
						SectorRunStart = i;
						SectorRunCount = 1;
					} else {
						SectorRunCount++;
					}
					if (SectorRunCount == NumRequiredSectors) {
						FinalStartSector = SectorRunStart;
						break;
					}
				}
			}
		}
		if (FinalStartSector == -1) {
			System.out.println("Cant find a run of that many sectors.");
		} else {
			System.out.println("Found sector run starting at: "+FinalStartSector);
			/*
			 * Write the sectors in turn.
			 */
			mdf.SetMDLogicalBlockFromSector(FinalStartSector, data, true, true, filename);

			/*
			 * Add file to the filename list.
			 */
			LoadPartitionSpecificInformation();
			result = true;
		}
		return (result);
	}
	
	/**
	 * Add a CODE file to the microdrive.
	 * 
	 * @param filename
	 * @param CodeFile
	 * @param loadAddress
	 * @return
	 * @throws IOException
	 */
	public boolean AddCodeFile(String filename, byte[] CodeFile, int loadAddress) throws IOException {
		//Create an array with space for the header, and add the file in. 
		byte data[] = new byte[CodeFile.length+9];
		System.arraycopy(CodeFile, 0, data, 9, CodeFile.length);
		//Create the header
		data[0] = Speccy.BASIC_CODE;
		data[1] = (byte) ((CodeFile.length % 0x100) & 0xff);
		data[2] = (byte) ((CodeFile.length / 0x100) & 0xff);
		data[3] = (byte) ((loadAddress % 0x100) & 0xff);
		data[4] = (byte) ((loadAddress / 0x100) & 0xff);
		data[5] = (byte) (0xff & 0xff);
		data[6] = (byte) (0xff & 0xff);
		data[7] = (byte) (0xff & 0xff);
		data[8] = (byte) (0xff & 0xff);

		//add the file.
		return(AddMicrodriveFile(filename, data));
	}
	
	/**
	 * Add an pre-encoded BASIC file to the microdrive. 
	 * @param filename
	 * @param EncodedBASICFile
	 * @param varsStart
	 * @param line
	 * @return
	 * @throws IOException
	 */
	public boolean AddBasicFile(String filename, byte[] EncodedBASICFile, int varsStart, int line) throws IOException {
		//Create an array with space for the header, and add the file in. 
		byte data[] = new byte[EncodedBASICFile.length+9];
		System.arraycopy(EncodedBASICFile, 0, data, 9, EncodedBASICFile.length);
		
		//Create the header
		data[0] = Speccy.BASIC_BASIC;
		data[1] = (byte) ((EncodedBASICFile.length % 0x100) & 0xff); //File length inc variables
		data[2] = (byte) ((EncodedBASICFile.length / 0x100) & 0xff);
		data[3] = (byte) ((23813 % 0x100) & 0xff); //Default load address. (Fixed for BASIC)
		data[4] = (byte) ((23813 / 0x100) & 0xff);
		data[5] = (byte) ((varsStart % 0x100) & 0xff);  //Start of the variables area
		data[6] = (byte) ((varsStart / 0x100) & 0xff);
		data[7] = (byte) ((line % 0x100) & 0xff);  //Start line.
		data[8] = (byte) ((line / 0x100) & 0xff);
		
		//add the file.
		return(AddMicrodriveFile(filename, data));
	}

	/**
	 * Add an pre-encoded character array to the microdrive. 
	 * 
	 * @param filename
	 * @param EncodedArray
	 * @param varname
	 * @return
	 * @throws IOException
	 */
	public boolean AddCharArray(String filename, byte[] EncodedArray, String varname) throws IOException {
		varname = (varname+"A").toUpperCase();
		byte varenc = (byte) (varname.charAt(0) & 0xff);
		varenc = (byte) (varenc - 'A');
		varenc = (byte) ((varenc | 0xC0) & 0xff);
		
		//Create an array with space for the header, and add the file in. 
		byte data[] = new byte[EncodedArray.length+9];
		System.arraycopy(EncodedArray, 0, data, 9, EncodedArray.length);
		//Create the header
		data[0] = Speccy.BASIC_CHRARRAY;
		data[1] = (byte) ((EncodedArray.length % 0x100) & 0xff); //File length inc variables
		data[2] = (byte) ((EncodedArray.length / 0x100) & 0xff);
		data[3] = (byte) ((24114 % 0x100) & 0xff); //Default load address. Ignored, but have to put something there. 
		data[4] = (byte) ((24114 / 0x100) & 0xff);
		data[5] = (byte) (varenc & 0xff);  //Encoded variable name and variable type.
		data[6] = (byte) (byte) (0xff & 0xff);
		data[7] = (byte) (byte) (0xff & 0xff);
		data[8] = (byte) (byte) (0xff & 0xff);
		
		//add the file.
		return(AddMicrodriveFile(filename, data));
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
	public boolean AddNumericArray(String filename, byte[] EncodedArray, String varname) throws IOException {
		varname = (varname+"A").toUpperCase();
		byte varenc = (byte) (varname.charAt(0) & 0xff);
		varenc = (byte) (varenc - 'A');
		varenc = (byte) ((varenc | 0x80) & 0xff);
		
		//Create an array with space for the header, and add the file in. 
		byte data[] = new byte[EncodedArray.length+9];
		System.arraycopy(EncodedArray, 0, data, 9, EncodedArray.length);
		//Create the header
		data[0] = Speccy.BASIC_NUMARRAY;
		data[1] = (byte) ((EncodedArray.length % 0x100) & 0xff); //File length inc variables
		data[2] = (byte) ((EncodedArray.length / 0x100) & 0xff);
		data[3] = (byte) ((24114 % 0x100) & 0xff); //Default load address. Ignored, but have to put something there. 
		data[4] = (byte) ((24114 / 0x100) & 0xff);
		data[5] = (byte) (varenc & 0xff);  //Encoded variable name and variable type.
		data[6] = (byte) (byte) (0xff & 0xff);
		data[7] = (byte) (byte) (0xff & 0xff);
		data[8] = (byte) (byte) (0xff & 0xff);
		
		//add the file.
		return(AddMicrodriveFile(filename, data));
	}

	/**
	 * Rename a named file.
	 * 
	 * @param filename
	 * @param newName
	 * @throws IOException 
	 */
	public void RenameFile(String filename, String newName) throws IOException {
		// find the file in the list
		int foundfile = -1;
		filename = filename.trim().toUpperCase();
		for (int i = 0; i < Files.length; i++) {
			if (Files[i].GetFilename().trim().toUpperCase().equals(filename)) {
				foundfile = i;
			}
		}
		if (foundfile == -1) {
			System.out.println("File "+filename+" not found.");
			throw new IOException("File "+filename+" not found.");
		} else {
			Files[foundfile].RenameMicrodriveFile(newName, CurrentDisk);
		}
	}


	/**
	 * GetAllDataInPartition overridden as sectors count backwards and are not in order as
	 * they are for most disks.
	 * 
	 * @return
	 * @throws IOException
	 */
	@Override
	public byte[] GetAllDataInPartition() throws IOException {
		byte allpossdata[] = new byte[255*512];
		MDFMicrodriveFile mdf = (MDFMicrodriveFile) CurrentDisk;
		int ptr=0;
		for (int i=0xfe;i>-1;i--) {
			MicrodriveSector mds = mdf.GetSectorBySectorNumber(i);
			if (mds!=null) {
				byte data[] = mds.SectorData;
				if (data!=null) {
					System.arraycopy(data, 0, allpossdata, ptr, data.length);
					ptr = ptr + data.length;
				}
			}
		}
		byte result[] = new byte[ptr];
		System.arraycopy(allpossdata, 0, result, 0, ptr);
		
		return (result);
	}
	
	@Override
	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public void SetAllDataInPartition(byte[] data) throws IOException {
		MDFMicrodriveFile mdf = (MDFMicrodriveFile) CurrentDisk;
		int ptr=0;
		int bytesleft = data.length;
		for (int i=0xfe;i>-1;i--) {
			MicrodriveSector mds = mdf.GetSectorBySectorNumber(i);
			if (mds!=null) {
				byte newdata[] = new byte[512];
				int currentlength = Math.min(bytesleft,512);
				
				System.arraycopy(data, ptr, newdata, 0, currentlength );
				mds.SectorData = newdata;
				mds.UpdateSectorOnDisk(mdf);
				
				ptr = ptr + currentlength;
				bytesleft = bytesleft - currentlength;
			}
		}
	}
	
	/**
	 * Get the size in Kbytes of the partition. 
	 * As usual, this differs for Microdrives. 
	 * 
	 * @return
	 */
	@Override
	public int GetSizeK() {
		MDFMicrodriveFile mdf = (MDFMicrodriveFile) CurrentDisk;
		int NumSectors = mdf.Sectors.length;
		return ((int) NumSectors / 2);
	}
	
	
}

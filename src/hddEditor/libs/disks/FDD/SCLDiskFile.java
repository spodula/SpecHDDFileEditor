package hddEditor.libs.disks.FDD;
/**
 * SCL file are just compressed TR-DOS files.
 * 00-07 - "SINCLAIR"  - header
 * 08 - Number of files in the file
 * [14 bytes x Number of files]
 * 		[+00-07] Filename
 *      [+08] File type
 *      [+09-0A] variables 1
 *      [+0B-0C] variables 2
 *      [+0D] - Number of sectors
 *      
 * After this, the sector data, one sector at a time.
 */     

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.partitions.trdos.TrdDirectoryEntry;

public class SCLDiskFile extends FloppyDisk {
	public static String Signature="SINCLAIR";
	
	private byte[] DiskInfoBlock = null;

	public boolean IsValid = false;
	public int FirstFreeSectorS = 0;
	public int FirstFreeSectorT = 0;
	public int LogicalDiskType = 0;
	public int NumFiles = 0;
	public int NumDeletedFiles = 0;
	public int NumFreeSectors = 0;
	public int TRDOSID = 0;
	public String Disklabel = "";

	private boolean BackupMade = false;

	/**
	 * 
	 * @param filename
	 * @throws IOException
	 * @throws BadDiskFileException
	 */
	public SCLDiskFile(String filename) throws IOException, BadDiskFileException {
		super(filename);
		IsValid = false;
		BackupMade = false;
		ParseDisk();
	}
	
	/**
	 * 
	 * @throws IOException
	 */
	private void ParseDisk() throws IOException {
		byte data[] = new byte[8];
		inFile.read(data);
		IsValid = new String(data).equals("SINCLAIR");

		if (IsValid) {
			LoadData();
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	private void LoadData() throws IOException {
		SetNumSectors(16);
		SetSectorSize(256);

		byte data[] = new byte[1];
		inFile.read(data);
		int numfiles = (data[0] & 0xff);

		ArrayList<TrdDirectoryEntry> headers = new ArrayList<TrdDirectoryEntry>();
		ArrayList<byte[]> FileData = new ArrayList<byte[]>();

		/**
		 * Load the directory entries
		 */
		for (int i = 0; i < numfiles; i++) {
			byte header[] = new byte[14];
			inFile.read(header);
//			System.out.println(GeneralUtils.HexDump(header, 0, 14,0));

			byte targetHeader[] = new byte[16];
			System.arraycopy(header, 0, targetHeader, 0, header.length);
			TrdDirectoryEntry entry = new TrdDirectoryEntry(null, i, targetHeader, false, i * 0x10);
			headers.add(entry);
		}

		/**
		 * Load the data
		 */
		int TotalSectors = 0;
		for (TrdDirectoryEntry entry : headers) {
			int numsectors = entry.GetFileLengthSectors();
			TotalSectors = TotalSectors + numsectors;
			int Datasize = numsectors * GetSectorSize();
			byte fData[] = new byte[Datasize];
			inFile.read(fData);
			FileData.add(fData);
		}

		/**
		 * Decide on a disk type:
		 */
		DiskInfoBlock = new byte[0x100];
		if (TotalSectors < 624) {
			LogicalDiskType = 0x19;
			DiskInfoBlock[0xe3] = 0x19;
			SetNumCylinders(40);
			SetNumHeads(1);
		} else if (TotalSectors < 1264) {
			LogicalDiskType = 0x19;
			DiskInfoBlock[0xe3] = 0x19;
			SetNumCylinders(80);
			SetNumHeads(1);
		} else {
			LogicalDiskType = 0x19;
			DiskInfoBlock[0xe3] = 0x19;
			SetNumCylinders(80);
			SetNumHeads(2);
		}

		/*
		 * Create a blank disk.
		 */
		diskTracks = new TrackInfo[GetNumHeads() * GetNumCylinders()];

		for (int track = 0; track < GetNumCylinders(); track++) {
			for (int head = 0; head < GetNumHeads(); head++) {
				TrackInfo NewTrack = new TrackInfo();
				NewTrack.minsectorID = 1;
				NewTrack.maxsectorID = GetNumSectors();
				NewTrack.numsectors = GetNumSectors();
				NewTrack.sectorsz = GetSectorSize();
				NewTrack.side = head;
				NewTrack.tracknum = track;
				// TrackStartPtr not applicable for SCL disks
				NewTrack.TrackStartPtr = 0;
				NewTrack.datarate = 1; // SS
				if (GetNumHeads() > 1) {
					NewTrack.datarate = 2; // DS (Alternating sides)
				}

				// Create each sector
				NewTrack.Sectors = new Sector[GetNumSectors()];
				for (int sNum = 0; sNum < GetNumSectors(); sNum++) {
					Sector newsect = new Sector();
					newsect.FDCsr1 = 0;
					newsect.FDCsr2 = 0;
					newsect.sectorID = sNum + 1;
					// Todo: Sector start
					newsect.SectorStart = 0;
					newsect.Sectorsz = GetSectorSize();
					newsect.side = head;
					newsect.track = track;

					byte sect[] = new byte[GetSectorSize()];

					newsect.ActualSize = sect.length;
					newsect.data = sect;
					NewTrack.Sectors[sNum] = newsect;
				}
				diskTracks[(GetNumHeads() * track) + head] = NewTrack;
			}
		}
		/**
		 * Update disk with the actual data.
		 */
		byte DirentSectors[] = new byte[0x900];
		int CurrentTrack = 1;
		int CurrentSector = 0;
		// Start with the directory entry.
		for (TrdDirectoryEntry entry : headers) {
			entry.SetStartSector(CurrentSector);
			entry.SetStartTrack(CurrentTrack);
			System.arraycopy(entry.DirEntryDescriptor, 0, DirentSectors, entry.DirentLoc, 0x10);
			// add in the file length...
			int numsectors = entry.GetFileLengthSectors();
			int Tracks = numsectors / GetNumSectors();
			int Sectors = numsectors % GetNumSectors();

			CurrentTrack = CurrentTrack + Tracks;
			CurrentSector = CurrentSector + Sectors;
			if (CurrentSector >= GetNumSectors()) {
				CurrentTrack++;
				CurrentSector = CurrentSector - GetNumSectors();
			}
		}
		// Create the Disk information block and write it
		int NumFreeSectors = (GetNumCylinders() * GetNumHeads() * GetNumSectors())
				- (CurrentTrack * GetNumSectors() + CurrentSector);
		this.FirstFreeSectorS = CurrentSector;
		this.FirstFreeSectorT = CurrentTrack;
		this.NumFiles = numfiles;
		this.NumFreeSectors = NumFreeSectors;
		this.TRDOSID = 0x10;
		DiskInfoBlock[0xe1] = (byte) (CurrentSector & 0xff);
		DiskInfoBlock[0xe2] = (byte) (CurrentTrack & 0xff);
		DiskInfoBlock[0xe4] = (byte) (numfiles & 0xff);
		DiskInfoBlock[0xe5] = (byte) ((NumFreeSectors % 0x100) & 0xff);
		DiskInfoBlock[0xe6] = (byte) ((NumFreeSectors / 0x100) & 0xff);
		DiskInfoBlock[0xe7] = 0x10;
		DiskInfoBlock[0xe9] = 0x20;
		// Take the first entry as the disk label.
		String diskname = "blank     ";
		if (headers.size() != 0) {
			diskname = headers.get(0).GetFilename().trim() + "        ";
		}
		for (int i = 0; i < 8; i++) {
			DiskInfoBlock[i + 0xf5] = (byte) diskname.charAt(i);
		}
		System.arraycopy(DiskInfoBlock, 0, DirentSectors, 0x800, 0x100);
		this.Disklabel = diskname.substring(0, 8);
		// Write the header.
		int ptr = 0;
		int sect = 0;
		while (ptr < DirentSectors.length) {
			System.arraycopy(DirentSectors, ptr, diskTracks[0].Sectors[sect].data, 0, GetSectorSize());
			ptr = ptr + diskTracks[0].Sectors[sect].data.length;
			sect++;
		}
		// Write the data.
		for (int i = 0; i < headers.size(); i++) {
			TrdDirectoryEntry entry = headers.get(i);
			byte file[] = FileData.get(i);
			System.out.println("--------------------------------------");
			System.out.println("File: " + entry.GetFilename() + " sectors: " + entry.GetFileLengthSectors() + " len:"
					+ entry.GetFileSize());
			System.out.println("bin: " + file.length);
			System.out.println("--------------------------------------");
			int fileptr = 0;
			int track = entry.GetStartTrack();
			int sector = entry.GetStartSector();
			while (fileptr < file.length) {
				System.arraycopy(file, fileptr, diskTracks[track].Sectors[sector].data, 0, GetSectorSize());
				fileptr = fileptr + GetSectorSize();
				sector++;
				if (sector == GetNumSectors()) {
					sector = 0;
					track++;
				}
			}
		}
	}

	/**
	 * 
	 */
	public SCLDiskFile() {
		inFile = null;
		filename = "";
		IsValid = false;
		SetNumCylinders(0);
	}

	/**
	 * A bit more complex than the simple Files.
	 */
	@Override
	public byte[] GetBytesStartingFromSector(long SectorNum, int sz) throws IOException {
		byte result[] = new byte[sz];
		// Find the track and sector...
		int TrStart = 0;
		int TrackNum = 0;
		TrackInfo Track = null;
		while (TrStart <= SectorNum) {
			Track = diskTracks[TrackNum++];
			TrStart = TrStart + Track.Sectors.length;
		}
		// Track is now the start track.
		TrackNum--;
		TrStart = TrStart - Track.Sectors.length;
		long FirstSector = SectorNum - TrStart + Track.minsectorID;

		int ptr = 0;
		byte sector[] = new byte[1];
		while ((ptr < sz) && (sector.length > 0)) {
			sector = Track.GetSectorBySectorID((int)FirstSector).data;
			System.arraycopy(sector, 0, result, ptr, Math.min(sector.length, result.length - ptr));

			ptr = ptr + sector.length;

			FirstSector++;
			if (FirstSector > Track.maxsectorID) {
				TrackNum++;
				if (TrackNum < diskTracks.length) {
					Track = diskTracks[TrackNum];
					FirstSector = Track.minsectorID;
				} else {
					sector = new byte[0];
				}
			}
		}
		return result;
	}

	/**
	 * Write block from logical sector.
	 */
	@Override
	public void SetLogicalBlockFromSector(long SectorNum, byte[] result) throws IOException {
		// Find the track and sector...
		int TrStart = 0;
		int TrackNum = 0;
		TrackInfo Track = null;
		while (TrStart <= SectorNum) {
			Track = diskTracks[TrackNum++];
			TrStart = TrStart + Track.Sectors.length;
		}
		// Track is now the start track.
		TrackNum--;
		TrStart = TrStart - Track.Sectors.length;
		long FirstSector = SectorNum - TrStart + Track.minsectorID;

		int ptr = 0;
		byte sectorData[] = new byte[1];
		while ((ptr < result.length)) {
			Sector sect = Track.GetSectorBySectorID((int)FirstSector);
			sectorData = sect.data;
			System.arraycopy(result, ptr, sectorData, 0, Math.min(sectorData.length, result.length - ptr));

			ptr = ptr + sectorData.length;

			FirstSector++;
			if (FirstSector > Track.maxsectorID) {
				TrackNum++;
				Track = diskTracks[TrackNum];
				FirstSector = Track.minsectorID;
			}
		}
	}

	/**
	 * Write the given sector back to disk.
	 * 
	 * @param sect
	 */
	/*
	 * private void WriteSector(Sector sect) { try { System.out.println();
	 * inFile.seek(sect.SectorStart); inFile.write(sect.data); } catch (IOException
	 * e) { System.out.println("Failed writing sector...." + e.getMessage());
	 * e.printStackTrace(); } }
	 */

	/**
	 * Add the extra details.
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
		return (result);
	}

	/**
	 * Can we open this file type....
	 */
	@Override
	public Boolean IsMyFileType(File filename) throws IOException {
		boolean result = false;

		inFile = new RandomAccessFile(filename, "rw");
		try {
			byte data[] = new byte[8];
			inFile.read(data);
			result = new String(data).equals("SINCLAIR");
		} finally {
			inFile.close();
		}
		return (result);
	}

	/**
	 * @throws IOException
	 * 
	 */
	public void OperationCompleted(TrdDirectoryEntry DirectoryEntries[]) throws IOException {
		/*
		 * Create a .bak file of the file if it hasnt been modified already...
		 */
		if (!BackupMade) {
			inFile.seek(0);
			byte oldFileData[] = new byte[(int) inFile.length()];
			inFile.read(oldFileData);

			File f = new File(GetFilename() + ".bak");
			GeneralUtils.WriteBlockToDisk(oldFileData, f.getAbsolutePath());
		}
		BackupMade = true;
		/*
		 * Write data
		 */
		// Header data
		inFile.seek(0);
		inFile.write("SINCLAIR".getBytes());
		byte b[] = new byte[1];
		b[0] = (byte) DirectoryEntries.length;
		inFile.write(b);

		// Directory entries
		for (TrdDirectoryEntry dirent : DirectoryEntries) {
			byte newdirent[] = new byte[14];
			System.arraycopy(dirent.DirEntryDescriptor, 0, newdirent, 0, newdirent.length);
			inFile.write(newdirent);
		}

		// data
		for (TrdDirectoryEntry dirent : DirectoryEntries) {
			int startsector = (dirent.GetStartTrack() * GetNumSectors()) + dirent.GetStartSector();
			int length = dirent.GetFileLengthSectors() * GetSectorSize();
			byte result[] = GetBytesStartingFromSector(startsector, length);
			inFile.write(result);
		}
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 * @throws BadDiskFileException
	 */
	public static void main(String[] args) {
		SCLDiskFile tdf;
		try {
			tdf = new SCLDiskFile("/home/graham/tmp/bj2.scl");
			System.out.println(tdf);
		} catch (IOException | BadDiskFileException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Create a blank SCL disk. this is possibly the simplest disk creation ever. 
	 * just a file containing "SINCLAIR[0]"
	 * 
	 * @param filename
	 * @throws IOException 
	 */
	public void CreateBlankSCLDisk(String filename) throws IOException {
		FileOutputStream NewFile = new FileOutputStream(filename);
		try {
			NewFile.write(Signature.getBytes());
			byte numfiles[] = new byte[1];
			numfiles[0] = 0x00;
			NewFile.write(numfiles);			
		} finally {
			// Close, forcing flush
			NewFile.close();
			NewFile = null;
		}
		
		/*
		 *  Load the newly created file.
		 */
		inFile = new RandomAccessFile(filename, "rw");
		this.filename = filename;
		FileSize = new File(filename).length();
		IsValid = false;
		BackupMade = false;
		ParseDisk();
	}
}

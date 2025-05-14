package hddEditor.libs.disks.FDD;
/*
 * MGT disk files (AKA DISCiPLE/+d/Sam Coupé Disk format). 
 * There are a couple of limitations to this object:
 * o Will only create disks of 80 tracks, 2 sides
 * o Will only detect disks of 80 tracks, 2 sides
 * 
 * Disks are 10 sectors/track 512 byte sectors Numbered 1-10
 * SS or DS
 * 40 or 80 tracks. only 80 tracks, DS disks are supported.
 * 
 * logical tracks are HC format
 * Tracks are logically labelled 0-79 (side 0) 128-207 (side 1)
 *  
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.MGT;

public class MGTDiskFile extends FloppyDisk {

	public boolean IsValid = false;

	/**
	 * Load and parse a disk
	 * 
	 * @param filename
	 * @throws IOException
	 * @throws BadDiskFileException
	 */
	public MGTDiskFile(File file) throws IOException, BadDiskFileException {
		super(file);
		IsValid = false;
		ParseDisk();
	}

	/**
	 * Re-load the track/sectors.
	 * 
	 * @throws IOException
	 */
	private void ParseDisk() throws IOException {
		// This is just a list of sectors. We will have to determine the number of heads
		// after loading.
		SetNumSectors(MGT.MGT_SECTORSPERTRACK);
		SetSectorSize(MGT.MGT_SECTORSIZE);
				
		long numLogicalTracks = GetFileSize() / GetSectorSize() / GetNumSectors();

		if (numLogicalTracks > 159) {
			SetNumCylinders(80);
			SetNumHeads(2);
		} else if (numLogicalTracks < 41) {
			SetNumCylinders(40);
			SetNumHeads(1);
		} else {
			// Note, no easy way to determine Single density OR single heads so just assume
			// 40 tracks, 2 sides as i don't think 80 tracks 1 side is a thing.
			SetNumCylinders(40);
			SetNumHeads(2);
		}

		diskTracks = new TrackInfo[(int) numLogicalTracks];
		inFile.seek(0);

		int headNum = 0;
		int trackNum = 0;
		while (trackNum != GetNumCylinders()) {
			TrackInfo NewTrack = new TrackInfo();
			NewTrack.minsectorID = 1;
			NewTrack.maxsectorID = GetNumSectors();
			NewTrack.numsectors = GetNumSectors();
			NewTrack.sectorsz = GetSectorSize();
			NewTrack.side = headNum;
			NewTrack.tracknum = trackNum;
			NewTrack.TrackStartPtr = (int) inFile.getFilePointer();
			NewTrack.datarate = 1; // SS
			if (GetNumHeads() > 1) {
				NewTrack.datarate = 3; // DS (successive sides)
			}
			// Read each sector
			NewTrack.Sectors = new Sector[GetNumSectors()];
			for (int sNum = 0; sNum < GetNumSectors(); sNum++) {
				Sector newsect = new Sector();
				newsect.FDCsr1 = 0;
				newsect.FDCsr2 = 0;
				newsect.sectorID = sNum + 1;
				newsect.SectorStart = inFile.getFilePointer();
				newsect.Sectorsz = GetSectorSize();
				newsect.side = headNum;
				newsect.track = trackNum;

				byte sect[] = new byte[GetSectorSize()];
				inFile.read(sect);

				newsect.ActualSize = sect.length;
				newsect.data = sect;

				NewTrack.Sectors[sNum] = newsect;
			}
			diskTracks[(GetNumHeads() * trackNum) + headNum] = NewTrack;

			// Point to the next track.
			headNum++;
			if (headNum == GetNumHeads()) {
				trackNum++;
				headNum = 0;
			}

		}
	}

	/**
	 * Create the object without openning a file.
	 */
	public MGTDiskFile() {
		inFile = null;
		file = null;
		IsValid = false;
		SetNumCylinders(0);
	}

	/**
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = "/home/graham/Desktop/testdisk-mgt.mgt";
		MGTDiskFile mdf;
		try {
			mdf = new MGTDiskFile(new File(filename));
			try {
				if (mdf.IsMyFileType(new File(filename))) {
					System.out.println("File is MGT");
				}
				byte data[] = mdf.GetBytesStartingFromSector(0, 2048);
				System.out.println(GeneralUtils.HexDump(data, 0, 2048, 0));
				// System.out.println(mdf);
			} finally {
				mdf.close();
				mdf = null;
			}
		} catch (IOException | BadDiskFileException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This differs by the default because of the track numbering of MGT disks.
	 */
	@Override
	public byte[] GetBytesStartingFromSector(long SectorNum, long sz) throws IOException {
		// Note, this will restrict length for MGT files of 128Mb.
		long resultSz = Math.min(sz, 1024 * 1024 * 128);
		byte result[] = new byte[(int) resultSz];

		// Find the start track and sector...
		int logicalSector = (int) (SectorNum % GetNumSectors()) + 1;
		int logicalTrack = (int) (SectorNum / GetNumSectors());
		int logicalHead = 0;

		// For MGT disks, cylinders are ordered 0-39/79 (side 0) then 128-167/207 (side
		// 1)
		if (logicalTrack > GetNumCylinders()) {
			logicalTrack = logicalTrack - 128;
			logicalHead = 1;
		}

		// How many sectors do we need?
		int NumSectors = (int) (sz / GetSectorSize()) + 1;
		if (sz % GetSectorSize() == 0) { // stop exact multiples eg, 512 asking for 2 sectors when it only needs one
			NumSectors--;
		}

		int ptr = 0;
		while (NumSectors > 0) {
			// Get Sector with LogicalSector, LogicalTrack, LogicalHead
			Sector sect = GetSectorByCHS(logicalTrack, logicalHead, logicalSector);
			// Copy to result
			System.arraycopy(sect.data, 0, result, ptr, sect.data.length);
			ptr = ptr + sect.data.length;

			// Point to next sector. Note, MGT disks are HCS format.
			logicalSector++;
			if (logicalSector == 11) {
				logicalSector = 1;
				logicalTrack++;
				if (logicalTrack > GetNumCylinders()) {
					logicalTrack = 0;
					logicalHead++;
				}
			}
			NumSectors--;
		}
		return (result);
	}

	/**
	 * Write the given sector back to disk.
	 * 
	 * @param sect
	 */
	private void WriteSector(Sector sect) {
		try {
			inFile.seek(sect.SectorStart);
			inFile.write(sect.data);
		} catch (IOException e) {
			System.out.println("Failed writing sector...." + e.getMessage());
			e.printStackTrace();
		}
		UpdateLastModified();
	}

	/**
	 * Write block from logical sector.
	 * 
	 * @param SectorNum
	 * @param result
	 */
	@Override
	public void SetLogicalBlockFromSector(long SectorNum, byte[] result) throws IOException {
		// Find the start track and sector...
		int logicalSector = (int) (SectorNum % GetNumSectors()) + 1;
		int logicalTrack = (int) (SectorNum / GetNumSectors());
		int logicalHead = 0;

		// For MGT disks, cylinders are ordered 0-39/79 (side 0) then 128-167/207 (side
		// 1)
		if (logicalTrack > GetNumCylinders()) {
			logicalTrack = logicalTrack - 128;
			logicalHead = 1;
		}

		// How many sectors do we need?
		int NumSectors = (int) (result.length / GetSectorSize()) + 1;
		if (result.length % GetSectorSize() == 0) { // stop exact multiples eg, 512 asking for 2 sectors when it only
													// needs one
			NumSectors--;
		}

		int ptr = 0;
		while (NumSectors > 0) {
			// Get Sector with LogicalSector, LogicalTrack, LogicalHead
			Sector sect = GetSectorByCHS(logicalTrack, logicalHead, logicalSector);
			if (sect == null) {
				throw new IOException("Sector: C:" + logicalTrack + " H:" + logicalHead + " S:" + logicalSector
						+ " cannot be found.");
			}

			// Copy to result
			System.arraycopy(result, ptr, sect.data, 0, Math.min(sect.data.length, result.length - ptr));
			ptr = ptr + sect.data.length;
			WriteSector(sect);

			// Point to next sector. Note, MGT disks are HCS format.
			logicalSector++;
			if (logicalSector > MGT.MGT_SECTORSPERTRACK) {
				logicalSector = 1;
				logicalTrack++;
				if (logicalTrack > GetNumCylinders()) {
					logicalTrack = 0;
					logicalHead++;
				}
			}
			NumSectors--;
		}
		UpdateLastModified();
	}

	/**
	 * Add the MGT specific info.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		return (result);
	}

	/**
	 * Create a file with the given number of tracks and sides.
	 * NOTE, creating a disk with values other than 40/80 tracks and 1/2 sides will produce
	 * an error.
	 * 
	 * @param Filename
	 * @param tracks
	 * @param sides
	 * @throws IOException
	 * @throws BadDiskFileException
	 */
	public void CreateBlankMGTDiskParams(File Filename, int tracks, int sides) throws IOException, BadDiskFileException {
		if (tracks != 40 && tracks != 80) {
			throw new BadDiskFileException("MGT disks must have either 40 or 80 tracks!");
		}
		if (sides != 1 && sides != 2) {
			throw new BadDiskFileException("MGT disks must have either 1 or 2 sides!");
		}
		
		FileOutputStream NewFile = new FileOutputStream(Filename);
		try {
			byte track[] = new byte[MGT.MGT_SECTORSIZE * MGT.MGT_SECTORSPERTRACK];
			// Blank the track
			Arrays.fill(track, (byte)0x00);

			/*
			 * Write the tracks.
			 */
			for (int tracknum = 0; tracknum < (sides*tracks); tracknum++) {
				NewFile.write(track);
			}
		} finally {
			NewFile.close();
			NewFile = null;
		}

		/*
		 * Load the newly created file.
		 */
		inFile = new RandomAccessFile(Filename, "rw");
		this.file = Filename;
		FileSize = file.length();
		IsValid = false;
		ParseDisk();
		
	}
	
	
	/**
	 * Create a blank disk, 80 tracks, 2 heads, 9 sectors per track, 512 bytes per
	 * sector.
	 * 
	 * @param Filename
	 * @param VolumeName
	 * @throws IOException
	 * @throws BadDiskFileException
	 */
	public void CreateBlankMGTDisk(File Filename) throws IOException, BadDiskFileException {
		FileOutputStream NewFile = new FileOutputStream(Filename);
		try {
			byte track[] = new byte[MGT.MGT_SECTORSIZE * MGT.MGT_SECTORSPERTRACK];
			// Blank the track
			Arrays.fill(track, (byte)0x00);
			/*
			 * Write the tracks.
			 */
			for (int tracknum = 0; tracknum < 160; tracknum++) {
				NewFile.write(track);
			}
		} finally {
			NewFile.close();
			NewFile = null;
		}

		/*
		 * Load the newly created file.
		 */
		inFile = new RandomAccessFile(Filename, "rw");
		this.file = Filename;
		FileSize = file.length();
		IsValid = false;
		ParseDisk();
	}

	/**
	 * Can we open this file type....
	 * 
	 * Ok, this is a pain in the arse for MGT files as there doesn't seem to have
	 * any IDS anywhere, either in the file or the filesystem. There is literally no
	 * way of telling a blank file of the correct length from any other valid file.
	 * 
	 * The best we can do is check the file extension, size, and check the catalogue
	 * to make sure there isn't any identifiable duff entries. Fortunately, for file
	 * lengths, all i have seen are 819200 (80 tracks, 2 heads, 10 sectors of 512
	 * bytes)
	 */
	@Override
	public Boolean IsMyFileType(File filename) throws IOException {
		boolean result = false;
		
		if ((filename.length() == 0xC8000) && filename.getName().toLowerCase().endsWith(".mgt")) { // length of 819200
			byte dirents[] = new byte[4 * 512 * 10]; // Load first 4 sectors...
			inFile = new RandomAccessFile(filename, "r");
			try {
				inFile.read(dirents);
			} finally {
				inFile.close();
			}
			result = true;
		}
		return (result);
	}
}

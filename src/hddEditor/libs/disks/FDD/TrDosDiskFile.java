package hddEditor.libs.disks.FDD;
/*
 * TRD disks are just raw floppy disk images with the TR-DOS filesystem on them. 
 * 
 * https://sinclair.wiki.zxnet.co.uk/wiki/TR-DOS_filesystem
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class TrDosDiskFile extends FloppyDisk {
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

	public TrDosDiskFile(String filename) throws IOException, BadDiskFileException {
		super(filename);
		IsValid = false;
		ParseDisk();
	}

	public TrDosDiskFile() {
		inFile = null;
		filename = "";
		IsValid = false;
		SetNumCylinders(0);
	}

	private void ParseDisk() throws IOException {
		LoadDPB();
		if (IsValid) {
			LoadData();
		}
	}

	/**
	 * Load the sector data. 
	 * Note, this assumes the disk is valid. 
	 * @throws IOException 
	 */
	private void LoadData() throws IOException {
		diskTracks = new TrackInfo[GetNumHeads() * GetNumCylinders()];
		inFile.seek(0);
		
		int headNum = 0;
		int trackNum = 0;
		while (trackNum < GetNumCylinders()) {
			TrackInfo NewTrack = new TrackInfo();
			NewTrack.minsectorID = 1;
			NewTrack.maxsectorID = GetNumSectors();
			NewTrack.numsectors = GetNumSectors();
			NewTrack.sectorsz = GetSectorSize();
			NewTrack.side = headNum;
			NewTrack.tracknum = trackNum;
			NewTrack.TrackStartPtr = (int) inFile.getFilePointer();
			NewTrack.datarate = 1; //SS
			if (GetNumHeads() > 1) {
				NewTrack.datarate = 2; //DS (Alternating sides)
			}
			
			//Read each sector
			NewTrack.Sectors = new Sector[GetNumSectors()];
			for (int sNum=0;sNum<GetNumSectors();sNum++) {
				Sector newsect = new Sector();
				newsect.FDCsr1 = 0;
				newsect.FDCsr2 = 0;
				newsect.sectorID = sNum+1;
				newsect.SectorStart = (int) inFile.getFilePointer();
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
			
			//Point to the next track. 
			headNum++;
			if (headNum == GetNumHeads()) {
				trackNum++;
				headNum = 0;
			}
		}
	}

	private void LoadDPB() throws IOException {
		// TRD disks are just basically arrays of sectors arranged in C/H/S order. 
		// 0/0/9 contains the disk information. Load these first
		//these values are common to all TR-DOS disks. 
		SetNumSectors(16);
		SetSectorSize(256);
		
		DiskInfoBlock = new byte[0x100];
		inFile.seek(8 * SectorSize);
		inFile.read(DiskInfoBlock);

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

		switch (LogicalDiskType) {
		case 0x16:
			SetNumCylinders(80);
			SetNumHeads(2);
			break;
		case 0x17:
			SetNumCylinders(40);
			SetNumHeads(2);
			break;
		case 0x18:
			SetNumCylinders(80);
			SetNumHeads(1);
			break;
		case 0x19:
			SetNumCylinders(40);
			SetNumHeads(1);
			break;

		}

		// Check some of the disk parameters for stupid values.
		if ((DiskInfoBlock[0] != 0x00) || (TRDOSID != 0x10)|| (DiskInfoBlock[0xff] != 0))
			IsValid = false;
		if ((LogicalDiskType < 0x16) || (LogicalDiskType > 0x19)) {
			IsValid = false;
		}
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 * @throws BadDiskFileException
	 */
	public static void main(String[] args) {
		TrDosDiskFile tdf;
		try {
			tdf = new TrDosDiskFile("/home/graham/tmp/ufo.trd");
			System.out.println(tdf);
		} catch (IOException | BadDiskFileException e) {
			e.printStackTrace();
		}

	}

	/**
	 * A bit more complex than the simple Files.
	 */
	@Override
	public byte[] GetBytesStartingFromSector(int SectorNum, int sz) throws IOException {
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
		int FirstSector = SectorNum - TrStart + Track.minsectorID;

		int ptr = 0;
		byte sector[] = new byte[1];
		while ((ptr < sz) && (sector.length > 0)) {
			sector = Track.GetSectorBySectorID(FirstSector).data;
			System.arraycopy(sector, 0, result, ptr, Math.min(sector.length, result.length - ptr));

			ptr = ptr + sector.length;

			FirstSector++;
			if (FirstSector > Track.maxsectorID) {
				TrackNum++;
				if (TrackNum < diskTracks.length ) {
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
	public void SetLogicalBlockFromSector(int SectorNum, byte[] result) throws IOException {
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
		int FirstSector = SectorNum - TrStart + Track.minsectorID;

		int ptr = 0;
		byte sectorData[] = new byte[1];
		while ((ptr < result.length)) {
			Sector sect = Track.GetSectorBySectorID(FirstSector);
			sectorData = sect.data;
			System.arraycopy(result, ptr, sectorData, 0, Math.min(sectorData.length, result.length - ptr));
			WriteSector(sect);

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
	private void WriteSector(Sector sect) {
		try {
			inFile.seek(sect.SectorStart);
			inFile.write(sect.data);
		} catch (IOException e) {
			System.out.println("Failed writing sector...."+e.getMessage());
			e.printStackTrace();
		}
	}

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
			LoadDPB();
			result = IsValid;
		} finally {
			inFile.close();
		}
		return (result);
	}

}

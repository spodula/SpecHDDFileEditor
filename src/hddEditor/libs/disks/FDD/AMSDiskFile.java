package hddEditor.libs.disks.FDD;

/**

 * Wrapper around an AMS file. (Used for Amstrad CPC and Spectrum +3 Disk images) 
 * Handles low level Sector/track parsing and reading the disk information 
 * structures.
 *
 * (NOTE: most of this information was gathered from  https://www.cpcwiki.eu/index.php/Format:DSK_disk_image_file_format )
 * 
 * AMS file:
 * The first 256 bytes are the "disk information block".
 * There are two types, Normal and Extended (I have found +3 disks in both) 
 * 
 * the main differences are:
 *   for NORMAL disks, the track size is defined in the DIB. 
 *   For EXTENDED disks, each track size is defined individually from byte $34 onwards. 
 *   Extended disks enable the encoding of copy-protected disks with variable sector and track sizes. 
 *   
 * 
 * Normal:
 * 	00-16 "MV - CPCEMU Disk-File\r\n"
 * 	17-21 "Disk-Info\r\n"
 * 	22-2f Name of the creator
 * 	30    Number of tracks
 * 	31    Number of sides
 * 	32-33 Track size 
 * 	34-FF Unused
 * 	
 * Extended:
 * 	00-16 "EXTENDED CPC DSK File\r\n"
 * 	17-21 "Disk-Info\r\n"
 * 	22-2f Name of the creator
 * 	30    Number of tracks
 * 	31    Number of sides
 * 	32-33 Not used
 * 	34-FF For each track, one byte representing MSB of track size, 
 * 				Eg, of the track length is 4864 (0x1300 = 9 sectors of 512 bytes + 256 bytes for the track header)
 * 					then the byte would be 13
 * 
 * From $100 onwards is the track data. 
 * For each track:
 *	00-0b "Track Info\r\n"
 *	0c-0f unused
 *	10    Track number
 *	11    Side number
 *	12-13 unused
 *	14    Sector size (1=256, 2=512, 3=1024 ect) 
 *	15    Number of sectors 
 *	16    Gap#3 length
 *	17    Filler byte
 *
 * Next from 18 onwards, is the sector list information.
 * Note that the sectors in the file are not nesserilly consecutive (Indeed i have found they are mostly interleaved)  
 * This list is the same order as the data in the file
 * There are 8 bytes per record
 *  00    Track (Equivalent to "C" parameter in the 765 FDC)
 *  01    Side   (Equivalent to "H" parameter in the 765 FDC)
 *  02    Sector ID (Equivalent to "R" parameter in the 765 FDC) (These are 1-9 for +3 disks, Others use $40-49 and $C0-C9)
 *  03    Sector size  (Equivalent to "N" parameter in the 765 FDC) should be the same as #14 above
 *  04    FDC status register 1 after reading
 *  05    FDC status register 2 after reading
 *  06-07 Actual data length of the sector in bytes
 *   
 * At the next $100 boundary, the actual data starts.
 * this is just a stream of data for each sector in the same order and with the same size as the data above. 
 * 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import hddEditor.libs.GeneralUtils;

public class AMSDiskFile extends FloppyDisk {
	private static String AMSDISKHEADER_NORMAL = "MV - CPCEMU Disk-File\r\n";
	private static String AMSDISKHEADER_EXTENDED = "EXTENDED CPC DSK File\r\n";
	private static String AMSDISKTRACKHEADER = "Track-Info\r\n";

	private String Creator = "";

	// Copy of the disk information block at the start of the DSK file
	private byte DiskInfoBlock[] = new byte[256];

	// Parsed version of above.
	private DiskInfo ParsedDiskInfo = null;

	public boolean IsValid = false;

	/**
	 * 
	 * @param file
	 * @throws IOException
	 * @throws BadDiskFileException
	 */
	public AMSDiskFile(File file) throws IOException, BadDiskFileException {
		super(file);
		IsValid = false;
		ParseDisk();
	}

	/**
	 * 
	 */
	public AMSDiskFile() {
		inFile = null;
		file = null;
		IsValid = false;
		LastModified = 0;
		SetNumCylinders(0);
	}

	/**
	 * 
	 * @throws IOException
	 * @throws BadDiskFileException
	 */
	private void ParseDisk() throws IOException, BadDiskFileException {
		NumLogicalSectors = 0;
		InputStream in = new FileInputStream(file);
		try {
			// Read the ADF Disk info block into a bit of memory
			// This bit is always 256 bytes long.
			DiskInfoBlock = in.readNBytes(256);
			if (DiskInfoBlock.length != 256) {
				throw new BadDiskFileException("Disk file not big enough");
			}
			// Parse the disk information into a structure.
			ParsedDiskInfo = new DiskInfo(DiskInfoBlock);

			NumCylinders = ParsedDiskInfo.tracks;
			NumHeads = ParsedDiskInfo.sides;
			NumSectors = 0;
			Creator = ParsedDiskInfo.Creator;

			SectorSize = 512;

			int fileptr = 256;

			// Allocate enough space for all the tracks on both sides of the disk.
			// (+3 disks are usually single sided, but you can do funky things
			// with 720K disks so lets not assume Single sided)
			diskTracks = new TrackInfo[GetNumHeads() * GetNumCylinders()];
			int Tracknum = 0;

			// Track sizes can be variable in the case of extended disks.
			// eg, in the case of some copy protection methods.
			// its easier to just load each track into an array.
			for (int tracknum = 0; tracknum < ParsedDiskInfo.tracks; tracknum++) {
				// Load the track
				byte CurrentRawTrack[] = in.readNBytes(ParsedDiskInfo.TrackSizes[tracknum]);
				if (CurrentRawTrack.length != ParsedDiskInfo.TrackSizes[tracknum]) {
					throw new BadDiskFileException("Disk file not big enough");
				}
				// *********************************************************
				// get the track header...
				// *********************************************************
				TrackInfo CurrentTrack = new TrackInfo();
				CurrentTrack.TrackStartPtr = fileptr;
				fileptr = fileptr + CurrentRawTrack.length;
				if (CurrentRawTrack.length == 0) {
					System.out.println("Track: " + tracknum + " contains no data.");
					CurrentTrack.header = "**BAD**";
					CurrentTrack.tracknum = Tracknum / NumHeads;
					CurrentTrack.side = tracknum % NumHeads;
					CurrentTrack.datarate = 0;
					CurrentTrack.recordingmode = 0;
					CurrentTrack.sectorsz = 0;
					CurrentTrack.numsectors = 0;
					CurrentTrack.gap3len = 0;
					CurrentTrack.fillerByte = 0xf5;
					CurrentTrack.Sectors = new Sector[0];
					CurrentTrack.minsectorID = 0;
					CurrentTrack.maxsectorID = 0;

				} else {
					// Track-Info
					for (int i = 0; i < 12; i++) {
						CurrentTrack.header = CurrentTrack.header + (char) CurrentRawTrack[i];
					}
					// track number
					CurrentTrack.tracknum = (int) CurrentRawTrack[16] & 0xff;
					// side number
					CurrentTrack.side = (int) CurrentRawTrack[17] & 0xff;
					// Data rate (optional)
					CurrentTrack.datarate = (int) CurrentRawTrack[18] & 0xff;
					// Recording mode(optional)
					CurrentTrack.recordingmode = (int) CurrentRawTrack[19] & 0xff;
					// sector size
					CurrentTrack.sectorsz = (int) CurrentRawTrack[20] * 256;
					// Number of sectors
					CurrentTrack.numsectors = (int) CurrentRawTrack[21] & 0xff;
					// gap #3 length
					CurrentTrack.gap3len = (int) CurrentRawTrack[22] & 0xff;
					// Filler byte
					CurrentTrack.fillerByte = (int) CurrentRawTrack[23] & 0xff;

					// *********************************************************
					// Sector information list starts here.
					// *********************************************************
					CurrentTrack.Sectors = new Sector[CurrentTrack.numsectors];
					int sectorbase = 24;
					int minsector = 255;
					int maxsector = 0;
					NumSectors = Math.max(NumSectors, CurrentTrack.numsectors);
					for (int i = 0; i < CurrentTrack.numsectors; i++) {
						Sector CurrentSector = new Sector();
						// track
						CurrentSector.track = (int) CurrentRawTrack[sectorbase] & 0xff;
						// side
						CurrentSector.side = (int) CurrentRawTrack[sectorbase + 1] & 0xff;
						// sector id
						CurrentSector.sectorID = (int) CurrentRawTrack[sectorbase + 2] & 0xff;
						if (CurrentSector.sectorID > maxsector) {
							maxsector = CurrentSector.sectorID;
						}
						if (CurrentSector.sectorID < minsector) {
							minsector = CurrentSector.sectorID;
						}
						// sector sz
						CurrentSector.Sectorsz = (int) CurrentRawTrack[sectorbase + 3] & 0xff;
						// fdc status 1
						CurrentSector.FDCsr1 = (int) CurrentRawTrack[sectorbase + 4] & 0xff;
						// fdc status 2
						CurrentSector.FDCsr2 = (int) CurrentRawTrack[sectorbase + 5] & 0xff;
						// actual data length. Note this is only valid on EXTENDED format disks.
						// If not the case, the sector size read from the track block.
						CurrentSector.ActualSize = (int) (CurrentRawTrack[sectorbase + 7] & 0xff) * 256
								+ (int) (CurrentRawTrack[sectorbase + 6] & 0xff);
						if (!ParsedDiskInfo.IsExtended) {
							CurrentSector.ActualSize = CurrentTrack.sectorsz;
						}
						// Add sector
						CurrentTrack.Sectors[i] = CurrentSector;
						sectorbase = sectorbase + 8;
						NumLogicalSectors++;
					}
					CurrentTrack.minsectorID = minsector;
					CurrentTrack.maxsectorID = maxsector;

					// The first sector is is after the track information block on the next $100
					// junction.
					sectorbase = sectorbase + 0x100;
					sectorbase = sectorbase - (sectorbase % 0x100);

					// *********************************************************
					// now the sector data
					// sectorbase should now point to the start of the first sector.
					// *********************************************************
					for (Sector sect : CurrentTrack.Sectors) {
						sect.SectorStart = sectorbase + CurrentTrack.TrackStartPtr;
						byte rawdata[] = new byte[sect.ActualSize];
						for (int i = 0; i < sect.ActualSize; i++) {
							rawdata[i] = CurrentRawTrack[sectorbase++];
						}
						sect.data = rawdata;
					}

					// Now add the completed track to the track list.
					System.out.print(".");
				}
				diskTracks[Tracknum++] = CurrentTrack;
			}
			System.out.println(" " + String.valueOf(Tracknum) + " tracks");

		} finally {
			in.close();
		}
		IsValid = true;
	}

	/**
	 * Can we open this file type....
	 */
	@Override
	public Boolean IsMyFileType(File filename) throws IOException {
		boolean result = false;

		RandomAccessFile inFile = new RandomAccessFile(filename, "rw");
		try {
			byte HeaderData[] = new byte[0x10];
			inFile.seek(0);
			inFile.read(HeaderData);
			String header = new String(HeaderData, StandardCharsets.UTF_8);
			result = header.startsWith("MV - CPCEMU") || header.startsWith("EXTENDED CPC DSK");
		} finally {
			inFile.close();
		}
		return (result);
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 * @throws BadDiskFileException
	 */
	public static void main(String[] args) {
		AMSDiskFile h;
		try {
			// String filename = "/data1/IDEDOS/Workbench2.3_4Gb_8Bits.hdf";
			String filename = "/home/graham/Desktop/disks/RSGAME.DSK";
			if (new AMSDiskFile().IsMyFileType(new File(filename))) {
//				new AMSDiskFile().CreateBlankAMSDisk(filename,true);
				h = new AMSDiskFile(new File(filename));
				System.out.println(h);
				System.out.println("Track 1:");
				byte data[] = h.GetBytesStartingFromSector(9, 512);
				System.out.println(GeneralUtils.HexDump(data, 0, 512, 0));
				// data[2] = 0x49;
				h.SetLogicalBlockFromSector(9, data);
				System.out.println("Track 1:");
				data = h.GetBytesStartingFromSector(9, 512);
				System.out.println(GeneralUtils.HexDump(data, 0, 512, 0));
				h.close();
			} else {
				System.out.println(filename + " is Not an AMS disk file");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (BadDiskFileException e) {
			e.printStackTrace();
		}
	}

	/**
	 * A bit more complex than the simple Files.
	 */
	@Override
	public byte[] GetBytesStartingFromSector(long SectorNum, long sz) throws IOException {
		// Note, this will restrict length for AMS files of 128Mb. This probably
		// shouldnt be an issue.
		long asz = Math.min(sz, 1024 * 1024 * 128);
		byte result[] = new byte[(int) asz];
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
			Sector sect = Track.GetSectorBySectorID((int) FirstSector);
			if (sect == null) {
				sector = new byte[SectorSize];
			} else {
				sector = sect.data;
			}
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
			Sector sect = Track.GetSectorBySectorID((int) FirstSector);
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
		UpdateLastModified();
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
	 * Add the extra creation user.
	 */
	@Override
	public String toString() {
		String result = "\n Creator: " + Creator + "\n" + super.toString();
		return (result);
	}

	/**
	 * Create a blank disk, 40 tracks, 1 head, 9 sectors per track, 512 bytes per
	 * sector.
	 * 
	 * 
	 * @param Filename
	 * @param VolumeName
	 * @throws IOException
	 * @throws BadDiskFileException
	 */
	public void CreateBlankAMSDisk(File file, boolean Extended) throws IOException, BadDiskFileException {
		FileOutputStream NewFile = new FileOutputStream(file);
		try {
			/*
			 * Disk information block.....
			 */
			DiskInfoBlock = new byte[256];
			for (int i = 0; i < DiskInfoBlock.length; i++) {
				DiskInfoBlock[i] = 0;
			}

			// File header
			String header = AMSDISKHEADER_NORMAL;
			if (Extended) {
				header = AMSDISKHEADER_EXTENDED;
			}
			for (int i = 0; i < header.length(); i++) {
				DiskInfoBlock[i] = (byte) header.charAt(i);
			}

			// Name of creator
			String CREATOR = "HDDDiskEditor     ";
			for (int i = 0; i < 15; i++) {
				DiskInfoBlock[0x22 + i] = (byte) CREATOR.charAt(i);
			}

			// tracks and sides.
			DiskInfoBlock[0x30] = (byte) 40;
			DiskInfoBlock[0x31] = (byte) 1;

			// Size of track. (Sector size * sector + track info block (256) )
			int tracksz = (512 * 9) + 0x100;

			if (!Extended) {
				// For normal disks, all track sizes are the same.
				DiskInfoBlock[0x32] = (byte) (tracksz & 0xff);
				DiskInfoBlock[0x33] = (byte) (tracksz / 256);
			} else {
				// extended disks have a track size for each track.
				// just want the highest byte.
				for (int i = 0; i < 40 * 1; i++) {
					DiskInfoBlock[0x34 + i] = (byte) ((tracksz / 0x100) & 0xff);
				}
			}

			// Write the disk header
			NewFile.write(DiskInfoBlock);

			/*
			 * Tracks
			 */
			for (int TrackNum = 0; TrackNum < 40; TrackNum++) {
				// Track header
				byte TrackBlock[] = new byte[tracksz];
				header = AMSDISKTRACKHEADER;
				for (int i = 0; i < header.length(); i++) {
					TrackBlock[i] = (byte) header.charAt(i);
				}

				TrackBlock[0x10] = (byte) (TrackNum & 0xff); // tracknum
				TrackBlock[0x11] = 0x00; // side number
				TrackBlock[0x14] = 2; // sector size (Fiddled as usual)
				TrackBlock[0x15] = 9; // Number of sectors
				TrackBlock[0x16] = 0x4e; // gap3 length
				TrackBlock[0x17] = (byte) (0xe5 & 0xff); // Filler byte

				// Sector list - Sectors from 1-9 inclusive
				int SectorBase = 0x18;
				for (int SectorNum = 1; SectorNum < 10; SectorNum++) {
					TrackBlock[SectorBase + 0] = (byte) (TrackNum & 0xff); // tracknum
					TrackBlock[SectorBase + 1] = 0x00; // side
					TrackBlock[SectorBase + 2] = (byte) (SectorNum & 0xff); // Sectornum
					TrackBlock[SectorBase + 3] = 0x02; // Sectorsize
					TrackBlock[SectorBase + 4] = 0x00; // fdc SR0
					TrackBlock[SectorBase + 5] = 0x00; // fdc SR1
					if (!Extended) {
						TrackBlock[SectorBase + 6] = 0x00; // Not used
						TrackBlock[SectorBase + 7] = 0x00; // Not used
					} else {
						TrackBlock[SectorBase + 6] = (byte) 0x00; // Sector size (LSB first)
						TrackBlock[SectorBase + 7] = (byte) 0x02;
					}
					SectorBase = SectorBase + 0x08;
				}

				// Actual sector data
				for (int ptr = 0x100; ptr < TrackBlock.length; ptr++) {
					TrackBlock[ptr] = (byte) (0xe5 & 0xff);
				}

				// Write the track.
				NewFile.write(TrackBlock);
			}
		} finally {
			// Close, forcing flush
			NewFile.close();
			NewFile = null;
		}

		/*
		 * Load the newly created file.
		 */
		this.file = file;
		inFile = new RandomAccessFile(file, "rw");
		FileSize = file.length();
		IsValid = false;
		ParseDisk();

	}

}

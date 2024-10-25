package hddEditor.libs.partitions;
/**
 * Implementation of a raw disk data partition.
 * 
 * Basically only used for Copy protected floppies, Wont currently work for hard disks
 * as there is no easily identifiable track listing.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FDD.FloppyDisk;
import hddEditor.libs.disks.FDD.Sector;
import hddEditor.libs.disks.FDD.TrackInfo;

public class RawDiskData extends IDEDosPartition {
	TrackInfo tracks[];

	public RawDiskData(int DirentLocation, Disk RawDisk, byte[] RawPartition, int DirentNum, boolean Initialise) {
		super(DirentLocation, RawDisk, RawPartition, DirentNum, Initialise);
		SetName("Floppy disk Raw data");
		SetPartType(PLUSIDEDOS.PARTITION_RAWFDD);
		CanExport = true;
		if (RawDisk instanceof FloppyDisk) {
			tracks = ((FloppyDisk) RawDisk).diskTracks;
		} else {
			tracks = null;
		}

	}

	@Override
	public void ExtractPartitiontoFolderAdvanced(File folder, int BasicAction, int CodeAction, int ArrayAction,
			int ScreenAction, int MiscAction, int SwapAction, ProgressCallback progress, boolean IncludeDeleted)
					throws IOException {
		try {
			FileWriter SysConfig = new FileWriter(new File(folder, "partition.index"));
			try {
				SysConfig.write("<disk>\n".toCharArray());
				int entrynum = 0;
				for (TrackInfo track : tracks) {
					if (progress != null) {
						if (progress.Callback(tracks.length, entrynum++, "Track: " + track.tracknum)) {
							break;
						}
					}
					
					SysConfig.write("<track>\n".toCharArray());
					String inf = " <num>"+track.tracknum+"</num>"+System.lineSeparator();
					inf = inf + " <side>"+track.side+"</side>"+System.lineSeparator();
					inf = inf + " <gap3>"+track.gap3len+"</gap3>"+System.lineSeparator();
					inf = inf + " <fillerByte>"+track.fillerByte+"</fillerByte>"+System.lineSeparator();
					inf = inf + " <minsector>"+track.minsectorID+"</minsector>"+System.lineSeparator();
					inf = inf + " <maxsector>"+track.maxsectorID+"</maxsector>"+System.lineSeparator();
					SysConfig.write(inf.toCharArray());
					
					for (Sector sector:track.Sectors) {
						File TargetFilename = new File(folder, String.format("T%03x_S%02x.raw", track.tracknum,sector.sectorID));
						GeneralUtils.WriteBlockToDisk(sector.data, TargetFilename);
						File hexFilename = new File(folder, String.format("T%03x_S%02x.hex", track.tracknum,sector.sectorID));
						String wr = "Track:"+track+System.lineSeparator();
						wr = wr + "Sector:"+sector+System.lineSeparator();
						wr = wr + System.lineSeparator()+"Data: "+System.lineSeparator();
						
						int index = 0;
						for (byte d:sector.data) {
							wr = wr + String.format("%02x ",d);
							index++;
							if (index==16) {
								index = 0;
								wr = wr + System.lineSeparator();
							}
						}
						wr = wr + System.lineSeparator();
						GeneralUtils.WriteBlockToDisk(wr.getBytes(), hexFilename);

						SysConfig.write(" <sector>\n".toCharArray());
						inf = "  <track>"+sector.track+"</track>"+System.lineSeparator();
						inf = inf + "  <id>"+sector.sectorID+"</id>"+System.lineSeparator();
						inf = inf + "  <side>"+sector.side+"</side>"+System.lineSeparator();
						inf = inf + "  <sectorsz>"+sector.Sectorsz+"</sectorsz>"+System.lineSeparator();
						inf = inf + "  <FDCsr1>"+sector.FDCsr1+"</FDCsr1>"+System.lineSeparator();
						inf = inf + "  <FDCsr2>"+sector.FDCsr2+"</FDCsr2>"+System.lineSeparator();
						inf = inf + "  <actualsz>"+sector.ActualSize+"</actualsz>"+System.lineSeparator();
						SysConfig.write(inf.toCharArray());
						SysConfig.write(" </sector>\n".toCharArray());
					}
					SysConfig.write("</track>\n".toCharArray());
				}

				SysConfig.write("</disk>\n".toCharArray());
			} finally {
				SysConfig.close();
			}
		} catch (IOException e) {
			System.out.println("Error extracting files: " + e.getMessage());
			e.printStackTrace();
		}

	}
}

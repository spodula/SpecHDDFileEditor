package hddEditor.libs.handlers;

import java.io.FileNotFoundException;
import java.io.IOException;

import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FDD.BadDiskFileException;
import hddEditor.libs.disks.FDD.FloppyDisk;
import hddEditor.libs.disks.FDD.TrDosDiskFile;
import hddEditor.libs.partitions.FloppyBootTrack;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.NonCPMDiskImagePartition;
import hddEditor.libs.partitions.PLUS3DOSPartition;
import hddEditor.libs.partitions.SystemPartition;
import hddEditor.libs.partitions.TrDosPartition;
import hddEditor.libs.partitions.cpm.DirectoryEntry;

public class NonPartitionedDiskHandler extends OSHandler {

	public NonPartitionedDiskHandler(Disk disk) throws IOException {
		super(disk);
		CreateDummyPartitions();
	}

	private void CreateDummyPartitions() throws IOException {
		MaxPartitions = 3;
		/*
		 * We need to create three dummy partitions: o System partition: o Boot sector o
		 * +3DOS partition
		 */

		// System partition:
		byte rawData[] = PLUSIDEDOS.GetSystemPartition(CurrentDisk.GetNumCylinders(), CurrentDisk.GetNumHeads(),
				CurrentDisk.GetNumSectors(), CurrentDisk.GetSectorSize(), false);
		SystemPart = new SystemPartition(0, CurrentDisk, rawData, 0, false);
		SystemPart.SetName("Floppy disk");
		SystemPart.DummySystemPartiton = true;
		SystemPart.SetEndSector(
				(long) (CurrentDisk.GetNumCylinders() * CurrentDisk.GetNumHeads() * CurrentDisk.GetNumSectors()));

		// Boot sector:

		rawData = PLUSIDEDOS.MakeGenericIDEDOSPartition("Disk Boot track", PLUSIDEDOS.PARTITION_BOOT, 0, 0, 0, 0, 9);
		FloppyBootTrack BootPart = new FloppyBootTrack(0, CurrentDisk, rawData, 0, false);
		BootPart.SetName("Boot Track");
		BootPart.SetEndSector((long) (BootPart.reservedTracks * CurrentDisk.GetNumSectors()));
		int ActualReservedTracks = BootPart.reservedTracks;
		BootPart.reservedTracks = 0;

		// +3DOS partition
		rawData = PLUSIDEDOS.MakeGenericIDEDOSPartition("Disk Data area", PLUSIDEDOS.PARTITION_PLUS3DOS,
				ActualReservedTracks, 0, 40, 0, 9);

		try {
			// Setup the Generic XDPB for a disk.
			int NumDirents = (BootPart.reservedblocks * BootPart.blockSize) / 32;

			int RecordsPerTrack = (CurrentDisk.GetNumSectors() * CurrentDisk.GetSectorSize()) / 128;

			FloppyDisk fdd = (FloppyDisk) CurrentDisk;

			if (BootPart.IsValidCPMFileStructure) {
				rawData[0x20] = (byte) (RecordsPerTrack & 0xff); // SPT
				rawData[0x21] = (byte) ((RecordsPerTrack / 0x100) & 0xff);
				rawData[0x22] = (byte) (BootPart.blockPow & 0xff);
				rawData[0x23] = (byte) (((BootPart.blockSize / 128) - 1) & 0xff); // BLM
				rawData[0x24] = 0; // EXM
				rawData[0x25] = (byte) ((BootPart.maxblocks - 1) & 0xff); // DSM
				rawData[0x26] = (byte) (((BootPart.maxblocks - 1) / 0x100) & 0xff);
				rawData[0x27] = (byte) ((NumDirents - 1) & 0xff); // DRM
				rawData[0x28] = (byte) (((NumDirents - 1) / 0x100) & 0xff);

				int DirAllocBmp = 0;
				for (int a = 0; a < BootPart.reservedblocks; a++) {
					DirAllocBmp = DirAllocBmp << 1;
					DirAllocBmp = DirAllocBmp + 1;
				}
				while ((DirAllocBmp & 0x8000) == 0) {
					DirAllocBmp = DirAllocBmp << 1;
				}

				int cksumsize = NumDirents / 4;

				rawData[0x29] = (byte) ((DirAllocBmp / 0x100) & 0xff); // Al0
				rawData[0x2a] = (byte) (DirAllocBmp & 0xff); // Al1
				rawData[0x2b] = (byte) (cksumsize & 0xff); // CKS
				rawData[0x2c] = (byte) ((cksumsize / 0x100) & 0xff);
				rawData[0x2d] = 0;// (byte) (BootPart.reservedTracks & 0xff); // OFF
				rawData[0x2e] = 0;// (byte) ((BootPart.reservedTracks / 0x100) & 0xff);
				rawData[0x2f] = (byte) (BootPart.sectorPow & 0xff); // PSH
				rawData[0x30] = (byte) ((BootPart.sectorSize / 128) - 1); // PHM
				rawData[0x31] = 0; // SS - Assume a Single sided, single density disk.

				rawData[0x32] = (byte) (CurrentDisk.GetNumCylinders() & 0xff); // TPS
				rawData[0x33] = (byte) (CurrentDisk.GetNumSectors() & 0xff); // SPT

				rawData[0x34] = (byte) fdd.diskTracks[0].minsectorID; // First sector
				rawData[0x35] = (byte) (CurrentDisk.GetSectorSize() & 0xff); // Sector size
				rawData[0x36] = (byte) ((CurrentDisk.GetSectorSize() / 0x100) & 0xff);
				rawData[0x37] = (byte) (BootPart.rwGapLength & 0xff); // RW gap length;
				rawData[0x38] = (byte) (BootPart.fmtGapLength & 0xff); // Format gap length
				rawData[0x39] = 0x60; // MFM mode | Skip Deleted address mark.
			} else {
				// Sensible defaults for everything we cant get from the Disk itself.
				rawData[0x20] = (byte) (RecordsPerTrack & 0xff); // SPT
				rawData[0x21] = (byte) ((RecordsPerTrack / 0x100) & 0xff);
				rawData[0x22] = 3; // BSH
				rawData[0x23] = 7; // BLM
				rawData[0x24] = 0; // EXM
				rawData[0x25] = (byte) (174 & 0xff); // DSM
				rawData[0x26] = 0;
				rawData[0x27] = (byte) (63 & 0xff); // DRM
				rawData[0x28] = 0;

				rawData[0x29] = (byte) (192 & 0xff); // Al0
				rawData[0x2a] = 0; // Al0
				rawData[0x2b] = 16; // CKS
				rawData[0x2c] = 0;
				rawData[0x2d] = 1; // OFF
				rawData[0x2e] = 0;
				rawData[0x2f] = 2; // PSH
				rawData[0x30] = 3; // PHM
				rawData[0x31] = 0; // SS
				rawData[0x32] = (byte) (CurrentDisk.GetNumCylinders() & 0xff); // TPS
				rawData[0x33] = (byte) (CurrentDisk.GetNumSectors() & 0xff); // SPT
				rawData[0x34] = (byte) fdd.diskTracks[0].minsectorID; // First sector
				rawData[0x35] = (byte) (CurrentDisk.GetSectorSize() & 0xff); // Sector size
				rawData[0x36] = (byte) ((CurrentDisk.GetSectorSize() / 0x100) & 0xff);
				rawData[0x37] = 42; // RW gap length;
				rawData[0x38] = 82; // Format gap length
				rawData[0x39] = 0x60; // MFM mode | Skip Deleted address mark.
			}

			String fn = CurrentDisk.GetFilename().toUpperCase();
			if (fn.endsWith(".DSK")) {
				PLUS3DOSPartition p3dp = new PLUS3DOSPartition(1, CurrentDisk, rawData, 1, false);
				p3dp.SetEndSector((long) ((CurrentDisk.GetNumCylinders() - ActualReservedTracks)
						* CurrentDisk.GetNumHeads() * CurrentDisk.GetNumSectors()));
				p3dp.ReservedTracks = 0;
				SystemPart.partitions = new IDEDosPartition[3];
				SystemPart.partitions[0] = SystemPart;
				SystemPart.partitions[1] = BootPart;
				SystemPart.partitions[2] = p3dp;
			} else if (fn.endsWith(".TRD")) {
				TrDosPartition tdp = new TrDosPartition(1, CurrentDisk, rawData, 1, false);
				tdp.SetPartType(PLUSIDEDOS.PARTITION_DISK_TRDOS);
				tdp.SetName("TR-DOS disk");
				tdp.SetStartCyl(0);
				tdp.SetStartHead(0);
				SystemPart.partitions = new IDEDosPartition[2];
				SystemPart.partitions[0] = SystemPart;
				SystemPart.partitions[1] = tdp;
			} else if (fn.endsWith(".SCL")) {
				TrDosPartition tdp = new TrDosPartition(1, CurrentDisk, rawData, 1, false);
				tdp.SetPartType(PLUSIDEDOS.PARTITION_DISK_TRDOS);
				tdp.SetName("TR-DOS disk");
				tdp.SetStartCyl(0);
				tdp.SetStartHead(0);
				SystemPart.partitions = new IDEDosPartition[2];
				SystemPart.partitions[0] = SystemPart;
				SystemPart.partitions[1] = tdp;
			} else {
				NonCPMDiskImagePartition np3dp = new NonCPMDiskImagePartition(1, CurrentDisk, rawData, 1, false);
				np3dp.SetPartType(PLUSIDEDOS.PARTITION_UNKNOWN);
				np3dp.SetName("Disk data");
				SystemPart.partitions = new IDEDosPartition[2];
				SystemPart.partitions[0] = SystemPart;
				SystemPart.partitions[1] = np3dp;
			}
		} catch (Exception E) {
			NonCPMDiskImagePartition ncdip = new NonCPMDiskImagePartition(1, CurrentDisk, rawData, 1, false);
			ncdip.SetPartType(PLUSIDEDOS.PARTITION_UNKNOWN);
			ncdip.SetName("Disk data");
			SystemPart.partitions = new IDEDosPartition[3];
			SystemPart.partitions[0] = SystemPart;
			SystemPart.partitions[1] = BootPart;
			SystemPart.partitions[2] = ncdip;
		}

	}

	/**
	 * ToString overridden to show the partitions in the IDEDOS disk
	 */
	@Override
	public String toString() {
		String result = "----------------------------------------------\n";
		for (IDEDosPartition p : SystemPart.partitions) {
			if (p.GetPartType() != 0) {
				result = result + p.toString() + "\n----------------------------------------------\n";
			}
		}
		return (result);
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		NonPartitionedDiskHandler h;
		try {
			Disk disk = new TrDosDiskFile("/home/graham/tmp/ufo.trd");
			h = new NonPartitionedDiskHandler(disk);
			PLUS3DOSPartition p3d = (PLUS3DOSPartition) h.SystemPart.partitions[2];
			System.out.println("---------------------");
			System.out.println(p3d);
			System.out.println("---------------------");
			String result = "";
			for (DirectoryEntry de : p3d.DirectoryEntries) {
				if (!de.IsDeleted) {
					String fn = de.filename();
					while (fn.length() < 15) {
						fn = fn + " ";
					}
					result = result + fn;
					result = result + "\tCPMLen:" + de.GetFileSize() + "\tDeleted?:" + de.IsDeleted + "\n";
				}
			}
			System.out.println(result);

			disk.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (BadDiskFileException e) {
			e.printStackTrace();
		}
	}

}

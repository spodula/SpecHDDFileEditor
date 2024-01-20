package hddEditor.libs.handlers;

import java.io.IOException;

import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.SinclairMicrodrivePartition;
import hddEditor.libs.partitions.SystemPartition;
import hddEditor.libs.partitions.TAPPartition;

public class LinearTapeHandler extends OSHandler {
	public LinearTapeHandler(Disk disk) throws IOException {
		super(disk);
		CreateDummyPartitions();
	}

	private void CreateDummyPartitions() throws IOException {
		MaxPartitions = 3;

		// System partition:
		byte rawData[] = PLUSIDEDOS.GetSystemPartition(CurrentDisk.GetNumCylinders(), CurrentDisk.GetNumHeads(),
				CurrentDisk.GetNumSectors(), CurrentDisk.GetSectorSize(), false);
		SystemPart = new SystemPartition(0, CurrentDisk, rawData, 0, false);
		SystemPart.SetName("Tape");
		SystemPart.DummySystemPartiton = true;
		SystemPart.SetEndSector(
				(long) (CurrentDisk.GetNumCylinders() * CurrentDisk.GetNumHeads() * CurrentDisk.GetNumSectors()));

		String cn = CurrentDisk.getClass().getName();
		
		if (cn.endsWith("MDFMicrodriveFile")) {
			rawData = PLUSIDEDOS.GetSystemPartition(CurrentDisk.GetNumCylinders(), CurrentDisk.GetNumHeads(),
					CurrentDisk.GetNumSectors(),CurrentDisk.GetSectorSize() , false);
			SinclairMicrodrivePartition smp = new SinclairMicrodrivePartition(1, CurrentDisk, rawData, 1, false);
			smp.SetPartType(PLUSIDEDOS.PARTITION_TAPE_SINCLAIRMICRODRIVE);
			smp.SetName("Microdrive Cartrige");
			smp.SetStartCyl(0);
			smp.SetStartHead(0);
			smp.SetEndCyl(0);
			SystemPart.partitions = new IDEDosPartition[2];
			SystemPart.partitions[0] = SystemPart;
			SystemPart.partitions[1] = smp;
		} else if (cn.endsWith("TAPFile")) {
			rawData = PLUSIDEDOS.GetSystemPartition(CurrentDisk.GetNumCylinders(), CurrentDisk.GetNumHeads(),
					CurrentDisk.GetNumSectors(),CurrentDisk.GetSectorSize() , false);
	
			TAPPartition tap = new TAPPartition(1, CurrentDisk, rawData, 1, false);
			tap.SetPartType(PLUSIDEDOS.PARTITION_TAPE_TAP);
			tap.SetName("TAP file");
			tap.SetStartCyl(0);
			tap.SetStartHead(0);
			tap.SetEndCyl(0);
			SystemPart.partitions = new IDEDosPartition[2];
			SystemPart.partitions[0] = SystemPart;
			SystemPart.partitions[1] = tap;
		}
	}
}

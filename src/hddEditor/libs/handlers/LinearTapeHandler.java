package hddEditor.libs.handlers;

import java.io.IOException;

import hddEditor.libs.Languages;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.SinclairMicrodrivePartition;
import hddEditor.libs.partitions.SystemPartition;
import hddEditor.libs.partitions.TAPPartition;
import hddEditor.libs.partitions.TZXPartition;

public class LinearTapeHandler extends OSHandler {
	public LinearTapeHandler(Disk disk,Languages lang) throws IOException {
		super(disk, lang);
		CreateDummyPartitions();
	}

	private void CreateDummyPartitions() throws IOException {
		MaxPartitions = 3;

		// System partition:
		byte rawData[] = PLUSIDEDOS.GetSystemPartition(CurrentDisk.GetNumCylinders(), CurrentDisk.GetNumHeads(),
				CurrentDisk.GetNumSectors(), CurrentDisk.GetSectorSize(), false);
		SystemPart = new SystemPartition(0, CurrentDisk, rawData, 0, false, lang);
		SystemPart.SetName("Tape");
		SystemPart.DummySystemPartiton = true;
		SystemPart.SetEndSector(
				(long) (CurrentDisk.GetNumCylinders() * CurrentDisk.GetNumHeads() * CurrentDisk.GetNumSectors()));

		String cn = CurrentDisk.getClass().getName();

		if (cn.endsWith("MDFMicrodriveFile")) {
			rawData = PLUSIDEDOS.GetSystemPartition(CurrentDisk.GetNumCylinders(), CurrentDisk.GetNumHeads(),
					CurrentDisk.GetNumSectors(), CurrentDisk.GetSectorSize(), false);
			SinclairMicrodrivePartition smp = new SinclairMicrodrivePartition(1, CurrentDisk, rawData, 1, false, lang);
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
					CurrentDisk.GetNumSectors(), CurrentDisk.GetSectorSize(), false);

			TAPPartition tap = new TAPPartition(1, CurrentDisk, rawData, 1, false, lang);
			tap.SetPartType(PLUSIDEDOS.PARTITION_TAPE_TAP);
			tap.SetName("TAP file");
			tap.SetStartCyl(0);
			tap.SetStartHead(0);
			tap.SetEndCyl(0);
			SystemPart.partitions = new IDEDosPartition[2];
			SystemPart.partitions[0] = SystemPart;
			SystemPart.partitions[1] = tap;
		} else if (cn.endsWith("TZXFile")) {
			rawData = PLUSIDEDOS.GetSystemPartition(CurrentDisk.GetNumCylinders(), CurrentDisk.GetNumHeads(),
					CurrentDisk.GetNumSectors(), CurrentDisk.GetSectorSize(), false);

			TZXPartition tzx = new TZXPartition(1, CurrentDisk, rawData, 1, false, lang);
			tzx.SetPartType(PLUSIDEDOS.PARTITION_TAPE_TZX);
			tzx.SetName("TZX file");
			tzx.SetStartCyl(0);
			tzx.SetStartHead(0);
			tzx.SetEndCyl(0);
			SystemPart.partitions = new IDEDosPartition[2];
			SystemPart.partitions[0] = SystemPart;
			SystemPart.partitions[1] = tzx;
		}
	}
}

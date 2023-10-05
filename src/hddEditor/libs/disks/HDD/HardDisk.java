package hddEditor.libs.disks.HDD;

import java.io.IOException;

import hddEditor.libs.disks.Disk;

public interface HardDisk extends Disk {
	public void ResizeDisk(int NewCyls) throws IOException;
}

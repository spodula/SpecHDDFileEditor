package hddEditor.libs.disks.HDD;
/**
 * Generic type for hard disks.
 */

import java.io.IOException;

import hddEditor.libs.disks.Disk;

public interface HardDisk extends Disk {
	public void ResizeDisk(int NewCyls) throws IOException;
}

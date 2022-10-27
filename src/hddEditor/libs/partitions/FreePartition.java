package hddEditor.libs.partitions;
/**
 * Implementation of the Free partititon (type 0xff)
 * A Type 0xFF partition entry as created by the +3e ROMs is all 
 * blank except for the type byte and the location and size 
 * information. The type specific information is not used.
 * 
 * See https://sinclair.wiki.zxnet.co.uk/wiki/IDEDOS#Partition%20Type%200xFF%20-%20Free%20Disk%20Space
 */

import hddEditor.libs.disks.Disk;

public class FreePartition extends IDEDosPartition {

	public FreePartition(int tag, Disk ideDosHandler, byte[] RawPartition,int DirentNum) {
		super(tag, ideDosHandler, RawPartition,DirentNum);
	}

}

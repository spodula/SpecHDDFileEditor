package hddEditor.libs.partitions;
import hddEditor.libs.disks.Disk;
/**
 * Handler for Non CPM floppy disk images embedded onto the HD.  (Type 0x3x)
 * @author graham
 * 
 * The following non-CP/M format disk image types are defined:
 * 0x30: TR-DOS image.
 * 0x31: +D/SAMDOS image.
 * 0x32: MB-02 image.
 * 0x33: TOS A.2 image.
 *
 * 0x0000	PN PN PN PN PN PN PN PN PN PN PN PN PN PN PN PN	
 * 0x0010	PT SC SC SH EC EC EH LS LS LS LS SS V1 V2 V3 V4	
 * 0x0020	TD TD TD TD TD TD TD TD TD TD TD TD TD TD TD TD 
 * 0x0030	TD TD TD TD TD TD TD TD TD TD TD TD TD TD TD TD 
 * 
 * Key:
 * SS: Sector Shift.
 * V1: Virtual Sector Size (0-128B, 1-256B, 2-512B, 3-1024B).
 * V2: Virtual first sector number (0 to 240).
 * V3: Virtual heads.
 * V4: Virtual sectors.
 * TD: Type specific data if any.
 *
 */


public class NonCPMDiskImagePartition extends IDEDosPartition {
	int SectorShift=0;
	int VirtualSectorShift=0;
	int VirtualSectorSize=0;
	int VirtualFirstSectorNumber=0;
	int VirtualHeads=0;
	int VirtualSectors=0;

	/**
	 * Constructor
	 *
	 * @param tag
	 * @param disk
	 * @param RawPartition
	 */
	public NonCPMDiskImagePartition(int tag, Disk disk, byte[] RawPartition,int DirentNum, boolean Initialise) {
		super(tag, disk, RawPartition,DirentNum, Initialise);
	}
	
	/**
	 * Disk specific information for this class of disks.
	 */
	@Override
	protected void LoadPartitionSpecificInformation() {
	    byte Unused[] = getUnused();
		SectorShift = (Unused[0] & 0xff);
		VirtualSectorShift = (Unused[1] & 0xff);
		VirtualFirstSectorNumber = (Unused[2] & 0xff);
		VirtualHeads = (Unused[3] & 0xff);
		VirtualSectors = (Unused[4] & 0xff);
		VirtualSectorSize = 128 << VirtualSectorShift;
	}

	/**
	 * return details as a string.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + "\n  Sector shift: "+SectorShift;
		result = result + "\tVirtual sector shift: "+VirtualSectorShift;
		result = result + "\tVirtual sector size: "+VirtualSectorSize;
		result = result + "\tVirtual First Sector: "+VirtualFirstSectorNumber;
		result = result + "\n  Virtual Heads: "+VirtualHeads;
		result = result + "\tVirtual sectors: "+VirtualSectors;
		
		return(result);
	}
	
	/**
	 * Reload the partition details.
	 */
	@Override
	public void Reload() {
	}

}

package hddEditor.libs.partitions;
/**
 * Implementation of Swap partition
 * 
 * IDEDOS Partition entry - 64 bytes
 * 0x0000	PN PN PN PN PN PN PN PN PN PN PN PN PN PN PN PN	
 * 0x0010	02 SC SC SH EC EC EH LS LS LS LS BS CB CB MB MB
 * 0x0020	00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00	
 * 0x0030	00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00	
 * 
 * Key:
 * BS: Block size in sectors (1 to 32)
 * CB: Current block number (0 to 65535)
 * MB: Maximum block number (0 to 65535)
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.disks.Disk;

public class SwapPartition extends IDEDosPartition {
	// Block size in sectors (1 to 32)
	public int SwapblockSize=0;
	// Current block number (0 to 65535)
	public int CurrentBlock=0;
	// Maximum block number (0 to 65535)
	public int MaxBlock=0;

	/**
	 * Constructor
	 * 
	 * @param tag
	 * @param disk
	 * @param RawPartition
	 */
	public SwapPartition(int tag, Disk disk, byte[] RawPartition,int DirentNum, boolean Initialise) {
		super(tag, disk, RawPartition, DirentNum, Initialise);
	}

	/**
	 * populate specific data
	 */
	@Override
	protected void LoadPartitionSpecificInformation() {
	    byte Unused[] = getUnused();

		SwapblockSize = (Unused[0] & 0xff);
		CurrentBlock = ((Unused[2] & 0xff) * 0x100) + (Unused[1] & 0xff);
		MaxBlock= ((Unused[4] & 0xff) * 0x100) + (Unused[3] & 0xff);
	}

	/**
	 * ToString overridden for swap specific information
	 * 
	 * @return
	 */
	public String toString() {
		String result = super.toString();
		result = result + "\n  Swap block size: "+SwapblockSize+" sectors,";
		result = result + "\tCurrent Block: "+CurrentBlock;
		result = result + "\tMax Block: "+MaxBlock;
		return(result);
	}
	
	/**
	 * Reload the partition details.
	 */
	@Override
	public void Reload() {
	}

	
	/**
	 * 
	 */
	@Override
	public void ExtractPartitiontoFolder(File folder, boolean raw, boolean CodeAsHex, ProgressCallback progress) {
		try {
			FileWriter SysConfig = new FileWriter(new File(folder, "swap.index"));
			if (progress!= null) {
				progress.Callback(1, 0, "Swap data...");
			}
			try {
				SysConfig.write("<swap>\n".toCharArray());
				SysConfig.write(("   <blocksizesectors>"+SwapblockSize+"</blocksizesectors>\n").toCharArray());
				SysConfig.write(("   <currentblock>"+CurrentBlock+"</currentblock>\n").toCharArray());
				SysConfig.write(("   <maxblock>"+MaxBlock+"</maxblock>\n").toCharArray());
				SysConfig.write("</swap>\n".toCharArray());
			} finally {
				SysConfig.close();
			}
			byte data[] = GetAllDataInPartition();
			if (raw) {
				GeneralUtils.WriteBlockToDisk(data, new File(folder,"swap.data"));
			} else {
				String sdata = GeneralUtils.HexDump(data, 0, data.length);
				GeneralUtils.WriteBlockToDisk(sdata.getBytes(), new File(folder,"swap.data.txt"));
			}
		} catch (IOException e) {
			System.out.println("Error extracting files: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
}

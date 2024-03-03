package hddEditor.libs.disks.LINEAR.tzxblocks;
/**
 * Wrapper for TZX blocks at standard speed. (Almost always written by the Speccy Rom)
 * 
 * There are actually three levels to this:
 * TZX Block
 * =========
 * 00    - always 0x10, the Block ID
 * 01-02 - Pause in MS between this and the next block
 * 			in milliseconds (MSB first). THis is usually 954.
 * 03-04 - Data length that follows
 * 00-xxx - Data
 * 
 * The data is a raw data file as saved by the rom. This has a type and a checksum.
 * 
 * Save block
 * ==============
 * 00     - Rom data type. Usually 0 for header block or $FF for the data block.
 * 01-xxx - The raw data
 * XX+1   - Simple checksum.
 * 				The checksum is the XORd content of the entire datablock 
 * 				(including the rom data type, excluding the checksum itself)
 * 				If incorrect, will give a R. Tape loading error.
 *
 * Data:
 * =======
 * The data as presented to the speccy itself.
 * 
 * 
 */

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class StandardDataBlock extends TZXBlock {
	int blockpauseMS = 0;

	/**
	 * Constructor for when we are initialising from Data. (Usually new blocks)
	 * @param d
	 * @param pausems
	 */
	public StandardDataBlock(byte d[], int pausems) {
		blocktype = TZX.TZX_STANDARDSPEED_DATABLOCK;
		BlockDesc = "Standard speed data block";

		blockdata = d;
		rawdata = new byte[d.length + 5];
		rawdata[0] = TZX.TZX_STANDARDSPEED_DATABLOCK;
		rawdata[1] = (byte) (pausems & 0xff);
		rawdata[2] = (byte) ((pausems / 0xff) & 0xff);
		rawdata[3] = (byte) (d.length & 0xff);
		rawdata[4] = (byte) ((d.length / 0xff) & 0xff);
		System.arraycopy(d, 0, rawdata, 5, d.length);

		UpdateDataFromBlockData();
		System.out.println("rawdata: "+rawdata.length+" Blockdata: "+blockdata.length+" data:"+data.length);
		
	}

	/**
	 * Constructor for when we are loading from disk.
	 * 
	 * @param fs
	 * @throws IOException
	 */
	public StandardDataBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_STANDARDSPEED_DATABLOCK;
		BlockDesc = "Standard speed data block";
		byte blockpause[] = new byte[2];
		fs.read(blockpause);
		blockpauseMS = GetDblByte(blockpause,0);

		byte len[] = new byte[2];
		fs.read(len);
		int blockLength = GetDblByte(len,0);
		
		blockdata = new byte[blockLength];
		fs.read(blockdata);

		rawdata = new byte[blockLength + 5];
		rawdata[0] = TZX.TZX_STANDARDSPEED_DATABLOCK;
		rawdata[1] = blockpause[0];
		rawdata[2] = blockpause[1];
		rawdata[3] = len[0];
		rawdata[4] = len[1];
		System.arraycopy(blockdata, 0, rawdata, 5, blockLength);

		UpdateDataFromBlockData();
	}

	@Override
	public String toString() {
		String result = String.format("%s (%02X)", BlockDesc, blocktype) + " Pause " + blockpauseMS + " - BASIC Length:"
				+ data.length+" raw length:"+blockdata.length+" TZX Block length:"+rawdata.length;
		return (result);
	}
	

	/**
	 * 
	 * @param newdata
	 */
	@Override
	public void UpdateBlockData(byte[] newdata) {
		if (data!=null) {
			//does this block contain raw data
			if (data.length == blockdata.length) { //Yes
				blockdata = newdata;
			} else { //add in Speccy checksum and type
				byte SpecBlockType = blockdata[0];
				blockdata = new byte[newdata.length+2];
				blockdata[0] = SpecBlockType;
				System.arraycopy(newdata, 0, blockdata, 1, newdata.length );
			}
		} else {
			//add raw speccy data
			blockdata = newdata;
		}
		data = newdata;
		byte pausemsH = rawdata[1];
		byte pausemsL = rawdata[2];
		
		//Now update the raw block
		rawdata = new byte[blockdata.length + 5];
		rawdata[0] = TZX.TZX_STANDARDSPEED_DATABLOCK;
		rawdata[1] = pausemsH;
		rawdata[2] = pausemsL;
		rawdata[3] = (byte) (blockdata.length & 0xff);
		rawdata[4] = (byte) ((blockdata.length / 0xff) & 0xff);
		System.arraycopy(blockdata, 0, rawdata, 5, blockdata.length);
	}

}

package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class TurboSpeedDataBlock extends TZXBlock {
	public int Pilot = 0;
	public int Sync1 = 0;
	public int Sync2 = 0;
	public int Zero = 0;
	public int One = 0;
	public int PilotPulses = 0;
	public int UsedBitsInFinal = 0;
	public int Pause = 0;
		
	public TurboSpeedDataBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_TURBOSPEED_DATABLOCK;
		BlockDesc = "Turbo speed data block";
		byte header[] = new byte[0x12];
		fs.read(header);
		Pilot = GetDblByte(header,0);
		Sync1 = GetDblByte(header,2);
		Sync2 = GetDblByte(header,4);
		Zero = GetDblByte(header,6);
		One = GetDblByte(header,8);
		PilotPulses = GetDblByte(header,0x0A);
		UsedBitsInFinal = ((int) header[0x0C] & 0xff);
		Pause = GetDblByte(header,0x0D);

		int blockLength = ((int) header[0x0F] & 0xff) + (((int) header[0x10] & 0xff) * 0x100)
				+ (((int) header[0x11] & 0xff) * 0x10000);

		blockdata = new byte[blockLength];
		fs.read(blockdata);
		
		rawdata = new byte[blockLength + 0x13];
		rawdata[0] = 0x11;
		System.arraycopy(header, 0, rawdata, 1, header.length);
		System.arraycopy(blockdata, 0, rawdata, header.length+1, blockLength);

		UpdateDataFromBlockData();
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
		rawdata[0] = TZX.TZX_TURBOSPEED_DATABLOCK;
		rawdata[1] = pausemsH;
		rawdata[2] = pausemsL;
		rawdata[3] = (byte) (blockdata.length & 0xff);
		rawdata[4] = (byte) ((blockdata.length / 0xff) & 0xff);
		System.arraycopy(blockdata, 0, rawdata, 5, blockdata.length);
	}
	

	@Override
	public String toString() {
		String result = super.toString() + " Length:" + data.length + " Pilot:" + Pilot
				+ " Sync1:" + Sync1 + " Sync2:" + Sync2 + " Zero:" + Zero + " One:" + One + " PilotPulses:"
				+ PilotPulses + " UsedBitsInFinal:" + UsedBitsInFinal + " Pause:" + Pause;

		return (result);
	}
}

package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class PureToneBlock extends TZXBlock {
	public int PulseLen;
	public int Pulses;
	
	public PureToneBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_PURETONE;
		BlockDesc = "Pure tone";
		byte dat[] = new byte[4];
		fs.read(dat);
		PulseLen = ((int)dat[0] & 0xff) + (((int)dat[1] & 0xff) * 0x100);
		Pulses = ((int)dat[2] & 0xff) + (((int)dat[3] & 0xff) * 0x100);
		
		rawdata = new byte[5];
		rawdata[0] = (byte)blocktype;
		System.arraycopy(dat, 0, rawdata, 1, 4);
		blockdata = data;
	}
	
	@Override
	public String toString() {
		String result = String.format("%s (%02X)", BlockDesc, blocktype)+" Pulse duration:"+PulseLen+" T states, Pulses: "+Pulses;
		return(result);
	}
}

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
		PulseLen = GetDblByte(dat, 0);
		Pulses = GetDblByte(dat, 2);
		
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

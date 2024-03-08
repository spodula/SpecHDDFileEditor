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
		data = new byte[4];
		fs.read(data);
		PulseLen = GetDblByte(data, 0);
		Pulses = GetDblByte(data, 2);
		
		rawdata = new byte[5];
		rawdata[0] = (byte)blocktype;
		System.arraycopy(data, 0, rawdata, 1, 4);
		blockdata = data;
	}
	
	@Override
	public String toString() {
		String result = super.toString() + " Pulse duration:"+PulseLen+" T states, Pulses: "+Pulses;
		return(result);
	}
}

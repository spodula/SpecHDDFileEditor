package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class PulseSequence extends TZXBlock {
	public int PulseLen[];
	public int Pulses;
	
	public PulseSequence(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_PULSESEQ;
		BlockDesc = "Pulse Sequence";
		byte dat[] = new byte[1];
		fs.read(dat);
		Pulses = ((int)dat[0] & 0xff);
		rawdata = new byte[2+(Pulses*2)];
		PulseLen = new int[Pulses];
		
		rawdata[0] = (byte)blocktype;
		rawdata[1] = (byte)Pulses;
		
		data = new byte[Pulses*2];
		fs.read(data);
		System.arraycopy(data, 0, rawdata, 2, data.length);
		
		int ptr = 0;
		for (int i=0;i<Pulses;i++) {
			PulseLen[i] = (data[ptr] & 0xff) + ((data[ptr] & 0xff) * 0x100);
			ptr = ptr + 2;
		}
		
		blockdata = data;

	}
	
	@Override
	public String toString() {
		String result = String.format("%s (%02X)", BlockDesc, blocktype)+" Pulses: "+Pulses;
		return(result);
	}
}

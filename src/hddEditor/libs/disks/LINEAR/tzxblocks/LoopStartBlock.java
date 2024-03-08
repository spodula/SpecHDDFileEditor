package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class LoopStartBlock extends TZXBlock {
	public int Repeat;
	
	public LoopStartBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_LOOPSTART;
		data = new byte[2];
		fs.read(data);
		Repeat = GetDblByte(data, 0);
		
		rawdata = new byte[3];
		rawdata[0] = (byte)blocktype;
		System.arraycopy(data, 0, rawdata, 1, 2);
		blockdata = data;

	}
	
	@Override
	public String toString() {
		String result = super.toString() + " Repeat: "+Repeat;
		return(result);
	}
}

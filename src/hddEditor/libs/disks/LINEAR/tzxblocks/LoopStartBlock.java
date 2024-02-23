package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class LoopStartBlock extends TZXBlock {
	public int Repeat;
	
	public LoopStartBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_LOOPSTART;
		BlockDesc = "Loop start";
		byte dat[] = new byte[2];
		fs.read(dat);
		Repeat = ((int)dat[0] & 0xff) + (((int)dat[1] & 0xff) * 0x100);
		
		rawdata = new byte[3];
		rawdata[0] = (byte)blocktype;
		System.arraycopy(dat, 0, rawdata, 1, 2);
		blockdata = data;

	}
	
	@Override
	public String toString() {
		String result = String.format("%s (%02X)", BlockDesc, blocktype)+" Repeat: "+Repeat;
		return(result);
	}
}

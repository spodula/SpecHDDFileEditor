package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class JumpToBlock extends TZXBlock {
	public int Disp;
	
	public JumpToBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_LOOPSTART;
		BlockDesc = "Loop start";
		byte dat[] = new byte[2];
		fs.read(dat);
		Disp = ((int)dat[0] & 0xff) + (((int)dat[1] & 0xff) * 0x100);
		
		if (Disp > 0x8000) {
			Disp = Disp - 0x10000;
		}
		
		rawdata = new byte[3];
		rawdata[0] = (byte)blocktype;
		System.arraycopy(dat, 0, rawdata, 1, 2);
		blockdata = data;

	}
	
	@Override
	public String toString() {
		String result = String.format("%s (%02X)", BlockDesc, blocktype)+" Disp: "+Disp;
		return(result);
	}
}

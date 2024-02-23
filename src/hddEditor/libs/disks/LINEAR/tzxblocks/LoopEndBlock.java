package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class LoopEndBlock extends TZXBlock {
	public LoopEndBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_LOOPEND;
		BlockDesc = "Loop End";
		rawdata = new byte[1];
		rawdata[0] = (byte)blocktype;
		blockdata = data;

	}
	
	@Override
	public String toString() {
		String result = String.format("%s (%02X)", BlockDesc, blocktype);
		return(result);
	}
}

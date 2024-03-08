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
		data = new byte[0];
		blockdata = data;
	}
}

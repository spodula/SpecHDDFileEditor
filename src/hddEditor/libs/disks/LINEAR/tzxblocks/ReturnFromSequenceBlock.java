package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class ReturnFromSequenceBlock extends TZXBlock {
	public ReturnFromSequenceBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_RETSEQ;
		BlockDesc = "Return from sequence";
		rawdata = new byte[1];
		rawdata[0] = (byte)blocktype;
		blockdata = data;

	}
}

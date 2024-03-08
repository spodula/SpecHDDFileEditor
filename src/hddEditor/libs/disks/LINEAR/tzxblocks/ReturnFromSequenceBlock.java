package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class ReturnFromSequenceBlock extends TZXBlock {
	public ReturnFromSequenceBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_RETSEQ;
		rawdata = new byte[1];
		rawdata[0] = (byte)blocktype;
		
		data = new byte[0];
		blockdata = data;

	}
}

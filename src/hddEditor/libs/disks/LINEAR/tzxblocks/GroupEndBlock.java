package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class GroupEndBlock extends TZXBlock {
	public GroupEndBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_GROUPEND;
		rawdata = new byte[1];
		rawdata[0] = (byte)blocktype;
		data = new byte[0];
		blockdata = data;
	}	
}

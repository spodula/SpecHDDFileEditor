package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class FourtyEightkStopBlock extends TZXBlock {
	public FourtyEightkStopBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_STOP48;
		BlockDesc = "Stop if 48K mode";
		data = new byte[4];
		fs.read(data);
		
		rawdata = new byte[5];
		rawdata[0] = (byte)blocktype;
		System.arraycopy(data, 0, rawdata, 1, 4);
		
		blockdata = data;

	}	
}

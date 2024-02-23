package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class FourtyEightkStopBlock extends TZXBlock {
	public FourtyEightkStopBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_STOP48;
		BlockDesc = "Stop if 48K mode";
		byte uselessdata[] = new byte[4];
		fs.read(uselessdata);
		
		rawdata = new byte[5];
		rawdata[0] = (byte)blocktype;
		System.arraycopy(uselessdata, 0, rawdata, 1, 4);
		
		blockdata = data;

	}
	
	@Override
	public String toString() {
		String result = String.format("%s (%02X)", BlockDesc, blocktype);
		return(result);
	}
}

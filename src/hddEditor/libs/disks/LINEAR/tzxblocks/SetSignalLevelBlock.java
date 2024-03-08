package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class SetSignalLevelBlock extends TZXBlock {
	public int SignalLevel = 0;
	
	public SetSignalLevelBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_SETSIGNALLEVEL;
		BlockDesc = "Set Signal level";
		byte data[] = new byte[5];
		fs.read(data);
		
		SignalLevel = data[0x04];
		
		rawdata = new byte[6];
		rawdata[0] = (byte)blocktype;
		System.arraycopy(data, 0, rawdata, 1, 4);
		blockdata = data;
	}
	
	@Override
	public String toString() {
		String result = super.toString() + " Level: "+SignalLevel;
		return(result);
	}
}

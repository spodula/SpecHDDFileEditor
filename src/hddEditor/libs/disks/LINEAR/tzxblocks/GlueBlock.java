package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class GlueBlock extends TZXBlock {
	public String XTAPE = "";
	public int Major;
	public int Minor;
	
	public GlueBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_GLUE;
		data = new byte[9];
		fs.read(data);
		
		XTAPE = new String(data).substring(0,7);
		Major = data[7] & 0xff;
		Minor = data[8] & 0xff;
		
		
		rawdata = new byte[0x0A];
		rawdata[0] = (byte)blocktype;
		System.arraycopy(data, 0, rawdata, 1, 0x09);
		
		blockdata = data;
	}
	
	@Override
	public String toString() {
		String result = super.toString()+" XTAPE:"+XTAPE+" V"+Major+"."+Minor;
		return(result);
	}
}

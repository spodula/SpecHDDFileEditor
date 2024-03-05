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
		BlockDesc = "Glue block";
		byte header[] = new byte[9];
		fs.read(header);
		
		XTAPE = new String(header).substring(0,7);
		Major = header[7] & 0xff;
		Minor = header[8] & 0xff;
		
		
		rawdata = new byte[0x0A];
		rawdata[0] = (byte)blocktype;
		System.arraycopy(data, 0, rawdata, 1, 0x09);
		
		blockdata = data;
	}
	
	@Override
	public String toString() {
		String result = String.format("%s (%02X)", BlockDesc, blocktype)+" XTAPE:"+XTAPE+" V"+Major+"."+Minor;
		return(result);
	}
}

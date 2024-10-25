package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class JumpToBlock extends TZXBlock {
	public int Disp;
	
	public JumpToBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_JUMP;
		data = new byte[2];
		fs.read(data);
		Disp = GetDblByte(data, 0);
		
		if (Disp > 0x8000) {
			Disp = Disp - 0x10000;
		}
		
		rawdata = new byte[3];
		rawdata[0] = (byte)blocktype;
		System.arraycopy(data, 0, rawdata, 1, 2);
		
		blockdata = data;
	}
	
	@Override
	public String toString() {
		String s = String.valueOf(Disp);
		if (Disp>0) {
			s = "+"+s;
		}
		String target = String.valueOf(Disp+BlockNumber);
		
		
		String result = super.toString() + " Disp: "+s+" (Block"+target+")";
		return(result);
	}
}

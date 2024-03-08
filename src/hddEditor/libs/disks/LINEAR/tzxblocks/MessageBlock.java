package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class MessageBlock extends TZXBlock {
	public int time;
	
	public MessageBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_MESSAGEBLOCK;
		BlockDesc = "Message block";
		byte tSecs[] = new byte[1];
		fs.read(tSecs);
		byte ml[] = new byte[1];
		fs.read(ml);
		int msglength = (int) (ml[0] & 0xff);
		data = new byte[msglength];
		fs.read(data);
		BlockNotes = new String(data);

		rawdata = new byte[msglength+3];
		rawdata[0] = (byte)blocktype;
		rawdata[1] = tSecs[0];
		rawdata[2] = ml[0];
		System.arraycopy(data, 0, rawdata, 3, msglength);
		blockdata = data;
		
		time = (tSecs[0] & 0xff);
	}
	
	@Override
	public String toString() {
		String result = super.toString() + " Message:"+BlockNotes;
		return(result);
	}
	
}

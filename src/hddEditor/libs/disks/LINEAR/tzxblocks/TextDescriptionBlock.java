package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class TextDescriptionBlock extends TZXBlock {
	public TextDescriptionBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_TEXTDESC;
		BlockDesc = "Text Description block";
		byte ml[] = new byte[1];
		fs.read(ml);
		int msglength = (int) (ml[0] & 0xff);
		data = new byte[msglength];
		fs.read(data);
		BlockNotes = new String(data);

		rawdata = new byte[msglength+2];
		rawdata[0] = (byte) blocktype;
		rawdata[1] = ml[0];
		System.arraycopy(data, 0, rawdata, 2, msglength);
		blockdata = data;
	}
	
	@Override
	public String toString() {
		String result = super.toString() + " Message:"+BlockNotes;
		return(result);
	}
}

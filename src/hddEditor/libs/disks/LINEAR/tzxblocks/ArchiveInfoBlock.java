package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import hddEditor.libs.TZX;

public class ArchiveInfoBlock extends TZXBlock {
	public TextEntry list[] = null;

	public class TextEntry {
		public int id;
		public int length;
		public String text;

		public TextEntry(byte data[], int offset) {
			id = data[offset];
			length = (int) (data[offset+1] & 0xff);
			byte txt[] = new byte[length];
			System.arraycopy(data, offset+2, txt, 0, length);
			text = new String(txt);
		}
		
		public String IdByteAsString() {
			String result = "<undefined>";
			switch (id) {
			case 0:	result = "Full title"; break;
			case 1:	result = "Software house/publisher"; break;
			case 2:	result = "Author(s)"; break;
			case 3:	result = "Year of publication"; break;
			case 4:	result = "Language"; break;
			case 5:	result = "Game/Utility type"; break;
			case 6:	result = "Price"; break;
			case 7:	result = "Protection scheme/Loader"; break;
			case 8:	result = "Comments"; break;
			case 255:	result = ""; break;
			}
			return(result);
		}
		
	}
	
	public ArchiveInfoBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_ARCHIVEINFO;
		BlockDesc = "Archive Info block";
		byte bl[] = new byte[3];
		fs.read(bl);
		
		int msglength = GetDblByte(bl, 0);
		data = new byte[msglength-1];
		fs.read(data);

		rawdata = new byte[msglength+3];
		rawdata[0] = (byte)blocktype;
		rawdata[1] = bl[0];
		rawdata[2] = bl[1];
		rawdata[3] = bl[2];
		System.arraycopy(data, 0, rawdata, 4, data.length);
		
		
		ArrayList<TextEntry> entries = new ArrayList<TextEntry>();
		
		int offset=0;
		while (offset < data.length) {
			TextEntry te = new TextEntry(data, offset);
			offset = offset + te.length+2;
			entries.add(te);
		}
		
		blockdata = data;
		
		list = entries.toArray(new TextEntry[0]);
	}
	
	@Override
	public String toString() {
		String result = String.format("%s (%02X)", BlockDesc, blocktype)+"  "+list.length+" entries\n";
		for(TextEntry te:list) {
			result = result + "  "+te.IdByteAsString()+": "+te.text+"\n";
		}
		return(result);
	}
}

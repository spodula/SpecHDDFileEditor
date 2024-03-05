package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class PauseStopTape extends TZXBlock {
	public int PauseDuration = 0;
	
	public PauseStopTape(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_PAUSE;
		byte duration[] = new byte[2];
		fs.read(duration);
		PauseDuration = (duration[0] & 0xff) + ((duration[1] & 0xff) * 0x100);
		rawdata = new byte[3];
		rawdata[0] = (byte)blocktype;
		rawdata[1] = duration[0];
		rawdata[2] = duration[1];
		
		BlockDesc = "Stop the tape";
		
		if (PauseDuration > 0) {
			BlockDesc = "Pause ("+PauseDuration+")";
		}	
		blockdata = data;

	}
	
	@Override
	public String toString() {
		String result = String.format("%s (%02X)", BlockDesc, blocktype);
		return(result);
	}
}

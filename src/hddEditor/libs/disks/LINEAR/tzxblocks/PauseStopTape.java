package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class PauseStopTape extends TZXBlock {
	public int PauseDuration = 0;
	
	public PauseStopTape(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_PAUSE;
		data = new byte[2];
		fs.read(data);
		PauseDuration = GetDblByte(data, 0);
		rawdata = new byte[3];
		rawdata[0] = (byte)blocktype;
		rawdata[1] = data[0];
		rawdata[2] = data[1];
		
		BlockDesc = "Stop the tape";
		
		if (PauseDuration > 0) {
			BlockDesc = "Pause ("+PauseDuration+")";
		}	
		blockdata = data;

	}
}

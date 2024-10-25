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
		
		blockdata = data;
	}
	@Override
	public String toString() {
		String result = "";
		if (PauseDuration==0) {
			result = result + String.format("Stop tape (%02X)", blocktype);
		} else {
			result = result + String.format("Pause (%02X) ", blocktype) + PauseDuration+"ms";			
		}
		return(result);
	}
	
}

package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class DirectRecordingBlock extends TZXBlock {
	int SampleTStates = 0;
	int BlockPause = 0;
	int LastUsedBits = 0;

	public DirectRecordingBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_DIRECTRECORDING;

		byte Header[] = new byte[0x08];
		fs.read(Header);
		SampleTStates = ((int) Header[0] & 0xff) + (((int) Header[1] & 0xff) * 0x100);
		BlockPause = ((int) Header[2] & 0xff) + (((int) Header[3] & 0xff) * 0x100);
		LastUsedBits = (Header[4] & 0xff);
		
		int DataLength = ((int) Header[5] & 0xff) + (((int) Header[6] & 0xff) * 0x100) + (((int) Header[7] & 0xff) * 0x10000);

		data = new byte[DataLength];
		fs.read(data);

		rawdata = new byte[DataLength + Header.length+1];
		rawdata[0] = (byte) blocktype;
		
		blockdata = data;

		
		System.arraycopy(Header, 0, rawdata, 1, Header.length);
		System.arraycopy(data, 0, rawdata, Header.length+1, data.length);
	}

	@Override
	public String toString() {
		String result = super.toString() + "SampleTStates: "+SampleTStates+" Tstates,  Pause " + BlockPause + "ms Length:"
				+ data.length;
		return (result);
	}
}

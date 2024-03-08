package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class PureDataBlock extends TZXBlock {
	int ZeroLenTStates = 0;
	int OneLenTStates = 0;
	int LastUsedBits = 0;
	int BlockPause = 0;

	public PureDataBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_PUREDATA;
		BlockDesc = "Pure data block";

		byte Header[] = new byte[0x0A];
		fs.read(Header);
		ZeroLenTStates = ((int) Header[0] & 0xff) + (((int) Header[1] & 0xff) * 0x100);
		OneLenTStates = ((int) Header[2] & 0xff) + (((int) Header[3] & 0xff) * 0x100);
		LastUsedBits = (Header[4] & 0xff);
		BlockPause = ((int) Header[5] & 0xff) + (((int) Header[6] & 0xff) * 0x100);
		int DataLength = ((int) Header[7] & 0xff) + (((int) Header[8] & 0xff) * 0x100) + (((int) Header[9] & 0xff) * 0x10000);

		data = new byte[DataLength];
		fs.read(data);

		rawdata = new byte[DataLength + 11];
		rawdata[0] = 0x14;
		
		System.arraycopy(Header, 0, rawdata, 1, Header.length);
		System.arraycopy(data, 0, rawdata, Header.length+1, data.length);
		blockdata = data;

	}

	@Override
	public String toString() {
		String result = super.toString() + "Zero: "+ZeroLenTStates+" Tstates, One:"+OneLenTStates+" TStates, Pause " + BlockPause + "ms Length:"
				+ data.length;
		return (result);
	}

}

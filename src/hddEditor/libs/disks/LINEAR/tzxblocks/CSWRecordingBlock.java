package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class CSWRecordingBlock extends TZXBlock {
	int BlockPause;
	int SampleRate;
	int CompressionType;
	int StoredPulses;

	public CSWRecordingBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_CSWRECORDING;
		BlockDesc = "CSW Recording";
		byte blocklen[] = new byte[4];
		fs.read(blocklen);
		int BlockLen = GetDWORD(blocklen, 0);

		byte Header[] = new byte[0x0A];
		fs.read(Header);
		BlockPause = GetDblByte(Header, 0);
		SampleRate = (GetDblByte(Header, 2) * 0x100) + (Header[8] & 0xff);
		CompressionType = (Header[9] & 0xff);
		StoredPulses = GetDWORD(Header, 0x0A);

		data = new byte[BlockLen - 10];
		fs.read(data);

		blockdata = new byte[BlockLen + 4];
		System.arraycopy(blocklen, 0, blockdata, 0, 0x04);
		System.arraycopy(Header, 0, blockdata, 0x04, 0x0A);
		System.arraycopy(data, 0, blockdata, 0x0E, data.length);

		rawdata = new byte[blockdata.length + 1];
		rawdata[0] = (byte) blocktype;
		System.arraycopy(blockdata, 0, blockdata, 1, blockdata.length);
	}

	@Override
	public String toString() {
		String cType = "Unknown";
		if (CompressionType == 0) {
			cType = "RLE";
		} else if (CompressionType == 1) {
			cType = "Z-RLE";
		}

		String result = "Pause:" + BlockPause + " SampleRate:" + SampleRate + " CompressionType:" + cType + "("
				+ CompressionType + ") Pulses:" + StoredPulses;
		return (result);
	}

}

package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class CallSequenceBlock extends TZXBlock {
	public int Blocks[];
	
	public CallSequenceBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_CALLSEQ;
		BlockDesc = "Call sequence block";
		
		byte bl[] = new byte[2];
		fs.read(bl);
		int NumCalls = GetDblByte(bl, 0);

		data = new byte[(NumCalls * 2)];
		fs.read(data);
		blockdata = new byte[data.length+2];
		
		System.arraycopy(data, 0, blockdata, 2 , data.length);
		blockdata[0] = bl[0];
		blockdata[1] = bl[1];

		rawdata = new byte[blockdata.length+1];
		rawdata[0] = (byte) (blocktype & 0xff);
		System.arraycopy(blockdata, 0, rawdata, 1, blockdata.length);
		
		//decode call sequence.
		Blocks = new int[NumCalls];
		
		int dataPtr = 0;
		int arrayPtr = 0;
		while (dataPtr < data.length) {
			Blocks[arrayPtr++] = GetDblByte(data, dataPtr);
			dataPtr = dataPtr + 2;
		}
	}
}

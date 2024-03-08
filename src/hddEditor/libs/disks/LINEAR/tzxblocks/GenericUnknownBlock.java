package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This is a basic unsupported datablock using the TZX definition of future blocks (DWORD first containing data)
 */

public class GenericUnknownBlock  extends TZXBlock {
	
	public GenericUnknownBlock(RandomAccessFile fs, int bType, String bDesc) throws IOException {
		blocktype = bType;
		BlockDesc = bDesc;		
		
		byte rawBlockSize[] = new byte[4];
		fs.read(rawBlockSize);
		
		int BlockSize = GetDWORD(rawBlockSize, 0);
		
		data = new byte[BlockSize];
		fs.read(data);
		
		blockdata = new byte[BlockSize + 4];
		System.arraycopy(rawBlockSize, 0, blockdata, 0, 4);
		System.arraycopy(data,0,blockdata,4,BlockSize);
		
		rawdata = new byte[blockdata.length+1];
		rawdata[0] = (byte) (blocktype & 0xff);
		System.arraycopy(blockdata, 0, rawdata, 1, blockdata.length);
	}
	
	@Override
	public String toString() {
		String result = String.format("%s (%02X)", BlockDesc, blocktype)+" Content length:"+data.length+" entries\n";
		return(result);
	}
}

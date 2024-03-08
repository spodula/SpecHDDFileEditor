package hddEditor.libs.disks.LINEAR.tzxblocks;
/**
 * SNAPSHOT block. Depreciated in the TZX specs, and i can't
 * find any examples of this, so UNTESTED!
 */

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class SnapshotBlock extends TZXBlock {
	public int SnapShotType;
	public int SnapShotLength;
	
	public SnapshotBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_SNAPSHOT;
		BlockDesc = "Snapshot block";
		
		byte Header[] = new byte[0x04];
		fs.read(Header);
		
		SnapShotType = (Header[0] & 0xff);
		
		int w1 = GetDblByte(Header, 1); 
		int w2 = (Header[3] & 0xff);
		SnapShotLength = w1+(w2*0x10000);

		data = new byte[SnapShotLength];
		fs.read(data);
		
		blockdata = new byte[SnapShotLength+0x04];
		System.arraycopy(Header, 0, blockdata, 0, 4);
		System.arraycopy(data, 0, blockdata, 0x04, data.length);
		
		rawdata = new byte[blockdata.length+1];
		rawdata[0] = (byte) (blocktype & 0xff);
		System.arraycopy(blockdata, 0, rawdata, 1, blockdata.length);
	}
	
	@Override
	public String toString() {
		String sType = "Z80";
		if (SnapShotType==0x01) {
			sType = "SNA";
		}
		String result = super.toString()+" Type:"+sType;
		return(result);
	}
	

}

package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class CustomInfoBlock extends TZXBlock {
	public String ID;
	public byte data[];
	
	public CustomInfoBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_CUSTOMINFO;
		BlockDesc = "Custom Info";
		
		byte IDstring[] = new byte[0x10];
		fs.read(IDstring);
		ID = new String(IDstring);
		
		byte CustInfoLen[] = new byte[4];
		fs.read(CustInfoLen);
		int CustomInfoLength = GetDblByte(CustInfoLen, 0) + (0x10000 * GetDblByte(CustInfoLen, 2));
		
		data = new byte[CustomInfoLength];
		fs.read(data);
		
		blockdata = new byte[CustomInfoLength+0x14];
		System.arraycopy(IDstring, 0, blockdata, 0, 0x10);
		System.arraycopy(CustInfoLen, 0, blockdata, 0x10, 0x04);
		System.arraycopy(data, 0, blockdata, 0x10, data.length);
		
		rawdata = new byte[blockdata.length+1];
		rawdata[0] = (byte)(blocktype & 0xff);
		System.arraycopy(blockdata, 0, rawdata, 1, blockdata.length);
		
		System.out.println(String.format("%02X %02X %02X %02X" , CustInfoLen[0],CustInfoLen[1],CustInfoLen[2],CustInfoLen[3]));
		
	}
	
	@Override
	public String toString() {
		String result = "ID: "+ID+" Len:"+data.length+" First 10 bytes:";
		int x=10;
		if (data.length < 10)
			x = data.length;
		for(int i=0;i<x;i++) {
			result = result + String.format("%02X ",data[i]);
		}
		return(result);
	}

}

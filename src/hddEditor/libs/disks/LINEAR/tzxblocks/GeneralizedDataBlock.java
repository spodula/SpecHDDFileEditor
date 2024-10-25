package hddEditor.libs.disks.LINEAR.tzxblocks;
/**
 * Generalized datablock.
 * 
 * Not even going to try to decode most of this.
 */

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class GeneralizedDataBlock  extends TZXBlock {
	public int blocklength;
	public int blockpause;
	public int TOTP;
	public int NPP;
	public int ASP;
	public int TOTD;
	public int NPD;
	public int ASD;
	
	public GeneralizedDataBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_GENERAL;
		
		byte Header[] = new byte[0x12];
		fs.read(Header);
		
		blocklength = GetDWORD(Header, 0x00);
		blockpause  = GetDblByte(Header, 0x04);
		TOTP 		= GetDWORD(Header, 0x06);
		NPP			= (Header[0x0A] & 0xff);
		ASP			= (Header[0x0B] & 0xff);
		TOTD        = GetDWORD(Header, 0x0c);
		NPD         = (Header[0x10] & 0xff);
		ASD         = (Header[0x11] & 0xff);
	
		data = new byte[blocklength-0x0e];
		fs.read(data);
		
		blockdata = new byte[blocklength+0x04];
		System.arraycopy(Header, 0, blockdata, 0, 0x12);
		System.arraycopy(data,0,blockdata,0x12,data.length);
		
		rawdata = new byte[blockdata.length+1];
		rawdata[0] = (byte) (blocktype & 0xff);
		System.arraycopy(blockdata, 0, rawdata, 1, blockdata.length);
	}
	
	/**
	 * 
	 */
	public String toString() {
		String result = super.toString()+" Pause: "+blockpause+" Pilot/sync symbols used: "+TOTP+" Data symbols used:"+TOTD;
		return(result);
	}
	
	
}

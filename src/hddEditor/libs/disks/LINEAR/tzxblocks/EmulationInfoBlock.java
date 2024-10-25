package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class EmulationInfoBlock extends TZXBlock {
	int flags;
	int refreshdelay;
	int interruptfreq;
	byte unused[];
	
	public EmulationInfoBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_EMULATIONINFO;
		// read block
		data = new byte[0x08];
		fs.read(data);
		blockdata = data;
		
		rawdata = new byte[data.length+1];
		rawdata[0] = (byte) (blocktype & 0xff);
		System.arraycopy(data, 0, rawdata, 1, data.length);
		
		flags = GetDblByte(data, 0);
		refreshdelay = (data[0x02] & 0xff);
		interruptfreq = GetDblByte(data, 0x03);
		unused[0] = data[5];
		unused[1] = data[6];
		unused[2] = data[7];
	}

	String EmuFlags[] = {"R register emulation","LDIR emulation","HR Colour emulation","Vsync","","Fast ROM loading","Border emulation","Screen refresh mode","Start tape immediately","Auto load"};
	
	@Override
	public String toString() {
		String result = super.toString();
		result = result +" Screen Refresh delay:"+refreshdelay+" Interrupt freq:"+interruptfreq+"Hz";
		
		int bit = 0x01;
		for (String flagname:EmuFlags) {
			if (flagname.equals("Vsync")) {
				int val = (flags & 0x18) / 8;
				String set = "Normal";
				if (val==1) {
					set = "high";
				} else if (val==3) {
					set = "low";
				}
				result = result +" Vsync: "+set;
				
			} else if (!flagname.isBlank()) {
				String set="Off";
				if ((flags & bit) != 0) {
					set = "on";
				}
				result = result +" "+ flagname +": "+set;
			}
			bit = bit*2;
		}
		return (result);
	}

	
}

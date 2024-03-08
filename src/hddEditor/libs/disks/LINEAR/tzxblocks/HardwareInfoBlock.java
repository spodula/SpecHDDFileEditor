package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class HardwareInfoBlock  extends TZXBlock {
	public HardwareInfoEntry Entries[];
	
	/**
	 * This class is mainly storage and decoding of the individual hardware types.
	 */
	public class HardwareInfoEntry {
		byte rawdata[] = new byte[3];
		
		public HardwareInfoEntry(byte data[]) {
			rawdata[0] = data[0];
			rawdata[1] = data[1];
			rawdata[2] = data[2];
		}
		
		/**
		 * 
		 * @return
		 */
		public String GetHWInfo() {
			if (rawdata[2] < TZX.hwInfo.length) {
				return TZX.hwInfo[rawdata[2]];
			} else {
				return "Invalid value";
			}
		}
		
		/**
		 * 
		 * @return
		 */
		public String getHardwareType() {
			if (rawdata[0] < TZX.hwType.length) {
				return TZX.hwType[rawdata[0]];
			} else {
				return "Invalid value";
			}	
		}
		/**
		 * 
		 * @return
		 */
		public String GetHardwareInfo() {
			if (rawdata[0] < TZX.hwType.length) {
				String branch[] = TZX.HwInfoMatrix[rawdata[0]];
				if (rawdata[1] < branch.length) {
					int bLoc = rawdata[1]; 
					String data = branch[bLoc];
					return data;
				}
				return "Undefined hardware type";
			} else {
				return "Invalid value";
			}	
		}
		
		@Override
		public String toString() {
			String result = String.format("%02X %02X %02X", rawdata[0], rawdata[1], rawdata[2])+": "+getHardwareType()+" - "+ GetHardwareInfo()+" - "+GetHWInfo();
			return (result);
		}
		
	}
		
	public HardwareInfoBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_HARDWARETYPE;
		BlockDesc = "Hardware Info block";
		
		//read number of entries
		byte bl[] = new byte[1];
		fs.read(bl);
		int NumEntries = (bl[0] & 0xff);
		
		rawdata = new byte[(NumEntries *3) +2];
		rawdata[0] = (byte) (blocktype & 0xff);
		rawdata[1] = (byte) (NumEntries & 0xff);
		
		//Update Entry list
		Entries = new HardwareInfoEntry[NumEntries];
		int ptr=2;
		for(int i=0;i<NumEntries;i++) {
			byte block[] = new byte[3];
			fs.read(block);
			Entries[i] = new HardwareInfoEntry(block);
			System.arraycopy(block, 0, rawdata, ptr, 3);
			ptr = ptr + 3;
		}
		//Update the Blockdata and data
		blockdata = new byte[NumEntries *3];
		System.arraycopy(rawdata, 2, blockdata, 0, NumEntries*3);
		data = rawdata;
	}
	
	@Override
	public String toString() {
		String result = super.toString()+" ";
		for (HardwareInfoEntry e:Entries) {
			result = result +System.lineSeparator()+" "+e;
		}
		return (result);
	}
	

}

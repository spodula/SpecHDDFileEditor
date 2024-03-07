package hddEditor.libs.disks.LINEAR.tzxblocks;

/**
 * Support for Datablock type 28 (Select block)
 * Searching the TOSEC archive, it Only appears to be used on
 *    "Zynaps (1987)(Hewson Consultants)[a2].tzx" 
 *
 * Block layout:
 * =============
 *  00      0x28          +------ rawdata
 *  01 02   block length  | +---- Blockdata
 *  03      num entries   | | +-- data
 *  04      entries       / / /
 */

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class SelectBlock extends TZXBlock {
	public SelectItem[] Entries;

	public class SelectItem {
		public int relOffset;
		public int descTextlen;
		public String text;

		public SelectItem(int reloffset, int textlen, String Text) {
			relOffset = reloffset;
			descTextlen = textlen;
			text = Text;
		}

		@Override
		public String toString() {
			String result = "Offset: " + relOffset + " Len:" + descTextlen + " text:" + text;
			return (result);
		}
	}

	/**
	 * 
	 * @param fs
	 * @throws IOException
	 */
	public SelectBlock(RandomAccessFile fs) throws IOException {
		blocktype = TZX.TZX_SELECTBLOCK;
		BlockDesc = "SELECT block";

		// read block size
		byte bl[] = new byte[2];
		fs.read(bl);
		int size = GetDblByte(bl, 0);

		// Read all the entries and the number of selections to the datablock
		data = new byte[size];
		fs.read(data);

		// Block data is all this plus the blocksize
		blockdata = new byte[size + 2];
		blockdata[0] = bl[0];
		blockdata[1] = bl[1];
		System.arraycopy(data, 0, blockdata, 2, size);

		// raw data is all this plus the block type
		rawdata = new byte[size + 3];
		rawdata[0] = (byte) (blocktype & 0xff);
		System.arraycopy(blockdata, 0, rawdata, 1, blockdata.length);

		// Now to decode the selections
		int numselections = (data[0] & 0xff);
		Entries = new SelectItem[numselections];
		int dataPtr = 1;
		int entPtr = 0;
		try {
			while (dataPtr < data.length) {
				int reloffset = GetDblByte(data, dataPtr);
				dataPtr = dataPtr + 2;
				int desctextlen = (data[dataPtr++] & 0xff);
				byte txt[] = new byte[desctextlen];
				System.arraycopy(data, dataPtr, txt, 0, desctextlen);
				String desctext = new String(txt);
				dataPtr = dataPtr + desctextlen;
				Entries[entPtr++] = new SelectItem(reloffset, desctextlen, desctext);
			}
		} catch (Exception e) {
			System.err.println("Badly formatted select block.");
			Entries[entPtr++] = new SelectItem(0, 0, "Badly formatted block.");
		}
	}

	@Override
	public String toString() {
		String result = String.format("%s (%02X) ", BlockDesc, blocktype);
		for (SelectItem e : Entries) {
			result = result + System.lineSeparator() + e;
		}
		return (result);
	}

}

package hddEditor.libs.partitions.tap;

import java.io.IOException;

import hddEditor.libs.disks.ExtendedSpeccyBasicDetails;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.disks.LINEAR.TAPFile.TAPBlock;

public class TapDirectoryEntry implements FileEntry {
	public TAPBlock HeaderBlock = null;
	public TAPBlock DataBlock = null;

	public TapDirectoryEntry(TAPBlock Data, TAPBlock header) {
		HeaderBlock = header;
		DataBlock = Data;
	}

	@Override
	public String GetFilename() {
		String name = "Block" + DataBlock.blocknum;
		if (HeaderBlock != null) {
			name = HeaderBlock.DecodeHeader().filename;
		}

		return name;
	}

	@Override
	public void SetFilename(String filename) throws IOException {
		if (HeaderBlock!= null) {
			byte data[] = HeaderBlock.data;
			filename = filename+"             ";
			for(int i=0;i<10;i++) {
				data[i+1] = (byte) (filename.charAt(i) & 0xff);
			}
			HeaderBlock.data = data;
		}
	}

	@Override
	public boolean DoesMatch(String wildcard) {
		String StringToMatch = GetFilename().toUpperCase();
		// convert the wildcard into a search array:
		// Split into filename and extension. pad out with spaces.
		wildcard = wildcard.trim().toUpperCase();
		wildcard = wildcard + "            ";

		// create search array.
		byte comp[] = new byte[10];

		// populate with filename
		boolean foundstar = false;
		for (int i = 0; i < 10; i++) {
			if (foundstar) {
				comp[i] = '?';
			} else {
				char c = wildcard.charAt(i);
				if (c == '*') {
					foundstar = true;
					comp[i] = '?';
				} else {
					comp[i] = (byte) ((int) c & 0xff);
				}
			}
		}

		StringToMatch = (StringToMatch + "          ").substring(0, 10);
		// now search.
		// check the filename
		boolean match = true;
		for (int i = 0; i < 10; i++) {
			byte chr = (byte) StringToMatch.charAt(i);
			byte cchr = comp[i];
			if ((chr != cchr) && (cchr != '?')) {
				match = false;
			}
		}
		return (match);
	}

	@Override
	public int GetRawFileSize() {
		return DataBlock.data.length;
	}

	@Override
	public int GetFileSize() {
		if (HeaderBlock != null) {
			ExtendedSpeccyBasicDetails epd = HeaderBlock.DecodeHeader();
			if (epd != null) {
				return (epd.filelength);
			}
		}
		return DataBlock.data.length;
	}

	@Override
	public String GetFileTypeString() {
		if (HeaderBlock != null) {
			ExtendedSpeccyBasicDetails epd = HeaderBlock.DecodeHeader();
			if (epd != null) {
				return (epd.BasicTypeString());
			} else {
				return ("File with bad header");
			}
		} else {
			return ("Headerless #"+DataBlock.flagbyte);
		}
	}

	@Override
	public SpeccyBasicDetails GetSpeccyBasicDetails() {
		if (HeaderBlock != null) {
			return (HeaderBlock.DecodeHeader());
		} else {
			return (new ExtendedSpeccyBasicDetails(-1, DataBlock.data.length, 32768, 32768, 'A',
					"Headerless block #" + DataBlock.blocknum, DataBlock.data.length));
		}
	}

	@Override
	public byte[] GetFileData() throws IOException {
		return DataBlock.data;
	}

	@Override
	public byte[] GetFileRawData() throws IOException {
		return DataBlock.data;
	}

}

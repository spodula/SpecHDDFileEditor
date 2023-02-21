package hddEditor.libs.partitions.system;

/**
 * This is a dummy file entry. It is used by the SYSTEM partition to provide a list of
 * partitions as files so they can be treated as such.
 * 
 */

import java.io.IOException;

import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;

public class DummyFileEntry implements FileEntry {
	private String filename = "";
	private int filesize = 0;
	private String filetype = "";

	public DummyFileEntry(String filename, int filesize, String filetype) {
		this.filename = filename;
		this.filesize = filesize;
		this.filetype = filetype;
	}

	@Override
	public String GetFilename() {
		return filename;
	}

	@Override
	public void SetFilename(String filename) throws IOException {
		this.filename = filename;
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
		return filesize;
	}

	@Override
	public int GetFileSize() {
		return filesize;
	}

	@Override
	public String GetFileTypeString() {
		return filetype;
	}

	@Override
	public SpeccyBasicDetails GetSpeccyBasicDetails() {
		SpeccyBasicDetails result = new SpeccyBasicDetails(-1, 0, 0, 0, 'A');
		return (result);
	}

	@Override
	public byte[] GetFileData() throws IOException {
		System.out.println("Cannot get Raw file data for this entry");
		return null;
	}

	@Override
	public byte[] GetFileRawData() throws IOException {
		System.out.println("Cannot get Raw file data for this entry");
		return null;
	}

}

package hddEditor.libs.snapshots;

public class Z80Page {
	// Length while compressed
	public int CompressedLength;
	// Is block compressed
	public boolean IsCompressed;
	// z80 page number. (Note, this differs from the 128k page number).
	public int Pagenum;
	// Raw (compressed) data
	public byte Rawdata[];
	// uncompressed data
	public byte Data[];

	/**
	 * return details as a string.
	 */
	@Override
	public String toString() {
		return ("Page: " + Pagenum + "  compressed?:" + IsCompressed + "  Compressed length: " + CompressedLength);
	}

	public int get128Page() {
		int result = Math.max(-1, Pagenum - 3);
		if (result > 7)
			result = -1;
		return (result);
	}

}

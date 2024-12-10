package hddEditor.libs.snapshots;
/**
 * Wrapper for a data page in a .z80 file.
 * 
 * (From https://worldofspectrum.org/faq/reference/z80format.htm )
 * 
 */

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

	/**
	 * Get the 128K page given the z80 page number.
	 * https://worldofspectrum.org/faq/reference/z80format.htm
	 * 
	 *    Page    In '48 mode     In '128 mode 
     *   --------------------------------------
     *    0      48K rom         rom (basic)   
     *    1      Interface I, Disciple or Plus D rom, according to setting
     *    2      -               rom (reset)   
     *    3      -               page 0        
     *    4      8000-bfff       page 1        
     *    5      c000-ffff       page 2        
     *    6      -               page 3        
     *    7      -               page 4        
     *    8      4000-7fff       page 5        
     *    9      -               page 6        
     *   10      -               page 7        
     *   11      Multiface rom   Multiface rom 
	 * 
	 * @return 
	 * 	If z80 pages 3-10, return 128k page 0-7, else -1 if the page is not ram. 
	 */
	public int get128Page() {
		int result = Math.max(-1, Pagenum - 3);
		if (result > 7)
			result = -1;
		return (result);
	}

}

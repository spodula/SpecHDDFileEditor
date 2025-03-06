package hddEditor.libs.snapshots.readers.zxStateBlocks;

/*-*
 * Implements the ZXSTATARAM block
 * https://www.spectaculator.com/docs/zx-state/ataspram.shtml
 * This contains one 16K Ram page from a zxATASP interface.
 * 
 * $00..$03 	ID: "ATRP"  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTATARAM information====
 * $08-$09		Flags
 * $0A			Page number
 * $0B....		Page data
 * 
 */
public class ZXStATARam extends ZXStRamPage {

	public ZXStATARam(byte[] rawdata, int start) {
		super(rawdata, start);
	}

}

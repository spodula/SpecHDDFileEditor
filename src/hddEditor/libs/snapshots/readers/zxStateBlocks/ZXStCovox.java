package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * UNTESTED
 * Implements the ZXSTCOVOX block
 * https://www.spectaculator.com/docs/zx-state/covox.shtml
 * This contains the volume of the Covox interface
 * 
 * $00..$03 	ID: "COVX"  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) Should always be 4
 * ====start of ZXSTCOVOX information====
 * $08			Volume
 * $09..$0b		Reserved
 */
public class ZXStCovox extends GenericZXStateBlock {
	public int Volume;
	
	public ZXStCovox(byte[] rawdata, int start) {
		super(rawdata, start);
		Volume = rawword(0x08);
	}

	/**
	 * Add in the ZXSTCOVOX values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Volume:"+ Volume;
		return (result.trim());
	}

}

package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * Implements the ZXSTSPECDRUM block
 * https://www.spectaculator.com/docs/zx-state/specdrum.shtml
 * This contains the volume of the SpecDrum interface.
 * 
 * $00..$03 	ID: "DRUM" ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) Should always be 2 
 * ====start of ZXSTSPECDRUM information====
 * $08 			Current volume (+127 to -128)
 */
public class ZXStSpecDrum extends GenericZXStateBlock {
	public int Volume;
	
	public ZXStSpecDrum(byte[] rawdata, int start) {
		super(rawdata, start);
		Volume = rawword(0x08);
		if (Volume > 127)
			Volume = - Volume;
	}

	/**
	 * Add in the ZXSTSPECDRUM values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Volume:"+ Volume;
		return (result.trim());
	}

}

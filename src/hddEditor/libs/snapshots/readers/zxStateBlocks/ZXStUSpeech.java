package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * Implements the ZXSTUSPEECH block
 * https://www.spectaculator.com/docs/zx-state/zxprinter.shtml
 * This contains flag on if the ZX printer is connected. 
 * 
 * $00..$03 	ID: "USPE" ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) Should always be 2 
 * ====start of ZXSTUSPEECH information====
 * $08 			Flags
 */

public class ZXStUSpeech extends GenericZXStateBlock {
	public int flags;
	
	public ZXStUSpeech(byte[] rawdata, int start) {
		super(rawdata, start);
		flags = rawbyte(0x08);
	}
	
	/**
	 * Add in the ZXStUSpeech values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Rom Paged:"+ GetFlagsAsString();
		return (result.trim());
	}
	
	public String GetFlagsAsString() {
		String result = "Rom Not Paged";
		if ((flags & 0x01) == 0x01) {
			result = "Rom paged";
		}
		return(result);
	}
	

}

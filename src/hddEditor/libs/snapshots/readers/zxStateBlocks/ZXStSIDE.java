package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * Implements the ZXSTSIDE block
 * https://www.spectaculator.com/docs/zx-state/simple8bitide.shtml
 * Indicates the presence of a simple IDE interface. 
 * 
 * $00..$03 	ID: "SIDE" ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) Should always be 0 as there is no data
 * ====start of ZXSTSIDE information====
 * 
 * Nothing here. this block exists *purely* to indicate the interface's presence
 */
public class ZXStSIDE extends GenericZXStateBlock {

	public ZXStSIDE(byte[] rawdata, int start) {
		super(rawdata, start);
	}
	
	/**
	 * Add in the ZXSTSIDE values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		return (result.trim());
	}

}

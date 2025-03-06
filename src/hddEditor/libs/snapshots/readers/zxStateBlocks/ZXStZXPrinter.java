package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * Implements the ZXSTZXPRINTER block
 * https://www.spectaculator.com/docs/zx-state/zxprinter.shtml
 * This contains flag on if the ZX printer is connected. 
 * 
 * $00..$03 	ID: "ZXPR" ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) Should always be 2 
 * ====start of ZXSTZXPRINTER information====
 * $08=$09		Flags
 */

public class ZXStZXPrinter extends GenericZXStateBlock {
	public int flags;
	
	public ZXStZXPrinter(byte[] rawdata, int start) {
		super(rawdata, start);
		flags = rawword(0x08);
	}
	
	/**
	 * Add in the ZXStZXPrinter values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Printer State:"+ GetFlagsAsString();
		return (result.trim());
	}
	
	public String GetFlagsAsString() {
		String result = "No ZX Printer";
		if ((flags & 0x01) == 0x01) {
			result = "ZX Printer connected";
		}
		return(result);
	}
	

}

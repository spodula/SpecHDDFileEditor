package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * UNTESTED
 * Implements the ZXSTATASP block
 * https://www.spectaculator.com/docs/zx-state/atasp.shtml
 * Contains detail of the ZXATASP IDE hard disk interface. 
 * 
 * $00..$03 	ID: "ZXCF" ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTATASP information====
 * $08..$09		Flags
 * $0A			Num Ram Pages
 * $0B			Active page
 */
public class ZXStCF extends GenericZXStateBlock {
	public static String ZXCFFlags[][] = {
			{"1","ZXSTCF_UPLOADJUMPER","upload jumper on the interface is enabled"},
	};
	
	public int flags;
	public int NumRamPages;
	public int ActivePage;

	
	public ZXStCF(byte[] rawdata, int start) {
		super(rawdata, start);
		flags = rawword(0x08);
		NumRamPages = rawbyte(0x0A);
		ActivePage = rawbyte(0x0B);
	}
	
	/**
	 * Add in the ZXSTSPECDRUM values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " #Pages"+NumRamPages+" ActivePg:"+ActivePage+" Flags: "+flags+" - "+ GetFlags();
		
		return (result.trim());
	}

	public String GetFlags() {
		return(GetFlagsFromArray(ZXCFFlags, flags,2));
	}

}

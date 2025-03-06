package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * UNTESTED
 * Implements the ZXSTATASP block
 * https://www.spectaculator.com/docs/zx-state/atasp.shtml
 * Contains detail of the ZXATASP IDE hard disk interface. 
 * 
 * $00..$03 	ID: "ZXAT" ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTATASP information====
 * $08..$09		Flags
 * $0A			chPortA
 * $0B			chPortB
 * $0C			chPortC
 * $0D			chControl
 * $0E			Num Ram Pages
 * $0F			Active page
 */
public class ZXStATASp extends GenericZXStateBlock {
	public static String ATAFlags[][] = {
			{"1","ZXSTAF_UPLOADJUMPER","upload jumper on the interface is enabled"},
			{"2","ZXSTAF_WRITEPROTECT","on-board memory is write protected."}
	};
	
	public int flags;
	public int PortA;
	public int PortB;
	public int PortC;
	public int Control;
	public int NumRamPages;
	public int ActivePage;
	
	public ZXStATASp(byte[] rawdata, int start) {
		super(rawdata, start);
		flags = rawword(0x08);
		PortA = rawbyte(0x0A);
		PortB = rawbyte(0x0B);
		PortC = rawbyte(0x0C);
		Control = rawbyte(0x0D);
		NumRamPages = rawbyte(0x0E);
		ActivePage = rawbyte(0x0F);
	}
	
	
	/**
	 * Add in the ZXSTSPECDRUM values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " PA:"+PortA+" PB:"+PortB+" PC:"+PortC+" Ctrl:"+Control+" #Pages"+NumRamPages+" ActivePg:"+ActivePage+" Flags: "+flags+" - "+ GetFlags();
		
		return (result.trim());
	}

	public String GetFlags() {
		return(GetFlagsFromArray(ATAFlags, flags,2));
	}
	
}

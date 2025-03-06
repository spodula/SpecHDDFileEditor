package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * Implements the ZXSTAYBLOCK block
 * https://www.spectaculator.com/docs/zx-state/ay.shtml
 * This contains information on the AY chip used on all 128K speccys 
 * and Fuller box or Melodic boxes
 * 
 * $00..$03 	ID: "AY",$0,$0 ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTAYBLOCK information====
 * $08			flags
 * $09			Selected AY Register
 * $0A-$19	 	AY Registers
 */

public class ZXStAyBlock extends GenericZXStateBlock {
	public static String AYType[][] = {
			{"1","ZXSTAYF_FULLERBOX","Fuller box"},
			{"2","ZXSTAYF_128AY","128K AY/Melodik"}
	};
	
	public int flags;
	public int currentReg;
	public int Registers[];
	private boolean Is128K;
	
	public ZXStAyBlock(byte[] rawdata, int start, boolean Is128) {
		super(rawdata, start);
		flags = rawbyte(0x08);
		currentReg = rawbyte(0x09);
		Registers = new int[16];
		for (int i=0;i<16;i++) {
			Registers[i] = rawbyte(0x0A+i);
		}
		Is128K = Is128;			
	}
	/**
	 * Add in the ZXStAyBlock values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Emulation type: "+GetAYEmulationAsString()+"("+flags+") Reg:"+currentReg+" contents: ";
		for (int reg:Registers) {
			result = result + Hex(reg)+" ";
		}
		
		return (result.trim());
	}
	
	public String GetAYEmulationAsString() {
		if (Is128K) {
			return("Internal 128K AY chip");
		}
		return (GetNameFromArray(AYType, String.valueOf(flags), 0, 2));
	}
	

}

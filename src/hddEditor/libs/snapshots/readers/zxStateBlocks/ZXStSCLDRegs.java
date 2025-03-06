package hddEditor.libs.snapshots.readers.zxStateBlocks;
/**
 * UNTESTED
 * Implements the ZXSTSCLDREGS block
 * https://www.spectaculator.com/docs/zx-state/scld.shtml
 * Timex/Sinclair specific ports.
 * Snapshots using the standard roms wont have one of these.
 * 
 * $00..$03  ID: "SCLD") 		) Decoded by the parent class. 
 * $04..$07  size: LSB..MSB		) Always 2
 * ====start of  ZXSTSCLDREGS information====
 * $08			F4 port
 * $09			FF port
 */
public class ZXStSCLDRegs extends GenericZXStateBlock {
	public int PortF4;
	public int PortFF;
	
	public ZXStSCLDRegs(byte[] rawdata, int start) {
		super(rawdata, start);
		PortF4 = rawbyte(0x08);
		PortFF = rawbyte(0x09);
	}
	
	/**
	 * Add in the ZXSTSCLDREGS specific items
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result +" $F4:"+PortF4+" $FF:"+PortFF;
		
		return (result);
	}

}

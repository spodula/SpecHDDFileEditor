package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * Implements the ZXSTZ80REGS block
 * https://www.spectaculator.com/docs/zx-state/specregs.shtml
 * This contains the Machine registers (IE, Border colour, ect)
 * 
 * $00..$03  ID: "SPCR" 		) Decoded by the parent class. 
 * $04..$07  size: LSB..MSB		) Always 8 here.
 * ====start of  ZXSTSPECREGS information====
 * $08 		 Border colour
 * $09		 last value sent to $7FFD
 * $0A		 last value sent to $1FFD OR $eff7 on the Pentagon 1024. 
 * $0B		 last value sent to $FE
 * $0C-$0F   reserved
 */

public class ZXStSpecRegs extends GenericZXStateBlock {
	public int border;
	public int io7ffd;
	public int io1ffd;
	public int ioFe;
	
	public ZXStSpecRegs(byte[] rawdata, int start) {
		super(rawdata, start);
		
		border = rawbyte(0x08);
		io7ffd = rawbyte(0x09);
		io1ffd = rawbyte(0x0A);
		ioFe   = rawbyte(0x0B);
	}
	
	/**
	 *	Add in the ZSXSTZ80Regs values. 
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Border:"+border+" 7FFD:"+io7ffd+" 1FFD:"+io1ffd+" $FE:"+ioFe;
		
		return(result);
	}

}

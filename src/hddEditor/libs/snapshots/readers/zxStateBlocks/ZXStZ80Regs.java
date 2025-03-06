package hddEditor.libs.snapshots.readers.zxStateBlocks;

import hddEditor.libs.snapshots.CPUState;
import hddEditor.libs.snapshots.Registers;

/*-*
 * Implements the ZXSTZ80REGS block
 * https://www.spectaculator.com/docs/zx-state/z80regs.shtml
 * This contains the CPU registers
 * 
 * $00..$03  ID: "Z80R" 		) Decoded by the parent class. 
 * $04..$07  size: LSB..MSB		)
 * ====start of  ZXSTCREATOR information====
 * $08..$0f  AF BC DE HL
 * $10..$17  AF' BC' DE' HL'
 * $18..$19  IX
 * $1a..$1b  IY
 * $1c..$1d  SP
 * $1e..$1f PC
 * $20 		 I
 * $21       R
 * $22		 IFF1
 * $23		 IFF2
 * $24		 IM
 * $25..$28  T-States since start
 * $29		 Hold cycles
 * $2A		 Flags
 * $2B..$2C  wierd mem ptr thingy
 */

public class ZXStZ80Regs extends GenericZXStateBlock {
	public static String[][] Stateflags = { 
			{"1","ZXSTZF_EILAST","Last instruction was EU or $DD,$FD"},
			{"2","ZXSTZF_HALTED","CPU is halted"}
	};
	
	
	public static int ZXSTZF_EILAST=1;
	public static int ZXSTZF_HALTED=2;
	
	public CPUState cpustate;
	
	public int IFF1,IFF2;	
	
	public int TSTATES;
	public int HoldIntReqCyc;
	public int Flags;
	public int wMemPtr;
	
	public ZXStZ80Regs(byte[] rawdata, int start) {
		super(rawdata, start);
		Registers main = new Registers(raw[0x08], raw[0x09], raw[0x0a], raw[0x0b], raw[0x0c], raw[0x0d], raw[0x0e], raw[0x0f]);
		Registers alt  = new Registers(raw[0x10], raw[0x11], raw[0x12], raw[0x13], raw[0x14], raw[0x15], raw[0x16], raw[0x17]);
		
		boolean ei = (rawbyte(0x22)!=0x00);
				
		cpustate = new CPUState(main, alt, (byte)rawbyte(0x20), (byte)rawbyte(0x18), (byte)rawbyte(0x19), (byte)rawbyte(0x1A), (byte)rawbyte(0x1B), (byte)rawbyte(0x21), (byte)rawbyte(0x24),
				ei, (byte)rawbyte(0x1C), (byte)rawbyte(0x1D), rawword(0x1e)); 
		
		this.IFF1 = rawbyte(0x22);
		this.IFF2 = rawbyte(0x23); 
		
		this.TSTATES = rawDword(0x25);
		this.HoldIntReqCyc = rawbyte(0x29);
		this.Flags = rawbyte(0x2A);
		this.wMemPtr = rawword(0x2b);
		
	}
	
	/**
	 *	Add in the ZSXSTZ80Regs values. 
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " "+cpustate+" Cycles:"+TSTATES+" HoldInt:"+HoldIntReqCyc+" Flags: "+GetFlags()+" ("+Hex(Flags)+") wMemPtr:"+wMemPtr;
		return (result);
	}
	
	/**
	 * Decode basic flags.
	 * @return
	 */
	private String GetFlags() {
		return(GetFlagsFromArray(Stateflags,Flags,1));
	}

}

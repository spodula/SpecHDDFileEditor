package hddEditor.libs.snapshots.readers.zxStateBlocks;

import hddEditor.libs.snapshots.CPUState;
import hddEditor.libs.snapshots.Registers;

/*-*
 * UNTESTED
 * Implements the ZXSTGS block
 * https://www.spectaculator.com/docs/zx-state/gs.shtml
 * Z80 Registers and internal state for the General Sound interface which appears
 * to be its own little z80 computer.
 * 
 * $00..$03 	ID: "GS",$0,$0  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTGS information====
 * $08     		Model
 * $09     		UpperPage
 * $0A..$0D 	GsChanVol[4]
 * $0E..$11		GsChanOut[4]
 * $12 			Flags
 * $13..$1A		AF, BC, DE, HL
 * $1B..$22		AF1, BC1, DE1, HL1
 * $23..$2A		IX, IY, SP, PC
 * $2B			I
 * $2C			R
 * $2D..$2E		IFF1, IFF2
 * $2F			IM
 * $30..$33		dwCyclesStart;
 * $34			chHoldIntReqCycles;
 * $35 			chBitReg;
 * $36..end		chRomData[1];  
 */
public class ZXStGs extends GenericZXStateBlock {
	public int ZXSTGSF_CUSTOMROM = 0x40;
	public int ZXSTGSF_COMPRESSED = 0x80;
	
	public static String[][] Model = {
			{"0","ZXSTGSM_128","128k version"},
			{"1","ZXSTGSM_512","512k version"},
	};
	
	public static String MachineFlags[][] = {
			{"1"  ,"ZXSTZF_EILAST","Last instruction was EI"},
			{"2"  ,"ZXSTZF_HALTED","Currently Halted"},
			{"64" ,"ZXSTGSF_CUSTOMROM","Custom rom installed"},
			{"128","ZXSTGSF_COMPRESSED","Custom rom is compressed"}
	};
	
	public int GSModel;
	public int UpperPage;
	public int ChannelVolume[];
	public int ChannelOut[];
	public int flags;
	public int IFF1,IFF2;

	public CPUState cpustate;
	
	public int TSTATES;
	public int HoldIntReqCyc;
	public int Flags;
	public byte RomData[];
	public byte RomDataDecompressed[];
	
	public ZXStGs(byte[] rawdata, int start) {
		super(rawdata, start);
		GSModel = rawbyte(0x08);
		UpperPage = rawbyte(0x09);
		ChannelVolume = new int[4];
		for(int i=0;i<4;i++)
			ChannelVolume[i] = rawbyte(0x0A+i);
		ChannelOut = new int[4];
		for(int i=0;i<4;i++)
			ChannelOut[i] = rawbyte(0x0E+i);
		flags = rawbyte(0x12);
		
		Registers main = new Registers(raw[0x13], raw[0x14], raw[0x15], raw[0x16], raw[0x17], raw[0x18], raw[0x19], raw[0x1A]);
		Registers alt  = new Registers(raw[0x1B], raw[0x1C], raw[0x1D], raw[0x1E], raw[0x1F], raw[0x20], raw[0x21], raw[0x22]);

		boolean ei = (rawbyte(0x2d)!=0x00);
		cpustate = new CPUState(main, alt, (byte)rawbyte(0x2b), (byte)rawbyte(0x13), (byte)rawbyte(0x24), (byte)rawbyte(0x25), (byte)rawbyte(0x26), (byte)rawbyte(0x2c), (byte)rawbyte(0x2f),
				ei, (byte)rawbyte(0x27), (byte)rawbyte(0x28), rawword(0x29));
		
		this.IFF1 = rawbyte(0x2d);
		this.IFF2 = rawbyte(0x2e);
		
		this.TSTATES = rawDword(0x30);
		this.HoldIntReqCyc = rawbyte(0x34);
		this.Flags = rawbyte(0x35);
		this.RomData = null;
		this.RomDataDecompressed = null;

		byte data[] = new byte[raw.length-0x0F];
		System.arraycopy(raw, 0x0F, data, 0, data.length);
		
		if ((flags & ZXSTGSF_CUSTOMROM) == ZXSTGSF_CUSTOMROM) {
			RomData = data;
			if ((flags & ZXSTGSF_COMPRESSED) == ZXSTGSF_COMPRESSED) {
				RomDataDecompressed = zLibDecompressData(data, 0x4000);
			} else {
				RomDataDecompressed = RomData;
			}
		} 
	}
	
	/**
	 * Add in the ZXStGs values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + "Model:"+GetModel()+" CPU: "+cpustate+" IFF1:"+Hex(IFF1)+
				" IFF2:"+Hex(IFF2)+" Cycles:"+TSTATES+" HoldInt:"+HoldIntReqCyc+" Flags: "+GetFlags()+" ("+Hex(Flags)+")";
		return (result.trim());
	}
	
	public String GetFlags() {
		return(GetFlagsFromArray(MachineFlags,Flags,1));
	}
	public String GetModel() {
		return (GetNameFromArray(Model,String.valueOf(GSModel), 0, 2));
	}

}

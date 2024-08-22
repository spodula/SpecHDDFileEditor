package hddEditor.libs.snapshots;

import java.util.Hashtable;

/**
 * Wrapper for the entire CPU state.
 * 
 * Note PC, is generally 0 for when its not provided, EG SNA files. 
 * In this case, when generating a file, you should replace any final JP with a RET.
 */

public class CPUState {
	public Registers MainRegs;
	public Registers AltRegs;
	public byte I;
	public byte IXL;
	public byte IXH;
	public byte IYL;
	public byte IYH;
	public byte R;
	public byte IM;
	public boolean EI;
	public byte SPL;
	public byte SPH;
	public int PC;

	protected CPUState(Registers main, Registers alt, byte i, byte ixl, byte ixh, byte iyl, byte iyh, byte r, byte im,
			boolean ei, byte spl, byte sph, int pc) {
		MainRegs = main;
		AltRegs = alt;
		I = i;
		IXL = ixl;
		IXH = ixh;
		IYL = iyl;
		IYH = iyh;
		R = r;
		IM = im;
		EI = ei;
		SPL = spl;
		SPH = sph;
		PC = pc;
	}

	private String outReg(String reg, byte dat) {
		String result = reg + ": ";
		int i = dat & 0xff;
		result = result + String.format("%2X(%d)", i, i);
		return (result);
	}

	@Override
	public String toString() {
		String result = "Normal regs: " + MainRegs + "\n" + "Alt regs: " + AltRegs + "\n" + outReg("I", I) + ", "
				+ outReg("R", R) + ", " + outReg("IM", IM) + ", EI:" + EI + "\nSP=" + String.format("%02X %2X", SPH, SPL)
				+ "\n" + outReg("IXH", IXH) + " " + outReg("IXL", IXL) + "\n" + outReg("IYH", IYH) + " " + outReg("IYL", IYL)
				+ "PC: "+ String.format("%04X", PC)+"\n";

		return (result);
	}
	
	/**
	 * 
	 * @return
	 */
	public Hashtable<String, String> DetailsAsArray() {
		Hashtable<String, String> result = new Hashtable<String, String>();

		result.putAll(MainRegs.RegisterAsArray(""));
		result.putAll(AltRegs.RegisterAsArray("'"));
		
		result.put("IX",String.format("%02x%02x",IXH,IXL));
		result.put("IY",String.format("%02x%02x",IYH,IYL));
		result.put("SP",String.format("%02x%02x",SPH,SPL));
		if (PC!=-1) {
			result.put("PC",String.format("%04x",PC));
		}
		result.put("I",String.format("%02x",I));
		result.put("R",String.format("%02x",R));
		result.put("IM",String.format("%02x",IM));
		
		if (EI) {
			result.put("Interrupts","Enabled");
		} else {
			result.put("Interrupts","Disabled");
		}
		
		return(result);
	}



}

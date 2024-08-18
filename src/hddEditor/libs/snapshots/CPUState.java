package hddEditor.libs.snapshots;
/**
 * Wrapper for the entire CPU state.
 * 
 * Note PC, is generally 0 for when its not provided, EG SNA files. 
 * In this case, when generating a file, you should replace any final JP with a RET.
 */

public class CPUState {
	Registers MainRegs;
	Registers AltRegs;
	byte I;
	byte IXL;
	byte IXH;
	byte IYL;
	byte IYH;
	byte R;
	byte IM;
	boolean EI;
	byte SPL;
	byte SPH;
	int PC;
	byte BORDER;
	byte RAM[];

	public CPUState(Registers main, Registers alt, byte i, byte ixl, byte ixh, byte iyl, byte iyh, byte r, byte im,
			boolean ei, byte spl, byte sph, byte border, int pc, byte ram[]) {
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
		BORDER = border;
		RAM = ram;
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

}

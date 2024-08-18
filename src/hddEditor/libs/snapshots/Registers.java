package hddEditor.libs.snapshots;
/**
 * Wrapper for one set of normal registers.
 */

public class Registers {
	public byte A;
	public byte F;
	public byte B;
	public byte C;
	public byte D;
	public byte E;
	public byte H;
	public byte L;

	public Registers(byte a, byte f, byte b, byte c, byte d, byte e, byte h, byte l) {
		A = a;
		F = f;
		B = b;
		C = c;
		D = d;
		E = e;
		H = h;
		L = l;
	}

	private String outReg(String reg, byte dat) {
		String result = reg + ": ";
		int i = dat & 0xff;
		result = result + String.format("%2X(%d)", i, i);
		return (result);
	}

	@Override
	public String toString() {
		return (outReg("A", A) + "," + outReg("F", F) + "," + outReg("B", B) + "," + outReg("C", C) + "," + outReg("D", D) + ","
				+ outReg("E", E) + "," + outReg("H", H) + "," + outReg("L", L));
	}

}

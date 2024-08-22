package hddEditor.libs.snapshots;
/**
 * Wrapper for one set of normal registers.
 */

import java.util.Hashtable;

public class Registers {
	public byte A;
	public byte F;
	public byte B;
	public byte C;
	public byte D;
	public byte E;
	public byte H;
	public byte L;

	public Registers() {
		A=0;
		F=0;
		B=0;
		C=0;
		D=0;
		E=0;
		H=0;
		L=0;
	}
	
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
	
	public Hashtable<String, String> RegisterAsArray(String suffix) {
		Hashtable<String, String> result = new Hashtable<String, String>();
		
		result.put("AF"+suffix,String.format("%02x%02x",A,F));
		result.put("BC"+suffix,String.format("%02x%02x",B,C));
		result.put("DE"+suffix,String.format("%02x%02x",D,E));
		result.put("HL"+suffix,String.format("%02x%02x",H,L));
		
		return(result);
	}

}

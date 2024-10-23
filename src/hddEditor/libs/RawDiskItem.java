package hddEditor.libs;

import java.io.File;

public class RawDiskItem {
	public long realsz; // Real size in bytes
	public String name; // Name of the disk, eg sda, sdb
	public File dets; // FILE pointing to the drive
	public String driveType; // Drive type, USB or ATA
	public String model; // Model number
	public String Vendor; // Vendor string
	public int BlockSize;
	public int Cyls;
	public int Heads;
	public int Sectors;
	
	@Override
	/**
	 * Overriden TOSTRING method.
	 */
	public String toString() {
		String result = "name:" + name + " Bs: "+BlockSize+" drivetype:" + driveType +" C/H/S:"+Cyls+"/"+Heads+"/"+Sectors+" model:" + model + " size:" + GetTextSz();
		return (result.trim());
	}

	public String GetTextSz() {
		String postfix[] = { "", "b", "Kb", "Mb", "Tb", "Pb" };
		long v = realsz;
		int pfptr = 0;
		while (v > 1000) {
			v = v / 1000;
			pfptr++;
		}
		String result = String.format("%d%s", v, postfix[pfptr]);
		return (result);
	}
}

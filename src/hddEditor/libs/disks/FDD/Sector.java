package hddEditor.libs.disks.FDD;

/**
 * Representation of a single sector including its data.
 * 
 * @author Graham
 *
 */

public class Sector {
	//Start of sector in file.
	public long SectorStart = 0;

	// Equivalent to the C, H, R flags from the 765
	public int track = 0; // Cyl
	public int side = 0; // Head
	public int sectorID = 0; // Sector

	// the N parameter from the 765, IE, what the FDC *thinks* its read.
	public int Sectorsz = 0;

	// Representation of the FDC status flags when the read is completed. Used for
	// emulation.
	public int FDCsr1 = 0;
	public int FDCsr2 = 0;

	// Actual size of the data. This may differ from the default size presented in
	// the track. Usually as some form as copy protection.
	public int ActualSize = 0;

	// Raw data
	public byte[] data;

	public String AsString() {
		String result = "CHS: "+track+"/"+side+"/"+sectorID+
						"\n Sector size(N): "+Sectorsz+
						"\n FDCSR1: "+FDCsr1+
						"\n FDCSR2: "+FDCsr2+
						"\n Actual size: "+ActualSize;
		return(result);
	}

}

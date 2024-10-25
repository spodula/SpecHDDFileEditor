package hddEditor.libs.disks;

public class ExtendedSpeccyBasicDetails extends SpeccyBasicDetails {
	public int filelength;
	public String filename;
	
	public ExtendedSpeccyBasicDetails(int type, int varstart, int linestart, int loadaddress, char varname, String filename, int filelength) {
		super(type, varstart, linestart, loadaddress, varname);
		this.filename = filename;
		this.filelength = filelength;
	}
	
	/**
	 * return details as a string.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result +"\nFilename: "+filename+
				"\nFile length: "+filelength;
		
		return(result);
	}

	@Override
	public String GetSpecificDetails() {
		String result = super.GetSpecificDetails();
		if (BasicType==3) {
			result = result + ", len: "+filelength;
		}
		return(result);
	}
	

}

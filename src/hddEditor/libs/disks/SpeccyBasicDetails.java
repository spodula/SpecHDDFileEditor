package hddEditor.libs.disks;

import hddEditor.libs.Speccy;

public class SpeccyBasicDetails {
	// FIle type.
	public int BasicType = -1;
	// BASIC type
	public int VarStart = 0;
	public int LineStart = 0;
	// CODE type
	public int LoadAddress = 0;
	// ARRAY type
	public char VarName = 'A';

	public SpeccyBasicDetails(int type, int varstart, int linestart, int loadaddress, char varname) {
		BasicType = type;
		VarStart = varstart;
		LineStart = linestart;
		LoadAddress = loadaddress;
		VarName = varname;
	}
	
	public String BasicTypeString() {
		return(Speccy.SpecFileTypeToString(BasicType));
	}
	
}

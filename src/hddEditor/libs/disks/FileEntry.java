package hddEditor.libs.disks;

import java.io.IOException;

public interface FileEntry {
	//Get the current filename
	public String GetFilename();
	
	//Set the filename
	public void SetFilename(String filename) throws IOException;
	
	//Does this File entry match the given wildcard?
	public boolean DoesMatch(String wildcard); 

	//File size
	public int GetRawFileSize();
	
	//File size of the file as seen by BASIC.
	public int GetFileSize();
	
	//Get File type
	public String GetFileTypeString();

	//Get Speccy Basic details
	public SpeccyBasicDetails GetSpeccyBasicDetails();
	
	public byte[] GetFileData() throws IOException;
	
	public byte[] GetFileRawData() throws IOException;
	
}

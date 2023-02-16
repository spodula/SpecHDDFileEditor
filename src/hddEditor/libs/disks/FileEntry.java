package hddEditor.libs.disks;

import java.io.IOException;

public interface FileEntry {
	//Get the current filename
	public String GetFilename();
	
	//Set the filename
	public void SetFilename(String filename) throws IOException;
	
	//Does this File entry match the given wildcard?
	public boolean DoesMatch(String wildcard); 

}

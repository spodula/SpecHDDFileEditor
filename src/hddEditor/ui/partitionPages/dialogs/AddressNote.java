package hddEditor.ui.partitionPages.dialogs;
/**
 * Object containing a note associated with a given address range. Used to provide more
 * information when searching.  
 * 
 * For files, one AddressNote is provided per 8192 byte block.
 * StartAddress is the address with Partition. 
 * NodeDisp is the address within the file.
 * 
 * @author graham
 *
 */

public class AddressNote {
	//Start address of range within the partition
	public int StartAddress = 0;
	//End of address of range
	public int EndAddress = 0;
	//Block pointer within the given file. 
	public int NoteDisp=0;
	//Textual note. 
	public String note;
	
	/**
	 * Create an address note
	 * @param start
	 * @param end
	 * @param Displacement
	 * @param Note
	 */
	public AddressNote(int start, int end, int Displacement, String Note ) {
		StartAddress = start;
		EndAddress = end;
		note = Note;
		NoteDisp = Displacement;
	}
	
	/**
	 * Check if we are in range.
	 * @param Address
	 * @return
	 */
	public boolean DoesNoteApply(int Address) {
		boolean result = false;
		if ((Address >= StartAddress) && (Address <=EndAddress)) {
			result = true;
		}
		return(result);
	}
	
}

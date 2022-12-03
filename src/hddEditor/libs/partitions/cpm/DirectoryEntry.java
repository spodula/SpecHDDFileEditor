package hddEditor.libs.partitions.cpm;
/**
 * Implementation of a standard CPM 2.2 directory entry
 * This contains one or more Dirents. (Each Dirent records 8 or 16 block numbers) 
 * 
 * Note, this was originally written for another project and contains workarounds for attempts
 * at primitive floppy disk protection via messing up the block numbers and text. 
 * This shouldn't affect the IDEDOS, as most disks are from one source and copy protection
 * is not required, but its left in, in case i decide to add Floppy support later. 
 * 
 * 
 * Details of CPMs directory structure can be found here:
 * https://www.seasip.info/Cpm/format22.html
 * http://www.cpcwiki.eu/index.php/Disk_structure
 */

import java.io.IOException;

/**
 * Object wrapping a logical directory entry.
 *
 * This consists of one or more dirents (See dirent.java) 
 */

import java.util.ArrayList;

import hddEditor.libs.partitions.CPMPartition;


public class DirectoryEntry {
	// the raw dirents associated with this entry
	public Dirent[] dirents = null;

	// The disk its on.
	private CPMPartition ThisPartition = null;

	// is file deleted
	public boolean IsDeleted = false;
	
	//if TRUE, the directory entry is invalid.
	public boolean BadDirEntry = false;
	
	//used to validate Dirent. 
	private int maxBlocks=0;
	
	//Any errors parsing the Directory entry.
	public String Errors="";


	/**
	 * Parse and return the filename from the first DIRENT.
	 * 
	 * @return
	 */
	public String filename() {
		if (dirents != null) {
			return (dirents[0].GetFilename());
		} else {
			return ("");
		}
	}

	/**
	 * Create the directory entry.
	 * 
	 * @param filename
	 * @param disk
	 */
	public DirectoryEntry(CPMPartition disk, boolean IsDeleted, int maxBlocks) {
		this.ThisPartition = disk;
		this.IsDeleted = IsDeleted;
		dirents = new Dirent[0];
		this.maxBlocks = maxBlocks;
	}

	/**
	 * Add a DIRENT to the file.
	 * 
	 * @param d
	 */
	public void addDirent(Dirent d) {
		// Duplicate the dirent list and add the new one.
		Dirent[] newdirent = new Dirent[dirents.length + 1];
		for (int i = 0; i < dirents.length; i++) {
			newdirent[i] = dirents[i];
		}
		newdirent[dirents.length] = d;
		dirents = newdirent;
		//force a recalculation of if the file is valid. 
		getBlocks();
	}

	/**
	 * Get the dirent by number. Note this is required because i can't be certain
	 * DIRENTS are actually in order. (They may be, but i can't find any
	 * documentation saying so either way)
	 * 
	 * @param num
	 * @return
	 */
	public Dirent getExtentByNum(int num) {
		Dirent result = null;
		for (Dirent d : dirents) {
			if (d.GetLogicalExtentNum() == num) {
				result = d;
			}
		}
		return (result);
	}

	/**
	 * Get the list of blocks in the file. Note we are doing it using a sub
	 * function to get the numbered extent, rather than just iterating because
	 * although all the disks i have looked at so far put the dirents for a given
	 * file consecutively, i can't find anything in any documentation that actually
	 * says this.
	 * 
	 * Note, extents don't necessarily seem to start from 0 on +3e. unlike normal CPM.
	 *   As such, i have removed bad extent checking.
	 * 
	 * 
	 * @return
	 */
	public int[] getBlocks() {
		BadDirEntry = false;
		
		ArrayList<Integer> al = new ArrayList<Integer>();
		String badExtents="";
		
		String badBlocks="";
		
		for (int i = 0; i < dirents.length; i++) {
			Dirent d = dirents[i];
			if (d == null) {
				badExtents = badExtents+", "+i;
			} else {
				int blocks[] = d.getBlocks();
				for (int block : blocks) {
					if (block != 0) {
						if (block >= maxBlocks) {
							badBlocks = badBlocks+", "+block;
						} 
						al.add(block);
					}
				}
			}
		}
		
		if (!badExtents.isEmpty()) {
			Errors = "File is invalid due to missing (Or re-used) directory extents: #"+badExtents.substring(2);
			BadDirEntry = true;
		}
		if (!badBlocks.isEmpty()) {
			if (!Errors.isBlank()) {
				Errors= Errors+"\r\n";
			}
			Errors= Errors + "File is invalid due to referencing invalid blocks: #"+badBlocks.substring(2);
			BadDirEntry = true;
		}

		// convert the arraylist into a int[] to return
		int[] result = new int[al.size()];
		for (int i = 0; i < al.size(); i++) {
			result[i] = al.get(i);
		}
		return (result);

	}

	/**
	 * Get the number of bytes (Note, a multiple of 128 bytes) file size on disk.
	 * 
	 * @return
	 */
	public int GetFileSize() {
		//On a normal +3 disk, all dirents except the last one will be full.
		//This doesn't seem to be the case for PLUSIDEPOS, so just get the number of blocks, and
		//use that.
		//GDS 25/12/2022 - The bytes in the last logical Dirent are the bytes for the last DIRENT not Block. 
		//                  So ignore the last Dirent in file size calculations.
		int BytesInRestOfDirents = 0; 
		Dirent lastdirent = GetLastDirent();
		for(Dirent d:dirents) {
			if (d != lastdirent) {
				BytesInRestOfDirents = BytesInRestOfDirents + (d.getBlocks().length * ThisPartition.BlockSize);
			}
		}	
		
		// Get the number of records used in the last dirent.
		int bytesinlld = 0;
		if (lastdirent == null) {
			System.out.println("Cant get last dirent for " + filename());
		} else {
			bytesinlld = lastdirent.GetBytesInLastDirent();
		}
		return (bytesinlld + BytesInRestOfDirents);

	}
	
	/**
	 * Get the last dirent of a file. 
	 * This dirent will be the one that is not full
	 * 
	 * @return
	 */
	private Dirent GetLastDirent() {
		Dirent result = dirents[0];
		int MaxExtentNum = result.GetLogicalExtentNum();
		for (int j=1;j<dirents.length;j++) {
			if (dirents[j].GetLogicalExtentNum() > MaxExtentNum) {
				result = dirents[j];
				MaxExtentNum = result.GetLogicalExtentNum();
			}
		}
		
		return(result);
	}

	/**
	 * Get the file content. Note this includes the +3DOS header.
	 * 
	 * @return
	 * @throws IOException 
	 * @throws BadDiskFileException
	 */
	public byte[] GetFileData() throws IOException {
		byte result[] = new byte[GetFileSize()];

		// get all the blocks
		int[] blocks = getBlocks();

		// find the last valid byte
		int eob = GetFileSize();

		// iterate each block
		int resultptr = 0;
		for (int i = 0; i < blocks.length; i++) {
			byte currentblock[] = ThisPartition.GetLogicalBlock(blocks[i]);
			// copy the contents until we get to end last record in the block. (The rest of
			// the data is invalid)
			for (byte x : currentblock) {
				if (resultptr < eob) {
					result[resultptr++] = x;
				}
			} 
		}

		return (result);
	}

	/**
	 * parse the first block into a +3Dos header structure and return it.
	 * 
	 * @return
	 * @throws  
	 */
	public Plus3DosFileHeader GetPlus3DosHeader()  {

		// Load the first block of the file
		Plus3DosFileHeader pdh = null;
		int[] blocks = getBlocks();
		// this fix an issue with zero length CPM files.
		// we will just return an invalid +3 data structure.
		// Eg, the alcatraz development disks, "New word" side A
		if (blocks.length ==0) {
			pdh = new Plus3DosFileHeader(new byte[256]);
		} else {
			byte Block0[] = null;
				if (blocks[0] >= ThisPartition.MaxBlock) {
					//added for Double Dragon which has a directory entry 
					//with bad Block numbers, this prevents most of the files
					//appearing in the directory listing. 
					System.out.println("Block "+blocks[0]+" does not exist for entry: '"+filename()+"'");
					pdh = new Plus3DosFileHeader(new byte[256]);
				} else {
					try {
						Block0 = ThisPartition.GetLogicalBlock(blocks[0]);
						pdh = new Plus3DosFileHeader(Block0);
					} catch (IOException e) {
					}
					
				}
		}
		return (pdh);
	} 

	/**
	 * Check to see if the current directory entry is a complete file. Only applies
	 * to deleted files, Other files are assumed to be complete if the entries are complete.
	 * 
	 * @return
	 */
	public Boolean IsComplete() {
		if (!IsDeleted) {
			return (!BadDirEntry);
		} else {
			// Check to see if any of the blocks are marked as in-use by the BAM.
			boolean result = true;
			int blocks[] = getBlocks();
			for (int i : blocks) {
				if (i< ThisPartition.bam.length ) {
					if (ThisPartition.bam[i])
						result = false;
				} else {
					result = false;
				}
			}
			return (result);
		} 
	}

	/**
	 * Set the file to be deleted or not deleted.
	 * 
	 * @param deleted
	 * @throws IOException 
	 */
	public void SetDeleted(boolean deleted) throws IOException {
		// first, set all the dirents.
		for (Dirent d : dirents) {
			if (deleted) {
				d.setType(Dirent.DIRENT_DELETED);
			} else {
				d.setType(Dirent.DIRENT_FILE);
			}
		}
		// update the deleted flag
		IsDeleted = deleted;
		// fix sectors
		ThisPartition.updateDirentBlocks();
		// update the BAM.
		int blocks[] = getBlocks();
		for (int i = 0; i < blocks.length; i++) {
			int blocknum = blocks[i];
			if (deleted) {
				ThisPartition.bam[blocknum] = false;
			} else {
				if (ThisPartition.bam[blocknum]) {
					System.out.println("Warning! Block " + blocknum + " Already in use!");
				} else {
					ThisPartition.bam[blocknum] = true;
				}
			}
		} 
		ThisPartition.setModified(true); 
	}

	/**
	 * set the write protect / system / archive flag
	 * 
	 * @param value - new flag value
	 * @param flag  - which flag (R/S/A)
	 * @throws IOException 
	 */ 
	public void SetFlag(boolean value, char flag) throws IOException {
		int bytenum = "RSArsa".indexOf(flag);
		if (bytenum > 2)
			bytenum = bytenum - 3;
		if (bytenum == -1)
			System.out.println("Unknown attribute: " + flag);
		else {
			// set the dirents
			bytenum = bytenum + 9;
			for (Dirent d : dirents) {
				if (value) {
					d.rawdirent[bytenum] = (byte) ((int) (d.rawdirent[bytenum] | 0x80) & 0xff);
				} else {
					d.rawdirent[bytenum] = (byte) ((int) (d.rawdirent[bytenum] & 0x7f));
				}
			}
		}
		// fix sectors
		ThisPartition.updateDirentBlocks();
		ThisPartition.setModified(true);
	}



	/**
	 * Rename the current file
	 * 
	 * @param newFilename
	 * @throws IOException 
	 */
	public void RenameTo(String newFilename) throws IOException {
		// first, set all the dirents.
		for (Dirent d : dirents) {
			d.SetFilename(newFilename);
		}
		// update the sectors.
		ThisPartition.updateDirentBlocks();
	}
	
	/**
	 * ToString overridden to provide information for the directory entry
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		String result="";
		
		result = result + "Dirents: \n";
		for(Dirent d:dirents) {
			result = result + d+"\n  ";
			for(byte b: d.rawdirent) {
				result = result + " "+ String.format("  %02X",(int) (b & 0xff));
			}
			result = result + "\n";
		}
		
		return(result);
	}
	
}

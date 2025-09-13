package hddEditor.libs.disks.LINEAR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;

import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.ExtendedSpeccyBasicDetails;
import hddEditor.libs.disks.FDD.BadDiskFileException;
import hddEditor.libs.disks.LINEAR.tapblocks.TAPBlock;

public class TAPFile implements Disk {
	protected RandomAccessFile inFile;
	// filename of the currently open file
	public File file;
	// Storage for the Tape blocks
	public TAPBlock Blocks[];

	public long LastModified;

	/**
	 * Constructor, load a TAP file.
	 * 
	 * @param filename
	 * @throws IOException
	 * @throws BadDiskFileException
	 */
	public TAPFile(File file) throws IOException, BadDiskFileException {
		if (!file.exists()) {
			throw new BadDiskFileException("File " + file.getAbsolutePath() + " does not exist.");
		}
		inFile = new RandomAccessFile(file, "rw");
		this.file = file;
		ParseTAPFile();
	}

	/**
	 * Create a non-loaded tap file object
	 */
	public TAPFile() {
		inFile = null;
		this.file = null;
	}

	/**
	 * Get media tape. For TAPE files, it is a Linear device
	 */
	@Override
	public int GetMediaType() {
		return PLUSIDEDOS.MEDIATYPE_LINEAR;
	}

	/**
	 * Get the filename
	 */
	@Override
	public String GetFilename() {
		return (file.getAbsolutePath());
	}

	/**
	 * Set the current filename
	 */
	@Override
	public void SetFilename(String filename) {
		this.file = new File(filename);
	}

	/**
	 * Return a dummy value as the blocks can be any size for TAP files.
	 */
	@Override
	public int GetSectorSize() {
		return 1;
	}

	@Override
	/**
	 * Set the sector size. This has no effect.
	 */
	public void SetSectorSize(int sz) {
		System.out.println("Attempt to set sector size for a TAP file.");
	}

	@Override
	/**
	 * Get the number of cylinders. Returns a dummy value.
	 */
	public int GetNumCylinders() {
		// Dummy value
		return 1;
	}

	/**
	 * Set the number of cylinders. This has no effect.
	 */
	@Override
	public void SetNumCylinders(int sz) {
		System.out.println("Attempt to set Number of Cylinders for a TAP file.");
	}

	@Override
	/**
	 * Get the file size.
	 */
	public long GetFileSize() {
		return (file.length());
	}

	@Override
	/**
	 * Get the number of heads. Returns a dummy value.
	 */
	public int GetNumHeads() {
		return 1;
	}

	@Override
	/**
	 * Set the number of heads. has no effect.
	 */
	public void SetNumHeads(int sz) {
		System.out.println("Attempt to set Number of Heads for a TAP file.");
	}

	@Override
	/**
	 * Get the number of sectors, just return the block number.
	 */
	public int GetNumSectors() {
		return Blocks.length;
	}

	@Override
	/**
	 * Set the number of sectors. has no effect.
	 */
	public void SetNumSectors(int sz) {
		System.out.println("Attempt to set Number of Sectors for a TAP file.");
	}

	@Override
	/**
	 * Dummyvalue
	 */
	public long GetNumLogicalSectors() {
		// dummy
		return 255;
	}

	@Override
	/**
	 * Close the current file.
	 */
	public void close() {
		if (inFile != null) {
			try {
				inFile.close();
			} catch (IOException e) {
				System.out.println("Failed to close file " + file.getName() + " with error " + e.getMessage());
				e.printStackTrace();
			}
			inFile = null;
		}
	}

	@Override
	/**
	 * Check if a file is open.
	 */
	public boolean IsOpen() {
		return (inFile != null);
	}

	@Override
	/**
	 * This is not going to work for tapes, so just ignore for now.
	 */
	public void SetLogicalBlockFromSector(long SectorNum, byte[] result) throws IOException {
		System.out.println("SetLogicalBlock not supported for TAP files. Add or delete files instead.");
		throw new IOException("SetLogicalBlock not supported for TAP files. Add or delete files instead.");
	}

	@Override
	/**
	 * This will sort of work, but probably not a good way to access the data.
	 */
	public byte[] GetBytesStartingFromSector(long SectorNum, long sz) throws IOException {
		byte targetdata[] = new byte[(int) sz];
		int byteptr = 0;
		int NumBytesLeft = (int) sz;
		for (TAPBlock block : Blocks) {
			if (block.blocknum >= SectorNum) {
				System.arraycopy(block.data, 0, targetdata, byteptr, Math.min(NumBytesLeft, block.data.length));
				NumBytesLeft = NumBytesLeft - Math.min(NumBytesLeft, block.data.length);
				if (NumBytesLeft == 0) {
					break;
				}
			}
		}
		return null;
	}

	@Override
	/**
	 * Check to see if the file loads.
	 */
	public Boolean IsMyFileType(File filename) throws IOException {
		if (filename.getName().toUpperCase().endsWith("TAP")) {
			try {
				TAPFile mdt = new TAPFile(filename);
				mdt.close();
				return true;
			} catch (Exception E) {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Parse the TAP file into blocks.
	 * 
	 * @throws IOException
	 */
	public void ParseTAPFile() throws IOException {

		byte FileData[] = new byte[(int) GetFileSize()];
		inFile.seek(0);
		inFile.read(FileData);

		int ptr = 0;
		int blocknum = 0;

		ArrayList<TAPBlock> TapBlocks = new ArrayList<TAPBlock>();

		try {
			while (ptr < FileData.length - 1) {
				TAPBlock tb = new TAPBlock(FileData, ptr, blocknum++);
				TapBlocks.add(tb);
				ptr = ptr + tb.rawblocklength;
			}
		} catch (Exception E) {
			byte bd[] = new byte[19];
			bd[0] = 0x13;
			bd[1] = 0x00;
			bd[2] = 0x00;
			bd[3] = 0x00;
			bd[4] = 'B';
			bd[5] = 'A';
			bd[6] = 'D';
			bd[7] = ' ';
			bd[8] = ' ';
			bd[9] = 'D';
			bd[10] = 'A';
			bd[11] = 'T';
			bd[12] = 'A';

			TAPBlock tb = new TAPBlock(bd, 0, blocknum++);
			TapBlocks.add(tb);

		}
		Blocks = TapBlocks.toArray(new TAPBlock[0]);
		UpdateLastModified();
	}

	/**
	 * Return all the data as a block.
	 * 
	 * @return
	 * @throws IOException
	 */
	public byte[] GetAllData() throws IOException {
		byte FileData[] = new byte[(int) GetFileSize()];
		inFile.seek(0);
		inFile.read(FileData);
		return (FileData);
	}

	/**
	 * Set the tape file, Probably dangerous.
	 * 
	 * @param FileData
	 * @return
	 * @throws IOException
	 */
	public byte[] SetAllData(byte FileData[]) throws IOException {
		inFile.seek(0);
		inFile.read(FileData);
		UpdateLastModified();
		return (FileData);
	}

	@Override
	/**
	 * Overridden TOString for debugging purposes.
	 */
	public String toString() {
		String result = "Filename: " + file.getName() + "\n";
		for (TAPBlock t : Blocks) {
			result = result + "#" + t.blocknum + ": " + t + "\n";
		}
		return (result);
	}

	/**
	 * Update the file from the current block list.
	 * 
	 * @throws IOException
	 */
	public void RewriteFile() throws IOException {
		inFile.seek(0);
		int filelen = 0;
		for (TAPBlock Block : Blocks) {
			Block.fileLocation = filelen;
			byte data[] = Block.GetRawBlock();
			inFile.write(data);
			filelen = filelen + data.length;
		}
		inFile.setLength(filelen);
		UpdateLastModified();
	}

	/**
	 * Delete the given block.
	 * 
	 * @param block
	 * @throws IOException
	 */
	public void DeleteBlock(TAPBlock block) throws IOException {
		if (block != null) {
			TAPBlock newBlocks[] = new TAPBlock[Blocks.length - 1];
			int ptr = 0;
			for (TAPBlock blk : Blocks) {
				if (blk.blocknum != block.blocknum) {
					newBlocks[ptr++] = blk;
				}
			}
			Blocks = newBlocks;
			RewriteFile();
		}
		ParseTAPFile();
	}

	/**
	 * Add the given data as a block.
	 * 
	 * @param block
	 * @param flag
	 * @throws IOException
	 */
	public void AddBlock(byte[] block, int flag) throws IOException {
		TAPBlock newblock = new TAPBlock(Blocks.length, block, flag);
		TAPBlock newBlocks[] = new TAPBlock[Blocks.length + 1];
		System.arraycopy(Blocks, 0, newBlocks, 0, Blocks.length);
		newBlocks[Blocks.length] = newblock;
		Blocks = newBlocks;
		RewriteFile();
		ParseTAPFile();
	}

	/**
	 * Create a blank TAP file. Pretty simple
	 * 
	 * @param Filename
	 * @throws IOException
	 */
	public void CreateEmptyTapeFile(File file) throws IOException {
		FileOutputStream NewFile = new FileOutputStream(file);
		// Close, forcing flush
		NewFile.close();
		NewFile = null;
		// Load the newly created file.
		this.file = file;
		inFile = new RandomAccessFile(file, "rw");
		ParseTAPFile();
	}

	/**
	 * Return the index of the given block in the block list.
	 * 
	 * @param block
	 * @return
	 */
	private int GetBlockIndex(TAPBlock block) {
		int idx = 0;
		for (TAPBlock b : Blocks) {
			if (b.equals(block)) {
				return (idx);
			}
			idx++;
		}
		return (-1);
	}

	/**
	 * move the given block up the list. The DoRewrite flag is used to indicate the
	 * file should be re-written. This is because for blocks that need to be kept
	 * together, EG, header/data pairs. if you rewrite after the header, the data
	 * block becomes invalid and doesn't get moved.
	 * 
	 * @param block
	 * @param DoRewrite
	 * @throws IOException
	 */
	public void MoveBlockUp(TAPBlock block, boolean DoRewrite) throws IOException {
		int idx = GetBlockIndex(block);
		if (idx > 0) { // ignore -1 (not found) and 0 (top)
			TAPBlock block1 = Blocks[idx];
			TAPBlock block2 = Blocks[idx - 1];

			Blocks[idx - 1] = block1;
			Blocks[idx] = block2;

			if (DoRewrite) {
				RewriteFile();
				ParseTAPFile();
			}
		}
	}

	/**
	 * Move the given block down the list.
	 * 
	 * @param block
	 * @param DoRewrite
	 * @throws IOException
	 */
	public void MoveBlockDown(TAPBlock block, boolean DoRewrite) throws IOException {
		int idx = GetBlockIndex(block);
		if ((idx > -1) && (idx < (Blocks.length - 1))) { // ignore -1 (not found) and blocknum-1 (bottom)
			TAPBlock block1 = Blocks[idx];
			TAPBlock block2 = Blocks[idx + 1];

			Blocks[idx + 1] = block1;
			Blocks[idx] = block2;

			if (DoRewrite) {
				RewriteFile();
				ParseTAPFile();
			}
		}
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Expecting paramaters [Directory] [target file]");
		} else {
			int err = 0;
			int proc = 0;

			PrintWriter pr;
			pr = new PrintWriter(new FileWriter(args[1]));
			try {
				File folder = new File(args[0]);
				File contents[] = folder.listFiles();

				Arrays.sort(contents);

				for (File f : contents) {
					if (f.getName().toLowerCase().endsWith(".tap")) {
						proc++;
						pr.println("=============================================");
						pr.println(f.getName());
						pr.println("=============================================");
						System.out.println(f.getName());
						try {
							TAPFile mdt = new TAPFile(f);
							pr.println(mdt);
							pr.println("----------------------------------------------");
							if (mdt.Blocks == null) {
								pr.println("No blocks found. parsing error?");
							} else {
								for (TAPBlock block : mdt.Blocks) {
									if (block.DecodeHeader() != null) {
										pr.print("Blocks: " + block.blocknum + "\\" + (block.blocknum + 1) + " ");

										ExtendedSpeccyBasicDetails ebd = block.DecodeHeader();
										String s = "  '" + ebd.filename + "' " + ebd.BasicTypeString() + " ("
												+ ebd.BasicType + ") Len:" + ebd.filelength;

										switch (ebd.BasicType) {
										case Speccy.BASIC_BASIC:
											s = s + " Line: " + ebd.LineStart;
											s = s + " Vars: " + ebd.VarStart;
											break;
										case Speccy.BASIC_NUMARRAY:
											s = s + " Var: " + ebd.VarName;
											break;
										case Speccy.BASIC_CHRARRAY:
											s = s + " Var: " + ebd.VarName + "$";
											break;
										case Speccy.BASIC_CODE:
											s = s + " Loadaddr: " + ebd.LoadAddress;
											break;
										default:
											s = s + "UNKNOWN/INVALID TYPE ID";
										}
										pr.println(s.trim());
									}
								}
							}

							mdt.close();
							mdt = null;
						} catch (IOException | BadDiskFileException | NullPointerException
								| ArrayIndexOutOfBoundsException e) {
							err++;
							pr.println(e.getMessage());
							e.printStackTrace();
						}
					}
				}
			} finally {
				pr.close();
			}
			System.out.println("Processed " + proc + " files with " + err + " Errors.");
		}
	}

	/**
	 * Return if the disk is out of sync with the one on disk.
	 */
	@Override
	public boolean DiskOutOfDate() {
		if (inFile != null) {
			return (LastModified < file.lastModified());
		}
		return false;
	}

	/**
	 * Update the last modified flag.
	 */
	@Override
	public void UpdateLastModified() {
		if (inFile != null) {
			LastModified = file.lastModified();
		}
	}
}

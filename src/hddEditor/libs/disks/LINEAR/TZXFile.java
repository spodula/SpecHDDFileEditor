package hddEditor.libs.disks.LINEAR;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.TZX;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FDD.BadDiskFileException;
import hddEditor.libs.disks.LINEAR.tzxblocks.ArchiveInfoBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.CallSequenceBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.DirectRecordingBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.FourtyEightkStopBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.GlueBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.GroupEndBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.GroupStartBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.HardwareInfoBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.JumpToBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.LoopEndBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.LoopStartBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.MessageBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.PauseStopTape;
import hddEditor.libs.disks.LINEAR.tzxblocks.PulseSequence;
import hddEditor.libs.disks.LINEAR.tzxblocks.PureDataBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.PureToneBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.ReturnFromSequenceBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.SelectBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.SetSignalLevelBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.StandardDataBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.TZXBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.TextDescriptionBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.TurboSpeedDataBlock;

public class TZXFile implements Disk {
	// File opened.
	protected RandomAccessFile inFile;
	// filename of the currently open file
	public File file;

	// TZX file version
	public int MajorVersion = 0;
	public int MinorVersion = 0;

	// TZX file blocks
	public TZXBlock Blocks[] = null;

	public void ParseTZXFile() throws IOException {
		inFile.seek(0);
		byte buffer[] = new byte[8];
		inFile.read(buffer);
		// String FileID = new String(buffer);
		buffer = new byte[2];
		inFile.read(buffer);
		MajorVersion = (int) buffer[0] & 0xff;
		MinorVersion = (int) buffer[1] & 0xff;

		buffer = new byte[1];
		int BlockNum = 0;
		Vector<TZXBlock> results = new Vector<TZXBlock>();
		while (inFile.read(buffer) > 0) {
			TZXBlock r = DecodeDatablock((int) buffer[0] & 0xff, inFile, BlockNum);
			results.add(r);
			BlockNum++;
		}
		Blocks = results.toArray(new TZXBlock[results.size()]);
	}

	/**
	 * Constructor, load a TAP file.
	 * 
	 * @param filename
	 * @throws IOException
	 * @throws BadDiskFileException
	 */
	public TZXFile(File file) throws IOException, BadDiskFileException {
		if (!file.exists()) {
			throw new BadDiskFileException("File " + file.getAbsolutePath() + " does not exist.");
		}
		inFile = new RandomAccessFile(file, "rw");
		this.file = file;
		ParseTZXFile();
	}

	/**
	 * 
	 * @throws IOException
	 * @throws BadDiskFileException
	 */
	public TZXFile() throws IOException, BadDiskFileException {
		this.file = null;
	}

	/**
	 * 
	 * @param i
	 * @param fs
	 * @return
	 * @throws IOException
	 */
	private TZXBlock DecodeDatablock(int BlockID, RandomAccessFile fs, int BlockNum) throws IOException {
		TZXBlock block = null;
		switch (BlockID) {
		case 0x10:
			block = new StandardDataBlock(fs);
			break;
		case 0x11:
			block = new TurboSpeedDataBlock(fs);
			break;
		case 0x12:
			block = new PureToneBlock(fs);
			break;
		case 0x13:
			block = new PulseSequence(fs);
			break;
		case 0x14:
			block = new PureDataBlock(fs);
			break;
		case 0x15:
			block = new DirectRecordingBlock(fs);
			break;
		case 0x20:
			block = new PauseStopTape(fs);
			break;
		case 0x21:
			block = new GroupStartBlock(fs);
			break;
		case 0x22:
			block = new GroupEndBlock(fs);
			break;
		case 0x23:
			block = new JumpToBlock(fs);
			break;
		case 0x24:
			block = new LoopStartBlock(fs);
			break;
		case 0x25:
			block = new LoopEndBlock(fs);
			break;
		case 0x26:
			block = new CallSequenceBlock(fs);
			break;
		case 0x27:
			block = new ReturnFromSequenceBlock(fs);
			break;
		case 0x28:
			block = new SelectBlock(fs);
			break;
		case 0x2A:
			block = new FourtyEightkStopBlock(fs);
			break;
		case 0x2B:
			block = new SetSignalLevelBlock(fs);
			break;
		case 0x30:
			block = new TextDescriptionBlock(fs);
			break;
		case 0x31:
			block = new MessageBlock(fs);
			break;
		case 0x32:
			block = new ArchiveInfoBlock(fs);
			break;
		case 0x33:
			block = new HardwareInfoBlock(fs);
			break;
		case 0x5A:
			block = new GlueBlock(fs);
			break;
		default:
			System.err.println("UNSUPPORTED DATA BLOCK TYPE " + Integer.toHexString(BlockID));
			break;
		}
		if (block != null) {
			block.BlockNumber = BlockNum;
		}
		return (block);

	}

	@Override
	public int GetMediaType() {
		return PLUSIDEDOS.MEDIATYPE_LINEAR;
	}

	@Override
	public String GetFilename() {
		return (file.getAbsolutePath());
	}

	@Override
	public void SetFilename(String filename) {
		this.file = new File(filename);
	}

	@Override
	public int GetSectorSize() {
		return 1;
	}

	@Override
	public void SetSectorSize(int sz) {
		System.out.println("Attempt to set sector size for a TZX file.");
	}

	@Override
	public int GetNumCylinders() {
		// Dummy value
		return 1;
	}

	@Override
	public void SetNumCylinders(int sz) {
		System.out.println("Attempt to set Number of Cylinders for a TAP file.");
	}

	@Override
	public long GetFileSize() {
		return (file.length());
	}

	@Override
	public int GetNumHeads() {
		return 1;
	}

	@Override
	public void SetNumHeads(int sz) {
		System.out.println("Attempt to set Number of Heads for a TZX file.");
	}

	@Override
	public int GetNumSectors() {
		return Blocks.length;
	}

	@Override
	public void SetNumSectors(int sz) {
		System.out.println("Attempt to set Number of Sectors for a TZX file.");
	}

	@Override
	public long GetNumLogicalSectors() {
		// dummy
		return 255;
	}

	@Override
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
	public boolean IsOpen() {
		return (inFile != null);
	}

	@Override
	public void SetLogicalBlockFromSector(long SectorNum, byte[] result) throws IOException {
		System.out.println("SetLogicalBlock not supported for TZX files. Add or delete files instead.");
		throw new IOException("SetLogicalBlock not supported for TZX files. Add or delete files instead.");
	}

	@Override
	public byte[] GetBytesStartingFromSector(long SectorNum, long sz) throws IOException {
		byte targetdata[] = new byte[(int) sz];
		int byteptr = 0;
		int NumBytesLeft = (int) sz;
		for (TZXBlock block : Blocks) {
			if (block.BlockNumber >= SectorNum) {
				System.arraycopy(block.data, 0, targetdata, byteptr, Math.min(NumBytesLeft, block.data.length));
				NumBytesLeft = NumBytesLeft - Math.min(NumBytesLeft, block.data.length);
				if (NumBytesLeft == 0) {
					break;
				}
			}
		}
		return null;
	}

	/**
	 * Check to see if we have a valid TZX file header.
	 */
	@Override
	public Boolean IsMyFileType(File filename) throws IOException {
		try {
			FileInputStream fs = new FileInputStream(filename);
			try {
				byte buffer[] = new byte[8];
				fs.read(buffer);
				String id = new String(buffer);
				return (id.equals("ZXTape!" + (char) 0x1A));
			} finally {
				fs.close();
			}
		} catch (FileNotFoundException e) {
			return (false);
		} catch (IOException e) {
			return (false);
		}
	}

	/**
	 * Update the file from the current block list.
	 * 
	 * @throws IOException
	 */
	public void RewriteFile() throws IOException {
		inFile.seek(0x0A); // skip file header
		int filelen = 0x0a;
		for (TZXBlock Block : Blocks) {
			byte data[] = Block.rawdata;
			inFile.write(data);
			filelen = filelen + data.length;
		}
		inFile.setLength(filelen);
	}

	/**
	 * Delete the given block.
	 * 
	 * @param block
	 * @throws IOException
	 */
	public void DeleteBlock(TZXBlock block) throws IOException {
		if (block != null) {
			TZXBlock newBlocks[] = new TZXBlock[Blocks.length - 1];
			int ptr = 0;
			for (TZXBlock blk : Blocks) {
				if (blk.BlockNumber != block.BlockNumber) {
					newBlocks[ptr++] = blk;
				}
			}
			Blocks = newBlocks;
			RewriteFile();
		}
		ParseTZXFile();
	}

	/**
	 * rewrite the file.
	 * 
	 * @param FileData
	 * @return
	 * @throws IOException
	 */
	public byte[] SetAllData(byte[] FileData) throws IOException {
		inFile.seek(0);
		inFile.read(FileData);
		return (FileData);
	}

	/**
	 * Get the raw file.
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
	 * Add a standard speed rom-saved block to the list
	 * 
	 * @param codeFile - Data
	 * @param blockID  - Rom Loader Header ID
	 * @throws IOException
	 */
	public void AddStandardBlock(byte[] codeFile, int RomLoaderID, boolean RewriteFile) throws IOException {
		byte newdata[] = new byte[codeFile.length + 2];
		// add in the rom headerID
		newdata[0] = (byte) (RomLoaderID & 0xff);
		// copy the data
		System.arraycopy(codeFile, 0, newdata, 1, codeFile.length);

		// Calculate the rom loader checkum
		int checksum = TZX.CalculateChecksumForBlock(newdata);
		newdata[newdata.length - 1] = (byte) (checksum & 0xff);
		System.out.println("Calculated cs of " + (newdata[newdata.length - 1] & 0xff));

		// Add the new block to the list
		AddRawBlock(newdata, 0x10, RewriteFile);
	}

	/**
	 * Add a raw block to the block list.
	 * 
	 * @param codeFile
	 * @param blocktype
	 * @throws IOException
	 */
	public void AddRawBlock(byte[] codeFile, int TZXblocktype, boolean RewriteFile) throws IOException {
		TZXBlock newblock = null;
		switch (TZXblocktype) {
		case 0x10:
			newblock = new StandardDataBlock(codeFile, TZX.DEFAULT_STD_DELAY);
			break;
		default:
			System.out.println("Adding block of type " + TZXblocktype + " not supported");
		}
		if (newblock != null) {
			TZXBlock newBlocks[] = new TZXBlock[Blocks.length + 1];
			System.arraycopy(Blocks, 0, newBlocks, 0, Blocks.length);
			newBlocks[Blocks.length] = newblock;
			Blocks = newBlocks;
			if (RewriteFile) {
				RewriteFile();
				ParseTZXFile();
			}
		}

	}

	/**
	 * Return the index of the given block in the block list.
	 * 
	 * @param block
	 * @return
	 */
	private int GetBlockIndex(TZXBlock block) {
		int idx = 0;
		for (TZXBlock b : Blocks) {
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
	public void MoveBlockUp(TZXBlock block, boolean DoRewrite) throws IOException {
		int idx = GetBlockIndex(block);
		if (idx > 0) { // ignore -1 (not found) and 0 (top)
			TZXBlock block1 = Blocks[idx];
			TZXBlock block2 = Blocks[idx - 1];

			Blocks[idx - 1] = block1;
			Blocks[idx] = block2;

			if (DoRewrite) {
				RewriteFile();
				ParseTZXFile();
			}
		}
	}

	/**
	 * move the given block down the list. The DoRewrite flag is used to indicate
	 * the file should be re-written. This is because for blocks that need to be
	 * kept together, EG, header/data pairs. if you rewrite after the header, the
	 * data block becomes invalid and doesn't get moved.
	 * 
	 * @param block
	 * @param DoRewrite
	 * @throws IOException
	 */
	public void MoveBlockDown(TZXBlock block, boolean DoRewrite) throws IOException {
		int idx = GetBlockIndex(block);
		if ((idx > -1) && (idx < (Blocks.length - 1))) { // ignore -1 (not found) and blocknum-1 (bottom)
			TZXBlock block1 = Blocks[idx];
			TZXBlock block2 = Blocks[idx + 1];

			Blocks[idx + 1] = block1;
			Blocks[idx] = block2;

			if (DoRewrite) {
				RewriteFile();
				ParseTZXFile();
			}
		}
	}

	/**
	 * Create a blank TAP file. Pretty simple
	 * 
	 * @param Filename
	 * @throws IOException
	 */
	public void CreateEmptyTapeFile(File file) throws IOException {
		FileOutputStream NewFile = new FileOutputStream(file);

		byte header[] = new byte[] { 'Z', 'X', 'T', 'A', 'P', 'E', '!', 0x1A, 0x01, 0x01 };
		NewFile.write(header);

		String msg = "Created with SpecHDDFileEditor";
		byte info[] = new byte[2];
		info[0] = 0x30;
		info[1] = (byte) msg.getBytes().length;
		NewFile.write(info);
		NewFile.write(msg.getBytes());

		// Close, forcing flush
		NewFile.close();
		NewFile = null;

		// Load the newly created file.
		this.file = file;
		inFile = new RandomAccessFile(file, "rw");
		ParseTZXFile();
	}

	@Override
	/**
	 * Overridden TOString for debugging purposes.
	 */
	public String toString() {
		String result = "Filename: " + file.getName() + "\n";
		for (TZXBlock t : Blocks) {
			result = result + "#" + t.BlockNumber + ": " + t + "\n";
		}
		return (result);
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) {
		try {
			String filename = args[0];

			TZXFile mdt = new TZXFile(new File(filename));
			if (mdt.IsMyFileType(new File(filename))) {
				System.out.println("File is a valid TZX file.");
				System.out.println(mdt);
//				mdt.MoveBlockDown(mdt.Blocks[0],true);
//				System.out.println(mdt);
//				mdt.MoveBlockUp(mdt.Blocks[1],true);
//				System.out.println(mdt);

				// mdt.RewriteFile();

			} else {
				System.out.println("File is not a valid tzx file.");
			}

		} catch (IOException | BadDiskFileException e) {
			e.printStackTrace();
		}
	}


		/*		PrintWriter pr = new PrintWriter(new FileWriter("tzx.log"));
		try {

			File folder = new File("/media/CB4B-457D/Antique computers/Sinclair ZX Spectrum/Games/[TZX]");
			File contents[] = folder.listFiles();
			for (File f : contents) {
				if (f.getName().endsWith(".tzx")) {
					pr.println("=============================================");
					pr.println(f.getName());
					pr.println("=============================================");
					System.out.println(f.getName());
					try {
						TZXFile mdt = new TZXFile(f);
						pr.println(mdt);
						mdt.close();
						mdt = null;
					} catch (IOException | BadDiskFileException | NullPointerException
							| ArrayIndexOutOfBoundsException e) {
						pr.println(e.getMessage());
						e.printStackTrace();
					}
				}
			}
		} finally {
			pr.close();
		} */


}

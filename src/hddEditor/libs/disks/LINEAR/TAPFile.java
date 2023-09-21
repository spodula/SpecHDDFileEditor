package hddEditor.libs.disks.LINEAR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.ExtendedSpeccyBasicDetails;
import hddEditor.libs.disks.FDD.BadDiskFileException;

public class TAPFile implements Disk {
	protected RandomAccessFile inFile;
	// filename of the currently open file
	public String filename;
	// disk size in bytes
	public long FileSize;

	public TAPBlock Blocks[];

	public class TAPBlock {
		public byte data[];
		public int flagbyte;
		public int checksum;
		public int rawblocklength;
		public int blocknum;
		public boolean IsChecksumValid;
		public int fileLocation;

		public TAPBlock(byte block[], int start, int bNum) throws IOException {
			fileLocation = start;
			int length = (block[start] & 0xff) + ((block[start + 1] & 0xff) * 256);
			if (block.length < (start + length + 2)) {
				throw new IOException("TAP File is incomplete");
			}
			blocknum = bNum;
			rawblocklength = length + 2;
			data = new byte[length - 2];
			flagbyte = block[start + 2] & 0xff;
			checksum = block[start + length + 1] & 0xff;
			System.arraycopy(block, start + 3, data, 0, length - 2);
			UpdateChecksumValid();
		}

		public void UpdateBlockData(byte block[]) {
			data = block;
			rawblocklength = data.length + 4;
			UpdateChecksum();
		}

		public TAPBlock(int fileloc, byte block[], int flagbyte) throws IOException {
			data = block;
			this.flagbyte = flagbyte;
			UpdateChecksum();
			rawblocklength = data.length + 4;
		}

		public void UpdateChecksumValid() {
			int cs = flagbyte & 0xff;
			for (byte byt : data) {
				cs = cs ^ (byt & 0xff);
			}
			IsChecksumValid = (cs == checksum);
		}

		public void UpdateChecksum() {
			int cs = flagbyte & 0xff;
			for (byte byt : data) {
				cs = cs ^ (byt & 0xff);
			}
			IsChecksumValid = true;
			checksum = cs;
		}

		public byte[] GetRawBlock() {
			byte result[] = new byte[rawblocklength];
			int bl = rawblocklength - 2;
			result[0] = (byte) (bl & 0xff);
			result[1] = (byte) ((bl / 0x100) & 0xff);
			result[2] = (byte) (flagbyte & 0xff);
			result[rawblocklength - 1] = (byte) (checksum & 0xff);
			System.arraycopy(data, 0, result, 3, data.length);
			return (result);
		}

		@Override
		public String toString() {
			String result = "block length: " + data.length + " Flag:" + flagbyte + " checksum: " + checksum
					+ " CsValid?" + IsChecksumValid + " Location in file: " + fileLocation + " ";
			ExtendedSpeccyBasicDetails ebd = DecodeHeader();
			if (ebd != null) {
				String s = ebd.toString().replace("\n", ", ");
				result = result + "  (BASIC HEADER)" + s;
			}
			return (result);
		}

		public ExtendedSpeccyBasicDetails DecodeHeader() {
			ExtendedSpeccyBasicDetails result = null;
			if (data.length == 17 && flagbyte == 0x00) {
				int type = data[0] & 0xff;
				int filelen = (data[11] & 0xff) + ((data[12] & 0xff) * 0x100);
				int param1 = (data[13] & 0xff) + ((data[14] & 0xff) * 0x100);
				int param2 = (data[15] & 0xff) + ((data[16] & 0xff) * 0x100);
				byte filename[] = new byte[10];
				System.arraycopy(data, 1, filename, 0, 10);

				result = new ExtendedSpeccyBasicDetails(type, param2, param1, param1, 'A', new String(filename),
						filelen);
			}
			return (result);
		}
	}

	public TAPFile(String filename) throws IOException, BadDiskFileException {
		File fl = new File(filename);
		if (!fl.exists()) {
			throw new BadDiskFileException("File " + filename + " does not exist.");
		}
		inFile = new RandomAccessFile(fl, "rw");
		this.filename = filename;
		FileSize = fl.length();
		ParseTAPFile();
	}

	public TAPFile() {
		inFile = null;
		this.filename = "";
		FileSize = 0;
	}

	@Override
	public int GetMediaType() {
		return PLUSIDEDOS.MEDIATYPE_LINEAR;
	}

	@Override
	public String GetFilename() {
		return (filename);
	}

	@Override
	public void SetFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public int GetSectorSize() {

		return 1;
	}

	@Override
	public void SetSectorSize(int sz) {
		System.out.println("Attempt to set sector size for a TAP file.");
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
		return (FileSize);
	}

	@Override
	public int GetNumHeads() {
		return 1;
	}

	@Override
	public void SetNumHeads(int sz) {
		System.out.println("Attempt to set Number of Heads for a TAP file.");
	}

	@Override
	public int GetNumSectors() {
		return Blocks.length;
	}

	@Override
	public void SetNumSectors(int sz) {
		System.out.println("Attempt to set Number of Sectors for a TAP file.");
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
				System.out.println("Failed to close file " + filename + " with error " + e.getMessage());
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
		System.out.println("SetLogicalBlock not supported for TAP files. Add or delete files instead.");
		throw new IOException("SetLogicalBlock not supported for TAP files. Add or delete files instead.");
	}

	@Override
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
	public Boolean IsMyFileType(File filename) throws IOException {
		if (filename.getName().toUpperCase().endsWith("TAP")) {
			try {
				TAPFile mdt = new TAPFile(filename.getAbsolutePath());
				mdt.close();
				return true;
			} catch (Exception E) {
				return false;
			}
		} else {
			return false;
		}
	}

	public void ParseTAPFile() throws IOException {
		byte FileData[] = new byte[(int) FileSize];
		inFile.seek(0);
		inFile.read(FileData);

		int ptr = 0;
		int blocknum = 0;

		ArrayList<TAPBlock> TapBlocks = new ArrayList<TAPBlock>();

		while (ptr < FileData.length - 1) {
			TAPBlock tb = new TAPBlock(FileData, ptr, blocknum++);
			TapBlocks.add(tb);
			ptr = ptr + tb.rawblocklength;
		}
		Blocks = TapBlocks.toArray(new TAPBlock[0]);
	}

	public byte[] GetAllData() throws IOException {
		byte FileData[] = new byte[(int) FileSize];
		inFile.seek(0);
		inFile.read(FileData);
		return (FileData);
	}

	public byte[] SetAllData(byte FileData[]) throws IOException {
		inFile.seek(0);
		inFile.read(FileData);
		return (FileData);
	}

	@Override
	public String toString() {
		String result = "Filename: " + filename + "\n";
		for (TAPBlock t : Blocks) {
			result = result + "#" + t.blocknum + ": " + t + "\n";
		}
		return (result);
	}

	public static void main(String[] args) {
		try {
			String filename = "/home/graham/a.tap";

			TAPFile mdt = new TAPFile();
			if (mdt.IsMyFileType(new File(filename))) {
				System.out.println("File is a valid TAP file.");
				mdt = new TAPFile(filename);
				System.out.println(mdt);
			} else {
				System.out.println("File is not a valid Tap file.");
			}

		} catch (IOException | BadDiskFileException e) {
			e.printStackTrace();
		}
	}

	/**
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
	}

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

	public void AddBlock(byte[] block, int flag) throws IOException {
		TAPBlock newblock = new TAPBlock(Blocks.length, block, flag);
		TAPBlock newBlocks[] = new TAPBlock[Blocks.length + 1];
		System.arraycopy(Blocks, 0, newBlocks, 0, Blocks.length);
		newBlocks[Blocks.length] = newblock;
		Blocks = newBlocks;
		RewriteFile();
	}

	/**
	 * 
	 * @param Filename
	 * @throws IOException
	 */
	public void CreateEmptyTapeFile(String Filename) throws IOException {
		FileOutputStream NewFile = new FileOutputStream(Filename);
		try {
		} finally {
			filename = Filename;
			// Close, forcing flush
			NewFile.close();
			NewFile = null;
		}
		// Load the newly created file.
		inFile = new RandomAccessFile(Filename, "rw");
		FileSize = new File(Filename).length();
		ParseTAPFile();
	}

}

package hddEditor.libs.disks.LINEAR.tapblocks;

import java.io.IOException;

import hddEditor.libs.Speccy;
import hddEditor.libs.disks.ExtendedSpeccyBasicDetails;
import hddEditor.libs.disks.SpeccyBasicDetails;

/*
 * Storage for individual tape blocks.
 */
public class TAPBlock {
	// Actual data
	public byte data[];
	// Flag byte. (Usually $00 = header, $FF = data
	public int flagbyte;
	// TAP block Checksum
	public int checksum;
	// Length of the entire block including the two length bytes, checksum and flag
	// byte
	public int rawblocklength;
	// Block number in the file
	public int blocknum;
	// is the checksum valid?
	public boolean IsChecksumValid;
	// Pointer to the location in the file.
	public int fileLocation;

	/**
	 * Constructor used for reading from the tape
	 * 
	 * @param block - data block being parsed
	 * @param start - Start of the current block
	 * @param bNum  - Block number.
	 * @throws IOException
	 */
	public TAPBlock(byte block[], int start, int bNum) throws IOException {
		if (block.length < 4) {
			throw new IOException("TAP File has extraneous bits at the end");
		}
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

	/**
	 * Update a the current data block including its checksums and lengths.
	 * 
	 * @param block - New data block.
	 */
	public void UpdateBlockData(byte block[]) {
		data = block;
		rawblocklength = data.length + 4;
		UpdateChecksum();
	}

	/**
	 * Constructor for a new tape block.
	 * 
	 * @param fileloc  - Location in the file.
	 * @param block    - data for the block
	 * @param flagbyte - flag byte
	 * @throws IOException
	 */
	public TAPBlock(int fileloc, byte block[], int flagbyte) throws IOException {
		data = block;
		this.flagbyte = flagbyte;
		UpdateChecksum();
		rawblocklength = data.length + 4;
	}

	/**
	 * Update the checksum valid flag
	 */
	private void UpdateChecksumValid() {
		int cs = flagbyte & 0xff;
		for (byte byt : data) {
			cs = cs ^ (byt & 0xff);
		}
		IsChecksumValid = (cs == checksum);
	}

	/**
	 * Calculate the checksum and set it.
	 */
	private void UpdateChecksum() {
		int cs = flagbyte & 0xff;
		for (byte byt : data) {
			cs = cs ^ (byt & 0xff);
		}
		IsChecksumValid = true;
		checksum = cs;
	}

	/**
	 * Get the raw block for saving to disk including its length bytes, flag bytes
	 * and checksum. This block can be written straight to disk without further
	 * processing.
	 * 
	 * @return
	 */
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
	/**
	 * Overriden TOSTRING method.
	 */
	public String toString() {
		String result = "block length: " + data.length + " Flag:" + flagbyte + " checksum: " + checksum + " CsValid?"
				+ IsChecksumValid + " Location in file: " + fileLocation + " ";
		ExtendedSpeccyBasicDetails ebd = DecodeHeader();
		if (ebd != null) {
			String s = ebd.toString().replace("\n", ", ");
			result = result + "  (BASIC HEADER)" + s;
		}
		return (result);
	}

	/**
	 * If the data block is a basic header, return it as a BASIC header.
	 * 
	 * @return
	 */
	public ExtendedSpeccyBasicDetails DecodeHeader() {
		ExtendedSpeccyBasicDetails result = null;
		if (data.length == Speccy.TAPE_HEADER_LEN && flagbyte == 0x00) {
			int type = data[0] & 0xff;
			int filelen = (data[11] & 0xff) + ((data[12] & 0xff) * 0x100);
			int param1 = (data[13] & 0xff) + ((data[14] & 0xff) * 0x100);
			int param2 = (data[15] & 0xff) + ((data[16] & 0xff) * 0x100);
			byte filename[] = new byte[10];
			System.arraycopy(data, 1, filename, 0, 10);

			result = new ExtendedSpeccyBasicDetails(type, param2, param1, param1, 'A', new String(filename), filelen);
		}
		return (result);
	}
	
	/**
	 * Set the file as if its a BASIC header...
	 * @param sbd
	 */
	public void SetHeader(SpeccyBasicDetails sbd) {
		int param1 = 0;
		int param2 = 0;
		switch (sbd.BasicType) {
		case Speccy.BASIC_BASIC:
			param1 = sbd.LineStart;
			param2 = sbd.VarStart;
			break;
		case Speccy.BASIC_NUMARRAY:
			param1 = ((((sbd.VarName+"A").toUpperCase().charAt(0)-0x40) | 0x80) & 0xff) << 8;
			break;
		case Speccy.BASIC_CHRARRAY: 
			param1 = ((((sbd.VarName+"A").toUpperCase().charAt(0)-0x40) | 0xC0) & 0xff) << 8;
				break;
		case Speccy.BASIC_CODE:
			param1 = sbd.LoadAddress;
			param2 = 0x8000;
		default:
				break;
		}
		
		data[0] = (byte) sbd.BasicType;
		data[13] = (byte) (param1 & 0xff);
		data[14] = (byte) (((param1 & 0xff00) >> 8) & 0xff);
		data[15] = (byte) (param2 & 0xff);
		data[16] = (byte) (((param2 & 0xff00) >> 8) & 0xff);
		UpdateChecksum();
	}
	
	
}

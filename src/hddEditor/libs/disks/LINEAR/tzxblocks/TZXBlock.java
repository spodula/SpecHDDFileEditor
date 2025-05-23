package hddEditor.libs.disks.LINEAR.tzxblocks;

import hddEditor.libs.disks.ExtendedSpeccyBasicDetails;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.Speccy;
import hddEditor.libs.TZX;

public class TZXBlock {
	/*
	 * The Speccy contained data within the block. but for Standard and turbo data
	 * blocks, this is the data without the initial block ID and checksum (Note,
	 * technically the Turbo data block may or may not have these, but as most turbo
	 * loaders seem to be straight ports of the ROM loader, they usually seem to.
	 * The StandardDataBlock and TurboSpeedDatablock subclasses use the presence of
	 * a valid checksum to determine which is which.
	 * 
	 * For all other sub-classes, blockdata = data
	 */
	public byte data[];

	// The actually within the data block. including checksum and type for standard
	// and turbo data blocks
	public byte blockdata[];

	// The raw data including its Header ID, size and everything else not
	// data-related
	public byte rawdata[];

	// Block number in the tape
	public int BlockNumber = 0;

	// block type (equivalent to block rawdata[0])
	public int blocktype = 0;

	// Block textual description
	public String getBlockDesc() {
		return (TZX.GetDataBlockTypeForID(blocktype));
	}

	// if the block contains text (Archiveinfo, messageblock, ect), the contents as
	// text.
	public String BlockNotes = "";

	/**
	 * If the data block is 17 bytes, try to decode it as a ZX spectrum BASIC header
	 * block.
	 * 
	 * @return
	 */
	public ExtendedSpeccyBasicDetails DecodeHeader() {
		ExtendedSpeccyBasicDetails result = null;
		if (IsValidHeader()) {
			int type = data[0] & 0xff;
			int filelen = GetDblByte(data, 11);
			int param1 = GetDblByte(data, 13);
			int param2 = GetDblByte(data, 15);
			byte filename[] = new byte[10];
			System.arraycopy(data, 1, filename, 0, 10);
			char varname = (char) ((data[14] & 0x3f) + 0x40);

			result = new ExtendedSpeccyBasicDetails(type, param2, param1, param1, varname, new String(filename),
					filelen);
		}
		return (result);
	}

	public boolean IsValidHeader() {
		boolean result = false;
		if ((blocktype == TZX.TZX_STANDARDSPEED_DATABLOCK) && (blockdata != null) && (blockdata.length > 16)
				&& (blockdata[0] == 0)) {
			byte tmpblock[] = new byte[19];
			System.arraycopy(blockdata, 0, tmpblock, 0, Math.min(blockdata.length, tmpblock.length));
			int checksum = TZX.CalculateChecksumForBlock(tmpblock);
			int chsum = (tmpblock[tmpblock.length - 1] & 0xff);
			result = (chsum == checksum);
		}
		return (result);
	}

	/**
	 * Check to see if the block has a valid checksum The Speccy rom loader puts a
	 * checksum at the end of the block.
	 * 
	 * @return
	 */
	public boolean ValidChecksum() {
		if ((blocktype == TZX.TZX_STANDARDSPEED_DATABLOCK) && (blockdata != null) && (blockdata[0] == 0)) {
			return (IsValidHeader());
		} else {

			int checksum = -1;
			int chsum = 0;

			if (blockdata.length > 2) {
				checksum = TZX.CalculateChecksumForBlock(blockdata);
				chsum = (blockdata[blockdata.length - 1] & 0xff);
			}
			return (chsum == checksum);
		}
	}

	public void UpdateChecksum() {
		if ((blocktype == TZX.TZX_STANDARDSPEED_DATABLOCK) && (blockdata != null) && (blockdata[0] == 0)) {
			if (blockdata.length > 2) {
				int checksum = TZX.CalculateChecksumForBlock(blockdata);
				blockdata[blockdata.length - 1] = (byte) (checksum & 0xff);
			}
		}

	}

	/**
	 * Create the ZX spectrum data from the block data. This is for standard speed
	 * (and turbo speed loaders modified from the ROM loader) data blocks to extract
	 * the Speccy data from the block data. The standard ROM loader adds a byte at
	 * the start (The data block type ID as specified in A when calling 0556 (0 for
	 * header, 255 for data) and a byte at the end containing the checksum.
	 * 
	 * If the checksum isn't valid, it will assume the block is raw and not written
	 * by a Rom or Rom-derived saver.
	 */
	protected void UpdateDataFromBlockData() {
		if (ValidChecksum()) {
			// Remove the checksum and the type from the data
			data = new byte[blockdata.length - 2];
			System.arraycopy(blockdata, 1, data, 0, blockdata.length - 2);
		} else {
			data = blockdata;
		}
	}

	/**
	 * 
	 * @param newdata
	 */
	public void UpdateBlockData(byte[] newdata) {
		System.out.println("Update BlockData not supported for " + getClass().getName());
	}

	/**
	 * Return a WORD from the given byte array, TZX is LSB first.
	 * 
	 * @param data
	 * @param index
	 * @return
	 */
	public int GetDblByte(byte[] data, int index) {
		int d1 = (data[index] & 0xff);
		int d2 = (data[index + 1] & 0xff);

		return (d1 + (d2 * 0x100));
	}

	/**
	 * return a DWORD from the given byte array TZX is LSB first
	 * 
	 * @param data
	 * @param index
	 * @return
	 */
	public int GetDWORD(byte[] data, int index) {
		int w1 = GetDblByte(data, index);
		int w2 = GetDblByte(data, index + 2);

		return (w1 + (w2 * 0x10000));
	}

	@Override
	public String toString() {
		String result = String.format("%s (%02X)", getBlockDesc(), blocktype) + " Content length:";
		if (data == null) {
			result = result + " (No data)";
		} else {
			result = result + data.length;
		}
		return (result);
	}

	/**
	 * Set the file as if its a BASIC header...
	 * 
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
			param1 = ((((sbd.VarName + "A").toUpperCase().charAt(0) - 0x40) | 0x80) & 0xff) << 8;
			break;
		case Speccy.BASIC_CHRARRAY:
			param1 = ((((sbd.VarName + "A").toUpperCase().charAt(0) - 0x40) | 0xC0) & 0xff) << 8;
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
		
		System.arraycopy(data, 0, blockdata, 1, data.length);
		UpdateChecksum();
		System.arraycopy(blockdata, 0, rawdata, 5, blockdata.length);
	}
}

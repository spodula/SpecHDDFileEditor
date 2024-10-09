package hddEditor.libs.disks.HDD;

import java.io.File;
import java.io.FileInputStream;

/**
 * This is a wrapper around an IDE dos raw disk, which extracts the disk parameters
 * from the IDEDOS partition.  it will also transparently handle 8/16 bit access.
 * 
 * The first few sectors contain the partition information including the CHS information, 
 * Fortunately, as the system partition is ALWAYS the first in the first sector, this isn't required
 * to get the information.  
 * 
 * As such CHS information can always be found in the following locations in the first sector:
 *  00-0f: "PLUSIDEDOS      "
 *  ....
 *  20-21: Number of cylinders available to DOS
 *  22:    Number of heads
 *  23:    Sectors per track
 *  .....    
 *  This is all the information required for disk access at this level. 
 * 
 * http://zxvgs.yarek.com/en-idedos.html
 * https://sinclair.wiki.zxnet.co.uk/wiki/IDEDOS
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import hddEditor.libs.GeneralUtils;

public class IDEDosDisk extends RawHDDFile {
	public static final String IDEDOSHEADER = "PLUSIDEDOS";
	public boolean is8Bit = false;

	/** 
	 * Constructor. This one overrides the block size.
	 * 
	 * @param filename
	 * @param blocksize
	 * @throws IOException
	 */
	public IDEDosDisk(File file, int blocksize) throws IOException {
		super(file);
		this.DiskBlockSize = blocksize;
		parseDiskParameters();
	}
	
	/** 
	 * Constructor. This one uses the default block size.
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public IDEDosDisk(File file) throws IOException {
		super(file);
		parseDiskParameters();
	}

	
	/** 
	 * Constructor. Dont open a file
	 */
	public IDEDosDisk() {
		super();
	}

	/**
	 * Extract the disk parameters from the IDEDOS partition. Including disk size,
	 * 8/16 bit access and sector size.
	 * 
	 * @throws IOException
	 */
	private void parseDiskParameters() throws IOException {
		/**
		 * CHeck to see if we are only using the first 8 bytes of a word.
		 */
		byte FirstSector[] = GetBytesStartingFromSector(0, 512);
		if (new String(FirstSector, StandardCharsets.UTF_8).startsWith(IDEDOSHEADER)) {
			System.out.println("16 bit access");
		} else {
			FirstSector = HalfSector(FirstSector);
			if (new String(FirstSector, StandardCharsets.UTF_8).startsWith(IDEDOSHEADER)) {
				is8Bit = true;
				SectorSize = 256;
			} else {
				close();
				throw new IOException("Not an PlusIDEDOS Disk!");
			}
		}

		// Parse the cylinders from PLUSIDEDOS header.
		NumCylinders = (int) (FirstSector[0x20] & 0xff) * 256 + (FirstSector[0x21] & 0xff);
		NumHeads = (int) (FirstSector[0x22] & 0xff);
		NumSectors = (int) (FirstSector[0x23] & 0xff);
	}

	/**
	 * process the sector to extract the 8 bit data.
	 * 
	 * @param Sector
	 * @return
	 */
	private byte[] HalfSector(byte Sector[]) {
		byte result[] = new byte[Sector.length / 2];

		for (int i = 0; i < result.length; i++) {
			result[i] = Sector[i * 2];
		}

		return (result);
	}

	/**
	 * process the sector to expand it to 16 bits
	 * 
	 * @param Sector
	 * @return
	 */
	private byte[] DoubleSector(byte Sector[]) {
		byte result[] = new byte[Sector.length * 2];

		int ptr = 0;
		for (int i = 0; i < Sector.length; i++) {
			result[ptr++] = Sector[i];
			result[ptr++] = 0;
		}

		return (result);
	}

	/**
	 * ToString overridden to show local flags
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + "\nIs 8 bit?: " + is8Bit;
		return (result);
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String fName = "/dev/sde";
		File ffName = new File(fName);
		RawHDDFile h;
		try {
			System.out.println("Root? " + GeneralUtils.IsLinuxRoot());
			h = new IDEDosDisk();
			System.out.println("IDEDISK? " + h.IsMyFileType(ffName));
			h = new IDEDosDisk(ffName);
			System.out.println(h);
			h.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the specified number of bytes beginning at the given logical sector.
	 * 
	 * @param SectorNum
	 * @param sz
	 */
	@Override
	public byte[] GetBytesStartingFromSector(long SectorNum, long sz) throws IOException {
		if (!is8Bit) {
			byte bytes[] = super.GetBytesStartingFromSector(SectorNum, sz);
			return (bytes);
		} else {
			byte rawsector[] = super.GetBytesStartingFromSector(SectorNum * 2, sz * 2);
			byte HalvedSector[] = HalfSector(rawsector);

			return (HalvedSector);
		}
	}

	/**
	 * SetLogicalSector modified to take account of half sectors.
	 * 
	 * @param SectorNum
	 * @param result
	 */
	@Override
	public void SetLogicalBlockFromSector(long SectorNum, byte result[]) throws IOException {
		if (!is8Bit) {
			super.SetLogicalBlockFromSector(SectorNum, result);
		} else {
			byte[] doubledData = DoubleSector(result);
			super.SetLogicalBlockFromSector(SectorNum * 2, doubledData);
		}
	}

	/**
	 * Check to see if i can identify this file as one i can open...
	 * 
	 * @param filename
	 */
	@Override
	public Boolean IsMyFileType(File filename) throws IOException {
		boolean result = false;
		//Note, this wont work in Eclipse, but works fine stand-alone.
		if (filename.toString().contains("\\\\.\\DEVICE")) {
			FileInputStream FIS = new FileInputStream(filename);
			try {
				byte RawHeaderData[] = new byte[4096];
				FIS.read(RawHeaderData);

				if (new String(RawHeaderData, StandardCharsets.UTF_8).startsWith(IDEDOSHEADER)) {
					result = true;
				} else {
					RawHeaderData = HalfSector(RawHeaderData);
					if (new String(RawHeaderData, StandardCharsets.UTF_8).startsWith(IDEDOSHEADER)) {
						result = true;
					}
				}
			} finally {
				FIS.close();
			}
		} else {
			System.out.println("loading "+filename);
			RandomAccessFile inFile = new RandomAccessFile(filename, "rw");
			try {
				byte RawHeaderData[] = new byte[4096];
				inFile.seek(0);
				inFile.read(RawHeaderData);
				
				for (int i=0;i<16;i++) {
					System.out.print(String.format("%02x ", RawHeaderData[i] & 0xff));
				}
				System.out.println();
				
				
				if (new String(RawHeaderData, StandardCharsets.UTF_8).startsWith(IDEDOSHEADER)) {
					result = true;
				} else {
					RawHeaderData = HalfSector(RawHeaderData);
					if (new String(RawHeaderData, StandardCharsets.UTF_8).startsWith(IDEDOSHEADER)) {
						result = true;
					}
				}
			} finally {
				inFile.close();
			}
		}
		return (result);
	}

}

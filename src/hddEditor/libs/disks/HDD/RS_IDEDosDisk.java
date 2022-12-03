package hddEditor.libs.disks.HDD;

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
import java.nio.charset.StandardCharsets;

import hddEditor.libs.GeneralUtils;

public class RS_IDEDosDisk extends RS_IDEFile {
	public static final String IDEDOSHEADER = "PLUSIDEDOS";
	public boolean DataHalved = false;

	/**
	 * Constructor.
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public RS_IDEDosDisk(String filename) throws IOException {
		super(filename);
		parseDiskParameters();
	}

	public RS_IDEDosDisk() {
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
	
		String s = new String(FirstSector, StandardCharsets.UTF_8);
		if (s.startsWith(IDEDOSHEADER)) {
			//for HDF files, sectors can come pre-halved. 
			DataHalved = IsSectorHalved();
			if (DataHalved) {
				SectorSize = 256;
			}
		} else {
			FirstSector = HalfSector(FirstSector);
			if (new String(FirstSector, StandardCharsets.UTF_8).startsWith(IDEDOSHEADER)) {
				DataHalved = true;
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
	 * ToString overridden to show local flags
	 * 
	 * @return 
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + "\nData halved?: " + DataHalved;
		return (result);
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		RS_IDEDosDisk h;
		try {
			//h = new RS_IDEDosDisk("/data1/IDEDOS/Workbench2.3_4Gb_8Bits.hdf");
			h = new RS_IDEDosDisk("/home/graham/2gtest.hdf");
			System.out.println(h);
			//First sector:
			byte data[] = h.GetBytesStartingFromSector(0,512);
			System.out.println(GeneralUtils.HexDump(data, 0, 512));
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
	 * @return 
	 */
	@Override
	public byte[] GetBytesStartingFromSector(int SectorNum, int sz) throws IOException {
		boolean NeedTohalf = !IsSectorHalved() && DataHalved;
		
		if (!NeedTohalf) {
			byte bytes[] = super.GetBytesStartingFromSector(SectorNum, sz);
			return (bytes);
		} else {
			byte rawsector[] = super.GetBytesStartingFromSector(SectorNum*2, sz * 2);
			byte HalvedSector[] = HalfSector(rawsector);

			return (HalvedSector);
		}
	}

	/**
	 * Fill a byte array from the given SectorNum
	 * 
	 * @param SectorNum
	 * @param result[]
	 */
	@Override
	public void SetLogicalBlockFromSector(int SectorNum, byte result[]) throws IOException {
		boolean NeedToDouble = !IsSectorHalved() && DataHalved;
		if (!NeedToDouble) {
			super.SetLogicalBlockFromSector(SectorNum, result);
		} else {
			byte[] doubledData = DoubleSector(result);
			super.SetLogicalBlockFromSector(SectorNum*2, doubledData);
		}
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


}

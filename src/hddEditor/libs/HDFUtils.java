package hddEditor.libs;

import java.io.FileOutputStream;
import java.io.IOException;

import hddEditor.libs.disks.Disk;

public class HDFUtils {
	/**
	 * Write a generic HDF file header.
	 * 
	 * @param sourceDisk
	 * @param targetFile2
	 * @param isTarget8Bit
	 * @throws IOException
	 */
	public static void WriteHDFFileHeader(Disk sourceDisk, FileOutputStream targetFile2, boolean isTarget8Bit) 
			throws IOException {
		WriteHDFFileHeader(targetFile2, isTarget8Bit, sourceDisk.GetNumCylinders(), sourceDisk.GetNumHeads(), sourceDisk.GetNumSectors());
	}
	/**
	 * Write the header for HDF files.
	 * 
	 * @param sourceDisk
	 * @param targetFile2
	 * @param isTarget8Bit
	 * @throws IOException
	 */
	public static void WriteHDFFileHeader(FileOutputStream targetFile2, boolean isTarget8Bit, int cyl, int head, int spt)
			throws IOException {
		/*
		 * Write the start of the header
		 */
		byte header[] = new byte[0x16];
		for (int i = 0; i < header.length; i++) {
			header[i] = 0x00;
		}
		header[0x00] = 'R'; // Magic string
		header[0x01] = 'S';
		header[0x02] = '-';
		header[0x03] = 'I';
		header[0x04] = 'D';
		header[0x05] = 'E';
		header[0x06] = 0x1A; // End of magic string
		header[0x07] = 0x10; // V1.0
		if (isTarget8Bit)
			header[0x08] = 0x01; // Is 8 bit?
		header[0x09] = (byte) (0x80 & 0xff); // pointer to start of data
		targetFile2.write(header);

		/*
		 * Write the ATA IDENTIFY data (0x16-0x7f) Note, these are all words in LSB-MSB
		 * order
		 */
		int logicalCyls = cyl; 
		int logicalHeads = head; 
		int logicalSectors = spt; 

		byte ATA_IDENTIFY[] = new byte[0x6A];
		for (int i = 0; i < ATA_IDENTIFY.length; i++) {
			ATA_IDENTIFY[i] = 0x00;
		}
		ATA_IDENTIFY[0x00] = (byte) (0x8A & 0xff); // Magnetic device + some unused bits
		ATA_IDENTIFY[0x01] = (byte) (0x84 & 0xff); // Removable media + some unused bits

		//Logical cylinders 
		ATA_IDENTIFY[0x02] = (byte) (logicalCyls & 0xff);
		ATA_IDENTIFY[0x03] = (byte) ((logicalCyls / 0x100) & 0xff);
		
		// 4+5 reserved
		ATA_IDENTIFY[0x06] = (byte) (logicalHeads & 0xff);
		ATA_IDENTIFY[0x07] = (byte) ((logicalHeads / 0x100) & 0xff);
		// 8+9 reserved
		// 10+11 reserved
		// 12+13 SPT
		ATA_IDENTIFY[0x0c] = (byte) (logicalSectors & 0xff);
		ATA_IDENTIFY[0x0d] = (byte) ((logicalSectors / 0x100) & 0xff);
		// 0e+0f reserved
		// 10+11 reserved
		// 12+13 reserved
		// 14-27 - serial num
		int x = 0x14;
		for (char c : "JavaHDDEditor       ".toCharArray()) {
			ATA_IDENTIFY[x++] = (byte) c;
		}
		// 28-29 reserved
		// 2A-2B reserved
		// 2C-2D #num of vendor specific bytes for read/write long reads.
		ATA_IDENTIFY[0x2C] = (byte) 0x20;
		ATA_IDENTIFY[0x2D] = (byte) 0x20;
		// 2E-34 Firmware revision (8 bytes)
		x = 0x2E;
		for (char c : "01234567".toCharArray()) {
			ATA_IDENTIFY[x++] = (byte) c;
		}
		// 35-5D Model number (40 ascii characters)
		x = 0x36;   // 0123456789012345678901234567890123456789
		for (char c : "Created b V1.12 of RawHDDEditor-GDS 2025".toCharArray()) {
			ATA_IDENTIFY[x++] = (byte) c;
		}
		// 0x5e-5f MSB= vendor specific (Should be 0), LSB = max sectors transferred per
		// interrupt.
		ATA_IDENTIFY[0x5E] = (byte) 0x01; // Placeholder value of 1 sector.
		ATA_IDENTIFY[0x5F] = (byte) 0x00;
		// 0x60-0x61 reserved
		// 0x62-0x63 capabilities
		ATA_IDENTIFY[0x62] = 0x00; // Vendor specific (Should be 0)
		ATA_IDENTIFY[0x63] = 0x03; // Supports LBA + IORDY may be supported
		// 0x64-0x65 reserved
		// 0x66-0x67 PIO data transfer cycle timing mode
		ATA_IDENTIFY[0x66] = 0x00; // Vendor specific.
		ATA_IDENTIFY[0x67] = 0x02; // Absolutely no idea - placeholder value.
		// 0x68-0x69 DMA data transfer cycle timing mode
		ATA_IDENTIFY[0x68] = 0x00; // Vendor specific.
		ATA_IDENTIFY[0x69] = 0x00; // Again, Absolutely no idea Fortunately, DMA isnt used in anything that may use
									// this file.
		targetFile2.write(ATA_IDENTIFY);
	}

}

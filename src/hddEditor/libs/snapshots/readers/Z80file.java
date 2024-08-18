package hddEditor.libs.snapshots.readers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import hddEditor.libs.snapshots.CPUState;
import hddEditor.libs.snapshots.Registers;
import hddEditor.libs.snapshots.Z80Page;


public class Z80file {
	public static CPUState LoadZ80File(File Z80File) throws Exception {
		byte z80file[] = new byte[(int) Z80File.length()];
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(Z80File);
			inputStream.read(z80file);
			inputStream.close();
			return (LoadZ80File(z80file));
		} catch (IOException E) {
			System.out.println("Error openning " + Z80File.getAbsolutePath() + ". " + E.getMessage());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
				inputStream = null;
			}
		}
		return (null);
	}

	/**
	 * Parse the passed in bytes into a CPU state file.
	 * 
	 * @param snafile
	 * @return
	 * @throws Exception
	 */
	public static CPUState LoadZ80File(byte z80file[]) throws Exception {
		int version = 1;
		boolean is128K = false;
		
		if ((z80file[6] + z80file[7]) == 0) {
			version = 3;
			if (z80file[30] == 23)
				version = 2;
			
			// 128k snapshots are supported in V2 and V3 snapshots, 
			// so figure out which it is...
			if (version == 2) {
				is128K = (z80file[34] > 2);
			} else {
				is128K = (z80file[34] > 3);
			}
		}
		
		boolean compressed = true; // default for V2 and V3, for V1, controlled by flags
		if (version==1) {
			compressed = ((z80file[12] & 0x20) != 0);
		}

		byte ram[] = new byte[0xc000];
		if (version == 1) {
			if (!compressed) {
				System.arraycopy(z80file, 30, ram, 0, Math.min(ram.length, z80file.length - 30));
			} else {
				System.arraycopy(z80file, 30, ram, 0, Math.min(ram.length, z80file.length - 30));
				byte block[] = ExtractCompressedBlock(ram, 49152);
				System.arraycopy(block, 0, ram, 0, Math.min(ram.length, block.length));
			}
		} else {
			int blockstart = (z80file[30] & 0xff) + 29;
			Z80Page pages[] = ExtractPages(z80file, blockstart);
			if (!is128K) {
				// assemble a 48k block
				for (Z80Page page : pages) {
					if (page.Pagenum == 4)
						System.arraycopy(page.Data, 0, ram, 0x4000, 0x4000);
					if (page.Pagenum == 5)
						System.arraycopy(page.Data, 0, ram, 0x8000, 0x4000);
					if (page.Pagenum == 8)
						System.arraycopy(page.Data, 0, ram, 0x0000, 0x4000);
				}
			} else {
				/*
				 * Assembling pages in the data block as: 0000 - 3fff page 8 (128k Page 5) 4000
				 * - 7fff page 4 (128k page 1) 8000 - bfff Paged in page
				 * 
				 * 3 $0C000 (128k Page 0) 4 $10000 (128k Page 1) 5 $14000 (128k Page 2) 6 $18000
				 * (128k Page 3) 7 $1C000 (128k Page 4) 8 $20000 (128k Page 5) 9 $24000 (128k
				 * Page 6) 10 $28000 (128k Page 7)
				 */
				ram = new byte[11 * 0x4000];
				int pagedRam = (z80file[35] & 0x07);
				for (Z80Page page : pages) {
					if (page.get128Page() == pagedRam) {
						System.arraycopy(page.Data, 0, ram, 0x8000, 0x4000);
					}
					if (page.get128Page() == 0)
						System.arraycopy(page.Data, 0, ram, 0xC000, 0x4000);
					if (page.get128Page() == 1)
						System.arraycopy(page.Data, 0, ram, 0x10000, 0x4000);
					if (page.get128Page() == 2) {
						System.arraycopy(page.Data, 0, ram, 0x14000, 0x4000);
						System.arraycopy(page.Data, 0, ram, 0x4000, 0x4000);
					}
					if (page.get128Page() == 3)
						System.arraycopy(page.Data, 0, ram, 0x18000, 0x4000);
					if (page.get128Page() == 4)
						System.arraycopy(page.Data, 0, ram, 0x1C000, 0x4000);
					if (page.get128Page() == 5) {
						System.arraycopy(page.Data, 0, ram, 0x20000, 0x4000);
						System.arraycopy(page.Data, 0, ram, 0x0000, 0x4000);
					}
					if (page.get128Page() == 6)
						System.arraycopy(page.Data, 0, ram, 0x24000, 0x4000);
					if (page.get128Page() == 7)
						System.arraycopy(page.Data, 0, ram, 0x28000, 0x4000);
				}
			}
		}

		// a, f, b, c, d, e, h, l)
		Registers NormalRegs = new Registers(z80file[0], z80file[1], z80file[3], z80file[2], z80file[14], z80file[13],
				z80file[5], z80file[4]);

		Registers AltRegs = new Registers(z80file[21], z80file[22], z80file[16], z80file[15], z80file[18], z80file[17],
				z80file[20], z80file[19]);

		// mainref, altref, i, ixl, ixh,
		// iyl, iyh, r, im, ei, spl,
		// sph, border, pc, ram[]

		byte im = (byte) (z80file[29] & 0x03);
		byte border = (byte) ((z80file[12] & 0x0e) / 2);
		int pc = ((z80file[7] & 0xff) * 256) + (z80file[6] & 0xff);

		CPUState state = new CPUState(NormalRegs, AltRegs, z80file[10], z80file[25], z80file[26], z80file[23],
				z80file[24], z80file[11], im, z80file[27] != 0, z80file[8], z80file[9], border, pc, ram);
		
		return (state);
	}
	
	/**
	 * Extract the block and decompress it. 
	 * Z80 files are compressed using a simple RLE scheme.
	 * 
	 * @param block - Block to decompress
	 * @param ExpectedLength - Expected length of the block. Usually 16384.
	 * @return
	 */
	private static byte[] ExtractCompressedBlock(byte[] block, int ExpectedLength) {
		byte result[] = new byte[ExpectedLength];
		int destPtr = 0;
		boolean InED = false;
		int EDstate = 0;
		int edRUN = 0;
		int byteptr = 0;
		for (;byteptr < block.length;byteptr++) {
			byte b = block[byteptr];
			int x = (b & 0xff);
			if (!InED) {
				if (x == 0xED) {
					InED = true;
					EDstate = 0;
				} else {
					result[destPtr++] = b;
				}
			} else {
				if (EDstate == 0) {
					if (x != 0xED) {
						result[destPtr++] = (byte) 0xED;
						result[destPtr++] = b;
						InED = false;
						EDstate = 0;
					} else {
						EDstate++;
					}
				} else if (EDstate == 1) {
					edRUN = x;
					EDstate++;
				} else if (EDstate == 2) {
					for (int i = 0; i < edRUN; i++) {
						if (destPtr >= ExpectedLength)
							break;
						result[destPtr++] = b;
					}
					EDstate = 0;
					InED = false;
				}
			}
			if (destPtr >= ExpectedLength) {
				if (block.length == (byteptr-1))
					System.out.println("Stopping at " + destPtr+" ptr="+byteptr+"/"+block.length);
				break;
			}
		}
		return result;
	}

	
	/**
	 * Extract the pages from the z80 files into an array of z80Page objects.
	 * 
	 * @param data
	 * @param blockstart
	 * @return
	 */
	private static Z80Page[] ExtractPages(byte[] data, int blockstart) {
		ArrayList<Z80Page> pages = new ArrayList<Z80Page>();
		while (blockstart < data.length) {
			Z80Page page = new Z80Page();
			page.CompressedLength = (data[blockstart] & 0xff) + ((data[blockstart + 1] & 0xff) * 0x100);
			page.IsCompressed = true;
			if (page.CompressedLength == 0xffff) {
				page.CompressedLength = 0x4000;
				page.IsCompressed = false;
			}
			page.Pagenum = data[blockstart + 2] & 0xff;
			page.Rawdata = new byte[page.CompressedLength];
			blockstart = blockstart + 3;
			if (page.Pagenum > 18)
				break;
			if ((page.Pagenum > 2) && (page.Pagenum != 11)) {
				System.arraycopy(data, blockstart, page.Rawdata, 0, Math.min(data.length - blockstart, page.Rawdata.length));
				blockstart = blockstart + page.CompressedLength;

				if (!page.IsCompressed) {
					page.Data = new byte[0x4000];
					System.arraycopy(page.Rawdata, 0, page.Data, 0, page.Rawdata.length);
				} else {
					page.Data = ExtractCompressedBlock(page.Rawdata, 0x4000);
				}
			}

			System.out.println(page);
			pages.add(page);

		}
		return pages.toArray(new Z80Page[pages.size()]);
	}
}

package hddEditor.libs.snapshots.readers;
//SNAFile: support 128k

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;

import hddEditor.libs.snapshots.MachineState;
import hddEditor.libs.snapshots.Registers;
import hddEditor.libs.snapshots.Z80Page;

public class Z80file extends MachineState {
	private String HWv2[] = { "48K", "48K + IF1", "SAMRAM", "128K", "128K + IF1", "-", "-", "-", "-", "-", "-", "-",
			"-", "-", "-" };
	private String HWv3[] = { "48K", "48K + IF1", "SAMRAM", "48K + MGT", "128K", "128K + IF1", "128K + MGT", "+3", "+3",
			"Pentagon 128", "Scorpion 256", "Didaktik-Kompakt", "+2", "+2A", "TC2048", "TC2068" };

	private int hwtypeIDsv2[] = { MachineState.MT_48K, MachineState.MT_48K, MachineState.MT_48K, MachineState.MT_128K,
			MachineState.MT_128K };
	private int hwtypeIDsv3[] = { MachineState.MT_48K, MachineState.MT_48K, MachineState.MT_48K, MachineState.MT_48K,
			MachineState.MT_128K, MachineState.MT_128K, MachineState.MT_128K, MachineState.MT_PLUS2A3,
			MachineState.MT_PLUS2A3, MachineState.MT_Pentagon, MachineState.MT_Scorpion, MachineState.MT_Didaktik,
			MachineState.MT_PLUS2A3, MachineState.MT_PLUS2A3, MachineState.MT_TC20xx, MachineState.MT_TC20xx };

	/**
	 * Load a given SNA file.
	 * 
	 * @param SNAFile
	 * @throws Exception
	 */
	public Z80file(File SNAFile) throws Exception {
		byte snafile[] = new byte[(int) SNAFile.length()];
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(SNAFile);
			inputStream.read(snafile);
			inputStream.close();
			ParseZ80Data(snafile);
		} catch (IOException E) {
			System.out.println("Error openning " + SNAFile.getAbsolutePath() + ". " + E.getMessage());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
				inputStream = null;
			}
		}
	}

	/**
	 * Extract the SNA file from the given data.
	 * 
	 * @param SNAdata
	 * @throws Exception
	 */
	public Z80file(byte SNAdata[]) {
		ParseZ80Data(SNAdata);
	}

	/**
	 * Parse the passed in bytes into a CPU state file.
	 * 
	 * @param snafile
	 * @return
	 * @throws Exception
	 */
	public void ParseZ80Data(byte z80file[]) {
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

		SnapSpecific.put("File version", String.valueOf(version));

		boolean compressed = true; // default for V2 and V3, for V1, controlled by flags
		if (version == 1) {
			compressed = ((z80file[12] & 0x20) != 0);
		}

		// a, f, b, c, d, e, h, l)
		MainRegs = new Registers(z80file[0], z80file[1], z80file[3], z80file[2], z80file[14], z80file[13], z80file[5],
				z80file[4]);

		AltRegs = new Registers(z80file[21], z80file[22], z80file[16], z80file[15], z80file[18], z80file[17],
				z80file[20], z80file[19]);

		IM = (byte) (z80file[29] & 0x03);
		BORDER = (byte) ((z80file[12] & 0x0e) / 2);

		if (version == 1) {
			SnapSpecific.put("SAMROM Paged", String.valueOf((z80file[12] & 0x10) != 0));
			SnapSpecific.put("Compressed", String.valueOf((z80file[12] & 0x20) != 0));
		}

		PC = ((z80file[7] & 0xff) * 256) + (z80file[6] & 0xff);
		SPH = (byte) (z80file[9] & 0xff);
		SPL = (byte) (z80file[8] & 0xff);

		I = (byte) (z80file[10] & 0xff);
		R = (byte) (z80file[11] & 0xff);
		if ((z80file[12] & 0x01) == 1) {
			R = (byte) (R + 0x80);
		}

		IYL = (byte) (z80file[23] & 0xff);
		IYH = (byte) (z80file[24] & 0xff);
		IXL = (byte) (z80file[25] & 0xff);
		IXH = (byte) (z80file[26] & 0xff);
		EI = z80file[27] != 0;
		IM = (byte) (z80file[29] & 0x03);

		SnapSpecific.put("ISS 2 emulation", String.valueOf((z80file[27] & 0x04) != 0));
		SnapSpecific.put("Dbl Int Freq", String.valueOf((z80file[27] & 0x08) != 0));
		switch ((z80file[27] & 0x30) / 0x10) {
		case 1:
			SnapSpecific.put("Emu Vsync", "High");
			break;
		case 3:
			SnapSpecific.put("Emu Vsync", "Low");
			break;
		default:
			SnapSpecific.put("Emu Vsync", "Normal");
			break;
		}
		switch ((z80file[27] & 0xC0) / 0x40) {
		case 1:
			SnapSpecific.put("Joystick", "Cursor/Protek/AGF");
			break;
		case 2:
			SnapSpecific.put("Joystick", "Kempston");
			break;
		case 3:
			SnapSpecific.put("Joystick", "Sinclair IF2 left");
			break;
		case 4:
			SnapSpecific.put("Joystick", "Sinclair IF2 Right");
			break;
		}

		this.RAM = new byte[0xc000];
		if (version == 1) {
			if (!compressed) {
				System.arraycopy(z80file, 30, RAM, 0, Math.min(0xc000, z80file.length - 30));
			} else {
				System.arraycopy(z80file, 30, RAM, 0, Math.min(0xc000, z80file.length - 30));
				byte block[] = ExtractCompressedBlock(RAM, 49152);
				System.arraycopy(block, 0, RAM, 0, Math.min(0xc000, block.length));
			}
			this.MachineClass = MachineState.MT_48K;
		} else {
			PC = ((z80file[33] & 0xff) * 256) + (z80file[32] & 0xff);
			last7ffd = (z80file[35] & 0xff);
			if (z80file[34] == 2) {
				SnapSpecific.put("Samram page", String.valueOf(last7ffd));
				last7ffd = -1;
			}
			if (z80file[34] == 14 || z80file[34] == 15 || z80file[34] == 128) {
				SnapSpecific.put("Timex 0xf4", String.valueOf(last7ffd));
				last7ffd = -1;
			}
			/*
			 * Decode hardware type
			 */
			this.MachineClass = MachineState.MT_48K;
			boolean ModHwFlag = ((z80file[37] & 0x80) == 0x80);
			int hwflag = (z80file[34] & 0xff);
			String hwtype = "Unknown";
			if (version == 2) {
				if (hwflag < HWv2.length) {
					hwtype = HWv2[hwflag];
				}
				if (hwflag < hwtypeIDsv2.length) {
					this.MachineClass = hwtypeIDsv2[hwflag];
				}
			} else {
				if (hwflag < HWv3.length) {
					hwtype = HWv3[hwflag];
				} else if (hwflag == 128) {
					hwtype = "TS2068";
					this.MachineClass = MachineState.MT_TC20xx;
				}
				if (hwflag < hwtypeIDsv3.length) {
					this.MachineClass = hwtypeIDsv3[hwflag];
				}
			}
			if (ModHwFlag) {
				hwtype = hwtype.replace("48K", "16K");
				hwtype = hwtype.replace("128K", "+2");
				hwtype = hwtype.replace("+3", "+2A");
			}
			SnapSpecific.put("Hardware", hwtype);

			if (hwtype.contains("IF1")) {
				SnapSpecific.put("IF/1 paged", String.valueOf(z80file[36] == 0));
			}
			if (hwtype.startsWith("TC") || hwtype.startsWith("TS")) {
				SnapSpecific.put("Last out to $FF", String.valueOf(z80file[36] & 0xff));
			}

			SnapSpecific.put("R Emu", String.valueOf((z80file[37] & 0x01) == 0x01));
			SnapSpecific.put("LDIR Emu", String.valueOf((z80file[37] & 0x02) == 0x02));
			boolean AYPresent = ((z80file[37] & 0x04) == 0x04);
			SnapSpecific.put("AY sound", String.valueOf(AYPresent));
			if (AYPresent) {
				SnapSpecific.put("AY is fuller?", String.valueOf((z80file[37] & 0x40) == 0x40));
				if ((z80file[37] & 0x04) == 0x04) { // AY registers
					String s = "";
					for (int i = 0; i < 15; i++) {
						s = s + String.format("%02x ", z80file[39 + i] & 0xff);
					}
				}
			}
			lastfffd = (byte) (z80file[38] & 0xff);
			if (version == 3) {
				String s = String.format("%02x%02x%02x%02x", (z80file[58] & 0xff), (z80file[57] & 0xff),
						(z80file[56] & 0xff), (z80file[55] & 0xff));
				SnapSpecific.put("Tstate count", s);
				SnapSpecific.put("Speculator flags", String.valueOf(z80file[58] & 0xff));
				if (hwtype.contains("MGT")) {
					SnapSpecific.put("MGT paged", String.valueOf(z80file[59] != 0));
					switch (z80file[83] & 0xff) {
					case 0:
						SnapSpecific.put("MGT type", "Disciple+epson");
						break;
					case 1:
						SnapSpecific.put("MGT type", "Disciple+HL");
						break;
					case 16:
						SnapSpecific.put("MGT type", "+D");
						break;
					}
					SnapSpecific.put("Disciple Btn?", String.valueOf(z80file[84] != 0));
					SnapSpecific.put("Disciple Flag?", String.valueOf(z80file[85] != 0));
				}
				if (z80file[60] != 0) {
					SnapSpecific.put("Multiface paged", "True");
					SnapSpecific.put("$0000-$1fff Ram?", String.valueOf(z80file[61] == 0));
					SnapSpecific.put("$2000-$3fff Ram?", String.valueOf(z80file[62] == 0));
				}
				if ((z80file[30] & 0xff) == 55) {
					last1ffd = z80file[86] & 0xff;
				}
			}

			int blockstart = (z80file[30] & 0xff) + 29;
			Z80Page pages[] = ExtractPages(z80file, blockstart);
			int pagedram = 0;
			if (is128K) {
				pagedram = GetPagedRamNumber();
			}

			for (Z80Page page : pages) {
				if (page.get128Page() > -1)
					System.arraycopy(page.Data, 0, RamBanks[page.get128Page()], 0, 0x4000);

				// assemble a 48k block. Note the default layout differs
				// between 48K and 128K files.
				if (is128K) {
					if (page.get128Page() == 5)
						System.arraycopy(page.Data, 0, RAM, 0x0000, 0x4000);
					if (page.get128Page() == 2)
						System.arraycopy(page.Data, 0, RAM, 0x4000, 0x4000);
					if (page.get128Page() == pagedram)
						System.arraycopy(page.Data, 0, RAM, 0x8000, 0x4000);
				} else {
					if (page.Pagenum == 4)
						System.arraycopy(page.Data, 0, RAM, 0x4000, 0x4000);
					if (page.Pagenum == 5)
						System.arraycopy(page.Data, 0, RAM, 0x8000, 0x4000);
					if (page.Pagenum == 8)
						System.arraycopy(page.Data, 0, RAM, 0x0000, 0x4000);
				}
			}
		}
	}

	/**
	 * Extract the block and decompress it. Z80 files are compressed using a simple
	 * RLE scheme.
	 * 
	 * @param block          - Block to decompress
	 * @param ExpectedLength - Expected length of the block. Usually 16384.
	 * @return
	 */
	private byte[] ExtractCompressedBlock(byte[] block, int ExpectedLength) {
		byte result[] = new byte[ExpectedLength];
		int destPtr = 0;
		boolean InED = false;
		int EDstate = 0;
		int edRUN = 0;
		int byteptr = 0;
		for (; byteptr < block.length; byteptr++) {
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
				if (block.length == (byteptr - 1))
					System.out.println("Stopping at " + destPtr + " ptr=" + byteptr + "/" + block.length);
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
	private Z80Page[] ExtractPages(byte[] data, int blockstart) {
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
			if (page.Pagenum > 11)
				break;
			// If we are getting blank pages, time to exit. (See Addams family 128k
			// snapshot)
			if (page.Pagenum == 0 && pages.size() > 0)
				break;
			if ((page.Pagenum > 2) && (page.Pagenum != 11)) {
				System.arraycopy(data, blockstart, page.Rawdata, 0,
						Math.min(data.length - blockstart, page.Rawdata.length));
				blockstart = blockstart + page.CompressedLength;

				if (!page.IsCompressed) {
					page.Data = new byte[0x4000];
					System.arraycopy(page.Rawdata, 0, page.Data, 0, page.Rawdata.length);
				} else {
					page.Data = ExtractCompressedBlock(page.Rawdata, 0x4000);
				}
			}

			pages.add(page);

		}
		return pages.toArray(new Z80Page[pages.size()]);
	}

	@Override
	public Hashtable<String, String> DetailsAsArray() {
		Hashtable<String, String> result = super.DetailsAsArray();

		result.putAll(SnapSpecific);
		return (result);
	}

}

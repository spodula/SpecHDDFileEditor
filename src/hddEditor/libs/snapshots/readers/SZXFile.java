package hddEditor.libs.snapshots.readers;
/**
 * This implements support for the ZX-State file format as used by some modern emulators.
 * The format provides a lot more comprehensive support for all aspects of emulation than .Z80 and .SNA
 * 
 * It is of course, massively overkill for what we are doing here. 
 * 
 * https://www.spectaculator.com/docs/zx-state/intro.shtml
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;

import hddEditor.libs.snapshots.MachineState;
import hddEditor.libs.snapshots.readers.zxStateBlocks.GenericZXStateBlock;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStMCart;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStMouse;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStMultiface;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStOpus;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStOpusDisk;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStATARam;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStATASp;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStAyBlock;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStBeta128;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStBetaDisk;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStCF;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStCovox;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStCreator;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStDock;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStDskFile;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStGs;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStIF1;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStIF2Rom;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStJoystick;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStKeyboard;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStPlus3;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStPlusD;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStPlusDDisk;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStRamPage;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStRom;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStSCLDRegs;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStSIDE;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStSpecDrum;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStSpecRegs;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStTape;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStUSpeech;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStZ80Regs;
import hddEditor.libs.snapshots.readers.zxStateBlocks.ZXStZXPrinter;

public class SZXFile extends MachineState {
	/* @formatter:off */
	public static int ZXSTMF_ALTERNATETIMINGS = 1;
			//MachineID,Flag name,Real name,Contains a AY chip,Machinetype
	public static String[][] MachineIDs = { 
			{ "0", "ZXSTMID_16K", "16k ZX Spectrum", "N" ,"0"},
			{ "1", "ZXSTMID_48K", "48k ZX Spectrum or ZX Spectrum+", "N","0" },
			{ "2", "ZXSTMID_128K", "ZX Spectrum 128", "Y","1" }, 
			{ "3", "ZXSTMID_PLUS2", "ZX Spectrum +2", "Y","1" },
			{ "4", "ZXSTMID_PLUS2A", "ZX Spectrum +2A/+2B", "Y","2"  }, 
			{ "5", "ZXSTMID_PLUS3", "ZX Spectrum +3", "Y","2"},
			{ "6", "ZXSTMID_PLUS3E", "ZX Spectrum +3e", "Y","2" }, 
			{ "7", "ZXSTMID_PENTAGON128", "Pentagon 128", "Y","1" },
			{ "8", "ZXSTMID_TC2048", "Timex Sinclair TC2048", "N","6" },
			{ "9", "ZXSTMID_TC2068", "Timex Sinclair TC2068", "N","6" },
			{ "10", "ZXSTMID_SCORPION", "Scorpion ZS-256", "N","4" }, 
			{ "11", "ZXSTMID_SE", "ZX Spectrum SE", "Y","0" },
			{ "12", "ZXSTMID_TS2068", "Timex Sinclair TS2068", "N","6" },
			{ "13", "ZXSTMID_PENTAGON512", "Pentagon 512", "N","3" },
			{ "14", "ZXSTMID_PENTAGON1204", "Pentagon 1024", "N","3" },
			{ "15", "ZXSTMID_NTSC48K", "48k ZX Spectrum (NTSC)", "N","0" },
			{ "16", "ZXSTMID_128Ke", "ZX Spectrum 128Ke", "Y","1" } };
	/* @formatter:on */
	//Raw data of the file. so we only have to load it once.
	public byte rawdata[];

	//List of contained blocks
	public GenericZXStateBlock blocks[];

	/**
	 * Load a given SNA file.
	 * 
	 * @param SNAFile
	 * @throws Exception
	 */
	public SZXFile(File SZXFile) throws Exception {
		LoadSZXFile(SZXFile);
		ParseSZXData();
		ConvertSZXBlocksToMachineState();
	}

	/**
	 * Parse a SZX file from data already in memory.
	 * @param data
	 * @throws Exception
	 */
	public SZXFile(byte[] data) throws Exception {
		rawdata = data;
		ParseSZXData();
		ConvertSZXBlocksToMachineState();		
	}
	
	/**
	 * Decode each individual block in the file and put them into block storage.
	 * @throws Exception
	 */
	private void ParseSZXData() throws Exception  {
		if (!getMagic().equals("ZXST")) {
			throw new Exception("SZXfile: File header incorrect.");
		}

		// Decode blocks.
		int location = 8;
		ArrayList<GenericZXStateBlock> newblocks = new ArrayList<GenericZXStateBlock>();
		while (location < rawdata.length) {
			GenericZXStateBlock blk = new GenericZXStateBlock(rawdata, location);
			if (blk.BlockID.equals("CRTR")) {
				blk = new ZXStCreator(rawdata, location);
			} else if (blk.BlockID.equals("Z80R")) {
				blk = new ZXStZ80Regs(rawdata, location);
			} else if (blk.BlockID.equals("SPCR")) {
				blk = new ZXStSpecRegs(rawdata, location);
			} else if (blk.BlockID.equals("JOY")) {
				blk = new ZXStJoystick(rawdata, location);
			} else if (blk.BlockID.equals("KEYB")) {
				blk = new ZXStKeyboard(rawdata, location);
			} else if (blk.BlockID.equals("RAMP")) {
				blk = new ZXStRamPage(rawdata, location);
			} else if (blk.BlockID.equals("AY")) {
				blk = new ZXStAyBlock(rawdata, location, HasInternalAY());
			} else if (blk.BlockID.equals("ZXPR")) {
				blk = new ZXStZXPrinter(rawdata, location);
			} else if (blk.BlockID.equals("USPE")) {
				blk = new ZXStUSpeech(rawdata, location);
			} else if (blk.BlockID.equals("+3")) {
				blk = new ZXStPlus3(rawdata, location);
			} else if (blk.BlockID.equals("DRUM")) {
				blk = new ZXStSpecDrum(rawdata, location);
			} else if (blk.BlockID.equals("SIDE")) {
				blk = new ZXStSIDE(rawdata, location);
			} else if (blk.BlockID.equals("ZXAT")) {
				blk = new ZXStATASp(rawdata, location);
			} else if (blk.BlockID.equals("ATRP")) {
				blk = new ZXStATARam(rawdata, location);
			} else if (blk.BlockID.equals("ZXCF")) {
				blk = new ZXStCF(rawdata, location);
			} else if (blk.BlockID.equals("COVX")) {
				blk = new ZXStCovox(rawdata, location);
			} else if (blk.BlockID.equals("B128")) {
				blk = new ZXStBeta128(rawdata, location);
			} else if (blk.BlockID.equals("BDSK")) {
				blk = new ZXStBetaDisk(rawdata, location);
			} else if (blk.BlockID.equals("DOCK")) {
				blk = new ZXStDock(rawdata, location);
			} else if (blk.BlockID.equals("DSK0")) {
				blk = new ZXStDskFile(rawdata, location);
			} else if (blk.BlockID.equals("GS")) {
				blk = new ZXStGs(rawdata, location);
			} else if (blk.BlockID.equals("IF1")) {
				blk = new ZXStIF1(rawdata, location);
			} else if (blk.BlockID.equals("IF2R")) {
				blk = new ZXStIF2Rom(rawdata, location);
			} else if (blk.BlockID.equals("MDRV")) {
				blk = new ZXStMCart(rawdata, location);
			} else if (blk.BlockID.equals("ROM")) {
				blk = new ZXStRom(rawdata, location);
			} else if (blk.BlockID.equals("SCLD")) {
				blk = new ZXStSCLDRegs(rawdata, location);
			} else if (blk.BlockID.equals("AMXM")) {
				blk = new ZXStMouse(rawdata, location);
			} else if (blk.BlockID.equals("MFCE")) {
				blk = new ZXStMultiface(rawdata, location);
			} else if (blk.BlockID.equals("OPUS")) {
				blk = new ZXStOpus(rawdata, location);
			} else if (blk.BlockID.equals("ODSK")) {
				blk = new ZXStOpusDisk(rawdata, location);
			} else if (blk.BlockID.equals("PLSD")) {
				blk = new ZXStPlusD(rawdata, location);
			} else if (blk.BlockID.equals("PDSK")) {
				blk = new ZXStPlusDDisk(rawdata, location);
			} else if (blk.BlockID.equals("TAPE")) {
				blk = new ZXStTape(rawdata, location);
			}
			location = location + blk.raw.length;
			newblocks.add(blk);
		}
		blocks = newblocks.toArray(new GenericZXStateBlock[0]);
	}

	/**
	 * Load a given file into the RAWDATA array.
	 * @param SZXFile
	 * @throws Exception
	 */
	private void LoadSZXFile(File SZXFile) throws Exception {
		rawdata = new byte[(int) SZXFile.length()];
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(SZXFile);
			inputStream.read(rawdata);
			inputStream.close();
		} catch (IOException E) {
			System.out.println("Error openning " + SZXFile.getAbsolutePath() + ". " + E.getMessage());
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

	/* @formatter:off */ 
	/*-*
	 * Parse the SZX header.
	 * +-----+--------------+------------------------------------+
	 * | 0-3 | Magic Number | Always ZXST                        | 
	 * | 4   | Major ver    | Major version                      |
	 * | 5   | Minor ver    | Minor version                      |
	 * | 6   | machine ID   | Currently values 1-16 are defined  |
	 * | 7   | Flags        | Currently only one flag is defined.|
	 * |                    |    (ZXSTMF_ALTERNATE_TIMINGS)      |
	 * +-----+--------------+------------------------------------+
	 */	
	/* @formatter:on */
	
	/**
	 * Get the File Magic number. 
	 * 
	 * @return
	 */
	public String getMagic() {
		byte magic[] = new byte[4];
		System.arraycopy(rawdata, 0, magic, 0, magic.length);
		return (new String(magic));
	}

	/**
	 * Get the Major version of the file.
	 * 
	 * @return
	 */
	public int GetMajorVer() {
		return (rawdata[4] & 0xff);
	}

	/**
	 * Get the Minor version of the file.
	 * 
	 * @return
	 */
	public int GetMinorVer() {
		return (rawdata[5] & 0xff);
	}

	/**
	 * Get the Machine ID. 
	 * @return
	 */
	public int getMachineID() {
		return (rawdata[6] & 0xff);
	}

	/**
	 * Get the file flags.
	 * @return
	 */
	public int getFlags() {
		return (rawdata[7] & 0xff);
	}

	/**
	 * Decode the machine ID into a name.  
	 * 
	 * @return
	 */
	public String GetMachineShortName() {
		String machine = String.valueOf(getMachineID());
		String foundID[] = null;
		String result = "Invalid machine ID";
		for (String[] mc : MachineIDs) {
			if (mc[0].equals(machine)) {
				foundID = mc;
			}
		}
		if (foundID != null) {
			result = foundID[1];
		}
		return (result);
	}

	/**
	 * Returns TRUE if the selected machine has an internal AY chip. This is needed
	 * or the AY section stops making sense.
	 * 
	 * @return
	 */
	public boolean HasInternalAY() {
		String machine = String.valueOf(getMachineID());
		String foundID[] = null;
		String result = "Invalid machine ID";
		for (String[] mc : MachineIDs) {
			if (mc[0].equals(machine)) {
				foundID = mc;
			}
		}
		if (foundID != null) {
			result = foundID[3];
		}
		return (result.equals("Y"));
	}

	/**
	 * Get the machine full name from the machine ID
	 * 
	 * @return
	 */
	public String GetMachineFullName() {
		String machine = String.valueOf(getMachineID());
		String foundID[] = null;
		String result = "Invalid machine ID";
		for (String[] mc : MachineIDs) {
			if (mc[0].equals(machine)) {
				foundID = mc;
			}
		}
		if (foundID != null) {
			result = foundID[2];
		}
		return (result);
	}

	/**
	 * Get the flags as a string
	 * 
	 * @return
	 */
	public String GetFlagsAsString() {
		String result = "<none>";
		if ((getFlags() & 0x01) == 0x01) {
			result = "ZXSTMF_ALTERNATETIMINGS";
		}
		return (result);
	}

	/**
	 *  Override the default TOSTRING to provide some details.
	 *  
	 */
	public String toString() {
		String result = "Magic: " + getMagic();
		result = result + "\nVer: " + GetMajorVer() + "." + GetMinorVer();
		result = result + "\nMachine: #" + getMachineID() + " " + GetMachineShortName() + " \"" + GetMachineFullName()
				+ "\"";
		result = result + "\nFlags: " + getFlags() + " " + GetFlagsAsString();
		result = result + "\nBlocks: (" + blocks.length + ")\n";
		result = result + super.toString();

		return (result);
	}

	/**
	 * Test harness. Pass in a SZX file. get a list of the contents.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = args[0];
		try {
			SZXFile snap = new SZXFile(new File(filename));
			System.out.println(snap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This converts the loaded SZX blocks to the machine state. Note, as the SZX
	 * blocks provide a lot more information than we require, we will be ignoring
	 * most of it.
	 */
	private void ConvertSZXBlocksToMachineState() {
		this.SnapSpecific = new Hashtable<String, String>();

		// Data source type
		this.SOURCE = SOURCE_SZX;

		/*
		 * machine type.
		 */
		String SzxType = String.valueOf(getMachineID());
		String MCType = "0";

		for (String[] mc : MachineIDs) {
			if (mc[0].equals(SzxType)) {
				MCType = mc[4];
			}
		}
		MachineClass = Integer.valueOf(MCType);

		// Initialise ram banks for 128K machines.
		int PagedNum = 0;
		this.RAM = new byte[0xC000];
		if (MachineClass != MT_48K) {
			for (int page = 0; page < 7; page++) {
				byte data[] = new byte[0x4000];
				RamBanks[page] = data;
			}
			PagedNum = GetPagedRamNumber();
		}

		for (GenericZXStateBlock block : blocks) {
			String BlockTypeName = block.BlockID;

			if (BlockTypeName.equals("Z80R")) {
				ZXStZ80Regs blk = (ZXStZ80Regs) block;

				this.MainRegs = blk.cpustate.MainRegs;
				this.AltRegs = blk.cpustate.AltRegs;
				this.I = blk.cpustate.I;
				this.IXL = blk.cpustate.IXL;
				this.IXH = blk.cpustate.IXH;
				this.IYL = blk.cpustate.IYL;
				this.IYH = blk.cpustate.IYH;
				this.R = blk.cpustate.R;
				this.IM = blk.cpustate.IM;
				this.EI = blk.cpustate.EI;
				this.SPL = blk.cpustate.SPL;
				this.SPH = blk.cpustate.SPH;
				this.PC = blk.cpustate.PC;
			}

			if (BlockTypeName.equals("SPCR")) {
				ZXStSpecRegs blk = (ZXStSpecRegs) block;
				this.last1ffd = blk.io1ffd;
				this.last7ffd = blk.io7ffd;
				this.BORDER = (blk.ioFe & 0x07);
			}
			if (BlockTypeName.equals("RAMP")) {
				ZXStRamPage blk = (ZXStRamPage) block;
				if (MachineClass != MT_48K) {
					// Fix for Pentagon machines with more than 128K...
					if (blk.pagenum >= RamBanks.length) {
						// Double the size of the page list
						int newsize = RamBanks.length * 2;
						byte newBankList[][] = new byte[newsize][];
						// Copy the old page list to the new one.
						int newBankNum = 0;
						for (; newBankNum < RamBanks.length; newBankNum++) {
							newBankList[newBankNum] = newBankList[newBankNum];
						}
						// Fill the new extra pages with blank data.
						for (; newBankNum < newBankList.length; newBankNum++) {
							byte data[] = new byte[0x4000];
							newBankList[newBankNum] = data;
						}
						// Set the ram pages.
						RamBanks = newBankList;
					}
					// Set the ram paged data..
					RamBanks[blk.pagenum] = blk.UncompressedMemoryData;
				}

				// Copy the appropriate pages to the 48K of memory...
				if (blk.pagenum == 5) {
					System.arraycopy(blk.UncompressedMemoryData, 0, RAM, 0x0000, 0x4000);
				} else if (blk.pagenum == 2) {
					System.arraycopy(blk.UncompressedMemoryData, 0, RAM, 0x4000, 0x4000);
				} else if (blk.pagenum == PagedNum) {
					System.arraycopy(blk.UncompressedMemoryData, 0, RAM, 0x8000, 0x4000);
				}
			}
			if (BlockTypeName.equals("ZXAT")) {
				this.SnapSpecific.put("ZX-ATA SP", "ZX-ATASP present");
			}
			if (BlockTypeName.equals("ZXAT")) {
				ZXStAyBlock blk = (ZXStAyBlock) block;
				String regs = "";
				for (int reg : blk.Registers) {
					regs = regs + String.format("%02X", reg) + ":";
				}
				this.SnapSpecific.put("AY-3-891x Regs", regs);
				this.SnapSpecific.put("AY-3-891x Selected", String.valueOf(blk.currentReg));
			}
			if (BlockTypeName.equals("ZXCF")) {
				this.SnapSpecific.put("ZXCF interface", "ZXCF present");
			}
			if (BlockTypeName.equals("COVX")) {
				this.SnapSpecific.put("COVOX interface", "COVOX present");
			}
			if (BlockTypeName.equals("B128")) {
				ZXStBeta128 blk = (ZXStBeta128) block;
				this.SnapSpecific.put("Beta 128", "Beta 128 present with " + blk.NumDrives + " drives");
			}
			if (BlockTypeName.equals("CRTR")) {
				ZXStCreator blk = (ZXStCreator) block;
				this.SnapSpecific.put("Created by", blk.creator + " v" + blk.major + "." + blk.minor);
			}
			if (BlockTypeName.equals("GS")) {
				this.SnapSpecific.put("General sound interface", "GSI present");
			}
			if (BlockTypeName.equals("KEYB")) {
				ZXStKeyboard blk = (ZXStKeyboard) block;
				if (blk.kbflags != 0)
					this.SnapSpecific.put("Keyboard", "Issue 2 keyboard");
			}
			if (BlockTypeName.equals("IF1")) {
				ZXStIF1 blk = (ZXStIF1) block;
				this.SnapSpecific.put("Interface1", "Sinclair Interface 1 present");
				this.SnapSpecific.put("Microdrives", String.valueOf(blk.numMicrodrives) + " Microdrives present");
			}
			if (BlockTypeName.equals("IF2R")) {
				this.SnapSpecific.put("Interface2", "Inteface 2 Rom inserted");
			}
			if (BlockTypeName.equals("JOY")) {
				ZXStJoystick blk = (ZXStJoystick) block;
				this.SnapSpecific.put("Joystick Player 1", blk.Plr1AsString());
				this.SnapSpecific.put("Joystick Player 2", blk.Plr2AsString());
			}
			if (BlockTypeName.equals("AMXM")) {
				ZXStMouse blk = (ZXStMouse) block;
				this.SnapSpecific.put("Mouse emulation", blk.GetMouseType());
			}
			if (BlockTypeName.equals("OPUS")) {
				ZXStOpus blk = (ZXStOpus) block;
				this.SnapSpecific.put("DOpus", "Discovery Opus disk connected with " + blk.NumDrives + " drives");
			}
			if (BlockTypeName.equals("MFACE")) {
				this.SnapSpecific.put("Multiface", "Multiface connected");
			}
			if (BlockTypeName.equals("+3")) {
				ZXStPlus3 blk = (ZXStPlus3) block;
				this.SnapSpecific.put("+3 Disk drives", blk.numdrives + " Drives connected");
			}
			if (BlockTypeName.equals("PLSD")) {
				ZXStPlusD blk = (ZXStPlusD) block;
				this.SnapSpecific.put("Plus D disk interface", "Plus D disk interface connected");
				this.SnapSpecific.put("Plus D Disk drives", blk.NumDrives + " Drives connected");
			}
			if (BlockTypeName.equals("SIDE")) {
				this.SnapSpecific.put("Simple 8-bit IDE", "Simple 8-bit IDE interface connected.");
			}
			if (BlockTypeName.equals("DRUM")) {
				this.SnapSpecific.put("SpecDrum", "Spectrum interface connected.");
			}
			if (BlockTypeName.equals("USPE")) {
				this.SnapSpecific.put("uSpeech unit", "uSpeech unit connected.");
			}
			if (BlockTypeName.equals("ZXPR")) {
				ZXStZXPrinter blk = (ZXStZXPrinter) block;
				if (blk.flags == 1)
					this.SnapSpecific.put("ZX printer", "ZX printer connected");
			}

		}
	}

}

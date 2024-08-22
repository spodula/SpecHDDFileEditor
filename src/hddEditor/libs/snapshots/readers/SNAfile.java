package hddEditor.libs.snapshots.readers;
/**
 * Wrapper for SNA files.
 * 
 * This supports both the 48k an 128K versions. (And technically 
 * Pentagon 128 machines which are just a 128K machine with a TR-DOS interface).  
 * Note that SNA files do not properly support +2A/+3 machines.
 * 
 * SNA file reference from:
 * https://worldofspectrum.org/faq/reference/formats.htm
 */
/*-
 * All Words are stored Low-High
 *
 *48K version:
 *  Offset   Size   Description
 *  ------------------------------------------------------------------------
 *  0        1      byte   I
 *  1        8      word   HL',DE',BC',AF'
 *  9        10     word   HL,DE,BC,IY,IX
 *  19       1      byte   Interrupt (bit 2 contains IFF2, 1=EI/0=DI)
 *  20       1      byte   R
 *  21       4      words  AF,SP
 *  25       1      byte   IntMode (0=IM0/1=IM1/2=IM2)
 *  26       1      byte   BorderColor (0..7, not used by Spectrum 1.7)
 *  27       49152  bytes  RAM dump 16384..65535
 * ------------------------------------------------------------------------
 * Total: 49179 bytes
 * 
 * 48K files, the PC is on the top of the stack, so to run, you need to 
 * set the registers and execute a RETN
 * 
 *128K version:
 *   Offset   Size   Description
 *  ------------------------------------------------------------------------
 * 0        27     bytes  SNA header (see above)
 * 27       16Kb   bytes  RAM bank 5 \
 * 16411    16Kb   bytes  RAM bank 2  } - as standard 48Kb SNA file
 * 32795    16Kb   bytes  RAM bank n / (currently paged bank)
 * 49179    2      word   PC
 * 49181    1      byte   port 0x7ffd setting
 * 49182    1      byte   TR-DOS Rom paged (1) or not (0)
 * 49183    16Kb   bytes  remaining RAM banks in ascending order
 * ...
 * ------------------------------------------------------------------------
 * Total: 131103 or 147487 bytes
 * 
 * 128K files, the PC is stored actually in the file rather than using the stack. 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import hddEditor.libs.snapshots.MachineState;
import hddEditor.libs.snapshots.Registers;

public class SNAfile extends MachineState {

	/**
	 * Load a given SNA file.
	 * 
	 * @param SNAFile
	 * @throws Exception
	 */
	public SNAfile(File SNAFile) throws Exception {
		byte snafile[] = new byte[(int) SNAFile.length()];
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(SNAFile);
			inputStream.read(snafile);
			inputStream.close();
			ParseSNAData(snafile);
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
	public SNAfile(byte SNAdata[]) {
		ParseSNAData(SNAdata);
	}

	/**
	 * Parse the passed in bytes into a CPU state file.
	 * 
	 * @param snafile
	 * @return
	 * @throws Exception
	 */
	public void ParseSNAData(byte snafile[]) {
		/**
		 * Parse the first 48K file.
		 */
		byte ram[] = new byte[0xc000];
		System.arraycopy(snafile, 0x1b, ram, 0, 0xc000);
		// a, f, b, c, d, e, h, l)
		Registers AltRegs = new Registers(snafile[0x08], snafile[0x07], snafile[0x06], snafile[0x05], snafile[0x05],
				snafile[0x03], snafile[0x02], snafile[0x01]);
		Registers NormalRegs = new Registers(snafile[0x16], snafile[0x15], snafile[0x0e], snafile[0x0d], snafile[0x0c],
				snafile[0x0d], snafile[0x0a], snafile[0x09]);

		this.MainRegs = NormalRegs;
		this.AltRegs = AltRegs;

		this.I = snafile[0x00];
		this.IXL = snafile[0x11];
		this.IXH = snafile[0x12];
		this.IYL = snafile[0x0F];
		this.IYH = snafile[0x10];
		this.R = snafile[0x14];
		this.IM = snafile[0x19];
		this.EI = snafile[0x13] != 0;
		this.SPL = snafile[0x17];
		this.SPH = snafile[0x18];
		this.PC = -1;
		this.RAM = ram;

		MachineClass = MachineState.MT_48K;
		SOURCE = MachineState.SOURCE_SNA;

		/**
		 * If the snapshot is a 128K snapshot, parse the second bit. Note, .SNA files
		 * ONLY support 128k machine files, NOT +2A/+3 machines. These are not properly
		 * supported, although they will *probably* work.
		 * 
		 * This is due to the lack of 0x1ffd storage, meaning the file doesn't support
		 * the +2A/+3 Special Ram modes and the ROM bit 2.
		 */
		if (snafile.length > 50000) {
			MachineClass = MachineState.MT_128K;
			PC = (snafile[0xc01b] & 0xff) + ((snafile[0xc01c] & 0xff) * 256);
			last7ffd = (snafile[0xc01d] & 0xff);
			SnapSpecific.put("TRDOS_PAGED", String.valueOf((snafile[0xc01e] & 0xff)));

			System.arraycopy(ram, 0x0000, RamBanks[5], 0, 0x4000);
			System.arraycopy(ram, 0x4000, RamBanks[2], 0, 0x4000);
			System.arraycopy(ram, 0x8000, RamBanks[GetPagedRamNumber()], 0, 0x4000);

			int ptr = 0xc01f;
			for (int page = 0; page < 8; page++) {
				if (page != 5 && page != 2 && page != GetPagedRamNumber()) {
					System.arraycopy(snafile, ptr, RamBanks[page], 0, 0x4000);
					ptr = ptr + 0x4000;
				}
			}
		}
	}
	
	/**
	 * 
	 */
	@Override
	public Hashtable<String, String> DetailsAsArray() {
		Hashtable<String, String> result = super.DetailsAsArray();
		String s = SnapSpecific.get("TRDOS_PAGED");
		String tdpaged = "False";
		if ((s!=null) && (!s.equals("0"))) {
			tdpaged = "True";
		}
		result.put("TRDOS Paged?",tdpaged);
		
		return(result);
	}

	

}

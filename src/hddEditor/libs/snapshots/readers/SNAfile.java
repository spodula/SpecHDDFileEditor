package hddEditor.libs.snapshots.readers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import hddEditor.libs.snapshots.CPUState;
import hddEditor.libs.snapshots.Registers;

public class SNAfile {
	/**
	 * Load the given SNA file and return the CPU state.
	 * 
	 * @param SNAFile
	 * @return
	 * @throws Exception
	 */
	public static CPUState LoadSNAFile(File SNAFile) throws Exception {
		byte snafile[] = new byte[49179];
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(SNAFile);
			inputStream.read(snafile);
			inputStream.close();
			return(ParseSNAData(snafile));
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
		return(null);
	}

	/**
	 * Parse the passed in bytes into a CPU state file.
	 * 
	 * @param snafile
	 * @return
	 * @throws Exception
	 */
	public static CPUState ParseSNAData(byte snafile[]) throws Exception {
		if (snafile.length!=49179) {
			throw new Exception("Given file is not a 48k SNA snapshot file. ("+String.valueOf(snafile.length)+")");
		} else {
			byte ram[] = new byte[0xc000];
			System.arraycopy(snafile, 0x1b, ram, 0, 0xc000);
											//a, f, b, c, d, e, h, l)
			Registers AltRegs = new Registers(snafile[0x08], snafile[0x07], snafile[0x06], snafile[0x05], snafile[0x05],
					snafile[0x03], snafile[0x02], snafile[0x01]);
			Registers NormalRegs = new Registers(snafile[0x16], snafile[0x15], snafile[0x0e], snafile[0x0d], snafile[0x0c],
					snafile[0x0d], snafile[0x0a], snafile[0x09]);
			CPUState state = new CPUState(NormalRegs, AltRegs, snafile[0x00], snafile[0x11], snafile[0x12],
					snafile[0x0F], snafile[0x10], snafile[0x14], snafile[0x19], snafile[0x13] != 0, snafile[0x17],
					snafile[0x18], snafile[0x1A], 0, ram);
			
			return(state);
		}
	}
	
	
	
}

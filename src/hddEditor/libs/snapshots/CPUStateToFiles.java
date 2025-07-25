package hddEditor.libs.snapshots;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import hddEditor.libs.SpeccyFileEncoders;
import hddEditor.libs.partitions.IDEDosPartition;

public class CPUStateToFiles {
	/**
	 * Convert an CPU state into a set of 4 files.
	 * 
	 * @param cs
	 * @param TargetPartition
	 * @param fileprefix
	 * @throws Exception 
	 */
	public static void SaveToPartition(MachineState cs, IDEDosPartition TargetPartition, String fileprefix)
			throws Exception {
		if (cs.MachineClass != MachineState.MT_48K) {
			//throw new Exception("Can only process 48k snapshots.");
		}

		InputStream document = null;
		if (IsInJar()) {
			document = new CPUStateToFiles().getClass().getResourceAsStream("/snaloader.bas");
		} else {
			File file = new File("src/resources", "snaloader.bas");
			document = new FileInputStream(file);
		}

		ArrayList<String> SBF = new ArrayList<String>();
		BufferedReader bf = new BufferedReader(new InputStreamReader(document));
		try {
			String line = bf.readLine().trim();
			while (line != null) {
				if (line.length() > 0 && line.charAt(0) != '#') {
					SBF.add(line);
				}
				line = bf.readLine();
			}
		} finally {
			bf.close();
		}
		String StaticBasicFile[] = SBF.toArray(new String[0]);

		byte scr[] = new byte[0x1b00];
		byte bi1[] = new byte[0x9500];
		byte bi2[] = new byte[0x1000];
		System.arraycopy(cs.RAM, 0, scr, 0, 0x1b00);
		System.arraycopy(cs.RAM, 0x1b00, bi2, 0, 0x1000);
		System.arraycopy(cs.RAM, 0x2b00, bi1, 0, 0x9500);

		TargetPartition.AddCodeFile(fileprefix + ".scr", 0x4000, scr);
		TargetPartition.AddCodeFile(fileprefix + ".bi1", 0x6b00, bi1);
		TargetPartition.AddCodeFile(fileprefix + ".bi2", 0x5b00, bi2);

		String Notes = cs.toString();

		int sp = ((cs.SPH & 0xff) * 0x100) + (cs.SPL & 0xff);
		Notes = Notes + "\nSP: " + String.format("%04X (%d)", sp, sp) + "\n";
		try {
			for (int i = 0; i < 6; i++) {
				int value = (cs.RAM[sp - 0x4000] & 0xff) + ((cs.RAM[sp - 0x4000 + 1] & 0xff) * 0x100);

				Notes = Notes + String.format("%04X %04X (%d)", sp, value, value) + "\n";
				sp = sp + 2;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			Notes = Notes + "Run out of stack.\n";
		}

//		TargetPartition.AddCodeFile("NOTES.txt", 0x8000, Notes.getBytes());

		System.out.println(Notes);

		String target[] = new String[StaticBasicFile.length];

		for (int tptr = 0; tptr < StaticBasicFile.length; tptr++) {
			String line = StaticBasicFile[tptr];
			line = line.replace("--filename--", fileprefix);
			line = line.replace("--spl--", String.valueOf(cs.SPL & 0xff));
			line = line.replace("--sph--", String.valueOf(cs.SPH & 0xff));
			if (line.contains("DATA 62,--I--,")) {
				if (cs.I == 0x3f) {
					// iF I =3f (As it is in BASIC), just don't set it
					line = "";
				} else {
					line = line.replace("--I--", String.valueOf(cs.I & 0xff));
				}
			}
			if (line.contains("237,--IM--")) {
				switch(cs.IM) {
				case 0:
					line = line.replace("--IM--", String.valueOf(0x46));
					break;
				case 1:
					line = line.replace("--IM--", String.valueOf(0x56));
					break;
				case 2:
					line = line.replace("--IM--", String.valueOf(0x5E));
					break;
				}
				
				// If IM = 1 as in BASIC, just ignore it.
				if (cs.IM == 1) {
					line = "";
				}
			}
			line = line.replace("--AltF--", String.valueOf(cs.AltRegs.F & 0xff));
			line = line.replace("--AltA--", String.valueOf(cs.AltRegs.A & 0xff));
			line = line.replace("--AltB--", String.valueOf(cs.AltRegs.B & 0xff));
			line = line.replace("--AltC--", String.valueOf(cs.AltRegs.C & 0xff));
			line = line.replace("--AltD--", String.valueOf(cs.AltRegs.D & 0xff));
			line = line.replace("--AltE--", String.valueOf(cs.AltRegs.E & 0xff));
			line = line.replace("--AltH--", String.valueOf(cs.AltRegs.H & 0xff));
			line = line.replace("--AltL--", String.valueOf(cs.AltRegs.L & 0xff));
			line = line.replace("--F--", String.valueOf(cs.MainRegs.F & 0xff));
			line = line.replace("--A--", String.valueOf(cs.MainRegs.A & 0xff));
			line = line.replace("--B--", String.valueOf(cs.MainRegs.B & 0xff));
			line = line.replace("--C--", String.valueOf(cs.MainRegs.C & 0xff));
			line = line.replace("--D--", String.valueOf(cs.MainRegs.D & 0xff));
			line = line.replace("--E--", String.valueOf(cs.MainRegs.E & 0xff));
			line = line.replace("--H--", String.valueOf(cs.MainRegs.H & 0xff));
			line = line.replace("--L--", String.valueOf(cs.MainRegs.L & 0xff));
			line = line.replace("--IXL--", String.valueOf(cs.IXL & 0xff));
			line = line.replace("--IXH--", String.valueOf(cs.IXH & 0xff));
			if (line.contains("DATA 253,33,")) {
				// Dont bother setting IY if its 5C3A
				int iyl = cs.IYL & 0xff;
				int iyh = cs.IYH & 0xff;
				if ((iyl != 58) || (iyh != 92)) {
					line = line.replace("--IYL--", String.valueOf(iyl));
					line = line.replace("--IYH--", String.valueOf(iyh));
				} else {
					line = "";
				}
			}
			if (line.contains("--DIEI--")) {
				if (cs.PC > 0) {
					int pcl = (cs.PC & 0xff);
					int pch = (cs.PC / 0x100);
					line = line.replace("201", "195," + pcl + "," + pch);
				}

				if (cs.EI) {
					line = line.replace("--DIEI--", "251,");
				} else {
					line = line.replace("--DIEI--", "");
				}
			}
			target[tptr] = line.trim();
		}

		byte BasicData[] = SpeccyFileEncoders.EncodeTextFileToBASIC(target);

		TargetPartition.AddBasicFile(fileprefix + ".bas", BasicData, 10, BasicData.length);
	}

	/**
	 * returns TRUE is running from a JAR file. (This affects where to find some of
	 * the files)
	 * 
	 * @return
	 */
	public static boolean IsInJar() {
		@SuppressWarnings("rawtypes")
		Class me = new CPUStateToFiles().getClass();
		return (me.getResource(me.getSimpleName() + ".class").toString().startsWith("jar:"));
	}

}

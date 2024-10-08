package hddEditor.libs;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;

public class DiskListWindows {
	public WinDiskInfo disks[];
	public String errors[] = null;

	public class WinDiskInfo {
		public long realsz; // Real size in bytes
		public String name; // Name of the disk, eg sda, sdb
		public File dets; // FILE pointing to the drive
		public String driveType; // Drive type, USB or ATA
		public String model; // Model number
		public String Vendor; // Vendor string

		@Override
		/**
		 * Overriden TOSTRING method.
		 */
		public String toString() {
			String result = "name:" + name + " drivetype:" + driveType + " model:" + model + " size:" + GetTextSz();
			return (result.trim());
		}

		public String GetTextSz() {
			String postfix[] = { "", "b", "Kb", "Mb", "Tb", "Pb" };
			long v = realsz;
			int pfptr = 0;
			while (v > 1000) {
				v = v / 1000;
				pfptr++;
			}
			String result = String.format("%d%s", v, postfix[pfptr]);
			return (result);
		}
	}

	public DiskListWindows() {
		if (!System.getProperty("os.name").toUpperCase().contains("WIN")) {
			String err = "Cannot use DiskListWindows, os.name returns " + System.getProperty("os.name");
			System.err.println(err);
			this.disks = new WinDiskInfo[0];
			this.errors = new String[] { err };
		} else {

			// Excecute the wmic command to get details
			String output[] = null;
			try {
				String line;
				Process p = Runtime.getRuntime()
						.exec(new String[] { "wmic", "diskdrive", "get", "deviceID,model,size,interfaceType" });
				BufferedReader brInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader brError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				try {
					p.waitFor();
					ArrayList<String> al = new ArrayList<String>();
					while ((line = brInput.readLine()) != null) {
						line = line.trim();
						if (!line.isBlank()) {
							al.add(line);
						}
					}
					output = al.toArray(new String[0]);

					al.clear();
					while ((line = brError.readLine()) != null) {
						line = line.trim();
						if (!line.isBlank()) {
							al.add(line);
						}
					}
					errors = al.toArray(new String[0]);
				} finally {
					brInput.close();
					brError.close();
				}
			} catch (Exception err) {
				err.printStackTrace();
			}

			if (output != null) {
				// read the order from the first line.
				String header = output[0];

				String ColOrder[] = new String[4];
				int ColOrderLength[] = new int[4];

				boolean inspace = false;
				int colnumber = 0;
				int colorderlen = 0;
				for (char c : header.toCharArray()) {
					if (c != ' ') {
						if (inspace) {
							ColOrderLength[colnumber] = colorderlen;
							inspace = false;
							colnumber++;
							colorderlen = 1;
						} else {
							colorderlen++;
						}
					} else {
						if (!inspace) {
							inspace = true;
						}
						colorderlen++;
					}
				}
				// Marker for the last column length.
				ColOrderLength[ColOrderLength.length - 1] = 999;

				int start = 0;
				for (int i = 0; i < ColOrderLength.length; i++) {
					int len = ColOrderLength[i];
					ColOrder[i] = header.substring(start, Math.min(len + start, header.length()));
					start = start + len;
				}

				ArrayList<WinDiskInfo> dis = new ArrayList<WinDiskInfo>();

				for (int i = 1; i < output.length; i++) {
					String dat = output[i];
					Hashtable<String, String> tmpLine = new Hashtable<String, String>();
					int colstart = 0;
					for (int index = 0; index < ColOrderLength.length; index++) {
						int length = ColOrderLength[index];
						String content = dat.substring(Math.min(colstart, dat.length()),
								Math.min(length + colstart, dat.length()));
						String name = ColOrder[index];
						colstart = colstart + length;
						tmpLine.put(name.trim(), content.trim());
					}

					WinDiskInfo wdi = new WinDiskInfo();
					wdi.name = tmpLine.get("DeviceID");
					wdi.dets = new File(wdi.name);
					wdi.driveType = tmpLine.get("InterfaceType");
					wdi.model = tmpLine.get("Model");
					String sz = tmpLine.get("Size");
					if (sz != null && !sz.isEmpty()) {
						wdi.realsz = Long.valueOf(sz);
					} else {
						wdi.realsz = 0;
					}
					dis.add(wdi);
				}

				this.disks = dis.toArray(new WinDiskInfo[0]);
			}
		}

	}

	public static void main(String args[]) {
		DiskListWindows dlw = new DiskListWindows();
		for (WinDiskInfo d : dlw.disks) {
			System.out.println(d);
		}
	}

}

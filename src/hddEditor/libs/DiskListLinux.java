package hddEditor.libs;

/*-
 * Get a disk list along with details for Linux. This does not seen to require root privileges, although to 
 * actually access any of the returned devices, you probably will. 
 * This is a massive hack....
 *
 * It makes use of the following in sysfs:
 * 
 *  /sys/block/<drive>
 *  /sys/block/<drive>/device/model
 *	/sys/block/<drive>/device/vendor
 *	/sys/block/<drive>/size
 *	/sys/class/block/<drive>/queue/logical_block_size
 * 
 * and this in procfs
 * 
 *  /proc/partitions
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class DiskListLinux {
	// list of partitions
	public DiskInfo disks[];

	/**
	 * Class storage for the disk.
	 */
	public class DiskInfo {
		public int major; // Major Id
		public int minor; // Minor Id
		public int blocksz; // Block size
		public long blocks; // Number of blocks
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
			String result = name + ": " + major + "," + minor + " Blocksz:" + blocksz + " Blocks:" + blocks
					+ " drivetype:" + driveType + " Vendor:" + Vendor + " " + model + " size:" + GetTextSz();
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

	/**
	 * Constructor, load the drives
	 */
	public DiskListLinux() {
		if (!System.getProperty("os.name").toUpperCase().contains("LINUX")) {
			String err = "Cannot use DiskListLinux, os.name returns " + System.getProperty("os.name");
			System.err.println(err);
			this.disks = new DiskInfo[0];
		} else {

			ArrayList<DiskInfo> drives = new ArrayList<DiskInfo>();
			try {
				/*
				 * /proc/partitions format: Major minor #blocks name
				 */
				BufferedReader br = new BufferedReader(new FileReader("/proc/partitions"));
				try {
					String line = br.readLine();
					while (line != null) {
						String text = line;
						try {
							// remove extranious spaces
							text = text.trim();
							while (text.contains("  ")) {
								text = text.replace("  ", " ");
							}
							if (!text.isBlank()) {
								String cols[] = text.split(" ");
								DiskInfo pi = new DiskInfo();
								pi.major = Integer.valueOf(cols[0]);
								pi.minor = Integer.valueOf(cols[1]);
								pi.blocks = Long.valueOf(cols[2]);
								pi.name = cols[3];
								pi.dets = new File("/dev", pi.name);

								boolean ParentPartFound = false;
								for (DiskInfo p : drives) {
									if (pi.name.startsWith(p.name)) {
										ParentPartFound = true;
									}
								}
								if (!ParentPartFound) {
									drives.add(pi);
								}
							}
						} catch (Exception E) {
							// Eat any parsing exception. This is usually the header.
						}
						line = br.readLine();
					}
					// convert the disk array into a static array.
					disks = drives.toArray(new DiskInfo[drives.size()]);

					// try to populate more fieldss
					for (DiskInfo p : disks) {
						// model
						byte data[] = GeneralUtils.ReadFileIntoArray("/sys/block/" + p.name + "/device/model");
						p.model = new String(data).trim();
						// vendor
						data = GeneralUtils.ReadFileIntoArray("/sys/block/" + p.name + "/device/vendor");
						p.Vendor = new String(data).trim();
						// size
						data = GeneralUtils.ReadFileIntoArray("/sys/block/" + p.name + "/size");
						p.realsz = Long.valueOf(new String(data).trim()) * 512;

						// loglcal block size. Note, this sometimes had a header, so just extract the
						// number in the file.
						data = GeneralUtils
								.ReadFileIntoArray("/sys/class/block/" + p.name + "/queue/logical_block_size");
						String lbsString = new String(data).trim();
						String NumbersOnly = "";
						// Extract any numbers
						for (char c : lbsString.toCharArray()) {
							if (c >= '0' && c <= '9')
								NumbersOnly = NumbersOnly + c;
						}
						// and set it as block size.
						p.blocksz = Integer.parseInt(NumbersOnly);

						// Extract the symbolic link information. This will contain ATA or USB
						// This is useful for telling USB drives from internal drives.
						File f = new File("/sys/block", p.name);
						String pth = f.getCanonicalPath().toLowerCase();
						if (pth.contains("/usb")) {
							p.driveType = "usb";
						} else if (pth.contains("/ata")) {
							p.driveType = "ata";
						} else if (pth.contains("/scsi")) {
							p.driveType = "scsi";
						} else {
							p.driveType = "unknown";
						}
					}
				} catch (IOException e) {
					System.out.println(e.getMessage());
					System.out.println("Error fetching drive list, Cant read /proc/partitions. Is this linux?");
				} finally {
					try {
						br.close();
					} catch (IOException e) {
					}
				}
			} catch (FileNotFoundException e) {
				System.out.println("Error fetching drive list, cant open /proc/partitions. Is this linux?");
			}
		}
	}

}

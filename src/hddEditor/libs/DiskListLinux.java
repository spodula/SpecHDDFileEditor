package hddEditor.libs;
/*-
 * Get a disk list along with details for Linux. This does not seen to require root privileges, although to 
 * actually access any of the returned devices, you probably will. 
 * This is a massive hack....
 * 
 *
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
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DiskListLinux {
	// list of partitions
	public RawDiskItem disks[];

	/**
	 * Constructor, load the drives
	 */
	public DiskListLinux(Languages lang) {
		if (!System.getProperty("os.name").toUpperCase().contains("LINUX")) {
			String err = String.format(lang.Msg(Languages.MSG_NOTLINUX), System.getProperty("os.name"));
			System.err.println(err);
			this.disks = new RawDiskItem[0];
		} else {

			ArrayList<RawDiskItem> drives = new ArrayList<RawDiskItem>();
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
							// remove extraneous spaces
							text = text.trim();
							while (text.contains("  ")) {
								text = text.replace("  ", " ");
							}
							if (!text.isBlank() && !text.contains("major")) {
								String cols[] = text.split(" ");
								RawDiskItem pi = new RawDiskItem();
								pi.name = cols[3];
								pi.dets = new File("/dev", pi.name);

								boolean ParentPartFound = false;
								for (RawDiskItem p : drives) {
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
					disks = drives.toArray(new RawDiskItem[drives.size()]);

					// try to populate more fieldss
					for (RawDiskItem p : disks) {
						// model
						if (new File("/sys/block/" + p.name + "/device").exists()) {
							byte data[] = GeneralUtils.ReadFileIntoArray("/sys/block/" + p.name + "/device/model");
							p.model = new String(data).trim();
							// vendor
							data = GeneralUtils.ReadFileIntoArray("/sys/block/" + p.name + "/device/vendor");
							p.Vendor = new String(data).trim();
							// size
							data = GeneralUtils.ReadFileIntoArray("/sys/block/" + p.name + "/size");
							p.realsz = Long.valueOf(new String(data).trim()) * 512;

							// logical block size. Note, this sometimes had a header, so just extract the
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
							p.BlockSize = Integer.parseInt(NumbersOnly);

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

							if (GeneralUtils.IsLinuxRoot()) {
								p = GetCHSLinux(p);
							}
						}
					}
				} catch (IOException e) {
					System.out.println(e.getMessage());
					System.out.println(lang.Msg(Languages.MSG_ERRNOPROCPART));
				} finally {
					try {
						br.close();
					} catch (IOException e) {
					}
				}
			} catch (FileNotFoundException e) {
				System.out.println(lang.Msg(Languages.MSG_ERRNOPROCPART));
			}
		}
		for (RawDiskItem d : disks) {
			System.out.println(d);
		}

	}

	/**
	 * This will only run under root. It requires hdparm. What a hack.
	 */
	private RawDiskItem GetCHSLinux(RawDiskItem rdi) {
		// Excecute the wmic command to get details
		String output[] = null;
		try {
			String line;
			Process p = Runtime.getRuntime().exec(new String[] { "sudo", "hdparm", "-I", "/dev/" + rdi.name });
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
				for (String s : al) {
					System.out.println(s);
				}
			} finally {
				brInput.close();
				brError.close();
			}
		} catch (Exception err) {
			err.printStackTrace();
		}

		for (String line : output) {
			line = line.trim();
			line = line.replace("\t", " ");
			while (line.contains("  ")) {
				line = line.replace("  ", " ");
			}
			String str[] = line.split(" ");
			try {
				if (str[0].toLowerCase().startsWith("cylinders")) {
					rdi.Cyls = Integer.valueOf(str[1]);
				}
				if (str[0].toLowerCase().startsWith("heads")) {
					rdi.Heads = Integer.valueOf(str[1]);
				}
				if (str[0].toLowerCase().startsWith("sectors")) {
					rdi.Sectors = Integer.valueOf(str[1]);
				}
			} catch (ArrayIndexOutOfBoundsException e) {

			}
		}
		return (rdi);
	}

	public static void main(String args[]) {
		DiskListLinux dll = new DiskListLinux(new Languages());
		for (RawDiskItem d : dll.disks) {
			System.out.println(d);
		}
	}

}

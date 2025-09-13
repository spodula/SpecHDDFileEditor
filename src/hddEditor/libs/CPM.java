package hddEditor.libs;
/**
 * CPM Specific data and functions.
 * 
 */


public class CPM {
	public static int[] AMSHDRDescriptions = {
			Languages.MSG_CPMDISKTYPE,
			Languages.MSG_CPMSIDENESS,
			Languages.MSG_CPMTPS,
			Languages.MSG_CPMSPT,
			Languages.MSG_CPMSSHIFT,
			Languages.MSG_CPMRESERVED,
			Languages.MSG_CPMBLOCKSZS,
			Languages.MSG_CPMDIRBLOCKS,
			Languages.MSG_CPMRWGAP,
			Languages.MSG_FORMATGAP
	};

	
	
	/**
	 * Create a filename that should work in CPM
	 * @param s
	 * @return
	 */
	public static String FixFullName(String s) {
		if (s.trim().isBlank()) {
			s = "UNNAMED";
		}
		String fname = s.toUpperCase();
		String filename = "";
		String extension = "";
		if (fname.contains(".")) {
			int i = fname.lastIndexOf(".");
			extension = fname.substring(i + 1);
			filename = fname.substring(0, i);
		} else {
			filename = fname;
		}
		filename = filename + "        ";
		filename = FixFilePart(filename.substring(0, 8).trim());

		extension = extension + "   ";
		extension = FixFilePart(extension.substring(0, 3).trim());
		// NameOnDisk
		return(filename + "." + extension);
	}
	
	/**
	 * Ensure the part of the filename is valid for CPM
	 * 
	 * @param s
	 * @return
	 */
	public static String FixFilePart(String s) {
		char ss[] = s.toUpperCase().toCharArray();
		for (int i = 0; i < ss.length; i++) {
			if (!CharIsCPMValid(ss[i])) {
				ss[i] = '_';
			}
		}
		return (String.copyValueOf(ss));
	}

	/**
	 * Check for a valid character for a CPM file.
	 * @param c
	 * @return
	 */
	
	public static String Validchars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\"#$'@^_{}~`_";
	
	public static boolean CharIsCPMValid(char c) {
		for(int j=0;j<Validchars.length();j++) {
			if (Validchars.charAt(j) == c) {
				return(true);
			}
		}
		return (false);
	}

}

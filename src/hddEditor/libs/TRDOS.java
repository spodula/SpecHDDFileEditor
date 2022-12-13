package hddEditor.libs;

public class TRDOS {
	/**
	 * Create a filename that should work in CPM
	 * 
	 * @param s
	 * @return
	 */
	public static String FixFullName(String s) {
		if (s.length() > 8) {
			s = s.substring(0,8);
		}
		char ss[] = s.toCharArray();
		for (int i = 0; i < ss.length; i++) {
			if (!CharIsTRDOSValid(ss[i])) {
				ss[i] = '_';
			}
		}
		return (String.copyValueOf(ss));
	}

	/**
	 * Check for a valid character for a CPM file.
	 * 
	 * @param c
	 * @return
	 */
	public static boolean CharIsTRDOSValid(char c) {
		boolean result = true;
		if ((c == '@') || (c > 0x126) || (c < 0x21)) {
			result = false;
		}
		return (result);
	}
}

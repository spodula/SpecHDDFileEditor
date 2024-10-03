package hddEditor.libs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.SystemPartition;

public class GeneralUtils {
	public static final int EXPORT_TYPE_RAW = 0;
	public static final int EXPORT_TYPE_TXT = 1;
	public static final int EXPORT_TYPE_HEX = 2;
	public static final int EXPORT_TYPE_ASM = 3;
	public static final int EXPORT_TYPE_CSV = 4;
	public static final int EXPORT_TYPE_PNG = 5;
	public static final int EXPORT_TYPE_GIF = 6;
	public static final int EXPORT_TYPE_JPG = 7;
	public static final int EXPORT_TYPE_RAWANDHEADER = 8;

	public static String MasterList[] = { "Raw", "Text", "Hex", "Assembly", "CSV", "PNG", "GIF", "JPEG", "Raw+Header" };

	/**
	 * Get size as either k or m depending on size.
	 * 
	 * @return
	 */
	public static String GetSizeAsString(long size) {
		if (size < 1024) {
			return (String.format("%3d", size) + "b");
		}
		size = size / 1024;
		if (size < 1024) {
			return (String.format("%3d", size) + "Kb");
		}
		size = size / 1024;
		if (size < 1024) {
			return (String.format("%3d", size) + "Mb");
		}
		size = size / 1024;
		if (size < 1024) {
			return (String.format("%3d", size) + "Gb");
		}

		size = size / 1024;
		return (String.format("%3d", size) + "Tb");
	}

	/**
	 * Read a given file to a byte array.
	 * 
	 * @param filename
	 * @return
	 */
	public static byte[] ReadFileIntoArray(String filename) {
		long filesize = new File(filename).length();
		byte result[] = new byte[(int) filesize];
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(filename);
			try {
				inputStream.read(result);
				inputStream.close();
			} finally {

			}
			return (result);
		} catch (IOException e) {
			System.err.println("IO " + "Error in GeneralUtils.ReadFileIntoArray: " + e.getMessage());
			e.printStackTrace();
		}
		return (null);
	}

	/**
	 * Write the given byte block to the given filename This function reports but
	 * otherwise eats any errors.
	 * 
	 * @param data
	 * @param filename
	 */
	public static void WriteBlockToDisk(byte[] data, String filename) {
		WriteBlockToDisk(data, new File(filename));
	}

	public static void WriteBlockToDisk(byte[] data, File filename) {
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(filename);
			try {
				outputStream.write(data);
			} finally {
				outputStream.close();
			}
		} catch (FileNotFoundException e) {
			System.err.println("File not found Error in GeneralUtils.WriteBlockToDisk: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IO " + "Error in GeneralUtils.WriteBlockToDisk: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Return a given data block as an ascii hexdump.
	 * 
	 * @param data
	 * @param start
	 * @param length
	 * @return
	 */
	public static String HexDump(byte[] data, int start, int length, int Displacement) {
		StringBuilder sb = new StringBuilder();
		String cr = System.lineSeparator();

		char chars[] = new char[16];
		for (int s = 0; s < chars.length; s++) {
			chars[s] = 0x20;
		}
		int byteindex = 0;
		sb.append(String.format("%08X ", start + Displacement));
		for (int ptr = 0; ptr < length; ptr++) {
			if ((byteindex) == 16) {
				sb.append(" ");
				sb.append(new String(chars));
				sb.append(cr);
				for (int s = 0; s < chars.length; s++) {
					chars[s] = 0x20;
				}
				byteindex = 0;
				sb.append(String.format("%08X ", start + Displacement));
			}
			byte dd = data[start++];
			int xi = (int) (dd & 0xff);

			sb.append(String.format("%02X ", xi));
			if (dd >= 32 && dd <= 127) {
				chars[byteindex++] = (char) dd;
			} else {
				chars[byteindex++] = '.';
			}
		}
		sb.append(" ");
		sb.append(new String(chars));
		sb.append(cr);

		return (sb.toString());
	}

	/**
	 * Pad a string to a given length
	 * 
	 * @param s
	 * @param i
	 * @return
	 */
	public static String PadTo(String s, int i) {
		while (s.length() < i) {
			s = s + " ";
		}
		return (s);
	}

	/**
	 * Test function used for dumping partition list given the system partition.
	 * 
	 * @param Sysp
	 */
	public static void DumpPartitionList(SystemPartition Sysp) {
		for (IDEDosPartition p : Sysp.partitions) {
			if (p.GetPartType() > 0) {
				String result = p.DirentNum + ": " + GeneralUtils.PadTo(p.GetName(), 17);
				result = result + GeneralUtils.PadTo(PLUSIDEDOS.GetTypeAsString(p.GetPartType()), 8);
				result = result + String.format("%4d/%2d - %4d/%2d + %5d  ", p.GetStartCyl(), p.GetStartHead(),
						p.GetEndCyl(), p.GetEndHead(), p.GetEndSector());
				result = result + GeneralUtils.GetSizeAsString(p.GetSizeK() * 1024);
				result = result + " " + p.getClass().getName();
				System.out.println(result);
			}
		}
	}

	/*
	 * Read the first <numbytes> from the given file.
	 */
	public static byte[] ReadNBytes(File filename, int numbytes) throws IOException {
		int length = Math.min(numbytes, (int) filename.length());
		byte result[] = new byte[length];
		FileInputStream fis = new FileInputStream(filename);
		try {
			fis.read(result);
		} finally {
			fis.close();
		}
		return (result);
	}

	/**
	 * Split a string handling quotes. from:
	 * http://www.java2s.com/example/java-utility-method/string-split-by-quote/splithandlequotes-string-s-1ca04.html
	 * 
	 * @param s
	 * @return
	 */
	public static String[] splitHandleQuotes(String s) {
		ArrayList<String> results = new ArrayList<String>();
		char quoteChar = '"';
		char escapeChar = '\\';

		StringBuffer current = new StringBuffer("");
		boolean inQuotation = false;
		boolean escaping = false;

		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (escaping) {
				if (c == quoteChar)
					current.append(quoteChar);
				else {
					current.append(escapeChar);
					current.append(quoteChar);
				}
				escaping = false;
			} else {
				if (c == quoteChar) {
					inQuotation = !inQuotation;
				} else if (c == escapeChar) {
					escaping = true;
				} else if (!inQuotation)
					if (c == ' ' || c == '\t') {
						results.add(new String(current));
						current = new StringBuffer("");
					} else
						current.append(s.charAt(i));
				else
					current.append(s.charAt(i));
			}
		}

		results.add(new String(current));

		return (results.toArray(new String[0]));
	}
	
	/**
	 * This is a very hacky method to figure out if we are running as root or not.
	 * @return
	 */
	public static boolean IsLinuxRoot() {
		//TODO: Fix getting if we are root using a less hacky method.
		return(System.getProperty("user.home").toLowerCase().equals("/root"));
	}
	
}

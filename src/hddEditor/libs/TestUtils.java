package hddEditor.libs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.SystemPartition;

public class TestUtils {
	
	/**
	 * Get size as either k or m depending on size.
	 * @return
	 */
	public static String GetSizeAsString(int size) {
		if (size < 1024) {
			return(String.format("%3d", size)+"b");
		} 
		size = size / 1024;
		if (size < 1024) {
			return(String.format("%3d", size)+"Kb");
		} 
		size = size / 1024;
		if (size < 1024) {
			return(String.format("%3d", size)+"Mb");
		} 
		size = size / 1024;
		return(String.format("%3d", size)+"Gb");
		
	}
	
	/**
	 * Read a given file to a byte array.
	 * 
	 * @param filename
	 * @return
	 */
	public static byte[] ReadFileIntoArray(String filename) {
		long filesize = new File(filename).length();
		byte result[] = new byte[(int)filesize];
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(filename);
			try {
				inputStream.read(result);
				inputStream.close();
			} finally {
				
			}
			return(result);
		} catch (IOException e) {
			System.err.println("IO "
					+ "Error in TestUtils.ReadFileIntoArray: "+e.getMessage());			
			e.printStackTrace();
		} 
		return(null);

	}
	

	/**
	 * Write the given byte block to the given filename
	 * This function reports but otherwise eats any errors.
	 * 
	 * @param data
	 * @param filename
	 */
	public static void WriteBlockToDisk(byte[] data, String filename) {
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(filename);
			try {
				outputStream.write(data);
			} finally {
				outputStream.close();
			} 
		} catch (FileNotFoundException e) {
			System.err.println("File not found Error in TestUtils.WriteBlockToDisk: "+e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IO "
					+ "Error in TestUtils.WriteBlockToDisk: "+e.getMessage());			
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
	public static String HexDump(byte[] data, int start, int length) {
		String result = "";
		
		String s="";
		for(int ptr = 0;ptr < length;ptr++  ) {
			if ((ptr % 16)==0) {
				if (!s.isBlank()) {
					result = result + " "+s+"\n";
					s = "";
				} 
				result = result + String.format("%08X ", start);
			}
			byte dd = data[start++];
			int xi = (int)(dd & 0xff);
			
			result = result + String.format("%02X ", xi);
			if (dd >= 32 && dd<=127) {
				s = s + (char)dd;
			} else {
				s = s + ".";
			}
		}
		result = result + " "+s+"\n";
		return(result);		
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
		return(s);
	}
	
	/**
	 * Test function used for dumping partition list given the system partition.
	 * @param Sysp
	 */
	public static void DumpPartitionList(SystemPartition Sysp) {
		for (IDEDosPartition p : Sysp.partitions) {
			if (p.GetPartType() > 0) {
				String result = p.DirentNum + ": " + TestUtils.PadTo(p.GetName(), 17);
				result = result + TestUtils.PadTo(p.GetTypeAsString(), 8);
				result = result + String.format("%4d/%2d - %4d/%2d + %5d  ", p.GetStartCyl(), p.GetStartHead(),
						p.GetEndCyl(), p.GetEndHead(), p.GetEndSector());
				result = result + TestUtils.GetSizeAsString(p.GetSizeK() * 1024);
				result = result + " " + p.getClass().getName();
				System.out.println(result);
			}
		}
	}

	
}

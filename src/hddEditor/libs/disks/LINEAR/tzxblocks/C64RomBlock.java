package hddEditor.libs.disks.LINEAR.tzxblocks;
/**
 * This is basically a placeholder. It doesn't parse any useful data.
 */

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class C64RomBlock  extends GenericUnknownBlock {
	
	public C64RomBlock(RandomAccessFile fs) throws IOException {
		super(fs,TZX.TZX_C64ROMBLOCK, "C64 Rom block");		
	}

}

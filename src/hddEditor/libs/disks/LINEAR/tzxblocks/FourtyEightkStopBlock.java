package hddEditor.libs.disks.LINEAR.tzxblocks;

import java.io.IOException;
import java.io.RandomAccessFile;

import hddEditor.libs.TZX;

public class FourtyEightkStopBlock extends GenericUnknownBlock {
	public FourtyEightkStopBlock(RandomAccessFile fs) throws IOException {
		super(fs,TZX.TZX_STOP48);
	}	
}

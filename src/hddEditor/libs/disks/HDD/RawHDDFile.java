package hddEditor.libs.disks.HDD;
//TODO: Drag/drop doesn't work on Windows?

/**
 * This is a wrapper around a file with no header acting as a disk. It can be any format.
 * 
 * Note, the CHS values are not set at this level usually.
 * It basically provides some low level disk access, which can be overridden by superclasses
 * eg, to handle 8/16 bit access.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.ui.partitionPages.dialogs.ProgesssForm;

public class RawHDDFile implements HardDisk {
	// File handle
	private RandomAccessFile inFile = null;
	private FileInputStream inFileFIS = null;

	public int DiskBlockSize = 4096;

	// filename of the currently open file
	protected File file;
	// default sector size
	protected int SectorSize = 512;
	// disk size in bytes
	protected long FileSize = 0;

	// CHS information for inheriting objects to populate
	protected int NumCylinders = 0;
	protected int NumHeads = 0;
	protected int NumSectors = 0;

	// Simple one sector cache
	private byte[] cache;
	private long cachedSector = -1;

	long LastModified;

	/**
	 * Get and set the filename
	 */
	public String GetFilename() {
		return (file.getAbsolutePath());
	}

	public void SetFilename(String filename) {
		this.file = new File(filename);
	}

	/**
	 * Get and set the sector size
	 */
	public int GetSectorSize() {
		return (SectorSize);
	}

	public void SetSectorSize(int sz) {
		this.SectorSize = sz;
	}

	/**
	 * Get and set the number of cylinders
	 */
	public int GetNumCylinders() {
		return (NumCylinders);
	}

	public void SetNumCylinders(int sz) {
		this.NumCylinders = sz;
	}

	/**
	 * Get the file size
	 */
	public long GetFileSize() {
		return (FileSize);
	}

	/**
	 * Get and set the number of heads
	 */
	public int GetNumHeads() {
		return (NumHeads);
	}

	public void SetNumHeads(int sz) {
		this.NumHeads = sz;
	}

	/**
	 * Get and set the number of sectors
	 */
	public int GetNumSectors() {
		return (NumSectors);
	}

	public void SetNumSectors(int sz) {
		this.NumSectors = sz;
	}

	/**
	 * Constructor specifying file to load
	 * 
	 * @param filename
	 * @throws FileNotFoundException
	 */
	public RawHDDFile(File file,int c,int h, int s) throws FileNotFoundException {
		if (file.getAbsolutePath().startsWith("\\\\.\\PHYSICALDRIVE")) {
			inFileFIS = new FileInputStream(file);
			inFile = null;
		} else {
			inFile = new RandomAccessFile(file, "rw");
			inFileFIS = null;
		}
		if (c!=0) this.SetNumCylinders(c);
		if (h!=0) this.SetNumHeads(h);
		if (s!=0) this.SetNumSectors(s);
		
		this.file = file;
		FileSize = file.length();
		UpdateLastModified();
	}

	/**
	 * Constructor creating an unopened file object
	 */
	public RawHDDFile() {
		super();
	}

	/**
	 * ToString overridden to provide useful debug information
	 */
	@Override
	public String toString() {
		String result = "Filename: " + file.getAbsolutePath();
		result = result + "\nLogical sectors: " + GetNumLogicalSectors();
		result = result + "\nCylinders: " + NumCylinders;
		result = result + "\nHeads: " + NumHeads;
		result = result + "\nSectors: " + NumSectors;
		result = result + "\nSector size: " + SectorSize + " bytes";
		result = result + "\nFile size: " + FileSize + " bytes";
		return (result);
	}

	/**
	 * Get the number of logical sectors by the disks size.
	 * 
	 * @return
	 * @throws IOException
	 */
	public long GetNumLogicalSectors() {
		long filesize = 0;
		try {
			filesize = Files.size(Paths.get(file.getAbsolutePath()));
		} catch (IOException e) {
			System.out.println("Failed to get filesize. " + e.getMessage());
		}
		long numsectors = filesize / SectorSize;
		return (numsectors);
	}

	/**
	 * Close the disk, even if it doesn't want to..
	 */
	public void close() {
		if (inFile != null) {
			try {
				inFile.close();
			} catch (IOException e) {
				System.out.println("Failed to close file " + file.getName() + " with error " + e.getMessage());
				e.printStackTrace();
			}
			inFile = null;
		}
		if (inFileFIS != null) {
			try {
				inFileFIS.close();
			} catch (IOException e) {
				System.out.println("Failed to close file " + file.getName() + " with error " + e.getMessage());
				e.printStackTrace();
			}
			inFileFIS = null;
		}
	}

	/**
	 * Returns TRUE if the file opened correctly.
	 * 
	 * @return
	 */
	public boolean IsOpen() {
		return ((inFile != null) || (inFileFIS != null));
	}

	/**
	 * Test harness
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		RawHDDFile h;
		try {
			h = new RawHDDFile(new File("/dev/sde"),0,0,0);
			System.out.println(h);
			h.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set data from the given logical sector with the specified data.
	 * 
	 * @param SectorNum
	 * @param result
	 * @throws IOException
	 */
	public void SetLogicalBlockFromSector(long SectorNum, byte result[]) throws IOException {
		cachedSector = -1;

		long location = SectorNum * SectorSize;

		if (inFile != null) {
			inFile.seek(location);
			inFile.write(result);
			cachedSector = -1;

			inFile.seek(location);
			inFile.read(result);
		}
		if (inFileFIS != null) {
			/*
			 * I cant get this to work writing disks. RandomAccessFile just doesnt work, and
			 * NIO Channels appear to fail if there isnt a filesystem on the disk?
			 * As such just leaving this code here for the moment. 
			 */
			throw new IOException("Cannot write to a raw device in Windows. Sorry.");
			
			//For block devices we have to read in multiples of block size and on block size boundaries.
/*			int StartInBuffer = (int) (location % DiskBlockSize);	//Displacement within a raw disk block.
			long ActualRdWrStart = location - StartInBuffer;		//Actual block start
			long ActualResultLength = result.length + StartInBuffer;			//block length as a multiple of DiskBlockSize
			ActualResultLength = ActualResultLength + (ActualResultLength % DiskBlockSize); 
			byte tmpbuffer[] = new byte[(int)ActualResultLength];

			inFileFIS.close();
			inFileFIS = new FileInputStream(file);
			inFileFIS.skip(ActualRdWrStart);
			inFileFIS.read(tmpbuffer);
			
			System.arraycopy(result, 0, tmpbuffer, StartInBuffer, result.length);
			inFileFIS.close();
			
			
			Path diskRoot = file.toPath();

			FileChannel fc = FileChannel.open( diskRoot, StandardOpenOption.READ,
			      StandardOpenOption.WRITE );

			ByteBuffer bb = ByteBuffer.allocate( tmpbuffer.length );
			fc.position(ActualRdWrStart);
			fc.write(bb);
			fc.close();
			
			inFileFIS = new FileInputStream(file); */
		}
		UpdateLastModified();
	}

	/**
	 * Get a byte array of the specified size from the given sector number
	 * 
	 * @param SectorNum
	 * @param sz
	 * @return
	 * @throws IOException
	 */
	public byte[] GetBytesStartingFromSector(long SectorNum, long sz) throws IOException {
		if ((cachedSector == SectorNum) && (cache.length == sz)) {
			return (cache);
		}

		// Note, this will restrict length for files of 128Mb.
		long asz = Math.min(sz, 1024 * 1024 * 128);
		byte result[] = new byte[(int) asz];
		
		long location = SectorNum * SectorSize;

		if (inFile != null) {
			inFile.seek(location);
			inFile.read(result);
		}
		if (inFileFIS != null) {
			//For block devices we have to read in multiples of block size and on block size boundaries.
			int StartInBuffer = (int) (location % DiskBlockSize);	//Displacement within a raw disk block.
			long ActualRdWrStart = location - StartInBuffer;		//Actual block start
			long ActualResultLength = asz + StartInBuffer;			//block length as a multiple of DiskBlockSize
			ActualResultLength = ActualResultLength + (ActualResultLength % DiskBlockSize); 
			byte tmpbuffer[] = new byte[(int)ActualResultLength];

			inFileFIS.close();
			inFileFIS = new FileInputStream(file);
			inFileFIS.skip(ActualRdWrStart);
			inFileFIS.read(tmpbuffer);
			
			System.arraycopy(tmpbuffer,StartInBuffer, result, 0, result.length);
		}

		cache = new byte[result.length];
		System.arraycopy(result, 0, cache, 0, cache.length);
		cachedSector = SectorNum;

		return (result);
	}

	/**
	 * Check to see if i can identify this file as one i can open... At this level,
	 * always false.
	 * 
	 * @param filename
	 */
	@Override
	public Boolean IsMyFileType(File filename) throws IOException {
		return (false);
	}

	/**
	 * What Media type is this?
	 */
	@Override
	public int GetMediaType() {
		return PLUSIDEDOS.MEDIATYPE_HDD;
	}

	/**
	 * Resize disk
	 * 
	 * @throws IOException
	 */
	@Override
	public void ResizeDisk(int NewCyls) throws IOException {
		Long newsize = ((long) NewCyls) * ((long) (GetNumHeads() * GetNumSectors() * GetSectorSize()));
		System.out.println(this.getClass().getName() + ": Resizing disk " + GetFilename() + " from " + GetFileSize()
				+ " (" + GeneralUtils.GetSizeAsString(GetFileSize()) + ") " + " to " + String.valueOf(newsize) + " ("
				+ GeneralUtils.GetSizeAsString(NewCyls) + ") ");
		if (inFile != null) {
			inFile.setLength(newsize);
			SetNumCylinders(NewCyls);
			UpdateLastModified();
		} else {
			System.err.println("Cannot resize physical devices.");
		}

	}

	/**
	 * Create a blank Hard disk file with a system partition
	 * 
	 * @param file         - File to create
	 * @param cyl          - Cylinders
	 * @param head         - Heads
	 * @param spt          - Sectors per track
	 * @param IsTarget8Bit - 8 or 16 bit file type
	 * @param pf           - Progress
	 * @return TRUE if creation successful
	 */
	public boolean CreateBlankRawDisk(File file, int cyl, int head, int spt, boolean IsTarget8Bit, ProgesssForm pf) {
		try {
			boolean result = false;
			System.out.println("Openning " + file.getName() + " for writing...");

			String s = "8-bit";
			if (!IsTarget8Bit) {
				s = "16-bit";
			}

			pf.Show("Creating file...", "Creating " + s + " Raw disk image \"" + file.getName() + "\"");
			try {
				int sectorSz = 512;
				if (IsTarget8Bit) {
					sectorSz = 256;
				}

				FileOutputStream TargetFile = new FileOutputStream(file);
				try {
					// Write out an IDEDOS header
					byte SysPart[] = PLUSIDEDOS.GetSystemPartition(cyl, head, spt, sectorSz, IsTarget8Bit);
					TargetFile.write(SysPart);

					// Write out the free space header
					byte FsPart[] = PLUSIDEDOS.GetFreeSpacePartition(0, 1, cyl, 1, sectorSz, IsTarget8Bit, head, spt);
					TargetFile.write(FsPart);

					/*
					 * Write a blank file for the rest.
					 */
					int NumLogicalSectors = (cyl * head * spt) - spt + 1;

					byte oneSector[] = new byte[512];
					pf.SetMax(NumLogicalSectors);

					boolean ActionCancelled = false;
					for (int i = 0; (i < NumLogicalSectors) && !ActionCancelled; i++) {
						TargetFile.write(oneSector);
						if (i % 2000 == 0) {
							pf.SetValue(i);
						}
						ActionCancelled = pf.IsCancelled();
					}

					System.out.println();
					if (ActionCancelled) {
						System.out.println("Cancelled");
						pf.setMessage("Cancelled - Flushing work already done...");
						result = false;
					} else {
						result = true;
					}
				} finally {
					TargetFile.close();
				}
				System.out.println("Creation finished.");
				result = true;
			} catch (FileNotFoundException e) {
				System.out.println("Cannot open file " + file.getName() + " for writing.");
				System.out.println(e.getMessage());
				result = false;
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Cannot write to file file " + file.getName());
				System.out.println(e.getMessage());
				result = false;
				e.printStackTrace();
			}
			return (result);

		} catch (Exception E) {
			System.out.println(E.getMessage());
			E.printStackTrace();
			return (false);
		}
	}

	/**
	 * Return if the disk is out of sync with the one on disk.
	 */
	@Override
	public boolean DiskOutOfDate() {
		if (inFile != null) {
			return (LastModified < file.lastModified());
		}
		return false;
	}

	/**
	 * Update the last modified flag.
	 */
	@Override
	public void UpdateLastModified() {
		if (inFile != null) {
			LastModified = file.lastModified();
		}
	}

}

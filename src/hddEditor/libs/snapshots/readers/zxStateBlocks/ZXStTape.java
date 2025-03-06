package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * UNTESTED
 * Implements the ZXSTTAPE block
 * https://www.spectaculator.com/docs/zx-state/cassette_recorder.shtml
 * State of the virtual tape recorder
 * 
 * $00..$03 	ID: "TAPE"  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTTAPE information====
 * $08..$09		Block number
 * $0A..$0B		Flags
 * $0C..$0F		Uncompressed size
 * $10..$13		Compressed size
 * $14..$23		File extension
 * $24..end		either the filename or the tape data
 * 
 */
public class ZXStTape extends GenericZXStateBlock {
	public static int ZXSTTP_EMBEDDED = 1;
	public static int ZXSTTP_COMPRESSED = 2;
	public static String DiskFlags[][] = { { "1", "ZXSTTP_EMBEDDED", "Disk image is embedded" },
			{ "2", "ZXSTTP_COMPRESSED", "Embedded disk image is compressed" } };
	
	public int CurrentBlockNo;
	public int Flags;
	public int UncompressedSize;
	public int CompressedSize;
	public String FileExtension;
	public byte CompressedData[];
	public byte UnCompressedData[];
	public String Filename;

	public ZXStTape(byte[] rawdata, int start) {
		super(rawdata, start);
		CurrentBlockNo = rawword(0x08);
		Flags = rawword(0x0A);
		UncompressedSize = rawDword(0x0C);
		CompressedSize = rawDword(0x10);
		
		byte data[] = new byte[raw.length - 0x24];
		System.arraycopy(raw, 0x24, data, 0, data.length);
		//Pre-set the variables..
		Filename = null;
		FileExtension = null;
		UnCompressedData = null;
		FileExtension = null;
		
		if ((Flags & ZXSTTP_EMBEDDED) == ZXSTTP_EMBEDDED) {
			/*
			 * Tape is embedded in the file.
			 */
			//File Extension
			byte ext[] = new byte[16];
			System.arraycopy(raw, 0x14, ext, 0, 0x10);
			FileExtension = new String(ext);
			
			//Extract data
			CompressedData = data;
			
			if ((Flags & ZXSTTP_COMPRESSED) == ZXSTTP_COMPRESSED) {
				UnCompressedData = zLibDecompressData(CompressedData, UncompressedSize);
			} else {
				UnCompressedData = CompressedData;
			}
		} else {
			/*
			 * Tape filename is embedded
			 */
			Filename = new String(data).toString();	
		}
	}
	
	
	/**
	 * Add in the ZXSTPLUSDDISK values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Block num:" + CurrentBlockNo;
		result = result + " UncompressedLen:" + UncompressedSize; 
		result = result + " CompressedLen:"+CompressedSize;
		result = result + " Flags:" + GetFlags() + " (" + Flags + ")";
		if (FileExtension != null)
			result = result + " Ext:"+FileExtension;
		if (Filename != null) {
			result = result + " Filename:" + Filename;
		}
		return (result.trim());
	}

	/**
	 * 
	 * @return
	 */
	public String GetFlags() {
		return (GetFlagsFromArray(DiskFlags, Flags, 2));
	}
	
}

package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * UNTESTED
 * Implements the ZXSTOPUS block
 * https://www.spectaculator.com/docs/zx-state/opus.shtml
 * Discovery disk interface by Opus Supplies.
 * 
 * $00..$03 	ID: "OPUS"  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTOPUS information====
 * $08..$0B		Flags
 * $0C..$0F		Compressed Ram size in bytes
 * $10..$13		Compressed Rom size in bytes
 * $14			PIA Control register A
 * $15			PIA Periph reg A
 * $16			PIA Data direction register A
 * $17			PIA Control Register B
 * $18			PIA Periph reg B
 * $19			PIA Data direction register B	
 * $1A			Number drives
 * $1B			Track
 * $1C			Sector
 * $1D			Data
 * $1E			Status
 * $1F..end		Ram
 */
public class ZXStOpus extends GenericZXStateBlock {
	public static int ZXSTOPUSF_COMPRESSED=4;
	public static int ZXSTOPUSF_CUSTOMROM=8;
	
	public static String OpusFlags[][] = {
			{"1","ZXSTOPUSF_PAGED","Rom Paged"},
			{"2","ZXSTOPUSF_COMPRESSED","Ram (And custom rom if present) is compressed"},
			{"4","ZXSTOPUSF_SEEKLOWER","WD1770 seek direction is down"},
			{"8","ZXSTOPUSF_CUSTOMROM","Custom rom installed"}
	};
	
	public int flags;
	public int CompressedRamSize;
	public int CompressedRomSize;
	public int PIACtrlA;
	public int PIAPeriphA;
	public int PIADDRA;
	public int PIACtrlB;
	public int PIAPeriphB;
	public int PIADDRB;
	public int NumDrives;
	public int TrackReg;
	public int SectorReg;
	public int DataReg;
	public int StatusReg;

	public byte CompressedRam[];
	public byte UnCompressedRam[];
	public byte UnCompressedRom[];
	
	public ZXStOpus(byte[] rawdata, int start) {
		super(rawdata, start);
		flags = rawDword(0x08);
		CompressedRamSize = rawDword(0x0C);
		CompressedRomSize = rawDword(0x10);
		
		PIACtrlA = rawbyte(0x14);
		PIAPeriphA = rawbyte(0x15);
		PIADDRA = rawbyte(0x16);
		PIACtrlB = rawbyte(0x17);
		PIAPeriphB = rawbyte(0x18);
		PIADDRB = rawbyte(0x19);
		NumDrives = rawbyte(0x1A);
		TrackReg = rawbyte(0x1B);
		SectorReg = rawbyte(0x1C);
		DataReg = rawbyte(0x1D);
		StatusReg = rawbyte(0x1E);
		
		byte Data[] = new byte[CompressedRamSize];
		System.arraycopy(rawdata,0x1F, Data,0,CompressedRamSize);
		CompressedRam = Data;
		if ((flags & ZXSTOPUSF_COMPRESSED) == ZXSTOPUSF_COMPRESSED) {
			UnCompressedRam = zLibDecompressData(Data, 2048); 
		} else {
			UnCompressedRam = Data; 
		}
		
		if ((flags & ZXSTOPUSF_CUSTOMROM) == ZXSTOPUSF_CUSTOMROM) {
			Data = new byte[CompressedRomSize];
			System.arraycopy(rawdata,0x1F+CompressedRamSize, Data,0,CompressedRomSize);
			UnCompressedRom = Data;
		} else {
			UnCompressedRom = null;
		}
	}
	
	/**
	 * Add in the ZXSTOPUS values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result +  " Flags:" + FlagsAsString()+" ("+flags+")" +
				 "  Ramsize:" + CompressedRamSize +
				 "  Romsize:" + CompressedRomSize +
				 "  PIAA CTL:" + PIACtrlA +
				 "  PIAA PERIP:" + PIAPeriphA +
				 "  PIAA DDR:" + PIADDRA +
				 "  PIAB CTL:" + PIACtrlB +
				 "  PIAB PERPI:" + PIAPeriphB +
				 "  PIAB DDR:" + PIADDRB +
				 "  Num drives:" + NumDrives +
				 "  Track:" + TrackReg +
				 "  Sector:" + SectorReg +
				 "  Data:" + DataReg +
				 "  Status:" + StatusReg;
		
		return (result);
	}
	
	public String FlagsAsString() {
		return(GetFlagsFromArray(OpusFlags, flags, 2));
	}
	

}

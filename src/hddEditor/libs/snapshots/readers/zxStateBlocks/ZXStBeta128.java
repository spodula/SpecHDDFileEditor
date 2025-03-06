package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * UNTESTED
 * Implements the ZXSTBETA128 block
 * https://www.spectaculator.com/docs/zx-state/beta128.shtml
 * Beta 128 details
 * 
 * $00..$03 	ID: "B128" ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) Should always be 2 
 * ====start of ZXSTBETA128 information====
 * $08..$0B		Flags
 * $0C			Num drives
 * $0D			last value written to the System Reg
 * $0E			last value written to the Track register
 * $0F			last value written to the Sector register
 * $10			last value written to the Data register
 * $11			Current status register contents
 * $[12]..		If present the Any custom rom data.
 */

public class ZXStBeta128 extends GenericZXStateBlock {
	public static int ZXSTBETAF_CUSTOMROM = 0x02;
	public static int ZXSTBETAF_COMPRESSED = 0x20;
	
	public static String BetaFlags[][] = {
			{"1", "ZXSTBETAF_CONNECTED","The interface is connected and enabled."},
			{"2", "ZXSTBETAF_CUSTOMROM","A custom TR-DOS ROM is installed."},
			{"4", "ZXSTBETAF_PAGED","The TR-DOS ROM is currently paged in."},
			{"8", "ZXSTBETAF_AUTOBOOT","The Beta 128's Auto boot feature is enabled"},
			{"16","ZXSTBETAF_SEEKLOWER","FDC Seek direction is down"},
			{"32","ZXSTBETAF_COMPRESSED","If a custom ROM is embedded in this block, it has been compressed"}
	};

	public int flags;
	public int NumDrives;
	public int Sysreg;
	public int TrackReg;
	public int SectorReg;
	public int DataReg;
	public int StatusReg;
	public byte RomData[];
	public byte RomDataUnCompressed[];
	
	public ZXStBeta128(byte[] rawdata, int start) {
		super(rawdata, start);
		flags = rawDword(0x08);
		NumDrives = rawbyte(0x0C);
		Sysreg = rawbyte(0x0D);
		TrackReg = rawbyte(0x0E);
		SectorReg = rawbyte(0x0F);
		DataReg = rawbyte(0x10);
		StatusReg = rawbyte(0x11);
		
		if ((flags & ZXSTBETAF_CUSTOMROM) == ZXSTBETAF_CUSTOMROM) {
			RomData = new byte[raw.length - 0x12];
			if ((flags & ZXSTBETAF_COMPRESSED) == ZXSTBETAF_COMPRESSED) {
				RomDataUnCompressed = zLibDecompressData(RomData,0x4000);
			} else {
				RomDataUnCompressed = RomData;
			}
			
		} else {
			RomData = null;
			RomDataUnCompressed = null;
		}
		
		
	}
	
	/**
	 * Add in the ZXSTBETA128 values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Drives:"+NumDrives+" Sys:"+Sysreg+" tr:"+TrackReg+" S:"+SectorReg+" D:"+DataReg+" Status:"+StatusReg+" Flags: "+flags+" "+FlagsAsString();
	
		return (result);
	}
	
	public String FlagsAsString() {
		return(GetFlagsFromArray(BetaFlags, flags, 2));
	}

	
}

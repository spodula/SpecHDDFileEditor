package hddEditor.libs.snapshots.readers.zxStateBlocks;
/**
 * Implements the ZXSTIF1 block
 * https://www.spectaculator.com/docs/zx-state/interface1.shtml
 * This contains the program that created this file.
 * 
 * $00..$03  ID: "IF1",$0 		) Decoded by the parent class. 
 * $04..$07  size: LSB..MSB		)
 * ====start of  ZXSTIF1 information====
 * $08..$09		Flags
 * $0A			NumMicrodrives
 * $0B..$0D		Reserved
 * $0E..$11		Reserved
 * $12..$13		ROM size
 * $14....		Rom data 
 */
public class ZXStIF1 extends GenericZXStateBlock {
	public static int ZXSTIF1F_COMPRESSED=4;
	public static String IF1Flags[][] = {
			{"1"  ,"ZXSTIF1F_ENABLED","IF/1 emulation enabled"},
			{"2"  ,"ZXSTIF1F_COMPRESSED","Rom is compressd"},
			{"4"  ,"ZXSTIF1F_PAGED","Rom paged"}
	};
	
	public int flags;
	public int numMicrodrives;
	public int romSize;
	public byte RawRom[];
	public byte UncompressedRom[];
	
	public ZXStIF1(byte[] rawdata, int start) {
		super(rawdata, start);
		
		flags = rawword(0x08);
		numMicrodrives = rawbyte(0x0A);
		romSize = rawword(0x12);
		if (romSize==0) {
			RawRom = null;
			UncompressedRom = null;
		} else {
			byte data[] = new byte[raw.length-0x14];
			System.arraycopy(raw, 0x0F, data, 0, data.length);
			RawRom = data;
			
			if ((flags & ZXSTIF1F_COMPRESSED) == ZXSTIF1F_COMPRESSED ) {
				UncompressedRom = zLibDecompressData(data, romSize);
			} else {
				UncompressedRom = data;
			}
		}
	}
	/**
	 * Add in the ZXSTIF1 specific items
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Num Mdrv:"+numMicrodrives+" Flags:"+GetFlags()+" ("+flags+") Romsize:"+romSize;
		return (result);
	}
	
	public String GetFlags() {
		return(GetFlagsFromArray(IF1Flags,flags,1));
	}
	

}

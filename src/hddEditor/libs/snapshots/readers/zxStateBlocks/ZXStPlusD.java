package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * UNTESTED...
 * Implements the ZXSTPLUSD block
 * https://www.spectaculator.com/docs/zx-state/plusd.shtml
 * This contains the state of the +d Interface
 * 
 * $00..$03 	ID: "PLSD"  ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) 
 * ====start of ZXSTPLUSD information====
 * $08..$0B		Flags
 * $0C..$0F		Compressed Ram size
 * $10..$13		Compressed Rom size
 * $14			Rom type	
 * $15			Control Register
 * $16			Num Drives
 * $17			Track register
 * $18			Sector register
 * $19			Data register
 * $1A			Status register
 * $1B..end		Compressed Ram followed by the Compressed Rom if present.
 */

public class ZXStPlusD extends GenericZXStateBlock {
	public static int ZXSTPLUSDF_COMPRESSED=2;
	public static String DiskFlags[][] = {
			{"1","ZXSTPLUSDF_PAGED","Currently paged in"},
			{"2","ZXSTPLUSDF_COMPRESSED","Ram is compressed"},
			{"4","ZXSTPLUSDF_SEEKLOWER","Current seek direction Down"}
	};
	
	public static String aRomType[][] = {
			{"0","ZXSTPDRT_GDOS","The standard G+DOS ROM (Version 1.A)"},
			{"1","ZXSTPDRT_UNIDOS","Uni-DOS ROM"},
			{"2","ZXSTPDRT_CUSTOM","Custom Rom"}
	};
	
	public int Flags;
	public int RamSize;
	public int RomSize;
	public int RomType;
	public int ControlReg;
	public int NumDrives;
	public int TrackReg;
	public int SectorReg;
	public int DataReg;
	public int StatusReg;
	
	public byte CompressedRam[];
	public byte UnCompressedRam[];
	public byte CompressedRom[];
	public byte UnCompressedRom[];
	
	public ZXStPlusD(byte[] rawdata, int start) {
		super(rawdata, start);
		
		Flags = rawDword(0x08);
		RamSize = rawDword(0x0C);
		RomSize = rawDword(0x10);
		
		RomType = rawbyte(0x14);
		ControlReg = rawbyte(0x15);
		NumDrives = rawbyte(0x16);
		TrackReg = rawbyte(0x17);
		SectorReg = rawbyte(0x18);
		DataReg = rawbyte(0x19);
		StatusReg = rawbyte(0x1A);

		byte data[] = new byte[raw.length-0x1B];
		System.arraycopy(raw, 0x1B, data, 0, data.length);

		CompressedRam = new byte[RamSize];
		System.arraycopy(data, 0x00, CompressedRam, 0, CompressedRam.length);
		
		if ((Flags & ZXSTPLUSDF_COMPRESSED) == ZXSTPLUSDF_COMPRESSED) {
			UnCompressedRam = zLibDecompressData(CompressedRam, 8192);
		} else {
			UnCompressedRam = CompressedRam;
		}
		if (RomSize > 0) {
			CompressedRom = new byte[RomSize];
			System.arraycopy(data, RamSize, CompressedRom, 0, CompressedRom.length);
			if ((Flags & ZXSTPLUSDF_COMPRESSED) == ZXSTPLUSDF_COMPRESSED) {
				UnCompressedRom = zLibDecompressData(CompressedRom, 8192);
			} else {
				UnCompressedRom = CompressedRom;
			}
		}
	}
	
	
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " flags:"+GetFlags()+"  ("+Flags+")";
		result = result + " CRamSize:"+RamSize;
		result = result + " CRomSize:"+RomSize;
		result = result + " RomType:"+GetRomType() +" ("+RomType+")";
		result = result + " CtrlReg:"+ControlReg;
		result = result + " NumDrives:"+NumDrives;
		result = result + " Track:"+TrackReg;
		result = result + " Sector:"+SectorReg;
		result = result + " Data:"+DataReg;
		result = result + " Status:"+StatusReg;
		
		
		return (result.trim());
	}
	
	public String GetFlags() {
		return(GetFlagsFromArray(DiskFlags, Flags,2));
	}
	
	public String GetRomType() {
		return (GetNameFromArray(aRomType, String.valueOf(RomType), 0, 2));
	}

}

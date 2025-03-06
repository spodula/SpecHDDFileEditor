package hddEditor.libs.snapshots.readers.zxStateBlocks;
/**
 * UNTESTED
 * Implements the ZXSTMULTIFACE block
 * https://www.spectaculator.com/docs/zx-state/multiface.shtml
 * The current mouse emulation state
 * 
 * $00..$03  	ID: "MFCE" 		) Decoded by the parent class. 
 * $04..$07  	size: LSB..MSB	)
 * ====start of  ZXSTMULTIFACE information====
 * $08			Model to use when emulating 16/48k spectrums
 * $09			Flags
 * $0A..End		Rom data..
 * 
 */
public class ZXStMultiface extends GenericZXStateBlock {
	public static String f48KModel[][] = {
			{"0","ZXSTMFM_1","Multiface 1"},
			{"1","ZXSTMFM_128","Multiface 128"},
	};
	
	public int ZXSTMF_COMPRESSED = 2;
	public int ZXSTMF_16KRAMMODE = 32;
	
	public static String ZXCFFlags[][] = {
			{"1","ZXSTMF_PAGEDIN","upload jumper on the interface is enabled"},
			{"2","ZXSTMF_COMPRESSED","upload jumper on the interface is enabled"},
			{"4","ZXSTMF_SOFTWARELOCKOUT","upload jumper on the interface is enabled"},
			{"8","ZXSTMF_REDBUTTONDISABLED","upload jumper on the interface is enabled"},
			{"16","ZXSTMF_DISABLED","upload jumper on the interface is enabled"},
			{"32","ZXSTMF_16KRAMMODE","upload jumper on the interface is enabled"},
	};
	
	public int f48KModelEmu;
	public int flags;
	public byte RawRomData[];
	public byte UncompressedRawData[];
	
	public ZXStMultiface(byte[] rawdata, int start) {
		super(rawdata, start);
		f48KModelEmu = rawbyte(0x08);
		flags = rawbyte(0x09);
		
		if (raw.length < 0x0B) {
			RawRomData = null;
			UncompressedRawData = null;
		} else {
			byte data[] = new byte[raw.length-0x0A];
			System.arraycopy(raw, 0x0A, data, 0, data.length);
			RawRomData = data;
			if ((flags & ZXSTMF_COMPRESSED) != ZXSTMF_COMPRESSED) {
				UncompressedRawData = RawRomData;
			} else {
				int length=0x2000;
				if ((flags & ZXSTMF_16KRAMMODE) == ZXSTMF_16KRAMMODE) {
					length = 0x4000;
				}
				UncompressedRawData = zLibDecompressData(data, length);
			}
		}
	}
	
	
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " 48KMode:" + Get48KMode();

		return (result);
	}
	
	public String Get48KMode() {
		return (GetNameFromArray(f48KModel,String.valueOf(f48KModelEmu), 0, 2));
	}
	
	public String GetFlags() {
		return (GetFlagsFromArray(ZXCFFlags, flags, 1));
	}

}

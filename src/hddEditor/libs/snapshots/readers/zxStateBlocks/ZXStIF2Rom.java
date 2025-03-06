package hddEditor.libs.snapshots.readers.zxStateBlocks;
/**
 * Implements the ZXSTIF2ROM block
 * https://www.spectaculator.com/docs/zx-state/if2rom.shtml
 * This contains a rom plugged into the IF/2
 * Only present if there is one, so there is always a rom.
 * It is also always compressed.
 * 
 * $00..$03  ID: "IF2R" 		) Decoded by the parent class. 
 * $04..$07  size: LSB..MSB		)
 * ====start of  ZXSTIF2ROM information====
 * $08..$0B  Compressed rom size. Uncompressed is always 16384
 * $0C..end	 Rom data
 */
public class ZXStIF2Rom extends GenericZXStateBlock {
	public int romSize;
	public byte RawRom[];
	public byte UncompressedRom[];
	
	public ZXStIF2Rom(byte[] rawdata, int start) {
		super(rawdata, start);
		romSize = rawDword(0x08);
		byte data[] = new byte[raw.length-0x14];
		System.arraycopy(raw, 0x0C, data, 0, data.length);
		RawRom = data;
		UncompressedRom = zLibDecompressData(data, 0x4000);
	}
	/**
	 * Add in the STCREATOR specific items
	 */
	@Override
	public String toString() {
		String result = super.toString();
		return (result);
	}


}

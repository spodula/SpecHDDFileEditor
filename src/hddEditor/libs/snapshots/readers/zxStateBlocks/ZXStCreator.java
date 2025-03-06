package hddEditor.libs.snapshots.readers.zxStateBlocks;
/**
 * Implements the ZXSTCREATOR block
 * https://www.spectaculator.com/docs/zx-state/creator.shtml
 * This contains the program that created this file.
 * 
 * $00..$03  ID: "CRTR" 		) Decoded by the parent class. 
 * $04..$07  size: LSB..MSB		)
 * ====start of  ZXSTCREATOR information====
 * $08..$27  Name of the program
 * $28.     Program major version
 * $29.     Program minor version
 * $30..end Variable length data specific to program.
 */

public class ZXStCreator extends GenericZXStateBlock {
	public String creator;
	public int major;
	public int minor;
	public String data;
	
	public ZXStCreator(byte[] rawdata, int start) {
		super(rawdata, start);
		byte creator[] = new byte[0x20];
		System.arraycopy(raw, 8, creator,0,creator.length);
		this.creator = new String(creator).trim();
		
		this.major = rawbyte(0x28);
		this.minor = rawbyte(0x29);
		
		byte data[] = new byte[raw.length-0x2a];
		System.arraycopy(raw, 0x2a, data, 0, data.length);
		String target="";
		for (byte b:data) {
			if ((b<0x20) || (b>0x7f)) {
				target = target + "<"+String.valueOf((int)b)+">";
			} else {
				target = target + Character.valueOf((char)b);
			}
		}
		
		this.data = target;
	}
	/**
	 * Add in the STCREATOR specific items
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Creator: "+creator+" v"+major+"."+minor+" data: \""+data+"\"";
		
		return (result);
	}

}

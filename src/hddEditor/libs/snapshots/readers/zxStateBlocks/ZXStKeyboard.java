package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * Implements the ZXSTKEYBOARD block
 * https://www.spectaculator.com/docs/zx-state/keyboard.shtml
 * This contains the state of any keyboard joystick emulation
 * 
 * $00..$03 ID: "KEYB" ) Decoded by the parent class. 
 * $04..$07 size: LSB..MSB ) Always 5 here. 
 * ====start of ZXSTKEYBOARD information====
 * $08..$0B - Flags
 * $0C  Keyboard joystick emulation 
 */

public class ZXStKeyboard extends GenericZXStateBlock {
	public static String kbFlags[][] = {
			{"1","ZXSTKF_ISSUE2","Issue 2 kb emulation"}
	};
	
	public int kbflags;
	public int kbjs;

	public ZXStKeyboard(byte[] rawdata, int start) {
		super(rawdata, start);
		kbflags = rawDword(0x08);
		kbjs = rawbyte(0x0C);
	}
	
	/**
	 * Add in the ZXStKeyboard values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Keyboard joystick:"+KbJsAsString()+" ("+kbjs+") Flags:" +FlagsAsString()+" ("+kbflags+")";

		return (result);
	}
	
	public String FlagsAsString() {
		return(GetFlagsFromArray(kbFlags, kbflags, 2));
	}
	
	public String KbJsAsString() {
		String name = GetNameFromArray(ZXStJoystick.JoystickTypes, String.valueOf(kbjs),0, 2);
		if (name.equals("Disabled")) {
			name = "None";
		}
		return(name);
	}
	

}

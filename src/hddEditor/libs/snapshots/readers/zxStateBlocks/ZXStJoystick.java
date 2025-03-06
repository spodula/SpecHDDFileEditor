package hddEditor.libs.snapshots.readers.zxStateBlocks;

/*-*
 * Implements the ZXSTJOYSTICK block
 * https://www.spectaculator.com/docs/zx-state/joystick.shtml This contains the
 * connected joystick interfaces.
 * 
 * $00..$03 ID: "JOY",$0 ) Decoded by the parent class. 
 * $04..$07 size: LSB..MSB ) Always 6 here. 
 * ====start of ZXSTJOYSTICK information==== 
 * $08-0B Flags 
 * $0C Player 1 
 * $0D Player 2 
 */

public class ZXStJoystick extends GenericZXStateBlock {
	public static String JoystickTypes[][] =
	{
			{"0","ZXSTJT_KEMPSTON","Kempston Joystick"},
			{"1","ZXSTJT_FULLER","Fuller Joystick"},
			{"2","ZXSTJT_CURSOR","Cursor Joystick"},
			{"3","ZXSTJT_SINCLAIR1","Sinclair Port 1"},
			{"4","ZXSTJT_SINCLAIR2","Sinclair Port 2"},
			{"5","ZXSTJT_COMCOM","ComCon Programmable"},
			{"6","ZXSTJT_TIMEX1","Timex/Spectrum SE Port 1"},
			{"7","ZXSTJT_TIMEX2","Timex/Spectrum SE Port 2"},
			{"8","ZXSTJT_DISABLED","Disabled"}
	};
	public static String JoystickFlags[][] = 
	{	//Note, this flag is depreciated, but is provided for completeness.
			{"1","ZXSTJOYF_ALWAYSPORT31","Always return 0 for IN 31"}
	};
	
	

	int JoyFlags;
	int Plr1Joy;
	int Plr2Joy;

	public ZXStJoystick(byte[] rawdata, int start) {
		super(rawdata, start);
		JoyFlags = rawDword(0x08);
		Plr1Joy = rawbyte(0x0C);
		Plr2Joy = rawbyte(0x0D);
	}

	/**
	 * Add in the ZSXSTZ80Regs values.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Plr 1:"+Plr1AsString()+" Plr 2:"+Plr2AsString()+" Flags:"+FlagsAsString();

		return (result);
	}
	
	public String Plr1AsString() {
		return(GetNameFromArray(JoystickTypes, String.valueOf(Plr1Joy),0, 2));
	}
	public String Plr2AsString() {
		return(GetNameFromArray(JoystickTypes, String.valueOf(Plr2Joy),0, 2));
	}
	public String FlagsAsString() {
		return(GetFlagsFromArray(JoystickFlags, JoyFlags, 2));
	}
}

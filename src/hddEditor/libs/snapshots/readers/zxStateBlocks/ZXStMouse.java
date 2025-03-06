package hddEditor.libs.snapshots.readers.zxStateBlocks;
/**
 * Implements the ZXSTMOUSE block
 * https://www.spectaculator.com/docs/zx-state/mouse.shtml
 * The current mouse emulation state
 * 
 * $00..$03  	ID: "AMXM" 		) Decoded by the parent class. 
 * $04..$07  	size: LSB..MSB		)
 * ====start of  ZXSTMOUSE information====
 * $08			Type
 * $09..$0C		Ctrl A x3	} AMX only		
 * $0D..$0F		Ctrl B x3	} 
 */
public class ZXStMouse extends GenericZXStateBlock {
	public static String MouseTypes[][] = {
			{"0","ZXSTM_NONE","No mouse connected"},
			{"1","ZXSTM_AMX","AMX mouse connected"},
			{"2","ZXSTM_KEMPSTON","Kempston mouse connected"},
	};
	
	public int type;
	public int CtrlA[];
	public int CtrlB[];
	
	
	public ZXStMouse(byte[] rawdata, int start) {
		super(rawdata, start);
		type = rawbyte(0x08);
		CtrlA = new int[3];
		CtrlB = new int[3];
		
		for (int i=0;i<3;i++) {
			CtrlA[i] = rawbyte(i+0x09);
		}
		for (int i=0;i<3;i++) {
			CtrlB[i] = rawbyte(i+0x0D);
		}
	}

	
	@Override
	public String toString() {
		String result = super.toString();
		result = result + " Type:" + GetMouseType();
		result = result + " CtrlA:";
		for (int i=0;i<CtrlA.length;i++)
			result = result + " "+String.format("%02X",(int)CtrlA[i] );

		result = result + " CtrlB:";
		for (int i=0;i<CtrlA.length;i++)
			result = result + " "+String.format("%02X",(int)CtrlB[i] );
		
		return (result);
	}
	
	public String GetMouseType() {
		return (GetNameFromArray(MouseTypes,String.valueOf(type), 0, 2));
	}

}

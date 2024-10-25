package hddEditor.libs.snapshots;

import java.util.Hashtable;

import hddEditor.libs.GeneralUtils;

public class MachineState extends CPUState {
	public static int MT_48K = 0;
	public static int MT_128K = 1;
	public static int MT_PLUS2A3 = 2;
	public static int MT_Pentagon = 3;
	public static int MT_Scorpion = 4;
	public static int MT_Didaktik = 5;
	public static int MT_TC20xx = 6;
	
	public static String[] MachineTypes = {"48K","128K","Plus2/3","Pentagon","Scorpion","Didaktik","TC20xx"};
	
	public static int SOURCE_SNA = 1;
	public static int SOURCE_Z80 = 2;
	public static int SOURCE_UNKNOWN = 3;
	
	public byte RAM[];
	public int BORDER;
	public int MachineClass;
	public int SOURCE;
	
	public int last7ffd;
	public int last1ffd;
	public int lastfffd;
	
	
	public byte[][] RamBanks;
	
	public Hashtable<String,String> SnapSpecific;
		
	public int GetPagedRamNumber() {
		return (last7ffd & 0x07);
	}
	
	public MachineState(Registers main, Registers alt, byte i, byte ixl, byte ixh, byte iyl, byte iyh, byte r, byte im,
			boolean ei, byte spl, byte sph, int pc, byte[] ram, int border) {
		super(main, alt, i, ixl, ixh, iyl, iyh, r, im, ei, spl, sph, pc);
		RAM = ram;
		this.BORDER = border;
		this.RamBanks = new byte[8][0x4000];
		this.MachineClass = MT_48K;
		this.last7ffd = -1;
		this.last1ffd = -1;
		this.lastfffd = -1;
		this.SOURCE = SOURCE_UNKNOWN;
		this.SnapSpecific = new Hashtable<String,String> ();
	}
	
	public MachineState() {
		super(new Registers(), new Registers(), (byte)0, (byte)0,(byte)0, (byte)0,(byte)0, (byte)0, (byte)0, false, (byte)0,(byte)0, 0);
		RAM = new byte[0x10000];
		this.BORDER = 7;
		this.RamBanks = new byte[8][0x4000];
		this.MachineClass = MT_48K;
		this.last7ffd = -1;
		this.last1ffd = -1;
		this.lastfffd = -1;
		this.SOURCE = SOURCE_UNKNOWN;
		this.SnapSpecific = new Hashtable<String,String> ();
	}
	
	@Override
	public String toString() {
		String result = super.toString();
		
		result = result+"Border: "+this.BORDER+"\n";
		result = result+"McClass: "+this.MachineClass+"\n";
		result = result+"Source: "+this.SOURCE+"\n";
		result = result+"7ffd: "+this.last7ffd+"\n";
		result = result+"1ffd: "+this.last1ffd+"\n";
		result = result+"fffd: "+this.lastfffd+"\n";
		result = result+"Paged: "+this.GetPagedRamNumber() +"\n";
		
		if (this.MachineClass == MT_48K ) {
			result = result + "Ram:\n";
			result = result + GeneralUtils.HexDump(this.RAM,0,0x40,0x4000);
			result = result + "\n....\n";
			result = result + GeneralUtils.HexDump(this.RAM,0xbfc0,0x40,0x4000)+"\n";
		} else {
			for (int i=0;i<7;i++) {
				result = result + "Page "+i+":\n";
				result = result + GeneralUtils.HexDump(this.RAM,0,0x20,0x0000);
				result = result + "\n....\n";
				result = result + GeneralUtils.HexDump(this.RAM,0x3fc0,0x20,0x0000)+"\n";				
			}
		}
		return(result);
	}
	
	/**
	 * 
	 * @return
	 */
	@Override
	public Hashtable<String, String> DetailsAsArray() {
		Hashtable<String, String> result = super.DetailsAsArray();

		result.put("Border",String.valueOf(BORDER));
		if (last7ffd!= -1) {
			result.put("7FFD",String.format("%2x", last7ffd));
		}
		if (last1ffd!= -1) {
			result.put("1FFD",String.format("%2x", last1ffd));
		}
		if (lastfffd!= -1) {
			result.put("FFFD",String.format("%2x", lastfffd));			
		}
		result.put("Machine",MachineTypes[MachineClass]);
		
		return(result);
	}

}

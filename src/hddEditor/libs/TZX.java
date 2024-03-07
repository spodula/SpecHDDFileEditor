package hddEditor.libs;
/**
 * Details of Raw supported/unsupported. (Note, actually displaying them is different). The list of these is in TzxFileEditDialog
 * 
 * Unsupported:
 * ID 19 //Generalised data block         *Eg  Zoids - The Battle Begins (1985)(Martech Games)[a2].tzx Zaxxon (1985)(U.S. Gold)[a].tzx
 *  
 * Depreciated unsupported
 * 
 * ID 16 //C64 ROM type data
 * ID 17 //C64 Turbo type data
 * ID 34 //Emulation info
 * id 40 //Snapshot blocks

 * Depreciated supported
 * id 33 // Hardware type                
 *
 */

/**
 * Useful constants for TZX file decoding.
 * 
 */

public class TZX {
	// TZX block types
	public static final int TZX_STANDARDSPEED_DATABLOCK = 0x10;
	public static final int TZX_TURBOSPEED_DATABLOCK = 0x11;
	public static final int TZX_PURETONE = 0x12;
	public static final int TZX_PULSESEQ = 0x13;
	public static final int TZX_PUREDATA = 0x14;
	public static final int TZX_DIRECTRECORDING = 0x15;
	public static final int TZX_CSWRECORDING = 0x18;
	public static final int TZX_GENERAL = 0x19;
	public static final int TZX_PAUSE = 0x20;
	public static final int TZX_GROUPSTART = 0x21;
	public static final int TZX_GROUPEND = 0x22;
	public static final int TZX_JUMP = 0x23;
	public static final int TZX_LOOPSTART = 0x24;
	public static final int TZX_LOOPEND = 0x25;
	public static final int TZX_CALLSEQ = 0x26;
	public static final int TZX_RETSEQ = 0x27;
	public static final int TZX_SELECTBLOCK = 0x28;
	public static final int TZX_STOP48 = 0x2A;
	public static final int TZX_SETSIGNALLEVEL = 0x2B;
	public static final int TZX_TEXTDESC = 0x30;
	public static final int TZX_MESSAGEBLOCK = 0x31;
	public static final int TZX_ARCHIVEINFO = 0x32;
	public static final int TZX_HARDWARETYPE = 0x33;
	public static final int TZX_EMULATIONINFO = 0x34;
	public static final int TZX_CUSTOMINFO = 0x35;
	public static final int TZX_SNAPSHOT = 0x40;
	public static final int TZX_GLUE = 0x5A;

	public class intdesc {
		public int value;
		public String description;

		public intdesc(int val, String desc) {
			value = val;
			description = desc;
		}
	}

	/**
	 * Array of ID and descriptions for the TZX datablock types
	 */
	public intdesc TZXTypes[] = { new intdesc(TZX_STANDARDSPEED_DATABLOCK, "Standard speed datablock"),
			new intdesc(TZX_TURBOSPEED_DATABLOCK, "Turbo speed datablock"),
			new intdesc(TZX_PURETONE, "Pure tone datablock"), new intdesc(TZX_PULSESEQ, "Pulse sequence"),
			new intdesc(TZX_PUREDATA, "Pure data"), new intdesc(TZX_DIRECTRECORDING, "Direct recording"),
			new intdesc(TZX_CSWRECORDING, "Compressed square wave (CSW) recording"),
			new intdesc(TZX_GENERAL, "General data block"), new intdesc(TZX_PAUSE, "Pause/Stop tape"),
			new intdesc(TZX_GROUPSTART, "Group start"), new intdesc(TZX_GROUPEND, "Group end"),
			new intdesc(TZX_JUMP, "Jump"), new intdesc(TZX_LOOPSTART, "Loop start"),
			new intdesc(TZX_LOOPEND, "Loop end"), new intdesc(TZX_CALLSEQ, "Call sequence"),
			new intdesc(TZX_RETSEQ, "Return from sequence"), new intdesc(TZX_SELECTBLOCK, "Select block"),
			new intdesc(TZX_STOP48, "Stop if 48k"), new intdesc(TZX_SETSIGNALLEVEL, "Set signal level"),
			new intdesc(TZX_TEXTDESC, "Text description"), new intdesc(TZX_MESSAGEBLOCK, "Message block"),
			new intdesc(TZX_ARCHIVEINFO, "Archive information block"),
			new intdesc(TZX_EMULATIONINFO, "Emulation information block"),
			new intdesc(TZX_HARDWARETYPE, "Hardware type block"), new intdesc(TZX_CUSTOMINFO, "Custom info block"),
			new intdesc(TZX_GLUE, "Glue block") };

	/**
	 * Textual description of the TZX data block type.
	 * 
	 * @param id
	 * @return
	 */
	public static String GetDataBlockTypeForID(int id) {
		TZX t = new TZX();
		for (intdesc x : t.TZXTypes) {
			if (x.value == id) {
				return (x.description);
			}
		}
		return ("Unknown block");
	}

	// Default time in MS for the delay between blocks
	public static int DEFAULT_STD_DELAY = 954;

	/**
	 * Calculate the checksum for the given datablock.
	 * 
	 * This probably wants moving to the Speccy object as its common to all ROM and
	 * ROM-based loading routines
	 * 
	 * @param block
	 * @return
	 */

	public static int CalculateChecksumForBlock(byte block[]) {
		int checksum = 0;
		if (block.length > 0) {
			for (int i = 0; i < block.length - 1; i++) {
				checksum = checksum ^ (block[i] & 0xff);
			}
		}
		int i = (checksum & 0xff);
		return (i);
	}

	/**
	 * Big static array for the contents of TZX_HARDWARETYPE.
	 */
	public static String hwInfo[] = { "Runs (may use special HW)", "Runs (Uses Special HW)",
			"Runs (Doesnt use Special Hw)", "Doesnt run" };
	public static String hwType[] = { "Computer", "Ext Storage", "Rom/Ram", "Sound", "Joystick", "Mouse",
			"Other controller", "Serial", "Parallel", "Printer", "Modem", "Digitiser", "Network", "Keypad",
			"AD/DA converter", "EPROM Programmer", "Graphic" };

	public static String hwTypeComputer[] = { "ZX Spectrum 16k", "ZX Spectrum 48k, Plus", "ZX Spectrum 48k ISSUE 1",
			"ZX Spectrum 128k +(Sinclair)", "ZX Spectrum 128k +2 (grey case)", "ZX Spectrum 128k +2A, +3",
			"Timex Sinclair TC-2048", "Timex Sinclair TS-2068", "Pentagon 128", "Sam Coupe", "Didaktik M",
			"Didaktik Gama", "ZX-80", "ZX-81", "ZX Spectrum 128k, Spanish version", "ZX Spectrum, Arabic version",
			"Microdigital TK 90-X", "Microdigital TK 95", "Byte", "Elwro 800-3", "ZS Scorpion 256", "Amstrad CPC 464",
			"Amstrad CPC 664", "Amstrad CPC 6128", "Amstrad CPC 464+", "Amstrad CPC 6128+", "Jupiter ACE", "Enterprise",
			"Commodore 64", "Commodore 128", "Inves Spectrum+", "Profi", "GrandRomMax", "Kay 1024", "Ice Felix HC 91",
			"Ice Felix HC 2000", "Amaterske RADIO Mistrum", "Quorum 128", "MicroART ATM", "MicroART ATM Turbo 2",
			"Chrome", "ZX Badaloc", "TS-1500", "Lambda", "TK-65", "ZX-97" };
	public static String hwTypeExtStorage[] = { "ZX Microdrive", "Opus Discovery", "MGT Disciple", "MGT Plus-D",
			"Rotronics Wafadrive", "TR-DOS (BetaDisk)", "Byte Drive", "Watsford", "FIZ", "Radofin",
			"Didaktik disk drives", "BS-DOS (MB-02)", "ZX Spectrum +3 disk drive", "JLO (Oliger) disk interface",
			"Timex FDD3000", "Zebra disk drive", "Ramex Millenia", "Larken", "Kempston disk interface", "Sandy",
			"ZX Spectrum +3e hard disk", "ZXATASP", "DivIDE", "ZXCF" };
	public static String hwTypeRomRam[] = { "Sam Ram", "Multiface ONE", "Multiface 128k", "Multiface +3", "MultiPrint",
			"MB-02 ROM/RAM expansion", "SoftROM", "1k", "16k", "48k", "Memory in 8-16k used" };
	public static String hwTypeSound[] = { "Classic AY hardware (compatible with 128k ZXs)",
			"Fuller Box AY sound hardware", "Currah microSpeech", "SpecDrum", "AY ACB stereo (A+C=left",
			"AY ABC stereo (A+B=left", "RAM Music Machine", "Covox", "General Sound",
			"Intec Electronics Digital Interface B8001", "Zon-X AY", "QuickSilva AY", "Jupiter ACE" };
	public static String hwTypeJoysticks[] = { "Kempston", "Cursor, Protek, AGF", "Sinclair 2 Left (12345)",
			"Sinclair 1 Right (67890)", "Fuller" };
	public static String hwTypeMouse[] = { "AMX mouse", "Kempston mouse" };
	public static String hwTypeOtherControl[] = { "Trickstick", "ZX Light gun", "Zebra Graphics tablet",
			"Defender light gun" };
	public static String hwTypeSerial[] = { "ZX Interface 1", "ZX Spectrum 128" };
	public static String hwTypeParallel[] = { "Kempston S", "Kempston E", "ZX Spectrum +3", "Tasman", "DK'Tronics",
			"Hilderbay", "INES Printerface", "ZX LPrint Interface 3", "MultiPrint", "Opus Discovery",
			"Standard 8255 chip with ports 31 63 95" };
	public static String hwTypePrinters[] = { "ZX Printer/Alphacom 32", "Generic printer", "Epson compatible" };
	public static String hwTypeModems[] = { "Prism VTX 5000", "TS 2050 or Westridge 2050" };
	public static String hwTypeDigitiser[] = { "RD Digital tracer", "DK'tronics light pen", "British Micrograph pad",
			"Romantic Robot Videoface" };
	public static String hwTypeNetwork[] = { "ZX Interface 1" };
	public static String hwTypeKeypad[] = { "Keypad for ZX Spectrum 128" };
	public static String hwTypeADDA[] = { "Harley systems ADC 8.2", "Blackboard electronics" };
	public static String hwTypeEPROMMER[] = { "Orme Electronics" };
	public static String hwTypeGRAPHICS[] = { "WRX Hi-res", "G007", "Memotech", "Lambda Colour" };

	public static String HwInfoMatrix[][] = { hwTypeComputer, hwTypeExtStorage, hwTypeRomRam, hwTypeSound,
			hwTypeJoysticks, hwTypeMouse, hwTypeOtherControl, hwTypeSerial, hwTypeParallel, hwTypePrinters,
			hwTypeModems, hwTypeDigitiser, hwTypeNetwork, hwTypeKeypad, hwTypeADDA, hwTypeEPROMMER, hwTypeGRAPHICS };

}

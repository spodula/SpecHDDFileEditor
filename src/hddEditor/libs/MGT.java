package hddEditor.libs;

public class MGT {
	public static int MGT_SECTORSIZE = 512;
	public static int MGT_SECTORSPERTRACK = 10;
	
	public static String MGTFileTypes[] = { "Empty", "ZX Basic", "ZX NumArray", "ZX StrArray", // 0-3
			"ZX Code", "48k SNA", "ZX Microdrive", "ZX Screen", // 4-7
			"Special", "128k SNA", "OpenType", "ZX Execute", // 8-11
			"UniDOS SubDir", "UniDOS Create", "Unknown", "Unknown", // 12-15
			"SAM Basic", "SAM NumArray", "SAM StrArray", "SAM Code", // 16-19
			"SAM Screen", "MasterDOS SubDir", "SAM Driver App", "Sam Drive Boot", // 20-23
			"EDOS NOMEN", "EDOS System", "EDOS Overlay", "Unknown", // 24-27
			"HDOS hdos", "HDOS Hdir", "HDOS Hdisk", "HDOS Hfree/Htmp" }; // 28-31
	public static int MGTFT_ERASED = 0;
	public static int MGTFT_ZXBASIC = 1;
	public static int MGTFT_ZXNUMARRAY = 2;
	public static int MGTFT_ZXSTRARRAY = 3;
	public static int MGTFT_ZXCODE = 4;
	public static int MGTFT_ZX48SNA = 5;
	public static int MGTFT_ZXMDR = 6;
	public static int MGTFT_ZXSCREEN = 7;
	public static int MGTFT_SPECIAL = 8;
	public static int MGTFT_ZX128SNA = 9;
	public static int MGTFT_OPENTYPE = 10;
	public static int MGTFT_ZXEXE = 11;
	
	public static int MGTFT_UNDOSSUBDIR = 12;
	public static int MGTFT_UNDOSCREATE = 13;
	
	public static int MGTFT_SAMBASIC = 16;
	public static int MGTFT_SAMNUMARRAY = 17;
	public static int MGTFT_SAMSTRARRAY = 18;
	public static int MGTFT_SAMCODE = 19;
	public static int MGTFT_SAMSCREEN = 20;
	
	public static int MGTFT_MASTERDOSSUBDIR = 21;
	public static int MGTFT_SAMDRIVER = 22;
	public static int MGTFT_SAMBOOT = 23;
	
	public static int MGTFT_EDOSNOMEN = 24;
	public static int MGTFT_EDOSSYSTEM = 25;
	public static int MGTFT_EDOSOVERLAY = 26;
	
	public static int MGTFT_HDOSHDOS = 28;
	public static int MGTFT_HDOSHDIR = 29;
	public static int MGTFT_HDOSHDISK = 30;
	public static int MGTFT_HDOSHFREETMP = 31;
	
}

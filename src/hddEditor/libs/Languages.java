package hddEditor.libs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Languages {
	public static int MENU_FILE = 1;
	public static int MENU_NEWHDD = 2;
	public static int MENU_NEWFDD = 3;
	public static int MENU_LOAD = 4;
	public static int MENU_SELFILE = 5;
	public static int MENU_SELDISK = 6;
	public static int MENU_RELOAD = 7;
	public static int MENU_SAVEAS = 8;
	public static int MENU_IMPPART = 9;
	public static int MENU_IMPPAWS = 10;
	public static int MENU_EXIT = 11;
	public static int MENU_DRAGDEF = 12;
	public static int MENU_HELP = 13;
	public static int MENU_HELPDESC = 14;
	public static int MENU_SAVEASASCII = 15;
	public static int MENU_EDIT = 16;
	public static int MENU_SEARCH = 17;
	public static int MENU_GETHELP = 18;
	
	
	public static int MSG_LOADING = 100;
	public static int MSG_BLOCSZOVERRIDE = 101;
	public static int MSG_CANTLOAD = 102;
	public static int MSG_FILECHANGED = 103;
	public static int MSG_PROCESSING = 104;
	public static int MSG_CONVRAWHDF = 105;
	public static int MSG_SELSOURCE = 106;
	public static int MSG_SELTARGET = 107;
	public static int MSG_TARGETTYPE = 108;
	public static int MSG_NOTRAWHDD = 109;
	public static int MSG_ERRORLOADING = 110;
	public static int MSG_CONVERTING = 111;
	public static int MSG_CONVERTFILE = 112;
	public static int MSG_OPENINGWRITE = 113;
	public static int MSG_COPYINGXXSECTPRS = 114;
	public static int MSG_CANCELLED = 115;
	public static int MSG_COPIEDSECT = 116;
	public static int MSG_CONVCOMPLETE = 117;
	public static int MSG_CANTOPENWRITE = 118;
	public static int MSG_CANTWRITE = 119;
	public static int MSG_SELFILEIMP = 120;
	public static int MSG_SOURCEPART = 121;
	public static int MSG_SELECTALL = 122;
	public static int MSG_SELECTNONE = 123;
	public static int MSG_INVSEL = 124;
	public static int MSG_TARGETPART = 125;
	public static int MSG_COPYFAILED = 126;
	public static int MSG_FILENAME = 127;
	public static int MSG_NOTES = 128;
	public static int MSG_BASEFN = 129;
	public static int MSG_ZENOBI_INCORRECT = 130;
	public static int MSG_ZENOBI_CANTFINDLOADER = 131;
	public static int MSG_ZENOBI_CANTFINDSTART = 132;
	public static int MSG_INVALIDFILE = 133;
	public static int MSG_UNCOMPRESSEDSCREEN = 134;
	public static int MSG_FONTFILE = 135;
	public static int MSG_LOADERPOKE = 136;
	public static int MSG_128KPAWS = 137;
	public static int MSG_128KFILE = 138;
	public static int MSG_COMPRESSEDSCREEN = 139;
	public static int MSG_UNKNOWNFILE = 140;
	public static int MSG_REALLOADER = 141;
	public static int MSG_CONFUSINGLOADER = 142;
	public static int MSG_STARTADDRESS = 143;
	public static int MSG_RUNADDRESS = 144;
	public static int MSG_TRDDISKSIZE = 145;
	public static int MSG_DSKLABEL = 146;
	public static int MSG_AMSEXTENDED = 147;
	public static int MSG_TRDSCL = 148;
	public static int MSG_CREATENEW = 149;
	public static int MSG_UNTITLED = 150;
	public static int MSG_ERRCART = 151;
	public static int MSG_ERRTAPE = 152;
	public static int MSG_ERRTZX = 153;
	public static int MSG_ERRTRD = 154;
	public static int MSG_ERRSCL = 155;
	public static int MSG_ERRAMS = 156;
	public static int MSG_CYLS = 157;
	public static int MSG_HEADS = 158;
	public static int MSG_DISKSIZE = 159;
	public static int MSG_SPT = 160;
	public static int MSG_OPENNINGFORWRITING = 161;
	public static int MSG_SELDEVICE = 162;
	public static int MSG_DEV = 163;
	public static int MSG_CONN = 164;
	public static int MSG_SIZE = 165;
	public static int MSG_DETAILS = 166;
	public static int MSG_SELDEV = 167;
	public static int MSG_NODISK = 168;
	
	public static int MSG_XDPBINFO = 169;
	public static int MSG_NOTBOOTABLE = 170;
	public static int MSG_BPLUS3DPS = 171;
	public static int MSG_BPCW9512 = 172;
	public static int MSG_BPCW8256 = 173;
	public static int MSG_FORMAT = 174;
	public static int MSG_SECTORS = 175;
	public static int MSG_SECTSZ = 176;
	public static int MSG_RESERVEDTR = 177;
	public static int MSG_BLOCKSZ = 178;
	public static int MSG_DIRBLOCKS = 179;
	public static int MSG_RWGAP = 180;
	public static int MSG_FMTGAP = 181;
	public static int MSG_CSUM = 182;
	public static int MSG_BOOTABLE = 183;
	public static int MSG_MAXCPMB = 184;
	public static int MSG_RESERVEDB = 185;
	public static int MSG_MAXDIRENTS = 186;
	public static int MSG_BPB = 187;
	public static int MSG_DISKSZ = 188;
	public static int MSG_SECTORR = 189;
	public static int MSG_IDTYPE = 190;
	public static int MSG_BOOTSECTCODE = 191;
	public static int MSG_SAVEASM = 192;
	public static int MSG_SAVERAWBS = 193;
	public static int MSG_BSLOADERR = 194;
	public static int MSG_BSOFFILE = 195;
	public static int MSG_ORG = 196;
	public static int MSG_LENGTH = 197;
	public static int MSG_RESERVED = 198;
	public static int MSG_CHECKSUM = 199;
	public static int MSG_ERRORATADDR = 200;
	public static int MSG_ERRSAVING = 201;
	public static int MSG_DIRNOTFOUND = 202;
	public static int MSG_IOERROR = 203;
	public static int MSG_ADDRESS = 204;
	public static int MSG_HEX = 205;
	public static int MSG_ASM = 206;
	public static int MSG_CHR = 207;
	public static int MSG_TRACK = 208;
	public static int MSG_SIDE = 209;
	public static int MSG_FILLERB = 210;
	public static int MSG_GAP3 = 211;
	public static int MSG_DATARATE = 212;
	public static int MSG_NUMSECTORS = 213;
	public static int MSG_RECMODE = 214;
	public static int MSG_SECTOR = 215;
	public static int MSG_ACTSIZE = 216;
	public static int MSG_SECTDATA = 217;
	
	public static int MSG_PARTNAME = 218;
	public static int MSG_PARTTYPE = 219;
	public static int MSG_SIZECH = 220;
	public static int MSG_LASTSECT = 221;
	public static int MSG_FILETYPE = 222;
	public static int MSG_START =  223;
	public static int MSG_LENREPORTED = 224;
	public static int MSG_LEGSECTORS = 225;
	public static int MSG_FILEPROPERTIES = 226;
	public static int MSG_EDITRAWFILE = 227;
	public static int MSG_DELETEFILE = 228;
	public static int MSG_ADDGFILES = 229;
	public static int MSG_EXTRACTALLFILES = 230;
	public static int MSG_RENAMEFILE = 231;
	public static int MSG_CANNOTPARSE = 232;
	public static int MSG_ERRORRENAME = 233;
	public static int MSG_THESELECTEDFILES = 234;
	public static int MSG_AREYOUSUREDEL = 235;
	public static int MSG_ERRIODEL = 236;
	public static int MSG_EDITINGX = 237;
	public static int MSG_ERROREDITING = 238;
	public static int MSG_SSMDRCART = 239;
	public static int MSG_BLOCKS = 240;
	public static int MSG_CARTNAME = 241;
	public static int MSG_FREEBLOCKS = 242;
	public static int MSG_PACKCART = 243;
	public static int MSG_BADFILEHEADER = 244;
	public static int MSG_BADSECTHEADER = 245;
	public static int MSG_BADDATA = 246;
	public static int MSG_ERRORWRITEBACK = 247;
	public static int MSG_UPDATEIGNORED = 248;
	public static int MSG_ERRPACKINGMDR = 249;
	
	public static int MSG_PARTDETS = 250;
	public static int MSG_PLUS3DOSPARTINVALID = 251;
	public static int MSG_PARTFREESPACE = 252;
	public static int MSG_DRIVEMAP = 253;
	public static int MSG_USEDBLOCKS = 254;
	public static int MSG_FREESPACE = 255;
	public static int MSG_USEDDIRENTS = 256;
	public static int MSG_CPMLEN = 257;
	public static int MSG_REALLEN = 258;
	public static int MSG_FLAGS = 259;
	public static int MSG_UNDELETEFILE = 260;
	public static int MSG_MSGMAYBEINCOMPLETE = 261;
	public static int MSG_INCOMPLETEFILE = 262;
	public static int MSG_ERRORREADINGPART = 263;
	public static int MSG_INVALIDPLUS3HEADER = 264;
	public static int MSG_DELETED = 265;
	public static int MSG_INCOMPLETE = 266;
	public static int MSG_COMPLETE = 267;
	public static int MSG_NOUPDATTENOHEADER = 268;
	public static int MSG_FILE = 269;
	public static int MSG_ERRREADNGFILE = 270;
	public static int MSG_RAWFLOPPY = 271;
	public static int MSG_TRACKS = 272;
	public static int MSG_SIDES = 273;
	public static int MSG_EXPORTDISK = 274;
	public static int MSG_SELTARGETFLDR = 275;
	public static int MSG_ORIGFILENAME = 276;
	public static int MSG_NUMLOGSECTORS = 277;
	public static int MSG_STATUSR1 = 278;
	public static int MSG_STATUSR2 = 279;
	public static int MSG_SECTSIZEM = 280;
	public static int MSG_NODATA = 281;
	public static int MSG_ASSEMBLY = 282;
	
	
	public static int MSG_SYSDETS = 283;
	public static int MSG_UNALLOCSPC = 284;
	public static int MSG_ALLOCSPACE = 285;
	public static int MSG_FREEPARTS = 286;
	public static int MSG_ALLOCPARTS = 287;
	public static int MSG_DEFAULTCOLS = 288;
	public static int MSG_PAPER = 289;
	public static int MSG_INK = 290;
	public static int MSG_BRIGHT = 291;
	public static int MSG_EDITORCOL = 292;
	public static int MSG_DEFCOL = 293;
	public static int MSG_UNMAPDRIVE = 294;
	public static int MSG_DEFAULTDRIVE = 295;
	public static int MSG_PARTITIONS = 296;
	public static int MSG_FDDSECTS = 297;
	public static int MSG_PARTTYPEM = 298;
	public static int MSG_TAPESECT = 299;
	public static int MSG_FDDSECT = 300;
	public static int MSG_END = 301;
	public static int MSG_DELPART = 302;
	public static int MSG_EDITRAWPART = 303;
	public static int MSG_GOTOPART = 304;
	public static int MSG_NEWPART = 305;
	public static int MSG_DUMPPART = 306;
	public static int MSG_SHRINKDISK = 307;
	public static int MSG_RENAMEPART = 308;
	public static int MSG_CYL = 309;
	public static int MSG_HEAD = 310;
	public static int MSG_CHANGEPARTQ = 311;
	public static int MSG_CHANGEPART = 312;
	public static int MSG_CANTEDITPART = 313;
	public static int MSG_ERRCREATEPART = 314;
	public static int MSG_ERRDELETEPART = 315;
	public static int MSG_NOSYSPART = 316;
	public static int MSG_NOFREEPART = 317;
	public static int MSG_AREYOUSURE = 318;
	public static int MSG_ABSOLUTELYSUREPART = 319;

	public static int MSG_FILES = 321;
	public static int MSG_DATABLOCKS = 322;
	public static int MSG_MOVEFUP = 323;
	public static int MSG_MOVEFDOWN = 324;
	public static int MSG_ERRDELFILE = 325;
	public static int MSG_DISKLABEL = 326;
	public static int MSG_FREESECTORS = 327;
	public static int MSG_DELETEDFILES = 328;
	public static int MSG_LOGICALDISKTYP = 329;
	public static int MSG_FIRSTFREESECT = 330;
	public static int MSG_DEFRAGDISK = 331;
	
	public static int MSG_PRESSENTERTOEDIT = 332;
	public static int MSG_PRESSENTERTOSET = 333;
	public static int MSG_LENXBYTESX = 334;
	public static int MSG_ASCII = 335;
	public static int MSG_CLOSEREQ = 336;
	public static int MSG_SAVEASASCII = 337;
	public static int MSG_SAVEASBIN = 338;
	public static int MSG_CREATEPART = 339;
	public static int MSG_SIZEMSG = 340;
	public static int MSG_OLDNAME = 341;
	public static int MSG_NEWNAME = 342;
	public static int MSG_SELECT = 343;
	public static int MSG_RANGESTART = 344;
	public static int MSG_ASCIISECTT = 345;
	public static int MSG_START0T = 346;
	public static int MSG_DECHEXT = 347;
	public static int MSG_SEPERATORT = 348;
	public static int MSG_SAMPLE = 349;
	public static int MSG_SAVEFILEAS = 350;
	public static int MSG_SEARCHREPLACE = 351;
	public static int MSG_REPLACE = 352;
	public static int MSG_HEXASCII = 353;
	public static int MSG_SAVECLOSE = 354;
	public static int MSG_XITEMSFOUND = 355;
	public static int MSG_SRSAMELEN = 356;
	public static int MSG_REPLACEDXITEMS = 357;
	public static int MSG_CURRMAXCYL = 358;
	public static int MSG_POSSCYL = 359;
	public static int MSG_OLDMAXSZ = 360;
	public static int MSG_MINSZPOSS = 361;
	public static int MSG_SHRINGOREXP = 362;
	public static int MSG_NEWCYLS = 363;
	public static int MSG_MINXMAXX = 364;
	public static int MSG_WARNPARTSIZE = 365;
	public static int MSG_ERRVALUEINVALID = 366;
	public static int MSG_ERRCANTSHRINKDISK = 367;
	public static int MSG_CANTRESIZEFREEPART = 368;
	public static int MSG_CANTSHRINK = 369;
	public static int MSG_OPENCSV = 370;
	public static int MSG_CHARARRAY= 371;
	public static int MSG_FAILEDTOADD = 372;
	public static int MSG_NUMARRAY = 373;
	public static int MSG_OPENIMGFILE = 374;
	public static int MSG_OPENRAWBINFILE = 375;
	public static int MSG_OPENBASICTXTFILE = 376;
	public static int MSG_OPENBASICENCFILE = 377;
	public static int MSG_SELECTTXTBASICFILE = 378;
	public static int MSG_SELECTBINBASICFILE = 379;
	public static int MSG_SELECTCODEFILE = 380;
	public static int MSG_SELECTIMAGEFILE = 381;
	public static int MSG_SELECTNUMFILE = 382;
	public static int MSG_SELECTCHARFILE = 383;
	public static int MSG_MDRFILE = 384;
	public static int MSG_DEFAULTS = 385;
	public static int MSG_BASICFILES = 386;
	public static int MSG_STARTLINE = 387;
	public static int MSG_IMAGEFILES = 388;
	public static int MSG_MONOCHROME = 389;
	public static int MSG_CODELOADADD = 390;
	public static int MSG_CUTOFF = 391;
	public static int MSG_ERRORADDING = 392;
	public static int MSG_DISKFILENAME = 393;
	public static int MSG_SELPLUS3FILES = 394;
	public static int MSG_SELCPMFILE = 395;
	public static int MSG_PLUS3FILES = 396;
	public static int MSG_PLUS3FILEBAD = 397;
	public static int MSG_PLUS3HEADER = 398;
	public static int MSG_NOPLUS3HEADER = 399;
	public static int MSG_ERRREADFILEXFNF = 400;
	public static int MSG_ERRREADFILEX = 401;
	public static int MSG_SELFILESTOADD = 402;
	public static int MSG_ADDGENERICTYPE = 403;
	public static int MSG_TRDOSFILENAME = 404;
	public static int MSG_OPENCODE = 405;
	
	public static int MSG_EXTFILENAME = 406;
	public static int MSG_PARTFILENAME = 407;
	public static int MSG_NONESELECTED = 408;
	public static int MSG_CODEFILES = 409;
	public static int MSG_NOTIMP = 410;
	public static int MSG_VALIDHEADER = 411;
	public static int MSG_PLUS3DOSFILELEN = 412;
	public static int MSG_DISKFILELENGTH = 413;
	public static int MSG_DOSVERSION = 414;
	public static int MSG_DOSISSUE = 415;
	public static int MSG_VARSTART = 416;
	public static int MSG_VARNAME = 417;
	public static int MSG_FILEWPLUS3DOSHDR = 418;
	public static int MSG_FILELENGTHXX = 419;
	public static int MSG_UPDATEFILETYPE = 420;
	public static int MSG_ERRORSHOWING = 421;
	public static int MSG_ERRORUPDATINGDIRENT = 422;
	public static int MSG_LENGTHWHEADER = 423;
	public static int MSG_LENGTHWOHEADER = 424;
	public static int MSG_USEDSECTORS = 425;
	public static int MSG_ARRAYNAME = 426;
	public static int MSG_CPMLENXX = 427;
	public static int MSG_LOGICALBLOCKS = 428;
	public static int MSG_RAWCPMFILE = 429;
	public static int MSG_BASIC = 430;
	public static int MSG_CODE = 431;
	public static int MSG_ARRAY = 432;
	public static int MSG_BLOCK = 433;
	public static int MSG_PULSECNT = 434;
	public static int MSG_PULSELEN = 435;
	public static int MSG_REPETITIONS  = 436;
	public static int MSG_RELJUMP = 437;
	public static int MSG_ACTTARGET = 438;
	public static int MSG_TDELAY = 439;
	public static int MSG_MSG = 440;
	public static int MSG_MAJOR = 441;
	public static int MSG_MINOR = 442;
	public static int MSG_XTAPE = 443;
	public static int MSG_STOPTAPE = 444;
	public static int MSG_PAUSE = 445;
	public static int MSG_HIGH = 446;
	public static int MSG_LOW = 447;
	public static int MSG_SIGNALLEVEL = 448;
	public static int MSG_FILEOFTYPE = 449;
	public static int MSG_PULSENUM = 450;
	public static int MSG_TSTATES = 451;
	public static int MSG_OFFSET = 452;
	public static int MSG_SAVEXASHEX = 453;
	public static int MSG_SAVEXASBIN = 454;
	public static int MSG_WRITING = 455;
	public static int MSG_BASICPROGRAM = 456;
	public static int MSG_UPDATESTARTLINE = 457;
	public static int MSG_UPDATEVARSTART = 458;
	public static int MSG_EXTRACTASTEXT = 459;
	public static int MSG_EXTRACTASBIN = 460;
	public static int MSG_EXTRACTASBINHEADER = 461;
	public static int MSG_EXTRACTASHEX = 462;
	public static int MSG_SAVEXASTEXT = 463;
	public static int MSG_ERRENC = 464;
	public static int MSG_UPDVARNAME = 465;
	public static int MSG_DIMENSIONS = 466;
	public static int MSG_SAVEARRAYAS = 467;
	public static int MSG_CODEFILE = 468;
	public static int MSG_UPDSTARTADDRESS = 469;
	public static int MSG_EXTRACTASIMG = 470;
	public static int MSG_EXTRACTASASM = 471;
	public static int MSG_EXTRACTASASMDESC = 472;
	public static int MSG_SAVEXASASM = 473;
	public static int MSG_SAVEXASIMG = 474;
	public static int MSG_STARTADDRSHOULDBE = 475;
	public static int MSG_TEXT = 476;
	public static int MSG_TEXTDESC = 477;
	public static int MSG_CONTENT = 478;
	public static int MSG_ERRORATXX = 479;
	public static int MSG_VARIABLES = 480;
	public static int MSG_NOVARS = 481;
	public static int MSG_LINENUM = 482;
	public static int MSG_LINE = 483;
	public static int MSG_VARIABLE = 484;
	public static int MSG_VARTYPE = 485;
	public static int MSG_CONTENTS = 486;
	public static int MSG_BASICPARSERR = 487;
	public static int MSG_INVALID = 488;
	public static int MSG_BADLINENO = 489;
	public static int MSG_REMCODE = 490;
	public static int MSG_BYTES = 491;
	public static int MSG_ENDOFVARS = 492;
	public static int MSG_UNKNOWNTYP = 493;
	public static int MSG_FAILEDTODECODE = 494;
	public static int MSG_VALUE = 495;
	public static int MSG_LIMIT = 496;
	public static int MSG_STEP = 497;
	public static int MSG_LOOPLINE = 498;
	public static int MSG_NEXTSTATE = 499;
	public static int MSG_FORNEXT = 500;
	public static int MSG_NUMBER = 501;
	public static int MSG_BADNUMARRAY = 502;
	public static int MSG_STRING = 503;
	public static int MSG_ALTFLAGS = 504;
	public static int MSG_7ffdLine = 505;
	public static int MSG_EXTRACTDISK = 506;
	public static int MSG_CONVERTSNAPSHOT = 507;
	public static int MSG_PAGEDINRAM128 = 508;
	public static int MSG_RAM48 = 509;
	public static int MSG_RAMBANKMSG = 510;
	public static int MSG_SELECTTARGETFOLDER = 511;
	public static int MSG_CANNOTWRITEBASICFILEIO = 512;
	public static int MSG_48SNASNAPSHOT = 513;
	public static int MSG_128SNASNAPSHOT = 514;
	public static int MSG_16SPSNAPSHOT = 515;
	public static int MSG_48SPSNAPSHOT = 516;
	public static int MSG_BADHEADER = 517;
	public static int MSG_VIEWASSPR = 518;
	public static int MSG_SPRITEWIDTH = 519;
	public static int MSG_SPRITEHEIGHT = 520;
	public static int MSG_DISPLACE = 521;
	public static int MSG_EXPORTSELASBIN = 522;
	public static int MSG_EXPORTSELASASM = 523;
	public static int MSG_SYSTEMVARS = 524;
	public static int MSG_FLAGSNOTDEF = 525;
	public static int MSG_FILEHWINFO = 526;
	public static int MSG_48Z80SNAPSHOT = 527;
	public static int MSG_128Z80SNAPSHOT = 528;
	
	
	
	
	
	
	
	
	
	
	
	public static int FILET_DSKFILE = 1300;
	public static int FILET_MDRFILE = 1301;
	public static int FILET_TRDFILE = 1302;
	public static int FILET_TAPFILE = 1303;
	public static int FILET_TZXFILE = 1304;

	public static int MSG_CONVERT = 2000;
	public static int MSG_OK = 2001;
	public static int MSG_CANCEL = 2002;
	public static int MSG_CLOSE = 2003;
	public static int MSG_IMPORT = 2004;
	public static int MSG_APPLY = 2005;
	public static int MSG_SAVE = 2006;
	public static int MSG_MODIFY = 2007;
	
	public static int MSG_MODIFIED = 3000;

	

	public Hashtable<Integer, String> Messages;
	public Hashtable<Integer, String> Default;
	
	public static String DefaultLang = "en";

	public Languages() {
		//use EN as the default languages
		load (DefaultLang);
		Default = Messages;
		//Load the current language
		Locale defaultLocale = Locale.getDefault();
		String language = defaultLocale.getLanguage().toLowerCase();
		if (!load(language)) {
			System.err.println("Cannot find current language, defaulting to '"+DefaultLang+"'. ");
			if (!load(DefaultLang)) {
				System.err.println("Cannot find default language");
			}
		}
	}

	public String Msg(int msg) {
		String result = Messages.get(msg);
		if (result == null) {
			result = Default.get(msg);
			if (result == null) {
				result = "Message '" + msg + "'";
			}
		}
		return (result);
	}

	private boolean load(String lang) {
		Messages = new Hashtable<Integer, String>();
		try {
			InputStream document = null;

			if (IsInJar()) {
				document = getClass().getResourceAsStream("lang/" + lang + ".xml");
			} else {
				File file = new File("src/resources/lang", lang + ".xml");
				document = new FileInputStream(file);
			}

			// Open file and build an XML doctree
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(document);
			doc.getDocumentElement().normalize();

			// Iterate all the opcode elements.
			NodeList nList = doc.getElementsByTagName("msg");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					// add it to the list.
					String msgid = eElement.getAttribute("id");
					String message = eElement.getTextContent();
					Messages.put(Integer.parseInt(msgid), message);
				}
			}

			return (true);
		} catch (IOException e) {
			System.err.println("Error initialising Language libarary for " + lang);
			System.err.println("IOException: " + e.getMessage());
			e.printStackTrace();

		} catch (SAXException e) {
			System.err.println("Error initialising Language libarary for " + lang);
			System.err.println("SAXexception: " + e.getMessage());
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			System.err.println("Error initialising Language libarary for " + lang);
			System.err.println("ParserConfigurationException: " + e.getMessage());
			e.printStackTrace();
		}
		return (false);
	}

	/**
	 * returns TRUE is running from a JAR file. (This affects where to find some of
	 * the files)
	 * 
	 * @return
	 */
	public boolean IsInJar() {
		@SuppressWarnings("rawtypes")
		Class me = getClass();
		return (me.getResource(me.getSimpleName() + ".class").toString().startsWith("jar:"));
	}

	public static void main(String[] args) {
		Languages l = new Languages();
		System.out.println(l.Msg(1));
		System.out.println(l.Msg(2));
	}
	
	
}

package hddEditor.libs;

/**
 * Implementation of the Speccy system variables.
 * 
 * This will load the sysvars.xml file from either the root if its being run from a JAR file 
 * or ./src/resources if its being run from an IDE.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SpeccySystemVariables {
	/*
	 * The system variable types.
	 */
	public static final int SV_BYTE = 1;
	public static final int SV_WORD = 2;
	public static final int SV_CHAR = 3;
	public static final int SV_SUBROUTINE = 4;
	public static final int SV_STACK = 5;
	public static final int SV_TRIPLE = 6;
	public static final int SV_FLOAT = 7;
	public static final int SV_COLOUR = 8;
	public static final int SV_FLAGS = 9;

	/**
	 * Machine types
	 */
	public static final int MT_ANY = 0; // used for searching only.
	public static final int MT_48K = 1;
	public static final int MT_128 = 2;
	public static final int MT_PLUS2A3 = 3;

	/**
	 * Stores for the three known system variable classes.
	 */
	public ArrayList<SystemVariable> Speccy48SystemVariables = null;
	public ArrayList<SystemVariable> Speccy128SystemVariables = null;
	public ArrayList<SystemVariable> SpeccyPlus3SystemVariables = null;

	// The store of known flags.
	public ArrayList<FlagStore> Flagstores = null;

	/**
	 * Store for one individual bit including its description, and what the bit
	 * state means.
	 */
	public class FlagBit {
		public int bit;
		public String FalseDesc;
		public String TrueDesc;
		public String description;

		public FlagBit(int bit, String fl, String tr, String desc) {
			this.bit = bit;
			this.FalseDesc = fl;
			this.TrueDesc = tr;
			this.description = desc;
		}
	}

	/**
	 * Store for one entire flag. This consists of the class name and 8 bits. Unused
	 * bits are NULL.
	 */
	public class FlagStore {
		public String name;
		public FlagBit FlagBits[];

		public FlagStore() {
			FlagBits = new FlagBit[8];
			for (int i = 0; i < 8; i++) {
				FlagBits[i] = null;
			}
		}

		@Override
		public String toString() {
			String result = "name: " + name + "; ";
			for (int i = 0; i < 8; i++) {
				if (FlagBits[i] != null) {
					result = result + i + ": " + FlagBits[i].description + "; ";
				} else {
					result = result + i + ": Invalid";
				}
			}
			return (result);
		}

	}

	/**
	 * Implementation of one system variable.
	 * 
	 * This contains the variable type (48, 128,+3), the variable address, its
	 * length, the type of the variable, its abbreviaion, description and
	 * descriptions of each individual bits for flags.
	 */
	public class SystemVariable {
		public int type;
		public int address;
		public int length;
		public int machinetype;
		public String abbrev;
		public String description;
		public FlagBit flagBitDescriptions[];

		public SystemVariable(int typ, int addr, int len, int machine, String abbrev, String desc) {
			this.flagBitDescriptions = new FlagBit[8];
			this.type = typ;
			this.address = addr;
			this.length = len;
			this.machinetype = machine;
			this.abbrev = abbrev;
			this.description = desc;
		}

		@Override
		public String toString() {
			String result = "";

			result = "Type: " + type;
			result = result + " Address: " + address;
			result = result + " Length: " + length;
			result = result + " Machine type: " + machinetype;
			result = result + " Abbrev: " + abbrev;
			result = result + " Desc: " + description;

			result = result + " Flags: ";
			if (type == SV_FLAGS) {
				for (FlagBit s : flagBitDescriptions) {
					if (s != null) {
						result = result + "'" + s.description + "'";
					} else {
						result = result + "''";
					}
					result = result + ",";
				}
				result = result.substring(0, result.length() - 1);
			} else {
				result = result + "N/A";
			}

			return (result);
		}
	}

	/**
	 * Load the sysvars.xml file.
	 */
	public SpeccySystemVariables() {
		InputStream document = null;
		try {
			if (IsInJar()) {
				document = getClass().getResourceAsStream("/sysvars.xml");
			} else {
				File file = new File("src/resources", "sysvars.xml");
				document = new FileInputStream(file);
			}

			// Open file and build an XML doctree
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(document);
			doc.getDocumentElement().normalize();

			// initialise the lists
			Speccy48SystemVariables = new ArrayList<SystemVariable>();
			Speccy128SystemVariables = new ArrayList<SystemVariable>();
			SpeccyPlus3SystemVariables = new ArrayList<SystemVariable>();
			Flagstores = new ArrayList<FlagStore>();

			/*
			 * Load the flag definitions
			 */
			NodeList nList = doc.getElementsByTagName("flag");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					String FlagName = eElement.getAttribute("abbrev");

					FlagStore fs = new FlagStore();
					fs.name = FlagName;

					NodeList bits = eElement.getChildNodes();

					for (int temp2 = 0; temp2 < bits.getLength(); temp2++) {
						Node bitNode = bits.item(temp2);
						if (bitNode.getNodeType() == Node.ELEMENT_NODE) {
							Element bitElement = (Element) bitNode;

							int bit = Integer.parseInt(bitElement.getAttribute("num"));
							String FalseDesc = bitElement.getAttribute("false");
							String TrueDesc = bitElement.getAttribute("true");
							String description = bitElement.getTextContent();

							FlagBit fb = new FlagBit(bit, FalseDesc, TrueDesc, description);

							fs.FlagBits[bit] = fb;
						}
					}
					Flagstores.add(fs);
				}
			}

			/*
			 * Load the System variables.
			 */
			nList = doc.getElementsByTagName("sysvar");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;

					String sAddress = eElement.getAttribute("address");
					String sMachine = eElement.getAttribute("machine");
					String svClass = eElement.getAttribute("class");
					String abbrev = eElement.getAttribute("abbrev");
					String descript = eElement.getTextContent();

					int iAddress = Integer.parseInt(sAddress, 16);

					// Convert the machine string into an enum.
					int iMachine = MT_48K;
					if (sMachine.equals("128")) {
						iMachine = MT_128;
					} else if (sMachine.equals("+3")) {
						iMachine = MT_PLUS2A3;
					}

					// Convert the class type into an enum
					int iClass = SV_FLAGS; // Default if we dont recognise class type
					int iLength = 1; // Default variable length (1 byte)
					String split[] = svClass.split("-"); // Split over '-'. There are two formats "xxxx" and "xxxx-num"
															// where num=variable length
					String ClassName = split[0];

					if (ClassName.toUpperCase().equals("BYTE")) {
						iClass = SV_BYTE;
					} else if (ClassName.toUpperCase().equals("WORD")) {
						iClass = SV_WORD;
						iLength = 2;
					} else if (ClassName.toUpperCase().equals("CHAR")) {
						iClass = SV_CHAR;
					} else if (ClassName.toUpperCase().equals("SUBROUTINE")) {
						iClass = SV_SUBROUTINE;
					} else if (ClassName.toUpperCase().equals("STACK")) {
						iClass = SV_STACK;
					} else if (ClassName.toUpperCase().equals("FLOAT")) {
						iClass = SV_FLOAT;
						iLength = 5;
					} else if (ClassName.toUpperCase().equals("COLOUR")) {
						iClass = SV_COLOUR;
					} else if (ClassName.toUpperCase().equals("TRIPLE")) {
						iClass = SV_TRIPLE;
						iLength = 3;
					}

					// check the override of the length. Eg, CHAR-10
					if (split.length > 1) {
						iLength = Integer.parseInt(split[1]);
					}

					// add it to the list.
					SystemVariable sv = new SystemVariable(iClass, iAddress, iLength, iMachine, abbrev, descript);

					// if we have a flag type, try to load the associated flag description.
					if (iClass == SV_FLAGS) {
						FlagStore FoundStore = null;
						for (FlagStore fs : Flagstores) {
							if (fs.name.equals(ClassName)) {
								FoundStore = fs;
							}
						}
						if (FoundStore != null) {
							for (FlagBit fb : FoundStore.FlagBits) {
								if (fb != null) {
									sv.flagBitDescriptions[fb.bit] = fb;
								}
							}
						} else {
							System.err.println("Cant find flag descriptions for variable: " + abbrev);
						}
					}

					// Add to the appropriate list
					switch (iMachine) {
					case SpeccySystemVariables.MT_48K:
						Speccy48SystemVariables.add(sv);
						break;
					case SpeccySystemVariables.MT_128:
						Speccy128SystemVariables.add(sv);
						break;
					case SpeccySystemVariables.MT_PLUS2A3:
						SpeccyPlus3SystemVariables.add(sv);
						break;
					}
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("Cant find sysvars.xml");
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			System.err.println("Cant config XML reader");
			e.printStackTrace();
		} catch (SAXException e) {
			System.err.println("Document sysvars.xml is invalid");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error openning sysvars.xml");
			e.printStackTrace();
		}

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
	
	/**
	 * Search for a system variable by its abbreviation.
	 * @param type
	 * @param abbrev
	 * @return
	 */
	public SystemVariable GetByAbbrev(int type, String abbrev) { 
		SystemVariable result = null;
		abbrev = abbrev.toUpperCase();
		
		if (type==MT_ANY || type == MT_PLUS2A3) {
			for (SystemVariable s:SpeccyPlus3SystemVariables) {
				if (s.abbrev.equals(abbrev)) 
					result = s;
			}
		}
		if (type==MT_ANY || type == MT_128) {
			for (SystemVariable s:Speccy128SystemVariables) {
				if (s.abbrev.equals(abbrev)) 
					result = s;
			}
		}
		if (type==MT_ANY || type == MT_48K) {
			for (SystemVariable s:Speccy48SystemVariables) {
				if (s.abbrev.equals(abbrev)) 
					result = s;
			}
		}
		return(result);
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Tests for SpeccySystemVariables:");
		SpeccySystemVariables ssv = new SpeccySystemVariables();
		System.out.println("Loaded...");
		System.out.println("Speccy 48 system variables: ");
		for (SystemVariable sv : ssv.Speccy48SystemVariables) {
			System.out.println("   " + sv);
		}
		System.out.println("Speccy 128 system variables: ");
		for (SystemVariable sv : ssv.Speccy128SystemVariables) {
			System.out.println("   " + sv);
		}
		System.out.println("Speccy +3 system variables: ");
		for (SystemVariable sv : ssv.SpeccyPlus3SystemVariables) {
			System.out.println("   " + sv);
		}

		
	}

}

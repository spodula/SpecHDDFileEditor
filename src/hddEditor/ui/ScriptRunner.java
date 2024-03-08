package hddEditor.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.Speccy;
import hddEditor.libs.SpeccyFileEncoders;
import hddEditor.libs.disks.ExtendedSpeccyBasicDetails;
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.PlusIDEDosException;
import hddEditor.libs.partitions.cpm.DirectoryEntry;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;
import hddEditor.libs.partitions.mdf.MicrodriveDirectoryEntry;
import hddEditor.libs.partitions.tap.TapDirectoryEntry;
import hddEditor.libs.partitions.trdos.TrdDirectoryEntry;

public class ScriptRunner {
	HDDEditor hdi = null;
	boolean LeaveOpen = false;

	/**
	 * Main function.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ScriptRunner sr = new ScriptRunner();
		sr.RunScript(args[0]);
	}

	/**
	 * 
	 * @param filename
	 */
	public void RunScript(String filename) {
		LeaveOpen = false;
		hdi = new HDDEditor();
		try {
			hdi.MakeForm();
			BufferedReader reader;
			try {
				reader = new BufferedReader(new FileReader(filename));
				String line = reader.readLine();
				int linenum = 1;
				while (line != null) {
					line = line.trim();
					if (!line.startsWith("#")) {
						if (!line.isBlank()) {
							ExecuteCommand(linenum, line);
						}
					}
					linenum++;
					// read next line
					line = reader.readLine();
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} finally {
			if (LeaveOpen) {
				hdi.UpdateDropdown();
				hdi.loop();
			}
		}
	}

	/**
	 * 
	 * @param linenum
	 * @param command
	 */
	public void ExecuteCommand(int linenum, String command) {
		try {
			command = command.trim();
			String restOfCommand = "";
			int spc = command.indexOf(' ');
			if (spc != -1) {
				restOfCommand = command.substring(spc);
				command = command.substring(0, spc).trim();
			}
			command = command.toLowerCase();

			if (command.equals("load")) {
				System.out.println(restOfCommand);
				hdi.LoadFile(restOfCommand.trim());
			} else if (command.equals("new")) {
				DoNew(restOfCommand);
			} else if (command.equals("show")) {
				DoShow(restOfCommand);
			} else if (command.equals("select")) {
				DoSelect(restOfCommand);
			} else if (command.equals("cat")) {
				DoCat(restOfCommand);
			} else if (command.equals("delete")) {
				DoDelete(restOfCommand);
			} else if (command.equals("rename")) {
				DoRename(restOfCommand);
			} else if (command.equals("add")) {
				DoAdd(restOfCommand);
			} else if (command.equals("export")) {
				DoExport(restOfCommand);
			} else {
				System.out.println("Unknown command: " + command + " params: " + restOfCommand);
			}
		} catch (Exception E) {
			System.out.println("Command " + command + " failed with error.");
			E.printStackTrace();
		}
	}

	/**
	 * SHOW command
	 * 
	 * Basically just leave the form open after creation.
	 * 
	 * @param restOfCommand
	 */
	private void DoShow(String restOfCommand) {
		LeaveOpen = true;
	}

	/**
	 * NEW command: Create a new disk image in the specified format or a new
	 * partition
	 * 
	 * @param restOfCommand
	 */
	private void DoNew(String restOfCommand) {
		boolean ShowOptions = false;
		String params[] = GeneralUtils.splitHandleQuotes(restOfCommand.trim());

		if (params.length == 0) {
			System.out.println("New what?");
			ShowOptions = true;
		} else {
			try {
				String disktype = params[0].toUpperCase();
				if (disktype.equals("PARTITION")) {
					if (hdi.CurrentHandler.SystemPart == null || hdi.CurrentHandler.SystemPart.DummySystemPartiton) {
						System.out.println("Cannot add new partitions to this media type.");
					} else {
						if (params.length != 4) {
							System.out.println("Expecting: new partition <name> <type> <size Mb>");
						}
						try {
							hdi.CurrentHandler.SystemPart.CreatePartition(params[2], Integer.valueOf(params[3]),
									Integer.valueOf(params[1]));
							System.out.println(">Partition " + params[2] + " created.");
						} catch (PlusIDEDosException E) {
							System.out.println(E.partition + ": " + E.getMessage());
						}
					}
				} else if (disktype.startsWith("HDF") || disktype.startsWith("IMG")) {
					boolean IsHDF = disktype.contains("HDF");
					boolean Is8Bit = disktype.contains("8 bit");
					int cyl = Integer.valueOf(params[1]);
					int hed = Integer.valueOf(params[2]);
					int spt = Integer.valueOf(params[3]);
					String filename = params[4];
					FileNewHDDForm FNHDD = new FileNewHDDForm(hdi.display);
					FNHDD.DoCreateFile(IsHDF, Is8Bit, filename, cyl, hed, spt);
					hdi.LoadFile(filename);
					System.out.println(">Disk " + params[4] + " created and loaded.");
				} else if (disktype.startsWith("DSK")) {
					FileNewFDDForm FNFDD = new FileNewFDDForm(hdi.display);
					FNFDD.DoCreateFile(params[1], "AMSTRAD", "", false, "", false);
					hdi.LoadFile(params[1]);
					System.out.println(">Disk " + params[1] + " created and loaded.");
				} else if (disktype.startsWith("EDSK")) {
					FileNewFDDForm FNFDD = new FileNewFDDForm(hdi.display);
					FNFDD.DoCreateFile(params[1], "AMSTRAD", "", true, "", false);
					hdi.LoadFile(params[1]);
					System.out.println(">Disk " + params[1] + " created and loaded.");
				} else if (disktype.startsWith("MDF")) {
					FileNewFDDForm FNFDD = new FileNewFDDForm(hdi.display);
					FNFDD.DoCreateFile(params[2], "MICRODRIVE", params[1], false, "", false);
					hdi.LoadFile(params[2]);
					System.out.println(">Cart " + params[2] + " created and loaded.");
				} else if (disktype.startsWith("TAP")) {
					FileNewFDDForm FNFDD = new FileNewFDDForm(hdi.display);
					FNFDD.DoCreateFile(params[1], "TAP", params[1], false, "", false);
					hdi.LoadFile(params[1]);
					System.out.println(">Tape " + params[1] + " created and loaded.");
				} else if (disktype.startsWith("TZX")) {
					FileNewFDDForm FNFDD = new FileNewFDDForm(hdi.display);
					FNFDD.DoCreateFile(params[1], "TZX", params[1], false, "", false);
					hdi.LoadFile(params[1]);
					System.out.println(">TZX " + params[1] + " created and loaded.");
				} else if (disktype.startsWith("TRD")) {
					FileNewFDDForm FNFDD = new FileNewFDDForm(hdi.display);
					String TRDOSFormat = params[1] + " TRACKS " + params[2] + " HEADS";
					FNFDD.DoCreateFile(params[4], "TR-DOS", params[3], false, TRDOSFormat, false);
					hdi.LoadFile(params[4]);
					System.out.println(">Disk " + params[4] + " created and loaded.");
				} else if (disktype.startsWith("SCL")) {
					FileNewFDDForm FNFDD = new FileNewFDDForm(hdi.display);
					FNFDD.DoCreateFile(params[2], "TR-DOS", params[1], false, "", true);
					hdi.LoadFile(params[2]);
					System.out.println(">Disk " + params[2] + " created and loaded.");
				} else {
					System.out.println(">>Unidentified disk type: " + disktype);
					ShowOptions = true;
				}
			} catch (Exception E) {
				System.out.println("Could not create file: " + E.getMessage());
				System.out.println("Command: " + restOfCommand);
				for (int i = 0; i < params.length; i++) {
					System.out.println(i + ":" + params[i]);
				}
				ShowOptions = true;
			}
		}

		if (ShowOptions) {
			System.out.println("Options: ");
			System.out.println("  HDF8 HDF16 - 8 or 16 bit Ramsoft HDF file");
			System.out.println("     eg. new HDF8 [cyl] [heads] [spt] <filename>");
			System.out.println("  IMG8 IMG16 - 8 or 16 bit RAW file");
			System.out.println("     eg. new IMG8 [cyl] [heads] [spt] <filename>");
			System.out.println("  DSK EDSK - Amstrad/+3 Disk");
			System.out.println("     eg. new EDSK <filename>");
			System.out.println("  MDF - Sinclair microdrive cart");
			System.out.println("     eg. new MDF <label> <filename>");
			System.out.println("  TAP - Basic TAP file");
			System.out.println("     eg. new TAP <filename>");
			System.out.println("  TRD - TR-DOS file (Linear)");
			System.out.println("     eg. new TRD [40|80] [1|2] <label> <filename>");
			System.out.println("  SCL - TR-DOS file (Compressed)");
			System.out.println("     eg. new SCL <label> <filename>");
			System.out.println("  PARTITION <type num> <Size mb> <name>");
			System.out.println("     eg. new PARTITION 3 testpart 16");
		}
	}

	/**
	 * Select a partition
	 * 
	 * @param restOfCommand
	 */
	private void DoSelect(String restOfCommand) {
		String params[] = GeneralUtils.splitHandleQuotes(restOfCommand.trim());

		if (params.length != 1) {
			System.out.println("Select which partition?");
			System.out.println("Options: ");
			System.out.println("  <partition to select>\n   eg. select testpart\n");
		} else {
			hdi.GotoPartitionByName(params[0]);
			System.out.println(">Partition " + params[0] + " selected.");
		}
	}

	/**
	 * Display a catalog
	 * 
	 * @param restOfCommand
	 */
	private void DoCat(String restOfCommand) {
		if (hdi.CurrentSelectedPartition == null) {
			System.out.println("cat: Select a partition first");
		} else {
			IDEDosPartition part = hdi.CurrentSelectedPartition;
			int numfiles = 0;
			System.out.println("Catalog of " + part.GetName());
			FileEntry entries[] = part.GetFileList(restOfCommand);
			for (FileEntry fe : entries) {
				String line = fe.GetFilename();
				while (line.length() < 14)
					line = line + " ";
				line = line + fe.GetFileTypeString();
				while (line.length() < 26)
					line = line + " ";
				line = line + fe.GetFileSize();
				while (line.length() < 33)
					line = line + " ";
				SpeccyBasicDetails sbd = fe.GetSpeccyBasicDetails();
				switch (sbd.BasicType) {
				case Speccy.BASIC_BASIC:
					line = line + "Line: " + sbd.LineStart + " Vars: " + sbd.VarStart;
					break;
				case Speccy.BASIC_CODE:
					line = line + "Load address: " + sbd.LoadAddress;
					break;
				default:
					break;
				}

				numfiles++;
				System.out.println(line);
			}
			System.out.println("\n  " + numfiles + " files found.");
		}
	}

	/**
	 * 
	 * 
	 * @param restOfCommand
	 * @throws PlusIDEDosException
	 */
	private void DoDelete(String restOfCommand) {
		boolean ShowOptions = false;
		String params[] = GeneralUtils.splitHandleQuotes(restOfCommand.trim());

		if (params.length != 2) {
			System.out.println("Delete what?");
			ShowOptions = true;
		} else {
			String delType = params[0].toLowerCase();
			String delName = params[1];
			if (delType.equals("partition")) {
				try {
					// Delete partition
					hdi.CurrentHandler.SystemPart.DeletePartition(delName);
					// force a UI reload.
					hdi.GotoPartitionByName("PLUSIDEDOS");
					System.out.println(">Deleted partition " + delName);
				} catch (PlusIDEDosException e) {
					System.out.println("Error deleting partition: " + e.getMessage());
				}
			} else if (delType.equals("file")) {
				if (hdi.CurrentSelectedPartition == null) {
					System.out.println("delete: Select a partition first");
				} else {
					IDEDosPartition part = hdi.CurrentSelectedPartition;
					try {
						part.DeleteFile(delName);
					} catch (IOException e) {
						System.out.println("Error deleting file '" + delName + "'. " + e.getMessage());
					}
				}
			} else {
				System.out.println("I dont know how to delete " + delType + " Expecting [partition|file]");
				ShowOptions = true;
			}
		}
		if (ShowOptions) {
			System.out.println("Options: ");
			System.out.println("  File <filename>");
			System.out.println("     eg. delete file bob.txt");
			System.out.println("  partition <partitionname>");
			System.out.println("     eg. delete partition testpart\n\n");

		}
	}

	/**
	 * 
	 * @param restOfCommand
	 */
	private void DoRename(String restOfCommand) {
		boolean ShowOptions = false;
		String params[] = GeneralUtils.splitHandleQuotes(restOfCommand.trim());

		if (params.length != 2) {
			System.out.println("Expecting Rename <from> <to>");
			ShowOptions = true;
		} else {
			String srcFile = params[0].trim();
			String destFile = params[1].trim();
			if (hdi.CurrentSelectedPartition == null) {
				System.out.println("delete: Select a partition first");
			} else {
				IDEDosPartition part = hdi.CurrentSelectedPartition;
				if ((part.GetPartType() == PLUSIDEDOS.PARTITION_PLUS3DOS)
						|| (part.GetPartType() == PLUSIDEDOS.PARTITION_TAPE_SINCLAIRMICRODRIVE)
						|| (part.GetPartType() == PLUSIDEDOS.PARTITION_TAPE_TAP)
						|| (part.GetPartType() == PLUSIDEDOS.PARTITION_DISK_TRDOS)
						|| (part.GetPartType() == PLUSIDEDOS.PARTITION_CPM)) {
					try {
						part.RenameFile(srcFile, destFile);
					} catch (IOException e) {
						System.out.println("Error Renaming file '" + srcFile + "'. " + e.getMessage());
					}
				} else if (part.GetPartType() == PLUSIDEDOS.PARTITION_SYSTEM) {
					System.out.println("System partition selected. Use 'delete partition' to remove partitions.");
				} else {
					System.out.println("delete: Currently selected partition (" + hdi.CurrentSelectedPartition.GetName()
							+ ") does not have file support");
				}
			}
		}

		if (ShowOptions) {
			System.out.println("Options: ");
			System.out.println("  Rename <from> <to>");
			System.out.println("     eg. rename x.txt xdxx.txt\n");
		}
	}

	/**
	 * 
	 * @param restOfCommand
	 */
	private void DoExport(String restOfCommand) {
		boolean ShowOptions = false;
		String params[] = GeneralUtils.splitHandleQuotes(restOfCommand.trim());

		if (params.length != 3) {
			System.out.println("Expecting Export <targetfolder> <wildcard> <type>");
			ShowOptions = true;
		} else {
			if (hdi.CurrentSelectedPartition == null) {
				System.out.println("Export: Select a partition first");
			} else {

				String TargetFolder = params[0];
				String Wildcard = params[1];
				String Type = params[2].trim().toLowerCase();
				IDEDosPartition part = hdi.CurrentSelectedPartition;
				FileEntry entries[] = part.GetFileList(Wildcard);
				for (FileEntry entry : entries) {
					try {
						byte rawdata[] = null;
						byte filedata[] = null;
						int filelength = 0;
						int SpeccyFileType = Speccy.BASIC_CODE;
						int basicline = 32768;
						int basicVarsOffset = 0;
						int codeLoadAddress = 0;
						String arrayVarName = "A";
						int actiontype = GeneralUtils.EXPORT_TYPE_RAW;
						switch (part.GetPartType()) {
						case PLUSIDEDOS.PARTITION_CPM:
						case PLUSIDEDOS.PARTITION_PLUS3DOS:
							DirectoryEntry de = (DirectoryEntry) entry;
							rawdata = de.GetFileData();
							Plus3DosFileHeader p3d = de.GetPlus3DosHeader();
							if (p3d != null && p3d.IsPlusThreeDosFile) {
								filedata = new byte[rawdata.length - 0x80];
								System.arraycopy(rawdata, 0x80, filedata, 0, filedata.length);
								filelength = p3d.filelength;
								SpeccyFileType = p3d.filetype;
								basicline = p3d.line;
								basicVarsOffset = p3d.VariablesOffset;
								codeLoadAddress = p3d.loadAddr;
								arrayVarName = (p3d.VarName + "A").substring(0, 1);
							} else {
								filedata = rawdata;
								filelength = rawdata.length;
								codeLoadAddress = 0;
							}
							break;
						case PLUSIDEDOS.PARTITION_TAPE_SINCLAIRMICRODRIVE:
							MicrodriveDirectoryEntry mde = (MicrodriveDirectoryEntry) entry;
							SpeccyBasicDetails sbd = entry.GetSpeccyBasicDetails();
							filedata = mde.GetFileData();
							rawdata = mde.GetFileRawData();
							filelength = mde.GetFileSize();
							SpeccyFileType = sbd.BasicType;
							basicline = sbd.LineStart;
							basicVarsOffset = sbd.VarStart;
							codeLoadAddress = mde.GetVar2();
							arrayVarName = "A";
							break;
						case PLUSIDEDOS.PARTITION_TAPE_TAP:
							TapDirectoryEntry tde = (TapDirectoryEntry) entry;
							ExtendedSpeccyBasicDetails tsbd = (ExtendedSpeccyBasicDetails) entry
									.GetSpeccyBasicDetails();
							filedata = tde.GetFileData();
							rawdata = tde.GetFileRawData();
							filelength = tde.GetFileSize();
							SpeccyFileType = tsbd.BasicType;
							basicline = tsbd.LineStart;
							basicVarsOffset = tsbd.VarStart;
							codeLoadAddress = tsbd.LoadAddress;
							arrayVarName = tsbd.VarName + "";
							break;
						case PLUSIDEDOS.PARTITION_DISK_TRDOS:
							TrdDirectoryEntry trd = (TrdDirectoryEntry) entry;
							filedata = trd.GetFileData();
							rawdata = filedata;
							filelength = trd.GetFileSize();
							basicline = trd.startline;
							basicVarsOffset = trd.GetVar2();
							codeLoadAddress = trd.GetVar1();
							arrayVarName = "A";
							switch (trd.GetFileType()) {
							case 'B':
								SpeccyFileType = Speccy.BASIC_BASIC;
								break;
							case 'D':
								SpeccyFileType = Speccy.BASIC_NUMARRAY;
								if (trd.IsCharArray())
									SpeccyFileType = Speccy.BASIC_CHRARRAY;
							default:
								SpeccyFileType = Speccy.BASIC_CODE;
							}
							break;
						}
						// decide on the action type.
						if (Type.equals("astype")) {
							switch (SpeccyFileType) {
							case Speccy.BASIC_BASIC:
								actiontype = GeneralUtils.EXPORT_TYPE_TXT;
								break;
							case Speccy.BASIC_NUMARRAY:
								actiontype = GeneralUtils.EXPORT_TYPE_CSV;
								break;
							case Speccy.BASIC_CHRARRAY:
								actiontype = GeneralUtils.EXPORT_TYPE_CSV;
								break;
							case Speccy.BASIC_CODE:
								actiontype = GeneralUtils.EXPORT_TYPE_HEX;
								if (filelength == 6912) {
									actiontype = GeneralUtils.EXPORT_TYPE_PNG;
								}
								break;
							}
						} else if (Type.equals("raw")) {
							actiontype = GeneralUtils.EXPORT_TYPE_RAW;
						} else if (Type.equals("rawheader")) {
							actiontype = GeneralUtils.EXPORT_TYPE_RAWANDHEADER;
						} else if (Type.equals("hex")) {
							actiontype = GeneralUtils.EXPORT_TYPE_HEX;
						} else if (Type.equals("asm")) {
							actiontype = GeneralUtils.EXPORT_TYPE_ASM;
						}

						Speccy.SaveFileToDiskAdvanced(new File(TargetFolder, entry.GetFilename().trim()), filedata,
								rawdata, filelength, SpeccyFileType, basicline, basicVarsOffset, codeLoadAddress,
								arrayVarName, actiontype);
						System.out.println("Written " + entry.GetFilename().trim());
					} catch (Exception E) {
						System.out.println("Error writing: '" + entry.GetFilename() + "'" + E.getMessage());
					}
				}
			}
		}

		if (ShowOptions) {
			System.out.println("Options: ");
			System.out.println("  Export <targetfolder> <wildcard> <type>");
			System.out.println("Where: type is one of: ");
			System.out.println(" * raw       - Raw binary file in the data");
			System.out.println(" * rawheader - Raw binary file in the data Along with any Microdrive or +3DOS header");
			System.out.println(" * hex       - Hex dump of the file");
			System.out.println(" * asm       - A Disassembly of the file");
			System.out.println(
					" * astype    - For Basic - Text file, Array - CSV file, code length 6912 - PNG file, All others, Hex ");
		}
	}

	/**
	 * 
	 * @param restOfCommand
	 */
	private void DoAdd(String restOfCommand) {
		boolean ShowOptions = false;
		String params[] = GeneralUtils.splitHandleQuotes(restOfCommand.trim());

		if (params.length < 3) {
			System.out.println("Expecting add <file> <targetname> <type> <params>");
			ShowOptions = true;
		} else {
			if (hdi.CurrentSelectedPartition == null) {
				System.out.println("Add: Select a partition first");
			} else {
				String Sourcefile = params[0];
				String Targetfile = params[1];
				String Type = params[2].trim().toLowerCase();

				int BasicStartLine = 32768;
				String varname = "A";
				int CodeLoadAddr = 0;
				int intensity = 50;
				boolean isbw = false;
				int csvLineLimit = 2000;

				for (int i = 3; i < params.length; i++) {
					String param = params[i];
					int x = param.indexOf('=');
					if (x == -1) {
						System.out.println("Add: Dont understand " + param + " Ignoring.");
					} else {
						String paramName = param.substring(0, x).toLowerCase();
						String paramData = param.substring(x + 1);
						if (paramName.equals("codeload")) {
							CodeLoadAddr = Integer.valueOf(paramData);
						} else if (paramName.equals("variable")) {
							varname = (paramData + "A").substring(0, 1);
						} else if (paramName.equals("line")) {
							BasicStartLine = Integer.valueOf(paramData);
						} else if (paramName.equals("bw")) {
							if (paramData.length() > 0 && paramData.toUpperCase().charAt(0) == 'T') {
								isbw = true;
							} else {
								isbw = false;
							}
						} else if (paramName.equals("intensity")) {
							intensity = Integer.valueOf(paramData);
						} else if (paramName.equals("arraylinelimit")) {
							csvLineLimit = Integer.valueOf(paramData);
						} else {
							System.out.println("add: Dont understand " + param + " Ignoring.");
						}
					}
				}

				File fFolder = new File(Sourcefile).getParentFile();
				String wildcard = new File(Sourcefile).getName();
				// If the folder isnt specified, use the CWD.
				if (fFolder == null) {
					fFolder = new File(".").getAbsoluteFile().getParentFile();
				}

				FileFilter ff = new WildcardFileFilter(wildcard);
				File files[] = fFolder.listFiles(ff);
				for (File file : files) {
					/*
					 * Read the file into memory decoding if required
					 */
					if (file.isFile()) {
						byte rawdata[] = null;
						try {
							int BasicFileType = Speccy.BASIC_CODE;
							if (Type.equals("text")) {
								rawdata = SpeccyFileEncoders.EncodeTextFileToBASIC(file);
								BasicFileType = Speccy.BASIC_BASIC;
							} else if (Type.equals("image")) {
								rawdata = SpeccyFileEncoders.LoadImage(hdi.display, intensity, file, isbw);
								BasicFileType = Speccy.BASIC_CODE;
							} else if (Type.equals("numarray")) {
								// CSV file for numeric array
								rawdata = SpeccyFileEncoders.EncodeNumericArray(file, csvLineLimit);
								BasicFileType = Speccy.BASIC_NUMARRAY;
							} else if (Type.equals("chrarray")) {
								// CSV for character array
								rawdata = SpeccyFileEncoders.EncodeCharacterArray(file, csvLineLimit);
								BasicFileType = Speccy.BASIC_CHRARRAY;
							} else if (Type.equals("code")) {
								// Treat as raw data
								rawdata = GeneralUtils.ReadFileIntoArray(file.getAbsolutePath());
								BasicFileType = Speccy.BASIC_CODE;
							}
							/*
							 * Write the file to the partition.
							 */
							if (rawdata != null) {
								IDEDosPartition part = hdi.CurrentSelectedPartition;
								switch (BasicFileType) {
								case Speccy.BASIC_BASIC:
									part.AddBasicFile(Targetfile, rawdata, BasicStartLine, rawdata.length);
									break;
								case Speccy.BASIC_CODE:
									part.AddCodeFile(Targetfile, CodeLoadAddr, rawdata);
									break;
								case Speccy.BASIC_CHRARRAY:
									part.AddCharArray(Targetfile, rawdata, varname);
									break;
								case Speccy.BASIC_NUMARRAY:
									part.AddNumericArray(Targetfile, rawdata, varname);
									break;
								default:
									part.AddCodeFile(Targetfile, 0, rawdata);
									break;
								}
								System.out.println("Added " + file.getName());
							}
						} catch (IOException E) {
							System.out.println("Error processing file: " + file.getName() + " " + E.getMessage());
						}
					}
				}
			}
		}

		if (ShowOptions) {
			System.out.println("Options: ");
			System.out.println("  add <file> <targetname> <type> <params>");
			System.out.println("Where: type is one of: ");
			System.out.println(" * text - Basic as text");
			System.out.println(" * image - Image file as Screen$");
			System.out.println(" * NumArray - Numeric array");
			System.out.println(" * ChrArray - Character array");
			System.out.println(" * Code");
			System.out.println("and Params are space seperated:");
			System.out.println(" * codeload=<address>");
			System.out.println(" * variable=varname");
			System.out.println(" * line=<BASIC start line>");
			System.out.println(" * bw=<true|false>");
			System.out.println(" * intensity=[0..100]");
			System.out.println(" * arraylinelimit=<max lines to load>");
			System.out.println("Eg.");
			System.out.println("  add c:/temp/test.jpeg test.scr image bw=false intensity=80");
		}
	}

}

package hddEditor.ui.partitionPages;
/**
 * Partition page for floppy boot track..
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.libs.ASMLib;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.Languages;
import hddEditor.libs.ASMLib.DecodedASM;
import hddEditor.libs.CPM;
import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.disks.FDD.FloppyDisk;
import hddEditor.libs.partitions.FloppyBootTrack;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.ui.HDDEditor;

public class FloppyBootTrackPage extends GenericPage {
	private Table HexTable = null;
	
	/**
	 * 
	 * @param root
	 * @param parent
	 * @param partition
	 */
	public FloppyBootTrackPage(HDDEditor root, Composite parent, IDEDosPartition partition, FileSelectDialog fsd, Languages lang) {
		super(root, parent, partition, fsd, lang);
		AddComponents();
	}

	/**
	 * 
	 */
	private void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			FloppyBootTrack fbc = (FloppyBootTrack) partition;
			FloppyDisk Disk = (FloppyDisk) fbc.CurrentDisk;
			super.AddBasicDetails();
			label("",4);
			Label lbl = label(lang.Msg(Languages.MSG_XDPBINFO)+":", 4);
			FontData fontData = lbl.getFont().getFontData()[0];
			Font font = new Font(ParentComp.getDisplay(),
					new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
			lbl.setFont(font);

			try {
				byte data[] = fbc.GetDataInPartition(0, Disk.SectorSize);

				String ChecksumStatus = lang.Msg(Languages.MSG_NOTBOOTABLE);
				int cs = (fbc.checksum + fbc.fiddleByte) & 0xff;
				if (cs == 3) {
					ChecksumStatus = lang.Msg(Languages.MSG_BPLUS3DPS);
				} else if (cs == 1) {
					ChecksumStatus = lang.Msg(Languages.MSG_BPCW9512);
				} else if (cs == 255) {
					ChecksumStatus = lang.Msg(Languages.MSG_BPCW8256);
				}

				label(lang.Msg(Languages.MSG_FORMAT)+": " + fbc.diskformat + " (" + fbc.disktype + ")", 1);
				label(lang.Msg(Languages.MSG_SECTORS)+": " + fbc.numsectors, 1);
				label(lang.Msg(Languages.MSG_SECTSZ)+": " + fbc.sectorSize + " (" + fbc.sectorPow + ")", 1);
				label(lang.Msg(Languages.MSG_RESERVEDTR)+": " + fbc.reservedTracks, 1);
				
				label(lang.Msg(Languages.MSG_BLOCKSZ)+": "  + fbc.blockSize + " (" + fbc.blockPow + ")", 1);
				label(lang.Msg(Languages.MSG_DIRBLOCKS)+": " + fbc.dirBlocks, 1);
				label(lang.Msg(Languages.MSG_RWGAP)+": " + fbc.rwGapLength, 1);
				label(lang.Msg(Languages.MSG_FMTGAP)+": " + fbc.fmtGapLength, 1);
				
				label(lang.Msg(Languages.MSG_CSUM)+": " + cs, 1);
				label(lang.Msg(Languages.MSG_BOOTABLE)+": " + ChecksumStatus, 1);
				label(lang.Msg(Languages.MSG_MAXCPMB)+": " + fbc.maxblocks, 1);
				label(lang.Msg(Languages.MSG_RESERVEDB)+": " + fbc.reservedblocks, 1);
				
				label(lang.Msg(Languages.MSG_MAXDIRENTS)+": " + fbc.maxDirEnts, 1);
				label(lang.Msg(Languages.MSG_BPB)+": " + fbc.BlockIDWidth, 1);
				label(lang.Msg(Languages.MSG_DISKSZ)+": " + fbc.diskSize + "k", 1);
				label(lang.Msg(Languages.MSG_SECTORR)+": " + Disk.diskTracks[0].minsectorID + "-" + Disk.diskTracks[0].maxsectorID, 1);
				
				label(lang.Msg(Languages.MSG_IDTYPE)+": " +fbc.Identifiedby,4);
				
				label("",4);
				
				lbl = label(lang.Msg(Languages.MSG_BOOTSECTCODE)+":", 4);
				lbl.setFont(font);
				
				AddAsm(data);
				
				Button SaveBootCode = new Button(ParentComp,SWT.BORDER);
				SaveBootCode.setText(lang.Msg(Languages.MSG_SAVEASM));
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
				gridData.widthHint = 200;
				SaveBootCode.setLayoutData(gridData);
				SaveBootCode.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent arg0) {
						DoSaveAsm(data);
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent arg0) {
						widgetSelected(arg0);
					}
				});
				Button SaveRawBootCode = new Button(ParentComp,SWT.BORDER);
				SaveRawBootCode.setText(lang.Msg(Languages.MSG_SAVERAWBS));
				gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
				gridData.widthHint = 200;
				SaveRawBootCode.setLayoutData(gridData);
				SaveRawBootCode.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent arg0) {
						DoSaveRawData(data);
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent arg0) {
						widgetSelected(arg0);
					}
				});
			} catch (IOException e) {
				System.out.println(lang.Msg(Languages.MSG_BSLOADERR));
			}
			ParentComp.pack();
		}
	}
	
	/**
	 * Handler for the "Save Raw Data" button
	 * @param data
	 */
	protected void DoSaveRawData(byte[] data) {
		String defaultfilename = new File(partition.CurrentDisk.GetFilename()).getName();
		
		File Selected = fsd.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES, lang.Msg(Languages.MSG_SAVERAWBS), new String[] {"*"},defaultfilename+".raw");
		if (Selected != null) {
			GeneralUtils.WriteBlockToDisk(data, Selected);
		}
	}

	/**
	 * Handler for the "Save assembly" button.
	 * 
	 * @param data
	 */
	private void DoSaveAsm(byte data[]) {
		String defaultfilename = new File(partition.CurrentDisk.GetFilename()).getName();
		File Selected = fsd.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES, lang.Msg(Languages.MSG_SAVEASM), new String[] {"*.asm"},defaultfilename+".asm");
		if (Selected != null) {
			FileOutputStream file;
			try {
				file = new FileOutputStream(Selected);
				file.write((lang.Msg(Languages.MSG_BSOFFILE)+": " + partition.CurrentDisk.GetFilename() + System.lineSeparator()).getBytes());
				file.write((lang.Msg(Languages.MSG_ORG)+": 0xfe00" + System.lineSeparator()).getBytes());
				file.write((lang.Msg(Languages.MSG_LENGTH)+": 512 bytes" + System.lineSeparator() + System.lineSeparator()).getBytes());
				try {
					ASMLib asm = new ASMLib(lang);
					int loadedaddress = 0xfe00;
					int realaddress = 0x0080;
					try {
						int asmData[] = new int[5];
						while (realaddress < data.length) {
							String chrdata = "";
							for (int i = 0; i < 5; i++) {
								int d = 0;
								if (realaddress + i < data.length) {
									d = (int) data[realaddress + i] & 0xff;
								}
								asmData[i] = d;

								if ((d > 0x1F) && (d < 0x7f)) {
									chrdata = chrdata + (char) d;
								} else {
									chrdata = chrdata + "?";
								}
							}
							int decLen=0;
							String decStr="";
							if (loadedaddress < 0xfe10) {
								if (loadedaddress < 0xfe0a) {
									decLen = 1;
									String label = lang.Msg(CPM.AMSHDRDescriptions[loadedaddress - 0xfe00]);
									decStr = String.format("defb %d ; %s", asmData[0] & 0xff, label);
									
								} else if (loadedaddress < 0xfe0f) {
									decLen = 5;
									decStr = "DEFB ";
									String s = "";
									for(int i:asmData) {
										s = s + ", "+String.valueOf(i);
									}
									decStr = decStr +s.substring(2)+" ; "+lang.Msg(Languages.MSG_RESERVED);
								} else {
									decLen = 1;
									decStr = String.format("DEFB %d ; "+lang.Msg(Languages.MSG_CHECKSUM), asmData[0] & 0xff);
								}
							} else {
								// decode instruction
								DecodedASM Instruction = asm.decode(asmData, loadedaddress);
								decLen = Instruction.length;
								decStr = Instruction.instruction;
							}

							// output it. - First, assemble a list of hex bytes, but pad out to 12 chars
							// (4x3)
							String hex = "";
							for (int j = 0; j < decLen; j++) {
								hex = hex + String.format("%02X", asmData[j]) + " ";
							}

							file.write(String.format("%04X", loadedaddress).getBytes());
							file.write(("\t" + hex + "\t\t" + decStr + "\t\t"
									+ chrdata.substring(0, decLen)).getBytes());
							file.write(System.lineSeparator().getBytes());

							realaddress = realaddress + decLen;
							loadedaddress = loadedaddress + decLen;

						} // while
					} catch (Exception E) {						
						System.out.println(String.format(lang.Msg(Languages.MSG_ERRORATADDR), realaddress,loadedaddress));
						System.out.println(E.getMessage());
						E.printStackTrace();
					}
				} finally {
					file.close();
				}

			} catch (FileNotFoundException e) {
				ErrorBox(lang.Msg(Languages.MSG_ERRSAVING)+" - "+lang.Msg(Languages.MSG_DIRNOTFOUND));
				e.printStackTrace();
			} catch (IOException e) {
				ErrorBox(lang.Msg(Languages.MSG_ERRSAVING)+" - "+lang.Msg(Languages.MSG_IOERROR) + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Render the code as ASM.
	 * @param data
	 * @param startaddress
	 */
	private void AddAsm(byte data[]) {
		if (HexTable!=null) {
			if (!HexTable.isDisposed()) {
				HexTable.dispose();
				HexTable = null;
			}
		}
		HexTable = new Table(ParentComp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		HexTable.setLinesVisible(true);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = 400;
		HexTable.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(HexTable, SWT.LEFT);
		tc1.setText(lang.Msg(Languages.MSG_ADDRESS));
		tc1.setWidth(80);
		TableColumn tc2 = new TableColumn(HexTable, SWT.LEFT);
		tc2.setText(lang.Msg(Languages.MSG_HEX));
		tc2.setWidth(160);
		TableColumn tc3 = new TableColumn(HexTable, SWT.LEFT);
		tc3.setText(lang.Msg(Languages.MSG_ASM));
		tc3.setWidth(300);
		TableColumn tc4 = new TableColumn(HexTable, SWT.LEFT);
		tc4.setText(lang.Msg(Languages.MSG_CHR));
		tc4.setWidth(40);
		HexTable.setHeaderVisible(true);

		ASMLib asm = new ASMLib(lang);
		int loadedaddress = 0xfe00;
		int realaddress = 0x0000;
		int asmData[] = new int[5];
		try {
			while (realaddress < data.length) {
				String chrdata = "";
				for (int i = 0; i < 5; i++) {
					int d = 0;
					if (realaddress + i < data.length) {
						d = (int) data[realaddress + i] & 0xff;
					}
					asmData[i] = d;

					if ((d > 0x1F) && (d < 0x7f)) {
						chrdata = chrdata + (char) d;
					} else {
						chrdata = chrdata + "?";
					}
				}
				
				int decLen=0;
				String decStr="";
				if (loadedaddress < 0xfe10) {
					if (loadedaddress < 0xfe0a) {
						decLen = 1;
						String label = lang.Msg(CPM.AMSHDRDescriptions[loadedaddress - 0xfe00]);
						decStr = String.format("defb %d ; %s", asmData[0] & 0xff, label);
					} else if (loadedaddress < 0xfe0f) {
						decLen = 5;
						decStr = "DEFB ";
						String s = "";
						for(int i:asmData) {
							s = s + ", "+String.valueOf(i);
						}
						decStr = decStr +s.substring(2)+" ; "+lang.Msg(Languages.MSG_RESERVED);
					} else {
						decLen = 1;
						decStr = String.format("DEFB %d ; "+lang.Msg(Languages.MSG_CHECKSUM), asmData[0] & 0xff);
					}
				} else {
					// decode instruction
					DecodedASM Instruction = asm.decode(asmData, loadedaddress);
					decLen = Instruction.length;
					decStr = Instruction.instruction;
				}
				// output it. - First, assemble a list of hex bytes, but pad out to 12 chars
				// (4x3)
				String hex = "";
				for (int j = 0; j < decLen; j++) {
					hex = hex + String.format("%02X", asmData[j]) + " ";
				}

				TableItem Row = new TableItem(HexTable, SWT.NONE);
				String dta[] = new String[4];
				dta[0] = String.format("%04X", loadedaddress);
				dta[1] = hex;
				dta[2] = decStr;
				dta[3] = chrdata.substring(0, decLen);

				Row.setText(dta);

				realaddress = realaddress + decLen;
				loadedaddress = loadedaddress + decLen;

			} // while
		} catch (Exception E) {
			System.out.println(String.format(lang.Msg(Languages.MSG_ERRORATADDR), realaddress,loadedaddress));
			System.out.println(E.getMessage());
			E.printStackTrace();
		}
	}
}

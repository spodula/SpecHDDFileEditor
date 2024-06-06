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
	public FloppyBootTrackPage(HDDEditor root, Composite parent, IDEDosPartition partition, FileSelectDialog fsd) {
		super(root, parent, partition, fsd);
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
			Label lbl = label("XDPB info:", 4);
			FontData fontData = lbl.getFont().getFontData()[0];
			Font font = new Font(ParentComp.getDisplay(),
					new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
			lbl.setFont(font);

			try {
				byte data[] = fbc.GetDataInPartition(0, Disk.SectorSize);

				String ChecksumStatus = "Not bootable";
				int cs = (fbc.checksum + fbc.fiddleByte) & 0xff;
				if (cs == 3) {
					ChecksumStatus = "Bootable +3 disk";
				} else if (cs == 1) {
					ChecksumStatus = "Bootable PCW9512 disk";
				} else if (cs == 255) {
					ChecksumStatus = "Bootable PCW8256 disk";
				}

				label("Format: " + fbc.diskformat + " (" + fbc.disktype + ")", 1);
				label("Sectors: " + fbc.numsectors, 1);
				label("Sector size: " + fbc.sectorSize + " (" + fbc.sectorPow + ")", 1);
				label("Reserved Tracks: " + fbc.reservedTracks, 1);
				
				label("Block size: " + fbc.blockSize + " (" + fbc.blockPow + ")", 1);
				label("Directory blocks: " + fbc.dirBlocks, 1);
				label("R/W Gap: " + fbc.rwGapLength, 1);
				label("Format Gap: " + fbc.fmtGapLength, 1);
				
				label("Checksum+fb: " + cs, 1);
				label("Bootable?: " + ChecksumStatus, 1);
				label("Max CPM Blocks: " + fbc.maxblocks, 1);
				label("Reserved blocks: " + fbc.reservedblocks, 1);
				
				label("Max Dirents: " + fbc.maxDirEnts, 1);
				label("Bytes per block ID: " + fbc.BlockIDWidth, 1);
				label("Disk size: " + fbc.diskSize + "k", 1);
				label("Sector range: " + Disk.diskTracks[0].minsectorID + "-" + Disk.diskTracks[0].maxsectorID, 1);
				
				label("Identification type: "+fbc.Identifiedby,4);
				
				label("",4);
				
				lbl = label("Boot sector code:", 4);
				lbl.setFont(font);
				
				AddAsm(data);
				
				Button SaveBootCode = new Button(ParentComp,SWT.BORDER);
				SaveBootCode.setText("Save Asm");
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
				SaveRawBootCode.setText("Save Raw Bootsector");
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
				System.out.println("Error loading boot track");
			}
			ParentComp.pack();
		}
	}
	
	/**
	 * Handler for the "Save Raw Data" button
	 * @param data
	 */
	protected void DoSaveRawData(byte[] data) {
		File Selected = fsd.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES, "Save Raw Bootsector data as");
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
		File Selected = fsd.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES, "Save assembly file as");
		if (Selected != null) {
			FileOutputStream file;
			try {
				file = new FileOutputStream(Selected);
				file.write(("Boot sector of : " + partition.CurrentDisk.GetFilename() + System.lineSeparator()).getBytes());
				file.write(("Org: 0xfe00" + System.lineSeparator()).getBytes());
				file.write(("Length: 512 bytes" + System.lineSeparator() + System.lineSeparator()).getBytes());
				try {
					ASMLib asm = new ASMLib();
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
									decStr = CPM.datadesc[loadedaddress-0xfe00];
									decStr = String.format(decStr, asmData[0] & 0xff);
								} else if (loadedaddress < 0xfe0f) {
									decLen = 5;
									decStr = "DEFB ";
									String s = "";
									for(int i:asmData) {
										s = s + ", "+String.valueOf(i);
									}
									decStr = decStr +s.substring(2)+" ; Reserved";
								} else {
									decLen = 1;
									decStr = String.format("DEFB %d ; Checksum", asmData[0] & 0xff);
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
						System.out.println("Error at: " + realaddress + "(" + loadedaddress + ")");
						System.out.println(E.getMessage());
						E.printStackTrace();
					}
				} finally {
					file.close();
				}

			} catch (FileNotFoundException e) {
				ErrorBox("Error saving file - Directory not found!");
				e.printStackTrace();
			} catch (IOException e) {
				ErrorBox("Error saving file - I/O error!" + e.getMessage());
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
		tc1.setText("Address");
		tc1.setWidth(80);
		TableColumn tc2 = new TableColumn(HexTable, SWT.LEFT);
		tc2.setText("Hex");
		tc2.setWidth(160);
		TableColumn tc3 = new TableColumn(HexTable, SWT.LEFT);
		tc3.setText("Asm");
		tc3.setWidth(300);
		TableColumn tc4 = new TableColumn(HexTable, SWT.LEFT);
		tc4.setText("Chr");
		tc4.setWidth(40);
		HexTable.setHeaderVisible(true);

		ASMLib asm = new ASMLib();
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
						decStr = CPM.datadesc[loadedaddress-0xfe00];
						decStr = String.format(decStr, asmData[0] & 0xff);
					} else if (loadedaddress < 0xfe0f) {
						decLen = 5;
						decStr = "DEFB ";
						String s = "";
						for(int i:asmData) {
							s = s + ", "+String.valueOf(i);
						}
						decStr = decStr +s.substring(2)+" ; Reserved";
					} else {
						decLen = 1;
						decStr = String.format("DEFB %d ; Checksum", asmData[0] & 0xff);
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
			System.out.println("Error at: " + realaddress + "(" + loadedaddress + ")");
			System.out.println(E.getMessage());
			E.printStackTrace();
		}
	}
}

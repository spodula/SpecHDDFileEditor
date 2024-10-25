package hddEditor.ui.partitionPages;

/**
 * Basic raw floppy drive page.
 * 
 * Isnt terribly useful, but is (slightly) better than nothing.
 */

import java.io.File;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;

import hddEditor.libs.ASMLib;
import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.ASMLib.DecodedASM;
import hddEditor.libs.disks.FDD.FloppyDisk;
import hddEditor.libs.disks.FDD.Sector;
import hddEditor.libs.disks.FDD.TrackInfo;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.RawDiskData;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.BinaryRenderer;

public class RawFloppyPage extends GenericPage {
	RawDiskData rawdiskdata;
	Combo TrackCombo;
	Combo SideCombo;
	BinaryRenderer renderer;
	FloppyDisk fdd;
	Button ExportDiskBtn;

	public RawFloppyPage(HDDEditor root, Composite parent, IDEDosPartition partition, FileSelectDialog fsd) {
		super(root, parent, partition, fsd);
		rawdiskdata = (RawDiskData) partition;
		AddComponents();

	}

	/**
	 * 
	 */
	private void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			super.AddBasicDetails();

			label("Raw floppy disk:", 4);
			label("Tracks: " + rawdiskdata.GetStartCyl() + "-" + rawdiskdata.GetEndCyl(), 1);
			label("Sides: " + rawdiskdata.GetStartHead() + "-" + rawdiskdata.GetEndHead(), 1);
			label("", 1);
			ExportDiskBtn = button("Export disk");
			ExportDiskBtn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					DoExportDisk();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					DoExportDisk();
				}
			});

			String s[] = new String[rawdiskdata.GetEndCyl() - rawdiskdata.GetStartCyl() + 1];
			int ptr = 0;
			for (int i = rawdiskdata.GetStartCyl(); i < rawdiskdata.GetEndCyl() + 1; i++) {
				s[ptr++] = String.valueOf(i);
			}

			label("Track: ", 1);
			TrackCombo = combo(s, s[0]);
			TrackCombo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int trnum = Integer.valueOf(TrackCombo.getText());
					int sdnum = Integer.valueOf(SideCombo.getText());

					SetTrackData(trnum, sdnum);
					ParentComp.pack();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});

			s = new String[rawdiskdata.GetEndHead() - rawdiskdata.GetStartHead() + 1];
			ptr = 0;
			for (int i = rawdiskdata.GetStartHead(); i < rawdiskdata.GetEndHead() + 1; i++) {
				s[ptr++] = String.valueOf(i);
			}

			label("Side: ", 1);
			SideCombo = combo(s, s[0]);
			SideCombo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int trnum = Integer.valueOf(TrackCombo.getText());
					int sdnum = Integer.valueOf(SideCombo.getText());
					SetTrackData(trnum, sdnum);
					ParentComp.pack();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});

			fdd = (FloppyDisk) partition.CurrentDisk;
			renderer = new BinaryRenderer();

			SetTrackData(fdd.diskTracks[0].tracknum, fdd.diskTracks[0].side);
		}
		ParentComp.pack();
	}

	/**
	 * Set the track data.
	 * 
	 * @param track
	 * @param Side
	 */
	public void SetTrackData(int track, int Side) {
		// Find track
		TrackInfo CurrentTrack = null;
		for (TrackInfo tr : fdd.diskTracks) {
			if (tr.tracknum == track && tr.side == Side) {
				CurrentTrack = tr;
			}
		}

		byte data[] = new byte[0];

		if (CurrentTrack != null) {
			// Total up the track length.
			int total = 0;
			for (Sector sect : CurrentTrack.Sectors) {
				if (sect != null) {
					byte dat[] = sect.data;
					if (dat != null) {
						total = total + dat.length;
					}
				}
			}

			// create byte array.
			data = new byte[total];
			int dataptr = 0;
			for (Sector sect : CurrentTrack.Sectors) {
				if (sect != null) {
					if (sect.data != null) {
						System.arraycopy(sect.data, 0, data, dataptr, sect.data.length);
						dataptr = dataptr + sect.data.length;
					}
				}
			}
		}

		renderer.Render(ParentComp, data, 0, 400);
	}

	/**
	 * 
	 */
	public void DoExportDisk() {
		DirectoryDialog dialog = new DirectoryDialog(ParentComp.getShell());
		dialog.setText("Select folder for target");
		String result = dialog.open();
		if (result != null) {
			File rootfolder = new File(result);
			StringBuffer sb = new StringBuffer();
			sb.append("Original file name: " + fdd.file.getAbsolutePath() + System.lineSeparator());
			sb.append("Cylinders: " + fdd.NumCylinders + System.lineSeparator());
			sb.append("Heads: " + fdd.NumHeads + System.lineSeparator());
			sb.append("Sectors: " + fdd.NumSectors + System.lineSeparator());
			sb.append("Num Logical Sectors: " + fdd.NumLogicalSectors + System.lineSeparator());
			sb.append("Sector size: " + fdd.SectorSize + System.lineSeparator());
			sb.append("=======================================" + System.lineSeparator());
			for (TrackInfo t : fdd.diskTracks) {
				sb.append("Track: " + t.tracknum);
				sb.append(" Side: " + t.side);
				sb.append(" Sectors:" + t.minsectorID + " - " + t.maxsectorID + " (" + t.Sectors.length + " sectors.)");
				sb.append(" Sect size: " + t.sectorsz);
				sb.append(System.lineSeparator());
			}

			File filename = new File(rootfolder, "disk.info");
			GeneralUtils.WriteBlockToDisk(sb.toString().getBytes(), filename);

			for (TrackInfo t : fdd.diskTracks) {
				int dlen = 0;
				sb = new StringBuffer();
				sb.append("Track: " + t.tracknum + " Side:" + t.side + System.lineSeparator());
				sb.append("=======================================" + System.lineSeparator());
				sb.append("Data rate: " + t.GetDataRate() + " (" + t.datarate + ")" + System.lineSeparator());
				sb.append("Recording mode: " + t.GetRecordingMode() + " (" + t.recordingmode + ")"
						+ System.lineSeparator());
				sb.append("Filler byte: " + String.format("%d (%02X)", t.fillerByte, t.fillerByte)
						+ System.lineSeparator());
				sb.append("Gap3Len: " + t.gap3len + System.lineSeparator());
				sb.append("Sector range:" + t.minsectorID + " - " + t.maxsectorID + System.lineSeparator());
				sb.append("Num Sectors: " + t.numsectors + System.lineSeparator());
				sb.append("=======================================" + System.lineSeparator() + System.lineSeparator());
				for (Sector s : t.Sectors) {
					sb.append("Sector " + s.sectorID + System.lineSeparator());
					sb.append("Actual size: " + s.ActualSize + System.lineSeparator());
					sb.append("Status reg 1: " + s.FDCsr1 + System.lineSeparator());
					sb.append("Status reg 2: " + s.FDCsr2 + System.lineSeparator());
					sb.append("Sector size marker: " + s.Sectorsz + System.lineSeparator());
					sb.append("Side: " + t.side + System.lineSeparator());
					sb.append("Track: " + t.tracknum + System.lineSeparator());
					sb.append("Data:");
					int xptr = 0;
					String xbytes = "";
					if (s.data != null) {
						for (byte b : s.data) {
							if (xptr % 32 == 0) {
								if (xptr == 0) {
									sb.append(System.lineSeparator() + String.format("%04X", xptr));
								} else {
									sb.append(" " + xbytes + System.lineSeparator() + String.format("%04X", xptr));
								}
								xbytes = "";
							}
							sb.append(String.format(" %02X", (b & 0xff)));
							xptr++;

							if (Character.isAlphabetic(b)) {
								xbytes = xbytes + String.valueOf((char) b);
							} else {
								xbytes = xbytes + " ";
							}
						}
						dlen = dlen + s.data.length;
					} else {
						sb.append(System.lineSeparator() + "No data");
					}
					sb.append(System.lineSeparator());

					sb.append(System.lineSeparator());
					sb.append("Assembly: " + System.lineSeparator());

					ASMLib asm = new ASMLib();
					int loadedaddress = 0x0000;
					int realaddress = 0x0080;
					try {
						int asmData[] = new int[5];
						while (realaddress < s.data.length) {
							String chrdata = "";
							for (int i = 0; i < 5; i++) {
								int d = 0;
								if (realaddress + i < s.data.length) {
									d = (int) s.data[realaddress + i] & 0xff;
								}
								asmData[i] = d;

								if ((d > 0x1F) && (d < 0x7f)) {
									chrdata = chrdata + (char) d;
								} else {
									chrdata = chrdata + "?";
								}
							}
							int decLen = 0;
							String decStr = "";
							// decode instruction
							DecodedASM Instruction = asm.decode(asmData, loadedaddress);
							decLen = Instruction.length;
							decStr = Instruction.instruction;

							// output it. - First, assemble a list of hex bytes, but pad out to 12 chars
							// (4x3)
							String hex = "";
							for (int j = 0; j < decLen; j++) {
								hex = hex + String.format("%02X", asmData[j]) + " ";
							}

							sb.append(String.format("%04X", loadedaddress));
							sb.append(
									("\t" + hex + "\t\t" + decStr + "\t\t" + chrdata.substring(0, decLen)));
							sb.append(System.lineSeparator());

							realaddress = realaddress + decLen;
							loadedaddress = loadedaddress + decLen;

						} // while
					} catch (Exception E) {
						System.out.println("Error at: " + realaddress + "(" + loadedaddress + ")");
						System.out.println(E.getMessage());
						E.printStackTrace();
					}
				}
				sb.append(System.lineSeparator());

				filename = new File(rootfolder, "T" + t.tracknum + "S" + t.side + ".details");
				GeneralUtils.WriteBlockToDisk(sb.toString().getBytes(), filename);

				byte fulldata[] = new byte[dlen];
				int xptr = 0;
				for (Sector s : t.Sectors) {
					System.arraycopy(s.data, 0, fulldata, xptr, s.data.length);
					xptr = xptr + s.data.length;
				}
				filename = new File(rootfolder, "T" + t.tracknum + "S" + t.side + ".raw");
				GeneralUtils.WriteBlockToDisk(fulldata, filename);
			}
		}
	}

}

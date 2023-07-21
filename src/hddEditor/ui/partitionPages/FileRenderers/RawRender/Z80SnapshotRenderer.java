package hddEditor.ui.partitionPages.FileRenderers.RawRender;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import hddEditor.libs.Speccy;

public class Z80SnapshotRenderer extends RamDump {
	private ArrayList<Label> labels = null;
	private ArrayList<Renderer> Renderers = null;

	/**
	 * Array object for each individual page.
	 */
	private class z80Page {
		//Length while compressed
		public int CompressedLength;
		//Is block compressed
		public boolean IsCompressed;
		//z80 page number. (Note, this differs from the 128k page number).
		public int Pagenum;
		//Raw (compressed) data
		public byte Rawdata[];
		//uncompressed data
		public byte Data[];

		/**
		 * return details as a string.
		 */
		@Override
		public String toString() {
			return ("Page: " + Pagenum + "  compressed?:" + IsCompressed + "  Compressed length: " + CompressedLength);
		}
		
		public int get128Page() {
			int result = Math.max(-1,Pagenum-3);
			if (result > 7) 
				result = -1;
			return(result);
		}
	}

	/**
	 * Remove all the components created by this object
	 */
	@Override
	public void DisposeRenderer() {
		super.DisposeRenderer();
		if (labels != null) {
			for (Label l : labels) {
				l.dispose();
			}
			labels.clear();
			labels = null;
		}
		if (Renderers != null) {
			for (Renderer r : Renderers) {
				r.DisposeRenderer();
			}
			Renderers.clear();
			Renderers = null;
		}
	}

	/**
	 * Treat the file as a z80 file.
	 * 
	 * @param TargetPage - Page to render to.
	 * @param data - Data to render
	 * @param loadAddr - Load address (unused)
	 * @param filename - Filename
	 */
	private String[] snaVars = { "A", "F", "BC", "HL", "PC", "SP", "I", "R", "FLAGS", "DE", "BC'", "DE'", "HL'", "A'",
			"F'", "IY", "IX", "Int Status", "IFF2", "FLAGS2" };
	private int[] snaLen = { 1, 1, 2, 2, 2, 2, 1, 1, 1, 2, 2, 2, 2, 1, 1, 2, 2, 1, 1, 1 };
	
	public void Render(Composite TargetPage, byte[] data, int loadAddr, String filename) {
		labels = new ArrayList<Label>();
		Renderers = new ArrayList<Renderer>();

		boolean is128K = false;
		Label lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(lbl.getShell().getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));

		int version = 1;
		if ((data[6] + data[7]) == 0) {
			version = 3;
			if (data[30] == 23)
				version = 2;

			// 128k snapshots are supported in V2 and V3 snapshots, so figure out which it
			// is...
			if (version == 2) {
				is128K = (data[34] > 2);
			} else {
				is128K = (data[34] > 3);
			}
		}

		if (!is128K)
			lbl.setText("48K z80 snapshot file V" + version + ": ");
		else
			lbl.setText("128K z80 snapshot file V" + version + ": ");

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setFont(boldFont);
		lbl.setLayoutData(gd);
		int fptr = 0;
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;
		int IY = 0;
		boolean compressed = true; // default for V2 and V3, for V1, controlled by flags
		for (int i = 0; i < snaVars.length; i++) {
			boolean skip = false;
			String varName = snaVars[i];
			int varLength = snaLen[i];
			String varval = String.format("%02X", data[fptr++] & 0xff);
			if (varLength == 2) {
				varval = String.format("%02X", data[fptr++] & 0xff) + varval;
			}
			// PC invalid for all except v1 files.
			if (varName.equals("PC") && (version != 1))
				skip = true;
			if (varName.equals("FLAGS")) {
				int xData = data[fptr - 1];
				int rMSB = xData & 0x01;
				int BorderClr = (xData & 0x0E) / 2;
				boolean SamRom = ((xData & 0x10) != 0);
				compressed = ((xData & 0x20) != 0);

				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText("R MSB: " + rMSB);
				lbl.setLayoutData(gd);

				if (version == 1) {
					lbl = new Label(TargetPage, SWT.NONE);
					labels.add(lbl);
					lbl.setText("Samrom Paged: " + SamRom);
					lbl.setLayoutData(gd);

					lbl = new Label(TargetPage, SWT.NONE);
					labels.add(lbl);
					lbl.setText("Compressed: " + compressed);
					lbl.setLayoutData(gd);
				}

				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText("Border: " + BorderClr + " (" + Speccy.SPECTRUM_COLOURS[BorderClr] + ")");
				lbl.setLayoutData(gd);
				skip = true;
			}
			if (varName.equals("IY")) {
				IY = Integer.parseInt(varval, 16);
			}
			if (varName.equals("Int Status")) {
				if (varval.equals("00"))
					varval = "Disabled";
				else
					varval = "Enabled";
			}

			if (varName.equals("FLAGS2")) {
				int xData = data[fptr - 1];
				int im = (xData & 0x03);
				boolean Issue2 = (xData & 0x04) != 0;
				boolean DoubleInt = (xData & 0x08) != 0;
				int VideoSync = (xData & 0x30) / 0x10;
				int Joystick = (xData & 0xC0) / 0x40;

				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText("IM: " + im);
				lbl.setLayoutData(gd);

				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText("Iss 2 emulation?: " + Issue2);
				lbl.setLayoutData(gd);

				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText("Double Interrupt freq?: " + DoubleInt);
				lbl.setLayoutData(gd);

				lbl = new Label(TargetPage, SWT.NONE);
				String vSyncVals[] = { "Normal", "High", "Normal", "Low" };
				labels.add(lbl);
				lbl.setText("Video Sync: " + vSyncVals[VideoSync]);
				lbl.setLayoutData(gd);

				lbl = new Label(TargetPage, SWT.NONE);
				String jStickVals[] = { "Cursor/Protek/AGF", "Kempston", "Sinclair 2 left", "Sinclair 2 Right" };
				labels.add(lbl);
				lbl.setText("Joystick: " + jStickVals[Joystick]);
				lbl.setLayoutData(gd);
				skip = true;
			}

			if (!skip) {
				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText(varName + ": " + varval);
				lbl.setLayoutData(gd);
			}
		}
		int pagedRam = 0;
		// V2 and 3 have extra header data.
		if (version != 1) {
			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText("V2 Extended data:");
			GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd1.horizontalSpan = 4;
			lbl.setLayoutData(gd1);
			lbl.setFont(boldFont);

			String PC = String.format("%02X", data[32] & 0xff) + String.format("%02X", data[33] & 0xff);
			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText("PC: " + PC);
			lbl.setLayoutData(gd);

			boolean IsModified = ((data[37] & 0xff) & 0x80) != 0;

			String HWv2[] = { "48K", "48K + IF1", "SAMRAM", "128K", "128K + IF1", "-", "-", "-", "-", "-", "-", "-",
					"-", "-", "-" };
			String HWv3[] = { "48K", "48K + IF1", "SAMRAM", "48K + MGT", "128K", "128K + IF1", "128K + MGT", "+3", "+3",
					"Pentagon 128", "Scorpion 256", "Didaktik-Kompakt", "+2", "+2A", "TC2048", "TC2068" };
			int hwMode = data[34] & 0x0f;
			String shwMode = HWv3[hwMode];
			if (version == 2) {
				shwMode = HWv2[hwMode];
			}

			if (IsModified) {
				shwMode = shwMode.replace("48K", "16K");
				shwMode = shwMode.replace("128K", "+2");
				shwMode = shwMode.replace("+3", "+2A");
			}

			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText("Hardware mode: " + shwMode);
			lbl.setLayoutData(gd);

			String lblname = "$7FFD port: ";
			pagedRam = data[45] & 0x07;
			if (shwMode.equals("SAMRAM")) {
				lblname = "state of 74ls259: ";
			}

			if (shwMode.startsWith("TC")) {
				lblname = "Timex $f4 port: ";
			}
			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText(lblname + (data[35] & 0xff));
			lbl.setLayoutData(gd);

			boolean IsPaged = (data[36] & 0xff) == 0xff;

			String txt = "Interface 1 Paged: " + IsPaged;
			if (shwMode.startsWith("TC")) {
				txt = "Last OUT to 0xFF: " + (data[36] & 0xff);
			}
			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText(txt);
			lbl.setLayoutData(gd);

			int moreflags = data[37] & 0xff;

			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText("R emulation: " + OnOff((moreflags & 0x01) != 0));
			lbl.setLayoutData(gd);

			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText("LDIR emulation: " + OnOff((moreflags & 0x02) != 0));
			lbl.setLayoutData(gd);

			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText("AY sound: " + OnOff((moreflags & 0x04) != 0));
			lbl.setLayoutData(gd);

			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText("Fuller Audio box: " + OnOff((moreflags & 0x40) != 0));
			lbl.setLayoutData(gd);

			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText("Modify HW: " + OnOff((moreflags & 0x80) != 0));
			lbl.setLayoutData(gd);

			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText("Last OUT to 0xFFFD (Soundchip): " + (data[38] & 0xff));
			lbl.setLayoutData(gd);

			String sndRegs = "";
			for (int i = 39; i < 55; i++) {
				sndRegs = sndRegs + "," + String.format("%2X", (data[i] & 0xff));
			}

			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd2.horizontalSpan = 2;
			lbl.setLayoutData(gd2);
			lbl.setText("Soundchip registers: " + sndRegs.substring(1));

			if (version == 3) {

				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText("MGT Rom Paged: " + ((data[59]) == 0xff));
				lbl.setLayoutData(gd);

				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText("Multiface Rom paged: " + ((data[60]) == 0xff));
				lbl.setLayoutData(gd);

				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText("Rom at 0-8191?: " + ((data[61]) == 0xff));
				lbl.setLayoutData(gd);

				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText("Rom at 8192-16383?: " + ((data[62]) == 0xff));
				lbl.setLayoutData(gd);

				if (data[30] == 55) {
					lbl = new Label(TargetPage, SWT.NONE);
					labels.add(lbl);
					lbl.setText("$1FFD port: " + (data[68] & 0xff));
					lbl.setLayoutData(gd);
				}
			}
		}
		if (version == 1) {
			byte b48kRam[] = new byte[0xc000];
			if (!compressed) {
				System.arraycopy(data, 30, b48kRam, 0, Math.min(b48kRam.length, data.length - 30));
			} else {
				System.arraycopy(data, 30, b48kRam, 0, Math.min(b48kRam.length, data.length - 30));
				byte block[] = ExtractCompressedBlock(b48kRam, 49152);
				System.arraycopy(block, 0, b48kRam, 0, Math.min(b48kRam.length, block.length));
			}
			super.Render(TargetPage, b48kRam, 0, false, IY, new int[0], filename);
		} else {
			int blockstart = (data[30] & 0xff) + 29;
			z80Page pages[] = ExtractPages(data, blockstart);
			if (!is128K) {
				//assemble a 48k block
				byte ram[] = new byte[0xc000];
				for(z80Page page:pages) {
					if (page.Pagenum==4)
						System.arraycopy(page.Data, 0, ram, 0x4000 , 0x4000);
					if (page.Pagenum==5)
						System.arraycopy(page.Data, 0, ram, 0x8000 , 0x4000);
					if (page.Pagenum==8)
						System.arraycopy(page.Data, 0, ram, 0x0000 , 0x4000);
				}
				super.Render(TargetPage, ram, 0, false, IY, new int[0], filename);
			} else {
				/*
				 * Assembling pages in the data block as:
				 *  0000 -  3fff  page 8 (128k Page 5)
				 *  4000 -  7fff  page 4 (128k page 1)
				 *  8000 -  bfff  Paged in page
				 * 
				 * 3  $0C000  (128k Page 0)
				 * 4  $10000  (128k Page 1) 
				 * 5  $14000  (128k Page 2)
				 * 6  $18000  (128k Page 3) 
				 * 7  $1C000  (128k Page 4) 
				 * 8  $20000  (128k Page 5) 
				 * 9  $24000  (128k Page 6)
				 * 10 $28000  (128k Page 7) 
				 */
				byte ram[] = new byte[11*0x4000];
				for(z80Page page:pages) {
					if (page.get128Page()==pagedRam) { 
						System.arraycopy(page.Data, 0, ram, 0x8000, 0x4000);
					}
					if (page.get128Page()==0) 
						System.arraycopy(page.Data, 0, ram, 0xC000, 0x4000);
					if (page.get128Page()==1) 
						System.arraycopy(page.Data, 0, ram, 0x10000, 0x4000);
					if (page.get128Page()==2) {
						System.arraycopy(page.Data, 0, ram, 0x14000, 0x4000);
						System.arraycopy(page.Data, 0, ram, 0x4000, 0x4000);						
					}
					if (page.get128Page()==3) 
						System.arraycopy(page.Data, 0, ram, 0x18000, 0x4000);
					if (page.get128Page()==4) 
						System.arraycopy(page.Data, 0, ram, 0x1C000, 0x4000);
					if (page.get128Page()==5) {
						System.arraycopy(page.Data, 0, ram, 0x20000, 0x4000);
						System.arraycopy(page.Data, 0, ram, 0x0000, 0x4000);
					}
					if (page.get128Page()==6) 
						System.arraycopy(page.Data, 0, ram, 0x24000, 0x4000);
					if (page.get128Page()==7) 
						System.arraycopy(page.Data, 0, ram, 0x28000, 0x4000);
				}			
				int pagelist[] = {5,2,pagedRam,0,1,2,3,4,5,6,7};
				
				super.Render(TargetPage, ram, 0, true, IY, pagelist, filename);
			}
		}
	}

	/**
	 * Extract the pages from the z80 files into an array of z80Page objects.
	 * 
	 * @param data
	 * @param blockstart
	 * @return
	 */
	private z80Page[] ExtractPages(byte[] data, int blockstart) {
		ArrayList<z80Page> pages = new ArrayList<z80Page>();
		while (blockstart < data.length) {
			z80Page page = new z80Page();
			page.CompressedLength = (data[blockstart] & 0xff) + ((data[blockstart + 1] & 0xff) * 0x100);
			page.IsCompressed = true;
			if (page.CompressedLength == 0xffff) {
				page.CompressedLength = 0x4000;
				page.IsCompressed = false;
			}
			page.Pagenum = data[blockstart + 2] & 0xff;
			page.Rawdata = new byte[page.CompressedLength];
			blockstart = blockstart + 3;
			if (page.Pagenum > 18)
				break;
			if ((page.Pagenum > 2) && (page.Pagenum != 11)) {
				System.arraycopy(data, blockstart, page.Rawdata, 0, Math.min(data.length - blockstart, page.Rawdata.length));
				blockstart = blockstart + page.CompressedLength;

				if (!page.IsCompressed) {
					page.Data = new byte[0x4000];
					System.arraycopy(page.Rawdata, 0, page.Data, 0, page.Rawdata.length);
				} else {
					page.Data = ExtractCompressedBlock(page.Rawdata, 0x4000);
				}
			}

			System.out.println(page);
			pages.add(page);

		}
		return pages.toArray(new z80Page[pages.size()]);
	}

	/**
	 * Extract the block and decompress it. 
	 * Z80 files are compressed using a simple RLE scheme.
	 * 
	 * @param block - Block to decompress
	 * @param ExpectedLength - Expected length of the block. Usually 16384.
	 * @return
	 */
	private byte[] ExtractCompressedBlock(byte[] block, int ExpectedLength) {
		byte result[] = new byte[ExpectedLength];
		int destPtr = 0;
		boolean InED = false;
		int EDstate = 0;
		int edRUN = 0;
		int byteptr = 0;
		for (;byteptr < block.length;byteptr++) {
			byte b = block[byteptr];
			int x = (b & 0xff);
			if (!InED) {
				if (x == 0xED) {
					InED = true;
					EDstate = 0;
				} else {
					result[destPtr++] = b;
				}
			} else {
				if (EDstate == 0) {
					if (x != 0xED) {
						result[destPtr++] = (byte) 0xED;
						result[destPtr++] = b;
						InED = false;
						EDstate = 0;
					} else {
						EDstate++;
					}
				} else if (EDstate == 1) {
					edRUN = x;
					EDstate++;
				} else if (EDstate == 2) {
					for (int i = 0; i < edRUN; i++) {
						if (destPtr >= ExpectedLength)
							break;
						result[destPtr++] = b;
					}
					EDstate = 0;
					InED = false;
				}
			}
			if (destPtr >= ExpectedLength) {
				if (block.length == (byteptr-1))
					System.out.println("Stopping at " + destPtr+" ptr="+byteptr+"/"+block.length);
				break;
			}
		}
		return result;
	}

	/**
	 * convert a boolean value to "on" or "off"
	 * 
	 * @param value
	 * @return
	 */
	private String OnOff(boolean value) {
		if (value)
			return ("on");
		return ("off");
	}
}

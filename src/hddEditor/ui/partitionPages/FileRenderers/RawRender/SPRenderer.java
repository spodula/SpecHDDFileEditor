package hddEditor.ui.partitionPages.FileRenderers.RawRender;

/**
 * Render a .SP file as generated by the SPECTRUM program
 * 
 * This is an old and obsolete file format which only supports 16k and 48k files, 
 * but found a few i would like  to examine, and its mostly a simple copy of SNA with only a few changes.
 * 
 * https://rk.nvg.ntnu.no/sinclair/faq/fileform.html#SP
 * 
 *   Offset   Size   Description
   ------------------------------------------------------------------------
   0        2      byte   "SP" (signature)
   2        2      word   Program length in bytes (normally 49152 for 48K
                          snaps, or 16384 for 16K snaps)
   4        2      word   Program location (normally 16384)
   6        8      word   BC,DE,HL,AF
   14       4      word   IX,IY
   18       8      word   BC',DE',HL',AF'
   26       2      byte   R,I
   28       4      word   SP,PC
   32       2      word   0 (reserved for future use)
   34       1      byte   Border color
   35       1      byte   0 (reserved for future use)
   36       2      word   Status word
   ------------------------------------------------------------------------

   Status word:
   Bit     Description
   ------------------------------------------------------------------------
   15-8    Reserved for future use
    7-6    Reserved for internal use (0)
      5    Flash: 0=INK/1=PAPER
      4    Interrupt pending for execution
      3    If 1, IM 0; if 0, bit 1 determines interrupt mode
           (Spectrum v 0.99e had this behaviour reversed, and this
           bit was not used in versions previous to v 0.99e)
      2    IFF2 (internal use)
      1    Interrupt Mode (if bit 3 reset): 0=>IM1, 1=>IM2
      0    IFF1: 0=DI/1=EI
 * 
 */

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import hddEditor.libs.ASMLib;
import hddEditor.libs.Speccy;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.snapshots.MachineState;

public class SPRenderer extends RamDump {
	private ArrayList<Label> labels = null;
	private ArrayList<Renderer> Renderers = null;

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

	private String[] snaVars = { "Length", "BC", "DE", "HL", "AF", "IX", "IY", "BC'", "DE'", "HL'", "AF'", "R", "I",
			"SP", "PC", "reserved", "Border", "Reserved", "Status" };
	private int[] snaLen = { 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 2, 2, 1, 1, 1, 2 };

	/**
	 * Treat the file as a SNA style file.
	 * 
	 * @param TargetPage - page to render to.
	 * @param data       - data to render
	 * @param loadAddr   - Load address - Unused for the SNA renderer.
	 * @param filename   - Filename
	 */
	@Override
	public void Render(Composite TargetPage, byte[] data, int loadAddr, boolean is128K, int xx, int i128BankOrder[],
			String filename, MachineState cpustate, IDEDosPartition targetpartition) {
		labels = new ArrayList<Label>();
		Renderers = new ArrayList<Renderer>();

		int proglen = ((data[3] & 0xff) * 256) + (data[2] & 0xff);
		System.out.println(proglen);

		Label lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(lbl.getShell().getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));

		String s = "16K";
		if (proglen == 49152)
			s = "48K";
		s = s + " .SP (SPECTRUM) snapshot file: ";
		if (data[0] != 'S' || data[1] != 'P') {
			s = s + "(Bad header)";
		}

		lbl.setText(s);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setFont(boldFont);
		lbl.setLayoutData(gd);

		int fptr = 2;
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;
		for (int i = 0; i < snaVars.length; i++) {
			String varName = snaVars[i];
			int varLength = snaLen[i];
			String varval = String.format("%02X", data[fptr++] & 0xff);
			if (varLength == 2) {
				varval = String.format("%02X", data[fptr++] & 0xff) + varval;
			}
			if (varName.equals("Border"))
				varval = varval + " (" + Speccy.SPECTRUM_COLOURS[(data[fptr - 1] & 0x07)] + ")";
			if (varName.equals("Status")) {
				varval = varval+" (";
				int value = (data[fptr-2] & 0xff);
				if ((value & 0x10) != 0) {
					varval = varval+ "Int Pending,";
				}
				if ((value & 0x08) != 0) {
					varval = varval+ "IM0,";
				} else {
					if ((value & 0x02) != 0) {
						varval = varval+ "IM2,";
					} else {
						varval = varval+ "IM0,";
					}
				}
				if ((value & 0x01) != 0)
					varval = varval+ "DI";
				else 
					varval = varval+ "EI";
				varval = varval+")";
			}
			
			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText(varName + ": " + varval);
			lbl.setLayoutData(gd);
		}

		String flags = ASMLib.GetFlagsAsString(data[12]);

		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText("Flags: " + flags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		flags = ASMLib.GetFlagsAsString(data[24]);

		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText("Alt Flags: " + flags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		int IY = (int) ((data[0x11] & 0xff) * 0x100) + (data[0x10] & 0xff);

		byte rawdata[] = new byte[49152];
		System.arraycopy(data, 38, rawdata, 0, Math.min(data.length - 38, 49152));

		super.Render(TargetPage, rawdata, loadAddr, false, IY, new int[8], filename, null,null);
	}

}

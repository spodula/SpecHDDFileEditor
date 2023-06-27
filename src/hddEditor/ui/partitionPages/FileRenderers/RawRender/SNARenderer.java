package hddEditor.ui.partitionPages.FileRenderers.RawRender;
/**
 * Render a SNA file, including trying to decode BASIC programs.
 * https://sinclair.wiki.zxnet.co.uk/wiki/SNA_format
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

public class SNARenderer extends RamDump {
	private ArrayList<Label> labels = null;
	private ArrayList<Renderer> Renderers = null;
	
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
		if (Renderers!=null) {
			for(Renderer r:Renderers) {
				r.DisposeRenderer();
			}
			Renderers.clear();
			Renderers = null;
		}
	}
	
	
	/**
	 * Render the code as SNA
	 * 
	 * @param data
	 * @param loadAddr
	 */
	private String[] snaVars = { "I", "HL'", "DE'", "BC'", "AF'", "HL", "DE", "BC", "IY", "IX", "IFF2", "R", "AF", "SP",
			"IM", "Border" };
	private int[] snaLen = { 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 2, 2, 1, 1 };

	
	public void Render(Composite TargetPage, byte[] data, int loadAddr, String filename) {
		labels = new ArrayList<Label>();
		Renderers = new ArrayList<Renderer>();
		boolean is128K = (data.length > 50000);
		Label lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(lbl.getShell().getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));

		if (!is128K)
			lbl.setText("48K SNA snapshot file: ");
		else
			lbl.setText("128K SNA snapshot file: ");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setFont(boldFont);
		lbl.setLayoutData(gd);
		int fptr = 0;
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
			if (varName.equals("IFF2")) {
				int iff2 = (int) data[fptr - 1] & 0xff;
				if ((iff2 & 0x04) == 0)
					varval = varval + " (DI)";
				else
					varval = varval + " (EI)";
			}
			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText(varName + ": " + varval);
			lbl.setLayoutData(gd);
		}

		String flags = ASMLib.GetFlagsAsString(data[0x15]);

		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText("Flags: " + flags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		flags = ASMLib.GetFlagsAsString(data[0x07]);

		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText("Alt Flags: " + flags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		int IY = (int) ((data[0x10] & 0xff) * 0x100) + (data[0x0f] & 0xff);

		int RamBankOrder[] = new int[8];
		
		byte rawdata[] = null;
		if (is128K) {
			int pagedram = (data[49181] & 0x07);
			if ((pagedram==5) || (pagedram==2)) {
				RamBankOrder= new int[9];
			}
			RamBankOrder[0] = 5;
			RamBankOrder[1] = 2;
			RamBankOrder[2] = pagedram;
			int ptr=3;
			for (int i=0;i<8;i++) {
				if ((i!=2) && (i!=5) && (i!=pagedram)) {
					RamBankOrder[ptr++] = i;
				}
			}

			rawdata = new byte[8 * 0x4000];
			//page 5 2 and paged...
			System.arraycopy(data, 0x1b, rawdata, 0, 49152);
			int restbase = 0xc01f;
			int targBase = 0xc000;
			for(int i=0;i<5;i++) {	
				if (data.length > restbase) {
					System.arraycopy(data, restbase, rawdata, targBase , Math.min(data.length- restbase,16384));
				}
				restbase = restbase + 0x4000;
				targBase = targBase + 0x4000;
			}
		} else {
			rawdata = new byte[49152];
			System.arraycopy(data, 0x1b, rawdata, 0, Math.min(data.length-0x1b,49152));
		}
		
		
		super.Render(TargetPage, rawdata, loadAddr, is128K, IY, RamBankOrder, filename);
	}
}

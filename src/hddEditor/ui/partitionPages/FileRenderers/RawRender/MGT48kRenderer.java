package hddEditor.ui.partitionPages.FileRenderers.RawRender;
/**
 * Render an MGT 48K snapshot. This is incomplete at the moment.
 */

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import hddEditor.libs.ASMLib;
import hddEditor.libs.partitions.mgt.MGTDirectoryEntry;

public class MGT48kRenderer extends RamDump {
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
	
	private String[] snaVars = { "IY", "IX", "DE'", "BC'", "HL'", "AF'", "DE", "BC", "HL", "junk", "I", "SP"};
	private int[] snaLen = { 2,2,2,2,2,2,2,2,2,1,1,2 };
	
	public void Render(Composite TargetPage, byte[] data, MGTDirectoryEntry entry ) {
		labels = new ArrayList<Label>();
		Renderers = new ArrayList<Renderer>();

		Label lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(lbl.getShell().getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setFont(boldFont);
		lbl.setLayoutData(gd);
		int fptr = 0xdc;
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;
		int sp=0;
		for (int i = 0; i < snaVars.length; i++) {
			String varName = snaVars[i];
			int varLength = snaLen[i];
			String varval = String.format("%02X", entry.RawDirectoryEntry[fptr++] & 0xff);
			if (varLength == 2) {
				varval = String.format("%02X", entry.RawDirectoryEntry[fptr++] & 0xff) + varval;
			}
			if (varName.equals("SP")) {
				sp = Integer.decode("0x"+varval);
			}
			if (varName.equals("I")) {
				if (varval.equals("00")||varval.equals("3F"))
					varval = varval+" (IM 1)";
				else 
					varval = varval+" (IM 2)";
			}
			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText(varName + ": " + varval);
			lbl.setLayoutData(gd);
		}
		
		/**
		 * PUSH PC
		 * Push AF
		 * Push RF
		 */
		int baseloc = sp - 0x4000;
		String stackVars[] = { "Iff2","R","AF","PC" };
		int    stklen[]    = { 1,1,2,2 };
		byte flagReg=0;
		
		
		for (int i = 0; i < stackVars.length; i++) {
			String varName = stackVars[i];
			if (varName.equals("AF")) {
				flagReg = data[baseloc];				
			}
			int varLength = stklen[i];
			String varval = String.format("%02X", data[baseloc++] & 0xff);
			if (varLength == 2) {
				varval = String.format("%02X", data[baseloc++] & 0xff) + varval;
			}
			if (varName.equals("Iff2")) {
				int iff = data[baseloc-1] & 0xff;
				if (iff!=0) 
					varval = "EI";
				else
					varval = "DI";
			}
			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText(varName + ": " + varval);
			lbl.setLayoutData(gd);
		}
		
		
		String Flags = ASMLib.GetFlagsAsString(flagReg);
		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText("Flags: " + Flags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		String altFlags = ASMLib.GetFlagsAsString(data[0x09]);
		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText("Alt Flags: " + altFlags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);
		
		TargetPage.pack();
		
		int IY = (int) ((entry.RawDirectoryEntry[0xdd] & 0xff) * 0x100) + (entry.RawDirectoryEntry[0xdc] & 0xff);
		
		super.Render(TargetPage,data, 0x4000, false, IY,new int[1], entry.GetFilename());
	}
}

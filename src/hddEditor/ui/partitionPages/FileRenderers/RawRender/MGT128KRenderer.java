package hddEditor.ui.partitionPages.FileRenderers.RawRender;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import hddEditor.libs.ASMLib;
import hddEditor.libs.Languages;
import hddEditor.libs.partitions.mgt.MGTDirectoryEntry;

public class MGT128KRenderer extends RamDump {
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
	
	public void Render(Composite TargetPage, byte[] data, MGTDirectoryEntry entry , Languages lang) {
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
		lbl.setText(lang.Msg(Languages.MSG_FLAGS) + ": " + Flags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		String altFlags = ASMLib.GetFlagsAsString(data[0x09]);
		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText(lang.Msg(Languages.MSG_ALTFLAGS) + ": " + altFlags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);
		
		int x7ffd = data[0x00] & 0xff;
		int pagenum = x7ffd & 0x07;
		int screen = ((x7ffd & 0x08) / 0x04) + 5;
		int rom    = ((x7ffd & 0x10) / 0x10);
		int pagelock = ((x7ffd & 0x20) / 0x20);
		
		String s = String.format(lang.Msg(Languages.MSG_7ffdLine), x7ffd, pagenum, screen, rom, pagelock);
		
		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText(s);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 3;
		lbl.setLayoutData(gd);
		
		TargetPage.pack();
		
		int IY = (int) ((entry.RawDirectoryEntry[0xdd] & 0xff) * 0x100) + (entry.RawDirectoryEntry[0xdc] & 0xff);
		
		byte newdata[] = new byte[data.length-1];
		System.arraycopy(data, 1, newdata, 0, newdata.length-1);
		
		super.Render(TargetPage,data, 0x4000, true, IY,new int[] {0,1,2,3,4,5,6,7}, entry.GetFilename(),null,null, lang);
	}

}

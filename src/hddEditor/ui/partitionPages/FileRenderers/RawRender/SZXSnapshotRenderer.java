package hddEditor.ui.partitionPages.FileRenderers.RawRender;
/**
 * This implements support for the ZX-State file format as used by some modern emulators.
 * The format provides a lot more comprehensive support for all aspects of emulation than .Z80 and .SNA
 * 
 * It is of course, massively overkill for what we are doing here. 
 * 
 * https://www.spectaculator.com/docs/zx-state/intro.shtml
 */

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import hddEditor.libs.ASMLib;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.snapshots.MachineState;
import hddEditor.libs.snapshots.readers.SZXFile;

public class SZXSnapshotRenderer extends RamDump {
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

	/**
	 * Treat the file as a z80 file.
	 * 
	 * @param TargetPage - Page to render to.
	 * @param data       - Data to render
	 * @param loadAddr   - Load address (unused)
	 * @param filename   - Filename
	 */
	public void Render(Composite TargetPage, byte[] data, int loadAddr, String filename, IDEDosPartition targetpart) {
		SZXFile szxfile;
		try {
			szxfile = new SZXFile(data);

			labels = new ArrayList<Label>();
			Renderers = new ArrayList<Renderer>();
			Label lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			FontData fontData = lbl.getFont().getFontData()[0];
			Font boldFont = new Font(lbl.getShell().getDisplay(),
					new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));

			lbl.setText(szxfile.GetMachineFullName());

			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.horizontalSpan = 4;
			lbl.setFont(boldFont);
			lbl.setLayoutData(gd);

			Hashtable<String, String> SnapshotRegisters = szxfile.DetailsAsArray();

			gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.horizontalSpan = 1;

			Iterator<String> iterator = SnapshotRegisters.keySet().iterator();

			while (iterator.hasNext()) {
				String key = iterator.next();
				String value = SnapshotRegisters.get(key);
				// Process key-value pair
				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText(key + ": " + value);
				lbl.setLayoutData(gd);
			}

			String flags = ASMLib.GetFlagsAsString(szxfile.MainRegs.F);

			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText("Flags: " + flags);
			gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.horizontalSpan = 2;
			lbl.setLayoutData(gd);

			flags = ASMLib.GetFlagsAsString(szxfile.AltRegs.F);

			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);
			lbl.setText("Alt Flags: " + flags);
			gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.horizontalSpan = 2;
			lbl.setLayoutData(gd);

			lbl = new Label(TargetPage, SWT.NONE);
			labels.add(lbl);

			lbl.setText("File HW info.");

			gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.horizontalSpan = 4;
			lbl.setFont(boldFont);
			lbl.setLayoutData(gd);

			
			Set<String> keys = szxfile.SnapSpecific.keySet(); 
			Iterator<String> itr = keys.iterator();
			while (itr.hasNext()) {
				String key = itr.next();
				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText(key);
				gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				gd.horizontalSpan = 1;
				lbl.setFont(boldFont);
				lbl.setLayoutData(gd);
				
				String value = szxfile.SnapSpecific.get(key);
				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText(value);
				gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				gd.horizontalSpan = 1;
				lbl.setLayoutData(gd);
				
				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				gd.horizontalSpan = 2;
				lbl.setLayoutData(gd);
				
				
				
				
			}
			
			
			
			int RamBankOrder[] = new int[8];
			byte rawdata[] = null;
			if (szxfile.MachineClass != MachineState.MT_48K) {

				rawdata = new byte[8 * 0x4000];
				System.arraycopy(szxfile.RAM, 0x0000, rawdata, 0x0000, 49152);
				RamBankOrder = new int[8];
				RamBankOrder[0] = 5;
				RamBankOrder[1] = 2;
				RamBankOrder[2] = szxfile.GetPagedRamNumber();
				int targetloc = 0xc000;
				int RBOptr = 3;
				for (int i = 0; i < 8; i++) {
					if ((i != 2) && (i != 5) && (i != szxfile.GetPagedRamNumber())) {
						RamBankOrder[RBOptr++] = i;
						System.arraycopy(szxfile.RamBanks[i], 0x0000, rawdata, targetloc, 0x4000);
						targetloc = targetloc + 0x4000;
					}
				}
			} else {
				rawdata = szxfile.RAM;
				RamBankOrder = null;
			}

			super.Render(TargetPage, rawdata, loadAddr, szxfile.MachineClass != MachineState.MT_48K, szxfile.IY(),
					RamBankOrder, filename, szxfile, targetpart);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

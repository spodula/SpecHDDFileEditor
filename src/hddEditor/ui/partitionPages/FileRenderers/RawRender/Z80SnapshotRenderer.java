package hddEditor.ui.partitionPages.FileRenderers.RawRender;
/**
 * Render a .z80 file
 * https://worldofspectrum.org/faq/reference/z80format.htm
 * 
 * This implements the decoding a z80 file as a Ram Dump with registers.
 * Its probably the most common snapshot file format.
 * 
 * Notes:
 * 	There are currently 3 .Z80 versions. Version 1 is only good for 48K snapshots only.
 * 	 Detection of version is as follows:
 *  	[6/7] PC != 0 ? 		Version 1
 *  		PC=0, [30/31] = 23? Version 2
 *  			else Version 3
 *  Compression is simple RLE encoding and can easily be written in z80
 * 
 * 
 */

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import hddEditor.libs.ASMLib;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.snapshots.MachineState;
import hddEditor.libs.snapshots.readers.Z80file;

public class Z80SnapshotRenderer extends RamDump {
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
		Z80file z80file = new Z80file(data);

		labels = new ArrayList<Label>();
		Renderers = new ArrayList<Renderer>();
		Label lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(lbl.getShell().getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));

		if (z80file.MachineClass == MachineState.MT_48K)
			lbl.setText("48K Z80 snapshot file: ");
		else
			lbl.setText("128K Z80 snapshot file: ");

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setFont(boldFont);
		lbl.setLayoutData(gd);

		Hashtable<String, String> SnapshotRegisters = z80file.DetailsAsArray();

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

		String flags = ASMLib.GetFlagsAsString(z80file.MainRegs.F);

		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText("Flags: " + flags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		flags = ASMLib.GetFlagsAsString(z80file.AltRegs.F);

		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText("Alt Flags: " + flags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		int RamBankOrder[] = new int[8];

		byte rawdata[] = null;
		if (z80file.MachineClass != MachineState.MT_48K) {

			rawdata = new byte[8 * 0x4000];
			System.arraycopy(z80file.RAM, 0x0000, rawdata, 0x0000, 49152);
			RamBankOrder = new int[8];
			RamBankOrder[0] = 5;
			RamBankOrder[1] = 2;
			RamBankOrder[2] = z80file.GetPagedRamNumber();
			int targetloc = 0xc000;
			int RBOptr = 3;
			for (int i = 0; i < 8; i++) {
				if ((i != 2) && (i != 5) && (i != z80file.GetPagedRamNumber())) {
					RamBankOrder[RBOptr++] = i;
					System.arraycopy(z80file.RamBanks[i], 0x0000, rawdata, targetloc, 0x4000);
					targetloc = targetloc + 0x4000;
				}
			}
		} else {
			rawdata = z80file.RAM;
			RamBankOrder = null;
		}

		super.Render(TargetPage, rawdata, loadAddr, z80file.MachineClass != MachineState.MT_48K, z80file.IY(),
				RamBankOrder, filename, z80file, targetpart);
	}

}

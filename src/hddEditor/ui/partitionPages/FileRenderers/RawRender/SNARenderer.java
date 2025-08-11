package hddEditor.ui.partitionPages.FileRenderers.RawRender;
/**
 * Render a SNA file, including trying to decode BASIC programs.
 * https://sinclair.wiki.zxnet.co.uk/wiki/SNA_format  (48k)
 * https://worldofspectrum.org/faq/reference/formats.htm
 * 
 * The original emulator snapshot file format. 
 * The first known use is in Peter McGavin's 'Spectrum' emulator on the Amiga,
 * 
 * It is an extremely simple format, in both its 48K and 128K versions.
 * 
 * Notes:
 * 	The 48K version has a problem in that the PC is pushed onto the stack.
 * 		and a RETN is needed to return. 
 * 		This is fine for most snapshots, but for programs that use SP manipulation for quick 
 * 		block moves, this can fail. As well as for programs that have limited stack space.
 * 
 *  That the 128k version cannot properly represent +2AB/+3AB machines as it lacks
 *   	storage for the $1FFD port. 
 * 
 * 	For the above reasons, the SNA file format is considered obsolete, although
 * 		there are a lot of SNA files created in the early days of emulation that
 * 		still use them. 
 * 
 *  48K snapshot format:
 *  ====================
 *  
 *  $00  I
 *  $01  HL'    
 *  $03  DE'
 *  $05  BC'
 *  $07  AF'
 *  $09  HL
 *  $0B  DE
 *  $0D  BC
 *  $0F  IY
 *  $11  IX
 *  $13  IFF2    [Only bit 2 is defined: 1 for EI, 0 for DI]
 *  $14  R
 *  $15  AF
 *  $17  SP
 *  $19  Interrupt mode: 0, 1 or 2
 *  $1A  Border colour
 *  $1B+ 49152 bytes of RAM.
 *  
 *  128K version:
 *  ==============
 *  $00000->$0001A 	(Same as above)
 *  $0001B->$0401A 	(Bank 5)
 *  $0401B->$0801A 	(Bank 2)
 *  $0801B->$0C01A 	(Currently paged in Bank)
 *  $0C01B		   	PC
 *  $0C01D			Last out to 0x7ffd
 *  $0C01E			TR-DOS rom Paged
 *  $0C01F			Remaining 16K RAM banks in ascending order ignoring banks 5,2 and the currently paged ram.
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
import hddEditor.libs.snapshots.readers.SNAfile;

public class SNARenderer extends RamDump {
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
		if (Renderers!=null) {
			for(Renderer r:Renderers) {
				r.DisposeRenderer();
			}
			Renderers.clear();
			Renderers = null;
		}
	}
	
	/**
	 * Treat the file as a SNA style file.
	 * 
	 * @param TargetPage - page to render to.
	 * @param data - data to render
	 * @param loadAddr - Load address - Unused for the SNA renderer.
	 * @param filename - Filename
	 */
	public void Render(Composite TargetPage, byte[] data, int loadAddr, String filename, IDEDosPartition targetpart) {
		SNAfile snafile = new SNAfile(data);
		
		labels = new ArrayList<Label>();
		Renderers = new ArrayList<Renderer>();
		Label lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(lbl.getShell().getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));

		if (snafile.MachineClass == MachineState.MT_48K) 
			lbl.setText("48K SNA snapshot file: ");
		else
			lbl.setText("128K SNA snapshot file: ");
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setFont(boldFont);
		lbl.setLayoutData(gd);
		
		Hashtable<String, String> SnapshotRegisters = snafile.DetailsAsArray();
		
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
		
		String flags = ASMLib.GetFlagsAsString(snafile.MainRegs.F);

		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText("Flags: " + flags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		flags = ASMLib.GetFlagsAsString(snafile.AltRegs.F);

		lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		lbl.setText("Alt Flags: " + flags);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		int RamBankOrder[] = new int[8];
		
		byte rawdata[] = null;
		if (snafile.MachineClass != MachineState.MT_48K) {
			if ((snafile.GetPagedRamNumber()==5) || (snafile.GetPagedRamNumber()==2)) {
				RamBankOrder= new int[9];
			}
			RamBankOrder[0] = 5;
			RamBankOrder[1] = 2;
			RamBankOrder[2] = snafile.GetPagedRamNumber();
			int ptr=3;
			for (int i=0;i<8;i++) {
				if ((i!=2) && (i!=5) && (i!=snafile.GetPagedRamNumber())) {
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
					System.arraycopy(data, restbase, rawdata, targBase , Math.min(data.length - restbase,16384));
				}
				restbase = restbase + 0x4000;
				targBase = targBase + 0x4000;
			}
		} else {
			rawdata = snafile.RAM;
		}
		
		super.Render(TargetPage, rawdata, loadAddr, snafile.MachineClass != MachineState.MT_48K, snafile.IY(), RamBankOrder, filename,snafile,targetpart);
	}
}

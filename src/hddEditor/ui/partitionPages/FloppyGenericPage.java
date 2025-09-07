package hddEditor.ui.partitionPages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.Languages;
import hddEditor.libs.disks.FDD.FloppyDisk;
import hddEditor.libs.disks.FDD.Sector;
import hddEditor.libs.disks.FDD.TrackInfo;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.ui.HDDEditor;

public class FloppyGenericPage extends GenericPage {
	Combo TrackCombo = null;
	Composite TrackComposite = null;

	public FloppyGenericPage(HDDEditor root, Composite parent, IDEDosPartition partition, FileSelectDialog filesel, Languages lang) {
		super(root, parent, partition, filesel,lang);
		AddComponents();
	}

	private void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			super.AddBasicDetails();
			label("", 4);
			FloppyDisk fdd = (FloppyDisk) partition.CurrentDisk;
			Label lbl = label(lang.Msg(Languages.MSG_TRACK)+ ":", 1);
			FontData fontData = lbl.getFont().getFontData()[0];
			Font font = new Font(ParentComp.getDisplay(),
					new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
			lbl.setFont(font);

			String tracknames[] = new String[fdd.diskTracks.length];
			for (int i = 0; i < fdd.diskTracks.length; i++) {
				tracknames[i] = lang.Msg(Languages.MSG_TRACK)+" #" + fdd.diskTracks[i].tracknum + " "+lang.Msg(Languages.MSG_SIDE)+" #" + fdd.diskTracks[i].side;
			}
			TrackCombo = combo(tracknames, tracknames[0]);

			label("", 2);
			
			SetTrack(fdd.diskTracks[0]);
			
			TrackCombo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					if (TrackCombo!=null) {
						int selecteditem = TrackCombo.getSelectionIndex();
						if (selecteditem> -1) {
							TrackInfo SelectedTrack = fdd.diskTracks[selecteditem];
							SetTrack(SelectedTrack);
						}
						ParentComp.pack();
					}
					
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
			
		}
		ParentComp.pack();
	}

	private void SetTrack(TrackInfo SelectedTrack) {
		if ((TrackComposite != null) && !TrackComposite.isDisposed()) {
			for(Control child:TrackComposite.getChildren()) {
				if (!child.isDisposed())
					child.dispose();
			}
			TrackComposite.dispose();
			ParentComp.pack();
		}

		Composite MainComp = ParentComp;
		try {
			TrackComposite = new Composite(ParentComp, SWT.BORDER);
			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan = 4;
			gd.heightHint = 500;
			gd.widthHint = 800;
			TrackComposite.setLayoutData(gd);
			
			GridLayout gridLayout = new GridLayout(4, true);
			gridLayout.marginWidth = 5;
			gridLayout.marginHeight = 5;
			gridLayout.verticalSpacing = 0;
			gridLayout.horizontalSpacing = 20;
			TrackComposite.setLayout(gridLayout);
			
			ParentComp = TrackComposite;
			label(lang.Msg(Languages.MSG_TRACK)+": " + SelectedTrack.tracknum, 1);
			label(lang.Msg(Languages.MSG_SIDE)+": " + SelectedTrack.side, 1);
			label(lang.Msg(Languages.MSG_SECTORR)+": " + Integer.toHexString(SelectedTrack.minsectorID) + "-"
					+ Integer.toHexString(SelectedTrack.maxsectorID), 1);
			label(lang.Msg(Languages.MSG_FILLERB)+"Filler byte: " + SelectedTrack.fillerByte, 1);
			
			label(lang.Msg(Languages.MSG_GAP3)+": " + SelectedTrack.gap3len, 1);
			label(lang.Msg(Languages.MSG_DATARATE)+": " + SelectedTrack.datarate, 1);
			label(lang.Msg(Languages.MSG_NUMSECTORS)+": " + SelectedTrack.numsectors, 1);
			label(lang.Msg(Languages.MSG_RECMODE)+": " + SelectedTrack.recordingmode, 1);
			
			label(lang.Msg(Languages.MSG_SECTSZ)+": " + SelectedTrack.sectorsz, 1);
			label("",3);
		    // Create a multiple-line text field
		    Text t = new Text(TrackComposite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		    gd = new GridData(GridData.FILL_BOTH);
		    gd.horizontalSpan = 4;
		    t.setLayoutData(gd);
			Font MonoFont = new Font(ParentComp.getDisplay(), new FontData("Monospace", 12, SWT.NONE));
			t.setFont(MonoFont);
		    
		    String s = "";
		    for(Sector sect: SelectedTrack.Sectors) {
		    	s = s + lang.Msg(Languages.MSG_SECTOR)+" "+Integer.toHexString(sect.sectorID)+" ("+sect.sectorID+") "+System.lineSeparator();
		    	s = s + "FDC Sr1: "+sect.FDCsr1;
		    	s = s + "  FDC Sr2: "+sect.FDCsr2;
		    	s = s + "  "+lang.Msg(Languages.MSG_ACTSIZE)+": "+sect.ActualSize;
		    	s = s + "  FDC size: "+sect.Sectorsz+System.lineSeparator();
		    	s = s + lang.Msg(Languages.MSG_SECTDATA)+":"+System.lineSeparator();
		    	s = s + GeneralUtils.HexDump(sect.data,0, sect.data.length,0 );
		    	s = s + System.lineSeparator()+System.lineSeparator();
		    }
		    t.setText(s);
		} finally {
			ParentComp = MainComp;
		}
	}
}

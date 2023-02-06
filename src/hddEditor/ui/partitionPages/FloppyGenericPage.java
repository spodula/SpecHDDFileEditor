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

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.disks.FDD.FloppyDisk;
import hddEditor.libs.disks.FDD.Sector;
import hddEditor.libs.disks.FDD.TrackInfo;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.ui.HDDEditor;

public class FloppyGenericPage extends GenericPage {
	Combo TrackCombo = null;
	Composite TrackComposite = null;

	public FloppyGenericPage(HDDEditor root, Composite parent, IDEDosPartition partition) {
		super(root, parent, partition);
		AddComponents();
	}

	private void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			super.AddBasicDetails();
			label("", 4);
			FloppyDisk fdd = (FloppyDisk) partition.CurrentDisk;
			Label lbl = label("Track:", 1);
			FontData fontData = lbl.getFont().getFontData()[0];
			Font font = new Font(ParentComp.getDisplay(),
					new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
			lbl.setFont(font);

			String tracknames[] = new String[fdd.diskTracks.length];
			for (int i = 0; i < fdd.diskTracks.length; i++) {
				tracknames[i] = "Track #" + fdd.diskTracks[i].tracknum + " Side #" + fdd.diskTracks[i].side;
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
			label("Track: " + SelectedTrack.tracknum, 1);
			label("Side: " + SelectedTrack.side, 1);
			label("Sector range: " + Integer.toHexString(SelectedTrack.minsectorID) + "-"
					+ Integer.toHexString(SelectedTrack.maxsectorID), 1);
			label("Filler byte: " + SelectedTrack.fillerByte, 1);
			
			label("Gap3Len: " + SelectedTrack.gap3len, 1);
			label("Data rate: " + SelectedTrack.datarate, 1);
			label("Number of sectors: " + SelectedTrack.numsectors, 1);
			label("Recording mode: " + SelectedTrack.recordingmode, 1);
			
			label("Sector size: " + SelectedTrack.sectorsz, 1);
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
		    	s = s + "Sector "+Integer.toHexString(sect.sectorID)+" ("+sect.sectorID+") \n";
		    	s = s + "FDC Sr1: "+sect.FDCsr1;
		    	s = s + "  FDC Sr2: "+sect.FDCsr2;
		    	s = s + "  Actual size: "+sect.ActualSize;
		    	s = s + "  FDC size: "+sect.Sectorsz+"\n";
		    	s = s + "Data:\n";
		    	s = s + GeneralUtils.HexDump(sect.data,0, sect.data.length,0 );
		    	s = s + "\n\n";
		    }
		    
		    t.setText(s);
			
			
		} finally {
			ParentComp = MainComp;
		}


		
	}

}

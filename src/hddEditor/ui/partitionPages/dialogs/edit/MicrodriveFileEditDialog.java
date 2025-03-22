package hddEditor.ui.partitionPages.dialogs.edit;
/**
 * Implementation of the file edit page for a Microdrive file.
 * 
 * TODO: Saves for MDR BASIC/Start line  BASIC/VARSTART
 * TODO: Saves for MDR Variable name for Variables.
 * TODO: Saves for MDR Code load address

 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.disks.LINEAR.MicrodriveSector;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.mdf.MicrodriveDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;
import hddEditor.ui.partitionPages.FileRenderers.FileRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;


public class MicrodriveFileEditDialog extends EditFileDialog {     

	public MicrodriveFileEditDialog(Display display,FileSelectDialog filesel,IDEDosPartition CurrentPartition) {
		super(display,filesel, CurrentPartition);
	}

	/**
	 * Create the form
	 */
	@Override
	protected void Createform() {
		shell = new Shell(display);
		shell.setSize(900, 810);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.makeColumnsEqualWidth = true;
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;
		shell.setLayout(gridLayout);
		
		MicrodriveDirectoryEntry mde = (MicrodriveDirectoryEntry)ThisEntry;

		Label lbl = label(ThisEntry.GetSpeccyBasicDetails().BasicTypeString()+" file",4);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setFont(boldFont);
		
		label(String.format("Length Inc Header: %d bytes (%X)", ThisEntry.GetRawFileSize(), ThisEntry.GetRawFileSize()),2);
		label(String.format("Length without Header: %d bytes (%X)", ThisEntry.GetFileSize(), ThisEntry.GetFileSize()),2);

		String logblocks = "";
		for (MicrodriveSector mds : mde.sectors) {
			logblocks = logblocks + ", " + mds.GetSectorNumber();
		}
		if (logblocks.length() > 2) {
			logblocks = logblocks.substring(2);
		}
		
		label("Used sectors: " + mde.sectors.length + " (" + logblocks + ")",2);
		
		MainPage1 = new ScrolledComposite(shell, SWT.V_SCROLL);
		MainPage1.setExpandHorizontal(true);
		MainPage1.setExpandVertical(true);
		MainPage1.setAlwaysShowScrollBars(true);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 4;
		MainPage1.setLayoutData(gd);

		
		MainPage = new Composite(MainPage1, SWT.NONE);
		MainPage1.setContent(MainPage);
		
		gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.makeColumnsEqualWidth = true;
		MainPage.setLayout(gridLayout);
		
		MainPage1.addControlListener(new ControlListener() {
			
			@Override
			public void controlResized(ControlEvent arg0) {
				MainPage1.setMinSize(MainPage.computeSize(MainPage1.getClientArea().width, SWT.DEFAULT));
			}
			
			@Override
			public void controlMoved(ControlEvent arg0) {
			}
		});
				
		RenderAppropriatePage();
		shell.pack();
	}
	
	private void RenderAppropriatePage() {
		/**
		 * Render the page.
		 */
		switch (ThisEntry.GetSpeccyBasicDetails().BasicType) {
		case Speccy.BASIC_BASIC: 
			BasicRenderer BR = new BasicRenderer();
			SpeccyBasicDetails sbd = ThisEntry.GetSpeccyBasicDetails();
			BR.RenderBasic(MainPage, data, null, ThisEntry.GetFilename(), data.length, 
					sbd.VarStart, sbd.LineStart, filesel, null);
			break;
		case Speccy.BASIC_CODE:
				CodeRenderer CR = new CodeRenderer();
				//TODO: implement MicroDriveFileEdit.saveevent for CODE

				CR.RenderCode(MainPage, data, null, ThisEntry.GetFilename(), data.length, ((MicrodriveDirectoryEntry)ThisEntry).GetVar2(), filesel,CurrentPartition, null);
				break;
		case Speccy.BASIC_NUMARRAY:
				NumericArrayRenderer NR = new NumericArrayRenderer();
				NR.RenderNumericArray(MainPage, data, null, ThisEntry.GetFilename(), "A", filesel, null);
				break;
		case Speccy.BASIC_CHRARRAY:	
				CharArrayRenderer CAR = new CharArrayRenderer();
				CAR.RenderCharArray(MainPage, data, null, ThisEntry.GetFilename(), "A", filesel, null);
		default:
				FileRenderer FR = new FileRenderer();
				FR.Render(MainPage, data, ThisEntry.GetFilename(), filesel);
		}
	}

}

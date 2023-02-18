package hddEditor.ui.partitionPages.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import hddEditor.libs.Speccy;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.disks.LINEAR.MicrodriveSector;
import hddEditor.libs.partitions.mdf.MicrodriveDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;
import hddEditor.ui.partitionPages.FileRenderers.FileRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;


public class MicrodriveFileEditDialog {
	// Title of the page
	private String Title = "";
	
	// Form details
	private Shell shell;
	private Display display;

	// Composite we are parented to
	private Composite MainPage = null;
	
	// Result
	private boolean result = false;

	// Data for the file
	public byte[] data = new byte[0];

	// Directory entry of the file being displayed
	private MicrodriveDirectoryEntry ThisEntry = null;
	
	/*
	 * Set modified text in the title
	 */
	private void SetModified(boolean Modified) {
		String s = Title;
		if (Modified) {
			s = s + " (Modified)";
		}
		shell.setText(s);
	}
	
	public MicrodriveFileEditDialog(Display display) {
		this.display = display;
	}

	public boolean Show(byte[] data, String title, MicrodriveDirectoryEntry entry) {
		this.result = false;
		this.ThisEntry = entry;
		this.Title = title;
		this.data = data;
		Createform();
		SetModified(false);
		loop();
		return (result);
	}
	
	/**
	 * Loop and wait
	 */
	public void loop() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	/**
	 * Create the form
	 */
	private void Createform() {
		shell = new Shell(display);
		shell.setSize(900, 810);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.makeColumnsEqualWidth = true;
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;
		shell.setLayout(gridLayout);

		Label lbl = label(ThisEntry.GetSpeccyBasicDetails().BasicTypeString()+" file",4);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setFont(boldFont);
		
		label(String.format("Length Inc Header: %d bytes (%X)", ThisEntry.GetRawFileSize(), ThisEntry.GetRawFileSize()),2);
		label(String.format("Length without Header: %d bytes (%X)", ThisEntry.GetFileSize(), ThisEntry.GetFileSize()),2);

		String logblocks = "";
		for (MicrodriveSector mds : ThisEntry.sectors) {
			logblocks = logblocks + ", " + mds.GetSectorNumber();
		}
		if (logblocks.length() > 2) {
			logblocks = logblocks.substring(2);
		}
		
		label("Used sectors: " + ThisEntry.sectors.length + " (" + logblocks + ")",2);
		
		MainPage = new Composite(shell, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = shell.getSize().x;
		gd.horizontalSpan = 4;
		MainPage.setLayoutData(gd);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.makeColumnsEqualWidth = true;
		MainPage.setLayout(gridLayout);

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
					sbd.VarStart, sbd.LineStart);
			break;
		case Speccy.BASIC_CODE:
				CodeRenderer CR = new CodeRenderer();
				CR.RenderCode(MainPage, data, null, ThisEntry.GetFilename(), data.length, ThisEntry.GetVar2());
				break;
		case Speccy.BASIC_NUMARRAY:
				NumericArrayRenderer NR = new NumericArrayRenderer();
				NR.RenderNumericArray(MainPage, data, null, ThisEntry.GetFilename(), "A");
				break;
		case Speccy.BASIC_CHRARRAY:	
				CharArrayRenderer CAR = new CharArrayRenderer();
				CAR.RenderCharArray(MainPage, data, null, ThisEntry.GetFilename(), "A");
		default:
				FileRenderer FR = new FileRenderer();
				FR.Render(MainPage, data, ThisEntry.GetFilename());
				
		}
	}

	/**
	 * Create a generic label with the given text and span.
	 * 
	 * @param text
	 * @param span
	 * @return
	 */
	public Label label(String text, int span) {
		Label label = new Label(shell, SWT.SHADOW_NONE);
		label.setText(text);
		if (span>1) {
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 4;
			label.setLayoutData(gd);
		}
		return(label);
	}
	
	/**
	 * Close dialog
	 */
	public void close() {
		shell.close();
		if (!shell.isDisposed()) {
			shell.dispose();
		}
	}

}

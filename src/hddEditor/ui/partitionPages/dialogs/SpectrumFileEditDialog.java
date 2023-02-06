package hddEditor.ui.partitionPages.dialogs;

/**
 * FIle edit dialog for the +3DOS partition page
 */

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
import hddEditor.libs.partitions.cpm.DirectoryEntry;
import hddEditor.libs.partitions.cpm.Dirent;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.FileRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;

public class SpectrumFileEditDialog {
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
	private DirectoryEntry ThisEntry = null;

	// size of the data as as +3 Basic sees it (With +3Header)
	// This is used to trim off excess records before rendering.
	private int Plus3Size = 0;

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

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public SpectrumFileEditDialog(Display display) {
		this.display = display;
	}

	/**
	 * Show the form
	 * 
	 * @param data
	 * @param title
	 * @param entry
	 * @return
	 */
	public boolean Show(byte[] data, String title, DirectoryEntry entry) {
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

		Label lbl = new Label(shell, SWT.NONE);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setText(String.format("CPM Length: %d bytes (%X)", data.length, data.length));
		lbl.setFont(boldFont);
		lbl = new Label(shell, SWT.NONE);
		String logblocks = "";
		int BlockCount = 0;
		for (Dirent dirent : ThisEntry.dirents) {
			for (int blockNum : dirent.getBlocks()) {
				logblocks = logblocks + ", " + blockNum;
				BlockCount++;
			}
		}
		if (logblocks.length() > 2) {
			logblocks = logblocks.substring(2);
		}
		lbl.setText("Logical blocks: " + BlockCount + " (" + logblocks + ")");

		Plus3DosFileHeader p3d = new Plus3DosFileHeader(data);
		if (p3d.IsPlusThreeDosFile) {
			lbl = new Label(shell, SWT.NONE);
			lbl.setText(String.format("+3DOS Length: %d bytes (%X)", p3d.fileSize, p3d.fileSize));
			lbl.setFont(boldFont);
			Plus3Size = p3d.fileSize + 0x80;
		} else {
			lbl = new Label(shell, SWT.NONE);
			lbl.setText("Not a +3DOS file (Or header corrupt)");
			lbl.setFont(boldFont);
			Plus3Size = data.length;
		}
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		MainPage = new Composite(shell, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
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

	/**
	 * Render the correct page for the file.
	 */
	private void RenderAppropriatePage() {
		Plus3DosFileHeader p3d = ThisEntry.GetPlus3DosHeader();

		byte header[] = null;
		byte newdata[] = data;
		if (p3d.IsPlusThreeDosFile) {
			// Separate the header and the file data.
			header = new byte[0x80];
			System.arraycopy(data, 0, header, 0, 0x80);
			newdata = new byte[data.length - 0x80];
			System.arraycopy(data, 0x80, newdata, 0, newdata.length);
		}

		/*
		 * CPM saves file in 128 byte chunks. This trims the data down to the size that
		 * Speccy basic sees.
		 */
		byte TrimmedData[] = newdata;
		if (Plus3Size < newdata.length) {
			TrimmedData = new byte[Plus3Size];
			System.arraycopy(newdata, 0, TrimmedData, 0, Plus3Size);
		}
		newdata = TrimmedData;

		/**
		 * Render the page.
		 */
		if (!p3d.IsPlusThreeDosFile) {
//			FileRenderer FR = new FileRenderer();
//			FR.Render(MainPage, newdata, ThisEntry.filename());
			CodeRenderer CR = new CodeRenderer();
			CR.RenderCode(MainPage, newdata, null, ThisEntry.filename(), data.length, 0x0000);
		} else if (p3d.filetype == Speccy.BASIC_BASIC) {
			BasicRenderer BR = new BasicRenderer();
			BR.RenderBasic(MainPage, newdata, header, ThisEntry.filename(), p3d.filelength, 
					p3d.VariablesOffset, p3d.line);
		} else if (p3d.filetype == Speccy.BASIC_CODE) {
			CodeRenderer CR = new CodeRenderer();
			CR.RenderCode(MainPage, newdata, header, ThisEntry.filename(), data.length, p3d.loadAddr);
		} else if (p3d.filetype == Speccy.BASIC_NUMARRAY) {
			NumericArrayRenderer NR = new NumericArrayRenderer();
			NR.RenderNumericArray(MainPage, newdata, header, ThisEntry.filename(), p3d.VarName);
		} else { // Char array
			CharArrayRenderer CR = new CharArrayRenderer();
			CR.RenderCharArray(MainPage, newdata, header, ThisEntry.filename(), p3d.VarName);
		}
	}

	/**
	 * CLose the form.
	 */
	public void close() {
		shell.close();
		if (!shell.isDisposed()) {
			shell.dispose();
		}
	}

}

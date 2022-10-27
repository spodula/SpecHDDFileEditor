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

import hddEditor.libs.partitions.PLUS3DOSPartition;
import hddEditor.libs.partitions.cpm.DirectoryEntry;
import hddEditor.libs.partitions.cpm.Dirent;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.FileRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;

public class SpectrumFileEditDialog {
	private String Title = "";

	private Shell shell;
	private Display display;

	private boolean result = false;

	public byte[] data = new byte[0];

	private DirectoryEntry ThisEntry = null;

	private Composite MainPage = null;

	private FileRenderer CurrentRenderer = null;

	// size of the data as as +3 Basic sees it (With +3Header)
	// This is used to trim off excess records before rendering.
	private int Plus3Size = 0;

	private void SetModified(boolean Modified) {
		String s = Title;
		if (Modified) {
			s = s + " (Modified)";
		}
		shell.setText(s);
	}

	public SpectrumFileEditDialog(Display display) {
		this.display = display;
	}

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

	public void loop() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

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

	private void RenderAppropriatePage() {

		Plus3DosFileHeader p3d = ThisEntry.GetPlus3DosHeader();
		if (!p3d.IsPlusThreeDosFile) {
			CurrentRenderer = new FileRenderer();
		} else {
			switch (p3d.filetype) {
			case PLUS3DOSPartition.BASIC_BASIC:
				CurrentRenderer = new BasicRenderer();
				break;
			case PLUS3DOSPartition.BASIC_CHRARRAY:
				CurrentRenderer = new CharArrayRenderer();
				break;
			case PLUS3DOSPartition.BASIC_NUMARRAY:
				CurrentRenderer = new NumericArrayRenderer();
				break;
			case PLUS3DOSPartition.BASIC_CODE:
				CurrentRenderer = new CodeRenderer();
				break;
			}
		}

		byte TrimmedData[] = this.data;
		if (Plus3Size < data.length) {
			TrimmedData = new byte[Plus3Size];
			System.arraycopy(data, 0, TrimmedData, 0, Plus3Size);
		}
		CurrentRenderer.Render(MainPage, TrimmedData, ThisEntry.filename());
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

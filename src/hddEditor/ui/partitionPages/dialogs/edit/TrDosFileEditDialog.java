package hddEditor.ui.partitionPages.dialogs.edit; 
//TODO: SCL files just dont seem to work.
/**
 * Implementation of the Edit file page for a TR-DOS file.
 * 
 * TODO: Saves for TR-DOS BASIC/Start line  BASIC/VARSTART
 * TODO: Saves for TR-DOS Variable name for Variables.
 * TODO: Saves for TR-DOS Code load address

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
import hddEditor.libs.disks.FileEntry;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.trdos.TrdDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;

public class TrDosFileEditDialog extends EditFileDialog {
	// Directory entry of the file being displayed
	//private TrdDirectoryEntry ThisEntry = null;
	private FileEntry ThisEntry = null;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public TrDosFileEditDialog(Display display, FileSelectDialog filesel,IDEDosPartition CurrentPartition) {
		super(display, filesel,CurrentPartition);
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

		Label lbl = new Label(shell, SWT.NONE);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setText(String.format("CPM Length: %d bytes (%X)", data.length, data.length));
		lbl.setFont(boldFont);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		MainPage1 = new ScrolledComposite(shell, SWT.V_SCROLL);
		MainPage1.setExpandHorizontal(true);
		MainPage1.setExpandVertical(true);
		MainPage1.setAlwaysShowScrollBars(true);
		gd = new GridData(GridData.FILL_BOTH);
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

	/**
	 * Render the correct page for the file.
	 */
	private void RenderAppropriatePage() {
		try {
			TrdDirectoryEntry trde = (TrdDirectoryEntry)ThisEntry; 
			char ftype = trde.GetFileType();
			if (ftype == 'B') {
				BasicRenderer CurrentRenderer = new BasicRenderer();
				CurrentRenderer.RenderBasic(MainPage, data, null, ThisEntry.GetFilename(), ThisEntry.GetFileSize(), 
						trde.GetVar2(), trde.startline, filesel, null);
			} else if (ftype != 'D') {
				CodeRenderer CurrentRenderer = new CodeRenderer();
				//TODO: implement TRDosFileEdit.saveevent for CODE

				CurrentRenderer.RenderCode(MainPage, data, null, ThisEntry.GetFilename(), data.length,
						trde.GetVar1(), filesel,CurrentPartition,null);
			} else if (trde.IsCharArray()) {
				CharArrayRenderer CurrentRenderer = new CharArrayRenderer();
				CurrentRenderer.RenderCharArray(MainPage, data, null, ThisEntry.GetFilename(), "A", filesel, null);
			} else {
				NumericArrayRenderer CurrentRenderer = new NumericArrayRenderer();
				CurrentRenderer.RenderNumericArray(MainPage, data, null, ThisEntry.GetFilename(), "A", filesel, null);
			}
		} catch (Exception E) {
			System.out.println("Error Showing " + ThisEntry.GetFilename() + ": " + E.getMessage());
		}
	}
}

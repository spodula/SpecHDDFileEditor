package hddEditor.ui.partitionPages.dialogs.edit;
/**
 * Implementation of the Edit file page for an MGT file.
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

import hddEditor.libs.MGT;
import hddEditor.libs.partitions.mgt.MGTDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;
import hddEditor.ui.partitionPages.FileRenderers.MGT48kSnapshotRenderer;
import hddEditor.ui.partitionPages.FileRenderers.MGT128kSnapshotRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.MGTExecuteRenderer;

public class MGTDosFileEditDialog extends EditFileDialog {

	public MGTDosFileEditDialog(Display display) {
		super(display);
	}
	
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
		lbl.setText(String.format("File Length: %d bytes (%X)", data.length, data.length));
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

	private void RenderAppropriatePage() {
		try {
			MGTDirectoryEntry mEnt = (MGTDirectoryEntry)ThisEntry;
			int ftype = mEnt.GetFileType();
			if (ftype == MGT.MGTFT_ZXBASIC) {
				BasicRenderer CurrentRenderer = new BasicRenderer();
				CurrentRenderer.RenderBasic(MainPage, data, null, mEnt.GetFilename(), mEnt.GetFileSize(),
						mEnt.GetSpeccyBasicDetails().VarStart, mEnt.GetSpeccyBasicDetails().LineStart);
			} else if (ftype == MGT.MGTFT_ZXNUMARRAY) {
				NumericArrayRenderer CurrentRenderer = new NumericArrayRenderer();
				CurrentRenderer.RenderNumericArray(MainPage, data, null, mEnt.GetFilename(),
						"" + mEnt.GetSpeccyBasicDetails().VarName);
			} else if (ftype == MGT.MGTFT_ZXSTRARRAY) {
				CharArrayRenderer CurrentRenderer = new CharArrayRenderer();
				CurrentRenderer.RenderCharArray(MainPage, data, null, mEnt.GetFilename(),
						"" + mEnt.GetSpeccyBasicDetails().VarName);
			} else if (ftype == MGT.MGTFT_ZX48SNA) {
				MGT48kSnapshotRenderer CurrentRenderer = new MGT48kSnapshotRenderer();
				CurrentRenderer.RenderSnapshot(MainPage, data, mEnt.GetFilename(), mEnt);
			} else if (ftype == MGT.MGTFT_ZX128SNA) {
				MGT128kSnapshotRenderer CurrentRenderer = new MGT128kSnapshotRenderer();
				CurrentRenderer.RenderSnapshot(MainPage, data, mEnt.GetFilename(), mEnt);
				
			} else if (ftype == MGT.MGTFT_ZXEXE) {
				MGTExecuteRenderer CurrentRenderer = new MGTExecuteRenderer();
				CurrentRenderer.Render(MainPage, data, mEnt.GetFilename());
			} else {
				CodeRenderer CurrentRenderer = new CodeRenderer();
				CurrentRenderer.RenderCode(MainPage, data, null, mEnt.GetFilename(), data.length,
						mEnt.GetVar1());
			}
		} catch (Exception E) {
			System.out.println("Error Showing " + ThisEntry.GetFilename()+ ": " + E.getMessage());
		}
	}

}

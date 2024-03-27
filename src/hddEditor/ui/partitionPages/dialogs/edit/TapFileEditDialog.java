package hddEditor.ui.partitionPages.dialogs.edit;
/**
 * Implementation of the Edit file page for an TAP file.
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

import hddEditor.libs.Speccy;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;

public class TapFileEditDialog extends EditFileDialog {
	public TapFileEditDialog(Display display) {
		super(display);
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

		Label lbl = label(ThisEntry.GetSpeccyBasicDetails().BasicTypeString() + " file", 4);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setFont(boldFont);

		label(String.format("Length : %d bytes (%X)", ThisEntry.GetRawFileSize(), ThisEntry.GetRawFileSize()), 2);

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
		CodeRenderer CR;
		SpeccyBasicDetails sbd = ThisEntry.GetSpeccyBasicDetails();
		switch (sbd.BasicType) {
		case Speccy.BASIC_BASIC:
			BasicRenderer BR = new BasicRenderer();
			BR.RenderBasic(MainPage, data, null, ThisEntry.GetFilename(), data.length, sbd.VarStart, sbd.LineStart);
			break;
		case Speccy.BASIC_CODE:
			CR = new CodeRenderer();
			CR.RenderCode(MainPage, data, null, ThisEntry.GetFilename(), data.length, sbd.LoadAddress);
			break;
		case Speccy.BASIC_NUMARRAY:
			NumericArrayRenderer NR = new NumericArrayRenderer();
			NR.RenderNumericArray(MainPage, data, null, ThisEntry.GetFilename(), "A");
			break;
		case Speccy.BASIC_CHRARRAY:
			CharArrayRenderer CAR = new CharArrayRenderer();
			CAR.RenderCharArray(MainPage, data, null, ThisEntry.GetFilename(), "A");
		default:
			CR = new CodeRenderer();
			CR.RenderCode(MainPage, data, null, ThisEntry.GetFilename(), data.length, 0x0000);
		}
	}

}


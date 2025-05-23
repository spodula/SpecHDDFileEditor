package hddEditor.ui.partitionPages.dialogs.edit;

/**
 * Implementation of the Edit file page for an MGT file.
 *
 */

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.MGT;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.MGTDosPartition;
import hddEditor.libs.partitions.mgt.MGTDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;
import hddEditor.ui.partitionPages.FileRenderers.MGT48kSnapshotRenderer;
import hddEditor.ui.partitionPages.FileRenderers.MGT128kSnapshotRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;
import hddEditor.ui.partitionPages.dialogs.edit.callbacks.GenericSaveEvent;
import hddEditor.ui.partitionPages.FileRenderers.MGTExecuteRenderer;
import hddEditor.ui.partitionPages.FileRenderers.MGTScreenRenderer;

public class MGTDosFileEditDialog extends EditFileDialog {
	public int NewFileType;
	public boolean FileTypeHasChanged;

	public MGTDosFileEditDialog(Display display, FileSelectDialog filesel, IDEDosPartition CurrentPartition) {
		super(display, filesel, CurrentPartition);
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


		MGTDirectoryEntry mgt = (MGTDirectoryEntry) ThisEntry;
		Combo filetype = new Combo(shell, SWT.NONE);
		
		filetype.setItems(MGT.MGTFileTypes);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		filetype.setLayoutData(gd);

		Button SetFileType = new Button(shell, SWT.NONE);
		SetFileType.setText("Update file type");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		SetFileType.setLayoutData(gd);

		lbl = new Label(shell, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		filetype.select(mgt.GetFileType());

		SetFileType.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				NewFileType = filetype.getSelectionIndex();
				FileTypeHasChanged = true;
				close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

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
			MGTDirectoryEntry mEnt = (MGTDirectoryEntry) ThisEntry;
			int ftype = mEnt.GetFileType();
			if (ftype == MGT.MGTFT_ZXBASIC) {
				BasicRenderer CurrentRenderer = new BasicRenderer();
				CurrentRenderer.RenderBasic(MainPage, data, null, mEnt.GetFilename(), mEnt.GetFileSize(),
						mEnt.GetSpeccyBasicDetails().VarStart, mEnt.GetSpeccyBasicDetails().LineStart, filesel,
						new BasicSave());
			} else if (ftype == MGT.MGTFT_ZXNUMARRAY) {
				NumericArrayRenderer CurrentRenderer = new NumericArrayRenderer();
				CurrentRenderer.RenderNumericArray(MainPage, data, null, mEnt.GetFilename(),
						"" + mEnt.GetSpeccyBasicDetails().VarName, filesel, null);
			} else if (ftype == MGT.MGTFT_SAMSCREEN) {
				MGTScreenRenderer CurrentRenderer = new MGTScreenRenderer();
				CurrentRenderer.RenderScreen(MainPage, data, mEnt.GetFilename(), mEnt, filesel);
			} else if (ftype == MGT.MGTFT_ZXSTRARRAY) {
				CharArrayRenderer CurrentRenderer = new CharArrayRenderer();
				CurrentRenderer.RenderCharArray(MainPage, data, null, mEnt.GetFilename(),
						"" + mEnt.GetSpeccyBasicDetails().VarName, filesel, null);
			} else if (ftype == MGT.MGTFT_ZX48SNA) {
				MGT48kSnapshotRenderer CurrentRenderer = new MGT48kSnapshotRenderer();
				CurrentRenderer.RenderSnapshot(MainPage, data, mEnt.GetFilename(), mEnt, filesel);
			} else if (ftype == MGT.MGTFT_ZX128SNA) {
				MGT128kSnapshotRenderer CurrentRenderer = new MGT128kSnapshotRenderer();
				CurrentRenderer.RenderSnapshot(MainPage, data, mEnt.GetFilename(), mEnt, filesel);
			} else if (ftype == MGT.MGTFT_ZXEXE) {
				MGTExecuteRenderer CurrentRenderer = new MGTExecuteRenderer();
				CurrentRenderer.Render(MainPage, data, mEnt.GetFilename(), filesel);
			} else {
				CodeRenderer CurrentRenderer = new CodeRenderer();
				CurrentRenderer.RenderCode(MainPage, data, null, mEnt.GetFilename(), data.length, mEnt.GetLoadAddress(),
						filesel, CurrentPartition, new CodeSave());
			}
		} catch (Exception E) {
			System.out.println("Error Showing " + ThisEntry.GetFilename() + ": " + E.getMessage());
		}
	}

	/**
	 * Save for CODE files. Only the LOAD address is save-able.
	 */
	private class CodeSave implements GenericSaveEvent {
		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			MGTDirectoryEntry direntry = (MGTDirectoryEntry) ThisEntry;

			System.out.print("Load address: " + direntry.GetLoadAddress() + " -> ");
			direntry.SetLoadAddress(Value);
			System.out.println(direntry.GetLoadAddress());

			try {
				((MGTDosPartition) CurrentPartition).SaveDirectoryEntry(direntry);
				return true;
			} catch (IOException e) {
				System.out.println("Error updating directory entry");
				e.printStackTrace();
			}
			return false;
		}
	}

	/**
	 * Save for BASIC files. Start line(0) and Vars(1) offset are save-able.
	 */
	private class BasicSave implements GenericSaveEvent {
		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			MGTDirectoryEntry direntry = (MGTDirectoryEntry) ThisEntry;

			if (valtype == 0) {
				System.out.print("Start Line: " + direntry.GetStartLine() + " -> ");
				direntry.SetStartLine(Value);
				System.out.println(direntry.GetStartLine());
			} else {
				System.out.print("Vars Offset: " + direntry.GetVar1() + " -> ");
				direntry.SetVar1(Value);
				System.out.println(direntry.GetVar1());
			}

			try {
				((MGTDosPartition) CurrentPartition).SaveDirectoryEntry(direntry);
				return true;

			} catch (IOException e) {
				System.out.println("Error updating directory entry");
				e.printStackTrace();
			}
			return false;
		}
	}

}

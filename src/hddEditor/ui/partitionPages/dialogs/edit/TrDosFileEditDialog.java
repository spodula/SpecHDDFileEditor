package hddEditor.ui.partitionPages.dialogs.edit;

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
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.TrDosPartition;
import hddEditor.libs.partitions.trdos.TrdDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;
import hddEditor.ui.partitionPages.dialogs.edit.callbacks.GenericSaveEvent;

public class TrDosFileEditDialog extends EditFileDialog {
	public String NewFileType;
	public boolean FileTypeHasChanged;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public TrDosFileEditDialog(Display display, FileSelectDialog filesel, IDEDosPartition CurrentPartition) {
		super(display, filesel, CurrentPartition);
		FileTypeHasChanged = false;
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
		lbl.setText(String.format("Length: %d bytes (%X)", data.length, data.length));
		lbl.setFont(boldFont);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);
		
		TrdDirectoryEntry trd = (TrdDirectoryEntry) ThisEntry;
		char ftype = trd.GetFileType();
		
		if ((ftype=='C') || (ftype=='D') || (ftype=='B')) {
			Combo filetype = new Combo(shell, SWT.NONE);
			filetype.setItems(new String[] {"B - Basic","C - Code","D - Array"});
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

			if (ftype=='B') {
				filetype.select(0);	
			} else if (ftype=='C') {
				filetype.select(1);	
			} else if (ftype=='D') {
				filetype.select(2);	
			}
			
			SetFileType.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					NewFileType = filetype.getItem(filetype.getSelectionIndex());
					FileTypeHasChanged = true;
					close();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
		}
		

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

	shell.pack();}

	/**
	 * Render the correct page for the file.
	 */
	private void RenderAppropriatePage() {
		try {
			TrdDirectoryEntry trde = (TrdDirectoryEntry) ThisEntry;

			char ftype = trde.GetFileType();
			if (ftype == 'B') {
				BasicRenderer CurrentRenderer = new BasicRenderer();
				CurrentRenderer.RenderBasic(MainPage, data, null, ThisEntry.GetFilename(), ThisEntry.GetFileSize(),
						trde.GetVar2(), trde.startline, filesel, new BasicSave());
			} else if (ftype != 'D') {
				CodeRenderer CurrentRenderer = new CodeRenderer();
				CurrentRenderer.RenderCode(MainPage, data, null, ThisEntry.GetFilename(), data.length, trde.GetVar1(),
						filesel, CurrentPartition, new CodeSave());
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

	/**
	 * Save for BASIC files. Only the LOAD address is save-able.
	 */
	private class BasicSave implements GenericSaveEvent {
		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			TrdDirectoryEntry trde = (TrdDirectoryEntry) ThisEntry;
			SpeccyBasicDetails sbd = trde.GetSpeccyBasicDetails();
			if (valtype == 0) {
				System.out.print("Start Line: " + sbd.LineStart + " -> ");
				sbd.LineStart = Value;
				System.out.println(sbd.LineStart);
			} else {
				System.out.print("Vars Offset: " + sbd.VarStart + " -> ");
				sbd.VarStart = Value;
				System.out.println(sbd.VarStart);
			}

			trde.SetSpeccyBasicDetails(sbd, (TrDosPartition) CurrentPartition);

			return true;
		}
	}

	/**
	 * Save for SAVE files. Start line(0) and Vars(1) offset are save-able.
	 */
	private class CodeSave implements GenericSaveEvent {
		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			TrdDirectoryEntry trde = (TrdDirectoryEntry) ThisEntry;
			SpeccyBasicDetails sbd = trde.GetSpeccyBasicDetails();
			System.out.print("Code start: " + sbd.LoadAddress + " -> ");
			sbd.LoadAddress = Value;
			System.out.println(sbd.LoadAddress);

			trde.SetSpeccyBasicDetails(sbd, (TrDosPartition) CurrentPartition);
			return true;
		}
	}

}

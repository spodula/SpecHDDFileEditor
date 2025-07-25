package hddEditor.ui.partitionPages.dialogs.edit;

/**
 * Implementation of a File/edit page for +3DOS files.
 * 
 */
import java.io.IOException;

/**
 * FIle edit dialog for the +3DOS partition page
 */

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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Speccy;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.PLUS3DOSPartition;
import hddEditor.libs.partitions.cpm.CPMDirectoryEntry;
import hddEditor.libs.partitions.cpm.Dirent;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;
import hddEditor.ui.partitionPages.dialogs.edit.callbacks.GenericSaveEvent;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;

public class Plus3DosFileEditDialog extends EditFileDialog {
	public boolean FileTypeHasChanged;
	public int NewFileType;

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public Plus3DosFileEditDialog(Display display, FileSelectDialog filesel, IDEDosPartition CurrentPartition) {
		super(display, filesel, CurrentPartition);
		FileTypeHasChanged = false;
	}

	/**
	 * Create the form
	 */
	@Override
	protected void Createform() {
		shell = new Shell(display);
		shell.setSize(970, 810);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.makeColumnsEqualWidth = true;
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;
		gridLayout.marginBottom = 20;
		shell.setLayout(gridLayout);

		Label lbl = new Label(shell, SWT.NONE);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setText(String.format("CPM Length: %d bytes (%X)", data.length, data.length));
		lbl.setFont(boldFont);
		lbl = new Label(shell, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		String logblocks = "";
		int BlockCount = 0;
		for (Dirent dirent : ((CPMDirectoryEntry) ThisEntry).dirents) {
			for (int blockNum : dirent.getBlocks()) {
				logblocks = logblocks + ", " + blockNum;
				BlockCount++;
			}
		}
		if (logblocks.length() > 2) {
			logblocks = logblocks.substring(2);
		}
		lbl.setText("Logical blocks: " + BlockCount + " (" + logblocks + ")");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);

		Plus3DosFileHeader p3d = new Plus3DosFileHeader(data);
		lbl = new Label(shell, SWT.NONE);
		lbl.setFont(boldFont);

		if (p3d.IsPlus3DosFile()) {
			lbl.setText(
					String.format("+3DOS Length: %d bytes (%X)", p3d.GetBasicFileLength(), p3d.GetBasicFileLength()));
		} else {
			lbl.setText("Not a +3DOS file (Or header corrupt)");
		}
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		String filetypes[] = new String[Speccy.filetypeNames.length + 1];
		System.arraycopy(Speccy.filetypeNames, 0, filetypes, 0, Speccy.filetypeNames.length);
		filetypes[Speccy.filetypeNames.length] = "Raw CPM File";

		// Only display file type change for valid +3DOS files
		// to avoid hilarious consequences for changing Raw CPM files.
		if (p3d.IsPlus3DosFile()) {
			Combo filetype = new Combo(shell, SWT.NONE);
			filetype.setItems(filetypes);
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

			filetype.select(p3d.GetFileType());

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
	}

	/**
	 * Render the correct page for the file.
	 */
	private void RenderAppropriatePage() {
		Plus3DosFileHeader p3d = ((CPMDirectoryEntry) ThisEntry).GetPlus3DosHeader();

		byte header[] = null;
		byte newdata[] = data;
		if (p3d.IsPlus3DosFile()) {
			// Separate the header and the file data.
			header = new byte[0x80];
			System.arraycopy(data, 0, header, 0, 0x80);
			newdata = new byte[p3d.GetBasicFileLength()];
			if (p3d.GetBasicFileLength() > data.length - 0x80) {
				System.arraycopy(data, 0x80, newdata, 0, data.length - 0x80);
			} else {
				System.arraycopy(data, 0x80, newdata, 0, newdata.length);
			}
		}

		/**
		 * Render the page.
		 */
		if (!p3d.IsPlus3DosFile()) {
			CodeRenderer CR = new CodeRenderer();
			CR.RenderCode(MainPage, newdata, null, ThisEntry.GetFilename(), newdata.length, 0x0000, filesel,
					CurrentPartition, null);
		} else if (p3d.GetFileType() == Speccy.BASIC_BASIC) {
			BasicRenderer BR = new BasicRenderer();
			BR.RenderBasic(MainPage, newdata, header, ThisEntry.GetFilename(), p3d.GetBasicFileLength(),
					p3d.GetVarsOffset(), p3d.GetLine(), filesel, new BasicSave());
		} else if (p3d.GetFileType() == Speccy.BASIC_CODE) {
			CodeRenderer CR = new CodeRenderer();
			CR.RenderCode(MainPage, newdata, header, ThisEntry.GetFilename(), newdata.length, p3d.GetLoadAddress(),
					filesel, CurrentPartition, new CodeSave());
		} else if (p3d.GetFileType() == Speccy.BASIC_NUMARRAY) {
			NumericArrayRenderer NR = new NumericArrayRenderer();
			NR.RenderNumericArray(MainPage, newdata, header, ThisEntry.GetFilename(), p3d.GetVarName(), filesel,
					new ArraySave());
		} else { // Char array
			CharArrayRenderer CR = new CharArrayRenderer();
			CR.RenderCharArray(MainPage, newdata, header, ThisEntry.GetFilename(), p3d.GetVarName(), filesel,
					new ArraySave());
		}
	}

	/**
	 * Save for CODE files. Only the LOAD address is save-able.
	 */
	private class CodeSave implements GenericSaveEvent {
		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			CPMDirectoryEntry direntry = (CPMDirectoryEntry) ThisEntry;
			Plus3DosFileHeader p3d = direntry.GetPlus3DosHeader();
			if (p3d != null && p3d.IsPlus3DosFile()) {
				System.out.print("Load address: " + p3d.GetLoadAddress() + " -> ");
				p3d.SetLoadAddress(Value);
				System.out.println(p3d.GetLoadAddress());
				PLUS3DOSPartition p3dPart = (PLUS3DOSPartition) CurrentPartition;
				try {
					direntry.SetDeleted(true);
					byte rawdata[] = direntry.GetFileRawData();
					System.arraycopy(p3d.RawHeader, 0, rawdata, 0, 0x80);
					p3dPart.AddCPMFile(direntry.GetFilename(), rawdata);
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("Update ignored, No +3DOS Basic header to update.");
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
			CPMDirectoryEntry direntry = (CPMDirectoryEntry) ThisEntry;
			Plus3DosFileHeader p3d = direntry.GetPlus3DosHeader();
			if (p3d != null && p3d.IsPlus3DosFile()) {
				if (valtype == 0) {
					System.out.print("Start Line: " + p3d.GetLine() + " -> ");
					p3d.SetLine(Value);
					System.out.println(p3d.GetLine());
				} else {
					System.out.print("Vars Offset: " + p3d.GetVarsOffset() + " -> ");
					p3d.SetVarsOffset(Value);
					System.out.println(p3d.GetVarsOffset());
				}
				PLUS3DOSPartition p3dPart = (PLUS3DOSPartition) CurrentPartition;
				try {
					direntry.SetDeleted(true);
					byte rawdata[] = direntry.GetFileRawData();
					System.arraycopy(p3d.RawHeader, 0, rawdata, 0, 0x80);
					p3dPart.AddCPMFile(direntry.GetFilename(), rawdata);
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("Update ignored, No +3DOS Basic header to update.");
			}
			return false;
		}
	}

	/**
	 * Save for BASIC files. Start line(0) and Vars(1) offset are save-able.
	 */
	private class ArraySave implements GenericSaveEvent {
		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			CPMDirectoryEntry direntry = (CPMDirectoryEntry) ThisEntry;
			Plus3DosFileHeader p3d = direntry.GetPlus3DosHeader();
			if (p3d != null && p3d.IsPlus3DosFile()) {
				System.out.print("Array name: " + p3d.GetVarName() + " -> ");
				p3d.SetVarName(sValue);
				System.out.println(p3d.GetVarName());
				PLUS3DOSPartition p3dPart = (PLUS3DOSPartition) CurrentPartition;
				try {
					direntry.SetDeleted(true);
					byte rawdata[] = direntry.GetFileRawData();
					System.arraycopy(p3d.RawHeader, 0, rawdata, 0, 0x80);
					p3dPart.AddCPMFile(direntry.GetFilename(), rawdata);
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("Update ignored, No +3DOS Basic header to update.");
			}
			return false;
		}
	}
}

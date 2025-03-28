package hddEditor.ui.partitionPages.dialogs.edit;

import java.io.IOException;

/**
 * Implementation of the Edit file page for an TAP file.
 * 
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
import hddEditor.libs.disks.LINEAR.TAPFile;
import hddEditor.libs.disks.LINEAR.tapblocks.TAPBlock;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.TAPPartition;

import hddEditor.libs.partitions.tap.TapDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;
import hddEditor.ui.partitionPages.dialogs.edit.callbacks.GenericSaveEvent;

public class TapFileEditDialog extends EditFileDialog {
	public TapFileEditDialog(Display display, FileSelectDialog filesel, IDEDosPartition CurrentPartition) {
		super(display, filesel, CurrentPartition);
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
		TapDirectoryEntry tde = (TapDirectoryEntry) ThisEntry;
		SpeccyBasicDetails sbd = tde.GetSpeccyBasicDetails();
		switch (sbd.BasicType) {
		case Speccy.BASIC_BASIC:
			BasicRenderer BR = new BasicRenderer();
			BR.RenderBasic(MainPage, data, null, tde.GetFilename(), data.length, sbd.VarStart, sbd.LineStart, filesel,
					new TapBasicSave());
			break;
		case Speccy.BASIC_CODE:
			CR = new CodeRenderer();
			CR.RenderCode(MainPage, data, null, tde.GetFilename(), data.length, sbd.LoadAddress, filesel,
					CurrentPartition, new TapCodeSave());
			break;
		case Speccy.BASIC_NUMARRAY:
			NumericArrayRenderer NR = new NumericArrayRenderer();
			NR.RenderNumericArray(MainPage, data, null, tde.GetFilename(), sbd.VarName + "", filesel,
					new TapArraySave());
			break;
		case Speccy.BASIC_CHRARRAY:
			CharArrayRenderer CAR = new CharArrayRenderer();
			CAR.RenderCharArray(MainPage, data, null, tde.GetFilename(), sbd.VarName + "", filesel, new TapArraySave());
		default:
			CR = new CodeRenderer();
			CR.RenderCode(MainPage, data, null, tde.GetFilename(), data.length, 0x0000, filesel, CurrentPartition,
					null);
		}
	}

	/**
	 * Save for CODE files. Only the LOAD address is save-able.
	 */
	private class TapCodeSave implements GenericSaveEvent {
		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			TapDirectoryEntry direntry = (TapDirectoryEntry) ThisEntry;
			TAPBlock header = direntry.HeaderBlock;
			if (header != null) {
				SpeccyBasicDetails sbd = direntry.GetSpeccyBasicDetails();
				System.out.print("Load address: " + sbd.LoadAddress + " -> ");
				sbd.LoadAddress = Value;

				TAPPartition TapPart = (TAPPartition) CurrentPartition;
				TAPFile tapfile = (TAPFile) TapPart.CurrentDisk;
				try {
					header.SetHeader(sbd);
					tapfile.RewriteFile();
					TapPart.LoadPartitionSpecificInformation();
					System.out.println(direntry.GetSpeccyBasicDetails().LoadAddress);
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("Update ignored, No Basic header to update.");
			}
			return false;
		}
	}

	/**
	 * Save for BASIC files. Start line(0) and Vars(1) offset are save-able.
	 */
	private class TapBasicSave implements GenericSaveEvent {
		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			TapDirectoryEntry direntry = (TapDirectoryEntry) ThisEntry;
			TAPBlock header = direntry.HeaderBlock;
			if (header != null) {
				SpeccyBasicDetails sbd = direntry.GetSpeccyBasicDetails();
				if (valtype == 0) {
					System.out.print("Start Line: " + sbd.LineStart + " -> ");
					sbd.LineStart = Value;
					System.out.println(sbd.LineStart);
				} else {
					System.out.print("Vars Offset: " + sbd.VarStart + " -> ");
					sbd.VarStart = Value;
					System.out.println(sbd.VarStart);
				}

				TAPPartition TapPart = (TAPPartition) CurrentPartition;
				TAPFile tapfile = (TAPFile) TapPart.CurrentDisk;
				try {
					header.SetHeader(sbd);
					tapfile.RewriteFile();
					TapPart.LoadPartitionSpecificInformation();
					System.out.println(direntry.GetSpeccyBasicDetails().LoadAddress);
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("Update ignored, No Basic header to update.");
			}
			return false;
		}
	}

	/**
	 * Save for BASIC files. Start line(0) and Vars(1) offset are save-able.
	 */
	private class TapArraySave implements GenericSaveEvent {
		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			TapDirectoryEntry direntry = (TapDirectoryEntry) ThisEntry;
			TAPBlock header = direntry.HeaderBlock;
			if (header != null) {
				SpeccyBasicDetails sbd = direntry.GetSpeccyBasicDetails();
				System.out.print("Array name: " + sbd.VarName + " -> ");
				sbd.VarName = (sValue + "A").charAt(0);
				header.SetHeader(sbd);
				System.out.println(direntry.GetSpeccyBasicDetails().VarName);

				TAPPartition TapPart = (TAPPartition) CurrentPartition;
				TAPFile tapfile = (TAPFile) TapPart.CurrentDisk;
				try {
					tapfile.RewriteFile();
					TapPart.LoadPartitionSpecificInformation();
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("Update ignored, No Basic header to update.");
			}
			return false;
		}
	}

}

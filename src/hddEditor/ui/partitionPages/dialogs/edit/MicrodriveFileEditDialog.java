package hddEditor.ui.partitionPages.dialogs.edit;

/**
 * Implementation of the file edit page for a Microdrive file.
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
import hddEditor.libs.Languages;
import hddEditor.libs.Speccy;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.disks.LINEAR.MDFMicrodriveFile;
import hddEditor.libs.disks.LINEAR.MicrodriveSector;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.mdf.MicrodriveDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;
import hddEditor.ui.partitionPages.FileRenderers.FileRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;
import hddEditor.ui.partitionPages.dialogs.edit.callbacks.GenericSaveEvent;

public class MicrodriveFileEditDialog extends EditFileDialog {
	public int NewFileType;
	public boolean FileTypeHasChanged;

	public MicrodriveFileEditDialog(Display display, FileSelectDialog filesel, IDEDosPartition CurrentPartition, Languages lang) {
		super(display, filesel, CurrentPartition, lang);
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

		MicrodriveDirectoryEntry mde = (MicrodriveDirectoryEntry) ThisEntry;

		Label lbl = label(ThisEntry.GetSpeccyBasicDetails().BasicTypeString() + " "+lang.Msg(Languages.MSG_FILE), 4);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setFont(boldFont);

		label(String.format(lang.Msg(Languages.MSG_LENGTHWHEADER), ThisEntry.GetRawFileSize(), ThisEntry.GetRawFileSize()),
				2);
		label(String.format(lang.Msg(Languages.MSG_LENGTHWOHEADER), ThisEntry.GetFileSize(), ThisEntry.GetFileSize()),
				2);

		String logblocks = "";
		for (MicrodriveSector mds : mde.sectors) {
			logblocks = logblocks + ", " + mds.GetSectorNumber();
		}
		if (logblocks.length() > 2) {
			logblocks = logblocks.substring(2);
		}

		label(String.format(lang.Msg(Languages.MSG_USEDSECTORS),mde.sectors.length, logblocks),1);

		// Only display file type change for Tap files with headers.

		MicrodriveDirectoryEntry tde = (MicrodriveDirectoryEntry) ThisEntry;
		SpeccyBasicDetails sbd = tde.GetSpeccyBasicDetails();
		if (sbd.IsValidFileType()) {
			Combo filetype = new Combo(shell, SWT.NONE);
			filetype.setItems(Speccy.filetypeNames);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 1;
			filetype.setLayoutData(gd);

			Button SetFileType = new Button(shell, SWT.NONE);
			SetFileType.setText(lang.Msg(Languages.MSG_UPDATEFILETYPE));
			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 1;
			SetFileType.setLayoutData(gd);

			lbl = new Label(shell, SWT.NONE);
			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 2;
			lbl.setLayoutData(gd);

			filetype.select(sbd.BasicType);

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
		MDFMicrodriveFile mdf = (MDFMicrodriveFile) CurrentPartition.CurrentDisk;

		SpeccyBasicDetails sbd = ThisEntry.GetSpeccyBasicDetails();
		switch (sbd.BasicType) {
		case Speccy.BASIC_BASIC:
			BasicRenderer BR = new BasicRenderer();
			BR.RenderBasic(MainPage, data, null, ThisEntry.GetFilename(), data.length, sbd.VarStart, sbd.LineStart,
					filesel, new MDRBasicSave(mdf), lang);
			break;
		case Speccy.BASIC_CODE:
			CodeRenderer CR = new CodeRenderer();
			CR.RenderCode(MainPage, data, null, ThisEntry.GetFilename(), data.length,
					((MicrodriveDirectoryEntry) ThisEntry).GetVar2(), filesel, CurrentPartition, new MDRCodeSave(mdf), lang);
			break;
		case Speccy.BASIC_NUMARRAY:
			NumericArrayRenderer NR = new NumericArrayRenderer();
			NR.RenderNumericArray(MainPage, data, null, ThisEntry.GetFilename(), sbd.VarName + "", filesel,
					new MDRArraySave(mdf), lang);
			break;
		case Speccy.BASIC_CHRARRAY:
			CharArrayRenderer CAR = new CharArrayRenderer();
			CAR.RenderCharArray(MainPage, data, null, ThisEntry.GetFilename(), sbd.VarName + "", filesel,
					new MDRArraySave(mdf), lang);
		default:
			FileRenderer FR = new FileRenderer();
			FR.Render(MainPage, data, ThisEntry.GetFilename(), filesel, lang);
		}
	}

	/**
	 * Unlike most classes, the MDF needs to be supplied seperately. Should probably
	 * fix this at some stage for consistancy. but for now....
	 */
	private class MDRSpecial implements GenericSaveEvent {
		public MDFMicrodriveFile file;

		public MDRSpecial(MDFMicrodriveFile mdf) {
			this.file = mdf;
		}

		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			return true;
		}

	}

	/**
	 * Save for CODE files. Only the LOAD address is save-able.
	 */
	private class MDRCodeSave extends MDRSpecial {
		public MDRCodeSave(MDFMicrodriveFile mdf) {
			super(mdf);
		}

		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			MicrodriveDirectoryEntry direntry = (MicrodriveDirectoryEntry) ThisEntry;
			SpeccyBasicDetails sbd = direntry.GetSpeccyBasicDetails();
			System.out.print(lang.Msg(Languages.MSG_CODELOADADD)+ ": " + sbd.LoadAddress + " -> ");
			sbd.LoadAddress = Value;
			try {
				direntry.SetHeader(sbd, file);
				System.out.println(direntry.GetSpeccyBasicDetails().LoadAddress);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	/**
	 * Save for BASIC files. Start line(0) and Vars(1) offset are save-able.
	 */
	private class MDRBasicSave extends MDRSpecial {
		public MDRBasicSave(MDFMicrodriveFile mdf) {
			super(mdf);
		}

		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			MicrodriveDirectoryEntry direntry = (MicrodriveDirectoryEntry) ThisEntry;
			SpeccyBasicDetails sbd = direntry.GetSpeccyBasicDetails();
			if (valtype == 0) {
				sbd.LineStart = Value;
			} else {
				sbd.VarStart = Value;
			}
			try {
				direntry.SetHeader(sbd, file);
				System.out.println(direntry.GetSpeccyBasicDetails().LoadAddress);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	/**
	 * Save for BASIC Array files. Array name is saveable
	 */
	private class MDRArraySave extends MDRSpecial {
		public MDRArraySave(MDFMicrodriveFile mdf) {
			super(mdf);
		}

		@Override
		public boolean DoSave(int valtype, String sValue, int Value) {
			MicrodriveDirectoryEntry direntry = (MicrodriveDirectoryEntry) ThisEntry;
			SpeccyBasicDetails sbd = direntry.GetSpeccyBasicDetails();
			System.out.print(lang.Msg(Languages.MSG_ARRAYNAME) +  ": " + sbd.VarName + " -> ");
			sbd.VarName = (sValue + "A").charAt(0);

			try {
				direntry.SetHeader(sbd, file);
				System.out.println(direntry.GetSpeccyBasicDetails().VarName);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

}

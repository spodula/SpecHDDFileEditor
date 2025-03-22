package hddEditor.ui.partitionPages.dialogs.edit;
/**
 * Implementation of the Edit file(block?) page for a TZX file.
 *
 * TODO: Saves for TZX BASIC/Start line  BASIC/VARSTART
 * TODO: Saves for TZX Variable name for Variables.
 * TODO: Saves for TZX Code load address
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
import hddEditor.libs.TZX;
import hddEditor.libs.disks.SpeccyBasicDetails;
import hddEditor.libs.disks.LINEAR.tzxblocks.ArchiveInfoBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.CallSequenceBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.CustomInfoBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.ArchiveInfoBlock.TextEntry;
import hddEditor.libs.disks.LINEAR.tzxblocks.GlueBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.HardwareInfoBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.HardwareInfoBlock.HardwareInfoEntry;
import hddEditor.libs.disks.LINEAR.tzxblocks.JumpToBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.LoopStartBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.MessageBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.PauseStopTape;
import hddEditor.libs.disks.LINEAR.tzxblocks.PulseSequence;
import hddEditor.libs.disks.LINEAR.tzxblocks.PureToneBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.SelectBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.SetSignalLevelBlock;
import hddEditor.libs.disks.LINEAR.tzxblocks.SnapshotBlock;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.tzx.TzxDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.BasicRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CharArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.CodeRenderer;
import hddEditor.ui.partitionPages.FileRenderers.NumericArrayRenderer;
import hddEditor.ui.partitionPages.FileRenderers.StaticTextsBlockRender;
import hddEditor.ui.partitionPages.FileRenderers.TextDescRenderer;

public class TzxFileEditDialog extends EditFileDialog {
	public TzxFileEditDialog(Display display,FileSelectDialog filesel,IDEDosPartition CurrentPartition) {
		super(display, filesel, CurrentPartition);
	}

	/**
	 * 
	 * @return
	 */
	private String GetFileType() {
		TzxDirectoryEntry te = (TzxDirectoryEntry)ThisEntry;
		if ((te.GetTZXFileType() != TZX.TZX_STANDARDSPEED_DATABLOCK)
				&& (te.GetTZXFileType() != TZX.TZX_TURBOSPEED_DATABLOCK)) {
			return (TZX.GetDataBlockTypeForID(te.GetTZXFileType()) + " Block");
		}

		return (ThisEntry.GetSpeccyBasicDetails().BasicTypeString() + " File");
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

		Label lbl = label(GetFileType(), 4);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setFont(boldFont);

		if (ThisEntry.GetRawFileSize() == 0) {
			label(String.format("Length : No data"), 2);
		} else {
			label(String.format("Length : %d bytes (%X)", ThisEntry.GetRawFileSize(), ThisEntry.GetRawFileSize()), 2);
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

	/**
	 * 
	 */
	private void RenderAppropriatePage() {
		TzxDirectoryEntry te = (TzxDirectoryEntry)ThisEntry;
		/**
		 * Render the page.
		 */
		CodeRenderer CR;
		int TzxFileType = te.GetTZXFileType();
		if (TzxFileType == TZX.TZX_GROUPEND || TzxFileType == TZX.TZX_LOOPEND || TzxFileType == TZX.TZX_RETSEQ
				|| TzxFileType == TZX.TZX_STOP48) {
			// Cant render this, so just exit.
		} else if ((TzxFileType == TZX.TZX_TEXTDESC) || (TzxFileType == TZX.TZX_GROUPSTART)) {
			TextDescRenderer tdr = new TextDescRenderer();
			tdr.RenderText(MainPage, te.GetTZXBlockData(), null, te.GetTZXBlockString(),filesel);
		} else if ((TzxFileType == TZX.TZX_PURETONE)) {
			PureToneBlock ptb = (PureToneBlock) te.DataBlock;
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();
			sdr.RenderTexts(MainPage, new String[] { "Pulse length", "Pulse count" },
					new String[] { String.valueOf(ptb.PulseLen) + " Tstates", String.valueOf(ptb.Pulses) });
		} else if ((TzxFileType == TZX.TZX_LOOPSTART)) {
			LoopStartBlock lsb = (LoopStartBlock) te.DataBlock;
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();
			sdr.RenderTexts(MainPage, new String[] { "Repetitions" }, new String[] { String.valueOf(lsb.Repeat) });
		} else if ((TzxFileType == TZX.TZX_JUMP)) {
			JumpToBlock jtb = (JumpToBlock) te.DataBlock;
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();
			sdr.RenderTexts(MainPage, new String[] { "Relative jump", "Actual target" },
					new String[] { String.valueOf(jtb.Disp), String.valueOf(jtb.Disp + jtb.BlockNumber) });
		} else if ((TzxFileType == TZX.TZX_MESSAGEBLOCK)) {
			MessageBlock mb = (MessageBlock) te.DataBlock;
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();
			sdr.RenderTexts(MainPage, new String[] { "Time delay", "Message"},
					new String[] { String.valueOf(mb.time), mb.BlockNotes });		
		} else if ((TzxFileType == TZX.TZX_GLUE)) {
			GlueBlock gb = (GlueBlock) te.DataBlock;
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();
			sdr.RenderTexts(MainPage, new String[] { "XTAPE", "Major", "Minor" },
					new String[] { gb.XTAPE, String.valueOf(gb.Major), String.valueOf(gb.Minor) });
		} else if ((TzxFileType == TZX.TZX_PAUSE)) {
			PauseStopTape pst = (PauseStopTape) te.DataBlock;
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();
			String s = "Stop tape";
			if (pst.PauseDuration != 0) {
				s = pst.PauseDuration + "ms";
			}
			sdr.RenderTexts(MainPage, new String[] { "Pause" }, new String[] { s });
		} else if ((TzxFileType == TZX.TZX_SETSIGNALLEVEL)) {
			SetSignalLevelBlock ssl = (SetSignalLevelBlock) te.DataBlock;
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();
			String level = "High (1)";
			if (ssl.SignalLevel == 0) {
				level = "Low (0)";
			}
			sdr.RenderTexts(MainPage, new String[] { "Signal level" }, new String[] { level });
		} else if ((TzxFileType == TZX.TZX_CUSTOMINFO)) {
			CustomInfoBlock cib = (CustomInfoBlock) te.DataBlock;
			CR = new CodeRenderer();
			CR.RenderCode(MainPage, cib.data, null, cib.ID, cib.data.length, 0, filesel,CurrentPartition,null);
		} else if ((TzxFileType == TZX.TZX_SNAPSHOT)) {
			SnapshotBlock sb = (SnapshotBlock) te.DataBlock;
			CR = new CodeRenderer();
			String filename = "Block"+sb.BlockNumber+" File of type: ";
			if(sb.SnapShotType==1) {
				filename = filename +".SNA";
			} else {
				filename = filename +".Z80";
			}
			CR.RenderCode(MainPage, sb.data, null, filename, sb.data.length, 0, filesel,CurrentPartition,null);
		} else if ((TzxFileType == TZX.TZX_ARCHIVEINFO)) {
			ArchiveInfoBlock aab = (ArchiveInfoBlock) te.DataBlock;
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();
			String labels[] = new String[aab.list.length];
			String content[] = new String[aab.list.length];

			int ptr = 0;
			for (TextEntry t : aab.list) {
				labels[ptr] = t.IdByteAsString();
				content[ptr] = t.text;
				ptr++;
			}
			sdr.RenderTexts(MainPage, labels, content);
		} else if ((TzxFileType == TZX.TZX_CALLSEQ)) {
			CallSequenceBlock csb = (CallSequenceBlock) te.DataBlock;
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();
			String labels[] = new String[csb.Blocks.length];
			String content[] = new String[csb.Blocks.length];

			int ptr = 0;
			for (int t : csb.Blocks) {
				labels[ptr] = "Call #" + ptr;
				content[ptr] = String.valueOf(t);
				ptr++;
			}
			sdr.RenderTexts(MainPage, labels, content);

		} else if ((TzxFileType == TZX.TZX_HARDWARETYPE)) {
			HardwareInfoBlock hwb = (HardwareInfoBlock) te.DataBlock;
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();

			String labels[] = new String[hwb.Entries.length];
			String content[] = new String[hwb.Entries.length];

			int ptr = 0;
			for (HardwareInfoEntry t : hwb.Entries) {
				labels[ptr] = t.getHardwareType() + " - " + t.GetHardwareInfo();
				content[ptr] = t.GetHWInfo();
				ptr++;
			}
			sdr.RenderTexts(MainPage, labels, content);
		} else if ((TzxFileType == TZX.TZX_PULSESEQ)) {
			PulseSequence psb = (PulseSequence) te.DataBlock;
			String labels[] = new String[psb.Pulses];
			String data[] = new String[psb.Pulses];
			for (int cnt = 0; cnt < psb.Pulses; cnt++) {
				labels[cnt] = "Pulse number " + cnt;
				data[cnt] = psb.PulseLen[cnt] + " T states";
			}
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();
			sdr.RenderTexts(MainPage, labels, data);
		} else if ((TzxFileType == TZX.TZX_SELECTBLOCK)) {
			SelectBlock sb = (SelectBlock) te.DataBlock;
			String labels[] = new String[sb.Entries.length];
			String data[] = new String[sb.Entries.length];
			for (int cnt = 0; cnt < sb.Entries.length; cnt++) {
				labels[cnt] = "Offset " + sb.Entries[cnt].relOffset;
				data[cnt] = sb.Entries[cnt].text;
			}
			StaticTextsBlockRender sdr = new StaticTextsBlockRender();
			sdr.RenderTexts(MainPage, labels, data);

		} else {
			SpeccyBasicDetails sbd = ThisEntry.GetSpeccyBasicDetails();
			switch (sbd.BasicType) {
			case Speccy.BASIC_BASIC:
				BasicRenderer BR = new BasicRenderer();
				BR.RenderBasic(MainPage, data, null, ThisEntry.GetFilename(), data.length, sbd.VarStart, sbd.LineStart, filesel);
				break;
			case Speccy.BASIC_CODE:
				//TODO: implement TZXFileEdit.saveevent for CODE

				CR = new CodeRenderer();
				CR.RenderCode(MainPage, data, null, ThisEntry.GetFilename(), data.length, sbd.LoadAddress, filesel,CurrentPartition,null);
				break;
			case Speccy.BASIC_NUMARRAY:
				NumericArrayRenderer NR = new NumericArrayRenderer();
				NR.RenderNumericArray(MainPage, data, null, ThisEntry.GetFilename(), "A", filesel);
				break;
			case Speccy.BASIC_CHRARRAY:
				CharArrayRenderer CAR = new CharArrayRenderer();
				CAR.RenderCharArray(MainPage, data, null, ThisEntry.GetFilename(), "A", filesel);
			default:
				CR = new CodeRenderer();
				CR.RenderCode(MainPage, data, null, ThisEntry.GetFilename(), data.length, 0x0000, filesel,CurrentPartition,null);
			}
		}
	}

}

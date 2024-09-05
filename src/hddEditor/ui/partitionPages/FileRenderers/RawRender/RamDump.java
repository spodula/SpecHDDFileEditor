package hddEditor.ui.partitionPages.FileRenderers.RawRender;

/**
 * This object implements displaying of a dump of memory. from 16384 to 65535.
 * It will try to decode any BASIC if found. 
 * 
 * It is supposed to be used for the various snapshot formats, (SNA, MGT, Z80) which should
 * inherit from this and use the RENDER method once the memory is setup.
 * 
 * It has been expanded to 128K.  If this is set, there should be a list of pages provided so it knows
 * what order things are provided in. For best effect, the first 32k should be pages 5,2
 */

import java.util.ArrayList;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.graphics.ImageLoader;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.Speccy;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.snapshots.CPUStateToFiles;
import hddEditor.libs.snapshots.MachineState;

import org.eclipse.swt.widgets.Button;

public class RamDump implements Renderer {
	private ArrayList<Label> labels = null;
	private ArrayList<Renderer> Renderers = null;
	private Button ExtractBtn = null;
	private Button AddSnapshotBtn = null;
	private Composite Targetpg = null;
	private byte rawdata[] = null;
	private String fName = null;
	private int IYReg = 0;
	private int i128BankOrder[];

	private boolean HasBasic;
	private BasicRenderer BR;
	private SystemVariablesRenderer SVR;

	private byte BasicData[];
	private int VarsOffset;

	@Override
	public void DisposeRenderer() {
		if (labels != null) {
			for (Label l : labels) {
				l.dispose();
			}
			labels.clear();
			labels = null;
		}
		if (Renderers != null) {
			for (Renderer r : Renderers) {
				r.DisposeRenderer();
			}
			Renderers.clear();
			Renderers = null;
		}
		if (ExtractBtn != null) {
			ExtractBtn.dispose();
			ExtractBtn = null;
		}
	}

	/**
	 * 
	 * @param TargetPage
	 * @param data
	 * @param loadAddr
	 * @param is128K
	 * @param IY
	 * @param i128BankOrder
	 * @param filename
	 * @param cpustate
	 * @param targetpartition
	 */
	public void Render(Composite TargetPage, byte[] data, int loadAddr, boolean is128K, int IY, int i128BankOrder[],
			String filename, MachineState cpustate, IDEDosPartition targetpartition) {
		labels = new ArrayList<Label>();
		Renderers = new ArrayList<Renderer>();
		Targetpg = TargetPage;
		rawdata = data;
		fName = filename;
		IYReg = IY;
		this.i128BankOrder = i128BankOrder;

		byte screen[] = new byte[0x1b00];
		for (int i = 0; i < 0x1800; i++)
			screen[i] = 0x00;
		for (int i = 0x1800; i < 0x1B00; i++)
			screen[i] = 0b00111000;
		System.arraycopy(data, 0x00, screen, 0, Math.min(0x1b00, data.length));

		ScreenRenderer ScrRenderer = new ScreenRenderer();
		Renderers.add(ScrRenderer);
		ScrRenderer.Render(TargetPage, screen);

		ExtractBtn = new Button(TargetPage, SWT.NONE);
		ExtractBtn.setText("Extract to disk");
		ExtractBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (is128K) {
					doExtractFiles128();
				} else {
					doExtractFiles();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		if (!is128K && (cpustate != null)) {
			AddSnapshotBtn = new Button(TargetPage, SWT.NONE);
			AddSnapshotBtn.setText("Convert Snapshot to files");
			AddSnapshotBtn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					doConvertSnaToLoadableFiles(cpustate, targetpartition, filename);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
		}

		Label lbl = new Label(TargetPage, SWT.NONE);
		labels.add(lbl);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(lbl.getShell().getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));

		// check for BASIC.
		int diff = 0x4000;

		int PROG = (data[0x5c54 - diff] & 0xff) * 256 + (data[0x5c53 - diff] & 0xff);
		int VARS = (data[0x5c4c - diff] & 0xff) * 256 + (data[0x5c4b - diff] & 0xff);
		int E_LINE = (data[0x5c5a - diff] & 0xff) * 256 + (data[0x5c59 - diff] & 0xff);

		// some basic checking.
		HasBasic = false;
		BasicData = null;
		VarsOffset = 0;
		if ((VARS > PROG) && (E_LINE > VARS) && (PROG > 23754) && (PROG < 25000)) {
			BasicData = new byte[E_LINE - PROG];
			VarsOffset = VARS - PROG;
			System.arraycopy(data, PROG - diff, BasicData, 0, Math.min(BasicData.length, data.length - (PROG - diff)));
			BR = new BasicRenderer();
			Renderers.add(BR);
			BR.AddBasicFile(TargetPage, BasicData, BasicData.length, VarsOffset, false);

			byte SysVars[] = new byte[512];
			System.arraycopy(data, 0x5b00 - diff, SysVars, 0, SysVars.length);
			SVR = new SystemVariablesRenderer();
			Renderers.add(SVR);
			SVR.AddSysVars(TargetPage, SysVars, false, false);
			HasBasic = true;
		}

		if (is128K) {
			lbl.setText("Paged in memory (5B00-7FFF)=bank 5, (8000-BFFF)=bank 2, (C000-FFFF)=bank " + i128BankOrder[2]);
		} else {
			lbl.setText("48K memory dump (4000-FFFF)");
		}
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setFont(boldFont);
		lbl.setLayoutData(gd);

		if (is128K) {
			int pagedram = i128BankOrder[2];
			int ptr = 0;
			for (int i = 0; i < i128BankOrder.length; i++) {
				int rambank = i128BankOrder[i];
				if (rambank == 99)
					rambank = pagedram;
				int startAddress = 0xc000;
				if (i == 0)
					startAddress = 0x4000;
				if (i == 1)
					startAddress = 0x8000;

				lbl = new Label(TargetPage, SWT.NONE);
				labels.add(lbl);
				lbl.setText("Ram bank " + rambank + " (" + Integer.toHexString(startAddress) + "-"
						+ Integer.toHexString(startAddress + 0x3fff) + ")");
				gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				gd.horizontalSpan = 4;
				lbl.setFont(boldFont);
				lbl.setLayoutData(gd);

				byte memdata[] = new byte[0x4000];
				int actualdatalength = Math.min(0x4000, data.length - ptr);
				System.arraycopy(data, ptr, memdata, 0, actualdatalength);
				ptr = ptr + actualdatalength;
				BinaryRenderer BinRenderer = new BinaryRenderer();
				Renderers.add(BinRenderer);
				BinRenderer.Render(TargetPage, memdata, startAddress, 200);
			}
		} else {
			byte memdata[] = new byte[42240];
			for (int i = 0; i < 42240; i++)
				memdata[i] = 0x00;
			System.arraycopy(data, 0, memdata, 0, Math.min(0xa500, data.length));
			BinaryRenderer BinRenderer = new BinaryRenderer();
			Renderers.add(BinRenderer);
			BinRenderer.Render(TargetPage, memdata, 0x4000, 200);
		}
	}

	protected void doConvertSnaToLoadableFiles(MachineState cpustate, IDEDosPartition targetpartition,
			String filename) {
		int i = filename.indexOf(".");
		if (i > 0) {
			filename = filename.substring(0, i).trim();
		}
		try {
			CPUStateToFiles.SaveToPartition(cpustate, targetpartition, filename);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void doExtractFiles128() {
		DirectoryDialog dialog = new DirectoryDialog(Targetpg.getShell());
		dialog.setText("Select folder for target");
		String result = dialog.open();
		if (result != null) {
			File rootfolder = new File(result);
			File RootFile = new File(rootfolder, fName);
			// firstly BASIC...
			// save any basic
			if (IYReg == 0x5c3a) {
				SaveBasicFile(RootFile);
			}
			// Paged in RAM
			// make sure we have a full ram dump. If not, pad to the full 49152.
			byte ram[] = new byte[49152];
			System.arraycopy(rawdata, 0, ram, 0, Math.min(49152, rawdata.length));
			GeneralUtils.WriteBlockToDisk(ram, RootFile.getAbsoluteFile() + ".ramdump");

			// save the raw scr file.
			byte scr[] = new byte[6912];
			System.arraycopy(rawdata, 0, scr, 0, Math.min(6912, rawdata.length));
			GeneralUtils.WriteBlockToDisk(scr, RootFile.getAbsoluteFile() + ".scr");

			// Each ram bank.
			int ptr = 0;
			for (int page : i128BankOrder) {
				byte currentdata[] = new byte[0x4000];
				System.arraycopy(rawdata, ptr, currentdata, 0, 0x4000);
				GeneralUtils.WriteBlockToDisk(currentdata, RootFile.getAbsoluteFile() + "Page" + page);
				if ((page == 5) || (page == 7)) {
					// Images, both in page 5 and 7
					ImageData image = Speccy.GetImageFromFileArray(currentdata, 0);
					ImageLoader saver = new ImageLoader();
					saver.data = new ImageData[] { image };
					saver.save(RootFile.getAbsoluteFile() + "Page" + page + ".image", SWT.IMAGE_PNG);
				}
				ptr = ptr + 0x4000;
			}

			// save BASIC
			if (HasBasic) {
				if (BasicData != null) {
					StringBuilder sb = new StringBuilder();
					Speccy.DecodeBasicFromLoadedFile(BasicData, sb, VarsOffset, true, false);
					GeneralUtils.WriteBlockToDisk(sb.toString().getBytes(), RootFile.getAbsoluteFile() + ".basic");

					sb = new StringBuilder();
					Speccy.DecodeBasicFromLoadedFile(BasicData, sb, VarsOffset, false, false);
					GeneralUtils.WriteBlockToDisk(sb.toString().getBytes(), RootFile.getAbsoluteFile() + ".bas");
					
					GeneralUtils.WriteBlockToDisk(BasicData, RootFile.getAbsoluteFile() + ".rawbasic");
				}
				if (SVR != null) {
					String Sysvars = SVR.getSystemVariableSummary();
					GeneralUtils.WriteBlockToDisk(Sysvars.getBytes(), RootFile.getAbsoluteFile() + ".SYSVARS");
				}
			}
		}
	}

	/**
	 * 
	 */
	protected void doExtractFiles() {
		DirectoryDialog dialog = new DirectoryDialog(Targetpg.getShell());
		dialog.setText("Select folder for target");
		String result = dialog.open();
		if (result != null) {
			File rootfolder = new File(result);
			File RootFile = new File(rootfolder, fName);

			// make sure we have a full ram dump. If not, pad to the full 49152.
			byte ram[] = new byte[49152];
			System.arraycopy(rawdata, 0, ram, 0, Math.min(49152, rawdata.length));

			// save the snapshot
			GeneralUtils.WriteBlockToDisk(ram, RootFile.getAbsoluteFile() + ".ramdump");

			// save the raw scr file.
			byte scr[] = new byte[6912];
			System.arraycopy(rawdata, 0, scr, 0, Math.min(6912, rawdata.length));
			GeneralUtils.WriteBlockToDisk(scr, RootFile.getAbsoluteFile() + ".scr");

			// save the screen as a PNG
			ImageData image = Speccy.GetImageFromFileArray(ram, 0);
			ImageLoader saver = new ImageLoader();
			saver.data = new ImageData[] { image };
			saver.save(RootFile.getAbsoluteFile() + ".image", SWT.IMAGE_PNG);

			// save BASIC
			if (HasBasic) {
				if (BasicData != null) {
					StringBuilder sb = new StringBuilder();
					Speccy.DecodeBasicFromLoadedFile(BasicData, sb, VarsOffset, true, false);
					GeneralUtils.WriteBlockToDisk(sb.toString().getBytes(), RootFile.getAbsoluteFile() + ".basic");

					sb = new StringBuilder();
					Speccy.DecodeBasicFromLoadedFile(BasicData, sb, VarsOffset, false, false);
					GeneralUtils.WriteBlockToDisk(sb.toString().getBytes(), RootFile.getAbsoluteFile() + ".bas");

					GeneralUtils.WriteBlockToDisk(BasicData, RootFile.getAbsoluteFile() + ".rawbasic");
				}
				if (SVR != null) {
					String Sysvars = SVR.getSystemVariableSummary();
					GeneralUtils.WriteBlockToDisk(Sysvars.getBytes(), RootFile.getAbsoluteFile() + ".SYSVARS");
				}
			}
		}
	}

	private int SaveBasicFile(File RootFile) {
		// check for BASIC.
		int diff = 0x4000;
		int PROG = (rawdata[0x5c54 - diff] & 0xff) * 256 + (rawdata[0x5c53 - diff] & 0xff);
		int VARS = (rawdata[0x5c4c - diff] & 0xff) * 256 + (rawdata[0x5c4b - diff] & 0xff);
		int E_LINE = (rawdata[0x5c5a - diff] & 0xff) * 256 + (rawdata[0x5c59 - diff] & 0xff);
		int RAMTOP = (rawdata[0x5cb3 - diff] & 0xff) * 256 + (rawdata[0x5cb2 - diff] & 0xff);

		int EndOfAnyBasic = RAMTOP - diff;
		// some basic checking.
		if ((VARS > PROG) && (E_LINE > VARS) && (PROG > 23754) && (PROG < 25000)) {
			byte BasicData[] = new byte[E_LINE - PROG];
			System.arraycopy(rawdata, PROG - diff, BasicData, 0, BasicData.length);
			GeneralUtils.WriteBlockToDisk(BasicData, RootFile.getAbsoluteFile() + ".rawbasic");
			FileWriter fileWriter;
			try {
				fileWriter = new FileWriter(new File(RootFile.getAbsoluteFile() + ".basic"));
				try {
					PrintWriter printWriter = new PrintWriter(fileWriter);
					try {
						int ptr = 0;
						int VariablesOffset = VARS - PROG;
						int EndOfBasicArea = Math.min(E_LINE - PROG, VariablesOffset);
						while (ptr < EndOfBasicArea) {
							int linenum = -1;
							int linelen = 0;
							try {
								linenum = ((BasicData[ptr++] & 0xff) * 256);
								linenum = linenum + (BasicData[ptr++] & 0xff);
								linelen = (int) BasicData[ptr++] & 0xff;
								linelen = linelen + ((int) (BasicData[ptr++] & 0xff) * 256);
								// Record original line length for REM purposes.
								// fiddles bad line lengths
								linelen = Math.min(BasicData.length - ptr + 4, linelen);
							} catch (Exception E) {
								printWriter.println("Basic parsing error, bad linenum.");
								ptr = 99999999;
							}

							if ((ptr >= VariablesOffset) || (linenum < 0)) {
								// now into the variables area. Ignoring for the moment.
								ptr = 99999999;
							} else {
								String sixdigit = String.valueOf(linenum);
								while (sixdigit.length() < 6) {
									sixdigit = sixdigit + " ";
								}
								StringBuilder sb = new StringBuilder();
								try {
									byte line[] = new byte[linelen];
									for (int i = 0; i < linelen; i++) {
										line[i] = BasicData[ptr + i];
									}
									Speccy.DecodeBasicLine(sb, line, 0, linelen, false);
								} catch (Exception E) {
									sb.append("Bad line: " + E.getMessage());
									ptr = 99999999;
								}
								// point to next line.
								ptr = ptr + linelen;

								printWriter.println(sixdigit + " " + sb.toString());
							}
						}
					} finally {
						printWriter.close();
					}
				} finally {
					fileWriter.close();
				}
			} catch (IOException e) {
				System.out.println("Cannot write basic file - IO error.");
			}
		}
		return (EndOfAnyBasic);
	}
}

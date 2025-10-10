package hddEditor.ui.partitionPages.FileRenderers;

/**
 * Render a CODE file
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Languages;
import hddEditor.libs.Speccy;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.Renderer;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.SNARenderer;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.SPRenderer;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.SZXSnapshotRenderer;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.BinaryRenderer;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.RamDump;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.ScreenRenderer;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.SpriteRenderer;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.TextRenderer;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.Z80SnapshotRenderer;
import hddEditor.ui.partitionPages.dialogs.edit.callbacks.GenericSaveEvent;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.AssemblyRenderer;

public class CodeRenderer extends FileRenderer {
	// components
	private Text StartAddress = null;
	private Combo CodeTypeDropDown = null;
	private Vector<Renderer> Renderers = null;
	private IDEDosPartition part;
	private Color DefaultBackgroundColor;

	// Rendering options
	private String[] CODETYPES = { "Binary", "Screen", "Assembly", "SNA file", "Z80 file", "48k Ram Dump", ".SP file",
			"ASCII Text", "Sprite", "48K ram dump (At loaded address)", "SZX file" };

	/**
	 * 
	 * @param mainPage
	 * @param data
	 * @param header
	 * @param Filename
	 * @param fileSize
	 * @param loadAddr
	 */
	public void RenderCode(Composite mainPage, byte data[], byte header[], String Filename, int fileSize, int loadAddr,
			FileSelectDialog filesel, IDEDosPartition currentpart, GenericSaveEvent saveevent, Languages lang) {

		super.Render(mainPage, data, Filename, filesel, lang);
		part = currentpart;
		Renderers = new Vector<Renderer>();
		Label lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_CODEFILE) + ": ");
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		gd.horizontalSpan = 2;
		gd.heightHint = 20;
		lbl.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_PLUS3DOSFILELEN) +": ");
		gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		gd.horizontalSpan = 1;
		gd.heightHint = 20;
		lbl.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(String.format("%d (%X)", fileSize, fileSize));
		gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		gd.horizontalSpan = 1;
		gd.heightHint = 20;
		lbl.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_STARTADDRESS) + ": ");
		gd.horizontalSpan = 1;
		gd.heightHint = 20;
		lbl.setLayoutData(gd);

		StartAddress = new Text(mainPage, SWT.NONE);
		StartAddress.setText(String.valueOf(loadAddr));
		gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		gd.minimumWidth = 50;
		gd.heightHint = 20;
		StartAddress.setLayoutData(gd);

		Button btn;
		DefaultBackgroundColor = lbl.getBackground();
		if (saveevent != null) {
			btn = new Button(mainPage, SWT.NONE);
			btn.setText(lang.Msg(Languages.MSG_UPDSTARTADDRESS));
			btn.setLayoutData(gd);
			btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					if (saveevent != null) {
						try {
							String sa = StartAddress.getText();
							Integer sai = Integer.valueOf(sa);

							if (!saveevent.DoSave(0, sa, sai)) {
								StartAddress.setBackground(new Color(mainPage.getDisplay(), 255,0,0));
							} else {
								StartAddress.setBackground(DefaultBackgroundColor);
							}
							
						} catch (NumberFormatException e) {
							StartAddress.setBackground(new Color(mainPage.getDisplay(), 255,0,0));
						}
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
		} else {
			lbl = new Label(mainPage, 0);
		}

		btn = new Button(mainPage, SWT.NONE);
		btn.setText(lang.Msg(Languages.MSG_EXTRACTASHEX));
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsHex(data, mainPage, 0, data.length, filename);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		btn.setToolTipText(lang.Msg(Languages.MSG_EXTRACTASHEX));
		gd.horizontalSpan = 1;
		gd.heightHint = 20;
		btn.setLayoutData(gd);

		btn = new Button(mainPage, SWT.NONE);
		btn.setText(lang.Msg(Languages.MSG_EXTRACTASBIN));
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsBin(data, mainPage, Filename);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		gd.horizontalSpan = 1;
		gd.heightHint = 20;
		btn.setLayoutData(gd);

		if (header != null) {
			btn = new Button(mainPage, SWT.NONE);
			btn.setText(lang.Msg(Languages.MSG_EXTRACTASBINHEADER));
			btn.setLayoutData(gd);
			btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					byte newdata[] = new byte[data.length + header.length];
					System.arraycopy(header, 0, newdata, 0, header.length);
					System.arraycopy(data, 0, newdata, header.length, data.length);

					DoSaveFileAsBin(newdata, mainPage, Filename);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
			gd.horizontalSpan = 1;
			gd.heightHint = 20;
			btn.setLayoutData(gd);
		} else {
			new Label(mainPage, SWT.NONE);
		}

		btn = new Button(mainPage, SWT.NONE);
		btn.setText(lang.Msg(Languages.MSG_EXTRACTASIMG));
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsPic(data, mainPage, Filename);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		btn.setToolTipText(lang.Msg(Languages.MSG_EXTRACTASIMG));
		gd.horizontalSpan = 1;
		gd.heightHint = 20;
		btn.setLayoutData(gd);

		btn = new Button(mainPage, SWT.NONE);
		btn.setText(lang.Msg(Languages.MSG_EXTRACTASASM));
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsAsm(data, mainPage, loadAddr, Filename);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		btn.setToolTipText(lang.Msg(Languages.MSG_EXTRACTASASMDESC));
		gd.horizontalSpan = 1;
		gd.heightHint = 20;
		btn.setLayoutData(gd);

		CodeTypeDropDown = new Combo(mainPage, SWT.NONE);
		CodeTypeDropDown.setItems(CODETYPES);
		CodeTypeDropDown.setText(CODETYPES[0]);
		if (data.length == 6912) {
			CodeTypeDropDown.setText(CODETYPES[1]);
		}
		CodeTypeDropDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CodeTypeComboChanged(data, loadAddr);
			}
		});
		gd.horizontalSpan = 1;
		gd.heightHint = 35;
		CodeTypeDropDown.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);
		lbl = new Label(mainPage, SWT.NONE);
		lbl = new Label(mainPage, SWT.NONE);

		/*
		 * Default the code type depending on the file extension. 
		 */
		if (Filename.toUpperCase().endsWith(".SNA")) {
			CodeTypeDropDown.setText(CODETYPES[3]);
		}
		if (Filename.toUpperCase().endsWith(".Z80")) {
			CodeTypeDropDown.setText(CODETYPES[4]);
		}
		if (Filename.toUpperCase().trim().endsWith(".SP")) {
			CodeTypeDropDown.setText(CODETYPES[6]);
		}
		if (Filename.toUpperCase().trim().endsWith(".SZX")) {
			CodeTypeDropDown.setText(CODETYPES[10]);
		}

		CodeTypeComboChanged(data, loadAddr);

		mainPage.pack();
	}

	/**
	 * Render the code file as selected in the combo.
	 * 
	 * @param data
	 * @param loadAddr
	 */
	private void CodeTypeComboChanged(byte data[], int loadAddr) {
		String s = CodeTypeDropDown.getText().trim();
		DoChangeCodeType(s, data, loadAddr);
	}

	/**
	 * Actually render
	 * 
	 * @param s
	 * @param data
	 * @param loadAddr
	 */
	private void DoChangeCodeType(String s, byte data[], int loadAddr) {

		// Dispose of any items that are already on the form
		for (Renderer r : Renderers) {
			r.DisposeRenderer();
		}
		Renderers.clear();

		try {
			// Render the appropriate type
			if (s.equals(CODETYPES[1])) {
				ScreenRenderer renderer = new ScreenRenderer();
				Renderers.add(renderer);
				renderer.Render(MainPage, data);
			} else if (s.equals(CODETYPES[2])) {
				AssemblyRenderer renderer = new AssemblyRenderer();
				Renderers.add(renderer);
				renderer.Render(MainPage, data, loadAddr, lang);
			} else if (s.equals(CODETYPES[3])) {
				SNARenderer renderer = new SNARenderer();
				Renderers.add(renderer);
				renderer.Render(MainPage, data, loadAddr, filename, part, lang);
			} else if (s.equals(CODETYPES[4])) {
				Z80SnapshotRenderer renderer = new Z80SnapshotRenderer();
				Renderers.add(renderer);
				renderer.Render(MainPage, data, loadAddr, filename, part, lang);
			} else if (s.equals(CODETYPES[5])) {
				RamDump renderer = new RamDump();
				Renderers.add(renderer);
				renderer.Render(MainPage, data, loadAddr, false, 0x5c3a, new int[0], filename, null, null, lang);
			} else if (s.equals(CODETYPES[6])) {
				RamDump renderer = new SPRenderer();
				Renderers.add(renderer);
				renderer.Render(MainPage, data, loadAddr, false, 0x5c3a, new int[0], filename, null, null, lang);
			} else if (s.equals(CODETYPES[7])) {
				TextRenderer renderer = new TextRenderer();
				Renderers.add(renderer);
				renderer.Render(MainPage, data, filename);
			} else if (s.equals(CODETYPES[8])) {
				SpriteRenderer renderer = new SpriteRenderer();
				Renderers.add(renderer);
				renderer.Render(MainPage, data, 400, loadAddr, filesel, filename, lang);
			} else if (s.equals(CODETYPES[9])) {
				RamDump renderer = new RamDump();
				Renderers.add(renderer);

				byte data1[] = new byte[0xc000];
				int start = loadAddr - 0x4000;
				int length = Math.min(0xc000 - 0x1b00, data.length);
				//Cut off all data in the ROM area.
				if (start <0) {
					length = length + start;
					start = 0;
				}
				
				System.arraycopy(data, 0, data1, start, length);

				renderer.Render(MainPage, data1, loadAddr, false, 0x5c3a, new int[0], filename, null, null, lang);
			} else if (s.equals(CODETYPES[10])) {
				SZXSnapshotRenderer renderer = new SZXSnapshotRenderer();
				Renderers.add(renderer);
				renderer.Render(MainPage, data, loadAddr, filename, part, lang);
			} else {
				BinaryRenderer renderer = new BinaryRenderer();
				Renderers.add(renderer);
				renderer.Render(MainPage, data, loadAddr, 400, lang);
			}
		} catch (Exception E) {
			System.out.println("Error rendering:");
			E.printStackTrace();
		}
		MainPage.layout();
		MainPage.pack();
		((ScrolledComposite) MainPage.getParent())
				.setMinSize(MainPage.computeSize(MainPage.getClientArea().width + 1, SWT.DEFAULT));

	}

	protected void DoSaveFileAsAsm(byte[] data, Composite mainPage2, int loadAddr, String Origfilename) {
		File Selected = filesel.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES,
				String.format(lang.Msg(Languages.MSG_SAVEXASASM), Origfilename), new String[] { "*.asm" }, filename);

		if (Selected != null) {
			Speccy.DoSaveFileAsAsm(data, Selected, loadAddr, lang);
		}
		mainPage2.getShell().moveAbove(null);

	}

	/**
	 * Save the file as an image file. (Note, this will be 256x192 of whatever
	 * format is selected)
	 * 
	 * @param data
	 * @param mainPage
	 */
	protected void DoSaveFileAsPic(byte[] data, Composite mainPage, String Origfilename) {
		File Selected = filesel.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES,
				String.format(lang.Msg(Languages.MSG_SAVEXASIMG), Origfilename),
				new String[] { "*.png", "*.gif", "*.bmp", "*.tiff", "*.jpg", "*.ico" }, filename + ".png");

		if (Selected != null) {
			FileOutputStream file;
			try {
				file = new FileOutputStream(Selected);
				try {
					String selected = Selected.getName();
					ImageData image = Speccy.GetImageFromFileArray(data, 0x00);
					ImageLoader imageLoader = new ImageLoader();
					imageLoader.data = new ImageData[] { image };
					int filetyp = SWT.IMAGE_JPEG;
					if (selected.toLowerCase().endsWith(".gif")) {
						filetyp = SWT.IMAGE_GIF;
					} else if (selected.toLowerCase().endsWith(".png")) {
						filetyp = SWT.IMAGE_PNG;
					} else if (selected.toLowerCase().endsWith(".bmp")) {
						filetyp = SWT.IMAGE_BMP;
					} else if (selected.toLowerCase().endsWith(".tiff")) {
						filetyp = SWT.IMAGE_TIFF;
					} else if (selected.toLowerCase().endsWith(".ico")) {
						filetyp = SWT.IMAGE_ICO;
					}

					imageLoader.save(selected, filetyp);
				} finally {
					file.close();
				}
			} catch (FileNotFoundException e) {
				MessageBox dialog = new MessageBox(MainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText(lang.Msg(Languages.MSG_ERRSAVING));
				dialog.setMessage(lang.Msg(Languages.MSG_DIRNOTFOUND));
				dialog.open();

				e.printStackTrace();
			} catch (IOException e) {
				MessageBox dialog = new MessageBox(MainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText(lang.Msg(Languages.MSG_ERRSAVING));
				dialog.setMessage(lang.Msg(Languages.MSG_IOERROR) + ": " + e.getMessage());
				dialog.open();
				e.printStackTrace();
			}
		}
		mainPage.getShell().moveAbove(null);

	}

}

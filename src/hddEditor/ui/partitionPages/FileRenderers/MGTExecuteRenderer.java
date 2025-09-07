package hddEditor.ui.partitionPages.FileRenderers;
/**
 * from :https://web.archive.org/web/20080514125600/http://www.ramsoft.bbk.org/tech/mgt_tech.txt
 * MGT file type: 
 * EXECUTE (type 11)
 * -----------------
 * 210-255 Same as CODE file (type 4), but Length=510 and Start=0x1BD6 implicitly
 *         (0x3DB6 for +D). The sector is loaded into the interface RAM
 *         and executed (it should contain relocatable code!).
 */

import java.io.File;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Languages;
import hddEditor.libs.Speccy;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.AssemblyRenderer;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.Renderer;

public class MGTExecuteRenderer extends FileRenderer {
	// components
	private Vector<Renderer> Renderers = null;

	public void RenderCode(Composite mainPage, byte data[], byte header [], String Filename, int fileSize,
			int loadAddr , FileSelectDialog filesel, Languages lang) {
		
		super.Render(mainPage, data, Filename, filesel, lang);
		Renderers = new Vector<Renderer>();
		Label lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_CODEFILE) + ": ");
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		gd.horizontalSpan = 2;
		gd.heightHint = 20;
		lbl.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_LENGTH) + ": ");
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
		lbl.setText(lang.Msg(Languages.MSG_STARTADDRSHOULDBE) + ": ");
		gd.horizontalSpan = 1;
		gd.heightHint = 20;
		lbl.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(String.valueOf(loadAddr));
		gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		gd.minimumWidth = 50;
		gd.heightHint = 20;
		lbl.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);

		Button btn = new Button(mainPage, SWT.NONE);
		btn.setText(lang.Msg(Languages.MSG_EXTRACTASHEX) );
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
		btn.setToolTipText(lang.Msg(Languages.MSG_EXTRACTASHEX) );
		gd.horizontalSpan = 1;
		gd.heightHint = 20;
		btn.setLayoutData(gd);

		btn = new Button(mainPage, SWT.NONE);
		btn.setText(lang.Msg(Languages.MSG_EXTRACTASBIN) );
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
		btn.setToolTipText(lang.Msg(Languages.MSG_EXTRACTASBIN) );
		gd.horizontalSpan = 1;
		gd.heightHint = 20;
		btn.setLayoutData(gd);

		if (header != null) {
			btn = new Button(mainPage, SWT.NONE);
			btn.setText(lang.Msg(Languages.MSG_EXTRACTASBINHEADER)) ;
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
		} else {
			new Label(mainPage, SWT.NONE);
		}

		btn = new Button(mainPage, SWT.NONE);
		btn.setText(lang.Msg(Languages.MSG_EXTRACTASASM) );
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

		lbl = new Label(mainPage, SWT.NONE);
		lbl = new Label(mainPage, SWT.NONE);
		lbl = new Label(mainPage, SWT.NONE);

		AssemblyRenderer renderer = new AssemblyRenderer();
		Renderers.add(renderer);
		renderer.Render(MainPage, data, loadAddr, lang);

		mainPage.pack();
	}

	protected void DoSaveFileAsAsm(byte[] data, Composite mainPage2, int loadAddr, String Origfilename) {
		File Selected = filesel.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES,
				String.format(lang.Msg(Languages.MSG_SAVEXASASM), Origfilename), new String[] {"*.asm"}, Origfilename+".asm");
		
		if (Selected != null) {
			Speccy.DoSaveFileAsAsm(data, Selected, loadAddr);
		}
		mainPage2.getShell().moveAbove(null);
	}

	
}

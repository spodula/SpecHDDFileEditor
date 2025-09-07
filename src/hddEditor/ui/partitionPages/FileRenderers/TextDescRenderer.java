package hddEditor.ui.partitionPages.FileRenderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Languages;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.TextRenderer;

public class TextDescRenderer extends FileRenderer {
	private TextRenderer tr = null;
	
	public void RenderText(Composite mainPage, byte data[], byte header[], String Filename, FileSelectDialog filesel, Languages lang) {
		super.Render(mainPage, data, Filename, filesel, lang);


		Label lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_TEXT) +": ");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;

		Button btn = new Button(mainPage, SWT.NONE);
		btn.setText(lang.Msg(Languages.MSG_EXTRACTASTEXT) );
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

		btn = new Button(mainPage, SWT.NONE);
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

		if (header != null) {
			btn = new Button(mainPage, SWT.NONE);
			btn.setText(lang.Msg(Languages.MSG_EXTRACTASBINHEADER) );
			btn.setLayoutData(gd);
			btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					byte newdata[] = new byte[data.length + header.length];
					System.arraycopy(header, 0, newdata, 0, header.length);
					System.arraycopy(data, 0, newdata, header.length, data.length);

					DoSaveFileAsBin(data, mainPage,Filename);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
		} else {
			new Label(mainPage, SWT.NONE);
		}
		new Label(mainPage, SWT.NONE);
		
		
		tr = new TextRenderer();
		tr.Render(mainPage, data,Filename +" - "+lang.Msg(Languages.MSG_TEXTDESC)+ ": ");
		
		mainPage.pack();
		
	}

}

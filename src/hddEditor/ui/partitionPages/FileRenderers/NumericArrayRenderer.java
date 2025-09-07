package hddEditor.ui.partitionPages.FileRenderers;
/**
 * Renderer for Numeric array file types.
 */

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Languages;
import hddEditor.libs.Speccy;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;
import hddEditor.ui.partitionPages.dialogs.edit.callbacks.GenericSaveEvent;

public class NumericArrayRenderer extends FileRenderer {
	// Variable name edit box.
	private Text VariableEdit = null;
	
	private Color DefaultBackgroundColor;
	/**
	 * render the array to the given composite.
	 */
	@Override
	public void Render(Composite mainPage, byte data[], String Filename, FileSelectDialog filesel, Languages lang) {
		this.filesel = filesel;
		this.lang = lang;
		Plus3DosFileHeader p3d = new Plus3DosFileHeader(data);
		String Varname ="A";
		byte header[] = null;
		byte newdata[] = data;
		if (p3d.IsPlus3DosFile()) {
			Varname = p3d.GetVarName();
			newdata = new byte[data.length - 0x80];
			header = new byte[0x80];
			System.arraycopy(data, 0, header, 0, 0x80);
			System.arraycopy(data, 0x80, newdata, 0, newdata.length);
		} 

		RenderNumericArray(mainPage, newdata, header, Filename, Varname, filesel,null, lang);
	}


	/**
	 * Render a numeric array page. 
	 * 
	 * @param mainPage - Page to parent to
	 * @param data - File data
	 * @param header - Header if appropriate. (If this is null, "Save with header" button will not be shown)
	 * @param Filename - Filename
	 * @param varname - Variable name.
	 * @param saveevent - If not null, called when the variable name changes.
	 */
	public void RenderNumericArray(Composite mainPage, byte data[], byte header[], String Filename, String varname, FileSelectDialog filesel, GenericSaveEvent saveevent, Languages lang) {
		super.Render(mainPage, data, Filename, filesel, lang);


		Label lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_NUMARRAY) + ": ");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;

		Button btn = new Button(mainPage, SWT.NONE);
		btn.setText(lang.Msg(Languages.MSG_EXTRACTASTEXT));
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveArrayAsText(data, mainPage, varname);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		btn = new Button(mainPage, SWT.NONE);
		btn.setText(lang.Msg(Languages.MSG_EXTRACTASBIN));
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsBin(data, mainPage,Filename);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

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

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_VARNAME)+": ");

		VariableEdit = new Text(mainPage, SWT.NONE);
		VariableEdit.setText("");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		VariableEdit.setLayoutData(gd);
		VariableEdit.setText(varname);
		
		
		DefaultBackgroundColor = lbl.getBackground();
		if (saveevent != null) {
			btn = new Button(mainPage, SWT.NONE);
			btn.setText(lang.Msg(Languages.MSG_UPDVARNAME));
			btn.setLayoutData(gd);
			btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					if (saveevent != null) {
						try {
							String sa = VariableEdit.getText();
							if (!saveevent.DoSave(0, sa, 0)) {
								VariableEdit.setBackground(new Color(mainPage.getDisplay(), 255,0,0));
							} else {
								VariableEdit.setBackground(DefaultBackgroundColor);
								VariableEdit.setText(sa.toUpperCase().substring(0,1));
							}
							
						} catch (NumberFormatException e) {
							VariableEdit.setBackground(new Color(mainPage.getDisplay(), 255,0,0));
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
		lbl = new Label(mainPage, 0);


		int location = 0x00;

		// Number of dimensions
		int numDimensions = data[location++] & 0xff;

		// LOad the dimension sizes into an array
		int Dimsizes[] = new int[numDimensions];
		for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
			int dimsize = data[location++] & 0xff;
			dimsize = dimsize + (data[location++] & 0xff) * 0x100;
			Dimsizes[dimnum] = dimsize;
		}

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_DIMENSIONS) + ": " + numDimensions);

		String s = varname + "(";
		for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
			if (dimnum > 0)
				s = s + ",";
			s = s + String.valueOf(Dimsizes[dimnum]);
		}
		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Dim " + s + ")");

		Text ArrayEdit = new Text(mainPage, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.minimumHeight = 400;
		ArrayEdit.setLayoutData(gd);

		// count of what dimensions have been processed.
		int DimCounts[] = new int[numDimensions];
		for (int dimnum = 0; dimnum < numDimensions; dimnum++)
			DimCounts[dimnum] = 0;

		StringBuilder sb = new StringBuilder();

		boolean complete = false;
		while (!complete) {
			for (int cc = 0; cc < Dimsizes[Dimsizes.length - 1]; cc++) {

				if (cc != 0) {
					sb.append(",");
				}
				double x = Speccy.GetNumberAtByte(data, location);
				// special case anything thats an exact integer because it makes the arrays look
				// less messy when displayed.
				if (x != Math.rint(x)) {
					sb.append(x);
					sb.append(",");
				} else {
					sb.append((int) x);
				}
				location = location + 5;
			}
			sb.append(System.lineSeparator());
			int diminc = Dimsizes.length - 2;
			boolean doneInc = false;
			while (!doneInc) {
				if (diminc == -1) {
					doneInc = true;
					complete = true;
				} else {
					int x = DimCounts[diminc];
					x++;
					if (x == Dimsizes[diminc]) {
						DimCounts[diminc] = 0;
						diminc--;
					} else {
						DimCounts[diminc] = x;
						doneInc = true;
					}
				}
			}

		}
		Font mono = new Font(mainPage.getDisplay(), "Monospace", 10, SWT.NONE);

		ArrayEdit.setText(sb.toString());
		ArrayEdit.setFont(mono);
		mainPage.pack();
	}

	/**
	 * Save the array to file
	 * 
	 * @param data - Data to be saved
	 * @param mainPage - Owning page
	 * @param varname - Variable name
	 */
	protected void DoSaveArrayAsText(byte[] data, Composite mainPage, String varname) {
		File Selected = filesel.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES, lang.Msg(Languages.MSG_SAVEARRAYAS) , new String[] {"*"}, filename);
		
		if (Selected != null) {
			Speccy.DoSaveNumericArrayAsText(Selected, data, varname);
		}
		mainPage.getShell().moveAbove(null);

	}


}

package hddEditor.ui.partitionPages.FileRenderers;

/**
 * Render a numeric array
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.Speccy;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;

public class NumericArrayRenderer extends FileRenderer {
	// Variable name edit box.
	Text VariableEdit = null;

	/**
	 * render the array to the given composite.
	 */
	@Override
	public void Render(Composite mainPage, byte data[], String Filename) {
		Plus3DosFileHeader p3d = new Plus3DosFileHeader(data);

		byte newdata[] = new byte[data.length - 0x80];
		byte header[] = new byte[0x80];
		System.arraycopy(data, 0, header, 0, 0x80);
		System.arraycopy(data, 0x80, newdata, 0, newdata.length);

		RenderNumericArray(mainPage, newdata, header, Filename, p3d.VarName);
	}

	/**
	 * Render a numeric array page. 
	 * 
	 * @param mainPage - Page to parent to
	 * @param data - File data
	 * @param header - Header if appropriate. (If this is null, "Save with header" button will not be shown)
	 * @param Filename - Filename
	 * @param varname - Variable name.
	 */
	public void RenderNumericArray(Composite mainPage, byte data[], byte header[], String Filename, String varname) {
		this.filename = Filename;
		this.MainPage = mainPage;
		this.data = data;

		Label lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Numeric array: ");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;

		Button btn = new Button(mainPage, SWT.NONE);
		btn.setText("Extract array as text");
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
		btn.setText("Extract file as Binary");
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
			btn.setText("Extract file as Binary Inc Header");
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
		lbl.setText("Variable: ");

		VariableEdit = new Text(mainPage, SWT.NONE);
		VariableEdit.setText("");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		VariableEdit.setLayoutData(gd);
		VariableEdit.setText(varname);

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
		lbl.setText("Dimensions: " + numDimensions);

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
			sb.append("\r\n");
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
		FileDialog fd = new FileDialog(MainPage.getShell(), SWT.SAVE);
		fd.setText("Save Array as");
		String[] filterExt = { "*.*" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			Speccy.DoSaveNumericArrayAsText(data, selected, varname);
		}
	}


}

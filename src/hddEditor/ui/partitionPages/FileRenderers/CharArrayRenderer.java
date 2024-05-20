package hddEditor.ui.partitionPages.FileRenderers;
import java.io.File;

//Fixed bug with rendering.
/**
 * Render a character array
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

public class CharArrayRenderer extends FileRenderer {
	Text VariableEdit = null;

	/**
	 * Render the character array to the composite.
	 */
	@Override
	public void Render(Composite mainPage, byte data[], String Filename) {
		super.Render(mainPage, data, Filename);
		Plus3DosFileHeader p3d = new Plus3DosFileHeader(data);
		String Varname ="A";
		byte header[] = null;
		byte newdata[] = data;
		if (p3d.IsPlusThreeDosFile && p3d.ChecksumValid) {
			Varname = p3d.VarName;
			newdata = new byte[data.length - 0x80];
			header = new byte[0x80];
			System.arraycopy(data, 0, header, 0, 0x80);
			System.arraycopy(data, 0x80, newdata, 0, newdata.length);
		} 

		RenderCharArray(mainPage, newdata, header, Filename, Varname);
	}

	/**
	 * Render a character array to the given page
	 * 
	 * @param mainPage - Page to parent to.
	 * @param data     - data to be rendered
	 * @param header   - file header if appropriate (If null, "Save with header"
	 *                 button not shown)
	 * @param Filename - filename
	 * @param varname  - variable name
	 */
	public void RenderCharArray(Composite mainPage, byte data[], byte header[], String Filename, String varname) {
		Label lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Character array: ");
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
				DoSaveFileAsBin(data, mainPage, Filename);
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
				String chr = Speccy.tokens[data[location++] & 0xff];
				chr = chr.replace("&amp;", "&");
				chr = chr.replace("&gt;", ">");
				chr = chr.replace("&lt;", "<");

				sb.append(chr);
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
	 * Save the character array
	 * 
	 * @param data
	 * @param mainPage
	 * @param p3d
	 */
	protected void DoSaveArrayAsText(byte[] data, Composite mainPage, String varname) {
		FileDialog fd = new FileDialog(MainPage.getShell(), SWT.SAVE);
		fd.setText("Save Array as");
		String[] filterExt = { "*.*" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			Speccy.DoSaveCharArrayAsText(new File(selected), data, varname);
		}
	}
}

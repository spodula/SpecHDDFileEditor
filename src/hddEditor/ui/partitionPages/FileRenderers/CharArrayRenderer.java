package hddEditor.ui.partitionPages.FileRenderers;
/**
 * Render a character array
 */
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.Speccy;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;

public class CharArrayRenderer extends FileRenderer {
	Text VariableEdit=null;

	/**
	 * Render the character array to the composite.
	 */
	@Override
	public void Render(Composite mainPage, byte data[], String Filename) {
		super.Render(mainPage, data, Filename);

		Plus3DosFileHeader p3d = new Plus3DosFileHeader(data);

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
				DoSaveArrayAsText(data, mainPage, p3d);
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
				DoSaveFileAsBin(data, mainPage, false, p3d);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		btn = new Button(mainPage, SWT.NONE);
		btn.setText("Extract file as Binary Inc Header");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsBin(data, mainPage, true, p3d);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		new Label(mainPage, SWT.NONE);


		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Variable: ");

		VariableEdit = new Text(mainPage, SWT.NONE);
		VariableEdit.setText("");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		VariableEdit.setLayoutData(gd);
		VariableEdit.setText(p3d.VarName);
		
		int location = 0x80; // skip header

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
		lbl.setText("Dimensions: "+numDimensions);
		
		
		String s = p3d.VarName +"(";
		for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
			if (dimnum > 0)
				s = s + ",";
			s = s + String.valueOf(Dimsizes[dimnum]);
		}
		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Dim "+s+")");
		
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
				chr = chr.replace("&amp;","&");
				chr = chr.replace("&gt;",">");
				chr = chr.replace("&lt;","<");
				
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
	 * @param data
	 * @param mainPage
	 * @param p3d
	 */
	protected void DoSaveArrayAsText(byte[] data, Composite mainPage, Plus3DosFileHeader p3d) {
		FileDialog fd = new FileDialog(MainPage.getShell(), SWT.SAVE);
		fd.setText("Save Array as");
		String[] filterExt = { "*.*" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			FileOutputStream file;
			try {
				file = new FileOutputStream(selected);
				try {
					file.write(("File: " + filename + System.lineSeparator()).getBytes());
					int location = 0x80; // skip header

					// Number of dimensions
					int numDimensions = data[location++] & 0xff;

					// LOad the dimension sizes into an array
					int Dimsizes[] = new int[numDimensions];
					for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
						int dimsize = data[location++] & 0xff;
						dimsize = dimsize + (data[location++] & 0xff) * 0x100;
						Dimsizes[dimnum] = dimsize;
					}
					String s = "DIM "+p3d.VarName + "(";
					for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
						if (dimnum > 0)
							s = s + ",";
						s = s + String.valueOf(Dimsizes[dimnum]);
					}
					s = s +") = "+System.lineSeparator();
					file.write(s.getBytes());
					
					// count of what dimensions have been processed.
					int DimCounts[] = new int[numDimensions];
					for (int dimnum = 0; dimnum < numDimensions; dimnum++)
						DimCounts[dimnum] = 0;
					
					StringBuffer sb = new StringBuffer();
					boolean complete = false;
					while (!complete) {
						for (int cc = 0; cc < Dimsizes[Dimsizes.length - 1]; cc++) {
							
							if (cc != 0) {
							   sb.append(",");
							}
							String chr = Speccy.tokens[data[location++] & 0xff];
							chr = chr.replace("&amp;","&");
							chr = chr.replace("&gt;",">");
							chr = chr.replace("&lt;","<");
							
							sb.append(chr);
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
					file.write(sb.toString().getBytes());					
					

				} finally {
					file.close();
				}
			} catch (FileNotFoundException e) {
				MessageBox dialog = new MessageBox(MainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Directory not found!");
				dialog.open();

				e.printStackTrace();
			} catch (IOException e) {
				MessageBox dialog = new MessageBox(MainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("IO error: " + e.getMessage());
				dialog.open();
				e.printStackTrace();
			}
		}
	}
}

package hddEditor.ui.partitionPages.FileRenderers;
//BUGFIX: GDS 22/01/2023 - Fixed error deciding when REM statements need to be disassembled. Was calculating with wrong values.

import java.io.File;

//BUGFIX: GDS 23/01/2023 - Now handles bad basic files a bit better when the +3Size> CPMsize 
//QOLFIX: GDS 22/01/2023 - DoSaveFileAsText: Now defaults to the current filename and puts in title bar.

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.FileSelectDialog;

public class BasicRenderer extends FileRenderer {
	// Components
	private Text StartLineEdit = null;
	private Text VariableStartEdit = null;
	public Table Listing = null;
	public Table Variables = null;
	public Label VarLBL = null;
	
	/**
	 * Version of Render that doesn't rely on the +3DOS header
	 * 
	 * @param mainPage
	 * @param data
	 * @param Filename
	 * @param filelength
	 * @param loadaddr
	 * @param fileSize
	 * @param VariablesOffset
	 * @param Startline
	 */
	public void RenderBasic(Composite mainPage, byte data[], byte header[], String Filename, int filelength,
			int VariablesOffset, int Startline, FileSelectDialog filesel) {
		super.Render(mainPage, data, Filename, filesel);

		Label lbl = new Label(this.MainPage, SWT.NONE);
		lbl.setText("BASIC program: ");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		lbl = new Label(this.MainPage, SWT.NONE);
		lbl.setText("Start line: ");

		StartLineEdit = new Text(this.MainPage, SWT.NONE);
		StartLineEdit.setText("");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		StartLineEdit.setLayoutData(gd);

		lbl = new Label(this.MainPage, SWT.NONE);
		lbl.setText("Variable start: ");

		VariableStartEdit = new Text(this.MainPage, SWT.NONE);
		VariableStartEdit.setText("");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		VariableStartEdit.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;

		Button btn = new Button(this.MainPage, SWT.NONE);
		btn.setText("Extract file as text");
		btn.setLayoutData(gd);
		Composite mainpage = this.MainPage;
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsText(mainpage);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		btn = new Button(this.MainPage, SWT.NONE);
		btn.setText("Extract file as Binary");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsBin(data, mainpage, Filename);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);

			}
		});

		if (header != null) {
			btn = new Button(this.MainPage, SWT.NONE);
			btn.setText("Extract file as Binary Inc Header");
			btn.setLayoutData(gd);
			btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					byte newdata[] = new byte[data.length + header.length];
					System.arraycopy(header, 0, newdata, 0, header.length);
					System.arraycopy(data, 0, newdata, header.length, data.length);
					DoSaveFileAsBin(newdata, mainpage, Filename);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});
		}

		btn = new Button(this.MainPage, SWT.NONE);
		btn.setText("Extract file as Hex");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsHex(data, mainpage, 0, filelength, Filename);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		StartLineEdit.setText(String.valueOf(Startline));
		VariableStartEdit.setText(String.valueOf(VariablesOffset));

		
		hddEditor.ui.partitionPages.FileRenderers.RawRender.BasicRenderer br = new hddEditor.ui.partitionPages.FileRenderers.RawRender.BasicRenderer();
		br.AddBasicFile(mainpage, data, filelength, VariablesOffset);
		br.AddVariables(mainpage, data, filelength, VariablesOffset);

		Listing = br.Listing;
		Variables = br.Variables;
		
		this.MainPage.pack();
	}
	
	/**
	 * Save file as text.
	 *
	 * @param mainPage
	 */
	protected void DoSaveFileAsText(Composite mainPage) {
		File Selected = filesel.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES, "Save " + filename + " as text file", new String[] {"*.txt"},filename);
		
		if (Selected != null) {
			PrintWriter file;
			try {
				file = new PrintWriter(Selected, "UTF-8");
				try {
					for (int line = 0; line < Listing.getItemCount(); line++) {
						TableItem itm = Listing.getItem(line);
						String lineno = itm.getText(0);
						String content = itm.getText(1);
						file.write(lineno.trim() + " ");
						file.write(content.trim() + System.lineSeparator());
					}
				} finally {
					file.close();
				}
			} catch (FileNotFoundException e) {
				MessageBox dialog = new MessageBox(mainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Directory not found!");
				dialog.open();

				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				MessageBox dialog = new MessageBox(mainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Internal error, cannot write UTF-8?");
				dialog.open();
				e.printStackTrace();
			}

		}
	}



}

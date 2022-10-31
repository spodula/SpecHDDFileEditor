package hddEditor.ui.partitionPages.dialogs;
/**
 * Implement the hex editors SAVE AS dialog
 */

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SaveAsDialog {
	//Form components
	private Display display = null;
	private Shell shell = null;
	
	private Text startEdit=null;
	private Text lengthEdit=null;
	private Text FileNameEdit=null;
	
	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public SaveAsDialog(Display display) {
		this.display = display;
	}
	
	/**
	 * Show the form
	 * 
	 * @param data
	 * @param title
	 */
	public void Show(byte[] data, String title) {
		Createform(data, title);
		loop();
	}
	
	/**
	 * Create the form.
	 * 
	 * @param data
	 * @param title
	 */
	private void Createform(byte[] data, String title) {
		shell = new Shell(display);
		shell.setSize(900, 810);
		GridLayout gridLayout = new GridLayout(4,false);
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;

		shell.setLayout(gridLayout);
		shell.setText(title);

		Label lbl = new Label(shell,SWT.NONE);
		lbl.setText(title);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setFont(boldFont);
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		
		lbl = new Label(shell,SWT.NONE);
		lbl.setText("Filename:");
		lbl.setFont(boldFont);
		
		FileNameEdit = new Text(shell,SWT.BORDER);
		FileNameEdit.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		FileNameEdit.setLayoutData(gd);
		FileNameEdit.setSize(200, FileNameEdit.getSize().y);
		
		Button Selbtn = new Button(shell,SWT.NONE);
		Selbtn.setText("Select");
		Selbtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				SelectFile();
			}			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		GridData SingleColFillGridData = new GridData();
		SingleColFillGridData.grabExcessHorizontalSpace = true;
		SingleColFillGridData.horizontalSpan = 1;
		SingleColFillGridData.minimumWidth = 100;
		Selbtn.setLayoutData(SingleColFillGridData);
		
		
		
		lbl = new Label(shell,SWT.NONE);
		lbl.setText("Range start:");
		lbl.setFont(boldFont);
		
		startEdit = new Text(shell,SWT.BORDER);
		startEdit.setText("00000000000000000000000000000000");
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 3;
		startEdit.setLayoutData(gd);
		
		lbl = new Label(shell,SWT.NONE);
		lbl.setText("Length:");

		lengthEdit = new Text(shell,SWT.BORDER);
		lengthEdit.setText("00000000000000000000000000000000");
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 3;
		lengthEdit.setLayoutData(gd);
		
		lbl = new Label(shell,SWT.NONE);
		lbl.setText("");
		lbl = new Label(shell,SWT.NONE);
		lbl.setText("");
		
		Button Cancelbtn = new Button(shell,SWT.NONE);
		Cancelbtn.setText("Cancel");
		Cancelbtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				shell.close();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = SWT.RIGHT;
		gd.minimumWidth = 100;
		Cancelbtn.setLayoutData(gd);
		
		Button OKbtn = new Button(shell,SWT.NONE);
		OKbtn.setText("Save");
		OKbtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFile(data);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		OKbtn.setLayoutData(SingleColFillGridData);
		
		shell.pack();
		FileNameEdit.setText("");
		startEdit.setText("0");
		lengthEdit.setText(String.valueOf(data.length));
	}
	
	
	/**
	 * Dialog loop, open and wait until closed.
	 */
	public void loop() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		shell.dispose();
	}

	/**
	 * Select the file to save as
	 */
	public void SelectFile() {
		FileDialog fd = new FileDialog(shell, SWT.SAVE);
		fd.setText("Save file as");
		String[] filterExt = { "*.*" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			FileNameEdit.setText(selected);
		}
	}
	
	/**
	 * Actually save the file.
	 * 
	 * @param data
	 */
	public void DoSaveFile(byte[] data) {
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(FileNameEdit.getText());
			try {
				int start = Integer.valueOf(startEdit.getText());
				int length = Integer.valueOf(lengthEdit.getText());
				length = Math.min(length,data.length-start);
				byte newdata[] = new byte[length];
				System.arraycopy(data, start, newdata, 0, length);
				outputStream.write(newdata);
				shell.close();
			} finally {
				outputStream.close();
			} 
		} catch (FileNotFoundException e) {
			MessageBox dialog =
				    new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Directory not found!");
				dialog.open();
			e.printStackTrace();
		} catch (IOException e) {
			MessageBox dialog =
				    new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Cannot write to file! "+e.getMessage());
				dialog.open();
			e.printStackTrace();
		} 	
	}
	
	/**
	 * Callback from the HexEditDialog to force this form to close when the HexEdit
	 * dialog closes.
	 */
	public void close() {
		shell.setVisible(false);
		shell.dispose();
	}

}

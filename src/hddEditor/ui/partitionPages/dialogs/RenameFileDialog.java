package hddEditor.ui.partitionPages.dialogs;
/**
 * very simple Rename file dialog
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.Languages;

public class RenameFileDialog {
	//Form components
	private Display display = null;
	public Shell shell = null;
	private Text NewNameEdit = null;
	
	//return values
	public String NewName = "";
	private boolean result = false;
	
	private Languages lang;
	
	/**
	 * Constructor
	 * @param display
	 */
	public RenameFileDialog(Display display, Languages lang) {
		this.display = display;
		this.lang = lang;
	}

	/**
	 * Show the form
	 * 
	 * @param OldName
	 * @return
	 */
	public boolean Show(String OldName) {
		Createform(OldName);
		loop();
		return (result);
	}
	
	/**
	 * Dialog loop, open and wait until closed.
	 */
	public void loop() {
		shell.open();
		while (!shell.isDisposed() && shell.isVisible()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
	
	/**
	 * Create box.
	 */
	private void Createform(String OldName) {
		NewName = "";
		shell = new Shell(display);
		shell.setSize(900, 810);
		GridLayout gridLayout = new GridLayout(4, true);
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;
		gridLayout.marginBottom = 20;

		shell.setLayout(gridLayout);
		shell.setText(lang.Msg(Languages.MSG_RENAMEFILE));

		Label lbl = new Label(shell, SWT.NONE);
		FontData fontData = lbl.getFont().getFontData()[0];
		Font boldFont = new Font(display, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		lbl.setText(lang.Msg(Languages.MSG_OLDNAME));
		lbl.setFont(boldFont);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		lbl.setLayoutData(gd);

		Text OldNameTxt = new Text(shell, SWT.BORDER);
		OldNameTxt.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxxxxxxxxXXXXXXXXXXXx");
		OldNameTxt.setEnabled(false);
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 3;
		OldNameTxt.setLayoutData(gd);
		OldNameTxt.setSize(200, OldNameTxt.getSize().y);

		lbl = new Label(shell, SWT.NONE);
		lbl.setText(lang.Msg(Languages.MSG_NEWNAME));
		lbl.setFont(boldFont);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		lbl.setLayoutData(gd);

		NewNameEdit = new Text(shell, SWT.BORDER);
		NewNameEdit.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxxxxxxxxXXXXXXXXx");
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 3;
		NewNameEdit.setLayoutData(gd);
		NewNameEdit.setSize(200, NewNameEdit.getSize().y);

		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 160;
		Button Btn = new Button(shell, SWT.PUSH);
		Btn.setText(lang.Msg(Languages.MSG_CANCEL));
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				shell.dispose();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		Btn = new Button(shell, SWT.PUSH);
		Btn.setText(lang.Msg(Languages.MSG_RENAMEFILE));
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				result = true;
				NewName = NewNameEdit.getText();
				shell.dispose();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		shell.pack();
		OldNameTxt.setText(OldName);
		NewNameEdit.setText(OldName);
	}

	/**
	 * Force the form closed. Called when the parent form closes.
	 */
	public void close() {
		shell.close();
		if (!shell.isDisposed()) {
			shell.dispose();
		}
	}

	
}


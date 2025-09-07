package hddEditor.ui.partitionPages.dialogs;
/**
 * Generic progress bar form with cancel. 
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import hddEditor.libs.Languages;

public class ProgesssForm {
	//Form details
	private Display display = null;
	private Shell shell = null;
	private ProgressBar progress = null;
	private Label MessageLabel = null;

	//result
	private boolean cancelled = true;
	
	private Languages lang;
	
	/**
	 * Check if the form has been cancelled 
	 * @return
	 */
	public boolean IsCancelled() {
		return (cancelled);
	}

	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public ProgesssForm(Display display, Languages lang) {
		this.display = display;
		this.lang = lang;
	}

	/**
	 * Show the form and reset the cancelled flag
	 * 
	 */
	public void Show(String title, String message) {
		cancelled = false;
		Createform(title, message);
		shell.open();
	}

	/**
	 * Set the max value
	 * 
	 * @param max
	 */
	public void SetMax(int max) {
		progress.setMaximum(max);
		display.readAndDispatch();
	}

	/**
	 * Set the current value
	 * 
	 * @param value
	 */
	public void SetValue(int value) {
		progress.setSelection(value);
		display.readAndDispatch();
	}

	/**
	 * Create the components on the form.
	 * 
	 * @param title
	 */
	private void Createform(String title, String message) {
		cancelled = false;

		shell = new Shell(display);
		shell.setSize(900, 810);

		GridLayout gridLayout = new GridLayout(3, true);
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;

		shell.setLayout(gridLayout);
		shell.setText(title);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 600;
		gd.horizontalSpan = 3;

		MessageLabel = new Label(shell, SWT.NONE);
		MessageLabel.setText(message);
		MessageLabel.setLayoutData(gd);

		progress = new ProgressBar(shell, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 3;
		gd.widthHint = 600;
		progress.setLayoutData(gd);
		progress.setMinimum(0);
		progress.setMaximum(100);
		progress.setSelection(0);

		new Label(shell, SWT.NONE);

		Button CancelBtn = new Button(shell, SWT.BORDER);
		CancelBtn.setText(lang.Msg(Languages.MSG_CANCEL));
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 1;
		gd.widthHint = 200;
		CancelBtn.setLayoutData(gd);
		CancelBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				cancelled = true;
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		shell.pack();
	}
	
	/**
	 * Force the form to close. 
	 */
	public void close() {
		if (!shell.isDisposed())
			shell.dispose();
	}

	/**
	 * Set the message. 
	 * 
	 * @param string
	 */
	public void setMessage(String string) {
		MessageLabel.setText(string);
		shell.pack();
		display.readAndDispatch();
	}

}
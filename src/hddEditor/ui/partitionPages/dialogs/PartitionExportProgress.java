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

public class PartitionExportProgress {
	//Form details
	private Display display = null;
	private Shell shell = null;
	private ProgressBar progress1 = null;
	private ProgressBar progress2 = null;
	private Label MessageLabel1 = null;
	private Label MessageLabel2 = null;

	//result
	private boolean cancelled = true;
	
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
	public PartitionExportProgress(Display display) {
		this.display = display;
	}

	/**
	 * Show the form and reset the cancelled flag
	 * 
	 */
	public void Show(String title, String message1, String message2) {
		cancelled = false;
		Createform(title, message1, message2);
		shell.open();
	}

	/**
	 * Set the max value
	 * 
	 * @param max
	 */
	public void SetMax1(int max) {
		progress1.setMaximum(max);
		display.readAndDispatch();
	}
	public void SetMax2(int max) {
		progress2.setMaximum(max);
		display.readAndDispatch();
	}

	/**
	 * Set the current value
	 * 
	 * @param value
	 */
	public void SetValue1(int value) {
		progress1.setSelection(value);
		display.readAndDispatch();
	}
	public void SetValue2(int value) {
		progress2.setSelection(value);
		display.readAndDispatch();
	}

	/**
	 * Create the components on the form.
	 * 
	 * @param title
	 */
	private void Createform(String title, String message1, String message2) {
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
		MessageLabel1 = new Label(shell, SWT.NONE);
		MessageLabel1.setText(message1);
		MessageLabel1.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 3;
		gd.widthHint = 600;
		progress1 = new ProgressBar(shell, SWT.BORDER);
		progress1.setLayoutData(gd);
		progress1.setMinimum(0);
		progress1.setMaximum(100);
		progress1.setSelection(0);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 600;
		gd.horizontalSpan = 3;
		MessageLabel2 = new Label(shell, SWT.NONE);
		MessageLabel2.setText(message1);
		MessageLabel2.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 3;
		gd.widthHint = 600;
		progress2 = new ProgressBar(shell, SWT.BORDER);
		progress2.setLayoutData(gd);
		progress2.setMinimum(0);
		progress2.setMaximum(100);
		progress2.setSelection(0);
		
		new Label(shell, SWT.NONE);

		Button CancelBtn = new Button(shell, SWT.BORDER);
		CancelBtn.setText("Cancel");
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
	public void setMessage1(String string) {
		MessageLabel1.setText(string);
		shell.pack();
		display.readAndDispatch();
	}
	public void setMessage2(String string) {
		MessageLabel2.setText(string);
		shell.pack();
		display.readAndDispatch();
	}

}
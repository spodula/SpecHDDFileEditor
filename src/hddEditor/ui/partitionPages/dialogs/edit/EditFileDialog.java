package hddEditor.ui.partitionPages.dialogs.edit;
/**
 * Contains some base functions for Edit file dialogs.
 * This needs to be subclassed.
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.disks.FileEntry;

public class EditFileDialog {
	// Directory entry of the file being displayed
	protected FileEntry ThisEntry = null;

	// Title of the page
	protected String Title = "";

	// Composite we are parented to
	protected Composite MainPage = null;
	protected ScrolledComposite MainPage1 = null; 
	
	// Result
	protected boolean result = false;

	// Data for the file
	public byte[] data = new byte[0];
	
	// Form details
	protected Shell shell;
	protected Display display;
	
	protected FileSelectDialog filesel;
	
	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public EditFileDialog(Display display, FileSelectDialog filesel) {
		this.display = display;
		this.filesel = filesel;
	}

	/**
	 * Loop and wait
	 */
	public void loop() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
	
	/**
	 * CLose the form.
	 */
	public void close() {
		if (!shell.isDisposed()) {
			shell.close();
			shell.dispose();
		}
	}
	

	/**
	 * Create a generic label with the given text and span.
	 * 
	 * @param text - Text to put in the label.
	 * @param span - Number of columns to span. 
	 * @return - The label.
	 */
	protected Label label(String text, int span) {
		Label label = new Label(shell, SWT.SHADOW_NONE);
		label.setText(text);
		if (span > 1) {
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 4;
			label.setLayoutData(gd);
		}
		return (label);
	}
	

	/**
s	 * Set modified text in the title
	 *
	 * @param Modified - Show the (Modified) text in the title?
	 */
	protected void SetModified(boolean Modified) {
		String s = Title;
		if (Modified) {
			s = s + " (Modified)";
		}
		shell.setText(s);
	}

	/**
	 * Show the form. Common to all edit file dialogs.
	 * 
	 * @param data	- Data to be edited.
	 * @param title	- Title of the form. Usually the filename.
	 * @param entry - The FileEntry.
	 * @return - TRUE if data has changed and the user OK's it.
	 */
	public boolean Show(byte[] data, String title, FileEntry entry) {
		this.result = false;
		this.ThisEntry = entry;
		this.Title = title;
		this.data = data;
		Createform();
		SetModified(false);
		loop();
		return (result);
	}

	/**
	 * Create the shell and populate it. This needs to be overridden by the child
	 */
	protected void Createform() {
	}
	
}

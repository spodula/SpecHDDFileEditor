package hddEditor.ui.partitionPages;
/**
 * Generic partition page. 
 * 
 * This unit provides some useful functions for partition pages including handling the 
 * composite parenting and removal details. 
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.ui.HDDEditor;

public class GenericPage {
	Composite ParentComp = null;
	IDEDosPartition partition = null;
	HDDEditor RootPage = null;
	
	/**
	 * Generic constructor.
	 * 
	 * @param root - Editor page
	 * @param parent - Parent component to put stuff on.
	 * @param partition - Partition.
	 */
	public GenericPage(HDDEditor root, Composite parent, IDEDosPartition partition) {
		ParentComp = parent;
		RootPage = root;
		this.partition = partition;
		AddBasicDetails();
	}

	/**
	 * Remove all components parented on the composite. 
	 */
	public void RemoveComponents() {
		if ((ParentComp != null) && !ParentComp.isDisposed()) {
			for(Control child:ParentComp.getChildren()) {
				if (!child.isDisposed())
					child.dispose();
			}
			ParentComp.pack();
		}
	}
	
	/**
	 * Create a generic label with the given text and span.
	 * 
	 * @param text
	 * @param span
	 * @return
	 */
	public Label label(String text, int span) {
		Label label = new Label(ParentComp, SWT.SHADOW_NONE);
		label.setText(text);
		if (span>1) {
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 4;
			label.setLayoutData(gd);
		}
		return(label);
	}
	
	/**
	 * Create a generic Combo with the given text and content.
	 * 
	 * @param content
	 * @param def
	 * @return
	 */
	public Combo combo(String[] content, String def) {
		Combo c = new Combo(ParentComp, SWT.DROP_DOWN);
		c.setItems(content);
		c.setText(def);
		return(c);
	}
	
	/**
	 * Create a generic checkbox with the given text and check status.
	 * 
	 * @param title
	 * @param IsChecked
	 * @return
	 */
	public Button checkbox(String title, boolean IsChecked) {
		Button b = new Button(ParentComp, SWT.CHECK);
		b.setText(title);
		b.setSelection(IsChecked);
		return(b);
	}
	
	/**
	 * Create a generic button with the given text.
	 * 
	 * @param title
	 * @return
	 */
	public Button button(String title) {
		Button b = new Button(ParentComp, SWT.PUSH);
		b.setText(title);
		return(b);	
	}
	
	/**
	 * Create a generic editbox with the given limit.
	 * 
	 * @param value
	 * @param limit
	 * @return
	 */
	public Text editbox(String value, int limit) {
		Text t = new Text(ParentComp,SWT.BORDER);
		t.setTextLimit(limit);
		t.setText(value);
		return(t);
	}
	

	/**
	 * Add the generic details common to all IDEPLUSDOS partition types.
	 */
	public void AddBasicDetails() {
		if ((ParentComp != null) && !ParentComp.isDisposed()) {
			//Note, for most partition types, this would be editable. 
			//but for the system partition, this cannot be changed. 
		   RemoveComponents();
		   label("Name: "+partition.GetName(),1);
		   label("Type: "+partition.GetTypeAsString(),1);
		   label("Size (c/h): "+partition.GetStartCyl()+"/"+partition.GetStartHead()+" - "+partition.GetEndCyl()+"/"+partition.GetEndHead(),1);
		   label("Last sector: "+partition.GetEndSector(),1);
		   ParentComp.pack();
		}		
	}
	
	/**
	 * Quick handler for error messages.
	 * @param msg
	 */
	protected void ErrorBox(String msg) {
		MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_ERROR | SWT.CLOSE);
		messageBox.setMessage(msg);
		messageBox.setText(msg);
		messageBox.open();
	}
	

}

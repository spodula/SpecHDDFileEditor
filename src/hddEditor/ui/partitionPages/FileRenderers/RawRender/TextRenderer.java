package hddEditor.ui.partitionPages.FileRenderers.RawRender;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class TextRenderer implements Renderer {
	Text Message = null;


	@Override
	public void DisposeRenderer() {
		if (Message!= null) {
			if (!Message.isDisposed()) {
				Message.dispose();
			}
			Message = null;
		}
	}
	
	/**
	 * 
	 * @param TargetPage
	 * @param data
	 * @param title
	 */
	public void Render(Composite TargetPage, byte[] data, String title) {
		Message = new Text(TargetPage, SWT.MULTI);
		Message.setText(new String(data));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.verticalSpan = 4;
		gd.minimumWidth = 400;
		gd.minimumHeight = 100;
		Message.setLayoutData(gd);
		
		TargetPage.pack();
	}
}

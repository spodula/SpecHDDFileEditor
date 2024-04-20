package hddEditor.ui.partitionPages.FileRenderers.RawRender;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


public class SpriteRenderer implements Renderer {
	Text SpriteWidth = null;
	Text SpriteHeight = null;
	Text Displacement = null;
	

	public void Render(Composite mainPage, byte data[], String Filename) {
	
		Label lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("View as sprites: ");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);
		
		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Sprite Width (cols): ");
		SpriteWidth = new Text(mainPage, SWT.NONE);
		SpriteWidth.setText("1");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		SpriteWidth.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Sprite Height (px): ");
		SpriteHeight = new Text(mainPage, SWT.NONE);
		SpriteHeight.setText("1");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		SpriteHeight.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Displacement: ");
		Displacement = new Text(mainPage, SWT.NONE);
		Displacement.setText("0");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		Displacement.setLayoutData(gd);
		
		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("");
		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("");
		
		
		
	}

	@Override
	public void DisposeRenderer() {
		if (SpriteWidth!=null) {
			if (!SpriteWidth.isDisposed()) {
				SpriteWidth.dispose();
				SpriteWidth = null;
			}
		}
		if (SpriteHeight!=null) {
			if (!SpriteHeight.isDisposed()) {
				SpriteHeight.dispose();
				SpriteHeight = null;
			}
		}
		if (Displacement!=null) {
			if (!Displacement.isDisposed()) {
				Displacement.dispose();
				Displacement = null;
			}
		}
		
	}
}

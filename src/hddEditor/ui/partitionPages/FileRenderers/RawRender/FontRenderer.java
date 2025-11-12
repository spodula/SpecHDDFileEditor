package hddEditor.ui.partitionPages.FileRenderers.RawRender;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import hddEditor.libs.Speccy;

public class FontRenderer implements Renderer {
	ArrayList<Label> ImageLabels = null;

	public void DisposeRenderer() {
		if (ImageLabels != null) {
			for (Label l : ImageLabels) {
				l.dispose();
			}
			ImageLabels.clear();
			ImageLabels = null;
		}
	}

	public void Render(Composite TargetPage, byte data[]) {
		Font font = new Font(TargetPage.getDisplay(), "Tahoma", 9, SWT.BOLD);
		Image img = Speccy.RenderDataAsFontBlock(data,font, TargetPage.getDisplay()); 
		
		Label lbl = new Label(TargetPage, SWT.NONE);
		lbl.setImage(img);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumHeight = img.getBounds().width;
		gd.minimumWidth = img.getBounds().height;
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);
		
		ImageLabels = new ArrayList<Label>();
		ImageLabels.add(lbl);

		TargetPage.pack();
	}

}

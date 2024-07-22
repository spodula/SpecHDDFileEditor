package hddEditor.ui.partitionPages.FileRenderers.RawRender;
/**
 * Implementation of a Sam Coupe screen renderer. 
 * 
 * There are some limitations over the speccy one in addition to the SAM 
 * Because of the layout of the image file, we cant determine multiple screens.
 */

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import hddEditor.libs.SamCoupe;

public class SamScreenRenderer implements Renderer {
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

	public void RenderScreen(Composite TargetPage, byte data[]) {
		ImageLabels = new ArrayList<Label>();

		SamCoupe.SAMScreen sc = new SamCoupe.SAMScreen(data, false);
		ImageData image = sc.GetImage();
		Image img = new Image(TargetPage.getDisplay(), image);
		Label lbl = new Label(TargetPage, SWT.NONE);
		lbl.setImage(img);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumHeight = 192;
		gd.minimumWidth = 256;
		gd.horizontalSpan = 2;
		lbl.setLayoutData(gd);
		ImageLabels.add(lbl);
		TargetPage.pack();
	}
}

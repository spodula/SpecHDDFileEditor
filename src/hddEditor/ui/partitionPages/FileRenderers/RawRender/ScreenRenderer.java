package hddEditor.ui.partitionPages.FileRenderers.RawRender;
/**
 * Render 1 or more zx Spectrum format screen from a binary file of at least 6912 bytes.
 * It will repeat until it runs out of stuff to render.
 * 
 * If a file is incomplete, the attributes will be set to black on white.
 */

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import hddEditor.libs.Speccy;

public class ScreenRenderer implements Renderer {
	ArrayList<Label> ImageLabels=null;

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
		int base = 0;
		ImageLabels = new ArrayList<Label>();
		while (base < data.length) {
			byte screen[] = new byte[0x1b00];
			for (int i = 0; i < 0x1800; i++) {
				screen[i] = 0;
			}
			byte wob = Speccy.ToAttribute(Speccy.COLOUR_BLACK, Speccy.COLOUR_WHITE, false, false);
			for (int i = 0x1800; i < 0x1b00; i++) {
				screen[i] = wob;
			}
			System.arraycopy(data, base, screen, 0, Math.min(0x1b00, data.length - base));

			ImageData image = Speccy.GetImageFromFileArray(screen, 0);
			Image img = new Image(TargetPage.getDisplay(), image);
			Label lbl = new Label(TargetPage, SWT.NONE);
			lbl.setImage(img);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.minimumHeight = 192;
			gd.minimumWidth = 256;
			gd.horizontalSpan = 2;
			lbl.setLayoutData(gd);
			ImageLabels.add(lbl);
			base = base + 0x1b00;
		}
		TargetPage.pack();
	}
}

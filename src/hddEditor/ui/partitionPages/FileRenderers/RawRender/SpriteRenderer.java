package hddEditor.ui.partitionPages.FileRenderers.RawRender;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class SpriteRenderer implements Renderer {
	private Text SpriteWidth = null;
	private Text SpriteHeight = null;
	private Text Displacement = null;
	private Table SprTable = null;
	private int BaseAddress = 0;
	private byte data[];

	private Composite page;

	public void Render(Composite mainPage, byte data[], int HeightLimit, int baseaddress) {
		page = mainPage;
		this.data = data;
		this.BaseAddress = baseaddress;
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
		SpriteWidth.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
				UpdateTable();
			}

			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Sprite Height (px): ");
		SpriteHeight = new Text(mainPage, SWT.NONE);
		SpriteHeight.setText("8");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		SpriteHeight.setLayoutData(gd);
		SpriteHeight.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
				UpdateTable();
			}

			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Displacement: ");
		Displacement = new Text(mainPage, SWT.NONE);
		Displacement.setText("0");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		Displacement.setLayoutData(gd);
		Displacement.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
				UpdateTable();
			}
			
			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("");
		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("");

		SprTable = new Table(mainPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		SprTable.setLinesVisible(true);
		SprTable.addListener(SWT.MeasureItem, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				arg0.height = 60;
				
			}
			
		});
		

		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = HeightLimit;
		SprTable.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(SprTable, SWT.LEFT);
		tc1.setText("Sprite");
		tc1.setWidth(80);
		TableColumn tc2 = new TableColumn(SprTable, SWT.LEFT);
		tc2.setText("Start");
		tc2.setWidth(160);
		TableColumn tc3 = new TableColumn(SprTable, SWT.LEFT);
		tc3.setText("End");
		tc3.setWidth(160);

		SprTable.setHeaderVisible(true);
		UpdateTable();
	}

	/**
	 * 
	 */
	private void UpdateTable() {
		//TODO: dispose of all the images.
		//TODO: try to keep scale size.
		//TODO: add export of selected sprites. Images, Binary, asm
		SprTable.removeAll();
		SprTable.clearAll();

		int dataPtr = Integer.valueOf(Displacement.getText());
		int addr = BaseAddress + dataPtr;
		int width = Math.max(Integer.valueOf(SpriteWidth.getText()), 1);
		int height = Math.max(Integer.valueOf(SpriteHeight.getText()), 8);
		
		PaletteData pd = new PaletteData(new RGB[] { new RGB(0, 0, 0), new RGB(255, 255, 255) });

		while (dataPtr < data.length) {
			String textbits[] = { "", "", "" };
			textbits[1] = "$" + String.format("%x", addr + dataPtr);

			ImageData imgdata = new ImageData(width * 8, height, 1, pd);

			for (int y = 0; y < height; y++) {
				int pixX = 0;
				for (int xb = 0; xb < width; xb++) {
					byte d = 0;
					if (dataPtr < data.length) {
						d = data[dataPtr++];
					}
					for (int ByteX = 0; ByteX < 8; ByteX++) {
						if ((d & 0x80) == 0) {
							imgdata.setPixel(pixX++, y, 0xFFFFFF);
						} else {
							imgdata.setPixel(pixX++, y, 0x00);
						}
						d = (byte) (d * 2);
					}
				}
			}
			Image img = new Image(page.getDisplay(), imgdata);
			
			Image scaled = new Image(page.getDisplay(), 60,60);
			GC gc = new GC(scaled);
			gc.setAntialias(SWT.ON);
			gc.setInterpolation(SWT.HIGH);
			gc.drawImage(img, 0, 0,
					img.getBounds().width, img.getBounds().height,
			0, 0, 60, 60);
			gc.dispose();
			
			TableItem item = new TableItem(SprTable, SWT.LEFT);
			item.setImage(scaled);
			item.setText(textbits);
		}

	}

	/**
	 * Dispose of the child components.
	 */
	@Override
	public void DisposeRenderer() {
		if (SpriteWidth != null) {
			if (!SpriteWidth.isDisposed()) {
				SpriteWidth.dispose();
				SpriteWidth = null;
			}
		}
		if (SpriteHeight != null) {
			if (!SpriteHeight.isDisposed()) {
				SpriteHeight.dispose();
				SpriteHeight = null;
			}
		}
		if (Displacement != null) {
			if (!Displacement.isDisposed()) {
				Displacement.dispose();
				Displacement = null;
			}
		}

		if (SprTable != null) {
			if (!SprTable.isDisposed()) {
				SprTable.dispose();
				SprTable = null;
			}
		}
	}
}

package hddEditor.ui.partitionPages.FileRenderers.RawRender;
/**
 * Renderer for code files as sprites.
 */

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
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
	private Button ExportSelectedAsm = null;
	private Button ExportSelectedBin = null;

	private static int DISPLAYIMAGEWIDTH = 60;
	private static int DISPLAYIMAGEHEIGHT = 60;
	private static int BLACK = 0x000000;
	private static int WHITE = 0xFFFFFF;

	private ArrayList<Image> Images = null;

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

		ExportSelectedBin = new Button(mainPage, SWT.BORDER);
		ExportSelectedBin.setText("Export Selected as bin");
		ExportSelectedBin.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				DoExportSelected(true);
			}
		});

		ExportSelectedAsm = new Button(mainPage, SWT.BORDER);
		ExportSelectedAsm.setText("Export Selected as asm");
		ExportSelectedAsm.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				widgetDefaultSelected(arg0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				DoExportSelected(false);
			}
		});

		SprTable = new Table(mainPage, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
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

		TableColumn tc1 = new TableColumn(SprTable, SWT.RIGHT);
		tc1.setText("1");
		tc1.setWidth(200);
		TableColumn tc2 = new TableColumn(SprTable, SWT.RIGHT);
		tc2.setText("2");
		tc2.setWidth(200);
		TableColumn tc3 = new TableColumn(SprTable, SWT.RIGHT);
		tc3.setText("3");
		tc3.setWidth(200);
		TableColumn tc4 = new TableColumn(SprTable, SWT.RIGHT);
		tc4.setText("4");
		tc4.setWidth(200);

		SprTable.setHeaderVisible(true);
		UpdateTable();
	}

	/**
	 * Update the table.
	 */
	private void UpdateTable() {
		// TODO: bugfix: when exporting with more than 1 column, seems to export incorrect data.
		SprTable.removeAll();
		SprTable.clearAll();

		// dispose of any existing images
		if (Images != null) {
			for (Image i : Images) {
				i.dispose();
			}
			Images.clear();
			Images = null;
		}
		Images = new ArrayList<Image>();

		// get parameters
		int dataPtr = Integer.valueOf(Displacement.getText());
		int addr = BaseAddress + dataPtr;
		int width = Math.max(Integer.valueOf(SpriteWidth.getText()), 1);
		int height = Math.max(Integer.valueOf(SpriteHeight.getText()), 8);

		// default pallette, black and white.
		PaletteData pd = new PaletteData(new RGB[] { new RGB(0, 0, 0), new RGB(255, 255, 255) });

		// One rows worth of data.
		String textbits[] = { "", "", "", "" };
		Image imagebits[] = { null, null, null, null };

		// current column
		int currentCol = 0;

		while (dataPtr < data.length) {
			// start address
			textbits[currentCol] = "$" + String.format("%x", addr + dataPtr) + "\n$";

			/*
			 * Create image from the data.
			 */
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
							imgdata.setPixel(pixX++, y, SpriteRenderer.WHITE);
						} else {
							imgdata.setPixel(pixX++, y, SpriteRenderer.BLACK);
						}
						d = (byte) (d * 2);
					}
				}
			}
			// add the final address into the column text.
			textbits[currentCol] = textbits[currentCol] + String.format("%x", addr + dataPtr - 1);

			// convert the data into an image.
			Image img = new Image(page.getDisplay(), imgdata);

			// scale the image to 60x60
			Image scaled = new Image(page.getDisplay(), DISPLAYIMAGEWIDTH, DISPLAYIMAGEHEIGHT);
			GC gc = new GC(scaled);
			gc.setAntialias(SWT.OFF);
			gc.setInterpolation(SWT.HIGH);
			gc.drawImage(img, 0, 0, img.getBounds().width, img.getBounds().height, 0, 0, DISPLAYIMAGEWIDTH,
					DISPLAYIMAGEHEIGHT);
			gc.dispose();
			
			// dispose of the source image.
			img.dispose();
			img = null;

			// add the scaled image to the image list
			Images.add(scaled);

			// add the image to the column
			imagebits[currentCol] = scaled;

			currentCol++;
			// if we have reached the end of the row
			if (currentCol == 4) {
				// add the row with all four items.
				TableItem item = new TableItem(SprTable, SWT.LEFT);
				item.setImage(imagebits);
				item.setText(textbits);
				currentCol = 0;
				textbits = new String[] { "", "", "", "" };
				imagebits = new Image[] { null, null, null, null };
			}
		}
		// if we have images left over, add them to the table.
		if (currentCol != 0) {
			TableItem item = new TableItem(SprTable, SWT.LEFT);
			item.setImage(imagebits);
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
		if (Images != null) {
			for (Image i : Images) {
				i.dispose();
			}
			Images.clear();
			Images = null;
		}
	}

	/**
	 * 
	 * @param Binary
	 */
	protected void DoExportSelected(boolean Binary) {
		TableItem selecteditems[] = SprTable.getSelection();
		if (selecteditems == null) {
			selecteditems = SprTable.getItems();
		}
		int width = Math.max(Integer.valueOf(SpriteWidth.getText()), 1);
		FileDialog fd = new FileDialog(page.getShell(), SWT.SAVE);
		fd.setText("Save block as binary");
		if (Binary) {
			fd.setFileName("sprites.bin");
		} else {
			fd.setFileName("sprites.asm");
		}
		String[] filterExt = { "*.*" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			FileOutputStream file;
			try {
				file = new FileOutputStream(selected);
				try {
					for (TableItem t : selecteditems) {
						for (int ColNo = 0; ColNo < 4; ColNo++) {
							String texts = t.getText(ColNo);
							String t2[] = texts.split("\n");
							int start = Integer.parseInt(t2[0].substring(1), 16) - BaseAddress;
							int end = Integer.parseInt(t2[1].substring(1), 16) - BaseAddress;
							System.out.println(start+"->"+end+" "+t2[0]+","+t2[1]);
							byte datax[] = new byte[end - start + 1];
							System.arraycopy(data, start, datax, 0, datax.length);
							if (Binary) {
								file.write(datax);
							} else {
								int ptr = 0;
								int col = 0;
								String CurrentLine = "    defb ";
								while (ptr < datax.length) {
									int d = datax[ptr++] & 0xff;
									String binval = Integer.toBinaryString(d);

									String xxx = "00000000".substring(binval.length()) + binval;

									CurrentLine = CurrentLine + xxx + "b";

									col++;
									if (col == width) {
										CurrentLine = CurrentLine + System.lineSeparator();
										file.write(CurrentLine.getBytes());
										CurrentLine = "    defb ";
										col = 0;
									} else {
										CurrentLine = CurrentLine + ", ";
									}
								}
								CurrentLine = CurrentLine + System.lineSeparator();
								if (col != 0) {
									file.write(CurrentLine.getBytes());
								}
							}
							file.write(System.lineSeparator().getBytes());
						}
					}

				} finally {
					file.close();
				}
			} catch (FileNotFoundException e) {
				MessageBox dialog = new MessageBox(page.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Directory not found!");
				dialog.open();
				e.printStackTrace();
			} catch (IOException e) {
				MessageBox dialog = new MessageBox(page.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("IO error: " + e.getMessage());
				dialog.open();
				e.printStackTrace();
			}
		}
	}
}

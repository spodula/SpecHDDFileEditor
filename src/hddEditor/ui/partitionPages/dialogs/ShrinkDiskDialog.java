package hddEditor.ui.partitionPages.dialogs;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.disks.HDD.HardDisk;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.SystemPartition;

public class ShrinkDiskDialog {
	private Display display = null;
	private Shell shell = null;

	// System partition
	private SystemPartition syspart = null;

	// Minimum size of the current partitions.
	private long MinSizeK;

	// Current actual size
	private long CurrSizeK;

	// Minimum cylinder
	private int MinCyl;

	// Current max cyl
	private int CurrCyl;
	
	// Highest partition
	private IDEDosPartition MaxPartition;

	// If file is smaller than the allocated cylinders, display a message, but this
	// flag means it only happens once.
	private boolean ExceedMsg = false;

	// Components.
	private Label DisplayLabel;
	private Table PartList;
	private Text NumCyl;

	//global result
	private boolean result = false;
	/**
	 * Constructor
	 * 
	 * @param display
	 */
	public ShrinkDiskDialog(Display display) {
		this.display = display;
	}

	/**
	 * Show the form
	 * 
	 * @param ExistingPartitions
	 * @return
	 */
	public boolean Show(SystemPartition syspart) {
		this.syspart = syspart;

		CurrCyl = 0;
		MinCyl = 0;
		for (IDEDosPartition part : syspart.partitions) {
			if (part.GetPartType() != PLUSIDEDOS.PARTITION_FREE) {
				MinCyl = Math.max(MinCyl, part.GetEndCyl() + 1);
			}
			if (CurrCyl < part.GetEndCyl()+1) {
				MaxPartition = part; 
				CurrCyl = Math.max(CurrCyl, part.GetEndCyl() + 1);
			}
		}
		System.out.println("Current max cyl:" + CurrCyl);
		System.out.println("Possible Shrink cyl:" + MinCyl);
		int KPerCyl = syspart.CurrentDisk.GetSectorSize() * syspart.CurrentDisk.GetNumHeads()
				* syspart.CurrentDisk.GetNumSectors() / 1024;
		CurrSizeK = (CurrCyl + 1) * KPerCyl;
		MinSizeK = (MinCyl + 1) * KPerCyl;

		System.out.println("Old max size: " + CurrSizeK / 1024 + "M");
		System.out.println("Min size possible: " + MinSizeK / 1024 + "M");

		Createform();
		loop();
		return (result);
	}

	/**
	 * Dialog loop, open and wait until closed.
	 */
	public void loop() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	/**
	 * Create form.
	 */
	private void Createform() {
		shell = new Shell(display);
		shell.setSize(400, 200);
		shell.setText("Shrink or expand disk");
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;
		gridLayout.makeColumnsEqualWidth = true;
		shell.setLayout(gridLayout);

		DisplayLabel = new Label(shell, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 4;
		gridData.widthHint = 600;
		gridData.heightHint = 60;

		DisplayLabel.setLayoutData(gridData);
		ResizeImage();
		DisplayLabel.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent arg0) {
				ResizeImage();
			}
		});

		PartList = new Table(shell, SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = 200;
		PartList.setLayoutData(gd);
		TableColumn tc1 = new TableColumn(PartList, SWT.LEFT);
		TableColumn tc2 = new TableColumn(PartList, SWT.LEFT);
		TableColumn tc3 = new TableColumn(PartList, SWT.LEFT);
		TableColumn tc4 = new TableColumn(PartList, SWT.LEFT);
		TableColumn tc5 = new TableColumn(PartList, SWT.LEFT);
		tc1.setText("Name");
		tc2.setText("size");
		tc3.setText("Type");
		tc4.setText("Start");
		tc5.setText("End");
		tc1.setWidth(150);
		tc2.setWidth(100);
		tc3.setWidth(150);
		tc4.setWidth(150);
		tc5.setWidth(150);
		PartList.setHeaderVisible(true);
		UpdatePartitionList();

		Label lbl = new Label(shell, SWT.NONE);
		lbl.setText("New number of cyls:");
		NumCyl = new Text(shell, SWT.BORDER);
		NumCyl.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		lbl = new Label(shell, SWT.NONE);
		lbl.setText("Min:" + MinCyl + " Max: 32000");

		new Label(shell, SWT.NONE);

		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		Button CancelButton = new Button(shell, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;
		CancelButton.setText("Cancel");
		CancelButton.setLayoutData(gd);
		CancelButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				shell.dispose();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		Button ModifyDiskButton = new Button(shell, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;
		ModifyDiskButton.setText("Modify");
		ModifyDiskButton.setLayoutData(gd);
		ModifyDiskButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoModifyDisk();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		shell.pack();
		NumCyl.setText(String.valueOf(MinCyl));
		ResizeImage();
	}

	private int sectionval[] = { 0xdddddd, 0xff0000, 0x00ff00, 0x0000ff, 0xffff00, 0x00ffff, 0xff00ff, 0xddddfff,
			0x880000, 0x008800, 0x000088, 0x888800, 0x008888, 0x880088, 0x8888888, 0x440000, 0x004400, 0x000044,
			0x444400, 0x004444, 0x440044, 0x4444444, 0xCC0000, 0x00CC00, 0x0000CC, 0xCCCC00, 0x00CCCC, 0xCC00CC,
			0xCCCCCCC, 0xCC8800, 0x00CC88, 0x0088CC, 0xCCCC88, 0x88CCCC, 0xCC88CC };

	/**
	 * Resize the display image.
	 */
	private void ResizeImage() {
		int h = Math.max(70, DisplayLabel.getBounds().height);
		int w = Math.max(600, DisplayLabel.getBounds().width);

		PaletteData palette = new PaletteData(0xFF, 0xFF00, 0xFF0000);
		ImageData imageData = new ImageData(w, h, 24, palette);

		int col[] = new int[w];
		// Preset all colours to "Not allocated"
		for (int i = 0; i < w; i++) {
			col[i] = sectionval[0];
		}

		int currcol = 1;
		int maxcyl = syspart.CurrentDisk.GetNumCylinders();
		for (IDEDosPartition part : syspart.partitions) {
			int Startpos = (part.GetStartCyl() * w) / maxcyl;
			int Endpos = (part.GetEndCyl() * w) / maxcyl;

			for (int i = Startpos; i < Endpos; i++) {
				if (i < w) {
					col[i] = sectionval[currcol% sectionval.length];
				} else {
					if (!ExceedMsg) {
						System.out.println("WARNING: Partition \"" + part.GetName() + "\" exceeds size of file.");
						ExceedMsg = true;
						i = Endpos;
					}
					break;
				}
			}
			currcol++;
		}

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				imageData.setPixel(x, y, col[x]);
			}
		}
		Image img = new Image(display, imageData);
		DisplayLabel.setImage(img);
	}

	/**
	 * 
	 */
	public void close() {
		result = false;
		if (!shell.isDisposed())
			shell.dispose();
	}

	/**
	 * 
	 */
	private void UpdatePartitionList() {
		PartList.removeAll();
		int currcol = 1;
		for (IDEDosPartition part : syspart.partitions) {
			if ((part.GetPartType() != PLUSIDEDOS.PARTITION_UNUSED)) {
				TableItem item = new TableItem(PartList, SWT.NONE);
				String s[] = new String[5];

				s[0] = part.GetName();
				s[1] = GeneralUtils.GetSizeAsString(part.GetSizeK() * 1024);
				s[2] = PLUSIDEDOS.GetTypeAsString(part.GetPartType());
				s[3] = String.valueOf(part.GetStartCyl());
				s[4] = String.valueOf(part.GetEndCyl());
				item.setText(s);

				int rgb = sectionval[currcol++ % sectionval.length];
				int r = rgb & 0xff;
				int g = (rgb / 0x100) & 0xff;
				int b = (rgb / 0x10000) & 0xff;
				Color col = new Color(display, r, g, b);
				try {
					item.setBackground(col);
				} finally {
					col.dispose();
				}
			} else {
				currcol++;
			}
		}
	}

	protected void DoModifyDisk() {
		result = false;
		String error = null;
		int newCyl = 0;
		try {
			newCyl = Integer.valueOf(NumCyl.getText());
		} catch (Exception E) {
			error = "Value " + NumCyl.getText() + " is invalid.";
		}
		if (error == null) {
			if (newCyl < MinCyl) {
				error = "Cannot shrink disk to less than " + MinCyl + " cylinders.";
			}
		}
		if (error == null) {
			//Modify the last partition
			if (MaxPartition.GetPartType() != PLUSIDEDOS.PARTITION_FREE) {
				error = "Highest partition is a not a FREE partition. Cannot resize";
			} else {
				//Modify the last partition
				MaxPartition.SetEndCyl(newCyl);
				//Modify the IDEDOS disk parameters
				syspart.SetNumCyls(newCyl);
				//update the disk.
				syspart.UpdatePartitionListOnDisk();
				
				//Modify the underlying disk parameters
				HardDisk hdd = (HardDisk) syspart.CurrentDisk; 
				try {
					hdd.ResizeDisk(newCyl);
				} catch (IOException e) {
					error = e.getMessage();
				}
			}
		}

		if (error != null) {
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.ERROR);
			messageBox.setMessage(error);
			messageBox.setText("Cannot shrink disk");
			messageBox.open();
			System.out.println(error);
		} else {
			result = true;
			if (!shell.isDisposed())
				shell.dispose();
		}
	}
}

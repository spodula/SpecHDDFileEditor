package hddEditor.ui;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.libs.DiskListLinux;
import hddEditor.libs.DiskListWindows;
import hddEditor.libs.Languages;
import hddEditor.libs.RawDiskItem;


public class fileSelectDevice {
	private RawDiskItem disks[];
	// Form components
	private Display display = null;
	private Shell shell = null;
	private Table DeviceList = null;	
	private File SelectedDevice;
	public int blocksize;
	
	private Languages lang;
	
	public fileSelectDevice(Display display, Languages lang) {
		this.display = display;
		this.lang = lang;
		
		if (System.getProperty("os.name").toUpperCase().contains("LINUX")) {
			DiskListLinux dll = new DiskListLinux(lang);
			this.disks = dll.disks;
		} else {
			DiskListWindows dlw = new DiskListWindows(lang);
			this.disks = dlw.disks;
		}
	}
	
	/**
	 * Show the dialog
	 * 
	 * @param title
	 * @param p3d
	 */
	public File Show() {
		SelectedDevice = null;
		Createform();
		loop();
		return (SelectedDevice);
	}

	/**
	 * Create the components on the form.
	 * 
	 * @param title
	 */
	private void Createform() {
		shell = new Shell(display);
		shell.setSize(400, 300);

		GridLayout gridLayout = new GridLayout(4, true);
		gridLayout.marginLeft = 20;
		gridLayout.marginRight = 20;

		shell.setLayout(gridLayout);
		shell.setText(lang.Msg(Languages.MSG_SELDEVICE));

		DeviceList = new Table(shell, SWT.BORDER | SWT.FULL_SELECTION);
		DeviceList.setLinesVisible(true);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.heightHint = 200;
		gd.widthHint = 500;
		DeviceList.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(DeviceList, SWT.LEFT);
		TableColumn tc2 = new TableColumn(DeviceList, SWT.LEFT);
		TableColumn tc3 = new TableColumn(DeviceList, SWT.LEFT);
		TableColumn tc4 = new TableColumn(DeviceList, SWT.FILL);
		tc1.setText(lang.Msg(Languages.MSG_DEV));
		tc2.setText(lang.Msg(Languages.MSG_CONN));
		tc3.setText(lang.Msg(Languages.MSG_SIZE));
		tc4.setText(lang.Msg(Languages.MSG_DETAILS));
		tc1.setWidth(150);
		tc2.setWidth(75);
		tc3.setWidth(75);
		tc4.setWidth(200);
		DeviceList.setHeaderVisible(true);
		
		for (RawDiskItem disk:disks) {
			TableItem item = new TableItem(DeviceList, SWT.NONE);
			item.setData(disk);
			String content[] = new String[4];
			content[0] = disk.name;
			content[1] = disk.driveType;
			content[2] = disk.GetTextSz();
			content[3] = disk.Vendor+" - "+disk.model;
			item.setText(content);
		}
		
		new Label(shell, SWT.SHADOW_NONE);
		new Label(shell, SWT.SHADOW_NONE);
		
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		
		Button Btn = new Button(shell, SWT.PUSH);
		Btn.setText(lang.Msg(Languages.MSG_SELDEV));
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				TableItem itms[] = DeviceList.getSelection();
				for (TableItem en : itms) {
					SelectedDevice = ((RawDiskItem) en.getData()).dets;
					blocksize = ((RawDiskItem) en.getData()).BlockSize;
				}
				close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		Btn = new Button(shell, SWT.PUSH);
		Btn.setText(lang.Msg(Languages.MSG_CANCEL));
		Btn.setLayoutData(gd);
		Btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		
		shell.pack();

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
		shell.dispose();
	}

	/**
	 * Function so the parent form can force-close the form.
	 */
	public void close() {
		shell.close();
		if (!shell.isDisposed()) {
			shell.dispose();
		}
	}

}

package hddEditor.ui;

import org.eclipse.swt.widgets.Display;

public class DiskCheckTask implements Runnable {
	public HDDEditor rootpage;

	@Override
	public void run() {
		if (rootpage!=null) {
			if ((rootpage.CurrentDisk!= null) && rootpage.CurrentDisk.IsOpen()) {
				if (rootpage.CurrentDisk.DiskOutOfDate()) {
					rootpage.OnDiskOutOfDate();
				}
			}
		}
	
		//Schedule the next check...
		DiskCheckTask dct = new DiskCheckTask();
		dct.rootpage = rootpage;
		Display.getDefault().timerExec(HDDEditor.DISKCHECKPERIOD, dct);
		
	}

}

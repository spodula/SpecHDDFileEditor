package hddEditor.ui.partitionPages.FileRenderers.RawRender;
/**
 * Render the given file as a table of hex values.
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.libs.Languages;

public class BinaryRenderer implements Renderer {
	Table BinTable = null;
	
	@Override
	public void DisposeRenderer() {
		if (BinTable!=null) {
			BinTable.dispose();
			BinTable = null;
		}
	}
	/**
	 * Add the BIN (hex) option
	 * 
	 * @param TargetPage
	 * @param data
	 * @param loadAddr
	 * @param HeightLimit
	 */
	public void Render(Composite TargetPage, byte data[], int loadAddr, int HeightLimit, Languages lang) {
		if (BinTable!=null) {
			BinTable.dispose();
		}

		int AddressLength = String.format("%X", data.length - 1).length();

		BinTable = new Table(TargetPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		BinTable.setLinesVisible(true);

		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = HeightLimit;
		BinTable.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(BinTable, SWT.LEFT);
		tc1.setText(lang.Msg(Languages.MSG_ADDRESS));
		tc1.setWidth(80);
		for (int i = 0; i < 16; i++) {
			TableColumn tcx = new TableColumn(BinTable, SWT.LEFT);
			tcx.setText(String.format("%02X", i));
			tcx.setWidth(30);
		}
		TableColumn tc2 = new TableColumn(BinTable, SWT.LEFT);
		tc2.setText("Ascii");
		tc2.setWidth(160);

		BinTable.setHeaderVisible(true);

		int ptr = 0;
		int numrows = data.length / 16;
		if (data.length % 16 != 0) {
			numrows++;
		}

		int Address = loadAddr;

		Font mono = new Font(TargetPage.getDisplay(), "Monospace", 10, SWT.NONE);
		for (int rownum = 0; rownum < numrows; rownum++) {
			TableItem Row = new TableItem(BinTable, SWT.NONE);

			String asciiLine = "";
			String content[] = new String[18];
			String addr = String.format("%X", Address);
			Address = Address + 16;
			while (addr.length() < AddressLength) {
				addr = "0" + addr;
			}
			content[0] = addr;
			for (int i = 1; i < 17; i++) {
				byte b = 0;
				if (ptr < data.length) {
					b = data[ptr++];
					content[i] = String.format("%02X", (b & 0xff));
				} else {
					content[i] = "--";
				}
				if (b >= 32 && b <= 127) {
					asciiLine = asciiLine + (char) b;
				} else {
					asciiLine = asciiLine + ".";
				}
			}
			content[17] = asciiLine;
			Row.setText(content);
			Row.setFont(mono);
		}
	}

}

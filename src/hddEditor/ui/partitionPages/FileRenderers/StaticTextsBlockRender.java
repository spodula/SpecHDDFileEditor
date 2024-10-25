package hddEditor.ui.partitionPages.FileRenderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.ui.partitionPages.FileRenderers.RawRender.Renderer;

public class StaticTextsBlockRender implements Renderer {
	Table Tbl = null;
	
	@Override
	public void DisposeRenderer() {
		if (Tbl!=null) {
			Tbl.dispose();
			Tbl = null;
		}
	}
	
	public void RenderTexts(Composite mainPage, String labels[], String texts[] ) {
		Tbl = new Table(mainPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		Tbl.setLinesVisible(true);

		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = 400;
		Tbl.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(Tbl, SWT.LEFT);
		tc1.setText("#");
		tc1.setWidth(60);
		TableColumn tc2 = new TableColumn(Tbl, SWT.LEFT);
		tc2.setText("Variable");
		tc2.setWidth(160);
		TableColumn tc3 = new TableColumn(Tbl, SWT.LEFT);
		tc3.setText("Value");
		tc3.setWidth(160);
		Tbl.setHeaderVisible(true);

		for (int cnt=0;cnt<labels.length;cnt++) {
			TableItem Row = new TableItem(Tbl, SWT.NONE);
			String dta[] = new String[3];
			dta[0] = String.format("%d", cnt);
			dta[1] = labels[cnt];
			dta[2] = texts[cnt];
			Row.setText(dta);
		}
	}
}

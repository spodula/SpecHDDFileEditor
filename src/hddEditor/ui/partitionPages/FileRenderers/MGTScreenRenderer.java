package hddEditor.ui.partitionPages.FileRenderers;

import org.eclipse.swt.widgets.Composite;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Languages;
import hddEditor.libs.partitions.mgt.MGTDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.SamScreenRenderer;

public class MGTScreenRenderer extends FileRenderer {
	SamScreenRenderer renderer = null;

	public void RenderScreen(Composite mainPage, byte data[], String Filename, MGTDirectoryEntry entry , FileSelectDialog filesel, Languages lang) {
		super.Render(mainPage, data, Filename, filesel, lang);

		
		renderer = new SamScreenRenderer();
		renderer.RenderScreen(mainPage, data);
	}	

}

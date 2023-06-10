package hddEditor.ui.partitionPages.FileRenderers;

import org.eclipse.swt.widgets.Composite;

import hddEditor.libs.partitions.mgt.MGTDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.MGT48kRenderer;

public class MGT48kSnapshotRenderer extends FileRenderer {
	MGT48kRenderer renderer = null;

	public void RenderSnapshot(Composite mainPage, byte data[], String Filename, MGTDirectoryEntry entry ) {
		this.filename = Filename;
		this.MainPage = mainPage;
		this.data = data;
		
		renderer = new MGT48kRenderer();
		renderer.Render(mainPage, data, entry);
	}	
}

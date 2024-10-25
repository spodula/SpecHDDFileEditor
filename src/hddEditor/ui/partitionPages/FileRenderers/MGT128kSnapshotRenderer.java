package hddEditor.ui.partitionPages.FileRenderers;

import org.eclipse.swt.widgets.Composite;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.partitions.mgt.MGTDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.MGT128KRenderer;

public class MGT128kSnapshotRenderer extends FileRenderer {
	MGT128KRenderer renderer = null;

	public void RenderSnapshot(Composite mainPage, byte data[], String Filename, MGTDirectoryEntry entry , FileSelectDialog filesel) {
		super.Render(mainPage, data, Filename, filesel);

		renderer = new MGT128KRenderer();
		renderer.Render(mainPage, data, entry);
	}	
}

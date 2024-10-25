package hddEditor.ui.partitionPages.FileRenderers.RawRender;
/**
 * Base class for rendering.
 *  
 * These classes are responsible for allocating and de-allocating their own
 * components. DisposeRenderer should be populated to do this. 
 * 
 * These have been seperated out because we are starting to deal with complex file types like
 * Snapshot files, which may contain an image, a BASIC program and CODE.
 * 
 * @author graham
 *
 */

public interface Renderer {
	public void DisposeRenderer();
}

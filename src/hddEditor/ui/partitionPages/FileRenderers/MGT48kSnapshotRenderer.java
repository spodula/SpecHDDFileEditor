package hddEditor.ui.partitionPages.FileRenderers;
/**
 * from :https://web.archive.org/web/20080514125600/http://www.ramsoft.bbk.org/tech/mgt_tech.txt
 * 48K SNAPSHOT (type 5)
 * ---------------------
 * 211-219  Not used
 * 220-255  Z80 registers (in words) in the following order:
 *           IY IX DE' BC' HL' AF' DE BC HL I SP (see below for R and AF)
 *
 *        Register I is in the MSB of the corrisponding word (byte offset 239),
 *        so that it is loaded with:
 *
 *           POP AF
 *          LD I,A
 *
 *        The Interrupt Mode is desumed by the value of the I register: if
 *        it contains 00h or 3Fh then IM1 is assumed, else IM2 is set.
 *        The IFF2 status (IFF1=IFF2) is retrieved from the P/V bit of the
 *        flag register F.
 *
 *         SP is actually SP-6, because the original stack is "corrupted"
 *         with the following 6 bytes (in ascending order):
 *
 *           R AF PC  ( ----> decreasing stack )
 *          |     |
 *          SP    SP+6 (original SP)
 *
 *        (R is in the MSB of the corresponding word) so that the return code
 *        could be something like this (actually it is a bit more complex):
 *
 *          POP AF
 *          LD R,A
 *          POP AF
 *          RET
 */

import org.eclipse.swt.widgets.Composite;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Languages;
import hddEditor.libs.partitions.mgt.MGTDirectoryEntry;
import hddEditor.ui.partitionPages.FileRenderers.RawRender.MGT48kRenderer;

public class MGT48kSnapshotRenderer extends FileRenderer {
	MGT48kRenderer renderer = null;

	public void RenderSnapshot(Composite mainPage, byte data[], String Filename, MGTDirectoryEntry entry , FileSelectDialog filesel, Languages lang) {
		super.Render(mainPage, data, Filename, filesel, lang);

		
		renderer = new MGT48kRenderer();
		renderer.Render(mainPage, data, entry, lang);
	}	
}

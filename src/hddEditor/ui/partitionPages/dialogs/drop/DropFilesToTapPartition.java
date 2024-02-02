package hddEditor.ui.partitionPages.dialogs.drop;
/**
 * Extra bits for TAP files (And MDF files and TRD files)
 */


import org.eclipse.swt.widgets.Display;

public class DropFilesToTapPartition extends GenericDropForm {

	public DropFilesToTapPartition(Display display) {
		super(display);
	}
	
	@Override
	/**
	 * For TAP files, just need to make sure the name isnt too long.
	 */
	protected String UniqueifyName(String s) {
		s = s +"            ";
		s = s.substring(0,10);		
		return(s.trim());
	}

}

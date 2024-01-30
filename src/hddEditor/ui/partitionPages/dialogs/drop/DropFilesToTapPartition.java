package hddEditor.ui.partitionPages.dialogs.drop;

import org.eclipse.swt.widgets.Display;

public class DropFilesToTapPartition extends GenericDropForm {

	public DropFilesToTapPartition(Display display) {
		super(display);
		// TODO Auto-generated constructor stub
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

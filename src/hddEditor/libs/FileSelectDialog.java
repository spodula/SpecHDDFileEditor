package hddEditor.libs;

import java.io.File;

import javax.swing.JFileChooser;

/**
 * This is a hack to get around the fact that SWT file dialogs seem to not remember
 * their previous locations. This is here until a better solution comes along.
 * 
 * Swing dialogs are very ugly though. :(
 */

public class FileSelectDialog {
	public static int FILETYPE_DRIVE = 0;
	public static int FILETYPE_EXPORT = 1;
	public static int FILETYPE_FILES = 2;
	public static int FILETYPE_IMPORTDRIVE = 3;
	
	public static int NUMDIRTYPES = 4;
	
	public File[] DefaultFolders = null;

	public FileSelectDialog() {
		DefaultFolders = new File[NUMDIRTYPES];
		File f = new File(".");
		for (int i=0;i<NUMDIRTYPES;i++) {
			DefaultFolders[i] = f;
		}
	}
	
	/**
	 * 
	 * @param filetype
	 */
	public void SetDefaultFolderForType(int filetype, File f) {
		if (filetype < NUMDIRTYPES) {
			//if we have been passed a filename, select the directory.
			if (f.isFile()) {
				f = f.getParentFile();
			}
			DefaultFolders[filetype] = f;
		}
	}
	
	/**
	 * 
	 * @param filetype
	 * @return
	 */
	public File GetDefaultFolderForType(int filetype) {
		if (filetype < NUMDIRTYPES) {
			return(DefaultFolders[filetype]);
		}
		return(null);
	}
	
		
	/**
	 * Ask for and return a single file.
	 * @param filetype
	 * @return
	 */
	public File AskForSingleFileOpen(int filetype, String title) {
		JFileChooser chooser = new JFileChooser(DefaultFolders[filetype].getAbsolutePath());
		chooser.setDialogTitle(title);
		chooser.setMultiSelectionEnabled(true);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			DefaultFolders[filetype] = chooser.getSelectedFile().getParentFile();
			return ( chooser.getSelectedFile() );
		}
		return(null);
	}
	/**
	 * Ask for and return a single file.
	 * @param filetype
	 * @param title
	 * @return
	 */
	public File AskForSingleFileSave(int filetype, String title) {
		JFileChooser chooser = new JFileChooser(DefaultFolders[filetype].getAbsolutePath());
		chooser.setDialogTitle(title);
		int returnVal = chooser.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			DefaultFolders[filetype] = chooser.getSelectedFile().getParentFile();
			return ( chooser.getSelectedFile() );
		}
		return(null);
	}
	
	/**
	 * Ask for multiple files to open.
	 * 
	 * @param filetype
	 * @param title
	 * @return
	 */
	public File[] AskForMultipleFileOpen(int filetype, String title) {
		JFileChooser chooser = new JFileChooser(DefaultFolders[filetype].getAbsolutePath());
		chooser.setDialogTitle(title);
		chooser.setMultiSelectionEnabled(true);
		int returnVal = chooser.showOpenDialog(null);
		File files[] = chooser.getSelectedFiles();
		if (returnVal == JFileChooser.APPROVE_OPTION && files.length > 0) {
			DefaultFolders[filetype] = files[0].getParentFile();
			return (files);
		}
		return(null);
	}
	
}

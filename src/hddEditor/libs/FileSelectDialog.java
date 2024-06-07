package hddEditor.libs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

	//Used to store the current defaults file location.
	private File Defaults = null;
	
	public FileSelectDialog() {
		Defaults = new File(".","folders.defaults");
		System.out.println("Using "+Defaults.getAbsolutePath()+" for defaults file.");
		DefaultFolders = new File[NUMDIRTYPES];
		File f = new File(".");
		for (int i=0;i<NUMDIRTYPES;i++) {
			DefaultFolders[i] = f;
		}
		LoadDefaults();
	}
	
	/**
	 * Load the folder.defaults file.
	 */
	private void LoadDefaults() {
		if (Defaults.exists() && Defaults.isFile()) {
			byte result[] = new byte[(int) Defaults.length()];
			InputStream inputStream;
			try {
				inputStream = new FileInputStream(Defaults);
				try {
					inputStream.read(result);
				} finally {
					inputStream.close();
				}
				String defFile = new String(result);
				int entrynum=0;
				String entries[] = defFile.split("\n");
				for (String entry:entries) {
					if (entrynum < NUMDIRTYPES) {
						DefaultFolders[entrynum++] = new File(entry.trim());
					}
				}
			} catch (IOException E){
				//just eat any exception
				System.out.println("No folder defaults found.");
			}
		}		
	}
	
	/**
	 * @throws IOException 
	 * 
	 */
	public void SaveDefaults() {
		try {
			OutputStream outputStream = new FileOutputStream(Defaults);
			try {
				for (File f: DefaultFolders) {
					String entry = f.getAbsolutePath()+System.lineSeparator();
					outputStream.write(entry.getBytes());
				}
			} finally {
				outputStream.close();
			}
		} catch (FileNotFoundException e) {
			System.out.println("Failed to save folder defaults. ("+e.getMessage()+")");
		} catch (IOException e) {
			System.out.println("Failed to save folder defaults. ("+e.getMessage()+")");
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

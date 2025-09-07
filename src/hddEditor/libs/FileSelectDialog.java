package hddEditor.libs;
/**
 * This is a hack to get around the fact that SWT file dialogs seem to not
 * remember their previous locations. This is here until a better solution comes
 * along.
 * 
 * Swing dialogs are very ugly though. :(
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JFileChooser;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.FileDialog;



public class FileSelectDialog {
	public static int FILETYPE_DRIVE = 0;
	public static int FILETYPE_EXPORT = 1;
	public static int FILETYPE_FILES = 2;
	public static int FILETYPE_IMPORTDRIVE = 3;
	public static int NUMDIRTYPES = 4;

	public static int DIALOGTYPE_SWT = 0;
	public static int DIALOGTYPE_SWING = 1;

	// Stores what dialog type to use
	public int DialogType = 0;

	// stores the current default folders for each file type
	private File[] DefaultFolders = null;

	// Used to store the current defaults file location.
	private File Defaults = null;

	private Shell shell = null;
	
	/**
	 * 
	 */
	public FileSelectDialog(Shell shell) {
		this.DialogType = DIALOGTYPE_SWT;
		this.shell = shell;
		this.Defaults = new File(".", "folders.defaults");
		System.out.println("Using " + Defaults.getAbsolutePath() + " for defaults file.");
		this.DefaultFolders = new File[NUMDIRTYPES];
		File f = new File(".");
		for (int i = 0; i < NUMDIRTYPES; i++) {
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
				int entrynum = 0;
				String entries[] = defFile.split("\n");
				for (String entry : entries) {
					if (entrynum < NUMDIRTYPES) {
						DefaultFolders[entrynum++] = new File(entry.trim());
					}
				}
			} catch (IOException E) {
				// just eat any exception
				System.out.println("No folder defaults found.");
			}
		}
	}

	/**
	 * Save the default file types to the defaults file.
	 * 
	 * @throws IOException
	 * 
	 */
	public void SaveDefaults() {
		try {
			OutputStream outputStream = new FileOutputStream(Defaults);
			try {
				for (File f : DefaultFolders) {
					String entry = f.getAbsolutePath() + System.lineSeparator();
					outputStream.write(entry.getBytes());
				}
			} finally {
				outputStream.close();
			}
		} catch (FileNotFoundException e) {
			System.out.println("Failed to save folder defaults. (" + e.getMessage() + ")");
		} catch (IOException e) {
			System.out.println("Failed to save folder defaults. (" + e.getMessage() + ")");
		}
	}

	/**
	 * Set the default folder for a given type
	 * 
	 * @param filetype
	 */
	public void SetDefaultFolderForType(int filetype, File f) {
		if (filetype < NUMDIRTYPES) {
			// if we have been passed a filename, select the directory.
			if (f.isFile()) {
				f = f.getParentFile();
			}
			DefaultFolders[filetype] = f;
		}
	}

	/**
	 * get the default folder for a given type
	 * 
	 * @param filetype
	 * @return
	 */
	public File GetDefaultFolderForType(int filetype) {
		if (filetype < NUMDIRTYPES) {
			return (DefaultFolders[filetype]);
		}
		return (null);
	}

	/**
	 * Ask for and return a single file.
	 * 
	 * @param filetype
	 * @return
	 */
	public File AskForSingleFileOpen(int filetype, String title, String[] supportedfiltypes, String defaultFilename) {
		if (DialogType == DIALOGTYPE_SWING) {
			JFileChooser chooser = new JFileChooser(DefaultFolders[filetype].getAbsolutePath());
			chooser.setDialogTitle(title);
			chooser.setMultiSelectionEnabled(true);
			int returnVal = chooser.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				DefaultFolders[filetype] = chooser.getSelectedFile().getParentFile();
				return (chooser.getSelectedFile());
			}
		} else {
			String search = "*.*";
			String replace = "*";
			if (System.getProperty("os.name").toUpperCase().contains("WIN")) {
				search = "*";
				replace = "*.*";
			}
			for (int i=0;i<supportedfiltypes.length;i++) {
				if (supportedfiltypes[i].equals(search)) {
					supportedfiltypes[i] = replace;
				}
			}
			
			FileDialog fd = new FileDialog(shell, SWT.OPEN);
			fd.setText(title);
			
			fd.setFilterExtensions(supportedfiltypes);
			fd.setFilterPath(DefaultFolders[filetype].getAbsolutePath());
			fd.setFileName(defaultFilename);
			String selected = fd.open();
			if (selected != null) {
				return(new File(selected));
			}
		}
		return (null);
	}

	/**
	 * Ask for and return a single file.
	 * 
	 * @param filetype
	 * @param title
	 * @return
	 */
	public File AskForSingleFileSave(int filetype, String title, String[] extension, String defaultFilename) {
		if (DialogType == DIALOGTYPE_SWING) {
			JFileChooser chooser = new JFileChooser(DefaultFolders[filetype].getAbsolutePath());
			chooser.setDialogTitle(title);
			int returnVal = chooser.showSaveDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				DefaultFolders[filetype] = chooser.getSelectedFile().getParentFile();
				return (chooser.getSelectedFile());
			}
		} else {
						
			FileDialog fd = new FileDialog(shell, SWT.SAVE);
			fd.setText(title);
			fd.setFilterExtensions(extension);
			fd.setFilterPath(DefaultFolders[filetype].getAbsolutePath());
			fd.setFileName(defaultFilename.trim());
			
			String selected = fd.open();
			if (selected != null) {
				return(new File(selected));
			}
		}
		return (null);
	}

	/**
	 * Ask for multiple files to open.
	 * 
	 * @param filetype
	 * @param title;
	 * @return
	 */
	public File[] AskForMultipleFileOpen(int filetype, String title, String[] filetypes) {
		if (DialogType == DIALOGTYPE_SWING) {
			JFileChooser chooser = new JFileChooser(DefaultFolders[filetype].getAbsolutePath());
			chooser.setDialogTitle(title);
			chooser.setMultiSelectionEnabled(true);
			int returnVal = chooser.showOpenDialog(null);
			File files[] = chooser.getSelectedFiles();
			if (returnVal == JFileChooser.APPROVE_OPTION && files.length > 0) {
				DefaultFolders[filetype] = files[0].getParentFile();
				return (files);
			}
		} else {
			String search = "*.*";
			String replace = "*";
			if (System.getProperty("os.name").toUpperCase().contains("WIN")) {
				search = "*";
				replace = "*.*";
			}
			for (int i=0;i<filetypes.length;i++) {
				if (filetypes[i].equals(search)) {
					filetypes[i] = replace;
				}
			}

			FileDialog fd = new FileDialog(shell, SWT.OPEN|SWT.MULTI);
			fd.setText(title);
			fd.setFilterExtensions(filetypes);
			fd.setFilterPath(DefaultFolders[filetype].getAbsolutePath());
			String selected = fd.open();
			if (selected != null) {
				String filenames[] = fd.getFileNames();
				File retarray[] = new File[filenames.length];
				int retptr=0;
				String folder = fd.getFilterPath();
				for(String filename:filenames) {
					retarray[retptr++] = new File(folder,filename);
				}
				return(retarray);
			}
		}
		return (null);
	}

}

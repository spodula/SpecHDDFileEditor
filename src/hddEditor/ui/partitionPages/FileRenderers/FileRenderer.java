package hddEditor.ui.partitionPages.FileRenderers;
/**
 * This provides some helper functions for the rendering pages. 
 */

import java.io.File;

//BUGFIX: GDS 22/01/2023 - DoSaveFileAsHex: Was only saving from index 128 only. Hangover from the old system. Fixed 
//QOLFIX: GDS 22/01/2023 - DoSaveFileAsHex/DoSaveFileAsBin: Now defaults to the current filename and puts in title bar.
//BUGFIX: GDS 22/01/2023 - DoSaveFileAsHex: Will now stop at "filesize" rather than data.length 


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;

import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.Languages;

public class FileRenderer {
	//Storage for the file details
	protected String filename="";
	public byte data[] = null;
	public Composite MainPage =null;
	
	protected FileSelectDialog filesel = null;
	
	protected Languages lang;
	
	/**
	 * Generic constructor. 
	 * 
	 * @param mainPage
	 * @param data
	 * @param Filename
	 */
	public void Render(Composite mainPage, byte data[], String Filename, FileSelectDialog filesel, Languages lang) {
		this.filename = Filename;
		this.MainPage = mainPage;
		this.data = data;
		this.filesel = filesel;
		this.lang = lang;
	}

	/**
	 * Save file as Hex. 
	 * 
	 * @param data
	 * @param mainPage
	 * @param loadaddr
	 * @param filesize
	 * @param Origfilename
	 */
	public void DoSaveFileAsHex(byte[] data, Composite mainPage, int loadaddr, int filesize, String Origfilename) {
		File Selected = filesel.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES,String.format (lang.Msg(Languages.MSG_SAVEXASHEX),Origfilename), new String[] {"*.*"},filename);
		
		if (Selected != null) {
			FileOutputStream file;
			try {
				file = new FileOutputStream(Selected);
				try {
					file.write((lang.Msg(Languages.MSG_FILE) + ": " + filename + System.lineSeparator()).getBytes());
					file.write(("Org: " + loadaddr + System.lineSeparator()).getBytes());
					file.write((lang.Msg(Languages.MSG_LENGTH) + ": " + filesize+ System.lineSeparator() + System.lineSeparator()).getBytes());
					int AddressLength = String.format("%X", data.length - 1).length();
					int ptr = 0;
					int numrows = filesize / 16;
					if (filesize % 16 != 0) {
						numrows++;
					}
					int Address = loadaddr;

					for (int rownum = 0; rownum < numrows; rownum++) {
						String asciiLine = "";
						String addr = String.format("%X", Address);
						Address = Address + 16;
						while (addr.length() < AddressLength) {
							addr = "0" + addr;
						}
						file.write((addr+"\t").getBytes());
						for (int i = 1; i < 17; i++) {
							if (ptr < filesize) {
								byte bData = data[ptr++];
								file.write(String.format("%02X ", (bData & 0xff)).getBytes());
								if (bData >= 32 && bData <= 127) {
									asciiLine = asciiLine + (char) bData;
								} else {
									asciiLine = asciiLine + ".";
								}
							} else {
								file.write("-- ".getBytes());
								asciiLine = asciiLine + ".";
							}
						}
						file.write(("\t"+asciiLine+System.lineSeparator()).getBytes());
					}
					
					
				} finally {
					file.close();
				}
			} catch (FileNotFoundException e) {
				MessageBox dialog = new MessageBox(MainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText(lang.Msg(Languages.MSG_ERRSAVING));
				dialog.setMessage(lang.Msg(Languages.MSG_DIRNOTFOUND));
				dialog.open();

				e.printStackTrace();
			} catch (IOException e) {
				MessageBox dialog = new MessageBox(MainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText(lang.Msg(Languages.MSG_ERRSAVING));
				dialog.setMessage(lang.Msg(Languages.MSG_IOERROR) + ": " + e.getMessage());
				dialog.open();
				e.printStackTrace();
			}
		}
		mainPage.getShell().moveAbove(null);

	}

	/**
	 * Save file as raw binary.
	 * 
	 * @param data
	 * @param mainPage
	 * @param Origfilename
	 */
	protected void DoSaveFileAsBin(byte data[], Composite mainPage, String Origfilename) {
		File Selected = filesel.AskForSingleFileSave(FileSelectDialog.FILETYPE_FILES, String.format (lang.Msg(Languages.MSG_SAVEXASBIN),Origfilename), new String[] {"*.*"},filename);
		
		if (Selected != null) {
			FileOutputStream file;
			try {
				file = new FileOutputStream(Selected);
				try {
					System.out.println(lang.Msg(Languages.MSG_WRITING)+" "+ Selected + " from: 0 len: " + data.length);
					file.write(data, 0, data.length);
				} finally {
					file.close();
				}
			} catch (FileNotFoundException e) {
				MessageBox dialog = new MessageBox(mainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText(lang.Msg(Languages.MSG_ERRSAVING));
				dialog.setMessage(lang.Msg(Languages.MSG_DIRNOTFOUND));
				dialog.open();

				e.printStackTrace();
			} catch (IOException e) {
				MessageBox dialog = new MessageBox(mainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText(lang.Msg(Languages.MSG_ERRSAVING));
				dialog.setMessage(lang.Msg(Languages.MSG_IOERROR) + ": " + e.getMessage());
				dialog.open();

				e.printStackTrace();
			}
		}
	}

}

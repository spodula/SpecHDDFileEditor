package hddEditor.ui.partitionPages.FileRenderers;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;

import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;

public class FileRenderer {
	String filename="";
	byte data[] = null;
	Composite MainPage =null;
	
	public void Render(Composite mainPage, byte data[], String Filename) {
		this.filename = Filename;
		this.MainPage = mainPage;
		this.data = data;
	}
	
	
	public void DoSaveFileAsHex(byte[] data, Composite mainPage, Plus3DosFileHeader p3d) {
		FileDialog fd = new FileDialog(MainPage.getShell(), SWT.SAVE);
		fd.setText("Save Assembly file as");
		String[] filterExt = { "*.*" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			FileOutputStream file;
			try {
				file = new FileOutputStream(selected);
				try {
					file.write(("File: " + filename + System.lineSeparator()).getBytes());
					file.write(("Org: " + p3d.loadAddr + System.lineSeparator()).getBytes());
					file.write(("Length: " + p3d.fileSize + System.lineSeparator() + System.lineSeparator()).getBytes());
					int AddressLength = String.format("%X", data.length - 1).length();
					int ptr = 128;
					int numrows = data.length / 16;
					if (data.length % 16 != 0) {
						numrows++;
					}
					int Address = p3d.loadAddr;

					for (int rownum = 0; rownum < numrows; rownum++) {
						String asciiLine = "";
						String addr = String.format("%X", Address);
						Address = Address + 16;
						while (addr.length() < AddressLength) {
							addr = "0" + addr;
						}
						file.write((addr+"\t").getBytes());
						for (int i = 1; i < 17; i++) {
							if (ptr < data.length) {
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
				dialog.setText("Error saving file");
				dialog.setMessage("Directory not found!");
				dialog.open();

				e.printStackTrace();
			} catch (IOException e) {
				MessageBox dialog = new MessageBox(MainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("IO error: " + e.getMessage());
				dialog.open();
				e.printStackTrace();
			}
		}
	}


	protected void DoSaveFileAsBin(byte data[], Composite mainPage, boolean InclHeader, Plus3DosFileHeader p3d) {
		FileDialog fd = new FileDialog(mainPage.getShell(), SWT.SAVE);
		fd.setText("Save Binary file as");
		String[] filterExt = { "*.*" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			FileOutputStream file;
			try {
				file = new FileOutputStream(selected);
				try {
					int ptr = 0x80;
					int len = p3d.filelength;
					if (InclHeader) {
						ptr = 0;
						len = data.length;
					}
					System.out.println("Writing " + selected + " from: " + ptr + " len: " + len);
					file.write(data, ptr, len);
				} finally {
					file.close();
				}
			} catch (FileNotFoundException e) {
				MessageBox dialog = new MessageBox(mainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Directory not found!");
				dialog.open();

				e.printStackTrace();
			} catch (IOException e) {
				MessageBox dialog = new MessageBox(mainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("IO error: " + e.getMessage());
				dialog.open();

				e.printStackTrace();
			}
		}
	}

}

package hddEditor.ui.partitionPages.FileRenderers;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.GeneralUtils;
import hddEditor.libs.Speccy;
import hddEditor.libs.partitions.cpm.Plus3DosFileHeader;

public class BasicRenderer extends FileRenderer {
	//Components
	private Text StartLineEdit = null;
	private Text VariableStartEdit = null;
	private Table Listing = null;
	private Table Variables = null;

	/**
	 * Render the page to the composite
	 */
	@Override
	public void Render(Composite mainPage, byte data[], String Filename) {
		super.Render(mainPage, data, Filename);

		Plus3DosFileHeader p3d = new Plus3DosFileHeader(data);

		Label lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("BASIC program: ");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Start line: ");

		StartLineEdit = new Text(mainPage, SWT.NONE);
		StartLineEdit.setText("");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		StartLineEdit.setLayoutData(gd);

		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Variable start: ");

		VariableStartEdit = new Text(mainPage, SWT.NONE);
		VariableStartEdit.setText("");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth = 50;
		VariableStartEdit.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 200;

		Button btn = new Button(mainPage, SWT.NONE);
		btn.setText("Extract file as text");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsText(mainPage);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		btn = new Button(mainPage, SWT.NONE);
		btn.setText("Extract file as Binary");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsBin(data, mainPage, false, p3d);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});

		btn = new Button(mainPage, SWT.NONE);
		btn.setText("Extract file as Binary Inc Header");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsBin(data, mainPage, true, p3d);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});
		
		btn = new Button(mainPage, SWT.NONE);
		btn.setText("Extract file as Hex");
		btn.setLayoutData(gd);
		btn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DoSaveFileAsHex(data, mainPage, p3d);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}
		});


		Listing = new Table(mainPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		Listing.setLinesVisible(true);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.heightHint = 300;
		gd.widthHint = mainPage.getSize().x;
		Listing.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(Listing, SWT.LEFT);
		tc1.setText("Line number");
		tc1.setWidth(80);
		TableColumn tc2 = new TableColumn(Listing, SWT.FILL);
		tc2.setText("Line");
		tc2.setWidth(600);

		int ptr = 128; // skip the file header
		int EndOfBasicArea = Math.min(data.length, p3d.VariablesOffset + 128);
		while (ptr < EndOfBasicArea) {
			int linenum = ((data[ptr++] & 0xff) * 256);
			linenum = linenum + (data[ptr++] & 0xff);
			int linelen = (int) data[ptr++] & 0xff;
			linelen = linelen + ((int) (data[ptr++] & 0xff) * 256);

			if (ptr >= p3d.VariablesOffset + 0x80) {
				// now into the variables area. Ignoring for the moment.
				ptr = data.length;
			} else {
				String sixdigit = String.valueOf(linenum);
				while (sixdigit.length() < 6) {
					sixdigit = sixdigit + " ";
				}

				byte line[] = new byte[linelen];
				for (int i = 0; i < linelen; i++) {
					line[i] = data[ptr + i];
				}

				StringBuilder sb = new StringBuilder();
				Speccy.DecodeBasicLine(sb, line, 0, linelen, false);

				// point to next line.
				ptr = ptr + linelen;

				String details[] = new String[2];
				details[0] = sixdigit;
				details[1] = sb.toString();

				TableItem Row = new TableItem(Listing, SWT.NONE);
				Row.setText(details);
			}
		}
		StartLineEdit.setText(String.valueOf(p3d.line));
		VariableStartEdit.setText(String.valueOf(p3d.VariablesOffset));
		
		lbl = new Label(mainPage, SWT.NONE);
		lbl.setText("Variables: ");
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		lbl.setLayoutData(gd);

		Variables = new Table(mainPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		Variables.setLinesVisible(true);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.heightHint = 100;
		gd.widthHint = mainPage.getSize().x;
		Variables.setLayoutData(gd);

		TableColumn vc1 = new TableColumn(Variables, SWT.LEFT);
		vc1.setText("Variable");
		vc1.setWidth(80);
		TableColumn vc2 = new TableColumn(Variables, SWT.FILL);
		vc2.setText("Type");
		vc2.setWidth(80);
		TableColumn vc3 = new TableColumn(Variables, SWT.FILL);
		vc3.setText("Content");
		vc3.setWidth(600);

		DecodeVariables(mainPage, data, p3d);
		
		mainPage.pack();

	}

	/**
	 * Save file as text.
	 */
	protected void DoSaveFileAsText(Composite mainPage) {
		FileDialog fd = new FileDialog(mainPage.getShell(), SWT.SAVE);
		fd.setText("Save file as");
		String[] filterExt = { "*.*" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			PrintWriter file;
			try {
				file = new PrintWriter(selected, "UTF-8");
				try {
					for (int line = 0; line < Listing.getItemCount(); line++) {
						TableItem itm = Listing.getItem(line);
						String lineno = itm.getText(0);
						String content = itm.getText(1);
						file.write(lineno.trim() + " ");
						file.write(content.trim() + System.lineSeparator());
					}
				} finally {
					file.close();
				}
			} catch (FileNotFoundException e) {
				MessageBox dialog = new MessageBox(mainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Directory not found!");
				dialog.open();

				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				MessageBox dialog = new MessageBox(mainPage.getShell(), SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error saving file");
				dialog.setMessage("Internal error, cannot write UTF-8?");
				dialog.open();

				e.printStackTrace();
			}

		}
	}
	

	/**
	 * Decode the variables
	 * 
	 * @param mainPage
	 * @param file
	 * @param header
	 */
	private void DecodeVariables(Composite mainPage, byte[] file, Plus3DosFileHeader header) {
		int ptr = 0x80; // skip the file header
		ptr = ptr + header.VariablesOffset;
		if (ptr >= (header.filelength+0x80)) {
			TableItem Row = new TableItem(Variables, SWT.NONE);
			Row.setText(new String[] {"No Variables","",""});
		} else {
			while (ptr < header.filelength+0x80) {
				int var = (int) (file[ptr++] & 0xff);
				int vartype = var / 0x20;
			//	char c = (char) ((var & 0x1f) + 0x60);
				
				if (vartype == 0x00) {
					//anything after this marker is junk so just skip it.
					TableItem Row = new TableItem(Variables, SWT.NONE);
					Row.setText(new String[] {"End of variables","",""});
					ptr =  header.filelength+0x80;
				} else if (vartype == 1) {
					TableItem Row = new TableItem(Variables, SWT.NONE);
					Row.setText(new String[] {"Unknown type","",""});
					ptr = header.filelength+0x80;
				} else if (vartype == 2) { // string
					ptr = VariableType2(ptr, var, file );
				} else if (vartype == 3) { // number (1 letter)
					ptr = VariableType3(ptr, var, file );
				} else if (vartype == 4) { // Array of numbers
					ptr = VariableType4(ptr, var, file );
				} else if (vartype == 5) { // Number who's name is longer than 1 letter
					ptr = VariableType5(ptr, var, file );
				} else if (vartype == 6) { // array of characters
					ptr = VariableType6(ptr, var, file );
				} else if (vartype == 7) { // for/next control variable
					ptr = VariableType7(ptr, var, file );
				} else {
					System.out.print("UNKNOWN! $" + Integer.toHexString(var)+" at "+ptr);
				}				
				
			}
		}
		
	}

	/**
	 * Handler for type 7 variables (FOR/NEXT variables)
	 * Variable name is parsed from the original marker (first 5 bytes) + 0x60
	 *  format is:
	 *  byte:
	 *  [0-4] 	Current Value of variable (Speccy FP representation)
	 *  [5-9] 	TO value  (Speccy FP representation)
	 *  [10-14] STEP value (Speccy FP representation)
	 *  [15] statement within FOR line to start looping from. (integer)
	 * 
	 * Like the previous ones, this is similar to the ZX81 8k Rom equivelent, except the addition of byte 15
	 * (As the zx81 does not allow multiple statements on a line) 
	 * 
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int  VariableType7(int Address, int chr, byte[] file ) {
		int varname = (chr & 0x1f);
		varname = varname +0x60;
		String txt = "Value=" + String.valueOf(Speccy.GetNumberAtByte(file, Address));
		Address = Address + 5;
		txt = txt + " Limit=" + String.valueOf(Speccy.GetNumberAtByte(file, Address));
		Address = Address + 5;
		txt = txt + " Step=" + String.valueOf(Speccy.GetNumberAtByte(file, Address));
		Address = Address + 5;
		
		int lsb = (file[Address++] & 0xff);
		int msb = (file[Address++] & 0xff);
		int line =  (msb*256) + lsb;
		txt = txt + " Loop line=" + String.valueOf(line);
		
		int statement = (file[Address++] & 0xff);
		
		txt = txt + " Next Statement in line: "+ String.valueOf(statement);
		
		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = (char)varname+"";
		row[1] = "For/Next variable";
		row[2] = txt;
		Row.setText(row);
		
		return(Address);
	}

	/**
	 * Handler for type 6 variables (Character arrays)
	 * Variable name is parsed from the original marker (first 5 bytes) + 0x60
	 * format is:
	 *  [0-1] data length (Used to quickly skip over variable when searching)
	 *  [2] Number of dimensions (1-255)
	 *  [3.4] First dimension size.(1-65535)
	 *  ..
	 *  [xx.yy] last dimension size
	 *  [z] char for (1,0)
	 *  [z] char for (2,0)
	 *  ... 
	 *  [z] char for (<sz>,0)
	 *  [z] char for (1,1)  
	 *  and so on.
	 *  
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int  VariableType6(int Address, int chr, byte[] file ) {
		int varname = (chr & 0x1f);
		varname = varname +0x60;
		
		System.out.println(GeneralUtils.HexDump(file, Address, file.length-Address));
		
		String txt = "(";
		Address = Address + 2;
		int dimensions =  (file[Address++] & 0xff);
		
		int dims[] = new int[dimensions];
		int dimcounts[] = new int[dimensions];
		for (int i = 0; i < dimensions; i++) {
			int lsb = (file[Address++] & 0xff);
			int msb = (file[Address++] & 0xff);
			dims[i] = (msb*256) + lsb;
			if (i > 0) {
				txt = txt + ",";
			}
			txt = txt + String.valueOf(dims[i]);
			dimcounts[i] = 1;
		}
		
		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = (char)varname+"";
		row[1] = "Character array";
		row[2] = txt;
		Row.setText(row);
		
		return(Address);
	}

	/**
	 * Handler for type 5 variables (Number with a name > 1)
	 * Format:
	 *   (Original char) [101XXXXX] where XXXXX = char of name - 0x60
	 *   [0] [000XXXXX] where XXXXX = char of name - 0x40
	 *   ...
	 *   [Y] [100XXXXX] where XXXXX = char of name - 0x40 (Bit 7 is set to terminate string)
	 *   [N1..N5] Speccy Floating point number
	 *   
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int  VariableType5(int Address, int chr, byte[] file ) {
		boolean done = false;
		String vn = "";
		while (!done) {
			int varname = (chr & 0x1f);
			varname = varname +0x60;
			vn = vn + String.valueOf((char) varname);
			chr = file[Address++];
			done = (chr & 0x80) == 0x80;
		}
		int varname = (chr & 0x3f) +0x40;
		vn = vn + String.valueOf((char) varname);

		double value = Speccy.GetNumberAtByte(file, Address);
		
		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = vn;
		row[1] = "Number (#5)";
		row[2] = String.valueOf(value);
		Row.setText(row);
		
		Address = Address + 5;
		return(Address);
	}

	/**
	 * Handler for type 4 variables (Numeric arrays)
	 * Variable name is parsed from the original marker (first 5 bytes) + 0x40
	 * format is:
	 *  [0-1] data length (Used to quickly skip over variable when searching)
	 *  [2] Number of dimensions (1-255)
	 *  [3.4] First dimension size. (1-65535)
	 *  ..
	 *  [xx.yy] last dimension size
	 *  [ZZZZZ] Speccy FP representation of (1[,1[,1]]) 
	 *  [ZZZZZ] Speccy FP representation of (2[,1[,1]]) 
	 *  ... 
	 *  [ZZZZZ] Speccy FP representation of (xx[,1[,1]]) 
	 *  [ZZZZZ] Speccy FP representation of (1[,2[,1]]) 
	 *  and so on.
	 * 
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int VariableType4(int Address, int chr, byte[] file ) {
		try {
		int varname = chr & 0x1f;
		varname = varname + 0x60;
		String txt = "(";
		Address = Address + 2;
		int dimensions =  (file[Address++] & 0xff);
	
		int dims[] = new int[dimensions];
		int dimcounts[] = new int[dimensions];
		for (int i = 0; i < dimensions; i++) {
			int lsb = (file[Address++] & 0xff);
			int msb = (file[Address++] & 0xff);
			dims[i] = (msb*256) + lsb;
			if (i > 0) {
				txt = txt + ",";
			}
			txt = txt + String.valueOf(dims[i]);
			dimcounts[i] = 1;
		}
		txt = txt + ")";
		
		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = (char)chr+"";
		row[1] = "Numeric Array";
		row[2] = txt;
		Row.setText(row);

		} catch (Exception E) {
			E.printStackTrace();
			System.out.println(E.getMessage());
		}

		return(Address);
	}

	/**
	 * Handler for type 3 variables (Numbers with a one character name)
	 * Variable name is parsed from the original marker (first 5 bits) + 0x40
	 * [12345] Speccy floating point representation of the value.  
	 * 
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int  VariableType3(int Address, int chr, byte[] file ) {
		int varname = chr & 0x1f;
		varname = varname + 0x60;
		double value = Speccy.GetNumberAtByte(file,Address);
		String sValue = String.valueOf(value);
		
		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = (char)chr+"";
		row[1] = "Number (#3)";
		row[2] = sValue;
		Row.setText(row);
		
		Address = Address + 5;
		return(Address);
	}

	/**
	 * Handler for type 2 variables (Strings)
	 * Variable name is parsed from the original marker (first 5 bits) + 0x40
	 * [1..2] String length lsb first
	 * [3..x] Characters making up the string. 
	 * 
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int VariableType2(int Address, int chr, byte[] file ) {
		int varname = chr & 0x1f;
		varname = varname + 0x60;
		int lsb = (file[Address++] & 0xff);
		int msb = (file[Address++] & 0xff);
		int length =  (msb*256) + lsb;

		String s = "";
		while (length > 0 && Address < file.length) {
			char c = (char) file[Address++];
			s = s + Speccy.DecodeToken(""+c);
			length--;
		}
		
		TableItem Row = new TableItem(Variables, SWT.NONE);
		String row[] = new String[3];
		row[0] = (char)chr+"";
		row[1] = "String";
		row[2] = s.trim();
		
		Row.setText(row);

		return(Address);
	}
}

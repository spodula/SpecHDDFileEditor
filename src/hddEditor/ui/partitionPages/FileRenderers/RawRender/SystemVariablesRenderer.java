package hddEditor.ui.partitionPages.FileRenderers.RawRender;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.libs.ASMLib;
import hddEditor.libs.SpeccySystemVariables;
import hddEditor.libs.SpeccySystemVariables.FlagBit;
import hddEditor.libs.SpeccySystemVariables.SystemVariable;
import hddEditor.libs.ASMLib.DecodedASM;
import hddEditor.libs.Languages;
import hddEditor.libs.Speccy;

public class SystemVariablesRenderer implements Renderer {
	public Table SysVarList = null;
	public Label VarLBL = null;
	public Combo SysVarVersion = null;

	public static SpeccySystemVariables ssv = new SpeccySystemVariables();

	String SysVarTypes[] = {"48K only","128K","+2A/+3"};

	
	@Override
	public void DisposeRenderer() {
		if (SysVarList != null) {
			SysVarList.dispose();
			SysVarList = null;
		}
		if (VarLBL != null) {
			VarLBL.dispose();
			VarLBL = null;
		}
		if (SysVarVersion != null) {
			SysVarVersion.dispose();
			SysVarVersion = null;
		}
	}

	/**
	 * Render the System Variables.
	 * 
	 * @param targetPage
	 * @param sysVars
	 */
	public void AddSysVars(Composite targetPage, byte[] sysVars, boolean Is128, boolean IsPlus3, Languages lang) {
		DisposeRenderer();
		
		VarLBL = new Label(targetPage, SWT.NONE);
		VarLBL.setText(lang.Msg(Languages.MSG_SYSTEMVARS));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 3;
		VarLBL.setLayoutData(gd);
		
		SysVarVersion = new Combo(targetPage, SWT.NONE);
		SysVarVersion.setItems(SysVarTypes);
		int index = 0;
		if (Is128) 
			index = 1;
		if (IsPlus3)
			index = 2;
		SysVarVersion.select(index);
		SysVarVersion.addSelectionListener(new SelectionListener() {	
			@Override
			public void widgetSelected(SelectionEvent e) {
				int typ = SysVarVersion.getSelectionIndex();
				AddSysVars(targetPage, sysVars, (typ>0),(typ>1), lang);
				targetPage.pack();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		if (SysVarList != null) {
			SysVarList.dispose();
		}
		SysVarList = new Table(targetPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		SysVarList.setLinesVisible(true);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		gd.heightHint = 100;
		gd.widthHint = targetPage.getSize().x;
		SysVarList.setLayoutData(gd);

		TableColumn vc1 = new TableColumn(SysVarList, SWT.LEFT);
		vc1.setText(lang.Msg(Languages.MSG_VARIABLE));
		vc1.setWidth(120);
		TableColumn vc2 = new TableColumn(SysVarList, SWT.FILL);
		vc2.setText(lang.Msg(Languages.MSG_CONTENT));
		vc2.setWidth(100);
		TableColumn vc3 = new TableColumn(SysVarList, SWT.FILL);
		vc3.setText(lang.Msg(Languages.MSG_NOTES));
		vc3.setWidth(580);

		if (Is128) {
			if (IsPlus3) {
				AddVarList(ssv.SpeccyPlus3SystemVariables, sysVars, lang);
			} else {
				AddVarList(ssv.Speccy128SystemVariables, sysVars, lang);
			}
		}
		AddVarList(ssv.Speccy48SystemVariables, sysVars, lang);

	}

	private void AddVarList(ArrayList<SystemVariable> variables, byte sysVars[], Languages lang) {
		for (SpeccySystemVariables.SystemVariable sv : variables) {
			String vName = sv.abbrev + " (" + sv.address + ")";
			String vNotes = sv.description;
			String vData = "";

			// get the data to be parsed into its own block.
			int realaddress = sv.address - 0x5b00;
			byte data[] = new byte[sv.length];
			System.arraycopy(sysVars, realaddress, data, 0, sv.length);

			// convert the data into something useful..
			switch (sv.type) {
			case SpeccySystemVariables.SV_BYTE:
				vData = "";
				for (int byteptr = 0;byteptr < data.length;byteptr = byteptr +1) {
					int dValue = (data[byteptr] & 0xff);
					vData = vData + ", "+String.format("%d (%02Xh)", dValue, dValue);
				}
				if (vData.substring(0,1).equals(",")) {
					vData = vData.substring(2);
				}
				break;
			case SpeccySystemVariables.SV_WORD:
				vData = "";
				for (int wordptr =0;wordptr < data.length;wordptr = wordptr+2) {
					int dValue = ((data[wordptr+1] & 0xff) * 0x100) + (data[wordptr] & 0xff);
					vData = vData + String.format("%d (%04Xh)", dValue, dValue)+"\n";
				}
				break;
			case SpeccySystemVariables.SV_CHAR:
				if (data.length==1) {
					char c = (char) data[0];
					vData = String.format("%c (%d %02Xh)", c, c, c);
				} else {
					vData = "";
					for (byte byt:data) {
						if (byt==0) {
							vData = vData + ",0";
						} else {
							vData = vData + String.format("%c",(char)byt);
						}
					}
					if (vData.substring(0,1).equals(",")) {
						vData = vData.substring(1);
					}
				}
				break;
			case SpeccySystemVariables.SV_SUBROUTINE:
				vData = DecodeSubroutine(data, sv.address, lang);
				break;
			case SpeccySystemVariables.SV_STACK:
				vData = "";
				break;
			case SpeccySystemVariables.SV_TRIPLE:
				int i = data[2] & 0xff;
				i = (i * 0x100) + (data[1] & 0xff);
				i = (i * 0x100) + (data[0] & 0xff);
				vData = String.format("%d", i);
				break;
			case SpeccySystemVariables.SV_FLOAT:
				vData = "";
				for (int floatnum=0;floatnum<sv.length;floatnum=floatnum+5) {
					double fValue = Speccy.GetNumberAtByte(data, floatnum);
					vData = vData + String.format("%f", fValue)+"\n";	
				}				
				break;
			case SpeccySystemVariables.SV_COLOUR:
				int ink = data[0] & 0b00000111;
				int paper = (data[0] & 0b00111000) / 8;
				boolean bright = (data[0] & 0b01000000) != 0;
				boolean flash = (data[0] & 0b10000000) != 0;
				vData = "i:" + ink + " p:" + paper + " br:" + bright + " fl:" + flash;
			case SpeccySystemVariables.SV_FLAGS:
				int b = (data[0] & 0xff);
				vData = String.format("%d (%02Xh)", b,b);
				if (sv.flagBitDescriptions != null) {
					String result = "";
					for (FlagBit fb : sv.flagBitDescriptions) {
						if (fb != null) {
							int bitval = (b & 1);
							result = result + "\n" + fb.bit + ": "+bitval+" - " + fb.description + " - ";
							if (bitval == 1) {
								result = result + " " + fb.TrueDesc;
							} else {
								result = result + " " + fb.FalseDesc;
							}
						}
						b = b >> 1;
					}
					vNotes = vNotes+"\n"+result;
				} else {
					vNotes = vNotes+"\n"+String.format("%d (%02Xh)", (data[0] & 0xff), (data[0] & 0xff)) + "("+lang.Msg(Languages.MSG_FLAGSNOTDEF)+")";
				}
			}

			String row[] = new String[3];
			row[0] = vName;
			row[1] = vData.trim();
			row[2] = vNotes.trim();

			TableItem Row = new TableItem(SysVarList, SWT.NONE);
			Row.setText(row);
		}
	}

	/**
	 * Create an assembly listing of the given subroutine. 
	 * 
	 * @param data
	 * @param baseaddress
	 * @return
	 */
	private String DecodeSubroutine(byte data[], int baseaddress, Languages lang) {
		String result = "";
		ASMLib asm = new ASMLib(lang);
		int realaddress = 0x0000;
		int asmData[] = new int[5];
		try {
			while (realaddress < data.length) {
				for (int i = 0; i < 5; i++) {
					int d = 0;
					if ((realaddress + i) < data.length) {
						d = (int) data[realaddress + i] & 0xff;
					}
					asmData[i] = d;
				}
				// decode instruction
				DecodedASM Instruction = asm.decode(asmData, baseaddress + realaddress);

				result = result + String.format("%04X", baseaddress + realaddress) + " " + Instruction.instruction
						+ "\n";

				realaddress = realaddress + Instruction.length;

			} // while
		} catch (Exception E) {
			
			System.out.println(String.format(lang.Msg(Languages.MSG_ERRORATXX), realaddress,baseaddress + realaddress));
			System.out.println(E.getMessage());
			E.printStackTrace();
		}
		return (result.trim());

	}

	/**
	 * Return a string containing the system variables.
	 * @return
	 */
	public String getSystemVariableSummary() {
		String result = "";
		for (TableItem line:SysVarList.getItems()) {
			String varname = line.getText(0);
			String content = line.getText(1);
			String notes   = line.getText(2);
			result = result + varname+" '"+content+"' - "+notes+"\n";
		}
		return(result.trim());
	}

}

package hddEditor.ui.partitionPages.FileRenderers.RawRender;

/**
 * Render the given file as a table containing an assembly listing.
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import hddEditor.libs.ASMLib;
import hddEditor.libs.ASMLib.DecodedASM;

public class AssemblyRenderer implements Renderer {
	Table AsmTable = null;

	@Override
	public void DisposeRenderer() {
		if (AsmTable != null) {
			AsmTable.dispose();
			AsmTable = null;
		}
	}

	public void Render(Composite TargetPage, byte data[], int startaddress) {
		AsmTable = new Table(TargetPage, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		AsmTable.setLinesVisible(true);

		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalSpan = 4;
		gd.heightHint = 400;
		AsmTable.setLayoutData(gd);

		TableColumn tc1 = new TableColumn(AsmTable, SWT.LEFT);
		tc1.setText("Address");
		tc1.setWidth(80);
		TableColumn tc2 = new TableColumn(AsmTable, SWT.LEFT);
		tc2.setText("Hex");
		tc2.setWidth(160);
		TableColumn tc3 = new TableColumn(AsmTable, SWT.LEFT);
		tc3.setText("Asm");
		tc3.setWidth(160);
		TableColumn tc4 = new TableColumn(AsmTable, SWT.LEFT);
		tc4.setText("Chr");
		tc4.setWidth(160);
		AsmTable.setHeaderVisible(true);

		ASMLib asm = new ASMLib();
		int loadedaddress = startaddress;
		int realaddress = 0x0000;
		int asmData[] = new int[5];
		try {
			while (realaddress < data.length) {
				String chrdata = "";
				TableItem Row = new TableItem(AsmTable, SWT.NONE);
				String dta[] = new String[4];
				dta[0] = String.format("%04X", loadedaddress);
				int InstructionLen = 1;
				if (data[realaddress] == 0) {
					dta[1] = "00";
					dta[2] = "Nop";
					dta[3] = "";
				} else {
					for (int i = 0; i < 5; i++) {
						int d = 0;
						if (realaddress + i < data.length) {
							d = (int) data[realaddress + i] & 0xff;
						}
						asmData[i] = d;

						if ((d > 0x1F) && (d < 0x7f)) {
							chrdata = chrdata + (char) d;
						} else {
							chrdata = chrdata + "?";
						}
					}
					// decode instruction
					DecodedASM Instruction = asm.decode(asmData, loadedaddress);
					// output it. - First, assemble a list of hex bytes, but pad out to 12 chars
					// (4x3)
					String hex = "";
					for (int j = 0; j < Instruction.length; j++) {
						hex = hex + String.format("%02X", asmData[j]) + " ";
					}
					dta[1] = hex;
					dta[2] = Instruction.instruction;
					dta[3] = chrdata.substring(0, Instruction.length);
					InstructionLen = Instruction.length;
				}


				Row.setText(dta);

				realaddress = realaddress + InstructionLen;
				loadedaddress = loadedaddress + InstructionLen;

			} // while
		} catch (Exception E) {
			System.out.println("Error at: " + realaddress + "(" + loadedaddress + ")");
			System.out.println(E.getMessage());
			E.printStackTrace();
		}

	}

}

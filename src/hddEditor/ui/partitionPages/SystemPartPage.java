package hddEditor.ui.partitionPages;
// - Some files seem to be broken..

import java.io.File;

/**
 * Implementation of the System partition page. 
 */

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.Speccy;
import hddEditor.libs.FileSelectDialog;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.Languages;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.PlusIDEDosException;
import hddEditor.libs.partitions.SystemPartition;
import hddEditor.ui.FileExportAllPartitionsForm;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.dialogs.AddressNote;
import hddEditor.ui.partitionPages.dialogs.HexEditDialog;
import hddEditor.ui.partitionPages.dialogs.NewPartitionDialog;
import hddEditor.ui.partitionPages.dialogs.RenameFileDialog;
import hddEditor.ui.partitionPages.dialogs.ShrinkDiskDialog;

public class SystemPartPage extends GenericPage {
	// Flag to block writing to the disk.
	private boolean CanEditPartitions = false;

	// Sub-forms so we can close them if they are still open when the page changes.
	HexEditDialog HxEditDialog = null;
	NewPartitionDialog NewPartDialog = null;
	FileExportAllPartitionsForm ExportAllPartsForm = null;
	ShrinkDiskDialog ShrinkDiskForm = null;
	RenameFileDialog RenFileDialog = null;

	// Basic editor colour components.
	Button becBright = null;
	Combo becPaper = null;
	Combo becInk = null;

	// Default Colour components
	Button dbcBright = null;
	Combo dbcPaper = null;
	Combo dbcInk = null;

	// Unmap buttons
	Button unmapA = null;
	Button unmapB = null;
	Button unmapM = null;

	// Remaps
	Text Unit0DL = null;
	Text Unit1DL = null;
	Text UnitMDL = null;

	// Default drive
	Text DefaultDrive = null;

	// Buttons
	Button ApplyButton = null;
	Button CancelButton = null;

	// Partition edit buttons
	Button DeleteButton = null;
	Button EditPartitionButton = null;
	Button GotoPartitionButton = null;
	Button NewPartitionButton = null;
	Button DoExtractAllPartitionButton = null;
	Button ShrinkDisk = null;
	Button RenamePartition = null;

	// Partition table
	Table PartitionTable = null;

	// Colours for the default colour combos.
	Display d = Display.getCurrent();

	Color colours[] = { new Color(d, 0, 0, 0), new Color(d, 0, 0, 255), new Color(d, 255, 0, 0),
			new Color(d, 255, 0, 255), new Color(d, 0, 255, 0), new Color(d, 0, 255, 255), new Color(d, 255, 255, 0),
			new Color(d, 255, 255, 255) };

	/**
	 * Set the colour for a given colour combo taking into account we actually want
	 * to be able to read it afterwards.
	 * 
	 * @param comp
	 */
	public void SetComponentColour(Combo comp) {
		int index = TextToIndex(comp.getText(), Speccy.SPECTRUM_COLOURS);

		if (index < 4) { // If dark colour, set ink to white
			comp.setForeground(colours[7]);
		} else { // if light colour set ink to black
			comp.setForeground(colours[0]);
		}
		comp.setBackground(colours[index]);
	}

	/**
	 * Selection listener for checkboxes and combos, so if they are changed, it will
	 * enable the buttons.
	 */
	SelectionListener SetModifiedSelectionListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent arg0) {
			ApplyButton.setEnabled(true);
			CancelButton.setEnabled(true);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent arg0) {
			widgetSelected(arg0);
		}
	};

	/**
	 * Selection listener for text fields, so if they are changed, it will enable
	 * the buttons.
	 */
	ModifyListener SetModifiedTextListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent arg0) {
			ApplyButton.setEnabled(true);
			CancelButton.setEnabled(true);
		}
	};

	/**
	 * Constructor
	 * 
	 * @param root
	 * @param parent
	 * @param partition
	 */
	public SystemPartPage(HDDEditor root, Composite parent, IDEDosPartition partition, FileSelectDialog filesel, Languages lang) {
		super(root, parent, partition, filesel,lang);

		AddComponents();
	}

	/**
	 * Create the components for the form.
	 */
	public void AddComponents() {
		if (ParentComp != null) {
			RemoveComponents();
			SystemPartition sp = (SystemPartition) partition;
			int numUsedPartitions = 0;
			long usedSpace = 0;
			long freeSpace = 0;
			for (IDEDosPartition idp : sp.partitions) {
				if (idp.GetPartType() > 0) {
					numUsedPartitions++;
					if (idp.GetPartType() == 0xff) {
						freeSpace = idp.GetSizeK();
					} else {
						usedSpace = usedSpace + idp.GetSizeK();
					}
				}
			}

			super.AddBasicDetails();
			ParentComp.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent arg0) {
					DisposeSubDialogs();
				}
			});

			if (partition.CurrentDisk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_HDD) {

				label("", 4);
				Label l = label(lang.Msg(Languages.MSG_SYSDETS)+".", 1);
				FontData fontData = l.getFont().getFontData()[0];
				Font font = new Font(ParentComp.getDisplay(),
						new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
				l.setFont(font);
				label("", 3);

				label(lang.Msg(Languages.MSG_UNALLOCSPC) + ": " + GeneralUtils.GetSizeAsString(freeSpace), 1);
				label(lang.Msg(Languages.MSG_ALLOCSPACE) + ": " + GeneralUtils.GetSizeAsString(usedSpace), 1);
				label(lang.Msg(Languages.MSG_FREEPARTS)  + ": " + (sp.GetMaxPartitions() - numUsedPartitions), 1);
				label(lang.Msg(Languages.MSG_ALLOCPARTS) + ": " + numUsedPartitions, 1);

				label("", 4);

				l = label(lang.Msg(Languages.MSG_DEFAULTCOLS)+":", 1);
				l.setFont(font);

				label("", 1);
				label(lang.Msg(Languages.MSG_PAPER) , 1);
				label(lang.Msg(Languages.MSG_INK) , 1);

				// Colour attribute bits are:
				// 76543210
				// FBPPPIII
				// Where F = flash (ignored), B = bright, PPP = Paper (0-7) and I = ink (0-7)
				SelectionListener ComboColourChangeListener = new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent arg0) {
						Combo comp = (Combo) arg0.getSource();
						SetComponentColour(comp);
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent arg0) {
						widgetSelected(arg0);
					}
				};

				int col = sp.GetBasicEditColour();
				int ink = col & 0x07;
				int paper = (col & 0x38) / 8;
				label(lang.Msg(Languages.MSG_EDITORCOL), 1);
				becBright = checkbox(lang.Msg(Languages.MSG_BRIGHT), (col & 0x40) != 0);
				becBright.addSelectionListener(SetModifiedSelectionListener);

				becPaper = combo(Speccy.SPECTRUM_COLOURS, Speccy.SPECTRUM_COLOURS[paper]);
				becPaper.addSelectionListener(SetModifiedSelectionListener);
				becPaper.addSelectionListener(ComboColourChangeListener);
				SetComponentColour(becPaper);

				becInk = combo(Speccy.SPECTRUM_COLOURS, Speccy.SPECTRUM_COLOURS[ink]);
				becInk.addSelectionListener(SetModifiedSelectionListener);
				becInk.addSelectionListener(ComboColourChangeListener);
				SetComponentColour(becInk);

				col = sp.GetBasicColour();
				ink = col & 0x07;
				paper = (col & 0x38) / 8;
				label(lang.Msg(Languages.MSG_DEFCOL), 1);
				dbcBright = checkbox(lang.Msg(Languages.MSG_BRIGHT), (col & 0x40) != 0);
				dbcBright.addSelectionListener(SetModifiedSelectionListener);

				dbcPaper = combo(Speccy.SPECTRUM_COLOURS, Speccy.SPECTRUM_COLOURS[(col & 0x38) / 8]);
				dbcPaper.addSelectionListener(SetModifiedSelectionListener);
				dbcPaper.addSelectionListener(ComboColourChangeListener);
				SetComponentColour(dbcPaper);

				dbcInk = combo(Speccy.SPECTRUM_COLOURS, Speccy.SPECTRUM_COLOURS[(col & 0x07)]);
				dbcInk.addSelectionListener(SetModifiedSelectionListener);
				dbcInk.addSelectionListener(ComboColourChangeListener);
				SetComponentColour(dbcInk);

				label("", 4);

				unmapA = checkbox(String.format(lang.Msg(Languages.MSG_UNMAPDRIVE), "A"), sp.GetUnmapA());
				unmapA.addSelectionListener(SetModifiedSelectionListener);

				Unit0DL = editbox(sp.GetUnit0DriveLetter(), 1);
				Unit0DL.addModifyListener(SetModifiedTextListener);
				label("", 1);
				label("", 1);

				unmapB = checkbox(String.format(lang.Msg(Languages.MSG_UNMAPDRIVE), "B"), sp.GetUnmapB());
				unmapB.addSelectionListener(SetModifiedSelectionListener);
				Unit1DL = editbox(sp.GetUnit1DriveLetter(), 1);
				Unit1DL.addModifyListener(SetModifiedTextListener);

				label(lang.Msg(Languages.MSG_DEFAULTDRIVE)+":", 1);
				DefaultDrive = editbox(sp.GetDefaultDrive(), 1);
				DefaultDrive.addModifyListener(SetModifiedTextListener);

				unmapM = checkbox(String.format(lang.Msg(Languages.MSG_UNMAPDRIVE), "M"), sp.GetUnmapM());
				unmapM.addSelectionListener(SetModifiedSelectionListener);
				UnitMDL = editbox(sp.GetRamdiskDriveLetter(), 1);
				UnitMDL.addModifyListener(SetModifiedTextListener);

				CancelButton = button(lang.Msg(Languages.MSG_CANCEL));
				GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
				gd.widthHint = 200;
				CancelButton.setLayoutData(gd);
				CancelButton.setEnabled(false);
				CancelButton.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent arg0) {
						DoCancel();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent arg0) {
						widgetSelected(arg0);
					}
				});

				ApplyButton = button(lang.Msg(Languages.MSG_APPLY));
				ApplyButton.setLayoutData(gd);
				ApplyButton.setEnabled(false);
				ApplyButton.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent arg0) {
						DoApply();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent arg0) {
						widgetSelected(arg0);
					}
				});
				l = label(lang.Msg(Languages.MSG_PARTITIONS)+":", 1);
				l.setFont(font);
				label("", 3);

				CanEditPartitions = true;
			} else {
				Label l = label(lang.Msg(Languages.MSG_FDDSECTS) + ":", 1);
				FontData fontData = l.getFont().getFontData()[0];
				Font BoldFont = new Font(ParentComp.getDisplay(),
						new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
				l.setFont(BoldFont);
				label("", 3);
				CanEditPartitions = false;
			}

			PartitionTable = new Table(ParentComp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
			PartitionTable.setLinesVisible(true);

			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.horizontalSpan = 4;
			gd.heightHint = 110;
			PartitionTable.setLayoutData(gd);

			TableColumn tc1 = new TableColumn(PartitionTable, SWT.LEFT);
			TableColumn tc2 = new TableColumn(PartitionTable, SWT.LEFT);
			TableColumn tc3 = new TableColumn(PartitionTable, SWT.LEFT);
			TableColumn tc4 = new TableColumn(PartitionTable, SWT.LEFT);
			TableColumn tc5 = new TableColumn(PartitionTable, SWT.LEFT);
			if (partition.CurrentDisk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_HDD) {
				tc1.setText(lang.Msg(Languages.MSG_PARTNAME));
			} else if (partition.CurrentDisk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_LINEAR) {
				tc1.setText(lang.Msg(Languages.MSG_TAPESECT)+":");
			} else {
				tc1.setText(lang.Msg(Languages.MSG_FDDSECT)+":");
			}
			tc2.setText(lang.Msg(Languages.MSG_PARTTYPEM));
			tc3.setText(lang.Msg(Languages.MSG_START));
			tc4.setText(lang.Msg(Languages.MSG_END));
			tc5.setText(lang.Msg(Languages.MSG_SIZE));
			tc1.setWidth(150);
			tc2.setWidth(150);
			tc3.setWidth(150);
			tc4.setWidth(150);
			tc5.setWidth(100);
			PartitionTable.setHeaderVisible(true);

			UpdatePartitionList();

			PartitionTable.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					widgetDefaultSelected(arg0);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					DoPartitionSelectionChange();
				}
			});

			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.widthHint = 200;

			DeleteButton = button(lang.Msg(Languages.MSG_DELPART));
			DeleteButton.setLayoutData(gd);
			DeleteButton.setEnabled(false);
			DeleteButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoDeletePartition();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});

			EditPartitionButton = button(lang.Msg(Languages.MSG_EDITRAWPART));
			EditPartitionButton.setLayoutData(gd);
			EditPartitionButton.setEnabled(false);
			EditPartitionButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoEditRawPartition();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});

			GotoPartitionButton = button(lang.Msg(Languages.MSG_GOTOPART));
			GotoPartitionButton.setLayoutData(gd);
			GotoPartitionButton.setEnabled(false);
			GotoPartitionButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoGotoPartition();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});

			NewPartitionButton = button(lang.Msg(Languages.MSG_NEWPART));
			NewPartitionButton.setLayoutData(gd);
			NewPartitionButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoNewPartition();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});

			DoExtractAllPartitionButton = button(lang.Msg(Languages.MSG_DUMPPART));
			DoExtractAllPartitionButton.setLayoutData(gd);
			DoExtractAllPartitionButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoExtractAllPartitions();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});

			ShrinkDisk = button(lang.Msg(Languages.MSG_SHRINKDISK));
			ShrinkDisk.setLayoutData(gd);
			ShrinkDisk.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoShrinkDisk();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});

			RenamePartition = button(lang.Msg(Languages.MSG_RENAMEPART));
			RenamePartition.setLayoutData(gd);
			RenamePartition.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					DoRenamePartition();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			});

			NewPartitionButton.setEnabled(CanEditPartitions);
			ParentComp.pack();
		}
	}

	/**
	 * Update the partition list from the current partition (Assumed to be a system
	 * partition)
	 */
	private void UpdatePartitionList() {
		SystemPartition sp = (SystemPartition) partition;
		PartitionTable.removeAll();

		for (IDEDosPartition part : sp.partitions) {
			if (part.GetPartType() != 0) {
				TableItem item2 = new TableItem(PartitionTable, SWT.NONE);
				String content[] = new String[5];
				content[0] = part.GetName();
				content[1] = PLUSIDEDOS.GetTypeAsString(part.GetPartType());
				content[2] = lang.Msg(Languages.MSG_CYL)+":" + part.GetStartCyl() + " "+lang.Msg(Languages.MSG_HEAD)+":" + part.GetStartHead();
				content[3] = lang.Msg(Languages.MSG_CYL)+":" + part.GetEndCyl() + " "+lang.Msg(Languages.MSG_HEAD)+":" + part.GetEndHead();
				content[4] = GeneralUtils.GetSizeAsString(part.GetSizeK() * 1024);

				item2.setText(content);
				item2.setData(part);
			}
		}
	}

	/**
	 * Extract all partitions to disk.
	 */
	protected void DoExtractAllPartitions() {
		ExportAllPartsForm = new FileExportAllPartitionsForm(ParentComp.getDisplay(), lang);
		try {
			ExportAllPartsForm.Show(partition);
		} finally {
			ExportAllPartsForm = null;
		}
	}

	/**
	 * Save the top of the form to the partition.
	 */
	private void DoApply() {
		int beInk = TextToIndex(becInk.getText(), Speccy.SPECTRUM_COLOURS);
		int bePaper = TextToIndex(becPaper.getText(), Speccy.SPECTRUM_COLOURS);
		int becColour = (bePaper * 8) + beInk;
		if (becBright.getSelection()) {
			becColour = becColour + 64;
		}

		int dbInk = TextToIndex(dbcInk.getText(), Speccy.SPECTRUM_COLOURS);
		int dbPaper = TextToIndex(dbcPaper.getText(), Speccy.SPECTRUM_COLOURS);
		int dbcColour = (dbPaper * 8) + dbInk;
		if (dbcBright.getSelection()) {
			dbcColour = dbcColour + 64;
		}

		SystemPartition sp = (SystemPartition) partition;
		sp.SetDetails(becColour, dbcColour, unmapA.getSelection(), unmapB.getSelection(), unmapM.getSelection(),
				Unit0DL.getText(), Unit1DL.getText(), UnitMDL.getText(), DefaultDrive.getText());
		ApplyButton.setEnabled(false);
		CancelButton.setEnabled(false);
	}

	/**
	 * This is a hack to get around the Combo.getSelectionIndex returning
	 * inconsistent values. No idea why, sometimes it just returns -1 even if the
	 * text is valid.
	 * 
	 * @param s   - Text to find
	 * @param arr - Array of items
	 * @return - Index of item in the array.
	 */
	private int TextToIndex(String s, String[] arr) {
		int result = -1;
		for (int i = 0; i < arr.length; i++) {
			if (s.equals(arr[i])) {
				result = i;
			}
		}
		return (result);
	}

	/**
	 * Undo the changes to the mapping/default colour fields
	 */
	private void DoCancel() {
		SystemPartition sp = (SystemPartition) partition;
		int col = sp.GetBasicEditColour();
		int ink = col & 0x07;
		int paper = (col & 0x38) / 8;
		becBright.setSelection((col & 0x40) != 0);
		becPaper.setText(Speccy.SPECTRUM_COLOURS[paper]);
		becInk.setText(Speccy.SPECTRUM_COLOURS[ink]);

		col = sp.GetBasicColour();
		ink = col & 0x07;
		paper = (col & 0x38) / 8;
		dbcBright.setSelection((col & 0x40) != 0);
		dbcPaper.setText(Speccy.SPECTRUM_COLOURS[paper]);
		dbcInk.setText(Speccy.SPECTRUM_COLOURS[ink]);

		unmapA.setSelection(sp.GetUnmapA());
		unmapB.setSelection(sp.GetUnmapB());
		unmapM.setSelection(sp.GetUnmapM());

		Unit0DL.setText(sp.GetUnit0DriveLetter());
		Unit1DL.setText(sp.GetUnit1DriveLetter());
		UnitMDL.setText(sp.GetRamdiskDriveLetter());

		DefaultDrive.setText(sp.GetDefaultDrive());

		ApplyButton.setEnabled(false);
		CancelButton.setEnabled(false);
	}

	/**
	 * Enable and disable buttons according to the partition type selected.
	 * Basically, you can't delete the System or free space partition, and you can't
	 * edit or goto the free space partition.
	 */
	private void DoPartitionSelectionChange() {
		TableItem itms[] = PartitionTable.getSelection();
		boolean AllowDelete = false;
		boolean AllowGoto = false;
		if ((itms != null) && (itms.length > 0)) {
			String partType = itms[0].getText(1).trim();
			if (!partType.equals("System") && !partType.equals("Free")) {
				AllowDelete = CanEditPartitions;
			}
			if (!partType.equals("Free")) {
				AllowGoto = true;
			}
		} else {
			AllowGoto = false;
			AllowDelete = false;
		}
		NewPartitionButton.setEnabled(CanEditPartitions);
		DeleteButton.setEnabled(AllowDelete);
		EditPartitionButton.setEnabled(AllowGoto);
		GotoPartitionButton.setEnabled(AllowGoto);
	}

	/**
	 * Goto the selected partition
	 */
	private void DoGotoPartition() {
		TableItem itms[] = PartitionTable.getSelection();
		if ((itms != null) && (itms.length > 0)) {
			String partName = itms[0].getText(0).trim();
			boolean DoChange = true;
			if ((ApplyButton != null) && ApplyButton.getEnabled()) {
				MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				messageBox.setMessage(lang.Msg(Languages.MSG_CHANGEPARTQ));
				messageBox.setText(lang.Msg(Languages.MSG_CHANGEPART));
				int response = messageBox.open();
				DoChange = (response == SWT.YES);
			}
			if (DoChange) {
				RootPage.GotoPartitionByName(partName);
			}
		}
	}

	/**
	 * Implements the "New Partition" button.
	 */
	private void DoNewPartition() {
		if (!CanEditPartitions) {
			ErrorBox(lang.Msg(Languages.MSG_CANTEDITPART));
		} else {
			SystemPartition SystemPartition = (SystemPartition) RootPage.CurrentHandler
					.GetPartitionByType(PLUSIDEDOS.PARTITION_SYSTEM);
			if (SystemPartition == null) {
				ErrorBox(lang.Msg(Languages.MSG_ERRCREATEPART)+": "+lang.Msg(Languages.MSG_NOSYSPART));
			} else {
				ArrayList<String> ExistingPartitions = new ArrayList<String>();
				for (IDEDosPartition p : SystemPartition.partitions) {
					if (p.GetPartType() != 0) {
						ExistingPartitions.add(p.GetName().trim().toUpperCase());
					}
				}
				String sExistingPartitions[] = ExistingPartitions.toArray(new String[0]);

				IDEDosPartition freespacePartition = RootPage.CurrentHandler
						.GetPartitionByType(PLUSIDEDOS.PARTITION_FREE);
				if (freespacePartition == null) {
					ErrorBox(lang.Msg(Languages.MSG_ERRCREATEPART)+": "+lang.Msg(Languages.MSG_NOFREEPART));
				} else {
					NewPartDialog = new NewPartitionDialog(ParentComp.getDisplay(), lang);
					if (NewPartDialog.Show(sExistingPartitions) && !ParentComp.isDisposed()) {
						try {
							SystemPartition.CreatePartition(NewPartDialog.Name, NewPartDialog.SizeMB,
									NewPartDialog.PartType);
							// update the partition list (Github issue #2:
							// https://github.com/spodula/SpecHDDFileEditor/issues/2)
							UpdatePartitionList();
						} catch (PlusIDEDosException E) {
							ErrorBox(E.partition + ": " + E.getMessage());
						}
					}
					NewPartDialog = null;
				}
			}
		}
	}

	/**
	 * Implement deletion of selected partition
	 */
	protected void DoDeletePartition() {
		if (!CanEditPartitions) {
			ErrorBox(lang.Msg(Languages.MSG_CANTEDITPART));
		} else {
			TableItem itms[] = PartitionTable.getSelection();
			if (itms != null) {
				SystemPartition SystemPartition = (SystemPartition) RootPage.CurrentHandler
						.GetPartitionByType(PLUSIDEDOS.PARTITION_SYSTEM);
				if (SystemPartition == null) {
					ErrorBox(lang.Msg(Languages.MSG_ERRDELETEPART)+": "+lang.Msg(Languages.MSG_NOSYSPART));
				} else {
					String partName = itms[0].getText(0).trim();
					MessageBox messageBox = new MessageBox(ParentComp.getShell(),
							SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
					messageBox.setMessage(lang.Msg(Languages.MSG_AREYOUSURE));
					messageBox.setText(String.format(lang.Msg(Languages.MSG_ABSOLUTELYSUREPART),partName.trim()));
					if (messageBox.open() == SWT.OK) {
						try {
							SystemPartition.DeletePartition(partName);
						} catch (PlusIDEDosException e) {
							ErrorBox(lang.Msg(Languages.MSG_ERRDELETEPART)+"'" + partName + "'. " + e.getMessage());
						}
						RootPage.UpdateDropdown();
					}
				}
			}
		}
	}

	/**
	 * Handler for Edit Raw Partition. Basically show a hex editor.
	 * 
	 */
	private void DoEditRawPartition() {
		/*
		 * Get selected partition
		 */
		TableItem itms[] = PartitionTable.getSelection();
		if (itms != null) {
			String partName = itms[0].getText(0).trim();
			IDEDosPartition part = RootPage.CurrentHandler.GetPartitionByName(partName);

			// Create the hex edit dialog and start it.
			HxEditDialog = new HexEditDialog(ParentComp.getDisplay(), lang);

			if (!ParentComp.isDisposed()) {
				boolean WriteBackData = false;
				byte[] data;
				try {
					data = part.GetAllDataInPartition();
					AddressNote an[] = part.GetAddressNotes();
					WriteBackData = HxEditDialog.Show(data,
							String.format(lang.Msg(Languages.MSG_EDITINGX),  partName + " (" + PLUSIDEDOS.GetTypeAsString(part.GetPartType()) )
							, an,fsd);
					if (WriteBackData) {
						part.SetAllDataInPartition(HxEditDialog.Data);
						part.Reload();
						if (part.GetPartType() == PLUSIDEDOS.PARTITION_SYSTEM) {
							RootPage.LoadFile(new File(RootPage.CurrentDisk.GetFilename()), false);
						}
					}
				} catch (IOException e) {
					ErrorBox(lang.Msg(Languages.MSG_ERRORREADINGPART)+": " + e.getMessage());
					e.printStackTrace();
				}
			}
			HxEditDialog = null;
		}
	}

	/**
	 * Dispose of any open forms owned by this form.
	 */
	protected void DisposeSubDialogs() {
		if (HxEditDialog != null) {
			HxEditDialog.close();
		}
		if (NewPartDialog != null) {
			NewPartDialog.close();
		}
		if (ExportAllPartsForm != null) {
			ExportAllPartsForm.close();
		}
		if (ShrinkDiskForm != null) {
			ShrinkDiskForm.close();
		}
		if (RenFileDialog != null) {
			RenFileDialog.close();
		}
	}

	/**
	 * Do Shrink disk.
	 */
	protected void DoShrinkDisk() {
		SystemPartition sp = (SystemPartition) partition;
		boolean result = false;
		ShrinkDiskForm = new ShrinkDiskDialog(ParentComp.getDisplay(), lang);
		try {
			result = ShrinkDiskForm.Show(sp);
		} finally {
			ShrinkDiskForm = null;
		}
		// Force a reload to make sure all parameters are up to date.
		if (result) {
			RootPage.LoadFile(new File(sp.CurrentDisk.GetFilename()), false);
		}
	}

	protected void DoRenamePartition() {
		TableItem itms[] = PartitionTable.getSelection();
		if ((itms != null) && (itms.length != 0)) {
			String partName = itms[0].getText(0).trim();
			IDEDosPartition part = RootPage.CurrentHandler.GetPartitionByName(partName);
			RenFileDialog = new RenameFileDialog(ParentComp.getDisplay(), lang);
			if (RenFileDialog.Show(part.GetName())) {
				try {
					part.SetName(RenFileDialog.NewName);
					SystemPartition sp = (SystemPartition) partition;
					sp.UpdatePartitionListOnDisk();
					// refresh the screen.
					AddComponents();
				} catch (Exception e) {
					MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_ERROR | SWT.CLOSE);
					String s = String.format(lang.Msg(Languages.MSG_ERRREADNGFILE),part.GetName()) + ": " + e.getMessage();
					messageBox.setMessage(s);
					messageBox.setText(s);
					messageBox.open();
					e.printStackTrace();
				}
			}
			RenFileDialog = null;
		}
	}
}

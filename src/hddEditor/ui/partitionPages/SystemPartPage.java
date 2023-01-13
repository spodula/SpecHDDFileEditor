package hddEditor.ui.partitionPages;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hddEditor.libs.PLUSIDEDOS;
import hddEditor.libs.Speccy;
import hddEditor.libs.GeneralUtils;
import hddEditor.libs.handlers.IDEDosHandler;
import hddEditor.libs.partitions.FreePartition;
import hddEditor.libs.partitions.IDEDosPartition;
import hddEditor.libs.partitions.SystemPartition;
import hddEditor.ui.HDDEditor;
import hddEditor.ui.partitionPages.dialogs.AddressNote;
import hddEditor.ui.partitionPages.dialogs.HexEditDialog;
import hddEditor.ui.partitionPages.dialogs.NewPartitionDialog;

public class SystemPartPage extends GenericPage {
	//Flag to block writing to the disk. 
	private boolean CanEditPartitions = false;

	// Sub-forms so we can close them if they are still open when the page changes.
	HexEditDialog HxEditDialog = null;
	NewPartitionDialog NewPartDialog = null;

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

	// Partition table
	Table PartitionTable = null;

	// Colours for the default colour combos.
	Color colours[] = { new Color(0, 0, 0), new Color(0, 0, 255), new Color(255, 0, 0), new Color(255, 0, 255),
			new Color(0, 255, 0), new Color(0, 255, 255), new Color(255, 255, 0), new Color(255, 255, 255) };

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
	 * @param parent
	 * @param partition
	 */
	public SystemPartPage(HDDEditor root, Composite parent, IDEDosPartition partition) {
		super(root, parent, partition);
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
			int usedSpace = 0;
			int freeSpace = 0;
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
				Label l = label("SYSTEM Details.", 1);
				FontData fontData = l.getFont().getFontData()[0];
				Font font = new Font(ParentComp.getDisplay(),
						new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
				l.setFont(font);
				label("", 3);

				label("Unallocated space: " + GeneralUtils.GetSizeAsString(freeSpace), 1);
				label("Allocated Space: " + GeneralUtils.GetSizeAsString(usedSpace), 1);
				label("Free Partitions: " + (sp.GetMaxPartitions() - numUsedPartitions), 1);
				label("Allocated Partitions: " + numUsedPartitions, 1);

				label("", 4);

				l = label("Default colours:", 1);
				l.setFont(font);

				label("", 1);
				label("Paper", 1);
				label("Ink", 1);

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
				label("Basic editor colour", 1);
				becBright = checkbox("Bright", (col & 0x40) != 0);
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
				label("Default basic colour", 1);
				dbcBright = checkbox("Bright", (col & 0x40) != 0);
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

				unmapA = checkbox("Unmap A:?", sp.GetUnmapA());
				unmapA.addSelectionListener(SetModifiedSelectionListener);

				Unit0DL = editbox(sp.GetUnit0DriveLetter(), 1);
				Unit0DL.addModifyListener(SetModifiedTextListener);
				label("", 1);
				label("", 1);

				unmapB = checkbox("Unmap B:?", sp.GetUnmapB());
				unmapB.addSelectionListener(SetModifiedSelectionListener);
				Unit1DL = editbox(sp.GetUnit1DriveLetter(), 1);
				Unit1DL.addModifyListener(SetModifiedTextListener);

				label("Default drive letter:", 1);
				DefaultDrive = editbox(sp.GetDefaultDrive(), 1);
				DefaultDrive.addModifyListener(SetModifiedTextListener);

				unmapM = checkbox("Unmap M:?", sp.GetUnmapM());
				unmapM.addSelectionListener(SetModifiedSelectionListener);
				UnitMDL = editbox(sp.GetRamdiskDriveLetter(), 1);
				UnitMDL.addModifyListener(SetModifiedTextListener);

				CancelButton = button("Cancel");
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

				ApplyButton = button("Apply changes");
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
				l = label("Partitions:", 1);
				l.setFont(font);
				label("", 3);

				CanEditPartitions = true;
			} else {
				Label l = label("Floppy disk sections:", 1);
				FontData fontData = l.getFont().getFontData()[0];
				Font BoldFont = new Font(ParentComp.getDisplay(),
						new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
				l.setFont(BoldFont);
				label("", 3);
				CanEditPartitions = false;
			}
			
			PartitionTable = new Table(ParentComp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
			PartitionTable.setLinesVisible(true);

			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 4;
			gd.heightHint = 240;
			PartitionTable.setLayoutData(gd);

			TableColumn tc1 = new TableColumn(PartitionTable, SWT.LEFT);
			TableColumn tc2 = new TableColumn(PartitionTable, SWT.LEFT);
			TableColumn tc3 = new TableColumn(PartitionTable, SWT.LEFT);
			TableColumn tc4 = new TableColumn(PartitionTable, SWT.LEFT);
			TableColumn tc5 = new TableColumn(PartitionTable, SWT.LEFT);
			if (partition.CurrentDisk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_HDD) {
				tc1.setText("Partition name");
			} else {
				tc1.setText("FDD Section:");
			}
			tc2.setText("Type");
			tc3.setText("Start");
			tc4.setText("End");
			tc5.setText("Size");
			tc1.setWidth(150);
			tc2.setWidth(150);
			tc3.setWidth(150);
			tc4.setWidth(150);
			tc5.setWidth(100);
			PartitionTable.setHeaderVisible(true);

			for (IDEDosPartition part : sp.partitions) {
				if (part.GetPartType() != 0) {
					TableItem item2 = new TableItem(PartitionTable, SWT.NONE);
					String content[] = new String[5];
					content[0] = part.GetName();
					content[1] = PLUSIDEDOS.GetTypeAsString( part.GetPartType());
					content[2] = "Cyl:" + part.GetStartCyl() + " Head:" + part.GetStartHead();
					content[3] = "Cyl:" + part.GetEndCyl() + " Head:" + part.GetEndHead();
					content[4] = GeneralUtils.GetSizeAsString(part.GetSizeK() * 1024);

					item2.setText(content);
				}
			}

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

			DeleteButton = button("Delete partition");
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

			EditPartitionButton = button("Edit Raw Partition");
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

			GotoPartitionButton = button("Goto partition");
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

			NewPartitionButton = button("New partition");
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
			NewPartitionButton.setEnabled(CanEditPartitions);

			ParentComp.pack();
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
			if ((ApplyButton!= null) && ApplyButton.getEnabled()) {
				MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				messageBox.setMessage("Do you really want to change partition without saving?");
				messageBox.setText("Change Partition");
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
			ErrorBox("Cannot edit partitions on this media type.");
		} else {
		SystemPartition SystemPartition = (SystemPartition) RootPage.CurrentHandler
				.GetPartitionByType(PLUSIDEDOS.PARTITION_SYSTEM);
		if (SystemPartition == null) {
			ErrorBox("Error creating partition: System partition not found.");
		} else {
			ArrayList<String> ExistingPartitions = new ArrayList<String>();
			for (IDEDosPartition p : SystemPartition.partitions) {
				if (p.GetPartType() != 0) {
					ExistingPartitions.add(p.GetName().trim().toUpperCase());
				}
			}
			String sExistingPartitions[] = ExistingPartitions.toArray(new String[0]);

			IDEDosPartition freespacePartition = RootPage.CurrentHandler.GetPartitionByType(PLUSIDEDOS.PARTITION_FREE);
			if (freespacePartition == null) {
				ErrorBox("Error creating partition: Free space partition not found.");
			} else {
				NewPartDialog = new NewPartitionDialog(ParentComp.getDisplay());
				if (NewPartDialog.Show(sExistingPartitions) && !ParentComp.isDisposed()) {
					/*
					 * Find a free partition with enough space. Note, the flags are for how to deal
					 * with the partition when allocating. If its the last partition, remove the
					 * Free space partition. Otherwise, if its the last allocated partition, but
					 * there are still Partents left, Will need to move the Free space partition,
					 * and setup the new one in the old slot.
					 */
					boolean IsLastPartition = false;
					boolean IsLastFreeSpacePartition = false;
					IDEDosPartition FoundPartiton = null;
					for (IDEDosPartition part : SystemPartition.partitions) {
						if (part.GetPartType() == PLUSIDEDOS.PARTITION_FREE) {
							int PartitonSizeMb = part.GetSizeK() / 1024;
							if (PartitonSizeMb >= NewPartDialog.SizeMB) {
								FoundPartiton = part;
								if (FoundPartiton.DirentNum == SystemPartition.partitions.length - 1) {
									IsLastPartition = true;
								} else {
									if (SystemPartition.partitions[FoundPartiton.DirentNum + 1]
											.GetPartType() == PLUSIDEDOS.PARTITION_UNUSED) {
										IsLastFreeSpacePartition = true;
									}
								}
								break;
							}
						}
					}
					if (FoundPartiton == null) {
						ErrorBox("Error creating partition: Unable to find an empty partition.");
					} else {
						// If the partition isnt the last partition, just use it as is. Dont try to
						// defrag the disk.
						if (!IsLastFreeSpacePartition) {
							FoundPartiton.SetPartType(NewPartDialog.PartType);
							int PartitonSizeMb = FoundPartiton.GetSizeK() / 1024;
							if (PartitonSizeMb != NewPartDialog.SizeMB) {
								PartitonSizeMb = NewPartDialog.SizeMB;
								int NumSectors = PartitonSizeMb * 1024 * 1024 / partition.CurrentDisk.GetSectorSize();

								int NumCyls = NumSectors / partition.CurrentDisk.GetNumSectors();
								if (NumSectors % partition.CurrentDisk.GetNumSectors() != 0) {
									NumCyls++;
								}
								int Tracks = NumCyls / partition.CurrentDisk.GetNumHeads();
								int Heads = NumCyls % partition.CurrentDisk.GetNumHeads();

								Heads = Heads + FoundPartiton.GetStartHead();
								if (Heads >= partition.CurrentDisk.GetNumHeads()) {
									Heads = Heads - partition.CurrentDisk.GetNumHeads();
									Tracks++;
								}
								Tracks = Tracks + FoundPartiton.GetStartCyl();

								FoundPartiton.SetEndCyl(Tracks);
								FoundPartiton.SetEndHead(Heads);
								FoundPartiton.UpdateEndSector();

							}
							FoundPartiton = IDEDosHandler.GetNewPartitionByType(NewPartDialog.PartType,
									FoundPartiton.DirentLocation, FoundPartiton.CurrentDisk, FoundPartiton.RawPartition,
									FoundPartiton.DirentNum, false);
							SystemPartition.partitions[FoundPartiton.DirentNum] = FoundPartiton;
							FoundPartiton.SetName(NewPartDialog.Name);

						} else {
							int PartitonSizeMb = NewPartDialog.SizeMB;
							int NumSectors = PartitonSizeMb * 1024 * (1024 / partition.CurrentDisk.GetSectorSize());

							// This seems to be hard limit, so fiddle it.
							if (partition.CurrentDisk.GetSectorSize() == 512) {
								if (NumSectors > 32790) {
									NumSectors = 32790;
								}
							} else {
								if (NumSectors > 65580) {
									NumSectors = 65580;
								}
							}

							int NumCyls = NumSectors / partition.CurrentDisk.GetNumSectors();
							if (NumSectors % partition.CurrentDisk.GetNumSectors() != 0) {
								NumCyls++;
							}
							int Tracks = NumCyls / partition.CurrentDisk.GetNumHeads();
							int Heads = NumCyls % partition.CurrentDisk.GetNumHeads();

							IDEDosPartition NewFreePartition = null;

							/*
							 * copy the free space partition. Note, need to duplicate the Raw partition
							 * data, as its passed by reference.
							 */
							if (!IsLastPartition) {
								byte NewRawPartition[] = new byte[FoundPartiton.RawPartition.length];
								System.arraycopy(FoundPartiton.RawPartition, 0, NewRawPartition, 0,
										NewRawPartition.length);

								NewFreePartition = new FreePartition(FoundPartiton.DirentLocation + 0x40,
										FoundPartiton.CurrentDisk, NewRawPartition, FoundPartiton.DirentNum + 1, false);
								// Update the cylinder
								int NewTrack = FoundPartiton.GetStartCyl() + Tracks;
								int NewHead = FoundPartiton.GetStartHead() + Heads;
								if (NewHead > FoundPartiton.CurrentDisk.GetNumHeads()) {
									NewHead = 0;
									NewTrack++;
								}

								NewFreePartition.SetStartCyl(NewTrack);
								NewFreePartition.SetStartHead(NewHead);
								// Set the new type and add it.
								NewFreePartition.SetPartType(PLUSIDEDOS.PARTITION_FREE);
								NewFreePartition.UpdateEndSector();
								SystemPartition.partitions[NewFreePartition.DirentNum] = NewFreePartition;
								// extract the track and head information to use as the end of the new
								// partition.
								Tracks = NewFreePartition.GetStartCyl();
								Heads = NewFreePartition.GetStartHead();
								Heads = Heads - 1;
								if (Heads < 0) {
									Heads = FoundPartiton.CurrentDisk.GetNumHeads() - 1;
									Tracks = Tracks - 1;
								}
							} else {
								// If we havent created a new free space partition, use the old partitions
								// details.
								Tracks = FoundPartiton.GetStartCyl() + Tracks;
								Heads = FoundPartiton.GetStartHead() + Heads;
								if (Heads > FoundPartiton.CurrentDisk.GetNumHeads()) {
									Heads = 0;
									Tracks++;
								}
								// Now, modify the old partition
							}
							FoundPartiton.SetName(NewPartDialog.Name);
							FoundPartiton.SetPartType(NewPartDialog.PartType);
							FoundPartiton.SetEndCyl(Tracks);
							FoundPartiton.SetEndHead(Heads);

							// Re-create as the proper type
							FoundPartiton = IDEDosHandler.GetNewPartitionByType(NewPartDialog.PartType,
									FoundPartiton.DirentLocation, FoundPartiton.CurrentDisk, FoundPartiton.RawPartition,
									FoundPartiton.DirentNum, true);

							SystemPartition.partitions[FoundPartiton.DirentNum] = FoundPartiton;
						}
						RootPage.UpdateDropdown();
					}
					SystemPartition.UpdatePartitionListOnDisk();
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
			ErrorBox("Cannot Delete partitions on this media type.");
		} else {
		TableItem itms[] = PartitionTable.getSelection();
		if (itms != null) {
			SystemPartition SystemPartition = (SystemPartition) RootPage.CurrentHandler
					.GetPartitionByType(PLUSIDEDOS.PARTITION_SYSTEM);
			if (SystemPartition == null) {
				ErrorBox("Error Deleting partition: System partition not found. This should not happen");
			} else {
				String partName = itms[0].getText(0).trim();
				IDEDosPartition part = RootPage.CurrentHandler.GetPartitionByName(partName);
				MessageBox messageBox = new MessageBox(ParentComp.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
				messageBox.setMessage("Are you sure?");
				messageBox.setText("Are you absolutely sure you want to delete " + partName.trim()
						+ "?\nThis action is cant be undone!");
				if (messageBox.open() == SWT.OK) {
					/*
					 * Set the partition to FREE, and reallocate it.
					 */
					part.SetPartType(PLUSIDEDOS.PARTITION_FREE);

					IDEDosPartition NewFreePartition = IDEDosHandler.GetNewPartitionByType(PLUSIDEDOS.PARTITION_FREE,
							part.DirentLocation, part.CurrentDisk, part.RawPartition, part.DirentNum, false);

					NewFreePartition.SetPartType(PLUSIDEDOS.PARTITION_FREE);

					/*
					 * Try to merge together any free partitions.
					 */
					if (NewFreePartition.DirentNum != SystemPartition.partitions.length - 1) {
						if (SystemPartition.partitions[part.DirentNum + 1].GetPartType() == PLUSIDEDOS.PARTITION_FREE) {
							// Get the partition
							IDEDosPartition OldFreePartiton = SystemPartition.partitions[NewFreePartition.DirentNum
									+ 1];
							// Merge the partitions
							NewFreePartition.SetEndCyl(OldFreePartiton.GetEndCyl());
							NewFreePartition.SetEndHead(OldFreePartiton.GetEndHead());
							// reset the partition type
							OldFreePartiton.SetPartType(PLUSIDEDOS.PARTITION_UNUSED);
							// and set it back.
							SystemPartition.partitions[OldFreePartiton.DirentNum] = OldFreePartiton;
						}
					}
					/*
					 * Update the partition list and on the disk.
					 */
					SystemPartition.partitions[NewFreePartition.DirentNum] = NewFreePartition;
					SystemPartition.UpdatePartitionListOnDisk();
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
			HxEditDialog = new HexEditDialog(ParentComp.getDisplay());

			if (!ParentComp.isDisposed()) {
				boolean WriteBackData = false;
				byte[] data;
				try {
					data = part.GetAllDataInPartition();
					AddressNote an[] = part.GetAddressNotes();
					WriteBackData = HxEditDialog.Show(data, "Editing " + partName + " (" +PLUSIDEDOS.GetTypeAsString( part.GetPartType()) + ")",
							an);
					if (WriteBackData) {
						part.SetAllDataInPartition(HxEditDialog.Data);
						part.Reload();
						if (part.GetPartType() == PLUSIDEDOS.PARTITION_SYSTEM) {
							RootPage.LoadFile(RootPage.CurrentDisk.GetFilename());
						}
					}
				} catch (IOException e) {
					ErrorBox("Error reading partition: " + e.getMessage());
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

	}

}

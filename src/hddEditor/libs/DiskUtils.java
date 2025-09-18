package hddEditor.libs;
/**
 * Utilities to try to identify disk types and return the disk object
 * 
 * 
 */

import java.io.File;
import java.io.IOException;

import hddEditor.libs.disks.Disk;
import hddEditor.libs.disks.FDD.AMSDiskFile;
import hddEditor.libs.disks.FDD.BadDiskFileException;
import hddEditor.libs.disks.FDD.MGTDiskFile;
import hddEditor.libs.disks.FDD.SCLDiskFile;
import hddEditor.libs.disks.FDD.TrDosDiskFile;
import hddEditor.libs.disks.HDD.IDEDosDisk;
import hddEditor.libs.disks.HDD.RS_IDEDosDisk;
import hddEditor.libs.disks.LINEAR.MDFMicrodriveFile;
import hddEditor.libs.disks.LINEAR.TAPFile;
import hddEditor.libs.disks.LINEAR.TZXFile;
import hddEditor.libs.handlers.IDEDosHandler;
import hddEditor.libs.handlers.LinearTapeHandler;
import hddEditor.libs.handlers.NonPartitionedDiskHandler;
import hddEditor.libs.handlers.OSHandler;

public class DiskUtils {
	/**
	 * Try to identify the Disk format and return the disk
	 * 
	 * @param file
	 * @param lang
	 * @return
	 * @throws IOException 
	 * @throws BadDiskFileException
	 */
	public static Disk GetCorrectDiskFromFile(File file,Languages lang) throws IOException {
		Disk result = null;
		try {
			if (new IDEDosDisk().IsMyFileType(file)) {
				result = new IDEDosDisk(file,0,0,0);
			} else if (new RS_IDEDosDisk().IsMyFileType(file)) {
				result = new RS_IDEDosDisk(file);
			} else if (new AMSDiskFile().IsMyFileType(file)) {
				result = new AMSDiskFile(file);
			} else if (new SCLDiskFile().IsMyFileType(file)) {
				result = new SCLDiskFile(file);
			} else if (new TrDosDiskFile().IsMyFileType(file)) {
				result = new TrDosDiskFile(file);
			} else if (new MGTDiskFile().IsMyFileType(file)) {
				result = new MGTDiskFile(file);
			} else if (new MDFMicrodriveFile().IsMyFileType(file)) {
				result = new MDFMicrodriveFile(file);
			} else if (new TAPFile().IsMyFileType(file)) {
				result = new TAPFile(file);
			} else if (new TZXFile().IsMyFileType(file)) {
				result = new TZXFile(file);
			} else {				 
				throw new IOException(String.format(lang.Msg(Languages.MSG_ERRUNRECOGNISED),file.getAbsolutePath()));
			}
			/*
			 * System.out.println("Cylinders " + result.GetNumCylinders());
			 * System.out.println("Heads " + result.GetNumHeads());
			 * System.out.println("SPT " + result.GetNumSectors());
			 */
			if (result != null)
				System.out.println(lang.Msg(Languages.MSG_USING) + " " + result.getClass().getName());
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(String.format(lang.Msg(Languages.MSG_ERRORLOADING),file.getAbsolutePath()) + ": "+e.getMessage());
		}

		return result;
	}
	
	/**
	 * Get the high level handler for the disk. (Provides access to files) 
	 * 
	 * @param disk
	 * @param lang
	 * @return
	 * @throws IOException
	 */
	public static OSHandler GetHandlerForDisk(Disk disk,Languages lang) throws IOException {
		OSHandler result = null;
		if (disk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_HDD) {
			result = new IDEDosHandler(disk, lang);
		} else if (disk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_FDD) {
			result = new NonPartitionedDiskHandler(disk, lang);
		} else if (disk.GetMediaType() == PLUSIDEDOS.MEDIATYPE_LINEAR) {
			result = new LinearTapeHandler(disk, lang);
		} else {
			throw new IOException(String.format(lang.Msg(Languages.MSG_ERRNOFILEHANDLER),disk.GetFilename()));
		}
		return(result);
	}
	
	/**
	 * Load the given disk and return an OS handler object.
	 * 
	 * @param sourcefile
	 * @param lang
	 * @return
	 */
	public static OSHandler LoadDiskDetails(File sourcefile,Languages lang) {
		try {
			Disk CurrentDisk = DiskUtils.GetCorrectDiskFromFile(sourcefile, lang);
			if (CurrentDisk != null) {
				OSHandler CurrentHandler = DiskUtils.GetHandlerForDisk(CurrentDisk,lang);
				return(CurrentHandler);
			}
			return(null);
		} catch (IOException e) {
			return(null);
		} catch (ArrayIndexOutOfBoundsException e) {
			return(null);			
		}
	}
}

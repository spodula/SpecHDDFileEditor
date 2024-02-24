# SpecHDDFileEditor
This is a java based editor for either Realsoft HDF files or Raw disk Image files in both 8 and 16 bit versions as well as amstrad DSK, TR-DOS files, TAP, TZX (limited), MGT(speccy types only) and MDR files.

Limitations on supported file types: https://github.com/spodula/SpecHDDFileEditor/wiki/File-support

Features:
* Open and create Raw Disk image files OR HDF files in both 8 or 16 bit variants. 
* Open, read and write DSK and TRD files
* Open, read and write MDF microdrive files.
* Convert between Hard disk file types
* Add and delete partitions for various types on hard drives.
* Add and remove spectrum +3 files on +3DOS partitions, TR-DOS files, MGT files, Microdrive files and TAP files:
* * Basic files as both Plain text and pre-tokenised
* * Add JPEG/GIF/PNG files as SCREEN$ files
* * CSV files as Numeric arrays
* * Text files as character arrays
* * Raw CPM files without +3DOS headers 
* View and extract Spectrum files:
* * Decode and View basic files including export as text
* * View and extract SCREEN$ files
* * View and extract code files as binary, Hex and assembly
* * View and extract numeric and character arrays
* Edit files as binary (No higher level edit levels yet)
* Edit System partition details, unmap/remap drives, Default drive,default paper and ink colours.
* Drag and drop files to and from directories to different instances or Dos folders.

File system documentation:
* HDF files: https://sinclair.wiki.zxnet.co.uk/wiki/HDF_format
* IDEDOS: https://sinclair.wiki.zxnet.co.uk/wiki/IDEDOS / http://zxvgs.yarek.com/en-idedos.html
* CPM/+3DOS filesystem: https://cpctech.cpc-live.com/docs/p3xfer.html / https://www.seasip.info/Cpm/format22.html
* TR-DOS filesystem: https://sinclair.wiki.zxnet.co.uk/wiki/TR-DOS_filesystem
* MDR file: These are just Raw files with a Microdrive filesystem in them. Details of the filesystem are at  https://sinclair.wiki.zxnet.co.uk/wiki/ZX_Interface_1
* TAP file: https://sinclair.wiki.zxnet.co.uk/wiki/TAP_format
* MGT file: https://sinclair.wiki.zxnet.co.uk/wiki/MGT_filesystem
* TZX file: https://k1.spdns.de/Develop/Projects/zasm/Info/TZX%20format.html

Requires at least Java 11 (I use OpenJDK 18 on Linux.)

Building:
Copy the appropriate POM file. 
mvn clean package
Should leave a JAR file in the target folder. 

Running:
Depending on your java installation and OS, you may be able to double-click on the file. (On most linux distributions, you will have to explicity set the +x flag in file properties)
Alternatively, from the command line:

  Java -jar HDDEditorxxx.jar
  
Documentation:
https://github.com/spodula/SpecHDDFileEditor/wiki
  


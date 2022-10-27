# SpecHDDFileEditor
This is a java based editor for either Realsoft HDF files or Raw disk Image files in both 8 and 16 bit versions.

File system documentation:
* HDF files: https://sinclair.wiki.zxnet.co.uk/wiki/HDF_format
* IDEDOS: https://sinclair.wiki.zxnet.co.uk/wiki/IDEDOS / http://zxvgs.yarek.com/en-idedos.html
* CPM/+3DOS filesystem: https://cpctech.cpc-live.com/docs/p3xfer.html / https://www.seasip.info/Cpm/format22.html

Requires at least Java 11

Building:
Copy the appropriate POM file. 
mvn clean package
Should leave a JAR file in the target folder. 

Running:
Depending on your java installation and OS, you may be able to double-click on the file. 
Alternatively, from the command line:
  Java -jar HDDEditorxxx.jar
  


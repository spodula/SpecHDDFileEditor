package hddEditor.libs.snapshots.readers.zxStateBlocks;
/*-*
 * Implements the ZXSTPLUS3 block
 * https://www.spectaculator.com/docs/zx-state/plus3disk.shtml
 * Details of floppy disks connected to a +3 
 * 
 * $00..$03 	ID: "+3",$0,$0 ) Decoded by the parent class. 
 * $04..$07 	size: LSB..MSB ) Should always be 2 
 * ====start of ZXSTPLUS3 information====
 * $08 			Number of drives (1 or 2)
 * $09			Motor on
 */

public class ZXStPlus3 extends GenericZXStateBlock {
		public int numdrives;
		public boolean MotorOn;
		
		public ZXStPlus3(byte[] rawdata, int start) {
			super(rawdata, start);
			numdrives= rawbyte(0x08);
			MotorOn = (rawbyte(0x09)==0x01);
		}
		
		/**
		 * Add in the ZXStUSpeech values.
		 */
		@Override
		public String toString() {
			String result = super.toString();
			result = result + " Num drives:"+ numdrives+" Motor on?"+MotorOn;
			return (result.trim());
		}
}

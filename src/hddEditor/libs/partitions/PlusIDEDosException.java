package hddEditor.libs.partitions;

public class PlusIDEDosException extends Exception {
	private static final long serialVersionUID = 333506413544134635L;
	public String partition;
	public PlusIDEDosException(String Partition, String message) {
		this.partition = Partition;
	}
}

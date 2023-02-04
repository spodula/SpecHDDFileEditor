package hddEditor.libs.partitions;

public interface ProgressCallback {
	boolean Callback(int max, int value, String text);
}

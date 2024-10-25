package hddEditor.libs;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HtmlHelp {
	public static String baseURL = "https://github.com/spodula/SpecHDDFileEditor/wiki/";
	
	public static void DoHelp(String context)  {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
		    try {
				Desktop.getDesktop().browse(new URI(baseURL+context));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}
}

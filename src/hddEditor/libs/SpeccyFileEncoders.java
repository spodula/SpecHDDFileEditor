package hddEditor.libs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.eclipse.swt.widgets.Display;

/**
 * This file contains the functions used to encode various filetypes into
 * Spectrum basic compatable files.
 * 
 * @author graham
 *
 */

public class SpeccyFileEncoders {
	/**
	 * Read the file and tokenise it. Note, no syntax checking is done, so if your
	 * basic is invalid, it will still be added
	 * 
	 * @param filedets
	 * @return
	 */
	public static byte[] EncodeTextFileToBASIC(File filedets) {
		BufferedReader br;
		/*
		 * Read the file and tokenize it. Note, no syntax checking is done, so if your
		 * basic is invalid, it will still be added
		 */
		byte BasicAsBytes[] = new byte[0xffff];
		int targetPtr = 0;
		try {
			br = new BufferedReader(new FileReader(filedets));
			try {
				String line;
				while ((line = br.readLine()) != null) {
					targetPtr = Speccy.DecodeBasicLine(line, BasicAsBytes, targetPtr);
				}
			} finally {
				br.close();
			}
		} catch (FileNotFoundException e) {
			System.out.println("File " + filedets.getAbsolutePath() + " cannot be opened.");
		} catch (IOException e) {
			System.out.println("File " + filedets.getAbsolutePath() + " IO Error: " + e.getMessage());
		}

		// Copy to an array of the correct size.
		byte data[] = new byte[targetPtr];
		System.arraycopy(BasicAsBytes, 0, data, 0, targetPtr);
		return (data);
	}

	/**
	 * Load a given image file and scale it to zx Spectrum format and return in
	 * SCREEN$ file
	 * 
	 * @param display
	 * @param bwSlider
	 * @param file
	 * @param isBW
	 * @return
	 * @throws IOException
	 */
	public static byte[] LoadImage(Display display, int bwSlider, File file, boolean isBW) throws IOException {
		BufferedImage RawImage = ImageIO.read(file);
		return (ScaleImage(display, bwSlider, RawImage, isBW));
	}

	/**
	 * Scale the loaded image to 256x192 into a new image, run the speccy display
	 * conversion, then return it as a screen$ compatible image.
	 * 
	 * @param selected
	 * @return
	 */
	public static byte[] ScaleImage(Display display, int bwSlider, BufferedImage RawImage, boolean isBW) {
		BufferedImage TargetImg = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
		byte result[] = null;
		if (RawImage != null) {
			// Draw the loaded image to the new buffer
			Graphics2D graphics2D = TargetImg.createGraphics();
			graphics2D.drawImage(RawImage, 0, 0, 256, 192, null);
			graphics2D.dispose();
			
			// process it.

			if (isBW) {
				result = RenderBW(TargetImg, bwSlider);
			} else {
				result = RenderColour(TargetImg, bwSlider);
			}
		}
		return result;
	}

	/**
	 * Render the currently loaded image into a Spectrum compatible Coloured screen$
	 * file (6912 bytes of 8 colour-clashing goodness!). The conversion works, but
	 * is not very good. There are better libraries available. will use a decent one
	 * later.
	 * 
	 * @param SourceImage - Image to convert
	 * @param bwSlider    - Colour cutoff (0->255)
	 * @return - Screen$
	 */
	//TODO: scaling images with colours doesnt work properly.
	private static byte[] RenderColour(BufferedImage SourceImage, int CutoffSlider) {
		byte Screen[] = new byte[6912];
		int compval = (CutoffSlider * 256) / 100;
		// scale to 8 colours
		for (int x = 0; x < 256; x++) {
			for (int y = 0; y < 192; y++) {
				int col = SourceImage.getRGB(x, y);
				int red = (col & 0xff0000) >> 16;
				int green = (col & 0xff00) >> 8;
				int blue = col & 0xff;

				if (red > compval) {
					red = 0xff;
				} else {
					red = 0x00;
				}
				if (green > compval) {
					green = 0xff;
				} else {
					green = 0x00;
				}
				if (blue > compval) {
					blue = 0xff;
				} else {
					blue = 0x00;
				}

				col = (red << 16) + (green << 8) + blue;
				SourceImage.setRGB(x, y, col);
			}
		}
		// Group into attributes
		int attriblocation = 0x1800;
		int colours[] = new int[8];
		for (int y = 0; y < 24; y++) {
			for (int x = 0; x < 32; x++) {
				// Blank the colour indexes
				for (int i = 0; i < 8; i++) {
					colours[i] = 0;
				}
				// base positions.
				int basex = x * 8;
				int basey = y * 8;
				// get the square
				for (int a = 0; a < 7; a++) {
					for (int b = 0; b < 7; b++) {
						// col = 00000000 RRRRRRRR GGGGGGGG BBBBBBBB
						// Speccy = 00000GRB
						int col = SourceImage.getRGB(basex + a, basey + b);
						int red = (col >> 16) & 0x02;
						int green = (col >> 8) & 0x04;
						int blue = (col & 0x01);

						col = red + green + blue;
						colours[col]++;
					}
				}
				// find the max and max-1
				int ink = 0;
				int paper = 0;

				int maxnum = 0;
				for (int i = 0; i < 8; i++) {
					if (colours[i] > maxnum) {
						ink = i;
						maxnum = colours[i];
					}
				}

				colours[ink] = 0;
				maxnum = 0;
				for (int i = 0; i < 8; i++) {
					if (colours[i] > maxnum) {
						paper = i;
						maxnum = colours[i];
					}
				}
				if (maxnum == 0) {
					paper = ink;
				}
				// make an array of colours
				int newcolours[] = new int[8];
				for (int i = 0; i < 8; i++) {
					newcolours[i] = Speccy.colours[ink];
				}
				newcolours[paper] = Speccy.colours[paper];

				// rewrite the square
				Screen[attriblocation++] = (byte) (ink + (paper * 8));
				for (int a = 0; a < 8; a++) {
					int byt = 0;
					for (int b = 0; b < 8; b++) {
						int col = SourceImage.getRGB(basex + b, basey + a);
						int red = (col >> 16) & 0x02;
						int green = (col >> 8) & 0x04;
						int blue = (col & 0x01);
						col = red + green + blue;

						int newcol = newcolours[col];

						SourceImage.setRGB(basex + b, basey + a, newcol);
						// calculate if we are ink or paper.
						byt = byt << 1;
						if (newcol == Speccy.colours[ink]) {
							byt = byt + 1;
						}
					}
					// calculate the pixel data location [ 000 aabbb cccxxxxx ] where yptn =
					// [aacccbbb]
					int yptn = basey + a;
					int y1 = yptn & 0x07;
					int y2 = (yptn & 0x38) >> 3;
					int y3 = (yptn & 0xc0) >> 6;
					int address = (y3 << 11) + (y1 << 8) + (y2 << 5) + (basex >> 3);
					// write the pixel data
					Screen[address] = (byte) (byt & 0xff);

				}
			}
		}
		return (Screen);

	}

	/**
	 * Render the currently loaded images as a black and white image to a screen$.
	 * Note RGB->lum values are from ITU BT.601.
	 * 
	 * @param SourceImage - Image to render
	 * @param bwSlider    - Intensity cutoff
	 * @return - Screen$
	 */
	private static byte[] RenderBW(BufferedImage SourceImage, int bwSlider) {
		byte Screen[] = new byte[6912];
		// store for pixel data
		boolean pixels[] = new boolean[49152];
		int pxIdx = 0;

		// loop every pixel.
		for (int y = 0; y < 192; y++) {
			for (int x = 0; x < 256; x++) {
				// get the RGB values
				int col = SourceImage.getRGB(x, y);
				int red = (col & 0xff0000) >> 16;
				int green = (col & 0xff00) >> 8;
				int blue = col & 0xff;

				// convert into a luminance (Greyscale) value.
				double lum = (0.299 * red) + (0.587 * green) + (0.114 * blue);
				int iLum = (int) Math.round(lum * 100 / 256);

				// See if the Luminance crosses the value, if so set the local image.
				if (iLum > bwSlider) {
					iLum = 0xffffff;
					pixels[pxIdx++] = false;
				} else {
					iLum = 0x00;
					pixels[pxIdx++] = true;
				}

				SourceImage.setRGB(x, y, iLum);
			}
		}

		pxIdx = 0;
		for (int y = 0; y < 192; y++) {
			// calculate the pixel data location [ 000 aabbb cccxxxxx ] where yptn =
			// [aacccbbb]
			int y1 = y & 0x07;
			int y2 = (y & 0x38) >> 3;
			int y3 = (y & 0xc0) >> 6;
			int baseYAddress = (y3 << 11) + (y1 << 8) + (y2 << 5);

			// write the line
			for (int x = 0; x < 32; x++) {
				int byt = 0;
				for (int b = 0; b < 8; b++) {
					boolean px = pixels[pxIdx++];
					int col = 0;
					if (px) {
						col = 1;
					}
					byt = (byt << 1) + col;
				}
				int address = baseYAddress + x;
				// write the pixel data
				Screen[address] = (byte) (byt & 0xff);
			}
		}
		// make the entire attribute area black on white.
		byte wob = Speccy.ToAttribute(Speccy.COLOUR_BLACK, Speccy.COLOUR_WHITE, false, false);
		for (int i = 0x1800; i < 0x1b00; i++) {
			Screen[i] = wob;
		}

		return (Screen);
	}

	public static byte[] EncodeCharacterArray(File filename, int linelimit) throws IOException {
		/*
		 * Iterate all the selected files.
		 */
		ArrayList<String> lines = new ArrayList<String>();
		int numlines = 0;
		String CSVline;
		BufferedReader br = new BufferedReader(new FileReader(filename));
		try {
			while (((CSVline = br.readLine()) != null) && numlines < linelimit) {
				lines.add(CSVline);
				numlines++;
			}
		} finally {
			br.close();
		}
		if (numlines == linelimit) {
			System.out.println("Load stopped at " + linelimit + " lines. Too large");
		} else {
			System.out.println("Loaded " + numlines + " lines.");
		}

		// get second dimension from the file.
		int maxdim2 = 1;
		for (String line : lines) {
			String columns[] = SplitLine(line, ", \t", 1);
			if (columns.length > maxdim2)
				maxdim2 = columns.length;
		}
		// System.out.println("Number of columns is: " + maxdim2);

		// number of diumensions.
		int dimensions = 1;
		if (maxdim2 > 1) {
			dimensions = 2;
		}

		// dimensions. ((X * Y) Data) + 2 bytes for each dimension + 1 for dimension no.
		int arraysize = (maxdim2 * lines.size()) + (dimensions * 2) + 1;
		// System.out.println("Calcsize: " + arraysize);

		byte ArrayAsBytes[] = new byte[arraysize];

		// dimensions.

		int ptr = 1;
		// Each dimension.
		ArrayAsBytes[ptr++] = (byte) (lines.size() & 0xff);
		ArrayAsBytes[ptr++] = (byte) (lines.size() / 0x100);
		ArrayAsBytes[0] = 1;
		if (maxdim2 > 1) {
			ArrayAsBytes[0] = 2;
			ArrayAsBytes[ptr++] = (byte) (maxdim2 & 0xff);
			ArrayAsBytes[ptr++] = (byte) (maxdim2 / 0x100);
		}

		// for each item.
		for (int dim1 = 0; dim1 < lines.size(); dim1++) {
			// pad line to at least 255 characters
			String line = lines.get(dim1) + "                                                                "
					+ "                                                                "
					+ "                                                                "
					+ "                                                                ";
			// write string to the array
			for (int dim2 = 0; dim2 < maxdim2; dim2++) {
				char c = line.charAt(dim2);
				ArrayAsBytes[ptr++] = (byte) c;
			}
		}
		return (ArrayAsBytes);
	}

	/**
	 * very basic parser. Will take into account quotes.
	 *
	 * @param line    - Line to split
	 * @param splitby - list of characters to use as delimiters
	 * @return - Array of strings.
	 */
	private static String[] SplitLine(String line, String splitby, int padto) {
		ArrayList<String> al = new ArrayList<String>();
		String curritem = "";
		boolean InQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (InQuotes) {
				if (c == '"') {
					InQuotes = false;
					if (!curritem.isEmpty()) {
						al.add(curritem);
						curritem = "";
					}
				} else {
					curritem = curritem + c;
				}
			} else {
				if (c == '"') {
					InQuotes = true;
					if (!curritem.isEmpty()) {
						al.add(curritem);
						curritem = "";
					}
				} else {
					if (splitby.indexOf(c) > -1) {
						if (!curritem.isEmpty()) {
							al.add(curritem);
							curritem = "";
						}
					} else {
						curritem = curritem + c;
					}
				}
			}
		}

		while (al.size() < padto) {
			al.add("");
		}
		String result[] = al.toArray(new String[0]);

		return result;
	}

	/**
	 * Encode a CSV file into a numeric array
	 * 
	 * @param filename
	 * @param linelimit
	 * @return
	 * @throws IOException
	 */
	public static byte[] EncodeNumericArray(File filename, int linelimit) throws IOException {
		ArrayList<String> lines = new ArrayList<String>();
		/*
		 * Load the file into an array of lines.
		 */
		int numlines = 0;
		String CSVline;
		BufferedReader br = new BufferedReader(new FileReader(filename));
		try {
			while (((CSVline = br.readLine()) != null) && numlines < linelimit) {
				lines.add(CSVline);
				numlines++;
			}
		} finally {
			br.close();
		}
		if (numlines == linelimit) {
			System.out.println("Load stopped at " + linelimit + " lines. Too large");
		} else {
			System.out.println("Loaded " + numlines + " lines.");
		}

		// get second dimension from the file.
		int maxdim2 = 1;
		for (String line : lines) {
			String columns[] = SplitLine(line, ", \t", 1);
			if (columns.length > maxdim2)
				maxdim2 = columns.length;
		}
		System.out.println("Number of columns is: " + maxdim2);

		// number of diumensions.
		int dimensions = 1;
		if (maxdim2 > 1) {
			dimensions = 2;
		}

		int arraysize = (maxdim2 * lines.size() * 5) + (dimensions * 2) + 1;
		System.out.println("Calcsize:  " + arraysize);

		byte ArrayAsBytes[] = new byte[arraysize];

		// dimensions.

		int ptr = 1;
		// Each dimension.
		ArrayAsBytes[ptr++] = (byte) (lines.size() & 0xff);
		ArrayAsBytes[ptr++] = (byte) (lines.size() / 0x100);
		ArrayAsBytes[0] = 1;
		if (maxdim2 > 1) {
			ArrayAsBytes[0] = 2;
			ArrayAsBytes[ptr++] = (byte) (maxdim2 & 0xff);
			ArrayAsBytes[ptr++] = (byte) (maxdim2 / 0x100);
		}

		// for each item.
		for (int dim1 = 0; dim1 < lines.size(); dim1++) {
			String line = lines.get(dim1);
			String numbers[] = SplitLine(line, ", \t", maxdim2);
			for (int dim2 = 0; dim2 < maxdim2; dim2++) {
				String sNumber = numbers[dim2];
				if (!isNumeric(sNumber)) {
					sNumber = "0";
				}
				Double number = Double.valueOf(sNumber);
				byte[] newnum = Speccy.EncodeValue(number, true);
				for (int i = 1; i < newnum.length; i++) {
					ArrayAsBytes[ptr++] = newnum[i];
				}
			}
		}
		return(ArrayAsBytes);
	}

	/**
	 * Check to see if a given string is numeric.
	 * 
	 * @param strNum
	 * @return
	 */
	private static boolean isNumeric(String strNum) {
		if (strNum == null) {
			return false;
		}
		try {
			Double.parseDouble(strNum);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

}

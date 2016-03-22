package ch.fhnw.i4ds.asctopng;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

/**
 * Converts an ASC file to a PNG heightmap.
 *
 */
public class AscToPngHeightMap {
	public static void main(final String[] args) {
		if (args.length == 0 || args.length > 2) {
			throw new IllegalArgumentException("Usage: AscToRaw inputFile [-D]");
		}

		final boolean debug = args.length > 1 && args[1].equalsIgnoreCase("-d");

		try (BufferedReader reader = Files.newBufferedReader(Paths.get(args[0]))) {

			String line;
			int width = 0;
			int height = 0;
			int length = 0;

			// read header
			for (int lineIndex = 0; lineIndex < 6; ++lineIndex) {
				line = reader.readLine();

				if (line.startsWith("ncols")) {
					width = Integer.valueOf(line.split("\\s+")[1]);
				} else if (line.startsWith("nrows")) {
					height = Integer.valueOf(line.split("\\s+")[1]);
				}
			}

			if (debug) {
				System.out.println("width: " + width);
				System.out.println("height: " + height);
			}

			length = width * height;

			float min = Float.MAX_VALUE;
			float max = -Float.MAX_VALUE;

			final float[] depthMap = new float[length];

			int index = 0;

			if (debug) System.out.println("read data...");

			// read data
			while ((line = reader.readLine()) != null) {
				final String[] split = line.split(" ");
				for (final String stringValue : split) {
					final float value = Float.parseFloat(stringValue);
					depthMap[index++] = value;

					min = Math.min(min, value);
					max = Math.max(max, value);
				}
			}

			if (debug) {
				System.out.println("min: " + min);
				System.out.println("max: " + max);
			}
			// normalize data
			final int[] image = new int[length];
			final float delta = max - min;
			final float normalization = 65536 / delta;

			if (debug) {
				System.out.println("normalization factor: " + normalization);
				System.out.println("normalize");
			}

			for (int depthIndex = 0; depthIndex < length; ++depthIndex) {
				depthMap[depthIndex] -= min;
				image[depthIndex] = (int) (depthMap[depthIndex] * normalization);
			}

			// write image
			if (debug) System.out.println("write image...");

			final BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
			final WritableRaster raster = bufferedImage.getRaster();
			raster.setPixels(0, 0, width, height, image);

			Path folder = Paths.get(args[0]).getParent();
			String inputFileName = Paths.get(args[0]).getFileName().toString();
			String fileName = inputFileName.substring(0,inputFileName.lastIndexOf('.')) + ".png";
			
			final File output = folder.resolve(fileName).toFile();
			try {
				ImageIO.write(bufferedImage, "png", output);
			} catch (final IOException io) {
			}

			if (debug) System.out.println("done writing file: " + output);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}

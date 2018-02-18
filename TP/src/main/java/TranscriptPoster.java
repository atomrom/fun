import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.gson.Gson;

public class TranscriptPoster {
	private static final String OUTPUT_FILE_TYPE = "png";
	
	private String text;
	private BufferedImage picture;

	private BufferedImage img;

	private static final int TAKE_PICTURE_PIXEL_COLOR = -1;
	private int fontColorHint = TAKE_PICTURE_PIXEL_COLOR;
	
	private Config config;
	private static class Config {
		private String outputPath;
		private String textPath;
		private String baseImagePath;

		private int fontSize;

		private int backgroundColor;
		private int defaultFontColor;

		private Map<Integer, Integer> fontColorHints = new HashMap<Integer, Integer>();
	}

	public static void main(String[] args) throws IOException {
		TranscriptPoster a13 = new TranscriptPoster(args[0]);
		a13.readImage();
		a13.renderImage();
	}

	private TranscriptPoster(String configFilePath) throws IOException {
		Reader jsonReader = new FileReader(new File(configFilePath));
		config = new Gson().fromJson(jsonReader, Config.class);

		text = new String(Files.readAllBytes(FileSystems.getDefault().getPath(config.textPath)));
	}

	private void renderImage() throws IOException {
		final float pictureWidth = picture.getWidth();
		final float pictureHeight = picture.getHeight();

		img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = img.createGraphics();
		Font font = new Font("Monospaced", Font.PLAIN, config.fontSize);
		g2d.setFont(font);
		FontMetrics fm = g2d.getFontMetrics();
		final int fontWidth = fm.stringWidth("a");
		final int fontHeight = fm.getHeight();

		final int step = (int) Math
				.sqrt((pictureWidth * (float) (fontHeight * text.length())) / (pictureHeight * (float) fontWidth));

		int width = fm.stringWidth(text.substring(0, step));
		int height = fm.getHeight() * (text.length() / step + 1) + fm.getDescent();
		g2d.dispose();

		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		setBackgroundColor(config.backgroundColor);
		g2d = img.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2d.setFont(font);

		final float charImageWidth = step;
		final float charImageHeight = text.length() / step + 1;

		final float scaledPictureWidth = (pictureWidth * (((float) height) / pictureHeight));
		final int paddingHorizontal = (int) ((width - scaledPictureWidth) / 2f);
		final int paddingHorizontalInChars = paddingHorizontal / fontWidth;

		int r = 0;
		int c = 0;
		for (int index = 0; index < text.length(); ++index) {
			final String characterToPrint = String.valueOf(text.charAt(index));

			int charColumn = c * fontWidth;
			int charRow = (r + 1) * fontHeight;

			if (charColumn <= paddingHorizontal || charColumn >= paddingHorizontal + scaledPictureWidth) {
				g2d.setColor(new Color(config.defaultFontColor));
			} else {
				int x = (int) (pictureWidth / (charImageWidth - 2 * paddingHorizontalInChars)
						* (float) (c - paddingHorizontalInChars));
				int y = (int) (pictureHeight / charImageHeight * (float) r);

				updateFontColorHint(index);

				Color fontColor;
				if (fontColorHint == TAKE_PICTURE_PIXEL_COLOR) {
					fontColor = new Color(picture.getRGB(x, y));
				} else {
					fontColor = new Color(fontColorHint);
				}
				g2d.setColor(fontColor);
			}
			g2d.drawString(characterToPrint, charColumn, charRow);

			if (++c == step) {
				c = 0;
				++r;
			}
		}

		g2d.dispose();
		try {
			ImageIO.write(img, OUTPUT_FILE_TYPE, new File(config.outputPath));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void updateFontColorHint(int index) {
		if (config.fontColorHints.containsKey(index)) {
			fontColorHint = config.fontColorHints.get(index);
		}
	}

	private void setBackgroundColor(int color) {
		for (int x = 0; x < img.getWidth(); ++x) {
			for (int y = 0; y < img.getHeight(); ++y) {
				img.setRGB(x, y, color);
			}
		}
	}

	private BufferedImage readImage() {
		picture = null;
		try {
			picture = ImageIO.read(new File(config.baseImagePath));
		} catch (IOException e) {
		}

		return picture;
	}
}

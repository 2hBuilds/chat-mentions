package com.chatmentions;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * The sidebar button's icon, 16 x 16 px: the 2hBuilds coin top-left ({@link CoinGlyph}) and the plugin's own
 * picture clear of the coin, bottom-right. The picture is a chat bubble outlined in white with a word in the brand
 * orange, and the older messages behind it, stacked right above it and flush with its right edge, each smaller and
 * dimmer the older it is. It is sheet 13's number 1 of {@code docs/pictures}, picked 2026-10-28. Drawn in code, so it
 * needs no image file.
 */
public final class NavIcon
{
	/** The sidebar icon size. */
	public static final int SIZE = 16;

	/** The picture's colours: the oldest message's faint grey, the next one's mid grey, the bubble's white, the orange. */
	private static final int FAINT = new Color(80, 80, 80).getRGB();
	private static final int MID = new Color(125, 125, 125).getRGB();
	private static final int WHITE = new Color(240, 240, 240).getRGB();
	private static final int ORANGE = new Color(220, 138, 0).getRGB();

	/**
	 * The picture, one character a pixel, sixteen rows of sixteen: f the oldest message's faint grey, g the next
	 * one's mid grey, W the bubble's white outline and tail, O the word in orange, '.' nothing (the sidebar's own
	 * grey shows). The same map as the picture file's.
	 */
	private static final String[] PICTURE = {
		"................",
		"................",
		"................",
		".............ff.",
		".............ff.",
		"................",
		"...........gggg.",
		"...........gggg.",
		"................",
		"........WWWWWWW.",
		"........W.....W.",
		"........W.OOO.W.",
		"........W.....W.",
		"........WWWWWWW.",
		".........W......",
		"................",
	};

	private NavIcon()
	{
	}

	/** A new 16 x 16 ARGB image of the icon. */
	public static BufferedImage create()
	{
		final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			CoinGlyph.paint(g);
		}
		finally
		{
			g.dispose();
		}
		for (int y = 0; y < SIZE; y++)
		{
			for (int x = 0; x < SIZE; x++)
			{
				final int argb = colour(PICTURE[y].charAt(x));
				if (argb != 0)
				{
					img.setRGB(x, y, argb);
				}
			}
		}
		return img;
	}

	/** The opaque ARGB value a picture character stands for, or 0 for nothing. */
	private static int colour(char c)
	{
		switch (c)
		{
			case 'f':
				return FAINT;
			case 'g':
				return MID;
			case 'W':
				return WHITE;
			case 'O':
				return ORANGE;
			default:
				return 0;
		}
	}
}

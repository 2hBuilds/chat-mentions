package com.chatmentions;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * The sidebar button's icon, 16 x 16 px: the 2hBuilds coin top-left ({@link CoinGlyph}) and the plugin's own
 * picture clear of the coin, bottom-right. The picture is two stacked mention cards: the back one in soft grey,
 * the front one in white with a line lit in the mention blue, the colour the panel gives a keyword, and a clear
 * ring between the two so they read apart. It is sheet 1's number 4 of {@code docs/pictures}
 * ({@code chat_mentions_picture.py}, {@code icon_d}), picked 2026-10-28. Drawn in code, so it needs no image file.
 */
public final class NavIcon
{
	/** The sidebar icon size. */
	public static final int SIZE = 16;

	/** The picture's colours: the back card's soft grey, the front card's white, the mention blue. */
	private static final int SOFT = new Color(170, 170, 170).getRGB();
	private static final int WHITE = new Color(240, 240, 240).getRGB();
	private static final int BLUE = new Color(59, 130, 246).getRGB();

	/**
	 * The picture, one character a pixel, sixteen rows of sixteen: S the back card's soft grey, W the front card's
	 * white, B the mention blue, '.' nothing (the sidebar's own grey shows). The same map as the picture file's.
	 */
	private static final String[] PICTURE = {
		"................",
		"................",
		"................",
		"................",
		"................",
		"................",
		"................",
		"..........SSSSSS",
		"..........S....S",
		"...............S",
		".......WWWWWWW.S",
		".......W.....W.S",
		".......W.BBB.W..",
		".......W.....W..",
		".......WWWWWWW..",
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
			case 'S':
				return SOFT;
			case 'W':
				return WHITE;
			case 'B':
				return BLUE;
			default:
				return 0;
		}
	}
}

package com.chatmentions;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link NavIcon}, in memory: the size, the coin exactly as {@link CoinGlyph} draws it, the picture clear of it, and
 * the picture itself pinned: the outlined white bubble with an orange word and the older messages stacked behind
 * it, sheet 13's number 1, picked 2026-10-28.
 */
public class NavIconTest
{
	/** The coin's centre and the farthest its jittered rim reaches from it, with a pixel of margin. */
	private static final double COIN_CENTRE = 4.5;
	private static final double CLEAR_DISTANCE = 6.0;

	/** The picture's colours, as opaque ARGB: the faint and mid greys of the older messages, the bubble's white, the orange. */
	private static final int FAINT = 0xFF505050;
	private static final int MID = 0xFF7D7D7D;
	private static final int WHITE = 0xFFF0F0F0;
	private static final int ORANGE = 0xFFDC8A00;

	/** How many pixels the picture adds to the coin: 21 white, 4 faint grey, 8 mid grey, 3 orange. */
	private static final int PICTURE_PIXELS = 36;

	private static BufferedImage coinAlone()
	{
		final BufferedImage img = new BufferedImage(NavIcon.SIZE, NavIcon.SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			CoinGlyph.paint(g);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	private static void assertPixel(String what, int expected, BufferedImage icon, int x, int y)
	{
		assertEquals(what + " at " + x + "," + y, expected, icon.getRGB(x, y));
	}

	private static void assertClear(String what, BufferedImage icon, int x, int y)
	{
		assertEquals(what + " at " + x + "," + y + " is clear", 0, icon.getRGB(x, y) >>> 24);
	}

	@Test
	public void theIconIsSixteenBySixteenArgb()
	{
		final BufferedImage icon = NavIcon.create();
		assertEquals(16, icon.getWidth());
		assertEquals(16, icon.getHeight());
		assertEquals(BufferedImage.TYPE_INT_ARGB, icon.getType());
	}

	@Test
	public void theCoinIsDrawnPixelForPixelAsTheCoinGlyphDrawsIt()
	{
		final BufferedImage icon = NavIcon.create();
		final BufferedImage coin = coinAlone();
		int coinPixels = 0;
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				if ((coin.getRGB(x, y) >>> 24) != 0)
				{
					coinPixels++;
					assertEquals("coin pixel " + x + "," + y, coin.getRGB(x, y), icon.getRGB(x, y));
				}
			}
		}
		assertTrue("the coin drew something", coinPixels > 0);
	}

	@Test
	public void thePictureLiesClearOfTheCoinsCircle()
	{
		final BufferedImage icon = NavIcon.create();
		final BufferedImage coin = coinAlone();
		int pictureOnly = 0;
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				if (icon.getRGB(x, y) != coin.getRGB(x, y))
				{
					pictureOnly++;
					final double dx = x + 0.5 - COIN_CENTRE;
					final double dy = y + 0.5 - COIN_CENTRE;
					assertTrue("picture pixel " + x + "," + y + " is too near the coin",
						Math.sqrt(dx * dx + dy * dy) >= CLEAR_DISTANCE);
				}
			}
		}
		assertTrue("the picture drew something", pictureOnly > 0);
	}

	@Test
	public void thePictureIsTheBubbleWithTheChatBehindIt()
	{
		final BufferedImage icon = NavIcon.create();
		final BufferedImage coin = coinAlone();
		int differing = 0;
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				if (icon.getRGB(x, y) != coin.getRGB(x, y))
				{
					differing++;
				}
			}
		}
		assertEquals("the picture's pixels", PICTURE_PIXELS, differing);

		// Rows 0 to 2, 5 and 8 hold the coin alone; row 15 and column 15 are clear.
		final int[] coinRows = {0, 1, 2, 5, 8};
		for (int x = 0; x < NavIcon.SIZE; x++)
		{
			for (final int y : coinRows)
			{
				assertEquals("row " + y + " is the coin alone at " + x, coin.getRGB(x, y), icon.getRGB(x, y));
			}
			assertClear("row 15", icon, x, 15);
			assertClear("column 15", icon, 15, x);
		}

		// The far back: faint grey, two by two.
		assertPixel("faint", FAINT, icon, 13, 3);
		assertPixel("faint", FAINT, icon, 14, 3);
		assertPixel("faint", FAINT, icon, 13, 4);
		assertPixel("faint", FAINT, icon, 14, 4);
		assertClear("beside the far back", icon, 12, 3);
		assertClear("beside the far back", icon, 15, 3);
		// The mid back: mid grey, four by two.
		assertPixel("mid grey", MID, icon, 11, 6);
		assertPixel("mid grey", MID, icon, 14, 6);
		assertPixel("mid grey", MID, icon, 11, 7);
		assertPixel("mid grey", MID, icon, 14, 7);
		assertClear("beside the mid back", icon, 10, 6);
		// The bubble: white corners and the tail, the hollow clear.
		assertPixel("white", WHITE, icon, 8, 9);
		assertPixel("white", WHITE, icon, 14, 9);
		assertPixel("white", WHITE, icon, 8, 13);
		assertPixel("white", WHITE, icon, 14, 13);
		assertPixel("the tail", WHITE, icon, 9, 14);
		assertClear("beside the tail", icon, 8, 14);
		assertClear("beside the tail", icon, 10, 14);
		assertClear("the bubble's hollow", icon, 9, 10);
		assertClear("the bubble's hollow", icon, 13, 12);
		// The word: three orange pixels with air either side.
		assertPixel("orange", ORANGE, icon, 10, 11);
		assertPixel("orange", ORANGE, icon, 11, 11);
		assertPixel("orange", ORANGE, icon, 12, 11);
		assertClear("the air before the word", icon, 9, 11);
		assertClear("the air after the word", icon, 13, 11);
	}
}

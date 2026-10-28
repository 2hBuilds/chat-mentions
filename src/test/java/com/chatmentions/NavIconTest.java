package com.chatmentions;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link NavIcon}, in memory: the size, the coin exactly as {@link CoinGlyph} draws it, the picture clear of it, and
 * the picture itself pinned: the two stacked cards of sheet 1's number 4, picked 2026-10-28.
 */
public class NavIconTest
{
	/** The coin's centre and the farthest its jittered rim reaches from it, with a pixel of margin. */
	private static final double COIN_CENTRE = 4.5;
	private static final double CLEAR_DISTANCE = 6.0;

	/** The picture's colours, as opaque ARGB: the back card's soft grey, the front card's white, the mention blue. */
	private static final int SOFT = 0xFFAAAAAA;
	private static final int WHITE = 0xFFF0F0F0;
	private static final int BLUE = 0xFF3B82F6;

	/** How many pixels the picture adds to the coin: 20 white, 11 soft grey, 3 blue. */
	private static final int PICTURE_PIXELS = 34;

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
	public void thePictureIsTheTwoStackedCards()
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

		// Rows 0 to 6 and row 15 hold the coin alone: the picture starts at row 7 and keeps a row of air below.
		for (int x = 0; x < NavIcon.SIZE; x++)
		{
			for (int y = 0; y <= 6; y++)
			{
				assertEquals("row " + y + " is the coin alone at " + x, coin.getRGB(x, y), icon.getRGB(x, y));
			}
			assertClear("row 15", icon, x, 15);
		}

		// The back card: its top edge, its right edge down to row 11, its two pixels of left edge, the hollow inside.
		assertPixel("soft grey", SOFT, icon, 10, 7);
		assertPixel("soft grey", SOFT, icon, 15, 7);
		assertPixel("soft grey", SOFT, icon, 10, 8);
		assertPixel("soft grey", SOFT, icon, 15, 11);
		assertClear("the back card's hollow", icon, 12, 8);
		// The clear ring between the cards: the back card's left edge and bottom edge cut away.
		assertClear("the ring", icon, 10, 9);
		assertClear("the ring", icon, 14, 10);
		assertClear("the ring", icon, 14, 11);
		assertClear("the ring", icon, 15, 12);
		// The front card: its four corners, its hollow, and the blue line inside.
		assertPixel("white", WHITE, icon, 7, 10);
		assertPixel("white", WHITE, icon, 13, 10);
		assertPixel("white", WHITE, icon, 7, 14);
		assertPixel("white", WHITE, icon, 13, 14);
		assertClear("the front card's hollow", icon, 8, 11);
		assertClear("the front card's hollow", icon, 12, 13);
		assertClear("the air before the line", icon, 8, 12);
		assertPixel("blue", BLUE, icon, 9, 12);
		assertPixel("blue", BLUE, icon, 10, 12);
		assertPixel("blue", BLUE, icon, 11, 12);
		assertClear("the air after the line", icon, 12, 12);
	}
}

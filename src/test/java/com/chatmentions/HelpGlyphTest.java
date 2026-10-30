package com.chatmentions;

import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@link HelpGlyph}, in memory: its size, its transparent corners and the colour of its ink. */
public class HelpGlyphTest
{
	@Test
	public void theIconIsSquareTransparentAtTheCornersAndInkedInTheColour()
	{
		final ImageIcon icon = HelpGlyph.icon(12, ColorScheme.BRAND_ORANGE);
		assertEquals(12, icon.getIconWidth());
		assertEquals(12, icon.getIconHeight());
		final BufferedImage img = (BufferedImage) icon.getImage();
		assertEquals(0, img.getRGB(0, 0) >>> 24);
		boolean inked = false;
		for (int x = 0; x < 12; x++)
		{
			for (int y = 0; y < 12; y++)
			{
				final int argb = img.getRGB(x, y);
				if (argb >>> 24 == 255 && (argb & 0xFFFFFF) == (ColorScheme.BRAND_ORANGE.getRGB() & 0xFFFFFF))
				{
					inked = true;
				}
			}
		}
		assertTrue("an opaque pixel in the colour", inked);
	}

	@Test
	public void aSmallSizeIsRaisedToTheMinimum()
	{
		final ImageIcon icon = HelpGlyph.icon(3, null);
		assertEquals(7, icon.getIconWidth());
		assertEquals(7, icon.getIconHeight());
	}
}

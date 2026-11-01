package com.chatmentions;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@link GearsGlyph}, in memory: its size, its clear corners and holes, and the colour of its ink. */
public class GearsGlyphTest
{
	private static BufferedImage image(Color colour)
	{
		return (BufferedImage) GearsGlyph.icon(colour).getImage();
	}

	@Test
	public void theIconIsTwelveBySquare()
	{
		final ImageIcon icon = GearsGlyph.icon(Color.WHITE);
		assertEquals(12, icon.getIconWidth());
		assertEquals(12, icon.getIconHeight());
	}

	@Test
	public void theCornersAndBothHolesAreClear()
	{
		final BufferedImage img = image(Color.WHITE);
		assertEquals("top left", 0, img.getRGB(0, 0) >>> 24);
		// The small gear's tooth grazes the very corner of the top right pixel (alpha 6 of 255): clear to the eye.
		assertTrue("top right", img.getRGB(11, 0) >>> 24 <= 8);
		assertEquals("bottom left", 0, img.getRGB(0, 11) >>> 24);
		assertEquals("bottom right", 0, img.getRGB(11, 11) >>> 24);
		assertEquals("the big gear's hole", 0, img.getRGB(4, 7) >>> 24);
		// The small hole (radius 0.8 at 9.2, 3.0) straddles four pixels; none of them is solid.
		for (final int[] at : new int[][]{{8, 2}, {9, 2}, {8, 3}, {9, 3}})
		{
			assertTrue("the small gear's hole at " + at[0] + "," + at[1], img.getRGB(at[0], at[1]) >>> 24 < 255);
		}
	}

	@Test
	public void everyInkedPixelIsTheGivenColour()
	{
		final Color ink = new Color(200, 30, 90);
		final BufferedImage img = image(ink);
		boolean inked = false;
		for (int x = 0; x < 12; x++)
		{
			for (int y = 0; y < 12; y++)
			{
				final int argb = img.getRGB(x, y);
				if (argb >>> 24 >= 100)
				{
					inked = true;
					// A faint edge pixel (alpha under 100) rounds its colour by whole steps, so only the firm ones are compared.
					for (int shift = 0; shift <= 16; shift += 8)
					{
						final int want = (ink.getRGB() >> shift) & 0xFF;
						final int got = (argb >> shift) & 0xFF;
						assertTrue("pixel " + x + "," + y, Math.abs(want - got) <= 2);
					}
				}
			}
		}
		assertTrue("some pixel is inked", inked);
	}

	@Test
	public void aNullInkIsTheSidebarGrey()
	{
		final BufferedImage img = image(null);
		boolean solid = false;
		for (int x = 0; x < 12; x++)
		{
			for (int y = 0; y < 12; y++)
			{
				final int argb = img.getRGB(x, y);
				if (argb >>> 24 == 255)
				{
					solid = true;
					assertEquals(ColorScheme.LIGHT_GRAY_COLOR.getRGB() & 0xFFFFFF, argb & 0xFFFFFF);
				}
			}
		}
		assertTrue("a solid pixel", solid);
	}
}

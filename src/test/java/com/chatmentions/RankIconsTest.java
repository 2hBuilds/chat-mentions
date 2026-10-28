package com.chatmentions;

import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import net.runelite.api.IndexedSprite;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * {@link RankIcons}: the tag's number and the sprite drawn at its offsets, once. {@link Sprite} is package-private so
 * the panel's test builds the same sprite with {@link #six}.
 */
public class RankIconsTest
{
	/** A sprite with the values handed to it; its setters do nothing. */
	static final class Sprite implements IndexedSprite
	{
		private final byte[] pixels;
		private final int[] palette;
		private final int offsetX;
		private final int offsetY;
		private final int width;
		private final int height;
		private final int originalWidth;
		private final int originalHeight;

		Sprite(byte[] pixels, int[] palette, int offsetX, int offsetY, int width, int height, int originalWidth,
			int originalHeight)
		{
			this.pixels = pixels;
			this.palette = palette;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.width = width;
			this.height = height;
			this.originalWidth = originalWidth;
			this.originalHeight = originalHeight;
		}

		@Override
		public byte[] getPixels()
		{
			return pixels;
		}

		@Override
		public void setPixels(byte[] pixels)
		{
		}

		@Override
		public int[] getPalette()
		{
			return palette;
		}

		@Override
		public void setPalette(int[] palette)
		{
		}

		@Override
		public int getOffsetX()
		{
			return offsetX;
		}

		@Override
		public void setOffsetX(int offsetX)
		{
		}

		@Override
		public int getOffsetY()
		{
			return offsetY;
		}

		@Override
		public void setOffsetY(int offsetY)
		{
		}

		@Override
		public int getWidth()
		{
			return width;
		}

		@Override
		public void setWidth(int width)
		{
		}

		@Override
		public int getHeight()
		{
			return height;
		}

		@Override
		public void setHeight(int height)
		{
		}

		@Override
		public int getOriginalWidth()
		{
			return originalWidth;
		}

		@Override
		public void setOriginalWidth(int originalWidth)
		{
		}

		@Override
		public int getOriginalHeight()
		{
			return originalHeight;
		}

		@Override
		public void setOriginalHeight(int originalHeight)
		{
		}
	}

	/** An array of {@code size} slots with the plan's example sprite (2 x 2 in a 3 x 3 frame at (1, 0)) at the last. */
	static IndexedSprite[] six(int size)
	{
		final IndexedSprite[] array = new IndexedSprite[size];
		array[size - 1] = new Sprite(new byte[]{1, 0, 2, 1}, new int[]{0, 0xFF0000, 0x00FF00}, 1, 0, 2, 2, 3, 3);
		return array;
	}

	@Test
	public void theIndexIsTheFirstImgTag()
	{
		assertEquals(41, RankIcons.indexIn("<img=41>Klar"));
		assertEquals(2, RankIcons.indexIn("<img=2><img=41>Klar"));
		assertEquals(-1, RankIcons.indexIn("Klar"));
		assertEquals(-1, RankIcons.indexIn("<col=ff0000>Klar"));
		assertEquals(-1, RankIcons.indexIn("<img=x>Klar"));
		assertEquals(-1, RankIcons.indexIn(null));
		assertEquals(-1, RankIcons.indexIn("<img=1234567890>Klar"));
	}

	@Test
	public void captureDrawsTheSpriteAtItsOffsetsAndOnce()
	{
		final RankIcons icons = new RankIcons();
		final IndexedSprite[] array = six(6);
		icons.capture(5, array);
		final Icon icon = icons.icon(5);
		assertTrue(icon instanceof ImageIcon);
		assertEquals(3, icon.getIconWidth());
		assertEquals(3, icon.getIconHeight());
		final BufferedImage image = (BufferedImage) ((ImageIcon) icon).getImage();
		assertEquals(0xFFFF0000, image.getRGB(1, 0));
		assertEquals(0xFFFF0000, image.getRGB(2, 1));
		assertEquals(0xFF00FF00, image.getRGB(1, 1));
		assertEquals(0, image.getRGB(0, 0) >>> 24);
		assertEquals(0, image.getRGB(2, 0) >>> 24);
		assertEquals(0, image.getRGB(0, 2) >>> 24);

		icons.capture(5, array);
		assertSame("captured once", icon, icons.icon(5));
		icons.capture(4, array);
		assertNull(icons.icon(4));
		icons.capture(9, array);
		icons.capture(-1, array);
		icons.capture(1, null);
		assertNull(icons.icon(9));
		assertNull(icons.icon(-1));
		assertNull(icons.icon(1));
		assertNotNull(icons.icon(5));
	}
}

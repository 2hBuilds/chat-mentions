package com.chatmentions;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import net.runelite.api.IndexedSprite;

/**
 * The icons the game put before senders' names (a clan rank, a player's crown), as Swing icons at the sprite's own size.
 * The client thread captures an icon the first time a line carries its number; the event thread reads them. Every method
 * is synchronized on the object, since the two threads meet here.
 *
 * <p>Choice: an icon is drawn once and kept; the game's sprite for a number does not change while the client runs.
 */
final class RankIcons
{
	/** The tag the game puts in front of a name: {@code <img=N>}, N one to nine digits. */
	private static final Pattern TAG = Pattern.compile("<img=(\\d{1,9})>");

	private final Map<Integer, Icon> icons = new HashMap<>();

	/** The number in the first {@code <img=N>} tag of a raw name; -1 for none, a null name or a longer number. */
	static int indexIn(String rawName)
	{
		if (rawName == null)
		{
			return -1;
		}
		final Matcher tag = TAG.matcher(rawName);
		return tag.find() ? Integer.parseInt(tag.group(1)) : -1;
	}

	/**
	 * Draws the sprite at {@code index} of the game's mod icons, unless it was drawn already or there is none. Each
	 * pixel's palette entry goes at its offset in an image of the sprite's original size (its own size when an original
	 * is 0 or less); palette entry 0 is see-through. Called on the client thread.
	 */
	synchronized void capture(int index, IndexedSprite[] modIcons)
	{
		if (index < 0 || modIcons == null || index >= modIcons.length || modIcons[index] == null
			|| icons.containsKey(index))
		{
			return;
		}
		final IndexedSprite sprite = modIcons[index];
		final int width = sprite.getOriginalWidth() > 0 && sprite.getOriginalHeight() > 0
			? sprite.getOriginalWidth() : sprite.getWidth();
		final int height = sprite.getOriginalWidth() > 0 && sprite.getOriginalHeight() > 0
			? sprite.getOriginalHeight() : sprite.getHeight();
		if (width <= 0 || height <= 0)
		{
			return;
		}
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final byte[] pixels = sprite.getPixels();
		final int[] palette = sprite.getPalette();
		for (int y = 0; y < sprite.getHeight(); y++)
		{
			for (int x = 0; x < sprite.getWidth(); x++)
			{
				final int at = y * sprite.getWidth() + x;
				if (pixels == null || palette == null || at >= pixels.length)
				{
					continue;
				}
				final int entry = pixels[at] & 0xff;
				final int px = sprite.getOffsetX() + x;
				final int py = sprite.getOffsetY() + y;
				if (entry == 0 || entry >= palette.length || px < 0 || py < 0 || px >= width || py >= height)
				{
					continue;
				}
				image.setRGB(px, py, 0xFF000000 | palette[entry]);
			}
		}
		icons.put(index, new ImageIcon(image));
	}

	/** The icon captured for {@code index}, or null when none was. Called on the event thread. */
	synchronized Icon icon(int index)
	{
		return icons.get(index);
	}
}

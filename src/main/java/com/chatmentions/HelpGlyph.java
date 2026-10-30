package com.chatmentions;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import net.runelite.client.ui.ColorScheme;

/**
 * The help mark, drawn in code: a ring with a bold question mark in the middle, the "(?)" that opens the guide. It is
 * {@link GearGlyph}'s sister in size, and it refers to no class of the plugin it sits in.
 *
 * <p>The ring is thin (a tenth of the size, 1 px at least) and fills the icon less half its own stroke, so no edge is
 * clipped; the "?" is a plain bold sans-serif at about five sixths of the size, centred with the font's metrics - the
 * sidebar's own bold font and a heavier ring filled the ring at 12 px and read as a blob. Everything else stays
 * transparent.
 */
final class HelpGlyph
{
	/** The smallest side drawn: below it the question mark closes into the ring. */
	static final int MIN_SIZE = 7;

	private HelpGlyph()
	{
	}

	/**
	 * A new square icon, transparent except for the ring and the question mark.
	 *
	 * @param size   the icon's side in px; anything under {@value #MIN_SIZE} is raised to it
	 * @param colour the ink; null falls back to the sidebar's quiet grey, {@link ColorScheme#LIGHT_GRAY_COLOR}
	 */
	static ImageIcon icon(int size, Color colour)
	{
		final int s = Math.max(MIN_SIZE, size);
		final BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setColor(colour == null ? ColorScheme.LIGHT_GRAY_COLOR : colour);
			final float ringWidth = (float) Math.max(1.0, s * 0.1);
			final double edge = ringWidth / 2.0;
			g.setStroke(new BasicStroke(ringWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(new Ellipse2D.Double(edge, edge, s - ringWidth, s - ringWidth));
			final Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.round(s * 0.85f));
			g.setFont(font);
			final FontMetrics metrics = g.getFontMetrics();
			final double x = (s - metrics.stringWidth("?")) / 2.0;
			final double y = (s - metrics.getHeight()) / 2.0 + metrics.getAscent();
			g.drawString("?", (float) x, (float) y);
		}
		finally
		{
			g.dispose();
		}
		return new ImageIcon(img);
	}
}

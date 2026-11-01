package com.chatmentions;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import net.runelite.client.ui.ColorScheme;

/**
 * The keyword options mark, drawn in code: two gears in a 12 px box, a big one at the lower left and a small one at the
 * upper right, the icon that opens the keyword strip's popup. It is {@link HelpGlyph}'s sister, and it refers to no class
 * of the plugin it sits in.
 *
 * <p>The geometry is Bank Price Movement's double gears, unchanged (units of the box, x to the right, y down): the big gear
 * at 4.3, 7.7 with a body of radius 3.0, seven teeth from radius 2.2 to 4.2 (0.9 either side of their centre line) and a
 * hole of radius 1.2; the small gear at 9.2, 3.0 with a body of radius 2.0, six teeth from 1.4 to 2.8 (0.7 either side) and
 * a hole of radius 0.8. The first tooth of each points along the positive x axis. Everything else stays transparent.
 */
final class GearsGlyph
{
	/** The box's side in px. */
	static final int SIZE = 12;

	private static final double BIG_X = 4.3;
	private static final double BIG_Y = 7.7;
	private static final double BIG_BODY = 3.0;
	private static final int BIG_TEETH = 7;
	private static final double BIG_TOOTH_IN = 2.2;
	private static final double BIG_TOOTH_OUT = 4.2;
	private static final double BIG_TOOTH_HALF = 0.9;
	private static final double BIG_HOLE = 1.2;

	private static final double SMALL_X = 9.2;
	private static final double SMALL_Y = 3.0;
	private static final double SMALL_BODY = 2.0;
	private static final int SMALL_TEETH = 6;
	private static final double SMALL_TOOTH_IN = 1.4;
	private static final double SMALL_TOOTH_OUT = 2.8;
	private static final double SMALL_TOOTH_HALF = 0.7;
	private static final double SMALL_HOLE = 0.8;

	private GearsGlyph()
	{
	}

	/**
	 * A new {@value #SIZE} x {@value #SIZE} icon, transparent except for the two gears.
	 *
	 * @param colour the ink; null falls back to the sidebar's quiet grey, {@link ColorScheme#LIGHT_GRAY_COLOR}
	 */
	static ImageIcon icon(Color colour)
	{
		final Color ink = colour == null ? ColorScheme.LIGHT_GRAY_COLOR : colour;
		final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setColor(new Color(ink.getRed(), ink.getGreen(), ink.getBlue()));
			g.fill(gear(BIG_X, BIG_Y, BIG_BODY, BIG_TEETH, BIG_TOOTH_IN, BIG_TOOTH_OUT, BIG_TOOTH_HALF, BIG_HOLE));
			g.fill(gear(SMALL_X, SMALL_Y, SMALL_BODY, SMALL_TEETH, SMALL_TOOTH_IN, SMALL_TOOTH_OUT, SMALL_TOOTH_HALF,
				SMALL_HOLE));
		}
		finally
		{
			g.dispose();
		}
		return new ImageIcon(img);
	}

	/**
	 * One gear's outline: the body disc, the teeth (rectangles standing on the body, evenly round it, the first along the
	 * positive x axis), then the hole cut out of the lot.
	 */
	private static Area gear(double centreX, double centreY, double body, int teeth, double toothIn, double toothOut,
		double toothHalf, double hole)
	{
		final Area area = new Area(new Ellipse2D.Double(centreX - body, centreY - body, body * 2.0, body * 2.0));
		for (int i = 0; i < teeth; i++)
		{
			final Area tooth = new Area(new Rectangle2D.Double(toothIn, -toothHalf, toothOut - toothIn, toothHalf * 2.0));
			tooth.transform(AffineTransform.getRotateInstance(i * 2.0 * Math.PI / teeth));
			tooth.transform(AffineTransform.getTranslateInstance(centreX, centreY));
			area.add(tooth);
		}
		area.subtract(new Area(new Ellipse2D.Double(centreX - hole, centreY - hole, hole * 2.0, hole * 2.0)));
		return area;
	}
}

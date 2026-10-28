package com.chatmentions;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Where a window goes on the screen, worked out apart from any window so that it can be tested: nothing here touches
 * the screen itself, the caller hands over the room it has.
 */
final class ScreenPlace
{
	/** How far left of and above the point a window's top-left corner sits, so that the point rests on its top edge. */
	static final int NUDGE = 10;

	private ScreenPlace()
	{
	}

	/**
	 * The top-left corner for a window of {@code size} that opens at {@code at}: {@link #NUDGE} px left of and above
	 * it, held inside {@code screen} (at its left and top edges when the window is bigger than the room).
	 *
	 * @param at the point on the screen the window opens at, the mouse at a press
	 * @param size the window's size
	 * @param screen the usable area of the screen the point is on
	 */
	static Point near(Point at, Dimension size, Rectangle screen)
	{
		final int x = at.x - NUDGE;
		final int y = at.y - NUDGE;
		return new Point(
			Math.max(screen.x, Math.min(x, screen.x + screen.width - size.width)),
			Math.max(screen.y, Math.min(y, screen.y + screen.height - size.height)));
	}
}

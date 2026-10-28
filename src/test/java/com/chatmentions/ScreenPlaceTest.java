package com.chatmentions;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import org.junit.Test;

public class ScreenPlaceTest
{
	@Test
	public void theWindowSitsUpLeftOfThePointInsideTheScreen()
	{
		final Rectangle screen = new Rectangle(0, 0, 1920, 1040);
		final Dimension size = new Dimension(300, 200);
		assertEquals(new Point(490, 390), ScreenPlace.near(new Point(500, 400), size, screen));
		assertEquals(new Point(1620, 390), ScreenPlace.near(new Point(1900, 400), size, screen));
		assertEquals(new Point(490, 840), ScreenPlace.near(new Point(500, 1030), size, screen));
		assertEquals(new Point(0, 0), ScreenPlace.near(new Point(5, 5), size, screen));
		assertEquals(new Point(1920, 0),
			ScreenPlace.near(new Point(1925, 10), size, new Rectangle(1920, 0, 1920, 1040)));
		assertEquals(new Point(0, 0), ScreenPlace.near(new Point(500, 400), new Dimension(3000, 3000), screen));
	}
}

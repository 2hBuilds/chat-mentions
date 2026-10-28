package com.chatmentions;

import java.awt.Color;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** {@link WordColours}: the setting string read, written and changed. */
public class WordColoursTest
{
	@Test
	public void theSettingIsParsedWrittenAndChanged()
	{
		final WordColours colours = WordColours.of("bond=ff0000, Yama = 00FF00, x=zz, =123456");
		assertEquals(new Color(255, 0, 0), colours.colour("BOND"));
		assertEquals(new Color(0, 255, 0), colours.colour("yama"));
		assertNull(colours.colour("x"));
		assertNull(colours.colour("nope"));
		assertEquals("bond=ff0000, Yama=00ff00", colours.setting());
		assertEquals("bond=ff0000, Yama=00ff00, split=0000ff", colours.with("split", new Color(0, 0, 255)).setting());
		assertTrue(colours.with("BOND", new Color(1, 2, 3)).setting().startsWith("BOND=010203, Yama=00ff00"));
		assertEquals("bond=ff0000", colours.without("yama").setting());
		assertTrue(WordColours.of(null).isEmpty());
		assertEquals("", WordColours.of("").setting());
	}
}

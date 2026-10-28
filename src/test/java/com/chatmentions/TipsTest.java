package com.chatmentions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@link Tips}: the five tips and the draw among them. */
public class TipsTest
{
	@Test
	public void fiveTipsAndAPickAmongThem()
	{
		final List<String> five = Arrays.asList(
			"Tip: chat lines that name you or a keyword show here.",
			"Tip: click a keyword to pick its colour.",
			"Tip: type a keyword in the box below and press Enter.",
			"Tip: click a card to jump to its line in the chat.",
			"Tip: 'How to use' is in the settings menu.");
		assertEquals(five, Tips.ALL);
		assertTrue(Tips.ALL.contains(Tips.pick(new Random(7))));
		final Random random = new Random(1);
		final Set<String> seen = new HashSet<>();
		for (int i = 0; i < 200; i++)
		{
			seen.add(Tips.pick(random));
		}
		assertTrue("at least three distinct tips: " + seen.size(), seen.size() >= 3);
		try
		{
			Tips.ALL.add("Tip: more.");
			throw new AssertionError("the list took an addition");
		}
		catch (UnsupportedOperationException expected)
		{
			assertEquals(5, Tips.ALL.size());
		}
	}
}

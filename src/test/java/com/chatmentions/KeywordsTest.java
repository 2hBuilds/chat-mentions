package com.chatmentions;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** {@link Keywords}: the cleaned word list and the finder. */
public class KeywordsTest
{
	/** Checks the hits against the expected {@code {start, end}} pairs, given flat. */
	private static void hits(String message, List<int[]> found, int... flat)
	{
		assertEquals(message + ": how many hits", flat.length / 2, found.size());
		for (int i = 0; i < found.size(); i++)
		{
			assertArrayEquals(message + ": hit " + i, new int[]{flat[2 * i], flat[2 * i + 1]},
				Arrays.copyOf(found.get(i), 2));
		}
	}

	@Test
	public void theSettingSplitsOnCommasAndIsCleaned()
	{
		final Keywords keywords = Keywords.of(" 2h, Sanka ,,klar\u00a0x, sanka ", false, "Me");
		assertEquals("the name first, then cleaned, deduplicated, in order and case",
			Arrays.asList("Me", "2h", "Sanka", "klar x"), keywords.words());
		try
		{
			keywords.words().add("no");
			fail("the words cannot be added to");
		}
		catch (UnsupportedOperationException expected)
		{
			assertTrue("unmodifiable", true);
		}
	}

	@Test
	public void theNameIsAlwaysTheFirstWord()
	{
		assertEquals("empty setting", Arrays.asList("Sanka"), Keywords.of("", false, "Sanka").words());
		assertEquals("only separators", Arrays.asList("Sanka"), Keywords.of(" , ", false, "Sanka").words());
		assertEquals("the name leads and is not repeated", Arrays.asList("Sanka", "bond", "split"),
			Keywords.of("bond, SANKA, split", false, "Sanka").words());
		assertEquals("no name", Arrays.asList("bond"), Keywords.of("bond", false, null).words());
		final Keywords none = Keywords.of(null, false, null);
		assertTrue("no setting and no name", none.words().isEmpty());
		assertTrue("no words find nothing", none.find("Sanka").isEmpty());
		assertTrue("a blank name is no word", Keywords.of("", false, "  ").words().isEmpty());
	}

	@Test
	public void aWholeWordHitIgnoresCase()
	{
		final Keywords keywords = Keywords.of("sanka", false, null);
		hits("upper case", keywords.find("SANKA come to ge"), 0, 5);
		hits("at the end", keywords.find("hey Sanka"), 4, 9);
		hits("inside a word", keywords.find("sankas"));
		hits("empty text", keywords.find(""));
		hits("null text", keywords.find(null));
	}

	@Test
	public void lettersAndDigitsBreakAWordPunctuationDoesNot()
	{
		final Keywords keywords = Keywords.of("2h", false, null);
		hits("letters after", keywords.find("2hbuilds"));
		hits("letter before", keywords.find("x2h"));
		hits("digit after", keywords.find("2h5"));
		hits("brackets", keywords.find("(2h)"), 1, 3);
		hits("comma", keywords.find("2h,"), 0, 2);
		hits("full stop", keywords.find("the 2h."), 4, 6);
		hits("no-break space", keywords.find("2h\u00a0builds"), 0, 2);
	}

	@Test
	public void partialWordsHitInsideWords()
	{
		hits("inside words", Keywords.of("2h", true, null).find("2hbuilds and x2h"), 0, 2, 14, 16);
	}

	@Test
	public void hitsAreSortedAndOverlapsMerge()
	{
		hits("sorted", Keywords.of("klar,2h", false, null).find("2h and klar"), 0, 2, 7, 11);
		hits("contained", Keywords.of("sank,sanka", true, null).find("sanka"), 0, 5);
		hits("overlapping", Keywords.of("ab,bc", true, null).find("abc"), 0, 3);
		hits("touching stays apart", Keywords.of("2h,hi", true, null).find("2hhi"), 0, 2, 2, 4);
	}

	@Test
	public void theSameWordTwiceInALineGivesTwoHits()
	{
		hits("twice", Keywords.of("2h", false, null).find("2h here, 2h there"), 0, 2, 9, 11);
	}

	@Test
	public void aHitNamesItsWord()
	{
		final List<int[]> found = Keywords.of("bond, yama", false, "Sanka").find("yama and bond and sanka");
		assertEquals(3, found.size());
		assertArrayEquals(new int[]{0, 4, 2}, found.get(0));
		assertArrayEquals(new int[]{9, 13, 1}, found.get(1));
		assertArrayEquals(new int[]{18, 23, 0}, found.get(2));

		final List<int[]> first = Keywords.of("ab, bc", true, null).find("abc");
		assertEquals(1, first.size());
		assertArrayEquals(new int[]{0, 3, 0}, first.get(0));
		final List<int[]> second = Keywords.of("bc, ab", true, null).find("abc");
		assertEquals(1, second.size());
		assertArrayEquals(new int[]{0, 3, 0}, second.get(0));
	}
}

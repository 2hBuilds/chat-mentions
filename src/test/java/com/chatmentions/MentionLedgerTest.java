package com.chatmentions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** {@link MentionLedger}: the cards, newest first. */
public class MentionLedgerTest
{
	private static Mention mention(long seq)
	{
		return new Mention(Chat.CLAN, seq, 1000L * seq, "who", "text " + seq, Collections.<int[]>emptyList(), -1);
	}

	private static AtomicInteger counter(MentionLedger ledger)
	{
		final AtomicInteger told = new AtomicInteger();
		ledger.listen(told::incrementAndGet);
		return told;
	}

	@Test
	public void newestFirstAndTheOldestGoesPastTheCap()
	{
		final MentionLedger ledger = new MentionLedger(3);
		final AtomicInteger told = counter(ledger);
		for (int i = 1; i <= 4; i++)
		{
			ledger.add(mention(i));
		}
		final List<Mention> held = ledger.mentions();
		assertEquals("three held", 3, held.size());
		assertEquals("newest first", 4, held.get(0).seq());
		assertEquals("then 3", 3, held.get(1).seq());
		assertEquals("then 2", 2, held.get(2).seq());
		assertEquals("told four times", 4, told.get());
	}

	@Test
	public void markReadRemovesThatOneAndOnlyThatOne()
	{
		final MentionLedger ledger = new MentionLedger(5);
		final Mention a = mention(1);
		final Mention b = mention(2);
		final Mention c = mention(3);
		ledger.add(a);
		ledger.add(b);
		ledger.add(c);
		final AtomicInteger told = counter(ledger);

		assertTrue("the middle one is removed", ledger.markRead(b));
		final List<Mention> held = ledger.mentions();
		assertEquals("two left", 2, held.size());
		assertSame("newest first", c, held.get(0));
		assertSame("then the oldest", a, held.get(1));
		assertEquals("told once", 1, told.get());

		final Mention twin = mention(3);
		assertFalse("an equal-looking mention is not held", ledger.markRead(twin));
		assertEquals("nothing changed", 2, ledger.mentions().size());
		assertEquals("nobody told", 1, told.get());
		assertFalse("null is false", ledger.markRead(null));
		assertEquals("null tells nobody", 1, told.get());
	}

	@Test
	public void clearEmptiesAndTellsEvenWhenEmpty()
	{
		final MentionLedger ledger = new MentionLedger(5);
		final AtomicInteger told = counter(ledger);
		ledger.clear();
		assertEquals("told once when empty", 1, told.get());
		ledger.add(mention(1));
		ledger.add(mention(2));
		ledger.clear();
		assertEquals("empty", 0, ledger.mentions().size());
		assertEquals("told again", 4, told.get());
	}

	@Test
	public void resizeDropsTheOldestAndNeverBelowOne()
	{
		final MentionLedger ledger = new MentionLedger(5);
		final AtomicInteger told = counter(ledger);
		for (int i = 1; i <= 5; i++)
		{
			ledger.add(mention(i));
		}
		assertEquals("five adds told", 5, told.get());

		ledger.resize(2);
		assertEquals("two held", 2, ledger.mentions().size());
		assertEquals("newest", 5, ledger.mentions().get(0).seq());
		assertEquals("then 4", 4, ledger.mentions().get(1).seq());
		assertEquals("told for the drop", 6, told.get());

		ledger.resize(0);
		assertEquals("one left", 1, ledger.mentions().size());
		assertEquals("the newest", 5, ledger.mentions().get(0).seq());
		assertEquals("told again", 7, told.get());

		ledger.resize(9);
		assertEquals("nothing dropped", 1, ledger.mentions().size());
		assertEquals("nobody told", 7, told.get());
		for (int i = 6; i <= 15; i++)
		{
			ledger.add(mention(i));
		}
		assertEquals("cap 9", 9, ledger.mentions().size());

		final MentionLedger zero = new MentionLedger(0);
		zero.add(mention(1));
		zero.add(mention(2));
		assertEquals("a cap of 0 reads as 1", 1, zero.mentions().size());
	}

	@Test
	public void aListenerIsToldWithNoLockHeldAndSeesTheNewState()
	{
		final MentionLedger ledger = new MentionLedger(5);
		final List<Integer> sizes = new ArrayList<>();
		final List<Integer> listed = new ArrayList<>();
		ledger.listen(() ->
		{
			sizes.add(ledger.mentions().size());
			listed.add(ledger.mentions().size());
		});
		ledger.add(mention(1));
		assertEquals("told once", 1, sizes.size());
		assertEquals("size seen inside the listener", Integer.valueOf(1), sizes.get(0));
		assertEquals("mentions seen inside the listener", Integer.valueOf(1), listed.get(0));

		ledger.add(null);
		assertEquals("null adds nothing", 1, ledger.mentions().size());
		assertEquals("null tells nobody", 1, sizes.size());

		final List<int[]> source = new ArrayList<>(Arrays.asList(new int[]{0, 2}, new int[]{4, 6}));
		final Mention built = new Mention(Chat.PUBLIC, 1, 1, "w", "t", source, -1);
		source.clear();
		assertEquals("the spans were copied", 2, built.spans().size());
		try
		{
			built.spans().add(new int[]{1, 2});
			fail("the spans cannot be added to");
		}
		catch (UnsupportedOperationException expected)
		{
			assertTrue("unmodifiable", true);
		}
	}
}

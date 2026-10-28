package com.chatmentions;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** {@link ChatBuffer}: the kept lines of one chat. */
public class ChatBufferTest
{
	@Test
	public void linesComeBackOldestFirstWithRisingNumbers()
	{
		final ChatBuffer buffer = new ChatBuffer(10);
		final ChatLine one = buffer.add(Chat.CLAN, 1, "a", "one", true, -1);
		final ChatLine two = buffer.add(Chat.CLAN, 2, "b", "two", false, -1);
		final ChatLine three = buffer.add(Chat.CLAN, 3, null, null, true, -1);

		assertEquals("first seq", 1, one.seq());
		assertEquals("second seq", 2, two.seq());
		assertEquals("third seq", 3, three.seq());
		final List<ChatLine> lines = buffer.lines();
		assertEquals("three held", 3, lines.size());
		assertEquals("oldest first", one, lines.get(0));
		assertEquals("then the second", two, lines.get(1));
		assertEquals("then the third", three, lines.get(2));
		assertEquals("who", "b", two.who());
		assertEquals("text", "two", two.text());
		assertEquals("at", 2, two.at());
		assertFalse("spoken", two.spoken());
		assertEquals("a null who is empty", "", three.who());
		assertEquals("a null text is empty", "", three.text());
	}

	@Test
	public void theRingDropsTheOldestPastItsCapacity()
	{
		final ChatBuffer buffer = new ChatBuffer(3);
		for (int i = 1; i <= 5; i++)
		{
			buffer.add(Chat.CLAN, i, "w", "t" + i, true, -1);
		}
		final List<ChatLine> lines = buffer.lines();
		assertEquals("three held", 3, lines.size());
		assertEquals("first held", 3, lines.get(0).seq());
		assertEquals("second held", 4, lines.get(1).seq());
		assertEquals("last held", 5, lines.get(2).seq());
		assertEquals("2 is gone", -1, indexOf(buffer, 2));
		assertEquals("3 is first", 0, indexOf(buffer, 3));
		assertEquals("5 is last", 2, indexOf(buffer, 5));
		assertEquals("6 is not yet", -1, indexOf(buffer, 6));
		assertEquals("0 is never", -1, indexOf(buffer, 0));
	}

	@Test
	public void resizeDropsTheOldestAndNeverBelowOne()
	{
		final ChatBuffer buffer = new ChatBuffer(5);
		for (int i = 1; i <= 5; i++)
		{
			buffer.add(Chat.CLAN, i, "w", "t", true, -1);
		}
		buffer.resize(2);
		assertEquals("two held", 2, buffer.lines().size());
		assertEquals("older of the two", 4, buffer.lines().get(0).seq());
		assertEquals("newer of the two", 5, buffer.lines().get(1).seq());

		buffer.resize(10);
		assertEquals("growing keeps both", 2, buffer.lines().size());
		for (int i = 0; i < 10; i++)
		{
			buffer.add(Chat.CLAN, 10 + i, "w", "t", true, -1);
		}
		assertEquals("ten held", 10, buffer.lines().size());

		final ChatBuffer zero = new ChatBuffer(0);
		zero.add(Chat.CLAN, 1, "w", "t", true, -1);
		zero.add(Chat.CLAN, 2, "w", "t", true, -1);
		assertEquals("a capacity of 0 reads as 1", 1, zero.lines().size());

		final ChatBuffer three = new ChatBuffer(5);
		for (int i = 1; i <= 3; i++)
		{
			three.add(Chat.CLAN, i, "w", "t", true, -1);
		}
		three.resize(-3);
		assertEquals("only the newest is held", 1, three.lines().size());
		assertEquals("the newest", 3, three.lines().get(0).seq());
	}

	@Test
	public void clearEmptiesAndTheNumbersKeepRising()
	{
		final ChatBuffer buffer = new ChatBuffer(5);
		buffer.add(Chat.CLAN, 1, "w", "t", true, -1);
		buffer.add(Chat.CLAN, 2, "w", "t", true, -1);
		buffer.clear();
		assertEquals("empty", 0, buffer.lines().size());
		assertEquals("1 is gone", -1, indexOf(buffer, 1));
		assertEquals("no lines", 0, buffer.lines().size());
		final ChatLine next = buffer.add(Chat.CLAN, 3, "w", "t", true, -1);
		assertEquals("the number keeps rising", 3, next.seq());
		assertEquals("and is first", 0, indexOf(buffer, 3));
	}

	@Test
	public void linesIsACopy()
	{
		final ChatBuffer buffer = new ChatBuffer(5);
		buffer.add(Chat.CLAN, 1, "w", "t", true, -1);
		buffer.add(Chat.CLAN, 2, "w", "t", true, -1);
		buffer.lines().clear();
		assertEquals("the buffer is untouched", 2, buffer.lines().size());
	}

	private static int indexOf(ChatBuffer buffer, long seq)
	{
		final List<ChatLine> lines = buffer.lines();
		for (int i = 0; i < lines.size(); i++)
		{
			if (lines.get(i).seq() == seq)
			{
				return i;
			}
		}
		return -1;
	}
}

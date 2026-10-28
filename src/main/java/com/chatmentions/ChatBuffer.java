package com.chatmentions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The kept lines of one chat, oldest first, up to a capacity; the oldest goes when more arrive. Every method is
 * synchronized on the buffer, because a line arrives on the client thread and the panel reads on the event thread.
 *
 * <p>Choice: the lines are an {@link ArrayDeque}; the numbers held are always contiguous (added one by one, dropped only
 * from the front). The numbers are never reset by {@link #clear} or {@link #resize}.
 */
final class ChatBuffer
{
	private final Deque<ChatLine> lines = new ArrayDeque<>();
	private int capacity;
	private long next = 1;

	/** A buffer of the capacity; below 1 reads as 1. */
	ChatBuffer(int capacity)
	{
		this.capacity = Math.max(1, capacity);
	}

	/** Keeps a line as the newest with the next number; the oldest goes when more than the capacity are held. */
	synchronized ChatLine add(Chat chat, long at, String who, String text, boolean spoken, int icon)
	{
		final ChatLine line = new ChatLine(chat, next++, at, who, text, spoken, icon);
		lines.addLast(line);
		trim();
		return line;
	}

	/** A copy of the lines, oldest first. */
	synchronized List<ChatLine> lines()
	{
		return new ArrayList<>(lines);
	}

	/** Sets the capacity (below 1 reads as 1) and drops the oldest lines beyond it. */
	synchronized void resize(int capacity)
	{
		this.capacity = Math.max(1, capacity);
		trim();
	}

	/** Drops every line; the numbers keep rising. */
	synchronized void clear()
	{
		lines.clear();
	}

	private void trim()
	{
		while (lines.size() > capacity)
		{
			lines.removeFirst();
		}
	}
}

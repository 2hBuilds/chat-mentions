package com.chatmentions;

import java.util.ArrayList;
import java.util.List;

/**
 * The cards, newest first, up to a cap. Shaped as the clan plugin's card history: the plugin adds a mention as it finds
 * one; the panel lists the entries and listens for a change.
 *
 * <p>Choice: every method is synchronized on the ledger itself, because a line arrives on the client thread and the
 * panel reads on the event thread; the listeners are told after the lock is let go, on the thread that changed the
 * ledger, so a listener never holds the lock while it works.
 */
final class MentionLedger
{
	private final List<Mention> entries = new ArrayList<>();
	private final List<Runnable> listeners = new ArrayList<>();
	private int cap;

	/** A ledger of the cap; below 1 reads as 1. */
	MentionLedger(int cap)
	{
		this.cap = Math.max(1, cap);
	}

	/** Keeps the mention as the newest; the oldest goes when more than the cap are held. Tells every listener. Null adds nothing. */
	void add(Mention mention)
	{
		if (mention == null)
		{
			return;
		}
		synchronized (this)
		{
			entries.add(0, mention);
			while (entries.size() > cap)
			{
				entries.remove(entries.size() - 1);
			}
		}
		changed();
	}

	/** A copy of the mentions, newest first. */
	synchronized List<Mention> mentions()
	{
		return new ArrayList<>(entries);
	}

	/** Removes that very object (identity) and tells; false, and silent, when it is null or not held. */
	boolean markRead(Mention mention)
	{
		if (mention == null)
		{
			return false;
		}
		synchronized (this)
		{
			int found = -1;
			for (int i = 0; i < entries.size(); i++)
			{
				if (entries.get(i) == mention)
				{
					found = i;
					break;
				}
			}
			if (found < 0)
			{
				return false;
			}
			entries.remove(found);
		}
		changed();
		return true;
	}

	/** Empties the ledger and tells every listener, whether or not it held anything. */
	void clear()
	{
		synchronized (this)
		{
			entries.clear();
		}
		changed();
	}

	/** Sets the cap (below 1 reads as 1), drops the oldest beyond it, and tells only when something went. */
	void resize(int cap)
	{
		boolean dropped = false;
		synchronized (this)
		{
			this.cap = Math.max(1, cap);
			while (entries.size() > this.cap)
			{
				entries.remove(entries.size() - 1);
				dropped = true;
			}
		}
		if (dropped)
		{
			changed();
		}
	}

	/** Has {@code listener} told after every change, on the changing thread. */
	synchronized void listen(Runnable listener)
	{
		listeners.add(listener);
	}

	/** Tells every listener, on the caller's thread, with no lock held. */
	private void changed()
	{
		final List<Runnable> told;
		synchronized (this)
		{
			told = new ArrayList<>(listeners);
		}
		for (final Runnable listener : told)
		{
			listener.run();
		}
	}
}

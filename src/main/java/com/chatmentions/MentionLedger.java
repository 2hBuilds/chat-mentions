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

	/**
	 * Keeps the mention as the newest; the oldest of the same chat goes when that chat holds more than the cap. Tells
	 * every listener. Null adds nothing.
	 */
	void add(Mention mention)
	{
		if (mention == null)
		{
			return;
		}
		synchronized (this)
		{
			entries.add(0, mention);
			trim(mention.chat());
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

	/**
	 * Empties the ledger of that chat's mentions; null or {@link Chat#ALL} empties it of all. Tells every listener only
	 * when something went.
	 */
	void clear(Chat chat)
	{
		boolean removed;
		synchronized (this)
		{
			if (chat == null || chat == Chat.ALL)
			{
				removed = !entries.isEmpty();
				entries.clear();
			}
			else
			{
				removed = entries.removeIf(m -> m.chat() == chat);
			}
		}
		if (removed)
		{
			changed();
		}
	}

	/** Sets the cap (below 1 reads as 1), drops each chat's oldest beyond it, and tells only when something went. */
	void resize(int cap)
	{
		boolean dropped = false;
		synchronized (this)
		{
			this.cap = Math.max(1, cap);
			final int before = entries.size();
			for (final Chat chat : Chat.values())
			{
				trim(chat);
			}
			dropped = entries.size() < before;
		}
		if (dropped)
		{
			changed();
		}
	}

	/** Drops the oldest mentions of the chat beyond the cap. Called with the lock held. */
	private void trim(Chat chat)
	{
		int seen = 0;
		for (int i = 0; i < entries.size(); i++)
		{
			if (entries.get(i).chat() == chat && ++seen > cap)
			{
				entries.remove(i--);
			}
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

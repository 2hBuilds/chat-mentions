package com.chatmentions;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.runelite.api.ChatMessageType;

/**
 * The wiring engine: every line of a watched chat goes into that chat's buffer and into the All buffer, and a spoken line from someone else
 * that holds a keyword also becomes a card in the ledger. It holds no RuneLite state but the message kind it is
 * handed.
 *
 * <p>Choice: every method is synchronized on the feed, so one line is handled at a time; the setters come from the
 * client thread or from start-up. The buffers and the ledger lock themselves, and the ledger tells its listeners with
 * its own lock let go, so a listener that only hands work to the event thread is fine with the feed's lock held.
 *
 * <p>Choice: the feed's own listeners are told with the feed's lock held, since a listener only hands work to the event
 * thread.
 */
final class MentionFeed
{
	private final Map<Chat, ChatBuffer> buffers = new EnumMap<>(Chat.class);
	private final MentionLedger ledger;
	private final Set<Chat> watched = EnumSet.allOf(Chat.class);
	private String setting = "";
	private boolean partial;
	private String playerName = "";
	private Keywords keywords;
	private final List<Consumer<Chat>> listeners = new ArrayList<>();

	MentionFeed(int linesKept, int cardsKept)
	{
		for (final Chat chat : Chat.values())
		{
			buffers.put(chat, new ChatBuffer(linesKept));
		}
		ledger = new MentionLedger(cardsKept);
		keywords = Keywords.of(setting, partial, playerName);
	}

	/** Remembers the keyword setting and the partial switch; the player's name is always the first keyword. */
	synchronized void keywords(String setting, boolean partial)
	{
		this.setting = setting;
		this.partial = partial;
		keywords = Keywords.of(setting, partial, playerName);
	}

	/** Keeps the player's own name (null reads as empty) and rebuilds the keywords. */
	synchronized void playerName(String rawName)
	{
		playerName = PlainText.name(rawName);
		keywords = Keywords.of(setting, partial, playerName);
	}

	/** The player's own cleaned name; empty while it is unknown. */
	synchronized String playerName()
	{
		return playerName;
	}

	/** Choice: All is always watched, so switching it off does nothing. */
	synchronized void watch(Chat chat, boolean on)
	{
		if (chat == Chat.ALL)
		{
			return;
		}
		if (on)
		{
			watched.add(chat);
		}
		else
		{
			watched.remove(chat);
		}
	}

	synchronized void linesKept(int n)
	{
		for (final ChatBuffer buffer : buffers.values())
		{
			buffer.resize(n);
		}
		tellEveryChat();
	}

	synchronized void cardsKept(int n)
	{
		ledger.resize(n);
	}

	/**
	 * Handles one chat line.
	 *
	 * @return whether the line made a card
	 */
	synchronized boolean line(ChatMessageType type, String rawName, String rawMessage, long now)
	{
		final Chat chat = Chat.of(type);
		if (chat == null || !watched.contains(chat))
		{
			return false;
		}
		String who = PlainText.name(rawName);
		if (type == ChatMessageType.PRIVATECHATOUT)
		{
			who = "To " + who;
		}
		final String text = PlainText.plain(rawMessage);
		final boolean spoken = Chat.spoken(type);
		final int icon = RankIcons.indexIn(rawName);
		final ChatLine line = buffers.get(chat).add(chat, now, who, text, spoken, icon);
		buffers.get(Chat.ALL).add(chat, now, who, text, spoken, icon);
		final boolean card = card(line);
		tell(chat);
		tell(Chat.ALL);
		return card;
	}

	/** Makes a card of a kept line when a player other than this one typed it and it holds a keyword. */
	private boolean card(ChatLine line)
	{
		if (!line.spoken() || (!playerName.isEmpty() && line.who().equalsIgnoreCase(playerName)))
		{
			return false;
		}
		final List<int[]> spans = keywords.find(line.text());
		if (spans.isEmpty())
		{
			return false;
		}
		ledger.add(new Mention(line.chat(), line.seq(), line.at(), line.who(), line.text(), spans, line.icon()));
		return true;
	}

	/**
	 * The keyword spans in a text, by the keywords in force now.
	 *
	 * <p>Choice: the kept chat colours its lines' keywords with the feed's own keywords, the player's name
	 * included, so the panel asks the feed rather than building keywords of its own.
	 */
	synchronized List<int[]> spans(String text)
	{
		return keywords.find(text);
	}

	/** Has {@code listener} told, with the chat, after each kept line, and with every chat after a clear or a resize. */
	synchronized void listen(Consumer<Chat> listener)
	{
		listeners.add(listener);
	}

	synchronized ChatBuffer buffer(Chat chat)
	{
		return buffers.get(chat);
	}

	synchronized MentionLedger ledger()
	{
		return ledger;
	}

	/** Empties every buffer and the ledger. */
	synchronized void clear()
	{
		for (final ChatBuffer buffer : buffers.values())
		{
			buffer.clear();
		}
		ledger.clear();
		tellEveryChat();
	}

	private void tell(Chat chat)
	{
		for (final Consumer<Chat> listener : listeners)
		{
			listener.accept(chat);
		}
	}

	private void tellEveryChat()
	{
		for (final Chat chat : Chat.values())
		{
			tell(chat);
		}
	}
}

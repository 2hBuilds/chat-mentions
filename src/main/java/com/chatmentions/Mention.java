package com.chatmentions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One card: a kept line that named a keyword, with the chat it came from and the spans to colour. */
final class Mention
{
	private final Chat chat;
	private final long seq;
	private final long at;
	private final String who;
	private final String text;
	private final List<int[]> spans;
	private final int icon;

	Mention(Chat chat, long seq, long at, String who, String text, List<int[]> spans, int icon)
	{
		this.chat = chat;
		this.seq = seq;
		this.at = at;
		this.who = who == null ? "" : who;
		this.text = text == null ? "" : text;
		this.spans = spans == null ? Collections.<int[]>emptyList() : Collections.unmodifiableList(new ArrayList<>(spans));
		this.icon = icon;
	}

	/** The chat the line came from. */
	Chat chat()
	{
		return chat;
	}

	/** The line's number in its chat's buffer. */
	long seq()
	{
		return seq;
	}

	/** When the line arrived, in milliseconds since the epoch. */
	long at()
	{
		return at;
	}

	/** The sender's name; empty when there is none. */
	String who()
	{
		return who;
	}

	/** The line's plain text. */
	String text()
	{
		return text;
	}

	/** The {@code {start, end}} spans of the keywords in the text; cannot be changed. */
	List<int[]> spans()
	{
		return spans;
	}

	/** The index of the sender's rank icon, -1 for none. */
	int icon()
	{
		return icon;
	}
}

package com.chatmentions;

/** One kept chat line: the chat it belongs to (never {@link Chat#ALL}), its sequence number, when it arrived, who sent it, its plain text, whether a player typed it and the index of the rank icon
 * in front of the sender's name (-1 for none). */
final class ChatLine
{
	private final Chat chat;
	private final long seq;
	private final long at;
	private final String who;
	private final String text;
	private final boolean spoken;
	private final int icon;

	ChatLine(Chat chat, long seq, long at, String who, String text, boolean spoken, int icon)
	{
		this.chat = chat;
		this.seq = seq;
		this.at = at;
		this.who = who == null ? "" : who;
		this.text = text == null ? "" : text;
		this.spoken = spoken;
		this.icon = icon;
	}

	/** The chat the line belongs to. */
	Chat chat()
	{
		return chat;
	}

	/** The line's number in its buffer. */
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

	/** Whether a player typed the line. */
	boolean spoken()
	{
		return spoken;
	}

	/** The index of the sender's rank icon, -1 for none. */
	int icon()
	{
		return icon;
	}
}

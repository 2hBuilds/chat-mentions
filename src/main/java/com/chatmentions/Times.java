package com.chatmentions;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** The words for a time on a card. */
final class Times
{
	private Times()
	{
	}

	/**
	 * The moment {@code at} (milliseconds since the epoch) in {@code zone} as {@code HH:mm} in 24 hours, English digits
	 * whatever the player's language.
	 */
	static String clock(long at, ZoneId zone)
	{
		return DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).format(Instant.ofEpochMilli(at).atZone(zone));
	}
}

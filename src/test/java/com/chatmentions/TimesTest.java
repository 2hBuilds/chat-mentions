package com.chatmentions;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link Times}: the clock on a card. */
public class TimesTest
{
	@Test
	public void theClockReadsHoursAndMinutesInTheZone()
	{
		final long at = Instant.parse("2026-10-28T14:32:10Z").toEpochMilli();
		assertEquals("UTC", "14:32", Times.clock(at, ZoneOffset.UTC));
		assertEquals("Toronto", "10:32", Times.clock(at, ZoneId.of("America/Toronto")));
		assertEquals("a leading zero", "00:05",
			Times.clock(Instant.parse("2026-10-28T00:05:00Z").toEpochMilli(), ZoneOffset.UTC));
	}
}

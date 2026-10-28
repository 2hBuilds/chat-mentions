package com.chatmentions;

import java.awt.Color;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link ChatMentionsConfig}: the defaults are the brief's. */
public class ChatMentionsConfigTest
{
	@Test
	public void theDefaultsAreTheBriefs()
	{
		final ChatMentionsConfig config = new ChatMentionsConfig()
		{
		};
		assertEquals("", config.keywords());
		assertTrue(config.wholeWordsOnly());
		assertTrue(config.watchPublic());
		assertTrue(config.watchPrivate());
		assertTrue(config.watchChannel());
		assertTrue(config.watchClan());
		assertFalse(config.watchGame());
		assertFalse(config.watchTrade());
		assertEquals(1000, config.cardsKept());
		assertEquals(3000, config.linesKept());
		assertEquals(new Color(59, 130, 246), config.defaultKeywordColour());
		assertEquals("", config.keywordColours());
		assertEquals(50, config.cardsShare());
		assertEquals("ALL", config.shownTab());
	}
}

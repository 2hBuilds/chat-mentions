package com.chatmentions;

import java.util.List;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link MentionFeed}: the lines kept, the cards made, the watched set and the sizes. */
public class MentionFeedTest
{
	private static void assertOthersEmpty(MentionFeed feed, Chat except)
	{
		for (final Chat chat : Chat.values())
		{
			if (chat != except && chat != Chat.ALL)
			{
				assertEquals(chat + " is empty", 0, feed.buffer(chat).lines().size());
			}
		}
	}

	@Test
	public void aSpokenLineWithAKeywordIsKeptAndMakesACard()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		feed.keywords("2h", false);
		feed.playerName("Sanka");
		assertTrue(feed.line(ChatMessageType.CLAN_CHAT, "<img=41>Klar", "<col=0000ff>gz 2h!</col>", 5000));

		final List<ChatLine> lines = feed.buffer(Chat.CLAN).lines();
		assertEquals(1, lines.size());
		assertEquals(1, lines.get(0).seq());
		assertEquals(5000, lines.get(0).at());
		assertEquals("Klar", lines.get(0).who());
		assertEquals("gz 2h!", lines.get(0).text());
		assertTrue(lines.get(0).spoken());

		final List<Mention> cards = feed.ledger().mentions();
		assertEquals(1, cards.size());
		assertEquals(Chat.CLAN, cards.get(0).chat());
		assertEquals(1, cards.get(0).seq());
		assertEquals(5000, cards.get(0).at());
		assertEquals("Klar", cards.get(0).who());
		assertEquals("gz 2h!", cards.get(0).text());
		assertEquals(1, cards.get(0).spans().size());
		assertArrayEquals(new int[]{3, 5}, java.util.Arrays.copyOf(cards.get(0).spans().get(0), 2));
		assertOthersEmpty(feed, Chat.CLAN);
	}

	@Test
	public void anUnwatchedChatKeepsNothing()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		feed.keywords("2h", false);
		feed.watch(Chat.PUBLIC, false);
		assertFalse(feed.line(ChatMessageType.PUBLICCHAT, "Klar", "2h", 1));
		assertEquals(0, feed.buffer(Chat.PUBLIC).lines().size());
		assertEquals(0, feed.ledger().mentions().size());

		feed.watch(Chat.PUBLIC, true);
		assertTrue(feed.line(ChatMessageType.PUBLICCHAT, "Klar", "2h", 1));
		assertEquals(1, feed.buffer(Chat.PUBLIC).lines().size());
	}

	@Test
	public void theGamesBroadcastIsKeptButMakesNoCard()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		feed.keywords("2h", false);
		assertFalse(feed.line(ChatMessageType.CLAN_MESSAGE, "", "2h has completed a quest", 1));
		final List<ChatLine> lines = feed.buffer(Chat.CLAN).lines();
		assertEquals(1, lines.size());
		assertEquals("", lines.get(0).who());
		assertFalse(lines.get(0).spoken());
		assertEquals(0, feed.ledger().mentions().size());
	}

	@Test
	public void thePlayersOwnLineMakesNoCard()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		feed.keywords("2h", false);
		feed.playerName("<img=2>Sanka");
		assertFalse(feed.line(ChatMessageType.CLAN_CHAT, "SANKA", "2h here", 1));
		assertEquals(1, feed.buffer(Chat.CLAN).lines().size());
		assertTrue(feed.line(ChatMessageType.CLAN_CHAT, "Sankaa", "2h here", 2));
	}

	@Test
	public void thePlayersNameIsAlwaysAKeyword()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		feed.keywords("", false);
		assertFalse(feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz sanka", 1));

		feed.playerName("Sanka");
		assertTrue(feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz Sanka!", 2));
		final Mention card = feed.ledger().mentions().get(0);
		assertEquals(1, card.spans().size());
		assertArrayEquals(new int[]{3, 8}, java.util.Arrays.copyOf(card.spans().get(0), 2));

		feed.keywords("2h", false);
		assertTrue("the name is still a keyword", feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz sanka", 3));
		assertTrue(feed.line(ChatMessageType.CLAN_CHAT, "Klar", "2h", 4));
	}

	@Test
	public void theSendersIconIndexIsKept()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		feed.keywords("2h", false);
		feed.playerName("<img=2>Sanka");
		assertEquals("Sanka", feed.playerName());
		assertTrue(feed.line(ChatMessageType.CLAN_CHAT, "<img=41>Klar", "2h here", 1));
		final ChatLine first = feed.buffer(Chat.CLAN).lines().get(0);
		assertEquals("Klar", first.who());
		assertEquals(41, first.icon());
		assertEquals(Chat.CLAN, first.chat());
		final ChatLine inAll = feed.buffer(Chat.ALL).lines().get(0);
		assertEquals(Chat.CLAN, inAll.chat());
		assertEquals(41, inAll.icon());
		assertEquals(41, feed.ledger().mentions().get(0).icon());
		assertTrue(feed.line(ChatMessageType.CLAN_CHAT, "Klar", "2h again", 2));
		assertEquals(-1, feed.buffer(Chat.CLAN).lines().get(1).icon());
	}

	@Test
	public void privateMessagesInAndOut()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		feed.keywords("2h", false);
		feed.playerName("Sanka");
		assertTrue(feed.line(ChatMessageType.PRIVATECHAT, "Klar", "2h?", 1));
		assertFalse(feed.line(ChatMessageType.PRIVATECHATOUT, "Klar", "2h yes", 2));
		final List<ChatLine> lines = feed.buffer(Chat.PRIVATE).lines();
		assertEquals(2, lines.size());
		assertEquals("Klar", lines.get(0).who());
		assertTrue(lines.get(0).spoken());
		assertEquals("To Klar", lines.get(1).who());
		assertFalse(lines.get(1).spoken());
		assertEquals(1, feed.ledger().mentions().size());
	}

	@Test
	public void aKindThatIsNoChatAndNullsAreHarmless()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		feed.keywords("2h", false);
		assertFalse(feed.line(ChatMessageType.DIALOG, "", "2h", 1));
		for (final Chat chat : Chat.values())
		{
			assertEquals(0, feed.buffer(chat).lines().size());
		}
		assertFalse(feed.line(null, null, null, 1));
		assertFalse(feed.line(ChatMessageType.CLAN_CHAT, null, null, 1));
		final List<ChatLine> lines = feed.buffer(Chat.CLAN).lines();
		assertEquals(1, lines.size());
		assertEquals("", lines.get(0).who());
		assertEquals("", lines.get(0).text());
	}

	@Test
	public void theSizesFollowTheSettings()
	{
		final MentionFeed feed = new MentionFeed(3, 2);
		feed.keywords("2h", false);
		feed.playerName("Sanka");
		for (int i = 1; i <= 5; i++)
		{
			feed.line(ChatMessageType.CLAN_CHAT, "Klar", "2h " + i, i);
		}
		final List<ChatLine> lines = feed.buffer(Chat.CLAN).lines();
		assertEquals(3, lines.size());
		assertEquals(3, lines.get(0).seq());
		assertEquals(4, lines.get(1).seq());
		assertEquals(5, lines.get(2).seq());
		final List<Mention> cards = feed.ledger().mentions();
		assertEquals(2, cards.size());
		assertEquals(5, cards.get(0).seq());
		assertEquals(4, cards.get(1).seq());

		feed.linesKept(1);
		assertEquals(1, feed.buffer(Chat.CLAN).lines().size());
		assertEquals(5, feed.buffer(Chat.CLAN).lines().get(0).seq());
		feed.cardsKept(1);
		assertEquals(1, feed.ledger().mentions().size());
		assertEquals(5, feed.ledger().mentions().get(0).seq());

		feed.clear();
		for (final Chat chat : Chat.values())
		{
			assertEquals(0, feed.buffer(chat).lines().size());
		}
		assertEquals(0, feed.ledger().mentions().size());
	}

	@Test
	public void everyChatHasItsOwnBuffer()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		final ChatMessageType[] kinds = {ChatMessageType.GAMEMESSAGE, ChatMessageType.PUBLICCHAT,
			ChatMessageType.PRIVATECHAT, ChatMessageType.FRIENDSCHAT, ChatMessageType.CLAN_CHAT, ChatMessageType.TRADEREQ};
		final Chat[] chats = {Chat.GAME, Chat.PUBLIC, Chat.PRIVATE, Chat.CHANNEL, Chat.CLAN, Chat.TRADE};
		feed.watch(Chat.ALL, false);
		for (final ChatMessageType kind : kinds)
		{
			feed.line(kind, "Klar", "hello", 1);
		}
		for (final Chat chat : chats)
		{
			final List<ChatLine> lines = feed.buffer(chat).lines();
			assertEquals(chat + " holds one line", 1, lines.size());
			assertEquals(1, lines.get(0).seq());
			assertEquals(chat, lines.get(0).chat());
		}
		final List<ChatLine> all = feed.buffer(Chat.ALL).lines();
		assertEquals("All holds the six in the order they came", 6, all.size());
		for (int i = 0; i < 6; i++)
		{
			assertEquals(chats[i], all.get(i).chat());
			assertEquals(i + 1, all.get(i).seq());
		}
	}

	@Test
	public void aKeywordMatchesInAnyCase()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		feed.keywords("2h", false);
		feed.playerName("2hBuilds");
		assertTrue(feed.line(ChatMessageType.CLAN_CHAT, "Klar", "2HBUILDS here", 1));
		assertTrue(feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz 2H", 2));
		assertTrue(feed.line(ChatMessageType.CLAN_CHAT, "Klar", "2hbuilds", 3));
		final List<Mention> cards = feed.ledger().mentions();
		assertEquals(3, cards.size());
		assertArrayEquals(new int[]{3, 5}, java.util.Arrays.copyOf(cards.get(1).spans().get(0), 2));
		assertEquals(1, cards.get(2).spans().size());
		assertArrayEquals(new int[]{0, 8}, java.util.Arrays.copyOf(cards.get(2).spans().get(0), 2));
		assertEquals(3, feed.buffer(Chat.ALL).lines().size());
	}

	@Test
	public void gameAndTradeLinesAreKeptAndMakeNoCard()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		feed.keywords("2h", false);
		feed.watch(Chat.GAME, true);
		feed.watch(Chat.TRADE, true);
		assertFalse(feed.line(ChatMessageType.GAMEMESSAGE, "", "2h has logged in.", 1));
		assertEquals(1, feed.buffer(Chat.GAME).lines().size());
		assertEquals(1, feed.buffer(Chat.ALL).lines().size());
		assertFalse(feed.line(ChatMessageType.TRADEREQ, "Klar", "Klar wishes to trade with you.", 2));
		assertEquals(1, feed.buffer(Chat.TRADE).lines().size());
		assertEquals(2, feed.buffer(Chat.ALL).lines().size());
		assertEquals(0, feed.ledger().mentions().size());

		feed.watch(Chat.GAME, false);
		assertFalse(feed.line(ChatMessageType.GAMEMESSAGE, "", "2h has logged in.", 3));
		assertEquals(1, feed.buffer(Chat.GAME).lines().size());
		assertEquals(2, feed.buffer(Chat.ALL).lines().size());
	}

	@Test
	public void partialWordsFollowTheSetting()
	{
		final MentionFeed feed = new MentionFeed(100, 10);
		feed.playerName("Sanka");
		feed.keywords("2h", false);
		assertFalse(feed.line(ChatMessageType.CLAN_CHAT, "Klar", "2hbuilds", 1));
		feed.keywords("2h", true);
		assertTrue(feed.line(ChatMessageType.CLAN_CHAT, "Klar", "2hbuilds", 2));
		final Mention card = feed.ledger().mentions().get(0);
		assertEquals(1, card.spans().size());
		assertArrayEquals(new int[]{0, 2}, java.util.Arrays.copyOf(card.spans().get(0), 2));
	}
}

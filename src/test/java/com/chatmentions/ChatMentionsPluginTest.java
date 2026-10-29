package com.chatmentions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.ChatMessageType;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.Test;

/**
 * Standalone runner: boots RuneLite in developer mode with 2h Chat Mentions injected. This is what {@code gradlew run}
 * starts. It lives in the test source set, so the packaged jar never holds it. Its tests are the partial-flag check and
 * the plugin's Chat Filter hand-off; {@code ExternalPluginManager.loadBuiltin} needs assertions on, which the run task
 * passes.
 */
public class ChatMentionsPluginTest
{
	private final Map<String, String> values = new HashMap<>();

	@Test
	public void partialIsTheInverseOfWholeWordsOnly()
	{
		assertFalse(ChatMentionsPlugin.partialOf(true));
		assertTrue(ChatMentionsPlugin.partialOf(false));
	}

	private MentionFeed feed()
	{
		final MentionFeed feed = new MentionFeed(100, 20);
		feed.keywords("gp", false);
		feed.playerName("Sanka");
		feed.watch(Chat.PUBLIC, true);
		return feed;
	}

	private ChatFilterRules rules(boolean follow)
	{
		return new ChatFilterRules((g, k) -> values.get(g + "." + k), follow, new ChatFilterRules.Who()
		{
			@Override
			public boolean isLocalPlayer(String name)
			{
				return false;
			}

			@Override
			public boolean isFriend(String name)
			{
				return false;
			}

			@Override
			public boolean isFriendsChatMember(String name)
			{
				return false;
			}

			@Override
			public boolean isClanMember(String name)
			{
				return false;
			}
		});
	}

	private void chatFilterOn()
	{
		values.put("runelite.chatfilterplugin", "true");
		values.put("chatfilter.filteredWords", "gp");
		values.put("chatfilter.filterType", "REMOVE_MESSAGE");
	}

	private static boolean say(MentionFeed feed, ChatFilterRules rules, String text)
	{
		return ChatMentionsPlugin.feedLine(feed, rules, PublicSetting.AUTOCHAT, ChatMessageType.PUBLICCHAT, "Bob", text, 1L);
	}

	@Test
	public void aLineChatFilterRemovesMakesNoCardAndIsNotKept()
	{
		chatFilterOn();
		final MentionFeed feed = feed();
		assertFalse(say(feed, rules(true), "buy gp"));
		assertTrue(feed.buffer(Chat.ALL).lines().isEmpty());
		assertEquals(0, feed.ledger().mentions().size());
	}

	@Test
	public void aCensoredWordMakesNoCardButTheLineIsKept()
	{
		chatFilterOn();
		values.put("chatfilter.filterType", "CENSOR_WORDS");
		final MentionFeed feed = feed();
		assertFalse(say(feed, rules(true), "buy gp"));
		assertEquals(1, feed.buffer(Chat.ALL).lines().size());
		assertEquals("buy **", feed.buffer(Chat.ALL).lines().get(0).text());
	}

	@Test
	public void withFollowChatFilterOffNothingIsFiltered()
	{
		chatFilterOn();
		final MentionFeed feed = feed();
		assertTrue(say(feed, rules(false), "buy gp"));
		assertEquals(1, feed.buffer(Chat.ALL).lines().size());
	}

	@Test
	public void withChatFilterDisabledNothingIsFiltered()
	{
		chatFilterOn();
		values.remove("runelite.chatfilterplugin");
		final MentionFeed feed = feed();
		assertTrue(say(feed, rules(true), "buy gp"));
		values.put("runelite.chatfilterplugin", "false");
		assertTrue(say(feed, rules(true), "buy gp"));
	}

	@Test
	public void aChatFilterSettingChangeTakesEffectOnTheNextLine()
	{
		final MentionFeed feed = feed();
		assertTrue(say(feed, rules(true), "buy gp"));
		chatFilterOn();
		assertFalse(say(feed, rules(true), "buy gp"));
		assertEquals(1, feed.buffer(Chat.ALL).lines().size());
	}

	@Test
	public void anAutochatLineIsLeftOutWhileTheGameShowsPublicOn()
	{
		final MentionFeed feed = feed();
		assertFalse(ChatMentionsPlugin.feedLine(feed, rules(true), PublicSetting.ON, ChatMessageType.AUTOTYPER, "Bob", "buy gp", 1L));
		assertTrue(feed.buffer(Chat.ALL).lines().isEmpty());
		assertEquals(0, feed.ledger().mentions().size());
	}

	@Test
	public void withTheTickOffTheGameSettingIsNotFollowed()
	{
		final MentionFeed feed = feed();
		assertTrue(ChatMentionsPlugin.feedLine(feed, rules(false), PublicSetting.ON, ChatMessageType.AUTOTYPER, "Bob", "buy gp", 1L));
	}

	@Test
	public void withAutochatShownTheAdvertMakesACard()
	{
		final MentionFeed feed = feed();
		assertTrue(ChatMentionsPlugin.feedLine(feed, rules(true), PublicSetting.AUTOCHAT, ChatMessageType.AUTOTYPER, "Bob", "buy gp", 1L));
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ChatMentionsPlugin.class);
		RuneLite.main(args);
	}
}

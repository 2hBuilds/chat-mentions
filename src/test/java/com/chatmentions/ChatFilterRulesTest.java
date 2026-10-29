package com.chatmentions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.ChatMessageType;
import org.junit.Test;

/** {@link ChatFilterRules}: Chat Filter's rules, pure, with the who-questions stubbed. */
public class ChatFilterRulesTest
{
	private final Map<String, String> values = new HashMap<>();
	private final Set<String> friends = new HashSet<>();
	private final Set<String> members = new HashSet<>();
	private final Set<String> clan = new HashSet<>();
	private final ChatFilterRules.Who who = new ChatFilterRules.Who()
	{
		@Override
		public boolean isLocalPlayer(String name)
		{
			return "Sanka".equalsIgnoreCase(name);
		}

		@Override
		public boolean isFriend(String name)
		{
			return friends.contains(name);
		}

		@Override
		public boolean isFriendsChatMember(String name)
		{
			return members.contains(name);
		}

		@Override
		public boolean isClanMember(String name)
		{
			return clan.contains(name);
		}
	};

	private void on()
	{
		values.put("runelite.chatfilterplugin", "true");
	}

	private void set(String key, String value)
	{
		values.put("chatfilter." + key, value);
	}

	private ChatFilterRules rules()
	{
		return new ChatFilterRules((group, key) -> values.get(group + "." + key), true, who);
	}

	private String pub(ChatFilterRules rules, String name, String text)
	{
		return rules.apply(ChatMessageType.PUBLICCHAT, name, text);
	}

	@Test
	public void offChangesNothing()
	{
		set("filteredWords", "gp");
		set("filterType", "REMOVE_MESSAGE");
		assertEquals("Selling gp", pub(rules(), "Bob", "Selling gp"));
		values.put("runelite.chatfilterplugin", "false");
		assertEquals("Selling gp", pub(rules(), "Bob", "Selling gp"));
		on();
		final ChatFilterRules notFollowing = new ChatFilterRules((g, k) -> values.get(g + "." + k), false, who);
		assertEquals("Selling gp", pub(notFollowing, "Bob", "Selling gp"));
	}

	@Test
	public void aFilteredWordRemovesTheLine()
	{
		on();
		set("filteredWords", "gp, selling");
		set("filterType", "REMOVE_MESSAGE");
		final ChatFilterRules rules = rules();
		assertNull(pub(rules, "Bob", "Selling cheap"));
		assertEquals("hello", pub(rules, "Bob", "hello"));
	}

	@Test
	public void censorWordsStarsEachMatch()
	{
		on();
		set("filteredWords", "gp, selling");
		set("filterType", "CENSOR_WORDS");
		assertEquals("******* ** now", pub(rules(), "Bob", "selling gp now"));
	}

	@Test
	public void censorMessageGivesTheSentence()
	{
		on();
		set("filteredWords", "gp");
		set("filterType", "CENSOR_MESSAGE");
		assertEquals("Hey, everyone, I just tried to say something very silly!", pub(rules(), "Bob", "buy gp"));
	}

	@Test
	public void regexLinesMatchAndABadOneIsSkipped()
	{
		on();
		set("filteredRegex", "^buy.*\n(\nfoo");
		set("filterType", "REMOVE_MESSAGE");
		final ChatFilterRules rules = rules();
		assertNull(pub(rules, "Bob", "buy bonds"));
		assertEquals("sell bonds", pub(rules, "Bob", "sell bonds"));
	}

	@Test
	public void aFilteredNameRemovesAllItsLines()
	{
		on();
		set("filteredNames", "bot\\d+");
		set("filterType", "REMOVE_MESSAGE");
		final ChatFilterRules rules = rules();
		assertNull(pub(rules, "Bot123", "hello"));
		assertEquals("hello", pub(rules, "Bot 123", "hello"));
	}

	@Test
	public void friendsAndMembersAreSparedUnlessAsked()
	{
		on();
		set("filteredWords", "gp");
		set("filterType", "REMOVE_MESSAGE");
		friends.add("Pal");
		members.add("Mate");
		clan.add("Kin");
		assertEquals("gp", pub(rules(), "Pal", "gp"));
		assertEquals("gp", pub(rules(), "Mate", "gp"));
		assertEquals("gp", pub(rules(), "Kin", "gp"));
		set("filterFriends", "true");
		assertNull(pub(rules(), "Pal", "gp"));
		assertEquals("gp", pub(rules(), "Mate", "gp"));
		set("filterClan", "true");
		assertNull(pub(rules(), "Mate", "gp"));
		assertEquals("gp", pub(rules(), "Kin", "gp"));
		set("filterClanChat", "true");
		assertNull(pub(rules(), "Kin", "gp"));
	}

	@Test
	public void yourOwnLinesAreNeverFiltered()
	{
		on();
		set("filteredWords", "gp");
		set("filterType", "REMOVE_MESSAGE");
		assertEquals("gp", pub(rules(), "Sanka", "gp"));
		assertEquals("gp", rules().apply(ChatMessageType.PRIVATECHATOUT, "Bob", "gp"));
	}

	@Test
	public void gameLinesOnlyWithFilterGameChat()
	{
		on();
		set("filteredWords", "gp");
		set("filterType", "REMOVE_MESSAGE");
		assertEquals("gp", rules().apply(ChatMessageType.GAMEMESSAGE, null, "gp"));
		set("filterGameChat", "true");
		assertNull(rules().apply(ChatMessageType.GAMEMESSAGE, null, "gp"));
		assertEquals("hi", rules().apply(ChatMessageType.GAMEMESSAGE, null, "hi"));
	}

	@Test
	public void accentsAreStrippedWhenAsked()
	{
		on();
		set("filteredWords", "cafe");
		set("filterType", "REMOVE_MESSAGE");
		assertEquals("un café", pub(rules(), "Bob", "un café"));
		set("stripAccents", "true");
		assertNull(pub(rules(), "Bob", "un café"));
	}

	@Test
	public void aRepeatedPublicLineIsDroppedWhenCollapsing()
	{
		on();
		set("collapsePlayerChat", "true");
		final ChatFilterRules rules = rules();
		assertEquals("hi all", pub(rules, "Bob", "hi all"));
		assertNull(pub(rules, "Bob", "hi all"));
		assertEquals("hi all", pub(rules, "Ann", "hi all"));
		assertEquals("hi clan", rules.apply(ChatMessageType.CLAN_CHAT, "Bob", "hi clan"));
		assertEquals("hi clan", rules.apply(ChatMessageType.CLAN_CHAT, "Bob", "hi clan"));
	}

	@Test
	public void theRepeatCountAloneChangesNothingAsInChatFilter()
	{
		on();
		set("maxRepeatedPublicChats", "2");
		final ChatFilterRules rules = rules();
		assertEquals("x", pub(rules, "Bob", "x"));
		assertEquals("x", pub(rules, "Bob", "x"));
		assertEquals("x", pub(rules, "Bob", "x"));
	}

	@Test
	public void anAccentedWordMatchesWithOrWithoutItsAccentWhenStripping()
	{
		on();
		set("filteredWords", "caf\u00e9");
		set("filterType", "REMOVE_MESSAGE");
		set("stripAccents", "true");
		assertNull(pub(rules(), "Bob", "un cafe"));
		assertNull(pub(rules(), "Bob", "un caf\u00e9"));
	}
}

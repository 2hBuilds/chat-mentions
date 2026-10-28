package com.chatmentions;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

/**
 * The plugin's settings. Flat on purpose: every item is declared here and this interface extends {@link Config}
 * alone, because RuneLite stores defaults only for the methods declared on the interface it is given. The group and
 * the keys are frozen at first release; renaming one silently discards every player's setting.
 */
@ConfigGroup(ChatMentionsConfig.GROUP)
public interface ChatMentionsConfig extends Config
{
	String GROUP = "chat-mentions";

	@ConfigItem(
		keyName = "keywords",
		name = "Keywords",
		description = "Words that make a card, separated by commas. Your own name is always one.",
		position = 1
	)
	default String keywords()
	{
		return "";
	}

	@ConfigItem(
		keyName = "wholeWordsOnly",
		name = "Whole words only",
		description = "A keyword counts as a whole word only. Untick to match it inside longer words too, like 2h in 2hbuilds.",
		position = 2
	)
	default boolean wholeWordsOnly()
	{
		return true;
	}

	@ConfigItem(
		keyName = "watchPublic",
		name = "Public",
		description = "Keep the public chat and show cards from it.",
		position = 3
	)
	default boolean watchPublic()
	{
		return true;
	}

	@ConfigItem(
		keyName = "watchPrivate",
		name = "Private",
		description = "Keep private messages and show cards from them.",
		position = 4
	)
	default boolean watchPrivate()
	{
		return true;
	}

	@ConfigItem(
		keyName = "watchChannel",
		name = "Channel",
		description = "Keep the friends chat (the Channel tab) and show cards from it.",
		position = 5
	)
	default boolean watchChannel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "watchClan",
		name = "Clan",
		description = "Keep the clan, guest clan and group ironman chat and show cards from it.",
		position = 6
	)
	default boolean watchClan()
	{
		return true;
	}

	@ConfigItem(
		keyName = "watchGame",
		name = "Game",
		description = "Keep the game's own messages, for reading.",
		position = 7
	)
	default boolean watchGame()
	{
		return false;
	}

	@ConfigItem(
		keyName = "watchTrade",
		name = "Trade",
		description = "Keep trade and challenge requests, for reading.",
		position = 8
	)
	default boolean watchTrade()
	{
		return false;
	}

	@ConfigItem(
		keyName = "cardsKept",
		name = "Cards kept",
		description = "How many mention cards the panel keeps.",
		position = 9
	)
	@Range(min = 1, max = 1000)
	default int cardsKept()
	{
		return 1000;
	}

	@ConfigItem(
		keyName = "linesKept",
		name = "Chat lines kept",
		description = "How many lines of each chat are kept.",
		position = 10
	)
	@Range(min = 100, max = 20000)
	default int linesKept()
	{
		return 3000;
	}

	@ConfigItem(
		keyName = "defaultKeywordColour",
		name = "Default keyword colour",
		description = "The colour of your name and of every keyword without a colour of its own, in the cards and the kept chat.",
		position = 11
	)
	default Color defaultKeywordColour()
	{
		return new Color(59, 130, 246);
	}

	@ConfigItem(
		keyName = "keywordColours",
		name = "Keyword colours",
		description = "A colour per keyword, as word=rrggbb pairs separated by commas. Click a keyword in the panel to set one.",
		position = 12
	)
	default String keywordColours()
	{
		return "";
	}

	@ConfigItem(
		keyName = "cardsShare",
		name = "Cards share",
		description = "How much of the panel's height the cards take, in percent. Drag the divider to set it.",
		position = 13
	)
	@Range(min = 10, max = 90)
	default int cardsShare()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "shownTab",
		name = "Tab shown",
		description = "The chat tab the panel opens on.",
		position = 14
	)
	default String shownTab()
	{
		return "ALL";
	}
}

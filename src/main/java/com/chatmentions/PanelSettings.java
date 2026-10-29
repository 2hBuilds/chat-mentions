package com.chatmentions;

import java.awt.Color;

/**
 * The settings the panel's menu shows and writes: what it reads when it opens, and what it writes when a control
 * changes. The plugin backs it with the config and the config manager; a test backs it with lists.
 */
interface PanelSettings
{
	/** The keywords setting, as typed. */
	String keywords();

	/** Writes the keywords setting. */
	void setKeywords(String words);

	/** Whether a keyword counts as a whole word only. */
	boolean wholeWordsOnly();

	/** Writes whether a keyword counts as a whole word only. */
	void setWholeWordsOnly(boolean on);

	/** Whether chat lines are hidden and censored the way RuneLite's Chat Filter does, when it is on. */
	boolean followChatFilter();

	/** Writes whether chat lines follow RuneLite's Chat Filter. */
	void setFollowChatFilter(boolean on);

	/** Whether {@code chat} is kept and makes cards. */
	boolean watched(Chat chat);

	/** Writes whether {@code chat} is kept and makes cards. */
	void setWatched(Chat chat, boolean on);

	/** How many cards the panel keeps. */
	int cardsKept();

	/** Writes how many cards the panel keeps. */
	void setCardsKept(int n);

	/** How many lines of each chat are kept. */
	int linesKept();

	/** Writes how many lines of each chat are kept. */
	void setLinesKept(int n);

	/** The colour of a keyword in a card and in the kept chat. */
	Color keywordColour();

	/** Writes the colour of a keyword. */
	void setKeywordColour(Color colour);

	/** The per-word colours, as word=rrggbb pairs separated by commas. */
	String keywordColours();

	/** Writes the per-word colours. */
	void setKeywordColours(String setting);

	/** The share of the panel's height the cards take, in whole percent. */
	int cardsShare();

	/** Writes the cards' share, in whole percent. */
	void setCardsShare(int percent);

	/** The name of the {@link Chat} whose tab the panel opens on. */
	String shownTab();

	/** Writes the name of the {@link Chat} whose tab the panel opens on. */
	void setShownTab(String name);
}

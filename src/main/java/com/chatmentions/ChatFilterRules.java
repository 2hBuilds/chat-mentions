package com.chatmentions;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.runelite.api.ChatMessageType;
import net.runelite.client.util.Text;

/**
 * RuneLite's Chat Filter, as this plugin applies it: the same words, regular expressions, names, friends and clan
 * exceptions, game-message switch, accent stripping and repeat rules, read from Chat Filter's own settings and run on
 * every line as it arrives, so a line the chatbox does not draw (another tab, a minimized chatbox) is filtered too.
 * Pure: the settings come in through a lookup and the who-questions through {@link Who}; nothing here touches the client.
 * Not thread safe; the plugin calls it from the client thread alone.
 */
final class ChatFilterRules
{
	/** Chat Filter's own settings group. */
	static final String GROUP = "chatfilter";

	/** The group and key of the switch that tells whether the Chat Filter plugin is on. */
	static final String PLUGIN_GROUP = "runelite";
	static final String PLUGIN_KEY = "chatfilterplugin";

	/** What Chat Filter shows in place of a whole line when its type is "Censor message". */
	static final String CENSORED_MESSAGE = "Hey, everyone, I just tried to say something very silly!";

	/** How many of the latest public lines the repeat rules look back over. */
	private static final int REPEAT_WINDOW = 100;

	/** The questions about the people a line comes from, answered by the plugin with the client. */
	interface Who
	{
		/** Whether {@code name} is the local player. */
		boolean isLocalPlayer(String name);

		/** Whether {@code name} is on the friends list. */
		boolean isFriend(String name);

		/** Whether {@code name} is in the friends chat the player is in. */
		boolean isFriendsChatMember(String name);

		/** Whether {@code name} is in the player's clan or guest clan channel. */
		boolean isClanMember(String name);
	}

	private enum Type
	{
		CENSOR_WORDS,
		CENSOR_MESSAGE,
		REMOVE_MESSAGE
	}

	private final boolean enabled;
	private final boolean follow;
	private final Who who;
	private final List<Pattern> patterns = new ArrayList<>();
	private final List<Pattern> namePatterns = new ArrayList<>();
	private Type type = Type.CENSOR_WORDS;
	private boolean filterFriends;
	private boolean filterClan;
	private boolean filterClanChat;
	private boolean filterGameChat;
	private boolean collapsePlayerChat;
	private boolean stripAccents;
	private final Deque<String> recentPublic = new ArrayDeque<>();

	/**
	 * Reads Chat Filter's settings through {@code config} (group, key to the stored value, or null).
	 *
	 * @param follow whether this plugin's own "Block what the chatbox blocks" setting is on; off makes rules that change nothing
	 */
	ChatFilterRules(BiFunction<String, String, String> config, boolean follow, Who who)
	{
		this.who = who;
		this.follow = follow;
		this.enabled = follow && Boolean.parseBoolean(config.apply(PLUGIN_GROUP, PLUGIN_KEY));
		if (!enabled)
		{
			return;
		}
		// Read first: Chat Filter strips the accents of its words and patterns too, not only of the lines.
		stripAccents = flag(config, "stripAccents");
		for (final String word : Text.fromCSV(orEmpty(config.apply(GROUP, "filteredWords"))))
		{
			patterns.add(Pattern.compile(Pattern.quote(strip(word)), Pattern.CASE_INSENSITIVE));
		}
		addPatterns(patterns, config.apply(GROUP, "filteredRegex"));
		addPatterns(namePatterns, config.apply(GROUP, "filteredNames"));
		try
		{
			final String chosen = config.apply(GROUP, "filterType");
			type = chosen == null ? Type.CENSOR_WORDS : Type.valueOf(chosen.trim());
		}
		catch (IllegalArgumentException e)
		{
			type = Type.CENSOR_WORDS;
		}
		filterFriends = flag(config, "filterFriends");
		filterClan = flag(config, "filterClan");
		filterClanChat = flag(config, "filterClanChat");
		filterGameChat = flag(config, "filterGameChat");
		collapsePlayerChat = flag(config, "collapsePlayerChat");
	}

	/** Whether this plugin's own tick is on: the Public setting follows it too. */
	boolean follows()
	{
		return follow;
	}

	/** Whether the sender is the local player, by the same answer the rules use. */
	boolean fromYou(String name)
	{
		return who.isLocalPlayer(name == null ? "" : Text.removeTags(name));
	}

	/** Whether the sender is on the friends list, by the same answer the rules use. */
	boolean fromFriend(String name)
	{
		return who.isFriend(name == null ? "" : Text.removeTags(name));
	}

	/**
	 * Runs one line through the rules.
	 *
	 * @param name the sender as the game gives it (null for a game line)
	 * @return the text to keep (the given text, unchanged, when nothing applied), or null when the line is removed
	 */
	String apply(ChatMessageType messageType, String name, String text)
	{
		if (!enabled || messageType == null || text == null)
		{
			return text;
		}
		if (isGame(messageType))
		{
			return filterGameChat ? censor(null, text) : text;
		}
		if (!isPlayer(messageType))
		{
			return text;
		}
		final String plainName = name == null ? "" : Text.removeTags(name);
		if (!canFilterPlayer(plainName))
		{
			return text;
		}
		final String censored = censor(plainName, text);
		if (censored == null)
		{
			return null;
		}
		// Chat Filter weighs repeats only with "Collapse player chat" on, and then shows one copy; its "Repeat filter"
		// count is read inside that same switch, so with one copy kept here it needs nothing more.
		if ((messageType == ChatMessageType.PUBLICCHAT || messageType == ChatMessageType.MODCHAT) && collapsePlayerChat)
		{
			final String key = plainName + ":" + text;
			if (recentPublic.contains(key))
			{
				return null;
			}
			recentPublic.addLast(key);
			if (recentPublic.size() > REPEAT_WINDOW)
			{
				recentPublic.removeFirst();
			}
		}
		return censored;
	}

	private boolean canFilterPlayer(String name)
	{
		if (who.isLocalPlayer(name))
		{
			return false;
		}
		return (filterFriends || !who.isFriend(name))
			&& (filterClan || !who.isFriendsChatMember(name))
			&& (filterClanChat || !who.isClanMember(name));
	}

	/** Chat Filter's censorMessage: the text to show, or null to remove the line; the text as given when unchanged. */
	private String censor(String name, String text)
	{
		if (name != null)
		{
			final String standard = Text.standardize(name);
			for (final Pattern pattern : namePatterns)
			{
				if (pattern.matcher(standard).find())
				{
					switch (type)
					{
						case CENSOR_WORDS:
							return Text.escapeJagex(stars(Text.unescapeJagex(text).length()));
						case CENSOR_MESSAGE:
							return CENSORED_MESSAGE;
						default:
							return null;
					}
				}
			}
		}
		String message = Text.JAGEX_PRINTABLE_CHAR_MATCHER.retainFrom(Text.unescapeJagex(text)).replace('\u00A0', ' ');
		message = strip(message);
		boolean changed = false;
		for (final Pattern pattern : patterns)
		{
			final Matcher matcher = pattern.matcher(message);
			if (!matcher.find())
			{
				continue;
			}
			switch (type)
			{
				case CENSOR_WORDS:
					final StringBuffer out = new StringBuffer();
					do
					{
						matcher.appendReplacement(out, Matcher.quoteReplacement(stars(matcher.group().length())));
					}
					while (matcher.find());
					matcher.appendTail(out);
					message = out.toString();
					changed = true;
					break;
				case CENSOR_MESSAGE:
					return CENSORED_MESSAGE;
				default:
					return null;
			}
		}
		return changed ? Text.escapeJagex(message) : text;
	}

	/** The text without its accents when "Strip accents" is on; as given otherwise. */
	private String strip(String text)
	{
		return stripAccents ? Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "") : text;
	}

	private static String stars(int length)
	{
		final StringBuilder out = new StringBuilder(length);
		for (int i = 0; i < length; i++)
		{
			out.append('*');
		}
		return out.toString();
	}

	private static boolean isPlayer(ChatMessageType t)
	{
		switch (t)
		{
			case PUBLICCHAT:
			case MODCHAT:
			case AUTOTYPER:
			case PRIVATECHAT:
			case MODPRIVATECHAT:
			case FRIENDSCHAT:
			case CLAN_CHAT:
			case CLAN_GUEST_CHAT:
			case CLAN_GIM_CHAT:
				return true;
			default:
				return false;
		}
	}

	private static boolean isGame(ChatMessageType t)
	{
		switch (t)
		{
			case GAMEMESSAGE:
			case ENGINE:
			case FRIENDSCHATNOTIFICATION:
			case ITEM_EXAMINE:
			case NPC_EXAMINE:
			case OBJECT_EXAMINE:
			case SPAM:
			case CLAN_MESSAGE:
			case CLAN_GUEST_MESSAGE:
			case CLAN_GIM_MESSAGE:
			case NPC_SAY:
				return true;
			default:
				return false;
		}
	}

	private void addPatterns(List<Pattern> into, String lines)
	{
		for (final String line : orEmpty(lines).split("\n"))
		{
			final String trimmed = line.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}
			try
			{
				into.add(Pattern.compile(strip(trimmed), Pattern.CASE_INSENSITIVE));
			}
			catch (PatternSyntaxException e)
			{
				// Chat Filter skips a pattern that does not compile; so do we.
			}
		}
	}

	private static boolean flag(BiFunction<String, String, String> config, String key)
	{
		return Boolean.parseBoolean(config.apply(GROUP, key));
	}

	private static String orEmpty(String s)
	{
		return s == null ? "" : s;
	}
}

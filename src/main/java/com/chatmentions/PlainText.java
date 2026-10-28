package com.chatmentions;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.util.Text;

/**
 * A chat line as plain text: the game's string variables filled in, the tags gone, the no-break space a plain space,
 * the ends trimmed. Pure; the only client code it touches is {@link Text#removeTags}.
 */
final class PlainText
{
	/** The task prefix {@code CA_ID:<digits>|} with the whitespace around it, at the start of a text. */
	private static final Pattern LEAD = Pattern.compile("^\\s*(?:CA_ID:\\d+\\|)?\\s*");
	/** Any tag. */
	private static final Pattern TAG = Pattern.compile("<[^>]*>");
	/**
	 * A string variable tag: {@code <str_NAME=VALUE>} defines one (group 3 is the value, up to the first "&gt;") and
	 * {@code <str_NAME>} refers to one (group 2 is null).
	 */
	private static final Pattern VARIABLE = Pattern.compile("<str_([A-Za-z0-9_]+)(=([^>]*))?>");

	private PlainText()
	{
	}

	/**
	 * The raw message with the game's string variables resolved: each definition tag is removed and remembered (a later
	 * one of the same NAME replaces the earlier), each reference tag becomes the value remembered for its NAME, or
	 * nothing when it was never defined. Every other tag and character stays; null gives an empty text.
	 */
	static String expand(String raw)
	{
		if (raw == null)
		{
			return "";
		}
		final Map<String, String> values = new HashMap<>();
		final StringBuilder out = new StringBuilder(raw.length());
		final Matcher tags = TAG.matcher(raw);
		int from = 0;
		while (tags.find())
		{
			out.append(raw, from, tags.start());
			from = tags.end();
			final Matcher variable = VARIABLE.matcher(tags.group());
			if (!variable.matches())
			{
				out.append(tags.group());
			}
			else if (variable.group(2) != null)
			{
				values.put(variable.group(1), variable.group(3));
			}
			else
			{
				out.append(values.getOrDefault(variable.group(1), ""));
			}
		}
		return out.append(raw, from, raw.length()).toString();
	}

	/**
	 * The message expanded, without its tags, with every U+00A0 written as a plain space, without a leading
	 * {@code CA_ID:<digits>|} and the whitespace around it, and without trailing whitespace; an empty text for null.
	 *
	 * <p>Choice: the prefix is taken off only when it stands first once the tags are gone, which is where the game puts
	 * it. Each no-break space is one character, so the length and every offset stay true.
	 */
	static String plain(String raw)
	{
		if (raw == null)
		{
			return "";
		}
		final String text = Text.removeTags(expand(raw)).replace('\u00a0', ' ');
		final Matcher lead = LEAD.matcher(text);
		final int from = lead.find() ? lead.end() : 0;
		int to = text.length();
		while (to > from && Character.isWhitespace(text.charAt(to - 1)))
		{
			to--;
		}
		return text.substring(from, to);
	}

	/**
	 * A sender's name as the game sends it (a rank icon tag in front, a no-break space inside): {@link #plain} with
	 * every run of spaces collapsed to one; an empty text for null.
	 */
	static String name(String raw)
	{
		return plain(raw).replaceAll(" +", " ");
	}
}

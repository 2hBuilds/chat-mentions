package com.chatmentions;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The colour of each keyword that has one of its own, read from and written to the setting string. Immutable.
 *
 * <p>The setting is a comma list of {@code word=rrggbb} pairs: the hex is six digits, case ignored on both the word and
 * the hex; spaces round the word and the hex are trimmed; a pair that does not parse, or has an empty word, is dropped;
 * a later pair for the same word (ignoring case) replaces the earlier one.
 */
final class WordColours
{
	private static final WordColours NONE = new WordColours(Collections.<String>emptyList(), Collections.<Color>emptyList());

	private final List<String> words;
	private final List<Color> colours;

	private WordColours(List<String> words, List<Color> colours)
	{
		this.words = words;
		this.colours = colours;
	}

	/** The colours of a setting string; null reads as empty. */
	static WordColours of(String setting)
	{
		if (setting == null || setting.isEmpty())
		{
			return NONE;
		}
		final List<String> words = new ArrayList<>();
		final List<Color> colours = new ArrayList<>();
		for (final String piece : setting.split(",", -1))
		{
			final int equals = piece.indexOf('=');
			if (equals < 0)
			{
				continue;
			}
			final String word = piece.substring(0, equals).trim();
			final String hex = piece.substring(equals + 1).trim();
			if (word.isEmpty() || !hex.matches("[0-9a-fA-F]{6}"))
			{
				continue;
			}
			final Color colour = new Color(Integer.parseInt(hex, 16));
			final int at = indexOf(words, word);
			if (at >= 0)
			{
				// Choice: a later pair replaces the earlier in place, keeping the later spelling of the word.
				words.set(at, word);
				colours.set(at, colour);
			}
			else
			{
				words.add(word);
				colours.add(colour);
			}
		}
		return new WordColours(words, colours);
	}

	private static int indexOf(List<String> list, String word)
	{
		for (int i = 0; i < list.size(); i++)
		{
			if (list.get(i).equalsIgnoreCase(word))
			{
				return i;
			}
		}
		return -1;
	}

	/** The colour of {@code word}, ignoring case; null when it has none. */
	Color colour(String word)
	{
		if (word == null)
		{
			return null;
		}
		final int at = indexOf(words, word);
		return at < 0 ? null : colours.get(at);
	}

	/** These colours with {@code word} set to {@code colour} (its RGB), replacing in place or added last. */
	WordColours with(String word, Color colour)
	{
		final List<String> nextWords = new ArrayList<>(words);
		final List<Color> nextColours = new ArrayList<>(colours);
		final Color rgb = new Color(colour.getRGB() & 0xffffff);
		final int at = indexOf(nextWords, word);
		if (at >= 0)
		{
			nextWords.set(at, word);
			nextColours.set(at, rgb);
		}
		else
		{
			nextWords.add(word);
			nextColours.add(rgb);
		}
		return new WordColours(nextWords, nextColours);
	}

	/** These colours without {@code word}, ignoring case. */
	WordColours without(String word)
	{
		final int at = word == null ? -1 : indexOf(words, word);
		if (at < 0)
		{
			return this;
		}
		final List<String> nextWords = new ArrayList<>(words);
		final List<Color> nextColours = new ArrayList<>(colours);
		nextWords.remove(at);
		nextColours.remove(at);
		return new WordColours(nextWords, nextColours);
	}

	/** The setting string: the pairs in order as {@code word=rrggbb} joined by {@code ", "}, the hex lowercase. */
	String setting()
	{
		final StringBuilder out = new StringBuilder();
		for (int i = 0; i < words.size(); i++)
		{
			if (i > 0)
			{
				out.append(", ");
			}
			out.append(words.get(i)).append('=').append(String.format("%06x", colours.get(i).getRGB() & 0xffffff));
		}
		return out.toString();
	}

	/** Whether no word has a colour. */
	boolean isEmpty()
	{
		return words.isEmpty();
	}
}

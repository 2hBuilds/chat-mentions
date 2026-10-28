package com.chatmentions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The words that make a chat line a mention, and the finder that marks them in a line. Immutable.
 *
 * <p>Choice: a word is matched ignoring case; with {@code partial} off a hit must stand between characters that are not
 * letters or digits (or at an end of the text), with it on a hit may sit inside a word.
 */
final class Keywords
{
	private final List<String> words;
	private final boolean partial;

	private Keywords(List<String> words, boolean partial)
	{
		this.words = Collections.unmodifiableList(words);
		this.partial = partial;
	}

	/**
	 * The keywords of a comma-separated setting, led by the player's own name: the cleaned {@code name}, when it is not
	 * empty, is the first word; the setting's pieces follow in their order, each with U+00A0 as a space, trimmed, runs of
	 * spaces collapsed, empties dropped, and a piece equal to an earlier word or to the name ignoring case dropped; the
	 * rest is kept in order and case.
	 */
	static Keywords of(String setting, boolean partial, String name)
	{
		final List<String> kept = new ArrayList<>();
		if (name != null)
		{
			final String own = cleaned(name);
			if (!own.isEmpty())
			{
				kept.add(own);
			}
		}
		if (setting != null)
		{
			for (final String piece : setting.split(",", -1))
			{
				final String word = cleaned(piece);
				if (!word.isEmpty() && !has(kept, word))
				{
					kept.add(word);
				}
			}
		}
		return new Keywords(kept, partial);
	}

	private static String cleaned(String piece)
	{
		return piece.replace('\u00a0', ' ').trim().replaceAll(" +", " ");
	}

	private static boolean has(List<String> list, String word)
	{
		for (final String have : list)
		{
			if (have.equalsIgnoreCase(word))
			{
				return true;
			}
		}
		return false;
	}

	/** The words, in the setting's order; cannot be changed. */
	List<String> words()
	{
		return words;
	}

	/**
	 * The hits in the text as {@code {start, end, word}}, sorted by start, {@code word} being the index into
	 * {@link #words()} of the keyword that made the hit (the name is word 0); two that overlap are merged into one that
	 * keeps the word of the one that comes first in {@code words()}, two that only touch are left apart. U+00A0 reads as a space (one character for one, so the offsets are the text's own). A null or
	 * empty text, or no words, gives an empty list.
	 */
	List<int[]> find(String text)
	{
		final List<int[]> hits = new ArrayList<>();
		if (text == null || text.isEmpty() || words.isEmpty())
		{
			return hits;
		}
		final String read = text.replace('\u00a0', ' ');
		for (int which = 0; which < words.size(); which++)
		{
			final String word = words.get(which);
			final int length = word.length();
			for (int index = 0; index + length <= read.length(); index++)
			{
				if (!read.regionMatches(true, index, word, 0, length))
				{
					continue;
				}
				final int end = index + length;
				final boolean whole = (index == 0 || !Character.isLetterOrDigit(read.charAt(index - 1)))
					&& (end == read.length() || !Character.isLetterOrDigit(read.charAt(end)));
				if (partial || whole)
				{
					hits.add(new int[]{index, end, which});
				}
			}
		}
		Collections.sort(hits, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
		final List<int[]> merged = new ArrayList<>();
		for (final int[] hit : hits)
		{
			final int[] last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
			if (last != null && hit[0] < last[1])
			{
				last[1] = Math.max(last[1], hit[1]);
				last[2] = Math.min(last[2], hit[2]);
			}
			else
			{
				merged.add(hit);
			}
		}
		return merged;
	}
}

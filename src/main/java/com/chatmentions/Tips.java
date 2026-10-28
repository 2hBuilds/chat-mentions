package com.chatmentions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** The tips the empty cards half shows, one drawn each time the half becomes empty. */
final class Tips
{
	/** The five tips, in order; each starts with "Tip:". */
	static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
		"Tip: chat lines that name you or a keyword show here.",
		"Tip: click a keyword to pick its colour.",
		"Tip: type a keyword in the box below and press Enter.",
		"Tip: click a card to jump to its line in the chat.",
		"Tip: 'How to use' is in the settings menu."));

	private Tips()
	{
	}

	/** One of {@link #ALL}, drawn with {@code random}. */
	static String pick(Random random)
	{
		return ALL.get(random.nextInt(ALL.size()));
	}
}

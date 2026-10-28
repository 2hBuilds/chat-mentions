package com.chatmentions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import net.runelite.client.RuneLite;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Standalone runner: boots RuneLite in developer mode with 2h Chat Mentions injected. This is what {@code gradlew run}
 * starts. It lives in the test source set, so the packaged jar never holds it. Its one test is the partial-flag
 * check; {@code ExternalPluginManager.loadBuiltin} needs assertions on, which the run task passes.
 */
public class ChatMentionsPluginTest
{
	@Test
	public void partialIsTheInverseOfWholeWordsOnly()
	{
		assertFalse(ChatMentionsPlugin.partialOf(true));
		assertTrue(ChatMentionsPlugin.partialOf(false));
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ChatMentionsPlugin.class);
		RuneLite.main(args);
	}
}

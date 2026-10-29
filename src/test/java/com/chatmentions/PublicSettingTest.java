package com.chatmentions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.ChatMessageType;
import org.junit.Test;

/** {@link PublicSetting}: the chatbox's Public label and what each setting draws. */
public class PublicSettingTest
{
	@Test
	public void theLabelsReadAsTheSettings()
	{
		assertEquals(PublicSetting.ON, PublicSetting.of("<br>On"));
		assertEquals(PublicSetting.AUTOCHAT, PublicSetting.of("<br><col=00afff>Autochat</col>"));
		assertEquals(PublicSetting.FRIENDS, PublicSetting.of("<br>Friends"));
		assertEquals(PublicSetting.OFF, PublicSetting.of("<br>Off"));
		assertEquals(PublicSetting.OFF, PublicSetting.of("<br>Hide"));
		assertEquals(PublicSetting.UNKNOWN, PublicSetting.of(""));
		assertEquals(PublicSetting.UNKNOWN, PublicSetting.of(null));
		assertEquals(PublicSetting.UNKNOWN, PublicSetting.of("<br>Something"));
	}

	@Test
	public void onLeavesOutAutochatOnly()
	{
		final PublicSetting on = PublicSetting.ON;
		assertTrue(on.keeps(ChatMessageType.PUBLICCHAT, false, false));
		assertTrue(on.keeps(ChatMessageType.MODCHAT, false, false));
		assertFalse(on.keeps(ChatMessageType.AUTOTYPER, false, false));
		assertFalse(on.keeps(ChatMessageType.MODAUTOTYPER, false, false));
	}

	@Test
	public void autochatAndUnknownKeepEverything()
	{
		for (final PublicSetting setting : new PublicSetting[]{PublicSetting.AUTOCHAT, PublicSetting.UNKNOWN})
		{
			assertTrue(setting.keeps(ChatMessageType.PUBLICCHAT, false, false));
			assertTrue(setting.keeps(ChatMessageType.AUTOTYPER, false, false));
			assertTrue(setting.keeps(ChatMessageType.MODAUTOTYPER, false, false));
		}
	}

	@Test
	public void friendsKeepsOnlyFriends()
	{
		final PublicSetting friends = PublicSetting.FRIENDS;
		assertTrue(friends.keeps(ChatMessageType.PUBLICCHAT, false, true));
		assertFalse(friends.keeps(ChatMessageType.PUBLICCHAT, false, false));
		assertFalse(friends.keeps(ChatMessageType.AUTOTYPER, false, false));
	}

	@Test
	public void offKeepsNoPublicLine()
	{
		assertFalse(PublicSetting.OFF.keeps(ChatMessageType.PUBLICCHAT, false, false));
		assertFalse(PublicSetting.OFF.keeps(ChatMessageType.AUTOTYPER, false, false));
	}

	@Test
	public void yourOwnLinesAndOtherChatsAreAlwaysKept()
	{
		assertTrue(PublicSetting.OFF.keeps(ChatMessageType.PUBLICCHAT, true, false));
		assertTrue(PublicSetting.OFF.keeps(ChatMessageType.CLAN_CHAT, false, false));
		assertTrue(PublicSetting.OFF.keeps(ChatMessageType.PRIVATECHAT, false, false));
		assertTrue(PublicSetting.OFF.keeps(ChatMessageType.FRIENDSCHAT, false, false));
	}
}

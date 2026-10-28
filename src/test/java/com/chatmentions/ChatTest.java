package com.chatmentions;

import net.runelite.api.ChatMessageType;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** {@link Chat}: which chat a kind of line belongs to, which kinds a player typed, and the labels. */
public class ChatTest
{
	private static void kind(ChatMessageType type, Chat expected)
	{
		assertEquals("the chat of " + type, expected, Chat.of(type));
	}

	private static void kinds(Chat expected, ChatMessageType... types)
	{
		for (final ChatMessageType type : types)
		{
			kind(type, expected);
		}
	}

	@Test
	public void everyKeptKindMapsToItsChat()
	{
		kinds(Chat.PUBLIC, ChatMessageType.PUBLICCHAT, ChatMessageType.MODCHAT, ChatMessageType.AUTOTYPER,
			ChatMessageType.MODAUTOTYPER);
		kinds(Chat.PRIVATE, ChatMessageType.PRIVATECHAT, ChatMessageType.PRIVATECHATOUT, ChatMessageType.MODPRIVATECHAT);
		kinds(Chat.CHANNEL, ChatMessageType.FRIENDSCHAT, ChatMessageType.FRIENDSCHATNOTIFICATION);
		kinds(Chat.CLAN, ChatMessageType.CLAN_CHAT, ChatMessageType.CLAN_MESSAGE, ChatMessageType.CLAN_GUEST_CHAT,
			ChatMessageType.CLAN_GUEST_MESSAGE, ChatMessageType.CLAN_GIM_CHAT, ChatMessageType.CLAN_GIM_MESSAGE);
		kinds(Chat.GAME, ChatMessageType.GAMEMESSAGE, ChatMessageType.ENGINE, ChatMessageType.BROADCAST,
			ChatMessageType.WELCOME, ChatMessageType.LOGINLOGOUTNOTIFICATION, ChatMessageType.FRIENDNOTIFICATION,
			ChatMessageType.IGNORENOTIFICATION, ChatMessageType.ITEM_EXAMINE, ChatMessageType.NPC_EXAMINE,
			ChatMessageType.OBJECT_EXAMINE, ChatMessageType.SPAM, ChatMessageType.PLAYERRELATED,
			ChatMessageType.TENSECTIMEOUT, ChatMessageType.LEVELUPMESSAGE, ChatMessageType.CONSOLE,
			ChatMessageType.DIDYOUKNOW, ChatMessageType.CLAN_CREATION_INVITATION, ChatMessageType.CLAN_GIM_FORM_GROUP,
			ChatMessageType.CLAN_GIM_GROUP_WITH);
		kinds(Chat.TRADE, ChatMessageType.TRADEREQ, ChatMessageType.TRADE, ChatMessageType.TRADE_SENT,
			ChatMessageType.CHALREQ_TRADE, ChatMessageType.CHALREQ_FRIENDSCHAT, ChatMessageType.CHALREQ_CLANCHAT);
		kinds(null, ChatMessageType.DIALOG, ChatMessageType.MESBOX, ChatMessageType.NPC_SAY,
			ChatMessageType.SNAPSHOTFEEDBACK, ChatMessageType.UNKNOWN);
		assertNull("null is no chat", Chat.of(null));
		for (final ChatMessageType type : ChatMessageType.values())
		{
			assertTrue(type + " is never All", Chat.of(type) != Chat.ALL);
		}
	}

	@Test
	public void spokenIsTrueOnlyForAPlayersOwnWords()
	{
		final ChatMessageType[] typed = {ChatMessageType.CLAN_CHAT, ChatMessageType.CLAN_GUEST_CHAT,
			ChatMessageType.CLAN_GIM_CHAT, ChatMessageType.FRIENDSCHAT, ChatMessageType.PRIVATECHAT,
			ChatMessageType.MODPRIVATECHAT, ChatMessageType.PUBLICCHAT, ChatMessageType.MODCHAT,
			ChatMessageType.AUTOTYPER, ChatMessageType.MODAUTOTYPER};
		for (final ChatMessageType type : typed)
		{
			assertTrue(type + " is a line a player typed", Chat.spoken(type));
		}
		final ChatMessageType[] broadcast = {ChatMessageType.CLAN_MESSAGE, ChatMessageType.CLAN_GUEST_MESSAGE,
			ChatMessageType.CLAN_GIM_MESSAGE, ChatMessageType.FRIENDSCHATNOTIFICATION, ChatMessageType.PRIVATECHATOUT,
			ChatMessageType.GAMEMESSAGE, ChatMessageType.TRADEREQ};
		for (final ChatMessageType type : broadcast)
		{
			assertFalse(type + " is not a line a player typed", Chat.spoken(type));
		}
		assertFalse("null is not spoken", Chat.spoken(null));
	}

	@Test
	public void theLabelsAreTheShortWords()
	{
		assertArrayEquals("the seven tabs in the game's order", new Chat[]{Chat.ALL, Chat.GAME, Chat.PUBLIC, Chat.PRIVATE,
			Chat.CHANNEL, Chat.CLAN, Chat.TRADE}, Chat.values());
		assertEquals("All label", "All", Chat.ALL.label());
		assertEquals("Game label", "Game", Chat.GAME.label());
		assertEquals("Public label", "Public", Chat.PUBLIC.label());
		assertEquals("Private label", "Private", Chat.PRIVATE.label());
		assertEquals("Channel label", "Channel", Chat.CHANNEL.label());
		assertEquals("Clan label", "Clan", Chat.CLAN.label());
		assertEquals("Trade label", "Trade", Chat.TRADE.label());
	}
}

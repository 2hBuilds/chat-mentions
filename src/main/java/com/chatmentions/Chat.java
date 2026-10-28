package com.chatmentions;

import net.runelite.api.ChatMessageType;

/**
 * The game's chat tabs, in the order its chatbox lists them, each with its label. All is the merged view of every kept
 * line, not a kind of line: {@link #of} never answers it. Pure: it reads a {@link ChatMessageType} and nothing else of
 * the client.
 */
enum Chat
{
	ALL("All"),
	GAME("Game"),
	PUBLIC("Public"),
	PRIVATE("Private"),
	CHANNEL("Channel"),
	CLAN("Clan"),
	TRADE("Trade");

	private final String label;

	Chat(String label)
	{
		this.label = label;
	}

	/** The tab's word: "All", "Game", "Public", "Private", "Channel", "Clan" or "Trade". */
	String label()
	{
		return label;
	}

	/**
	 * The chat a kind of line belongs to, or null for every kind the plugin does not keep (and for null). The guest clan
	 * and group ironman lines fold under {@link #CLAN} as the game folds them; the friends chat is {@link #CHANNEL}. Never
	 * {@link #ALL}.
	 */
	static Chat of(ChatMessageType type)
	{
		if (type == null)
		{
			return null;
		}
		switch (type)
		{
			case PUBLICCHAT:
			case MODCHAT:
			case AUTOTYPER:
			case MODAUTOTYPER:
				return PUBLIC;
			case PRIVATECHAT:
			case PRIVATECHATOUT:
			case MODPRIVATECHAT:
				return PRIVATE;
			case FRIENDSCHAT:
			case FRIENDSCHATNOTIFICATION:
				return CHANNEL;
			case CLAN_CHAT:
			case CLAN_MESSAGE:
			case CLAN_GUEST_CHAT:
			case CLAN_GUEST_MESSAGE:
			case CLAN_GIM_CHAT:
			case CLAN_GIM_MESSAGE:
				return CLAN;
			case GAMEMESSAGE:
			case ENGINE:
			case BROADCAST:
			case WELCOME:
			case LOGINLOGOUTNOTIFICATION:
			case FRIENDNOTIFICATION:
			case IGNORENOTIFICATION:
			case ITEM_EXAMINE:
			case NPC_EXAMINE:
			case OBJECT_EXAMINE:
			case SPAM:
			case PLAYERRELATED:
			case TENSECTIMEOUT:
			case LEVELUPMESSAGE:
			case CONSOLE:
			case DIDYOUKNOW:
			case CLAN_CREATION_INVITATION:
			case CLAN_GIM_FORM_GROUP:
			case CLAN_GIM_GROUP_WITH:
				return GAME;
			case TRADEREQ:
			case TRADE:
			case TRADE_SENT:
			case CHALREQ_TRADE:
			case CHALREQ_FRIENDSCHAT:
			case CHALREQ_CLANCHAT:
				return TRADE;
			default:
				return null;
		}
	}

	/**
	 * Whether a line is one a player typed: true for the ten chat kinds, false for the game's own broadcasts, for a
	 * private message the player sent, for every other kind and for null.
	 */
	static boolean spoken(ChatMessageType type)
	{
		if (type == null)
		{
			return false;
		}
		switch (type)
		{
			case CLAN_CHAT:
			case CLAN_GUEST_CHAT:
			case CLAN_GIM_CHAT:
			case FRIENDSCHAT:
			case PRIVATECHAT:
			case MODPRIVATECHAT:
			case PUBLICCHAT:
			case MODCHAT:
			case AUTOTYPER:
			case MODAUTOTYPER:
				return true;
			default:
				return false;
		}
	}
}

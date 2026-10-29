package com.chatmentions;

import java.util.Locale;
import net.runelite.api.ChatMessageType;
import net.runelite.client.util.Text;

/**
 * The game's Public chat setting, as the chatbox shows it under the Public button. The game keeps it in the client, so
 * the label is all there is to read; {@link #keeps} then answers what the chatbox would draw. Pure: it reads a label
 * and a {@link ChatMessageType} and nothing else of the client.
 */
enum PublicSetting
{
	AUTOCHAT,
	ON,
	FRIENDS,
	OFF,
	UNKNOWN;

	/**
	 * Reads the chatbox's label (like {@code <br>On}).
	 *
	 * @param label the text of the label, tags and all, or null
	 * @return the setting, or UNKNOWN for anything not recognised
	 */
	static PublicSetting of(String label)
	{
		if (label == null)
		{
			return UNKNOWN;
		}
		switch (Text.removeTags(label).trim().toLowerCase(Locale.ROOT))
		{
			case "autochat":
				return AUTOCHAT;
			case "on":
				return ON;
			case "friends":
				return FRIENDS;
			case "off":
			case "hide":
				return OFF;
			default:
				return UNKNOWN;
		}
	}

	/**
	 * Whether the chatbox draws a line of this kind.
	 *
	 * @param fromYou whether the local player said it
	 * @param fromFriend whether a friend said it
	 */
	boolean keeps(ChatMessageType type, boolean fromYou, boolean fromFriend)
	{
		if (!isPublic(type) || fromYou)
		{
			return true;
		}
		switch (this)
		{
			case ON:
				return type != ChatMessageType.AUTOTYPER && type != ChatMessageType.MODAUTOTYPER;
			case FRIENDS:
				return fromFriend;
			case OFF:
				return false;
			default:
				return true;
		}
	}

	private static boolean isPublic(ChatMessageType type)
	{
		return type == ChatMessageType.PUBLICCHAT || type == ChatMessageType.MODCHAT
			|| type == ChatMessageType.AUTOTYPER || type == ChatMessageType.MODAUTOTYPER;
	}
}

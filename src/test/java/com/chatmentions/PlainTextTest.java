package com.chatmentions;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link PlainText}: a chat line and a sender's name as plain text. */
public class PlainTextTest
{
	@Test
	public void tagsIconsAndTheNoBreakSpaceGoAndTheEndsAreTrimmed()
	{
		assertEquals("tags and icon go, the end is trimmed", "Klar: gz 2h!",
			PlainText.plain("<col=0000a0><img=22>Klar: gz 2h!</col> "));
		assertEquals("the no-break space is a plain space", "self ctrl: hi", PlainText.plain("self\u00a0ctrl: hi"));
		assertEquals("null is empty", "", PlainText.plain(null));
		assertEquals("the task prefix and its spaces go", "Klar has completed a task",
			PlainText.plain("  CA_ID:279| Klar has completed a task"));
	}

	@Test
	public void theGamesStringVariablesAreFilledIn()
	{
		assertEquals("a defined variable is filled in", "Klar has completed a quest: King's Ransom",
			PlainText.plain("<str_quest_name_0=King's Ransom>Klar has completed a quest: <str_quest_name_0>"));
		assertEquals("an undefined variable is nothing", "a", PlainText.plain("<str_x>a"));
		assertEquals("other tags stay in expand", "<col=ff>x</col>", PlainText.expand("<col=ff>x</col>"));
		assertEquals("expand of null is empty", "", PlainText.expand(null));
	}

	@Test
	public void aSendersNameLosesItsIconAndKeepsOneSpace()
	{
		assertEquals("icon gone, no-break space a space", "Iron Pete", PlainText.name("<img=41>Iron\u00a0Pete"));
		assertEquals("runs of spaces collapse and the ends are trimmed", "Klar x", PlainText.name("  Klar   x "));
		assertEquals("null is empty", "", PlainText.name(null));
	}
}

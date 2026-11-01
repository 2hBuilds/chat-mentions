package com.chatmentions;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChipBreathTest
{
	@Test
	public void theAlphaFollowsTwoBreaths()
	{
		assertEquals(0f, ChipBreath.alpha(0), 0.001f);
		assertEquals(0.707f, ChipBreath.alpha(150), 0.01f);
		assertEquals(1f, ChipBreath.alpha(300), 0.001f);
		assertEquals(0f, ChipBreath.alpha(600), 0.001f);
		assertEquals(1f, ChipBreath.alpha(900), 0.001f);
		assertEquals(0f, ChipBreath.alpha(1200), 0.001f);
		assertEquals(0f, ChipBreath.alpha(5000), 0.001f);
	}

	@Test
	public void theAlphaStaysWithinZeroAndOne()
	{
		for (long ms = 0; ms <= 1300; ms++)
		{
			final float alpha = ChipBreath.alpha(ms);
			assertTrue("alpha at " + ms + " ms is " + alpha, alpha >= 0f && alpha <= 1f);
		}
	}
}

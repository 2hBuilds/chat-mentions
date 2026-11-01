package com.chatmentions;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;

/**
 * The breath of a keyword chip: its edge glows in the 2h gold twice and settles, the way a chip answers a word typed
 * again. It refers to no class of the plugin it sits in; the chip hands it the component to repaint and calls
 * {@link #paint} last.
 *
 * <p>Choice: the glow is a 1 px outline drawn inside the chip's own bounds, so the chip's size never changes, and the
 * word's colour is left alone. A {@link Timer} repaints the chip about every 30 ms and stops itself at the end.
 */
final class ChipBreath
{
	/** The whole breath, two rises and falls, in milliseconds. */
	static final int DURATION_MS = 1200;

	/** One rise and fall, in milliseconds. */
	private static final int BREATH_MS = DURATION_MS / 2;

	/** The pause between repaints, in milliseconds. */
	private static final int TICK_MS = 30;

	private final JComponent chip;
	private final Timer timer;
	private long started;

	/** @param chip the component that breathes and is repainted at each tick */
	ChipBreath(JComponent chip)
	{
		this.chip = chip;
		this.timer = new Timer(TICK_MS, e -> tick());
		this.timer.setRepeats(true);
	}

	/**
	 * The glow's strength {@code elapsedMillis} after the breath began: two sine-shaped breaths over
	 * {@value #DURATION_MS} ms, 0 at the start, 1 at 300 and 900, 0 at 600 and at the end and after. Always within 0..1.
	 */
	static float alpha(long elapsedMillis)
	{
		if (elapsedMillis <= 0 || elapsedMillis >= DURATION_MS)
		{
			return 0f;
		}
		final double phase = (elapsedMillis % BREATH_MS) / (double) BREATH_MS;
		final double value = Math.sin(Math.PI * phase);
		return (float) Math.max(0.0, Math.min(1.0, value));
	}

	/** Starts the breath from 0; one already running begins again. */
	void start()
	{
		started = System.currentTimeMillis();
		if (!timer.isRunning())
		{
			timer.start();
		}
		chip.repaint();
	}

	/** Whether the chip is breathing now. */
	boolean running()
	{
		return timer.isRunning();
	}

	private void tick()
	{
		if (System.currentTimeMillis() - started >= DURATION_MS)
		{
			timer.stop();
		}
		chip.repaint();
	}

	/**
	 * Draws the outline over the chip's edge at the current strength; nothing while the chip is not breathing.
	 *
	 * @param g the chip's graphics, after its own painting
	 */
	void paint(Graphics g)
	{
		if (!running())
		{
			return;
		}
		final float alpha = alpha(System.currentTimeMillis() - started);
		if (alpha <= 0f || chip.getWidth() < 2 || chip.getHeight() < 2)
		{
			return;
		}
		final Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			final Color gold = ColorScheme.BRAND_ORANGE;
			g2.setColor(new Color(gold.getRed(), gold.getGreen(), gold.getBlue(), Math.round(alpha * 255f)));
			g2.drawRect(0, 0, chip.getWidth() - 1, chip.getHeight() - 1);
		}
		finally
		{
			g2.dispose();
		}
	}
}

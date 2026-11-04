package com.chatmentions;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link WrapKit} in a scroll pane of known width: a word longer than the line breaks inside the line and never widens
 * the pane, and words that fit are never split.
 */
public class WrapKitTest
{
	private JFrame window;
	private JScrollPane scroll;

	/** A pane with the kit, tracking the viewport's width as the chat view does, holding {@code text} in a view {@code width} wide. */
	private JTextPane open(String text, int width) throws Exception
	{
		final JTextPane[] pane = new JTextPane[1];
		SwingUtilities.invokeAndWait(() ->
		{
			pane[0] = new JTextPane()
			{
				@Override
				public boolean getScrollableTracksViewportWidth()
				{
					return true;
				}
			};
			pane[0].setEditorKit(new WrapKit());
			pane[0].setEditable(false);
			pane[0].setText(text);
			scroll = new JScrollPane(pane[0], ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scroll.setPreferredSize(new Dimension(width, 300));
			window = new JFrame();
			window.setFocusableWindowState(false);
			window.getContentPane().add(scroll);
			window.pack();
			window.setVisible(true);
			window.validate();
		});
		return pane[0];
	}

	@After
	public void closeWindow() throws Exception
	{
		if (window != null)
		{
			final JFrame old = window;
			SwingUtilities.invokeAndWait(old::dispose);
			window = null;
		}
	}

	@Test
	public void aWordLongerThanTheLineBreaksInsideItAndNeverWidensThePane() throws Exception
	{
		final StringBuilder built = new StringBuilder("a ");
		for (int i = 0; i < 80; i++)
		{
			built.append('h');
		}
		built.append(" b");
		final String text = built.toString();
		final JTextPane pane = open(text, 200);
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				assertEquals("as wide as the viewport", scroll.getViewport().getWidth(), pane.getWidth());
				final FontMetrics metrics = pane.getFontMetrics(pane.getFont());
				for (int i = 0; i < text.length(); i++)
				{
					final Rectangle2D at = pane.modelToView2D(i);
					assertTrue("character " + i + " ends inside the pane",
						at.getX() + metrics.charWidth(text.charAt(i)) <= pane.getWidth() + 1);
				}
				assertTrue("taller than one line",
					pane.modelToView2D(text.length() - 1).getY() > pane.modelToView2D(0).getY());
			}
			catch (BadLocationException e)
			{
				throw new AssertionError(e);
			}
		});
	}

	@Test
	public void wordsThatFitAreNeverSplit() throws Exception
	{
		final String text = "I barely learned how to do solo 150s lmfao";
		final JTextPane pane = open(text, 120);
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				int from = 0;
				boolean wrapped = false;
				for (final String word : text.split(" "))
				{
					final int start = text.indexOf(word, from);
					final double y = pane.modelToView2D(start).getY();
					for (int i = start; i < start + word.length(); i++)
					{
						assertEquals("'" + word + "' shares one line", y, pane.modelToView2D(i).getY(), 0.5);
					}
					wrapped |= y > pane.modelToView2D(0).getY();
					from = start + word.length();
				}
				assertTrue("the text did wrap", wrapped);
			}
			catch (BadLocationException e)
			{
				throw new AssertionError(e);
			}
		});
	}
}

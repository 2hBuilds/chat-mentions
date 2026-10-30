package com.chatmentions;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import net.runelite.api.ChatMessageType;
import net.runelite.client.ui.ColorScheme;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * {@link ChatMentionsPanel} in a laid-out frame, with a settings double that records every write and a feed of its own:
 * the settings menu, the top card, the mention cards and the kept chat.
 */
public class ChatMentionsPanelTest
{
	private String keywords = "2h, klar";
	private boolean wholeWordsOnly = true;
	private boolean followChatFilter = true;
	private final Map<Chat, Boolean> watched = new EnumMap<>(Chat.class);
	private int cards = 20;
	private int lines = 3000;
	private final List<String> writes = new ArrayList<>();
	private int resets;
	private final List<String> copied = new ArrayList<>();
	private MentionFeed feed = new MentionFeed(100, 20);
	private Color keywordColour = new Color(59, 130, 246);
	private String keywordColours = "";
	private int cardsShare = 50;
	private String shownTab = "ALL";
	private final PickStub picker = new PickStub();
	private final RankIcons icons = new RankIcons();
	private ChatMentionsPanel shown;
	private JFrame window;

	{
		feed.keywords("2h", false);
		feed.playerName("Sanka");
	}

	private final PanelSettings settings = new PanelSettings()
	{
		@Override
		public String keywords()
		{
			return keywords;
		}

		@Override
		public void setKeywords(String words)
		{
			writes.add("keywords=" + words);
		}

		@Override
		public boolean followChatFilter()
		{
			return followChatFilter;
		}

		@Override
		public void setFollowChatFilter(boolean on)
		{
			writes.add("followChatFilter=" + on);
		}

		@Override
		public boolean wholeWordsOnly()
		{
			return wholeWordsOnly;
		}

		@Override
		public void setWholeWordsOnly(boolean on)
		{
			writes.add("wholeWords=" + on);
		}

		@Override
		public boolean watched(Chat chat)
		{
			return chat == Chat.ALL || watched.get(chat);
		}

		@Override
		public void setWatched(Chat chat, boolean on)
		{
			writes.add("watched " + chat + "=" + on);
		}

		@Override
		public int cardsKept()
		{
			return cards;
		}

		@Override
		public void setCardsKept(int n)
		{
			writes.add("cards=" + n);
		}

		@Override
		public int linesKept()
		{
			return lines;
		}

		@Override
		public void setLinesKept(int n)
		{
			writes.add("lines=" + n);
		}

		@Override
		public Color keywordColour()
		{
			return keywordColour;
		}

		@Override
		public void setKeywordColour(Color colour)
		{
			writes.add("defaultKeywordColour=" + colour.getRed() + "," + colour.getGreen() + "," + colour.getBlue());
		}

		@Override
		public String keywordColours()
		{
			return keywordColours;
		}

		@Override
		public void setKeywordColours(String setting)
		{
			writes.add("keywordColours=" + setting);
		}

		@Override
		public int cardsShare()
		{
			return cardsShare;
		}

		@Override
		public void setCardsShare(int percent)
		{
			writes.add("cardsShare=" + percent);
		}

		@Override
		public String shownTab()
		{
			return shownTab;
		}

		@Override
		public void setShownTab(String name)
		{
			shownTab = name;
			writes.add("shownTab=" + name);
		}
	};

	/** A colour picker that records what it was opened with and keeps the callbacks for a test to fire. */
	private static final class PickStub implements ColourPicker
	{
		private String title;
		private Color current;
		private int opened;
		private Consumer<Color> onChange;
		private Consumer<Color> onClose;
		private Component parent;
		private Point at;

		@Override
		public void pick(Component parent, String title, Color current, Consumer<Color> onChange, Consumer<Color> onClose,
			Point at)
		{
			this.parent = parent;
			this.at = at;
			this.title = title;
			this.current = current;
			this.onChange = onChange;
			this.onClose = onClose;
			opened++;
		}
	}

	private void defaults()
	{
		for (final Chat chat : Chat.values())
		{
			watched.put(chat, chat != Chat.GAME && chat != Chat.TRADE);
		}
	}

	/** Builds the panel in a laid-out frame, opens the menu and hands it to {@code body}. */
	private void withMenu(MenuBody body) throws Exception
	{
		defaults();
		onEdt(() ->
		{
			final ChatMentionsPanel panel = new ChatMentionsPanel(() -> resets++, settings, picker, feed, icons, copied::add);
			final JFrame frame = new JFrame();
			frame.setFocusableWindowState(false);
			frame.add(panel);
			frame.setBounds(100, 100, 500, 600);
			frame.setVisible(true);
			try
			{
				frame.validate();
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				assertTrue("the menu is up", menu.isVisible());
				body.run(panel, menu);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	private interface MenuBody
	{
		void run(ChatMentionsPanel panel, JPopupMenu menu);
	}

	@Test
	public void theTwoPressableRowsDarkenUnderTheMouseLikeTheMenuItems() throws Exception
	{
		withMenu((panel, menu) ->
		{
			final Component[] parts = menu.getComponents();
			for (final int index : new int[]{2, 17})
			{
				final JPanel row = (JPanel) parts[index];
				final Component caption = row.getComponent(0);
				assertFalse(row.getName() + " is clear at rest", row.isOpaque());
				row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 2, 2, 0, false));
				assertTrue(row.getName() + " darkens under the mouse", row.isOpaque());
				assertEquals(ChatMentionsPanel.menuHover(), row.getBackground());
				caption.dispatchEvent(new MouseEvent(caption, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 2, 2, 0, false));
				assertTrue(row.getName() + " stays dark while the mouse is still on it", row.isOpaque());
				row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -5, -5, 0, false));
				assertFalse(row.getName() + " clears when the mouse leaves", row.isOpaque());
			}
		});
	}

	@Test
	public void theMenuHoldsTheSwitchesTheBoxesAndTheOkRow() throws Exception
	{
		withMenu((panel, menu) ->
		{
			final Component[] parts = menu.getComponents();
			assertEquals(24, parts.length);
			assertTrue("the header", parts[0] instanceof JPanel);
			assertTrue("a rule", parts[1] instanceof JPopupMenu.Separator);
			assertEquals(ChatMentionsPanel.HOW_TO_ROW_NAME, parts[2].getName());
			assertEquals("How to use is white like every row a player can press", Color.WHITE,
				((JLabel) ((JPanel) parts[2]).getComponent(0)).getForeground());
			assertTrue("a rule", parts[3] instanceof JPopupMenu.Separator);
			assertTrue("the caption row", parts[4] instanceof JPanel);
			assertEquals("Watch", ((JLabel) ((JPanel) parts[4]).getComponent(0)).getText());

			final List<String> names = Arrays.asList("Public", "Private", "Channel", "Clan", "Game", "Trade");
			for (int i = 0; i < 6; i++)
			{
				assertTrue(parts[5 + i] instanceof JCheckBoxMenuItem);
				final JCheckBoxMenuItem item = (JCheckBoxMenuItem) parts[5 + i];
				assertEquals(names.get(i), item.getText());
				assertEquals(names.get(i) + " ticked", i < 4, item.isSelected());
			}
			assertTrue("a rule", parts[11] instanceof JPopupMenu.Separator);
			assertTrue(parts[12] instanceof JCheckBoxMenuItem);
			assertEquals("Whole words only", ((JCheckBoxMenuItem) parts[12]).getText());
			assertTrue(((JCheckBoxMenuItem) parts[12]).isSelected());
			assertEquals("Ticked: 2h matches only the word 2h. Unticked: 2h also matches 2hbuilds.", ((JCheckBoxMenuItem) parts[12]).getToolTipText());
			assertTrue(parts[13] instanceof JCheckBoxMenuItem);
			assertEquals("Block what the chatbox blocks", ((JCheckBoxMenuItem) parts[13]).getText());
			assertTrue(((JCheckBoxMenuItem) parts[13]).isSelected());
			assertEquals("Leaves out what your chatbox hides: the game's Public setting (autochat, friends, off) and RuneLite's Chat Filter plugin. Spam getting through? Add the words to Chat Filter.", ((JCheckBoxMenuItem) parts[13]).getToolTipText());
			assertTrue("a rule", parts[14] instanceof JPopupMenu.Separator);

			assertEquals("Colours", captionOf((JPanel) parts[15]));
			assertEquals(new Color(165, 165, 165), ((JLabel) ((JPanel) parts[15]).getComponent(0)).getForeground());
			final JPanel colourRow = (JPanel) parts[16];
			assertEquals(ChatMentionsPanel.KEYWORD_COLOUR_ROW_NAME, colourRow.getName());
			assertEquals("Default keyword colour", captionOf(colourRow));
			assertEquals(Color.WHITE, ((JLabel) colourRow.getComponent(0)).getForeground());
			final Component swatch = lastOf(colourRow);
			assertEquals(ChatMentionsPanel.KEYWORD_SWATCH_NAME, swatch.getName());
			assertEquals(new Color(59, 130, 246), swatch.getBackground());
			final JPanel resetRow = (JPanel) parts[17];
			assertEquals(ChatMentionsPanel.KEYWORD_CLEAR_NAME, resetRow.getName());
			assertEquals("Reset all keyword colours", captionOf(resetRow));
			assertEquals(Color.WHITE, ((JLabel) resetRow.getComponent(0)).getForeground());
			assertTrue("a rule", parts[18] instanceof JPopupMenu.Separator);
			assertEquals("Keywords", captionOf((JPanel) parts[19]));
			final Component box = lastOf((JPanel) parts[19]);
			assertTrue(box instanceof JTextField);
			assertEquals("2h, klar", ((JTextField) box).getText());

			assertEquals("Cards kept", captionOf((JPanel) parts[20]));
			final JSpinner cardsSpinner = (JSpinner) lastOf((JPanel) parts[20]);
			assertEquals(20, ((Number) cardsSpinner.getValue()).intValue());
			assertTrue(((JSpinner.DefaultEditor) cardsSpinner.getEditor()).getTextField().getColumns() <= 4);

			assertEquals("Chat lines kept", captionOf((JPanel) parts[21]));
			final JSpinner linesSpinner = (JSpinner) lastOf((JPanel) parts[21]);
			assertEquals(3000, ((Number) linesSpinner.getValue()).intValue());
			assertTrue(((JSpinner.DefaultEditor) linesSpinner.getEditor()).getTextField().getColumns() <= 5);

			assertTrue("a rule", parts[22] instanceof JPopupMenu.Separator);
			assertTrue(parts[23] instanceof JPanel);
			assertNotNull(button((JPanel) parts[23], "Reset to default"));
			assertNotNull(button((JPanel) parts[23], "OK"));
			final int swatchRight = SwingUtilities.convertPoint(swatch, swatch.getWidth(), 0, menu).x;
			final int boxRight = SwingUtilities.convertPoint(box, box.getWidth(), 0, menu).x;
			assertTrue("the swatch's right edge is at the boxes' (" + swatchRight + " vs " + boxRight + ")",
				Math.abs(swatchRight - boxRight) <= 2);
			assertTrue("opening wrote nothing", writes.isEmpty());
		});
	}

	@Test
	public void aSwitchClickWritesAndKeepsTheMenuOpen() throws Exception
	{
		withMenu((panel, menu) ->
		{
			click(check(menu, "Game"));
			assertEquals(Arrays.asList("watched GAME=true"), writes);
			assertTrue("the menu is still showing", menu.isVisible());
			click(check(menu, "Whole words only"));
			assertEquals(Arrays.asList("watched GAME=true", "wholeWords=false"), writes);
			assertTrue("the menu is still showing", menu.isVisible());
		});
	}

	@Test
	public void followChatFilterIsTickedByDefaultAndAClickWritesFalse() throws Exception
	{
		withMenu((panel, menu) ->
		{
			assertTrue(check(menu, "Block what the chatbox blocks").isSelected());
			click(check(menu, "Block what the chatbox blocks"));
			assertEquals(Arrays.asList("followChatFilter=false"), writes);
			assertTrue("the menu is still showing", menu.isVisible());
		});
	}

	@Test
	public void theBoxesWriteAsTypedAndTakeTheKeyboardOnlyWhenPressed() throws Exception
	{
		withMenu((panel, menu) ->
		{
			final Component[] parts = menu.getComponents();
			final JTextField box = (JTextField) lastOf((JPanel) parts[19]);
			final JSpinner cardsSpinner = (JSpinner) lastOf((JPanel) parts[20]);
			final JSpinner linesSpinner = (JSpinner) lastOf((JPanel) parts[21]);
			final JFormattedTextField cardsField = ((JSpinner.DefaultEditor) cardsSpinner.getEditor()).getTextField();
			final JFormattedTextField linesField = ((JSpinner.DefaultEditor) linesSpinner.getEditor()).getTextField();
			assertFalse(box.isFocusable());
			assertFalse(cardsField.isFocusable());
			assertFalse(linesField.isFocusable());

			box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
				MouseEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false, MouseEvent.BUTTON1));
			assertTrue("a left press makes it focusable", box.isFocusable());
			for (final FocusListener listener : box.getFocusListeners())
			{
				listener.focusLost(new FocusEvent(box, FocusEvent.FOCUS_LOST));
			}
			assertFalse("losing the keyboard takes it away again", box.isFocusable());

			box.setText("sanka");
			assertEquals("keywords=sanka", writes.get(writes.size() - 1));
			writes.clear();
			cardsSpinner.setValue(50);
			assertEquals(Arrays.asList("cards=50"), writes);
			writes.clear();
			linesSpinner.setValue(500);
			assertEquals(Arrays.asList("lines=500"), writes);

			menu.setVisible(false);
			writes.clear();
			keywords = "x";
			cards = 7;
			lines = 2000;
			watched.put(Chat.GAME, true);
			// Choice: the menu is shown again straight on the icon, not through the icon's press, whose guard
			// refuses a reopen this soon after a close.
			final JLabel icon = settingsIcon(panel);
			menu.show(icon, 0, icon.getHeight());
			assertTrue(menu.isVisible());
			final Component[] again = menu.getComponents();
			assertEquals("x", ((JTextField) lastOf((JPanel) again[19])).getText());
			assertEquals(7, ((Number) ((JSpinner) lastOf((JPanel) again[20])).getValue()).intValue());
			assertEquals(2000, ((Number) ((JSpinner) lastOf((JPanel) again[21])).getValue()).intValue());
			assertTrue("Game ticked", ((JCheckBoxMenuItem) again[9]).isSelected());
			assertTrue("the sync wrote nothing back", writes.isEmpty());
		});
	}

	@Test
	public void resetRunsTheRunnableAndOkCloses() throws Exception
	{
		withMenu((panel, menu) ->
		{
			final JPanel okRow = (JPanel) menu.getComponents()[23];
			button(okRow, "Reset to default").doClick(0);
			assertEquals(1, resets);
			assertTrue("the menu is still showing", menu.isVisible());
			button(okRow, "OK").doClick(0);
			assertFalse("OK took the menu down", menu.isVisible());
			writes.clear();
			panel.settingsChanged();
			assertTrue("nothing written", writes.isEmpty());
		});
	}

	/** Builds the panel in a laid-out 260 x 700 frame on the event thread, with Game and Trade not watched. */
	private void open() throws Exception
	{
		defaults();
		onEdt(() ->
		{
			shown = new ChatMentionsPanel(() -> resets++, settings, picker, feed, icons, copied::add);
			window = new JFrame();
			window.setFocusableWindowState(false);
			window.add(shown);
			window.setBounds(100, 100, 260, 700);
			window.setVisible(true);
			window.validate();
		});
	}

	@After
	public void closeWindow() throws Exception
	{
		if (window != null)
		{
			final JFrame old = window;
			onEdt(old::dispose);
			window = null;
		}
	}

	@Test
	public void theTopCardReadsMentionsTheCountClearAndThePair() throws Exception
	{
		open();
		onEdt(() ->
		{
			final JLabel title = labelSaying(shown, "Mentions");
			assertTrue("bold", title.getFont().isBold());
			assertEquals(13, title.getFont().getSize());
			final Container row = title.getParent();
			final List<Component> parts = Arrays.asList(row.getComponents());
			final JLabel count = labelSaying(row, "0 unread");
			final JLabel clear = labelSaying(row, "Clear");
			assertTrue("the count after the title", parts.indexOf(title) < parts.indexOf(count));
			assertTrue("Clear after the count", parts.indexOf(count) < parts.indexOf(clear));
			final Component last = parts.get(parts.size() - 1);
			assertTrue("the pair is last", last instanceof Container && settingsIcon((Container) last) != null);
			assertTrue("at the right", parts.indexOf(clear) < parts.size() - 1
				&& last.getX() + last.getWidth() >= row.getWidth() - 1);
		});
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz 2h", 1000);
		feed.line(ChatMessageType.CLAN_CHAT, "Iron Pete", "2h here", 2000);
		flush();
		onEdt(() -> assertNotNull(labelSaying(shown, "2 unread")));
	}

	@Test
	public void theCardsListTheLedgerNewestFirst() throws Exception
	{
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz 2h", 1000);
		feed.line(ChatMessageType.CLAN_CHAT, "Iron Pete", "2h here", 2000);
		flush();
		onEdt(() ->
		{
			final List<Container> cards = cards();
			assertEquals(2, cards.size());
			assertTrue(textsIn(cards.get(0)).containsAll(Arrays.asList("Iron Pete", "Clan", clock(2000), "2h here")));
			assertTrue(textsIn(cards.get(1)).containsAll(Arrays.asList("Klar", clock(1000), "gz 2h")));
			final JTextPane text = paneIn(cards.get(0));
			for (int offset = 0; offset < 2; offset++)
			{
				final AttributeSet keyword = attributesAt(text, offset);
				assertEquals(new Color(59, 130, 246), StyleConstants.getForeground(keyword));
				assertTrue(StyleConstants.isBold(keyword));
			}
			final AttributeSet plain = attributesAt(text, 3);
			assertEquals(Color.WHITE, StyleConstants.getForeground(plain));
			assertFalse(StyleConstants.isBold(plain));
		});
	}

	@Test
	public void theKeywordStripSitsInADarkWellApartFromTheCards() throws Exception
	{
		open();
		onEdt(() ->
		{
			final JComponent strip = (JComponent) named(shown, ChatMentionsPanel.STRIP_NAME);
			assertTrue("the strip is painted", strip.isOpaque());
			assertEquals(ChatMentionsPanel.WELL, strip.getBackground());
			final javax.swing.border.Border edge = ((javax.swing.border.CompoundBorder) strip.getBorder()).getOutsideBorder();
			assertEquals(ChatMentionsPanel.WELL_RIM, ((javax.swing.border.MatteBorder) edge).getMatteColor());
			assertEquals("a 1 px rim", 1, edge.getBorderInsets(strip).top);
			assertEquals("the rim takes nothing from the strip's room", 6, strip.getBorder().getBorderInsets(strip).top);
			final Component cards = named(shown, ChatMentionsPanel.CARDS_NAME);
			final JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, cards);
			assertFalse("the cards' scroll stays unpainted", scroll.isOpaque() || scroll.getViewport().isOpaque());
			final int cardsBottom = SwingUtilities.convertPoint(scroll, 0, scroll.getHeight(), shown).y;
			final int stripTop = SwingUtilities.convertPoint(strip, 0, 0, shown).y;
			assertTrue("the strip stands at least 6 px under the cards: " + (stripTop - cardsBottom), stripTop - cardsBottom >= 6);
		});
	}

	@Test
	public void theEmptyColumnShowsTheTip() throws Exception
	{
		open();
		onEdt(() ->
		{
			final JLabel tip = (JLabel) named(shown, ChatMentionsPanel.TIP_NAME);
			assertTrue("the tip shows", tip.isShowing());
			assertEquals("centred across", SwingConstants.CENTER, tip.getHorizontalAlignment());
			assertEquals("and down", SwingConstants.CENTER, tip.getVerticalAlignment());
			assertTrue("'Tip:' in bold", tip.getText().contains("<b>Tip:</b>"));
			assertTrue("one of the tips", Tips.ALL.contains(plain(tip.getText())));
			assertTrue("the column always shows", named(shown, ChatMentionsPanel.CARDS_NAME).isShowing());
		});
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz 2h", 1000);
		flush();
		onEdt(() ->
		{
			assertFalse(named(shown, ChatMentionsPanel.TIP_NAME).isShowing());
			assertTrue(named(shown, ChatMentionsPanel.CARDS_NAME).isShowing());
		});
		feed.ledger().clear();
		flush();
		onEdt(() -> assertTrue(named(shown, ChatMentionsPanel.TIP_NAME).isShowing()));
	}

	@Test
	public void theHowToShowsAtStartWithItsSteps() throws Exception
	{
		open();
		openGuide();
		onEdt(() ->
		{
			final Container howTo = (Container) named(shown, ChatMentionsPanel.HOW_TO_NAME);
			assertNotNull(howTo);
			assertTrue("the how-to shows", howTo.isShowing());
			final Component tip = named(shown, ChatMentionsPanel.TIP_NAME);
			assertTrue("the tip shows under it", tip.isShowing());
			final int top = labelSaying(shown, "Mentions").getLocationOnScreen().y;
			assertTrue("below the top card", howTo.getLocationOnScreen().y > top);
			assertTrue("above the tip", howTo.getLocationOnScreen().y < tip.getLocationOnScreen().y);
			final List<String> texts = textsIn(howTo);
			assertEquals(1, Collections.frequency(texts, "How to use"));
			assertEquals(1, Collections.frequency(texts, "Hide"));
			assertEquals("Hide", ((JLabel) named(howTo, ChatMentionsPanel.HOW_TO_HIDE_NAME)).getText());
			final String sentence = "Every chat line that names you or a keyword becomes a card.";
			assertEquals(1, Collections.frequency(texts, sentence));
			assertEquals(1, Collections.frequency(texts, "Catch up after being away:"));
			assertTrue("the hook is bold", labelSaying(howTo, "Catch up after being away:").getFont().isBold());
			final JLabel note = (JLabel) named(howTo, ChatMentionsPanel.HOW_TO_NOTE_NAME);
			assertNotNull("the note under the steps", note);
			assertEquals("This guide hides when your first card comes in. 'How to use' in the settings menu opens it anytime.",
				plain(note.getText()));
			assertEquals("the note is grey", ColorScheme.LIGHT_GRAY_COLOR, note.getForeground());
			assertTrue("the note is under the last step", note.getLocationOnScreen().y
				> labelSaying(howTo, "6.").getLocationOnScreen().y);
			final Container titleRow = (Container) named(howTo, ChatMentionsPanel.HOW_TO_TITLE_NAME);
			assertNotNull(titleRow);
			assertSame("Hide sits on the title row, at the card's top right",
				titleRow, named(howTo, ChatMentionsPanel.HOW_TO_HIDE_NAME).getParent());
			final JLabel mark = labelSaying(titleRow, "2h");
			final JLabel name = labelSaying(titleRow, "Chat Mentions");
			assertEquals(ColorScheme.BRAND_ORANGE, mark.getForeground());
			assertEquals(Color.WHITE, name.getForeground());
			assertTrue("2h is bold", mark.getFont().isBold());
			assertTrue("the name is bold", name.getFont().isBold());
			final JLabel intro = (JLabel) named(howTo, ChatMentionsPanel.HOW_TO_INTRO_NAME);
			assertNotNull(intro);
			assertEquals(sentence, plain(intro.getText()));
			assertEquals("the explanation is grey", ColorScheme.LIGHT_GRAY_COLOR, intro.getForeground());
			assertTrue("the title is above the sentence", titleRow.getLocationOnScreen().y < intro.getLocationOnScreen().y);
			assertTrue("the sentence is above the heading", intro.getLocationOnScreen().y < labelSaying(howTo, "How to use").getLocationOnScreen().y);
			final List<String> steps = Arrays.asList(
				"Add keywords under the cards. Your name is one.",
				"A chat line with a keyword becomes a card.",
				"Click a card to jump to its line in the chat.",
				"Click a keyword to pick its colour.",
				"Tabs: the game's chats. All shows every chat.",
				"The settings menu (the icon): chats, colours, sizes.");
			for (int i = 0; i < 6; i++)
			{
				assertEquals(steps.get(i), 1, Collections.frequency(texts, steps.get(i)));
				assertEquals((i + 1) + ".", 1, Collections.frequency(texts, (i + 1) + "."));
			}
		});
	}

	@Test
	public void aTabPressIsKeptAndLitAtTheNextStart() throws Exception
	{
		open();
		onEdt(() -> leftPress(chip("Clan")));
		assertEquals("shownTab=CLAN", writes.get(writes.size() - 1));
		closeWindow();
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "clan only", 1000);
		feed.line(ChatMessageType.FRIENDSCHAT, "Mossy Rock", "fc only", 2000);
		flush();
		shownTab = "CLAN";
		open();
		flush();
		onEdt(() ->
		{
			assertEquals(Arrays.asList("Clan"), litChips());
			assertEquals(Arrays.asList(clock(1000) + " Klar: clan only"), paragraphs(view()));
		});
		closeWindow();
		shownTab = "GAME";
		open();
		onEdt(() -> assertEquals(Arrays.asList("All"), litChips()));
		closeWindow();
		shownTab = "nonsense";
		writes.clear();
		open();
		onEdt(() -> assertEquals(Arrays.asList("All"), litChips()));
		assertTrue("nothing written at open", writes.isEmpty());
	}

	@Test
	public void aResetLightsAllAndACardClickWritesItsTab() throws Exception
	{
		shownTab = "CLAN";
		open();
		onEdt(() -> assertEquals(Arrays.asList("Clan"), litChips()));
		shownTab = "ALL";
		onEdt(() -> shown.settingsChanged());
		onEdt(() -> assertEquals(Arrays.asList("All"), litChips()));
		feed.line(ChatMessageType.PUBLICCHAT, "Klar", "hey 2h", 1000);
		flush();
		onEdt(() ->
		{
			leftPress(paneIn(cards().get(0)));
			assertEquals(Arrays.asList("Public"), litChips());
		});
		assertEquals("shownTab=PUBLIC", writes.get(writes.size() - 1));
	}

	@Test
	public void theHowToWrapsToTheCardsWidthAndNeverClips() throws Exception
	{
		open();
		openGuide();
		onEdt(() -> assertHowToFits(260));
		final int before = lastSentenceHeight;
		onEdt(() ->
		{
			window.setSize(200, 700);
			window.validate();
		});
		// The card's resize event comes from the window system after the size change; let the event queue turn a
		// few times, as the client does, before the labels are read.
		for (int pass = 0; pass < 5; pass++)
		{
			flush();
			onEdt(() -> window.validate());
		}
		onEdt(() -> assertHowToFits(200));
		assertTrue("the sentence is taller when narrower", lastSentenceHeight > before);
	}

	/** Every wrapped label of the how-to is no wider than the card's inner width, and its text names that width. */
	private void assertHowToFits(int frameWidth)
	{
		final Container howTo = (Container) named(shown, ChatMentionsPanel.HOW_TO_NAME);
		final Container card = (Container) howTo.getComponent(0);
		final int inner = card.getWidth() - card.getInsets().left - card.getInsets().right;
		assertTrue("the card has a width", inner > 0 && inner < frameWidth);
		final List<JLabel> wrapped = new ArrayList<>();
		for (final JLabel label : labelsIn(howTo))
		{
			if (label.getText() != null && label.getText().startsWith("<html>"))
			{
				wrapped.add(label);
			}
		}
		assertEquals("the sentence, the six steps and the note", 8, wrapped.size());
		final int right = card.getLocationOnScreen().x + card.getWidth() - card.getInsets().right;
		for (final JLabel label : wrapped)
		{
			assertTrue("a label fits the card: " + label.getWidth() + " > " + inner, label.getWidth() <= inner);
			// The guard against Swing's px scaling (96/72): the words as laid out ask for no more than the card gives.
			assertTrue("a label's words fold inside the card: " + label.getPreferredSize().width + " > " + inner,
				label.getPreferredSize().width <= inner);
			assertTrue("a label's right edge stays inside",
				label.getLocationOnScreen().x + label.getWidth() <= right);
			assertTrue("its text holds the width", label.getText().contains("width:"));
		}
		final JLabel sentence = wrapped.get(0);
		assertEquals(ChatMentionsPanel.HOW_TO_INTRO_NAME, sentence.getName());
		assertTrue("the sentence names the card's width", sentence.getText().contains("width:" + inner + "pt"));
		assertTrue("the sentence takes two lines or more", sentence.getHeight() > 20);
		lastSentenceHeight = sentence.getHeight();
	}

	private int lastSentenceHeight;

	/** Opens the guide with the (?) the way a player does, on the event thread, and lays the panel out. */
	private void openGuide() throws Exception
	{
		onEdt(() ->
		{
			leftPress(named(shown, ChatMentionsPanel.HELP_NAME));
			window.validate();
		});
	}

	/** True when a showing label in {@code root} says {@code text}. */
	private static boolean showsLabel(Container root, String text)
	{
		for (final Component child : root.getComponents())
		{
			if (child instanceof JLabel && text.equals(((JLabel) child).getText()) && child.isShowing())
			{
				return true;
			}
			if (child instanceof Container && showsLabel((Container) child, text))
			{
				return true;
			}
		}
		return false;
	}

	@Test
	public void theGuideStartsHiddenWithTheGoldHelpMark() throws Exception
	{
		open();
		onEdt(() ->
		{
			assertFalse(named(shown, ChatMentionsPanel.HOW_TO_NAME).isShowing());
			final JLabel help = (JLabel) named(shown, ChatMentionsPanel.HELP_NAME);
			assertTrue(help.isShowing());
			assertEquals("Help", help.getToolTipText());
			assertFalse("no Clear yet", showsLabel(labelSaying(shown, "Mentions").getParent(), "Clear"));
			assertTrue(named(shown, ChatMentionsPanel.TIP_NAME).isShowing());
		});
	}

	@Test
	public void theHelpMarkOpensAndClosesTheGuide() throws Exception
	{
		open();
		onEdt(() ->
		{
			final Component help = named(shown, ChatMentionsPanel.HELP_NAME);
			leftPress(help);
			window.validate();
			assertTrue(named(shown, ChatMentionsPanel.HOW_TO_NAME).isShowing());
			leftPress(help);
			window.validate();
			assertFalse(named(shown, ChatMentionsPanel.HOW_TO_NAME).isShowing());
			leftPress(help);
			window.validate();
			assertTrue(named(shown, ChatMentionsPanel.HOW_TO_NAME).isShowing());
			leftPress(named(shown, ChatMentionsPanel.HOW_TO_HIDE_NAME));
			assertFalse(named(shown, ChatMentionsPanel.HOW_TO_NAME).isShowing());
		});
	}

	@Test
	public void theHelpMarkIsWhiteWhileTheGuideShowsAndGoldWhileHidden() throws Exception
	{
		open();
		onEdt(() ->
		{
			final JLabel help = (JLabel) named(shown, ChatMentionsPanel.HELP_NAME);
			assertEquals("gold while hidden", ColorScheme.BRAND_ORANGE.getRGB() & 0xFFFFFF, inkOf(help));
			help.dispatchEvent(new MouseEvent(help, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 2, 2, 0, false));
			assertEquals("white under the mouse", 0xFFFFFF, inkOf(help));
			leftPress(help);
			assertEquals("gold under the mouse once the guide shows", ColorScheme.BRAND_ORANGE.getRGB() & 0xFFFFFF,
				inkOf(help));
			help.dispatchEvent(new MouseEvent(help, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 2, 2, 0, false));
			assertEquals("white while the guide shows", 0xFFFFFF, inkOf(help));
			leftPress(named(shown, ChatMentionsPanel.HOW_TO_HIDE_NAME));
			assertEquals("gold again once hidden", ColorScheme.BRAND_ORANGE.getRGB() & 0xFFFFFF, inkOf(help));
		});
	}

	/** The RGB of the first fully opaque pixel of a label's icon. */
	private static int inkOf(JLabel label)
	{
		final java.awt.image.BufferedImage img =
			(java.awt.image.BufferedImage) ((javax.swing.ImageIcon) label.getIcon()).getImage();
		for (int y = 0; y < img.getHeight(); y++)
		{
			for (int x = 0; x < img.getWidth(); x++)
			{
				if (img.getRGB(x, y) >>> 24 == 255)
				{
					return img.getRGB(x, y) & 0xFFFFFF;
				}
			}
		}
		return -1;
	}

	@Test
	public void theHelpMarkSitsBesideTheDiscordMark() throws Exception
	{
		open();
		onEdt(() ->
		{
			final Component help = named(shown, ChatMentionsPanel.HELP_NAME);
			final Component settings = settingsIcon(shown);
			final int helpRight = help.getLocationOnScreen().x + help.getWidth();
			final int settingsLeft = settings.getLocationOnScreen().x;
			assertTrue("left of the settings icon", helpRight <= settingsLeft);
			assertTrue("within 40 px: " + (settingsLeft - helpRight), settingsLeft - helpRight <= 40);
			final int helpMid = help.getLocationOnScreen().y + help.getHeight() / 2;
			final int settingsMid = settings.getLocationOnScreen().y + settings.getHeight() / 2;
			assertTrue("same band", Math.abs(helpMid - settingsMid) <= 3);
		});
	}

	@Test
	public void theFirstCardHidesTheHelpMarkAndShowsClear() throws Exception
	{
		open();
		openGuide();
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz 2h", 1000);
		flush();
		onEdt(() ->
		{
			assertFalse(named(shown, ChatMentionsPanel.HELP_NAME).isShowing());
			assertFalse(named(shown, ChatMentionsPanel.HOW_TO_NAME).isShowing());
			assertTrue(showsLabel(labelSaying(shown, "Mentions").getParent(), "Clear"));
		});
	}

	@Test
	public void theHelpMarkComesBackWhenTheCardsAreCleared() throws Exception
	{
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz 2h", 1000);
		flush();
		feed.ledger().clear();
		flush();
		onEdt(() ->
		{
			assertEquals(0, cards().size());
			assertTrue(named(shown, ChatMentionsPanel.HELP_NAME).isShowing());
			assertFalse(showsLabel(labelSaying(shown, "Mentions").getParent(), "Clear"));
		});
	}

	@Test
	public void clearShowsOnlyWhenTheSelectedTabHasCards() throws Exception
	{
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz 2h", 1000);
		flush();
		onEdt(() -> leftPress(chip("Public")));
		flush();
		onEdt(() ->
		{
			assertFalse(showsLabel(labelSaying(shown, "Mentions").getParent(), "Clear"));
			assertTrue("the (?) in its place", named(shown, ChatMentionsPanel.HELP_NAME).isShowing());
		});
		onEdt(() -> leftPress(chip("Clan")));
		flush();
		onEdt(() ->
		{
			assertTrue(showsLabel(labelSaying(shown, "Mentions").getParent(), "Clear"));
			assertFalse(named(shown, ChatMentionsPanel.HELP_NAME).isShowing());
		});
	}

	@Test
	public void theHideLinkIsGreyLikeTheOtherLinks() throws Exception
	{
		open();
		openGuide();
		onEdt(() -> assertEquals(ColorScheme.LIGHT_GRAY_COLOR,
			named(shown, ChatMentionsPanel.HOW_TO_HIDE_NAME).getForeground()));
	}

	@Test
	public void theFirstCardHidesTheHowTo() throws Exception
	{
		open();
		openGuide();
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz 2h", 1000);
		flush();
		onEdt(() ->
		{
			assertFalse(named(shown, ChatMentionsPanel.HOW_TO_NAME).isShowing());
			assertTrue(named(shown, ChatMentionsPanel.CARDS_NAME).isShowing());
		});
		feed.ledger().clear();
		flush();
		onEdt(() ->
		{
			assertFalse(named(shown, ChatMentionsPanel.HOW_TO_NAME).isShowing());
			assertTrue(named(shown, ChatMentionsPanel.TIP_NAME).isShowing());
		});
	}

	@Test
	public void hideHidesAndTheMenuRowShowsAgain() throws Exception
	{
		open();
		openGuide();
		onEdt(() ->
		{
			assertTrue(named(shown, ChatMentionsPanel.HOW_TO_NAME).isShowing());
			leftPress(named(shown, ChatMentionsPanel.HOW_TO_HIDE_NAME));
			assertFalse(named(shown, ChatMentionsPanel.HOW_TO_NAME).isShowing());
		});
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz 2h", 1000);
		feed.line(ChatMessageType.CLAN_CHAT, "Pete", "2h here", 2000);
		flush();
		onEdt(() ->
		{
			assertEquals(2, cards().size());
			press(settingsIcon(shown));
			final JPopupMenu menu = popupMenu(window);
			assertNotNull("the menu is on screen", menu);
			final Component[] parts = menu.getComponents();
			assertTrue("the header", parts[0] instanceof JPanel);
			assertTrue("a rule", parts[1] instanceof JPopupMenu.Separator);
			assertEquals(ChatMentionsPanel.HOW_TO_ROW_NAME, parts[2].getName());
			assertEquals("How to use", captionOf((JPanel) parts[2]));
			assertEquals("Shows the steps above the cards.", ((JPanel) parts[2]).getToolTipText());
			assertTrue("a rule after it", parts[3] instanceof JPopupMenu.Separator);
			leftPress(parts[2]);
			assertFalse("the menu is down", menu.isVisible());
			final Component howTo = named(shown, ChatMentionsPanel.HOW_TO_NAME);
			assertTrue("the how-to shows again", howTo.isShowing());
			window.validate();
			assertTrue("above the cards",
				howTo.getLocationOnScreen().y < cards().get(0).getLocationOnScreen().y);
			leftPress(named(shown, ChatMentionsPanel.HOW_TO_HIDE_NAME));
			assertFalse(howTo.isShowing());
		});
	}

	@Test
	public void theTipIsDrawnEachTimeTheHalfEmpties() throws Exception
	{
		open();
		final Set<String> read = new HashSet<>();
		final String[] text = new String[1];
		onEdt(() -> text[0] = plain(((JLabel) named(shown, ChatMentionsPanel.TIP_NAME)).getText()));
		assertTrue(Tips.ALL.contains(text[0]));
		read.add(text[0]);
		for (int i = 0; i < 30; i++)
		{
			feed.line(ChatMessageType.CLAN_CHAT, "Klar", "gz 2h", 1000 + i);
			flush();
			feed.ledger().clear();
			flush();
			onEdt(() -> text[0] = plain(((JLabel) named(shown, ChatMentionsPanel.TIP_NAME)).getText()));
			assertTrue(text[0], Tips.ALL.contains(text[0]));
			read.add(text[0]);
		}
		assertTrue("more than one tip was drawn: " + read, read.size() >= 2);
	}

	@Test
	public void markReadRemovesTheCardAndClearEmptiesTheColumn() throws Exception
	{
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "A", "2h", 1);
		feed.line(ChatMessageType.CLAN_CHAT, "B", "2h", 2);
		feed.line(ChatMessageType.CLAN_CHAT, "C", "2h", 3);
		flush();
		onEdt(() ->
		{
			final List<Container> cards = cards();
			assertEquals(3, cards.size());
			leftPress(labelSaying(cards.get(1), "Mark read"));
		});
		assertEquals(Arrays.asList("C", "A"), whos());
		flush();
		onEdt(() ->
		{
			assertEquals(2, cards().size());
			leftPress(labelSaying(shown, "Clear"));
		});
		assertTrue(feed.ledger().mentions().isEmpty());
		flush();
		onEdt(() ->
		{
			assertEquals(0, cards().size());
			assertNotNull(labelSaying(shown, "0 unread"));
		});
	}

	@Test
	public void copyNameHandsTheSenderToTheCopier() throws Exception
	{
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "<img=41>Klar", "gz 2h", 1000);
		flush();
		onEdt(() -> leftPress(labelSaying(cards().get(0), "Copy name")));
		assertEquals(Arrays.asList("Klar"), copied);
		assertEquals(1, feed.ledger().mentions().size());
	}

	@Test
	public void theChipsAreTheWatchedChatsAndTheFirstIsLit() throws Exception
	{
		open();
		onEdt(() ->
		{
			assertEquals(Arrays.asList("All", "Public", "Private", "Channel", "Clan"), chipWords());
			assertEquals(Arrays.asList("All"), litChips());
		});
		watched.put(Chat.GAME, true);
		watched.put(Chat.PUBLIC, false);
		onEdt(() ->
		{
			shown.settingsChanged();
			assertEquals(Arrays.asList("All", "Game", "Private", "Channel", "Clan"), chipWords());
			assertEquals(Arrays.asList("All"), litChips());
			leftPress(chip("Clan"));
			assertEquals(Arrays.asList("Clan"), litChips());
		});
		watched.put(Chat.CLAN, false);
		onEdt(() ->
		{
			shown.settingsChanged();
			assertEquals(Arrays.asList("All"), litChips());
		});
	}

	@Test
	public void theKeptChatShowsItsLinesOldestFirst() throws Exception
	{
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "hello 2h", 1000);
		feed.line(ChatMessageType.CLAN_MESSAGE, "", "Klar has completed a quest", 2000);
		feed.line(ChatMessageType.CLAN_CHAT, "Sanka", "hi", 3000);
		feed.line(ChatMessageType.FRIENDSCHAT, "Mossy Rock", "fc here", 4000);
		flush();
		onEdt(() ->
		{
			leftPress(chip("Clan"));
			final JTextPane view = view();
			assertEquals(Arrays.asList(clock(1000) + " Klar: hello 2h", clock(2000) + " Klar has completed a quest",
				clock(3000) + " Sanka: hi"), paragraphs(view));
			final Element first = paragraph(view, 0);
			final String firstText = textOf(view, first);
			final AttributeSet who = attributesAt(view, first.getStartOffset() + firstText.indexOf("Klar"));
			assertTrue(StyleConstants.isBold(who));
			assertEquals(Color.WHITE, StyleConstants.getForeground(who));
			assertEquals(new Color(59, 130, 246), StyleConstants.getForeground(
				attributesAt(view, first.getStartOffset() + firstText.indexOf("2h"))));
			final Element second = paragraph(view, 1);
			assertEquals(ColorScheme.LIGHT_GRAY_COLOR, StyleConstants.getForeground(
				attributesAt(view, second.getStartOffset() + textOf(view, second).indexOf("Klar has"))));
			leftPress(chip("Channel"));
			assertEquals(Arrays.asList(clock(4000) + " Mossy Rock: fc here"), paragraphs(view()));
		});
	}

	@Test
	public void aNewLineAppendsAndTheRingTrims() throws Exception
	{
		open();
		feed.linesKept(3);
		for (int i = 1; i <= 5; i++)
		{
			feed.line(ChatMessageType.CLAN_CHAT, "Klar", "t" + i, i);
			flush();
		}
		onEdt(() ->
		{
			leftPress(chip("Clan"));
			final List<String> paragraphs = paragraphs(view());
			assertEquals(Arrays.asList(clock(3) + " Klar: t3", clock(4) + " Klar: t4", clock(5) + " Klar: t5"), paragraphs);
			for (final String paragraph : paragraphs)
			{
				assertFalse(paragraph.endsWith("t1") || paragraph.endsWith("t2"));
			}
		});
		feed.clear();
		flush();
		onEdt(() -> assertEquals(0, view().getDocument().getLength()));
	}

	@Test
	public void aCardClickLightsTheChipAndScrollsToTheLine() throws Exception
	{
		open();
		for (int i = 1; i <= 60; i++)
		{
			feed.line(ChatMessageType.CLAN_CHAT, "Klar", i == 30 ? "2h" : "line " + i, i * 1000L);
		}
		feed.line(ChatMessageType.FRIENDSCHAT, "Mossy Rock", "fc here", 70_000L);
		flush();
		onEdt(() ->
		{
			leftPress(chip("Channel"));
			leftPress(paneIn(cards().get(0)));
			assertEquals(Arrays.asList("Clan"), litChips());
			final JTextPane view = view();
			assertTrue("the line is in the view", inView(view, paragraph(view, 29)));
			assertTrue("the line is marked", marked(view, paragraph(view, 29)));
			assertFalse("the first is not", marked(view, paragraph(view, 0)));
		});
		feed.linesKept(10);
		flush();
		onEdt(() ->
		{
			leftPress(paneIn(cards().get(0)));
			final JTextPane view = view();
			final JViewport viewport = (JViewport) view.getParent();
			assertEquals(0, viewport.getViewPosition().y);
			final int count = view.getDocument().getDefaultRootElement().getElementCount();
			for (int i = 0; i < count; i++)
			{
				assertFalse("no paragraph is marked", marked(view, paragraph(view, i)));
			}
		});
	}

	@Test
	public void theViewFollowsNewLinesWhileAtTheBottom() throws Exception
	{
		open();
		for (int i = 1; i <= 80; i++)
		{
			feed.line(ChatMessageType.CLAN_CHAT, "Klar", i == 10 ? "2h" : "line " + i, i * 1000L);
		}
		flush();
		onEdt(() -> assertTrue("it followed", inView(view(), lastParagraph(view()))));
		onEdt(() ->
		{
			leftPress(paneIn(cards().get(0)));
			assertTrue(inView(view(), paragraph(view(), 9)));
			assertFalse(inView(view(), lastParagraph(view())));
		});
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "line 81", 81_000L);
		flush();
		onEdt(() ->
		{
			assertTrue("it stayed", inView(view(), paragraph(view(), 9)));
			assertFalse(inView(view(), lastParagraph(view())));
			final JViewport viewport = (JViewport) view().getParent();
			viewport.setViewPosition(new Point(0, viewport.getViewSize().height - viewport.getExtentSize().height));
		});
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "line 82", 82_000L);
		flush();
		onEdt(() -> assertTrue("it follows again", inView(view(), lastParagraph(view()))));
	}

	@Test
	public void theStripShowsTheNameAndTheKeywordsWithACrossEach() throws Exception
	{
		open();
		onEdt(() ->
		{
			final Container row = (Container) named(shown, ChatMentionsPanel.KEYWORD_CHIPS_NAME);
			final Component[] parts = row.getComponents();
			assertEquals(3, parts.length);
			assertEquals(ChatMentionsPanel.KEYWORD_NAME_NAME, parts[0].getName());
			assertEquals("Sanka", ((JLabel) parts[0]).getText());
			assertEquals(new Color(59, 130, 246), parts[0].getForeground());
			assertEquals(ChatMentionsPanel.KEYWORD_CHIP_NAME, parts[1].getName());
			assertEquals(ChatMentionsPanel.KEYWORD_CHIP_NAME, parts[2].getName());
			assertEquals(Arrays.asList("2h", "×"), textsIn((Container) parts[1]));
			assertEquals(Arrays.asList("klar", "×"), textsIn((Container) parts[2]));
			assertEquals("a keyword without its own colour is in the default colour", new Color(59, 130, 246),
				((Container) parts[1]).getComponent(1).getForeground());
			assertNotNull(named((Container) parts[1], ChatMentionsPanel.KEYWORD_REMOVE_NAME));
			assertNotNull(named((Container) parts[2], ChatMentionsPanel.KEYWORD_REMOVE_NAME));

			keywords = "bond";
			shown.settingsChanged();
			assertEquals(2, row.getComponentCount());
			assertEquals(Arrays.asList("bond", "×"), textsIn((Container) row.getComponent(1)));

			feed.playerName(null);
			shown.settingsChanged();
			assertEquals("Your name", ((JLabel) row.getComponent(0)).getText());

			keywordColour = new Color(255, 0, 0);
			shown.settingsChanged();
			assertEquals(new Color(255, 0, 0), row.getComponent(0).getForeground());
			assertEquals("the keyword chip follows the default colour", new Color(255, 0, 0),
				((Container) row.getComponent(1)).getComponent(1).getForeground());
		});
	}

	@Test
	public void aClickOnTheNameChipPicksTheDefaultColour() throws Exception
	{
		open();
		onEdt(() ->
		{
			final JLabel name = (JLabel) named(shown, ChatMentionsPanel.KEYWORD_NAME_NAME);
			click(name, 1);
			assertEquals(1, picker.opened);
			assertEquals("Default keyword colour", picker.title);
			assertEquals(new Color(59, 130, 246), picker.current);
			assertSame(shown, picker.parent);
			assertTrue("at inside the chip", new Rectangle(name.getLocationOnScreen(), name.getSize()).contains(picker.at));

			click(name, 1);
			assertEquals("a second click while the picker is open opens nothing", 1, picker.opened);
			picker.onChange.accept(new Color(0, 255, 0));
			assertEquals("defaultKeywordColour=0,255,0", writes.get(writes.size() - 1));
			click(name, 1);
			assertEquals("a change does not close the picker", 1, picker.opened);

			picker.onClose.accept(new Color(1, 2, 3));
			assertEquals("defaultKeywordColour=1,2,3", writes.get(writes.size() - 1));
			click(name, 1);
			assertEquals(2, picker.opened);
		});
	}

	@Test
	public void aCrossWritesTheSettingWithoutThatWord() throws Exception
	{
		open();
		onEdt(() ->
		{
			final Container row = (Container) named(shown, ChatMentionsPanel.KEYWORD_CHIPS_NAME);
			leftPress(named((Container) row.getComponent(1), ChatMentionsPanel.KEYWORD_REMOVE_NAME));
			assertEquals("keywords=klar", writes.get(writes.size() - 1));
			leftPress(named((Container) row.getComponent(2), ChatMentionsPanel.KEYWORD_REMOVE_NAME));
			assertEquals("keywords=2h", writes.get(writes.size() - 1));
			assertEquals(2, writes.size());
		});
	}

	@Test
	public void theFieldAddsOnEnterAndOnPlus() throws Exception
	{
		open();
		onEdt(() ->
		{
			final JTextField field = (JTextField) named(shown, ChatMentionsPanel.KEYWORD_FIELD_NAME);
			field.setText("bond, Split ,KLAR");
			field.postActionEvent();
			assertEquals(Arrays.asList("keywords=2h, klar, bond, Split"), writes);
			assertEquals("", field.getText());

			field.setText("klar");
			field.postActionEvent();
			assertEquals(1, writes.size());
			assertEquals("", field.getText());

			field.setText("yama");
			leftPress(named(shown, ChatMentionsPanel.KEYWORD_ADD_NAME));
			assertEquals("keywords=2h, klar, yama", writes.get(writes.size() - 1));
			assertEquals(2, writes.size());
			assertEquals("", field.getText());

			field.setText("  ");
			field.postActionEvent();
			assertEquals(2, writes.size());
		});
	}

	@Test
	public void fiveTabsSitOnOneLineAndSevenWrap() throws Exception
	{
		open();
		onEdt(() ->
		{
			assertEquals(Arrays.asList("All", "Public", "Private", "Channel", "Clan"), chipWords());
			final List<JToggleButton> tabs = chips();
			for (final JToggleButton tab : tabs)
			{
				assertEquals(tab.getText() + " is on the first tab's line", tabs.get(0).getY(), tab.getY());
			}
			final int height = named(shown, ChatMentionsPanel.CHIPS_NAME).getHeight();
			assertTrue("the row is under 30 px: " + height, height < 30);
			assertEquals(Arrays.asList("All"), litChips());
		});
		for (final Chat chat : Chat.values())
		{
			watched.put(chat, true);
		}
		onEdt(() -> shown.settingsChanged());
		flush();
		onEdt(() ->
		{
			window.validate();
			assertEquals(Arrays.asList("All", "Game", "Public", "Private", "Channel", "Clan", "Trade"), chipWords());
			assertTrue("Trade wraps under All", chip("Trade").getY() > chip("All").getY());
			final int height = named(shown, ChatMentionsPanel.CHIPS_NAME).getHeight();
			assertTrue("the row is at most 50 px: " + height, height <= 50);
		});
	}

	@Test
	public void theIconShowsBeforeTheNameInTheKeptChatAndOnTheCard() throws Exception
	{
		icons.capture(41, RankIconsTest.six(42));
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "<img=41>Klar", "hello 2h", 1000);
		feed.line(ChatMessageType.CLAN_CHAT, "Mossy Rock", "2h too", 2000);
		flush();
		onEdt(() ->
		{
			leftPress(chip("Clan"));
			final StyledDocument doc = view().getStyledDocument();
			final Element root = doc.getDefaultRootElement();
			final int after1 = root.getElement(0).getStartOffset() + (clock(1000) + " ").length();
			assertNotNull("an icon after the clock", StyleConstants.getIcon(doc.getCharacterElement(after1).getAttributes()));
			assertEquals(clock(1000) + " Klar: hello 2h", textOf(view(), root.getElement(0)).replaceAll(" +", " "));

			final int after2 = root.getElement(1).getStartOffset() + (clock(2000) + " ").length();
			assertEquals("M", textOf(view(), root.getElement(1)).substring((clock(2000) + " ").length(),
				(clock(2000) + " ").length() + 1));
			assertFalse("no icon", StyleConstants.getIcon(doc.getCharacterElement(after2).getAttributes()) != null);

			final List<Container> cards = cards();
			assertEquals(2, cards.size());
			for (final JLabel label : labelsIn(cards.get(0)))
			{
				assertEquals("no icon on the Mossy Rock card", null, label.getIcon());
			}
			final List<JLabel> top = labelsIn(cards.get(1));
			int iconAt = -1;
			int whoAt = -1;
			for (int i = 0; i < top.size(); i++)
			{
				if (top.get(i).getIcon() != null && iconAt < 0)
				{
					iconAt = i;
				}
				if ("Klar".equals(top.get(i).getText()))
				{
					whoAt = i;
				}
			}
			assertTrue("an icon label before the name", iconAt >= 0 && whoAt > iconAt);
		});
	}

	@Test
	public void theAllTabShowsEveryChatWithItsWord() throws Exception
	{
		open();
		onEdt(() -> assertEquals(Arrays.asList("All"), litChips()));
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "hello 2h", 1000);
		feed.line(ChatMessageType.PUBLICCHAT, "Mossy Rock", "selling lobs", 2000);
		feed.line(ChatMessageType.FRIENDSCHAT, "Iron Pete", "fc here", 3000);
		feed.line(ChatMessageType.CLAN_MESSAGE, "", "Klar has completed a quest", 4000);
		flush();
		onEdt(() ->
		{
			final JTextPane view = view();
			final List<String> all = new ArrayList<>();
			for (final String paragraph : paragraphs(view))
			{
				all.add(paragraph.replaceAll(" +", " "));
			}
			assertEquals(Arrays.asList(clock(1000) + " Clan Klar: hello 2h", clock(2000) + " Public Mossy Rock: selling lobs",
				clock(3000) + " Channel Iron Pete: fc here", clock(4000) + " Clan Klar has completed a quest"), all);
			final Element first = paragraph(view, 0);
			final String firstText = textOf(view, first);
			final AttributeSet word = attributesAt(view, first.getStartOffset() + firstText.indexOf("Clan"));
			assertEquals(new Color(130, 130, 130), StyleConstants.getForeground(word));
			final AttributeSet who = attributesAt(view, first.getStartOffset() + firstText.indexOf("Klar"));
			assertTrue(StyleConstants.isBold(who));
			assertEquals(Color.WHITE, StyleConstants.getForeground(who));

			leftPress(chip("Clan"));
			assertEquals(Arrays.asList(clock(1000) + " Klar: hello 2h", clock(4000) + " Klar has completed a quest"),
				paragraphs(view()));
		});
	}

	@Test
	public void aCardFromAGuestLineReadsClan() throws Exception
	{
		open();
		feed.line(ChatMessageType.CLAN_GUEST_CHAT, "Klar", "gz 2h", 1000);
		flush();
		onEdt(() ->
		{
			final List<Container> cards = cards();
			assertEquals(1, cards.size());
			assertTrue(textsIn(cards.get(0)).containsAll(Arrays.asList("Klar", "Clan")));
			assertEquals(Arrays.asList("All"), litChips());
			leftPress(paneIn(cards.get(0)));
			assertEquals(Arrays.asList("Clan"), litChips());
		});
	}

	@Test
	public void aThousandCardsAreKeptAndTheFirstIsTheNewest() throws Exception
	{
		feed = new MentionFeed(2000, 1000);
		feed.keywords("2h", false);
		feed.playerName("Sanka");
		open();
		for (int i = 1; i <= 1000; i++)
		{
			feed.line(ChatMessageType.CLAN_CHAT, "Klar", "2h " + i, i);
		}
		flush();
		onEdt(() ->
		{
			final List<Container> cards = cards();
			assertEquals(1000, cards.size());
			assertNotNull(labelSaying(shown, "1000 unread"));
			assertTrue(textsIn(cards.get(0)).contains("2h 1000"));
			assertTrue(textsIn(cards.get(999)).contains("2h 1"));
		});
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "2h 1001", 1001);
		flush();
		onEdt(() ->
		{
			final List<Container> cards = cards();
			assertEquals(1000, cards.size());
			assertTrue(textsIn(cards.get(0)).contains("2h 1001"));
			for (final Container card : cards)
			{
				assertFalse("no card reads 2h 1", textsIn(card).contains("2h 1"));
			}
		});
	}

	@Test
	public void theSwatchOpensThePickerAndAPickWrites() throws Exception
	{
		withMenu((panel, menu) ->
		{
			final Component[] parts = menu.getComponents();
			assertEquals("Whole words only", ((JCheckBoxMenuItem) parts[12]).getText());
			assertEquals("Default keyword colour", captionOf((JPanel) parts[16]));
			assertEquals("Keywords", captionOf((JPanel) parts[19]));
			final Component swatch = named((JPanel) parts[16], ChatMentionsPanel.KEYWORD_SWATCH_NAME);
			assertTrue(swatch instanceof JLabel);
			assertEquals(ChatMentionsPanel.KEYWORD_SWATCH_NAME, swatch.getName());
			assertEquals(new Color(59, 130, 246), swatch.getBackground());

			final Point swatchAt = swatch.getLocationOnScreen();
			leftPress(swatch);
			assertEquals(1, picker.opened);
			assertSame(panel, picker.parent);
			assertTrue("the point is inside the swatch as it was at the press",
				new Rectangle(swatchAt, swatch.getSize()).contains(picker.at));
			assertEquals("Default keyword colour", picker.title);
			assertEquals(new Color(59, 130, 246), picker.current);

			picker.onChange.accept(new Color(255, 0, 0));
			assertEquals(Arrays.asList("defaultKeywordColour=255,0,0"), writes);
			picker.onClose.accept(new Color(0, 255, 0));
			assertEquals(Arrays.asList("defaultKeywordColour=255,0,0", "defaultKeywordColour=0,255,0"), writes);

			keywordColour = new Color(255, 0, 0);
			panel.settingsChanged();
			assertEquals(new Color(255, 0, 0), swatch.getBackground());
		});
	}

	@Test
	public void aNewColourRecoloursTheKeywordsEverywhere() throws Exception
	{
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "hello 2h", 1000);
		flush();
		final Color blue = new Color(59, 130, 246);
		final Color red = new Color(255, 0, 0);
		onEdt(() ->
		{
			leftPress(chip("Clan"));
			assertEquals(blue, keywordColourIn(view(), 0));
		});
		keywordColour = red;
		onEdt(() ->
		{
			shown.settingsChanged();
			final JTextPane view = view();
			final Element line = paragraph(view, 0);
			final String text = textOf(view, line);
			final AttributeSet keyword = attributesAt(view, line.getStartOffset() + text.indexOf("2h"));
			assertEquals(red, StyleConstants.getForeground(keyword));
			assertTrue(StyleConstants.isBold(keyword));
			assertEquals(Color.WHITE, StyleConstants.getForeground(attributesAt(view, line.getStartOffset() + text.indexOf("hello"))));
			leftPress(chip("All"));
			assertEquals(red, keywordColourIn(view(), 0));
			assertEquals(red, StyleConstants.getForeground(attributesAt(paneIn(cards().get(0)), 6)));
		});
		feed.line(ChatMessageType.CLAN_CHAT, "Klar", "2h again", 2000);
		flush();
		onEdt(() ->
		{
			assertEquals(red, keywordColourIn(view(), 1));
			leftPress(chip("Clan"));
			assertEquals(red, keywordColourIn(view(), 1));
		});
	}

	/** A click (a MOUSE_CLICKED with {@code count}, left button) on {@code target}. */
	private static void click(Component target, int count)
	{
		target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 3, 3, count,
			false, MouseEvent.BUTTON1));
	}

	private Container keywordChip(String word)
	{
		final Container row = (Container) named(shown, ChatMentionsPanel.KEYWORD_CHIPS_NAME);
		for (final Component part : row.getComponents())
		{
			if (ChatMentionsPanel.KEYWORD_CHIP_NAME.equals(part.getName()) && word.equals(textsIn((Container) part).get(0)))
			{
				return (Container) part;
			}
		}
		throw new AssertionError("no chip for " + word);
	}

	@Test
	public void aChipShowsItsColourAndAClickOpensThePicker() throws Exception
	{
		keywordColours = "klar=ff0000";
		open();
		onEdt(() ->
		{
			final Container klar = keywordChip("klar");
			final JLabel klarWord = labelSaying(klar, "klar");
			assertEquals(new Color(255, 0, 0), klarWord.getForeground());
			assertEquals("a chip without its own colour is in the default colour", new Color(59, 130, 246),
				labelSaying(keywordChip("2h"), "2h").getForeground());

			click(klarWord, 1);
			assertEquals(1, picker.opened);
			assertEquals("klar", picker.title);
			assertEquals(new Color(255, 0, 0), picker.current);
			assertTrue("at inside the chip", new Rectangle(klar.getLocationOnScreen(), klar.getSize()).contains(picker.at));
			click(klarWord, 1);
			assertEquals("a second click while the picker is open opens nothing", 1, picker.opened);
			picker.onChange.accept(new Color(0, 255, 0));
			assertEquals("keywordColours=klar=00ff00", writes.get(writes.size() - 1));
			click(klarWord, 1);
			assertEquals("a change does not close the picker", 1, picker.opened);

			picker.onClose.accept(new Color(0, 255, 0));
			assertEquals("keywordColours=klar=00ff00", writes.get(writes.size() - 1));
			click(keywordChip("2h"), 1);
			assertEquals(2, picker.opened);
			assertEquals("2h", picker.title);
			assertEquals(new Color(59, 130, 246), picker.current);
			picker.onClose.accept(new Color(1, 2, 3));
			assertEquals("keywordColours=klar=ff0000, 2h=010203", writes.get(writes.size() - 1));
		});
	}

	@Test
	public void twoWordsTwoColoursAndTheFallbackNeverOverwrites() throws Exception
	{
		keywordColours = "klar=ff0000";
		feed.keywords("2h, klar", false);
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "Mossy Rock", "klar and 2h", 1000);
		flush();
		final Color red = new Color(255, 0, 0);
		final Color blue = new Color(59, 130, 246);
		final Color green = new Color(0, 255, 0);
		onEdt(() ->
		{
			leftPress(chip("Clan"));
			checkLine(red, blue);
		});
		keywordColour = green;
		onEdt(() ->
		{
			shown.settingsChanged();
			checkLine(red, green);
		});
		keywordColours = "";
		onEdt(() ->
		{
			shown.settingsChanged();
			checkLine(green, green);
		});
	}

	/** The colours of "klar" and of "2h" in the view's first paragraph and in the one card. */
	private void checkLine(Color klar, Color twoH)
	{
		final JTextPane view = view();
		final Element line = paragraph(view, 0);
		final int at = line.getStartOffset() + textOf(view, line).indexOf("klar");
		final int other = line.getStartOffset() + textOf(view, line).indexOf("2h");
		for (int i = 0; i < 4; i++)
		{
			assertEquals(klar, StyleConstants.getForeground(attributesAt(view, at + i)));
		}
		for (int i = 0; i < 2; i++)
		{
			assertEquals(twoH, StyleConstants.getForeground(attributesAt(view, other + i)));
		}
		final JTextPane card = paneIn(cards().get(0));
		assertEquals(klar, StyleConstants.getForeground(attributesAt(card, 0)));
		assertEquals(twoH, StyleConstants.getForeground(attributesAt(card, 9)));
	}

	@Test
	public void theResetRowAndTheCrossForgetColours() throws Exception
	{
		keywordColours = "klar=ff0000, 2h=00ff00";
		withMenu((panel, menu) ->
		{
			final JPanel row = (JPanel) menu.getComponents()[17];
			assertEquals(ChatMentionsPanel.KEYWORD_CLEAR_NAME, row.getName());
			assertEquals("Reset all keyword colours", captionOf(row));
			leftPress(row);
			assertEquals(Arrays.asList("keywordColours="), writes);
			assertTrue("the menu stays open", menu.isVisible());
		});
		writes.clear();
		keywordColours = "klar=ff0000";
		open();
		onEdt(() ->
		{
			leftPress(named(keywordChip("klar"), ChatMentionsPanel.KEYWORD_REMOVE_NAME));
			assertEquals(Arrays.asList("keywords=2h", "keywordColours="), writes);
			writes.clear();
			leftPress(named(keywordChip("2h"), ChatMentionsPanel.KEYWORD_REMOVE_NAME));
			assertEquals(Arrays.asList("keywords=klar"), writes);
		});
	}

	@Test
	public void theDividerFollowsTheShareAndADragWritesIt() throws Exception
	{
		open();
		onEdt(() ->
		{
			final JSplitPane split = splitIn(shown);
			final int room = split.getHeight() - split.getDividerSize();
			assertTrue(Math.abs(split.getDividerLocation() - room / 2.0) <= 2);

			cardsShare = 30;
			shown.settingsChanged();
			window.validate();
			assertTrue(Math.abs(split.getDividerLocation() - room * 0.3) <= 2);

			split.setDividerLocation(0.7);
			window.validate();
			final Component divider = ((BasicSplitPaneUI) split.getUI()).getDivider();
			release(divider);
			assertEquals("cardsShare=70", writes.get(writes.size() - 1));
			final int count = writes.size();
			release(divider);
			assertEquals(count, writes.size());
		});
	}

	@Test
	public void theHalfScrollsOverTheHowToOnAShortWindow() throws Exception
	{
		open();
		openGuide();
		onEdt(() ->
		{
			window.setSize(260, 400);
			window.validate();
		});
		flush();
		onEdt(() ->
		{
			cardsShare = 40;
			shown.settingsChanged();
			window.validate();
		});
		flush();
		onEdt(() ->
		{
			final JSplitPane split = splitIn(shown);
			final int room = split.getHeight() - split.getDividerSize();
			assertTrue("the divider is at 40 %: " + split.getDividerLocation() + " of " + room,
				Math.abs(split.getDividerLocation() - room * 0.4) <= 2);
			assertTrue("the how-to shows", named(shown, ChatMentionsPanel.HOW_TO_NAME).isShowing());
			final JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class,
				named(shown, ChatMentionsPanel.CARDS_NAME));
			assertNotNull(scroll);
			final JViewport viewport = scroll.getViewport();
			assertTrue("the view is taller than the viewport",
				viewport.getView().getHeight() > viewport.getHeight());
			assertTrue("the bar shows", scroll.getVerticalScrollBar().isVisible());
			viewport.setViewPosition(new Point(0, viewport.getView().getHeight() - viewport.getHeight()));
			final Component tip = named(shown, ChatMentionsPanel.TIP_NAME);
			final Rectangle seen = SwingUtilities.convertRectangle(tip.getParent(), tip.getBounds(), viewport.getView());
			assertTrue("the tip is in view", viewport.getViewRect().intersects(seen));
		});
	}

	@Test
	public void theShareSurvivesAReopenAtAnotherHeight() throws Exception
	{
		cardsShare = 40;
		open();
		flush();
		onEdt(() ->
		{
			final JSplitPane split = splitIn(shown);
			final int room = split.getHeight() - split.getDividerSize();
			assertTrue("at 40 % of " + room + ": " + split.getDividerLocation(),
				Math.abs(split.getDividerLocation() - room * 0.4) <= 2);
		});
		closeWindow();
		onEdt(() ->
		{
			shown = new ChatMentionsPanel(() -> resets++, settings, picker, feed, icons, copied::add);
			window = new JFrame();
			window.setFocusableWindowState(false);
			window.add(shown);
			window.setBounds(100, 100, 260, 400);
			window.setVisible(true);
			window.validate();
		});
		flush();
		onEdt(() ->
		{
			final JSplitPane split = splitIn(shown);
			final int room = split.getHeight() - split.getDividerSize();
			assertTrue("at 40 % of " + room + ": " + split.getDividerLocation(),
				Math.abs(split.getDividerLocation() - room * 0.4) <= 2);
			for (final String write : writes)
			{
				assertFalse("no share written: " + write, write.startsWith("cardsShare="));
			}
		});
	}

	@Test
	public void aVeryShortWindowHoldsBothLeastHeights() throws Exception
	{
		open();
		onEdt(() ->
		{
			window.setSize(260, 250);
			window.validate();
		});
		flush();
		onEdt(() ->
		{
			window.validate();
			final JSplitPane split = splitIn(shown);
			final int cardsHeight = split.getTopComponent().getHeight();
			final int stripHeight = named(shown, ChatMentionsPanel.STRIP_NAME).getHeight();
			assertTrue("the cards half holds the strip and 40: " + cardsHeight, cardsHeight >= stripHeight + 40);
			assertTrue("the kept chat holds 40: " + split.getBottomComponent().getHeight(),
				split.getBottomComponent().getHeight() >= 40);
			for (final String write : writes)
			{
				assertFalse("no share written: " + write, write.startsWith("cardsShare="));
			}
		});
	}

	@Test
	public void theTipSitsUnderTheHowToThenAtTheTop() throws Exception
	{
		open();
		openGuide();
		flush();
		onEdt(() ->
		{
			final Component howTo = named(shown, ChatMentionsPanel.HOW_TO_NAME);
			final Component tip = named(shown, ChatMentionsPanel.TIP_NAME);
			final int bottom = howTo.getLocationOnScreen().y + howTo.getHeight();
			final int tipY = tip.getLocationOnScreen().y;
			assertTrue("under the how-to: " + tipY + " vs " + bottom, tipY > bottom);
			assertTrue("the next thing under it: " + tipY + " vs " + bottom, tipY - bottom <= 20);
			leftPress(named(shown, ChatMentionsPanel.HOW_TO_HIDE_NAME));
			window.validate();
		});
		flush();
		onEdt(() ->
		{
			final Component tip = named(shown, ChatMentionsPanel.TIP_NAME);
			final Component scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class,
				named(shown, ChatMentionsPanel.CARDS_NAME));
			final int top = scroll.getLocationOnScreen().y;
			assertTrue("at the top: " + tip.getLocationOnScreen().y + " vs " + top,
				tip.getLocationOnScreen().y - top <= 20);
		});
	}

	private static void release(Component target)
	{
		target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 3, 3, 1,
			false, MouseEvent.BUTTON1));
	}

	private static JSplitPane splitIn(Container root)
	{
		for (final Component child : root.getComponents())
		{
			if (child instanceof JSplitPane)
			{
				return (JSplitPane) child;
			}
			if (child instanceof Container)
			{
				final JSplitPane found = splitIn((Container) child);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	/** The foreground of the characters of "2h" in the view's paragraph. */
	private static Color keywordColourIn(JTextPane view, int paragraphIndex)
	{
		final Element line = paragraph(view, paragraphIndex);
		return StyleConstants.getForeground(attributesAt(view, line.getStartOffset() + textOf(view, line).indexOf("2h")));
	}

	@Test
	public void theCardsFollowTheLitTab() throws Exception
	{
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "A", "2h a", 1);
		feed.line(ChatMessageType.PUBLICCHAT, "B", "2h b", 2);
		flush();
		onEdt(() ->
		{
			assertEquals(2, cards().size());
			assertNotNull(labelSaying(shown, "2 unread"));
			leftPress(chip("Clan"));
		});
		flush();
		onEdt(() ->
		{
			assertEquals(1, cards().size());
			assertNotNull(labelSaying(shown, "1 unread"));
			assertNotNull(labelSaying(cards().get(0), "A"));
			leftPress(chip("Public"));
		});
		flush();
		onEdt(() ->
		{
			assertEquals(1, cards().size());
			assertNotNull(labelSaying(cards().get(0), "B"));
			leftPress(chip("All"));
		});
		flush();
		onEdt(() -> assertEquals(2, cards().size()));
	}

	@Test
	public void aLineForAnotherChatMakesNoCardOnTheLitTab() throws Exception
	{
		open();
		onEdt(() -> leftPress(chip("Clan")));
		flush();
		feed.line(ChatMessageType.PUBLICCHAT, "B", "2h b", 2);
		flush();
		onEdt(() ->
		{
			assertEquals(0, cards().size());
			assertNotNull(labelSaying(shown, "0 unread"));
			assertTrue(named(shown, ChatMentionsPanel.TIP_NAME).isShowing());
		});
		assertEquals(1, feed.ledger().mentions().size());
		onEdt(() -> leftPress(chip("All")));
		flush();
		onEdt(() -> assertEquals(1, cards().size()));
	}

	@Test
	public void clearOnATabClearsOnlyItsCards() throws Exception
	{
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "A", "2h a", 1);
		feed.line(ChatMessageType.PUBLICCHAT, "B", "2h b", 2);
		flush();
		onEdt(() -> leftPress(chip("Clan")));
		flush();
		onEdt(() -> leftPress(labelSaying(shown, "Clear")));
		assertEquals(Arrays.asList("B"), whos());
		flush();
		onEdt(() -> leftPress(chip("All")));
		flush();
		onEdt(() ->
		{
			assertEquals(1, cards().size());
			assertNotNull(labelSaying(cards().get(0), "B"));
		});
	}

	@Test
	public void aCardClickOnAllLightsItsChatAndTheCardsFollow() throws Exception
	{
		open();
		feed.line(ChatMessageType.CLAN_CHAT, "A", "2h a", 1);
		feed.line(ChatMessageType.PUBLICCHAT, "B", "2h b", 2);
		flush();
		onEdt(() -> leftPress(cards().get(0)));
		flush();
		onEdt(() ->
		{
			assertEquals(Arrays.asList("Public"), litChips());
			assertEquals(1, cards().size());
		});
	}

	private static String clock(long at)
	{
		return Times.clock(at, ZoneId.systemDefault());
	}

	private List<String> whos()
	{
		final List<String> out = new ArrayList<>();
		for (final Mention mention : feed.ledger().mentions())
		{
			out.add(mention.who());
		}
		return out;
	}

	/** The cards in the column, top first. */
	private List<Container> cards()
	{
		final List<Container> out = new ArrayList<>();
		for (final Component part : ((Container) named(shown, ChatMentionsPanel.CARDS_NAME)).getComponents())
		{
			if (ChatMentionsPanel.CARD_NAME.equals(part.getName()))
			{
				out.add((Container) part);
			}
		}
		return out;
	}

	private List<JToggleButton> chips()
	{
		final List<JToggleButton> out = new ArrayList<>();
		for (final Component part : ((Container) named(shown, ChatMentionsPanel.CHIPS_NAME)).getComponents())
		{
			if (part instanceof JToggleButton)
			{
				out.add((JToggleButton) part);
			}
		}
		return out;
	}

	private List<String> chipWords()
	{
		final List<String> out = new ArrayList<>();
		for (final JToggleButton chip : chips())
		{
			out.add(chip.getText());
		}
		return out;
	}

	private List<String> litChips()
	{
		final List<String> out = new ArrayList<>();
		for (final JToggleButton chip : chips())
		{
			if (chip.isSelected())
			{
				out.add(chip.getText());
			}
		}
		return out;
	}

	private JToggleButton chip(String words)
	{
		for (final JToggleButton chip : chips())
		{
			if (words.equals(chip.getText()))
			{
				return chip;
			}
		}
		throw new AssertionError("no chip reads: " + words);
	}

	private JTextPane view()
	{
		return (JTextPane) named(shown, ChatMentionsPanel.VIEW_NAME);
	}

	/** The first part under {@code root} with that name. */
	private static Component named(Container root, String name)
	{
		for (final Component child : root.getComponents())
		{
			if (name.equals(child.getName()))
			{
				return child;
			}
			if (child instanceof Container)
			{
				final Component found = named((Container) child, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static JTextPane paneIn(Container root)
	{
		for (final Component child : root.getComponents())
		{
			if (child instanceof JTextPane)
			{
				return (JTextPane) child;
			}
			if (child instanceof Container)
			{
				final JTextPane found = paneIn((Container) child);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	/** Every label under {@code root}, in the tree's order. */
	private static List<JLabel> labelsIn(Container root)
	{
		final List<JLabel> found = new ArrayList<>();
		for (final Component child : root.getComponents())
		{
			if (child instanceof JLabel)
			{
				found.add((JLabel) child);
			}
			if (child instanceof Container)
			{
				found.addAll(labelsIn((Container) child));
			}
		}
		return found;
	}

	/** The words of every label under {@code root}, tags taken out, and of every text pane, in the tree's order. */
	private static List<String> textsIn(Container root)
	{
		final List<String> texts = new ArrayList<>();
		for (final Component child : root.getComponents())
		{
			if (child instanceof JLabel)
			{
				texts.add(plain(((JLabel) child).getText()));
			}
			else if (child instanceof JTextPane)
			{
				texts.add(((JTextPane) child).getText().trim());
			}
			else if (child instanceof Container)
			{
				texts.addAll(textsIn((Container) child));
			}
		}
		return texts;
	}

	/** A label's text without its HTML tags, a line break read as a space. */
	private static String plain(String text)
	{
		return text == null ? "" : text.replaceAll("<br>", " ").replaceAll("<[^>]*>", "").trim();
	}

	/** The label under {@code root} whose words are {@code words}. */
	private static JLabel labelSaying(Container root, String words)
	{
		for (final JLabel label : labelsIn(root))
		{
			if (words.equals(plain(label.getText())))
			{
				return label;
			}
		}
		throw new AssertionError("no label says: " + words);
	}

	/** A left-button press on {@code target}, delivered the way the event queue delivers it. */
	private static void leftPress(Component target)
	{
		target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK, 3, 3, 1, false, MouseEvent.BUTTON1));
	}

	/** Lets everything already queued for the event thread run, from a thread that is not the event thread. */
	private static void flush() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { });
		SwingUtilities.invokeAndWait(() -> { });
	}

	private static AttributeSet attributesAt(JTextPane pane, int offset)
	{
		return pane.getStyledDocument().getCharacterElement(offset).getAttributes();
	}

	private static Element paragraph(JTextPane pane, int index)
	{
		return pane.getDocument().getDefaultRootElement().getElement(index);
	}

	private static Element lastParagraph(JTextPane pane)
	{
		final Element root = pane.getDocument().getDefaultRootElement();
		return root.getElement(root.getElementCount() - 1);
	}

	/** A paragraph's text, its line end left off. */
	private static String textOf(JTextPane pane, Element paragraph)
	{
		final StyledDocument doc = pane.getStyledDocument();
		final int end = Math.min(paragraph.getEndOffset(), doc.getLength());
		try
		{
			return doc.getText(paragraph.getStartOffset(), end - paragraph.getStartOffset()).replace("\n", "");
		}
		catch (BadLocationException e)
		{
			throw new AssertionError(e);
		}
	}

	/** Every paragraph of the view's document, trimmed. */
	private static List<String> paragraphs(JTextPane pane)
	{
		final List<String> out = new ArrayList<>();
		final Element root = pane.getDocument().getDefaultRootElement();
		for (int i = 0; i < root.getElementCount(); i++)
		{
			out.add(textOf(pane, root.getElement(i)).trim());
		}
		return out;
	}

	/**
	 * Whether the rectangle of the paragraph's start offset lies in the view's visible rectangle. Choice: the caret's
	 * rectangle is 0 px wide, which {@link Rectangle#contains(Rectangle)} never holds, so it is read 1 px wide.
	 */
	private static boolean inView(JTextPane pane, Element paragraph)
	{
		try
		{
			final Rectangle at = pane.modelToView2D(paragraph.getStartOffset()).getBounds();
			return pane.getVisibleRect().contains(new Rectangle(at.x, at.y, Math.max(1, at.width), at.height));
		}
		catch (BadLocationException e)
		{
			throw new AssertionError(e);
		}
	}

	/** Whether every character of the paragraph (its line end left off) has the mark's ground (55, 55, 55). */
	private static boolean marked(JTextPane pane, Element paragraph)
	{
		final int length = textOf(pane, paragraph).length();
		if (length == 0)
		{
			return false;
		}
		for (int i = 0; i < length; i++)
		{
			final AttributeSet attributes = attributesAt(pane, paragraph.getStartOffset() + i);
			if (!attributes.isDefined(StyleConstants.Background)
				|| !new Color(55, 55, 55).equals(StyleConstants.getBackground(attributes)))
			{
				return false;
			}
		}
		return true;
	}

	private static String captionOf(JPanel row)
	{
		return ((JLabel) row.getComponent(0)).getText();
	}

	private static Component lastOf(JPanel row)
	{
		return row.getComponent(row.getComponentCount() - 1);
	}

	private static JButton button(JPanel row, String text)
	{
		for (final Component part : row.getComponents())
		{
			if (part instanceof JButton && text.equals(((JButton) part).getText()))
			{
				return (JButton) part;
			}
		}
		return null;
	}

	private static JCheckBoxMenuItem check(JPopupMenu menu, String text)
	{
		for (final Component part : menu.getComponents())
		{
			if (part instanceof JCheckBoxMenuItem && text.equals(((JCheckBoxMenuItem) part).getText()))
			{
				return (JCheckBoxMenuItem) part;
			}
		}
		return null;
	}

	/** A left-button press and release inside the item, delivered the way the event queue delivers them. */
	private static void click(AbstractButton item)
	{
		final int x = Math.max(1, item.getWidth() / 2);
		final int y = Math.max(1, item.getHeight() / 2);
		item.dispatchEvent(new MouseEvent(item, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
		item.dispatchEvent(new MouseEvent(item, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, x, y, 1, false,
			MouseEvent.BUTTON1));
	}

	private static void onEdt(Runnable body) throws Exception
	{
		final Throwable[] failure = new Throwable[1];
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				body.run();
			}
			catch (Throwable thrown)
			{
				failure[0] = thrown;
			}
		});
		if (failure[0] instanceof Error)
		{
			throw (Error) failure[0];
		}
		if (failure[0] instanceof RuntimeException)
		{
			throw (RuntimeException) failure[0];
		}
	}

	private static JLabel settingsIcon(Container root)
	{
		for (final Component child : root.getComponents())
		{
			if (child instanceof JLabel && SupportLinks.SETTINGS_TIP.equals(((JLabel) child).getToolTipText()))
			{
				return (JLabel) child;
			}
			if (child instanceof Container)
			{
				final JLabel found = settingsIcon((Container) child);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static void press(JLabel label)
	{
		assertNotNull("the settings icon is in the panel", label);
		final MouseEvent event = new MouseEvent(label, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 1, 1, 1,
			false, MouseEvent.BUTTON1);
		for (final MouseListener listener : label.getMouseListeners())
		{
			listener.mousePressed(event);
		}
	}

	private static JPopupMenu popupMenu(JFrame frame)
	{
		final JPopupMenu inFrame = popupIn(frame.getLayeredPane());
		if (inFrame != null)
		{
			return inFrame;
		}
		for (final Window window : Window.getWindows())
		{
			final JPopupMenu inWindow = popupIn(window);
			if (inWindow != null)
			{
				return inWindow;
			}
		}
		return null;
	}

	private static JPopupMenu popupIn(Component component)
	{
		if (component instanceof JPopupMenu)
		{
			return (JPopupMenu) component;
		}
		if (component instanceof Container)
		{
			for (final Component child : ((Container) component).getComponents())
			{
				final JPopupMenu found = popupIn(child);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}

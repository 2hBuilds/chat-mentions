package com.chatmentions;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/**
 * The sidebar panel: the top card ("Mentions", the count, "Clear" and the support pair), then a split of two halves
 * that each scroll on their own: the mention cards, newest first, with the keyword strip at their foot, over the kept
 * chat of the lit tab. A card's press jumps the kept chat to its line; "Copy name" and "Mark read" act on the card. The settings icon opens the settings
 * menu (STANDARD A4 and A5).
 *
 * <p>The views follow the feed the model's way: the ledger's listener and the feed's each set a flag and queue one sync
 * on the event thread, so a burst of lines costs one sync.
 */
public class ChatMentionsPanel extends PluginPanel
{
	/** The plugin's GitHub page, what the menu header's fourth mark opens. */
	static final String GITHUB_URL = "https://github.com/2hBuilds/chat-mentions";

	/** The menu's rows. */
	static final String RESET_TEXT = "Reset to default";
	static final String OK_TEXT = "OK";

	/** The names the tests find the parts by. */
	static final String CARDS_NAME = "mention-cards";
	static final String TIP_NAME = "mention-tip";
	static final String CARD_NAME = "mention-card";
	static final String CHIPS_NAME = "chat-chips";
	static final String VIEW_NAME = "chat-view";
	static final String STRIP_NAME = "keyword-strip";
	static final String KEYWORD_CHIPS_NAME = "keyword-chips";
	static final String KEYWORD_NAME_NAME = "keyword-name";
	static final String KEYWORD_CHIP_NAME = "keyword-chip";
	static final String KEYWORD_REMOVE_NAME = "keyword-remove";
	static final String KEYWORD_FIELD_NAME = "keyword-field";
	static final String KEYWORD_ADD_NAME = "keyword-add";
	static final String KEYWORD_SWATCH_NAME = "keyword-swatch";
	static final String KEYWORD_CLEAR_NAME = "keyword-clear";
	static final String KEYWORD_COLOUR_ROW_NAME = "keyword-colour-row";
	static final String HOW_TO_NAME = "how-to";
	static final String HOW_TO_HIDE_NAME = "how-to-hide";
	static final String HOW_TO_INTRO_NAME = "how-to-intro";
	static final String HOW_TO_TITLE_NAME = "how-to-title";
	static final String HOW_TO_STEP_NAME = "how-to-step";
	static final String HOW_TO_NOTE_NAME = "how-to-note";
	static final String HOW_TO_ROW_NAME = "how-to-row";
	static final String HELP_NAME = "how-to-help";
	/** The (?)'s one-word hover. */
	static final String HELP_TIP = "Help";

	/** The words of the top card and of a card. */
	static final String TITLE_TEXT = "Mentions";
	static final String CLEAR_TEXT = "Clear";
	static final String COPY_TEXT = "Copy name";
	static final String MARK_READ_TEXT = "Mark read";
	private static final String UNREAD_TEXT = " unread";
	private static final String NAME_CHIP_FALLBACK = "Your name";
	private static final String HINT_TEXT = "Add keyword";
	private static final String REMOVE_TEXT = "\u00d7";
	private static final String ADD_TEXT = "+";
	private static final String COLOURS_TEXT = "Colours";
	private static final String RESET_COLOURS_TEXT = "Reset all keyword colours";
	private static final String DEFAULT_COLOUR_TEXT = "Default keyword colour";
	static final String HOW_TO_TEXT = "How to use";
	static final String HOW_TO_TIP = "Shows the steps above the cards.";
	private static final String HIDE_TEXT = "Hide";
	/** The how-to's title: the family mark in orange, then the plugin's name in white. */
	private static final String HOW_TO_TITLE_2H = "2h";
	private static final String HOW_TO_TITLE = "Chat Mentions";
	/** The how-to's sentence, the words exactly. */
	private static final String HOW_TO_HOOK = "Catch up after being away:";
	private static final String HOW_TO_INTRO = "Every chat line that names you or a keyword becomes a card.";
	/** The note under the steps, the words exactly. */
	private static final String HOW_TO_NOTE =
		"This guide hides when your first card comes in. 'How to use' in the settings menu opens it anytime.";
	/** The how-to's six steps, the words exactly. */
	private static final String[] HOW_TO_STEPS = {
		"Add keywords under the cards. Your name is one.",
		"A chat line with a keyword becomes a card.",
		"Click a card to jump to its line in the chat.",
		"Click a keyword to pick its colour.",
		"Tabs: the game's chats. All shows every chat.",
		"The settings menu (the icon): chats, colours, sizes."};
	/** The width of a step's number column, in px. */
	private static final int STEP_NUMBER_WIDTH = 16;
	/** The room between two steps, in px. */
	private static final int STEP_GAP = 4;

	/** The keyword colour's default, 2h Chat Hover Info's blue (STANDARD A3: orange is for the lit chip alone); the setting holds the colour in force. */
	static final Color MENTION_COLOUR = new Color(59, 130, 246);
	/** A time's grey. */
	private static final Color TIME_GREY = new Color(130, 130, 130);
	/** The ground of the paragraph a card click jumped to. */
	static final Color MARK_COLOUR = new Color(55, 55, 55);

	/** The panel's and the top card's padding, in px. */
	private static final int PADDING = 8;
	/** A card's padding; the room between two cards, between a card's rows and under the top card, in px. */
	private static final int CARD_PADDING = 6;
	private static final int GAP = 6;
	/** The room between a card's two links, in px. */
	private static final int LINK_GAP = 12;
	/** The orange bar at a hot card's left edge, in px. */
	private static final int BAR_WIDTH = 2;
	/** The tabs' gap, in px. */
	private static final int CHIP_GAP = 2;
	/** The keyword strip's padding, the room between its two rows, the chips' gap and a chip's height, in px. */
	private static final int STRIP_PADDING = 6;
	private static final int STRIP_ROWS_GAP = 5;
	private static final int KEYWORD_GAP = 4;
	private static final int KEYWORD_CHIP_HEIGHT = 18;
	/** The least height of the add row, so a strip of one line of chips is 57 px tall, and that height. */
	private static final int ADD_ROW_HEIGHT = 22;
	private static final int STRIP_HEIGHT = 2 * STRIP_PADDING + KEYWORD_CHIP_HEIGHT + STRIP_ROWS_GAP + ADD_ROW_HEIGHT;
	private static final Color STRIP_EDGE = new Color(77, 77, 77);
	private static final Color KEYWORD_CHIP_GROUND = new Color(55, 55, 55);
	/** The keyword swatch's side, in px. */
	private static final int SWATCH_SIZE = 16;
	private static final Color FAINT_GREY = new Color(120, 120, 120);
	/** A scroll wheel step, in px. */
	private static final int SCROLL_STEP = 16;
	/** The split's divider, in px, and the least and the most of the height the cards may take, in percent. */
	private static final int DIVIDER_SIZE = 6;
	private static final int SHARE_MIN = 10;
	private static final int SHARE_MAX = 90;
	/** The left inset of the two rows under "Colours", in px from the row's edge; with the menu's 5 px border their words stand 30 px from the menu's left edge. */
	private static final int COLOURS_INDENT = 25;
	/** The height both halves ask for, so the first split follows the weight alone; and the least each may be. */
	private static final int HALF_HEIGHT = 200;
	private static final int HALF_MIN_HEIGHT = 40;
	/** The room above and below the tip line, in px. */
	private static final int TIP_ROOM = 10;
	/** The column's first two children, the how-to's holder and the tip's, which {@link #syncCards} leaves where they are. */
	private static final int FIXED_PARTS = 2;
	/**
	 * The keyword strip's ground: the panel's own colour inside a 1 px rim, so the strip reads as an outlined area of the
	 * panel, the same ground as the tabs under it, and never as one more card (the user, 2026-10-29: the well belongs on
	 * the keywords, and in the colour round the chat tabs).
	 */
	static final Color WELL = ColorScheme.DARKER_GRAY_COLOR;
	static final Color WELL_RIM = new Color(52, 52, 52);

	/** A card's border at rest and under the mouse; the bar takes the room the resting border leaves. */
	private static final Border CARD_BORDER = new EmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING);
	private static final Border CARD_HOT_BORDER = new CompoundBorder(
		new MatteBorder(0, BAR_WIDTH, 0, 0, ColorScheme.BRAND_ORANGE),
		new EmptyBorder(CARD_PADDING, CARD_PADDING - BAR_WIDTH, CARD_PADDING, CARD_PADDING));

	/** The room between a sender's rank icon and the name on a card, in px. */
	private static final int ICON_ROOM = 4;
	/** The settings icon's size, in px. */
	private static final int ICON_SIZE = 12;
	/** The (?) in its two colours: gold while the guide is hidden, white while it shows. */
	private static final ImageIcon HELP_GOLD = HelpGlyph.icon(ICON_SIZE, ColorScheme.BRAND_ORANGE);
	private static final ImageIcon HELP_WHITE = HelpGlyph.icon(ICON_SIZE, Color.WHITE);
	/** A press on the settings icon this soon after the menu closed is the press that closed it, not a new open. */
	private static final long REOPEN_GUARD_NANOS = 250_000_000L;
	/** The gap between a row's caption and its control, in px. */
	private static final int ROW_CAPTION_GAP = 10;
	/** The number boxes' ranges. */
	private static final int CARDS_MIN = 1;
	private static final int CARDS_MAX = 1000;
	private static final int LINES_MIN = 100;
	private static final int LINES_MAX = 20000;
	private static final int LINES_STEP = 100;

	private final Runnable resetToDefault;
	private final PanelSettings settings;
	private final ColourPicker picker;
	private final MentionFeed feed;
	private final RankIcons icons;
	private final Consumer<String> copyName;
	private final SupportLinks support;
	private final Map<Chat, JCheckBoxMenuItem> watchChecks = new EnumMap<>(Chat.class);
	/** The hover on the Block what the chatbox blocks check. */
	static final String FOLLOW_FILTER_TIP = "Leaves out what your chatbox hides: the game's Public setting (autochat, friends, off) and RuneLite's Chat Filter plugin. Spam getting through? Add the words to Chat Filter.";
	/** The hover on the whole-words check. */
	static final String WHOLE_WORDS_TIP = "Ticked: 2h matches only the word 2h. Unticked: 2h also matches 2hbuilds.";

	private JCheckBoxMenuItem wholeWordsCheck;
	private JCheckBoxMenuItem followFilterCheck;
	private JTextField keywordsBox;
	private JPanel keywordChips;
	private JTextField keywordField;
	private JSpinner cardsSpinner;
	private JSpinner linesSpinner;
	private JLabel keywordSwatch;
	/** The keyword colour the documents and the cards were last written in. */
	private Color keywordColourUsed;
	/** The word colours setting the documents and the cards were last written with. */
	private String coloursUsed;
	/** The split, and the cards' share (whole percent) its divider was last put at or let go at; -1 before the first. */
	private JSplitPane split;
	private int sharePlaced = -1;
	private boolean syncing;
	private final Random random = new Random();
	private JLabel tipLabel;
	/** The tip's holder in the column, the words of the tip drawn, and the width (px) its label's text is laid for; 0 before the first. */
	private JPanel tipHolder;
	private String tipWords = "";
	private int tipWidth;
	/** The how-to's holder (the card and the room under it), and whether the how-to is on. */
	private JPanel howTo;
	/** The cards half, whose least height is the strip's plus 40 px whatever the how-to does. */
	private JPanel cardsHalfPanel;
	private boolean howToShown;
	/** The "Clear" word, which shows only while the selected tab shows a card. */
	private JLabel clearLabel;
	/** The (?) beside the Discord mark, in Clear's place: it shows while the selected tab shows no card. */
	private JLabel helpIcon;
	/** True while the mouse is over the (?). */
	private boolean helpHovered;
	/** True while the cards half shows the tip; the how-to goes when the first card replaces it. */
	private boolean tipShown = true;
	/** True from a keyword chip's click until the picker it opened is closed. */
	private boolean pickerOpen;
	private final JPopupMenu menu;
	private long closedAtNanos = System.nanoTime() - 2 * REOPEN_GUARD_NANOS;

	private final JLabel count = new JLabel();
	private final CardColumn column = new CardColumn();
	private final Map<Mention, JPanel> cardPanels = new IdentityHashMap<>();
	private final AtomicBoolean cardsQueued = new AtomicBoolean();

	private final JPanel chipRow = new JPanel(new WrapLayout(CHIP_GAP));
	private final Map<Chat, JToggleButton> chips = new EnumMap<>(Chat.class);
	private ButtonGroup chipGroup = new ButtonGroup();
	private final JTextPane chatView = new JTextPane();
	private JScrollPane chatScroll;
	private final Map<Chat, DefaultStyledDocument> documents = new EnumMap<>(Chat.class);
	/** Per chat, the first and the last line number its document holds; -1 when it holds none. */
	private final Map<Chat, long[]> held = new EnumMap<>(Chat.class);
	private Chat shown;
	/** The tab the first {@link #rebuildChips} lights: the kept one, or null for the first (All). */
	private Chat wanted;
	private Chat markedChat;
	private long markedSeq = -1;
	private final Set<Chat> dirtyChats = EnumSet.noneOf(Chat.class);
	private final AtomicBoolean chatsQueued = new AtomicBoolean();

	/**
	 * @param resetToDefault what "Reset to default" does: puts the plugin's settings back to their defaults
	 * @param settings the fifteen settings the menu shows and writes
	 * @param picker opens the colour picker for the keyword colour
	 * @param feed the kept lines and the cards the panel shows
	 * @param icons the senders' rank icons the client thread captured
	 * @param copyName what "Copy name" hands a card's sender to; called on the event thread
	 */
	ChatMentionsPanel(Runnable resetToDefault, PanelSettings settings, ColourPicker picker, MentionFeed feed,
		RankIcons icons, Consumer<String> copyName)
	{
		super(false);
		this.resetToDefault = resetToDefault;
		this.settings = settings;
		this.picker = picker;
		this.feed = feed;
		this.icons = icons;
		this.copyName = copyName;
		this.keywordColourUsed = settings.keywordColour();
		this.coloursUsed = settings.keywordColours();
		this.support = new SupportLinks("2h Chat Mentions", Version.CURRENT, GITHUB_URL, LinkBrowser::browse, this::closeMenu);
		this.menu = buildMenu();
		for (final Chat chat : Chat.values())
		{
			documents.put(chat, new DefaultStyledDocument());
			held.put(chat, new long[]{-1, -1});
		}

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
		add(topCard(), BorderLayout.NORTH);

		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, cardsHalf(), chatHalf());
		split.setDividerSize(DIVIDER_SIZE);
		split.setResizeWeight(share() / 100.0);
		split.setBorder(null);
		split.setOpaque(false);
		watchDivider();
		final JPanel below = new JPanel(new BorderLayout());
		below.setOpaque(false);
		below.setBorder(new EmptyBorder(GAP, 0, 0, 0));
		below.add(split, BorderLayout.CENTER);
		add(below, BorderLayout.CENTER);

		wanted = chosenTab();
		rebuildChips();
		rebuildStrip();
		for (final Chat chat : Chat.values())
		{
			syncChat(chat);
		}
		syncCards();
		feed.ledger().listen(this::queueCardsSync);
		feed.listen(this::queueChatSync);
	}

	/**
	 * Called on the event thread when a setting changed elsewhere (or the player's name became known): a menu on show
	 * follows it, and the tabs and the keyword strip's chips are made again.
	 */
	void settingsChanged()
	{
		if (menu.isVisible())
		{
			syncControls();
		}
		redraw();
		if (sharePlaced >= 0 && share() != sharePlaced)
		{
			placeDivider();
		}
		rebuildChips();
		final Chat chosen = chosenTab();
		if (chosen != null && chosen != shown)
		{
			show(chosen, true);
		}
		rebuildStrip();
	}

	/**
	 * The chat whose name the tab setting holds, when it has a tab (All always has); null when the setting is none of
	 * them or the chat is unwatched.
	 *
	 * <p>Choice: a bad setting is read by comparing names, never by {@code valueOf}, so it stays quiet.
	 */
	private Chat chosenTab()
	{
		final String name = settings.shownTab();
		for (final Chat chat : Chat.values())
		{
			if (chat.name().equals(name))
			{
				return chat == Chat.ALL || settings.watched(chat) ? chat : null;
			}
		}
		return null;
	}

	/** A player's choice of tab (a press or a card click): shows it and keeps it as the tab the panel opens on. */
	private void choose(Chat chat, boolean fromNewest)
	{
		show(chat, fromNewest);
		if (!chat.name().equals(settings.shownTab()))
		{
			settings.setShownTab(chat.name());
		}
	}

	/**
	 * When the keyword colour or the word colours setting differs from the one last used, empties every kept-chat
	 * document and fills it again from its buffer (the marked paragraph is forgotten and the shown chat goes to the
	 * bottom), and makes the cards again.
	 */
	private void redraw()
	{
		final Color now = settings.keywordColour();
		final String colours = settings.keywordColours();
		if (now == null || (now.equals(keywordColourUsed) && Objects.equals(colours, coloursUsed)))
		{
			syncSwatch();
			return;
		}
		keywordColourUsed = now;
		coloursUsed = colours;
		try
		{
			for (final Chat chat : Chat.values())
			{
				final DefaultStyledDocument doc = documents.get(chat);
				doc.remove(0, doc.getLength());
				final long[] range = held.get(chat);
				range[0] = -1;
				range[1] = -1;
			}
		}
		catch (BadLocationException e)
		{
			// The whole length of the document is removed, so this does not come.
		}
		markedChat = null;
		markedSeq = -1;
		for (final Chat chat : Chat.values())
		{
			syncChat(chat);
		}
		if (shown != null)
		{
			chatScroll.validate();
			toBottom();
		}
		cardPanels.clear();
		syncCards();
		syncSwatch();
	}

	/** The cards' share of the split, in whole percent, from the setting held to 10-90. */
	private int share()
	{
		return Math.max(SHARE_MIN, Math.min(SHARE_MAX, settings.cardsShare()));
	}

	/** Puts the divider at the share, and the split's weight with it, so a resize keeps the share. */
	private void placeDivider()
	{
		final int share = share();
		split.setResizeWeight(share / 100.0);
		final int room = split.getHeight() - split.getDividerSize();
		if (room <= 0)
		{
			split.setDividerLocation(share / 100.0);
		}
		else
		{
			// A split pane holds a least height on a drag, not on a placement: the place is clamped here, so the
			// cards half is never shorter than the how-to it shows, nor the kept chat than its least.
			final int least = cardsHalfPanel == null ? 0 : cardsHalfPanel.getMinimumSize().height;
			final int most = Math.max(least, room - HALF_MIN_HEIGHT);
			final int wanted = (int) Math.round(room * share / 100.0);
			split.setDividerLocation(Math.max(least, Math.min(most, wanted)));
		}
		sharePlaced = share;
	}

	/**
	 * Places the divider the first time the split has a height, and writes the share when a drag lets go of the divider.
	 *
	 * <p>Choice: the release listener is added only when the split's UI is Swing's {@link BasicSplitPaneUI} (RuneLite's
	 * look and feel extends it); otherwise the drag is not kept.
	 */
	private void watchDivider()
	{
		split.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				if (sharePlaced < 0 && split.getHeight() > 0)
				{
					placeDivider();
				}
			}
		});
		if (!(split.getUI() instanceof BasicSplitPaneUI))
		{
			return;
		}
		((BasicSplitPaneUI) split.getUI()).getDivider().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				final int room = split.getHeight() - split.getDividerSize();
				if (!SwingUtilities.isLeftMouseButton(e) || room <= 0 || sharePlaced < 0)
				{
					return;
				}
				final int percent = Math.max(SHARE_MIN,
					Math.min(SHARE_MAX, (int) Math.round(100.0 * split.getDividerLocation() / room)));
				if (percent != sharePlaced)
				{
					sharePlaced = percent;
					split.setResizeWeight(percent / 100.0);
					settings.setCardsShare(percent);
				}
			}
		});
	}

	/** The per-word colours in force. */
	private WordColours colours()
	{
		return WordColours.of(settings.keywordColours());
	}

	/**
	 * The keyword style of a span, given the span and the text it covers: the colour of the word the text IS (a hit
	 * reads as its keyword, ignoring case), else the colour of the word the span's index names among the keywords in
	 * force now (a merged hit of two words), else the keyword colour.
	 *
	 * <p>Choice: the text comes first because a card keeps its spans from the moment it was made, and a word's index
	 * shifts when a keyword before it is removed; the text does not.
	 */
	private BiFunction<int[], String, AttributeSet> spanStyle()
	{
		final List<String> words = Keywords.of(settings.keywords(), false, feed.playerName()).words();
		final WordColours own = colours();
		final Color fallback = settings.keywordColour();
		return (span, hit) ->
		{
			Color colour = own.colour(hit);
			if (colour == null && span.length > 2 && span[2] >= 0 && span[2] < words.size())
			{
				colour = own.colour(words.get(span[2]));
			}
			return keywordStyle(colour != null ? colour : fallback);
		};
	}

	/** Paints the menu's swatch with the colour in force. */
	private void syncSwatch()
	{
		if (keywordSwatch != null)
		{
			keywordSwatch.setBackground(settings.keywordColour());
		}
	}

	/** The top card's one row: the title, the count, "Clear", then the support pair at the right end. */
	private JPanel topCard()
	{
		final JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.add(label(TITLE_TEXT, FontManager.getDefaultBoldFont().deriveFont(13f), Color.WHITE));
		row.add(Box.createHorizontalStrut(PADDING));
		count.setFont(FontManager.getDefaultFont().deriveFont(11f));
		count.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		count.setText(0 + UNREAD_TEXT);
		row.add(count);
		row.add(Box.createHorizontalGlue());
		clearLabel = link(CLEAR_TEXT, () -> feed.ledger().clear(shown), null);
		clearLabel.setVisible(false);
		row.add(clearLabel);
		helpIcon = helpIcon();
		row.add(helpIcon);
		row.add(Box.createHorizontalStrut(PADDING));
		row.add(support.supportPair(settingsIcon()));
		card.add(row, BorderLayout.CENTER);
		return card;
	}

	/**
	 * The upper half: one scrolling column (the how-to while it shows, then the tip while the ledger is empty, else the
	 * cards, newest first) with the keyword strip fixed at its foot, just above the divider.
	 *
	 * <p>Choice: the tip's room is 10 px above and below; under the how-to card (whose holder already leaves {@code GAP})
	 * the top room is cut by that gap, so the tip stands 10 px under the card.
	 */
	private JPanel cardsHalf()
	{
		column.setName(CARDS_NAME);
		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
		column.setOpaque(false);
		column.add(howToHolder());
		column.add(tipHolder());
		final JScrollPane scroll = new JScrollPane(column, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.getVerticalScrollBar().setUnitIncrement(SCROLL_STEP);

		// The strip stands GAP under the well, as the cards stand apart, so the two never touch.
		final JPanel strip = new JPanel(new BorderLayout());
		strip.setOpaque(false);
		strip.setBorder(new EmptyBorder(GAP, 0, 0, 0));
		strip.add(keywordStrip(), BorderLayout.CENTER);

		final JPanel half = new JPanel(new BorderLayout());
		half.setOpaque(false);
		half.add(scroll, BorderLayout.CENTER);
		half.add(strip, BorderLayout.SOUTH);
		sized(half, GAP + STRIP_HEIGHT + HALF_MIN_HEIGHT);
		cardsHalfPanel = half;
		return half;
	}

	/** The tip's line in the column: a see-through holder with the tip centred across, as tall as its words. */
	private JPanel tipHolder()
	{
		final JLabel tip = label("", FontManager.getDefaultFont().deriveFont(12f), ColorScheme.LIGHT_GRAY_COLOR);
		tipLabel = tip;
		tip.setName(TIP_NAME);
		tip.setHorizontalAlignment(SwingConstants.CENTER);
		tip.setVerticalAlignment(SwingConstants.CENTER);
		tipHolder = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		tipHolder.setOpaque(false);
		tipHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
		tipHolder.setBorder(new EmptyBorder(TIP_ROOM, 0, TIP_ROOM, 0));
		tipHolder.add(tip, BorderLayout.CENTER);
		tipHolder.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				if (tipHolder.getWidth() > 0 && tipHolder.getWidth() != tipWidth)
				{
					tipWidth = tipHolder.getWidth();
					showTip();
					tipHolder.revalidate();
				}
			}
		});
		drawTip();
		return tipHolder;
	}

	/** Draws one of the tips afresh: "Tip:" in bold, the rest plain. */
	private void drawTip()
	{
		final String tip = Tips.pick(random);
		tipWords = tip.startsWith("Tip:") ? tip.substring("Tip:".length()) : tip;
		showTip();
	}

	/**
	 * Writes the tip label's text: laid for the holder's width (in pt, which Swing takes as the pixels asked for), so
	 * the words fold inside the column and the holder is as tall as they are.
	 */
	private void showTip()
	{
		final String body = tipWidth > 0 ? "<body style='width:" + tipWidth + "pt;text-align:center'>" : "<body>";
		tipLabel.setText("<html>" + body + "<b>Tip:</b>" + tipWords + "</body></html>");
	}

	/**
	 * The how-to's holder: the card with the room of {@code GAP} under it, so the cards or the tip start below it.
	 *
	 * <p>Choice: the sentence and the steps' words are {@link WrapLabel}s (HTML labels) whose width is set from the
	 * card's real inner width (less the number column) each time the card's width changes, so they wrap and never clip.
	 */
	private JPanel howToHolder()
	{
		final JPanel card = new JPanel();
		card.setName(HOW_TO_NAME);
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

		final JPanel title = new JPanel();
		title.setName(HOW_TO_TITLE_NAME);
		title.setLayout(new BoxLayout(title, BoxLayout.X_AXIS));
		title.setOpaque(false);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		title.add(label(HOW_TO_TITLE_2H, FontManager.getDefaultBoldFont().deriveFont(13f), ColorScheme.BRAND_ORANGE));
		title.add(Box.createHorizontalStrut(4));
		title.add(label(HOW_TO_TITLE, FontManager.getDefaultBoldFont().deriveFont(13f), Color.WHITE));
		title.add(Box.createHorizontalGlue());
		// Hide sits on the title row, at the card's top right: it takes the whole card down, not the steps alone.
		final JLabel hide = link(HIDE_TEXT, () -> showHowTo(false), null);
		hide.setName(HOW_TO_HIDE_NAME);
		title.add(hide);
		card.add(title);
		// The user's pick, how-to sheet 8 number 1: a thin rule right under the title, the hook in bold, the
		// explanation in grey, a second rule before the steps.
		card.add(Box.createVerticalStrut(4));
		card.add(rule());
		card.add(Box.createVerticalStrut(10));

		final JLabel hook = label(HOW_TO_HOOK, FontManager.getDefaultBoldFont().deriveFont(12f), Color.WHITE);
		hook.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.add(hook);
		card.add(Box.createVerticalStrut(3));
		final List<WrapLabel> wraps = new ArrayList<>();
		final WrapLabel intro = new WrapLabel(0, HOW_TO_INTRO);
		intro.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		wraps.add(intro);
		intro.setName(HOW_TO_INTRO_NAME);
		card.add(intro);
		card.add(Box.createVerticalStrut(6));
		card.add(rule());
		card.add(Box.createVerticalStrut(10));

		final JPanel head = new JPanel(new BorderLayout());
		head.setOpaque(false);
		head.setAlignmentX(Component.LEFT_ALIGNMENT);
		head.add(label(HOW_TO_TEXT, FontManager.getDefaultBoldFont().deriveFont(12f), Color.WHITE), BorderLayout.WEST);
		card.add(head);

		for (int i = 0; i < HOW_TO_STEPS.length; i++)
		{
			card.add(Box.createVerticalStrut(i == 0 ? GAP : STEP_GAP));
			final JPanel step = new JPanel(new BorderLayout());
			step.setName(HOW_TO_STEP_NAME);
			step.setOpaque(false);
			step.setAlignmentX(Component.LEFT_ALIGNMENT);
			final JLabel number = label((i + 1) + ".", FontManager.getDefaultBoldFont().deriveFont(12f),
				ColorScheme.LIGHT_GRAY_COLOR);
			number.setVerticalAlignment(SwingConstants.TOP);
			number.setPreferredSize(new Dimension(STEP_NUMBER_WIDTH, number.getPreferredSize().height));
			step.add(number, BorderLayout.WEST);
			final WrapLabel words = new WrapLabel(STEP_NUMBER_WIDTH, HOW_TO_STEPS[i]);
			wraps.add(words);
			step.add(words, BorderLayout.CENTER);
			card.add(step);
		}
		// The note under the steps (the user's words, 2026-10-28): what hides the guide and what brings it back.
		card.add(Box.createVerticalStrut(STEP_GAP + 2));
		final WrapLabel note = new WrapLabel(0, HOW_TO_NOTE);
		note.setFont(FontManager.getDefaultFont().deriveFont(11f));
		note.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		note.setName(HOW_TO_NOTE_NAME);
		wraps.add(note);
		card.add(note);

		card.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				final Insets insets = card.getInsets();
				final int inner = card.getWidth() - insets.left - insets.right;
				if (inner <= 0)
				{
					return;
				}
				for (final WrapLabel wrap : wraps)
				{
					wrap.fit(inner);
				}
				card.revalidate();
			}
		});

		howTo = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		howTo.setOpaque(false);
		howTo.setAlignmentX(Component.LEFT_ALIGNMENT);
		howTo.setBorder(new EmptyBorder(0, 0, GAP, 0));
		howTo.add(card, BorderLayout.CENTER);
		howTo.setVisible(false);
		return howTo;
	}

	/** A 1 px rule of (77, 77, 77) across the how-to card. */
	private static JPanel rule()
	{
		final JPanel rule = new JPanel();
		rule.setBackground(new Color(77, 77, 77));
		rule.setPreferredSize(new Dimension(1, 1));
		rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		rule.setAlignmentX(Component.LEFT_ALIGNMENT);
		return rule;
	}

	/** Shows or hides the how-to; nothing of it is saved. */
	private void showHowTo(boolean on)
	{
		howToShown = on;
		howTo.setVisible(on);
		paintHelp();
		tipHolder.setBorder(new EmptyBorder(on ? TIP_ROOM - GAP : TIP_ROOM, 0, TIP_ROOM, 0));
		column.revalidate();
		column.repaint();
	}

	/** The lower half: the tab row over the kept chat's view. */
	private JPanel chatHalf()
	{
		chipRow.setName(CHIPS_NAME);
		chipRow.setOpaque(false);

		chatView.setName(VIEW_NAME);
		chatView.setEditable(false);
		chatView.setFocusable(false);
		chatView.setBackground(ColorScheme.DARK_GRAY_COLOR);
		chatView.setCaret(new StillCaret());
		chatScroll = new JScrollPane(chatView, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		chatScroll.setBorder(null);
		chatScroll.getVerticalScrollBar().setUnitIncrement(SCROLL_STEP);

		final JPanel half = new JPanel(new BorderLayout());
		half.setOpaque(false);
		half.add(chipRow, BorderLayout.NORTH);
		half.add(chatScroll, BorderLayout.CENTER);
		sized(half, HALF_MIN_HEIGHT);
		return half;
	}

	/** Both halves ask for the same height, so the split's first place follows its weight; the least each may be. */
	private static void sized(JComponent half, int minHeight)
	{
		half.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 2 * PADDING, HALF_HEIGHT));
		half.setMinimumSize(new Dimension(0, minHeight));
	}

	/**
	 * A grey link word, white under the mouse, with the hand cursor; a left press runs {@code action}. The mouse's way in
	 * and out is passed on to {@code hover} when there is one (a card's links keep the card hot).
	 */
	private static JLabel link(String text, Runnable action, MouseAdapter hover)
	{
		return pressable(text, FontManager.getDefaultFont().deriveFont(11f), ColorScheme.LIGHT_GRAY_COLOR, action, hover);
	}

	/** A word in {@code rest} colour, white under the mouse, with the hand cursor; a left press runs {@code action}. */
	private static JLabel pressable(String text, Font font, Color rest, Runnable action, MouseAdapter hover)
	{
		final JLabel link = label(text, font, rest);
		link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		link.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				link.setForeground(Color.WHITE);
				if (hover != null)
				{
					hover.mouseEntered(e);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				link.setForeground(rest);
				if (hover != null)
				{
					hover.mouseExited(e);
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger())
				{
					action.run();
				}
			}
		});
		return link;
	}

	private static JLabel label(String text, Font font, Color colour)
	{
		final JLabel label = new JLabel(text);
		label.setFont(font);
		label.setForeground(colour);
		return label;
	}

	/** Asks for one {@link #syncCards} on the event thread, unless one is already queued; may be called on any thread. */
	private void queueCardsSync()
	{
		if (cardsQueued.compareAndSet(false, true))
		{
			SwingUtilities.invokeLater(this::syncCards);
		}
	}

	/**
	 * Makes the column again from the ledger, newest first, the lit tab's mentions only (every one on All), keeping the
	 * panel of a mention already shown; sets the count to the cards shown, and shows the tip line in the column while the
	 * lit tab has none. Runs on the event thread.
	 *
	 * <p>Choice: the queued flag is let go before the ledger is read, so a change that comes while this runs asks for a
	 * sync of its own. The how-to goes only when the first card replaces the tip, so
	 * "How to use" in the menu shows it again beside cards until "Hide" or the next start.
	 */
	private void syncCards()
	{
		cardsQueued.set(false);
		final List<Mention> mentions = new ArrayList<>();
		for (final Mention m : feed.ledger().mentions())
		{
			if (shown == null || shown == Chat.ALL || m.chat() == shown)
			{
				mentions.add(m);
			}
		}
		final Map<Mention, JPanel> kept = new IdentityHashMap<>();
		while (column.getComponentCount() > FIXED_PARTS)
		{
			column.remove(FIXED_PARTS);
		}
		for (final Mention mention : mentions)
		{
			JPanel card = cardPanels.get(mention);
			if (card == null)
			{
				card = card(mention);
			}
			kept.put(mention, card);
			if (column.getComponentCount() > FIXED_PARTS)
			{
				column.add(gap());
			}
			column.add(card);
		}
		cardPanels.clear();
		cardPanels.putAll(kept);
		count.setText(mentions.size() + UNREAD_TEXT);
		if (mentions.isEmpty())
		{
			if (!tipShown)
			{
				drawTip();
			}
		}
		else if (tipShown && howToShown)
		{
			showHowTo(false);
		}
		tipShown = mentions.isEmpty();
		tipHolder.setVisible(mentions.isEmpty());
		clearLabel.setVisible(!mentions.isEmpty());
		helpIcon.setVisible(mentions.isEmpty());
		column.revalidate();
		column.repaint();
	}

	/**
	 * One card: who, the chat and the time; the line with its keywords in the mention colour; "Copy name" and "Mark
	 * read". A left press anywhere but on the links jumps the kept chat to the line.
	 *
	 * <p>Choice: the card's rows are STANDARD A4's {@code ROW_GAP} (6 px) apart.
	 */
	private JPanel card(Mention mention)
	{
		final JPanel card = new JPanel()
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		card.setName(CARD_NAME);
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(CARD_BORDER);
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		final MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				hot(card, true);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hot(card, card.contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), card)));
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger())
				{
					jumpTo(mention);
				}
			}
		};
		card.addMouseListener(mouse);

		final JPanel top = row();
		final Icon rank = mention.icon() >= 0 ? icons.icon(mention.icon()) : null;
		if (rank != null)
		{
			top.add(new JLabel(rank));
			top.add(Box.createHorizontalStrut(ICON_ROOM));
		}
		top.add(label(mention.who(), FontManager.getDefaultBoldFont().deriveFont(12f), Color.WHITE));
		top.add(Box.createHorizontalStrut(GAP));
		top.add(label(mention.chat().label(), FontManager.getDefaultFont().deriveFont(11f), ColorScheme.LIGHT_GRAY_COLOR));
		top.add(Box.createHorizontalGlue());
		top.add(label(Times.clock(mention.at(), ZoneId.systemDefault()), FontManager.getDefaultFont().deriveFont(10f),
			TIME_GREY));
		card.add(top);
		card.add(gap());

		final DefaultStyledDocument doc = new DefaultStyledDocument();
		writeSpans(doc, 0, mention.text(), mention.spans(), style(12, false, Color.WHITE),
			spanStyle());
		final CardText text = new CardText(column);
		text.setDocument(doc);
		text.addMouseListener(mouse);
		card.add(text);
		card.add(gap());

		final JPanel links = row();
		links.add(link(COPY_TEXT, () -> copyName.accept(mention.who()), mouse));
		links.add(Box.createHorizontalStrut(LINK_GAP));
		links.add(link(MARK_READ_TEXT, () -> feed.ledger().markRead(mention), mouse));
		links.add(Box.createHorizontalGlue());
		card.add(links);
		return card;
	}

	/** A card's ground and border: hot under the mouse, at rest otherwise. */
	private static void hot(JPanel card, boolean on)
	{
		card.setBackground(on ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(on ? CARD_HOT_BORDER : CARD_BORDER);
	}

	/** A see-through row of a card, left-aligned in it. */
	private static JPanel row()
	{
		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		return row;
	}

	/** The room between two cards or two rows of a card, left-aligned like them. */
	private static Component gap()
	{
		final JComponent gap = (JComponent) Box.createVerticalStrut(GAP);
		gap.setAlignmentX(Component.LEFT_ALIGNMENT);
		return gap;
	}

	/** The tabs again: All, then each watched chat, in the game's order; the lit one stays lit while it is there, else the first (All) is lit. */
	private void rebuildChips()
	{
		chipRow.removeAll();
		chips.clear();
		chipGroup = new ButtonGroup();
		Chat first = null;
		for (final Chat chat : Chat.values())
		{
			if (chat != Chat.ALL && !settings.watched(chat))
			{
				continue;
			}
			if (first == null)
			{
				first = chat;
			}
			final TabButton chip = new TabButton(chat.label());
			chip.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (SwingUtilities.isLeftMouseButton(e))
					{
						choose(chat, true);
					}
				}
			});
			chipGroup.add(chip);
			chips.put(chat, chip);
			chipRow.add(chip);
		}
		if (shown != null && chips.containsKey(shown))
		{
			chips.get(shown).setSelected(true);
		}
		else if (wanted != null && chips.containsKey(wanted))
		{
			show(wanted, true);
		}
		else if (first != null)
		{
			show(first, true);
		}
		wanted = null;
		chipRow.revalidate();
		chipRow.repaint();
	}

	/**
	 * Lights the tab of {@code chat} and shows its document; a chat newly shown is seen from its newest line when
	 * {@code fromNewest}.
	 *
	 * <p>Choice: a chat with no tab (unwatched since its card was made) is shown with no tab lit.
	 */
	private void show(Chat chat, boolean fromNewest)
	{
		chipGroup.clearSelection();
		final JToggleButton chip = chips.get(chat);
		if (chip != null)
		{
			chip.setSelected(true);
		}
		if (chat == shown)
		{
			return;
		}
		shown = chat;
		queueCardsSync();
		chatView.setDocument(documents.get(chat));
		chatScroll.validate();
		if (fromNewest)
		{
			toBottom();
		}
	}

	/** Asks for one {@link #syncChats} on the event thread for the chat, unless one is already queued; any thread. */
	private void queueChatSync(Chat chat)
	{
		synchronized (dirtyChats)
		{
			dirtyChats.add(chat);
		}
		if (chatsQueued.compareAndSet(false, true))
		{
			SwingUtilities.invokeLater(this::syncChats);
		}
	}

	/** Brings the document of every chat told of since the last sync up to date. Runs on the event thread. */
	private void syncChats()
	{
		chatsQueued.set(false);
		final List<Chat> dirty;
		synchronized (dirtyChats)
		{
			dirty = new ArrayList<>(dirtyChats);
			dirtyChats.clear();
		}
		for (final Chat chat : dirty)
		{
			syncChat(chat);
		}
	}

	/**
	 * Brings one chat's document up to date with its buffer: the lines that left the ring leave the front, the lines
	 * numbered above the last appended are added at the end; an empty buffer empties the document. The view follows the
	 * newest line when it was at the bottom.
	 */
	private void syncChat(Chat chat)
	{
		final List<ChatLine> lines = feed.buffer(chat).lines();
		final DefaultStyledDocument doc = documents.get(chat);
		final long[] range = held.get(chat);
		final JScrollBar bar = chatScroll.getVerticalScrollBar();
		final BiFunction<int[], String, AttributeSet> style = spanStyle();
		final boolean atBottom = bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 1;
		try
		{
			final long bufferFirst = lines.isEmpty() ? Long.MAX_VALUE : lines.get(0).seq();
			if (range[0] >= 0 && bufferFirst > range[0])
			{
				final long drop = bufferFirst - range[0];
				final Element root = doc.getDefaultRootElement();
				if (drop > range[1] - range[0] || drop >= root.getElementCount())
				{
					doc.remove(0, doc.getLength());
					range[0] = -1;
					range[1] = -1;
				}
				else
				{
					doc.remove(0, root.getElement((int) drop).getStartOffset());
					range[0] = bufferFirst;
				}
				if (chat == markedChat && markedSeq < bufferFirst)
				{
					markedChat = null;
					markedSeq = -1;
				}
			}
			for (final ChatLine line : lines)
			{
				if (line.seq() <= range[1])
				{
					continue;
				}
				append(doc, line, chat == Chat.ALL, style);
				if (range[0] < 0)
				{
					range[0] = line.seq();
				}
				range[1] = line.seq();
			}
		}
		catch (BadLocationException e)
		{
			// Every offset above is inside the document, so this does not come.
			return;
		}
		if (chat == shown && atBottom)
		{
			chatScroll.validate();
			toBottom();
		}
	}

	/** One kept line as the document's last paragraph: the clock, the chat's word when {@code withChat} (the All tab), then the sender and the text, or a broadcast in grey. */
	private void append(DefaultStyledDocument doc, ChatLine line, boolean withChat,
		BiFunction<int[], String, AttributeSet> style) throws BadLocationException
	{
		int at = doc.getLength();
		final AttributeSet text = style(12, false, Color.WHITE);
		if (at > 0)
		{
			doc.insertString(at, "\n", text);
			at++;
		}
		final String clock = Times.clock(line.at(), ZoneId.systemDefault()) + " ";
		doc.insertString(at, clock, style(10, false, TIME_GREY));
		at += clock.length();
		if (withChat)
		{
			final String word = line.chat().label() + " ";
			doc.insertString(at, word, style(10, false, TIME_GREY));
			at += word.length();
		}
		if (line.who().isEmpty())
		{
			doc.insertString(at, line.text(), style(12, false, ColorScheme.LIGHT_GRAY_COLOR));
			return;
		}
		final Icon rank = line.icon() >= 0 ? icons.icon(line.icon()) : null;
		if (rank != null)
		{
			final SimpleAttributeSet mark = new SimpleAttributeSet();
			StyleConstants.setIcon(mark, rank);
			doc.insertString(at, " ", mark);
			doc.insertString(at + 1, " ", text);
			at += 2;
		}
		doc.insertString(at, line.who(), style(12, true, Color.WHITE));
		at += line.who().length();
		doc.insertString(at, ": ", text);
		at += 2;
		writeSpans(doc, at, line.text(), feed.spans(line.text()), text, style);
	}

	/** Writes {@code text} at {@code at}, each span in the keyword style {@code keyword} gives it, the rest in the plain one. */
	private static void writeSpans(DefaultStyledDocument doc, int at, String text, List<int[]> spans, AttributeSet plain,
		BiFunction<int[], String, AttributeSet> keyword)
	{
		try
		{
			int from = 0;
			for (final int[] span : spans)
			{
				final int start = Math.max(from, Math.min(text.length(), span[0]));
				final int end = Math.max(start, Math.min(text.length(), span[1]));
				doc.insertString(at + from, text.substring(from, start), plain);
				doc.insertString(at + start, text.substring(start, end), keyword.apply(span, text.substring(start, end)));
				from = end;
			}
			doc.insertString(at + from, text.substring(from), plain);
		}
		catch (BadLocationException e)
		{
			// The offsets follow the text just written, so this does not come.
		}
	}

	private static AttributeSet keywordStyle(Color colour)
	{
		return style(12, true, colour);
	}

	/** The default face at {@code size}, bold or not, in {@code colour}. */
	private static AttributeSet style(int size, boolean bold, Color colour)
	{
		final SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setFontFamily(style, FontManager.getDefaultFont().getFamily());
		StyleConstants.setFontSize(style, size);
		StyleConstants.setBold(style, bold);
		StyleConstants.setForeground(style, colour);
		return style;
	}

	/**
	 * A card click: lights the card's chat, scrolls its line's paragraph to the middle of the view and marks it; a line
	 * that has left the ring scrolls the view to the top and marks nothing.
	 */
	private void jumpTo(Mention mention)
	{
		unmark();
		final Chat chat = mention.chat();
		choose(chat, false);
		final JViewport viewport = chatScroll.getViewport();
		final long[] range = held.get(chat);
		final long seq = mention.seq();
		if (range[0] < 0 || seq < range[0] || seq > range[1])
		{
			viewport.setViewPosition(new Point(0, 0));
			return;
		}
		final DefaultStyledDocument doc = documents.get(chat);
		final Element paragraph = doc.getDefaultRootElement().getElement((int) (seq - range[0]));
		final int start = paragraph.getStartOffset();
		final int end = Math.min(paragraph.getEndOffset(), doc.getLength());
		final SimpleAttributeSet mark = new SimpleAttributeSet();
		StyleConstants.setBackground(mark, MARK_COLOUR);
		doc.setCharacterAttributes(start, end - start, mark, false);
		markedChat = chat;
		markedSeq = seq;
		chatScroll.validate();
		try
		{
			final Rectangle2D at = chatView.modelToView2D(start);
			if (at != null)
			{
				final Dimension extent = viewport.getExtentSize();
				final int most = Math.max(0, viewport.getViewSize().height - extent.height);
				final int y = (int) (at.getY() + at.getHeight() / 2 - extent.height / 2.0);
				viewport.setViewPosition(new Point(0, Math.max(0, Math.min(most, y))));
			}
		}
		catch (BadLocationException e)
		{
			// The paragraph's start is inside the document, so this does not come.
		}
	}

	/** Takes the mark's ground off the paragraph marked last, when it is still held. */
	private void unmark()
	{
		final Chat chat = markedChat;
		final long seq = markedSeq;
		markedChat = null;
		markedSeq = -1;
		if (chat == null)
		{
			return;
		}
		final long[] range = held.get(chat);
		if (range[0] < 0 || seq < range[0] || seq > range[1])
		{
			return;
		}
		final DefaultStyledDocument doc = documents.get(chat);
		final Element paragraph = doc.getDefaultRootElement().getElement((int) (seq - range[0]));
		final int end = Math.min(paragraph.getEndOffset(), doc.getLength());
		int at = paragraph.getStartOffset();
		while (at < end)
		{
			final Element run = doc.getCharacterElement(at);
			final int runEnd = Math.max(at + 1, Math.min(end, run.getEndOffset()));
			final SimpleAttributeSet plain = new SimpleAttributeSet(run.getAttributes());
			plain.removeAttribute(StyleConstants.Background);
			doc.setCharacterAttributes(at, runEnd - at, plain, true);
			at = runEnd;
		}
	}

	/** Scrolls the kept chat's view to its newest line. */
	private void toBottom()
	{
		final JViewport viewport = chatScroll.getViewport();
		final int most = Math.max(0, viewport.getViewSize().height - viewport.getExtentSize().height);
		viewport.setViewPosition(new Point(0, most));
	}

	/**
	 * The (?): gold while the guide is hidden, white while it shows (the user, 2026-10-30), and the other colour under
	 * the mouse; a left press opens or closes the guide. Not {@link SupportLinks#namedIconButton}, whose two icons are
	 * fixed.
	 */
	private JLabel helpIcon()
	{
		final JLabel icon = new JLabel(HELP_GOLD);
		icon.setName(HELP_NAME);
		icon.setToolTipText(HELP_TIP);
		icon.setBorder(SupportLinks.iconBorder());
		icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		icon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger())
				{
					showHowTo(!howToShown);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				helpHovered = true;
				paintHelp();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				helpHovered = false;
				paintHelp();
			}
		});
		return icon;
	}

	/** Sets the (?)'s colour from whether the guide shows and whether the mouse is over it. */
	private void paintHelp()
	{
		helpIcon.setIcon(howToShown != helpHovered ? HELP_WHITE : HELP_GOLD);
	}

	/** The settings icon: grey at rest, orange under the mouse; a press opens the menu under it. */
	private JLabel settingsIcon()
	{
		final JLabel[] self = new JLabel[1];
		self[0] = SupportLinks.namedIconButton(GearGlyph.icon(ICON_SIZE, ColorScheme.LIGHT_GRAY_COLOR),
			GearGlyph.icon(ICON_SIZE, ColorScheme.BRAND_ORANGE), SupportLinks.SETTINGS_TIP, () -> openMenu(self[0]));
		self[0].setBorder(SupportLinks.iconBorder());
		return self[0];
	}

	/** Opens the menu under {@code anchor}; the press that closed it a moment ago opens nothing, so the icon toggles. */
	private void openMenu(JLabel anchor)
	{
		if (!anchor.isShowing() || System.nanoTime() - closedAtNanos < REOPEN_GUARD_NANOS)
		{
			return;
		}
		menu.show(anchor, 0, anchor.getHeight());
	}

	/** Takes the menu down: OK, a link press, or the reset. */
	private void closeMenu()
	{
		menu.setVisible(false);
	}

	/**
	 * The settings menu, in STANDARD A5's order: the header (focusable, so the menu's window takes the keyboard and no
	 * box holds a caret), a rule, the "Watch" caption and the six chat checks, a rule, the whole-words check and the Block what the chatbox blocks check, a rule,
	 * a rule, the "Colours" caption with its two indented rows ("Default keyword colour" with the swatch at the right
	 * end, "Reset all keyword colours"), a rule, the keywords box and the two number boxes, a rule, then the OK row.
	 */
	private JPopupMenu buildMenu()
	{
		final JPopupMenu popup = new JPopupMenu();
		popup.setBorder(new EmptyBorder(5, 5, 5, 5));
		final JPanel header = support.header();
		header.setFocusable(true);
		popup.add(header);
		popup.addSeparator();
		popup.add(howToRow());
		popup.addSeparator();
		popup.add(captionRow("Watch"));
		addWatch(popup, Chat.PUBLIC, "Public");
		addWatch(popup, Chat.PRIVATE, "Private");
		addWatch(popup, Chat.CHANNEL, "Channel");
		addWatch(popup, Chat.CLAN, "Clan");
		addWatch(popup, Chat.GAME, "Game");
		addWatch(popup, Chat.TRADE, "Trade");
		popup.addSeparator();
		wholeWordsCheck = check("Whole words only", on -> settings.setWholeWordsOnly(on));
		wholeWordsCheck.setToolTipText(WHOLE_WORDS_TIP);
		popup.add(wholeWordsCheck);
		followFilterCheck = check("Block what the chatbox blocks", on -> settings.setFollowChatFilter(on));
		followFilterCheck.setToolTipText(FOLLOW_FILTER_TIP);
		popup.add(followFilterCheck);
		popup.addSeparator();
		popup.add(captionRow(COLOURS_TEXT));
		popup.add(colourRow());
		popup.add(resetColoursRow());
		popup.addSeparator();
		popup.add(row("Keywords", keywordsBox()));
		cardsSpinner = spinner(CARDS_MIN, CARDS_MIN, CARDS_MAX, 1, 4, n -> settings.setCardsKept(n));
		popup.add(row("Cards kept", cardsSpinner));
		linesSpinner = spinner(LINES_MIN, LINES_MIN, LINES_MAX, LINES_STEP, 5, n -> settings.setLinesKept(n));
		popup.add(row("Chat lines kept", linesSpinner));
		popup.addSeparator();
		popup.add(okRow());
		popup.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				syncControls();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
				closedAtNanos = System.nanoTime();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e)
			{
				closedAtNanos = System.nanoTime();
			}
		});
		return popup;
	}

	private void addWatch(JPopupMenu popup, Chat chat, String text)
	{
		final JCheckBoxMenuItem item = check(text, on -> settings.setWatched(chat, on));
		watchChecks.put(chat, item);
		popup.add(item);
	}

	/** The see-through row holding one grey caption. */
	private static JPanel captionRow(String text)
	{
		final JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 6, 2, 6));
		final JLabel caption = new JLabel(text);
		caption.setFont(menuFont());
		caption.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(caption, BorderLayout.WEST);
		return row;
	}

	/** The ground a menu item takes under the mouse, from the look and feel; the pressable rows take the same. */
	static Color menuHover()
	{
		final Color laf = UIManager.getColor("MenuItem.selectionBackground");
		return laf != null ? laf : ColorScheme.DARK_GRAY_HOVER_COLOR;
	}

	/**
	 * Gives a pressable menu row the menu items' hover: its ground darkens while the mouse is over it, its caption
	 * included, so it reads as something to press (the user, 2026-10-28: "it isnt obvious you can click it").
	 *
	 * <p>Choice: leaving the row for its own caption fires an exit on the row; the ground goes only when the mouse is
	 * outside the row itself.
	 */
	private static void hoverLikeAMenuItem(JPanel row, JLabel caption)
	{
		row.setBackground(menuHover());
		final MouseAdapter hover = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setOpaque(true);
				row.repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (!row.contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), row)))
				{
					row.setOpaque(false);
					row.repaint();
				}
			}
		};
		row.addMouseListener(hover);
		caption.addMouseListener(hover);
	}

	/** The menu's "How to use" row: a pressable caption that takes the menu down and shows the how-to. */
	private JPanel howToRow()
	{
		final JPanel row = captionRow(HOW_TO_TEXT);
		final JLabel caption = (JLabel) row.getComponent(0);
		hoverLikeAMenuItem(row, caption);
		// White at rest like every row a player can press (the user, 2026-10-28); grey is for the captions alone.
		caption.setForeground(Color.WHITE);
		row.setName(HOW_TO_ROW_NAME);
		row.setToolTipText(HOW_TO_TIP);
		caption.setToolTipText(HOW_TO_TIP);
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		final MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger())
				{
					closeMenu();
					showHowTo(true);
				}
			}
		};
		row.addMouseListener(mouse);
		caption.addMouseListener(mouse);
		return row;
	}

	/**
	 * The "Default keyword colour" row: white words indented under "Colours", the swatch at the right end.
	 *
	 * <p>Choice: the 30 px is counted from the menu's left edge, so the row's own inset is 25 (the menu's border is 5).
	 */
	private JPanel colourRow()
	{
		final JPanel row = row(DEFAULT_COLOUR_TEXT, swatch());
		row.setName(KEYWORD_COLOUR_ROW_NAME);
		row.setBorder(new EmptyBorder(2, COLOURS_INDENT, 2, 6));
		((JLabel) row.getComponent(0)).setForeground(Color.WHITE);
		return row;
	}

	/** The "Reset all keyword colours" row: a pressable white caption that forgets every word's colour; the menu stays open. */
	private JPanel resetColoursRow()
	{
		final JPanel row = captionRow(RESET_COLOURS_TEXT);
		final JLabel caption = (JLabel) row.getComponent(0);
		hoverLikeAMenuItem(row, caption);
		caption.setForeground(Color.WHITE);
		row.setName(KEYWORD_CLEAR_NAME);
		row.setBorder(new EmptyBorder(2, COLOURS_INDENT, 2, 6));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		final MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger())
				{
					settings.setKeywordColours("");
				}
			}
		};
		row.addMouseListener(mouse);
		caption.addMouseListener(mouse);
		return row;
	}

	/**
	 * The keyword colour's swatch: a 16 px label, opaque, on the colour in force, with a 1 px edge and the hand cursor. A
	 * left press takes the menu down and opens the picker; the picker's changes and its close both write the colour.
	 *
	 * <p>Choice: nothing reopens the menu when the picker closes; the player opens it again from the settings icon.
	 */
	private JLabel swatch()
	{
		keywordSwatch = new JLabel();
		keywordSwatch.setName(KEYWORD_SWATCH_NAME);
		keywordSwatch.setOpaque(true);
		keywordSwatch.setBackground(settings.keywordColour());
		keywordSwatch.setBorder(new MatteBorder(1, 1, 1, 1, STRIP_EDGE));
		keywordSwatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		final Dimension size = new Dimension(SWATCH_SIZE, SWATCH_SIZE);
		keywordSwatch.setPreferredSize(size);
		keywordSwatch.setMinimumSize(size);
		keywordSwatch.setMaximumSize(size);
		keywordSwatch.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger())
				{
					final Point at = e.getLocationOnScreen();
					closeMenu();
					pickDefaultColour(at);
				}
			}
		});
		return keywordSwatch;
	}

	/**
	 * Opens the colour picker on the default keyword colour, at {@code at}; each pick and the close write the colour.
	 * The swatch and the name chip share it.
	 *
	 * <p>Choice: it holds {@code pickerOpen} until the close, so a second click on any chip opens nothing more.
	 */
	private void pickDefaultColour(Point at)
	{
		final Consumer<Color> write = settings::setKeywordColour;
		final Consumer<Color> close = colour ->
		{
			pickerOpen = false;
			write.accept(colour);
		};
		pickerOpen = true;
		try
		{
			picker.pick(this, DEFAULT_COLOUR_TEXT, settings.keywordColour(), write, close, at);
		}
		catch (RuntimeException e)
		{
			pickerOpen = false;
			throw e;
		}
	}

	/** The keywords box: 14 columns of the menu's face, writing the setting on every edit, as typed. */
	private JTextField keywordsBox()
	{
		keywordsBox = new JTextField(14);
		keywordsBox.setFont(menuFont());
		typesOnPress(keywordsBox);
		keywordsBox.setMaximumSize(keywordsBox.getPreferredSize());
		keywordsBox.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				typed();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				typed();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				typed();
			}

			private void typed()
			{
				if (!syncing)
				{
					settings.setKeywords(keywordsBox.getText());
				}
			}
		});
		return keywordsBox;
	}

	/**
	 * The keyword strip: a card with the name chip and one chip per keyword over the add field.
	 *
	 * <p>Choice: the chips' row has a border of minus four px all round, which takes back the room the wrapping flow
	 * leaves at its edges, so the first chip stands flush with the field's edge; the add row is at least 22 px tall, so a
	 * strip of one line of chips is 57 px.
	 */
	private JPanel keywordStrip()
	{
		final JPanel strip = new JPanel();
		strip.setName(STRIP_NAME);
		strip.setLayout(new BoxLayout(strip, BoxLayout.Y_AXIS));
		// The strip on the panel's own colour inside a 1 px rim, so it never reads as one more card (the user, 2026-10-29).
		strip.setBackground(WELL);
		strip.setBorder(new CompoundBorder(new MatteBorder(1, 1, 1, 1, WELL_RIM),
			new EmptyBorder(STRIP_PADDING - 1, STRIP_PADDING - 1, STRIP_PADDING - 1, STRIP_PADDING - 1)));

		keywordChips = new JPanel(new WrapLayout(KEYWORD_GAP))
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		keywordChips.setName(KEYWORD_CHIPS_NAME);
		keywordChips.setOpaque(false);
		keywordChips.setBorder(new EmptyBorder(-KEYWORD_GAP, -KEYWORD_GAP, -KEYWORD_GAP, -KEYWORD_GAP));
		keywordChips.setAlignmentX(Component.LEFT_ALIGNMENT);
		strip.add(keywordChips);
		strip.add(Box.createVerticalStrut(STRIP_ROWS_GAP));
		strip.add(addRow());
		return strip;
	}

	/** The add row: one framed row, the field in the centre and the "+" at its right end. */
	private JPanel addRow()
	{
		final JPanel row = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getPreferredSize()
			{
				final Dimension size = super.getPreferredSize();
				return new Dimension(size.width, Math.max(size.height, ADD_ROW_HEIGHT));
			}

			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new MatteBorder(1, 1, 1, 1, STRIP_EDGE));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		keywordField = new HintField();
		keywordField.setName(KEYWORD_FIELD_NAME);
		keywordField.setFont(FontManager.getDefaultFont().deriveFont(11f));
		keywordField.setForeground(Color.WHITE);
		keywordField.setCaretColor(Color.WHITE);
		keywordField.setOpaque(true);
		keywordField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		keywordField.setBorder(new EmptyBorder(2, 5, 2, 5));
		typesOnPress(keywordField);
		keywordField.addActionListener(e -> addKeywords());
		row.add(keywordField, BorderLayout.CENTER);

		final JLabel add = pressable(ADD_TEXT, FontManager.getDefaultBoldFont().deriveFont(12f), ColorScheme.LIGHT_GRAY_COLOR,
			this::addKeywords, null);
		add.setName(KEYWORD_ADD_NAME);
		add.setBorder(new EmptyBorder(0, 6, 0, 6));
		row.add(add, BorderLayout.EAST);
		return row;
	}

	/** The strip's chips again: the name chip, then one chip per keyword of the setting, in its order. */
	private void rebuildStrip()
	{
		keywordChips.removeAll();
		final String own = feed.playerName();
		final JLabel name = label(own.isEmpty() ? NAME_CHIP_FALLBACK : own, FontManager.getDefaultFont().deriveFont(11f),
			settings.keywordColour());
		name.setName(KEYWORD_NAME_NAME);
		name.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		name.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1 && !pickerOpen)
				{
					pickDefaultColour(e.getLocationOnScreen());
				}
			}
		});
		name.setBorder(new CompoundBorder(new MatteBorder(1, 1, 1, 1, STRIP_EDGE), new EmptyBorder(1, 3, 1, 3)));
		name.setPreferredSize(new Dimension(name.getPreferredSize().width, KEYWORD_CHIP_HEIGHT));
		keywordChips.add(name);
		for (final String word : keywordsOfSetting())
		{
			keywordChips.add(keywordChip(word));
		}
		keywordChips.revalidate();
		keywordChips.repaint();
	}

	/** One keyword chip: the word, then a cross that takes the word out of the setting; a single click on the word or the ground opens its colour picker. */
	private JPanel keywordChip(String word)
	{
		final JPanel chip = new JPanel();
		chip.setName(KEYWORD_CHIP_NAME);
		chip.setLayout(new BoxLayout(chip, BoxLayout.X_AXIS));
		chip.setBackground(KEYWORD_CHIP_GROUND);
		chip.add(Box.createHorizontalStrut(4));
		// The word in its own colour, else the default keyword colour: the chip shows the colour the chat paints.
		final Color own = colours().colour(word);
		final JLabel text = label(word, FontManager.getDefaultFont().deriveFont(11f),
			own != null ? own : settings.keywordColour());
		chip.add(text);
		chip.add(Box.createHorizontalStrut(3));
		final JLabel remove = pressable(REMOVE_TEXT, FontManager.getDefaultFont().deriveFont(11f), FAINT_GREY,
			() -> removeKeyword(word), null);
		remove.setName(KEYWORD_REMOVE_NAME);
		chip.add(remove);
		chip.add(Box.createHorizontalStrut(5));
		chip.setPreferredSize(new Dimension(chip.getPreferredSize().width, KEYWORD_CHIP_HEIGHT));
		chip.setMaximumSize(chip.getPreferredSize());
		final MouseAdapter single = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1 && !pickerOpen)
				{
					pickWordColour(word, e.getLocationOnScreen());
				}
			}
		};
		chip.addMouseListener(single);
		text.addMouseListener(single);
		return chip;
	}

	/**
	 * Opens the colour picker on a word's colour, or on the keyword colour when it has none; each pick and the close write
	 * the word's colour.
	 */
	private void pickWordColour(String word, Point at)
	{
		final Color own = colours().colour(word);
		final Consumer<Color> write = colour -> settings.setKeywordColours(colours().with(word, colour).setting());
		final Consumer<Color> close = colour ->
		{
			pickerOpen = false;
			write.accept(colour);
		};
		pickerOpen = true;
		try
		{
			picker.pick(this, word, own != null ? own : settings.keywordColour(), write, close, at);
		}
		catch (RuntimeException e)
		{
			pickerOpen = false;
			throw e;
		}
	}

	/** The words of the keywords setting, in its order, cleaned the way the feed cleans them. */
	private List<String> keywordsOfSetting()
	{
		return Keywords.of(settings.keywords(), false, null).words();
	}

	/** Writes the setting without {@code word}. */
	private void removeKeyword(String word)
	{
		final List<String> rest = new ArrayList<>();
		for (final String have : keywordsOfSetting())
		{
			if (!have.equalsIgnoreCase(word))
			{
				rest.add(have);
			}
		}
		settings.setKeywords(String.join(", ", rest));
		final WordColours had = colours();
		if (!had.isEmpty() && had.colour(word) != null)
		{
			settings.setKeywordColours(had.without(word).setting());
		}
	}

	/**
	 * Adds the words typed in the field to the setting, once, after the words it holds; a word the setting already has
	 * (ignoring case) is left out, and the field is emptied whatever came of it.
	 */
	private void addKeywords()
	{
		final List<String> all = new ArrayList<>(keywordsOfSetting());
		final int before = all.size();
		for (final String word : Keywords.of(keywordField.getText(), false, null).words())
		{
			boolean have = false;
			for (final String held : all)
			{
				have |= held.equalsIgnoreCase(word);
			}
			if (!have)
			{
				all.add(word);
			}
		}
		keywordField.setText("");
		if (all.size() > before)
		{
			settings.setKeywords(String.join(", ", all));
		}
	}

	/** Sets every control from the settings; the guard keeps the sync from writing anything back. */
	private void syncControls()
	{
		syncing = true;
		try
		{
			for (final Map.Entry<Chat, JCheckBoxMenuItem> entry : watchChecks.entrySet())
			{
				entry.getValue().setSelected(settings.watched(entry.getKey()));
			}
			wholeWordsCheck.setSelected(settings.wholeWordsOnly());
			followFilterCheck.setSelected(settings.followChatFilter());
			syncSwatch();
			final String words = settings.keywords();
			final String shown = words == null ? "" : words;
			if (!shown.equals(keywordsBox.getText()))
			{
				keywordsBox.setText(shown);
			}
			cardsSpinner.setValue(Math.max(CARDS_MIN, Math.min(CARDS_MAX, settings.cardsKept())));
			linesSpinner.setValue(Math.max(LINES_MIN, Math.min(LINES_MAX, settings.linesKept())));
		}
		finally
		{
			syncing = false;
		}
	}

	/** The menu's 12 px face. */
	private static Font menuFont()
	{
		return FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE);
	}

	/**
	 * A switch: a check in the menu's 12 px type, white, writing its state when clicked. Choice: the release is taken
	 * over so a click flips it and the menu stays open.
	 */
	private JCheckBoxMenuItem check(String text, Consumer<Boolean> write)
	{
		final JCheckBoxMenuItem check = new JCheckBoxMenuItem(text)
		{
			@Override
			protected void processMouseEvent(MouseEvent e)
			{
				if (!takeRelease(this, e))
				{
					super.processMouseEvent(e);
				}
			}
		};
		check.setFont(menuFont());
		check.setForeground(Color.WHITE);
		check.addActionListener(e ->
		{
			if (!syncing)
			{
				write.accept(check.isSelected());
			}
		});
		return check;
	}

	/** Takes a left release inside {@code item} over: clicks it and keeps Swing from closing the menu. */
	private static boolean takeRelease(JMenuItem item, MouseEvent e)
	{
		if (e.getID() != MouseEvent.MOUSE_RELEASED || !SwingUtilities.isLeftMouseButton(e) || !item.contains(e.getPoint()))
		{
			return false;
		}
		item.doClick(0);
		// The click leaves the item un-armed; arming it again keeps its hover highlight while the mouse stays on it.
		item.getModel().setArmed(true);
		e.consume();
		return true;
	}

	/** A number box from {@code min} to {@code max}, {@code columns} wide, writing as typed. */
	private JSpinner spinner(int value, int min, int max, int step, int columns, IntConsumer write)
	{
		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
		spinner.setFont(menuFont());
		final JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
		editor.getTextField().setFont(menuFont());
		editor.getTextField().setColumns(columns);
		if (editor.getTextField().getFormatter() instanceof DefaultFormatter)
		{
			((DefaultFormatter) editor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		}
		typesOnPress(editor.getTextField());
		spinner.setMaximumSize(spinner.getPreferredSize());
		spinner.addChangeListener(e ->
		{
			if (!syncing)
			{
				write.accept(((Number) spinner.getValue()).intValue());
			}
		});
		return spinner;
	}

	/** A row of the menu: the grey caption at its left, the control at its right; see-through. */
	private static JPanel row(String captionText, JComponent control)
	{
		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 6, 2, 6));
		final JLabel caption = new JLabel(captionText);
		caption.setFont(menuFont());
		caption.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(caption);
		row.add(Box.createHorizontalStrut(ROW_CAPTION_GAP));
		row.add(Box.createHorizontalGlue());
		row.add(control);
		return row;
	}

	/** Not focusable until pressed, so no box holds a caret when the menu opens; a press takes the keyboard. */
	private static void typesOnPress(JComponent part)
	{
		part.setFocusable(false);
		part.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					takeKeyboard(part);
				}
			}
		});
		part.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				part.setFocusable(false);
			}
		});
	}

	/** Makes the typing part focusable and asks for the keyboard for it. */
	private static void takeKeyboard(JComponent part)
	{
		part.setFocusable(true);
		part.requestFocus();
	}

	/** The menu's last row: "Reset to default" at its left, "OK" at its right. */
	private JPanel okRow()
	{
		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 6, 2, 6));
		final JButton reset = button(RESET_TEXT);
		reset.addActionListener(e -> resetToDefault.run());
		final JButton ok = button(OK_TEXT);
		ok.addActionListener(e -> closeMenu());
		row.add(reset);
		row.add(Box.createHorizontalGlue());
		row.add(ok);
		return row;
	}

	/** A small button in the menu's 12 px type, no wider than its word. */
	private static JButton button(String text)
	{
		final JButton button = new JButton(text);
		button.setFont(FontManager.getDefaultFont().deriveFont(12f));
		button.setFocusable(false);
		button.setMaximumSize(button.getPreferredSize());
		return button;
	}

	/**
	 * The column of cards: as wide as the scroll pane's view, so a card wraps its line and the horizontal bar stays away
	 * (the model's {@code HistoryList.getScrollableTracksViewportWidth}). A change of its width asks for a layout again,
	 * since the cards' heights follow it.
	 */
	private static final class CardColumn extends JPanel implements Scrollable
	{
		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return SCROLL_STEP;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return Math.max(SCROLL_STEP, visibleRect.height - SCROLL_STEP);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}

		@Override
		public void setBounds(int x, int y, int width, int height)
		{
			final boolean widthChanges = width != getWidth();
			super.setBounds(x, y, width, height);
			if (widthChanges)
			{
				revalidate();
			}
		}
	}

	/**
	 * A card's line: non-editable, non-focusable, see-through, wrapping to the card's width, which it reads off the
	 * column (the card's own width is not yet set when the column asks for its height).
	 */
	private static final class CardText extends JTextPane
	{
		private final JPanel column;
		/** The width the height was measured for, and the size it gave; the document never changes after the card is made. */
		private int measuredFor = -1;
		private Dimension measured;

		CardText(JPanel column)
		{
			this.column = column;
			setEditable(false);
			setFocusable(false);
			setOpaque(false);
			setBorder(null);
			setCaret(new StillCaret());
			setHighlighter(null);
			setAlignmentX(Component.LEFT_ALIGNMENT);
		}

		/** The height the text takes wrapped at the card's inner width, once the column has a width. */
		@Override
		public Dimension getPreferredSize()
		{
			final Container card = getParent();
			final int width = card == null ? 0 : column.getWidth() - card.getInsets().left - card.getInsets().right;
			if (width <= 0)
			{
				return super.getPreferredSize();
			}
			if (width != measuredFor || measured == null)
			{
				final View root = getUI().getRootView(this);
				root.setSize(width, Integer.MAX_VALUE);
				measured = new Dimension(width, (int) Math.ceil(root.getPreferredSpan(View.Y_AXIS)));
				measuredFor = width;
			}
			return new Dimension(measured);
		}
	}

	/**
	 * The how-to's wrapped words: a 12 px white HTML label whose text carries a body width, set by {@link #fit} from the
	 * card's inner width less its indent, so the browser-style layout wraps the words to the card.
	 *
	 * <p>Choice: a width that changes by less than 1 px does nothing; before the first fit the label holds the plain words.
	 */
	private static final class WrapLabel extends JLabel
	{
		private final int indent;
		private final String words;
		private int fitted = -1;

		WrapLabel(int indent, String words)
		{
			super(words);
			this.indent = indent;
			this.words = words;
			setFont(FontManager.getDefaultFont().deriveFont(12f));
			setForeground(Color.WHITE);
			setVerticalAlignment(SwingConstants.TOP);
			setAlignmentX(Component.LEFT_ALIGNMENT);
		}

		/** Sets the label's text for a card whose inner width is {@code width}; the same width sets nothing again. */
		void fit(int width)
		{
			final int room = width - indent;
			if (room <= 0 || room == fitted)
			{
				return;
			}
			fitted = room;
			// Swing's HTML renderer scales a width given in px by 96/72 (its px are 1.3 of its points); a width in
			// pt is taken as the pixels asked for, so the lines fold inside the card.
			setText("<html><body style='width:" + room + "pt'>" + escape(words) + "</body></html>");
		}

		private static String escape(String text)
		{
			return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}
	}

	/** A caret that never scrolls its text: the kept chat's view stays where a card click put it. */
	private static final class StillCaret extends DefaultCaret
	{
		StillCaret()
		{
			setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		}

		@Override
		protected void adjustVisibility(Rectangle nloc)
		{
			// Never scrolls.
		}
	}

	/** A left-aligned flow that wraps to further lines and asks for the height they take (the tab row, the chips). */
	private static final class WrapLayout extends FlowLayout
	{
		WrapLayout(int gap)
		{
			super(FlowLayout.LEFT, gap, gap);
		}

		@Override
		public Dimension preferredLayoutSize(Container target)
		{
			return wrapped(target, true);
		}

		@Override
		public Dimension minimumLayoutSize(Container target)
		{
			return wrapped(target, false);
		}

		/** The size of the parts set in lines no wider than the room the row has (its parent's width once laid out). */
		private Dimension wrapped(Container target, boolean preferred)
		{
			synchronized (target.getTreeLock())
			{
				final Container parent = target.getParent();
				int room = parent != null && parent.getWidth() > 0
					? parent.getWidth() - parent.getInsets().left - parent.getInsets().right : target.getWidth();
				if (room <= 0)
				{
					room = Integer.MAX_VALUE;
				}
				final Insets insets = target.getInsets();
				final int most = room == Integer.MAX_VALUE ? room
					: room - insets.left - insets.right - 2 * getHgap();
				int width = 0;
				int height = 0;
				int lineWidth = 0;
				int lineHeight = 0;
				for (final Component part : target.getComponents())
				{
					if (!part.isVisible())
					{
						continue;
					}
					final Dimension size = preferred ? part.getPreferredSize() : part.getMinimumSize();
					if (lineWidth > 0 && lineWidth + getHgap() + size.width > most)
					{
						width = Math.max(width, lineWidth);
						height += lineHeight + getVgap();
						lineWidth = 0;
						lineHeight = 0;
					}
					lineWidth += (lineWidth > 0 ? getHgap() : 0) + size.width;
					lineHeight = Math.max(lineHeight, size.height);
				}
				width = Math.max(width, lineWidth);
				height += lineHeight;
				return new Dimension(width + insets.left + insets.right + 2 * getHgap(),
					height + insets.top + insets.bottom + 2 * getVgap());
			}
		}
	}

	/**
	 * One chat tab: its word in the default face at 11 px, the lit one in the brand orange with a 2 px bar of the same
	 * orange under the word, the others grey (white under the mouse). It paints itself whatever look and feel is on and
	 * takes no keyboard.
	 */
	private static final class TabButton extends JToggleButton
	{
		private static final int PAD_X = 2;
		private static final int PAD_TOP = 2;
		private static final int BAR_GAP = 1;
		private static final int BAR_HEIGHT = 2;
		private static final int PAD_BOTTOM = 2;

		private boolean under;

		TabButton(String words)
		{
			super(words);
			setFont(FontManager.getDefaultFont().deriveFont(11f));
			setFocusable(false);
			setOpaque(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					under = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					under = false;
					repaint();
				}
			});
		}

		/** The word with its padding, the bar and the room under it. */
		@Override
		public Dimension getPreferredSize()
		{
			final FontMetrics metrics = getFontMetrics(getFont());
			return new Dimension(metrics.stringWidth(getText()) + 2 * PAD_X,
				PAD_TOP + metrics.getHeight() + BAR_GAP + BAR_HEIGHT + PAD_BOTTOM);
		}

		/** The whole tab: its word and, when lit, the bar under it. */
		@Override
		protected void paintComponent(Graphics g)
		{
			final boolean on = isSelected();
			g.setFont(getFont());
			g.setColor(on ? ColorScheme.BRAND_ORANGE : under ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
			final FontMetrics metrics = g.getFontMetrics();
			g.drawString(getText(), PAD_X, PAD_TOP + metrics.getAscent());
			if (on)
			{
				g.fillRect(PAD_X, PAD_TOP + metrics.getHeight() + BAR_GAP, getWidth() - 2 * PAD_X, BAR_HEIGHT);
			}
		}
	}

	/** A text field that paints its hint in grey where the words would start, while it is empty and has no focus. */
	private static final class HintField extends JTextField
	{
		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if (getText().isEmpty() && !hasFocus())
			{
				g.setFont(getFont());
				g.setColor(FAINT_GREY);
				final FontMetrics metrics = g.getFontMetrics();
				g.drawString(HINT_TEXT, getInsets().left, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
			}
		}
	}
}

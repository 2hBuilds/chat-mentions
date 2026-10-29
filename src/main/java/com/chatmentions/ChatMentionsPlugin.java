package com.chatmentions;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 2h Chat Mentions: keeps every line of the watched chats and makes a card of each mention, with the fifteen settings
 * working in RuneLite's config panel and in the sidebar's menu, and shows the cards over the kept chat in the sidebar.
 */
@PluginDescriptor(
	name = "2h Chat Mentions",
	internalName = "chat-mentions",
	description = "Catch up after being afk: chat lines that name you or a keyword become cards in the sidebar. (v" + Version.CURRENT + ")",
	tags = {"clan", "friends", "fc", "public", "private", "pm", "highlight", "ping", "away", "missed", "log", "history"}
)
public class ChatMentionsPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(ChatMentionsPlugin.class);

	/** The sidebar button's place. */
	private static final int NAV_PRIORITY = 7;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Client client;

	@Inject
	private ChatMentionsConfig config;

	@Inject
	private ColorPickerManager colorPickerManager;

	private NavigationButton navButton;
	private volatile MentionFeed feed;
	private volatile ChatMentionsPanel panel;
	private volatile boolean nameWanted;
	private volatile ChatFilterRules filterRules;
	private final RankIcons icons = new RankIcons();

	@Provides
	ChatMentionsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChatMentionsConfig.class);
	}

	@Override
	protected void startUp()
	{
		feed = new MentionFeed(config.linesKept(), config.cardsKept());
		applySettings();
		filterRules = readFilterRules();
		nameWanted = true;
		final PanelSettings settings = new PanelSettings()
		{
			@Override
			public String keywords()
			{
				return config.keywords();
			}

			@Override
			public void setKeywords(String words)
			{
				write("keywords", words);
			}

			@Override
			public boolean wholeWordsOnly()
			{
				return config.wholeWordsOnly();
			}

			@Override
			public void setWholeWordsOnly(boolean on)
			{
				write("wholeWordsOnly", on);
			}

			@Override
			public boolean followChatFilter()
			{
				return config.followChatFilter();
			}

			@Override
			public void setFollowChatFilter(boolean on)
			{
				write("followChatFilter", on);
			}

			@Override
			public boolean watched(Chat chat)
			{
				return ChatMentionsPlugin.this.watched(chat);
			}

			@Override
			public void setWatched(Chat chat, boolean on)
			{
				final String key = watchKey(chat);
				if (key != null)
				{
					write(key, on);
				}
			}

			@Override
			public int cardsKept()
			{
				return config.cardsKept();
			}

			@Override
			public void setCardsKept(int n)
			{
				write("cardsKept", n);
			}

			@Override
			public int linesKept()
			{
				return config.linesKept();
			}

			@Override
			public void setLinesKept(int n)
			{
				write("linesKept", n);
			}

			@Override
			public Color keywordColour()
			{
				return config.defaultKeywordColour();
			}

			@Override
			public void setKeywordColour(Color colour)
			{
				write("defaultKeywordColour", colour);
			}

			@Override
			public String keywordColours()
			{
				return config.keywordColours();
			}

			@Override
			public void setKeywordColours(String setting)
			{
				write("keywordColours", setting);
			}

			@Override
			public int cardsShare()
			{
				return config.cardsShare();
			}

			@Override
			public void setCardsShare(int percent)
			{
				write("cardsShare", percent);
			}

			@Override
			public String shownTab()
			{
				return config.shownTab();
			}

			@Override
			public void setShownTab(String name)
			{
				write("shownTab", name);
			}

			private void write(String key, Object value)
			{
				configManager.setConfiguration(ChatMentionsConfig.GROUP, key, value);
			}
		};
		panel = new ChatMentionsPanel(this::resetToDefault, settings, (parent, title, current, onChange, onClose, at) ->
		{
			final Window owner = SwingUtilities.windowForComponent(parent);
			final RuneliteColorPicker colourPicker = colorPickerManager.create(owner, current, title, true);
			colourPicker.setOnColorChange(onChange);
			colourPicker.setOnClose(onClose);
			// Owned by the client's window, the picker inherits its always-on-top state, so it never pins itself.
			colourPicker.setLocation(ScreenPlace.near(at, colourPicker.getSize(), screenOf(at)));
			colourPicker.setVisible(true);
		}, feed, icons, ChatMentionsPlugin::copyToClipboard);
		navButton = NavigationButton.builder()
			.tooltip("2h Chat Mentions")
			.icon(NavIcon.create())
			.priority(NAV_PRIORITY)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		log.info("2h Chat Mentions {} started on RuneLite {}", Version.CURRENT, RuneLiteProperties.getVersion());
	}

	@Override
	protected void shutDown()
	{
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		final MentionFeed old = feed;
		if (old != null)
		{
			old.clear();
		}
		feed = null;
		filterRules = null;
		panel = null;
		nameWanted = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			nameWanted = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		final MentionFeed current = feed;
		if (!nameWanted || current == null)
		{
			return;
		}
		final Player player = client.getLocalPlayer();
		final String name = player == null ? null : player.getName();
		if (name != null && !name.isEmpty())
		{
			current.playerName(name);
			nameWanted = false;
			final ChatMentionsPanel named = panel;
			if (named != null)
			{
				SwingUtilities.invokeLater(named::settingsChanged);
			}
			log.debug("The player's name is known");
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		final MentionFeed current = feed;
		if (current == null)
		{
			return;
		}
		icons.capture(RankIcons.indexIn(event.getName()), client.getModIcons());
		final Widget publicFilter = client.getWidget(InterfaceID.Chatbox.CHAT_PUBLIC_FILTER);
		final PublicSetting setting = PublicSetting.of(publicFilter == null ? null : publicFilter.getText());
		if (feedLine(current, filterRules, setting, event.getType(), event.getName(), event.getMessage(), System.currentTimeMillis()))
		{
			log.debug("A mention card was made in the {} chat", Chat.of(event.getType()).label());
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (feed == null)
		{
			return;
		}
		if (ChatFilterRules.GROUP.equals(event.getGroup())
			|| (ChatFilterRules.PLUGIN_GROUP.equals(event.getGroup()) && ChatFilterRules.PLUGIN_KEY.equals(event.getKey())))
		{
			filterRules = readFilterRules();
			return;
		}
		if (!ChatMentionsConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		filterRules = readFilterRules();
		applySettings();
		final ChatMentionsPanel current = panel;
		if (current != null)
		{
			SwingUtilities.invokeLater(current::settingsChanged);
		}
	}

	/**
	 * Hands one chat line to the feed, after the game's Public setting and Chat Filter's rules: a line either removes is
	 * not kept and makes no card, and a censored one is kept as censored. The Public setting applies only while the
	 * tick is on.
	 * Choice: package-private static so the plugin's own call is also what the test reads.
	 *
	 * @param rules the rules in force, or null for none
	 * @param setting the game's Public setting as the chatbox shows it
	 * @return whether the line made a card
	 */
	static boolean feedLine(MentionFeed feed, ChatFilterRules rules, PublicSetting setting, ChatMessageType type, String name, String message, long now)
	{
		String kept = message;
		if (rules != null)
		{
			if (rules.follows() && setting != null
				&& !setting.keeps(type, rules.fromYou(name), rules.fromFriend(name)))
			{
				return false;
			}
			kept = rules.apply(type, name, message);
			if (kept == null)
			{
				return false;
			}
		}
		return feed.line(type, name, kept, now);
	}

	/** Rebuilds the Chat Filter rules from Chat Filter's settings and this plugin's own switch. */
	private ChatFilterRules readFilterRules()
	{
		return new ChatFilterRules((group, key) -> configManager.getConfiguration(group, key), config.followChatFilter(),
			new ChatFilterRules.Who()
			{
				@Override
				public boolean isLocalPlayer(String name)
				{
					final Player player = client.getLocalPlayer();
					return player != null && player.getName() != null
						&& Text.standardize(player.getName()).equals(Text.standardize(name));
				}

				@Override
				public boolean isFriend(String name)
				{
					return client.isFriended(name, false);
				}

				@Override
				public boolean isFriendsChatMember(String name)
				{
					return client.getFriendsChatManager() != null && client.getFriendsChatManager().findByName(name) != null;
				}

				@Override
				public boolean isClanMember(String name)
				{
					final ClanChannel clan = client.getClanChannel();
					final ClanChannel guest = client.getGuestClanChannel();
					return (clan != null && clan.findMember(name) != null) || (guest != null && guest.findMember(name) != null);
				}
			});
	}

	/**
	 * The feed's partial flag for the whole-words setting: the inverse.
	 * Choice: package-private static so the plugin's own call is also what the test reads.
	 */
	static boolean partialOf(boolean wholeWordsOnly)
	{
		return !wholeWordsOnly;
	}

	/** Hands the config's values to the feed. */
	private void applySettings()
	{
		final MentionFeed current = feed;
		if (current == null)
		{
			return;
		}
		current.keywords(config.keywords(), partialOf(config.wholeWordsOnly()));
		for (final Chat chat : Chat.values())
		{
			current.watch(chat, watched(chat));
		}
		current.linesKept(config.linesKept());
		current.cardsKept(config.cardsKept());
	}

	private boolean watched(Chat chat)
	{
		switch (chat)
		{
			case GAME:
				return config.watchGame();
			case PUBLIC:
				return config.watchPublic();
			case PRIVATE:
				return config.watchPrivate();
			case CHANNEL:
				return config.watchChannel();
			case CLAN:
				return config.watchClan();
			case TRADE:
				return config.watchTrade();
			default:
				return true;
		}
	}

	/** The config key of a chat's switch; null for All, which has none and writes nothing. */
	private static String watchKey(Chat chat)
	{
		switch (chat)
		{
			case GAME:
				return "watchGame";
			case PUBLIC:
				return "watchPublic";
			case PRIVATE:
				return "watchPrivate";
			case CHANNEL:
				return "watchChannel";
			case CLAN:
				return "watchClan";
			case TRADE:
				return "watchTrade";
			default:
				return null;
		}
	}

	/**
	 * Puts the text on the system clipboard (the bundled Chat History's way); called on the event thread by a card's
	 * "Copy name".
	 */
	private static void copyToClipboard(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	/** The usable area (the screen less its task bar and the like) of the screen {@code at} is on. */
	private static Rectangle screenOf(Point at)
	{
		final GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsConfiguration found = environment.getDefaultScreenDevice().getDefaultConfiguration();
		for (final GraphicsDevice device : environment.getScreenDevices())
		{
			final GraphicsConfiguration candidate = device.getDefaultConfiguration();
			if (candidate.getBounds().contains(at))
			{
				found = candidate;
				break;
			}
		}
		final Rectangle bounds = found.getBounds();
		final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(found);
		return new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
			bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
	}

	/** "Reset to default": unsets the fifteen keys. */
	private void resetToDefault()
	{
		final String[] keys = {"keywords", "wholeWordsOnly", "followChatFilter", "watchPublic", "watchPrivate", "watchChannel", "watchClan",
			"watchGame", "watchTrade", "cardsKept", "linesKept", "defaultKeywordColour",
			"keywordColours", "cardsShare", "shownTab"};
		for (final String key : keys)
		{
			configManager.unsetConfiguration(ChatMentionsConfig.GROUP, key);
		}
	}
}

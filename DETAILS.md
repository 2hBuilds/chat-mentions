# 2h Chat Mentions

> This is the full description of 2h Chat Mentions - every rule and setting. The short version, with pictures, is
> the plugin's page on the Plugin Hub (the repository's README).

Every chat line that names you, or one of your keywords, becomes a card in the sidebar, over the chat it came
from, kept so you can scroll back. Nothing is typed, sent or changed in the game's chatbox; the plugin only reads
the chat lines RuneLite hands every plugin, and keeps them in memory.

## What the sidebar shows

**The top card**: *Mentions*, how many cards are unread, **Clear**, and the Discord mark and the settings icon.

**The guide**, under it, at every start: what the plugin does and six steps. It hides when your first card comes
in, or when you press **Hide**. **How to use** in the settings menu brings it back. Nothing about it is saved.

**The cards**, newest first. One card is who said it, which chat, the time, and the line with the keyword
coloured; under the line, **Copy name** (the sender's name onto your clipboard) and **Mark read** (the card
goes). A click anywhere else on the card lights that chat's tab and scrolls the kept chat to the line, marked
darker. A line that has already left the kept chat scrolls the view to the top instead. While there is no card,
a tip shows in the cards' place, a different one each time.

**The keyword strip**, at the foot of the cards half. Your name is the first chip and cannot be removed; its
colour is the default keyword colour, and a click on it opens the colour picker for that default. Each keyword
you add is a chip with a **x** that removes it; a click on the chip's word opens the picker for that keyword's
own colour. The box under the chips adds keywords: type one, or several separated by commas, and press Enter or
the **+**. A word already in the list is not added twice.

**The tabs**, the game's own: **All**, **Game**, **Public**, **Private**, **Channel**, **Clan**, **Trade**. A tab
shows while its chat is kept (All always). All shows every kept chat together, each line with its chat in grey
after the time. The lit tab is remembered across restarts.

**The kept chat**, under the tabs: the newest lines of the lit tab, oldest at the top, each with its time, the
sender in bold with the game's rank icon before the name when it had one, and the line with every keyword in its
colour. The game's own lines (a broadcast, a welcome) are in grey. The view follows the newest line while you are
at the bottom and stays put while you are scrolled up.

**The divider** between the two halves drags. Where you leave it is saved as a share of the height, so it comes
back in the same proportion at any window size.

## What makes a card

- A line typed by a player other than you, in a kept chat, that holds your name or a keyword as a whole word.
- Your name is always a keyword, in any case: *2hBuilds*, *2HBUILDS* and *2hbuilds* all count.
- With **Whole words only** unticked a keyword also counts inside a longer word, so *2h* fires on *2hbuilds*.
- The game's own lines never make a card, and neither does a line you sent yourself. Game and Trade lines are
  kept for reading only.
- A card keeps its place while its line is kept; the oldest card goes when more than **Cards kept** are held.

## What it reads, and never does

The plugin reads the chat message events RuneLite gives every plugin, the sender's rank icon from the game's own
icon set, and your own name from the client once you log in. It never reads the chatbox's widgets, never types
into or changes the chat input, never sends a message, never writes a file and never makes a request. Everything
it keeps is in memory and gone when the plugin is switched off or the client closes.

## Settings

Fourteen items, in RuneLite's settings panel and in the sidebar's settings menu alike; change one in either place
and the other follows.

| Setting | What it does |
|---|---|
| Keywords | Words that make a card, separated by commas. Your own name is always one. |
| Whole words only | A keyword counts as a whole word only. Untick to match it inside longer words too, like 2h in 2hbuilds. On by default. |
| Public | Keep the public chat and show cards from it. On by default. |
| Private | Keep private messages and show cards from them. On by default. |
| Channel | Keep the friends chat (the Channel tab) and show cards from it. On by default. |
| Clan | Keep the clan, guest clan and group ironman chat and show cards from it. On by default. |
| Game | Keep the game's own messages, for reading. Off by default. |
| Trade | Keep trade and challenge requests, for reading. Off by default. |
| Cards kept | How many mention cards the panel keeps, 1 to 1,000. 1,000 by default. |
| Chat lines kept | How many lines of each chat are kept, 100 to 20,000. 3,000 by default. |
| Default keyword colour | The colour of your name and of every keyword without a colour of its own. |
| Keyword colours | A colour per keyword, as word=rrggbb pairs separated by commas. Click a keyword in the panel to set one. |
| Cards share | How much of the panel's height the cards take, in percent. Drag the divider to set it. |
| Tab shown | The chat tab the panel opens on. |

**Reset to default** at the foot of the settings menu puts every one of them back.

## Caveats worth knowing

- Memory only: the cards and the kept chat start empty at every client start.
- A keyword's own colour is kept by its word; remove the keyword and the colour goes with it. **Reset all
  keyword colours** in the settings menu forgets them all and leaves the default.
- The All tab keeps its own newest lines, so with every chat on and a busy bank it fills faster than one tab.
- A private message you send shows in the Private tab as *To <name>* and never makes a card.

## Licence

BSD 2-Clause. See `LICENSE`.

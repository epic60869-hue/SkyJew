# Changelog

All notable changes to SkyJew are listed here, newest first.

## Unreleased

### Added
- Leap Menu (Dungeons): an Odin-style Spirit Leap menu with one box per teammate. Each class has its own colour (Archer orange, Mage blue, Berserk red, Healer purple, Tank green) and corner, and both can be changed.
- Blood Camp (Dungeons): Watcher move prediction with a Move Timer HUD and a "Kill Mobs" title, and kill timers that box where each blood mob lands with a countdown until it spawns.
- Door and key highlight: wither and blood doors are outlined green when your team has the key and red when locked; dropped keys are outlined and announced.
- Score (Dungeons): 270 and 300 score titles, sounds and chat messages, a separate party-chat toggle and editable message for each (sent as "[SJ] ..."), and a Score Display HUD with score, secrets, crypts, deaths and mimic/prince.
- Positional messages: `/sj posmsg add here|at|in ...` sends a party message when you reach a spot, like Odin. Built-in waypoints include "Py Stand Here" at 95, 165.5, 94.4 during Storm on floor 7 (Mage only), "Mage Stop" at 34, 169, 65 during Storm (Mage only), "Arch Stand Here" at 102-104, 168, 49 during Storm (Archer only), "Tank Stand Here" at 109, 170, 93 during Storm (Tank only), "Healer Stand Here After Lighting" at 58, 169, 66 during Storm (Healer only), and "SS" during Goldor until Necron (Healer only): the block at 109, 120, 93 is highlighted and standing at 108, 120, 93 sends "At SS" to party chat once. Radius messages draw an Odin-style ring.
- `/sj route import`: imports routes from your clipboard or a file (Stella / SkyJew format, or SecretRoutes files).
- Last Breath release cue: a sound and RELEASE title once you have charged Last Breath for 5 server ticks (adjustable).
- Misc > Random: Low Fire (lowers the burning overlay) and Hide Explosions.
- Platform Highlight (3x3) (Dungeons): one box over the floor 7 3x3 platform (x 53-55, z 113-115) from when Goldor starts, like NoFrills, with Healer Only, style and colour options.
- Pristine Record (Mining): remembers your highest pristine proc, overall and per gemstone, and alerts on a new PB. `/sj pristine` lists them.
- Portfolio (`/sj portfolio`): track items you own (e.g. pet skins) with live prices. Quantities are counted from your inventory and every Ender Chest / backpack page you have opened, plus an extra amount you can type. Set a buy price to see profit/loss. Auction and bazaar sales of tracked items can be logged (asking first or automatically), value history is saved every 30 minutes with an in-game graph, and Export CSV writes portfolio.csv, history.csv and sold.csv for Excel.
- `/sj recipe` now works like SkyOcean's craft helper: the panel next to your inventory has -/+ buttons for the amount (Shift 10, Ctrl 64), and a movable Recipe HUD shows the item and the base ingredients you still need while you play (Misc > Recipe HUD).
- Misc > Player Size: make yourself, other players, or both smaller or bigger (client side), like Odin.
- Misc > Held Item Model: move, rotate and scale the item in your hand and change swing speed, like Skysoft; `/sj helditem save` stores settings for one item.

### Changed
- Splits now work like Odin's: Blood Open, Blood Clear, Portal Entry, each boss phase and Total, with Boss Entry, tick times, per-floor personal bests and a "took" message after each split.
- Mask timers now follow Odin: Spirit, Bonzo and Phoenix always show invincibility time, cooldown or ready, counted in server ticks, with your Bonzo cooldown read from the mask. Optional party announce.
- Nicknames now also replace your name in Hypixel's name lines above heads, not just the vanilla nametag.
- Starred mob boxes no longer show through walls.
- The puzzle solvers now use Odin's: Ice Fill (with optional shorter paths), Boulder, Creeper Beams, Three Weirdos, Quiz, Teleport Maze, Water Board (with optional faster solutions) and Blaze. Tic Tac Toe and Silverfish keep their solvers.

### Fixed
- SkyJew now reads chat straight from the server, so it still sees [BOSS] and [NPC] lines when another mod hides them. This was why Simon Says, splits, boss tick timers, the dungeon score and the map did not start.
- Simon Says also turns on near the device on floor 7 if the Maxor message was missed.
- Terminal solver highlights are drawn with the slots, so mods that hide tooltips in terminals no longer hide them.
- The dungeon map now updates whenever Hypixel sends a map update, not only when a room is identified.
- The dungeon floor is read correctly from the sidebar ("Floor pattern doesn't match").
- Beacon beams from mods built for Minecraft 26.1 (such as Odin's quiz and terminal beams) no longer show the missing texture.
- The "Kills Since Rare Drop" setting was not saved.
- Storage search (`/sj search`) kept one storage for every profile, so your ironman showed your main profile's items. Each SkyBlock profile now has its own storage (reopen your Ender Chest and backpacks once per profile).

## 1.2.1 — 2026-09-25

### Fixed
- The Zealot Tracker could count zealots other players killed right next to you. A kill now only counts when you also get the Combat XP for it.
- The Item Price Tooltip didn't show NPC sell prices. The lines are now in the order NPC Sell Price, Lowest BIN Price, 3 Day Avg. Price, and the settings are in the same order.

## 1.2 — 2026-09-25

### Added
- Commands are no longer case-sensitive: `/SJ GUI` works the same as `/sj gui`.
- Item Price Tooltip (Misc): 3 day average price, lowest BIN and NPC sell price on SkyBlock items, like Skyblocker.
- Current chat display: shows which chat you're typing in (All, Party, Guild, Officer, Co-op, a private conversation or SkyJew chat) above the chat box, like SkyHanni.
- Ultrasequencer order numbers: every slot shows its place in the order, with the next one in yellow.

### Changed
- The zealot counter is now a Zealot Tracker in the style of SkyHanni's trackers: kills, Summoning Eyes and kills since your last eye. Click Total / This Session while your inventory is open to switch. Only your own kills count, including Hyperion multi-kills.
- HUD elements no longer have a background by default. Right-click any HUD in `/sj gui` to turn its background on.
- In every settings section, dropdowns are now listed above the single toggles.
- The Enchanting tab has moved into Misc as the Experimental Table section. Your settings carry over.
- Settings now save as soon as you change them, not only when the settings screen closes.
- The "no custom skin" barrier is now the first slot in the helmet skin picker.
- In the helmet skin picker, animated heads only animate when you hover or select them.
- The README is much shorter.

### Removed
- The Alchemy tab and the Alchemy 50 Estimate HUD.
- The Hunting tab (Safari Critter Tracker and Hunting Box Value), for now.

### Fixed
- Animated helmet skins (Celestial Necron/Storm/Goldor, Golden Dragon Swap Plushies and every other animated head) couldn't be applied: selecting one cleared the skin instead. They now apply, wait for each frame to load instead of flashing Steve, and Cancel restores them properly. Skins that fail to load are retried.
- The zealot counter counted other players' kills.
- The item rarity style went back to Square after restarting the game.
- The Chronomatron solver didn't highlight anything.
- Calendar real-time dates didn't show in the calendar menus.

## 1.1 — 2026-09-25

### Added

**General**
- `/sj gui` can now move and resize every HUD element, not just a few. Elements snap to the screen edges and can't be dragged off screen.
- Right-click any HUD in `/sj gui` to turn its background on or off.
- `/sj keys` now uses a full port of the CommandKeys mod (keybinds, macros and profiles). Your old command keys are migrated automatically.
- `/sj custom` has been rebuilt as a port of Skyblocker's item customisation: custom names, dyes (including animated dyes), armor trims and helmet textures.
- `/sj recipe <item> [amount]` shows a SkyOcean-style craft helper. It shows the full recipe tree and what you still need, counting items in your storage. It also has `/sj recipe amount <n>` and `/sj recipe clear`.
- Item Rarity is now a dropdown with style and opacity options. It works in your inventory, in containers and on the hotbar.
- SkyBlock calendar time and a date calculator, ported from Skyblocker.
- Nicknames now show above players' heads and in the tab list for everyone using SkyJew. Hovering a nickname shows the player's real name. Offensive nicknames are blocked.
- SJ chat now shows each player's SkyBlock level in its level colour. Hovering a name shows the real name, and clicking starts a `/msg`.
- `/sjc <message>` sends a message to SJ chat. `/chat sj` switches your chat to SJ chat, and `/chat <anything else>` switches back.

**Pets**
- The pet HUD now shows overflow levels automatically from the tab list, like NopoMod. You no longer need to open the Pets menu. It also shows progress to the next level.
- Pet display background option.

**Combat**
- Arrow counter: your selected arrow and how many are left in your quiver.
- Zealot counter: zealot kills, kills since your last Summoning Eye and eyes dropped.
- Legion display: how many players are in Legion range.
- Cocoon alert for slayer bosses, slayer minibosses, elusive mobs and important bosses.
- Rare drops: copy drop messages and show an animation for valuable drops.

**Slayer**
- Slayer tracker: progress to spawning the boss, XP to your next level and session totals.
- Boss phase display showing your boss's nametag lines (Voidgloom hits, Inferno attunement and so on).

**Garden**
- Yaw, pitch and facing direction HUD (Garden only).
- Pest cooldown, blocks-per-second and special drop animation.
- Mouse lock can be set to only work in the Garden.

**Other skills**
- Fishing, mining (Crystal Hollows map, Mineshaft timer), foraging, alchemy, enchanting, runecrafting and hunting features.

**Dungeons** (ported from Skyblocker)
- Dungeon map with player heads and room names.
- Puzzle solvers: Tic Tac Toe, Three Weirdos, Creeper Beams, Water Board, Blaze, Boulder, Ice Fill, Silverfish, Trivia and Teleport Maze.
- Secret waypoints.
- Floor 7 terminal and device solvers.
- Splits, tick timers, mask timers and an M7 debuff alert.
- Highlight starred mobs: draws a box around ✯ mobs, visible through walls.
- Secret routes in Stella's format. Rooms without your own route use Stella's routes.
  - `/sj route start` records a route automatically: secrets, levers, etherwarps, superbooms, pearls and mined blocks. `/sj route stop` saves it.
  - `/sj route next` / `back` step through a route, and `/sj route clear` deletes your route for the current room.
  - All your routes are saved in one file. `/sj export` writes them to `routes-export.json` and copies them to your clipboard.

**Party**
- Party commands.

### Changed
- The notes screen (`/sj notes`) has been rebuilt with a proper multi-line editor and autosave.
- The cocoon alert no longer has a mob list text box. It alerts for important mobs only.
- The commission HUD now looks like Skyblocker's. Its background toggle is now saved.

### Removed
- The "you have been ratted" start-up screen and its sound.
- The slayer profit tracker, for now.

### Fixed
- HUDs saved off screen (such as the zealot counter) now appear on screen.
- The zealot counter never counted kills.
- The arrow counter didn't detect your arrows.
- Pet level was shown twice in the pet HUD.
- Item rarity backgrounds didn't show in the player inventory.
- Calendar time was wrong.
- `/sj search` still showed the first page's items on later pages.
- Image link previews in SJ chat stopped showing on hover.
- Other players' nicknames in the tab list.
- A crash on start-up when items were created too early.
- A crash from nicknames being applied repeatedly in the tab list.

## 1 — 2026-09-25

First release.

- Farming RNG tracker and HUD.
- Storage search (`/sj search`).
- Custom item tools (`/sj custom`).
- Command keys (`/sj keys`).
- Notes (`/sj notes`).
- Main `/sj` GUI.
- Version checker.

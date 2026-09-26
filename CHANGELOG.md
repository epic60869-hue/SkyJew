# Changelog

All notable changes to SkyBalls are listed here, newest first.

## 1.2.5 — 2026-09-26

### Changed
- SkyJew is now **SkyBalls**: `/sb` and `/skyballs` (the old `/sj` and `/skyjew` still work), `/sbc` for SkyBalls chat (`/sjc` still works), and [SB] in chat. The [SB] in SkyBalls chat is dark green, like Hypixel's "Guild >". Your settings carry over.
- Ranks ([OWNER], [TESTER], ...) only show in SkyBalls chat, and Chat > Custom Chat > Show Ranks turns them off completely. Rank changes made on the website show up right away when the server announces them.
- The Collection Tracker is just two lines now: "Collection: 12,345,678" (with the item's icon), and the next player above you on the Elite leaderboard with how far ahead of you they are.
- The Pickaxe Ability HUD reads Hypixel's Pickaxe Ability tab list widget, like SkyHanni, instead of guessing from chat. Turn that widget on in Hypixel's tab list settings.
- The Enchanting Runes nickname font is made of letters now (the Unicode enchanting table alphabet), so it shows in every tab list, including other mods'.
- Leap menu: press 1-4 (changeable) to leap to the teammate in that box; each box shows its key.
- Chat image previews work with Discord, Imgur and Gyazo links, show "Loading image..." while downloading, and sit above the link's tooltip.

### Added
- `/sb bugreport`, `/sb suggest` and `/sb feedback`: send a bug report, suggestion or feedback (a title and a description) to the SkyBalls team.
- Dungeon Case Opening (Dungeons > Case Opening, off by default): Obsidian and Bedrock chests open like a CS2 case (SkyOcean's Dungeon Gambling) and stop on the best item. Legendary or better plays "GOLD GOLD GOLD".
- Item Notification (Misc > Item Notification): SkyOcean's Sack Notification as a HUD. When an item on your list goes into your sacks or inventory it shows like the farming RNG HUD ("5x Enchanted Diamond 8.5k"). The list is edited in its own window with item name suggestions (EDIT, or `/sb itemnotify`).
- Storage Overlay (Misc, on by default): every Ender Chest page and backpack at once in /storage and in any page, like Firmament. Click a page's name to open it; the open page and your inventory work as normal.
- Slot Locking & Binding (Misc): press L over a slot to lock it (it can't be clicked, moved or dropped), and B to bind a hotbar slot to an inventory slot so a shift-click swaps them (Odin's Slot Binds).
- Copy Chat (Chat > Copy Chat): right-click a chat message to copy it, Shift+right-click for one line, with a preview (NoFrills' Chat Tweaks). Rank prefixes aren't copied.
- `/sb toggle <setting>` turns any setting on or off, `/sb togglenick <player>` hides or shows someone's nickname for you, `/sb who` lists who is online with the mod, and `/sb disableall` (or `/sbdisableall`) turns everything off after you click to confirm.
- No Swing Animation (Misc > Held Item Model, off by default), like NoFrills.
- SB Chat Ping (Chat > Custom Chat, off by default): a ping when someone sends a message in /sbc.
- Farming RNG HUD: Epic and Legendary slug pets from pests, shown in their rarity colour ("1x Legendary Slug Pet") at a set price of 500k (Epic) and 5m (Legendary).

### Fixed
- `/sb nick`: picking a preset colour also updates the colour picker and hex box.
- Calendar dates are also added from the menu's own tooltip, so another mod's tooltip code can't stop them showing.
- Mouse Reset puts the cursor in the middle of the screen (it went near the top-left at GUI scales above 1).

## 1.2.4 — 2026-09-26

### Added
- Update notifications: when a newer SkyJew is out you get "New SkyJew Mod Version 1.2.4 --> 1.2.6" in chat (always the newest one), with a Download link (Misc > Update Notifications).
- Auto Welcome (Misc > Auto Welcome): put players on your list and SkyJew welcomes them when they come online, in guild chat or with /msg, with your own message ({name} is their name). It can also welcome new guild members. Manage the list with `/sj welcome add|remove <name>` and `/sj welcome list`.

### Changed
- SkyJew's chat messages start with [SJ] instead of [SkyJew].
- SkyJew ranks ([OWNER], [TESTER] and the ones set on tastyfish.org) now show in front of those players' names in every chat, not only /sjc (Chat > Custom Chat > Ranks In All Chat).
- The Collection Tracker HUD copies SkyHanni's Crop Milestones display, for any collection: "Collection Milestones", the item's icon with your collection tier ("Cobblestone 11➜12"), your progress in that tier ("12,345/20,000"), the time to the next tier, Items/Hour and the percentage, then your Elite rank and how much you need to pass the next player. `/sj trackcollection <item> [goal]` pins one collection with an optional goal (e.g. `/sj trackcollection wheat 10m`), like SkyHanni's /shtrackcollection; `/sj trackcollection` follows what you gather again and `/sj trackcollection stop` hides it.
- Ranked players can use their own name as a nickname.

### Fixed
- `/sj nick` Font button: the ◀ arrow went forward too; it now goes back.
- The Enchanting Runes and Illager Runes nickname fonts showed broken characters for anything they don't have (numbers and symbols in the enchanting one); those now use the normal font.
- Rune prices: the lowest BIN list only has runes someone is selling right now, so most rune levels had no price. The price tooltip and Portfolio now fall back to the 3 day average and then to the last price seen (with how long ago), and Portfolio knows the names of runes that are in the 3 day average too.
- Nicknames (with their font) now show in tab lists drawn by other mods (SkyHanni, Skyblocker) too.
- The `/sj nick` menu couldn't be closed: Esc and Done sent you back to the chat box, or did nothing when the name wasn't allowed. Esc now always closes (a name that isn't allowed just isn't saved) and Done closes once the name is allowed.

## 1.2.3 — 2026-09-26

### Changed
- Dungeon map: new NoammAddons (Legit) style, now the default. Rooms are redrawn in clean colours with checkmarks, room names or secret counts, bordered player heads with optional names (Holding Leap / Always), and optional extra info under the map. Every colour and size can be changed. The old look is still there as Map Style: Skyblocker.
- Score Display now uses NoammAddons' score calculation, read straight from the tab list and sidebar, so it no longer depends on the dungeon start being detected. Optional Detailed mode and Force Paul. The 270/300 alerts use it too.
- Terminals: Odin's terminal solvers for all six terminals, with misclick blocking (clicks on wrong slots are ignored), first click protection and client prediction. Skyblocker's highlights are still available with Odin Terminal Solver off.
- Devices: Odin's Simon Says, Arrow Align and Sharp Shooter (i4) solvers. Simon Says and Arrow Align block wrong clicks (hold Shift to click anyway).
- Blood Camp now follows Odin: a head is only tracked once the Watcher throws it, so the heads hanging on the walls no longer throw off the landing boxes. Adds a position box moved ahead by your ping, box colours and size, offset, spawn tick, interpolation and the Watcher bar (blood mobs left in the boss bar).
- Goldor tick timer counts 50 ticks by default (adjustable).
- `/sj nick` has a new screen laid out like `/sj custom`: name and toggles, colour swatches with Rainbow and a custom colour picker, and live TAB / chat / nametag previews.
- `/sj recipe` only uses the Recipe HUD now; the panel next to the inventory is gone.
- HUDs keep their place relative to the screen when you change GUI scale or window size, instead of piling up at an edge. Storage search fits on the screen at high GUI scale.
- All boxes, lines and text in the world are drawn every frame, so they follow moving mobs and your camera smoothly.
- Last Breath RELEASE plays a bell by default (Bell, Note Block Bell, Ding, XP Orb or None) at full volume.
- Compact chat stacks repeats onto the newest message at the bottom instead of the old one higher up.
- Price tooltips: items sold on the bazaar show the bazaar insta-buy and insta-sell price where the lowest BIN and 3 day average would be (for the whole sack in the Sacks menu).
- The settings title shows the installed version (e.g. SkyJew Mod v1.2.3).
- `/sj log` opens a changelog window instead of printing in chat: arrows (or the left and right keys) switch versions, and the list scrolls.
- Launchers show the mod as "SkyJew by 2m3s", with 2m3s's head as its icon.

### Added
- Dungeon routes folder: put any route file in `config/skyjew/dungeon route/` (Stella / SkyJew, SecretRoutes or Dungeon Rooms Mod style). Rooms without a route there use Stella's routes. Changes load automatically; `/sj route reload` and `/sj route folder` too.
- Water Board: Skyblocker's water path and lever previews, next to Odin's solver.
- SS skip helper: counts your Simon Says start button clicks above the button and blocks clicks past the limit (default 4).
- Starred mob highlight colour, fill and line width.
- Scrollable tooltips: scroll to move long tooltips (Shift for sideways).
- Toggle Sprint, with a key in Controls and a small HUD.
- "Your Class" option for the built-in positional waypoints, if your class isn't detected.
- `/sj waypoints`: Skyblocker's waypoint editor, with groups per island, ordered waypoints (`/sj waypoints ordered next|previous|reset`) and Skyblocker / Skytils / Coleweight import and export.
- `/sj log`: the changes in the version you have installed, in game.
- Crystal Hollows waypoints, like Skyblocker: places you find are marked (Mines of Divan, Jungle Temple, Goblin Queen's Den, ...), coordinates in chat become waypoints, and `/sj crystalwaypoints add|share|remove|clear`.
- Pickaxe ability HUD: cooldown of Mining Speed Boost, Pickobulus and the other abilities, with a ready alert.
- Warp shortcuts: `/dhub` instead of `/warp dhub`, and the same for every warp in Misc > Warp Shortcut List.
- Price Paid tooltip, like NoFrills: items you buy on the auction house show what you paid for them.
- Collection tracker, like SkyHanni's farming display but for every collection: while you mine, farm, forage or fish it shows your collection, what you've gained this session (and per hour) and your rank on the Elite collection leaderboard, with how much you need to pass the next player. Enchanted items from compactors count as the items they're made of (Enchanted Cobblestone = 160 Cobblestone).
- /sj nick fonts: a Font button in the nickname menu with Bold, Italic, Small Caps, Full Width, Bubble, Script, Fraktur, Double Struck, Monospace, Sans Bold, Minecraft's enchanting and illager runes and Uniform. The nickname filter also sees through look-alike letters now.
- Custom Chat > Show SJ Chat: turn off to hide other players' `/sjc` messages.
- SJ chat rank prefixes, tied to the accounts themselves and managed from the tastyfish.org admin page (the mod checks for changes every 5 minutes). [OWNER] and [TESTER] are built in. Nicknames that copy a ranked player's name or a rank are not allowed.
- Portfolio: auction house and bazaar prices have their own buttons (AH: Lowest BIN / 3-day avg, BZ: Sell / Buy price), so all four combinations work.
- Portfolio: new rows get today's lowest BIN (or bazaar buy price) as their buy price; you can still change it.
- Portfolio: click any column header to sort by it, click again to reverse; the sorted column shows an arrow.
- Portfolio: item icons next to names, columns sized to fit so Qty and Buy each no longer overlap, sales can be removed from the Sold list, and the summary shows profit per hour (each row's profit divided by the hours since you added it).

### Fixed
- Portfolio: runes (e.g. Barkshatter Rune III) showed "?" as their price. Runes, pets and enchanted books are now counted and priced by their market ID, and rune names can be typed in.
- Water Board: the line to the next lever jumped around while moving. Only Odin's solver draws it now (Skyblocker's solver drew a second line), and it starts from the camera.
- Built-in positional waypoints (Py Stand Here, Mage Stop, ...) didn't show: your class is now read from any tab line with your name and remembered for the run, even as a ghost.
- Sharp Shooter (i4) solver didn't work.
- Pet display was blank when the server address had a port or wasn't exactly hypixel.net, and when Hypixel put icons around "[Lvl N]".
- Calendar real-time dates didn't show: calendar menus and dates written in tooltips are matched more loosely.
- Slayer tracker and boss phase HUDs didn't work: Hypixel's padding emoji in the sidebar broke the matching, and boss nametags are now text displays.
- Last Breath, Ice Spray, masks, arrows, farming tools and other items renamed with `/sj custom` are still recognised.
- The 1/10,000 jumpscare no longer happens in dungeons.
- Storm pad tick timer was one tick short; it now counts down to 0 on the pad tick like Odin (the Goldor timer too).
- Superpairs solver didn't show the cards you had revealed; they now stay visible on their slots.
- Scrollable tooltips are now Skysoft's Tooltip Scroll: smooth panning, WASD / Page Up / Page Down keys, a reset key, speed and smoothness settings (Misc > Tooltip Scroll). It steps aside when Skysoft itself is installed.

### Removed
- Farming Profit Tracker (the Skysoft port).

## 1.2.2 — 2026-09-25

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

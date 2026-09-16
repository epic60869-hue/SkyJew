# You Must Have Skysoft Downloaded

# TastyFish Mod

Fabric 26.2 companion mod for the Tasty Fish farming leaderboard.

## Farming analytics

The mod reads **SkySoft's live FARMING session tracker** and keeps a local farming history:

- Farming session recorder
- Up to 100 recent saved sessions
- One-hour rolling personal best
- Continuous farming streak with inactivity detection
- Farming achievements
- `/tf stats` summary

The local analytics file is `config/tastyfish-farming.json`.

## Guild collection HUD

TastyFish now includes a compact SkyHanni-style guild collection display backed by the public TastyFish website leaderboard API.

When a farming collection leaderboard is enabled on `tastyfish.org`, the HUD automatically matches the current SkySoft farming crop and only displays the guild gap:

```text
Carrot Collection: 81,837,732 [#21]
7,155,361 behind BigLando [#19]
```

The data is matched by Minecraft UUID first and username second. Only rows returned by the TastyFish guild leaderboard are used; it does not display unrelated global players.

Use `/tf gui` to position the Guild HUD and `/tf guildhud` to toggle it. Website/refresh settings are also available under `/tf` → **Guild HUD**.

## Discord reports

Discord reporting no longer uses a webhook URL in the client.

Open `/tf` → **Discord** and configure:

- Discord **channel ID** for normal channel reports
- Discord **forum channel ID** for forum posts
- TastyFish website relay endpoint
- Optional relay secret in `config/tastyfish-mod.json`

The website-side Discord bot is responsible for posting the report. The Minecraft mod never stores a Discord bot token or webhook URL.

The mod can report:

- Completed farming sessions
- New one-hour personal bests
- Streak milestones
- Achievement unlocks

Session reports include duration, profit, actions, pest kills, tracked items and the session ID.

## SkySoft integration

The mod reads the current SkySoft FARMING session locally and sends the configured farming snapshot to the Tasty Fish backend on the normal upload interval:

- Minecraft username and UUID
- SkyBlock profile
- farming profit
- active farming time
- actions
- item counts
- pest kills
- a unique session ID

The server is responsible for accumulating leaderboard totals. If Minecraft is restarted, the new session gets a new ID and the player's existing leaderboard total is preserved.

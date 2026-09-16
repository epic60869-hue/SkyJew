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

## Farming server API key

The farming API key is **not included in the source code or repository**.

The mod supports three ways to provide the key, in this order of priority:

1. **JVM system property** — add this to the Minecraft JVM arguments:
   ```text
   -Dtastyfish.farming.apiKey=YOUR_KEY_HERE
   ```
2. **Environment variable** available to the Minecraft process:
   ```text
   TASTYFISH_FARMING_API_KEY=YOUR_KEY_HERE
   ```
3. **Local config file** at `config/tastyfish-mod.json`:
   ```json
   "farmingServerApiKey": "YOUR_KEY_HERE"
   ```

The JVM property and environment variable override the JSON value. Runtime values are never written back to the config file.

If using the local JSON method, replace `YOUR_KEY_HERE` with the current key and keep `config/tastyfish-mod.json` private. Do not commit that file or the key to GitHub.

The farming server endpoint is fixed to `https://tastyfish.org/api/farming` in this build. The Discord destination is fixed to the Tasty Fish farming destination used by the backend. The Minecraft mod never stores a Discord bot token or webhook URL.

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

The website-side Discord bot is responsible for posting the report. The Minecraft mod never stores a Discord bot token or webhook URL.

The mod can report:

- Completed farming sessions
- New one-hour personal bests
- Streak milestones
- Achievement unlocks

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

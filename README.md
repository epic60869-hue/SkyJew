# You Must Have Skysoft Downloaded

# TastyFish Mod

Fabric 26.2 companion mod for the Tasty Fish farming leaderboard.

## Farming analytics

The mod reads **SkySoft's live FARMING session tracker** and now also keeps a local farming history:

- Farming session recorder
- Up to 100 recent saved sessions
- One-hour rolling personal best
- Continuous farming streak with inactivity detection
- Farming achievements
- `/tf stats` summary

The local analytics file is `config/tastyfish-farming.json`.

## Discord forum reporting

Open `/tf` and select **Discord**. Paste a webhook created for the Discord **Forum Channel** you want to use and enable reporting.

The mod can create a separate forum post for:

- Completed farming sessions
- New one-hour personal bests
- Streak milestones
- Achievement unlocks

Session reports include duration, profit, actions, pest kills, tracked items and the session ID.

The webhook is stored locally in `config/tastyfish-mod.json`. Keep it private; anyone with the webhook URL can post to that Discord channel.

## SkySoft integration

Every 30 seconds it sends the current session snapshot to the Tasty Fish backend:

- Minecraft username and UUID
- SkyBlock profile
- farming profit
- active farming time
- actions
- item counts
- pest kills
- a unique session ID

The server is responsible for accumulating the leaderboard. If Minecraft is restarted, the new session gets a new ID and the player's existing leaderboard total is preserved.

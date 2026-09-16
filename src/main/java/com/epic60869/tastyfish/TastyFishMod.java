package com.epic60869.tastyfish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class TastyFishMod implements ClientModInitializer {
    private static final String DISCORD_DESTINATION_ID = "1538133706294829106";
    private static final long DISCORD_SESSION_UPDATE_INTERVAL_MS = 60_000L;

    private TastyFishConfig config;
    private final TastyFishServerClient farmingServer = new TastyFishServerClient();
    private FarmingHistory history;
    private String sessionId = TastyFishServerClient.newSessionId();
    private long lastUploadMillis = 0L;
    private long lastActiveMillis = -1L;
    private long lastSnapshotWallMillis = 0L;
    private long lastOneHourPbAlertActiveMillis = -1L;
    private long lastDiscordSessionUpdateWallMillis = 0L;
    private boolean wasConnected = false;
    private SkysoftSessionReader.Snapshot lastSnapshot;

    @Override
    public void onInitializeClient() {
        Minecraft minecraft = Minecraft.getInstance();
        Path configDir = minecraft.gameDirectory.toPath().resolve("config");
        config = TastyFishConfig.load(configDir.resolve("tastyfish-mod.json"));
        config.discordDestinationId = DISCORD_DESTINATION_ID;
        history = new FarmingHistory(configDir.resolve("tastyfish-farming.json"));
        FarmingRngTracker.get().register();
        TastyFishRngHud.register(config);
        TastyFishGuildLeaderboardHud.register(config);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> finishSession("game closed"));
        registerCommands();
        System.out.println("[TastyFish] SkySoft integration, local analytics, guild HUD and standalone farming server loaded.");
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("tf")
                .executes(context -> openMenu())
                .then(ClientCommands.literal("gui").executes(context -> openGuiEditor()))
                .then(ClientCommands.literal("stats").executes(context -> { printStats(); return 1; }))
                .then(ClientCommands.literal("discord").executes(context -> { printDiscordHelp(); return 1; })));
            dispatcher.register(ClientCommands.literal("tastyfish")
                .executes(context -> openMenu())
                .then(ClientCommands.literal("gui").executes(context -> openGuiEditor()))
                .then(ClientCommands.literal("stats").executes(context -> { printStats(); return 1; }))
                .then(ClientCommands.literal("discord").executes(context -> { printDiscordHelp(); return 1; })));
        });
    }

    private int openMenu() {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setScreen(new TastyFishScreen(config)));
        return 1;
    }

    private int openGuiEditor() {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setScreen(new TastyFishGuiEditor(config)));
        return 1;
    }

    private void printStats() {
        Minecraft mc = Minecraft.getInstance();
        FarmingHistory.Data d = history.data();
        mc.showDebugChat(net.minecraft.network.chat.Component.literal(
            "§6TastyFish §7| 1h PB: §e" + formatCoins(d.bestOneHourProfit) +
            " §7| Best streak: §e" + formatDuration(d.bestStreakMs) +
            " §7| Sessions: §e" + d.sessions.size()));
    }

    private void printDiscordHelp() {
        Minecraft.getInstance().showDebugChat(net.minecraft.network.chat.Component.literal(
            "§6TastyFish §7| Farming sessions use the owner's fixed Discord destination."));
    }

    private void tick(Minecraft minecraft) {
        boolean connected = minecraft.player != null;
        if (!connected) {
            if (wasConnected) finishSession("disconnect");
            wasConnected = false;
            return;
        }

        TastyFishGuildLeaderboardHud.tick();

        if (!wasConnected) {
            lastUploadMillis = 0L;
            wasConnected = true;
            lastDiscordSessionUpdateWallMillis = 0L;
            sessionId = TastyFishServerClient.newSessionId();
            TastyFishVersionChecker.check(minecraft);

            if (config.discordSendSessions) {
                farmingServer.startSession(config, minecraft.getUser().getName(), sessionId, 0L, 0L, 0L);
            }
        }

        long now = System.currentTimeMillis();
        if (lastSnapshot != null && now - lastSnapshotWallMillis >= 2L * 60L * 1000L) finishSession("inactive");
        if (now - lastUploadMillis < config.uploadIntervalSeconds * 1000L) {
            sendDiscordSessionUpdateIfDue(minecraft, now);
            return;
        }
        lastUploadMillis = now;

        SkysoftSessionReader.Snapshot snapshot = SkysoftSessionReader.read();
        if (!snapshot.valid()) {
            sendDiscordSessionUpdateIfDue(minecraft, now);
            return;
        }

        if (lastActiveMillis >= 0L && snapshot.activeMillis() < lastActiveMillis) {
            finishSession("skysoft session reset");
            sessionId = TastyFishServerClient.newSessionId();
            lastActiveMillis = -1L;
            lastSnapshot = null;
            lastOneHourPbAlertActiveMillis = -1L;
            lastDiscordSessionUpdateWallMillis = 0L;
            if (config.discordSendSessions) {
                farmingServer.startSession(config, minecraft.getUser().getName(), sessionId, 0L, 0L, 0L);
            }
        }
        lastActiveMillis = snapshot.activeMillis();
        lastSnapshot = snapshot;
        lastSnapshotWallMillis = now;

        if (config.farmingAnalyticsEnabled) processAnalytics(minecraft, snapshot);

        String username = minecraft.getUser().getName();
        UUID uuid = minecraft.getUser().getProfileId();
        farmingServer.upload(config, username, uuid, currentSkysoftProfile(), sessionId, snapshot);
        sendDiscordSessionUpdateIfDue(minecraft, now);
    }

    private void sendDiscordSessionUpdateIfDue(Minecraft minecraft, long now) {
        if (!config.discordSendSessions || lastSnapshot == null) return;
        if (lastDiscordSessionUpdateWallMillis != 0L
            && now - lastDiscordSessionUpdateWallMillis < DISCORD_SESSION_UPDATE_INTERVAL_MS) return;

        lastDiscordSessionUpdateWallMillis = now;
        farmingServer.updateSession(
            config,
            minecraft.getUser().getName(),
            sessionId,
            lastSnapshot.activeMillis(),
            (long) Math.max(0L, lastSnapshot.profit()),
            sum(lastSnapshot.pests())
        );
    }

    private void processAnalytics(Minecraft minecraft, SkysoftSessionReader.Snapshot snapshot) {
        FarmingHistory.Update update = history.update(sessionId, snapshot);
        String username = minecraft.getUser().getName();

        if (update.fiveMinuteReport() && config.farmingAnalyticsEnabled) {
            minecraft.showDebugChat(net.minecraft.network.chat.Component.literal(
                "§6§l5 Minute Profit: §e" + formatCoins(update.fiveMinuteProfit()) +
                " §7| §e" + formatCoins(update.sessionProfit()) + " §7" +
                formatCompactDuration(update.completedFiveMinuteIntervals() * 5L * 60L * 1000L)));
        }

        boolean pbAlertReady = lastOneHourPbAlertActiveMillis < 0L ||
            snapshot.activeMillis() - lastOneHourPbAlertActiveMillis >= 60L * 60L * 1000L;
        if (update.newOneHourPb() && pbAlertReady && config.farmingPersonalBestEnabled) {
            lastOneHourPbAlertActiveMillis = snapshot.activeMillis();
            minecraft.showDebugChat(net.minecraft.network.chat.Component.literal(
                "§6§lNEW 1-HOUR PERSONAL BEST! §e" + formatCoins(update.oneHourProfit()) + " coins"));
            if (config.discordForumEnabled && config.discordSendPersonalBests)
                farmingServer.report(config, username, "personalBest", "1-hour PB: " + formatCoins(update.oneHourProfit()) + " coins");
        }
        if (update.newStreak() && config.farmingStreakEnabled && isStreakMilestone(update.streakMs())) {
            if (config.discordForumEnabled && config.discordSendStreaks)
                farmingServer.report(config, username, "streak", "Reached a " + formatDuration(update.streakMs()) + " farming streak.");
        }
        if (config.farmingAchievementsEnabled) {
            List<String> unlocked = history.newlyUnlockedAchievements();
            for (String id : unlocked) {
                String name = achievementName(id);
                minecraft.showDebugChat(net.minecraft.network.chat.Component.literal("§d§lACHIEVEMENT UNLOCKED! §f" + name));
                if (config.discordForumEnabled && config.discordSendAchievements)
                    farmingServer.report(config, username, "achievement", name + " unlocked.");
            }
        }
    }

    private void finishSession(String reason) {
        if (history == null) return;

        SkysoftSessionReader.Snapshot snapshot = lastSnapshot;
        if (snapshot != null) {
            FarmingHistory.Update update = history.finish(reason, snapshot);
            if (update.sessionEnded() && config.discordSendSessions && update.session() != null) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.getUser() != null) {
                    String username = minecraft.getUser().getName();
                    FarmingHistory.Session s = update.session();
                    farmingServer.endSession(
                        config,
                        username,
                        sessionId,
                        s.activeMillis(),
                        (long) Math.max(0L, s.profit()),
                        s.pests()
                    );
                }
            }
        }

        lastSnapshot = null;
        lastSnapshotWallMillis = 0L;
        lastActiveMillis = -1L;
        lastOneHourPbAlertActiveMillis = -1L;
        lastDiscordSessionUpdateWallMillis = 0L;
        sessionId = TastyFishServerClient.newSessionId();
    }

    private static long sum(java.util.Map<String, Long> map) {
        long total = 0L;
        if (map == null) return total;
        for (Long value : map.values()) if (value != null) total += value;
        return total;
    }

    private static boolean isStreakMilestone(long millis) {
        long[] milestones = {30L * 60_000L, 60L * 60_000L, 2L * 60L * 60_000L, 5L * 60L * 60_000L, 10L * 60L * 60_000L};
        for (long milestone : milestones) if (millis >= milestone && millis < milestone + 35_000L) return true;
        return false;
    }

    private static String achievementName(String id) {
        return switch (id) {
            case "FIRST_HARVEST" -> "First Harvest";
            case "MILLION_CROPS" -> "Million Crops";
            case "TEN_MILLION_CROPS" -> "Ten Million Crops";
            case "MILLIONAIRE" -> "Farming Millionaire";
            case "BILLIONAIRE" -> "Farming Billionaire";
            case "ONE_HOUR_FARMER" -> "One-Hour Farmer";
            case "FIVE_HOUR_STREAK" -> "Five-Hour Streak";
            case "PEST_CONTROL" -> "Pest Control";
            default -> id;
        };
    }

    private static String formatCoins(long coins) { return String.format("%,d", coins); }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format("%dh %02dm", seconds / 3600L, (seconds % 3600L) / 60L);
    }

    private static String formatCompactDuration(long millis) {
        long minutes = Math.max(0L, millis / 60_000L);
        long hours = minutes / 60L;
        long remainder = minutes % 60L;
        if (hours > 0) return hours + "h " + remainder + "m";
        return minutes + "m";
    }

    private String currentSkysoftProfile() {
        try {
            Class<?> api = Class.forName("com.skysoft.data.hypixel.SkyBlockProfileApi");
            Field field = api.getDeclaredField("currentProfileKey");
            field.setAccessible(true);
            Object value = field.get(null);
            return value == null ? "" : value.toString();
        } catch (Throwable ignored) { return ""; }
    }
}

package com.epic60869.tastyfish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.StringArgumentType;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class TastyFishMod implements ClientModInitializer {

    private TastyFishConfig config;
    private final TastyFishServerClient farmingServer = new TastyFishServerClient();
    private FarmingHistory history;
    private String sessionId = TastyFishServerClient.newSessionId();
    private long lastUploadMillis = 0L;
    private long lastActiveMillis = -1L;
    private long lastSnapshotWallMillis = 0L;
    private long lastOneHourPbAlertActiveMillis = -1L;
    private long gameSessionStartedWallMillis = 0L;
    private boolean gameSessionStarted = false;
    private SkysoftSessionReader.Snapshot lastSnapshot;

    @Override
    public void onInitializeClient() {
        Minecraft minecraft = Minecraft.getInstance();
        Path configDir = minecraft.gameDirectory.toPath().resolve("config");
        config = TastyFishConfig.load(configDir.resolve("tastyfish-mod.json"));
        history = new FarmingHistory(configDir.resolve("tastyfish-farming.json"));
        FarmingRngTracker.get().register();
        TastyFishRngHud.register(config);
        TastyFishCommandKeys.init(configDir);
        TastyFishStorageSearch.init(configDir);
        TastyFishCustom.init(configDir);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        startGameSession(minecraft);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> finishGameSession("game closed"));

        registerCommands();
        System.out.println("[TastyFish] Core mod loaded.");
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("tf")
                .executes(context -> openMenu())
                .then(ClientCommands.literal("notes").executes(context -> openNotes()))
                .then(ClientCommands.literal("keys").executes(context -> openCommandKeys()))
                .then(ClientCommands.literal("search").executes(context -> openStorageSearch()))
                .then(customCommand())));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> customCommand() {
        var custom = ClientCommands.literal("custom")
            .executes(context -> openCustom());

        custom.then(ClientCommands.literal("renameItem")
            .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                .executes(context -> customRename(StringArgumentType.getString(context, "name")))));

        custom.then(ClientCommands.literal("dyeColor")
            .executes(context -> customDye(""))
            .then(ClientCommands.argument("hex", StringArgumentType.word())
                .executes(context -> customDye(StringArgumentType.getString(context, "hex")))));

        custom.then(ClientCommands.literal("armorTrim")
            .executes(context -> customTrim("", ""))
            .then(ClientCommands.argument("material", StringArgumentType.word())
                .then(ClientCommands.argument("pattern", StringArgumentType.word())
                    .executes(context -> customTrim(
                        StringArgumentType.getString(context, "material"),
                        StringArgumentType.getString(context, "pattern"))))));

        custom.then(ClientCommands.literal("animatedDye")
            .then(ClientCommands.argument("hex1", StringArgumentType.word())
                .then(ClientCommands.argument("hex2", StringArgumentType.word())
                    .then(ClientCommands.argument("duration", StringArgumentType.word())
                        .then(ClientCommands.argument("cycleBack", StringArgumentType.word())
                            .executes(context -> customAnimated(
                                StringArgumentType.getString(context, "hex1"),
                                StringArgumentType.getString(context, "hex2"),
                                StringArgumentType.getString(context, "duration"),
                                StringArgumentType.getString(context, "cycleBack"), "0"))
                            .then(ClientCommands.argument("delay", StringArgumentType.word())
                                .executes(context -> customAnimated(
                                    StringArgumentType.getString(context, "hex1"),
                                    StringArgumentType.getString(context, "hex2"),
                                    StringArgumentType.getString(context, "duration"),
                                    StringArgumentType.getString(context, "cycleBack"),
                                    StringArgumentType.getString(context, "delay")))))))));

        return custom;
    }

    private int openMenu() {
        Minecraft.getInstance().execute(() ->
            Minecraft.getInstance().gui.setScreen(new TastyFishScreen(config)));
        return 1;
    }

    private int openCommandKeys() {
        Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        Minecraft.getInstance().execute(() ->
            Minecraft.getInstance().gui.setScreen(new TastyFishCommandKeysScreen(configDir)));
        return 1;
    }

    private int openCustom() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> TastyFishCustom.open(mc, mc.gui.screen()));
        return 1;
    }

    private int customRename(String value) {
        Minecraft mc = Minecraft.getInstance();
        TastyFishCustom.setName(TastyFishCustom.held(mc), value);
        return 1;
    }

    private int customDye(String value) {
        Minecraft mc = Minecraft.getInstance();
        try {
            TastyFishCustom.setDye(TastyFishCustom.held(mc), value.isBlank() ? null : TastyFishCustom.parseHex(value));
        } catch (Exception ignored) {}
        return 1;
    }

    private int customTrim(String material, String pattern) {
        Minecraft mc = Minecraft.getInstance();
        TastyFishCustom.setTrim(TastyFishCustom.held(mc), material, pattern);
        return 1;
    }

    private int customAnimated(String a, String b, String duration, String cycleBack, String delay) {
        Minecraft mc = Minecraft.getInstance();
        try {
            TastyFishCustom.setAnimatedDye(TastyFishCustom.held(mc),
                TastyFishCustom.parseHex(a), TastyFishCustom.parseHex(b),
                Float.parseFloat(duration), Boolean.parseBoolean(cycleBack), Float.parseFloat(delay));
        } catch (Exception ignored) {}
        return 1;
    }

    private int openStorageSearch() {
        Minecraft.getInstance().execute(() ->
            TastyFishStorageSearch.open(Minecraft.getInstance(), ""));
        return 1;
    }

    private int openNotes() {
        Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        Minecraft.getInstance().execute(() ->
            Minecraft.getInstance().gui.setScreen(new TastyFishNotesScreen(configDir)));
        return 1;
    }

    private void startGameSession(Minecraft minecraft) {
        if (gameSessionStarted) return;
        gameSessionStarted = true;
        gameSessionStartedWallMillis = System.currentTimeMillis();
        lastUploadMillis = 0L;
        lastActiveMillis = -1L;
        lastSnapshotWallMillis = 0L;
        lastOneHourPbAlertActiveMillis = -1L;
        sessionId = TastyFishServerClient.newSessionId();

        String username = minecraft.getUser() == null ? "" : minecraft.getUser().getName();
        TastyFishVersionChecker.check(minecraft);
        System.out.println("[TastyFish] Minecraft game session started: " + sessionId + " username=" + username);
    }

    private void tick(Minecraft minecraft) {
        long now = System.currentTimeMillis();
        TastyFishCommandKeys.tick(minecraft);
        TastyFishStorageSearch.tick(minecraft);
        TastyFishCustom.tick(minecraft);
        if (!gameSessionStarted || minecraft.player == null) return;

        if (now - lastUploadMillis < config.uploadIntervalSeconds * 1000L) {
            return;
        }
        lastUploadMillis = now;

        SkysoftSessionReader.Snapshot snapshot = SkysoftSessionReader.read();
        if (!snapshot.valid()) return;

        if (lastActiveMillis >= 0L && snapshot.activeMillis() < lastActiveMillis) {
            history.rebaseline(sessionId, snapshot);
            lastActiveMillis = -1L;
            lastOneHourPbAlertActiveMillis = -1L;
        }

        lastActiveMillis = snapshot.activeMillis();
        lastSnapshot = snapshot;
        lastSnapshotWallMillis = now;

        if (config.farmingAnalyticsEnabled) processAnalytics(minecraft, snapshot);

        String username = minecraft.getUser().getName();
        UUID uuid = minecraft.getUser().getProfileId();
        farmingServer.upload(config, username, uuid, currentSkysoftProfile(), sessionId, snapshot);
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
        }

        if (update.newStreak() && config.farmingStreakEnabled && isStreakMilestone(update.streakMs())) {
        }

        if (config.farmingAchievementsEnabled) {
            List<String> unlocked = history.newlyUnlockedAchievements();
            for (String id : unlocked) {
                String name = achievementName(id);
                minecraft.showDebugChat(net.minecraft.network.chat.Component.literal(
                    "§d§lACHIEVEMENT UNLOCKED! §f" + name));
            }
        }
    }

    private void finishGameSession(String reason) {
        if (!gameSessionStarted) return;

        SkysoftSessionReader.Snapshot snapshot = lastSnapshot;
        if (snapshot != null) history.finish(reason, snapshot);

        gameSessionStarted = false;
        gameSessionStartedWallMillis = 0L;
        lastSnapshot = null;
        lastSnapshotWallMillis = 0L;
        lastActiveMillis = -1L;
        lastOneHourPbAlertActiveMillis = -1L;
        System.out.println("[TastyFish] Minecraft game session ended: " + sessionId + " reason=" + reason);
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
        } catch (Throwable ignored) {
            return "";
        }
    }
}

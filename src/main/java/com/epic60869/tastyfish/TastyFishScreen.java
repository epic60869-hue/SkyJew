package com.epic60869.tastyfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** Clean, compact SkyHanni/SkySoft-inspired TastyFish control centre. */
public final class TastyFishScreen extends Screen {
    private static final int BG = 0xFF080B12;
    private static final int SIDEBAR = 0xFF0B111B;
    private static final int PANEL = 0xF4141B27;
    private static final int PANEL_2 = 0xF70C121C;
    private static final int BORDER = 0xFF26364D;
    private static final int TEXT = 0xFFF1F4FF;
    private static final int MUTED = 0xFF8D9AAF;
    private static final int CYAN = 0xFF58D8FF;
    private static final int PURPLE = 0xFF9A6CFF;
    private static final int YELLOW = 0xFFFFD34D;
    private static final int GREEN = 0xFF35E39B;
    private static final int RED = 0xFFFF657A;

    private static final int MAIN = 0;
    private static final int FARMING = 1;
    private static final int HUD = 2;
    private static final int SESSION = 3;
    private static final int PB = 4;
    private static final int STREAK = 5;
    private static final int ACHIEVEMENTS = 6;
    private static final int DISCORD = 7;
    private static final int SETTINGS = 8;

    private final TastyFishConfig config;
    private final FarmingHistory history;
    private int page = FARMING;
    private long nextSkysoftRead;
    private SkysoftSessionReader.Snapshot snapshot = SkysoftSessionReader.Snapshot.empty();
    private boolean skysoftAvailable;

    private EditBox discordChannelBox;
    private EditBox discordForumBox;

    public TastyFishScreen(TastyFishConfig config) {
        super(Component.literal("TastyFish"));
        this.config = config;
        this.history = new FarmingHistory(Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("tastyfish-farming.json"));
    }

    @Override
    protected void init() {
        clearWidgets();
        discordChannelBox = null;
        discordForumBox = null;

        if (page == DISCORD) {
            int left = contentLeft() + 30;
            int boxWidth = 180;
            discordChannelBox = field(left, 150, boxWidth, config.discordChannelId, "Discord channel ID");
            discordForumBox = field(left, 215, boxWidth, config.discordForumId, "Discord forum channel ID");
        }
        refreshSkysoft(true);
    }

    private EditBox field(int x, int y, int width, String value, String hint) {
        EditBox box = new EditBox(font, x, y, width, 24, Component.literal(hint));
        box.setValue(value == null ? "" : value);
        box.setHint(Component.literal(hint));
        addRenderableWidget(box);
        return box;
    }

    private void rebuild() { init(); }

    private void refreshSkysoft(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now < nextSkysoftRead) return;
        nextSkysoftRead = now + 1000L;
        snapshot = SkysoftSessionReader.read();
        skysoftAvailable = snapshot.valid();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        refreshSkysoft(false);
        resizeDiscordFields();
        drawBackground(graphics);
        drawSidebar(graphics, mouseX, mouseY);
        drawHeader(graphics);
        drawPage(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }


    private void resizeDiscordFields() {
        if (page != DISCORD) return;
        int collapsed = 180;
        int expanded = Math.min(520, Math.max(260, contentWidth() - 80));
        if (discordChannelBox != null) discordChannelBox.setWidth(discordChannelBox.isFocused() ? expanded : collapsed);
        if (discordForumBox != null) discordForumBox.setWidth(discordForumBox.isFocused() ? expanded : collapsed);
    }

    private void drawBackground(GuiGraphicsExtractor g) {
        g.fill(0, 0, width, height, BG);
        g.fill(0, 0, width, 2, 0xFF5D3CFF);
        g.fill(0, height - 1, width, height, 0xFF152033);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mx, int my) {
        int sw = 185;
        g.fill(0, 0, sw, height, SIDEBAR);
        g.fill(sw - 1, 0, sw, height, BORDER);

        g.text(font, "✦", 22, 20, CYAN, true);
        g.text(font, "TastyFish", 45, 16, YELLOW, true);
        g.text(font, "SkyBlock Farming", 45, 32, MUTED, false);

        int y = 66;
        y = nav(g, "⌂", "Dashboard", MAIN, y, mx, my);
        y = nav(g, "❖", "Farming", FARMING, y, mx, my);
        y = nav(g, "◈", "Guild HUD", HUD, y, mx, my);
        y = nav(g, "◷", "Sessions", SESSION, y, mx, my);
        y = nav(g, "★", "Personal Bests", PB, y, mx, my);
        y = nav(g, "♨", "Streaks", STREAK, y, mx, my);
        y = nav(g, "☆", "Achievements", ACHIEVEMENTS, y, mx, my);
        y = nav(g, "◉", "Discord", DISCORD, y, mx, my);
        y = nav(g, "⚙", "Settings", SETTINGS, y, mx, my);

        int bottom = height - 72;
        g.fill(16, bottom, sw - 16, bottom + 1, BORDER);
        g.text(font, "Farm • Track • Improve", 22, bottom + 14, MUTED, false);
        g.text(font, skysoftAvailable ? "SkySoft connected" : "SkySoft waiting", 22, bottom + 31,
            skysoftAvailable ? GREEN : RED, false);
    }

    private int nav(GuiGraphicsExtractor g, String icon, String label, int id, int y, int mx, int my) {
        boolean selected = page == id;
        boolean hover = mx >= 10 && mx <= 174 && my >= y && my <= y + 30;
        if (selected) {
            g.fill(10, y, 174, y + 30, 0xFF6742D9);
            g.fill(10, y, 13, y + 30, CYAN);
        } else if (hover) {
            g.fill(10, y, 174, y + 30, 0xFF172233);
        }
        g.text(font, icon, 22, y + 8, selected ? TEXT : CYAN, true);
        g.text(font, label, 46, y + 8, selected ? TEXT : 0xFFD5DCEA, false);
        return y + 34;
    }

    private void drawHeader(GuiGraphicsExtractor g) {
        int left = contentLeft();
        g.text(font, pageTitle(), left, 15, TEXT, true);
        g.text(font, pageSubtitle(), left, 31, MUTED, false);

        int sx = width - 230;
        g.fill(sx, 10, width - 16, 42, PANEL_2);
        outline(g, sx, 10, width - 16, 42, BORDER);
        g.text(font, "26.2", sx + 14, 19, 0xFF8FA3C2, false);
        g.text(font, "•", sx + 54, 19, skysoftAvailable ? GREEN : RED, true);
        g.text(font, skysoftAvailable ? "SkySoft Connected" : "SkySoft Waiting", sx + 67, 19,
            skysoftAvailable ? GREEN : RED, true);
    }

    private void drawPage(GuiGraphicsExtractor g, int mx, int my) {
        switch (page) {
            case MAIN -> renderMain(g);
            case FARMING -> renderFarming(g);
            case HUD -> renderHudPage(g);
            case SESSION -> renderSession(g);
            case PB -> renderPersonalBest(g);
            case STREAK -> renderStreak(g);
            case ACHIEVEMENTS -> renderAchievements(g);
            case DISCORD -> renderDiscord(g);
            case SETTINGS -> renderSettings(g);
            default -> renderFarming(g);
        }
    }

    private void renderMain(GuiGraphicsExtractor g) {
        int left = contentLeft();
        int right = width - 16;
        int gap = 12;
        int cardW = (contentWidth() - gap * 2) / 3;
        statCard(g, left, 58, cardW, "Current Crop", currentCrop(), YELLOW);
        statCard(g, left + cardW + gap, 58, cardW, "Session Profit", coins(snapshot.profit()), GREEN);
        statCard(g, left + (cardW + gap) * 2, 58, cardW, "Active Time", duration(snapshot.activeMillis()), CYAN);

        int top = 150;
        int bottom = height - 18;
        int leftW = (contentWidth() - 12) * 3 / 5;
        panel(g, left, top, left + leftW, bottom, "Live Farming", CYAN);
        drawStat(g, left + 24, top + 58, "Actions", number(snapshot.actions()), PURPLE);
        drawStat(g, left + 24, top + 108, "Tracked Items", number(snapshot.valuedItems()), CYAN);
        drawStat(g, left + 24, top + 158, "Pests", number(sum(snapshot.pests())), YELLOW);
        drawStat(g, left + 250, top + 58, "1h Personal Best", coins(history.data().bestOneHourProfit), YELLOW);
        drawStat(g, left + 250, top + 108, "Best Streak", duration(history.data().bestStreakMs), PURPLE);
        drawStat(g, left + 250, top + 158, "Sessions", number(history.data().sessions.size()), CYAN);
        g.text(font, skysoftAvailable ? "Live SkySoft FARMING tracker detected" : "Enable SkySoft's Farming Profit Tracker",
            left + 24, top + 225, skysoftAvailable ? GREEN : RED, true);
        g.text(font, "ProfitTrackerStatistics.sessionStats", left + 24, top + 247, MUTED, false);

        int rightX = left + leftW + 12;
        panel(g, rightX, top, right, bottom, "Guild Collection", PURPLE);
        TastyFishWebsiteClient.Result guild = getGuildResult();
        if (guild.available()) {
            g.text(font, guild.boardName(), rightX + 22, top + 58, YELLOW, true);
            g.text(font, format(guild.value()) + "  [#" + guild.position() + "]", rightX + 22, top + 83, TEXT, true);
            if (guild.hasAheadPlayer()) {
                g.text(font, format(guild.behind()) + " behind " + guild.aheadName(), rightX + 22, top + 122, CYAN, false);
                g.text(font, "[#" + guild.aheadPosition() + "]", rightX + 22, top + 143, MUTED, false);
            }
        } else {
            g.text(font, "Waiting for tastyfish.org", rightX + 22, top + 62, MUTED, false);
            g.text(font, "The HUD automatically matches the current crop", rightX + 22, top + 91, MUTED, false);
            g.text(font, "to an enabled guild collection leaderboard.", rightX + 22, top + 109, MUTED, false);
        }
    }

    private void renderFarming(GuiGraphicsExtractor g) {
        int left = contentLeft();
        int right = width - 16;
        int mid = left + (contentWidth() * 3 / 5);
        int bottom = height - 18;

        panel(g, left, 58, mid, bottom, "Farming", CYAN);
        toggleRow(g, left + 24, 106, "Enable Farming Features", config.enabled, "Main TastyFish features", 0);
        toggleRow(g, left + 24, 156, "Farming RNG Overlay", config.farmingRngEnabled, "Rare farming drops", 1);
        toggleRow(g, left + 24, 206, "RNG Background", config.farmingRngBackground, "Dark background behind drops", 2);
        toggleRow(g, left + 24, 256, "Farming Analytics", config.farmingAnalyticsEnabled, "Sessions, PBs, streaks and achievements", 3);
        toggleRow(g, left + 24, 326, "Session Recorder", config.farmingSessionRecorderEnabled, "Save completed sessions locally", 4);
        toggleRow(g, left + 24, 376, "Personal Bests", config.farmingPersonalBestEnabled, "Track rolling one-hour PB", 5);
        toggleRow(g, left + 24, 426, "Farming Streak", config.farmingStreakEnabled, "Track continuous farming time", 6);
        toggleRow(g, left + 24, 476, "Achievements", config.farmingAchievementsEnabled, "Unlock farming milestones", 7);

        panel(g, mid + 12, 58, right, bottom, "Preview", PURPLE);
        int px = mid + 34;
        int py = 106;
        g.fill(px, py, right - 22, py + 170, PANEL_2);
        outline(g, px, py, right - 22, py + 170, BORDER);
        g.text(font, "❖ FARMING", px + 18, py + 18, GREEN, true);
        g.text(font, "Crop: " + currentCrop(), px + 18, py + 48, TEXT, false);
        g.text(font, "Session: " + duration(snapshot.activeMillis()), px + 18, py + 70, CYAN, false);
        g.text(font, "Profit: " + coins(snapshot.profit()), px + 18, py + 92, GREEN, false);
        g.text(font, "Guild HUD: " + (config.guildLeaderboardHudEnabled ? "ON" : "OFF"), px + 18, py + 114,
            config.guildLeaderboardHudEnabled ? PURPLE : MUTED, false);
        g.text(font, "Use Guild HUD for the collection gap display.", px + 18, py + 145, MUTED, false);
    }

    private void renderHudPage(GuiGraphicsExtractor g) {
        int left = contentLeft();
        int right = width - 16;
        int bottom = height - 18;
        int split = left + contentWidth() * 3 / 5;

        panel(g, left, 58, split, bottom, "Guild Collection HUD", PURPLE);
        toggleRow(g, left + 24, 106, "Enable Guild HUD", config.guildLeaderboardHudEnabled,
            "Show the collection gap while farming", 20);
        g.text(font, "Website", left + 24, 174, MUTED, false);
        g.text(font, "Public read-only leaderboard data from tastyfish.org", left + 24, 191, TEXT, false);
        g.text(font, "Refresh", left + 24, 245, MUTED, false);
        g.text(font, config.guildLeaderboardRefreshSeconds + " seconds", left + 24, 264, TEXT, false);
        g.text(font, "Position", left + 24, 315, MUTED, false);
        g.text(font, config.guildLeaderboardHudX + ", " + config.guildLeaderboardHudY, left + 24, 334, TEXT, false);
        g.text(font, "Scale", left + 24, 385, MUTED, false);
        g.text(font, String.format(Locale.ROOT, "%.1fx", config.guildLeaderboardHudScale), left + 24, 404, TEXT, false);
        g.text(font, "The HUD automatically follows the current SkySoft farming crop.", left + 24, 470, MUTED, false);
        g.text(font, "It only uses guild leaderboard rows from the website.", left + 24, 490, MUTED, false);

        panel(g, split + 12, 58, right, bottom, "Live Preview", CYAN);
        int px = split + 36;
        int py = 118;
        g.fill(px, py, right - 24, py + 120, 0xB6070B11);
        outline(g, px, py, right - 24, py + 120, BORDER);
        TastyFishGuildLeaderboardHud.renderPreview(g, px + 18, py + 20);
        g.text(font, "Only two lines are shown in-game:", px + 18, py + 86, MUTED, false);
        g.text(font, "collection + placement, then the member ahead + gap", px + 18, py + 103, MUTED, false);
    }

    private void renderSession(GuiGraphicsExtractor g) {
        int left = contentLeft();
        int right = width - 16;
        panel(g, left, 58, right, height - 18, "Recent Sessions", CYAN);
        List<FarmingHistory.Session> sessions = history.data().sessions;
        if (sessions.isEmpty()) {
            g.text(font, "No completed sessions yet.", left + 24, 108, MUTED, false);
            return;
        }
        int y = 105;
        int start = Math.max(0, sessions.size() - 10);
        for (int i = sessions.size() - 1; i >= start; i--) {
            FarmingHistory.Session s = sessions.get(i);
            g.fill(left + 18, y - 7, right - 18, y + 37, PANEL_2);
            g.text(font, s.crop(), left + 30, y, YELLOW, true);
            g.text(font, duration(s.activeMillis()), left + 220, y, TEXT, false);
            g.text(font, coinsDouble(s.profit()), left + 330, y, GREEN, false);
            g.text(font, s.reason(), right - 145, y, MUTED, false);
            y += 50;
        }
    }

    private void renderPersonalBest(GuiGraphicsExtractor g) {
        int left = contentLeft();
        int right = width - 16;
        panel(g, left, 58, right, 250, "1-Hour Personal Best", YELLOW);
        g.text(font, coins(history.data().bestOneHourProfit), left + 26, 112, YELLOW, true);
        g.text(font, "rolling one-hour farming profit", left + 26, 138, MUTED, false);
        g.text(font, "Best crop", left + 300, 104, MUTED, false);
        g.text(font, history.data().bestOneHourCrop, left + 300, 124, TEXT, true);
        g.text(font, "Recorded", left + 300, 154, MUTED, false);
        g.text(font, history.data().bestOneHourAt == 0 ? "Not yet" : formatEpoch(history.data().bestOneHourAt), left + 300, 174, TEXT, false);
        drawProgress(g, left + 26, 205, contentWidth() - 52, 10, Math.min(1f, history.data().bestOneHourProfit / 10_000_000f), YELLOW);

        panel(g, left, 270, right, height - 18, "How it works", PURPLE);
        g.text(font, "TastyFish samples SkySoft's cumulative FARMING session profit.", left + 26, 315, TEXT, false);
        g.text(font, "The rolling one-hour value is saved locally and survives restarts.", left + 26, 340, MUTED, false);
    }

    private void renderStreak(GuiGraphicsExtractor g) {
        int left = contentLeft();
        int right = width - 16;
        panel(g, left, 58, right, 265, "Farming Streak", PURPLE);
        g.text(font, duration(history.data().bestStreakMs), left + 26, 115, PURPLE, true);
        g.text(font, "best continuous farming time", left + 26, 141, MUTED, false);
        drawProgress(g, left + 26, 185, contentWidth() - 52, 10,
            Math.min(1f, history.data().bestStreakMs / (5f * 3600000f)), PURPLE);
        g.text(font, "A gap longer than five minutes without progress ends the streak.", left + 26, 225, TEXT, false);

        panel(g, left, 285, right, height - 18, "Milestones", CYAN);
        milestone(g, left + 26, 330, "30 minutes", history.data().bestStreakMs >= 30 * 60000L);
        milestone(g, left + 26, 370, "1 hour", history.data().bestStreakMs >= 60 * 60000L);
        milestone(g, left + 26, 410, "2 hours", history.data().bestStreakMs >= 2 * 60 * 60000L);
        milestone(g, left + 26, 450, "5 hours", history.data().bestStreakMs >= 5 * 60 * 60000L);
        milestone(g, left + 26, 490, "10 hours", history.data().bestStreakMs >= 10 * 60 * 60000L);
    }

    private void renderAchievements(GuiGraphicsExtractor g) {
        int left = contentLeft();
        int right = width - 16;
        panel(g, left, 58, right, height - 18, "Farming Achievements", YELLOW);
        String[][] entries = {
            {"FIRST_HARVEST", "First Harvest", "Track your first crop"},
            {"MILLION_CROPS", "Million Crops", "Track 1,000,000 crops"},
            {"TEN_MILLION_CROPS", "Ten Million Crops", "Track 10,000,000 crops"},
            {"MILLIONAIRE", "Farming Millionaire", "Earn 1,000,000 farming coins"},
            {"BILLIONAIRE", "Farming Billionaire", "Earn 1,000,000,000 farming coins"},
            {"ONE_HOUR_FARMER", "One-Hour Farmer", "Reach a 1,000,000 coin one-hour PB"},
            {"FIVE_HOUR_STREAK", "Five-Hour Streak", "Farm continuously for five hours"},
            {"PEST_CONTROL", "Pest Control", "Track 1,000 pest kills"}
        };
        int y = 100;
        for (String[] e : entries) {
            boolean unlocked = history.data().unlockedAchievements.contains(e[0]);
            g.fill(left + 18, y - 7, right - 18, y + 42, unlocked ? 0xFF182B28 : PANEL_2);
            g.text(font, unlocked ? "★" : "☆", left + 30, y + 7, unlocked ? YELLOW : MUTED, true);
            g.text(font, e[1], left + 62, y, unlocked ? YELLOW : TEXT, true);
            g.text(font, e[2], left + 62, y + 19, MUTED, false);
            g.text(font, unlocked ? "UNLOCKED" : "LOCKED", right - 92, y + 7, unlocked ? GREEN : MUTED, true);
            y += 57;
        }
    }

    private void renderDiscord(GuiGraphicsExtractor g) {
        int left = contentLeft();
        int right = width - 16;
        panel(g, left, 58, right, height - 18, "Discord Reports", PURPLE);
        toggleRow(g, left + 24, 104, "Enable Discord Reports", config.discordForumEnabled,
            "Relay reports through the TastyFish website/bot", 30);
        g.text(font, "Channel ID", left + 24, 133, MUTED, false);
        g.text(font, "Forum ID", left + 24, 198, MUTED, false);
        g.text(font, "Website (fixed)", left + 24, 283, MUTED, false);
        g.text(font, "https://tastyfish.org", left + 24, 301, TEXT, false);
        g.text(font, "Report Types", left + 24, 360, MUTED, false);
        checkbox(g, left + 24, 386, "Sessions", config.discordSendSessions);
        checkbox(g, left + 140, 386, "PBs", config.discordSendPersonalBests);
        checkbox(g, left + 220, 386, "Streaks", config.discordSendStreaks);
        checkbox(g, left + 330, 386, "Achievements", config.discordSendAchievements);

        panel(g, left + contentWidth() / 2, 58, right, height - 18, "How Discord Works", CYAN);
        int rx = left + contentWidth() / 2 + 22;
        g.text(font, "No webhook is stored in the mod.", rx, 108, TEXT, true);
        g.text(font, "The mod sends the report to tastyfish.org", rx, 140, MUTED, false);
        g.text(font, "with the target Discord channel/forum IDs.", rx, 160, MUTED, false);
        g.text(font, "The main TastyFish Discord bot posts it.", rx, 192, GREEN, false);
        g.text(font, "Channel ID = normal text channel", rx, 235, TEXT, false);
        g.text(font, "Forum ID = forum channel for new posts", rx, 255, TEXT, false);
        g.text(font, "Channel/forum IDs are local settings; the bot token stays on the server.", rx, 300, 0xFFFFB85A, false);
    }

    private void renderSettings(GuiGraphicsExtractor g) {
        int left = contentLeft();
        int right = width - 16;
        panel(g, left, 58, right, height - 18, "Settings", CYAN);
        g.text(font, "SkySoft endpoint", left + 24, 104, MUTED, false);
        g.text(font, config.endpoint, left + 24, 124, TEXT, false);
        g.text(font, "Upload interval", left + 24, 165, MUTED, false);
        g.text(font, config.uploadIntervalSeconds + " seconds", left + 24, 185, TEXT, false);
        g.text(font, "Local analytics", left + 24, 225, MUTED, false);
        g.text(font, "config/tastyfish-farming.json", left + 24, 245, CYAN, false);
        g.text(font, "Guild HUD commands", left + 24, 285, MUTED, false);
        g.text(font, "/tf stats  •  /tf discord", left + 24, 305, TEXT, false);
        g.text(font, "Discord relay secret", left + 24, 345, MUTED, false);
        g.text(font, config.discordReportSecret.isBlank() ? "Not configured" : "Configured", left + 24, 365,
            config.discordReportSecret.isBlank() ? MUTED : GREEN, false);
        g.text(font, "Edit it in config/tastyfish-mod.json; it is never sent to Discord.", left + 24, 400, MUTED, false);
    }

    private void panel(GuiGraphicsExtractor g, int l, int t, int r, int b, String title, int accent) {
        g.fill(l, t, r, b, PANEL);
        outline(g, l, t, r, b, BORDER);
        g.fill(l, t, r, t + 2, accent);
        g.text(font, title, l + 20, t + 19, TEXT, true);
    }

    private void statCard(GuiGraphicsExtractor g, int x, int y, int w, String label, String value, int accent) {
        g.fill(x, y, x + w, y + 72, PANEL);
        outline(g, x, y, x + w, y + 72, BORDER);
        g.fill(x, y, x + 3, y + 72, accent);
        g.text(font, label, x + 16, y + 15, MUTED, false);
        g.text(font, value, x + 16, y + 38, accent, true);
    }

    private void toggleRow(GuiGraphicsExtractor g, int x, int y, String title, boolean enabled, String subtitle, int rowId) {
        g.text(font, title, x, y, TEXT, false);
        g.text(font, subtitle, x, y + 17, MUTED, false);
        int tx = Math.min(width - 100, x + 500);
        toggle(g, tx, y - 5, enabled);
    }

    private void toggle(GuiGraphicsExtractor g, int x, int y, boolean enabled) {
        g.fill(x, y, x + 54, y + 24, enabled ? 0xFF147A64 : 0xFF252B38);
        outline(g, x, y, x + 54, y + 24, enabled ? GREEN : 0xFF4C5669);
        g.fill(enabled ? x + 31 : x + 4, y + 4, enabled ? x + 50 : x + 23, y + 20,
            enabled ? GREEN : 0xFF8D96A7);
        g.text(font, enabled ? "ON" : "OFF", x - 36, y + 7, enabled ? GREEN : MUTED, true);
    }

    private void checkbox(GuiGraphicsExtractor g, int x, int y, String label, boolean checked) {
        g.fill(x, y, x + 14, y + 14, checked ? PURPLE : 0xFF1A2230);
        outline(g, x, y, x + 14, y + 14, checked ? PURPLE : BORDER);
        if (checked) g.text(font, "✓", x + 2, y - 1, TEXT, true);
        g.text(font, label, x + 22, y, TEXT, false);
    }

    private void drawStat(GuiGraphicsExtractor g, int x, int y, String label, String value, int accent) {
        g.text(font, label, x, y, MUTED, false);
        g.text(font, value, x, y + 20, accent, true);
    }

    private void drawProgress(GuiGraphicsExtractor g, int x, int y, int w, int h, float value, int accent) {
        g.fill(x, y, x + w, y + h, 0xFF1A2432);
        g.fill(x, y, x + Math.max(2, Math.round(w * Math.max(0f, Math.min(1f, value)))), y + h, accent);
    }

    private void milestone(GuiGraphicsExtractor g, int x, int y, String label, boolean unlocked) {
        g.text(font, unlocked ? "★" : "☆", x, y, unlocked ? YELLOW : MUTED, true);
        g.text(font, label, x + 28, y, unlocked ? TEXT : MUTED, false);
        g.text(font, unlocked ? "Reached" : "Locked", x + 150, y, unlocked ? GREEN : MUTED, false);
    }

    private void outline(GuiGraphicsExtractor g, int l, int t, int r, int b, int color) {
        g.fill(l, t, r, t + 1, color);
        g.fill(l, b - 1, r, b, color);
        g.fill(l, t, l + 1, b, color);
        g.fill(r - 1, t, r, b, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int x = (int) event.x();
        int y = (int) event.y();

        if (x >= 10 && x <= 174) {
            int navY = 66;
            int[] ids = {MAIN, FARMING, HUD, SESSION, PB, STREAK, ACHIEVEMENTS, DISCORD, SETTINGS};
            for (int id : ids) {
                if (y >= navY && y <= navY + 30) {
                    saveFields();
                    page = id;
                    rebuild();
                    return true;
                }
                navY += 34;
            }
        }

        if (page == FARMING) {
            int toggleX = Math.min(width - 100, contentLeft() + 24 + 500);
            if (x >= toggleX - 8 && x <= toggleX + 62) {
                int[] rows = {106, 156, 206, 256, 326, 376, 426, 476};
                int row = nearestRow(y, rows);
                if (row >= 0) {
                    switch (row) {
                        case 0 -> config.enabled = !config.enabled;
                        case 1 -> config.farmingRngEnabled = !config.farmingRngEnabled;
                        case 2 -> config.farmingRngBackground = !config.farmingRngBackground;
                        case 3 -> config.farmingAnalyticsEnabled = !config.farmingAnalyticsEnabled;
                        case 4 -> config.farmingSessionRecorderEnabled = !config.farmingSessionRecorderEnabled;
                        case 5 -> config.farmingPersonalBestEnabled = !config.farmingPersonalBestEnabled;
                        case 6 -> config.farmingStreakEnabled = !config.farmingStreakEnabled;
                        case 7 -> config.farmingAchievementsEnabled = !config.farmingAchievementsEnabled;
                    }
                    save();
                    return true;
                }
            }
        }

        if (page == HUD) {
            int toggleX = Math.min(width - 100, contentLeft() + 24 + 500);
            if (x >= toggleX - 8 && x <= toggleX + 62 && y >= 96 && y <= 135) {
                config.guildLeaderboardHudEnabled = !config.guildLeaderboardHudEnabled;
                save();
                return true;
            }
        }

        if (page == DISCORD) {
            if (y >= 90 && y <= 135) {
                config.discordForumEnabled = !config.discordForumEnabled;
                save();
                return true;
            }
            if (y >= 375 && y <= 410) {
                int base = contentLeft() + 24;
                if (x >= base && x < base + 115) config.discordSendSessions = !config.discordSendSessions;
                else if (x >= base + 115 && x < base + 195) config.discordSendPersonalBests = !config.discordSendPersonalBests;
                else if (x >= base + 195 && x < base + 305) config.discordSendStreaks = !config.discordSendStreaks;
                else if (x >= base + 305 && x < base + 450) config.discordSendAchievements = !config.discordSendAchievements;
                save();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private int nearestRow(int y, int[] rows) {
        for (int i = 0; i < rows.length; i++) if (y >= rows[i] - 12 && y <= rows[i] + 30) return i;
        return -1;
    }

    @Override
    public void onClose() {
        saveFields();
        save();
        super.onClose();
    }

    private void saveFields() {
        if (discordChannelBox != null) config.discordChannelId = discordChannelBox.getValue().trim();
        if (discordForumBox != null) config.discordForumId = discordForumBox.getValue().trim();
    }

    private void save() {
        Minecraft mc = Minecraft.getInstance();
        config.save(mc.gameDirectory.toPath().resolve("config").resolve("tastyfish-mod.json"));
    }

    private int contentLeft() { return 205; }
    private int contentWidth() { return Math.max(400, width - contentLeft() - 16); }

    private String pageTitle() {
        return switch (page) {
            case MAIN -> "Dashboard";
            case FARMING -> "Farming";
            case HUD -> "Guild Collection HUD";
            case SESSION -> "Sessions";
            case PB -> "Personal Bests";
            case STREAK -> "Streaks";
            case ACHIEVEMENTS -> "Achievements";
            case DISCORD -> "Discord Reports";
            case SETTINGS -> "Settings";
            default -> "TastyFish";
        };
    }

    private String pageSubtitle() {
        return switch (page) {
            case MAIN -> "Live SkySoft tracking and guild progress";
            case FARMING -> "Configure farming features without the clutter";
            case HUD -> "Guild-only collection gap display from tastyfish.org";
            case SESSION -> "Your locally recorded farming sessions";
            case PB -> "Rolling one-hour farming records";
            case STREAK -> "Continuous farming milestones";
            case ACHIEVEMENTS -> "Milestones unlocked from farming history";
            case DISCORD -> "Target a Discord channel or forum by ID";
            case SETTINGS -> "Connection, storage and command information";
            default -> "";
        };
    }

    private String currentCrop() {
        String best = "Waiting";
        long count = 0L;
        for (var entry : snapshot.items().entrySet()) {
            long value = entry.getValue() == null ? 0L : entry.getValue();
            if (value > count) { count = value; best = prettyId(entry.getKey()); }
        }
        return best;
    }

    private TastyFishWebsiteClient.Result getGuildResult() {
        // The HUD owns the cached website result; use the public preview data only when unavailable.
        return TastyFishGuildLeaderboardHudResultHolder.result();
    }

    private static long sum(java.util.Map<String, Long> map) {
        long total = 0L;
        if (map == null) return 0L;
        for (Long value : map.values()) if (value != null) total += value;
        return total;
    }

    private static String prettyId(String id) {
        if (id == null || id.isBlank()) return "Unknown";
        return id.toLowerCase(Locale.ROOT).replace("minecraft:", "").replace('_', ' ').replace(" item", "");
    }

    private static String number(long value) { return String.format(Locale.ROOT, "%,d", value); }
    private static String format(long value) { return String.format(Locale.ROOT, "%,d", Math.max(0L, value)); }
    private static String coins(double value) { return String.format(Locale.ROOT, "%,.0f coins", value); }
    private static String coinsDouble(double value) { return String.format(Locale.ROOT, "%,.0f coins", value); }

    private static String duration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long h = totalSeconds / 3600L;
        long m = (totalSeconds % 3600L) / 60L;
        long s = totalSeconds % 60L;
        return h > 0 ? String.format(Locale.ROOT, "%dh %02dm", h, m) : String.format(Locale.ROOT, "%dm %02ds", m, s);
    }

    private static String formatEpoch(long millis) {
        return java.time.Instant.ofEpochMilli(millis).toString().replace('T', ' ').replace('Z', ' ');
    }

    /** Small bridge to keep the screen independent of the HUD's renderer internals. */
    private static final class TastyFishGuildLeaderboardHudResultHolder {
        private static TastyFishWebsiteClient.Result result() {
            try {
                java.lang.reflect.Field field = TastyFishGuildLeaderboardHud.class.getDeclaredField("WEBSITE");
                field.setAccessible(true);
                TastyFishWebsiteClient client = (TastyFishWebsiteClient) field.get(null);
                return client.result();
            } catch (Throwable ignored) {
                return TastyFishWebsiteClient.Result.empty();
            }
        }
    }
}

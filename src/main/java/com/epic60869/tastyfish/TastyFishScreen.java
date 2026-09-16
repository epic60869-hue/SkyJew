package com.epic60869.tastyfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** SkyHanni/Skysoft-inspired TastyFish control centre. */
public final class TastyFishScreen extends Screen {
    private static final int BG = 0xFF080A10;
    private static final int PANEL = 0xE9161C29;
    private static final int PANEL_2 = 0xE90D1420;
    private static final int BORDER = 0xFF24344D;
    private static final int TEXT = 0xFFF1F4FF;
    private static final int MUTED = 0xFF8E99AD;
    private static final int CYAN = 0xFF58D8FF;
    private static final int PURPLE = 0xFF9A6CFF;
    private static final int YELLOW = 0xFFFFD34D;
    private static final int GREEN = 0xFF35E39B;
    private static final int RED = 0xFFFF657A;

    private static final int MAIN = 0;
    private static final int FARMING = 1;
    private static final int SESSION = 2;
    private static final int PB = 3;
    private static final int STREAK = 4;
    private static final int ACHIEVEMENTS = 5;
    private static final int DISCORD = 6;
    private static final int SETTINGS = 7;

    private final TastyFishConfig config;
    private final FarmingHistory history;
    private int page = FARMING;
    private EditBox webhookBox;
    private long nextSkysoftRead;
    private SkysoftSessionReader.Snapshot snapshot = SkysoftSessionReader.Snapshot.empty();
    private boolean skysoftAvailable;

    public TastyFishScreen(TastyFishConfig config) {
        super(Component.literal("TastyFish"));
        this.config = config;
        this.history = new FarmingHistory(Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("tastyfish-farming.json"));
    }

    @Override
    protected void init() {
        clearWidgets();
        webhookBox = null;
        if (page == DISCORD) {
            webhookBox = new EditBox(font, 680, 455, Math.max(180, Math.min(430, width - 900)), 24,
                Component.literal("Discord forum webhook"));
            webhookBox.setValue(config.discordForumWebhook == null ? "" : config.discordForumWebhook);
            webhookBox.setHint(Component.literal("https://discord.com/api/webhooks/..."));
            addRenderableWidget(webhookBox);
        }
        refreshSkysoft(true);
    }

    private void rebuild() {
        init();
    }

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
        drawBackground(graphics);
        drawSidebar(graphics, mouseX, mouseY);
        drawHeader(graphics);
        drawPage(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawBackground(GuiGraphicsExtractor g) {
        g.fill(0, 0, width, height, BG);
        // Soft layered panels emulate the translucent Skysoft/SkyHanni look
        // without requiring shader support or external textures.
        g.fill(0, 0, width, 3, 0xFF5D3CFF);
        g.fill(0, 3, width, 5, 0xFF1C5E8A);
        g.fill(0, height - 2, width, height, 0xFF131C2A);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int sw = 205;
        g.fill(0, 0, sw, height, 0xF40B111B);
        g.fill(sw - 1, 0, sw, height, BORDER);

        g.text(font, "✦", 24, 24, CYAN, true);
        g.text(font, "TastyFish", 48, 20, YELLOW, true);
        g.text(font, "SkyBlock Farming Mod", 48, 35, MUTED, false);

        int y = 78;
        y = nav(g, "⌂", "Main", MAIN, y, mouseX, mouseY);
        y = nav(g, "❖", "Farming", FARMING, y, mouseX, mouseY);
        y = nav(g, "◈", "Session", SESSION, y, mouseX, mouseY);
        y = nav(g, "★", "Personal Bests", PB, y, mouseX, mouseY);
        y = nav(g, "♨", "Streaks", STREAK, y, mouseX, mouseY);
        y = nav(g, "☆", "Achievements", ACHIEVEMENTS, y, mouseX, mouseY);
        y = nav(g, "◉", "Discord", DISCORD, y, mouseX, mouseY);
        y = nav(g, "⚙", "Settings", SETTINGS, y, mouseX, mouseY);

        int by = height - 76;
        g.fill(16, by - 10, sw - 16, by + 1, BORDER);
        g.text(font, "TastyFish", 24, by + 12, YELLOW, true);
        g.text(font, "Farm • Track • Improve", 24, by + 27, MUTED, false);
        g.text(font, "SkySoft session integration", 24, by + 42, 0xFF66758C, false);
    }

    private int nav(GuiGraphicsExtractor g, String icon, String label, int id, int y, int mouseX, int mouseY) {
        boolean selected = page == id;
        boolean hover = mouseX >= 12 && mouseX <= 190 && mouseY >= y && mouseY <= y + 32;
        if (selected) {
            g.fill(12, y, 190, y + 32, 0xFF714BE6);
            g.fill(12, y, 16, y + 32, CYAN);
        } else if (hover) {
            g.fill(12, y, 190, y + 32, 0xFF192435);
        }
        g.text(font, icon, 25, y + 9, selected ? TEXT : CYAN, true);
        g.text(font, label, 50, y + 9, selected ? TEXT : 0xFFD3DBEA, false);
        return y + 38;
    }

    private void drawHeader(GuiGraphicsExtractor g) {
        int left = 225;
        g.text(font, pageTitle(), left, 20, TEXT, true);
        g.text(font, pageSubtitle(), left, 35, MUTED, false);

        int statusX = width - 235;
        g.fill(statusX, 14, width - 18, 45, PANEL_2);
        outline(g, statusX, 14, width - 18, 45, BORDER);
        g.text(font, "26.2", statusX + 15, 23, 0xFF8EA5C5, false);
        g.text(font, "•", statusX + 55, 23, skysoftAvailable ? GREEN : RED, true);
        g.text(font, skysoftAvailable ? "SkySoft Connected" : "SkySoft Not Detected", statusX + 68, 23,
            skysoftAvailable ? GREEN : RED, true);
    }

    private void drawPage(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        switch (page) {
            case MAIN -> renderMain(g, mouseX, mouseY);
            case FARMING -> renderFarming(g, mouseX, mouseY);
            case SESSION -> renderSession(g, mouseX, mouseY);
            case PB -> renderPersonalBest(g, mouseX, mouseY);
            case STREAK -> renderStreak(g, mouseX, mouseY);
            case ACHIEVEMENTS -> renderAchievements(g, mouseX, mouseY);
            case DISCORD -> renderDiscord(g, mouseX, mouseY);
            case SETTINGS -> renderSettings(g, mouseX, mouseY);
            default -> renderFarming(g, mouseX, mouseY);
        }
    }

    private void renderMain(GuiGraphicsExtractor g, int mx, int my) {
        card(g, 225, 65, width - 425, 245, "Farming Overview", CYAN);
        drawStat(g, 250, 110, "Current Crop", currentCrop(), YELLOW);
        drawStat(g, 250, 155, "Session Profit", coins(snapshot.profit()), GREEN);
        drawStat(g, 250, 200, "Active Time", duration(snapshot.activeMillis()), CYAN);
        drawStat(g, 480, 110, "Actions", number(snapshot.actions()), PURPLE);
        drawStat(g, 480, 155, "Tracked Items", number(snapshot.valuedItems()), CYAN);
        drawStat(g, 480, 200, "Pests", number(sum(snapshot.pests())), YELLOW);

        card(g, 225, 330, width - 425, 225, "SkySoft Session Tracker", YELLOW);
        g.text(font, "Status", 250, 375, MUTED, false);
        g.text(font, skysoftAvailable ? "● Connected" : "● Waiting for SkySoft", 315, 375, skysoftAvailable ? GREEN : RED, true);
        g.text(font, "Reader", 250, 405, MUTED, false);
        g.text(font, "ProfitTracker → ProfitTrackerStatistics.sessionStats", 315, 405, 0xFFC7D2E7, false);
        g.text(font, skysoftAvailable ? "Live FARMING session data is being read." :
            "Install/enable SkySoft and activate its Farming Profit Tracker.", 250, 438, TEXT, false);
        drawProgress(g, 250, 475, width - 475, 10, Math.min(1f, snapshot.activeMillis() / 3600000f), PURPLE);
        g.text(font, "1 hour reference", width - 410, 470, MUTED, false);

        card(g, width - 385, 65, width - 18, 490, "Quick Stats", PURPLE);
        drawStat(g, width - 360, 115, "1h Personal Best", coins(history.data().bestOneHourProfit), YELLOW);
        drawStat(g, width - 360, 165, "Best Streak", duration(history.data().bestStreakMs), PURPLE);
        drawStat(g, width - 360, 215, "Sessions", number(history.data().sessions.size()), CYAN);
        drawStat(g, width - 360, 265, "Total Profit", coinsDouble(history.data().totalProfit), GREEN);
        g.text(font, "Use the navigation on the left to", width - 360, 330, MUTED, false);
        g.text(font, "view sessions, PBs, streaks and", width - 360, 346, MUTED, false);
        g.text(font, "achievements.", width - 360, 362, MUTED, false);
    }

    private void renderFarming(GuiGraphicsExtractor g, int mx, int my) {
        int right = width - 405;
        card(g, 225, 65, right, 245, "Farming Settings", CYAN);
        toggleRow(g, 250, 105, "Enable Farming Features", config.enabled, "Main TastyFish features");
        toggleRow(g, 250, 145, "Farming RNG Overlay", config.farmingRngEnabled, "Show rare farming drops");
        toggleRow(g, 250, 185, "Background", config.farmingRngBackground, "Dark background behind RNG overlay");
        toggleRow(g, 250, 225, "Farming Analytics", config.farmingAnalyticsEnabled, "Sessions, PBs, streaks and achievements");

        card(g, 225, 330, right, 610, "Tracking Options", PURPLE);
        toggleRow(g, 250, 370, "Session Recorder", config.farmingSessionRecorderEnabled, "Save completed farming sessions locally");
        toggleRow(g, 250, 410, "Personal Bests", config.farmingPersonalBestEnabled, "Track rolling one-hour profit PB");
        toggleRow(g, 250, 450, "Farming Streak", config.farmingStreakEnabled, "Track continuous farming time");
        toggleRow(g, 250, 490, "Achievements", config.farmingAchievementsEnabled, "Unlock farming milestones");
        g.text(font, "SkySoft connection", 250, 545, MUTED, false);
        g.text(font, skysoftAvailable ? "Connected to live FARMING tracker" : "Waiting for SkySoft FARMING tracker", 250, 567,
            skysoftAvailable ? GREEN : RED, true);
        g.text(font, "SkySoft's current session is read directly from its internal", 250, 605, MUTED, false);
        g.text(font, "ProfitTrackerStatistics session map; no API key is required.", 250, 621, MUTED, false);

        card(g, right + 18, 65, width - 18, 365, "Farming HUD Preview", CYAN);
        drawHudPreview(g, right + 40, 110, width - 40, 300);
        card(g, right + 18, 450, width - 18, 490, "About TastyFish Farming", YELLOW);
        g.text(font, "Track your farming progress in real time.", right + 40, 500, TEXT, false);
        g.text(font, "Sessions are stored locally in", right + 40, 528, MUTED, false);
        g.text(font, "config/tastyfish-farming.json", right + 40, 548, CYAN, false);
        g.text(font, "and can optionally be reported to Discord.", right + 40, 575, MUTED, false);
    }

    private void renderSession(GuiGraphicsExtractor g, int mx, int my) {
        card(g, 225, 65, width - 18, 220, "Current Farming Session", CYAN);
        drawStat(g, 250, 110, "Crop", currentCrop(), YELLOW);
        drawStat(g, 250, 155, "Profit", coins(snapshot.profit()), GREEN);
        drawStat(g, 250, 200, "Active", duration(snapshot.activeMillis()), CYAN);
        drawStat(g, 510, 110, "Actions", number(snapshot.actions()), PURPLE);
        drawStat(g, 510, 155, "Items", number(snapshot.valuedItems()), CYAN);
        drawStat(g, 510, 200, "Pests", number(sum(snapshot.pests())), YELLOW);

        card(g, 225, 305, width - 18, 635, "Recent Sessions", PURPLE);
        List<FarmingHistory.Session> sessions = history.data().sessions;
        if (sessions.isEmpty()) {
            g.text(font, "No completed sessions yet.", 250, 355, MUTED, false);
            g.text(font, "Leave/reconnect from a farming session to create the first record.", 250, 380, MUTED, false);
            return;
        }
        int y = 350;
        int start = Math.max(0, sessions.size() - 9);
        for (int i = sessions.size() - 1; i >= start; i--) {
            FarmingHistory.Session s = sessions.get(i);
            g.fill(245, y - 7, width - 45, y + 35, PANEL_2);
            g.text(font, s.crop(), 260, y, YELLOW, true);
            g.text(font, duration(s.activeMillis()), 430, y, TEXT, false);
            g.text(font, coinsDouble(s.profit()), 545, y, GREEN, false);
            g.text(font, s.reason(), width - 210, y, MUTED, false);
            g.text(font, s.startedAt().replace('T', ' ').replace('Z', ' '), 260, y + 18, 0xFF69788F, false);
            y += 55;
        }
    }

    private void renderPersonalBest(GuiGraphicsExtractor g, int mx, int my) {
        card(g, 225, 65, width - 18, 275, "1-Hour Personal Best", YELLOW);
        long pb = history.data().bestOneHourProfit;
        g.text(font, coins(pb), 260, 125, YELLOW, true);
        g.text(font, "coins in a rolling one-hour farming window", 260, 155, MUTED, false);
        g.text(font, "Crop at PB", 260, 195, MUTED, false);
        g.text(font, history.data().bestOneHourCrop, 360, 195, TEXT, true);
        g.text(font, "Recorded", 260, 225, MUTED, false);
        g.text(font, history.data().bestOneHourAt == 0 ? "Not yet" : formatEpoch(history.data().bestOneHourAt), 360, 225, TEXT, false);
        drawProgress(g, 260, 255, width - 520, 12, Math.min(1f, pb / 10_000_000f), YELLOW);

        card(g, 225, 365, width - 18, 575, "How the PB is calculated", PURPLE);
        g.text(font, "TastyFish samples SkySoft's cumulative FARMING session profit.", 255, 415, TEXT, false);
        g.text(font, "Once a full hour of active SkySoft time is available, the oldest", 255, 445, MUTED, false);
        g.text(font, "sample inside the rolling window is subtracted from the current profit.", 255, 465, MUTED, false);
        g.text(font, "The highest one-hour value is saved locally and survives restarts.", 255, 495, MUTED, false);
    }

    private void renderStreak(GuiGraphicsExtractor g, int mx, int my) {
        card(g, 225, 65, width - 18, 280, "Farming Streak", PURPLE);
        g.text(font, duration(history.data().bestStreakMs), 260, 130, PURPLE, true);
        g.text(font, "Best continuous farming streak", 260, 160, MUTED, false);
        g.text(font, "A gap of more than five minutes without farming progress ends a streak.", 260, 205, TEXT, false);
        drawProgress(g, 260, 245, width - 520, 12, Math.min(1f, history.data().bestStreakMs / (5f * 3600000f)), PURPLE);

        card(g, 225, 375, width - 18, 565, "Milestones", CYAN);
        milestone(g, 260, 425, "30 minutes", history.data().bestStreakMs >= 30 * 60000L);
        milestone(g, 260, 470, "1 hour", history.data().bestStreakMs >= 60 * 60000L);
        milestone(g, 260, 515, "2 hours", history.data().bestStreakMs >= 2 * 60 * 60000L);
        milestone(g, 260, 560, "5 hours", history.data().bestStreakMs >= 5 * 60 * 60000L);
        milestone(g, 260, 605, "10 hours", history.data().bestStreakMs >= 10 * 60 * 60000L);
    }

    private void renderAchievements(GuiGraphicsExtractor g, int mx, int my) {
        card(g, 225, 65, width - 18, 875, "Farming Achievements", YELLOW);
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
        int y = 115;
        for (String[] e : entries) {
            boolean unlocked = history.data().unlockedAchievements.contains(e[0]);
            g.fill(250, y - 8, width - 45, y + 48, unlocked ? 0xFF1B2C2A : PANEL_2);
            g.text(font, unlocked ? "★" : "☆", 270, y + 8, unlocked ? YELLOW : MUTED, true);
            g.text(font, e[1], 305, y, unlocked ? YELLOW : TEXT, true);
            g.text(font, e[2], 305, y + 20, MUTED, false);
            g.text(font, unlocked ? "UNLOCKED" : "LOCKED", width - 170, y + 8, unlocked ? GREEN : MUTED, true);
            y += 70;
        }
    }

    private void renderDiscord(GuiGraphicsExtractor g, int mx, int my) {
        card(g, 225, 65, width - 18, 360, "Discord Forum Integration", PURPLE);
        toggleRow(g, 250, 110, "Enable Discord Reports", config.discordForumEnabled, "Post reports using a Discord forum webhook");
        g.text(font, "Forum Webhook URL", 250, 185, MUTED, false);
        g.text(font, "The field on the right is editable.", 250, 210, MUTED, false);
        g.text(font, "Keep the webhook private. It is saved locally only.", 250, 240, 0xFFFFB85A, false);
        g.text(font, "Report Types", 250, 285, MUTED, false);
        checkbox(g, 250, 310, "Sessions", config.discordSendSessions);
        checkbox(g, 350, 310, "Personal Bests", config.discordSendPersonalBests);
        checkbox(g, 490, 310, "Streaks", config.discordSendStreaks);
        checkbox(g, 585, 310, "Achievements", config.discordSendAchievements);

        card(g, 225, 450, width - 18, 490, "Discord Reports", CYAN);
        g.text(font, "Each completed session can create a new forum post.", 250, 500, TEXT, false);
        g.text(font, "PBs, streak milestones and achievements can also be sent.", 250, 528, MUTED, false);
        g.text(font, "Webhook format", 250, 575, MUTED, false);
        g.text(font, "Discord forum/media channel webhook with thread_name", 250, 598, CYAN, false);
        g.text(font, "Use /tf stats in game to check local analytics at any time.", 250, 650, MUTED, false);
        if (webhookBox != null) {
            g.text(font, "Save by leaving the screen or switching pages.", 680, 490, MUTED, false);
        }
    }

    private void renderSettings(GuiGraphicsExtractor g, int mx, int my) {
        card(g, 225, 65, width - 18, 390, "TastyFish Settings", CYAN);
        g.text(font, "SkySoft endpoint", 250, 110, MUTED, false);
        g.text(font, config.endpoint, 250, 132, TEXT, false);
        g.text(font, "Upload interval", 250, 175, MUTED, false);
        g.text(font, config.uploadIntervalSeconds + " seconds", 250, 197, TEXT, false);
        g.text(font, "Local analytics file", 250, 240, MUTED, false);
        g.text(font, "config/tastyfish-farming.json", 250, 262, CYAN, false);
        g.text(font, "Config file", 250, 305, MUTED, false);
        g.text(font, "config/tastyfish-mod.json", 250, 327, CYAN, false);
        g.text(font, "Commands", 250, 370, MUTED, false);
        g.text(font, "/tf  •  /tf stats  •  /tf discord", 250, 392, TEXT, false);
    }

    private void drawHudPreview(GuiGraphicsExtractor g, int left, int top, int right, int bottom) {
        g.fill(left, top, right, bottom, 0xE6090D14);
        outline(g, left, top, right, bottom, 0xFF2C4260);
        g.text(font, "❖ FARMING", left + 18, top + 18, GREEN, true);
        g.text(font, "Crop: " + currentCrop(), left + 18, top + 48, TEXT, false);
        g.text(font, "Coins/h: " + coins(snapshot.profit()), left + 18, top + 70, GREEN, false);
        g.text(font, "Session: " + duration(snapshot.activeMillis()), left + 18, top + 92, CYAN, false);
        g.text(font, "Items: " + number(snapshot.valuedItems()), left + 18, top + 114, PURPLE, false);
        g.fill(left + 18, top + 142, right - 18, top + 153, 0xFF1F2938);
        g.fill(left + 18, top + 142, left + 18 + Math.max(2, (right - left - 36) / 2), top + 153, PURPLE);
        g.text(font, "TastyFish Farming HUD", left + 18, top + 178, MUTED, false);
    }

    private void card(GuiGraphicsExtractor g, int left, int top, int right, int bottom, String title, int accent) {
        g.fill(left, top, right, bottom, PANEL);
        outline(g, left, top, right, bottom, BORDER);
        g.fill(left, top, right, top + 2, accent);
        g.text(font, title, left + 20, top + 20, TEXT, true);
    }

    private void toggleRow(GuiGraphicsExtractor g, int x, int y, String title, boolean enabled, String subtitle) {
        g.text(font, title, x, y, TEXT, false);
        g.text(font, subtitle, x, y + 16, MUTED, false);
        toggle(g, width - 455, y - 4, enabled);
    }

    private void toggle(GuiGraphicsExtractor g, int x, int y, boolean enabled) {
        g.fill(x, y, x + 54, y + 24, enabled ? 0xFF147A64 : 0xFF252B38);
        outline(g, x, y, x + 54, y + 24, enabled ? GREEN : 0xFF4C5669);
        g.fill(enabled ? x + 31 : x + 4, y + 4, enabled ? x + 50 : x + 23, y + 20, enabled ? GREEN : 0xFF8D96A7);
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

        if (x >= 12 && x <= 190) {
            int navY = 78;
            int[] ids = {MAIN, FARMING, SESSION, PB, STREAK, ACHIEVEMENTS, DISCORD, SETTINGS};
            for (int id : ids) {
                if (y >= navY && y <= navY + 32) {
                    if (webhookBox != null) config.discordForumWebhook = webhookBox.getValue().trim();
                    page = id;
                    save();
                    rebuild();
                    return true;
                }
                navY += 38;
            }
        }

        if (page == FARMING && x >= width - 510 && x <= width - 395) {
            int row = rowAt(y, 101, 40, 4);
            if (row >= 0) {
                switch (row) {
                    case 0 -> config.enabled = !config.enabled;
                    case 1 -> config.farmingRngEnabled = !config.farmingRngEnabled;
                    case 2 -> config.farmingRngBackground = !config.farmingRngBackground;
                    case 3 -> config.farmingAnalyticsEnabled = !config.farmingAnalyticsEnabled;
                }
                save();
                return true;
            }
            row = rowAt(y, 366, 40, 4);
            if (row >= 0) {
                switch (row) {
                    case 0 -> config.farmingSessionRecorderEnabled = !config.farmingSessionRecorderEnabled;
                    case 1 -> config.farmingPersonalBestEnabled = !config.farmingPersonalBestEnabled;
                    case 2 -> config.farmingStreakEnabled = !config.farmingStreakEnabled;
                    case 3 -> config.farmingAchievementsEnabled = !config.farmingAchievementsEnabled;
                }
                save();
                return true;
            }
        }

        if (page == DISCORD) {
            if (y >= 104 && y <= 145) {
                config.discordForumEnabled = !config.discordForumEnabled;
                save();
                return true;
            }
            if (y >= 300 && y <= 335) {
                if (x >= 245 && x < 345) config.discordSendSessions = !config.discordSendSessions;
                else if (x >= 345 && x < 480) config.discordSendPersonalBests = !config.discordSendPersonalBests;
                else if (x >= 480 && x < 575) config.discordSendStreaks = !config.discordSendStreaks;
                else if (x >= 575 && x < 690) config.discordSendAchievements = !config.discordSendAchievements;
                save();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private int rowAt(int y, int start, int spacing, int count) {
        for (int i = 0; i < count; i++) if (y >= start + i * spacing - 10 && y <= start + i * spacing + 25) return i;
        return -1;
    }

    @Override
    public void onClose() {
        if (webhookBox != null) config.discordForumWebhook = webhookBox.getValue().trim();
        save();
        super.onClose();
    }

    private void save() {
        Minecraft mc = Minecraft.getInstance();
        config.save(mc.gameDirectory.toPath().resolve("config").resolve("tastyfish-mod.json"));
    }

    private String pageTitle() {
        return switch (page) {
            case MAIN -> "TastyFish Dashboard";
            case FARMING -> "Farming Settings";
            case SESSION -> "Farming Sessions";
            case PB -> "Personal Bests";
            case STREAK -> "Farming Streaks";
            case ACHIEVEMENTS -> "Achievements";
            case DISCORD -> "Discord Integration";
            case SETTINGS -> "Settings";
            default -> "TastyFish";
        };
    }

    private String pageSubtitle() {
        return switch (page) {
            case MAIN -> "Live SkySoft session data and TastyFish analytics";
            case FARMING -> "Configure your farming experience and tracking";
            case SESSION -> "Browse your locally recorded farming sessions";
            case PB -> "Rolling one-hour farming records";
            case STREAK -> "Continuous farming milestones";
            case ACHIEVEMENTS -> "Milestones unlocked from your farming history";
            case DISCORD -> "Send farming reports to a Discord Forum channel";
            case SETTINGS -> "Connection, storage and command information";
            default -> "";
        };
    }

    private String currentCrop() {
        if (!snapshot.items().isEmpty()) {
            String best = "Unknown";
            long count = 0L;
            for (var entry : snapshot.items().entrySet()) {
                if (entry.getValue() != null && entry.getValue() > count) { count = entry.getValue(); best = prettyId(entry.getKey()); }
            }
            return best;
        }
        return "Waiting";
    }

    private static long sum(java.util.Map<String, Long> map) {
        long total = 0L;
        for (Long value : map.values()) if (value != null) total += value;
        return total;
    }

    private static String prettyId(String id) {
        if (id == null || id.isBlank()) return "Unknown";
        return id.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String number(long value) { return String.format(Locale.ROOT, "%,d", value); }
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
}

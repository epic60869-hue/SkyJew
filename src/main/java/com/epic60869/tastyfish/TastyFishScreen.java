package com.epic60869.tastyfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Main TastyFish control centre.
 *
 * This screen is intentionally separate from the HUD systems: it is a
 * configuration/control menu only.
 */
public final class TastyFishScreen extends Screen {
    private static final int BG = 0xFF070A10;
    private static final int SIDEBAR = 0xFF0B1019;
    private static final int PANEL = 0xFF101722;
    private static final int PANEL_2 = 0xFF0C131D;
    private static final int BORDER = 0xFF263448;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF8794A8;
    private static final int CYAN = 0xFF58D8FF;
    private static final int PURPLE = 0xFF9A6CFF;
    private static final int YELLOW = 0xFFFFD34D;
    private static final int GREEN = 0xFF35E39B;
    private static final int RED = 0xFFFF657A;

    private static final int HOME = 0;
    private static final int FARMING = 1;
    private static final int RNG = 2;
    private static final int SESSIONS = 3;
    private static final int NOTES = 4;
    private static final int SETTINGS = 5;

    private final TastyFishConfig config;
    private final FarmingHistory history;
    private int page = HOME;
    private long nextSnapshotRead;
    private SkysoftSessionReader.Snapshot snapshot = SkysoftSessionReader.Snapshot.empty();
    private boolean skysoftAvailable;

    public TastyFishScreen(TastyFishConfig config) {
        super(Component.literal("TastyFish"));
        this.config = config;
        this.history = new FarmingHistory(
            Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("tastyfish-farming.json")
        );
    }

    @Override
    protected void init() {
        clearWidgets();
        refreshSkysoft(true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        refreshSkysoft(false);
        drawBackground(g);
        drawSidebar(g, mouseX, mouseY);
        drawTopBar(g);
        switch (page) {
            case HOME -> drawHome(g);
            case FARMING -> drawFarming(g);
            case RNG -> drawRng(g);
            case SESSIONS -> drawSessions(g);
            case NOTES -> drawNotes(g);
            case SETTINGS -> drawSettings(g);
            default -> drawHome(g);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void refreshSkysoft(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now < nextSnapshotRead) return;
        nextSnapshotRead = now + 1000L;
        snapshot = SkysoftSessionReader.read();
        skysoftAvailable = snapshot.valid();
    }

    private void drawBackground(GuiGraphicsExtractor g) {
        g.fill(0, 0, width, height, BG);
        g.fill(0, 0, width, 3, PURPLE);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mx, int my) {
        int sw = 190;
        g.fill(0, 0, sw, height, SIDEBAR);
        g.fill(sw - 1, 0, sw, height, BORDER);

        g.text(font, "✦", 20, 19, CYAN, true);
        g.text(font, "TastyFish", 43, 16, YELLOW, true);
        g.text(font, "SkyBlock Companion", 43, 31, MUTED, false);

        int y = 63;
        y = nav(g, "⌂", "Overview", HOME, y, mx, my);
        y = nav(g, "❖", "Farming", FARMING, y, mx, my);
        y = nav(g, "✧", "RNG Tracker", RNG, y, mx, my);
        y = nav(g, "◷", "Sessions", SESSIONS, y, mx, my);
        y = nav(g, "✎", "Notes", NOTES, y, mx, my);
        y = nav(g, "⚙", "Settings", SETTINGS, y, mx, my);

        int bottom = height - 70;
        g.fill(16, bottom, sw - 16, bottom + 1, BORDER);
        g.text(font, "TastyFish Mod", 20, bottom + 14, TEXT, true);
        g.text(font, "26.2", 20, bottom + 31, MUTED, false);
        g.text(font, skysoftAvailable ? "● SkySoft connected" : "● SkySoft waiting",
            68, bottom + 31, skysoftAvailable ? GREEN : RED, false);
    }

    private int nav(GuiGraphicsExtractor g, String icon, String label, int id, int y, int mx, int my) {
        boolean selected = page == id;
        boolean hover = mx >= 10 && mx <= 178 && my >= y && my <= y + 32;

        if (selected) {
            g.fill(10, y, 178, y + 32, 0xFF5136A8);
            g.fill(10, y, 13, y + 32, CYAN);
        } else if (hover) {
            g.fill(10, y, 178, y + 32, 0xFF172333);
        }

        g.text(font, icon, 22, y + 9, selected ? TEXT : CYAN, true);
        g.text(font, label, 48, y + 9, selected ? TEXT : 0xFFD7DEEA, false);
        return y + 38;
    }

    private void drawTopBar(GuiGraphicsExtractor g) {
        int left = 210;
        g.text(font, pageTitle(), left, 15, TEXT, true);
        g.text(font, pageSubtitle(), left, 31, MUTED, false);

        int x = width - 210;
        g.fill(x, 10, width - 16, 43, PANEL_2);
        outline(g, x, 10, width - 16, 43, BORDER);
        g.text(font, "26.2", x + 13, 19, MUTED, false);
        g.text(font, "●", x + 56, 19, skysoftAvailable ? GREEN : RED, true);
        g.text(font, skysoftAvailable ? "Connected" : "Waiting", x + 72, 19,
            skysoftAvailable ? GREEN : RED, true);
    }

    private void drawHome(GuiGraphicsExtractor g) {
        int l = 210;
        int r = width - 16;
        int gap = 12;
        int w = (r - l - gap * 2) / 3;

        card(g, l, 60, w, "SESSION PROFIT", coins(snapshot.profit()), GREEN);
        card(g, l + w + gap, 60, w, "ACTIVE TIME", duration(snapshot.activeMillis()), CYAN);
        card(g, l + (w + gap) * 2, 60, w, "ACTIONS", number(snapshot.actions()), PURPLE);

        panel(g, l, 145, l + (r - l) * 2 / 3, height - 18, "Farming overview", CYAN);
        int x = l + 24;
        stat(g, x, 195, "Current crop", currentCrop(), YELLOW);
        stat(g, x, 245, "1-hour personal best", coins(history.data().bestOneHourProfit), YELLOW);
        stat(g, x, 295, "Best streak", duration(history.data().bestStreakMs), PURPLE);
        stat(g, x, 345, "Completed sessions", number(history.data().sessions.size()), CYAN);
        stat(g, x, 395, "Tracked items", number(snapshot.valuedItems()), GREEN);

        g.text(font, skysoftAvailable
            ? "SkySoft FARMING data is being detected."
            : "SkySoft FARMING data is not currently available.",
            x, 455, skysoftAvailable ? GREEN : RED, true);
        g.text(font, "Use the sidebar to configure farming, view RNG drops,",
            x, 480, MUTED, false);
        g.text(font, "review sessions, or open your local notes.", x, 498, MUTED, false);

        int rx = l + (r - l) * 2 / 3 + 12;
        panel(g, rx, 145, r, height - 18, "Quick actions", PURPLE);
        action(g, rx + 20, 195, r - 20, "Farming settings", "Configure analytics and overlays", FARMING);
        action(g, rx + 20, 255, r - 20, "RNG tracker", "View farming rare-drop tracking", RNG);
        action(g, rx + 20, 315, r - 20, "Notes", "Open your persistent notepad", NOTES);
        action(g, rx + 20, 375, r - 20, "Sessions", "Review locally saved sessions", SESSIONS);
    }

    private void drawFarming(GuiGraphicsExtractor g) {
        int l = 210;
        int r = width - 16;
        int split = l + (r - l) * 3 / 5;

        panel(g, l, 60, split, height - 18, "Farming features", CYAN);
        toggleRow(g, l + 24, 108, "TastyFish features", config.enabled, "Master switch", 0);
        toggleRow(g, l + 24, 166, "Farming RNG overlay", config.farmingRngEnabled, "Rare farming drops", 1);
        toggleRow(g, l + 24, 224, "RNG background", config.farmingRngBackground, "Background behind RNG notifications", 2);
        toggleRow(g, l + 24, 282, "Farming analytics", config.farmingAnalyticsEnabled, "Profit, PBs, streaks and achievements", 3);
        toggleRow(g, l + 24, 340, "Session recorder", config.farmingSessionRecorderEnabled, "Save completed sessions locally", 4);
        toggleRow(g, l + 24, 398, "Personal bests", config.farmingPersonalBestEnabled, "Rolling one-hour PB tracking", 5);
        toggleRow(g, l + 24, 456, "Farming streak", config.farmingStreakEnabled, "Continuous farming time", 6);

        panel(g, split + 12, 60, r, height - 18, "Live preview", PURPLE);
        int px = split + 32;
        g.text(font, currentCrop(), px, 112, YELLOW, true);
        g.text(font, coins(snapshot.profit()), px, 145, GREEN, true);
        g.text(font, duration(snapshot.activeMillis()), px, 178, CYAN, true);
        g.text(font, number(snapshot.actions()) + " actions", px, 211, MUTED, false);
        g.text(font, "Achievements", px, 260, MUTED, false);
        toggle(g, px, 278, config.farmingAchievementsEnabled);
        g.text(font, "Upload interval", px, 330, MUTED, false);
        g.text(font, config.uploadIntervalSeconds + " seconds", px, 350, TEXT, false);
        g.text(font, "Data stays locally recorded and farming",
            px, 405, MUTED, false);
        g.text(font, "uploads use the TastyFish farming server.", px, 423, MUTED, false);
    }

    private void drawRng(GuiGraphicsExtractor g) {
        int l = 210;
        int r = width - 16;
        panel(g, l, 60, r, height - 18, "Farming RNG tracker", YELLOW);

        g.text(font, "Overlay", l + 24, 108, MUTED, false);
        toggle(g, l + 24, 126, config.farmingRngEnabled);
        g.text(font, "Background", l + 150, 108, MUTED, false);
        toggle(g, l + 150, 126, config.farmingRngBackground);

        g.text(font, "Position", l + 24, 195, MUTED, false);
        g.text(font, config.farmingRngX + ", " + config.farmingRngY, l + 24, 216, TEXT, false);
        g.text(font, "Scale", l + 24, 260, MUTED, false);
        g.text(font, String.format(Locale.ROOT, "%.1fx", config.farmingRngScale), l + 24, 281, TEXT, false);

        g.text(font, "Tracked rare drops", l + 260, 195, MUTED, false);
        g.text(font, "FarmingRngTracker is active", l + 260, 216, GREEN, true);
        g.text(font, "Drops are detected from farming chat/events.", l + 260, 245, MUTED, false);
        g.text(font, "Use the overlay in-game to see the latest drops.", l + 260, 265, MUTED, false);

        g.fill(l + 24, 330, r - 24, 390, PANEL_2);
        outline(g, l + 24, 330, r - 24, 390, BORDER);
        g.text(font, "Tip", l + 40, 350, YELLOW, true);
        g.text(font, "The RNG tracker is independent from the guild leaderboard HUD.",
            l + 40, 371, MUTED, false);
    }

    private void drawSessions(GuiGraphicsExtractor g) {
        int l = 210;
        int r = width - 16;
        panel(g, l, 60, r, height - 18, "Recent sessions", CYAN);

        List<FarmingHistory.Session> sessions = history.data().sessions;
        if (sessions.isEmpty()) {
            g.text(font, "No completed farming sessions yet.", l + 26, 112, MUTED, false);
            return;
        }

        int y = 104;
        int start = Math.max(0, sessions.size() - 9);
        for (int i = sessions.size() - 1; i >= start; i--) {
            FarmingHistory.Session s = sessions.get(i);
            g.fill(l + 18, y - 8, r - 18, y + 39, PANEL_2);
            g.text(font, s.crop(), l + 30, y, YELLOW, true);
            g.text(font, duration(s.activeMillis()), l + 205, y, CYAN, false);
            g.text(font, coins(s.profit()), l + 320, y, GREEN, false);
            g.text(font, s.reason(), r - 125, y, MUTED, false);
            y += 52;
        }
    }

    private void drawNotes(GuiGraphicsExtractor g) {
        int l = 210;
        int r = width - 16;
        panel(g, l, 60, r, height - 18, "Notes", YELLOW);

        g.text(font, "Persistent local notepad", l + 28, 115, YELLOW, true);
        g.text(font, "Write anything you want and keep it between Minecraft sessions.", l + 28, 145, MUTED, false);
        g.text(font, "Your notes are stored in:", l + 28, 195, MUTED, false);
        g.text(font, "config/tastyfish-notes.txt", l + 28, 218, CYAN, true);
        g.fill(l + 28, 265, l + 210, 302, 0xFF5136A8);
        g.text(font, "OPEN NOTEPAD", l + 63, 278, TEXT, true);

        g.text(font, "Unlimited lines", l + 28, 350, GREEN, true);
        g.text(font, "ENTER creates a new line • mouse wheel scrolls • ESC saves", l + 28, 375, MUTED, false);
    }

    private void drawSettings(GuiGraphicsExtractor g) {
        int l = 210;
        int r = width - 16;
        panel(g, l, 60, r, height - 18, "Settings", CYAN);

        setting(g, l + 26, 110, "Farming server", config.farmingServerEndpoint, config.farmingServerEnabled ? GREEN : RED);
        setting(g, l + 26, 160, "Upload interval", config.uploadIntervalSeconds + " seconds", TEXT);
        setting(g, l + 26, 210, "Local history", "config/tastyfish-farming.json", CYAN);
        setting(g, l + 26, 260, "Notes", "config/tastyfish-notes.txt", YELLOW);
        setting(g, l + 26, 310, "SkySoft", skysoftAvailable ? "Connected" : "Waiting", skysoftAvailable ? GREEN : RED);

        g.fill(l + 26, 370, r - 26, 430, PANEL_2);
        outline(g, l + 26, 370, r - 26, 430, BORDER);
        g.text(font, "TastyFish", l + 42, 388, YELLOW, true);
        g.text(font, "Farming tools, local analytics, RNG tracking and notes.",
            l + 42, 410, MUTED, false);
    }

    private void action(GuiGraphicsExtractor g, int l, int y, int r, String title, String subtitle, int target) {
        boolean hover = false;
        g.fill(l, y, r, y + 45, PANEL_2);
        outline(g, l, y, r, y + 45, BORDER);
        g.text(font, title, l + 14, y + 9, TEXT, true);
        g.text(font, subtitle, l + 14, y + 26, MUTED, false);
        g.text(font, "›", r - 20, y + 13, CYAN, true);
    }

    private void panel(GuiGraphicsExtractor g, int l, int t, int r, int b, String title, int accent) {
        g.fill(l, t, r, b, PANEL);
        outline(g, l, t, r, b, BORDER);
        g.fill(l, t, r, t + 2, accent);
        g.text(font, title, l + 20, t + 19, TEXT, true);
    }

    private void card(GuiGraphicsExtractor g, int x, int y, int w, String label, String value, int accent) {
        g.fill(x, y, x + w, y + 72, PANEL);
        outline(g, x, y, x + w, y + 72, BORDER);
        g.fill(x, y, x + 3, y + 72, accent);
        g.text(font, label, x + 16, y + 14, MUTED, false);
        g.text(font, value, x + 16, y + 38, accent, true);
    }

    private void stat(GuiGraphicsExtractor g, int x, int y, String label, String value, int accent) {
        g.text(font, label, x, y, MUTED, false);
        g.text(font, value, x, y + 20, accent, true);
    }

    private void setting(GuiGraphicsExtractor g, int x, int y, String label, String value, int accent) {
        g.text(font, label, x, y, MUTED, false);
        g.text(font, value, x, y + 19, accent, false);
    }

    private void toggleRow(GuiGraphicsExtractor g, int x, int y, String title, boolean enabled, String subtitle, int id) {
        g.text(font, title, x, y, TEXT, false);
        g.text(font, subtitle, x, y + 18, MUTED, false);
        toggle(g, Math.min(width - 105, x + 430), y - 5, enabled);
    }

    private void toggle(GuiGraphicsExtractor g, int x, int y, boolean enabled) {
        g.fill(x, y, x + 54, y + 24, enabled ? 0xFF147A64 : 0xFF252B38);
        outline(g, x, y, x + 54, y + 24, enabled ? GREEN : 0xFF4C5669);
        g.fill(enabled ? x + 31 : x + 4, y + 4, enabled ? x + 50 : x + 23, y + 20,
            enabled ? GREEN : 0xFF8D96A7);
        g.text(font, enabled ? "ON" : "OFF", x - 36, y + 7, enabled ? GREEN : MUTED, true);
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

        if (x >= 10 && x <= 178) {
            int navY = 63;
            int[] ids = {HOME, FARMING, RNG, SESSIONS, NOTES, SETTINGS};
            for (int id : ids) {
                if (y >= navY && y <= navY + 32) {
                    page = id;
                    init();
                    return true;
                }
                navY += 38;
            }
        }

        if (page == HOME) {
            int l = 210;
            int r = width - 16;
            int rx = l + (r - l) * 2 / 3 + 12;
            if (x >= rx && x <= r && y >= 195 && y <= 240) {
                page = FARMING;
                init();
                return true;
            }
            if (x >= rx && x <= r && y >= 255 && y <= 300) {
                page = RNG;
                init();
                return true;
            }
            if (x >= rx && x <= r && y >= 315 && y <= 360) {
                page = NOTES;
                init();
                return true;
            }
            if (x >= rx && x <= r && y >= 375 && y <= 420) {
                page = SESSIONS;
                init();
                return true;
            }
        }

        if (page == NOTES && x >= 235 && x <= 420 && y >= 265 && y <= 310) {
            Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
            Minecraft.getInstance().setScreen(new TastyFishNotesScreen(configDir));
            return true;
        }

        if (page == FARMING) {
            int tx = Math.min(width - 105, 234 + 430);
            int[] rows = {108, 166, 224, 282, 340, 398, 456};
            if (x >= tx - 10 && x <= tx + 65) {
                for (int i = 0; i < rows.length; i++) {
                    if (y >= rows[i] - 10 && y <= rows[i] + 30) {
                        switch (i) {
                            case 0 -> config.enabled = !config.enabled;
                            case 1 -> config.farmingRngEnabled = !config.farmingRngEnabled;
                            case 2 -> config.farmingRngBackground = !config.farmingRngBackground;
                            case 3 -> config.farmingAnalyticsEnabled = !config.farmingAnalyticsEnabled;
                            case 4 -> config.farmingSessionRecorderEnabled = !config.farmingSessionRecorderEnabled;
                            case 5 -> config.farmingPersonalBestEnabled = !config.farmingPersonalBestEnabled;
                            case 6 -> config.farmingStreakEnabled = !config.farmingStreakEnabled;
                        }
                        save();
                        return true;
                    }
                }
            }
        }

        if (page == RNG) {
            if (x >= 230 && x <= 310 && y >= 120 && y <= 165) {
                config.farmingRngEnabled = !config.farmingRngEnabled;
                save();
                return true;
            }
            if (x >= 355 && x <= 440 && y >= 120 && y <= 165) {
                config.farmingRngBackground = !config.farmingRngBackground;
                save();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        save();
        super.onClose();
    }

    private void save() {
        config.save(Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config")
            .resolve("tastyfish-mod.json"));
    }

    private String pageTitle() {
        return switch (page) {
            case HOME -> "Overview";
            case FARMING -> "Farming";
            case RNG -> "RNG Tracker";
            case SESSIONS -> "Sessions";
            case NOTES -> "Notes";
            case SETTINGS -> "Settings";
            default -> "TastyFish";
        };
    }

    private String pageSubtitle() {
        return switch (page) {
            case HOME -> "Everything important at a glance";
            case FARMING -> "Control your farming features";
            case RNG -> "Rare farming drop tracking";
            case SESSIONS -> "Your locally recorded farming sessions";
            case NOTES -> "Your persistent in-game notepad";
            case SETTINGS -> "Connection and storage information";
            default -> "";
        };
    }

    private String currentCrop() {
        String best = "Waiting";
        long count = 0L;
        for (var entry : snapshot.items().entrySet()) {
            long value = entry.getValue() == null ? 0L : entry.getValue();
            if (value > count) {
                count = value;
                best = prettyId(entry.getKey());
            }
        }
        return best;
    }

    private static String prettyId(String id) {
        if (id == null || id.isBlank()) return "Unknown";
        return id.toLowerCase(Locale.ROOT)
            .replace("minecraft:", "")
            .replace('_', ' ');
    }

    private static long sum(java.util.Map<String, Long> map) {
        long total = 0L;
        if (map == null) return 0L;
        for (Long value : map.values()) if (value != null) total += value;
        return total;
    }

    private static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String coins(double value) {
        return String.format(Locale.ROOT, "%,.0f coins", value);
    }

    private static String duration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainder = seconds % 60L;
        return hours > 0
            ? String.format(Locale.ROOT, "%dh %02dm", hours, minutes)
            : String.format(Locale.ROOT, "%dm %02ds", minutes, remainder);
    }
}

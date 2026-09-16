package com.epic60869.tastyfish;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/** SkyHanni-style compact collection gap display backed by tastyfish.org. */
public final class TastyFishGuildLeaderboardHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("tastyfish-mod", "guild_collection_leaderboard");
    private static final int WIDTH = 340;
    private static final int HEIGHT = 30;
    private static TastyFishConfig config;
    private static final TastyFishWebsiteClient WEBSITE = new TastyFishWebsiteClient();
    private static long nextSnapshotRead;
    private static SkysoftSessionReader.Snapshot snapshot = SkysoftSessionReader.Snapshot.empty();

    private TastyFishGuildLeaderboardHud() {}

    public static void register(TastyFishConfig cfg) {
        config = cfg;
        HudElementRegistry.addLast(ID, TastyFishGuildLeaderboardHud::extract);
    }

    public static void tick() {
        if (config == null || !config.guildLeaderboardHudEnabled || Minecraft.getInstance().player == null) return;
        long now = System.currentTimeMillis();
        if (now < nextSnapshotRead) return;
        nextSnapshotRead = now + 2500L;
        snapshot = SkysoftSessionReader.read();
        WEBSITE.tick(config, Minecraft.getInstance().getUser().getName(), Minecraft.getInstance().getUser().getProfileId(), snapshot);
    }

    private static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        if (config == null || !config.guildLeaderboardHudEnabled || Minecraft.getInstance().player == null) return;
        TastyFishWebsiteClient.Result data = WEBSITE.result();
        if (!data.available() || !data.hasAheadPlayer()) return;
        render(graphics, data, config.guildLeaderboardHudX, config.guildLeaderboardHudY);
    }

    public static ResultView result() {
        TastyFishWebsiteClient.Result data = WEBSITE.result();
        return new ResultView(data.available(), data.boardName(), data.value(), data.position(),
            data.aheadName(), data.behind(), data.aheadPosition());
    }

    public record ResultView(boolean available, String boardName, long value, int position,
                             String aheadName, long behind, int aheadPosition) {
        public boolean hasAhead() { return available && aheadPosition > 0 && !aheadName.isBlank() && behind > 0; }
    }

    public static int width() { return Math.max(1, Math.round(WIDTH * scale())); }
    public static int height() { return Math.max(1, Math.round(HEIGHT * scale())); }
    public static float scale() { return config == null ? 1.0f : config.guildLeaderboardHudScale; }

    public static void setPosition(int x, int y) {
        if (config == null) return;
        config.guildLeaderboardHudX = Math.max(0, x);
        config.guildLeaderboardHudY = Math.max(0, y);
        save();
    }

    public static void setScale(float value) {
        if (config == null) return;
        config.guildLeaderboardHudScale = Math.max(0.5f, Math.min(3.0f,
            Math.round(value * 10.0f) / 10.0f));
        save();
    }

    public static void changeScale(float amount) { setScale(scale() + amount); }
    public static String scaleText() { return String.format(Locale.ROOT, "%.1fx", scale()); }

    public static void renderPreview(GuiGraphicsExtractor graphics, int x, int y) {
        render(graphics, new TastyFishWebsiteClient.Result(
            true, "carrot", "Carrot Collection", 81_837_732L, 21,
            "BigLando", 88_993_093L, 19), x, y);
    }

    private static void render(GuiGraphicsExtractor graphics, TastyFishWebsiteClient.Result data, int x, int y) {
        float scale = scale();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().scale(scale, scale);

        String collection = data.boardName().isBlank() ? "Collection" : data.boardName();
        String current = format(data.value());
        String position = "[#" + data.position() + "]";
        String gap = format(data.behind()) + " behind " + data.aheadName() + " [#" + data.aheadPosition() + "]";

        drawShadowed(graphics, collection + ": " + current + " " + position, 0, 0, 0xFFFFD84D, true);
        drawShadowed(graphics, gap, 0, 15, 0xFFF0F3FF, false);
        graphics.pose().popMatrix();
    }

    private static void drawShadowed(GuiGraphicsExtractor graphics, String text, int x, int y, int color, boolean bold) {
        var font = Minecraft.getInstance().font;
        graphics.text(font, text, x + 1, y + 1, 0xCC000000, false);
        graphics.text(font, text, x, y, color, bold);
    }

    private static String format(long value) { return String.format(Locale.ROOT, "%,d", Math.max(0L, value)); }

    private static void save() {
        Minecraft mc = Minecraft.getInstance();
        config.save(mc.gameDirectory.toPath().resolve("config").resolve("tastyfish-mod.json"));
    }
}

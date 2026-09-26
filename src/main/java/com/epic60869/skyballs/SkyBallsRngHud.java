package com.epic60869.skyballs;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

public final class SkyBallsRngHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("skyballs-mod", "farming_rng");
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 14;
    private static final int BASE_HEIGHT = 18;
    private static SkyBallsConfig config;

    private SkyBallsRngHud() {}

    public static void register(SkyBallsConfig cfg) {
        config = cfg;
        HudElementRegistry.addLast(ID, SkyBallsRngHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        if (config == null || !config.farming.rng.enabled || Minecraft.getInstance().player == null) return;
        List<FarmingRngTracker.Drop> drops = FarmingRngTracker.get().active();
        if (drops.isEmpty()) return;
        render(graphics, drops, com.epic60869.skyballs.features.core.SkyBallsHuds.mapX(positionX(), width()), com.epic60869.skyballs.features.core.SkyBallsHuds.mapY(positionY(), height()));
    }

    private static final List<FarmingRngTracker.Drop> PREVIEW = List.of(
        new FarmingRngTracker.Drop(1, "Crystalized Moonlight", "RARE DROP", 500000, Long.MAX_VALUE),
        new FarmingRngTracker.Drop(2, "Designer Coffee Beans", "RARE DROP", 500000, Long.MAX_VALUE),
        new FarmingRngTracker.Drop(1, "Legendary Slug Pet", "LEGENDARY", 5000000, Long.MAX_VALUE)
    );

    private static List<FarmingRngTracker.Drop> shown() {
        List<FarmingRngTracker.Drop> active = FarmingRngTracker.get().active();
        return active.isEmpty() ? PREVIEW : active;
    }

    /** Scaled on-screen width, matching exactly what is drawn. */
    public static int width() {
        return Math.max(1, Math.round(contentWidth(shown()) * scale()));
    }

    /** Scaled on-screen height, matching exactly what is drawn. */
    public static int height() {
        return Math.max(1, Math.round(contentHeight(shown()) * scale()));
    }

    private static int contentWidth(List<FarmingRngTracker.Drop> drops) {
        var font = Minecraft.getInstance().font;
        int w = 0;
        for (FarmingRngTracker.Drop drop : drops) {
            w = Math.max(w, PADDING + font.width(itemText(drop)) + 8 + font.width(priceText(drop)) + PADDING);
        }
        return Math.max(40, w);
    }

    private static int contentHeight(List<FarmingRngTracker.Drop> drops) {
        return PADDING + drops.size() * LINE_HEIGHT;
    }

    /** Slug pets in their rarity colour (Epic purple, Legendary gold); everything else cyan. */
    private static int rarityColour(FarmingRngTracker.Drop drop) {
        return switch (drop.rarity()) {
            case "LEGENDARY" -> 0xFFFFAA00;
            case "EPIC" -> 0xFFAA00AA;
            default -> 0xFF55FFFF;
        };
    }

    private static String itemText(FarmingRngTracker.Drop drop) {
        return drop.amount() + "x " + drop.name();
    }

    private static String priceText(FarmingRngTracker.Drop drop) {
        return drop.unitPrice() < 0 ? "—" : formatCoins(drop.unitPrice() * drop.amount());
    }

    public static float scale() {
        return config == null ? 1.0f : config.farming.rng.scale;
    }

    public static int x() { return positionX(); }
    public static int y() { return positionY(); }

    private static int positionX() {
        return config == null ? 8 : config.farming.rng.x;
    }

    private static int positionY() {
        return config == null ? 8 : config.farming.rng.y;
    }

    public static void setPosition(int x, int y) {
        if (config == null) return;
        config.farming.rng.x = Math.max(0, x);
        config.farming.rng.y = Math.max(0, y);
        save();
    }

    public static void setScale(float value) {
        if (config == null) return;
        config.farming.rng.scale = Math.max(0.5f, Math.min(3.0f,
            Math.round(value * 10.0f) / 10.0f));
        save();
    }

    public static void changeScale(float amount) {
        setScale(scale() + amount);
    }

    public static String scaleText() {
        return String.format(Locale.ROOT, "%.1fx", scale());
    }

    public static void renderPreview(GuiGraphicsExtractor graphics, int x, int y) {
        render(graphics, shown(), x, y);
    }

    private static void render(GuiGraphicsExtractor graphics,
                               List<FarmingRngTracker.Drop> drops,
                               int x,
                               int y) {
        float s = scale();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().scale(s, s);

        // The background fills exactly the HUD bounds so it can sit flush against a screen edge.
        int w = contentWidth(drops);
        int h = contentHeight(drops);
        if (config != null && config.farming.rng.background) {
            graphics.fill(0, 0, w, h, 0xA8000000);
            graphics.fill(0, 0, w, 1, 0x55FFFFFF);
        }

        int yOffset = PADDING;
        for (FarmingRngTracker.Drop drop : drops) {
            String item = itemText(drop);
            String price = priceText(drop);

            // One complete drop per line: amount, item name, then total value.
            drawShadowed(graphics, item, PADDING, yOffset, rarityColour(drop), true);
            drawShadowed(graphics, price, w - PADDING - Minecraft.getInstance().font.width(price), yOffset, 0xFFB8B8B8, false);
            yOffset += LINE_HEIGHT;
        }

        graphics.pose().popMatrix();
    }

    private static void drawShadowed(GuiGraphicsExtractor graphics,
                                     String text,
                                     int x,
                                     int y,
                                     int color,
                                     boolean bold) {
        var font = Minecraft.getInstance().font;
        graphics.text(font, text, x + 1, y + 1, 0xAA000000, false);
        graphics.text(font, text, x, y, color, bold);
    }

    private static String formatCoins(long value) {
        if (value >= 1_000_000_000L) {
            return compactNumber(value / 1_000_000_000.0, "b");
        }
        if (value >= 1_000_000L) {
            return compactNumber(value / 1_000_000.0, "m");
        }
        if (value >= 1_000L) {
            return compactNumber(value / 1_000.0, "k");
        }
        return Long.toString(value);
    }

    private static String compactNumber(double value, String suffix) {
        String formatted = String.format(Locale.ROOT, "%.2f", value)
            .replaceAll("0+$", "")
            .replaceAll("\\.$", "");
        return formatted + suffix;
    }

    private static void save() {
        Minecraft mc = Minecraft.getInstance();
        config.save(mc.gameDirectory.toPath().resolve("config").resolve("skyballs-mod.json"));
    }
}

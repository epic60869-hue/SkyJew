package com.epic60869.skyjew;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkyJewCommissionHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("skyjew", "commissions");
    private static final Pattern COMM_PATTERN =
        Pattern.compile("(?<name>.+?):\\s*(?<progress>DONE|[0-9]+(?:\\.[0-9]+)?%?)");

    private static SkyJewConfig config;
    private static List<Commission> commissions = List.of();

    private SkyJewCommissionHud() {}

    public static void register(SkyJewConfig cfg) {
        config = cfg;
        HudElementRegistry.addLast(ID, SkyJewCommissionHud::extract);
    }

    public static int x() { return config == null ? 8 : config.mining.commissions.x; }
    public static int y() { return config == null ? 80 : config.mining.commissions.y; }
    public static float scale() { return config == null ? 1.0f : config.mining.commissions.scale; }
    public static int width() { return 250; }
    public static int height() { return Math.max(18, 18 + commissions.size() * 18); }

    public static void setPosition(int x, int y) {
        if (config == null) return;
        config.mining.commissions.x = Math.max(0, x);
        config.mining.commissions.y = Math.max(0, y);
        save();
    }

    public static void renderPreview(GuiGraphicsExtractor g, int x, int y) {
        render(g, List.of(
            new Commission("Titanium Miner", "65%", 65),
            new Commission("Goblin Slayer", "DONE", 100)
        ), x, y);
    }

    private static void extract(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (config == null || !config.mining.commissions.enabled || mc.player == null) return;
        updateFromTab();
        if (!commissions.isEmpty()) render(g, commissions, x(), y());
    }

    private static void updateFromTab() {
        SkyJewTabWidgetManager.Widget widget = SkyJewTabWidgetManager.get("Commissions");
        List<Commission> found = new ArrayList<>();

        for (Component line : widget.lines()) {
            Matcher matcher = COMM_PATTERN.matcher(line.getString().strip());
            if (!matcher.matches()) continue;

            String name = matcher.group("name").strip();
            String progress = matcher.group("progress").strip();
            if (name.isEmpty()) continue;

            if (progress.equalsIgnoreCase("DONE")) {
                found.add(new Commission(name, "DONE", 100f));
                continue;
            }

            String numeric = progress.endsWith("%")
                ? progress.substring(0, progress.length() - 1)
                : progress;
            try {
                float percent = Math.max(0f, Math.min(100f, Float.parseFloat(numeric)));
                found.add(new Commission(name,
                    progress.endsWith("%") ? progress : progress + "%", percent));
            } catch (NumberFormatException ignored) {
            }
        }

        commissions = found;
    }

    private static void render(GuiGraphicsExtractor g, List<Commission> list, int x, int y) {
        float scale = scale();
        g.pose().pushMatrix();
        g.pose().translate((float)x, (float)y);
        g.pose().scale(scale, scale);

        // Deliberately no opaque panel/background. The old overlay looked like
        // a second TAB window over the game; this is a clean floating HUD.
        draw(g, "Commissions", 0, 0, 0xFF55FFFF, true);

        int row = 17;
        for (Commission c : list) {
            draw(g, "◆", 0, row, 0xFFFFAA00, false);
            draw(g, c.name, 12, row, 0xFFFFFFFF, false);

            int pw = Minecraft.getInstance().font.width(c.progress);
            int right = Math.max(115, width());
            draw(g, c.progress, right - pw, row,
                c.percent >= 100f ? 0xFF55FF55 : 0xFFFFD83D, false);

            int barX = 12;
            int barY = row + 10;
            int barWidth = Math.max(100, right - barX);
            g.fill(barX, barY, barX + barWidth, barY + 2, 0x55333333);
            int filled = Math.round(barWidth * c.percent / 100f);
            if (filled > 0) {
                g.fill(barX, barY, barX + filled, barY + 2,
                    c.percent >= 100f ? 0xFF55FF55 : 0xFF55FFFF);
            }
            row += 18;
        }

        g.pose().popMatrix();
    }

    private static void draw(GuiGraphicsExtractor g, String text, int x, int y,
                             int color, boolean bold) {
        var font = Minecraft.getInstance().font;
        g.text(font, text, x + 1, y + 1, 0x66000000, false);
        g.text(font, text, x, y, color, bold);
    }

    private static void save() {
        if (config != null) {
            config.save(Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("skyjew-mod.json"));
        }
    }

    private record Commission(String name, String progress, float percent) {}
}

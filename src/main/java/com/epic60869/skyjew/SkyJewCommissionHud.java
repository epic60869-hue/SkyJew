package com.epic60869.skyjew;

import com.epic60869.skyjew.mixin.SkyJewPlayerTabOverlayAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkyJewCommissionHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("skyjew", "commissions");
    private static final Pattern COMM_PATTERN = Pattern.compile("(?<name>.*): (?<progress>.*)%?");
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
    public static int width() { return 290; }
    public static int height() { return Math.max(24, 18 + commissions.size() * 20); }

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
        updateFromTab(mc);
        if (!commissions.isEmpty()) render(g, commissions, x(), y());
    }

    /** Mirrors Skyblocker's PlayerListManager: vanilla TAB ordering, then the named widget. */
    private static void updateFromTab(Minecraft mc) {
        if (mc.getConnection() == null) return;

        List<PlayerInfo> ordered = new ArrayList<>(mc.getConnection().getOnlinePlayers());
        try {
            ordered.sort(SkyJewPlayerTabOverlayAccessor.getOrdering());
        } catch (Throwable ignored) {}

        boolean inWidget = false;
        List<Commission> found = new ArrayList<>();

        for (PlayerInfo info : ordered) {
            Component component = info.getTabListDisplayName();
            if (component == null) continue;

            String string = component.getString();
            if (string.isBlank()) continue;

            if (!inWidget) {
                if (string.strip().equalsIgnoreCase("Commissions") || string.strip().startsWith("Commissions:")) {
                    inWidget = true;
                }
                continue;
            }

            // PlayerListManager.trim() feeds only indented lines to a widget.
            if (!string.startsWith(" ")) break;

            String line = string.strip();
            Matcher matcher = COMM_PATTERN.matcher(line);
            if (!matcher.matches()) continue;

            String name = matcher.group("name").strip();
            String progress = matcher.group("progress").strip();
            if (name.isEmpty()) continue;

            if ("DONE".equalsIgnoreCase(progress)) {
                found.add(new Commission(name, "DONE", 100f));
                continue;
            }

            String numeric = progress.endsWith("%")
                ? progress.substring(0, progress.length() - 1) : progress;
            try {
                float percent = Math.max(0f, Math.min(100f, Float.parseFloat(numeric)));
                found.add(new Commission(name, progress.endsWith("%") ? progress : progress + "%", percent));
            } catch (NumberFormatException ignored) {}
        }

        commissions = found;
    }

    private static void render(GuiGraphicsExtractor g, List<Commission> list, int x, int y) {
        float scale = scale();
        g.pose().pushMatrix();
        g.pose().translate((float)x, (float)y);
        g.pose().scale(scale, scale);

        int contentHeight = 18 + list.size() * 20;
        if (config != null && config.mining.commissions.background) {
            g.fill(-5, -4, width() + 5, contentHeight + 4, 0xB0000000);
        }

        draw(g, "Commissions", 4, 0, 0xFF00AAAA, true);
        int row = 18;
        for (Commission c : list) {
            draw(g, "▣", 4, row, 0xFF55FFFF, false);
            draw(g, c.name, 17, row, 0xFFFFFFFF, false);
            int pw = Minecraft.getInstance().font.width(c.progress);
            draw(g, c.progress, width() - pw - 4, row,
                c.percent >= 100f ? 0xFF55FF55 : 0xFFFFFFFF, false);

            int barX = 17, barY = row + 11, barWidth = width() - 21;
            g.fill(barX, barY, barX + barWidth, barY + 3, 0x55333333);
            int filled = Math.round(barWidth * c.percent / 100f);
            if (filled > 0) g.fill(barX, barY, barX + filled, barY + 3,
                c.percent >= 100f ? 0xFF55FF55 : 0xFF00AAAA);
            row += 20;
        }
        g.pose().popMatrix();
    }

    private static void draw(GuiGraphicsExtractor g, String text, int x, int y, int color, boolean bold) {
        var font = Minecraft.getInstance().font;
        g.text(font, text, x + 1, y + 1, 0xAA000000, false);
        g.text(font, text, x, y, color, bold);
    }

    private static void save() {
        if (config != null) config.save(Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("skyjew-mod.json"));
    }

    private record Commission(String name, String progress, float percent) {}
}

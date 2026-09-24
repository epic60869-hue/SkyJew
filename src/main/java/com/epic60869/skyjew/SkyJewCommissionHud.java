package com.epic60869.skyjew;

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
    private static final Identifier ID = Identifier.fromNamespaceAndPath("skyjew-mod", "mining_commissions");
    private static final Pattern COMMISSION = Pattern.compile("(?<name>.*): (?<progress>.*)%?");
    private static SkyJewConfig config;
    private static List<Commission> commissions = List.of();

    private SkyJewCommissionHud() {}

    public static void register(SkyJewConfig cfg) {
        config = cfg;
        HudElementRegistry.addLast(ID, SkyJewCommissionHud::extract);
    }

    public static int x() { return config == null ? 8 : config.mining.commissions.x; }
    public static int y() { return config == null ? 80 : config.farming.commissions.y; }
    public static float scale() { return config == null ? 1.0f : config.farming.commissions.scale; }
    public static int width() { return 290; }
    public static int height() { return Math.max(24, 18 + commissions.size() * 18); }

    public static void setPosition(int x, int y) {
        if (config == null) return;
        config.farming.commissions.x = Math.max(0, x);
        config.farming.commissions.y = Math.max(0, y);
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
        if (config == null || !config.farming.commissions.enabled || mc.player == null) return;

        updateFromTab(mc);
        if (commissions.isEmpty()) return;
        render(g, commissions, x(), y());
    }

    private static void updateFromTab(Minecraft mc) {
        if (mc.getConnection() == null) return;

        boolean inSection = false;
        List<Commission> found = new ArrayList<>();

        for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
            Component display = info.getTabListDisplayName();
            if (display == null) continue;

            String line = display.getString();
            if (!inSection) {
                if (line.trim().toLowerCase().startsWith("commissions")) {
                    inSection = true;
                }
                continue;
            }

            String stripped = line.trim();
            if (stripped.isEmpty()) break;
            if (stripped.equalsIgnoreCase("Commissions")) continue;
            Matcher matcher = COMMISSION.matcher(stripped);
            if (!matcher.matches()) continue;

            String name = matcher.group("name").trim();
            String progress = matcher.group("progress").trim();

            if ("DONE".equalsIgnoreCase(progress)) {
                found.add(new Commission(name, "DONE", 100));
            } else {
                String numeric = progress.replace("%", "").trim();
                try {
                    float value = Math.max(0, Math.min(100, Float.parseFloat(numeric)));
                    found.add(new Commission(name, progress, value));
                } catch (NumberFormatException ignored) {}
            }
        }

        commissions = found;
    }

    private static void render(GuiGraphicsExtractor g, List<Commission> list, int x, int y) {
        float scale = scale();
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);

        int contentHeight = 18 + list.size() * 18;
        if (config != null && config.farming.commissions.background) {
            g.fill(-5, -4, width() + 5, contentHeight + 4, 0xB0000000);
        }

        draw(g, "Commissions", 4, 0, 0xFF55FFFF, true);

        int row = 18;
        for (Commission commission : list) {
            draw(g, commission.name, 4, row, 0xFFFFFFFF, false);

            String progress = commission.progress;
            int progressWidth = Minecraft.getInstance().font.width(progress);
            draw(g, progress, width() - progressWidth - 4, row,
                "DONE".equalsIgnoreCase(progress) ? 0xFF55FF55 : 0xFFAAAAAA, false);

            int barX = 4;
            int barY = row + 12;
            int barWidth = width() - 8;
            int filled = Math.round(barWidth * (commission.percent / 100.0f));
            g.fill(barX, barY, barX + barWidth, barY + 2, 0x55333333);
            if (filled > 0) {
                g.fill(barX, barY, barX + filled, barY + 2,
                    "DONE".equalsIgnoreCase(progress) ? 0xFF55FF55 : 0xFF55AAAA);
            }
            row += 18;
        }

        g.pose().popMatrix();
    }

    private static void draw(GuiGraphicsExtractor g, String text, int x, int y, int color, boolean bold) {
        var font = Minecraft.getInstance().font;
        g.text(font, text, x + 1, y + 1, 0xAA000000, false);
        g.text(font, text, x, y, color, bold);
    }

    private static void save() {
        if (config != null) {
            config.save(Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("skyjew-mod.json"));
        }
    }

    private record Commission(String name, String progress, float percent) {}
}

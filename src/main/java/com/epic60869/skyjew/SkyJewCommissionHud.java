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
import com.epic60869.skyjew.mixin.SkyJewPlayerTabOverlayAccessor;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class SkyJewCommissionHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("skyjew", "commissions");
    private static final Pattern COMM_PATTERN =
        Pattern.compile("(?<name>.+?):\\s*(?<progress>DONE|[0-9,.]+\\s*/\\s*[0-9,.]+|[0-9]+(?:\\.[0-9]+)?%?)",
            Pattern.CASE_INSENSITIVE);

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            commissions = List.of();
            return;
        }

        List<PlayerInfo> entries = new ArrayList<>(mc.getConnection().getOnlinePlayers());
        try {
            entries.sort(SkyJewPlayerTabOverlayAccessor.getOrdering());
        } catch (Throwable ignored) {
        }

        List<Commission> found = new ArrayList<>();
        boolean inCommissions = false;
        final int maxCommissions = 4;

        for (PlayerInfo entry : entries) {
            Component display = entry.getTabListDisplayName();
            if (display == null && entry.getProfile() != null) {
                display = Component.literal(entry.getProfile().name());
            }
            if (display == null) continue;

            String raw = display.getString().replaceAll("§.", "");
            String line = raw.strip();
            if (line.isBlank()) continue;

            if (!inCommissions) {
                if (line.equalsIgnoreCase("Commissions") || line.equalsIgnoreCase("Commissions:")) {
                    inCommissions = true;
                    continue;
                }
                // Hypixel sometimes sends the header and its first value in the
                // same TAB component. Do not treat the value (for example
                // "Gemstone") as a commission.
                if (line.regionMatches(true, 0, "Commissions:", 0, "Commissions:".length())) {
                    inCommissions = true;
                    line = line.substring("Commissions:".length()).strip();
                    if (line.isBlank()) continue;
                } else {
                    continue;
                }
            }

            // These are TAB section headers/data that can also match the generic
            // "Name: number" pattern. They are never commission rows.
            String lower = line.toLowerCase(java.util.Locale.ROOT);
            if (lower.equals("bank") || lower.startsWith("bank:")
                || lower.equals("purse") || lower.startsWith("purse:")
                || lower.equals("fairy souls") || lower.startsWith("fairy souls:")
                || lower.equals("skills") || lower.startsWith("skills:")
                || lower.equals("slayer") || lower.startsWith("slayer:")
                || lower.equals("pets") || lower.startsWith("pets:")
                || lower.equals("profile") || lower.startsWith("profile:")) {
                break;
            }

            Matcher matcher = COMM_PATTERN.matcher(line);
            if (!matcher.matches()) continue;

            String lowerName = matcher.group("name").strip().toLowerCase(java.util.Locale.ROOT);
            // Resource/currency headings are not commissions even when Hypixel
            // formats them as "Name: value".
            if (lowerName.equals("gemstone") || lowerName.equals("mithril")
                || lowerName.equals("glacite") || lowerName.equals("powder")
                || lowerName.equals("bank") || lowerName.equals("purse")) {
                continue;
            }

            String name = matcher.group("name").strip();
            String progress = matcher.group("progress").strip();
            if (name.isEmpty() || progress.isEmpty()) continue;

            if (progress.equalsIgnoreCase("DONE")) {
                found.add(new Commission(name, "DONE", 100f));
                if (found.size() >= maxCommissions) break;
                continue;
            }

            try {
                float percent;
                String shown = progress;
                if (progress.contains("/")) {
                    String[] parts = progress.split("/", 2);
                    double current = Double.parseDouble(parts[0].replace(",", "").trim());
                    double total = Double.parseDouble(parts[1].replace(",", "").trim());
                    if (total <= 0) continue;
                    percent = (float) Math.max(0, Math.min(100, current * 100.0 / total));
                } else {
                    String numeric = progress.endsWith("%")
                        ? progress.substring(0, progress.length() - 1)
                        : progress;
                    percent = Math.max(0f, Math.min(100f, Float.parseFloat(numeric)));
                    if (!progress.endsWith("%")) shown = progress + "%";
                }
                found.add(new Commission(name, shown, percent));
                if (found.size() >= maxCommissions) break;
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

        if (config != null && config.mining.commissions.background) {
            int contentHeight = 18 + list.size() * 18;
            g.fill(-5, -4, width() + 5, contentHeight + 4, 0x99000000);
        }
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

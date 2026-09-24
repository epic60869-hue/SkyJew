package com.epic60869.skyjew;

import com.epic60869.skyjew.mixin.SkyJewPlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class SkyJewTabWidgetManager {
    private static final Map<String, Widget> widgets = new HashMap<>();
    private static List<PlayerInfo> players = List.of();
    private static long lastUpdate;

    private SkyJewTabWidgetManager() {}

    public static void tick(Minecraft mc) {
        if (mc.getConnection() == null) {
            widgets.clear();
            players = List.of();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastUpdate < 100L) return;
        lastUpdate = now;
        update(mc);
    }

    public static Widget get(String name) {
        return widgets.getOrDefault(name, Widget.EMPTY);
    }

    public static List<PlayerInfo> players() {
        return players;
    }

    private static void update(Minecraft mc) {
        List<PlayerInfo> ordered = new ArrayList<>(mc.getConnection().getOnlinePlayers());
        try {
            ordered.sort(SkyJewPlayerTabOverlayAccessor.getOrdering());
        } catch (Throwable ignored) {
        }
        players = List.copyOf(ordered);

        captureSection(ordered, "Commissions");
        captureSection(ordered, "Pet");
    }

    /**
     * Hypixel changes the whitespace/colon formatting of TAB sections between
     * islands and occasionally between backend updates. Do not depend on one
     * exact string layout. Instead, find the heading and consume the useful
     * lines until the next recognizable section.
     */
    private static void captureSection(List<PlayerInfo> list, String heading) {
        for (int i = 0; i < list.size(); i++) {
            Component c = list.get(i).getTabListDisplayName();
            if (c == null) continue;

            String text = normalize(c.getString());
            if (!isHeading(text, heading)) continue;

            List<Component> lines = new ArrayList<>();
            List<PlayerInfo> raw = new ArrayList<>();
            raw.add(list.get(i));

            for (int j = i + 1; j < list.size(); j++) {
                Component next = list.get(j).getTabListDisplayName();
                if (next == null) continue;

                String s = next.getString();
                String normalized = normalize(s);

                if (isKnownSectionHeading(normalized)) break;

                if (heading.equalsIgnoreCase("Commissions")) {
                    // Commission rows look like "Mithril Miner: 75%" or
                    // "Goblin Slayer: DONE". Accept them even if Hypixel
                    // removes the leading indentation.
                    if (normalized.matches(".+:\\s*(?:DONE|\\d+(?:\\.\\d+)?%?)")) {
                        lines.add(stripIndent(next));
                        raw.add(list.get(j));
                    }
                } else if (heading.equalsIgnoreCase("Pet")) {
                    if (normalized.matches("\\[Lvl\\s+\\d+].*")
                        || normalized.matches("\\+?[\\d,.]+\\s+XP.*")) {
                        lines.add(stripIndent(next));
                        raw.add(list.get(j));
                    }
                }
            }

            widgets.put(heading, new Widget(Component.empty(), lines, raw));
            return;
        }

        widgets.remove(heading);
    }

    private static boolean isHeading(String text, String heading) {
        return text.equalsIgnoreCase(heading)
            || text.equalsIgnoreCase(heading + ":");
    }

    private static boolean isKnownSectionHeading(String text) {
        if (text.isBlank()) return false;
        return text.equalsIgnoreCase("Commissions")
            || text.equalsIgnoreCase("Commissions:")
            || text.equalsIgnoreCase("Pet")
            || text.equalsIgnoreCase("Pet:")
            || text.equalsIgnoreCase("Profile")
            || text.equalsIgnoreCase("Skills")
            || text.equalsIgnoreCase("Stats")
            || text.equalsIgnoreCase("Dungeons")
            || text.equalsIgnoreCase("Slayers")
            || text.equalsIgnoreCase("Collections");
    }

    private static String normalize(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ");
    }

    private static Component stripIndent(Component text) {
        return Component.literal(normalize(text.getString())).withStyle(text.getStyle());
    }

    public record Widget(Component detail, List<Component> lines, List<PlayerInfo> playerListEntries) {
        public static final Widget EMPTY = new Widget(Component.empty(), List.of(), List.of());

        public Widget {
            detail = detail == null ? Component.empty() : detail.copy();
            lines = lines == null ? List.of() : lines.stream().map(c -> (Component) c.copy()).toList();
            playerListEntries = playerListEntries == null ? List.of() : List.copyOf(playerListEntries);
        }
    }
}
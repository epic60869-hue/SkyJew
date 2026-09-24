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
        if (mc.getConnection() == null) { widgets.clear(); players = List.of(); return; }
        long now = System.currentTimeMillis();
        if (now - lastUpdate < 100L) return;
        lastUpdate = now;
        update(mc);
    }

    public static Widget get(String name) {
        return widgets.getOrDefault(name, Widget.EMPTY);
    }

    public static List<PlayerInfo> players() { return players; }

    private static void update(Minecraft mc) {
        List<PlayerInfo> ordered = new ArrayList<>(mc.getConnection().getOnlinePlayers());
        try { ordered.sort(SkyJewPlayerTabOverlayAccessor.getOrdering()); } catch (Throwable ignored) {}
        players = List.copyOf(ordered);

        Map<String, WidgetBuilder> found = new LinkedHashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            Component display = ordered.get(i).getTabListDisplayName();
            if (display == null) continue;
            String text = display.getString();
            if (text == null || text.isBlank()) continue;

            if (!text.startsWith(" ") && text.contains(":")) {
                String name = text.substring(0, text.indexOf(':')).trim();
                WidgetBuilder b = found.computeIfAbsent(name, k -> new WidgetBuilder());
                String detail = text.substring(text.indexOf(':') + 1).trim();
                if (!detail.isEmpty()) b.detail = Component.literal(detail);
                b.raw.add(ordered.get(i));
            } else if (text.startsWith(" ")) {
                // Attach indented rows to the most recent header.
                String header = mostRecentHeader(ordered, i);
                if (header != null) {
                    WidgetBuilder b = found.computeIfAbsent(header, k -> new WidgetBuilder());
                    b.lines.add(trim(display));
                    b.raw.add(ordered.get(i));
                }
            }
        }

        captureHeading(ordered, found, "Commissions");
        captureHeading(ordered, found, "Pet");
        widgets.clear();
        for (Map.Entry<String, WidgetBuilder> e : found.entrySet()) widgets.put(e.getKey(), e.getValue().build());
    }

    private static String mostRecentHeader(List<PlayerInfo> list, int index) {
        for (int i = index - 1; i >= 0; i--) {
            Component c = list.get(i).getTabListDisplayName();
            if (c == null) continue;
            String s = c.getString();
            if (s.isBlank()) continue;
            if (!s.startsWith(" ") && s.contains(":")) return s.substring(0, s.indexOf(':')).trim();
            if (!s.startsWith(" ")) return null;
        }
        return null;
    }

    private static void captureHeading(List<PlayerInfo> list, Map<String, WidgetBuilder> found, String heading) {
        for (int i = 0; i < list.size(); i++) {
            Component c = list.get(i).getTabListDisplayName();
            if (c == null || !c.getString().strip().equalsIgnoreCase(heading + (heading.equals("Pet") ? ":" : ""))) continue;
            WidgetBuilder b = found.computeIfAbsent(heading, k -> new WidgetBuilder());
            b.lines.clear(); b.raw.clear(); b.raw.add(list.get(i));
            for (int j = i + 1; j < list.size(); j++) {
                Component n = list.get(j).getTabListDisplayName();
                if (n == null) continue;
                String s = n.getString();
                if (!s.startsWith(" ")) break;
                b.lines.add(trim(n)); b.raw.add(list.get(j));
            }
            return;
        }
    }

    private static Component trim(Component text) {
        String s = text.getString().strip();
        return Component.literal(s).withStyle(text.getStyle());
    }

    public record Widget(Component detail, List<Component> lines, List<PlayerInfo> playerListEntries) {
        public static final Widget EMPTY = new Widget(Component.empty(), List.of(), List.of());
        public Widget {
            detail = detail == null ? Component.empty() : detail.copy();
            lines = lines == null ? List.of() : lines.stream().map(c -> (Component) c.copy()).toList();
            playerListEntries = playerListEntries == null ? List.of() : List.copyOf(playerListEntries);
        }
    }
    private static final class WidgetBuilder {
        Component detail = Component.empty();
        final List<Component> lines = new ArrayList<>();
        final List<PlayerInfo> raw = new ArrayList<>();
        Widget build() { return new Widget(detail, lines, raw); }
    }
}

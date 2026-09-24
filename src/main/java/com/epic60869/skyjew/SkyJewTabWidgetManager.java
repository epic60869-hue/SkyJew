package com.epic60869.skyjew;

import com.epic60869.skyjew.mixin.SkyJewPlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SkyJew's TAB parser follows Skyblocker's widget-boundary approach rather than
 * looking only for hard-coded "Pet:"/"Commissions:" blocks. Hypixel lays the
 * TAB columns out as a single ordered PlayerInfo list, so widgets are detected
 * from the same column/colon boundaries that Skyblocker uses.
 */
public final class SkyJewTabWidgetManager {
    private static final Map<String, Widget> widgets = new LinkedHashMap<>();
    private static List<PlayerInfo> players = List.of();
    private static long lastUpdate;

    private static final java.util.regex.Pattern PLAYERS_COLUMN =
        java.util.regex.Pattern.compile("\\s*(Players \\(\\d+\\)|Island|Coop \\(\\d+\\))\\s*");
    private static final java.util.regex.Pattern INFO_COLUMN =
        java.util.regex.Pattern.compile("\\s*Info\\s*");

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

    public static Set<String> names() {
        return Collections.unmodifiableSet(widgets.keySet());
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
        updateWidgetsFrom(ordered);
    }

    private static void updateWidgetsFrom(List<PlayerInfo> lines) {
        PredicateState state = new PredicateState();
        Component sideThing = Component.empty();
        List<Component> contents = new ArrayList<>();
        List<PlayerInfo> raw = new ArrayList<>();

        boolean playersDone = false;
        String currentName = "Players";
        int currentColor = 0xFFFF00;

        widgets.clear();

        for (PlayerInfo entry : lines) {
            Component display = entry.getTabListDisplayName();
            if (display == null) continue;

            String string = display.getString();
            if (string.isBlank()) continue;

            if (!playersDone) {
                if (PLAYERS_COLUMN.matcher(string).matches()) {
                    continue;
                }
                if (INFO_COLUMN.matcher(string).matches()) {
                    playersDone = true;
                    put(currentName, sideThing, contents, raw, currentColor);
                    sideThing = Component.empty();
                    contents.clear();
                    raw.clear();
                    continue;
                }
                continue;
            }

            if (INFO_COLUMN.matcher(string).matches()) continue;

            // Skyblocker treats a non-indented line containing ':' as the
            // beginning of a new Hypixel widget. Keep the same exception for
            // mining-event "Ends in:" text.
            if (!string.startsWith(" ")
                && string.contains(":")
                && !(currentName.toLowerCase(Locale.ROOT).startsWith("mining event")
                    && string.toLowerCase(Locale.ROOT).startsWith("ends in"))) {

                if (!contents.isEmpty() || !sideThing.getString().isEmpty()) {
                    put(currentName, sideThing, contents, raw, currentColor);
                }

                sideThing = Component.empty();
                contents.clear();
                raw.clear();

                NameAndInfo parsed = getNameAndInfo(display);
                currentName = parsed.name();
                currentColor = parsed.color();
                if (!parsed.detail().getString().isBlank()) {
                    sideThing = trim(parsed.detail());
                    raw.add(entry);
                }
                continue;
            }

            contents.add(trim(display));
            raw.add(entry);
        }

        if (!contents.isEmpty() || !sideThing.getString().isEmpty()) {
            put(currentName, sideThing, contents, raw, currentColor);
        }

        // The parser above is intentionally generic. These aliases make
        // lookups tolerant of Hypixel occasionally appending punctuation.
        alias("Commission", "Commissions");
        alias("Pet", "Pet");
    }

    private static void put(String name, Component detail, List<Component> lines,
                            List<PlayerInfo> raw, int color) {
        if (name == null || name.isBlank()) return;
        widgets.put(name.strip(), new Widget(detail, lines, raw, color));
    }

    private static void alias(String source, String target) {
        if (!widgets.containsKey(target) && widgets.containsKey(source)) {
            widgets.put(target, widgets.get(source));
        }
    }

    private static NameAndInfo getNameAndInfo(Component text) {
        StringBuilder name = new StringBuilder();
        MutableComponent detail = Component.empty();
        AtomicBoolean inInfo = new AtomicBoolean(false);
        AtomicInteger color = new AtomicInteger(0xFFFF00);

        text.visit((style, value) -> {
            if (value == null) return Optional.empty();

            if (inInfo.get()) {
                detail.append(Component.literal(value).withStyle(style));
            } else if (value.contains(":")) {
                inInfo.set(true);
                String[] split = value.split(":", 2);
                name.append(split[0]);
                detail.append(Component.literal(split[1]).withStyle(style));
                if (style.getColor() != null) color.set(style.getColor().getValue());
            } else {
                name.append(value);
            }
            return Optional.empty();
        }, Style.EMPTY);

        String cleanName = name.toString().strip();
        // "Commissions:" and "Pet:" are both represented as names without
        // their trailing colon by this parser.
        return new NameAndInfo(cleanName, detail, color.get());
    }

    private static Component trim(Component text) {
        List<Component> parts = new ArrayList<>();
        AtomicBoolean leadingFound = new AtomicBoolean(false);

        text.visit((style, value) -> {
            if (value == null) return Optional.empty();

            String trimmed = value;
            if (!leadingFound.get()) {
                trimmed = trimmed.stripLeading();
                if (!trimmed.isBlank()) leadingFound.set(true);
                else return Optional.empty();
            }

            parts.add(Component.literal(trimmed).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);

        for (int i = parts.size() - 1; i >= 0; i--) {
            Component part = parts.get(i);
            String trimmed = part.getString().stripTrailing();
            if (!trimmed.isBlank()) {
                parts.set(i, Component.literal(trimmed).setStyle(part.getStyle()));
                break;
            }
            parts.remove(i);
        }

        MutableComponent out = Component.empty();
        parts.forEach(out::append);
        return out;
    }

    private record NameAndInfo(String name, Component detail, int color) {}

    private static final class PredicateState {}

    public record Widget(Component detail, List<Component> lines,
                         List<PlayerInfo> playerListEntries, int color) {
        public static final Widget EMPTY =
            new Widget(Component.empty(), List.of(), List.of(), 0xFFFF00);

        public Widget {
            detail = detail == null ? Component.empty() : detail.copy();
            lines = lines == null ? List.of()
                : lines.stream().map(Component::copy).toList();
            playerListEntries = playerListEntries == null ? List.of()
                : List.copyOf(playerListEntries);
        }

        public Widget(Component detail, List<Component> lines, List<PlayerInfo> raw) {
            this(detail, lines, raw, 0xFFFF00);
        }
    }
}

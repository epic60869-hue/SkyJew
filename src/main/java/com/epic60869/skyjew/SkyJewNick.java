package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkyJewNick {
    private static final Map<String, Integer> COLORS = Map.ofEntries(
        Map.entry("black", 0x000000), Map.entry("dark_blue", 0x0000AA), Map.entry("dark_green", 0x00AA00),
        Map.entry("dark_aqua", 0x00AAAA), Map.entry("dark_red", 0xAA0000), Map.entry("dark_purple", 0xAA00AA),
        Map.entry("gold", 0xFFAA00), Map.entry("gray", 0xAAAAAA), Map.entry("dark_gray", 0x555555),
        Map.entry("blue", 0x5555FF), Map.entry("green", 0x55FF55), Map.entry("aqua", 0x55FFFF),
        Map.entry("red", 0xFF5555), Map.entry("light_purple", 0xFF55FF), Map.entry("yellow", 0xFFFF55),
        Map.entry("white", 0xFFFFFF)
    );

    private static final String[] STYLES = {
        "Plain", "Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red",
        "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua",
        "Red", "Light Purple", "Yellow", "White", "Rainbow"
    };

    private static final Map<UUID, RemoteNick> REMOTE_NICKS = new ConcurrentHashMap<>();
    private static SkyJewConfig config;

    private SkyJewNick() {}

    public static void init(SkyJewConfig loadedConfig) {
        config = loadedConfig;
    }

    public static void set(String input) {
        String value = input == null ? "" : input.trim();

        if (value.isEmpty() || value.equalsIgnoreCase("off") || value.equalsIgnoreCase("reset")) {
            config().misc.nickname.enabled = false;
            config().misc.nickname.name = "";
            config().misc.nickname.style = "Plain";
            config().misc.nickname.customHex = "";
            save();
            SkyJewGlobalChat.sendNicknameUpdate();
            message("Nickname disabled.", 0x55FF55);
            return;
        }

        String name = value;
        String style = "Plain";
        String customHex = "";

        String[] parts = value.split("\\s+", 3);
        if (parts.length >= 2) {
            String first = parts[0].toLowerCase(Locale.ROOT);
            if (first.equals("rainbow")) {
                style = "Rainbow";
                name = value.substring(parts[0].length()).trim();
            } else if (COLORS.containsKey(first)) {
                style = displayStyle(first);
                name = value.substring(parts[0].length()).trim();
            } else if (first.matches("#[0-9a-fA-F]{6}")) {
                style = "Plain";
                customHex = first;
                name = value.substring(parts[0].length()).trim();
            }
        }

        if (name.isBlank()) {
            message("Usage: /sj nick <name> | /sj nick <color> <name> | /sj nick rainbow <name> | /sj nick off", 0xFFFF55);
            return;
        }

        name = clean(name);
        config().misc.nickname.enabled = true;
        config().misc.nickname.name = name;
        config().misc.nickname.style = style;
        config().misc.nickname.customHex = customHex;
        save();
        SkyJewGlobalChat.sendNicknameUpdate();

        message("Nickname set to " + name + ("Rainbow".equals(style) ? " (rainbow)" : ""), 0x55FF55);
    }

    public static boolean enabled() {
        return config() != null && config().misc.nickname.enabled
            && config().misc.nickname.name != null && !config().misc.nickname.name.isBlank();
    }

    public static String mode() {
        return config().misc.nickname.style == null ? "Plain" : config().misc.nickname.style;
    }

    public static String customHex() {
        return config().misc.nickname.customHex == null ? "" : config().misc.nickname.customHex;
    }

    public static void applyGuiName(String name) {
        String value = clean(name);
        config().misc.nickname.name = value;
        config().misc.nickname.enabled = !value.isBlank();
        save();
        SkyJewGlobalChat.sendNicknameUpdate();
    }

    public static String outgoingName() {
        if (!config().misc.nickname.enabled || config().misc.nickname.name == null || config().misc.nickname.name.isBlank()) {
            return Minecraft.getInstance().getUser().getName();
        }
        return config().misc.nickname.name;
    }

    public static Component tabDisplayName(Component original, UUID uuid, String actualName) {
        if (original == null || uuid == null || actualName == null || actualName.isBlank()) return original;

        Minecraft mc = Minecraft.getInstance();
        boolean local = uuid.equals(mc.getUser().getProfileId())
            || (mc.player != null && uuid.equals(mc.player.getUUID()))
            || actualName.equals(mc.getUser().getName());
        // A local setting is authoritative for our own TAB entry; do not let a
        // delayed relay packet overwrite the local nickname/style.
        RemoteNick remote = local ? null : REMOTE_NICKS.get(uuid);

        String nickName = null;
        String nickMode = null;
        String nickHex = null;

        if (remote != null && remote.enabled && !remote.name.isBlank()) {
            nickName = remote.name;
            nickMode = remote.mode;
            nickHex = remote.customHex;
        } else if (local
                && config().misc.nickname.enabled
                && config().misc.nickname.name != null
                && !config().misc.nickname.name.isBlank()) {
            nickName = config().misc.nickname.name;
            nickMode = config().misc.nickname.style;
            nickHex = config().misc.nickname.customHex;
        }

        if (nickName == null) {
            // Hypixel tab entries are fake profiles, so fall back to matching usernames in the text.
            if (local || config() == null || !config().misc.nickname.seeOtherNicks) return original;
            Component result = original;
            for (RemoteNick r : REMOTE_NICKS.values()) {
                if (!r.enabled || r.name.isBlank() || r.username.isBlank() || isLocalUuid(r.uuid)) continue;
                result = replaceExactName(result, r.username, styled(r.name, r.mode, r.customHex));
            }
            return result;
        }

        final String finalNickName = nickName;
        final String finalNickMode = nickMode;
        final String finalNickHex = nickHex;

        Component replacement = styled(finalNickName, finalNickMode, finalNickHex);
        Component result = replaceExactName(original, actualName, replacement);

        if (result != original) return result;
        if (local) {
            return styled(finalNickName, finalNickMode, finalNickHex);
        }
        return original;
    }

    /**
     * Applies the local nickname directly to the client-side TAB entry.
     * Hypixel can periodically replace PlayerInfo display names, so this is
     * re-applied from the client tick instead of relying only on a render mixin.
     */
    public static void applyToTab(PlayerInfo info) {
        if (info == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getUser() == null || info.getProfile() == null) return;
        if (!info.getProfile().id().equals(mc.getUser().getProfileId())
            && !info.getProfile().name().equals(mc.getUser().getName())) return;

        Component original = info.getTabListDisplayName();
        if (original == null) original = Component.literal(info.getProfile().name());

        Component replacement = tabDisplayName(original, info.getProfile().id(), info.getProfile().name());
        if (replacement != null && !replacement.equals(original)) {
            info.setTabListDisplayName(replacement);
        }
    }

    /**
     * Replaces the local player's name in normal Minecraft chat so /sj nick
     * is not limited to SkyJew's separate global-chat channel.
     */
    /** Replaces synced nicknames for other SkyJew users in normal Hypixel chat. */
    public static Component replaceOtherNamesInChat(Component message) {
        if (message == null || config() == null || !config().misc.nickname.seeOtherNicks) return message;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return message;

        Component result = message;
        // Use the relay's UUID -> username mapping first. This is important for
        // Hypixel /msg and guild messages: those are server/game messages and
        // the target player may not be present in the local tab list.
        for (RemoteNick remote : REMOTE_NICKS.values()) {
            if (!remote.enabled || remote.name.isBlank() || remote.username.isBlank()) continue;
            if (isLocalUuid(remote.uuid)) continue;
            result = replaceExactName(result, remote.username, styled(remote.name, remote.mode, remote.customHex));
        }

        // Fill in missing usernames from the live tab list for older persisted
        // nickname records which were created before usernames were persisted.
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            if (info == null || info.getProfile() == null) continue;
            UUID uuid = info.getProfile().id();
            String actualName = info.getProfile().name();
            RemoteNick remote = REMOTE_NICKS.get(uuid);
            if (remote == null || !remote.enabled || remote.name.isBlank()
                || actualName == null || actualName.isBlank() || isLocalUuid(uuid)) continue;
            if (!remote.username.equals(actualName)) {
                REMOTE_NICKS.put(uuid, remote.withUsername(actualName));
            }
            result = replaceExactName(result, actualName, styled(remote.name, remote.mode, remote.customHex));
        }
        return result;
    }

    private static Component replaceExactName(Component message, String actualName, Component replacement) {
        if (message == null || actualName == null || actualName.isBlank()) return message;

        // Hypixel splits chat into many styled component leaves. Usernames can
        // cross a leaf boundary, so matching each leaf separately is unreliable.
        List<StyledRun> runs = new java.util.ArrayList<>();
        StringBuilder plain = new StringBuilder();
        message.visit((style, value) -> {
            if (value != null && !value.isEmpty()) {
                runs.add(new StyledRun(value, style));
                plain.append(value);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);

        String text = plain.toString();
        MutableComponent result = Component.empty();
        int cursor = 0;
        boolean changed = false;

        while (cursor < text.length()) {
            int at = text.indexOf(actualName, cursor);
            if (at < 0) break;
            int end = at + actualName.length();
            boolean leftOk = at == 0 || !isNameChar(text.charAt(at - 1));
            boolean rightOk = end >= text.length() || !isNameChar(text.charAt(end));
            if (!leftOk || !rightOk) {
                cursor = at + 1;
                continue;
            }

            appendStyledRange(result, runs, cursor, at);
            result.append(withRealNameHover(replacement, actualName, styleAt(runs, at)));
            changed = true;
            cursor = end;
        }

        if (!changed) return message;
        appendStyledRange(result, runs, cursor, text.length());
        return result;
    }

    /**
     * Wraps a nickname so hovering it shows the player's real username. Any hover text
     * Hypixel already had on the name is kept below it, and click actions are preserved.
     */
    private static Component withRealNameHover(Component replacement, String actualName, Style original) {
        MutableComponent hoverText = Component.literal("Real name: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(actualName).withStyle(ChatFormatting.WHITE));
        if (original.getHoverEvent() instanceof HoverEvent.ShowText(Component existing)) {
            hoverText.append(Component.literal("\n")).append(existing);
        }
        Style style = Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(hoverText))
            .withClickEvent(original.getClickEvent());
        return Component.empty().setStyle(style).append(replacement.copy());
    }

    private static Style styleAt(List<StyledRun> runs, int index) {
        int offset = 0;
        for (StyledRun run : runs) {
            if (index < offset + run.text.length()) return run.style;
            offset += run.text.length();
        }
        return Style.EMPTY;
    }

    private static void appendStyledRange(MutableComponent out, List<StyledRun> runs,
                                          int start, int end) {
        if (start >= end) return;
        int offset = 0;
        for (StyledRun run : runs) {
            int runStart = offset;
            int runEnd = offset + run.text.length();
            int from = Math.max(start, runStart);
            int to = Math.min(end, runEnd);
            if (from < to) {
                out.append(Component.literal(run.text.substring(from - runStart, to - runStart))
                    .setStyle(run.style));
            }
            offset = runEnd;
            if (offset >= end) break;
        }
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    public static Component replaceOwnNameInChat(Component message) {
        if (message == null || config() == null || !config().misc.nickname.enabled
            || config().misc.nickname.name == null || config().misc.nickname.name.isBlank()) {
            return message;
        }

        String actualName = Minecraft.getInstance().getUser().getName();
        if (actualName == null || actualName.isBlank()) return message;

        return replaceExactName(message, actualName,
            styled(config().misc.nickname.name, mode(), customHex()));
    }

    public static Component displayName(String actualName) {
        if (!config().misc.nickname.enabled
            || !actualName.equals(Minecraft.getInstance().getUser().getName())
            || config().misc.nickname.name == null
            || config().misc.nickname.name.isBlank()) {
            return Component.literal(actualName);
        }
        return styled(config().misc.nickname.name, mode(), customHex());
    }

    public static Component displayName(UUID uuid, String actualName) {
        if (uuid != null) {
            RemoteNick remote = REMOTE_NICKS.get(uuid);
            if (remote != null && remote.enabled && !remote.name.isBlank()) {
                return styled(remote.name, remote.mode, remote.customHex);
            }
        }
        return displayName(actualName);
    }

    public static void updateRemote(UUID uuid, boolean enabled, String name, String mode, String customHex) {
        updateRemote(uuid, "", enabled, name, mode, customHex);
    }

    public static void updateRemote(UUID uuid, String username, boolean enabled, String name, String mode, String customHex) {
        if (uuid == null) return;
        if (!enabled || name == null || name.isBlank()) {
            REMOTE_NICKS.remove(uuid);
            return;
        }
        String safeUsername = username == null ? "" : cleanUsername(username);
        RemoteNick previous = REMOTE_NICKS.get(uuid);
        if (safeUsername.isBlank() && previous != null) safeUsername = previous.username;
        REMOTE_NICKS.put(uuid, new RemoteNick(
            uuid,
            safeUsername,
            clean(name),
            cleanMode(mode),
            cleanHex(customHex),
            true
        ));
    }

    public static void removeRemote(UUID uuid) {
        if (uuid != null) REMOTE_NICKS.remove(uuid);
    }

    public static void clearRemote() {
        REMOTE_NICKS.clear();
    }

    public static Component styled(String text) {
        return styled(text, mode(), customHex());
    }

    public static Component styled(String text, String style, String customHex) {
        String safeStyle = style == null ? "Plain" : style;

        if ("Rainbow".equalsIgnoreCase(safeStyle)) {
            MutableComponent out = Component.empty();
            int n = Math.max(1, text.length());
            for (int i = 0; i < text.length(); i++) {
                float hue = (float) i / n;
                int rgb = Color.HSBtoRGB(hue, 0.95f, 1.0f) & 0xFFFFFF;
                out.append(Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(rgb)));
            }
            return out;
        }

        String key = safeStyle.toLowerCase(Locale.ROOT).replace(' ', '_');
        Integer rgb = COLORS.get(key);

        if ("plain".equals(key) && customHex != null && customHex.matches("#[0-9a-fA-F]{6}")) {
            rgb = Integer.parseInt(customHex.substring(1), 16);
        }

        return rgb == null
            ? Component.literal(text)
            : Component.literal(text).setStyle(Style.EMPTY.withColor(rgb));
    }

    private static String clean(String value) {
        value = value.replace("\\r", "").replace("\\n", "").trim();
        return value.substring(0, Math.min(32, value.length()));
    }

    private static String cleanMode(String value) {
        if (value == null || value.isBlank()) return "Plain";
        String cleaned = value.replaceAll("[^A-Za-z ]", "").trim();
        return cleaned.isBlank() ? "Plain" : cleaned.substring(0, Math.min(20, cleaned.length()));
    }

    private static String cleanHex(String value) {
        return value != null && value.matches("#[0-9a-fA-F]{6}") ? value.toUpperCase(Locale.ROOT) : "";
    }

    private static String displayStyle(String key) {
        for (String style : STYLES) {
            if (style.toLowerCase(Locale.ROOT).replace(' ', '_').equals(key)) return style;
        }
        return "Plain";
    }

    private static SkyJewConfig config() {
        return config;
    }

    private static void save() {
        SkyJewConfig.saveCurrent(config());
    }

    private static void message(String text, int color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("[SkyJew] " + text)
                .setStyle(Style.EMPTY.withColor(color)));
        }
    }

    private static boolean isLocalUuid(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        return uuid != null && (uuid.equals(mc.getUser().getProfileId())
            || (mc.player != null && uuid.equals(mc.player.getUUID())));
    }

    private static String cleanUsername(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_]", "").substring(
            0, Math.min(16, value.replaceAll("[^A-Za-z0-9_]", "").length()));
    }

    private record StyledRun(String text, Style style) {}

    private record RemoteNick(UUID uuid, String username, String name, String mode, String customHex, boolean enabled) {
        private RemoteNick withUsername(String value) {
            return new RemoteNick(uuid, value, name, mode, customHex, enabled);
        }
    }
}

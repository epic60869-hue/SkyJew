package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Locale;
import java.util.Map;

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

        message("Nickname set to " + name + ("Rainbow".equals(style) ? " (rainbow)" : ""), 0x55FF55);
    }

    public static String mode() {
        return config().misc.nickname.style == null ? "Plain" : config().misc.nickname.style;
    }

    public static void applyGuiName(String name) {
        String value = clean(name);
        config().misc.nickname.name = value;
        config().misc.nickname.enabled = !value.isBlank();
        save();
    }

    public static String outgoingName() {
        if (!config().misc.nickname.enabled || config().misc.nickname.name == null || config().misc.nickname.name.isBlank()) {
            return Minecraft.getInstance().getUser().getName();
        }
        return config().misc.nickname.name;
    }

    public static Component displayName(String actualName) {
        if (!config().misc.nickname.enabled
            || !actualName.equals(Minecraft.getInstance().getUser().getName())
            || config().misc.nickname.name == null
            || config().misc.nickname.name.isBlank()) {
            return Component.literal(actualName);
        }
        return styled(config().misc.nickname.name);
    }

    public static Component styled(String text) {
        String style = config().misc.nickname.style == null ? "Plain" : config().misc.nickname.style;

        if ("Rainbow".equalsIgnoreCase(style)) {
            MutableComponent out = Component.empty();
            int n = Math.max(1, text.length());
            for (int i = 0; i < text.length(); i++) {
                float hue = (float) i / n;
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.95f, 1.0f) & 0xFFFFFF;
                out.append(Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(rgb)));
            }
            return out;
        }

        String key = style.toLowerCase(Locale.ROOT).replace(' ', '_');
        Integer rgb = COLORS.get(key);

        if ("plain".equals(key) && config().misc.nickname.customHex != null
            && config().misc.nickname.customHex.matches("#[0-9a-fA-F]{6}")) {
            rgb = Integer.parseInt(config().misc.nickname.customHex.substring(1), 16);
        }

        return rgb == null
            ? Component.literal(text)
            : Component.literal(text).setStyle(Style.EMPTY.withColor(rgb));
    }

    private static String clean(String value) {
        value = value.replace("\\r", "").replace("\\n", "").trim();
        return value.substring(0, Math.min(32, value.length()));
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
}

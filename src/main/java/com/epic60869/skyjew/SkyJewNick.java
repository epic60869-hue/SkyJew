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

    private static SkyJewConfig config;

    private SkyJewNick() {}

    public static void init(SkyJewConfig loadedConfig) { config = loadedConfig; }

    public static void set(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty() || value.equalsIgnoreCase("off") || value.equalsIgnoreCase("reset")) {
            config().nickname.enabled = false;
            config().nickname.name = "";
            config().nickname.style = "plain";
            save();
            message("Nickname disabled.", 0x55FF55);
            return;
        }

        String mode = "plain";
        String name = value;
        String color = "";

        String[] parts = value.split("\\s+", 3);
        if (parts.length >= 2) {
            String first = parts[0].toLowerCase(Locale.ROOT);
            if (first.equals("rainbow")) {
                mode = "rainbow";
                name = value.substring(parts[0].length()).trim();
            } else if (COLORS.containsKey(first)) {
                mode = first;
                name = value.substring(parts[0].length()).trim();
                color = first;
            } else if (first.startsWith("#") && first.matches("#[0-9a-fA-F]{6}")) {
                mode = first;
                name = value.substring(parts[0].length()).trim();
                color = first;
            }
        }

        if (name.isBlank()) {
            message("Usage: /sj nick <name> | /sj nick <color> <name> | /sj nick rainbow <name> | /sj nick off", 0xFFFF55);
            return;
        }

        name = name.replaceAll("[\\r\
]", "").substring(0, Math.min(32, name.length()));
        config().nickEnabled = true;
        config().nickName = name;
        config().nickMode = mode;
        config().nickname.customHex = color;
        save();
        message("Nickname set to " + name + (mode.equals("rainbow") ? " (rainbow)" : ""), 0x55FF55);
    }

    public static String mode() { return config().nickMode == null ? "plain" : config().nickMode; }

    public static void applyGuiName(String name) {
        String value = name == null ? "" : name.replaceAll("[\\r\\n]", "").trim();
        if (value.length() > 32) value = value.substring(0, 32);
        config().nickName = value;
        config().nickEnabled = !value.isBlank();
        save();
    }

    public static String outgoingName() {
        if (!config().nickEnabled || config().nickname.name == null || config().nickname.name.isBlank()) {
            return Minecraft.getInstance().getUser().getName();
        }
        return config().nickName;
    }

    public static Component displayName(String actualName) {
        if (!config().nickEnabled || !actualName.equals(Minecraft.getInstance().getUser().getName())
            || config().nickName == null || config().nickName.isBlank()) {
            return Component.literal(actualName);
        }
        return styled(config().nickName);
    }

    public static Component styled(String text) {
        String mode = config().nickMode == null ? "plain" : config().nickMode;
        if (mode.equals("rainbow")) {
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

        Integer rgb = COLORS.get(mode);
        if (rgb == null && mode.matches("#[0-9a-fA-F]{6}")) {
            rgb = Integer.parseInt(mode.substring(1), 16);
        }
        return rgb == null ? Component.literal(text) : Component.literal(text).setStyle(Style.EMPTY.withColor(rgb));
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

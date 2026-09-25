package com.epic60869.skyjew;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public enum SkyJewItemRarity {
    COMMON(TextColor.WHITE),
    UNCOMMON(TextColor.GREEN),
    RARE(0x5555FF, TextColor.BLUE),
    EPIC(0xAA00AA, TextColor.DARK_PURPLE),
    LEGENDARY(0xFFAA00, TextColor.GOLD),
    MYTHIC(TextColor.LIGHT_PURPLE),
    DIVINE(TextColor.AQUA),
    SPECIAL(TextColor.RED),
    VERY_SPECIAL(TextColor.RED),
    ULTIMATE(0xAA0000, TextColor.DARK_RED),
    ADMIN(0xAA0000, TextColor.DARK_RED),
    UNKNOWN(TextColor.DARK_GRAY);

    public static final Identifier BACKGROUND_SPRITE =
        Identifier.fromNamespaceAndPath("skyjew", "item_background_circular");

    public final String name;
    public final int color;
    public final int legacyColor;

    SkyJewItemRarity(TextColor color) {
        this(color.getValue(), color);
    }

    SkyJewItemRarity(int color, TextColor legacyColor) {
        this.name = name().replace("_", " ");
        this.color = color & 0xFFFFFF;
        this.legacyColor = legacyColor.getValue();
    }

    public static Optional<SkyJewItemRarity> containsName(String text) {
        if (text == null) return Optional.empty();
        String name = text.toUpperCase(java.util.Locale.ROOT);
        // Check the longer names first, matching Skyblocker's behaviour.
        return Arrays.stream(values())
            .filter(r -> r != UNKNOWN && name.contains(r.name()))
            .reduce((first, second) -> second);
    }

    public static SkyJewItemRarity fromColor(int color) {
        int rgb = ARGB.opaque(color);
        return Arrays.stream(values())
            .filter(r -> ARGB.opaque(r.color) == rgb)
            .findFirst()
            .orElse(UNKNOWN);
    }
}

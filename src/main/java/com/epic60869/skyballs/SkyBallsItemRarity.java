package com.epic60869.skyballs;

import com.epic60869.skyballs.custom.util.SkyBlockColors;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;

/** Ported from Skyblocker's SkyblockItemRarity (LGPL-3.0). */
public enum SkyBallsItemRarity {
    COMMON(TextColor.WHITE),
    UNCOMMON(TextColor.GREEN),
    RARE(SkyBlockColors.BLUE, TextColor.BLUE),
    EPIC(SkyBlockColors.DARK_PURPLE, TextColor.DARK_PURPLE),
    LEGENDARY(SkyBlockColors.GOLD, TextColor.GOLD),
    MYTHIC(TextColor.LIGHT_PURPLE),
    DIVINE(TextColor.AQUA),
    SPECIAL(TextColor.RED),
    VERY_SPECIAL(TextColor.RED),
    ULTIMATE(SkyBlockColors.DARK_RED, TextColor.DARK_RED),
    ADMIN(SkyBlockColors.DARK_RED, TextColor.DARK_RED),
    UNKNOWN(TextColor.DARK_GRAY);

    public final String name;
    public final int color;
    public final int legacyColor;

    SkyBallsItemRarity(TextColor color, TextColor legacyColor) {
        this.name = this.name().replace("_", " ");
        this.color = color.getValue();
        this.legacyColor = legacyColor.getValue();
    }

    SkyBallsItemRarity(TextColor color) {
        this(color, color);
    }

    public SkyBallsItemRarity next() {
        SkyBallsItemRarity[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    @Override
    public String toString() {
        return name;
    }

    public static Optional<SkyBallsItemRarity> containsName(String name) {
        // Find last because "UNCOMMON" contains "COMMON" and "VERY SPECIAL" contains "SPECIAL"
        return Arrays.stream(values())
            .filter(rarity -> name.contains(rarity.toString()))
            .reduce((first, second) -> second);
    }

    public static SkyBallsItemRarity fromColor(int color) {
        return Arrays.stream(values())
            .filter(rarity -> ARGB.opaque(rarity.color) == ARGB.opaque(color))
            .findFirst()
            .orElse(UNKNOWN);
    }
}

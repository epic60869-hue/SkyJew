package com.epic60869.skyballs;

import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/** /sj debug: prints what SkyBalls detects, to diagnose features that do not show. */
public final class SkyBallsDebug {
    private SkyBallsDebug() {}

    public static int run() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        SkyBallsConfig config = SkyBallsConfig.current();

        line("Server brand", mc.getConnection() == null ? "none" : String.valueOf(mc.getConnection().serverBrand()));
        line("Server address", mc.getCurrentServer() == null ? "none" : mc.getCurrentServer().ip);
        line("Hypixel detected", String.valueOf(Compat.isOnSkyblock()));
        line("SkyBlock scoreboard", String.valueOf(SkyBallsLocation.onSkyblock()));
        line("Area", SkyBallsLocation.area());
        line("Location", SkyBallsLocation.location());
        line("Dungeon floor", SkyBallsLocation.dungeonFloor());
        if (config != null) {
            line("Item rarity enabled", String.valueOf(config.misc.itemRarity.enabled));
            line("Calendar enabled", String.valueOf(config.misc.calendarTimeToRealTime));
        }

        ItemStack held = mc.player.getMainHandItem();
        if (!held.isEmpty()) {
            line("Held item", held.getHoverName().getString());
            line("Held rarity", SkyBallsItemBackgrounds.rarity(held).name());
            ItemLore lore = held.get(DataComponents.LORE);
            if (lore != null && !lore.lines().isEmpty()) {
                line("Last lore line", lore.lines().getLast().getString());
            }
        }
        return 1;
    }

    private static void line(String key, String value) {
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Compat.PREFIX.get()
            .append(Component.literal(key + ": ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(value == null || value.isEmpty() ? "-" : value).withStyle(ChatFormatting.WHITE)));
    }
}

package com.epic60869.skyjew;

import com.epic60869.skyjew.custom.CustomAnimatedHelmetTextures;
import com.epic60869.skyjew.custom.CustomArmorAnimatedDyes;
import com.epic60869.skyjew.custom.CustomArmorDyeColors;
import com.epic60869.skyjew.custom.CustomArmorTrims;
import com.epic60869.skyjew.custom.CustomConfigManager;
import com.epic60869.skyjew.custom.CustomHelmetTextures;
import com.epic60869.skyjew.custom.CustomItemNames;
import com.epic60869.skyjew.custom.RepoDyeColors;
import com.epic60869.skyjew.custom.RepoItems;
import com.epic60869.skyjew.custom.SkyblockItemModels;
import com.epic60869.skyjew.custom.screen.CustomizeScreen;
import com.epic60869.skyjew.custom.util.Compat;
import com.epic60869.skyjew.custom.util.GuiEquipmentRenderer;
import com.epic60869.skyjew.mixin.SkyJewCustomDataAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Entry point for SkyJew's port of Skyblocker's /skyblocker custom item and armor
 * customization (see the {@code custom} package). Everything is client-side and keyed
 * to the item's Hypixel UUID, so the server item itself is never modified.
 */
public final class SkyJewCustom {
    private SkyJewCustom() {}

    public static void init(Path configDir) {
        CustomConfigManager.init(configDir);
        Compat.init();
        GuiEquipmentRenderer.init();

        RepoItems.init();
        RepoDyeColors.init();
        CustomAnimatedHelmetTextures.init();
        CustomHelmetTextures.init();
        SkyblockItemModels.init();

        CustomItemNames.init();
        CustomArmorDyeColors.init();
        CustomArmorTrims.init();
        CustomArmorAnimatedDyes.init();
        CustomizeScreen.initThings();
    }

    /** Opens the customization screen on the armor tab, like /skyblocker custom. */
    public static void open(Minecraft mc, Screen parent) {
        // The armor tab previews the local player, so the screen needs a world.
        if (mc.player == null || mc.level == null) return;
        mc.execute(() -> mc.gui.setScreen(new CustomizeScreen(parent, false)));
    }

    /**
     * Whether the client is connected to Hypixel. Checks the server brand Hypixel sends as well as
     * the address, since the address can carry a port, an alias or come through a proxy.
     */
    public static boolean isHypixel(Minecraft mc) {
        try {
            if (mc.getConnection() == null) return false;
            String brand = mc.getConnection().serverBrand();
            if (brand != null && brand.toLowerCase(Locale.ROOT).contains("hypixel")) return true;
            if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null
                && mc.getCurrentServer().ip.toLowerCase(Locale.ROOT).contains("hypixel")) return true;
            return com.epic60869.skyjew.features.core.SkyJewLocation.onSkyblock();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** The Hypixel item UUID stored in the item's custom data, or an empty string. */
    public static String uuid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : ((SkyJewCustomDataAccessor) (Object) data).skyjew$getTag().getStringOr("uuid", "");
    }
}

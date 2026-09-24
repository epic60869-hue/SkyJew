package com.epic60869.skyjew;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** SkyOcean-inspired rebindable search keybind. Default: O. */
public final class SkyJewSearchKeybind {
    private static KeyMapping searchKey;

    private SkyJewSearchKeybind() {}

    public static void init() {
        searchKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.skyjew.search",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.skyjew"
        ));
    }

    public static void tick(Minecraft mc) {
        if (searchKey == null || mc.player == null || mc.gui.screen() != null) return;
        while (searchKey.consumeClick()) {
            mc.execute(() -> SkyJewStorageSearch.open(mc, ""));
        }
    }
}

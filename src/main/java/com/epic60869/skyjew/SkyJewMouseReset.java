package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Resets the cursor when selected Hypixel storage menus open. */
public final class SkyJewMouseReset {
    /**
     * True while the player is already inside one of the selected storage GUIs.
     * This is deliberately based on the GUI category, not the exact Screen
     * instance, so switching directly from Backpack -> Ender Chest -> Accessory
     * Bag does not move the mouse again.
     */
    private static boolean storageGuiOpen;
    private static long lastReset;

    private SkyJewMouseReset() {}

    public static void tick(Minecraft mc) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.mouseReset.enabled) {
            storageGuiOpen = false;
            return;
        }

        Screen screen = mc.gui.screen();
        if (screen == null) {
            // Leaving the storage GUI arms the reset for the next storage GUI.
            storageGuiOpen = false;
            return;
        }

        String title;
        try {
            title = screen.getTitle().getString()
                .replaceAll("§.", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        } catch (Throwable ignored) {
            return;
        }

        boolean isStorageGui =
            (config.misc.mouseReset.accessoryBag && matches(title, "accessory bag"))
            || (config.misc.mouseReset.enderChest && matches(title, "ender chest"))
            || (config.misc.mouseReset.backpack && matches(title, "backpack"));

        // Only reset when entering the storage-GUI group from outside it.
        // Changing between storage GUIs must leave the cursor where the user
        // put it.
        if (!isStorageGui) {
            storageGuiOpen = false;
            return;
        }

        if (storageGuiOpen || System.currentTimeMillis() - lastReset < 250L) {
            storageGuiOpen = true;
            return;
        }

        storageGuiOpen = true;
        lastReset = System.currentTimeMillis();

        // GLFW cursor coordinates are window coordinates, not framebuffer
        // pixel dimensions.
        mc.execute(() -> {
            long window = mc.getWindow().handle();
            double x = mc.getWindow().getGuiScaledWidth() / 2.0;
            double y = mc.getWindow().getGuiScaledHeight() / 2.0;
            GLFW.glfwSetCursorPos(window, x, y);
        });
    }

    private static boolean matches(String title, String value) {
        return title.equals(value)
            || title.startsWith(value + " ")
            || title.contains(" " + value + " ")
            || title.endsWith(" " + value);
    }
}

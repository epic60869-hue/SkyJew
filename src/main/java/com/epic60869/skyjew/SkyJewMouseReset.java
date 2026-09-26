package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.Locale;

/**
 * Resets the cursor when selected Hypixel storage menus open.
 *
 * The important part is preserving the cursor position across Hypixel's
 * storage-to-storage screen swaps. Minecraft/Hypixel can restore the mouse
 * position from the first container screen when a new container screen is
 * created, so simply avoiding a second reset is not enough.
 */
public final class SkyJewMouseReset {
    private static boolean storageGuiOpen;
    private static Screen lastStorageScreen;
    private static double lastCursorX;
    private static double lastCursorY;
    private static boolean haveCursorPosition;

    private SkyJewMouseReset() {}

    public static void tick(Minecraft mc) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.mouseReset.enabled) {
            resetState();
            return;
        }

        Screen screen = mc.gui.screen();
        if (screen == null) {
            resetState();
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

        if (!isStorageGui) {
            resetState();
            return;
        }

        if (!storageGuiOpen) {
            // First selected storage GUI after coming from outside storage:
            // perform the actual Mouse Reset.
            double x = mc.getWindow().getScreenWidth() / 2.0;
            double y = mc.getWindow().getScreenHeight() / 2.0;
            InputConstants.releaseMouse(mc.getWindow(), x, y);
        } else if (screen != lastStorageScreen && haveCursorPosition) {
            // Hypixel opened another storage screen. Do NOT reset to the
            // centre and do NOT accept the position restored by the new
            // Screen. Put the cursor back where it was in the previous
            // storage GUI.
            InputConstants.releaseMouse(mc.getWindow(), lastCursorX, lastCursorY);
        }

        storageGuiOpen = true;
        lastStorageScreen = screen;

        // Remember the position after handling the screen transition so the
        // next storage screen can restore exactly this position.
        readCursor(mc);
    }

    private static void readCursor(Minecraft mc) {
        lastCursorX = mc.mouseHandler.xpos();
        lastCursorY = mc.mouseHandler.ypos();
        haveCursorPosition = true;
    }

    private static void resetState() {
        storageGuiOpen = false;
        lastStorageScreen = null;
        haveCursorPosition = false;
    }

    private static boolean matches(String title, String value) {
        return title.equals(value)
            || title.startsWith(value + " ")
            || title.contains(" " + value + " ")
            || title.endsWith(" " + value);
    }
}

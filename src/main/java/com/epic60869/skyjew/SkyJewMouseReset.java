package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
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

        long window = mc.getWindow().handle();

        if (!storageGuiOpen) {
            // First selected storage GUI after coming from outside storage:
            // perform the actual Mouse Reset.
            double x = mc.getWindow().getGuiScaledWidth() / 2.0;
            double y = mc.getWindow().getGuiScaledHeight() / 2.0;
            GLFW.glfwSetCursorPos(window, x, y);
        } else if (screen != lastStorageScreen && haveCursorPosition) {
            // Hypixel opened another storage screen. Do NOT reset to the
            // centre and do NOT accept the position restored by the new
            // Screen. Put the cursor back where it was in the previous
            // storage GUI.
            GLFW.glfwSetCursorPos(window, lastCursorX, lastCursorY);
        }

        storageGuiOpen = true;
        lastStorageScreen = screen;

        // Remember the position after handling the screen transition so the
        // next storage screen can restore exactly this position.
        readCursor(window);
    }

    private static void readCursor(long window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1);
            DoubleBuffer y = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(window, x, y);
            lastCursorX = x.get(0);
            lastCursorY = y.get(0);
            haveCursorPosition = true;
        } catch (Throwable ignored) {
            // If the cursor cannot be read, the normal storage GUI still works.
        }
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

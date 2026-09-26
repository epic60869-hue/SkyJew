package com.epic60869.skyballs;

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
public final class SkyBallsMouseReset {
    private static boolean storageGuiOpen;
    private static Screen lastStorageScreen;
    private static double lastCursorX;
    private static double lastCursorY;
    private static boolean haveCursorPosition;

    private SkyBallsMouseReset() {}

    public static void tick(Minecraft mc) {
        SkyBallsConfig config = SkyBallsConfig.current();
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
            // GLFW works in window coordinates, not GUI-scaled ones (using those put the cursor near the
            // top-left corner at GUI scales above 1), so centre on the real window size.
            try (MemoryStack stack = MemoryStack.stackPush()) {
                java.nio.IntBuffer w = stack.mallocInt(1);
                java.nio.IntBuffer h = stack.mallocInt(1);
                GLFW.glfwGetWindowSize(window, w, h);
                GLFW.glfwSetCursorPos(window, w.get(0) / 2.0, h.get(0) / 2.0);
            }
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

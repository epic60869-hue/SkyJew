package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Optional cursor reset for common Hypixel storage menus. */
public final class SkyJewMouseReset {
    private static Screen lastScreen;
    private static long lastReset;

    private SkyJewMouseReset() {}

    public static void tick(Minecraft mc) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.mouseReset.enabled) return;

        Screen screen = mc.gui.screen();
        if (screen == null || screen == lastScreen) return;
        lastScreen = screen;

        String title;
        try {
            title = screen.getTitle().getString().toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
            return;
        }

        boolean reset = (config.misc.mouseReset.accessoryBag && contains(title, "accessory bag"))
            || (config.misc.mouseReset.enderChest && contains(title, "ender chest"))
            || (config.misc.mouseReset.backpack && contains(title, "backpack"));

        if (!reset || System.currentTimeMillis() - lastReset < 100L) return;

        lastReset = System.currentTimeMillis();
        mc.execute(() -> {
            long window = mc.getWindow().handle();
            GLFW.glfwSetCursorPos(window,
                mc.getWindow().getWidth() / 2.0,
                mc.getWindow().getHeight() / 2.0);
        });
    }

    private static boolean contains(String title, String value) {
        return title.contains(value);
    }
}

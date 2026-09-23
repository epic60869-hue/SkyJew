package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Resets the cursor when selected Hypixel storage menus open. */
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
            title = screen.getTitle().getString()
                .replaceAll("§.", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        } catch (Throwable ignored) {
            return;
        }

        boolean reset = (config.misc.mouseReset.accessoryBag && matches(title, "accessory bag"))
            || (config.misc.mouseReset.enderChest && matches(title, "ender chest"))
            || (config.misc.mouseReset.backpack && matches(title, "backpack"));

        if (!reset || System.currentTimeMillis() - lastReset < 250L) return;

        lastReset = System.currentTimeMillis();

        // GLFW cursor coordinates are window coordinates, not the framebuffer
        // pixel dimensions returned by Window#getWidth/#getHeight.
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

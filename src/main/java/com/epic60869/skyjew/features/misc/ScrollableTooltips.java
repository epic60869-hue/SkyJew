package com.epic60869.skyjew.features.misc;

import com.epic60869.skyjew.SkyJewConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * Scrollable tooltips: while a tooltip is showing, the mouse wheel moves it up and down (hold Shift to move it
 * sideways), so long tooltips that run off the screen can be read. The offset resets when a different tooltip
 * shows up. The tooltip is moved in SkyJewTooltipMixin after vanilla has placed it.
 */
public final class ScrollableTooltips {
    private static final int STEP = 10;
    private static int offsetX;
    private static int offsetY;
    private static long lastKey;
    private static long lastShown;
    private static boolean lastFits = true;

    private ScrollableTooltips() {}

    private static boolean enabled() {
        SkyJewConfig config = SkyJewConfig.current();
        return config != null && config.misc.scrollableTooltips;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            reset();
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) -> {
                if (!enabled() || System.currentTimeMillis() - lastShown > 100) return true;
                SkyJewConfig config = SkyJewConfig.current();
                if (config.misc.scrollOnlyLongTooltips && lastFits) return true;
                boolean sideways = Minecraft.getInstance().hasShiftDown() || horizontal != 0;
                double amount = horizontal != 0 ? horizontal : vertical;
                int step = (int) Math.signum(amount) * STEP;
                if (sideways) offsetX += step;
                else offsetY += step;
                return false;
            });
        });
    }

    public static void reset() {
        offsetX = 0;
        offsetY = 0;
    }

    /** Called with the tooltip's vanilla position and size; returns where to draw it. */
    public static Vector2ic offset(Vector2ic position, int width, int height, int lines, int screenWidth, int screenHeight) {
        if (!enabled()) return position;
        long key = ((long) width << 32) ^ ((long) height << 12) ^ lines;
        if (key != lastKey) {
            lastKey = key;
            reset();
        }
        lastShown = System.currentTimeMillis();
        lastFits = height + 8 <= screenHeight && width + 8 <= screenWidth;
        if (offsetX == 0 && offsetY == 0) return position;
        // Keep at least part of the tooltip on screen.
        offsetY = Math.max(-(height - 12) - position.y(), Math.min(screenHeight - 12 - position.y(), offsetY));
        offsetX = Math.max(-(width - 12) - position.x(), Math.min(screenWidth - 12 - position.x(), offsetX));
        return new Vector2i(position.x() + offsetX, position.y() + offsetY);
    }
}

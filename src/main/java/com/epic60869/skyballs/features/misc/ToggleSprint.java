package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsKeyMappings;
import com.epic60869.skyballs.features.core.SkyBallsHuds;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Toggle sprint: always sprint while it is on (like Odin's AutoSprint), switched with the "Toggle Sprint" key.
 * A small HUD shows "[Sprinting (Toggled)]" while it is on.
 */
public final class ToggleSprint {
    private static KeyMapping key;

    private ToggleSprint() {}

    private static SkyBallsConfig config() {
        return SkyBallsConfig.current();
    }

    /** Must run during client init, before the options are loaded. */
    public static void registerKey() {
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.skyballs.toggle_sprint", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), SkyBallsKeyMappings.CATEGORY));
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (key == null) return;
            while (key.consumeClick()) {
                SkyBallsConfig config = config();
                if (config == null) continue;
                config.misc.toggleSprint = !config.misc.toggleSprint;
                SkyBallsConfig.saveCurrent(config);
                if (mc.player != null) {
                    mc.gui.hud.setOverlayMessage(Component.literal("Toggle Sprint: " + (config.misc.toggleSprint ? "ON" : "OFF"))
                        .withStyle(config.misc.toggleSprint ? ChatFormatting.GREEN : ChatFormatting.RED), false);
                }
            }
        });
        SkyBallsHuds.register("toggle_sprint", "Toggle Sprint",
            () -> active() && config().misc.toggleSprintHud,
            () -> List.of(Component.literal("[Sprinting (Toggled)]").withStyle(ChatFormatting.GRAY)),
            List.of(Component.literal("[Sprinting (Toggled)]").withStyle(ChatFormatting.GRAY)),
            2, 250);
    }

    public static boolean active() {
        SkyBallsConfig config = config();
        return config != null && config.misc.toggleSprint;
    }
}

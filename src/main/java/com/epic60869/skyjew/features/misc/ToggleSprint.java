package com.epic60869.skyjew.features.misc;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewKeyMappings;
import com.epic60869.skyjew.features.core.SkyJewHuds;
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

    private static SkyJewConfig config() {
        return SkyJewConfig.current();
    }

    /** Must run during client init, before the options are loaded. */
    public static void registerKey() {
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.skyjew.toggle_sprint", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), SkyJewKeyMappings.CATEGORY));
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (key == null) return;
            while (key.consumeClick()) {
                SkyJewConfig config = config();
                if (config == null) continue;
                config.misc.toggleSprint = !config.misc.toggleSprint;
                SkyJewConfig.saveCurrent(config);
                if (mc.player != null) {
                    mc.gui.hud.setOverlayMessage(Component.literal("Toggle Sprint: " + (config.misc.toggleSprint ? "ON" : "OFF"))
                        .withStyle(config.misc.toggleSprint ? ChatFormatting.GREEN : ChatFormatting.RED), false);
                }
            }
        });
        SkyJewHuds.register("toggle_sprint", "Toggle Sprint",
            () -> active() && config().misc.toggleSprintHud,
            () -> List.of(Component.literal("[Sprinting (Toggled)]").withStyle(ChatFormatting.GRAY)),
            List.of(Component.literal("[Sprinting (Toggled)]").withStyle(ChatFormatting.GRAY)),
            2, 250);
    }

    public static boolean active() {
        SkyJewConfig config = config();
        return config != null && config.misc.toggleSprint;
    }
}

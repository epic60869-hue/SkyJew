package com.epic60869.skyballs;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class SkyBallsKeyMappings {
    private static final Identifier CATEGORY_ID =
        Identifier.fromNamespaceAndPath("skyballs", "main");

    public static KeyMapping.Category CATEGORY;
    public static KeyMapping SEARCH;

    private static boolean initialized;

    /**
     * Registers SkyBalls's key mappings during Fabric's client initialization.
     *
     * This must NOT happen from the client tick. Minecraft 26.2 has already
     * initialized GameOptions by the time the first tick runs, and registering
     * a key mapping then throws:
     * "GameOptions has already been initialised".
     */
    public static void init() {
        if (initialized) {
            return;
        }

        CATEGORY = KeyMapping.Category.register(CATEGORY_ID);

        SEARCH = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.skyballs.search",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_O,
                CATEGORY
            )
        );

        com.epic60869.skyballs.features.misc.ToggleSprint.registerKey();

        initialized = true;
    }

    private SkyBallsKeyMappings() {}
}

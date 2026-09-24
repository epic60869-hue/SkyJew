package com.epic60869.skyjew;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class SkyJewKeyMappings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("skyjew", "main")
    );

    public static final KeyMapping SEARCH = KeyMappingHelper.registerKeyMapping(
        new KeyMapping(
            "key.skyjew.search",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_O,
            CATEGORY
        )
    );

    private SkyJewKeyMappings() {}
}

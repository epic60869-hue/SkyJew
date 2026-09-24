package com.epic60869.skyjew.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Comparator;

@Mixin(PlayerTabOverlay.class)
public interface SkyJewPlayerTabOverlayAccessor {
    @Invoker("getOrdering")
    static Comparator<PlayerInfo> getOrdering() {
        throw new AssertionError();
    }
}
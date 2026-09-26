package com.epic60869.skyballs.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Comparator;

@Mixin(PlayerTabOverlay.class)
public interface SkyBallsPlayerTabOverlayAccessor {
    @Accessor("PLAYER_COMPARATOR")
    static Comparator<PlayerInfo> getOrdering() {
        throw new AssertionError();
    }
}

package com.epic60869.skyjew.mixin;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(PlayerTabOverlay.class)
public interface SkyJewPlayerTabOverlayAccessor {
    @Invoker("getPlayerInfos")
    List<PlayerInfo> skyjew$getPlayerInfos();
}

package com.epic60869.skyballs.mixin;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInfo.class)
public interface SkyBallsPlayerInfoAccessor {
    @Accessor("tabListDisplayName")
    Component skyballs$rawTabListDisplayName();
}

package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewNick;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class SkyJewPlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void skyjew$replaceTabName(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        GameProfile profile = info.getProfile();
        if (profile == null) return;

        cir.setReturnValue(SkyJewNick.displayName(profile.id(), cir.getReturnValue().getString()));
    }
}

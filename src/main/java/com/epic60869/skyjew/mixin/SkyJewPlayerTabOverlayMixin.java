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

        Component original = cir.getReturnValue();
        Component replacement = SkyJewNick.displayName(profile.id(), original.getString());
        // Do not replace the vanilla component when there is no nickname.
        // Returning a new literal here strips Hypixel's formatting and can make
        // the entire tab overlay appear white.
        if (!replacement.getString().equals(original.getString())) {
            cir.setReturnValue(replacement);
        }
    }
}

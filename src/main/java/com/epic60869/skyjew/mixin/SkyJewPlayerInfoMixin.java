package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewNick;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public class SkyJewPlayerInfoMixin {
    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void skyjew$replaceTabName(CallbackInfoReturnable<Component> cir) {
        PlayerInfo self = (PlayerInfo) (Object) this;
        GameProfile profile = self.getProfile();
        if (profile == null) return;

        Component original = cir.getReturnValue();
        if (original == null) {
            original = Component.literal(profile.name());
        }

        Component replacement = SkyJewNick.tabDisplayName(original, profile.id(), profile.name());
        if (replacement != null) {
            cir.setReturnValue(replacement);
        }
    }
}

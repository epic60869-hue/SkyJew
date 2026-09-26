package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsNick;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shows SkyBalls nicknames above players' heads (the nametag uses the player's display name). */
@Mixin(Player.class)
public abstract class SkyBallsPlayerNameTagMixin {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Component skyballs$nickNameTag(Component original) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide()) return original;
        return SkyBallsNick.nameTag(original, self.getUUID(), self.getGameProfile().name());
    }
}

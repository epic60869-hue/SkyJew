package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.ToggleSprint;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Misc > Toggle Sprint: sprint as if the sprint key were held, as in Odin's AutoSprint. */
@Mixin(LocalPlayer.class)
public abstract class SkyBallsToggleSprintMixin {
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
    private boolean skyballs$toggleSprint(boolean original) {
        return original || ToggleSprint.active();
    }
}

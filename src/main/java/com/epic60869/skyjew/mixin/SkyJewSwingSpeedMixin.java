package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.features.misc.HeldItemModel;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Misc > Held Item Model > Swing Speed: changes how long your own arm swing takes. */
@Mixin(LivingEntity.class)
public abstract class SkyJewSwingSpeedMixin {
    @ModifyReturnValue(method = "getCurrentSwingDuration", at = @At("RETURN"))
    private int skyjew$swingDuration(int original) {
        return (Object) this instanceof LocalPlayer ? HeldItemModel.swingDuration(original) : original;
    }
}

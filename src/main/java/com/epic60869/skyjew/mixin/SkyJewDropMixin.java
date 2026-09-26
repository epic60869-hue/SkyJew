package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.features.misc.SlotLocking;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Slot Locking: Q does nothing while the selected hotbar slot is locked. */
@Mixin(LocalPlayer.class)
public abstract class SkyJewDropMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void skyjew$lockedDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
        if (SlotLocking.blockDrop()) cir.setReturnValue(false);
    }
}

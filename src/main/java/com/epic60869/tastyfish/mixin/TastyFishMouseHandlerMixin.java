package com.epic60869.tastyfish.mixin;

import com.epic60869.tastyfish.TastyFishMouseLock;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyExpressionValue;

@Mixin(MouseHandler.class)
public class TastyFishMouseHandlerMixin {
    @ModifyExpressionValue(
        method = "turnPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
            ordinal = 0
        )
    )
    private Object tastyfish$mouseLock(Object original) {
        return TastyFishMouseLock.isLocked() ? -1 / 3d : original;
    }
}

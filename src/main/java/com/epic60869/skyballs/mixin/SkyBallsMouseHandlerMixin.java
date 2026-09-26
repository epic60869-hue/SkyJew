package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsMouseLock;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class SkyBallsMouseHandlerMixin {
    @Redirect(
        method = "turnPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
            ordinal = 0
        )
    )
    private Object skyballs$mouseLock(OptionInstance<?> option) {
        return SkyBallsMouseLock.isLocked() ? -1.0d / 3.0d : option.get();
    }
}

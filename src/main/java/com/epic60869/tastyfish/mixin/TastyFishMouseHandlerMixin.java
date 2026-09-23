package com.epic60869.tastyfish.mixin;

import com.epic60869.tastyfish.TastyFishMouseLock;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class TastyFishMouseHandlerMixin {
    @Redirect(
        method = "turnPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
            ordinal = 0
        )
    )
    private Object tastyfish$mouseLock(OptionInstance<?> option) {
        return TastyFishMouseLock.isLocked() ? -1.0d / 3.0d : option.get();
    }
}

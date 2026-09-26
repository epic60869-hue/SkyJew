package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsNick;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nicknames in text display entities, which Hypixel can use for name lines above heads. */
@Mixin(Display.TextDisplay.class)
public abstract class SkyBallsTextDisplayNickMixin {
    @ModifyReturnValue(method = "getText", at = @At("RETURN"))
    private Component skyballs$nickText(Component original) {
        Display.TextDisplay self = (Display.TextDisplay) (Object) this;
        return self.level().isClientSide() ? SkyBallsNick.worldText(original) : original;
    }
}

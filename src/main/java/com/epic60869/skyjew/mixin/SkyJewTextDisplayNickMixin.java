package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewNick;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nicknames in text display entities, which Hypixel can use for name lines above heads. */
@Mixin(Display.TextDisplay.class)
public abstract class SkyJewTextDisplayNickMixin {
    @ModifyReturnValue(method = "getText", at = @At("RETURN"))
    private Component skyjew$nickText(Component original) {
        Display.TextDisplay self = (Display.TextDisplay) (Object) this;
        return self.level().isClientSide() ? SkyJewNick.worldText(original) : original;
    }
}

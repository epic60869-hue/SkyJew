package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewNick;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nicknames in every entity nametag, including Hypixel's armor-stand name lines above players. */
@Mixin(EntityRenderer.class)
public abstract class SkyJewWorldNickMixin {
    @ModifyReturnValue(method = "getNameTag", at = @At("RETURN"))
    private Component skyjew$nickNameTag(Component original) {
        return SkyJewNick.worldText(original);
    }
}

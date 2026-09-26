package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.custom.CustomConfigManager;
import com.epic60869.skyballs.custom.util.Compat;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Custom item names, as in Skyblocker's ItemStackMixin (LGPL-3.0). */
@Mixin(ItemStack.class)
public abstract class SkyBallsItemStackMixin {
    @ModifyReturnValue(method = "getHoverName", at = @At("RETURN"))
    private Component skyballs$customItemNames(Component original) {
        if (Compat.isOnSkyblock() && !Compat.bypassCustomNames) {
            return CustomConfigManager.get().general.customItemNames.getOrDefault(Compat.uuid((ItemStack) (Object) this), original);
        }

        return original;
    }
}

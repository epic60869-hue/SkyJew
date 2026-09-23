package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewCustom;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class SkyJewItemStackMixin {
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void skyjew$customName(CallbackInfoReturnable<Component> cir) {
        ItemStack stack = (ItemStack)(Object)this;
        cir.setReturnValue(SkyJewCustom.customName(stack, cir.getReturnValue()));
    }
}

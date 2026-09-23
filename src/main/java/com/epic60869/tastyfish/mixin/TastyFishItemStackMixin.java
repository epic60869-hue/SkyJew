package com.epic60869.tastyfish.mixin;

import com.epic60869.tastyfish.TastyFishCustom;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class TastyFishItemStackMixin {
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void tastyfish$customName(CallbackInfoReturnable<Component> cir) {
        ItemStack stack = (ItemStack)(Object)this;
        cir.setReturnValue(TastyFishCustom.customName(stack, cir.getReturnValue()));
    }
}

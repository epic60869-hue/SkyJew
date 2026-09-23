package com.epic60869.tastyfish.mixin;

import com.epic60869.tastyfish.TastyFishCustom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DyedItemColor.class)
public class TastyFishDyedItemColorMixin {
    @Inject(method = "getOrDefault", at = @At("RETURN"), cancellable = true)
    private static void tastyfish$customDye(ItemStack stack, int defaultColor,
                                            CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(TastyFishCustom.customDye(stack, cir.getReturnValue()));
    }
}

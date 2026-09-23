package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewCustom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DyedItemColor.class)
public class SkyJewDyedItemColorMixin {
    @Inject(method = "getOrDefault", at = @At("RETURN"), cancellable = true)
    private static void skyjew$customDye(ItemStack stack, int defaultColor,
                                            CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SkyJewCustom.customDye(stack, cir.getReturnValue()));
    }
}

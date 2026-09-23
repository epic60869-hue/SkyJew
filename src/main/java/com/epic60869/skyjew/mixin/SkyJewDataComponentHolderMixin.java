package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewCustom;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataComponentHolder.class)
public interface SkyJewDataComponentHolderMixin {
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private <T> void skyjew$customTrim(DataComponentType<? extends T> type,
                                           CallbackInfoReturnable<T> cir) {
        if (type == DataComponents.TRIM && (Object)this instanceof ItemStack stack) {
            @SuppressWarnings("unchecked")
            T custom = (T) SkyJewCustom.customTrim(stack, (ArmorTrim) cir.getReturnValue());
            cir.setReturnValue(custom);
        }
    }
}

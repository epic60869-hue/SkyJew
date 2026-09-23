package com.epic60869.tastyfish.mixin;

import com.epic60869.tastyfish.TastyFishCustom;
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
public interface TastyFishDataComponentHolderMixin {
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private <T> void tastyfish$customTrim(DataComponentType<? extends T> type,
                                           CallbackInfoReturnable<T> cir) {
        if (type == DataComponents.TRIM && (Object)this instanceof ItemStack stack) {
            @SuppressWarnings("unchecked")
            T custom = (T) TastyFishCustom.customTrim(stack, (ArmorTrim) cir.getReturnValue());
            cir.setReturnValue(custom);
        }
    }
}

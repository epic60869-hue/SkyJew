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
    private <T> void skyjew$customComponents(DataComponentType<? extends T> type,
                                              CallbackInfoReturnable<T> cir) {
        if (!((Object) this instanceof ItemStack stack)) return;

        if (type == DataComponents.ENCHANTMENT_GLINT_OVERRIDE) {
            Boolean glint = SkyJewCustom.getGlint(stack);
            if (glint != null) {
                @SuppressWarnings("unchecked") T custom = (T) glint;
                cir.setReturnValue(custom);
                return;
            }
        }

        if (type == DataComponents.PROFILE && stack.is(net.minecraft.world.item.Items.PLAYER_HEAD)) {
            var profile = SkyJewCustom.helmetSkinProfile(stack);
            if (profile != null) {
                @SuppressWarnings("unchecked") T custom = (T) profile;
                cir.setReturnValue(custom);
                return;
            }
        }

        if (type == DataComponents.TRIM) {
            @SuppressWarnings("unchecked")
            T custom = (T) SkyJewCustom.customTrim(stack, (ArmorTrim) cir.getReturnValue());
            cir.setReturnValue(custom);
            return;
        }

        if (type == DataComponents.ITEM_MODEL) {
            String model = SkyJewCustom.getItemModel(stack);
            if (model != null && !model.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    T custom = (T) net.minecraft.resources.Identifier.parse(model);
                    cir.setReturnValue(custom);
                } catch (Throwable ignored) {}
            }
        }
    }
}

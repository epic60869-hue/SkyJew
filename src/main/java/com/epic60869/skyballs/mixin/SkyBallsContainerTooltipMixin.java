package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsDateCalculator;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Calendar dates in the menu's own tooltip too (see SkyBallsDateCalculator.addDates). */
@Mixin(AbstractContainerScreen.class)
public abstract class SkyBallsContainerTooltipMixin {
    @ModifyReturnValue(method = "getTooltipFromContainerItem", at = @At("RETURN"))
    private List<Component> skyballs$calendarDates(List<Component> lines, ItemStack stack) {
        try {
            return SkyBallsDateCalculator.addDates(stack, lines);
        } catch (Throwable t) {
            return lines;
        }
    }
}

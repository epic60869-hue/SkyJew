package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewItemBackgrounds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rarity backgrounds behind container items, as in Skyblocker's AbstractContainerScreenMixin. */
@Mixin(AbstractContainerScreen.class)
public abstract class SkyJewSlotBackgroundMixin {
    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void skyjew$drawItemBackground(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        SkyJewItemBackgrounds.draw(graphics, slot.getItem(), slot.x, slot.y);
    }
}

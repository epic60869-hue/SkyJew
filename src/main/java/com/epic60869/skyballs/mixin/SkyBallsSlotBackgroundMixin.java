package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsItemBackgrounds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks when an inventory screen is being drawn. The rarity background itself is drawn from
 * {@link SkyBallsItemDrawBackgroundMixin} at each item draw, so it also works when another mod
 * (e.g. Skysoft's custom inventory) replaces vanilla's slot rendering and draws items itself.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SkyBallsSlotBackgroundMixin {
    @Inject(method = "extractContents", at = @At("HEAD"))
    private void skyballs$beginContainer(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        SkyBallsItemBackgrounds.beginContainer((AbstractContainerScreen<?>) (Object) this);
    }

    @Inject(method = "extractContents", at = @At("RETURN"))
    private void skyballs$endContainer(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        SkyBallsItemBackgrounds.endContainer();
    }
}

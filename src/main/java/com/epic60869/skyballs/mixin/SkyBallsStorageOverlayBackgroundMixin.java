package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.StorageOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Storage Overlay: while it shows, the chest's background texture isn't drawn either. */
@Mixin(ContainerScreen.class)
public abstract class SkyBallsStorageOverlayBackgroundMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void skyballs$hideChestUnderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (StorageOverlay.applies((ContainerScreen) (Object) this)) ci.cancel();
    }
}

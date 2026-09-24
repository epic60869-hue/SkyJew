package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewRecipeOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class SkyJewRecipeOverlayMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void skyjew$renderRecipeOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        SkyJewRecipeOverlay.render(graphics, (AbstractContainerScreen<?>) (Object) this, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void skyjew$recipeOverlayClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (SkyJewRecipeOverlay.mouseClicked((AbstractContainerScreen<?>) (Object) this, event.x(), event.y(), event.button())) {
            cir.setReturnValue(true);
        }
    }
}

package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewExperimentHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class SkyJewExperimentScreenMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void skyjew$renderExperimentHelper(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    ) {
        SkyJewExperimentHelper.render(graphics, (AbstractContainerScreen<?>) (Object) this);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void skyjew$preventExperimentMisclick(
        MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir
    ) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (SkyJewExperimentHelper.handleClick(screen, event.x(), event.y())) {
            cir.setReturnValue(true);
        }
    }
}

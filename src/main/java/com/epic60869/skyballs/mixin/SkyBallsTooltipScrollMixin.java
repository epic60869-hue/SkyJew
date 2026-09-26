package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.ScrollableTooltips;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Misc > Tooltip Scroll: the mouse wheel pans a visible tooltip before the screen gets to scroll. */
@Mixin(MouseHandler.class)
public class SkyBallsTooltipScrollMixin {
    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean skyballs$scrollTooltip(Screen screen, double mouseX, double mouseY, double horizontal, double vertical, Operation<Boolean> original) {
        return ScrollableTooltips.didHandleMouseScroll(horizontal, vertical) || original.call(screen, mouseX, mouseY, horizontal, vertical);
    }
}

package com.epic60869.skyballs.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientTextTooltip.class)
public interface SkyBallsClientTextTooltipAccessor {
    @Accessor("text")
    FormattedCharSequence skyballs$getText();
}

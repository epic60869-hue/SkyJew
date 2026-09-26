package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.features.misc.ScrollableTooltips;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Misc > Scrollable Tooltips: moves the tooltip by the scroll offset after vanilla has placed it. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class SkyJewTooltipMixin {
    @ModifyExpressionValue(method = "tooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"))
    private Vector2ic skyjew$scrollTooltip(Vector2ic position, Font font, List<ClientTooltipComponent> lines, int xo, int yo, ClientTooltipPositioner positioner, Identifier style) {
        int width = 0;
        int height = lines.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent line : lines) {
            width = Math.max(width, line.getWidth(font));
            height += line.getHeight(font);
        }
        GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;
        return ScrollableTooltips.offset(position, width, height, lines.size(), self.guiWidth(), self.guiHeight());
    }
}

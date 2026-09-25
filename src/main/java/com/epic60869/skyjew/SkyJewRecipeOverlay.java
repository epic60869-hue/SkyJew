package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Draws the /sj recipe craft helper next to inventory screens. */
public final class SkyJewRecipeOverlay {
    private SkyJewRecipeOverlay() {}

    public static void render(GuiGraphicsExtractor g, AbstractContainerScreen<?> screen, int mouseX, int mouseY, float delta) {
        SkyJewCraftHelper.render(g, screen, mouseX, mouseY);
    }

    public static boolean mouseClicked(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        return SkyJewCraftHelper.mouseClicked(screen, mouseX, mouseY, button);
    }

    public static boolean mouseScrolled(AbstractContainerScreen<?> screen, double mouseX, double mouseY, double amount) {
        return SkyJewCraftHelper.mouseScrolled(screen, mouseX, mouseY, amount);
    }
}

package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Persistent SkyOcean-style Craft Helper shown on top of inventory screens. */
public final class SkyJewRecipeOverlay {
    private static final int WIDTH = 210;
    private static final int PADDING = 8;

    private SkyJewRecipeOverlay() {}

    public static void render(GuiGraphicsExtractor g, AbstractContainerScreen<?> screen, int mouseX, int mouseY, float delta) {
        if (!SkyJewRecipeCommand.hasSelectedRecipe()) return;
        var recipe = SkyJewRecipeCommand.selectedRecipe();
        if (recipe == null) return;

        int x = Math.min(screen.width - WIDTH - 8, (screen.width + 176) / 2 + 10);
        int y = Math.max(8, (screen.height - 166) / 2);
        int lines = 0;
        for (var i : recipe.ingredients()) if (i.amount() > 0) lines++;
        int height = Math.min(190, 46 + Math.min(lines, 9) * 15 + 10);

        g.fill(x, y, x + WIDTH, y + height, 0xD9101420);
        g.fill(x, y, x + WIDTH, y + 2, 0xFF9A6CFF);
        g.text(screen.getFont(), "Craft Helper", x + PADDING, y + 9, 0xFFFFFFFF, true);
        g.text(screen.getFont(), "×", x + WIDTH - 15, y + 8, 0xFFFF7777, true);

        String title = SkyJewRecipeCommand.selectedAmount() + "x " + SkyJewRecipeCommand.selectedName();
        if (title.length() > 27) title = title.substring(0, 26) + "…";
        g.text(screen.getFont(), title, x + PADDING, y + 25, 0xFFFFD34D, true);

        int row = 0;
        for (var ingredient : recipe.ingredients()) {
            if (ingredient.amount() <= 0 || row >= 9) continue;
            String name = SkyJewRecipeCommand.displayName(ingredient.id());
            int amount = ingredient.amount() * SkyJewRecipeCommand.selectedAmount();
            String line = "×" + amount + " " + name;
            if (line.length() > 30) line = line.substring(0, 29) + "…";
            g.text(screen.getFont(), line, x + PADDING, y + 43 + row * 15, 0xFFE1E7F0, false);
            row++;
        }
        g.text(screen.getFont(), "Click × or /sj recipe clear", x + PADDING, y + height - 10, 0xFF8794A8, false);
    }

    public static boolean mouseClicked(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        if (!SkyJewRecipeCommand.hasSelectedRecipe() || button != 0) return false;
        int x = Math.min(screen.width - WIDTH - 8, (screen.width + 176) / 2 + 10);
        int y = Math.max(8, (screen.height - 166) / 2);
        if (mouseX >= x + WIDTH - 25 && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + 25) {
            SkyJewRecipeCommand.clearSelected();
            return true;
        }
        return false;
    }
}

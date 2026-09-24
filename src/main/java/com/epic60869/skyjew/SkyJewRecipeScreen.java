package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** Lightweight SkyOcean-style recipe HUD. */
public final class SkyJewRecipeScreen extends Screen {
    private final Screen parent;
    private final String id;
    private final String name;
    private final int requestedAmount;
    private final SkyJewRecipeCommand.RecipeData recipe;

    public SkyJewRecipeScreen(Screen parent, String id, String name, int requestedAmount,
                              SkyJewRecipeCommand.RecipeData recipe) {
        super(Component.literal("SkyJew Recipe"));
        this.parent = parent;
        this.id = id;
        this.name = name;
        this.requestedAmount = requestedAmount;
        this.recipe = recipe;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x99000000);

        int w = 390;
        int h = 270;
        int left = (width - w) / 2;
        int top = (height - h) / 2;

        g.fill(left, top, left + w, top + h, 0xFF111722);
        g.fill(left, top, left + w, top + 2, 0xFF9A6CFF);
        g.text(font, "Recipe", left + 16, top + 14, 0xFFFFFFFF, true);
        g.text(font, name + " × " + requestedAmount, left + 16, top + 31, 0xFFFFD34D, true);
        g.text(font, id, left + 16, top + 46, 0xFF8794A8, false);

        int gx = left + 35;
        int gy = top + 70;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int x = gx + c * 42;
                int y = gy + r * 42;
                g.fill(x, y, x + 36, y + 36, 0xFF202B3C);
                SkyJewRecipeCommand.Ingredient ingredient = ingredientAt(r, c);
                if (ingredient != null && ingredient.amount() > 0) {
                    String n = SkyJewRecipeCommand.displayName(ingredient.id());
                    if (n.length() > 13) n = n.substring(0, 12) + "…";
                    g.text(font, n, x + 2, y + 6, 0xFFF3F6FF, false);
                    g.text(font, "×" + scaledAmount(ingredient.amount()), x + 2, y + 22, 0xFF58D8FF, true);
                }
            }
        }

        int tx = left + 190;
        int ty = top + 70;
        g.text(font, "Materials", tx, ty, 0xFFFFFFFF, true);
        int line = 0;
        for (SkyJewRecipeCommand.Ingredient ingredient : recipe.ingredients()) {
            if (ingredient.amount() <= 0) continue;
            String n = SkyJewRecipeCommand.displayName(ingredient.id());
            String text = "×" + scaledAmount(ingredient.amount()) + " " + n;
            if (text.length() > 29) text = text.substring(0, 28) + "…";
            g.text(font, text, tx, ty + 18 + line * 14, 0xFFE1E7F0, false);
            if (++line >= 10) break;
        }

        g.text(font, "ESC / click to close", left + 16, top + h - 24, 0xFF8794A8, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private int scaledAmount(int base) {
        return Math.max(1, base * requestedAmount);
    }

    private SkyJewRecipeCommand.Ingredient ingredientAt(int row, int col) {
        String slot = "" + (char)('A' + row) + (col + 1);
        for (SkyJewRecipeCommand.Ingredient ingredient : recipe.ingredients()) {
            if (ingredient.slot().equalsIgnoreCase(slot)) return ingredient;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}

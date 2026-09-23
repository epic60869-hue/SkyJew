package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Small Skyblocker-style item picker for the custom item editor. */
public final class SkyJewItemSelectScreen extends Screen {
    private final Screen parent;
    private final Consumer<ItemStack> callback;
    private final List<Integer> slots = new ArrayList<>();

    public SkyJewItemSelectScreen(Screen parent, Consumer<ItemStack> callback) {
        super(Component.literal("Select Item"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        slots.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack != null && !stack.isEmpty() && SkyJewCustom.hasUuid(stack)) {
                    slots.add(i);
                }
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF080B12);
        int panelW = Math.min(720, width - 30);
        int panelH = Math.min(420, height - 30);
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;

        g.fill(left, top, left + panelW, top + panelH, 0xFF141B27);
        g.text(font, "Select Item", left + 18, top + 16, 0xFFFFD34D, true);
        g.text(font, "Choose an item with a Hypixel UUID to customize.", left + 18, top + 34, 0xFF8D9AAF, false);

        int columns = 9;
        int cell = 52;
        int gridLeft = left + (panelW - columns * cell) / 2;
        int gridTop = top + 65;

        for (int i = 0; i < slots.size(); i++) {
            int x = gridLeft + (i % columns) * cell;
            int y = gridTop + (i / columns) * cell;
            boolean hover = mouseX >= x && mouseX < x + cell && mouseY >= y && mouseY < y + cell;
            g.fill(x + 1, y + 1, x + cell - 1, y + cell - 1, hover ? 0xFF303C4E : 0xFF1D2633);

            Minecraft mc = Minecraft.getInstance();
            ItemStack stack = mc.player.getInventory().getItem(slots.get(i));
            g.item(stack, x + 18, y + 10);
            g.itemDecorations(font, stack, x + 18, y + 10);
        }

        g.text(font, "Click an item · ESC to cancel", left + 18, top + panelH - 24, 0xFF8D9AAF, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        int panelW = Math.min(720, width - 30);
        int panelH = Math.min(420, height - 30);
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;
        int columns = 9;
        int cell = 52;
        int gridLeft = left + (panelW - columns * cell) / 2;
        int gridTop = top + 65;

        int col = (int)((event.x() - gridLeft) / cell);
        int row = (int)((event.y() - gridTop) / cell);
        int index = row * columns + col;
        if (col >= 0 && col < columns && row >= 0 && index < slots.size()
                && event.x() >= gridLeft && event.y() >= gridTop) {
            Minecraft mc = Minecraft.getInstance();
            ItemStack stack = mc.player.getInventory().getItem(slots.get(index)).copy();
            callback.accept(stack);
            mc.gui.setScreen(parent);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}

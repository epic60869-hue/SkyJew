package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Skyblocker-style item picker used by /sj custom.
 *
 * It intentionally resembles the vanilla/Skyblocker item browser:
 * search field at the top, dense item grid, hover highlighting and scrolling.
 */
public final class SkyJewItemSelectScreen extends Screen {
    private static final int PANEL = 0xFF111318;
    private static final int PANEL_2 = 0xFF1A1D23;
    private static final int CELL = 32;
    private static final int COLUMNS = 16;
    private static final int GRID_W = COLUMNS * CELL;
    private static final int ACCENT = 0xFFD7D7D7;
    private static final int TEXT = 0xFFF2F3F5;
    private static final int MUTED = 0xFFB5BAC1;
    private static final int HOVER = 0xFF3F4147;
    private static final int SELECTED = 0xFF5865F2;

    private final Screen parent;
    private final Consumer<ItemStack> callback;
    private final List<Item> items = new ArrayList<>();

    private EditBox search;
    private int scroll;
    private ItemStack hovered = ItemStack.EMPTY;

    public SkyJewItemSelectScreen(Screen parent, Consumer<ItemStack> callback) {
        super(Component.literal("Select Item"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        clearWidgets();
        items.clear();

        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (Item item : BuiltInRegistries.ITEM) {
            if (query.isEmpty()
                    || item.getDescription().getString().toLowerCase(Locale.ROOT).contains(query)) {
                items.add(item);
            }
        }

        int panelW = Math.min(GRID_W + 24, width - 30);
        int left = (width - panelW) / 2;
        int top = Math.max(20, (height - Math.min(560, height - 20)) / 2);

        search = new EditBox(font, left + 12, top + 12, panelW - 24, 24, Component.literal("Search items"));
        search.setValue(query);
        search.setHint(Component.literal("Search items..."));
        search.setResponder(value -> {
            scroll = 0;
            rebuildItems(value);
        });
        addRenderableWidget(search);
    }

    private void rebuildItems(String value) {
        items.clear();
        String query = value.trim().toLowerCase(Locale.ROOT);
        for (Item item : BuiltInRegistries.ITEM) {
            if (query.isEmpty()
                    || item.getDescription().getString().toLowerCase(Locale.ROOT).contains(query)) {
                items.add(item);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int panelW = Math.min(GRID_W + 24, width - 30);
        int panelH = Math.min(560, height - 20);
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;
        int right = left + panelW;
        int bottom = top + panelH;

        g.fill(0, 0, width, height, 0x99000000);
        g.fill(left, top, right, bottom, PANEL);
        outline(g, left, top, right, bottom, 0xFF44474E);

        g.text(font, "Select Item", left + 12, top - 14, TEXT, true);

        int gridTop = top + 48;
        int gridBottom = bottom - 28;
        int rows = Math.max(1, (items.size() + COLUMNS - 1) / COLUMNS);
        int visibleRows = Math.max(1, (gridBottom - gridTop) / CELL);
        int maxScroll = Math.max(0, rows - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        hovered = ItemStack.EMPTY;

        for (int i = 0; i < items.size(); i++) {
            int row = i / COLUMNS;
            int col = i % COLUMNS;
            int x = left + 12 + col * CELL;
            int y = gridTop + (row - scroll) * CELL;

            if (y < gridTop || y + CELL > gridBottom) continue;

            boolean hover = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            if (hover) {
                g.fill(x, y, x + CELL, y + CELL, HOVER);
                hovered = new ItemStack(items.get(i));
            }

            ItemStack stack = new ItemStack(items.get(i));
            g.item(stack, x + 8, y + 8);
        }

        if (!hovered.isEmpty()) {
            String name = hovered.getHoverName().getString();
            int tooltipW = Math.min(300, font.width(name) + 16);
            int tx = Math.min(mouseX + 10, width - tooltipW - 4);
            int ty = Math.min(mouseY + 10, height - 24);
            g.fill(tx, ty, tx + tooltipW, ty + 18, 0xF0101012);
            g.text(font, name, tx + 8, ty + 5, TEXT, false);
        }

        g.text(font, items.size() + " items  •  Scroll to browse  •  Click to select",
                left + 12, bottom - 18, MUTED, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int panelH = Math.min(560, height - 20);
        int top = (height - panelH) / 2;
        int gridTop = top + 48;
        int gridBottom = top + panelH - 28;
        int visibleRows = Math.max(1, (gridBottom - gridTop) / CELL);
        int rows = Math.max(1, (items.size() + COLUMNS - 1) / COLUMNS);
        int maxScroll = Math.max(0, rows - visibleRows);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int)Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        int panelW = Math.min(GRID_W + 24, width - 30);
        int panelH = Math.min(560, height - 20);
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;
        int gridTop = top + 48;
        int gridBottom = top + panelH - 28;

        if (event.x() < left + 12 || event.x() >= left + 12 + GRID_W
                || event.y() < gridTop || event.y() >= gridBottom) {
            return super.mouseClicked(event, doubleClick);
        }

        int col = (int)((event.x() - (left + 12)) / CELL);
        int row = (int)((event.y() - gridTop) / CELL) + scroll;
        int index = row * COLUMNS + col;

        if (col >= 0 && col < COLUMNS && index >= 0 && index < items.size()) {
            Minecraft mc = Minecraft.getInstance();
            ItemStack stack = new ItemStack(items.get(index));
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

    private static void outline(GuiGraphicsExtractor g, int l, int t, int r, int b, int c) {
        g.fill(l, t, r, t + 1, c);
        g.fill(l, b - 1, r, b, c);
        g.fill(l, t, l + 1, b, c);
        g.fill(r - 1, t, r, b, c);
    }
}

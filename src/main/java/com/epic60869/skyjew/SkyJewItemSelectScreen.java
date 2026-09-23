package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Skyblocker-style item selection popup adapted to SkyJew.
 *
 * Unlike the old registry browser, this shows the player's real inventory
 * stacks so Hypixel UUID/customization data is preserved when an item is
 * selected.
 *
 * Source inspiration: SkyblockerMod/Skyblocker
 * https://github.com/SkyblockerMod/Skyblocker
 * Skyblocker's source is licensed under LGPL-3.0.
 */
public final class SkyJewItemSelectScreen extends Screen {
    private static final int PANEL = 0xFF2B2D31;
    private static final int INNER = 0xFF313338;
    private static final int BORDER = 0xFF4A4D52;
    private static final int TEXT = 0xFFF2F3F5;
    private static final int MUTED = 0xFFB5BAC1;
    private static final int HOVER = 0x3333AAFF;
    private static final int CELL = 24;
    private static final int COLUMNS = 9;

    private final Screen parent;
    private final Consumer<ItemStack> callback;
    private final List<ItemStack> stacks = new ArrayList<>();

    private EditBox search;
    private int scroll;
    private int left;
    private int top;
    private int gridTop;
    private int gridBottom;

    public SkyJewItemSelectScreen(Screen parent, Consumer<ItemStack> callback) {
        super(Component.literal("Select Item"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        clearWidgets();

        String query = search == null ? "" : search.getValue();
        rebuildItems(query);

        int panelW = 9 * CELL + 24;
        int panelH = Math.min(300, height - 20);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        gridTop = top + 50;
        gridBottom = top + panelH - 30;

        search = new EditBox(font, left + 12, top + 12, panelW - 24, 22, Component.literal("Search"));
        search.setValue(query);
        search.setHint(Component.literal("Search inventory..."));
        search.setResponder(value -> {
            scroll = 0;
            rebuildItems(value);
        });
        addRenderableWidget(search);
    }

    private void rebuildItems(String value) {
        stacks.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String query = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

        addIfMatches(mc.player.getItemBySlot(EquipmentSlot.HEAD), query);
        addIfMatches(mc.player.getItemBySlot(EquipmentSlot.CHEST), query);
        addIfMatches(mc.player.getItemBySlot(EquipmentSlot.LEGS), query);
        addIfMatches(mc.player.getItemBySlot(EquipmentSlot.FEET), query);

        for (ItemStack stack : mc.player.getInventory()) {
            addIfMatches(stack, query);
        }
    }

    private void addIfMatches(ItemStack stack, String query) {
        if (stack == null || stack.isEmpty()) return;
        if (query.isEmpty() || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
            stacks.add(stack);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int panelW = 9 * CELL + 24;
        int panelH = Math.min(300, height - 20);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        gridTop = top + 50;
        gridBottom = top + panelH - 30;

        g.fill(0, 0, width, height, 0x99000000);
        g.fill(left, top, left + panelW, top + panelH, PANEL);
        outline(g, left, top, left + panelW, top + panelH, BORDER);
        g.text(font, "Select Item", left + 12, top - 14, TEXT, true);

        int rows = Math.max(1, (stacks.size() + COLUMNS - 1) / COLUMNS);
        int visibleRows = Math.max(1, (gridBottom - gridTop) / CELL);
        int maxScroll = Math.max(0, rows - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        ItemStack hovered = ItemStack.EMPTY;

        for (int i = 0; i < stacks.size(); i++) {
            int row = i / COLUMNS;
            int col = i % COLUMNS;
            int x = left + 12 + col * CELL;
            int y = gridTop + (row - scroll) * CELL;
            if (y < gridTop || y + CELL > gridBottom) continue;

            ItemStack stack = stacks.get(i);
            boolean hover = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            if (hover) {
                g.fill(x, y, x + CELL, y + CELL, HOVER);
                hovered = stack;
            }

            g.item(stack, x + 4, y + 4);
            if (!SkyJewCustom.hasUuid(stack)) {
                g.item(new ItemStack(Items.BARRIER), x + 4, y + 4);
            }
        }

        if (!hovered.isEmpty()) {
            String name = hovered.getHoverName().getString();
            String status = SkyJewCustom.hasUuid(hovered) ? "Customizable" : "No Hypixel UUID";
            int w = Math.min(300, Math.max(font.width(name), font.width(status)) + 18);
            int tx = Math.min(mouseX + 10, width - w - 4);
            int ty = Math.min(mouseY + 10, height - 42);
            g.fill(tx, ty, tx + w, ty + 32, 0xF0101012);
            g.text(font, name, tx + 8, ty + 5, TEXT, false);
            g.text(font, status, tx + 8, ty + 18, SkyJewCustom.hasUuid(hovered) ? 0xFF57F287 : 0xFFFF6B6B, false);
        }

        g.text(font, stacks.size() + " items  •  Click an item to customize it",
                left + 12, top + panelH - 18, MUTED, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int panelH = Math.min(300, height - 20);
        int visibleRows = Math.max(1, (panelH - 80) / CELL);
        int rows = Math.max(1, (stacks.size() + COLUMNS - 1) / COLUMNS);
        int maxScroll = Math.max(0, rows - visibleRows);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        if (event.x() >= left + 12 && event.x() < left + 12 + COLUMNS * CELL
                && event.y() >= gridTop && event.y() < gridBottom) {
            int col = (int) ((event.x() - (left + 12)) / CELL);
            int row = (int) ((event.y() - gridTop) / CELL) + scroll;
            int index = row * COLUMNS + col;

            if (col >= 0 && col < COLUMNS && index >= 0 && index < stacks.size()) {
                ItemStack stack = stacks.get(index);
                if (SkyJewCustom.hasUuid(stack)) {
                    callback.accept(stack);
                    minecraft.gui.setScreen(parent);
                }
                return true;
            }
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

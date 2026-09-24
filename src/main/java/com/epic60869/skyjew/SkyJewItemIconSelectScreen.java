package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Selects the client-side item model used as the icon/appearance of a
 * customized SkyBlock item. The source can be a normal Minecraft item or a
 * Hypixel item currently present in the player's inventory.
 */
public final class SkyJewItemIconSelectScreen extends Screen {
    private static final int CELL = 28;
    private static final int COLUMNS = 9;

    private final Screen parent;
    private final Consumer<Identifier> callback;
    private final List<ItemOption> options = new ArrayList<>();

    private EditBox search;
    private int left, top, gridTop, gridBottom;
    private int scroll;

    public SkyJewItemIconSelectScreen(Screen parent, Consumer<Identifier> callback) {
        super(Component.literal("Select Item Icon"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        String old = search == null ? "" : search.getValue();
        rebuildOptions(old);

        int panelW = COLUMNS * CELL + 24;
        int panelH = Math.min(360, height - 20);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        gridTop = top + 52;
        gridBottom = top + panelH - 28;

        search = new EditBox(font, left + 12, top + 12, panelW - 24, 22, Component.literal("Search"));
        search.setValue(old);
        search.setHint(Component.literal("Minecraft or Hypixel item..."));
        search.setResponder(value -> {
            scroll = 0;
            rebuildOptions(value);
        });
        addRenderableWidget(search);
    }

    private void rebuildOptions(String queryValue) {
        options.clear();
        String query = queryValue == null ? "" : queryValue.trim().toLowerCase(Locale.ROOT);

        // Real stacks first: these preserve Hypixel's custom ITEM_MODEL when present.
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            for (ItemStack stack : mc.player.getInventory()) addStackOption(stack, query);
            addStackOption(mc.player.getMainHandItem(), query);
            addStackOption(mc.player.getOffhandItem(), query);
        }

        // Then every normal Minecraft registry item.
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            String id = entry.getKey().toString();
            String name = new ItemStack(entry.getValue()).getHoverName().getString();
            if (!query.isEmpty()
                    && !name.toLowerCase(Locale.ROOT).contains(query)
                    && !id.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }

            var itemId = BuiltInRegistries.ITEM.getKey(entry.getValue());
            Identifier model = Identifier.fromNamespaceAndPath(
                itemId.getNamespace(),
                itemId.getPath()
            );
            options.add(new ItemOption(new ItemStack(entry.getValue()), model, name, id));
        }
    }

    private void addStackOption(ItemStack stack, String query) {
        if (stack == null || stack.isEmpty()) return;
        String name = stack.getHoverName().getString();
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        if (model == null) {
            model = Identifier.parse(id);
        }

        if (!query.isEmpty()
                && !name.toLowerCase(Locale.ROOT).contains(query)
                && !id.toLowerCase(Locale.ROOT).contains(query)) {
            return;
        }

        options.add(new ItemOption(stack.copy(), model, name, id));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int panelW = COLUMNS * CELL + 24;
        int panelH = Math.min(360, height - 20);

        g.fill(0, 0, width, height, 0x99000000);
        g.fill(left, top, left + panelW, top + panelH, 0xFF202328);
        g.fill(left, top, left + panelW, top + 2, 0xFF55FFFF);
        g.text(font, "Select Item Icon", left + 12, top - 14, 0xFFFFFFFF, true);
        g.text(font, "Minecraft items + Hypixel items in your inventory", left + 12, top + 39, 0xFFAAAAAA, false);

        int rows = Math.max(1, (options.size() + COLUMNS - 1) / COLUMNS);
        int visibleRows = Math.max(1, (gridBottom - gridTop) / CELL);
        int maxScroll = Math.max(0, rows - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        ItemOption hovered = null;
        for (int i = 0; i < options.size(); i++) {
            int row = i / COLUMNS;
            int col = i % COLUMNS;
            int x = left + 12 + col * CELL;
            int y = gridTop + (row - scroll) * CELL;
            if (y < gridTop || y + CELL > gridBottom) continue;

            ItemOption option = options.get(i);
            boolean hover = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            if (hover) {
                g.fill(x, y, x + CELL, y + CELL, 0x4433AAFF);
                hovered = option;
            }
            g.item(option.stack(), x + 6, y + 6);
        }

        if (hovered != null) {
            int tw = Math.min(360, Math.max(font.width(hovered.name()), font.width(hovered.model().toString())) + 20);
            int tx = Math.min(mouseX + 10, width - tw - 4);
            int ty = Math.min(mouseY + 10, height - 45);
            g.fill(tx, ty, tx + tw, ty + 34, 0xF0101012);
            g.text(font, hovered.name(), tx + 8, ty + 5, 0xFFFFFFFF, false);
            g.text(font, hovered.model().toString(), tx + 8, ty + 18, 0xFFAAAAAA, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int panelH = Math.min(360, height - 20);
        int visibleRows = Math.max(1, (panelH - 80) / CELL);
        int rows = Math.max(1, (options.size() + COLUMNS - 1) / COLUMNS);
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
            if (col >= 0 && col < COLUMNS && index >= 0 && index < options.size()) {
                callback.accept(options.get(index).model());
                minecraft.gui.setScreen(parent);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private record ItemOption(ItemStack stack, Identifier model, String name, String id) {}
}

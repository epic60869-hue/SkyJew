package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Search screen for cached Hypixel SkyBlock Ender Chests and Backpacks. */
public final class SkyJewStorageSearchScreen extends Screen {
    private static final int BG = 0xCC05070B;
    private static final int PANEL = 0xFF101722;
    private static final int PANEL_2 = 0xFF151D2A;
    private static final int BORDER = 0xFF33425A;
    private static final int ACCENT = 0xFF58D8FF;
    private static final int PURPLE = 0xFF9A6CFF;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF8794A8;
    private static final int RESULT_H = 52;

    private final Screen parent;
    private final String initialQuery;

    private EditBox searchBox;
    private boolean searchLore = true;
    private boolean includeInventory = true;
    private int scroll;
    private List<SkyJewStorageSearch.Result> results = List.of();

    public SkyJewStorageSearchScreen(Screen parent, String query) {
        super(Component.literal("Storage Search"));
        this.parent = parent;
        this.initialQuery = query == null ? "" : query;
    }

    @Override
    protected void init() {
        clearWidgets();

        int panelWidth = Math.min(1000, width - 50);
        int left = (width - panelWidth) / 2;
        int top = Math.max(30, (height - 650) / 2);

        searchBox = new EditBox(font, left + 18, top + 48, panelWidth - 220, 28,
                Component.literal("Search"));
        searchBox.setMaxLength(128);
        searchBox.setValue(initialQuery);
        searchBox.setHint(Component.literal("Search item name, SkyBlock ID or lore..."));
        searchBox.setResponder(value -> refresh());
        addRenderableWidget(searchBox);

        Button lore = Button.builder(Component.literal("Lore: ON"), button -> {
            searchLore = !searchLore;
            button.setMessage(Component.literal("Lore: " + (searchLore ? "ON" : "OFF")));
            refresh();
        }).bounds(left + panelWidth - 190, top + 48, 82, 28).build();
        addRenderableWidget(lore);

        Button inv = Button.builder(Component.literal("Inv: ON"), button -> {
            includeInventory = !includeInventory;
            button.setMessage(Component.literal("Inv: " + (includeInventory ? "ON" : "OFF")));
            refresh();
        }).bounds(left + panelWidth - 102, top + 48, 84, 28).build();
        addRenderableWidget(inv);

        refresh();
        setInitialFocus(searchBox);
    }

    private void refresh() {
        if (searchBox == null) return;
        results = SkyJewStorageSearch.search(Minecraft.getInstance(),
                searchBox.getValue(), searchLore, includeInventory);
        int visible = Math.max(1, visibleRows());
        scroll = Math.max(0, Math.min(scroll, Math.max(0, results.size() - visible)));
    }

    private int panelTop() {
        return Math.max(30, (height - 650) / 2);
    }

    private int panelWidth() {
        return Math.min(1000, width - 50);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int visibleRows() {
        return Math.max(1, (Math.min(650, height - 70) - 118) / RESULT_H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);

        int panelWidth = panelWidth();
        int left = panelLeft();
        int top = panelTop();
        int bottom = Math.min(height - 20, top + 650);

        g.fill(left, top, left + panelWidth, bottom, PANEL);
        outline(g, left, top, left + panelWidth, bottom, BORDER);
        g.fill(left, top, left + panelWidth, top + 2, PURPLE);

        g.text(font, "Storage Search", left + 18, top + 16, TEXT, true);
        g.text(font, "Cached Ender Chests + Backpacks", left + 150, top + 16, MUTED, false);

        int count = results.size();
        int storageCount = SkyJewStorageSearch.cachedStorageCount();
        g.text(font, count + " results  ·  " + storageCount + " storages cached",
                left + 18, top + 88, ACCENT, true);

        int listTop = top + 108;
        int listBottom = bottom - 32;
        g.fill(left + 14, listTop, left + panelWidth - 14, listBottom, 0xFF080C13);
        outline(g, left + 14, listTop, left + panelWidth - 14, listBottom, BORDER);

        int rows = Math.max(1, (listBottom - listTop - 2) / RESULT_H);
        for (int row = 0; row < rows; row++) {
            int index = scroll + row;
            if (index >= results.size()) break;

            int y = listTop + 1 + row * RESULT_H;
            SkyJewStorageSearch.Result result = results.get(index);
            boolean hover = mouseX >= left + 15 && mouseX <= left + panelWidth - 15
                    && mouseY >= y && mouseY < y + RESULT_H;

            g.fill(left + 15, y, left + panelWidth - 15, y + RESULT_H - 2,
                    hover ? 0xFF202B3C : PANEL_2);

            g.item(result.stack(), left + 25, y + 8);
            g.itemDecorations(font, result.stack(), left + 25, y + 8);

            String name = result.name();
            if (name.length() > 48) name = name.substring(0, 45) + "...";
            g.text(font, name, left + 61, y + 7, TEXT, true);

            String location = result.location();
            if (location.length() > 82) location = location.substring(0, 79) + "...";
            g.text(font, location, left + 61, y + 25, MUTED, false);

            if (result.stack().getCount() > 1) {
                g.text(font, "x" + result.stack().getCount(),
                        left + panelWidth - 80, y + 17, ACCENT, true);
            }
        }

        long age = SkyJewStorageSearch.oldestCacheAgeMs();
        String footer = age < 0 ? "No cached storages yet - open Ender Chests or Backpacks to build the cache."
                : "Oldest cache: " + formatAge(age) + " ago  ·  Click a result to open its storage";
        g.text(font, footer, left + 18, bottom - 21, MUTED, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int left = panelLeft();
            int top = panelTop();
            int bottom = Math.min(height - 20, top + 650);
            int listTop = top + 108;
            int listBottom = bottom - 32;
            int rows = Math.max(1, (listBottom - listTop - 2) / RESULT_H);

            if (event.x() >= left + 15 && event.x() <= left + panelWidth() - 15
                    && event.y() >= listTop && event.y() < listBottom) {
                int row = (int)((event.y() - listTop - 1) / RESULT_H);
                if (row >= 0 && row < rows) {
                    int index = scroll + row;
                    if (index >= 0 && index < results.size()) {
                        SkyJewStorageSearch.openResult(Minecraft.getInstance(), results.get(index));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
            int amount = scrollY > 0 ? -3 : 3;
            scroll = Math.max(0, Math.min(scroll + amount,
                    Math.max(0, results.size() - visibleRows())));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private static void outline(GuiGraphicsExtractor g, int left, int top, int right, int bottom, int color) {
        g.fill(left, top, right, top + 1, color);
        g.fill(left, bottom - 1, right, bottom, color);
        g.fill(left, top, left + 1, bottom, color);
        g.fill(right - 1, top, right, bottom, color);
    }

    private static String formatAge(long millis) {
        long seconds = millis / 1000L;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60L;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60L;
        if (hours < 24) return hours + "h";
        return (hours / 24L) + "d";
    }
}

package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SkyJewDyeSelectScreen extends Screen {
    private static final int COLUMNS = 4;
    private static final int CELL_W = 188;
    private static final int CELL_H = 28;
    private static final int PANEL_W = COLUMNS * CELL_W + 32;
    private static final int PANEL_H = 430;

    private final Screen parent;
    private final net.minecraft.world.item.ItemStack item;
    private final boolean animatedOnly;

    private final List<DyeEntry> entries = new ArrayList<>();
    private final List<Button> dyeButtons = new ArrayList<>();

    private EditBox search;
    private Button back;
    private int left;
    private int top;
    private int scroll;
    private boolean waitingForDyes;

    public SkyJewDyeSelectScreen(Screen parent, net.minecraft.world.item.ItemStack item) {
        this(parent, item, false);
    }

    public SkyJewDyeSelectScreen(Screen parent, net.minecraft.world.item.ItemStack item, boolean animatedOnly) {
        super(Component.literal(animatedOnly ? "Hypixel Animated Dyes" : "Hypixel Static Dyes"));
        this.parent = parent;
        this.item = item;
        this.animatedOnly = animatedOnly;
    }

    @Override
    protected void init() {
        waitingForDyes = !SkyJewCustom.dyeDataLoaded();
        rebuildEntries();
        rebuildWidgets();
    }

    private void rebuildEntries() {
        entries.clear();

        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);

        if (!SkyJewCustom.dyeDataLoaded()) return;

        if (animatedOnly) {
            for (Map.Entry<String, List<Integer>> entry : SkyJewCustom.hypixelAnimatedDyes().entrySet()) {
                String name = SkyJewCustom.dyeDisplayName(entry.getKey());
                if (query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query)
                        || entry.getKey().toLowerCase(Locale.ROOT).contains(query)) {
                    entries.add(new DyeEntry(name, entry.getValue(), true));
                }
            }
        } else {
            for (Map.Entry<String, Integer> entry : SkyJewCustom.hypixelStaticDyes().entrySet()) {
                String name = SkyJewCustom.dyeDisplayName(entry.getKey());
                if (query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query)
                        || entry.getKey().toLowerCase(Locale.ROOT).contains(query)) {
                    entries.add(new DyeEntry(name, List.of(entry.getValue()), false));
                }
            }
        }
    }

    private void rebuildWidgets() {
        clearWidgets();

        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;

        search = new EditBox(font, left + 16, top + 38, PANEL_W - 32, 24, Component.literal("Search"));
        search.setHint(Component.literal("Search Hypixel dyes..."));
        search.setResponder(value -> {
            scroll = 0;
            rebuildEntries();
            rebuildWidgets();
        });
        addRenderableWidget(search);

        dyeButtons.clear();

        int visibleRows = visibleRows();
        int start = scroll * COLUMNS;
        int end = Math.min(entries.size(), start + visibleRows * COLUMNS);

        for (int i = start; i < end; i++) {
            DyeEntry entry = entries.get(i);
            final int index = i;
            int local = i - start;
            int col = local % COLUMNS;
            int row = local / COLUMNS;

            int x = left + 16 + col * CELL_W;
            int y = top + 78 + row * CELL_H;

            int color = entry.colors().isEmpty() ? 0xFFFFFF : entry.colors().getFirst();
            Component message = Component.literal("■ ").withStyle(s -> s.withColor(color))
                .append(Component.literal(entry.name() + (entry.animated() ? "  (animated)" : ""))
                    .withColor(0xFFFFFFFF));

            Button button = Button.builder(message, b -> apply(index))
                .bounds(x, y, CELL_W - 6, 24)
                .build();

            addRenderableWidget(button);
            dyeButtons.add(button);
        }

        back = Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(left + PANEL_W - 100, top + PANEL_H - 34, 84, 24).build();
        addRenderableWidget(back);
    }

    private int visibleRows() {
        return Math.max(1, (PANEL_H - 130) / CELL_H);
    }

    private int maxScroll() {
        int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - visibleRows());
    }

    private void apply(int index) {
        if (index < 0 || index >= entries.size()) return;

        DyeEntry entry = entries.get(index);
        if (entry.animated()) {
            SkyJewCustom.setDye(item, null);
            SkyJewCustom.setAnimatedDye(item, entry.colors(), 10f,
                entry.colors().size() % 2 == 0, 0f);
        } else {
            int color = entry.colors().getFirst();
            SkyJewCustom.setAnimatedDye(item, null, null, 1f, false, 0f);
            SkyJewCustom.setDye(item, color);
        }

        minecraft.gui.setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();

        if (waitingForDyes && SkyJewCustom.dyeDataLoaded()) {
            waitingForDyes = false;
            rebuildEntries();
            rebuildWidgets();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF07090D);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF202328);
        g.fill(left, top, left + PANEL_W, top + 3, 0xFF55FFFF);

        g.text(font, animatedOnly ? "Hypixel Animated Dyes" : "Hypixel Static Dyes",
            left + 16, top + 16, 0xFFF2F3F5, true);

        if (!SkyJewCustom.dyeDataLoaded()) {
            g.text(font, "Loading Hypixel dye data...", left + 16, top + 68, 0xFFFFAA00, false);
        } else if (entries.isEmpty()) {
            g.text(font, "No dyes match your search.", left + 16, top + 90, 0xFFAAAAAA, false);
        } else {
            g.text(font, entries.size() + " dyes  •  click one to apply it immediately",
                left + 16, top + PANEL_H - 58, 0xFF9DA3AA, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        int next = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private record DyeEntry(String name, List<Integer> colors, boolean animated) {}
}

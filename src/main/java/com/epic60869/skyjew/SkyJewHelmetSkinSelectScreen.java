package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkyJewHelmetSkinSelectScreen extends Screen {
    private static final int COLUMNS = 6;
    private static final int CELL_W = 120;
    private static final int CELL_H = 40;
    private static final int PANEL_W = COLUMNS * CELL_W + 32;
    private static final int PANEL_H = 430;

    private final Screen parent;
    private final ItemStack item;
    private final List<SkinEntry> entries = new ArrayList<>();

    private EditBox search;
    private int left;
    private int top;
    private int scroll;
    private boolean waitingForSkins;

    public SkyJewHelmetSkinSelectScreen(Screen parent, ItemStack item) {
        super(Component.literal("Hypixel Helmet Skins"));
        this.parent = parent;
        this.item = item.copy();
    }

    @Override
    protected void init() {
        waitingForSkins = !SkyJewCustom.helmetSkinDataLoaded();
        rebuildEntries();
        rebuildWidgets();
    }

    private void rebuildEntries() {
        entries.clear();
        entries.add(new SkinEntry("None", null));

        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (!SkyJewCustom.helmetSkinDataLoaded()) return;

        for (SkyJewCustom.HelmetSkin skin : SkyJewCustom.helmetSkins()) {
            String name = skin.name();
            if (query.isEmpty()
                    || name.toLowerCase(Locale.ROOT).contains(query)
                    || skin.id().toLowerCase(Locale.ROOT).contains(query)) {
                entries.add(new SkinEntry(name, skin.texture()));
            }
        }
    }

    private void rebuildWidgets() {
        clearWidgets();

        left = Math.max(10, (width - PANEL_W) / 2);
        top = Math.max(10, (height - PANEL_H) / 2);

        search = new EditBox(font, left + 16, top + 38, PANEL_W - 32, 24, Component.literal("Search"));
        search.setHint(Component.literal("Search helmet skins..."));
        search.setResponder(value -> {
            scroll = 0;
            rebuildEntries();
            rebuildWidgets();
        });
        addRenderableWidget(search);

        int visibleRows = visibleRows();
        int start = scroll * COLUMNS;
        int end = Math.min(entries.size(), start + visibleRows * COLUMNS);

        for (int i = start; i < end; i++) {
            SkinEntry entry = entries.get(i);
            int local = i - start;
            int col = local % COLUMNS;
            int row = local / COLUMNS;
            int x = left + 16 + col * CELL_W;
            int y = top + 78 + row * CELL_H;

            addRenderableWidget(new SkinButton(
                x, y, CELL_W - 4, CELL_H - 2, entry,
                entry.texture() != null && entry.texture().equals(SkyJewCustom.getHelmetSkin(item)),
                this::apply
            ));
        }

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(left + PANEL_W - 100, top + PANEL_H - 34, 84, 24).build());
    }

    private int visibleRows() {
        return Math.max(1, (PANEL_H - 130) / CELL_H);
    }

    private int maxScroll() {
        int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - visibleRows());
    }

    private void apply(SkinEntry entry) {
        SkyJewCustom.setHelmetSkin(item, entry.texture());
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
        if (waitingForSkins && SkyJewCustom.helmetSkinDataLoaded()) {
            waitingForSkins = false;
            rebuildEntries();
            rebuildWidgets();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF07090D);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF202328);
        g.fill(left, top, left + PANEL_W, top + 3, 0xFF55FFFF);

        g.text(font, "Hypixel Helmet Skins", left + 16, top + 16, 0xFFF2F3F5, true);

        if (!SkyJewCustom.helmetSkinDataLoaded()) {
            g.text(font, "Loading helmet skins from the Hypixel item data...", left + 16, top + 68, 0xFFFFAA00, false);
        } else if (entries.size() <= 1) {
            g.text(font, "No helmet skins match your search.", left + 16, top + 90, 0xFFAAAAAA, false);
        } else {
            g.text(font, (entries.size() - 1) + " helmet skins  •  click one to apply it immediately",
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
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private record SkinEntry(String name, String texture) {}

    private static final class SkinButton extends AbstractWidget {
        private final SkinEntry entry;
        private final ItemStack head;
        private final boolean selected;
        private final java.util.function.Consumer<SkinEntry> onPress;

        private SkinButton(int x, int y, int width, int height, SkinEntry entry,
                           boolean selected, java.util.function.Consumer<SkinEntry> onPress) {
            super(x, y, width, height, Component.empty());
            this.entry = entry;
            this.selected = selected;
            this.onPress = onPress;
            this.head = entry.texture() == null
                ? new ItemStack(Items.BARRIER)
                : SkyJewCustom.createHelmetSkinStack(entry.texture());
            if (entry.texture() != null) {
                setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(entry.name())));
            }
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            int background = selected ? 0x4030FF70 : isHovered() ? 0x3020A0A0 : 0x18171A1D;
            g.fill(getX(), getY(), getRight(), getBottom(), background);
            g.item(head, getX() + 3, getY() + 3);

            String text = entry.name();
            if (text.length() > 15) {
                text = font.plainSubstrByWidth(text, Math.max(10, getWidth() - 30));
            }
            g.text(font, text, getX() + 27, getY() + 14, 0xFFF2F3F5, false);
            this.handleCursor(g);
        }

        @Override
        public void onClick(MouseButtonEvent click, boolean doubled) {
            onPress.accept(entry);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}

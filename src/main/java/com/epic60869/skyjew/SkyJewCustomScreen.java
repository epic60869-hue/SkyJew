package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class SkyJewCustomScreen extends Screen {
    private static final int PANEL = 0xFF202328;
    private static final int PANEL_2 = 0xFF292C31;
    private static final int BORDER = 0xFF3B4047;
    private static final int ACCENT = 0xFF55FFFF;
    private static final int TEXT = 0xFFF2F3F5;
    private static final int MUTED = 0xFF9DA3AA;
    private static final int SUCCESS = 0xFF57F287;

    private static final EquipmentSlot[] ARMOR = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final String[] ARMOR_NAMES = {"Helmet", "Chestplate", "Leggings", "Boots"};

    private final Screen parent;
    private int tab;
    private int selectedArmor;
    private ItemStack selectedItem = ItemStack.EMPTY;

    private EditBox itemName;
    private EditBox modelId;
    private EditBox animatedStart;
    private EditBox animatedEnd;
    private EditBox animatedDuration;
    private Checkbox cycleBack;
    private Checkbox glint;

    public SkyJewCustomScreen(Screen parent) {
        super(Component.literal("SkyJew Customisation"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();

        int panelW = Math.min(900, width - 28);
        int panelH = Math.min(500, height - 24);
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;

        addRenderableWidget(Button.builder(Component.literal("ARMOR"), b -> {
            tab = 0;
            selectedItem = ItemStack.EMPTY;
            rebuild();
        }).bounds(left + 18, top + 42, 120, 26).build());

        addRenderableWidget(Button.builder(Component.literal("ITEM"), b -> {
            tab = 1;
            selectedItem = ItemStack.EMPTY;
            rebuild();
        }).bounds(left + 144, top + 42, 120, 26).build());

        if (tab == 0) {
            buildArmor(left, top, panelW);
        } else {
            buildItem(left, top, panelW);
        }

        addRenderableWidget(Button.builder(Component.literal("Close"), b -> minecraft.gui.setScreen(parent))
            .bounds(left + panelW - 214, top + panelH - 36, 96, 26).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> minecraft.gui.setScreen(parent))
            .bounds(left + panelW - 110, top + panelH - 36, 96, 26).build());
    }

    private void buildArmor(int left, int top, int panelW) {
        int sideX = left + 18;
        int sideY = top + 82;
        int mainX = left + 215;
        ItemStack selected = selectedStack();

        for (int i = 0; i < ARMOR.length; i++) {
            final int index = i;
            addRenderableWidget(Button.builder(
                    Component.literal(index == selectedArmor ? "▶ " + ARMOR_NAMES[index] : ARMOR_NAMES[index]),
                    b -> {
                        selectedArmor = index;
                        selectedItem = ItemStack.EMPTY;
                        rebuild();
                    })
                .bounds(sideX, sideY + i * 32, 165, 26).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Select inventory item"), b ->
                minecraft.gui.setScreen(new SkyJewItemSelectScreen(this, stack -> {
                    selectedItem = stack.copy();
                    rebuild();
                }))
            .bounds(sideX, sideY + 140, 165, 26).build());

        if (selected.isEmpty()) return;

        addRenderableWidget(Button.builder(Component.literal("Hypixel Static Dye"), b ->
                minecraft.gui.setScreen(new SkyJewDyeSelectScreen(this, selected, false)))
            .bounds(mainX, top + 176, 205, 28).build());

        addRenderableWidget(Button.builder(Component.literal("Hypixel Animated Dye"), b ->
                minecraft.gui.setScreen(new SkyJewDyeSelectScreen(this, selected, true)))
            .bounds(mainX + 215, top + 176, 205, 28).build());

        addRenderableWidget(Button.builder(Component.literal("Reset Dye"), b -> {
            SkyJewCustom.setDye(selected, null);
            SkyJewCustom.setAnimatedDye(selected, (Integer) null, (Integer) null, 1f, false, 0f);
            rebuild();
        }).bounds(mainX, top + 210, 120, 24).build());

        animatedStart = box("Start #RRGGBB", mainX, top + 258, 145, "");
        animatedEnd = box("End #RRGGBB", mainX + 153, top + 258, 145, "");
        animatedDuration = box("Seconds", mainX + 306, top + 258, 88, "5");
        cycleBack = Checkbox.builder(Component.literal("Cycle back"), font)
            .pos(mainX + 402, top + 260).selected(true).build();

        addRenderableWidget(animatedStart);
        addRenderableWidget(animatedEnd);
        addRenderableWidget(animatedDuration);
        addRenderableWidget(cycleBack);

        addRenderableWidget(Button.builder(Component.literal("Apply custom animation"), b ->
                applyAnimatedDye(selected))
            .bounds(mainX, top + 292, 180, 25).build());
    }

    private void buildItem(int left, int top, int panelW) {
        int x = left + 215;
        int y = top + 92;

        addRenderableWidget(Button.builder(Component.literal("Select inventory item"), b ->
                minecraft.gui.setScreen(new SkyJewItemSelectScreen(this, stack -> {
                    selectedItem = stack.copy();
                    rebuild();
                }))
            ).bounds(left + 18, y, 165, 26).build());

        ItemStack selected = selectedItem.isEmpty()
            ? (Minecraft.getInstance().player == null
                ? ItemStack.EMPTY
                : Minecraft.getInstance().player.getMainHandItem())
            : selectedItem;

        if (selected.isEmpty()) return;

        itemName = box("Custom item name", x, y, 340,
            SkyJewCustom.getName(selected) == null ? "" : SkyJewCustom.getName(selected));
        addRenderableWidget(itemName);

        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            SkyJewCustom.setName(selected, itemName.getValue());
            rebuild();
        }).bounds(x + 348, y, 70, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            SkyJewCustom.setName(selected, null);
            rebuild();
        }).bounds(x + 424, y, 70, 22).build());

        Boolean currentGlint = SkyJewCustom.getGlint(selected);
        glint = Checkbox.builder(Component.literal("Override enchant glint"), font)
            .pos(x, y + 42).selected(currentGlint == null || currentGlint).build();
        addRenderableWidget(glint);

        addRenderableWidget(Button.builder(Component.literal("Apply glint"), b -> {
            SkyJewCustom.setGlint(selected, glint.selected());
            rebuild();
        }).bounds(x + 190, y + 40, 105, 24).build());

        modelId = box("Item model identifier", x, y + 82, 340,
            SkyJewCustom.getItemModel(selected) == null ? "" : SkyJewCustom.getItemModel(selected));
        addRenderableWidget(modelId);

        addRenderableWidget(Button.builder(Component.literal("Apply model"), b -> {
            SkyJewCustom.setItemModel(selected, modelId.getValue());
            rebuild();
        }).bounds(x + 348, y + 82, 110, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Select Item Icon"), b ->
                minecraft.gui.setScreen(new SkyJewItemIconSelectScreen(this, identifier -> {
                    SkyJewCustom.setItemModel(selected, identifier.toString());
                    rebuild();
                }))
            ).bounds(x, y + 118, 155, 25).build());

        addRenderableWidget(Button.builder(Component.literal("Reset Icon"), b -> {
            SkyJewCustom.setItemModel(selected, null);
            rebuild();
        }).bounds(x + 163, y + 118, 110, 25).build());
    }

    private EditBox box(String hint, int x, int y, int w, String value) {
        EditBox box = new EditBox(font, x, y, w, 22, Component.literal(hint));
        box.setHint(Component.literal(hint));
        box.setValue(value == null ? "" : value);
        box.setMaxLength(128);
        return box;
    }

    private ItemStack selectedStack() {
        if (!selectedItem.isEmpty()) return selectedItem;
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? ItemStack.EMPTY : mc.player.getItemBySlot(ARMOR[selectedArmor]);
    }

    private void applyAnimatedDye(ItemStack stack) {
        Integer a = parseHex(animatedStart.getValue());
        Integer b = parseHex(animatedEnd.getValue());
        if (a == null || b == null) return;

        float duration;
        try {
            duration = Float.parseFloat(animatedDuration.getValue());
        } catch (Exception ignored) {
            duration = 5f;
        }

        SkyJewCustom.setDye(stack, null);
        SkyJewCustom.setAnimatedDye(stack, a, b, duration, cycleBack.selected(), 0);
        rebuild();
    }

    private static Integer parseHex(String value) {
        if (value == null) return null;
        String s = value.trim();
        if (s.startsWith("#")) s = s.substring(1);
        return s.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(s, 16) : null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int panelW = Math.min(900, width - 28);
        int panelH = Math.min(500, height - 24);
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;

        g.fill(0, 0, width, height, 0xFF07090D);
        g.fill(left, top, left + panelW, top + panelH, PANEL);
        g.fill(left, top, left + panelW, top + 3, ACCENT);

        g.text(font, "SkyJew Customisation", left + 18, top + 15, TEXT, true);
        g.text(font, tab == 0 ? "Armour" : "Item", left + 18, top + 29, MUTED, false);

        g.fill(left + 18, top + 74, left + panelW - 18, top + panelH - 52, PANEL_2);
        outline(g, left + 18, top + 74, left + panelW - 18, top + panelH - 52, BORDER);

        if (tab == 0) {
            ItemStack selected = selectedStack();
            g.text(font, "Armour slot", left + 28, top + 86, TEXT, true);
            g.text(font, "Select a worn item or an inventory item to customise.", left + 28, top + 106, MUTED, false);

            if (!selected.isEmpty()) {
                int px = left + 235;
                int py = top + 100;
                g.fill(px - 8, py - 8, px + 56, py + 56, 0xFF17191D);
                outline(g, px - 8, py - 8, px + 56, py + 56, BORDER);
                g.item(selected, px + 16, py + 16);
                g.text(font, selected.getHoverName(), px + 68, py - 2, TEXT, false);

                String uuid = SkyJewCustom.uuid(selected);
                g.text(font, uuid.isBlank() ? "No Hypixel UUID — this item cannot be customised."
                        : "Hypixel UUID: " + uuid, px + 68, py + 16,
                    uuid.isBlank() ? 0xFFFF6B6B : SUCCESS, false);

                Integer dye = SkyJewCustom.getDye(selected);
                SkyJewCustom.AnimatedDye animated = SkyJewCustom.getAnimatedDye(selected);
                if (dye != null) {
                    g.fill(px + 68, py + 30, px + 84, py + 46, 0xFF000000 | dye);
                    g.text(font, "Static dye  #" + String.format(Locale.ROOT, "%06X", dye),
                        px + 90, py + 33, TEXT, false);
                } else if (animated != null) {
                    int preview = animated.keyframes().isEmpty() ? 0xFFFFFF : animated.keyframes().getFirst().color();
                    g.fill(px + 68, py + 30, px + 84, py + 46, 0xFF000000 | preview);
                    g.text(font, "Animated dye  (" + animated.keyframes().size() + " colours)",
                        px + 90, py + 33, TEXT, false);
                } else {
                    g.text(font, "No custom dye selected", px + 68, py + 34, MUTED, false);
                }

                g.text(font, "Dye", left + 235, top + 160, TEXT, true);
                g.text(font, "Choose a Hypixel dye below. The picker applies it immediately.", left + 235, top + 195, MUTED, false);
            } else {
                g.text(font, "No item selected", left + 235, top + 135, TEXT, true);
                g.text(font, "Equip an armour piece or click Select inventory item.", left + 235, top + 157, MUTED, false);
            }
        } else {
            ItemStack selected = selectedItem.isEmpty()
                ? (Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getMainHandItem())
                : selectedItem;
            if (!selected.isEmpty()) {
                int px = left + 235;
                int py = top + 100;
                g.item(selected, px + 18, py + 18);
                g.text(font, selected.getHoverName(), px + 62, py + 20, TEXT, false);
                g.text(font, "Rename, change the client-side icon/model, or override glint.", px + 62, py + 38, MUTED, false);
            } else {
                g.text(font, "No item selected", left + 235, top + 135, TEXT, true);
            }
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private static void outline(GuiGraphicsExtractor g, int l, int t, int r, int b, int c) {
        g.fill(l, t, r, t + 1, c);
        g.fill(l, b - 1, r, b, c);
        g.fill(l, t, l + 1, b, c);
        g.fill(r - 1, t, r, b, c);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}

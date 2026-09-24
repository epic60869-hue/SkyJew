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
    private final Screen parent;
    private int tab = 0;
    private int selectedArmor = 0;
    private ItemStack selectedItem = ItemStack.EMPTY;

    private EditBox itemName, dyeHex, trimMaterial, trimPattern, animatedStart, animatedEnd, animatedDuration, itemModel;
    private Checkbox cycleBack, glint;
    private static final EquipmentSlot[] ARMOR = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public SkyJewCustomScreen(Screen parent) {
        super(Component.literal("SkyJew Customization"));
        this.parent = parent;
    }

    @Override protected void init() { rebuild(); }

    private void rebuild() {
        clearWidgets();
        int left = width / 2 - 330, top = Math.max(12, height / 2 - 205);

        addRenderableWidget(Button.builder(Component.literal("Armor"), b -> { tab = 0; rebuild(); })
            .bounds(left, top + 28, 150, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Item"), b -> { tab = 1; rebuild(); })
            .bounds(left + 156, top + 28, 150, 24).build());

        if (tab == 0) buildArmor(left, top);
        else buildItem(left, top);

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
            .bounds(width / 2 - 100, height - 34, 95, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(width / 2 + 5, height - 34, 95, 24).build());
    }

    private void buildArmor(int left, int top) {
        int panelY = top + 68;
        int previewX = left + 12;
        int controlsX = left + 180;

        addRenderableWidget(Button.builder(Component.literal("Select armor/item"), b ->
            minecraft.gui.setScreen(new SkyJewItemSelectScreen(this, stack -> {
                selectedItem = stack.copy();
                tab = 0;
                rebuild();
            }))).bounds(previewX, panelY, 150, 24).build());

        for (int i = 0; i < ARMOR.length; i++) {
            final int slot = i;
            String label = switch (i) {
                case 0 -> "Helmet"; case 1 -> "Chestplate"; case 2 -> "Leggings"; default -> "Boots";
            };
            ItemStack stack = Minecraft.getInstance().player == null ? ItemStack.EMPTY
                : Minecraft.getInstance().player.getItemBySlot(ARMOR[i]);
            String marker = selectedArmor == i ? "▶ " : "";
            addRenderableWidget(Button.builder(Component.literal(marker + label),
                b -> { selectedArmor = slot; selectedItem = ItemStack.EMPTY; rebuild(); })
                .bounds(previewX, panelY + 30 + i * 28, 150, 24).build());
        }

        ItemStack selected = selectedStack();
        if (!selected.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("Reset all customization"), b -> {
                SkyJewCustom.clearAll(selected); rebuild();
            }).bounds(controlsX, panelY, 205, 24).build());

            dyeHex = box("Dye #RRGGBB", controlsX, panelY + 32, 205, selectedDye(selected));
            addRenderableWidget(dyeHex);
            addRenderableWidget(Button.builder(Component.literal("Apply Dye"), b -> applyDye(selected))
                .bounds(controlsX + 212, panelY + 32, 100, 24).build());

            trimMaterial = box("Trim material (e.g. minecraft:gold)", controlsX, panelY + 64, 205,
                SkyJewCustom.getTrim(selected) == null ? "" : SkyJewCustom.getTrim(selected).material());
            trimPattern = box("Trim pattern (e.g. minecraft:sentry)", controlsX, panelY + 96, 205,
                SkyJewCustom.getTrim(selected) == null ? "" : SkyJewCustom.getTrim(selected).pattern());
            addRenderableWidget(trimMaterial); addRenderableWidget(trimPattern);
            addRenderableWidget(Button.builder(Component.literal("Apply Trim"), b ->
                SkyJewCustom.setTrim(selected, trimMaterial.getValue(), trimPattern.getValue()))
                .bounds(controlsX + 212, panelY + 80, 100, 24).build());

            animatedStart = box("Animated start", controlsX, panelY + 128, 150, "");
            animatedEnd = box("Animated end", controlsX, panelY + 158, 150, "");
            animatedDuration = box("Seconds", controlsX + 158, panelY + 128, 75, "5");
            cycleBack = Checkbox.builder(Component.literal("Cycle back"), font).pos(controlsX + 158, panelY + 158).selected(true).build();
            addRenderableWidget(animatedStart); addRenderableWidget(animatedEnd);
            addRenderableWidget(animatedDuration); addRenderableWidget(cycleBack);
            addRenderableWidget(Button.builder(Component.literal("Apply Animated Dye"), b -> applyAnimatedDye(selected))
                .bounds(controlsX + 240, panelY + 128, 130, 24).build());

            addRenderableWidget(Button.builder(Component.literal("Clear Dye"), b -> {
                SkyJewCustom.setDye(selected, null);
                SkyJewCustom.setAnimatedDye(selected, null, null, 1, false, 0);
                rebuild();
            }).bounds(controlsX + 240, panelY + 158, 130, 24).build());
        }
    }

    private void buildItem(int left, int top) {
        int x = left + 180, y = top + 68;
        ItemStack selected = selectedItem.isEmpty()
            ? (Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getMainHandItem())
            : selectedItem;

        addRenderableWidget(Button.builder(Component.literal("Select item"), b ->
            minecraft.gui.setScreen(new SkyJewItemSelectScreen(this, stack -> {
                selectedItem = stack.copy();
                rebuild();
            }))).bounds(left + 12, y, 150, 24).build());

        if (selected.isEmpty()) return;

        String currentName = SkyJewCustom.getName(selected);
        itemName = box("Custom item name", x, y, 250, currentName == null ? "" : currentName);
        addRenderableWidget(itemName);
        addRenderableWidget(Button.builder(Component.literal("Apply name"), b -> {
            SkyJewCustom.setName(selected, itemName.getValue()); rebuild();
        }).bounds(x, y + 30, 120, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Reset name"), b -> {
            SkyJewCustom.setName(selected, null); rebuild();
        }).bounds(x + 126, y + 30, 120, 24).build());

        Boolean currentGlint = SkyJewCustom.getGlint(selected);
        glint = Checkbox.builder(Component.literal("Override enchant glint"), font)
            .pos(x, y + 68).selected(currentGlint == null || currentGlint).build();
        addRenderableWidget(glint);
        addRenderableWidget(Button.builder(Component.literal("Apply Glint"), b -> {
            SkyJewCustom.setGlint(selected, glint.selected()); rebuild();
        }).bounds(x + 190, y + 66, 110, 24).build());

        itemModel = box("Item model id (optional)", x, y + 104, 250,
            SkyJewCustom.getItemModel(selected) == null ? "" : SkyJewCustom.getItemModel(selected));
        addRenderableWidget(itemModel);
        addRenderableWidget(Button.builder(Component.literal("Apply Model"), b -> {
            SkyJewCustom.setItemModel(selected, itemModel.getValue()); rebuild();
        }).bounds(x + 256, y + 104, 110, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Reset Model"), b -> {
            SkyJewCustom.setItemModel(selected, null); rebuild();
        }).bounds(x + 256, y + 134, 110, 24).build());
    }

    private EditBox box(String hint, int x, int y, int w, String value) {
        EditBox b = new EditBox(font, x, y, w, 22, Component.literal(hint));
        b.setHint(Component.literal(hint));
        b.setValue(value == null ? "" : value);
        b.setMaxLength(128);
        return b;
    }

    private ItemStack selectedStack() {
        if (!selectedItem.isEmpty()) return selectedItem;
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? ItemStack.EMPTY : mc.player.getItemBySlot(ARMOR[selectedArmor]);
    }

    private String selectedDye(ItemStack stack) {
        Integer color = SkyJewCustom.getDye(stack);
        return color == null ? "" : String.format(Locale.ROOT, "#%06X", color);
    }

    private void applyDye(ItemStack stack) {
        Integer color = parseHex(dyeHex.getValue());
        if (color != null) SkyJewCustom.setDye(stack, color);
        rebuild();
    }

    private void applyAnimatedDye(ItemStack stack) {
        Integer a = parseHex(animatedStart.getValue()), b = parseHex(animatedEnd.getValue());
        if (a == null || b == null) return;
        float duration;
        try { duration = Float.parseFloat(animatedDuration.getValue()); }
        catch (Exception e) { duration = 5f; }
        SkyJewCustom.setAnimatedDye(stack, a, b, duration, cycleBack.selected(), 0);
        rebuild();
    }

    private static Integer parseHex(String value) {
        if (value == null) return null;
        String s = value.trim();
        if (s.startsWith("#")) s = s.substring(1);
        return s.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(s, 16) : null;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF080A0F);
        int w = 660, h = Math.min(455, height - 60), left = (width - w) / 2, top = Math.max(12, (height - h) / 2);
        g.fill(left, top, left + w, top + h, 0xFF202328);
        g.fill(left, top, left + w, top + 2, 0xFF55FFFF);
        g.text(font, Component.literal("SkyJew Customization"), left + 16, top + 9, 0xFFFFFFFF, true);
        g.text(font, Component.literal("Standalone port of Skyblocker's Armor + Item customization workflow"),
            left + 16, top + 46, 0xFFB5BAC1, false);

        ItemStack stack = selectedStack();
        if (!stack.isEmpty()) {
            g.item(stack, left + 66, top + 110);
            g.itemDecorations(font, stack, left + 66, top + 110);
            g.text(font, stack.getHoverName(), left + 18, top + 142, 0xFFFFFFFF, false);
            if (SkyJewCustom.getDye(stack) != null)
                g.text(font, "Dye: #" + String.format(Locale.ROOT, "%06X", SkyJewCustom.getDye(stack)),
                    left + 18, top + 158, 0xFF55FFFF, false);
            if (SkyJewCustom.getGlint(stack) != null)
                g.text(font, "Glint override: " + SkyJewCustom.getGlint(stack), left + 18, top + 174, 0xFFAA55FF, false);
        } else {
            g.text(font, "Select an armor piece or item to begin.", left + 18, top + 110, 0xFFAAAAAA, false);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}

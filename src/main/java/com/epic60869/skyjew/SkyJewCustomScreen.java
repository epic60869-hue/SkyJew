package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class SkyJewCustomScreen extends Screen {
    private final Screen parent;
    private int tab;
    private int selectedArmor;
    private EditBox itemName, dyeHex, trimMaterial, trimPattern, animatedStart, animatedEnd, animatedDuration;
    private boolean cycleBack;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public SkyJewCustomScreen(Screen parent) { super(Component.literal("SkyJew Customization")); this.parent = parent; }

    @Override protected void init() { rebuild(); }

    private void rebuild() {
        clearWidgets();
        int left = width / 2 - 270, top = height / 2 - 170;
        addRenderableWidget(Button.builder(Component.literal("Armor"), b -> { tab = 0; rebuild(); }).bounds(left, top + 28, 120, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Item"), b -> { tab = 1; rebuild(); }).bounds(left + 125, top + 28, 120, 22).build());
        if (tab == 0) buildArmor(left, top); else buildItem(left, top);
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width / 2 - 45, height / 2 + 145, 90, 24).build());
    }

    private void buildArmor(int left, int top) {
        ItemStack selected = selectedStack();
        int y = top + 62;
        for (int i = 0; i < ARMOR.length; i++) {
            final int slot = i;
            String label = switch (i) { case 0 -> "Helmet"; case 1 -> "Chestplate"; case 2 -> "Leggings"; default -> "Boots"; };
            addRenderableWidget(Button.builder(Component.literal((selectedArmor == i ? "> " : "") + label),
                b -> { selectedArmor = slot; rebuild(); }).bounds(left, y + i * 25, 115, 22).build());
        }
        int x = left + 135;
        addRenderableWidget(Button.builder(Component.literal("Reset customization"), b -> {
            SkyJewCustom.setName(selected, null); SkyJewCustom.setDye(selected, null);
            SkyJewCustom.setTrim(selected, null, null); SkyJewCustom.setAnimatedDye(selected, null, null, 1, false, 0); rebuild();
        }).bounds(x, y, 190, 22).build());

        dyeHex = box("Dye #RRGGBB", x, y + 30, 190, selectedDye(selected)); addRenderableWidget(dyeHex);
        addRenderableWidget(Button.builder(Component.literal("Apply Dye"), b -> applyDye(selected)).bounds(x + 195, y + 30, 95, 22).build());

        trimMaterial = box("Trim material", x, y + 60, 190, ""); trimPattern = box("Trim pattern", x, y + 90, 190, "");
        addRenderableWidget(trimMaterial); addRenderableWidget(trimPattern);
        addRenderableWidget(Button.builder(Component.literal("Apply Trim"), b -> SkyJewCustom.setTrim(selected, trimMaterial.getValue(), trimPattern.getValue()))
            .bounds(x + 195, y + 75, 95, 22).build());

        animatedStart = box("Animated start", x, y + 125, 140, ""); animatedEnd = box("Animated end", x, y + 155, 140, "");
        animatedDuration = box("Seconds", x + 145, y + 125, 80, "5");
        addRenderableWidget(animatedStart); addRenderableWidget(animatedEnd); addRenderableWidget(animatedDuration);
        cycleBack = Checkbox.builder(Component.literal("Loop back"), font).pos(x + 145, y + 155).selected(true).build();
        addRenderableWidget(cycleBack);
        addRenderableWidget(Button.builder(Component.literal("Apply Animated Dye"), b -> applyAnimatedDye(selected))
            .bounds(x + 225, y + 125, 135, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Clear Dye"), b -> {
            SkyJewCustom.setDye(selected, null); SkyJewCustom.setAnimatedDye(selected, null, null, 1, false, 0);
        }).bounds(x + 225, y + 155, 135, 22).build());
    }

    private void buildItem(int left, int top) {
        ItemStack selected = Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getMainHandItem();
        int x = left + 135, y = top + 65;
        itemName = box("Custom item name", x, y, 260, SkyJewCustom.getName(selected) == null ? "" : SkyJewCustom.getName(selected));
        addRenderableWidget(itemName);
        addRenderableWidget(Button.builder(Component.literal("Apply name"), b -> { SkyJewCustom.setName(selected, itemName.getValue()); rebuild(); })
            .bounds(x, y + 30, 120, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Reset name"), b -> { SkyJewCustom.setName(selected, null); rebuild(); })
            .bounds(x + 125, y + 30, 120, 22).build());
    }

    private EditBox box(String hint, int x, int y, int w, String value) {
        EditBox box = new EditBox(font, x, y, w, 22, Component.literal(hint));
        box.setHint(Component.literal(hint)); box.setValue(value == null ? "" : value); box.setMaxLength(128); return box;
    }

    private ItemStack selectedStack() {
        Minecraft mc = Minecraft.getInstance(); return mc.player == null ? ItemStack.EMPTY : mc.player.getItemBySlot(ARMOR[selectedArmor]);
    }

    private String selectedDye(ItemStack stack) {
        Integer color = SkyJewCustom.getDye(stack); return color == null ? "" : String.format(Locale.ROOT, "#%06X", color);
    }

    private void applyDye(ItemStack stack) { Integer color = parseHex(dyeHex.getValue()); if (color != null) SkyJewCustom.setDye(stack, color); }

    private void applyAnimatedDye(ItemStack stack) {
        Integer a = parseHex(animatedStart.getValue()), b = parseHex(animatedEnd.getValue()); if (a == null || b == null) return;
        float duration; try { duration = Float.parseFloat(animatedDuration.getValue()); } catch (Exception e) { duration = 5f; }
        SkyJewCustom.setAnimatedDye(stack, a, b, duration, cycleBack, 0);
    }

    private static Integer parseHex(String value) {
        if (value == null) return null; String s = value.trim(); if (s.startsWith("#")) s = s.substring(1);
        return s.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(s, 16) : null;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF090B0F);
        int w = 540, h = 350, left = (width - w) / 2, top = (height - h) / 2;
        g.fill(left, top, left + w, top + h, 0xFF171A20); g.fill(left, top, left + w, top + 2, 0xFF55FFFF);
        g.text(font, Component.literal("SkyJew Customization"), left + 16, top + 10, 0xFFFFFFFF, true);
        g.text(font, Component.literal("Standalone Skyblocker-style custom item editor"), left + 16, top + 50, 0xFF9EA5B2, false);
        ItemStack stack = tab == 0 ? selectedStack() : (Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getMainHandItem());
        if (!stack.isEmpty()) { g.item(stack, left + 24, top + 88); g.text(font, stack.getHoverName(), left + 16, top + 125, 0xFFFFFFFF, false); }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}

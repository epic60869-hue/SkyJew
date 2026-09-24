package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Standalone port of the Skyblocker /skyblocker custom workflow.
 *
 * The screen intentionally follows Skyblocker's structure: Armor/Item tabs,
 * a live player/item preview, armor-piece selector, trim/dye/animated-dye
 * controls, and item rename/glint/model controls. It does not depend on the
 * Skyblocker jar at runtime.
 */
public final class SkyJewCustomScreen extends Screen {
    private final Screen parent;
    private int tab;
    private int selectedArmor;

    private EditBox dyeHex;
    private EditBox trimMaterial;
    private EditBox trimPattern;
    private EditBox animatedStart;
    private EditBox animatedEnd;
    private EditBox animatedDuration;
    private EditBox itemName;
    private EditBox itemModel;
    private EditBox armorModel;
    private EditBox headTexture;
    private Checkbox cycleBack;

    private static final EquipmentSlot[] ARMOR = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public SkyJewCustomScreen(Screen parent) {
        super(Component.literal("SkyJew Customization"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();

        int w = Math.min(760, width - 24);
        int h = Math.min(430, height - 24);
        int left = (width - w) / 2;
        int top = (height - h) / 2;

        addRenderableWidget(Button.builder(Component.literal("Armor"), b -> {
            tab = 0;
            rebuild();
        }).bounds(left, top, 110, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Item"), b -> {
            tab = 1;
            rebuild();
        }).bounds(left + 115, top, 110, 24).build());

        if (tab == 0) {
            buildArmor(left, top);
        } else {
            buildItem(left, top);
        }

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
            .bounds(width / 2 - 92, top + h - 28, 85, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(width / 2 + 7, top + h - 28, 85, 22).build());
    }

    private void buildArmor(int left, int top) {
        ItemStack selected = selectedStack();
        int x = left + 150;
        int y = top + 55;

        // Skyblocker's armor piece selector is a mini-hotbar. The four buttons
        // below provide the same interaction while keeping the native 26.2 UI.
        for (int i = 0; i < ARMOR.length; i++) {
            final int slot = i;
            addRenderableWidget(Button.builder(Component.literal(armorLabel(i)), b -> {
                selectedArmor = slot;
                rebuild();
            }).bounds(left + 10, top + 238 + i * 25, 125, 22).build());
        }

        dyeHex = addBox("Dye #RRGGBB", x, y, 190, selectedDye(selected));
        addRenderableWidget(Button.builder(Component.literal("Apply Dye"), b -> applyDye(selected))
            .bounds(x + 195, y, 95, 22).build());

        trimMaterial = addBox("Trim material", x, y + 32, 190, currentTrimMaterial(selected));
        trimPattern = addBox("Trim pattern", x, y + 58, 190, currentTrimPattern(selected));
        addRenderableWidget(Button.builder(Component.literal("Apply Trim"), b ->
            SkyJewCustom.setTrim(selected, trimMaterial.getValue(), trimPattern.getValue()))
            .bounds(x + 195, y + 45, 105, 22).build());

        headTexture = addBox("Head texture / value", x, y + 92, 295, "");
        armorModel = addBox("Armor model identifier", x, y + 120, 295, "");

        animatedStart = addBox("Animated start", x, y + 155, 130, "");
        animatedEnd = addBox("Animated end", x, y + 181, 130, "");
        animatedDuration = addBox("Duration seconds", x + 135, y + 155, 100, "5");

        cycleBack = Checkbox.builder(Component.literal("Cycle back"), font)
            .pos(x + 135, y + 181)
            .selected(true)
            .build();
        addRenderableWidget(cycleBack);

        addRenderableWidget(Button.builder(Component.literal("Apply Animated Dye"), b ->
            applyAnimatedDye(selected))
            .bounds(x + 240, y + 155, 135, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Reset Armor"), b -> {
            SkyJewCustom.setName(selected, null);
            SkyJewCustom.setDye(selected, null);
            SkyJewCustom.setTrim(selected, null, null);
            SkyJewCustom.setAnimatedDye(selected, null, null, 1, false, 0);
            rebuild();
        }).bounds(x, y + 214, 120, 22).build());
    }

    private void buildItem(int left, int top) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack selected = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        int x = left + 170;
        int y = top + 65;

        itemName = addBox("Custom item name", x, y, 270,
            SkyJewCustom.getName(selected) == null ? "" : SkyJewCustom.getName(selected));

        addRenderableWidget(Button.builder(Component.literal("Apply Name"), b -> {
            SkyJewCustom.setName(selected, itemName.getValue());
            rebuild();
        }).bounds(x, y + 28, 125, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Reset Name"), b -> {
            SkyJewCustom.setName(selected, null);
            rebuild();
        }).bounds(x + 130, y + 28, 125, 22).build());

        itemModel = addBox("Item model identifier", x, y + 65, 270, "");
        addRenderableWidget(Button.builder(Component.literal("Apply Model"), b ->
            SkyJewCustom.setItemModel(selected, itemModel.getValue()))
            .bounds(x, y + 93, 120, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Reset Model"), b -> {
            SkyJewCustom.setItemModel(selected, null);
            rebuild();
        }).bounds(x + 125, y + 93, 120, 22).build());
    }

    private EditBox addBox(String hint, int x, int y, int width, String value) {
        EditBox box = new EditBox(font, x, y, width, 22, Component.literal(hint));
        box.setHint(Component.literal(hint));
        box.setValue(value == null ? "" : value);
        box.setMaxLength(256);
        addRenderableWidget(box);
        return box;
    }

    private ItemStack selectedStack() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? ItemStack.EMPTY : mc.player.getItemBySlot(ARMOR[selectedArmor]);
    }

    private static String armorLabel(int slot) {
        return switch (slot) {
            case 0 -> "Helmet";
            case 1 -> "Chestplate";
            case 2 -> "Leggings";
            default -> "Boots";
        };
    }

    private String selectedDye(ItemStack stack) {
        Integer color = SkyJewCustom.getDye(stack);
        return color == null ? "" : String.format(Locale.ROOT, "#%06X", color);
    }

    private String currentTrimMaterial(ItemStack stack) {
        SkyJewCustom.TrimId id = SkyJewCustom.getTrim(stack);
        return id == null ? "" : id.material();
    }

    private String currentTrimPattern(ItemStack stack) {
        SkyJewCustom.TrimId id = SkyJewCustom.getTrim(stack);
        return id == null ? "" : id.pattern();
    }

    private void applyDye(ItemStack stack) {
        Integer color = parseHex(dyeHex.getValue());
        if (color != null) SkyJewCustom.setDye(stack, color);
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

        SkyJewCustom.setAnimatedDye(stack, a, b, duration, cycleBack.selected(), 0);
    }

    private static Integer parseHex(String value) {
        if (value == null) return null;
        String s = value.trim();
        if (s.startsWith("#")) s = s.substring(1);
        return s.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(s, 16) : null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xAA000000);

        int w = Math.min(760, width - 24);
        int h = Math.min(430, height - 24);
        int left = (width - w) / 2;
        int top = (height - h) / 2;

        g.fill(left, top, left + w, top + h, 0xFF20242B);
        g.fill(left, top, left + w, top + 2, 0xFF55FFFF);

        g.text(font, title, left + 12, top + 7, 0xFFFFFFFF, true);
        g.text(font, Component.literal(
            tab == 0 ? "Armor customization" : "Item customization"),
            left + 245, top + 8, 0xFFAAAAAA, false);

        Minecraft mc = Minecraft.getInstance();

        if (tab == 0) {
            ItemStack selected = selectedStack();

            // Live player preview, matching the main visual element of
            // Skyblocker's ArmorTab.
            if (mc.player != null) {
                try {
                    InventoryScreen.extractEntityInInventoryFollowsMouse(
                        g,
                        left + 8, top + 42,
                        left + 132, top + 220,
                        72, 0,
                        mouseX, mouseY,
                        mc.player
                    );
                } catch (Throwable ignored) {
                    // A third-party renderer may reject the preview entity;
                    // the rest of the customization UI remains usable.
                }
            }

            for (int i = 0; i < ARMOR.length; i++) {
                ItemStack armor = mc.player == null
                    ? ItemStack.EMPTY
                    : mc.player.getItemBySlot(ARMOR[i]);
                int sx = left + 10;
                int sy = top + 238 + i * 25;
                g.fill(sx, sy, sx + 125, sy + 22,
                    i == selectedArmor ? 0x6655FFFF : 0x55333333);
                g.item(armor, sx + 3, sy + 3);
                g.text(font, Component.literal(armorLabel(i)), sx + 25, sy + 7,
                    0xFFFFFFFF, false);
            }

            g.text(font,
                selected.isEmpty()
                    ? Component.literal("No item in this armor slot")
                    : selected.getHoverName(),
                left + 150, top + 39, 0xFFFFFFFF, false);
            g.text(font, Component.literal(
                "Trim • Dye • Animated Dye • Model"),
                left + 150, top + 28, 0xFF777F8A, false);
        } else {
            ItemStack stack = mc.player == null
                ? ItemStack.EMPTY
                : mc.player.getMainHandItem();

            if (!stack.isEmpty()) {
                g.fill(left + 45, top + 70, left + 125, top + 150, 0x55333333);
                g.item(stack, left + 77, top + 88);
                g.text(font, stack.getHoverName(), left + 35, top + 158,
                    0xFFFFFFFF, false);
            }

            g.text(font, Component.literal(
                "Select an item by holding it in your main hand."),
                left + 170, top + 42, 0xFF777F8A, false);
            g.text(font, Component.literal(
                "Rename • Glint • Model"),
                left + 170, top + 28, 0xFF777F8A, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}

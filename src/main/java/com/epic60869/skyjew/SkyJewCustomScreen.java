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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SkyJewCustomScreen extends Screen {
    private static final Map<String, Integer> DYES = new LinkedHashMap<>();
    static {
        DYES.put("Black", 0x111111);
        DYES.put("White", 0xF2F2F2);
        DYES.put("Red", 0xFF5555);
        DYES.put("Orange", 0xFFAA00);
        DYES.put("Yellow", 0xFFFF55);
        DYES.put("Lime", 0x55FF55);
        DYES.put("Green", 0x00AA00);
        DYES.put("Cyan", 0x55FFFF);
        DYES.put("Aqua", 0x00AAAA);
        DYES.put("Blue", 0x5555FF);
        DYES.put("Dark Blue", 0x0000AA);
        DYES.put("Purple", 0xAA00AA);
        DYES.put("Pink", 0xFF55FF);
        DYES.put("Brown", 0x8B5A2B);
        DYES.put("Gray", 0xAAAAAA);
        DYES.put("Dark Gray", 0x555555);
        DYES.put("Gold", 0xFFD700);
        DYES.put("Sky", 0x7FDBFF);
        DYES.put("Mint", 0x7CFFC4);
        DYES.put("Coral", 0xFF7F66);
    }

    private static final EquipmentSlot[] ARMOR = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final Screen parent;
    private int tab = 0;
    private int selectedArmor = 0;
    private ItemStack selectedItem = ItemStack.EMPTY;
    private boolean showDyes;

    private EditBox itemName, trimMaterial, trimPattern, animatedStart, animatedEnd, animatedDuration, itemModel;
    private Checkbox cycleBack, glint;

    public SkyJewCustomScreen(Screen parent) {
        super(Component.literal("Customize Your Armour"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();

        int w = Math.min(780, width - 24);
        int left = (width - w) / 2;
        int top = Math.max(10, (height - 460) / 2);

        addRenderableWidget(Button.builder(Component.literal("Armor"), b -> {
            tab = 0;
            showDyes = false;
            rebuild();
        }).bounds(left + 12, top + 36, 100, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Item"), b -> {
            tab = 1;
            showDyes = false;
            rebuild();
        }).bounds(left + 116, top + 36, 100, 24).build());

        if (tab == 0) buildArmor(left, top);
        else buildItem(left, top);

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> minecraft.gui.setScreen(parent))
.bounds(left + w - 215, top + 426, 100, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> minecraft.gui.setScreen(parent))
.bounds(left + w - 110, top + 426, 100, 24).build());
    }

    private void buildArmor(int left, int top) {
        int panelY = top + 76;
        int sideW = 160;
        int x = left + sideW + 30;

        addRenderableWidget(Button.builder(Component.literal(selectedArmor == 0 ? "▶ Helmet" : "Helmet"),
            b -> { selectedArmor = 0; selectedItem = ItemStack.EMPTY; rebuild(); })
            .bounds(left + 18, panelY, sideW - 30, 25).build());
        addRenderableWidget(Button.builder(Component.literal(selectedArmor == 1 ? "▶ Chestplate" : "Chestplate"),
            b -> { selectedArmor = 1; selectedItem = ItemStack.EMPTY; rebuild(); })
            .bounds(left + 18, panelY + 31, sideW - 30, 25).build());
        addRenderableWidget(Button.builder(Component.literal(selectedArmor == 2 ? "▶ Leggings" : "Leggings"),
            b -> { selectedArmor = 2; selectedItem = ItemStack.EMPTY; rebuild(); })
            .bounds(left + 18, panelY + 62, sideW - 30, 25).build());
        addRenderableWidget(Button.builder(Component.literal(selectedArmor == 3 ? "▶ Boots" : "Boots"),
            b -> { selectedArmor = 3; selectedItem = ItemStack.EMPTY; rebuild(); })
            .bounds(left + 18, panelY + 93, sideW - 30, 25).build());

        addRenderableWidget(Button.builder(Component.literal("Select item"),
            b -> minecraft.gui.setScreen(new SkyJewItemSelectScreen(this, stack -> {
                selectedItem = stack.copy();
                rebuild();
            }))).bounds(left + 18, panelY + 135, sideW - 30, 25).build());

        ItemStack selected = selectedStack();
        if (selected.isEmpty()) return;

        addRenderableWidget(Button.builder(Component.literal("Pick Dye"),
            b -> minecraft.gui.setScreen(new SkyJewDyeSelectScreen(this, selected)))
            .bounds(x, panelY, 130, 25).build());
        addRenderableWidget(Button.builder(Component.literal("Reset colour"),
            b -> { SkyJewCustom.setDye(selected, null); rebuild(); })
            .bounds(x + 138, panelY, 130, 25).build());

        SkyJewCustom.TrimId trim = SkyJewCustom.getTrim(selected);
        int trimY = panelY + 52;
        trimMaterial = box("Trim material", x, trimY, 180, trim == null ? "" : trim.material());
        trimPattern = box("Trim pattern", x + 190, trimY, 180, trim == null ? "" : trim.pattern());
        addRenderableWidget(trimMaterial);
        addRenderableWidget(trimPattern);
        addRenderableWidget(Button.builder(Component.literal("Apply Trim"),
            b -> { SkyJewCustom.setTrim(selected, trimMaterial.getValue(), trimPattern.getValue()); rebuild(); })
            .bounds(x + 380, trimY, 105, 22).build());

        int animY = trimY + 42;
        animatedStart = box("Start #RRGGBB", x, animY, 135, "");
        animatedEnd = box("End #RRGGBB", x + 143, animY, 135, "");
        animatedDuration = box("Seconds", x + 286, animY, 85, "5");
        cycleBack = Checkbox.builder(Component.literal("Cycle"), font)
            .pos(x + 379, animY + 2).selected(true).build();
        addRenderableWidget(animatedStart);
        addRenderableWidget(animatedEnd);
        addRenderableWidget(animatedDuration);
        addRenderableWidget(cycleBack);

        addRenderableWidget(Button.builder(Component.literal("Apply animated dye"),
            b -> applyAnimatedDye(selected)).bounds(x, animY + 30, 160, 25).build());
        addRenderableWidget(Button.builder(Component.literal("Pick Hypixel animated dye"),
            b -> minecraft.gui.setScreen(new SkyJewDyeSelectScreen(this, selected)))
            .bounds(x + 168, animY + 30, 200, 25).build());
    }
    private void buildDyePalette(int x, int y, ItemStack selected) {
        int col = 0, row = 0;
        for (Map.Entry<String, Integer> entry : DYES.entrySet()) {
            final int color = entry.getValue();
            int bx = x + col * 62;
            int by = y + row * 30;
            addRenderableWidget(Button.builder(Component.literal(""),
                b -> { SkyJewCustom.setDye(selected, color); showDyes = false; rebuild(); })
                .bounds(bx, by, 56, 25).build());

            col++;
            if (col == 5) {
                col = 0;
                row++;
            }
        }
    }

    private void buildItem(int left, int top) {
        int y = top + 76;
        int x = left + 205;

        addRenderableWidget(Button.builder(Component.literal("Select item"),
            b -> minecraft.gui.setScreen(new SkyJewItemSelectScreen(this, stack -> {
                selectedItem = stack.copy();
                rebuild();
            }))).bounds(left + 18, y, 145, 25).build());

        ItemStack selected = selectedItem.isEmpty()
            ? (Minecraft.getInstance().player == null ? ItemStack.EMPTY
                : Minecraft.getInstance().player.getMainHandItem())
            : selectedItem;

        if (selected.isEmpty()) return;

        itemName = box("Custom item name", x, y, 300,
            SkyJewCustom.getName(selected) == null ? "" : SkyJewCustom.getName(selected));
        addRenderableWidget(itemName);
        addRenderableWidget(Button.builder(Component.literal("Apply"),
            b -> { SkyJewCustom.setName(selected, itemName.getValue()); rebuild(); })
            .bounds(x + 308, y, 70, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"),
            b -> { SkyJewCustom.setName(selected, null); rebuild(); })
            .bounds(x + 384, y, 70, 22).build());

        Boolean currentGlint = SkyJewCustom.getGlint(selected);
        glint = Checkbox.builder(Component.literal("Override enchant glint"), font)
            .pos(x, y + 36).selected(currentGlint == null || currentGlint).build();
        addRenderableWidget(glint);
        addRenderableWidget(Button.builder(Component.literal("Apply glint"),
            b -> { SkyJewCustom.setGlint(selected, glint.selected()); rebuild(); })
            .bounds(x + 190, y + 34, 105, 24).build());

        itemModel = box("Item model identifier", x, y + 74, 300,
            SkyJewCustom.getItemModel(selected) == null ? "" : SkyJewCustom.getItemModel(selected));
        addRenderableWidget(itemModel);
        addRenderableWidget(Button.builder(Component.literal("Apply"),
            b -> { SkyJewCustom.setItemModel(selected, itemModel.getValue()); rebuild(); })
            .bounds(x + 308, y + 74, 70, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Select Model"),
            b -> minecraft.gui.setScreen(new SkyJewItemIconSelectScreen(this, identifier -> {
                SkyJewCustom.setItemModel(selected, identifier.toString());
                rebuild();
            }))).bounds(x, y + 110, 145, 25).build());

        addRenderableWidget(Button.builder(Component.literal("Reset Model"),
            b -> { SkyJewCustom.setItemModel(selected, null); rebuild(); })
            .bounds(x + 153, y + 110, 145, 25).build());
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
        try { duration = Float.parseFloat(animatedDuration.getValue()); }
        catch (Exception ignored) { duration = 5f; }

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
        int w = Math.min(780, width - 24);
        int h = Math.min(460, height - 20);
        int left = (width - w) / 2;
        int top = (height - h) / 2;

        g.fill(0, 0, width, height, 0xFF07090D);
        g.fill(left, top, left + w, top + h, 0xFF202328);
        g.fill(left, top, left + w, top + 2, 0xFF55FFFF);

        g.text(font, Component.literal("Customize Your Armour"), left + 14, top + 12, 0xFFDDDDDD, true);
        g.text(font, Component.literal("Armor"), left + 25, top + 52, tab == 0 ? 0xFFFFFFFF : 0xFF777777, true);
        g.text(font, Component.literal("Item"), left + 129, top + 52, tab == 1 ? 0xFFFFFFFF : 0xFF777777, true);

        ItemStack selected = selectedStack();
        if (!selected.isEmpty()) {
            g.fill(left + 150, top + 75, left + 175, top + 100, 0x55333333);
            g.item(selected, left + 154, top + 79);
            g.itemDecorations(font, selected, left + 154, top + 79);
            g.text(font, selected.getHoverName(), left + 188, top + 84, 0xFFFFFFFF, false);

            Integer dye = SkyJewCustom.getDye(selected);
            if (dye != null) {
                g.text(font, Component.literal("Dye"), left + 18, top + 235, 0xFFBBBBBB, false);
                g.fill(left + 55, top + 232, left + 75, top + 244, 0xFF000000 | dye);
            }
        } else {
            g.text(font, Component.literal("Nothing customizable"), left + 18, top + 210, 0xFFAAAAAA, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}

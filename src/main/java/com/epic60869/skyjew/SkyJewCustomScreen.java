package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Skyblocker-style customization screen, ported to SkyJew's existing
 * customization backend. The layout follows Skyblocker's Customization
 * screen workflow: top tabs, a selectable equipment/item area on the left,
 * and compact customization controls on the right.
 *
 * Source inspiration: SkyblockerMod/Skyblocker
 * https://github.com/SkyblockerMod/Skyblocker
 * Skyblocker's source is licensed under LGPL-3.0.
 */
public final class SkyJewCustomScreen extends Screen {
    private static final int BG = 0xFF202124;
    private static final int PANEL = 0xFF2B2D31;
    private static final int INNER = 0xFF313338;
    private static final int INNER_DARK = 0xFF1E1F22;
    private static final int BORDER = 0xFF4A4D52;
    private static final int TEXT = 0xFFF2F3F5;
    private static final int MUTED = 0xFFB5BAC1;
    private static final int ACCENT = 0xFFE2E3E5;
    private static final int SELECTED = 0xFF5865F2;
    private static final int RED = 0xFFFF6B6B;
    private static final int GREEN = 0xFF57F287;

    private final Screen previousScreen;
    private boolean itemTab;
    private EquipmentSlot selectedArmor = EquipmentSlot.HEAD;
    private ItemStack selectedItem = ItemStack.EMPTY;
    private boolean showColorPicker;

    private EditBox nameField;
    private EditBox dyeField;
    private EditBox trimMaterialField;
    private EditBox trimPatternField;
    private EditBox animFirstField;
    private EditBox animSecondField;
    private EditBox animDurationField;
    private EditBox animDelayField;

    private boolean cycleBack;

    public SkyJewCustomScreen(Screen previousScreen) {
        super(Component.literal("SkyJew Customization"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        clearWidgets();

        int panelW = Math.min(920, width - 30);
        int panelH = Math.min(540, height - 30);
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;

        addRenderableWidget(Button.builder(Component.literal("Armor"), b -> {
            itemTab = false;
            selectedItem = ItemStack.EMPTY;
            showColorPicker = false;
            init();
        }).bounds(left + 8, top + 8, 120, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Item"), b -> {
            itemTab = true;
            showColorPicker = false;
            init();
        }).bounds(left + 133, top + 8, 120, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(left + panelW - 88, top + panelH + 5, 88, 24).build());

        if (itemTab) initItemControls(left, top, panelW, panelH);
        else initArmorControls(left, top, panelW, panelH);
    }

    private void initArmorControls(int left, int top, int panelW, int panelH) {
        ItemStack target = currentArmor();
        int x = left + 355;
        int y = top + 55;
        int w = panelW - 375;

        nameField = field(x, y + 28, w - 10, SkyJewCustom.getName(target));
        addLabel(x, y, "ITEM NAME");

        dyeField = field(x, y + 103, 105, colorText(SkyJewCustom.getDye(target)));
        addLabel(x, y + 75, "DYE COLOUR");
        addRenderableWidget(Button.builder(Component.literal("Pick Colour"), b -> showColorPicker = true)
                .bounds(x + 115, y + 103, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            try {
                SkyJewCustom.setDye(target, SkyJewCustom.parseHex(dyeField.getValue()));
                init();
            } catch (Exception ignored) {}
        }).bounds(x + 215, y + 103, 70, 20).build());

        if (target.is(net.minecraft.world.item.Items.PLAYER_HEAD)) {
            addLabel(x, y + 150, "HEAD TEXTURE");
            EditBox head = field(x, y + 178, w - 10, "");
            head.setHint(Component.literal("Texture / profile support"));
            addRenderableWidget(Button.builder(Component.literal("Clear"), b -> head.setValue(""))
                    .bounds(x + w - 85, y + 178, 75, 20).build());
        } else {
            trimMaterialField = field(x, y + 178, (w - 15) / 2, currentTrimMaterial(target));
            trimPatternField = field(x + (w - 15) / 2 + 15, y + 178, (w - 15) / 2, currentTrimPattern(target));
            addLabel(x, y + 150, "ARMOUR TRIM");
            addRenderableWidget(Button.builder(Component.literal("Apply Trim"), b -> {
                SkyJewCustom.setTrim(target, trimMaterialField.getValue(), trimPatternField.getValue());
                init();
            }).bounds(x, y + 203, 105, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
                SkyJewCustom.setTrim(target, null, null);
                init();
            }).bounds(x + 110, y + 203, 75, 20).build());

            int ay = y + 238;
            animFirstField = field(x, ay + 25, 78, currentAnimColor(target, true));
            animSecondField = field(x + 85, ay + 25, 78, currentAnimColor(target, false));
            animDurationField = field(x + 170, ay + 25, 58, currentAnimDuration(target));
            animDelayField = field(x + 235, ay + 25, 58, currentAnimDelay(target));
            addLabel(x, ay, "ANIMATED DYE");
            addRenderableWidget(Button.builder(Component.literal("Cycle Back: " + (cycleBack ? "ON" : "OFF")), b -> {
                cycleBack = !cycleBack;
                b.setMessage(Component.literal("Cycle Back: " + (cycleBack ? "ON" : "OFF")));
            }).bounds(x, ay + 52, 115, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Apply Animated"), b -> {
                try {
                    SkyJewCustom.setAnimatedDye(target,
                            SkyJewCustom.parseHex(animFirstField.getValue()),
                            SkyJewCustom.parseHex(animSecondField.getValue()),
                            Float.parseFloat(animDurationField.getValue()),
                            cycleBack,
                            Float.parseFloat(animDelayField.getValue()));
                    init();
                } catch (Exception ignored) {}
            }).bounds(x + 120, ay + 52, 125, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
                SkyJewCustom.setAnimatedDye(target, null, null, 1, false, 0);
                init();
            }).bounds(x + 250, ay + 52, 70, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Apply Name"), b -> {
            SkyJewCustom.setName(target, nameField.getValue());
            init();
        }).bounds(x + w - 185, y + 53, 85, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear Name"), b -> {
            SkyJewCustom.setName(target, null);
            init();
        }).bounds(x + w - 95, y + 53, 85, 20).build());

        addRenderableWidget(Button.builder(Component.literal("RESET ALL"), b -> {
            SkyJewCustom.clearAll(target);
            init();
        }).bounds(x, top + panelH - 48, 110, 24).build());
    }

    private void initItemControls(int left, int top, int panelW, int panelH) {
        if (selectedItem.isEmpty()) selectedItem = findFirstCustomizableItem();

        int x = left + 355;
        int y = top + 55;
        int w = panelW - 375;

        addRenderableWidget(Button.builder(Component.literal("Select Item"), b ->
                Minecraft.getInstance().gui.setScreen(new SkyJewItemSelectScreen(this, stack -> {
                    selectedItem = stack;
                    init();
                }))
        ).bounds(x, y, 105, 22).build());

        ItemStack target = selectedItem;
        nameField = field(x, y + 58, w - 10, SkyJewCustom.getName(target));
        addLabel(x, y + 32, "ITEM NAME");
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            SkyJewCustom.setName(target, nameField.getValue());
            init();
        }).bounds(x + w - 185, y + 58, 85, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
            SkyJewCustom.setName(target, null);
            init();
        }).bounds(x + w - 95, y + 58, 85, 20).build());

        dyeField = field(x, y + 113, 105, colorText(SkyJewCustom.getDye(target)));
        addLabel(x, y + 86, "ITEM COLOUR");
        addRenderableWidget(Button.builder(Component.literal("Pick Colour"), b -> showColorPicker = true)
                .bounds(x + 115, y + 113, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            try {
                SkyJewCustom.setDye(target, SkyJewCustom.parseHex(dyeField.getValue()));
                init();
            } catch (Exception ignored) {}
        }).bounds(x + 215, y + 113, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
            SkyJewCustom.setDye(target, null);
            init();
        }).bounds(x + 290, y + 113, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("RESET ALL CUSTOMIZATION"), b -> {
            SkyJewCustom.clearAll(target);
            init();
        }).bounds(x, y + 165, Math.min(230, w), 24).build());
    }

    private EditBox field(int x, int y, int width, String value) {
        EditBox box = new EditBox(font, x, y, Math.max(60, width), 20, Component.empty());
        box.setValue(value == null ? "" : value);
        box.setMaxLength(1000);
        addRenderableWidget(box);
        return box;
    }

    private void addLabel(int x, int y, String text) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> {})
                .bounds(x, y, 1, 1).build());
    }

    private ItemStack currentArmor() {
        return Minecraft.getInstance().player == null ? ItemStack.EMPTY
                : Minecraft.getInstance().player.getItemBySlot(selectedArmor);
    }

    private ItemStack findFirstCustomizableItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (SkyJewCustom.hasUuid(stack)) return stack;
        }
        for (ItemStack stack : mc.player.getInventory()) {
            if (SkyJewCustom.hasUuid(stack)) return stack;
        }
        return mc.player.getMainHandItem();
    }

    private static String colorText(Integer color) {
        return color == null ? "" : String.format(Locale.ROOT, "%06X", color & 0xFFFFFF);
    }

    private static String currentTrimMaterial(ItemStack stack) {
        SkyJewCustom.TrimId id = SkyJewCustom.getTrim(stack);
        return id == null ? "" : id.material();
    }

    private static String currentTrimPattern(ItemStack stack) {
        SkyJewCustom.TrimId id = SkyJewCustom.getTrim(stack);
        return id == null ? "" : id.pattern();
    }

    private static String currentAnimColor(ItemStack stack, boolean first) {
        SkyJewCustom.AnimatedDye d = SkyJewCustom.getAnimatedDye(stack);
        if (d == null) return "";
        return String.format(Locale.ROOT, "%06X", (first ? d.first().color() : d.second().color()) & 0xFFFFFF);
    }

    private static String currentAnimDuration(ItemStack stack) {
        SkyJewCustom.AnimatedDye d = SkyJewCustom.getAnimatedDye(stack);
        return d == null ? "2" : Float.toString(d.duration());
    }

    private static String currentAnimDelay(ItemStack stack) {
        SkyJewCustom.AnimatedDye d = SkyJewCustom.getAnimatedDye(stack);
        return d == null ? "0" : Float.toString(d.delay());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);

        int panelW = Math.min(920, width - 30);
        int panelH = Math.min(540, height - 30);
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;
        int right = left + panelW;
        int bottom = top + panelH;

        g.fill(left, top, right, bottom, PANEL);
        outline(g, left, top, right, bottom, BORDER);

        g.fill(left + 8, top + 8, left + 253, top + 32, INNER);
        g.fill(left + (itemTab ? 133 : 8), top + 8, left + (itemTab ? 253 : 128), top + 32, SELECTED);
        g.text(font, "SkyJew Customization", left + 265, top + 15, TEXT, true);

        int previewL = left + 15;
        int previewT = top + 48;
        int previewR = left + 340;
        int previewB = bottom - 15;
        g.fill(previewL, previewT, previewR, previewB, INNER);
        outline(g, previewL, previewT, previewR, previewB, BORDER);

        if (itemTab) drawItemPreview(g, previewL, previewT, previewR, previewB, selectedItem);
        else drawArmorPreview(g, previewL, previewT, previewR, previewB);

        int controlsL = left + 355;
        int controlsR = right - 15;
        int controlsT = top + 48;
        g.fill(controlsL, controlsT, controlsR, bottom - 15, INNER_DARK);
        outline(g, controlsL, controlsT, controlsR, bottom - 15, BORDER);
        g.text(font, itemTab ? "Item Customization" : "Armor Customization",
                controlsL + 12, controlsT + 12, ACCENT, true);

        super.extractRenderState(g, mouseX, mouseY, delta);
        if (showColorPicker) drawColorPicker(g);
    }

    private void drawArmorPreview(GuiGraphicsExtractor g, int l, int t, int r, int b) {
        g.text(font, "Armor", l + 12, t + 12, TEXT, true);
        g.text(font, "Select a piece below", l + 12, t + 29, MUTED, false);

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] labels = {"HEAD", "CHEST", "LEGS", "FEET"};

        for (int i = 0; i < slots.length; i++) {
            int x = l + 22 + i * 72;
            int y = t + 58;
            ItemStack stack = Minecraft.getInstance().player == null ? ItemStack.EMPTY
                    : Minecraft.getInstance().player.getItemBySlot(slots[i]);
            boolean selected = selectedArmor == slots[i];
            g.fill(x, y, x + 62, y + 62, selected ? SELECTED : PANEL);
            outline(g, x, y, x + 62, y + 62, selected ? ACCENT : BORDER);
            if (!stack.isEmpty()) g.item(stack, x + 23, y + 8);
            g.text(font, labels[i], x + 8, y + 42, MUTED, false);
        }

        ItemStack selected = currentArmor();
        g.text(font, selected.isEmpty() ? "No armor equipped" : selected.getHoverName().getString(),
                l + 15, t + 138, selected.isEmpty() ? RED : GREEN, false);
        if (!selected.isEmpty()) {
            g.item(selected, l + 143, t + 168);
            g.text(font, "UUID: " + trim(SkyJewCustom.uuid(selected), 30), l + 15, t + 215, MUTED, false);
        }
    }

    private void drawItemPreview(GuiGraphicsExtractor g, int l, int t, int r, int b, ItemStack stack) {
        g.text(font, "Item", l + 12, t + 12, TEXT, true);
        g.text(font, "Choose an item from your inventory", l + 12, t + 29, MUTED, false);
        g.fill(l + 100, t + 65, r - 100, t + 190, PANEL);
        outline(g, l + 100, t + 65, r - 100, t + 190, BORDER);
        if (!stack.isEmpty()) {
            g.item(stack, (l + r) / 2 - 8, t + 100);
            g.text(font, stack.getHoverName(), (l + r) / 2 - Math.min(80, font.width(stack.getHoverName()) / 2),
                    t + 145, TEXT, false);
            g.text(font, "UUID: " + trim(SkyJewCustom.uuid(stack), 28), l + 20, t + 215, MUTED, false);
        } else {
            g.text(font, "No item selected", l + 115, t + 120, RED, false);
        }
    }

    private void drawColorPicker(GuiGraphicsExtractor g) {
        int w = 300;
        int h = 210;
        int l = (width - w) / 2;
        int t = (height - h) / 2;

        g.fill(0, 0, width, height, 0x99000000);
        g.fill(l, t, l + w, t + h, PANEL);
        outline(g, l, t, l + w, t + h, ACCENT);
        g.text(font, "Select Colour", l + 12, t + 10, TEXT, true);

        int sx = l + 15;
        int sy = t + 38;
        int sw = 210;
        int sh = 125;

        // Saturation/value area with a fixed red hue, plus a hue strip.
        for (int yy = 0; yy < sh; yy++) {
            float v = 1f - yy / (float) (sh - 1);
            for (int xx = 0; xx < sw; xx++) {
                float s = xx / (float) (sw - 1);
                int rgb = hsvToRgb(0f, s, v);
                g.fill(sx + xx, sy + yy, sx + xx + 1, sy + yy + 1, 0xFF000000 | rgb);
            }
        }
        for (int yy = 0; yy < sh; yy++) {
            float hue = yy / (float) (sh - 1) * 360f;
            int rgb = hsvToRgb(hue, 1f, 1f);
            g.fill(sx + sw + 8, sy + yy, sx + sw + 28, sy + yy + 1, 0xFF000000 | rgb);
        }

        g.text(font, "Click the colour area or hue strip.", l + 15, t + 177, MUTED, false);
        g.fill(l + w - 80, t + h - 28, l + w - 15, t + h - 8, INNER);
        g.text(font, "Close", l + w - 66, t + h - 23, TEXT, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (showColorPicker) {
            int w = 300;
            int h = 210;
            int l = (width - w) / 2;
            int t = (height - h) / 2;
            int sx = l + 15;
            int sy = t + 38;
            int sw = 210;
            int sh = 125;

            if (event.button() == 0 && event.x() >= sx && event.x() < sx + sw
                    && event.y() >= sy && event.y() < sy + sh) {
                int rgb = hsvToRgb(0f,
                        (float) (event.x() - sx) / sw,
                        1f - (float) (event.y() - sy) / sh);
                if (dyeField != null) dyeField.setValue(String.format(Locale.ROOT, "%06X", rgb));
                showColorPicker = false;
                return true;
            }

            if (event.button() == 0 && event.x() >= sx + sw + 8 && event.x() < sx + sw + 28
                    && event.y() >= sy && event.y() < sy + sh) {
                float hue = (float) (event.y() - sy) / sh * 360f;
                int rgb = hsvToRgb(hue, 1f, 1f);
                if (dyeField != null) dyeField.setValue(String.format(Locale.ROOT, "%06X", rgb));
                showColorPicker = false;
                return true;
            }

            if (event.button() == 0 && event.x() >= l + w - 80 && event.y() >= t + h - 32) {
                showColorPicker = false;
                return true;
            }
            return true;
        }

        if (!itemTab && event.button() == 0) {
            int panelW = Math.min(920, width - 30);
            int panelH = Math.min(540, height - 30);
            int left = (width - panelW) / 2;
            int top = (height - panelH) / 2;
            int y = top + 106;
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

            for (int i = 0; i < slots.length; i++) {
                int x = left + 37 + i * 72;
                if (event.x() >= x && event.x() < x + 62 && event.y() >= y && event.y() < y + 62) {
                    selectedArmor = slots[i];
                    init();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static int hsvToRgb(float h, float s, float v) {
        h = ((h % 360f) + 360f) % 360f;
        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        return ((int) ((r + m) * 255) << 16)
                | ((int) ((g + m) * 255) << 8)
                | (int) ((b + m) * 255);
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static void outline(GuiGraphicsExtractor g, int l, int t, int r, int b, int c) {
        g.fill(l, t, r, t + 1, c);
        g.fill(l, b - 1, r, b, c);
        g.fill(l, t, l + 1, b, c);
        g.fill(r - 1, t, r, b, c);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(previousScreen);
    }
}

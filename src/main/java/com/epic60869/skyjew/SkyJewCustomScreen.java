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
 * Skyblocker-inspired customization GUI.
 *
 * The layout intentionally follows the same workflow: tabs at the top, an
 * item/armour preview on the left, selectable armour pieces, and live
 * customization controls on the right.
 */
public final class SkyJewCustomScreen extends Screen {
    private static final int BG = 0xFF101216;
    private static final int PANEL = 0xFF191D24;
    private static final int PANEL_2 = 0xFF20252E;
    private static final int PANEL_3 = 0xFF272D37;
    private static final int BORDER = 0xFF3A414D;
    private static final int TEXT = 0xFFF4F4F4;
    private static final int MUTED = 0xFFA7ADB8;
    private static final int ACCENT = 0xFFE7B84B;
    private static final int ACTIVE = 0xFF4B5360;
    private static final int HOVER = 0xFF343B46;
    private static final int GREEN = 0xFF6BE7A0;
    private static final int RED = 0xFFE56A6A;

    private final Screen parent;

    private boolean armorTab = true;
    private EquipmentSlot selectedSlot = EquipmentSlot.HEAD;
    private ItemStack target = ItemStack.EMPTY;
    private boolean customItemSelected;

    private EditBox name;
    private EditBox dye;
    private EditBox trimMaterial;
    private EditBox trimPattern;
    private EditBox animated1;
    private EditBox animated2;
    private EditBox duration;
    private EditBox delay;
    private boolean cycleBack;

    public SkyJewCustomScreen(Screen parent) {
        super(Component.literal("SkyJew Custom"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        refreshTarget();

        int panelWidth = Math.min(900, width - 30);
        int panelHeight = Math.min(520, height - 30);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        // Top tab bar.
        addRenderableWidget(Button.builder(Component.literal("ARMOUR"), b -> {
            armorTab = true;
            selectedSlot = firstEditableArmorSlot();
            init();
        }).bounds(left, top, 130, 28).build());

        addRenderableWidget(Button.builder(Component.literal("ITEM"), b -> {
            armorTab = false;
            init();
        }).bounds(left + 132, top, 130, 28).build());

        int right = left + panelWidth;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(right - 90, top + panelHeight + 5, 90, 24).build());

        int contentTop = top + 42;
        int previewLeft = left;
        int previewWidth = 285;
        int controlsLeft = left + 300;
        int controlsWidth = panelWidth - 315;

        if (armorTab) {
            addArmorFields(controlsLeft, contentTop, controlsWidth);
        } else {
            addItemFields(controlsLeft, contentTop, controlsWidth);
        }

        // Keep the editor comfortably usable at smaller resolutions.
        if (name != null) name.setFocused(true);
    }

    private void refreshTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            target = ItemStack.EMPTY;
            return;
        }
        if (!armorTab) {
            if (customItemSelected && !target.isEmpty()) return;
            if (!customItemSelected) target = mc.player.getMainHandItem();
            return;
        }
        target = mc.player.getItemBySlot(selectedSlot);
    }

    private EquipmentSlot firstEditableArmorSlot() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return EquipmentSlot.HEAD;
        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : slots) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (!stack.isEmpty() && SkyJewCustom.hasUuid(stack)) return slot;
        }
        return EquipmentSlot.HEAD;
    }

    private void addItemFields(int x, int y, int width) {
        int fieldWidth = Math.min(470, width - 20);

        addRenderableWidget(Button.builder(Component.literal("Select Item"), b -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gui.setScreen(new SkyJewItemSelectScreen(this, stack -> {
                target = stack;
                customItemSelected = true;
            }));
        }).bounds(x + fieldWidth - 95, y + 22, 95, 22).build());

        name = field(x, y + 55, fieldWidth, currentName());
        addLabelButton(x, y + 22, "ITEM NAME");
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> applyName())
                .bounds(x + fieldWidth - 190, y + 80, 90, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
            SkyJewCustom.setName(target, null);
            init();
        }).bounds(x + fieldWidth - 95, y + 80, 90, 22).build());

        dye = field(x, y + 140, 180, currentDye());
        addLabelButton(x, y + 107, "ITEM COLOUR");
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> applyDye())
                .bounds(x + 190, y + 140, 90, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
            SkyJewCustom.setDye(target, null);
            init();
        }).bounds(x + 285, y + 140, 90, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Remove ALL CUSTOMIZATION"), b -> {
            SkyJewCustom.clearAll(target);
            init();
        }).bounds(x, y + 195, Math.min(375, fieldWidth), 24).build());
    }

    private void addArmorFields(int x, int y, int width) {
        int half = Math.max(150, Math.min(230, (width - 15) / 2));

        name = field(x, y + 55, Math.min(width, 470), currentName());
        addLabelButton(x, y + 22, "ITEM NAME");
        int nameWidth = Math.min(width, 470);
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> applyName())
                .bounds(x + nameWidth - 190, y + 80, 90, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
            SkyJewCustom.setName(target, null);
            init();
        }).bounds(x + nameWidth - 95, y + 80, 90, 22).build());

        dye = field(x, y + 140, 150, currentDye());
        addLabelButton(x, y + 107, "DYE COLOUR");
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> applyDye())
                .bounds(x + 160, y + 140, 90, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
            SkyJewCustom.setDye(target, null);
            init();
        }).bounds(x + 255, y + 140, 90, 22).build());

        trimMaterial = field(x, y + 225, half, currentTrimMaterial());
        trimPattern = field(x + half + 15, y + 225, half, currentTrimPattern());
        addLabelButton(x, y + 192, "ARMOUR TRIM");
        addRenderableWidget(Button.builder(Component.literal("Apply Trim"), b -> applyTrim())
                .bounds(x, y + 250, 110, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
            SkyJewCustom.setTrim(target, null, null);
            init();
        }).bounds(x + 115, y + 250, 90, 22).build());

        // Animated dye editor.
        int animY = y + 292;
        animated1 = field(x, animY + 32, 125, currentAnimated(0));
        animated2 = field(x + 135, animY + 32, 125, currentAnimated(1));
        duration = field(x + 275, animY + 32, 85, currentDuration());
        delay = field(x + 375, animY + 32, 85, currentDelay());

        addLabelButton(x, animY, "ANIMATED DYE");
        addRenderableWidget(Button.builder(Component.literal("Cycle Back: " + (cycleBack ? "ON" : "OFF")), b -> {
            cycleBack = !cycleBack;
            b.setMessage(Component.literal("Cycle Back: " + (cycleBack ? "ON" : "OFF")));
        }).bounds(x, animY + 62, 125, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Apply Animated Dye"), b -> applyAnimated())
                .bounds(x + 135, animY + 62, 150, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
            SkyJewCustom.setAnimatedDye(target, null, null, 1, false, 0);
            init();
        }).bounds(x + 290, animY + 62, 90, 22).build());

        addRenderableWidget(Button.builder(Component.literal("RESET ALL"), b -> {
            SkyJewCustom.clearAll(target);
            init();
        }).bounds(x, animY + 100, 125, 24).build());
    }

    private void addLabelButton(int x, int y, String label) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {}).bounds(x, y, 1, 1).build());
        // A zero-size-ish button is hidden by the renderer below; the actual
        // section title is drawn in extractRenderState.
    }

    private EditBox field(int x, int y, int w, String value) {
        EditBox e = new EditBox(font, x, y, w, 20, Component.empty());
        e.setValue(value == null ? "" : value);
        e.setMaxLength(1000);
        addRenderableWidget(e);
        return e;
    }

    private String currentName() {
        String v = SkyJewCustom.getName(target);
        return v == null ? "" : v;
    }

    private String currentDye() {
        Integer v = SkyJewCustom.getDye(target);
        return v == null ? "" : String.format(Locale.ROOT, "%06X", v);
    }

    private String currentTrimMaterial() {
        SkyJewCustom.TrimId t = SkyJewCustom.getTrim(target);
        return t == null ? "" : t.material();
    }

    private String currentTrimPattern() {
        SkyJewCustom.TrimId t = SkyJewCustom.getTrim(target);
        return t == null ? "" : t.pattern();
    }

    private String currentAnimated(int which) {
        SkyJewCustom.AnimatedDye d = SkyJewCustom.getAnimatedDye(target);
        if (d == null) return "";
        return String.format(Locale.ROOT, "%06X", which == 0 ? d.first().color() : d.second().color());
    }

    private String currentDuration() {
        SkyJewCustom.AnimatedDye d = SkyJewCustom.getAnimatedDye(target);
        return d == null ? "2" : String.valueOf(d.duration());
    }

    private String currentDelay() {
        SkyJewCustom.AnimatedDye d = SkyJewCustom.getAnimatedDye(target);
        return d == null ? "0" : String.valueOf(d.delay());
    }

    private void applyName() {
        SkyJewCustom.setName(target, name.getValue());
        init();
    }

    private void applyDye() {
        try {
            SkyJewCustom.setDye(target, SkyJewCustom.parseHex(dye.getValue()));
            init();
        } catch (Exception ignored) {}
    }

    private void applyTrim() {
        SkyJewCustom.setTrim(target, trimMaterial.getValue(), trimPattern.getValue());
        init();
    }

    private void applyAnimated() {
        try {
            int a = SkyJewCustom.parseHex(animated1.getValue());
            int b = SkyJewCustom.parseHex(animated2.getValue());
            float d = Float.parseFloat(duration.getValue());
            float wait = Float.parseFloat(delay.getValue());
            SkyJewCustom.setAnimatedDye(target, a, b, d, cycleBack, wait);
            init();
        } catch (Exception ignored) {}
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);

        int panelWidth = Math.min(900, width - 30);
        int panelHeight = Math.min(520, height - 30);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        // Main Skyblocker-style window.
        g.fill(left, top, right, bottom, PANEL);
        outline(g, left, top, right, bottom, BORDER);
        g.fill(left, top, right, top + 2, ACCENT);

        // Tab bar.
        g.fill(left, top, left + 264, top + 28, PANEL_2);
        g.fill(left + (armorTab ? 0 : 132), top, left + (armorTab ? 130 : 262), top + 28, ACTIVE);

        // Content split.
        int contentTop = top + 42;
        int previewRight = left + 285;
        int controlsLeft = left + 300;

        g.fill(left + 10, contentTop, previewRight - 5, bottom - 10, PANEL_2);
        outline(g, left + 10, contentTop, previewRight - 5, bottom - 10, BORDER);

        g.text(font, armorTab ? "Armour Customization" : "Item Customization",
                left + 22, contentTop + 12, TEXT, true);

        g.text(font, target.isEmpty() ? "No item selected" : target.getHoverName().getString(),
                left + 22, contentTop + 32, target.isEmpty() ? MUTED : GREEN, false);

        if (!target.isEmpty()) {
            g.fill(left + 83, contentTop + 55, left + 202, contentTop + 174, 0xFF12151A);
            outline(g, left + 83, contentTop + 55, left + 202, contentTop + 174, BORDER);
            g.item(target, left + 121, contentTop + 75);
            g.text(font, "Selected", left + 117, contentTop + 144, MUTED, false);
            g.text(font, "UUID", left + 23, contentTop + 188, ACCENT, true);
            String uuid = SkyJewCustom.uuid(target);
            g.text(font, uuid.isBlank() ? "No UUID" : trim(uuid, 30),
                    left + 23, contentTop + 205, uuid.isBlank() ? RED : MUTED, false);
        }

        if (armorTab) {
            drawArmorSelector(g, left + 22, bottom - 78, mouseX, mouseY);
            g.text(font, "Select a piece to customize it", left + 22, bottom - 28, MUTED, false);
        } else {
            g.text(font, "Main-hand item", left + 22, bottom - 55, MUTED, false);
            g.text(font, "Hold the item you want to customize before opening this screen.",
                    left + 22, bottom - 38, MUTED, false);
        }

        // Section cards on the right.
        drawSection(g, controlsLeft, contentTop, right - 15, contentTop + 95, "ITEM NAME");
        drawSection(g, controlsLeft, contentTop + 105, right - 15, contentTop + 180, "DYE COLOUR");
        if (armorTab) {
            drawSection(g, controlsLeft, contentTop + 190, right - 15, contentTop + 275, "ARMOUR TRIM");
            drawSection(g, controlsLeft, contentTop + 285, right - 15, bottom - 10, "ANIMATED DYE");
        }

        if (!SkyJewCustom.hasUuid(target)) {
            g.fill(controlsLeft, bottom - 35, right - 15, bottom - 12, 0x663A1E1E);
            g.text(font, "This item has no Hypixel UUID, so customization cannot be saved.",
                    controlsLeft + 8, bottom - 29, RED, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawSection(GuiGraphicsExtractor g, int l, int t, int r, int b, String title) {
        g.fill(l, t, r, b, PANEL_2);
        outline(g, l, t, r, b, BORDER);
        g.text(font, title, l + 10, t + 9, ACCENT, true);
    }

    private void drawArmorSelector(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        String[] labels = {"HEAD", "CHEST", "LEGS", "FEET"};

        for (int i = 0; i < slots.length; i++) {
            int sx = x + i * 57;
            ItemStack stack = Minecraft.getInstance().player == null
                    ? ItemStack.EMPTY
                    : Minecraft.getInstance().player.getItemBySlot(slots[i]);
            boolean selected = selectedSlot == slots[i];
            boolean hover = mouseX >= sx && mouseX < sx + 50 && mouseY >= y && mouseY < y + 50;

            g.fill(sx, y, sx + 50, y + 50, selected ? ACTIVE : (hover ? HOVER : 0xFF171A20));
            outline(g, sx, y, sx + 50, y + 50, selected ? ACCENT : BORDER);
            if (!stack.isEmpty()) g.item(stack, sx + 17, y + 4);
            g.text(font, labels[i], sx + 5, y + 35, MUTED, false);
        }
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static void outline(GuiGraphicsExtractor g, int l, int t, int r, int b, int c) {
        g.fill(l, t, r, t + 1, c);
        g.fill(l, b - 1, r, b, c);
        g.fill(l, t, l + 1, b, c);
        g.fill(r - 1, t, r, b, c);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && armorTab) {
            int panelWidth = Math.min(900, width - 30);
            int panelHeight = Math.min(520, height - 30);
            int left = (width - panelWidth) / 2;
            int top = (height - panelHeight) / 2;
            int bottom = top + panelHeight;
            int x = left + 22;
            int y = bottom - 78;

            EquipmentSlot[] slots = {
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
            };
            for (int i = 0; i < slots.length; i++) {
                int sx = x + i * 57;
                if (event.x() >= sx && event.x() < sx + 50 && event.y() >= y && event.y() < y + 50) {
                    selectedSlot = slots[i];
                    init();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}

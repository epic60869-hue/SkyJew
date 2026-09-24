package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Alternate Skyblocker-inspired customization screen.
 *
 * This is intentionally separate from SkyJewCustomScreen so both UIs can be
 * tested at the same time. It keeps SkyJew's UUID-backed storage and applies
 * the same customization features currently implemented by SkyJew.
 *
 * Skyblocker reference: https://github.com/SkyblockerMod/Skyblocker
 */
public final class SkyJewSkyblockerCustomScreen extends Screen {
    private static final int BG = 0xFF111318;
    private static final int PANEL = 0xFF1B1E24;
    private static final int INNER = 0xFF14171C;
    private static final int BORDER = 0xFF4B4E54;
    private static final int TEXT = 0xFFF2F3F5;
    private static final int MUTED = 0xFFB5BAC1;
    private static final int SELECTED = 0xFF7B61FF;

    private final Screen previousScreen;
    private boolean itemTab;
    private int selectedArmor = 0;
    private ItemStack selectedItem = ItemStack.EMPTY;
    private RemotePlayer previewPlayer;

    private EditBox nameField;
    private EditBox dyeField;
    private EditBox trimMaterial;
    private EditBox trimPattern;
    private EditBox animFirst;
    private EditBox animSecond;
    private EditBox animDuration;
    private EditBox animDelay;
    private boolean cycleBack;

    private final EquipmentSlot[] armorSlots = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public SkyJewSkyblockerCustomScreen(Screen previousScreen) {
        super(Component.literal("SkyJew Customization"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        clearWidgets();
        buildPreview();

        int tabWidth = 150;
        addRenderableWidget(Button.builder(Component.literal("ARMOR"), b -> {
            itemTab = false;
            init();
        }).bounds(width / 2 - tabWidth - 3, 8, tabWidth, 22).build());

        addRenderableWidget(Button.builder(Component.literal("ITEM"), b -> {
            itemTab = true;
            init();
        }).bounds(width / 2 + 3, 8, tabWidth, 22).build());

        if (itemTab) initItemTab();
        else initArmorTab();

        int footerY = height - 30;
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> cancel())
            .bounds(width / 2 - 90, footerY, 85, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(width / 2 + 5, footerY, 85, 22).build());
    }

    private void buildPreview() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        previewPlayer = new RemotePlayer(mc.level, mc.getGameProfile()) {
            @Override public boolean isInvisibleTo(net.minecraft.world.entity.player.Player p) { return true; }
            @Override public int getId() { return -100002; }
        };
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            previewPlayer.setItemSlot(slot, mc.player.getItemBySlot(slot).copy());
        }
    }

    private void initArmorTab() {
        int top = 42;
        int left = 12;
        int sidebarWidth = 116;
        int previewLeft = left + sidebarWidth + 10;
        int previewWidth = 190;
        int controlLeft = previewLeft + previewWidth + 12;
        int controlWidth = width - controlLeft - 12;

        addRenderableWidget(new PlayerPreviewWidget(previewLeft, top + 8, previewWidth, 215, previewPlayer));

        for (int i = 0; i < armorSlots.length; i++) {
            final int index = i;
            addRenderableWidget(new ArmorPieceWidget(left + 10, top + 48 + i * 38, 34, 34, armorSlots[i],
                () -> {
                    selectedArmor = index;
                    init();
                }));
        }

        final ItemStack target = currentArmor();
        drawLabel("APPEARANCE", controlLeft, top + 5);
        drawLabel(target.isEmpty() ? "No customizable armor selected" : target.getHoverName().getString(),
            controlLeft, top + 24);

        int row = top + 43;
        drawLabel("Name", controlLeft, row);
        nameField = field(controlLeft, row + 13, Math.max(120, controlWidth - 150), SkyJewCustom.getName(target));
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            SkyJewCustom.setName(target, nameField.getValue());
            init();
        }).bounds(controlLeft + controlWidth - 72, row + 13, 68, 20).build());

        row += 48;
        if (target.is(Items.PLAYER_HEAD)) {
            drawLabel("Head texture", controlLeft, row);
            drawLabel("Player-head texture selection will be added to this test UI.", controlLeft, row + 16);
        } else {
            drawLabel("Dye color", controlLeft, row);
            dyeField = field(controlLeft, row + 13, 100, colorText(SkyJewCustom.getDye(target)));
            addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
                try { SkyJewCustom.setDye(target, SkyJewCustom.parseHex(dyeField.getValue())); init(); }
                catch (Exception ignored) {}
            }).bounds(controlLeft + 106, row + 13, 68, 20).build());

            row += 47;
            drawLabel("Armor trim", controlLeft, row);
            trimMaterial = field(controlLeft, row + 13, Math.max(90, controlWidth / 2 - 5), trimMaterial(target));
            trimPattern = field(controlLeft + controlWidth / 2 + 3, row + 13,
                Math.max(90, controlWidth / 2 - 3), trimPattern(target));
            addRenderableWidget(Button.builder(Component.literal("Apply trim"), b -> {
                SkyJewCustom.setTrim(target, trimMaterial.getValue(), trimPattern.getValue());
                init();
            }).bounds(controlLeft, row + 37, 100, 20).build());

            row += 65;
            drawLabel("Animated dye", controlLeft, row);
            animFirst = field(controlLeft, row + 13, 72, animColor(target, true));
            animSecond = field(controlLeft + 78, row + 13, 72, animColor(target, false));
            animDuration = field(controlLeft + 156, row + 13, 58, animDuration(target));
            animDelay = field(controlLeft + 218, row + 13, 58, animDelay(target));
            addRenderableWidget(Button.builder(Component.literal("Cycle back: " + (cycleBack ? "ON" : "OFF")), b -> {
                cycleBack = !cycleBack;
                b.setMessage(Component.literal("Cycle back: " + (cycleBack ? "ON" : "OFF")));
            }).bounds(controlLeft, row + 37, 118, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Apply animated"), b -> {
                try {
                    SkyJewCustom.setAnimatedDye(target,
                        SkyJewCustom.parseHex(animFirst.getValue()),
                        SkyJewCustom.parseHex(animSecond.getValue()),
                        Float.parseFloat(animDuration.getValue()),
                        cycleBack,
                        Float.parseFloat(animDelay.getValue()));
                    init();
                } catch (Exception ignored) {}
            }).bounds(controlLeft + 124, row + 37, 125, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Clear customization"), b -> {
            SkyJewCustom.clearAll(target);
            init();
        }).bounds(controlLeft, height - 62, 145, 20).build());
    }

    private void initItemTab() {
        int top = 38;
        int left = 12;
        int selectorWidth = Math.min(280, width / 2);
        int right = left + selectorWidth + 12;
        int controlsWidth = width - right - 12;

        addRenderableWidget(new ItemSelectorWidget(left, top + 8, selectorWidth, 285));

        ItemStack target = selectedItem;
        if (target.isEmpty()) target = findItem();
        selectedItem = target;
        final ItemStack itemTarget = target;

        drawLabel("ITEM CUSTOMIZATION", right, top + 5);
        drawLabel(target.isEmpty() ? "No Hypixel item selected" : target.getHoverName().getString(), right, top + 24);

        int row = top + 46;
        drawLabel("Custom name", right, row);
        nameField = field(right, row + 13, Math.max(120, controlsWidth - 80), SkyJewCustom.getName(target));
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            SkyJewCustom.setName(itemTarget, nameField.getValue());
            init();
        }).bounds(right + controlsWidth - 72, row + 13, 68, 20).build());

        row += 50;
        drawLabel("Dye color", right, row);
        dyeField = field(right, row + 13, 100, colorText(SkyJewCustom.getDye(target)));
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            try { SkyJewCustom.setDye(itemTarget, SkyJewCustom.parseHex(dyeField.getValue())); init(); }
            catch (Exception ignored) {}
        }).bounds(right + 106, row + 13, 68, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Clear customization"), b -> {
            SkyJewCustom.clearAll(itemTarget);
            init();
        }).bounds(right, row + 50, 145, 20).build());
    }

    private ItemStack currentArmor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;
        ItemStack stack = mc.player.getItemBySlot(armorSlots[selectedArmor]);
        return SkyJewCustom.hasUuid(stack) ? stack : ItemStack.EMPTY;
    }

    private ItemStack findItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;
        ItemStack hand = mc.player.getMainHandItem();
        if (SkyJewCustom.hasUuid(hand)) return hand;
        for (ItemStack stack : mc.player.getInventory()) {
            if (SkyJewCustom.hasUuid(stack)) return stack;
        }
        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (SkyJewCustom.hasUuid(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private EditBox field(int x, int y, int w, String value) {
        EditBox box = new EditBox(font, x, y, Math.max(55, w), 20, Component.empty());
        box.setValue(value == null ? "" : value);
        box.setMaxLength(1000);
        addRenderableWidget(box);
        return box;
    }

    private void drawLabel(String text, int x, int y) {
        // Labels are drawn in extractRenderState from the current layout.
    }

    private static String colorText(Integer color) {
        return color == null ? "" : String.format(Locale.ROOT, "%06X", color & 0xFFFFFF);
    }

    private static String trimMaterial(ItemStack stack) {
        SkyJewCustom.TrimId trim = SkyJewCustom.getTrim(stack);
        return trim == null ? "" : trim.material();
    }

    private static String trimPattern(ItemStack stack) {
        SkyJewCustom.TrimId trim = SkyJewCustom.getTrim(stack);
        return trim == null ? "" : trim.pattern();
    }

    private static String animColor(ItemStack stack, boolean first) {
        SkyJewCustom.AnimatedDye dye = SkyJewCustom.getAnimatedDye(stack);
        if (dye == null) return "";
        return String.format(Locale.ROOT, "%06X", (first ? dye.first().color() : dye.second().color()) & 0xFFFFFF);
    }

    private static String animDuration(ItemStack stack) {
        SkyJewCustom.AnimatedDye dye = SkyJewCustom.getAnimatedDye(stack);
        return dye == null ? "2" : Float.toString(dye.duration());
    }

    private static String animDelay(ItemStack stack) {
        SkyJewCustom.AnimatedDye dye = SkyJewCustom.getAnimatedDye(stack);
        return dye == null ? "0" : Float.toString(dye.delay());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);
        g.fill(10, 38, 128, height - 40, PANEL);
        g.fill(140, 38, 340, height - 40, PANEL);
        g.fill(352, 38, width - 12, height - 40, INNER);

        int tabY = 8;
        g.text(font, "Skyblocker-style Customization", 12, tabY + 6, TEXT, true);

        int top = 38;
        int left = 12;
        int previewWidth = itemTab ? Math.min(280, width / 2) : 190;
        int right = left + previewWidth + 12;

        

        if (itemTab) {
            g.text(font, "ITEM", right + 10, top + 5, TEXT, true);
            if (!selectedItem.isEmpty()) {
                g.item(selectedItem, left + previewWidth / 2 - 8, top + 38);
                g.text(font, selectedItem.getHoverName(), left + 12, top + 245, TEXT, false);
            }
        } else {
            g.text(font, "ARMOR", 20, top + 5, TEXT, true);
            ItemStack target = currentArmor();
            if (!target.isEmpty()) {
                g.text(font, target.getHoverName(), 155, top + 232, TEXT, false);
            }
        }

        // Draw section labels over the widgets. This mirrors Skyblocker's grouped
        // inner panels while leaving Minecraft's native text fields/buttons usable.
        if (!itemTab) {
            int x = right + 10;
            int y = top + 5;
            g.text(font, "ARMOR CUSTOMIZATION", x, y, TEXT, true);
            g.text(font, "Name", x, y + 35, MUTED, false);
            g.text(font, "Dye / Trim / Animated Dye", x, y + 83, MUTED, false);
        } else {
            int x = right + 10;
            g.text(font, "ITEM CUSTOMIZATION", x, top + 5, TEXT, true);
            g.text(font, "Custom name", x, top + 46, MUTED, false);
            g.text(font, "Dye color", x, top + 96, MUTED, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void cancel() {
        minecraft.gui.setScreen(previousScreen);
    }

    @Override
    public void onClose() {
        SkyJewCustom.save();
        minecraft.gui.setScreen(previousScreen);
    }

    private final class PlayerPreviewWidget extends AbstractWidget {
        private final RemotePlayer player;
        private float xRotation = -10;
        private float yRotation = 225;

        PlayerPreviewWidget(int x, int y, int w, int h, RemotePlayer player) {
            super(x, y, w, h, Component.empty());
            this.player = player;
        }

        @Override
        protected void onDrag(MouseButtonEvent e, double dx, double dy) {
            xRotation = Math.max(-50, Math.min(50, xRotation - (float) dy * 2.5f));
            yRotation += (float) dx * 2.5f;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            g.fill(getX(), getY(), getRight(), getBottom(), 0xFF1B1D20);
            float size = 64f;
            org.joml.Vector3f translation = new org.joml.Vector3f(0, player.getBbHeight() / 2f + 0.0625f, 0);
            org.joml.Quaternionf rotation = new org.joml.Quaternionf().rotationXYZ(
                -xRotation * net.minecraft.util.Mth.DEG_TO_RAD,
                -yRotation * net.minecraft.util.Mth.DEG_TO_RAD,
                (float) Math.PI
            );
            var state = com.epic60869.skyjew.mixin.SkyJewInventoryScreenInvoker.invokeExtractRenderState(player);
            g.entity(state, size, translation, rotation, null, getX(), getY(), getRight(), getBottom());
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput n) {}
    }

    private final class ArmorPieceWidget extends AbstractWidget {
        private final EquipmentSlot slot;
        private final Runnable click;

        ArmorPieceWidget(int x, int y, int w, int h, EquipmentSlot slot, Runnable click) {
            super(x, y, w, h, Component.literal(slot.getName()));
            this.slot = slot;
            this.click = click;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean selected = armorSlots[selectedArmor] == slot;
            g.fill(getX(), getY(), getRight(), getBottom(), selected ? SELECTED : 0xFF383A40);
            ItemStack stack = Minecraft.getInstance().player == null
                ? ItemStack.EMPTY
                : Minecraft.getInstance().player.getItemBySlot(slot);
            if (!stack.isEmpty()) g.item(stack, getX() + 4, getY() + 4);
        }

        @Override
        public void onClick(MouseButtonEvent e, boolean doubled) {
            if (e.button() == 0) click.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput n) {}
    }

    private final class ItemSelectorWidget extends AbstractWidget {
        private final List<ItemStack> items = new ArrayList<>();

        ItemSelectorWidget(int x, int y, int w, int h) {
            super(x, y, w, h, Component.literal("Item selector"));
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                for (ItemStack stack : mc.player.getInventory()) {
                    if (!stack.isEmpty()) items.add(stack);
                }
                for (EquipmentSlot slot : armorSlots) {
                    ItemStack stack = mc.player.getItemBySlot(slot);
                    if (!stack.isEmpty() && !items.contains(stack)) items.add(stack);
                }
            }
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            g.fill(getX(), getY(), getRight(), getBottom(), 0xFF17191C);
            for (int i = 0; i < items.size(); i++) {
                int col = i % 9;
                int row = i / 9;
                int x = getX() + 6 + col * 28;
                int y = getY() + 6 + row * 28;
                if (y + 24 > getBottom()) break;
                boolean selected = items.get(i) == selectedItem;
                if (selected) g.fill(x, y, x + 24, y + 24, 0xFF5865F2);
                g.item(items.get(i), x + 4, y + 4);
                if (!SkyJewCustom.hasUuid(items.get(i))) {
                    g.item(new ItemStack(Items.BARRIER), x + 4, y + 4);
                }
            }
        }

        @Override
        public void onClick(MouseButtonEvent e, boolean doubled) {
            if (e.button() != 0) return;
            int col = (int) ((e.x() - getX() - 6) / 28);
            int row = (int) ((e.y() - getY() - 6) / 28);
            int index = row * 9 + col;
            if (index >= 0 && index < items.size() && SkyJewCustom.hasUuid(items.get(index))) {
                selectedItem = items.get(index);
                init();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput n) {}
    }
}

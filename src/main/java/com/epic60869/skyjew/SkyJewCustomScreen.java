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
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * Skyblocker-style customization editor.
 *
 * The layout deliberately follows Skyblocker's customization screen:
 * - wide, centered content area
 * - clear ARMOR / ITEM tabs
 * - separate selection column and editor column
 * - generous spacing between controls
 * - footer actions kept away from the editor
 */
public final class SkyJewCustomScreen extends Screen {
    private static final int BG = 0xFF101216;
    private static final int PANEL = 0xFF24272C;
    private static final int INNER = 0xFF191C20;
    private static final int BORDER = 0xFF3A3E45;
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

    private int panelWidth() {
        return Math.min(720, width - 30);
    }

    private int panelHeight() {
        return Math.min(570, height - 20);
    }

    private int left() {
        return (width - panelWidth()) / 2;
    }

    private int top() {
        return (height - panelHeight()) / 2;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();

        int l = left();
        int t = top();
        int w = panelWidth();
        int h = panelHeight();

        // Skyblocker-style tab bar.
        addRenderableWidget(Button.builder(Component.literal("ARMOR"), b -> {
            tab = 0;
            selectedItem = ItemStack.EMPTY;
            rebuild();
        }).bounds(l + 14, t + 38, 92, 28).build());

        addRenderableWidget(Button.builder(Component.literal("ITEM"), b -> {
            tab = 1;
            selectedItem = ItemStack.EMPTY;
            rebuild();
        }).bounds(l + 112, t + 38, 92, 28).build());

        if (tab == 0) buildArmor(l, t, w);
        else buildItem(l, t, w);

        int footerY = t + h - 36;
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
            .bounds(l + w - 214, footerY, 96, 28).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(l + w - 110, footerY, 96, 28).build());
    }

    private void buildArmor(int l, int t, int w) {
        final int contentTop = t + 82;
        final int leftCol = l + 16;
        final int rightCol = l + 145;
        final int rightW = w - 161;

        // Selection column.
        label("ARMOR", leftCol, contentTop, true);
        label("Select a worn piece", leftCol, contentTop + 18, false);

        addRenderableWidget(new PieceSelectionWidget(leftCol, contentTop + 42));

        addRenderableWidget(Button.builder(Component.literal("Select Item"), b ->
            minecraft.gui.setScreen(new SkyJewItemSelectScreen(this, stack -> {
                selectedItem = stack.copy();
                rebuild();
            }))).bounds(leftCol, contentTop + 42 + ARMOR.length * 38 + 8, 112, 30).build());

        ItemStack selected = selectedStack();
        drawArmorEditorWidgets(rightCol, contentTop, rightW, selected);
    }

    private void drawArmorEditorWidgets(int x, int y, int availableWidth, ItemStack selected) {
        if (selected.isEmpty()) return;

        // Large item preview area, matching Skyblocker's left/right visual split.
        int previewW = Math.min(150, availableWidth);
        addRenderableWidget(Button.builder(Component.literal(selected.getHoverName().getString()), b -> {})
            .bounds(x, y + 24, previewW, 34).build());

        if (selected.is(Items.PLAYER_HEAD)) {
            addRenderableWidget(Button.builder(Component.literal("Select Helmet Skin"), b ->
                minecraft.gui.setScreen(new SkyJewHelmetSkinSelectScreen(this, selected)))
                .bounds(x, y + 72, 190, 30).build());

            addRenderableWidget(Button.builder(Component.literal("Reset Helmet Skin"), b -> {
                SkyJewCustom.setHelmetSkin(selected, null);
                rebuild();
            }).bounds(x + 200, y + 72, 170, 30).build());

            label("Helmet texture", x, y + 116, false);
            label(SkyJewCustom.helmetSkinDataLoaded()
                ? "Choose a Hypixel head texture from the selector."
                : "Loading Hypixel head textures...", x, y + 134, false);
            return;
        }

        label("DYE", x, y + 10, true);
        addRenderableWidget(new ColorSelectionWidget(x, y + 38, Math.min(390, availableWidth), selected));

        addRenderableWidget(Button.builder(Component.literal("Reset Dye"), b -> {
            SkyJewCustom.setDye(selected, null);
            SkyJewCustom.setAnimatedDye(selected, (Integer) null, (Integer) null, 1f, false, 0f);
            rebuild();
        }).bounds(x, y + 82, 110, 28).build());

        label("CUSTOM ANIMATED DYE", x, y + 126, true);

        int fieldY = y + 150;
        int gap = 8;
        int fieldW = Math.max(100, (availableWidth - gap * 2 - 86) / 3);

        animatedStart = box("Start #RRGGBB", x, fieldY, fieldW, "");
        animatedEnd = box("End #RRGGBB", x + fieldW + gap, fieldY, fieldW, "");
        animatedDuration = box("Seconds", x + (fieldW + gap) * 2, fieldY, 78, "5");
        addRenderableWidget(animatedStart);
        addRenderableWidget(animatedEnd);
        addRenderableWidget(animatedDuration);

        cycleBack = Checkbox.builder(Component.literal("Cycle"), font)
            .pos(x, fieldY + 31).selected(true).build();
        addRenderableWidget(cycleBack);

        addRenderableWidget(Button.builder(Component.literal("Apply Animation"), b ->
            applyAnimatedDye(selected))
            .bounds(x + 82, fieldY + 27, 150, 25).build());

        label("ITEM MODEL", x, fieldY + 72, true);
        label("Armor model override is kept client-side.", x, fieldY + 90, false);
    }

    private void buildItem(int l, int t, int w) {
        int contentTop = t + 82;
        int selectorX = l + 16;
        int editorX = l + 170;
        int editorW = w - 186;

        label("ITEM", selectorX, contentTop, true);
        label("Choose an item to edit", selectorX, contentTop + 18, false);

        addRenderableWidget(Button.builder(Component.literal("Select Item"), b ->
            minecraft.gui.setScreen(new SkyJewItemSelectScreen(this, stack -> {
                selectedItem = stack.copy();
                rebuild();
            }))).bounds(selectorX, contentTop + 45, 120, 30).build());

        ItemStack selected = selectedItem.isEmpty()
            ? (Minecraft.getInstance().player == null
                ? ItemStack.EMPTY
                : Minecraft.getInstance().player.getMainHandItem())
            : selectedItem;

        if (selected.isEmpty()) {
            label("No item selected", editorX, contentTop + 50, true);
            return;
        }

        label("ITEM CUSTOMISATION", editorX, contentTop, true);
        label(selected.getHoverName().getString(), editorX, contentTop + 20, false);

        itemName = box("Custom item name", editorX, contentTop + 50, Math.min(320, editorW - 100),
            SkyJewCustom.getName(selected) == null ? "" : SkyJewCustom.getName(selected));
        addRenderableWidget(itemName);

        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            SkyJewCustom.setName(selected, itemName.getValue());
            rebuild();
        }).bounds(editorX + Math.min(320, editorW - 100) + 8, contentTop + 50, 70, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            SkyJewCustom.setName(selected, null);
            rebuild();
        }).bounds(editorX + Math.min(320, editorW - 100) + 84, contentTop + 50, 70, 22).build());

        label("GLINT", editorX, contentTop + 92, true);
        Boolean currentGlint = SkyJewCustom.getGlint(selected);
        glint = Checkbox.builder(Component.literal("Override enchant glint"), font)
            .pos(editorX, contentTop + 113)
            .selected(currentGlint == null || currentGlint)
            .build();
        addRenderableWidget(glint);

        addRenderableWidget(Button.builder(Component.literal("Apply Glint"), b ->
            SkyJewCustom.setGlint(selected, glint.selected()))
            .bounds(editorX + 190, contentTop + 110, 110, 24).build());

        label("ITEM MODEL", editorX, contentTop + 153, true);
        modelId = box("Item model identifier", editorX, contentTop + 175, Math.min(320, editorW - 130),
            SkyJewCustom.getItemModel(selected) == null ? "" : SkyJewCustom.getItemModel(selected));
        addRenderableWidget(modelId);

        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            SkyJewCustom.setItemModel(selected, modelId.getValue());
            rebuild();
        }).bounds(editorX + Math.min(320, editorW - 130) + 8, contentTop + 175, 70, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Select Model"), b ->
            minecraft.gui.setScreen(new SkyJewItemIconSelectScreen(this, identifier -> {
                SkyJewCustom.setItemModel(selected, identifier.toString());
                rebuild();
            }))).bounds(editorX, contentTop + 211, 130, 26).build());

        addRenderableWidget(Button.builder(Component.literal("Reset Model"), b -> {
            SkyJewCustom.setItemModel(selected, null);
            rebuild();
        }).bounds(editorX + 138, contentTop + 211, 120, 26).build());
    }

    private void label(String text, int x, int y, boolean heading) {
        // Labels are rendered by extractRenderState so the widgets remain
        // uncluttered and all spacing is deterministic.
    }

    private EditBox box(String hint, int x, int y, int w, String value) {
        EditBox box = new EditBox(font, x, y, Math.max(70, w), 22, Component.literal(hint));
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
        int l = left();
        int t = top();
        int w = panelWidth();
        int h = panelHeight();

        g.fill(0, 0, width, height, BG);
        g.fill(l, t, l + w, t + h, PANEL);
        g.fill(l, t, l + w, t + 2, ACCENT);

        g.text(font, "SkyJew Customisation", l + 14, t + 13, TEXT, true);
        g.text(font, "Item & armour customisation", l + 14, t + 27, MUTED, false);

        // Tab selection underline.
        int selectedTabX = tab == 0 ? l + 14 : l + 112;
        g.fill(selectedTabX, t + 67, selectedTabX + 92, t + 69, ACCENT);

        // Content card.
        int cx = l + 10;
        int cy = t + 75;
        int cr = l + w - 10;
        int cb = t + h - 49;
        g.fill(cx, cy, cr, cb, INNER);
        outline(g, cx, cy, cr, cb, BORDER);

        if (tab == 0) {
            int x = l + 145;
            int y = t + 82;
            drawText(g, "ARMOUR CUSTOMISATION", x, y, TEXT, true);
            drawText(g, "Select an armour piece on the left, then choose its visual override.", x, y + 17, MUTED, false);

            ItemStack selected = selectedStack();
            if (!selected.isEmpty()) {
                g.item(selected, x + 4, y + 37);
                drawText(g, selected.getHoverName().getString(), x + 40, y + 41, TEXT, false);

                String uuid = SkyJewCustom.uuid(selected);
                drawText(g, uuid.isBlank() ? "This item has no Hypixel UUID." : "Client-side customization is active.",
                    x + 40, y + 56, uuid.isBlank() ? 0xFFFF6B6B : SUCCESS, false);
            } else {
                drawText(g, "No customizable item selected.", x, y + 50, MUTED, false);
            }
        } else {
            drawText(g, "ITEM CUSTOMISATION", l + 170, t + 82, TEXT, true);
            drawText(g, "Rename, change the model, or override enchant glint.", l + 170, t + 99, MUTED, false);
        }

        drawText(g, tab == 0 ? "Armor pieces" : "Item selector", l + 16, t + 86, TEXT, true);
        drawText(g, "All changes are client-side.", l + 16, t + h - 65, MUTED, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawText(GuiGraphicsExtractor g, String text, int x, int y, int color, boolean bold) {
        g.text(font, text, x, y, color, bold);
    }

    private static void outline(GuiGraphicsExtractor g, int l, int t, int r, int b, int c) {
        g.fill(l, t, r, t + 1, c);
        g.fill(l, b - 1, r, b, c);
        g.fill(l, t, l + 1, b, c);
        g.fill(r - 1, t, r, b, c);
    }

    private final class PieceSelectionWidget extends net.minecraft.client.gui.components.AbstractWidget {
        PieceSelectionWidget(int x, int y) { super(x, y, 120, 32, Component.literal("Armor pieces")); }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(getX(), getY(), getRight(), getBottom(), 0xFF30343A);
            for (int i=0;i<4;i++) { int sx=getX()+i*30; if(i==selectedArmor) g.fill(sx,getY(),sx+30,getBottom(),0x5555FFFF); ItemStack s=minecraft.player==null?ItemStack.EMPTY:minecraft.player.getItemBySlot(ARMOR[i]); if(!s.isEmpty()) g.item(s,sx+7,getY()+7); }
            if(isHovered()) g.fill(getX(),getY(),getRight(),getBottom(),0x18FFFFFF); handleCursor(g);
        }
        @Override public void onClick(net.minecraft.client.input.MouseButtonEvent e, boolean d) { int i=(int)((e.x()-getX())/30); if(i>=0&&i<4){selectedArmor=i;selectedItem=ItemStack.EMPTY;rebuild();} }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput n) {}
    }
    private final class ColorSelectionWidget extends net.minecraft.client.gui.components.AbstractWidget {
        private final ItemStack item; private final int[] colors={0xFFFFFF,0xFF5555,0x55FF55,0x5555FF,0xFFFF55,0xFF55FF,0x55FFFF,0xFFAA00,0xAAAAAA,0x555555};
        ColorSelectionWidget(int x,int y,int w,ItemStack item){super(x,y,w,66,Component.literal("Dye"));this.item=item.copy();}
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float d){ Integer cur=SkyJewCustom.getDye(item); for(int i=0;i<colors.length;i++){int sx=getX()+i*26;g.fill(sx,getY()+4,sx+22,getY()+26,0xFF000000|colors[i]);if(cur!=null&&(cur&0xFFFFFF)==colors[i])g.fill(sx,getY()+4,sx+22,getY()+6,0xFFFFFFFF);}g.text(font,"Click a swatch • right-click for animated dyes",getX(),getY()+38,0xFFAAAAAA,false);handleCursor(g);}
        @Override public void onClick(net.minecraft.client.input.MouseButtonEvent e,boolean d){if(e.button()==1){minecraft.gui.setScreen(new SkyJewDyeSelectScreen(SkyJewCustomScreen.this,item,true));return;}int i=(int)((e.x()-getX())/26);if(i>=0&&i<colors.length){SkyJewCustom.setDye(item,colors[i]);SkyJewCustom.setAnimatedDye(item,(Integer)null,(Integer)null,1f,false,0f);rebuild();}}
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput n) {}
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

}

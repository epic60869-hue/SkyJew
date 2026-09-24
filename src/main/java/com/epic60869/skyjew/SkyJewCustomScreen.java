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
 * Skyblocker customization GUI port for SkyJew.
 *
 * This follows Skyblocker's actual screen structure rather than only copying
 * the old visual style: a tabbed CustomizeScreen, player preview/equipment
 * selector, item selector, and grouped dye/trim controls.
 *
 * Source: SkyblockerMod/Skyblocker
 * https://github.com/SkyblockerMod/Skyblocker
 * Licensed under LGPL-3.0.
 */
public final class SkyJewCustomScreen extends Screen {
    private static final int BG = 0xFF202124;
    private static final int PANEL = 0xFF303236;
    private static final int INNER = 0xFF1E1F22;
    private static final int BORDER = 0xFF4A4D52;
    private static final int TEXT = 0xFFF2F3F5;
    private static final int MUTED = 0xFFB5BAC1;
    private static final int SELECTED = 0x5533AAFF;
    private static final int GREEN = 0xFF57F287;
    private static final int RED = 0xFFFF6B6B;

    private final Screen previousScreen;
    private boolean itemTab;
    private EquipmentSlot selectedSlot = EquipmentSlot.HEAD;
    private ItemStack selectedItem = ItemStack.EMPTY;
    private final List<AbstractWidget> dynamic = new ArrayList<>();

    private EditBox nameField;
    private EditBox dyeField;
    private EditBox trimMaterial;
    private EditBox trimPattern;
    private EditBox animFirst;
    private EditBox animSecond;
    private EditBox animDuration;
    private EditBox animDelay;
    private boolean cycleBack;
    private boolean colorPicker;

    private RemotePlayer previewPlayer;

    public SkyJewCustomScreen(Screen previousScreen) {
        super(Component.literal("SkyJew Customization"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        clearWidgets();
        dynamic.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            previewPlayer = new RemotePlayer(mc.level, mc.getGameProfile()) {
                @Override public boolean isInvisibleTo(net.minecraft.world.entity.player.Player p) { return true; }
                @Override public int getId() { return -100001; }
            };
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                previewPlayer.setItemSlot(slot, mc.player.getItemBySlot(slot).copy());
            }
        }

        int tabY = 12;
        int tabW = 130;
        addRenderableWidget(Button.builder(Component.literal("Armor"), b -> switchTab(false))
            .bounds(width / 2 - tabW - 4, tabY, tabW, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Item"), b -> switchTab(true))
            .bounds(width / 2 + 4, tabY, tabW, 22).build());

        if (itemTab) initItemTab();
        else initArmorTab();

        int footerY = height - 28;
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> cancel())
            .bounds(width / 2 - 92, footerY, 84, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(width / 2 + 8, footerY, 84, 22).build());
    }

    private void switchTab(boolean item) {
        itemTab = item;
        colorPicker = false;
        init();
    }

    private void initArmorTab() {
        int top = 48;
        int left = 20;
        int previewW = Math.min(245, width / 3);
        int right = left + previewW + 12;
        int controlsW = width - right - 20;

        addRenderableWidget(new SkyJewPlayerWidget(left + 10, top + 10, 84, 165, previewPlayer));

        EquipmentSlot[] slots = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (int i = 0; i < slots.length; i++) {
            final EquipmentSlot slot = slots[i];
            addRenderableWidget(new EquipmentWidget(
                left + 10 + i * 48, top + 184, 44, 42, slot,
                () -> { selectedSlot = slot; init(); }
            ));
        }

        ItemStack target = currentArmor();
        int x = right;
        int y = top;
        int w = controlsW;

        section(x, y, w, "ARMOUR CUSTOMIZATION");
        label(x, y + 28, "ITEM NAME");
        nameField = field(x, y + 42, w - 150, SkyJewCustom.getName(target));
        button(x + w - 142, y + 42, 68, "Apply", b -> { SkyJewCustom.setName(target, nameField.getValue()); init(); });
        button(x + w - 70, y + 42, 68, "Clear", b -> { SkyJewCustom.setName(target, null); init(); });

        label(x, y + 72, "DYE");
        dyeField = field(x, y + 86, 90, colorText(SkyJewCustom.getDye(target)));
        button(x + 96, y + 86, 92, "Pick Dye", b -> colorPicker = true);
        button(x + 194, y + 86, 68, "Apply", b -> applyDye(target));

        if (target.is(Items.PLAYER_HEAD)) {
            label(x, y + 120, "HEAD TEXTURE");
            button(x, y + 136, 105, "Select Head", b -> {});
        } else {
            label(x, y + 120, "ARMOUR TRIM");
            trimMaterial = field(x, y + 136, (w - 8) / 2, trimMaterial(target));
            trimPattern = field(x + (w - 8) / 2 + 8, y + 136, (w - 8) / 2, trimPattern(target));
            button(x, y + 160, 105, "Apply Trim", b -> {
                SkyJewCustom.setTrim(target, trimMaterial.getValue(), trimPattern.getValue()); init();
            });
            button(x + 110, y + 160, 75, "Clear", b -> {
                SkyJewCustom.setTrim(target, null, null); init();
            });

            label(x, y + 194, "ANIMATED DYE");
            animFirst = field(x, y + 208, 74, animColor(target, true));
            animSecond = field(x + 80, y + 208, 74, animColor(target, false));
            animDuration = field(x + 160, y + 208, 54, animDuration(target));
            animDelay = field(x + 220, y + 208, 54, animDelay(target));
            button(x, y + 232, 112, cycleBack ? "Cycle Back: ON" : "Cycle Back: OFF", b -> {
                cycleBack = !cycleBack; b.setMessage(Component.literal(cycleBack ? "Cycle Back: ON" : "Cycle Back: OFF"));
            });
            button(x + 118, y + 232, 118, "Apply Animated", b -> applyAnimated(target));
            button(x + 242, y + 232, 70, "Clear", b -> {
                SkyJewCustom.setAnimatedDye(target, null, null, 1, false, 0); init();
            });
        }

        button(x, height - 62, 150, "RESET ALL", b -> {
            SkyJewCustom.clearAll(target); init();
        });
    }

    private void initItemTab() {
        int top = 48;
        int left = 20;
        int gridW = Math.min(330, width / 2);
        int right = left + gridW + 18;
        int controlsW = width - right - 20;

        section(left, top, gridW, "ITEM SELECTION");
        ItemGridWidget grid = new ItemGridWidget(left + 10, top + 30, gridW - 20, 300);
        addRenderableWidget(grid);

        if (selectedItem.isEmpty()) selectedItem = findFirstCustomizableItem();
        ItemStack target = selectedItem;

        section(right, top, controlsW, "ITEM CUSTOMIZATION");
        label(right, top + 28, "ITEM NAME");
        nameField = field(right, top + 42, controlsW - 150, SkyJewCustom.getName(target));
        button(right + controlsW - 142, top + 42, 68, "Apply", b -> { SkyJewCustom.setName(target, nameField.getValue()); init(); });
        button(right + controlsW - 70, top + 42, 68, "Clear", b -> { SkyJewCustom.setName(target, null); init(); });

        label(right, top + 72, "ITEM COLOUR");
        dyeField = field(right, top + 86, 90, colorText(SkyJewCustom.getDye(target)));
        button(right + 96, top + 86, 92, "Pick Dye", b -> colorPicker = true);
        button(right + 194, top + 86, 68, "Apply", b -> applyDye(target));
        button(right, top + 126, 220, "RESET ALL CUSTOMIZATION", b -> { SkyJewCustom.clearAll(target); init(); });
    }

    private void applyDye(ItemStack target) {
        try {
            SkyJewCustom.setDye(target, SkyJewCustom.parseHex(dyeField.getValue()));
            init();
        } catch (Exception ignored) {}
    }

    private void applyAnimated(ItemStack target) {
        try {
            SkyJewCustom.setAnimatedDye(target,
                SkyJewCustom.parseHex(animFirst.getValue()),
                SkyJewCustom.parseHex(animSecond.getValue()),
                Float.parseFloat(animDuration.getValue()),
                cycleBack,
                Float.parseFloat(animDelay.getValue()));
            init();
        } catch (Exception ignored) {}
    }

    private void section(int x, int y, int w, String title) {
        gLabel(x, y, w, title);
    }

    private void gLabel(int x, int y, int w, String title) {
        // Layout marker; the actual title is drawn in extractRenderState.
    }

    private void label(int x, int y, String text) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> {})
            .bounds(x, y, Math.max(1, font.width(text)), 1).build());
    }

    private void button(int x, int y, int w, String text, Button.OnPress press) {
        addRenderableWidget(Button.builder(Component.literal(text), press).bounds(x, y, w, 20).build());
    }

    private EditBox field(int x, int y, int w, String value) {
        EditBox box = new EditBox(font, x, y, Math.max(55, w), 20, Component.empty());
        box.setValue(value == null ? "" : value);
        box.setMaxLength(1000);
        addRenderableWidget(box);
        return box;
    }

    private ItemStack currentArmor() {
        return Minecraft.getInstance().player == null ? ItemStack.EMPTY :
            Minecraft.getInstance().player.getItemBySlot(selectedSlot);
    }

    private ItemStack findFirstCustomizableItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = mc.player.getItemBySlot(s);
            if (SkyJewCustom.hasUuid(stack)) return stack;
        }
        for (ItemStack stack : mc.player.getInventory()) if (SkyJewCustom.hasUuid(stack)) return stack;
        return ItemStack.EMPTY;
    }

    private static String colorText(Integer c) {
        return c == null ? "" : String.format(Locale.ROOT, "%06X", c & 0xFFFFFF);
    }
    private static String trimMaterial(ItemStack s) { SkyJewCustom.TrimId t=SkyJewCustom.getTrim(s); return t==null?"":t.material(); }
    private static String trimPattern(ItemStack s) { SkyJewCustom.TrimId t=SkyJewCustom.getTrim(s); return t==null?"":t.pattern(); }
    private static String animColor(ItemStack s, boolean first) { SkyJewCustom.AnimatedDye d=SkyJewCustom.getAnimatedDye(s); return d==null?"":String.format(Locale.ROOT,"%06X",(first?d.first().color():d.second().color())&0xFFFFFF); }
    private static String animDuration(ItemStack s) { SkyJewCustom.AnimatedDye d=SkyJewCustom.getAnimatedDye(s); return d==null?"2":Float.toString(d.duration()); }
    private static String animDelay(ItemStack s) { SkyJewCustom.AnimatedDye d=SkyJewCustom.getAnimatedDye(s); return d==null?"0":Float.toString(d.delay()); }

    private void cancel() {
        minecraft.gui.setScreen(previousScreen);
    }

    @Override public void onClose() {
        SkyJewCustom.save();
        minecraft.gui.setScreen(previousScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);

        g.fill(0, 0, width, 2, 0xFF5B8CFF);
        g.text(font, "SkyJew Customization", 18, 16, TEXT, true);

        int top = 48;
        int left = 20;
        int previewW = itemTab ? Math.min(330, width / 2) : Math.min(245, width / 3);
        int right = left + previewW + 18;
        int controlsW = width - right - 20;

        g.fill(left, top, right - 8, height - 42, PANEL);
        outline(g, left, top, right - 8, height - 42, BORDER);
        g.fill(right, top, width - 20, height - 42, INNER);
        outline(g, right, top, width - 20, height - 42, BORDER);

        if (itemTab) {
            g.text(font, "ITEM SELECTION", left + 10, top + 10, TEXT, true);
            if (!selectedItem.isEmpty()) {
                g.item(selectedItem, left + 130, top + 185);
                g.text(font, selectedItem.getHoverName(), left + 25, top + 230, GREEN, false);
                g.text(font, "UUID: " + shortUuid(SkyJewCustom.uuid(selectedItem)), left + 25, top + 250, MUTED, false);
            }
            g.text(font, "Inventory items with a Hypixel UUID are selectable.", left + 10, height - 58, MUTED, false);
            g.text(font, "ITEM CUSTOMIZATION", right + 10, top + 10, TEXT, true);
        } else {
            g.text(font, "PLAYER", left + 10, top + 10, TEXT, true);
            g.text(font, "Drag the player preview to rotate it.", left + 10, top + 180, MUTED, false);
            ItemStack target = currentArmor();
            g.text(font, target.isEmpty() ? "No armor selected" : target.getHoverName(), left + 10, top + 238, target.isEmpty() ? RED : GREEN, false);
            if (!target.isEmpty()) g.text(font, "UUID: " + shortUuid(SkyJewCustom.uuid(target)), left + 10, top + 255, MUTED, false);
            g.text(font, "ARMOUR CUSTOMIZATION", right + 10, top + 10, TEXT, true);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
        if (colorPicker) drawColorPicker(g);
    }

    private void drawColorPicker(GuiGraphicsExtractor g) {
        int w=320,h=230,l=(width-w)/2,t=(height-h)/2;
        g.fill(0,0,width,height,0x99000000);
        g.fill(l,t,l+w,t+h,0xFF2B2D31);
        outline(g,l,t,l+w,t+h,0xFF7A7D84);
        g.text(font,"Select Dye Colour",l+12,t+10,TEXT,true);

        int sx=l+15,sy=t+38,sw=230,sh=140;
        for(int yy=0;yy<sh;yy++) for(int xx=0;xx<sw;xx++) {
            float s=xx/(float)(sw-1), v=1f-yy/(float)(sh-1);
            int rgb=hsv(0,s,v);
            g.fill(sx+xx,sy+yy,sx+xx+1,sy+yy+1,0xFF000000|rgb);
        }
        for(int yy=0;yy<sh;yy++) {
            int rgb=hsv(yy/(float)(sh-1)*360f,1,1);
            g.fill(sx+sw+8,sy+yy,sx+sw+30,sy+yy+1,0xFF000000|rgb);
        }
        g.text(font,"Click a colour or hue.",l+15,t+190,MUTED,false);
        g.text(font,"Close",l+w-55,t+h-18,TEXT,false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if(colorPicker) {
            int w=320,h=230,l=(width-w)/2,t=(height-h)/2,sx=l+15,sy=t+38,sw=230,sh=140;
            if(e.button()==0 && e.x()>=sx && e.x()<sx+sw && e.y()>=sy && e.y()<sy+sh) {
                int rgb=hsv(0,(float)(e.x()-sx)/sw,1-(float)(e.y()-sy)/sh);
                if(dyeField!=null)dyeField.setValue(String.format(Locale.ROOT,"%06X",rgb));
                colorPicker=false; return true;
            }
            if(e.button()==0 && e.x()>=sx+sw+8 && e.x()<sx+sw+30 && e.y()>=sy && e.y()<sy+sh) {
                int rgb=hsv((float)(e.y()-sy)/sh*360f,1,1);
                if(dyeField!=null)dyeField.setValue(String.format(Locale.ROOT,"%06X",rgb));
                colorPicker=false; return true;
            }
            if(e.button()==0 && e.x()>=l+w-75 && e.y()>=t+h-35){colorPicker=false;return true;}
            return true;
        }
        return super.mouseClicked(e,doubled);
    }

    private static int hsv(float h,float s,float v) {
        float c=v*s, x=c*(1-Math.abs((h/60f)%2-1)), m=v-c;
        float r=0,g=0,b=0;
        if(h<60){r=c;g=x;}else if(h<120){r=x;g=c;}else if(h<180){g=c;b=x;}
        else if(h<240){g=x;b=c;}else if(h<300){r=x;b=c;}else{r=c;b=x;}
        return ((int)((r+m)*255)<<16)|((int)((g+m)*255)<<8)|(int)((b+m)*255);
    }

    private static String shortUuid(String s) { return s == null || s.isBlank() ? "none" : s.substring(0, Math.min(18,s.length())) + (s.length()>18?"...":""); }
    private static void outline(GuiGraphicsExtractor g,int l,int t,int r,int b,int c){g.fill(l,t,r,t+1,c);g.fill(l,b-1,r,b,c);g.fill(l,t,l+1,b,c);g.fill(r-1,t,r,b,c);}

    private final class EquipmentWidget extends AbstractWidget {
        private final EquipmentSlot slot; private final Runnable click;
        EquipmentWidget(int x,int y,int w,int h,EquipmentSlot slot,Runnable click){super(x,y,w,h,Component.literal(slot.getName()));this.slot=slot;this.click=click;}
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
            boolean selected=selectedSlot==slot;
            g.fill(getX(),getY(),getRight(),getBottom(),selected?SELECTED:0xFF292C30);
            outline(g,getX(),getY(),getRight(),getBottom(),selected?0xFF7AB7FF:BORDER);
            ItemStack s=Minecraft.getInstance().player==null?ItemStack.EMPTY:Minecraft.getInstance().player.getItemBySlot(slot);
            if(!s.isEmpty())g.item(s,getX()+13,getY()+2);
            g.text(font,slot.getName().toUpperCase(Locale.ROOT),getX()+3,getY()+28,MUTED,false);
        }
        @Override public void onClick(MouseButtonEvent e,boolean d){if(e.button()==0)click.run();}
        @Override protected void updateWidgetNarration(NarrationElementOutput n){}
    }

    private final class ItemGridWidget extends AbstractWidget {
        private final List<ItemStack> items=new ArrayList<>();
        ItemGridWidget(int x,int y,int w,int h){super(x,y,w,h,Component.literal("Item selection"));build();}
        private void build(){
            Minecraft mc=Minecraft.getInstance(); if(mc.player==null)return;
            for(ItemStack s:mc.player.getInventory())if(!s.isEmpty())items.add(s);
            for(EquipmentSlot s:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){ItemStack st=mc.player.getItemBySlot(s);if(!st.isEmpty()&&!items.contains(st))items.add(st);}
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
            g.fill(getX(),getY(),getRight(),getBottom(),0xFF17191C); outline(g,getX(),getY(),getRight(),getBottom(),BORDER);
            for(int i=0;i<items.size();i++){int col=i%9,row=i/9,x=getX()+6+col*26,y=getY()+6+row*26;if(y+22>getBottom())break;
                if(mx>=x&&mx<x+22&&my>=y&&my<y+22)g.fill(x,y,x+22,y+22,0x553F8CFF);
                g.item(items.get(i),x+3,y+3);
                if(!SkyJewCustom.hasUuid(items.get(i)))g.item(new ItemStack(Items.BARRIER),x+3,y+3);
            }
        }
        @Override public void onClick(MouseButtonEvent e,boolean d){
            if(e.button()!=0)return;
            int col=(int)((e.x()-getX()-6)/26),row=(int)((e.y()-getY()-6)/26),i=row*9+col;
            if(i>=0&&i<items.size()&&SkyJewCustom.hasUuid(items.get(i))){selectedItem=items.get(i);init();}
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput n){}
    }
}
package com.epic60869.tastyfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** GUI equivalent of Skyblocker's /skyblocker custom screen for the held item. */
public final class TastyFishCustomScreen extends Screen {
    private static final int BG = 0xFF080B12;
    private static final int PANEL = 0xFF141B27;
    private static final int BORDER = 0xFF33425A;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF8D9AAF;
    private static final int CYAN = 0xFF58D8FF;
    private static final int PURPLE = 0xFF9A6CFF;
    private static final int GREEN = 0xFF69E6A3;

    private final Screen parent;
    private EditBox name;
    private EditBox dye;
    private EditBox trimMaterial;
    private EditBox trimPattern;
    private EditBox animated1;
    private EditBox animated2;
    private EditBox duration;
    private EditBox delay;
    private boolean cycleBack;

    public TastyFishCustomScreen(Screen parent) {
        super(Component.literal("TastyFish Custom"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int left = Math.max(25, (width - 760) / 2);
        int top = Math.max(25, (height - 520) / 2);
        int col2 = left + 390;

        name = field(left + 20, top + 95, 350, currentName());
        dye = field(left + 20, top + 180, 170, currentDye());
        trimMaterial = field(left + 20, top + 285, 170, currentTrimMaterial());
        trimPattern = field(left + 200, top + 285, 170, currentTrimPattern());

        animated1 = field(col2 + 20, top + 95, 155, currentAnimated(0));
        animated2 = field(col2 + 190, top + 95, 155, currentAnimated(1));
        duration = field(col2 + 20, top + 180, 155, currentDuration());
        delay = field(col2 + 190, top + 180, 155, currentDelay());

        addRenderableWidget(Button.builder(Component.literal("Apply Name"), b -> applyName())
                .bounds(left + 20, top + 125, 170, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Remove Name"), b -> {
            TastyFishCustom.setName(TastyFishCustom.held(Minecraft.getInstance()), null);
            init();
        }).bounds(left + 200, top + 125, 170, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Apply Dye"), b -> applyDye())
                .bounds(left + 20, top + 210, 170, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Remove Dye"), b -> {
            TastyFishCustom.setDye(TastyFishCustom.held(Minecraft.getInstance()), null);
            init();
        }).bounds(left + 200, top + 210, 170, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Apply Trim"), b -> applyTrim())
                .bounds(left + 20, top + 315, 170, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Remove Trim"), b -> {
            TastyFishCustom.setTrim(TastyFishCustom.held(Minecraft.getInstance()), null, null);
            init();
        }).bounds(left + 200, top + 315, 170, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Cycle Back: " + (cycleBack ? "ON" : "OFF")), b -> {
            cycleBack = !cycleBack;
            b.setMessage(Component.literal("Cycle Back: " + (cycleBack ? "ON" : "OFF")));
        }).bounds(col2 + 20, top + 210, 155, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Apply Animated Dye"), b -> applyAnimated())
                .bounds(col2 + 190, top + 210, 155, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Remove Animated Dye"), b -> {
            TastyFishCustom.setAnimatedDye(TastyFishCustom.held(Minecraft.getInstance()), null, null, 1, false, 0);
            init();
        }).bounds(col2 + 20, top + 240, 325, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Remove ALL Customization"), b -> {
            TastyFishCustom.clearAll(TastyFishCustom.held(Minecraft.getInstance()));
            init();
        }).bounds(col2 + 20, top + 285, 325, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width - 130, height - 45, 100, 24).build());
    }

    private EditBox field(int x, int y, int w, String value) {
        EditBox e = new EditBox(font, x, y, w, 20, Component.empty());
        e.setValue(value == null ? "" : value);
        e.setMaxLength(1000);
        addRenderableWidget(e);
        return e;
    }

    private String currentName() {
        String v = TastyFishCustom.getName(TastyFishCustom.held(Minecraft.getInstance()));
        return v == null ? "" : v;
    }

    private String currentDye() {
        Integer v = TastyFishCustom.getDye(TastyFishCustom.held(Minecraft.getInstance()));
        return v == null ? "" : String.format(Locale.ROOT, "%06X", v);
    }

    private String currentTrimMaterial() {
        TastyFishCustom.TrimId t = TastyFishCustom.getTrim(TastyFishCustom.held(Minecraft.getInstance()));
        return t == null ? "" : t.material();
    }

    private String currentTrimPattern() {
        TastyFishCustom.TrimId t = TastyFishCustom.getTrim(TastyFishCustom.held(Minecraft.getInstance()));
        return t == null ? "" : t.pattern();
    }

    private String currentAnimated(int which) {
        TastyFishCustom.AnimatedDye d = TastyFishCustom.getAnimatedDye(TastyFishCustom.held(Minecraft.getInstance()));
        if (d == null) return "";
        return String.format(Locale.ROOT, "%06X", which == 0 ? d.first().color() : d.second().color());
    }

    private String currentDuration() {
        TastyFishCustom.AnimatedDye d = TastyFishCustom.getAnimatedDye(TastyFishCustom.held(Minecraft.getInstance()));
        return d == null ? "2" : String.valueOf(d.duration());
    }

    private String currentDelay() {
        TastyFishCustom.AnimatedDye d = TastyFishCustom.getAnimatedDye(TastyFishCustom.held(Minecraft.getInstance()));
        return d == null ? "0" : String.valueOf(d.delay());
    }

    private void applyName() {
        TastyFishCustom.setName(TastyFishCustom.held(Minecraft.getInstance()), name.getValue());
        init();
    }

    private void applyDye() {
        try {
            TastyFishCustom.setDye(TastyFishCustom.held(Minecraft.getInstance()), TastyFishCustom.parseHex(dye.getValue()));
            init();
        } catch (Exception ignored) {}
    }

    private void applyTrim() {
        TastyFishCustom.setTrim(TastyFishCustom.held(Minecraft.getInstance()),
                trimMaterial.getValue(), trimPattern.getValue());
        init();
    }

    private void applyAnimated() {
        try {
            int a = TastyFishCustom.parseHex(animated1.getValue());
            int b = TastyFishCustom.parseHex(animated2.getValue());
            float d = Float.parseFloat(duration.getValue());
            float wait = Float.parseFloat(delay.getValue());
            TastyFishCustom.setAnimatedDye(TastyFishCustom.held(Minecraft.getInstance()), a, b, d, cycleBack, wait);
            init();
        } catch (Exception ignored) {}
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);
        g.fill(0, 0, width, 3, PURPLE);

        int left = Math.max(25, (width - 760) / 2);
        int top = Math.max(25, (height - 520) / 2);
        int right = left + 760;
        int col2 = left + 390;

        g.fill(left, top, right, top + 520, PANEL);
        outline(g, left, top, right, top + 520, BORDER);

        g.text(font, "TastyFish Custom", left + 20, top + 18, TEXT, true);
        g.text(font, "Client-side SkyBlock item & armour customization", left + 20, top + 37, MUTED, false);
        g.text(font, TastyFishCustom.describe(TastyFishCustom.held(Minecraft.getInstance())),
                left + 20, top + 57, GREEN, false);

        g.text(font, "ITEM NAME", left + 20, top + 82, CYAN, true);
        g.text(font, "Plain text or a JSON text component", left + 20, top + 117, MUTED, false);

        g.text(font, "DYE COLOUR", left + 20, top + 167, CYAN, true);
        g.text(font, "HEX, e.g. 0061FF", left + 200, top + 167, MUTED, false);

        g.text(font, "ARMOUR TRIM", left + 20, top + 272, CYAN, true);
        g.text(font, "Material", left + 20, top + 273, MUTED, false);
        g.text(font, "Pattern", left + 200, top + 273, MUTED, false);

        g.text(font, "ANIMATED DYE", col2 + 20, top + 82, CYAN, true);
        g.text(font, "Colour 1", col2 + 20, top + 167, MUTED, false);
        g.text(font, "Colour 2", col2 + 190, top + 167, MUTED, false);
        g.text(font, "Duration (seconds)", col2 + 20, top + 167, MUTED, false);

        g.text(font, "The customization is saved locally by item UUID.", col2 + 20, top + 330, MUTED, false);
        g.text(font, "It does not modify the server item or send NBT changes.", col2 + 20, top + 350, MUTED, false);
        g.text(font, "Requires a Hypixel SkyBlock item UUID for persistence.", col2 + 20, top + 370, MUTED, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private static void outline(GuiGraphicsExtractor g, int l, int t, int r, int b, int c) {
        g.fill(l, t, r, t + 1, c); g.fill(l, b - 1, r, b, c);
        g.fill(l, t, l + 1, b, c); g.fill(r - 1, t, r, b, c);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}

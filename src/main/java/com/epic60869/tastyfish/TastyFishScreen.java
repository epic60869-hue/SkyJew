package com.epic60869.tastyfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public final class TastyFishScreen extends Screen {
    private static final int BG = 0xFF070A10;
    private static final int PANEL = 0xFF101722;
    private static final int BORDER = 0xFF263448;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF8794A8;
    private static final int CYAN = 0xFF58D8FF;
    private static final int PURPLE = 0xFF9A6CFF;
    private static final int YELLOW = 0xFFFFD34D;
    private static final int GREEN = 0xFF55E68A;
    private static final int RED = 0xFFFF667A;

    private final TastyFishConfig config;

    public TastyFishScreen(TastyFishConfig config) {
        super(Component.literal("TastyFish"));
        this.config = config;
    }

    @Override
    protected void init() {
        clearWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);
        g.fill(0, 0, width, 3, PURPLE);

        int panelWidth = Math.min(620, width - 40);
        int panelHeight = Math.min(560, height - 40);
        int left = (width - panelWidth) / 2;
        int top = Math.max(20, (height - panelHeight) / 2);

        panel(g, left, top, left + panelWidth, top + panelHeight);

        g.text(font, "✦", left + 24, top + 18, CYAN, true);
        g.text(font, "TastyFish", left + 50, top + 14, YELLOW, true);
        g.text(font, "SkyBlock utilities", left + 50, top + 31, MUTED, false);

        int x = left + 24;
        int right = left + panelWidth - 24;
        int y = top + 58;

        g.text(font, "General", x, y, TEXT, true);
        drawToggle(g, x, right, y + 20, "TastyFish enabled", config.enabled, mouseX, mouseY, CYAN);

        g.text(font, "Farming RNG", x, y + 75, TEXT, true);
        drawToggle(g, x, right, y + 95, "RNG HUD enabled", config.farmingRngEnabled, mouseX, mouseY, CYAN);
        drawToggle(g, x, right, y + 140, "RNG HUD background", config.farmingRngBackground, mouseX, mouseY, CYAN);
        drawValue(g, x, right, y + 185, "RNG HUD scale", String.format("%.1fx", config.farmingRngScale), mouseX, mouseY, CYAN);

        g.text(font, "Mouse Lock", x, y + 250, TEXT, true);
        drawToggle(g, x, right, y + 270, "Mouse Lock enabled", config.mouseLockEnabled, mouseX, mouseY, PURPLE);
        drawToggle(g, x, right, y + 315, "Ground only", config.mouseLockGroundOnly, mouseX, mouseY, PURPLE);

        g.text(font, "RNG HUD position", x, y + 370, TEXT, true);
        drawPosition(g, x, right, y + 390, "X", config.farmingRngX, mouseX, mouseY, CYAN);
        drawPosition(g, x, right, y + 435, "Y", config.farmingRngY, mouseX, mouseY, CYAN);

        g.text(font, "Click a setting to change it. ESC to close.", x, top + panelHeight - 24, MUTED, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawToggle(GuiGraphicsExtractor g, int left, int right, int top, String title,
                            boolean value, int mouseX, int mouseY, int color) {
        boolean hover = inside(mouseX, mouseY, left, right, top);
        g.fill(left, top, right, top + 36, hover ? 0xFF1B2636 : 0xFF151E2B);
        outline(g, left, top, right, top + 36, color);
        g.text(font, title, left + 14, top + 10, TEXT, false);
        g.text(font, value ? "ON" : "OFF", right - 48, top + 10, value ? GREEN : RED, true);
    }

    private void drawValue(GuiGraphicsExtractor g, int left, int right, int top, String title,
                           String value, int mouseX, int mouseY, int color) {
        boolean hover = inside(mouseX, mouseY, left, right, top);
        g.fill(left, top, right, top + 36, hover ? 0xFF1B2636 : 0xFF151E2B);
        outline(g, left, top, right, top + 36, color);
        g.text(font, title + "  (click to cycle)", left + 14, top + 10, TEXT, false);
        g.text(font, value, right - 48, top + 10, CYAN, true);
    }

    private void drawPosition(GuiGraphicsExtractor g, int left, int right, int top, String axis,
                              int value, int mouseX, int mouseY, int color) {
        boolean hover = inside(mouseX, mouseY, left, right, top);
        g.fill(left, top, right, top + 36, hover ? 0xFF1B2636 : 0xFF151E2B);
        outline(g, left, top, right, top + 36, color);
        g.text(font, "RNG HUD " + axis + "  (click: +8 / shift: -8)", left + 14, top + 10, TEXT, false);
        g.text(font, Integer.toString(value), right - 42, top + 10, CYAN, true);
    }

    private void panel(GuiGraphicsExtractor g, int left, int top, int right, int bottom) {
        g.fill(left, top, right, bottom, PANEL);
        outline(g, left, top, right, bottom, BORDER);
        g.fill(left, top, right, top + 2, PURPLE);
    }

    private void outline(GuiGraphicsExtractor g, int left, int top, int right, int bottom, int color) {
        g.fill(left, top, right, top + 1, color);
        g.fill(left, bottom - 1, right, bottom, color);
        g.fill(left, top, left + 1, bottom, color);
        g.fill(right - 1, top, right, bottom, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        int panelWidth = Math.min(620, width - 40);
        int panelHeight = Math.min(560, height - 40);
        int left = (width - panelWidth) / 2;
        int top = Math.max(20, (height - panelHeight) / 2);
        int x = left + 24;
        int right = left + panelWidth - 24;
        int y = top + 58;

        if (inside(event.x(), event.y(), x, right, y + 20)) {
            config.enabled = !config.enabled;
        } else if (inside(event.x(), event.y(), x, right, y + 95)) {
            config.farmingRngEnabled = !config.farmingRngEnabled;
        } else if (inside(event.x(), event.y(), x, right, y + 140)) {
            config.farmingRngBackground = !config.farmingRngBackground;
        } else if (inside(event.x(), event.y(), x, right, y + 185)) {
            config.farmingRngScale += Minecraft.getInstance().hasShiftDown() ? -0.1f : 0.1f;
            config.farmingRngScale = clampScale(config.farmingRngScale);
        } else if (inside(event.x(), event.y(), x, right, y + 270)) {
            config.mouseLockEnabled = !config.mouseLockEnabled;
        } else if (inside(event.x(), event.y(), x, right, y + 315)) {
            config.mouseLockGroundOnly = !config.mouseLockGroundOnly;
        } else if (inside(event.x(), event.y(), x, right, y + 390)) {
            config.farmingRngX = Math.max(0, config.farmingRngX + (Minecraft.getInstance().hasShiftDown() ? -8 : 8));
        } else if (inside(event.x(), event.y(), x, right, y + 435)) {
            config.farmingRngY = Math.max(0, config.farmingRngY + (event.shift() ? -8 : 8));
        } else {
            return super.mouseClicked(event, doubleClick);
        }

        save();
        return true;
    }

    private void save() {
        Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("tastyfish-mod.json");
        config.save(path);
    }

    private static float clampScale(float value) {
        return Math.max(0.5f, Math.min(3.0f, Math.round(value * 10.0f) / 10.0f));
    }

    private static boolean inside(double mx, double my, int left, int right, int top) {
        return mx >= left && mx <= right && my >= top && my <= top + 36;
    }
}

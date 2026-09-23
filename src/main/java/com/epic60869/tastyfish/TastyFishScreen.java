package com.epic60869.tastyfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

/** Main TastyFish utility menu. */
public final class TastyFishScreen extends Screen {
    private static final int BG = 0xFF070A10;
    private static final int PANEL = 0xFF101722;
    private static final int BORDER = 0xFF263448;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF8794A8;
    private static final int CYAN = 0xFF58D8FF;
    private static final int PURPLE = 0xFF9A6CFF;
    private static final int YELLOW = 0xFFFFD34D;

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

        int panelWidth = Math.min(520, width - 40);
        int left = (width - panelWidth) / 2;
        int top = Math.max(30, (height - 400) / 2);

        panel(g, left, top, left + panelWidth, top + 400);

        g.text(font, "✦", left + 24, top + 22, CYAN, true);
        g.text(font, "TastyFish", left + 50, top + 18, YELLOW, true);
        g.text(font, "Simple SkyBlock tools", left + 50, top + 35, MUTED, false);

        g.text(font, "Menu", left + 24, top + 78, TEXT, true);
        g.text(font, "Notes, storage search and Command Keys.", left + 24, top + 101, MUTED, false);

        int buttonLeft = left + 24;
        int buttonRight = left + panelWidth - 24;
        int buttonTop = top + 128;

        drawButton(g, buttonLeft, buttonRight, buttonTop, "✎", "Notes", "/tf notes",
                mouseX, mouseY, CYAN);
        drawButton(g, buttonLeft, buttonRight, buttonTop + 50, "⌕", "Storage Search",
                "/tf search  •  Ctrl+F", mouseX, mouseY, CYAN);
        drawButton(g, buttonLeft, buttonRight, buttonTop + 100, "⌨", "Command Keys",
                "/tf keys", mouseX, mouseY, PURPLE);
        drawButton(g, buttonLeft, buttonRight, buttonTop + 150, "✦", "Custom",
                "/tf custom", mouseX, mouseY, PURPLE);

        g.text(font, "Storage Search learns pages as you open them.", left + 24, top + 379, MUTED, false);
        g.text(font, "ESC to close", left + 24, top + 329, MUTED, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawButton(GuiGraphicsExtractor g, int left, int right, int top,
                            String icon, String title, String subtitle,
                            int mouseX, int mouseY, int outlineColor) {
        boolean hover = mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= top + 42;
        g.fill(left, top, right, top + 42, hover ? 0xFF5B3FC0 : 0xFF5136A8);
        outline(g, left, top, right, top + 42, outlineColor);
        g.text(font, icon, left + 18, top + 12, TEXT, true);
        g.text(font, title, left + 45, top + 8, TEXT, true);
        g.text(font, subtitle, left + 45, top + 24, MUTED, false);
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
        if (event.button() == 0) {
            int panelWidth = Math.min(520, width - 40);
            int left = (width - panelWidth) / 2;
            int top = Math.max(30, (height - 350) / 2);
            int buttonLeft = left + 24;
            int buttonRight = left + panelWidth - 24;
            int buttonTop = top + 128;

            if (inside(event, buttonLeft, buttonRight, buttonTop)) {
                Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
                Minecraft.getInstance().gui.setScreen(new TastyFishNotesScreen(configDir));
                return true;
            }
            if (inside(event, buttonLeft, buttonRight, buttonTop + 50)) {
                TastyFishStorageSearch.open(Minecraft.getInstance(), "");
                return true;
            }
            if (inside(event, buttonLeft, buttonRight, buttonTop + 100)) {
                Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
                Minecraft.getInstance().gui.setScreen(new TastyFishCommandKeysScreen(configDir));
                return true;
            }
            if (inside(event, buttonLeft, buttonRight, buttonTop + 150)) {
                TastyFishCustom.open(Minecraft.getInstance(), Minecraft.getInstance().gui.screen());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(MouseButtonEvent event, int left, int right, int top) {
        return event.x() >= left && event.x() <= right
                && event.y() >= top && event.y() <= top + 42;
    }
}

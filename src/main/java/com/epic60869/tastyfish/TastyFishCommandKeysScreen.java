package com.epic60869.tastyfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

/** Minimal TastyFish menu. */
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
        int top = Math.max(45, (height - 260) / 2);

        panel(g, left, top, left + panelWidth, top + 260);

        g.text(font, "✦", left + 24, top + 22, CYAN, true);
        g.text(font, "TastyFish", left + 50, top + 18, YELLOW, true);
        g.text(font, "Simple SkyBlock tools", left + 50, top + 35, MUTED, false);

        g.text(font, "Menu", left + 24, top + 78, TEXT, true);
        g.text(font, "The TastyFish menu is intentionally minimal.", left + 24, top + 101, MUTED, false);
        g.text(font, "More tools can be added here later without bringing", left + 24, top + 120, MUTED, false);
        g.text(font, "back the old feature-heavy control centre.", left + 24, top + 138, MUTED, false);

        int buttonLeft = left + 24;
        int buttonRight = left + panelWidth - 24;
        int buttonTop = top + 174;
        boolean hover = mouseX >= buttonLeft && mouseX <= buttonRight
            && mouseY >= buttonTop && mouseY <= buttonTop + 42;

        g.fill(buttonLeft, buttonTop, buttonRight, buttonTop + 42,
            hover ? 0xFF5B3FC0 : 0xFF5136A8);
        outline(g, buttonLeft, buttonTop, buttonRight, buttonTop + 42, CYAN);
        g.text(font, "✎", buttonLeft + 18, buttonTop + 12, TEXT, true);
        g.text(font, "Notes", buttonLeft + 45, buttonTop + 8, TEXT, true);
        g.text(font, "/tf notes", buttonLeft + 45, buttonTop + 24, MUTED, false);

        g.text(font, "ESC to close", left + 24, top + 239, MUTED, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
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
            int top = Math.max(45, (height - 260) / 2);
            int buttonLeft = left + 24;
            int buttonRight = left + panelWidth - 24;
            int buttonTop = top + 174;

            if (event.x() >= buttonLeft && event.x() <= buttonRight
                && event.y() >= buttonTop && event.y() <= buttonTop + 42) {
                Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
                Minecraft.getInstance().setScreen(new TastyFishNotesScreen(configDir));
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }
}

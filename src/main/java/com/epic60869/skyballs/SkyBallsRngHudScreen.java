package com.epic60869.skyballs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Simple SkyHanni-style HUD editor for the Farming RNG HUD. */
public final class SkyBallsRngHudScreen extends Screen {
    private final Screen parent;
    private boolean dragging;

    public SkyBallsRngHudScreen(Screen parent) {
        super(Component.literal("Farming RNG HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        addRenderableWidget(Button.builder(
            Component.literal("Enabled: " + (enabled() ? "ON" : "OFF")),
            b -> {
                setEnabled(!enabled());
                b.setMessage(Component.literal("Enabled: " + (enabled() ? "ON" : "OFF")));
            }).bounds(20, 20, 120, 22).build());

        addRenderableWidget(Button.builder(
            Component.literal("Background: " + (background() ? "ON" : "OFF")),
            b -> {
                setBackground(!background());
                b.setMessage(Component.literal("Background: " + (background() ? "ON" : "OFF")));
            }).bounds(148, 20, 135, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(width - 90, 20, 70, 22).build());
    }

    private boolean enabled() {
        return SkyBallsConfig.current().farming.rng.enabled;
    }

    private boolean background() {
        return SkyBallsConfig.current().farming.rng.background;
    }

    private void setEnabled(boolean value) {
        SkyBallsConfig.current().farming.rng.enabled = value;
        SkyBallsConfig.saveCurrent(SkyBallsConfig.current());
    }

    private void setBackground(boolean value) {
        SkyBallsConfig.current().farming.rng.background = value;
        SkyBallsConfig.saveCurrent(SkyBallsConfig.current());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF090B10);
        g.text(font, "Farming RNG HUD", 20, 58, 0xFFFFFFFF, true);
        g.text(font, "Drag the preview anywhere on the screen.", 20, 74, 0xFF9AA4B2, false);

        int x = SkyBallsRngHud.x();
        int y = SkyBallsRngHud.y();
        int previewX = Math.max(8, Math.min(width - 310, x));
        int previewY = Math.max(90, Math.min(height - 70, y));
        SkyBallsRngHud.renderPreview(g, previewX, previewY);

        g.text(font, "Position: " + previewX + ", " + previewY, 20, height - 25, 0xFF9AA4B2, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            int x = SkyBallsRngHud.x();
            int y = SkyBallsRngHud.y();
            if (event.x() >= x - 8 && event.x() <= x + SkyBallsRngHud.width()
                    && event.y() >= y - 8 && event.y() <= y + SkyBallsRngHud.height()) {
                dragging = true;
                SkyBallsRngHud.setPosition((int) event.x(), (int) event.y());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            SkyBallsRngHud.setPosition((int) event.x(), (int) event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            dragging = false;
            SkyBallsRngHud.setPosition((int) event.x(), (int) event.y());
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        SkyBallsConfig.saveCurrent(SkyBallsConfig.current());
        minecraft.gui.setScreen(parent);
    }

}

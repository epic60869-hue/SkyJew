package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class SkyJewHudEditorScreen extends Screen {
    private final Screen parent;
    private String dragging = "";
    private int dragOffsetX;
    private int dragOffsetY;

    public SkyJewHudEditorScreen(Screen parent) {
        super(Component.literal("SkyJew HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(width - 90, 18, 70, 24).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF090B10);
        g.text(font, Component.literal("SkyJew HUD Editor"), 20, 20, 0xFFFFFFFF, true);
        g.text(font, Component.literal("Drag the HUD elements to move them."), 20, 38, 0xFF9AA4B2, false);

        int rx = SkyJewRngHud.x();
        int ry = SkyJewRngHud.y();
        g.fill(rx - 5, ry - 5, rx + SkyJewRngHud.width() + 5, ry + SkyJewRngHud.height() + 5, 0x22000000);
        SkyJewRngHud.renderPreview(g, rx, ry);
        g.text(font, Component.literal("Farming RNG"), rx, ry - 16, 0xFFFFFFFF, true);

        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) {
            int px = config.pets.x;
            int py = config.pets.y;
            SkyJewNopoFeatures.renderPetHudPreview(g, px, py);
            g.text(font, Component.literal("Pet Display"), px, py - 16, 0xFFFFFFFF, true);
        }

        g.text(font, Component.literal("Farming RNG: " + rx + ", " + ry), 20, height - 42, 0xFF9AA4B2, false);
        if (config != null) {
            g.text(font, Component.literal("Pet Display: " + config.pets.x + ", " + config.pets.y), 20, height - 26, 0xFF9AA4B2, false);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        int mx = (int) event.x();
        int my = (int) event.y();
        int rx = SkyJewRngHud.x();
        int ry = SkyJewRngHud.y();
        if (mx >= rx - 10 && mx <= rx + SkyJewRngHud.width() + 10
                && my >= ry - 20 && my <= ry + SkyJewRngHud.height() + 10) {
            dragging = "rng";
            dragOffsetX = mx - rx;
            dragOffsetY = my - ry;
            return true;
        }

        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) {
            int px = config.pets.x;
            int py = config.pets.y;
            if (mx >= px - 10 && mx <= px + 260 && my >= py - 20 && my <= py + 45) {
                dragging = "pet";
                dragOffsetX = mx - px;
                dragOffsetY = my - py;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging.isEmpty()) return super.mouseDragged(event, dx, dy);
        int x = Math.max(4, Math.min(width - 300, (int) event.x() - dragOffsetX));
        int y = Math.max(55, Math.min(height - 60, (int) event.y() - dragOffsetY));

        if (dragging.equals("rng")) {
            SkyJewRngHud.setPosition(x, y);
        } else {
            SkyJewConfig config = SkyJewConfig.current();
            if (config != null) {
                config.pets.x = x;
                config.pets.y = y;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            dragging = "";
            SkyJewConfig config = SkyJewConfig.current();
            if (config != null) SkyJewConfig.saveCurrent(config);
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) SkyJewConfig.saveCurrent(config);
        minecraft.gui.setScreen(parent);
    }
}

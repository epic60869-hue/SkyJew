package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.Locale;

public final class SkyJewNickScreen extends Screen {
    private final Screen parent;
    private EditBox nameBox;
    private EditBox hexBox;
    private float hue = 0.78f;
    private float saturation = 1.0f;
    private float brightness = 1.0f;
    private boolean draggingWheel;

    public SkyJewNickScreen(Screen parent) {
        super(Component.literal("SJ Nickname"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null) return;

        nameBox = new EditBox(font, width / 2 - 110, height / 2 - 105, 300, 24, Component.literal("Nickname"));
        nameBox.setValue(config.misc.nickname.name == null ? "" : config.misc.nickname.name);
        nameBox.setMaxLength(32);
        addRenderableWidget(nameBox);

        String hex = config.misc.nickname.customHex;
        if (hex == null || !hex.matches("#[0-9a-fA-F]{6}")) hex = "#FFFFFF";
        hexBox = new EditBox(font, width / 2 - 110, height / 2 + 66, 100, 24, Component.literal("Hex"));
        hexBox.setValue(hex.toUpperCase(Locale.ROOT));
        hexBox.setMaxLength(7);
        hexBox.setResponder(this::applyHex);
        addRenderableWidget(hexBox);

        addRenderableWidget(Button.builder(
            Component.literal("Enabled: " + (config.misc.nickname.enabled ? "ON" : "OFF")),
            b -> {
                config.misc.nickname.enabled = !config.misc.nickname.enabled;
                b.setMessage(Component.literal("Enabled: " + (config.misc.nickname.enabled ? "ON" : "OFF")));
                SkyJewConfig.saveCurrent(config);
            }).bounds(width / 2 + 5, height / 2 + 66, 115, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Rainbow"),
            b -> {
                config.misc.nickname.style = "Rainbow";
                SkyJewConfig.saveCurrent(config);
            }).bounds(width / 2 + 125, height / 2 + 66, 90, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> saveAndClose())
            .bounds(width / 2 - 45, height / 2 + 102, 90, 24).build());

        syncFromHex(hex);
    }

    private void applyHex(String value) {
        if (value == null) return;
        String clean = value.trim();
        if (!clean.startsWith("#")) clean = "#" + clean;
        if (!clean.matches("#[0-9a-fA-F]{6}")) return;
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null) return;
        config.misc.nickname.customHex = clean.toUpperCase(Locale.ROOT);
        config.misc.nickname.style = "Plain";
        syncFromHex(clean);
    }

    private void syncFromHex(String hex) {
        try {
            int rgb = Integer.parseInt(hex.substring(1), 16);
            float[] hsb = Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null);
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
        } catch (Exception ignored) {}
    }

    private int rgb() {
        return Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
    }

    private void updateHex() {
        if (hexBox != null) {
            hexBox.setValue(String.format("#%06X", rgb()));
        }
        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) {
            config.misc.nickname.customHex = String.format("#%06X", rgb());
            config.misc.nickname.style = "Plain";
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF0B0C10);

        int w = 500;
        int h = 300;
        int left = (width - w) / 2;
        int top = (height - h) / 2;

        g.fill(left, top, left + w, top + h, 0xFF181A20);
        g.fill(left, top, left + w, top + 1, 0xFF3A3D46);
        g.fill(left, top + h - 1, left + w, top + h, 0xFF3A3D46);

        g.text(font, Component.literal("SJ Nickname"), left + 18, top + 18, 0xFFFFFFFF, true);
        g.text(font, Component.literal("Name"), left + 18, top + 44, 0xFFB5BAC1, false);

        int wheelX = left + 20;
        int wheelY = top + 82;
        int wheelSize = 140;
        drawWheel(g, wheelX, wheelY, wheelSize);

        int markerX = wheelX + (int)((0.5 + Math.cos(hue * Math.PI * 2) * saturation * 0.5) * wheelSize);
        int markerY = wheelY + (int)((0.5 - Math.sin(hue * Math.PI * 2) * saturation * 0.5) * wheelSize);
        g.fill(markerX - 4, markerY - 4, markerX + 4, markerY + 4, 0xFFFFFFFF);
        g.fill(markerX - 2, markerY - 2, markerX + 2, markerY + 2, 0xFF222222);

        int barX = wheelX + wheelSize + 10;
        drawHueBar(g, barX, wheelY, 18, wheelSize);
        drawBrightnessBar(g, barX + 28, wheelY, 18, wheelSize);

        g.fill(left + 20, top + 236, left + 48, top + 264, 0xFF000000 | rgb());
        g.text(font, Component.literal(String.format("#%06X", rgb())), left + 55, top + 245, 0xFFFFFFFF, false);

        g.text(font, Component.literal("Pick a color"), left + 210, top + 94, 0xFFF2F3F5, true);
        g.text(font, Component.literal("Wheel: hue + saturation"), left + 210, top + 116, 0xFFB5BAC1, false);
        g.text(font, Component.literal("Bars: hue + brightness"), left + 210, top + 132, 0xFFB5BAC1, false);
        g.text(font, Component.literal("Rainbow can be enabled separately."), left + 210, top + 158, 0xFFB5BAC1, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawWheel(GuiGraphicsExtractor g, int x, int y, int size) {
        int step = 4;
        double center = size / 2.0;
        double radius = center - 2;
        for (int py = 0; py < size; py += step) {
            for (int px = 0; px < size; px += step) {
                double dx = px + step / 2.0 - center;
                double dy = py + step / 2.0 - center;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > radius) continue;
                float sat = (float)(dist / radius);
                float h = (float)((Math.atan2(-dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0);
                int color = Color.HSBtoRGB(h, sat, 1.0f);
                g.fill(x + px, y + py, x + Math.min(px + step, size), y + Math.min(py + step, size),
                    0xFF000000 | (color & 0xFFFFFF));
            }
        }
    }

    private void drawHueBar(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        for (int i = 0; i < h; i += 3) {
            float value = 1.0f - i / (float)h;
            int color = Color.HSBtoRGB(value, 1.0f, 1.0f);
            g.fill(x, y + i, x + w, y + Math.min(i + 3, h), 0xFF000000 | (color & 0xFFFFFF));
        }
    }

    private void drawBrightnessBar(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        for (int i = 0; i < h; i += 3) {
            float value = 1.0f - i / (float)h;
            int color = Color.HSBtoRGB(hue, saturation, value);
            g.fill(x, y + i, x + w, y + Math.min(i + 3, h), 0xFF000000 | (color & 0xFFFFFF));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int w = 500;
        int h = 300;
        int left = (width - w) / 2;
        int top = (height - h) / 2;
        int wx = left + 20;
        int wy = top + 82;
        int ws = 140;

        if (inside(event.x(), event.y(), wx, wy, ws, ws)) {
            draggingWheel = true;
            updateWheel(event.x(), event.y(), wx, wy, ws);
            return true;
        }

        int bx = wx + ws + 10;
        if (inside(event.x(), event.y(), bx, wy, 18, ws)) {
            hue = clamp((float)((event.y() - wy) / ws), 0, 1);
            updateHex();
            return true;
        }

        if (inside(event.x(), event.y(), bx + 28, wy, 18, ws)) {
            brightness = clamp(1.0f - (float)((event.y() - wy) / ws), 0, 1);
            updateHex();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!draggingWheel) return super.mouseDragged(event, dx, dy);
        int w = 500;
        int h = 300;
        int left = (width - w) / 2;
        int top = (height - h) / 2;
        updateWheel(event.x(), event.y(), left + 20, top + 82, 140);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) draggingWheel = false;
        return super.mouseReleased(event);
    }

    private void updateWheel(double mx, double my, int x, int y, int size) {
        double dx = mx - (x + size / 2.0);
        double dy = my - (y + size / 2.0);
        double radius = size / 2.0 - 2;
        double dist = Math.min(radius, Math.sqrt(dx * dx + dy * dy));
        saturation = (float)(dist / radius);
        hue = (float)((Math.atan2(-dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0);
        updateHex();
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void saveAndClose() {
        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) {
            if (nameBox != null) config.misc.nickname.name = nameBox.getValue().trim();
            if (hexBox != null && hexBox.getValue().matches("#[0-9a-fA-F]{6}")) {
                config.misc.nickname.customHex = hexBox.getValue().toUpperCase(Locale.ROOT);
                if (!"Rainbow".equals(config.misc.nickname.style)) config.misc.nickname.style = "Plain";
            }
            config.misc.nickname.enabled = !config.misc.nickname.name.isBlank();
            SkyJewConfig.saveCurrent(config);
        }
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void onClose() {
        saveAndClose();
    }
}

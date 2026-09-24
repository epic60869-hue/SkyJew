package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class SkyJewHudEditorScreen extends Screen {
    private final Screen parent;
    private final List<EditableHud> elements = new ArrayList<>();
    private EditableHud selected;
    private int dragOffsetX;
    private int dragOffsetY;

    public SkyJewHudEditorScreen(Screen parent) {
        super(Component.literal("SkyJew Position Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(width - 90, 16, 70, 24).build());
        rebuildElements();
    }

    private void rebuildElements() {
        elements.clear();
        elements.add(new EditableHud("Farming RNG", SkyJewRngHud::x, SkyJewRngHud::y,
            SkyJewRngHud::setPosition, SkyJewRngHud::width, SkyJewRngHud::height,
            SkyJewRngHud::renderPreview, "rng"));
        elements.add(new EditableHud("Mining Commissions", SkyJewCommissionHud::x, SkyJewCommissionHud::y,
            SkyJewCommissionHud::setPosition, SkyJewCommissionHud::width, SkyJewCommissionHud::height,
            SkyJewCommissionHud::renderPreview, "commissions"));

        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) {
            elements.add(new EditableHud("Pet Display", () -> config.pets.x, () -> config.pets.y,
                (x, y) -> { config.pets.x = Math.max(0, x); config.pets.y = Math.max(0, y); },
                () -> 260, () -> 44, SkyJewNopoFeatures::renderPetHudPreview, "pet"));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF15171C);
        g.fill(width / 2 - 1, 0, width / 2 + 1, height, 0x182FFFFFF);
        g.fill(0, height / 2 - 1, width, height / 2 + 1, 0x182FFFFFF);

        g.text(font, Component.literal("SkyJew Position Editor"), 18, 18, 0xFFFFFFFF, true);
        g.text(font, Component.literal("Drag to move • Scroll to resize • Arrow keys move • Shift + arrows = 10px"), 18, 36, 0xFFB8BEC9, false);

        EditableHud hovered = null;
        for (int i = elements.size() - 1; i >= 0; i--) {
            EditableHud e = elements.get(i);
            int x = e.x(), y = e.y(), w = Math.max(5, e.width()), h = Math.max(5, e.height());
            boolean hover = inside(mouseX, mouseY, x - 5, y - 5, w + 10, h + 10);
            if (hover && hovered == null) hovered = e;

            int border = e == selected ? 0xFFE8EAED : (hover ? 0xFF8F98A6 : 0x664B515B);
            int fill = e == selected ? 0x28FFFFFF : (hover ? 0x18FFFFFF : 0x10000000);
            g.fill(x - 5, y - 5, x + w + 5, y + h + 5, fill);
            g.fill(x - 5, y - 5, x + w + 5, y - 4, border);
            g.fill(x - 5, y + h + 4, x + w + 5, y + h + 5, border);
            g.fill(x - 5, y - 5, x - 4, y + h + 5, border);
            g.fill(x + w + 4, y - 5, x + w + 5, y + h + 5, border);

            e.preview().render(g, x, y);
            if (e == selected || hover) {
                g.text(font, Component.literal(e.name()), x, Math.max(52, y - 17), 0xFFFFFFFF, true);
            }
        }

        if (hovered != null) {
            int tx = Math.min(mouseX + 12, width - 235);
            int ty = Math.min(mouseY + 12, height - 82);
            g.fill(tx, ty, tx + 225, ty + 66, 0xF0101115);
            g.text(font, Component.literal(hovered.name()), tx + 8, ty + 8, 0xFFFFFFFF, true);
            g.text(font, Component.literal("x: " + hovered.x() + ", y: " + hovered.y()), tx + 8, ty + 23, 0xFFD0D5DC, false);
            g.text(font, Component.literal("Left-click + drag to move"), tx + 8, ty + 37, 0xFF9EA5B1, false);
            g.text(font, Component.literal("Scroll to resize"), tx + 8, ty + 51, 0xFF9EA5B1, false);
        } else if (selected == null) {
            g.text(font, Component.literal("SkyJew Position Editor"), 18, height - 42, 0xFF9097A3, true);
            g.text(font, Component.literal("Select a HUD element to edit its position and scale."), 18, height - 26, 0xFF707783, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x(), my = (int) event.y();
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        EditableHud hovered = findHovered(mx, my);
        if (hovered == null) {
            selected = null;
            return super.mouseClicked(event, doubleClick);
        }

        selected = hovered;
        dragOffsetX = mx - hovered.x();
        dragOffsetY = my - hovered.y();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (selected == null || event.button() != 0) return super.mouseDragged(event, dx, dy);

        int x = Math.max(0, Math.min(width - Math.max(5, selected.width()), (int) event.x() - dragOffsetX));
        int y = Math.max(0, Math.min(height - Math.max(5, selected.height()), (int) event.y() - dragOffsetY));
        selected.setPosition(x, y);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) save();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        EditableHud hovered = selected != null ? selected : findHovered((int) mouseX, (int) mouseY);
        if (hovered != null && scrollY != 0) {
            hovered.changeScale(scrollY > 0 ? 0.1f : -0.1f);
            save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selected == null) return super.keyPressed(keyCode, scanCode, modifiers);
        int d = hasShiftDown() ? 10 : 1;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> selected.move(-d, 0);
            case GLFW.GLFW_KEY_RIGHT -> selected.move(d, 0);
            case GLFW.GLFW_KEY_UP -> selected.move(0, -d);
            case GLFW.GLFW_KEY_DOWN -> selected.move(0, d);
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> selected.changeScale(-0.1f);
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> selected.changeScale(0.1f);
            default -> { return super.keyPressed(keyCode, scanCode, modifiers); }
        }
        save();
        return true;
    }

    private EditableHud findHovered(int mx, int my) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            EditableHud e = elements.get(i);
            if (inside(mx, my, e.x() - 5, e.y() - 5, e.width() + 10, e.height() + 10)) return e;
        }
        return null;
    }

    private static boolean inside(int x, int y, int l, int t, int w, int h) {
        return x >= l && x <= l + w && y >= t && y <= t + h;
    }

    private void save() {
        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) SkyJewConfig.saveCurrent(config);
    }

    @Override
    public void onClose() {
        save();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @FunctionalInterface
    private interface Preview { void render(GuiGraphicsExtractor g, int x, int y); }

    @FunctionalInterface
    private interface PositionSetter { void set(int x, int y); }

    private final class EditableHud {
        private final String name, type;
        private final IntSupplier x, y, width, height;
        private final PositionSetter setter;
        private final Preview preview;

        EditableHud(String name, IntSupplier x, IntSupplier y, PositionSetter setter,
                    IntSupplier width, IntSupplier height, Preview preview, String type) {
            this.name = name; this.x = x; this.y = y; this.setter = setter;
            this.width = width; this.height = height; this.preview = preview; this.type = type;
        }

        String name() { return name; }
        int x() { return x.getAsInt(); }
        int y() { return y.getAsInt(); }
        int width() { return width.getAsInt(); }
        int height() { return height.getAsInt(); }
        Preview preview() { return preview; }

        void setPosition(int x, int y) { setter.set(x, y); }
        void move(int dx, int dy) { setPosition(x() + dx, y() + dy); }

        void changeScale(float d) {
            SkyJewConfig c = SkyJewConfig.current();
            if (c == null) return;
            switch (type) {
                case "rng" -> c.farming.rng.scale = clamp(c.farming.rng.scale + d);
                case "commissions" -> c.farming.commissions.scale = clamp(c.farming.commissions.scale + d);
                case "pet" -> c.pets.scale = clamp(c.pets.scale + d);
            }
        }

        private float clamp(float v) { return Math.max(0.5f, Math.min(3.0f, v)); }
    }

    @FunctionalInterface
    private interface IntSupplier { int getAsInt(); }
}

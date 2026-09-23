package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

public final class SkyJewNickScreen extends Screen {
    private static final int BG = 0xFF070A10;
    private static final int PANEL = 0xFF101722;
    private static final int BORDER = 0xFF263448;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF8794A8;
    private static final int PURPLE = 0xFF9A6CFF;
    private static final int GREEN = 0xFF55E68A;
    private static final int RED = 0xFFFF667A;

    private static final List<ColorOption> COLORS = List.of(
        new ColorOption("Black", "black", 0xFF000000),
        new ColorOption("Dark Blue", "dark_blue", 0xFF0000AA),
        new ColorOption("Dark Green", "dark_green", 0xFF00AA00),
        new ColorOption("Dark Aqua", "dark_aqua", 0xFF00AAAA),
        new ColorOption("Dark Red", "dark_red", 0xFFAA0000),
        new ColorOption("Dark Purple", "dark_purple", 0xFFAA00AA),
        new ColorOption("Gold", "gold", 0xFFFFAA00),
        new ColorOption("Gray", "gray", 0xFFAAAAAA),
        new ColorOption("Dark Gray", "dark_gray", 0xFF555555),
        new ColorOption("Blue", "blue", 0xFF5555FF),
        new ColorOption("Green", "green", 0xFF55FF55),
        new ColorOption("Aqua", "aqua", 0xFF55FFFF),
        new ColorOption("Red", "red", 0xFFFF5555),
        new ColorOption("Light Purple", "light_purple", 0xFFFF55FF),
        new ColorOption("Yellow", "yellow", 0xFFFFFF55),
        new ColorOption("White", "white", 0xFFFFFFFF)
    );

    private final SkyJewConfig config;
    private final Screen previousScreen;
    private EditBox nameBox;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public SkyJewNickScreen(SkyJewConfig config, Screen previousScreen) {
        super(Component.literal("SkyJew Nickname"));
        this.config = config;
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        super.init();
        rebuildLayout();

        nameBox = new EditBox(font, panelX + 28, panelY + 92, panelW - 56, 30,
            Component.literal("Nickname"));
        nameBox.setMaxLength(32);
        nameBox.setValue(config.nickName == null ? "" : config.nickName);
        nameBox.setHint(Component.literal("Enter the name you want to use"));
        addRenderableWidget(nameBox);
    }

    private void rebuildLayout() {
        panelW = Math.min(620, width - 40);
        panelH = Math.min(500, height - 40);
        panelX = (width - panelW) / 2;
        panelY = Math.max(20, (height - panelH) / 2);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        rebuildLayout();

        g.fill(0, 0, width, height, BG);
        g.fill(0, 0, width, 3, PURPLE);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        outline(g, panelX, panelY, panelX + panelW, panelY + panelH, BORDER);
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, PURPLE);

        super.extractRenderState(g, mouseX, mouseY, delta);

        g.text(font, "✦  Nickname", panelX + 24, panelY + 18, 0xFFFFD34D, true);
        g.text(font, "Choose your display name, enable it, then pick a colour.", panelX + 24,
            panelY + 38, MUTED, false);

        g.text(font, "Name", panelX + 28, panelY + 76, TEXT, true);

        int toggleY = panelY + 136;
        boolean hoverToggle = inside(mouseX, mouseY, panelX + 28, panelX + panelW - 28, toggleY, 36);
        g.fill(panelX + 28, toggleY, panelX + panelW - 28, toggleY + 36,
            hoverToggle ? 0xFF1B2636 : 0xFF151E2B);
        outline(g, panelX + 28, toggleY, panelX + panelW - 28, toggleY + 36, PURPLE);
        g.text(font, "Nickname enabled", panelX + 42, toggleY + 10, TEXT, false);
        g.text(font, config.nickEnabled ? "ON" : "OFF",
            panelX + panelW - 72, toggleY + 10,
            config.nickEnabled ? GREEN : RED, true);

        g.text(font, "Colour", panelX + 28, panelY + 188, TEXT, true);

        int gridX = panelX + 28;
        int gridY = panelY + 210;
        int cellW = (panelW - 56 - 15) / 4;
        int cellH = 34;

        for (int i = 0; i < COLORS.size(); i++) {
            ColorOption option = COLORS.get(i);
            int col = i % 4;
            int row = i / 4;
            int x = gridX + col * (cellW + 5);
            int y = gridY + row * (cellH + 5);
            boolean hover = inside(mouseX, mouseY, x, y, cellW, cellH);
            boolean selected = option.mode.equals(config.nickMode);

            g.fill(x, y, x + cellW, y + cellH, hover ? 0xFF263243 : 0xFF151E2B);
            outline(g, x, y, x + cellW, y + cellH, selected ? option.rgb : 0xFF263448);
            g.fill(x + 7, y + 9, x + 21, y + 23, option.rgb);
            g.text(font, option.name, x + 28, y + 10, TEXT, false);
        }

        int rainbowY = gridY + 4 * (cellH + 5);
        boolean rainbowHover = inside(mouseX, mouseY, panelX + 28, rainbowY,
            panelW - 56, cellH);
        boolean rainbowSelected = "rainbow".equals(config.nickMode);
        g.fill(panelX + 28, rainbowY, panelX + panelW - 28, rainbowY + cellH,
            rainbowHover ? 0xFF263243 : 0xFF151E2B);
        outline(g, panelX + 28, rainbowY, panelX + panelW - 28, rainbowY + cellH,
            rainbowSelected ? 0xFFFFFFFF : 0xFF263448);
        g.text(font, "🌈  Rainbow / Chroma", panelX + 42, rainbowY + 10, TEXT, true);

        int previewY = rainbowY + 48;
        g.text(font, "Preview:", panelX + 28, previewY, MUTED, false);
        String preview = nameBox == null ? "" : nameBox.getValue();
        if (preview.isBlank()) preview = Minecraft.getInstance().getUser().getName();
        g.text(font, SkyJewNick.styled(preview), panelX + 90, previewY, 0xFFFFFFFF, true);

        g.text(font, "Click a colour to select it. Click the toggle to enable/disable.",
            panelX + 28, panelY + panelH - 42, MUTED, false);
        g.text(font, "ESC to close", panelX + panelW - 92, panelY + panelH - 24, MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        int mx = (int) event.x();
        int my = (int) event.y();

        int toggleY = panelY + 136;
        if (inside(mx, my, panelX + 28, panelY + 136, panelW - 56, 36)) {
            config.nickEnabled = !config.nickEnabled;
            save();
            return true;
        }

        int gridX = panelX + 28;
        int gridY = panelY + 210;
        int cellW = (panelW - 56 - 15) / 4;
        int cellH = 34;

        for (int i = 0; i < COLORS.size(); i++) {
            int col = i % 4;
            int row = i / 4;
            int x = gridX + col * (cellW + 5);
            int y = gridY + row * (cellH + 5);
            if (inside(mx, my, x, y, cellW, cellH)) {
                applyName();
                config.nickMode = COLORS.get(i).mode;
                config.nickColor = COLORS.get(i).mode;
                save();
                return true;
            }
        }

        int rainbowY = gridY + 4 * (cellH + 5);
        if (inside(mx, my, panelX + 28, rainbowY, panelW - 56, cellH)) {
            applyName();
            config.nickMode = "rainbow";
            config.nickColor = "";
            save();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void applyName() {
        if (nameBox == null) return;
        String name = nameBox.getValue().replaceAll("[\\r\\n]", "").trim();
        if (name.length() > 32) name = name.substring(0, 32);
        config.nickName = name;
    }

    private void save() {
        applyName();
        Path path = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("skyjew-mod.json");
        config.save(path);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static void outline(GuiGraphicsExtractor g, int left, int top, int right, int bottom, int color) {
        g.fill(left, top, right, top + 1, color);
        g.fill(left, bottom - 1, right, bottom, color);
        g.fill(left, top, left + 1, bottom, color);
        g.fill(right - 1, top, right, bottom, color);
    }

    private record ColorOption(String name, String mode, int rgb) {}
}

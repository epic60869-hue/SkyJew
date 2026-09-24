package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SkyJewNickScreen extends Screen {
    private static final Map<String, Integer> COLORS = new LinkedHashMap<>();
    static {
        COLORS.put("Black", 0x000000);
        COLORS.put("Dark Blue", 0x0000AA);
        COLORS.put("Dark Green", 0x00AA00);
        COLORS.put("Dark Aqua", 0x00AAAA);
        COLORS.put("Dark Red", 0xAA0000);
        COLORS.put("Dark Purple", 0xAA00AA);
        COLORS.put("Gold", 0xFFAA00);
        COLORS.put("Gray", 0xAAAAAA);
        COLORS.put("Dark Gray", 0x555555);
        COLORS.put("Blue", 0x5555FF);
        COLORS.put("Green", 0x55FF55);
        COLORS.put("Aqua", 0x55FFFF);
        COLORS.put("Red", 0xFF5555);
        COLORS.put("Light Purple", 0xFF55FF);
        COLORS.put("Yellow", 0xFFFF55);
        COLORS.put("White", 0xFFFFFF);
    }

    private final Screen parent;
    private EditBox nameBox;
    private boolean enabled;
    private boolean seeOtherNicks;
    private String selectedStyle = "Plain";

    public SkyJewNickScreen(Screen parent) {
        super(Component.literal("SkyJew Nick"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null) return;

        int w = Math.min(760, width - 30);
        int left = (width - w) / 2;
        int top = Math.max(12, (height - 430) / 2);

        nameBox = new EditBox(font, left + 24, top + 78, 330, 24, Component.literal("Nickname"));
        nameBox.setValue(config.misc.nickname.name == null ? "" : config.misc.nickname.name);
        nameBox.setMaxLength(32);
        addRenderableWidget(nameBox);

        enabled = config.misc.nickname.enabled;
        selectedStyle = config.misc.nickname.style == null ? "Plain" : config.misc.nickname.style;
        seeOtherNicks = config.misc.nickname.seeOtherNicks;

        addRenderableWidget(Button.builder(Component.literal(enabled ? "TAB nickname: ON" : "TAB nickname: OFF"), b -> {
            enabled = !enabled;
            b.setMessage(Component.literal(enabled ? "TAB nickname: ON" : "TAB nickname: OFF"));
        }).bounds(left + 370, top + 78, 180, 24).build());

        addRenderableWidget(Button.builder(Component.literal(seeOtherNicks ? "See Other Nicks: ON" : "See Other Nicks: OFF"), b -> {
            seeOtherNicks = !seeOtherNicks;
            b.setMessage(Component.literal(seeOtherNicks ? "See Other Nicks: ON" : "See Other Nicks: OFF"));
        }).bounds(left + 560, top + 78, 180, 24).build());

        int gridX = left + 24;
        int gridY = top + 140;
        int col = 0, row = 0;
        for (String style : COLORS.keySet()) {
            int bx = gridX + col * 118;
            int by = gridY + row * 31;
            addRenderableWidget(Button.builder(Component.literal(style), b -> selectedStyle = style)
                .bounds(bx, by, 112, 25).build());

            col++;
            if (col == 4) {
                col = 0;
                row++;
            }
        }

        int bottom = gridY + ((COLORS.size() + 3) / 4) * 31 + 8;
        addRenderableWidget(Button.builder(Component.literal("Disable"), b -> {
            nameBox.setValue("");
            enabled = false;
        }).bounds(left + 24, bottom, 112, 25).build());

        addRenderableWidget(Button.builder(Component.literal("Reset colour"), b -> selectedStyle = "Plain")
            .bounds(left + 142, bottom, 112, 25).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> minecraft.gui.setScreen(parent))
            .bounds(left + w - 238, bottom, 105, 25).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose())
            .bounds(left + w - 125, bottom, 105, 25).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int w = Math.min(760, width - 30);
        int h = Math.min(430, height - 24);
        int left = (width - w) / 2;
        int top = (height - h) / 2;

        g.fill(0, 0, width, height, 0xFF07090D);
        g.fill(left, top, left + w, top + h, 0xFF181B21);
        g.fill(left, top, left + w, top + 2, 0xFF55FFFF);

        g.text(font, Component.literal("SkyJew Nickname"), left + 24, top + 18, 0xFFFFFFFF, true);
        g.text(font, Component.literal("Pick a colour — no HEX code required"), left + 24, top + 39, 0xFF9EA6B2, false);

        SkyJewConfig config = SkyJewConfig.current();
        String previewName = nameBox == null || nameBox.getValue().isBlank()
            ? (config == null || config.misc.nickname.name == null || config.misc.nickname.name.isBlank()
                ? MinecraftName() : config.misc.nickname.name)
            : nameBox.getValue();

        Component preview = "Rainbow".equalsIgnoreCase(selectedStyle)
            ? SkyJewNick.styled(previewName, "Rainbow", "")
            : SkyJewNick.styled(previewName, selectedStyle, "");

        int previewX = left + 500;
        g.text(font, Component.literal("TAB preview"), previewX, top + 125, 0xFFB8C0CC, true);
        g.text(font, Component.literal("[MVP++] "), previewX, top + 151, 0xFF55FFFF, false);
        g.text(font, preview, previewX + 55, top + 151, 0xFFFFFFFF, false);

        g.text(font, Component.literal("Selected: " + selectedStyle), left + 24, top + 118, 0xFFE5E7EB, true);

        int gridX = left + 24;
        int gridY = top + 140;
        int col = 0, row = 0;
        for (String style : COLORS.keySet()) {
            int bx = gridX + col * 118;
            int by = gridY + row * 31;
            int rgb = COLORS.get(style);
            g.fill(bx + 4, by + 8, bx + 10, by + 14, 0xFF000000 | rgb);
            if (style.equals(selectedStyle)) {
                g.fill(bx + 1, by + 1, bx + 3, by + 24, 0xFFFFFFFF);
            }
            col++;
            if (col == 4) {
                col = 0;
                row++;
            }
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private String MinecraftName() {
        return minecraft == null ? "YourName" : minecraft.getUser().getName();
    }

    private void saveAndClose() {
        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) {
            String name = nameBox.getValue().trim();
            config.misc.nickname.name = name;
            config.misc.nickname.enabled = enabled && !name.isBlank();
            config.misc.nickname.style = selectedStyle;
            config.misc.nickname.customHex = "";
            SkyJewConfig.saveCurrent(config);
            SkyJewGlobalChat.sendNicknameUpdate();
        }
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void onClose() {
        saveAndClose();
    }
}

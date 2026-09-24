package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class SkyJewNickScreen extends Screen {
    private final Screen parent;
    private EditBox nameBox, hexBox;
    private Checkbox enabledBox;

    public SkyJewNickScreen(Screen parent) {
        super(Component.literal("SkyJew Nick"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null) return;

        int left = width / 2 - 250;
        int top = height / 2 - 145;

        nameBox = new EditBox(font, left + 22, top + 68, 300, 24, Component.literal("Nickname"));
        nameBox.setValue(config.misc.nickname.name == null ? "" : config.misc.nickname.name);
        nameBox.setMaxLength(32);
        addRenderableWidget(nameBox);

        enabledBox = Checkbox.builder(Component.literal("Enable nickname in TAB"), font)
            .pos(left + 22, top + 100).selected(config.misc.nickname.enabled).build();
        addRenderableWidget(enabledBox);

        addRenderableWidget(Button.builder(Component.literal("Plain"), b -> setStyle("Plain"))
            .bounds(left + 22, top + 132, 72, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Rainbow"), b -> setStyle("Rainbow"))
            .bounds(left + 100, top + 132, 90, 22).build());

        hexBox = new EditBox(font, left + 200, top + 132, 122, 22, Component.literal("#RRGGBB"));
        String hex = config.misc.nickname.customHex;
        hexBox.setValue(hex != null && hex.matches("#[0-9a-fA-F]{6}") ? hex.toUpperCase(Locale.ROOT) : "#FFFFFF");
        hexBox.setMaxLength(7);
        addRenderableWidget(hexBox);

        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
            nameBox.setValue("");
            enabledBox.onPress();
        }).bounds(left + 22, top + 165, 72, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose())
            .bounds(left + 232, top + 165, 90, 22).build());
    }

    private void setStyle(String style) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) config.misc.nickname.style = style;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF080A0E);
        int w = 500, h = 300, left = (width - w) / 2, top = (height - h) / 2;
        g.fill(left, top, left + w, top + h, 0xFF171A20);
        g.fill(left, top, left + w, top + 2, 0xFF55FFFF);

        g.text(font, Component.literal("SkyJew Nick"), left + 22, top + 18, 0xFFFFFFFF, true);
        g.text(font, Component.literal("Your Hypixel rank stays intact; only your username text is replaced."),
            left + 22, top + 38, 0xFF9EA5B2, false);
        g.text(font, Component.literal("Nickname"), left + 22, top + 55, 0xFFE5E7EB, true);

        SkyJewConfig config = SkyJewConfig.current();
        String preview = config == null || config.misc.nickname.name == null || config.misc.nickname.name.isBlank()
            ? "2m3s" : config.misc.nickname.name;
        Component previewComp = SkyJewNick.styled(preview,
            config == null ? "Plain" : config.misc.nickname.style,
            config == null ? "" : config.misc.nickname.customHex);

        g.text(font, Component.literal("TAB preview"), left + 350, top + 72, 0xFFB8C0CC, true);
        g.text(font, Component.literal("[MVP++] "), left + 350, top + 100, 0xFF55FFFF, false);
        g.text(font, previewComp, left + 405, top + 100, 0xFFFFFFFF, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void saveAndClose() {
        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) {
            config.misc.nickname.name = nameBox.getValue().trim();
            config.misc.nickname.enabled = enabledBox.selected() && !config.misc.nickname.name.isBlank();
            String hex = hexBox.getValue().trim();
            if (!hex.startsWith("#")) hex = "#" + hex;
            if (hex.matches("#[0-9a-fA-F]{6}")) config.misc.nickname.customHex = hex.toUpperCase(Locale.ROOT);
            SkyJewConfig.saveCurrent(config);
            SkyJewGlobalChat.sendNicknameUpdate();
        }
        minecraft.gui.setScreen(parent);
    }

    @Override public void onClose() { saveAndClose(); }
}

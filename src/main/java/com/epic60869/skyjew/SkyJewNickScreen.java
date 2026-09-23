package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Dedicated nickname editor opened by /sj nick and /skyjew nick. */
public final class SkyJewNickScreen extends Screen {
    private static final List<String> STYLES = List.of(
        "Plain", "Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red",
        "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua",
        "Red", "Light Purple", "Yellow", "White", "Rainbow"
    );

    private final Screen parent;
    private EditBox name;
    private int styleIndex;

    public SkyJewNickScreen(Screen parent) {
        super(Component.literal("SkyJew Nickname"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null) return;

        name = new EditBox(font, width / 2 - 160, height / 2 - 55, 320, 22, Component.literal("Nickname"));
        name.setValue(config.misc.nickname.name == null ? "" : config.misc.nickname.name);
        name.setMaxLength(32);
        addRenderableWidget(name);

        styleIndex = Math.max(0, STYLES.indexOf(config.misc.nickname.style));
        if (styleIndex < 0) styleIndex = 0;

        addRenderableWidget(Button.builder(
            Component.literal("Enabled: " + (config.misc.nickname.enabled ? "ON" : "OFF")),
            b -> {
                config.misc.nickname.enabled = !config.misc.nickname.enabled;
                b.setMessage(Component.literal("Enabled: " + (config.misc.nickname.enabled ? "ON" : "OFF")));
                SkyJewConfig.saveCurrent(config);
            }).bounds(width / 2 - 160, height / 2 - 20, 100, 22).build());

        addRenderableWidget(Button.builder(
            Component.literal("Style: " + STYLES.get(styleIndex)),
            b -> {
                styleIndex = (styleIndex + 1) % STYLES.size();
                config.misc.nickname.style = STYLES.get(styleIndex);
                b.setMessage(Component.literal("Style: " + STYLES.get(styleIndex)));
                SkyJewConfig.saveCurrent(config);
            }).bounds(width / 2 - 50, height / 2 - 20, 150, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose())
            .bounds(width / 2 + 110, height / 2 - 20, 50, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> saveAndClose())
            .bounds(width / 2 - 50, height / 2 + 20, 100, 22).build());
    }

    private void saveAndClose() {
        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) {
            config.misc.nickname.name = name == null ? "" : name.getValue().trim();
            SkyJewConfig.saveCurrent(config);
        }
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF080B12);
        int w = 390;
        int left = (width - w) / 2;
        int top = height / 2 - 95;
        g.fill(left, top, left + w, top + 190, 0xFF141B27);
        g.text(font, "SkyJew Nickname", left + 20, top + 18, 0xFFFFD34D, true);
        g.text(font, "This name is used in SkyJew global chat.", left + 20, top + 35, 0xFF8D9AAF, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        saveAndClose();
    }
}

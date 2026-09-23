package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class SkyJewFirstBootScreen extends Screen {
    private static final Identifier WARNING_TEXTURE =
        Identifier.fromNamespaceAndPath("skyjew-mod", "textures/gui/first_boot_ratted.png");

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 32;

    private final SkyJewConfig config;
    private final Screen previousScreen;
    private boolean audioStarted;

    public SkyJewFirstBootScreen(SkyJewConfig config, Screen previousScreen) {
        super(Component.literal("SkyJew"));
        this.config = config;
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        if (!audioStarted) {
            audioStarted = true;
            SkyJewSounds.playFirstBoot();
        }
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.fill(0, 0, width, height, 0xFF000000);

        int imageWidth = Math.min(width - 40, 1024);
        int imageHeight = imageWidth * 144 / 256;
        if (imageHeight > height / 2) {
            imageHeight = height / 2;
            imageWidth = imageHeight * 256 / 144;
        }

        int imageX = (width - imageWidth) / 2;
        int imageY = Math.max(20, height / 2 - imageHeight - 95);

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            WARNING_TEXTURE,
            imageX, imageY,
            0, 0,
            imageWidth, imageHeight,
            256, 144
        );

        Component warning = Component.literal("YOU HAVE BEEN RATTED LOL");
        int warningWidth = font.width(warning);
        int warningX = (width - warningWidth) / 2;
        int warningY = imageY + imageHeight + 18;

        graphics.text(font, warning, warningX, warningY, 0xFFFFFFFF, true);

        int buttonX = (width - BUTTON_WIDTH) / 2;
        int buttonY = warningY + 38;
        boolean hovered = mouseX >= buttonX && mouseX < buttonX + BUTTON_WIDTH
            && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT;

        int buttonColor = hovered ? 0xFF707070 : 0xFF555555;
        graphics.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT, buttonColor);
        graphics.outline(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, 0xFFAAAAAA);

        Component buttonText = Component.literal("I Understand");
        int textWidth = font.width(buttonText);
        graphics.text(
            font,
            buttonText,
            buttonX + (BUTTON_WIDTH - textWidth) / 2,
            buttonY + 10,
            0xFFFFFFFF,
            true
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return true;

        int imageWidth = Math.min(width - 40, 1024);
        int imageHeight = imageWidth * 144 / 256;
        if (imageHeight > height / 2) {
            imageHeight = height / 2;
            imageWidth = imageHeight * 256 / 144;
        }

        int imageY = Math.max(20, height / 2 - imageHeight - 95);
        int warningY = imageY + imageHeight + 18;
        int buttonX = (width - BUTTON_WIDTH) / 2;
        int buttonY = warningY + 38;

        if (event.x() >= buttonX && event.x() < buttonX + BUTTON_WIDTH
            && event.y() >= buttonY && event.y() < buttonY + BUTTON_HEIGHT) {

            SkyJewSounds.stopFirstBoot();
            config.general.firstBootAcknowledged = true;
            SkyJewConfig.saveCurrent(config);
            minecraft.gui.setScreen(previousScreen);
            return true;
        }

        return true;
    }

    @Override
    public void removed() {
        SkyJewSounds.stopFirstBoot();
        super.removed();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}

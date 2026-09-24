package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

/**
 * Skyblocker-inspired dye picker.
 *
 * Static and animated dye data comes directly from the Hypixel/NEU dye
 * definitions, so new dyes do not require a SkyJew release to be added.
 */
public final class SkyJewDyeSelectScreen extends Screen {
    private final Screen parent;
    private final net.minecraft.world.item.ItemStack item;

    private ScrollableLayout list;
    private StringWidget title;
    private Button close;
    private Button applyColor;
    private EditBox hex;

    public SkyJewDyeSelectScreen(Screen parent, net.minecraft.world.item.ItemStack item) {
        super(Component.literal("SkyJew Dye Selection"));
        this.parent = parent;
        this.item = item;
    }

    @Override
    protected void init() {
        hex = new EditBox(font, 0, 0, 150, 20, Component.literal("Any HEX colour"));
        hex.setHint(Component.literal("#RRGGBB"));
        hex.setMaxLength(7);
        addRenderableWidget(hex);

        addRenderableWidget(applyColor = Button.builder(Component.literal("Apply Colour"), b -> applyCustom()).bounds(0, 0, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).width(75).build());

        LinearLayout layout = LinearLayout.vertical().spacing(3);
        layout.defaultCellSetting().alignHorizontallyCenter();

        layout.addChild(new StringWidget(Component.literal("Any Colour"), font),
            layout.defaultCellSetting().copy().paddingBottom(3));

        if (!SkyJewCustom.dyeDataLoaded()) {
            layout.addChild(new StringWidget(Component.literal("Loading Hypixel dye data..."), font));
        } else {
            layout.addChild(new StringWidget(Component.literal("Hypixel Static Dyes"), font),
                layout.defaultCellSetting().copy().paddingBottom(2));

            for (Map.Entry<String, Integer> entry : SkyJewCustom.hypixelStaticDyes().entrySet()) {
                layout.addChild(new DyeButton(SkyJewCustom.dyeDisplayName(entry.getKey()), List.of(entry.getValue()),
                    b -> applyStatic(entry.getValue())));
            }

            layout.addChild(SpacerElement.height(10));
            layout.addChild(new StringWidget(Component.literal("Hypixel Animated Dyes"), font),
                layout.defaultCellSetting().copy().paddingBottom(2));

            for (Map.Entry<String, List<Integer>> entry : SkyJewCustom.hypixelAnimatedDyes().entrySet()) {
                layout.addChild(new DyeButton(SkyJewCustom.dyeDisplayName(entry.getKey()), entry.getValue(),
                    b -> applyAnimated(entry.getValue())));
            }
        }

        list = new ScrollableLayout(minecraft, layout, 0);
        list.visitWidgets(this::addRenderableWidget);
        addRenderableWidget(title = new StringWidget(Component.literal("Pick a Dye"), font));
        addRenderableWidget(close = Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).width(75).build());

        repositionElements();
    }

    private void applyCustom() {
        try {
            int color = SkyJewCustom.parseHex(hex.getValue());
            SkyJewCustom.setDye(item, color);
            SkyJewCustom.setAnimatedDye(item, null, null, 1f, false, 0f);
            onClose();
        } catch (IllegalArgumentException ignored) {
            hex.setValue("#FF00FF");
            hex.setCursorPosition(7);
        }
    }

    private void applyStatic(int color) {
        SkyJewCustom.setAnimatedDye(item, null, null, 1f, false, 0f);
        SkyJewCustom.setDye(item, color);
        onClose();
    }

    private void applyAnimated(List<Integer> colors) {
        SkyJewCustom.setDye(item, null);
        SkyJewCustom.setAnimatedDye(item, colors, 10f, colors.size() % 2 == 0, 0f);
        onClose();
    }

    @Override
    protected void repositionElements() {
        if (list == null) return;
        list.setMaxHeight(Math.min(330, (int) (height * 0.68)));
        list.arrangeElements();
        list.setPosition((width - list.getWidth()) / 2, Math.max(55, (height - list.getHeight()) / 2));

        title.setPosition((width - title.getWidth()) / 2, list.getY() - 28);
        close.setPosition((width - close.getWidth()) / 2, list.getY() + list.getHeight() + 10);

        hex.setPosition(width / 2 - 155, 22);
        applyColor.setPosition(width / 2 + 5, 22);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xFF07090D);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private static class DyeButton extends Button.Plain {
        private final List<Integer> colors;
        private int index;
        private float elapsed;

        DyeButton(String name, List<Integer> colors, OnPress onPress) {
            super(0, 0, 220, 20, Component.empty(), onPress, _ -> Component.empty());
            this.colors = colors;
            int color = colors.isEmpty() ? 0xFFFFFF : colors.getFirst();
            setMessage(Component.literal(name).withColor(color));
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            extractDefaultSprite(graphics);
            int color = colors.isEmpty() ? 0xFFFFFF : colors.get(index);
            graphics.fill(getX() + 5, getY() + 5, getX() + 19, getY() + 19, 0xFF000000 | color);

            if (colors.size() > 1) {
                elapsed += delta;
                if (elapsed >= 2f) {
                    elapsed = 0f;
                    index = (index + 1) % colors.size();
                    setMessage(Component.literal(getMessage().getString()).withColor(colors.get(index)));
                }
            }
            graphics.text(Minecraft.getInstance().font, getMessage(), getX() + 25, getY() + 6, 0xFFFFFFFF, false);
        }
    }
}

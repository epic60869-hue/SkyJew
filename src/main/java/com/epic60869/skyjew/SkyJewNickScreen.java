package com.epic60869.skyjew;

import com.epic60869.skyjew.custom.util.ARGBTextInput;
import com.epic60869.skyjew.custom.util.ColorPickerWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * /sj nick, laid out like /sj custom: a tab bar at the top, Cancel / Done at the bottom, and panels in between.
 * The left panel has the name and toggles, the middle a grid of colour swatches (plus Rainbow and a custom
 * colour picker), and the right a live preview of how the name looks in TAB, chat and above your head.
 */
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
    private static final int PANEL = 0xC0101216;
    private static final int PANEL_BORDER = 0xFF2C313A;
    private static final int SWATCH = 22;
    private static final int GAP = 4;

    private final Screen parent;
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private LinearLayout footer;
    private TabNavigationBar tabBar;

    private EditBox nameBox;
    private ColorPickerWidget picker;
    private ARGBTextInput hexInput;
    private boolean enabled;
    private boolean seeOtherNicks;
    /** A colour name, "Rainbow", or "Plain" (with customHex set for a custom colour, or none for default). */
    private String style = "Plain";
    private String customHex = "";
    private SkyJewNickFonts.NickFont nickFont = SkyJewNickFonts.NickFont.DEFAULT;

    // Panel rectangles, recomputed in layout().
    private int leftX, midX, rightX, panelY, panelH, leftW, midW, rightW;

    public SkyJewNickScreen(Screen parent) {
        super(Component.literal("SkyJew Nickname").withStyle(ChatFormatting.GRAY));
        this.parent = parent;
        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) {
            enabled = config.misc.nickname.enabled;
            seeOtherNicks = config.misc.nickname.seeOtherNicks;
            style = config.misc.nickname.style == null ? "Plain" : config.misc.nickname.style;
            customHex = config.misc.nickname.customHex == null ? "" : config.misc.nickname.customHex;
            nickFont = SkyJewNickFonts.parse(config.misc.nickname.font);
        }
    }

    @Override
    protected void init() {
        tabBar = MenuTabBar.builder(tabManager, width).addTabs(new GridLayoutTab(Component.literal("Nickname"))).build();
        tabBar.arrangeElements(width);
        tabBar.selectTab(0, false);
        addRenderableWidget(tabBar);

        footer = LinearLayout.horizontal().spacing(5);
        footer.addChild(Button.builder(Component.translatable("gui.cancel"), b -> close()).build());
        footer.addChild(Button.builder(Component.translatable("gui.done"), b -> saveAndClose()).build());
        footer.visitWidgets(this::addRenderableWidget);
        footer.arrangeElements();

        layout();

        SkyJewConfig config = SkyJewConfig.current();
        String current = config == null || config.misc.nickname.name == null ? "" : config.misc.nickname.name;
        String typed = nameBox == null ? current : nameBox.getValue();
        nameBox = new EditBox(font, leftX + 8, panelY + 28, leftW - 16, 20, Component.literal("Nickname"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal(minecraft.getUser().getName()).withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(typed);
        addRenderableWidget(nameBox);

        addRenderableWidget(CycleButton.onOffBuilder(enabled)
            .create(leftX + 8, panelY + 56, leftW - 16, 20, Component.literal("Nickname"), (b, v) -> enabled = v));
        addRenderableWidget(CycleButton.onOffBuilder(seeOtherNicks)
            .create(leftX + 8, panelY + 80, leftW - 16, 20, Component.literal("See Other Nicks"), (b, v) -> seeOtherNicks = v));
        addRenderableWidget(Button.builder(Component.literal("Remove Nickname").withStyle(ChatFormatting.RED), b -> {
            nameBox.setValue("");
            enabled = false;
            style = "Plain";
            customHex = "";
            nickFont = SkyJewNickFonts.NickFont.DEFAULT;
            rebuildWidgets();
        }).bounds(leftX + 8, panelY + panelH - 28, leftW - 16, 20).build());

        // Font: click to go to the next one, right-click for the previous one; the button shows it in that font.
        addRenderableWidget(new FontButton(leftX + 8, panelY + 136, leftW - 16));

        // Colour swatches: 4 per row, then Rainbow and Default.
        int gridX = midX + 8;
        int gridY = panelY + 28;
        int cols = Math.max(4, (midW - 16 + GAP) / (SWATCH + GAP));
        int i = 0;
        for (var entry : COLORS.entrySet()) {
            int x = gridX + (i % cols) * (SWATCH + GAP);
            int y = gridY + (i / cols) * (SWATCH + GAP);
            addRenderableWidget(new Swatch(x, y, entry.getKey(), 0xFF000000 | entry.getValue()));
            i++;
        }
        int rowsUsed = (COLORS.size() + cols - 1) / cols;
        int extraY = gridY + rowsUsed * (SWATCH + GAP) + 2;
        int half = (midW - 16 - GAP) / 2;
        addRenderableWidget(Button.builder(SkyJewNick.styled("Rainbow", "Rainbow", ""), b -> {
            style = "Rainbow";
            customHex = "";
        }).bounds(gridX, extraY, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Default"), b -> {
            style = "Plain";
            customHex = "";
        }).bounds(gridX + half + GAP, extraY, half, 20).build());

        // Custom colour: picker + hex box, like the dye picker in /sj custom.
        int pickerY = extraY + 44;
        int pickerH = Math.max(40, panelY + panelH - 30 - pickerY);
        picker = new ColorPickerWidget(gridX, pickerY, midW - 16, pickerH);
        hexInput = new ARGBTextInput(gridX, panelY + panelH - 22, font, true);
        int startColour = customHex.matches("#[0-9a-fA-F]{6}") ? 0xFF000000 | Integer.parseInt(customHex.substring(1), 16) : 0xFFFFFFFF;
        picker.setARGBColor(startColour);
        hexInput.setARGBColor(startColour);
        picker.setOnColorChange((colour, release) -> {
            setCustom(colour);
            hexInput.setARGBColor(colour);
        });
        hexInput.setOnChange(colour -> {
            setCustom(colour);
            picker.setARGBColor(colour);
        });
        addRenderableWidget(picker);
        addRenderableWidget(hexInput);
    }

    private void setCustom(int argb) {
        style = "Plain";
        customHex = String.format("#%06X", argb & 0xFFFFFF);
    }

    private void layout() {
        int top = tabBar.getRectangle().bottom() + 8;
        int bottom = height - footer.getHeight() - 12;
        panelY = top;
        panelH = Math.max(160, bottom - top);
        int total = Math.min(width - 24, 720);
        int x = (width - total) / 2;
        leftW = Math.max(150, total * 30 / 100);
        midW = Math.max(150, total * 36 / 100);
        rightW = total - leftW - midW - 16;
        leftX = x;
        midX = leftX + leftW + 8;
        rightX = midX + midW + 8;
        footer.setPosition((width - footer.getWidth()) / 2, height - footer.getHeight() - 5);
        tabManager.setTabArea(new ScreenRectangle(0, tabBar.getRectangle().bottom(), width, footer.getY() - tabBar.getRectangle().bottom() - 2));
    }

    @Override
    protected void repositionElements() {
        rebuildWidgets();
    }

    private void panel(GuiGraphicsExtractor g, int x, int w, String title) {
        g.fill(x, panelY, x + w, panelY + panelH, PANEL);
        g.outline(x, panelY, w, panelH, PANEL_BORDER);
        g.text(font, Component.literal(title).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), x + 8, panelY + 9, 0xFFFFFFFF, true);
    }

    private String previewName() {
        String name = nameBox == null ? "" : nameBox.getValue().trim();
        return name.isEmpty() ? minecraft.getUser().getName() : name;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        panel(g, leftX, leftW, "Name");
        panel(g, midX, midW, "Colour");
        panel(g, rightX, rightW, "Preview");

        int cols = Math.max(4, (midW - 16 + GAP) / (SWATCH + GAP));
        int rowsUsed = (COLORS.size() + cols - 1) / cols;
        int extraY = panelY + 28 + rowsUsed * (SWATCH + GAP) + 2;
        g.text(font, Component.literal("Custom colour").withStyle(ChatFormatting.GRAY), midX + 8, extraY + 30, 0xFFAAAAAA, false);

        String selected = "Rainbow".equalsIgnoreCase(style) ? "Rainbow"
            : !customHex.isEmpty() ? "Custom " + customHex
            : "Plain".equalsIgnoreCase(style) ? "Default" : style;
        g.text(font, Component.literal("Selected: ").withStyle(ChatFormatting.GRAY).append(Component.literal(selected).withStyle(ChatFormatting.WHITE)),
            leftX + 8, panelY + 108, 0xFFFFFFFF, false);

        // Live previews.
        g.text(font, Component.literal("Font").withStyle(ChatFormatting.GRAY), leftX + 8, panelY + 124, 0xFFAAAAAA, false);
        Component name = SkyJewNick.styled(previewName(), style, customHex, nickFont.label);
        int py = panelY + 30;
        int px = rightX + 8;
        g.text(font, Component.literal("TAB").withStyle(ChatFormatting.DARK_GRAY), px, py, 0xFFFFFFFF, false);
        g.fill(px, py + 11, rightX + rightW - 8, py + 23, 0x80000000);
        g.text(font, Component.literal("[MVP").withStyle(ChatFormatting.AQUA).append(Component.literal("++").withStyle(ChatFormatting.RED))
            .append(Component.literal("] ").withStyle(ChatFormatting.AQUA)).append(name), px + 2, py + 13, 0xFFFFFFFF, true);

        py += 36;
        g.text(font, Component.literal("Chat").withStyle(ChatFormatting.DARK_GRAY), px, py, 0xFFFFFFFF, false);
        g.fill(px, py + 11, rightX + rightW - 8, py + 23, 0x80000000);
        g.text(font, Component.empty().append(name).append(Component.literal(": hello!").withStyle(ChatFormatting.WHITE)), px + 2, py + 13, 0xFFFFFFFF, true);

        py += 36;
        g.text(font, Component.literal("Above your head").withStyle(ChatFormatting.DARK_GRAY), px, py, 0xFFFFFFFF, false);
        int tagW = font.width(name) + 4;
        int tagX = rightX + rightW / 2 - tagW / 2;
        g.fill(tagX, py + 12, tagX + tagW, py + 23, 0x40000000);
        g.text(font, name, tagX + 2, py + 14, 0xFFFFFFFF, false);

        if (!enabled) {
            g.text(font, Component.literal("Nickname is off: turn it on to use it.").withStyle(ChatFormatting.YELLOW),
                px, panelY + panelH - 20, 0xFFFFFFFF, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    /** Done: saves and closes, or stays open if the name isn't allowed. */
    private void saveAndClose() {
        if (save()) close();
    }

    private void close() {
        // Opened from chat, the menu goes back to the game rather than to the chat box.
        minecraft.gui.setScreen(parent instanceof net.minecraft.client.gui.screens.ChatScreen ? null : parent);
    }

    /** Saves the settings; false (with a message) if the name isn't allowed. */
    private boolean save() {
        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) {
            String name = nameBox.getValue().trim();
            if (SkyJewNickFilter.isBlocked(name, minecraft.getUser().getProfileId())) {
                minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal("[SJ] That nickname isn't allowed.").withStyle(ChatFormatting.RED));
                return false;
            }
            config.misc.nickname.name = name;
            config.misc.nickname.enabled = enabled && !name.isBlank();
            config.misc.nickname.seeOtherNicks = seeOtherNicks;
            config.misc.nickname.style = style;
            config.misc.nickname.customHex = "Plain".equalsIgnoreCase(style) ? customHex : "";
            config.misc.nickname.font = nickFont.label;
            SkyJewConfig.saveCurrent(config);
            SkyJewGlobalChat.sendNicknameUpdate();
        }
        return true;
    }

    /** Esc: saves if it can, and always closes. */
    @Override
    public void onClose() {
        if (!save()) {
            minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal("[SJ] Your nickname wasn't changed.").withStyle(ChatFormatting.GRAY));
        }
        close();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Cycles the nickname font; shows the font's name written in that font. */
    private final class FontButton extends AbstractWidget {
        FontButton(int x, int y, int w) {
            super(x, y, w, 20, Component.literal("Font"));
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                "Click ◀ for the previous font and ▶ for the next one.\nLetter fonts (Script, Bubble, ...) are seen by every SkyJew user.")));
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            g.fill(getX(), getY(), getX() + width, getY() + height, isHovered() ? 0xFFAAAAAA : 0xFF3A3F48);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF1B1E24);
            Component label = SkyJewNick.styled(nickFont.label, "White", "", nickFont.label);
            g.text(font, Component.literal("\u25C0").withStyle(ChatFormatting.DARK_GRAY), getX() + 5, getY() + 6, 0xFFFFFFFF, false);
            g.text(font, Component.literal("\u25B6").withStyle(ChatFormatting.DARK_GRAY), getX() + width - 11, getY() + 6, 0xFFFFFFFF, false);
            int textW = font.width(label);
            g.text(font, label, getX() + (width - textW) / 2, getY() + 6, 0xFFFFFFFF, true);
        }

        @Override
        public void onClick(MouseButtonEvent click, boolean doubled) {
            if (click.x() < getX() + width / 2.0) {
                SkyJewNickFonts.NickFont[] all = SkyJewNickFonts.NickFont.values();
                nickFont = all[(nickFont.ordinal() + all.length - 1) % all.length];
            } else {
                nickFont = nickFont.next();
            }
        }

        @Override
        protected boolean isValidClickButton(net.minecraft.client.input.MouseButtonInfo button) {
            return true;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            if (!active || !visible || !isMouseOver(click.x(), click.y())) return false;
            // The left half (the ◀ arrow) goes back, the right half (▶) forward; right-click also goes back.
            if (click.button() == 1 || click.x() < getX() + width / 2.0) {
                SkyJewNickFonts.NickFont[] all = SkyJewNickFonts.NickFont.values();
                nickFont = all[(nickFont.ordinal() + all.length - 1) % all.length];
            } else {
                nickFont = nickFont.next();
            }
            playDownSound(minecraft.getSoundManager());
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** A clickable colour square with a white frame when selected and the name as tooltip. */
    private final class Swatch extends AbstractWidget {
        private final String name;
        private final int colour;

        Swatch(int x, int y, String name, int colour) {
            super(x, y, SWATCH, SWATCH, Component.literal(name));
            this.name = name;
            this.colour = colour;
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(name).withColor(colour & 0xFFFFFF)));
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            boolean selected = name.equals(style) && customHex.isEmpty();
            g.fill(getX(), getY(), getX() + width, getY() + height, selected ? 0xFFFFFFFF : isHovered() ? 0xFFAAAAAA : 0xFF3A3F48);
            g.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, ARGB.opaque(colour));
        }

        @Override
        public void onClick(MouseButtonEvent click, boolean doubled) {
            style = name;
            customHex = "";
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class SkyJewScreen extends Screen {
    private static final int BG = 0xFF070A10;
    private static final int PANEL = 0xFF0E141E;
    private static final int CARD = 0xFF151E2B;
    private static final int HOVER = 0xFF202C3D;
    private static final int BORDER = 0xFF263448;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF8794A8;
    private static final int PURPLE = 0xFF9A6CFF;
    private static final int CYAN = 0xFF58D8FF;
    private static final int YELLOW = 0xFFFFD34D;
    private static final int GREEN = 0xFF55E68A;
    private static final int RED = 0xFFFF667A;

    private static final List<String> CATEGORIES = List.of(
        "General", "Chat", "Farming", "Slayers", "Pets",
        "Experiments", "Visual", "Discord"
    );

    private final SkyJewConfig config;
    private int selectedCategory = 0;
    private EditBox searchBox;
    private int left;
    private int top;
    private int panelW;
    private int panelH;
    private int sidebarW;

    public SkyJewScreen(SkyJewConfig config) {
        super(Component.literal("SkyJew"));
        this.config = config;
    }

    @Override
    protected void init() {
        super.init();
        rebuildLayout();

        searchBox = new EditBox(font, left + sidebarW + 22, top + 18,
            panelW - sidebarW - 44, 28, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search SkyJew settings..."));
        searchBox.setMaxLength(80);
        addRenderableWidget(searchBox);
    }

    private void rebuildLayout() {
        panelW = Math.min(820, width - 28);
        panelH = Math.min(570, height - 28);
        left = (width - panelW) / 2;
        top = Math.max(14, (height - panelH) / 2);
        sidebarW = 174;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        rebuildLayout();

        g.fill(0, 0, width, height, BG);
        g.fill(0, 0, width, 3, PURPLE);
        g.fill(left, top, left + panelW, top + panelH, PANEL);
        outline(g, left, top, left + panelW, top + panelH, BORDER);

        drawSidebar(g, mouseX, mouseY);

        g.text(font, "SkyJew", left + 22, top + 16, YELLOW, true);
        g.text(font, "SkyBlock utilities", left + 22, top + 34, MUTED, false);

        // The EditBox itself is rendered by Screen after this method.
        drawContent(g, mouseX, mouseY);

        g.text(font, "/sj  •  Search settings  •  Click a category to browse",
            left + sidebarW + 22, top + panelH - 20, MUTED, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.fill(left, top, left + sidebarW, top + panelH, 0xFF0A1018);
        g.fill(left + sidebarW - 1, top, left + sidebarW, top + panelH, BORDER);

        int y = top + 68;
        for (int i = 0; i < CATEGORIES.size(); i++) {
            String category = CATEGORIES.get(i);
            boolean selected = i == selectedCategory;
            boolean hover = inside(mouseX, mouseY, left + 10, y, sidebarW - 20, 31);

            if (selected) {
                g.fill(left + 10, y, left + sidebarW - 10, y + 31, 0xFF252039);
                g.fill(left + 10, y, left + 13, y + 31, PURPLE);
            } else if (hover) {
                g.fill(left + 10, y, left + sidebarW - 10, y + 31, HOVER);
            }

            g.text(font, category, left + 24, y + 9,
                selected ? 0xFFFFFFFF : TEXT, selected);
            y += 35;
        }

        g.text(font, "SkyJew Mod", left + 22, top + panelH - 50, CYAN, true);
        g.text(font, "26.2", left + 22, top + panelH - 34, MUTED, false);
    }

    private void drawContent(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int contentX = left + sidebarW + 22;
        int contentY = top + 62;
        int contentW = panelW - sidebarW - 44;

        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        String category = CATEGORIES.get(selectedCategory);

        g.text(font, category, contentX, contentY, TEXT, true);
        g.text(font, categoryDescription(category), contentX, contentY + 18, MUTED, false);

        int y = contentY + 48;
        int visible = 0;

        for (Setting setting : settingsFor(category)) {
            if (!query.isBlank() &&
                !setting.name.toLowerCase(Locale.ROOT).contains(query) &&
                !setting.description.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }

            if (y + 48 > top + panelH - 40) break;

            drawSetting(g, setting, contentX, y, contentW, mouseX, mouseY);
            y += 58;
            visible++;
        }

        if (visible == 0) {
            g.text(font, "No settings found.", contentX, y + 20, MUTED, false);
            g.text(font, "Try another search or category.", contentX, y + 38, MUTED, false);
        }
    }

    private void drawSetting(GuiGraphicsExtractor g, Setting s, int x, int y, int w,
                             int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 48);
        g.fill(x, y, x + w, y + 48, hover ? HOVER : CARD);
        outline(g, x, y, x + w, y + 48, s.color);

        g.text(font, s.name, x + 14, y + 9, TEXT, true);
        g.text(font, s.description, x + 14, y + 27, MUTED, false);

        String value = s.value.get();
        g.text(font, value, x + w - font.width(value) - 14, y + 17,
            s.toggle ? (s.enabled.get() ? GREEN : RED) : CYAN, true);
    }

    private List<Setting> settingsFor(String category) {
        return switch (category) {
            case "General" -> List.of(
                toggle("SkyJew enabled", "Enable the main SkyJew features.", () -> config.enabled,
                    v -> config.enabled = v, CYAN),
                action("Nickname", "Open the SkyJew nickname editor.", "OPEN", PURPLE),
                action("Custom item editor", "Rename, dye and trim held items.", "OPEN", PURPLE),
                action("Notes", "Open your saved SkyBlock notes.", "OPEN", PURPLE),
                action("Command keys", "Configure SkyJew command shortcuts.", "OPEN", PURPLE)
            );
            case "Chat" -> List.of(
                action("Global chat", "Use /sj chat to talk through the SkyJew relay.", "OPEN", CYAN),
                action("Chat emojis", "Nopo-style chat emoji support.", "ON", CYAN),
                action("Slayer drop counter", "Show kills since your last rare Slayer drop.", "COMING", PURPLE),
                action("Rare crop tracker", "Track rare farming drops in chat/HUD.", "COMING", GREEN)
            );
            case "Farming" -> List.of(
                toggle("Farming RNG HUD", "Show your farming RNG/progress overlay.", () -> config.farmingRngEnabled,
                    v -> config.farmingRngEnabled = v, GREEN),
                toggle("RNG HUD background", "Draw a background behind the RNG HUD.", () -> config.farmingRngBackground,
                    v -> config.farmingRngBackground = v, GREEN),
                value("RNG HUD scale", () -> String.format(Locale.ROOT, "%.1fx", config.farmingRngScale),
                    () -> cycleScale(), GREEN),
                value("RNG HUD position", () -> config.farmingRngX + ", " + config.farmingRngY,
                    () -> moveRngHud(), GREEN),
                action("Rare crop tracker", "Track rare crop drops and milestones.", "COMING", GREEN)
            );
            case "Slayers" -> List.of(
                action("Kills since drop", "Count Slayer kills since a rare drop.", "COMING", PURPLE),
                action("Rare drop alerts", "Prepare title/chat alerts for rare Slayer drops.", "COMING", PURPLE),
                action("Slayer HUD", "Customizable Slayer information display.", "COMING", PURPLE)
            );
            case "Pets" -> List.of(
                action("Pet display", "Show your active pet as a HUD element.", "COMING", YELLOW),
                action("Overflow pet levels", "Display pet XP beyond the normal level cap.", "COMING", YELLOW),
                action("Auto-pet display", "Keep the HUD synced with your active pet.", "COMING", YELLOW)
            );
            case "Experiments" -> List.of(
                toggle("Experiment solver", "Enable the Experimentation Table helper.", () -> config.experimentHelperEnabled,
                    v -> config.experimentHelperEnabled = v, CYAN),
                toggle("Next click highlight", "Highlight the next Chronomatron/Ultrasequencer click.", () -> config.experimentHelperHighlight,
                    v -> config.experimentHelperHighlight = v, CYAN),
                toggle("Prevent misclicks", "Block clicks that would break the sequence.", () -> config.experimentHelperPreventMisclicks,
                    v -> config.experimentHelperPreventMisclicks = v, CYAN),
                toggle("Debug mode", "Show experiment solver debug information.", () -> config.experimentHelperDebug,
                    v -> config.experimentHelperDebug = v, MUTED)
            );
            case "Visual" -> List.of(
                toggle("Mouse Lock", "Reduce camera sensitivity while aiming at a target.", () -> config.mouseLockEnabled,
                    v -> config.mouseLockEnabled = v, PURPLE),
                toggle("Ground only", "Only apply Mouse Lock while on the ground.", () -> config.mouseLockGroundOnly,
                    v -> config.mouseLockGroundOnly = v, PURPLE),
                action("Foxy jumpscare", "Random always-on Foxy jumpscare feature.", "ON", YELLOW),
                action("First boot warning", "Shows the first-launch SkyJew warning screen.", "VIEW", RED)
            );
            case "Discord" -> List.of(
                action("Discord", "Open the personal Discord linking/client screen.", "OPEN", CYAN),
                action("Discord DM", "Use /sj dm after linking your own Discord account.", "OPEN", CYAN)
            );
            default -> List.of();
        };
    }

    private String categoryDescription(String category) {
        return switch (category) {
            case "General" -> "Core SkyJew settings and utilities.";
            case "Chat" -> "Chat formatting, emojis and trackers.";
            case "Farming" -> "Farming overlays and RNG tools.";
            case "Slayers" -> "Slayer counters, alerts and displays.";
            case "Pets" -> "Pet information and XP displays.";
            case "Experiments" -> "Experimentation Table assistance.";
            case "Visual" -> "Visual helpers and HUD behaviour.";
            case "Discord" -> "Connect your own Discord account.";
            default -> "";
        };
    }

    private Setting toggle(String name, String description,
                           BoolGetter getter, BoolSetter setter, int color) {
        return new Setting(name, description, () -> getter.get() ? "ON" : "OFF",
            getter::get, () -> setter.set(!getter.get()), true, color);
    }

    private Setting value(String name, String description, String value, Runnable action, int color) {
        return new Setting(name, description, () -> value, () -> true, action, false, color);
    }

    private Setting value(String name, ValueGetter value, Runnable action, int color) {
        return new Setting(name, "", value::get, () -> true, action, false, color);
    }

    private Setting action(String name, String description, String value, int color) {
        return new Setting(name, description, () -> value, () -> true, () -> openAction(name), false, color);
    }

    private void openAction(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screen current = this;

        switch (name) {
            case "Nickname" -> mc.gui.setScreen(new SkyJewNickScreen(config, current));
            case "Custom item editor" -> SkyJewCustom.open(mc, current);
            case "Notes" -> mc.gui.setScreen(new SkyJewNotesScreen(
                Minecraft.getInstance().gameDirectory.toPath().resolve("config")));
            case "Command keys" -> mc.gui.setScreen(new SkyJewCommandKeysScreen(
                Minecraft.getInstance().gameDirectory.toPath().resolve("config")));
            case "Discord" -> mc.gui.setScreen(new SkyJewDiscordScreen(current));
            case "Global chat" -> mc.gui.setScreen(new SkyJewDiscordScreen(current));
            default -> { }
        }
    }

    private void cycleScale() {
        config.farmingRngScale += Minecraft.getInstance().hasShiftDown() ? -0.1f : 0.1f;
        config.farmingRngScale = clampScale(config.farmingRngScale);
        save();
    }

    private void moveRngHud() {
        boolean shift = Minecraft.getInstance().hasShiftDown();
        if (shift) {
            config.farmingRngX = Math.max(0, config.farmingRngX - 8);
            config.farmingRngY = Math.max(0, config.farmingRngY - 8);
        } else {
            config.farmingRngX += 8;
            config.farmingRngY += 8;
        }
        save();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        rebuildLayout();

        int y = top + 68;
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (inside(event.x(), event.y(), left + 10, y, sidebarW - 20, 31)) {
                selectedCategory = i;
                return true;
            }
            y += 35;
        }

        int contentX = left + sidebarW + 22;
        int contentY = top + 62;
        int contentW = panelW - sidebarW - 44;
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);

        int cardY = contentY + 48;
        for (Setting setting : settingsFor(CATEGORIES.get(selectedCategory))) {
            if (!query.isBlank() &&
                !setting.name.toLowerCase(Locale.ROOT).contains(query) &&
                !setting.description.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            if (inside(event.x(), event.y(), contentX, cardY, contentW, 48)) {
                setting.action.run();
                save();
                return true;
            }
            cardY += 58;
            if (cardY + 48 > top + panelH - 40) break;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void save() {
        Path path = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("skyjew-mod.json");
        config.save(path);
    }

    private static float clampScale(float value) {
        return Math.max(0.5f, Math.min(3.0f,
            Math.round(value * 10.0f) / 10.0f));
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

    private record Setting(String name, String description, ValueGetter value,
                           BoolGetter enabled, Runnable action, boolean toggle, int color) {}

    @FunctionalInterface
    private interface ValueGetter { String get(); }

    @FunctionalInterface
    private interface BoolGetter { boolean get(); }

    @FunctionalInterface
    private interface BoolSetter { void set(boolean value); }
}

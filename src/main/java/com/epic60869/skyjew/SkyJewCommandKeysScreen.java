package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Command Keys-style keybind editor used by SkyJew. */
public final class SkyJewCommandKeysScreen extends Screen {
    private static final int BG = 0xFF080B12;
    private static final int PANEL = 0xFF141B27;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF8D9AAF;
    private static final int CYAN = 0xFF58D8FF;
    private static final int PURPLE = 0xFF9A6CFF;
    private static final int YELLOW = 0xFFFFD34D;

    private final Path configDir;
    private int selected;
    private int selectedProfile;
    private boolean profileView = true;
    private EditBox name;
    private EditBox command;
    private EditBox key;
    private EditBox delay;
    private EditBox profileName;
    private EditBox profileMatch;
    private SkyJewCommandKeys.Mode mode = SkyJewCommandKeys.Mode.SEND;
    private SkyJewCommandKeys.Macro editing;
    private boolean pickingKey;

    public SkyJewCommandKeysScreen(Path configDir) {
        super(Component.literal("SkyJew Keybinds"));
        this.configDir = configDir;
        if (SkyJewCommandKeys.data().profiles.isEmpty()) {
            SkyJewCommandKeys.data().profiles.add(new SkyJewCommandKeys.Profile());
        }
    }

    private SkyJewCommandKeys.Profile profile() {
        if (SkyJewCommandKeys.data().profiles.isEmpty()) {
            SkyJewCommandKeys.data().profiles.add(new SkyJewCommandKeys.Profile());
        }
        selectedProfile = Math.max(0, Math.min(selectedProfile, SkyJewCommandKeys.data().profiles.size() - 1));
        return SkyJewCommandKeys.data().profiles.get(selectedProfile);
    }

    private List<SkyJewCommandKeys.Macro> keybinds() {
        return profile().macros;
    }

    @Override
    protected void init() {
        clearWidgets();

        if (profileView) {
            initProfileView();
        } else {
            initMacroView();
        }
    }

    private void initProfileView() {
        int w = Math.min(520, width - 40);
        int left = (width - w) / 2;
        int rowW = w - 48;
        int y = 58;

        addRenderableWidget(Button.builder(Component.literal("Conflict Strategy: Assert"), b -> {})
            .bounds(left, y, rowW / 2 - 3, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Send Mode: Send"), b -> {})
            .bounds(left + rowW / 2 + 3, y, rowW / 2 - 3, 20).build());

        y += 32;
        addRenderableWidget(Button.builder(Component.literal("Active Profile"), b -> {})
            .bounds(left, y, rowW, 20).build());

        y += 28;
        for (int i = 0; i < SkyJewCommandKeys.data().profiles.size(); i++) {
            final int index = i;
            SkyJewCommandKeys.Profile p = SkyJewCommandKeys.data().profiles.get(i);
            int by = y + i * 27;

            addRenderableWidget(Button.builder(
                Component.literal((i == selectedProfile ? "↑ " : "") + (p.name == null || p.name.isBlank() ? "Unnamed Profile" : p.name)),
                b -> {
                    selectedProfile = index;
                    selected = 0;
                    profileView = false;
                    init();
                }).bounds(left, by, rowW - 138, 22).build());

            addRenderableWidget(Button.builder(Component.literal("S"),
                b -> {
                    p.singleplayerDefault = !p.singleplayerDefault;
                    SkyJewCommandKeys.save();
                    init();
                }).bounds(left + rowW - 132, by, 40, 22).build());

            addRenderableWidget(Button.builder(Component.literal("M"),
                b -> {
                    p.multiplayerDefault = !p.multiplayerDefault;
                    SkyJewCommandKeys.save();
                    init();
                }).bounds(left + rowW - 88, by, 40, 22).build());

            addRenderableWidget(Button.builder(Component.literal("Edit"),
                b -> {
                    selectedProfile = index;
                    selected = 0;
                    profileView = false;
                    init();
                }).bounds(left + rowW - 44, by, 44, 22).build());
        }

        int addY = y + SkyJewCommandKeys.data().profiles.size() * 27 + 8;
        addRenderableWidget(Button.builder(Component.literal("+"),
            b -> {
                SkyJewCommandKeys.Profile p = new SkyJewCommandKeys.Profile();
                p.name = "Profile " + (SkyJewCommandKeys.data().profiles.size() + 1);
                SkyJewCommandKeys.data().profiles.add(p);
                selectedProfile = SkyJewCommandKeys.data().profiles.size() - 1;
                SkyJewCommandKeys.save();
                init();
            }).bounds(left, addY, rowW, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Done"),
            b -> onClose()).bounds(left, height - 42, rowW, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Remove Profile"),
            b -> {
                if (SkyJewCommandKeys.data().profiles.size() > 1) {
                    SkyJewCommandKeys.data().profiles.remove(selectedProfile);
                    selectedProfile = Math.max(0, selectedProfile - 1);
                    SkyJewCommandKeys.save();
                    init();
                }
            }).bounds(left, height - 70, rowW, 22).build());
    }

    private void initMacroView() {
        int left = 28;
        int listRight = Math.min(330, width / 2 - 20);
        int right = listRight + 25;
        int fieldW = Math.max(250, Math.min(460, width - right - 30));

        if (selected >= keybinds().size()) selected = Math.max(0, keybinds().size() - 1);
        editing = keybinds().isEmpty() ? null : keybinds().get(selected);
        mode = editing == null || editing.mode == null ? SkyJewCommandKeys.Mode.SEND : editing.mode;

        addRenderableWidget(Button.builder(Component.literal("< Profiles"),
            b -> {
                saveEditing();
                SkyJewCommandKeys.save();
                profileView = true;
                init();
            }).bounds(left, 20, 100, 22).build());

        addRenderableWidget(Button.builder(Component.literal("+ Add Keybind"), b -> addMacro())
            .bounds(left, height - 46, 125, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), b -> deleteMacro())
            .bounds(left + 132, height - 46, 85, 24).build());

        if (editing != null) {
            name = box(right, 80, fieldW, editing.name);
            command = box(right, 112, fieldW, String.join("\n", editing.commands));
            key = box(right, 144, 120, displayKey(editing));
            key.setEditable(false);
            delay = box(right + 130, 144, 90, String.valueOf(editing.delayMs));

            addRenderableWidget(Button.builder(
                    Component.literal(pickingKey ? "Press a key..." : "Pick Key"),
                    b -> pickingKey = true)
                    .bounds(right + 225, 144, 110, 20).build());

            int y = 180;
            for (SkyJewCommandKeys.Mode m : SkyJewCommandKeys.Mode.values()) {
                final SkyJewCommandKeys.Mode selectedMode = m;
                addRenderableWidget(Button.builder(
                    Component.literal((m == mode ? "✓ " : "") + modeName(m)),
                    b -> {
                        saveEditing();
                        mode = selectedMode;
                        init();
                    }).bounds(right + ((m.ordinal() % 3) * 112),
                        y + ((m.ordinal() / 3) * 25), 108, 20).build());
            }

            addRenderableWidget(Button.builder(Component.literal("Save"),
                b -> {
                    saveEditing();
                    SkyJewCommandKeys.save();
                    init();
                }).bounds(right, 240, 100, 24).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Save & Close"),
            b -> {
                saveEditing();
                SkyJewCommandKeys.save();
                onClose();
            }).bounds(width - 150, height - 46, 120, 24).build());
    }

    private String displayKey(SkyJewCommandKeys.Macro m) {
        String base = SkyJewCommandKeys.keyName(m.keyCode);
        if (m.modifier == 0) return base;
        StringBuilder s = new StringBuilder();
        if ((m.modifier & GLFW.GLFW_MOD_CONTROL) != 0) s.append("CTRL+");
        if ((m.modifier & GLFW.GLFW_MOD_SHIFT) != 0) s.append("SHIFT+");
        if ((m.modifier & GLFW.GLFW_MOD_ALT) != 0) s.append("ALT+");
        if ((m.modifier & GLFW.GLFW_MOD_SUPER) != 0) s.append("SUPER+");
        return s + base;
    }

    private String modeName(SkyJewCommandKeys.Mode mode) {
        return switch (mode) {
            case SEND -> "Send";
            case TYPE -> "Type";
            case EDIT -> "Edit";
            case CYCLE -> "Cycle";
            case RANDOM -> "Random";
            case REPEAT -> "Repeat";
        };
    }

    private EditBox box(int x, int y, int w, String value) {
        EditBox e = new EditBox(font, x, y, w, 20, Component.literal(""));
        e.setValue(value == null ? "" : value);
        e.setMaxLength(4000);
        addRenderableWidget(e);
        return e;
    }

    private void addMacro() {
        SkyJewCommandKeys.Macro m = new SkyJewCommandKeys.Macro();
        m.name = "Macro " + (keybinds().size() + 1);
        keybinds().add(m);
        selected = keybinds().size() - 1;
        SkyJewCommandKeys.save();
        init();
    }

    private void deleteMacro() {
        if (selected >= 0 && selected < keybinds().size()) {
            keybinds().remove(selected);
            selected = Math.max(0, selected - 1);
            SkyJewCommandKeys.save();
            init();
        }
    }

    private void saveEditing() {
        if (editing == null) return;

        editing.name = name == null ? editing.name : name.getValue();
        editing.commands = new ArrayList<>();
        String[] lines = command == null
                ? new String[]{"/help"}
                : command.getValue().replace("\r", "").split("\\n", -1);
        for (String line : lines) {
            if (!line.isBlank()) editing.commands.add(line);
        }
        if (editing.commands.isEmpty()) editing.commands.add("/help");

        try {
            editing.delayMs = Math.max(0, Integer.parseInt(delay == null ? "250" : delay.getValue().trim()));
        } catch (Exception ignored) {
            editing.delayMs = 250;
        }

        editing.mode = mode;
        if (profileName != null) profile().name = profileName.getValue();
        if (profileMatch != null) profile().match = profileMatch.getValue();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pickingKey && event.key() != GLFW.GLFW_KEY_ESCAPE && editing != null) {
            editing.keyCode = event.key();
            editing.modifier = event.modifiers()
                    & (GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL
                    | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER);
            pickingKey = false;
            SkyJewCommandKeys.save();
            init();
            return true;
        }

        if (pickingKey && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            pickingKey = false;
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        saveEditing();
        SkyJewCommandKeys.save();
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF10151F);
        int panelW = Math.min(560, width - 32);
        int left = (width - panelW) / 2;

        g.fill(left, 8, left + panelW, height - 8, 0xFF1A2230);
        g.fill(left + 1, 9, left + panelW - 1, height - 9, 0xFF111722);

        String title = profileView ? "CommandKeys Options" :
            "CommandKeys • " + (profile().name == null ? "Profile" : profile().name);
        g.text(font, title, width / 2 - font.width(title) / 2, 18, 0xFFFFFFFF, true);

        if (profileView) {
            g.text(font, "Default Options", left + 24, 42, 0xFFFFFFFF, true);
            g.text(font, "Profiles", left + 24, 112, 0xFFFFFFFF, true);
            g.text(font, "S = singleplayer default   M = multiplayer default", left + 24,
                height - 92, 0xFF8D9AAF, false);
        } else {
            g.text(font, "Keybinds", 28, 52, 0xFFFFFFFF, true);
            g.text(font, "Macro", Math.min(355, width / 2 + 25), 52, 0xFFFFFFFF, true);
            if (editing != null) {
                g.text(font, "Name", Math.min(355, width / 2 + 25), 68, 0xFF8D9AAF, false);
                g.text(font, "Messages / commands (one per line)", Math.min(355, width / 2 + 25),
                    100, 0xFF8D9AAF, false);
                g.text(font, "Keybind and delay", Math.min(355, width / 2 + 25), 132, 0xFF8D9AAF, false);
                g.text(font, "Send mode", Math.min(355, width / 2 + 25), 168, 0xFF8D9AAF, false);
                g.text(font, "Commands beginning with / are sent through the command packet.",
                    Math.min(355, width / 2 + 25), 272, 0xFF57F287, false);
            }
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (profileView) return super.mouseClicked(event, doubleClick);

        int left = 28;
        int listRight = Math.min(330, width / 2 - 20);

        if (event.button() == 0
                && event.x() >= left + 8
                && event.x() <= listRight - 8
                && event.y() >= 90
                && event.y() < height - 60) {
            int index = (int) ((event.y() - 90) / 36);
            if (index >= 0 && index < keybinds().size()) {
                saveEditing();
                selected = index;
                init();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }
}

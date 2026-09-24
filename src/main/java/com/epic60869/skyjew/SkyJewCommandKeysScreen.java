package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * SkyJew's Command Keys editor.
 *
 * The layout follows the useful parts of TerminalMC CommandKeys: profile
 * selection on the left, a macro list in the centre, and a proper editor on
 * the right. It intentionally uses SkyJew's own data model and does not
 * bundle the CommandKeys mod.
 */
public final class SkyJewCommandKeysScreen extends Screen {
    private static final int BG = 0xFF080B12;
    private static final int PANEL = 0xFF121925;
    private static final int PANEL_2 = 0xFF192231;
    private static final int BORDER = 0xFF293548;
    private static final int ACCENT = 0xFF58D8FF;
    private static final int PURPLE = 0xFF9A72FF;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF8794A8;
    private static final int GREEN = 0xFF57E389;
    private static final int RED = 0xFFFF647C;

    private final Path configDir;
    private int selectedProfile;
    private int selectedMacro = -1;
    private boolean pickingKey;
    private boolean pickingMouse;

    private EditBox profileName;
    private EditBox profileMatch;
    private EditBox macroName;
    private EditBox commands;
    private EditBox delay;
    private SkyJewCommandKeys.Mode mode;
    private SkyJewCommandKeys.Conflict conflict;

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
        selectedProfile = Math.max(0,
            Math.min(selectedProfile, SkyJewCommandKeys.data().profiles.size() - 1));
        return SkyJewCommandKeys.data().profiles.get(selectedProfile);
    }

    private SkyJewCommandKeys.Macro macro() {
        List<SkyJewCommandKeys.Macro> list = profile().macros;
        if (selectedMacro < 0 || selectedMacro >= list.size()) return null;
        return list.get(selectedMacro);
    }

    @Override
    protected void init() {
        clearWidgets();

        int margin = 18;
        int top = 32;
        int bottom = height - 34;

        // Header/profile controls.
        addRenderableWidget(Button.builder(Component.literal("+ Profile"), b -> addProfile())
            .bounds(margin, 8, 92, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
            .bounds(width - 190, 8, 78, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            save();
            onClose();
        }).bounds(width - 106, 8, 88, 20).build());

        int leftW = Math.min(190, Math.max(160, width / 5));
        int centreW = Math.min(300, Math.max(220, width / 4));
        int rightX = margin + leftW + 10 + centreW + 10;
        int centreX = margin + leftW + 10;
        int usableRight = width - margin;

        buildProfilePanel(margin, top, leftW, bottom);
        buildMacroPanel(centreX, top, centreW, bottom);

        if (rightX + 220 < usableRight) {
            buildEditor(rightX, top, usableRight - rightX, bottom);
        }
    }

    private void buildProfilePanel(int x, int top, int w, int bottom) {
        int y = top + 28;
        for (int i = 0; i < SkyJewCommandKeys.data().profiles.size(); i++) {
            final int index = i;
            SkyJewCommandKeys.Profile p = SkyJewCommandKeys.data().profiles.get(i);
            String label = (i == selectedProfile ? "▶ " : "") +
                (p.name == null || p.name.isBlank() ? "Unnamed" : p.name);

            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                saveEditor();
                selectedProfile = index;
                selectedMacro = -1;
                init();
            }).bounds(x, y, w, 24).build());
            y += 28;
        }

        SkyJewCommandKeys.Profile p = profile();
        int fieldY = bottom - 92;

        profileName = field(x, fieldY, w, p.name);
        profileMatch = field(x, fieldY + 28, w, p.match);
        addRenderableWidget(profileName);
        addRenderableWidget(profileMatch);

        addRenderableWidget(Button.builder(
            Component.literal(p.multiplayerDefault ? "★ Multiplayer default" : "☆ Set multiplayer default"),
            b -> {
                p.multiplayerDefault = !p.multiplayerDefault;
                if (p.multiplayerDefault) {
                    for (SkyJewCommandKeys.Profile other : SkyJewCommandKeys.data().profiles)
                        if (other != p) other.multiplayerDefault = false;
                }
                save();
                init();
            }).bounds(x, fieldY + 56, w, 22).build());
    }

    private void buildMacroPanel(int x, int top, int w, int bottom) {
        addRenderableWidget(Button.builder(Component.literal("+ New macro"), b -> addMacro())
            .bounds(x, top, w - 54, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Copy"), b -> copyMacro())
            .bounds(x + w - 48, top, 48, 24).build());

        int y = top + 32;
        List<SkyJewCommandKeys.Macro> list = profile().macros;
        for (int i = 0; i < list.size(); i++) {
            final int index = i;
            SkyJewCommandKeys.Macro m = list.get(i);
            String key = keyDisplay(m);
            String label = (i == selectedMacro ? "▶ " : "") +
                (m.name == null || m.name.isBlank() ? "Unnamed macro" : m.name);

            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                saveEditor();
                selectedMacro = index;
                init();
            }).bounds(x, y, w, 25).build());

            y += 29;
            if (y > bottom - 35) break;
        }

        addRenderableWidget(Button.builder(Component.literal("Delete"),
            b -> deleteMacro()).bounds(x, bottom - 26, w / 2 - 3, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"),
            b -> {
                SkyJewCommandKeys.load();
                selectedMacro = -1;
                init();
            }).bounds(x + w / 2 + 3, bottom - 26, w / 2 - 3, 22).build());
    }

    private void buildEditor(int x, int top, int w, int bottom) {
        SkyJewCommandKeys.Macro m = macro();
        if (m == null) {
            return;
        }

        mode = m.mode == null ? SkyJewCommandKeys.Mode.SEND : m.mode;
        conflict = m.conflict == null ? SkyJewCommandKeys.Conflict.ASSERT : m.conflict;

        int fieldW = Math.max(180, w - 20);
        int y = top + 30;

        macroName = field(x, y, fieldW, m.name);
        addRenderableWidget(macroName);
        y += 32;

        commands = field(x, y, fieldW, String.join("\n", m.commands));
        commands.setMaxLength(4096);
        addRenderableWidget(commands);
        y += 56;

        addRenderableWidget(Button.builder(
            Component.literal(pickingKey ? "Press keyboard key..." : "Keyboard: " + keyDisplay(m)),
            b -> {
                pickingKey = true;
                pickingMouse = false;
            }).bounds(x, y, Math.min(210, fieldW), 24).build());

        addRenderableWidget(Button.builder(
            Component.literal("Mouse"),
            b -> {
                pickingMouse = true;
                pickingKey = false;
            }).bounds(x + Math.min(218, fieldW - 100), y, 80, 24).build());

        delay = field(x + Math.min(304, Math.max(0, fieldW - 180)), y, 90,
            String.valueOf(m.delayMs));
        addRenderableWidget(delay);
        y += 34;

        gButtons(x, y, fieldW);
        y += 78;

        addRenderableWidget(Button.builder(
            Component.literal("✓ Apply changes"), b -> {
                saveEditor();
                save();
                init();
            }).bounds(x, bottom - 26, Math.min(150, fieldW), 22).build());
    }

    private void gButtons(int x, int y, int w) {
        addRenderableWidget(Button.builder(
            Component.literal("Mode: " + modeName(mode)), b -> cycleMode())
            .bounds(x, y, Math.min(180, w), 24).build());

        addRenderableWidget(Button.builder(
            Component.literal("Conflict: " + conflictName(conflict)), b -> cycleConflict())
            .bounds(x + Math.min(188, Math.max(0, w - 150)), y, Math.min(150, w), 24).build());
    }

    private EditBox field(int x, int y, int w, String value) {
        EditBox e = new EditBox(font, x, y, Math.max(70, w), 22, Component.empty());
        e.setValue(value == null ? "" : value);
        return e;
    }

    private void addProfile() {
        SkyJewCommandKeys.Profile p = new SkyJewCommandKeys.Profile();
        p.name = "Profile " + (SkyJewCommandKeys.data().profiles.size() + 1);
        SkyJewCommandKeys.data().profiles.add(p);
        selectedProfile = SkyJewCommandKeys.data().profiles.size() - 1;
        selectedMacro = -1;
        save();
        init();
    }

    private void addMacro() {
        SkyJewCommandKeys.Macro m = new SkyJewCommandKeys.Macro();
        m.name = "Macro " + (profile().macros.size() + 1);
        profile().macros.add(m);
        selectedMacro = profile().macros.size() - 1;
        save();
        init();
    }

    private void copyMacro() {
        SkyJewCommandKeys.Macro source = macro();
        if (source == null) return;

        SkyJewCommandKeys.Macro copy = new SkyJewCommandKeys.Macro();
        copy.name = source.name + " Copy";
        copy.keyCode = GLFW.GLFW_KEY_UNKNOWN;
        copy.mouseButton = source.mouseButton;
        copy.modifier = source.modifier;
        copy.mode = source.mode;
        copy.delayMs = source.delayMs;
        copy.conflict = source.conflict;
        copy.commands = new ArrayList<>(source.commands);
        profile().macros.add(copy);
        selectedMacro = profile().macros.size() - 1;
        save();
        init();
    }

    private void deleteMacro() {
        if (selectedMacro >= 0 && selectedMacro < profile().macros.size()) {
            profile().macros.remove(selectedMacro);
            selectedMacro = Math.min(selectedMacro, profile().macros.size() - 1);
            save();
            init();
        }
    }

    private void saveEditor() {
        SkyJewCommandKeys.Macro m = macro();
        if (m == null) return;

        if (macroName != null) m.name = macroName.getValue().trim();
        if (m.name == null || m.name.isBlank()) m.name = "Unnamed macro";

        if (commands != null) {
            List<String> result = new ArrayList<>();
            for (String line : commands.getValue().replace("\r", "").split("\\n", -1)) {
                // Empty lines are meaningful in TYPE mode but not in normal send
                // mode; preserve them only for TYPE.
                if (m.mode == SkyJewCommandKeys.Mode.TYPE || !line.isBlank()) {
                    result.add(line);
                }
            }
            if (result.isEmpty()) result.add("/help");
            m.commands = result;
        }

        try {
            m.delayMs = Math.max(0, Integer.parseInt(delay == null ? "250" : delay.getValue().trim()));
        } catch (Exception ignored) {
            m.delayMs = 250;
        }

        m.mode = mode == null ? SkyJewCommandKeys.Mode.SEND : mode;
        m.conflict = conflict == null ? SkyJewCommandKeys.Conflict.ASSERT : conflict;

        SkyJewCommandKeys.Profile p = profile();
        if (profileName != null) p.name = profileName.getValue().trim();
        if (profileMatch != null) p.match = profileMatch.getValue().trim();
    }

    private void cycleMode() {
        saveEditor();
        SkyJewCommandKeys.Mode[] values = SkyJewCommandKeys.Mode.values();
        mode = values[(mode.ordinal() + 1) % values.length];
        init();
    }

    private void cycleConflict() {
        saveEditor();
        SkyJewCommandKeys.Conflict[] values = SkyJewCommandKeys.Conflict.values();
        conflict = values[(conflict.ordinal() + 1) % values.length];
        init();
    }

    private String modeName(SkyJewCommandKeys.Mode m) {
        return switch (m) {
            case SEND -> "Send";
            case TYPE -> "Type";
            case EDIT -> "Edit";
            case CYCLE -> "Cycle";
            case RANDOM -> "Random";
            case REPEAT -> "Repeat";
            case RELEASE -> "Release";
        };
    }

    private String conflictName(SkyJewCommandKeys.Conflict c) {
        return switch (c) {
            case SUBMIT -> "Submit";
            case ASSERT -> "Assert";
            case VETO -> "Veto";
            case AVOID -> "Avoid";
        };
    }

    private String keyDisplay(SkyJewCommandKeys.Macro m) {
        if (m == null || m.keyCode == GLFW.GLFW_KEY_UNKNOWN) return "Unbound";
        String base = m.mouseButton
            ? "MOUSE " + m.keyCode
            : SkyJewCommandKeys.keyName(m.keyCode);
        StringBuilder s = new StringBuilder();
        if ((m.modifier & GLFW.GLFW_MOD_CONTROL) != 0) s.append("CTRL+");
        if ((m.modifier & GLFW.GLFW_MOD_SHIFT) != 0) s.append("SHIFT+");
        if ((m.modifier & GLFW.GLFW_MOD_ALT) != 0) s.append("ALT+");
        if ((m.modifier & GLFW.GLFW_MOD_SUPER) != 0) s.append("SUPER+");
        return s + base;
    }

    private void save() {
        saveEditor();
        SkyJewCommandKeys.save();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pickingKey && event.key() != GLFW.GLFW_KEY_ESCAPE && macro() != null) {
            SkyJewCommandKeys.Macro m = macro();
            m.keyCode = event.key();
            m.mouseButton = false;
            m.modifier = event.modifiers() &
                (GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL |
                 GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER);
            pickingKey = false;
            save();
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (pickingMouse && macro() != null) {
            SkyJewCommandKeys.Macro m = macro();
            m.keyCode = event.button();
            m.mouseButton = true;
            m.modifier = 0;
            pickingMouse = false;
            save();
            init();
            return true;
        }
        if (pickingKey) {
            // A mouse click while keyboard capture is active is ignored so the
            // capture button itself does not become the binding.
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        save();
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);

        int margin = 18;
        int top = 32;
        int bottom = height - 34;
        int leftW = Math.min(190, Math.max(160, width / 5));
        int centreW = Math.min(300, Math.max(220, width / 4));
        int centreX = margin + leftW + 10;
        int rightX = centreX + centreW + 10;
        int rightW = width - margin - rightX;

        panel(g, margin, top, leftW, bottom - top, "Profiles");
        panel(g, centreX, top, centreW, bottom - top, "Macros");

        if (rightW > 200) {
            panel(g, rightX, top, rightW, bottom - top, macro() == null ? "Macro editor" : "Macro editor");
            if (macro() != null) {
                SkyJewCommandKeys.Macro m = macro();
                g.text(font, "Name", rightX + 2, top + 4, MUTED, false);
                g.text(font, "Messages / commands", rightX + 2, top + 64, MUTED, false);
                g.text(font, "Binding", rightX + 2, top + 122, MUTED, false);
                g.text(font, "Activation", rightX + 2, top + 188, MUTED, false);
                g.text(font, "Type '/' to send a command, otherwise a normal chat message.",
                    rightX + 2, bottom - 55, GREEN, false);
            }
        }

        g.text(font, Component.literal("SKYJEW  •  COMMAND KEYS"), margin, 16, ACCENT, true);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x, y, x + w, y + 1, BORDER);
        g.text(font, Component.literal(title), x + 8, y + 8, TEXT, true);
    }
}

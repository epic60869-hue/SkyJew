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

/** Basic Command Keys editor used by SkyJew. */
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
        super(Component.literal("SkyJew Command Keys"));
        this.configDir = configDir;
        if (SkyJewCommandKeys.data().profiles.isEmpty()) {
            SkyJewCommandKeys.data().profiles.add(new SkyJewCommandKeys.Profile());
        }
    }

    private SkyJewCommandKeys.Profile profile() {
        return SkyJewCommandKeys.data().profiles.get(0);
    }

    private List<SkyJewCommandKeys.Macro> macros() {
        return profile().macros;
    }

    @Override
    protected void init() {
        clearWidgets();

        int left = 28;
        int listRight = Math.min(310, width / 2 - 20);
        int right = listRight + 25;
        int fieldW = Math.max(250, Math.min(460, width - right - 30));

        if (selected >= macros().size()) selected = Math.max(0, macros().size() - 1);
        editing = macros().isEmpty() ? null : macros().get(selected);

        addRenderableWidget(Button.builder(Component.literal("+ Add Macro"), b -> addMacro())
                .bounds(left, height - 46, 125, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), b -> deleteMacro())
                .bounds(left + 132, height - 46, 85, 24).build());

        profileName = box(right, 35, fieldW, profile().name);
        profileMatch = box(right, 67, fieldW, profile().match);

        if (editing != null) {
            name = box(right, 121, fieldW, editing.name);
            command = box(right, 153, fieldW, String.join("\n", editing.commands));
            key = box(right, 185, 100, SkyJewCommandKeys.keyName(editing.keyCode));
            key.setEditable(false);
            delay = box(right + 110, 185, 90, String.valueOf(editing.delayMs));

            addRenderableWidget(Button.builder(
                    Component.literal(pickingKey ? "Press a key..." : "Pick Key"),
                    b -> pickingKey = true)
                    .bounds(right + 205, 185, 105, 20).build());

            int y = 220;
            for (SkyJewCommandKeys.Mode m : SkyJewCommandKeys.Mode.values()) {
                String label = (m == mode ? "> " : "") + m.name();
                addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    saveEditing();
                    mode = m;
                    init();
                }).bounds(right + ((m.ordinal() % 3) * 105),
                        y + ((m.ordinal() / 3) * 25), 100, 20).build());
            }

            addRenderableWidget(Button.builder(Component.literal("Save"),
                    b -> {
                        saveEditing();
                        SkyJewCommandKeys.save();
                        init();
                    })
                    .bounds(right, 278, 100, 24).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Save & Close"),
                b -> {
                    saveEditing();
                    SkyJewCommandKeys.save();
                    onClose();
                })
                .bounds(width - 150, height - 46, 120, 24).build());

        if (editing != null) {
            mode = editing.mode == null ? SkyJewCommandKeys.Mode.SEND : editing.mode;
        }
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
        m.name = "Macro " + (macros().size() + 1);
        macros().add(m);
        selected = macros().size() - 1;
        SkyJewCommandKeys.save();
        init();
    }

    private void deleteMacro() {
        if (selected >= 0 && selected < macros().size()) {
            macros().remove(selected);
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
        g.fill(0, 0, width, height, BG);
        g.fill(0, 0, width, 3, PURPLE);

        int left = 28;
        int listRight = Math.min(310, width / 2 - 20);
        int right = listRight + 25;

        g.text(font, "SkyJew Command Keys", left, 20, YELLOW, true);
        g.text(font, "Command Keys-style macros built directly into SkyJew", left, 38, MUTED, false);

        g.fill(left, 58, listRight, height - 60, PANEL);
        g.fill(right, 58, width - 25, height - 60, PANEL);

        g.text(font, "MACROS", left + 12, 68, CYAN, true);
        int y = 94;
        for (int i = 0; i < macros().size(); i++) {
            SkyJewCommandKeys.Macro m = macros().get(i);
            boolean sel = i == selected;
            g.fill(left + 8, y - 4, listRight - 8, y + 27,
                    sel ? 0xFF253957 : 0xFF182231);
            g.text(font, m.name == null ? "Unnamed" : m.name,
                    left + 16, y + 2, TEXT, sel);
            g.text(font, SkyJewCommandKeys.keyName(m.keyCode) + " • " + m.mode.name(),
                    left + 16, y + 15, MUTED, false);
            y += 36;
        }

        g.text(font, "PROFILE", right + 12, 68, CYAN, true);
        g.text(font, "Name", right, 27, MUTED, false);
        g.text(font, "Auto-match (server/world contains)", right, 59, MUTED, false);

        if (editing != null) {
            g.text(font, "MACRO", right, 103, CYAN, true);
            g.text(font, "Name", right, 109, MUTED, false);
            g.text(font, "Commands (one per line)", right, 141, MUTED, false);
            g.text(font, "Key / delay (ms)", right, 173, MUTED, false);
            g.text(font, "Mode", right, 209, MUTED, false);
            g.text(font, "Send = all • Cycle = next • Random = random • Repeat = toggle • Type = chat box",
                    right, 258, MUTED, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        int left = 28;
        int listRight = Math.min(310, width / 2 - 20);

        if (event.button() == 0
                && event.x() >= left + 8
                && event.x() <= listRight - 8
                && event.y() >= 90
                && event.y() < height - 60) {
            int index = (int) ((event.y() - 90) / 36);
            if (index >= 0 && index < macros().size()) {
                saveEditing();
                selected = index;
                init();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }
}

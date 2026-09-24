package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent in-game notepad.
 *
 * The note file has no application-defined line limit. Only the visible
 * lines are represented by EditBox widgets at any one time, so very large
 * notes do not require thousands of GUI widgets.
 */
public final class SkyJewNotesScreen extends Screen {
    private static final int BG = 0xFFFFFFFF;
    private static final int PANEL = 0xFFFFFFFF;
    private static final int BORDER = 0xFFB8B8B8;
    private static final int MUTED = 0xFF666666;
    private static final int CYAN = 0xFF2468A8;
    private static final int YELLOW = 0xFF222222;
    private static final int LINE_HEIGHT = 24;
    private static final int MAX_LINE_LENGTH = 500;

    private final Path configDir;
    private final List<EditBox> visibleLines = new ArrayList<>();
    private List<String> noteLines = new ArrayList<>();
    private int scrollOffset = 0;

    public SkyJewNotesScreen(Path configDir) {
        super(Component.literal("SkyJew Notes"));
        this.configDir = configDir;
    }

    private int visibleLineCount() {
        return Math.max(8, (height - 105) / LINE_HEIGHT);
    }

    private void loadNotes() {
        String saved = SkyJewNotes.load(configDir)
            .replace("\r\n", "\n")
            .replace('\r', '\n');

        String[] savedLines = saved.split("\n", -1);
        noteLines = new ArrayList<>(List.of(savedLines));

        if (noteLines.isEmpty()) {
            noteLines.add("");
        }

        // Keep one empty line available so the user can immediately continue
        // writing at the end of the note.
        if (!noteLines.get(noteLines.size() - 1).isEmpty()) {
            noteLines.add("");
        }
    }

    private void syncVisibleLines() {
        for (int i = 0; i < visibleLines.size(); i++) {
            int index = scrollOffset + i;
            if (index < noteLines.size()) {
                noteLines.set(index, visibleLines.get(i).getValue());
            }
        }
    }

    @Override
    protected void init() {
        syncVisibleLines();
        clearWidgets();
        visibleLines.clear();

        if (noteLines.isEmpty()) {
            loadNotes();
        }

        int visible = visibleLineCount();
        int maxScroll = Math.max(0, noteLines.size() - visible);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        int left = Math.max(30, width / 2 - 350) + 50;
        int fieldWidth = Math.min(700, width - left - 30);
        int top = 76;

        int end = Math.min(noteLines.size(), scrollOffset + visible);
        for (int i = scrollOffset; i < end; i++) {
            EditBox box = new EditBox(
                font,
                left,
                top + (i - scrollOffset) * LINE_HEIGHT,
                fieldWidth,
                20,
                Component.literal("Note " + (i + 1))
            );
            box.setValue(noteLines.get(i));
            box.setMaxLength(MAX_LINE_LENGTH);
            box.setBordered(false);
            box.setHint(Component.empty());
            addRenderableWidget(box);
            visibleLines.add(box);
        }

        if (!visibleLines.isEmpty()) {
            visibleLines.get(0).setFocused(true);
            setFocused(visibleLines.get(0));
        }
    }

    private void save() {
        syncVisibleLines();

        // Remove only trailing empty lines, while preserving every intentional
        // blank line in the middle of the note.
        int last = noteLines.size() - 1;
        while (last > 0 && noteLines.get(last).isEmpty()) {
            last--;
        }

        List<String> toSave = noteLines.subList(0, last + 1);
        SkyJewNotes.save(configDir, String.join("\n", toSave));
    }

    private void addLineAfter(int index) {
        syncVisibleLines();

        int insertAt = Math.max(0, Math.min(index + 1, noteLines.size()));
        noteLines.add(insertAt, "");

        int visible = visibleLineCount();
        if (insertAt >= scrollOffset + visible) {
            scrollOffset = Math.max(0, insertAt - visible + 1);
        }

        init();

        int focusedIndex = insertAt - scrollOffset;
        if (focusedIndex >= 0 && focusedIndex < visibleLines.size()) {
            visibleLines.get(focusedIndex).setFocused(true);
            setFocused(visibleLines.get(focusedIndex));
        }
    }

    private void scroll(int amount) {
        syncVisibleLines();

        int visible = visibleLineCount();
        int maxScroll = Math.max(0, noteLines.size() - visible);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - amount));

        init();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.hasControlDown() && event.key() == GLFW.GLFW_KEY_S) {
            save();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            for (int i = 0; i < visibleLines.size(); i++) {
                if (visibleLines.get(i).isFocused()) {
                    addLineAfter(scrollOffset + i);
                    return true;
                }
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll((int) Math.signum(scrollY) * 3);
        }
        return true;
    }

    @Override
    public void onClose() {
        save();
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);
        g.fill(0, 0, width, 2, 0xFF4A90E2);

        int left = Math.max(20, width / 2 - 370);
        int right = Math.min(width - 20, width / 2 + 370);
        int bottom = height - 24;

        g.fill(left, 48, right, bottom, PANEL);
        g.fill(left, 48, left + 42, bottom, 0xFFF4F4F4);
        for (int i = 0; i < visibleLineCount(); i++) {
            int n = scrollOffset + i + 1;
            int y = 76 + i * LINE_HEIGHT;
            if (y >= bottom - 20) break;
            g.text(font, Integer.toString(n), left + 10, y + 3, 0xFF999999, false);
            g.fill(left + 42, y + 19, right, y + 20, 0xFFEAEAEA);
        }

        g.text(font, "SkyJew Notes", left + 18, 58, YELLOW, true);
        g.text(font, "Notepad • ENTER = new line • Ctrl+S = save", left + 150, 58, MUTED, false);

        int visible = visibleLineCount();
        int maxScroll = Math.max(0, noteLines.size() - visible);
        if (maxScroll > 0) {
            String page = "Lines " + (scrollOffset + 1) + "-" +
                Math.min(noteLines.size(), scrollOffset + visible) +
                " / " + noteLines.size();
            g.text(font, page, right - 155, 58, CYAN, false);
        }

        g.text(font, "ESC", left + 18, bottom - 14, CYAN, true);
        g.text(font, "to save & close", left + 50, bottom - 14, MUTED, false);
        g.text(font, "Mouse wheel: scroll", right - 145, bottom - 14, MUTED, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }
}

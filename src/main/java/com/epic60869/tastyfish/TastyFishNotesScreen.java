package com.epic60869.tastyfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple persistent in-game notepad.
 *
 * Minecraft's standard EditBox is single-line, so the notepad uses multiple
 * lines that behave like a small text editor. Every line is saved when the
 * screen closes, including with ESC.
 */
public final class TastyFishNotesScreen extends Screen {
    private static final int BG = 0xFF080B12;
    private static final int PANEL = 0xF4141B27;
    private static final int BORDER = 0xFF26364D;
    private static final int TEXT = 0xFFF1F4FF;
    private static final int MUTED = 0xFF8D9AAF;
    private static final int CYAN = 0xFF58D8FF;
    private static final int YELLOW = 0xFFFFD34D;
    private static final int EDIT_BG = 0xFF0C121C;
    private static final int MAX_LINES = 40;
    private static final int LINE_HEIGHT = 24;

    private final Path configDir;
    private final List<EditBox> lines = new ArrayList<>();
    private int scrollOffset = 0;

    public TastyFishNotesScreen(Path configDir) {
        super(Component.literal("TastyFish Notes"));
        this.configDir = configDir;
    }

    @Override
    protected void init() {
        clearWidgets();
        lines.clear();

        String saved = TastyFishNotes.load(configDir);
        String[] savedLines = saved.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);

        int visible = Math.max(8, Math.min(MAX_LINES, (height - 105) / LINE_HEIGHT));
        int maxScroll = Math.max(0, Math.max(savedLines.length, 1) - visible);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        int start = scrollOffset;
        int end = Math.min(MAX_LINES, start + visible);
        int left = Math.max(30, width / 2 - 350);
        int fieldWidth = Math.min(700, width - left - 30);
        int top = 76;

        for (int i = start; i < end; i++) {
            EditBox box = new EditBox(font, left, top + (i - start) * LINE_HEIGHT,
                fieldWidth, 20, Component.literal("Note " + (i + 1)));
            String value = i < savedLines.length ? savedLines[i] : "";
            box.setValue(value);
            box.setMaxLength(500);
            box.setBordered(true);
            box.setHint(Component.literal("Type a note..."));
            addRenderableWidget(box);
            lines.add(box);
        }

        if (!lines.isEmpty()) {
            lines.get(0).setFocused(true);
        }
    }

    private void save() {
        // Preserve all lines, including blank lines. Reload the existing file
        // first so scrolling does not accidentally erase lines outside the view.
        String saved = TastyFishNotes.load(configDir);
        String[] old = saved.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int visibleStart = scrollOffset;

        int total = Math.max(old.length, visibleStart + lines.size());
        total = Math.min(MAX_LINES, Math.max(total, 1));
        String[] result = new String[total];
        for (int i = 0; i < total; i++) result[i] = i < old.length ? old[i] : "";

        for (int i = 0; i < lines.size(); i++) {
            result[visibleStart + i] = lines.get(i).getValue();
        }

        int last = result.length - 1;
        while (last > 0 && result[last].isEmpty()) last--;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i <= last; i++) {
            if (i > 0) out.append('\n');
            out.append(result[i]);
        }
        TastyFishNotes.save(configDir, out.toString());
    }

    private void scroll(int amount) {
        save();
        String saved = TastyFishNotes.load(configDir);
        int lineCount = saved.isEmpty() ? 1 : saved.split("\n", -1).length;
        int visible = Math.max(8, Math.min(MAX_LINES, (height - 105) / LINE_HEIGHT));
        int maxScroll = Math.max(0, Math.max(lineCount, MAX_LINES) - visible);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - amount));
        init();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) scroll((int) Math.signum(scrollY) * 3);
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
        g.fill(0, 0, width, 2, 0xFF5D3CFF);

        int left = Math.max(20, width / 2 - 370);
        int right = Math.min(width - 20, width / 2 + 370);
        int bottom = height - 24;

        g.fill(left, 48, right, bottom, PANEL);
        g.fill(left, 48, right, 49, BORDER);
        g.fill(left, bottom - 1, right, bottom, BORDER);

        g.text(font, "TastyFish Notes", left + 18, 58, YELLOW, true);
        g.text(font, "Your notes are saved automatically when you close this screen.", left + 150, 58, MUTED, false);

        int visible = Math.max(8, Math.min(MAX_LINES, (height - 105) / LINE_HEIGHT));
        int maxScroll = Math.max(0, MAX_LINES - visible);
        if (maxScroll > 0) {
            String page = "Lines " + (scrollOffset + 1) + "-" + Math.min(MAX_LINES, scrollOffset + visible) + " / " + MAX_LINES;
            g.text(font, page, right - 130, 58, CYAN, false);
        }

        g.text(font, "ESC", left + 18, bottom - 14, CYAN, true);
        g.text(font, "to save & close", left + 50, bottom - 14, MUTED, false);
        g.text(font, "Mouse wheel: scroll", right - 145, bottom - 14, MUTED, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }
}

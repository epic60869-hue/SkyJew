package com.epic60869.skyjew;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SkyJewFoxyScreen extends Screen {
    private final Screen parent;
    private long startedAt;

    public SkyJewFoxyScreen(Screen parent) {
        super(Component.literal(""));
        this.parent = parent;
    }

    @Override
    protected void init() {
        startedAt = System.currentTimeMillis();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - startedAt);
        float progress = Math.min(1.0F, elapsed / 900.0F);
        int pulse = (int) (Math.sin(elapsed * 0.06D) * 18.0D);

        // Full-screen black/red flash.
        g.fill(0, 0, width, height, 0xFF050000);

        int cx = width / 2;
        int cy = height / 2;

        // Stylised original animatronic fox face. It deliberately fills the screen
        // so the scare also works over inventories and other menus.
        int faceW = (int) (width * (0.58F + progress * 0.16F));
        int faceH = (int) (height * (0.68F + progress * 0.14F));
        int left = cx - faceW / 2;
        int right = cx + faceW / 2;
        int top = cy - faceH / 2;
        int bottom = cy + faceH / 2;

        // Ears.
        int earW = Math.max(28, faceW / 6);
        int earH = Math.max(40, faceH / 4);
        g.fill(left + earW / 2, top - earH / 2, left + earW, top + earH / 3, 0xFF242124);
        g.fill(right - earW, top - earH / 2, right - earW / 2, top + earH / 3, 0xFF242124);
        g.fill(left + earW / 2 + 5, top - earH / 2 + 5, left + earW - 5, top + 5, 0xFF6E1118);
        g.fill(right - earW + 5, top - earH / 2 + 5, right - earW / 2 - 5, top + 5, 0xFF6E1118);

        // Damaged metal face.
        g.fill(left, top, right, bottom, 0xFF373238);
        g.fill(left + 8, top + 8, right - 8, top + 16, 0xFF171419);
        g.fill(left + 10, bottom - 18, right - 10, bottom - 8, 0xFF171419);

        // Random-looking damage strips.
        g.fill(left + faceW / 9, top + faceH / 5, left + faceW / 3, top + faceH / 5 + 7, 0xFF171419);
        g.fill(right - faceW / 3, top + faceH / 3, right - faceW / 10, top + faceH / 3 + 8, 0xFF171419);
        g.fill(left + faceW / 5, bottom - faceH / 5, left + faceW / 2, bottom - faceH / 5 + 9, 0xFF171419);

        // Glowing eyes.
        int eyeY = cy - faceH / 10;
        int eyeSize = Math.max(26, faceW / 10);
        int eyeOffset = Math.max(45, faceW / 6);
        int eyeGlow = ((elapsed / 55L) & 1L) == 0L ? 0xFFFF2A20 : 0xFFFF8A45;

        g.fill(cx - eyeOffset - eyeSize, eyeY - eyeSize, cx - eyeOffset + eyeSize, eyeY + eyeSize, 0xFF090708);
        g.fill(cx + eyeOffset - eyeSize, eyeY - eyeSize, cx + eyeOffset + eyeSize, eyeY + eyeSize, 0xFF090708);
        g.fill(cx - eyeOffset - eyeSize / 2, eyeY - eyeSize / 2,
               cx - eyeOffset + eyeSize / 2, eyeY + eyeSize / 2, eyeGlow);
        g.fill(cx + eyeOffset - eyeSize / 2, eyeY - eyeSize / 2,
               cx + eyeOffset + eyeSize / 2, eyeY + eyeSize / 2, eyeGlow);
        g.fill(cx - eyeOffset - 4, eyeY - eyeSize / 2, cx - eyeOffset + 4, eyeY + eyeSize / 2, 0xFF050000);
        g.fill(cx + eyeOffset - 4, eyeY - eyeSize / 2, cx + eyeOffset + 4, eyeY + eyeSize / 2, 0xFF050000);

        // Snout.
        int snoutW = faceW / 3;
        int snoutTop = cy + faceH / 12;
        g.fill(cx - snoutW / 2, snoutTop, cx + snoutW / 2, snoutTop + faceH / 5, 0xFF50494C);
        g.fill(cx - snoutW / 6, snoutTop + 10, cx + snoutW / 6, snoutTop + 34, 0xFF12090B);

        // Huge mouth.
        int mouthTop = snoutTop + faceH / 7;
        int mouthBottom = Math.min(bottom - 25, mouthTop + faceH / 4);
        g.fill(cx - snoutW / 2, mouthTop, cx + snoutW / 2, mouthBottom, 0xFF080506);
        g.fill(cx - snoutW / 2 + 12, mouthTop + 8, cx + snoutW / 2 - 12, mouthTop + 15, 0xFF8B2028);

        // Teeth.
        int teeth = 7;
        int toothW = Math.max(8, snoutW / 18);
        for (int i = 0; i < teeth; i++) {
            int tx = cx - snoutW / 2 + 20 + i * ((snoutW - 40) / Math.max(1, teeth - 1));
            int toothH = Math.max(20, faceH / 18) + (i % 2) * 8;
            g.fill(tx - toothW, mouthTop + 20, tx + toothW, mouthTop + 20 + toothH, 0xFFE6DCC2);
        }

        // Red glitch bars.
        if (((elapsed / 35L) & 1L) == 0L) {
            g.fill(0, Math.max(0, cy - 120 + pulse), width, Math.max(0, cy - 110 + pulse), 0x66FF0000);
            g.fill(0, Math.min(height, cy + 150 + pulse), width, Math.min(height, cy + 158 + pulse), 0x66FF0000);
        }

        // Final white flash.
        if (elapsed > 760L) {
            int alpha = (int) (((elapsed - 760L) / 140.0F) * 90.0F);
            alpha = Math.max(0, Math.min(90, alpha));
            g.fill(0, 0, width, height, (alpha << 24) | 0x00FFFFFF);
        }

        if (elapsed >= 900L) {
            minecraft.execute(() -> minecraft.gui.setScreen(parent));
            return;
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return true;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return true;
    }

    @Override
    public void onClose() {
        // Permanently enabled and intentionally not dismissible.
    }
}

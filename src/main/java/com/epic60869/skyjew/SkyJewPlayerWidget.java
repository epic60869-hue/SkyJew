package com.epic60869.skyjew;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;

public final class SkyJewPlayerWidget extends AbstractWidget {
    private static final float FLIP_ROTATION = (float) Math.PI;
    private final RemotePlayer player;
    private float xRotation = -10;
    private float yRotation = 225;

    public SkyJewPlayerWidget(int x, int y, int width, int height, RemotePlayer player) {
        super(x, y, width, height, Component.empty());
        this.player = player;
        this.player.yHeadRot = this.player.yHeadRotO = 0;
    }

    @Override
    protected void onDrag(MouseButtonEvent click, double offsetX, double offsetY) {
        super.onDrag(click, offsetX, offsetY);
        xRotation = Mth.clamp(xRotation - (float) offsetY * 2.5f, -50f, 50f);
        yRotation += (float) offsetX * 2.5f;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY(), getRight(), getBottom(), 0xFF15171A);
        graphics.fill(getX(), getY(), getRight(), getY() + 1, 0xFF4A4D52);
        graphics.fill(getX(), getBottom() - 1, getRight(), getBottom(), 0xFF4A4D52);
        graphics.fill(getX(), getY(), getX() + 1, getBottom(), 0xFF4A4D52);
        graphics.fill(getRight() - 1, getY(), getRight(), getBottom(), 0xFF4A4D52);

        float size = 64f;
        Vector3f translation = new Vector3f(0, player.getBbHeight() / 2f + 0.0625f, 0);
        Quaternionf rotation = new Quaternionf().rotationXYZ(
            -xRotation * Mth.DEG_TO_RAD,
            -yRotation * Mth.DEG_TO_RAD,
            FLIP_ROTATION
        );
        EntityRenderState state = SkyJewInventoryScreenInvoker.invokeExtractRenderState(player);
        graphics.entity(state, size, translation, rotation, null, getX(), getY(), getRight(), getBottom());
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput builder) {}
    @Override public void playDownSound(SoundManager soundManager) {}
}
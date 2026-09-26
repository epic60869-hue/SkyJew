package com.epic60869.skyballs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

public final class SkyBallsFoxy {
    private static final Random RANDOM = new Random();
    private static int tickCounter;

    private SkyBallsFoxy() {}

    public static void tick(Minecraft mc) {
        tickCounter++;

        // One independent 1/10,000 roll every second, matching the linked datapack's default.
        if (tickCounter < 20) return;
        tickCounter = 0;

        if (mc.gui.screen() instanceof SkyBallsFoxyScreen) return;
        // Never in dungeons, where a surprise screen would ruin a run.
        if (com.epic60869.skyballs.features.core.SkyBallsLocation.inDungeon()) return;

        if (RANDOM.nextInt(10_000) == 0) {
            mc.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.GHAST_SCREAM, 1.5F, 0.72F)
            );
            mc.gui.setScreen(new SkyBallsFoxyScreen(mc.gui.screen()));
        }
    }
}

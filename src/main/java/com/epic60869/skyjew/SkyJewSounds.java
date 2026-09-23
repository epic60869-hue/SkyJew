package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public final class SkyJewSounds {
    private static final Identifier FIRST_BOOT_ID =
        Identifier.fromNamespaceAndPath("skyjew", "first_boot_ratted");

    public static final SoundEvent FIRST_BOOT_RATTED = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        FIRST_BOOT_ID,
        SoundEvent.createVariableRangeEvent(FIRST_BOOT_ID)
    );

    private static boolean firstBootPlayed;
    private static SimpleSoundInstance firstBootSound;

    private SkyJewSounds() {}

    public static void initialize() {
        // Forces class initialization so FIRST_BOOT_RATTED is registered
        // during client startup.
    }

    /**
     * Plays the first-boot audio and keeps it looping until the user
     * acknowledges the first-boot screen.
     */
    public static void playFirstBoot() {
        if (firstBootPlayed) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        firstBootPlayed = true;

        // This uses the same non-positional/master-style playback as a UI sound,
        // but enables the SoundInstance loop flag so the OGG repeats indefinitely.
        firstBootSound = new SimpleSoundInstance(
            FIRST_BOOT_ID,
            SoundSource.MASTER,
            1.0F,
            1.0F,
            RandomSource.create(),
            true,
            0,
            SoundInstance.Attenuation.NONE,
            0.0D,
            0.0D,
            0.0D,
            true
        );

        mc.getSoundManager().play(firstBootSound);
    }

    public static void tickFirstBoot() {
        playFirstBoot();
    }

    public static void stopFirstBoot() {
        if (firstBootSound != null) {
            Minecraft.getInstance().getSoundManager().stop(firstBootSound);
            firstBootSound = null;
        }
    }
}

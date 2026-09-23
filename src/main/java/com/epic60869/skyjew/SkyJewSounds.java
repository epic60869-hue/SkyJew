package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class SkyJewSounds {
    private static final Identifier FIRST_BOOT_ID =
        Identifier.fromNamespaceAndPath("skyjew", "first_boot_ratted");

    public static final SoundEvent FIRST_BOOT_RATTED = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        FIRST_BOOT_ID,
        SoundEvent.createVariableRangeEvent(FIRST_BOOT_ID)
    );

    private static boolean firstBootPlayed;

    private SkyJewSounds() {}

    public static void initialize() {
        // Forces class initialization so FIRST_BOOT_RATTED is registered
        // during client startup.
    }

    /**
     * Plays the first-boot audio exactly once.
     *
     * The bundled OGG is about 5.07 seconds long. The previous implementation
     * restarted it every 80 ticks (~4 seconds), which cut the audio off before
     * it finished. It is now allowed to play naturally to completion.
     */
    public static void playFirstBoot() {
        if (firstBootPlayed) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        firstBootPlayed = true;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(FIRST_BOOT_RATTED, 1.0F));
    }

    public static void tickFirstBoot() {
        playFirstBoot();
    }

    public static void stopFirstBoot() {
        // The first-boot sound is a normal one-shot UI sound. We intentionally
        // do not restart it or forcibly cut it off while the screen is open.
    }
}

package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;

public final class SkyJewSounds {
    private static final Identifier FIRST_BOOT_ID =
        Identifier.fromNamespaceAndPath("skyjew", "first_boot_ratted");

    public static final SoundEvent FIRST_BOOT_RATTED = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        FIRST_BOOT_ID,
        SoundEvent.createVariableRangeEvent(FIRST_BOOT_ID)
    );

    private static SimpleSoundInstance firstBootInstance;
    private static int firstBootTicks;

    private SkyJewSounds() {}

    public static void initialize() {}

    public static void playFirstBoot() {
        Minecraft mc = Minecraft.getInstance();
        stopFirstBoot();
        firstBootInstance = SimpleSoundInstance.forUI(FIRST_BOOT_RATTED, 1.0F);
        firstBootTicks = 0;
        mc.getSoundManager().play(firstBootInstance);
    }

    public static void tickFirstBoot() {
        if (firstBootInstance == null) {
            playFirstBoot();
            return;
        }

        if (++firstBootTicks >= 80) {
            playFirstBoot();
        }
    }

    public static void stopFirstBoot() {
        Minecraft mc = Minecraft.getInstance();
        if (firstBootInstance != null) {
            mc.getSoundManager().stop(firstBootInstance);
            firstBootInstance = null;
        }
        firstBootTicks = 0;
    }
}

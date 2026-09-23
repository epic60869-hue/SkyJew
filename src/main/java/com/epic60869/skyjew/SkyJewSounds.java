package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Registry;

public final class SkyJewSounds {
    private static final Identifier FIRST_BOOT_ID =
        Identifier.fromNamespaceAndPath("skyjew-mod", "first_boot_ratted");

    public static final SoundEvent FIRST_BOOT_RATTED = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        FIRST_BOOT_ID,
        SoundEvent.createVariableRangeEvent(FIRST_BOOT_ID)
    );

    private static LoopingSoundInstance firstBootInstance;

    private SkyJewSounds() {}

    public static void initialize() {
        // Forces class initialization and SoundEvent registration.
    }

    public static void playFirstBoot() {
        Minecraft mc = Minecraft.getInstance();
        stopFirstBoot();

        firstBootInstance = new LoopingSoundInstance(FIRST_BOOT_RATTED);
        mc.getSoundManager().play(firstBootInstance);
    }

    public static void stopFirstBoot() {
        Minecraft mc = Minecraft.getInstance();
        if (firstBootInstance != null) {
            mc.getSoundManager().stop(firstBootInstance);
            firstBootInstance = null;
        }
    }

    private static final class LoopingSoundInstance extends AbstractSoundInstance {
        private LoopingSoundInstance(SoundEvent sound) {
            super(sound, SoundSource.MASTER, RandomSource.create());
            this.looping = true;
            this.relative = true;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
        }

        @Override
        public boolean isRelative() {
            return true;
        }

        @Override
        public boolean isLooping() {
            return true;
        }

        @Override
        public SoundInstance.Attenuation getAttenuation() {
            return SoundInstance.Attenuation.NONE;
        }
    }
}

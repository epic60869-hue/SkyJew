package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Registry;

public final class SkyJewSounds {
    private static final Identifier FIRST_BOOT_ID =
        Identifier.fromNamespaceAndPath("skyjew", "first_boot_ratted");

    public static final SoundEvent FIRST_BOOT_RATTED = Registry.register(
        BuiltInRegistries.SOUND_EVENT,
        FIRST_BOOT_ID,
        SoundEvent.createVariableRangeEvent(FIRST_BOOT_ID)
    );

    private static FirstBootSoundInstance firstBootInstance;
    private static int firstBootTicks;

    private SkyJewSounds() {}

    public static void initialize() {
        // Forces class initialization and SoundEvent registration.
    }

    public static void playFirstBoot() {
        Minecraft mc = Minecraft.getInstance();
        stopFirstBoot();

        FirstBootSoundInstance instance = new FirstBootSoundInstance();
        firstBootInstance = instance;
        firstBootTicks = 0;
        mc.getSoundManager().play(instance);
    }

    /**
     * Keeps the first-boot audio alive even if the sound manager ends a
     * looping instance. The bundled clip is short, so restart it before the
     * two-second clip can finish.
     */
    public static void tickFirstBoot() {
        if (firstBootInstance == null) {
            playFirstBoot();
            return;
        }

        if (++firstBootTicks >= 100) {
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

    private static final class FirstBootSoundInstance extends AbstractTickableSoundInstance {
        private FirstBootSoundInstance() {
            super(FIRST_BOOT_RATTED, SoundSource.MASTER, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.relative = true;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
        }

        @Override
        public void tick() {
            // The sound manager handles the looping. Nothing needs updating
            // because this sound is fixed to the listener.
        }

        @Override
        public boolean canStartSilent() {
            return true;
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

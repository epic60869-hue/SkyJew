package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsConfig;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Misc > Random > Hide Explosions: drops explosion particles from explosion and particle packets. */
@Mixin(ClientPacketListener.class)
public abstract class SkyBallsHideExplosionsMixin {
    private static boolean skyballs$hide() {
        SkyBallsConfig config = SkyBallsConfig.current();
        return config != null && config.misc.random.hideExplosions;
    }

    @WrapWithCondition(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private boolean skyballs$explosionParticle(ClientLevel level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) {
        return !skyballs$hide();
    }

    @WrapWithCondition(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;trackExplosionEffects(Lnet/minecraft/world/phys/Vec3;FILnet/minecraft/util/random/WeightedList;)V"))
    private boolean skyballs$explosionBlockParticles(ClientLevel level, Vec3 center, float radius, int blockCount, WeightedList<?> particles) {
        return !skyballs$hide();
    }

    @Inject(method = "handleParticleEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void skyballs$explosionParticlePacket(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!skyballs$hide()) return;
        var type = packet.getParticle().getType();
        if (type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER) ci.cancel();
    }
}

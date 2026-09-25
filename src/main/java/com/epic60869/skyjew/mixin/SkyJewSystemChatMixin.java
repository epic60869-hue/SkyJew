package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.features.core.SkyJewChat;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds every system chat packet to SkyJew before any mod can hide the message. */
@Mixin(value = ClientPacketListener.class, priority = 500)
public abstract class SkyJewSystemChatMixin {
    @Inject(method = "handleSystemChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void skyjew$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        SkyJewChat.onPacket(packet.content(), packet.overlay());
    }
}

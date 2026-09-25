// Mixins supporting SkyJew's port of Skyblocker's dungeon/experiment code (Skyblocker is LGPL-3.0).
package com.epic60869.skyjew.sb.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundPingPacket;

import com.epic60869.skyjew.sb.events.ServerTickCallback;

/** Hypixel sends a ping packet every server tick; Skyblocker uses it as a server tick counter. */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ServerTickMixin {
	private static int skyjew$lastId = -1;

	@Inject(method = "handlePing", at = @At("RETURN"))
	private void skyjew$onServerTick(ClientboundPingPacket packet, CallbackInfo ci) {
		if (packet.getId() != skyjew$lastId) {
			skyjew$lastId = packet.getId();
			ServerTickCallback.EVENT.invoker().onTick();
		}
	}
}

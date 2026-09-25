// Ported from Skyblocker's mixins (LGPL-3.0) for SkyJew's dungeon port.
package com.epic60869.skyjew.sb.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.item.ItemEntity;

import com.epic60869.skyjew.sb.skyblock.dungeon.DungeonScore;
import com.epic60869.skyjew.sb.skyblock.dungeon.puzzle.TeleportMaze;
import com.epic60869.skyjew.sb.skyblock.dungeon.secrets.DungeonManager;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
	private void skyjew$beforeTeleport(ClientboundPlayerPositionPacket packet, CallbackInfo ci, @Share("playerBeforeTeleportBlockPos") LocalRef<BlockPos> beforeTeleport) {
		Minecraft minecraft = Minecraft.getInstance();
		beforeTeleport.set(minecraft.player.blockPosition().immutable());
	}

	@Inject(method = "handleMovePlayer", at = @At(value = "RETURN"))
	private void skyjew$onTeleport(ClientboundPlayerPositionPacket packet, CallbackInfo ci, @Share("playerBeforeTeleportBlockPos") LocalRef<BlockPos> beforeTeleport) {
		Minecraft minecraft = Minecraft.getInstance();
		if (beforeTeleport.get() != null) {
			TeleportMaze.INSTANCE.onTeleport(minecraft, beforeTeleport.get(), minecraft.player.blockPosition().immutable());
		}
		com.epic60869.skyjew.features.dungeons.OdinPuzzleSolvers.onTeleport(packet);
	}

	@Inject(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;getItem()Lnet/minecraft/world/item/ItemStack;"))
	private void skyjew$onItemPickup(ClientboundTakeItemEntityPacket packet, CallbackInfo ci, @Local(name = "itemEntity") ItemEntity itemEntity) {
		DungeonManager.onItemPickup(itemEntity);
	}

	@ModifyExpressionValue(method = "handleEntityEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundEntityEventPacket;getEntity(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"))
	private Entity skyjew$onEntityDeath(Entity entity, @Local(name = "packet") ClientboundEntityEventPacket packet) {
		if (packet.getEventId() == EntityEvent.DEATH) {
			DungeonScore.handleEntityDeath(entity);
			com.epic60869.skyjew.features.combat.ZealotCounter.onEntityDeath(entity);
		}
		return entity;
	}

	// Skyblocker's map update hook, missing from the port: without it the dungeon map texture only refreshed when a room was identified.
	@Inject(method = "handleMapItemData", at = @At("RETURN"))
	private void skyjew$onMapItemData(net.minecraft.network.protocol.game.ClientboundMapItemDataPacket packet, CallbackInfo ci) {
		com.epic60869.skyjew.sb.skyblock.dungeon.DungeonMapTexture.onMapItemDataUpdate(packet.mapId(), packet.colorPatch().isPresent());
	}

	@Inject(method = "handleSoundEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
	private void skyjew$onSound(ClientboundSoundPacket packet, CallbackInfo ci) {
		com.epic60869.skyjew.features.dungeons.DungeonRoutes.onSound(packet.getSound().value(), packet.getX(), packet.getY(), packet.getZ());
	}
}

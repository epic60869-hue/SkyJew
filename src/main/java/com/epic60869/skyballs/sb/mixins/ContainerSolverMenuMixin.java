// Mixins supporting SkyBalls's port of Skyblocker's dungeon/experiment code (Skyblocker is LGPL-3.0).
package com.epic60869.skyballs.sb.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

import com.epic60869.skyballs.sb.utils.container.ContainerSolverManager;

/** Recomputes solver highlights when slot contents change, as in Skyblocker's AbstractContainerMenuMixin. */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerSolverMenuMixin {
	@Shadow
	public abstract void broadcastChanges();

	@Inject(method = "setItem", at = @At("RETURN"))
	private void skyballs$onSetItem(int slot, int revision, ItemStack stack, CallbackInfo ci) {
		ContainerSolverManager.markHighlightsDirty();
		com.epic60869.skyballs.features.dungeons.OdinTerminals.onSetItem((AbstractContainerMenu) (Object) this, slot);
		// On the client, slot listeners (used by the Chronomatron solver) only fire from broadcastChanges.
		if ((Object) this instanceof ChestMenu) broadcastChanges();
	}

	@Inject(method = "initializeContents", at = @At("RETURN"))
	private void skyballs$onInitializeContents(int stateId, List<ItemStack> items, ItemStack carried, CallbackInfo ci) {
		ContainerSolverManager.markHighlightsDirty();
		com.epic60869.skyballs.features.dungeons.OdinTerminals.onSetItem((AbstractContainerMenu) (Object) this, 32);
		if ((Object) this instanceof ChestMenu) broadcastChanges();
	}
}

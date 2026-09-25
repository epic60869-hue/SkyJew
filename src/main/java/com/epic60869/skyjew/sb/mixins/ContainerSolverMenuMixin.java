// Mixins supporting SkyJew's port of Skyblocker's dungeon/experiment code (Skyblocker is LGPL-3.0).
package com.epic60869.skyjew.sb.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import com.epic60869.skyjew.sb.utils.container.ContainerSolverManager;

/** Recomputes solver highlights when slot contents change, as in Skyblocker's AbstractContainerMenuMixin. */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerSolverMenuMixin {
	@Inject(method = "setItem", at = @At("RETURN"))
	private void skyjew$onSetItem(int slot, int revision, ItemStack stack, CallbackInfo ci) {
		ContainerSolverManager.markHighlightsDirty();
	}

	@Inject(method = "initializeContents", at = @At("RETURN"))
	private void skyjew$onInitializeContents(int stateId, List<ItemStack> items, ItemStack carried, CallbackInfo ci) {
		ContainerSolverManager.markHighlightsDirty();
	}
}

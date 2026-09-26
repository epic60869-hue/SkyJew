// Mixins supporting SkyJew's port of Skyblocker's dungeon/experiment code (Skyblocker is LGPL-3.0).
package com.epic60869.skyjew.sb.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import com.epic60869.skyjew.sb.utils.container.ContainerSolverManager;

/** Draws solver highlights and forwards slot clicks, as in Skyblocker's AbstractContainerScreenMixin. */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerSolverScreenMixin {
	// Drawn right after the slots (the pose is already translated to the container) rather than in extractTooltip,
	// which other mods cancel inside terminals to hide item tooltips.
	@Inject(method = "extractSlots", at = @At("TAIL"))
	private void skyjew$drawSolverHighlights(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (com.epic60869.skyjew.features.dungeons.OdinTerminals.active()) {
			com.epic60869.skyjew.features.dungeons.OdinTerminals.render(graphics, screen);
			return;
		}
		ContainerSolverManager.onExtract(graphics, screen, screen.getMenu().slots);
	}

	@Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true)
	private void skyjew$onSlotClicked(Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci) {
		// SkyJew: Odin's terminal solver takes every click in a terminal (misclick and first-click protection).
		if (com.epic60869.skyjew.features.dungeons.OdinTerminals.active()) {
			if (com.epic60869.skyjew.features.dungeons.OdinTerminals.onSlotClicked(slot == null ? slotId : slot.index, button)) ci.cancel();
			return;
		}
		if (slot == null || ContainerSolverManager.getCurrentSolver() == null) return;
		if (ContainerSolverManager.onSlotClick(slotId, slot.getItem(), button)) ci.cancel();
	}
}

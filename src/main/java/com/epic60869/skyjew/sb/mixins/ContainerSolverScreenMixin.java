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

import com.epic60869.skyjew.sb.utils.container.ContainerSolver;
import com.epic60869.skyjew.sb.utils.container.ContainerSolverManager;
import com.epic60869.skyjew.sb.utils.container.StackDisplayModifier;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
		com.epic60869.skyjew.features.misc.PricePaid.onSlotClicked((AbstractContainerScreen<?>) (Object) this, slot);
		// SkyJew: Odin's terminal solver takes every click in a terminal (misclick and first-click protection).
		if (com.epic60869.skyjew.features.dungeons.OdinTerminals.active()) {
			if (com.epic60869.skyjew.features.dungeons.OdinTerminals.onSlotClicked(slot == null ? slotId : slot.index, button)) ci.cancel();
			return;
		}
		if (slot == null || ContainerSolverManager.getCurrentSolver() == null) return;
		if (ContainerSolverManager.onSlotClick(slotId, slot.getItem(), button)) ci.cancel();
	}

	@Shadow
	protected Slot hoveredSlot;

	// Solvers that change what a slot shows (e.g. Superpairs showing the cards you've revealed), from Skyblocker's
	// AbstractContainerScreenMixin.
	@ModifyArg(method = "extractSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;III)V"))
	private ItemStack skyjew$displayStackFake(ItemStack stack, @Local(argsOnly = true) Slot slot) {
		return skyjew$displayStack(slot, stack);
	}

	@ModifyArg(method = "extractSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;III)V"))
	private ItemStack skyjew$displayStackReal(ItemStack stack, @Local(argsOnly = true) Slot slot) {
		return skyjew$displayStack(slot, stack);
	}

	@ModifyArg(method = "extractSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
	private ItemStack skyjew$displayStackDecorations(ItemStack stack, @Local(argsOnly = true) Slot slot) {
		return skyjew$displayStack(slot, stack);
	}

	@ModifyVariable(method = "extractTooltip", at = @At(value = "STORE"), ordinal = 0)
	private ItemStack skyjew$displayTooltipStack(ItemStack stack) {
		return hoveredSlot == null ? stack : skyjew$displayStack(hoveredSlot, stack);
	}

	private ItemStack skyjew$displayStack(Slot slot, ItemStack stack) {
		ContainerSolver solver = ContainerSolverManager.getCurrentSolver();
		if (solver instanceof StackDisplayModifier modifier && solver.isSolverSlot(slot, (AbstractContainerScreen<?>) (Object) this)) {
			return modifier.modifyDisplayStack(slot.getContainerSlot(), stack);
		}
		return stack;
	}
}

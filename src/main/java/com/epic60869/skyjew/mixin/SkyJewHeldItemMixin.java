package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.features.misc.HeldItemModel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Misc > Held Item Model: transforms the first-person item. Minecraft 26.3 draws it from
 * FirstPersonHandsAndItemsRenderer.submitArmWithItem, so the item's submit call is wrapped.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class SkyJewHeldItemMixin {
    @WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void skyjew$transformHeldItem(ItemStackRenderState state, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline,
                                         Operation<Void> original, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) InteractionHand hand) {
        HeldItemModel.Transform transform = stack.isEmpty() ? null : HeldItemModel.transform(stack);
        if (transform == null) {
            original.call(state, pose, collector, light, overlay, outline);
            return;
        }
        var player = Minecraft.getInstance().player;
        HumanoidArm mainArm = player == null ? HumanoidArm.RIGHT : player.getMainArm();
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
        pose.pushPose();
        HeldItemModel.apply(pose, transform, arm == HumanoidArm.LEFT);
        original.call(state, pose, collector, light, overlay, outline);
        pose.popPose();
    }
}

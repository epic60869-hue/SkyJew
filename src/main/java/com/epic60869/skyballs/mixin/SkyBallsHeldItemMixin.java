package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.HeldItemModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Misc > Held Item Model: transforms the first-person item. The collector copies the pose, so popping at the end is safe. */
@Mixin(ItemInHandRenderer.class)
public abstract class SkyBallsHeldItemMixin {
    /** Misc > Held Item Model > No Swing Animation: the first-person hand and item never swing. */
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackAnim(F)F"))
    private float skyballs$noSwing(float attack) {
        return HeldItemModel.noSwing() ? 0f : attack;
    }

    private boolean skyballs$pushed;

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void skyballs$transformHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        skyballs$pushed = false;
        if (!context.firstPerson() || stack.isEmpty()) return;
        HeldItemModel.Transform transform = HeldItemModel.transform(stack);
        if (transform == null) return;
        pose.pushPose();
        HeldItemModel.apply(pose, transform, context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        skyballs$pushed = true;
    }

    @Inject(method = "renderItem", at = @At("TAIL"))
    private void skyballs$restoreHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (skyballs$pushed) pose.popPose();
        skyballs$pushed = false;
    }
}

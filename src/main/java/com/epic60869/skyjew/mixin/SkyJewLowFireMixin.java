package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Misc > Random > Low Fire: lowers the burning overlay. The collector copies the pose, so pop at the end is safe. */
@Mixin(ScreenEffectRenderer.class)
public abstract class SkyJewLowFireMixin {
    private static boolean skyjew$pushed;

    @Inject(method = "submitFire", at = @At("HEAD"))
    private static void skyjew$lowerFire(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
        SkyJewConfig config = SkyJewConfig.current();
        skyjew$pushed = config != null && config.misc.random.lowFire;
        if (!skyjew$pushed) return;
        poseStack.pushPose();
        poseStack.translate(0f, -config.misc.random.fireOffset, 0f);
    }

    @Inject(method = "submitFire", at = @At("TAIL"))
    private static void skyjew$restoreFire(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (skyjew$pushed) poseStack.popPose();
        skyjew$pushed = false;
    }
}

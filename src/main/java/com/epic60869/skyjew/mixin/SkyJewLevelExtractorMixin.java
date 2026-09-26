package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.features.core.SkyJewWorldRender;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws SkyJew's world renderers once per frame, into the frame's main-thread gizmos. */
@Mixin(LevelExtractor.class)
public abstract class SkyJewLevelExtractorMixin {
    @Inject(method = "extractGizmos", at = @At("HEAD"))
    private void skyjew$renderFrame(CallbackInfo ci) {
        try (Gizmos.TemporaryCollection ignored = ((LevelExtractor) (Object) this).collectPerFrameMainThreadGizmos()) {
            SkyJewWorldRender.renderFrame();
        }
    }
}

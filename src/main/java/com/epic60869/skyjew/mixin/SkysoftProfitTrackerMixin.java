package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkysoftProfitTrackerAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

/**
 * Bridges Skysoft's internal session tracker to SkyJew without making
 * Skysoft a hard compile/runtime dependency.
 */
@Mixin(targets = "com.skysoft.features.profit.ProfitTracker", remap = false)
public abstract class SkysoftProfitTrackerMixin implements SkysoftProfitTrackerAccessor {
    @Shadow @Final private Map<String, ?> sessionStats;

    @Override
    public Map<String, ?> skyjew$getSessionStats() {
        return sessionStats;
    }
}

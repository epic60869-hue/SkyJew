package com.epic60869.skyballs.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the custom data tag without copying it, which matters on the per-frame render path. */
@Mixin(CustomData.class)
public interface SkyBallsCustomDataAccessor {
    @Accessor("tag")
    CompoundTag skyballs$getTag();
}

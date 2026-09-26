package com.epic60869.skyballs.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface SkyBallsContainerScreenAccessor {
    @Accessor("leftPos")
    int skyballs$getLeftPos();

    @Accessor("topPos")
    int skyballs$getTopPos();

    @Accessor("hoveredSlot")
    Slot skyballs$getHoveredSlot();
}

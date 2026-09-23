package com.epic60869.skyjew;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface SkyJewContainerScreenAccessor {
    @Accessor("leftPos")
    int skyjew$getLeftPos();

    @Accessor("topPos")
    int skyjew$getTopPos();
}

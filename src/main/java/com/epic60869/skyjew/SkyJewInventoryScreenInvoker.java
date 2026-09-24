package com.epic60869.skyjew;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;

@Mixin(InventoryScreen.class)
public interface SkyJewInventoryScreenInvoker {
    @Invoker
    static EntityRenderState invokeExtractRenderState(LivingEntity entity) {
        throw new UnsupportedOperationException();
    }
}
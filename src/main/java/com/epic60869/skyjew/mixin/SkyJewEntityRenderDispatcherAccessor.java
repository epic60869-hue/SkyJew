package com.epic60869.skyjew.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderDispatcher.class)
public interface SkyJewEntityRenderDispatcherAccessor {
    @Accessor
    EquipmentAssetManager getEquipmentAssets();

    @Accessor
    PalettedTextureManager getPalettedTextures();
}

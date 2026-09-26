package com.epic60869.skyballs.mixin;

import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelManager.class)
public interface SkyBallsModelManagerAccessor {
    @Accessor("bakedItemStackModels")
    Map<Identifier, ItemModel> skyballs$getBakedItemStackModels();
}

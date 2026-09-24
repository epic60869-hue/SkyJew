package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewCustom;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(ItemModelResolver.class)
public abstract class SkyJewItemModelResolverMixin {
    @Unique
    private static final Map<ItemStack, Identifier> SKYJEW_ORIGINAL_MODELS =
        Collections.synchronizedMap(new IdentityHashMap<>());

    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void skyjew$applyCustomModel(ItemStackRenderState output, ItemStack item,
                                         ItemDisplayContext displayContext, Level level,
                                         ItemOwner owner, int seed, CallbackInfo ci) {
        if (item == null || item.isEmpty()) return;

        Identifier custom = SkyJewCustom.getItemIcon(item);
        if (custom == null) return;

        Identifier original = item.get(DataComponents.ITEM_MODEL);
        if (custom.equals(original)) return;

        SKYJEW_ORIGINAL_MODELS.put(item, original);
        item.set(DataComponents.ITEM_MODEL, custom);
    }

    @Inject(method = "appendItemLayers", at = @At("RETURN"))
    private void skyjew$restoreCustomModel(ItemStackRenderState output, ItemStack item,
                                           ItemDisplayContext displayContext, Level level,
                                           ItemOwner owner, int seed, CallbackInfo ci) {
        if (item == null || item.isEmpty()) return;

        Identifier original = SKYJEW_ORIGINAL_MODELS.remove(item);
        if (original == null) {
            item.remove(DataComponents.ITEM_MODEL);
        } else {
            item.set(DataComponents.ITEM_MODEL, original);
        }
    }
}

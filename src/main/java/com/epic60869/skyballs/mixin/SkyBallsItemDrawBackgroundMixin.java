package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsItemBackgrounds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws rarity backgrounds under items drawn while an inventory screen renders; every item draw goes through this method. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class SkyBallsItemDrawBackgroundMixin {
    @Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
    private void skyballs$drawRarityBackground(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        SkyBallsItemBackgrounds.drawInContainer((GuiGraphicsExtractor) (Object) this, stack, x, y);
    }
}

package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsStorageSearch;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class SkyBallsItemHighlightMixin {
    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("TAIL"))
    private void skyballs$highlight(ItemStack stack, int x, int y, CallbackInfo ci) {
        drawHighlight(stack, x, y);
    }

    @Inject(method = "fakeItem(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("TAIL"))
    private void skyballs$highlightFake(ItemStack stack, int x, int y, CallbackInfo ci) {
        drawHighlight(stack, x, y);
    }

    private void drawHighlight(ItemStack stack, int x, int y) {
        if (!SkyBallsStorageSearch.shouldHighlight(stack)) return;

        GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;
        int color = 0xFF55FFFF;
        self.fill(x, y, x + 16, y + 1, color);
        self.fill(x, y + 15, x + 16, y + 16, color);
        self.fill(x, y, x + 1, y + 16, color);
        self.fill(x + 15, y, x + 16, y + 16, color);
        SkyBallsStorageSearch.consumeHighlight();
    }
}

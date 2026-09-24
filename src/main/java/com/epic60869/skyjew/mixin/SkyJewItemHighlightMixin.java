package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewStorageSearch;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Highlights the item selected from /sj search wherever Minecraft submits an
 * ItemStack for rendering. This is deliberately not limited to
 * AbstractContainerScreen, so custom mod UIs receive the same highlight.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class SkyJewItemHighlightMixin {
    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("TAIL"))
    private void skyjew$highlight(ItemStack stack, int x, int y, CallbackInfo ci) {
        drawHighlight(stack, x, y);
    }

    @Inject(method = "fakeItem(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("TAIL"))
    private void skyjew$highlightFake(ItemStack stack, int x, int y, CallbackInfo ci) {
        drawHighlight(stack, x, y);
    }

    private void drawHighlight(ItemStack stack, int x, int y) {
        if (!SkyJewStorageSearch.shouldHighlight(stack)) return;

        GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;
        int color = 0xFF55FFFF;
        self.fill(x, y, x + 16, y + 1, color);
        self.fill(x, y + 15, x + 16, y + 16, color);
        self.fill(x, y, x + 1, y + 16, color);
        self.fill(x + 15, y, x + 16, y + 16, color);
        SkyJewStorageSearch.consumeHighlight();
    }
}

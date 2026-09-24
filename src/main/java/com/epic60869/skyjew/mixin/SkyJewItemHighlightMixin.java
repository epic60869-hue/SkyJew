package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewStorageSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class SkyJewItemHighlightMixin {
    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
    private void skyjew$rarityBackground(ItemStack stack, int x, int y, CallbackInfo ci) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.itemRarityBackground || stack == null || stack.isEmpty()) return;

        int rgb = rarityColor(stack);
        if (rgb < 0 || !isSkyBlockRarityColor(rgb)) return;

        GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;

        // Do not rely on the GUI texture atlas here. Custom textures are not
        // guaranteed to be present in the atlas on every resource-pack setup.
        // Draw a small anti-aliased-looking circle from horizontal strips so
        // the rarity background works in every inventory/GUI render path.
        int color = ARGB.color(150, rgb);
        self.fill(x + 5, y + 1, x + 11, y + 2, color);
        self.fill(x + 3, y + 2, x + 13, y + 3, color);
        self.fill(x + 2, y + 3, x + 14, y + 5, color);
        self.fill(x + 1, y + 5, x + 15, y + 11, color);
        self.fill(x + 2, y + 11, x + 14, y + 13, color);
        self.fill(x + 3, y + 13, x + 13, y + 14, color);
        self.fill(x + 5, y + 14, x + 11, y + 15, color);
    }

    private static int rarityColor(ItemStack stack) {
        final int[] found = {-1};
        stack.getHoverName().visit((style, value) -> {
            if (found[0] < 0 && style.getColor() != null) {
                int rgb = style.getColor().getValue() & 0xFFFFFF;
                if (isSkyBlockRarityColor(rgb)) found[0] = rgb;
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return found[0];
    }

    private static boolean isSkyBlockRarityColor(int rgb) {
        return switch (rgb) {
            case 0xFFFFFF, 0x55FF55, 0x5555FF, 0xAA00AA,
                 0xFFAA00, 0xFF55FF, 0x55FFFF, 0xFF5555,
                 0xAA0000 -> true;
            default -> false;
        };
    }

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

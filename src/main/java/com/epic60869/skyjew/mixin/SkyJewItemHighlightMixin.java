package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewStorageSearch;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
        if (rgb < 0) return;

        GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;
        try {
            // Draw the circle as a normal GUI texture, exactly like NoammAddons.
            // Do not use the GUI atlas here: a normal texture cannot become a
            // missing-texture sprite if another inventory renderer changes the atlas.
            Identifier circleTexture = Identifier.fromNamespaceAndPath(
                "skyjew", "textures/gui/item_background_circular.png");
            self.blit(
                RenderPipelines.GUI_TEXTURED,
                circleTexture,
                x, y,
                0, 0,
                16, 16,
                16, 16,
                ARGB.color(128, rgb));
        } catch (Throwable ignored) {
            // The GUI atlas is loaded asynchronously during resource reload.
        }
    }

    private static int rarityColor(ItemStack stack) {
        // Match Skyblocker's approach: SkyBlock rarity is primarily encoded
        // in the lore, not merely in the first component of the display name.
        List<Component> lore = new java.util.ArrayList<>();
        var loreData = stack.get(DataComponents.LORE);
        if (loreData != null) lore.addAll(loreData.lines());

        for (int i = lore.size() - 1; i >= 0; i--) {
            String line = lore.get(i).getString().toUpperCase(Locale.ROOT);
            int color = rarityNameColor(line);
            if (color >= 0) return color;
        }

        int tooltipStyleColor = tooltipStyleColor(stack);
        if (tooltipStyleColor >= 0) return tooltipStyleColor;

        final int[] found = {-1};
        stack.getHoverName().visit((style, value) -> {
            if (found[0] < 0 && style.getColor() != null) {
                int rgb = style.getColor().getValue() & 0xFFFFFF;
                if (isSkyBlockRarityColor(rgb)) found[0] = rgb;
            }
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return found[0];
    }

    private static int rarityNameColor(String line) {
        if (line.contains("VERY SPECIAL")) return 0xFF5555;
        if (line.contains("SPECIAL")) return 0xFF5555;
        if (line.contains("ULTIMATE")) return 0xAA0000;
        if (line.contains("ADMIN")) return 0xAA0000;
        if (line.contains("DIVINE")) return 0x55FFFF;
        if (line.contains("MYTHIC")) return 0xFF55FF;
        if (line.contains("LEGENDARY")) return 0xFFAA00;
        if (line.contains("EPIC")) return 0xAA00AA;
        if (line.contains("RARE")) return 0x5555FF;
        if (line.contains("UNCOMMON")) return 0x55FF55;
        if (line.contains("COMMON")) return 0xFFFFFF;
        return -1;
    }

    private static int tooltipStyleColor(ItemStack stack) {
        Identifier style = stack.get(DataComponents.TOOLTIP_STYLE);
        if (style == null || !style.getNamespace().equals("skyblock")) return -1;
        return rarityNameColor(style.getPath().toUpperCase(Locale.ROOT));
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

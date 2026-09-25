package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewStorageSearch;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.util.ARGB;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import com.epic60869.skyjew.SkyJewItemRarity;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class SkyJewItemHighlightMixin {
    @Inject(
        method = "renderSlotContents",
        at = @At("HEAD")
    )
    private void skyjew$rarityBackgroundOnContainerSlot(
        GuiGraphicsExtractor graphics,
        ItemStack stack,
        net.minecraft.world.inventory.Slot slot,
        String itemCount,
        CallbackInfo ci
    ) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.itemRarityBackground || slot == null) return;

        if (stack == null || stack.isEmpty()) return;

        drawRarityBackground(graphics, stack, slot.x, slot.y);
    }

    private static SkyJewItemRarity rarity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return SkyJewItemRarity.UNKNOWN;

        var lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            List<Component> lines = lore.lines();
            for (int i = lines.size() - 1; i >= 0; i--) {
                var found = SkyJewItemRarity.containsName(lines.get(i).getString());
                if (found.isPresent()) return found.get();
            }
        }

        Identifier tooltipStyle = stack.get(DataComponents.TOOLTIP_STYLE);
        if (tooltipStyle != null && tooltipStyle.getNamespace().equals("skyblock")) {
            return SkyJewItemRarity.containsName(
                tooltipStyle.getPath().toUpperCase(Locale.ROOT)
            ).orElse(SkyJewItemRarity.UNKNOWN);
        }

        return SkyJewItemRarity.UNKNOWN;
    }

    private static void drawRarityBackground(
        GuiGraphicsExtractor graphics,
        ItemStack stack,
        int x,
        int y
    ) {
        SkyJewItemRarity rarity = rarity(stack);
        if (rarity == SkyJewItemRarity.UNKNOWN) return;

        Minecraft mc = Minecraft.getInstance();
        TextureAtlasSprite sprite = mc.getAtlasManager()
            .getAtlasOrThrow(AtlasIds.GUI)
            .getSprite(SkyJewItemRarity.BACKGROUND_SPRITE);

        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            x, y, 16, 16,
            ARGB.color(170, rarity.color)
        );
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

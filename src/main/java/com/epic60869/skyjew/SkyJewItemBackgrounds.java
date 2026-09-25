package com.epic60869.skyjew;

import com.epic60869.skyjew.custom.util.Compat;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Item rarity backgrounds, ported from Skyblocker's ItemRarityBackground and
 * ItemUtils.getItemRarity (LGPL-3.0). Drawn behind items in container slots and the hotbar.
 */
public final class SkyJewItemBackgrounds {
    // ItemStack uses identity equality, so this caches per stack instance like Skyblocker's ItemStack field.
    private static final Map<ItemStack, SkyJewItemRarity> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private SkyJewItemBackgrounds() {}

    public enum Style {
        SQUARE("Square", "item_background_square"),
        CIRCULAR("Circular", "item_background_circular");

        private final String displayName;
        public final Identifier sprite;

        Style(String displayName, String sprite) {
            this.displayName = displayName;
            this.sprite = Identifier.fromNamespaceAndPath("skyjew", sprite);
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static AbstractContainerScreen<?> currentContainer;

    public static void beginContainer(AbstractContainerScreen<?> screen) {
        currentContainer = screen;
    }

    public static void endContainer() {
        currentContainer = null;
    }

    /** Called for every item drawn; draws only while an inventory screen is rendering, skipping the item on the cursor (drawn outside extractContents anyway). */
    public static void drawInContainer(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        AbstractContainerScreen<?> screen = currentContainer;
        if (screen == null || stack == screen.getMenu().getCarried()) return;
        draw(graphics, stack, x, y);
    }

    public static void draw(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.itemRarity.enabled || stack == null || stack.isEmpty()) return;
        if (!Compat.isOnSkyblock()) return;

        SkyJewItemRarity rarity = CACHE.computeIfAbsent(stack, SkyJewItemBackgrounds::rarity);
        if (rarity == SkyJewItemRarity.UNKNOWN) return;

        Style style = config.misc.itemRarity.style == null ? Style.SQUARE : config.misc.itemRarity.style;
        var sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(style.sprite);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16,
            ARGB.color(config.misc.itemRarity.opacity, rarity.color));
    }

    public static SkyJewItemRarity rarity(ItemStack stack) {
        if (stack.isEmpty()) return SkyJewItemRarity.UNKNOWN;

        // Pets
        CompoundTag customData = Compat.getCustomData(stack);
        if (customData.getStringOr("id", "").equals("PET")) {
            try {
                JsonObject petInfo = JsonParser.parseString(customData.getStringOr("petInfo", "")).getAsJsonObject();
                SkyJewItemRarity tier = SkyJewItemRarity.valueOf(petInfo.get("tier").getAsString());
                boolean tierBoost = petInfo.has("heldItem") && petInfo.get("heldItem").getAsString().equals("PET_ITEM_TIER_BOOST");
                return tierBoost ? tier.next() : tier;
            } catch (Exception e) {
                return SkyJewItemRarity.UNKNOWN;
            }
        }

        // Attempt to parse rarity from lore
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            List<Component> lines = lore.lines();
            for (int i = lines.size() - 1; i >= 0; i--) {
                Optional<SkyJewItemRarity> found = SkyJewItemRarity.containsName(lines.get(i).getString());
                if (found.isPresent()) return found.get();
            }
        }

        // Fall back to tooltip style for reforge stone core to work
        Identifier tooltipStyle = stack.get(DataComponents.TOOLTIP_STYLE);
        if (tooltipStyle != null && tooltipStyle.getNamespace().equals("hypixel_skyblock")) {
            return SkyJewItemRarity.containsName(tooltipStyle.getPath().toUpperCase(Locale.ENGLISH)).orElse(SkyJewItemRarity.UNKNOWN);
        }

        return SkyJewItemRarity.UNKNOWN;
    }
}

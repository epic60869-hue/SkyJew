package com.epic60869.skyjew.features.dungeons;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewItemBackgrounds;
import com.epic60869.skyjew.SkyJewItemRarity;
import com.epic60869.skyjew.SkyJewPriceTooltip;
import com.epic60869.skyjew.custom.util.Compat;
import com.epic60869.skyjew.features.FeatureConfigs;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * CS2-style case opening for dungeon reward chests, ported from SkyOcean's Dungeon Gambling: opening an Obsidian or
 * Bedrock chest (or any chest, if turned on) spins a strip of item cards past a red marker, slowing down and stopping
 * on the chest's most valuable item, whose name then grows onto the screen. The menu can't be clicked while it
 * spins; Esc skips it. When the winner is Legendary (gold) or better, the "GOLD GOLD GOLD" sound plays
 * (assets/skyjew/sounds/gold.ogg; a resource pack can replace it).
 */
public final class CaseOpening {
    private static final Pattern CHEST = Pattern.compile("^(?<type>Wood|Gold|Diamond|Emerald|Obsidian|Bedrock)(?: Chest)?$");
    private static final Identifier GOLD_SOUND = Identifier.fromNamespaceAndPath("skyjew", "gold");

    private static final int ITEM_SCALE = 4;
    private static final int ITEMS = 50;
    private static final int WINNER_INDEX = ITEMS - 10;
    private static final int GAP = 5;
    private static final int CARD_W = 24;
    private static final int CARD_H = 18;
    private static final int FULL_W = CARD_W * ITEM_SCALE + GAP;
    private static final int FULL_H = CARD_H * ITEM_SCALE;

    private static final List<ItemStack> reel = new ArrayList<>();
    private static long start = -1;
    private static int randomOffset;
    private static int lastSound;
    private static boolean goldPlayed;
    private static int menuId = -1;

    private CaseOpening() {}

    private static FeatureConfigs.Dungeons config() {
        SkyJewConfig c = SkyJewConfig.current();
        return c == null ? null : c.dungeons;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;
            ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> {
                maybeStart(container);
                if (running(container)) render(g);
            });
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !running(container));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> !running(container));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
                if (!running(container)) return true;
                if (event.key() == GLFW.GLFW_KEY_ESCAPE) stop(); // skip the animation, then Esc works as normal
                return false;
            });
            ScreenEvents.remove(screen).register(s -> {
                stop();
                menuId = -1;
            });
        });
    }

    private static boolean running(AbstractContainerScreen<?> screen) {
        return start > 0 && screen.getMenu().containerId == menuId && !reel.isEmpty();
    }

    private static void stop() {
        start = -1;
        reel.clear();
    }

    /** Starts once per chest, as soon as its items have arrived. */
    private static void maybeStart(AbstractContainerScreen<?> screen) {
        FeatureConfigs.Dungeons c = config();
        if (c == null || !c.caseOpening || !Compat.isOnSkyblock()) return;
        int id = screen.getMenu().containerId;
        if (id == menuId) return;
        var m = CHEST.matcher(ChatFormatting.stripFormatting(screen.getTitle().getString()).trim());
        if (!m.matches()) return;
        String type = m.group("type");
        if (!c.caseOpeningAllChests && !type.equals("Obsidian") && !type.equals("Bedrock")) return;

        List<ItemStack> loot = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof Inventory) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()
                || stack.is(Items.BARRIER) || stack.is(Items.ARROW) || stack.is(Items.CHEST)) continue;
            if (Compat.neuName(stack).isEmpty()) continue;
            loot.add(stack);
        }
        if (loot.isEmpty()) return; // items not here yet; try again next frame
        menuId = id;

        // The winner is the most valuable item in the chest, like SkyOcean.
        ItemStack winner = loot.getFirst();
        double best = -1;
        for (ItemStack stack : loot) {
            double value = SkyJewPriceTooltip.unitPrice(SkyJewPriceTooltip.marketId(stack)) * stack.getCount();
            if (value > best) {
                best = value;
                winner = stack;
            }
        }
        reel.clear();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < ITEMS; i++) reel.add(loot.get(random.nextInt(loot.size())));
        reel.set(WINNER_INDEX, winner);
        randomOffset = ((4 * ITEM_SCALE) + random.nextInt(4 * ITEM_SCALE)) * (random.nextBoolean() ? 1 : -1);
        lastSound = 0;
        goldPlayed = false;
        start = System.currentTimeMillis();
    }

    private static float ease(float t) {
        return t < 0.5f ? (float) ((1 - Math.sqrt(1 - Math.pow(2 * t, 2))) / 2)
            : (float) ((Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2);
    }

    private static void render(GuiGraphicsExtractor g) {
        FeatureConfigs.Dungeons c = config();
        float seconds = c == null ? 6 : c.caseOpeningSeconds;
        int w = g.guiWidth();
        int h = g.guiHeight();
        // Cover the chest while it spins.
        g.fill(0, 0, w, h, 0xE0101010);

        float raw = (System.currentTimeMillis() - start) / (seconds * 1000f);
        float progress = Mth.clamp(raw + 0.25f, 0f, 1f);
        float endOffset = (WINNER_INDEX * FULL_W) * ease(progress);
        if (progress >= 0.96f && raw >= 1.4f) {
            stop();
            return;
        }

        int soundIndex = (int) endOffset / FULL_W;
        Minecraft mc = Minecraft.getInstance();
        if (soundIndex > lastSound) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 2f, 1f));
            lastSound = soundIndex;
        }

        g.pose().pushMatrix();
        g.pose().translate(w / 2f - endOffset - (8 * ITEM_SCALE + randomOffset), (h - FULL_H) / 2f);
        for (int i = 0; i < reel.size(); i++) {
            ItemStack item = reel.get(i);
            g.pose().pushMatrix();
            g.pose().translate(i * FULL_W, 0);
            g.pose().scale(ITEM_SCALE, ITEM_SCALE);
            card(g, item, ARGB.opaque(rarityColour(item)));
            g.pose().popMatrix();
        }
        g.pose().popMatrix();

        // The red marker in the middle.
        g.fill(w / 2 - 1, (h - FULL_H) / 2 - 6, w / 2 + 1, (h + FULL_H) / 2 + FULL_H / 4, 0xFFFF5555);

        if (progress >= 0.96f) {
            ItemStack winner = reel.get(WINNER_INDEX);
            Component name = Compat.realName(winner);
            float scale = Mth.lerp(Math.min(1f, (progress - 0.96f) / 0.04f), 1f, 3f);
            g.pose().pushMatrix();
            g.pose().translate(w / 2f, h * 0.2f);
            g.pose().scale(scale, scale);
            g.pose().translate(-mc.font.width(name) / 2f, 0);
            g.text(mc.font, name, 0, 0, 0xFFFFFFFF, true);
            g.pose().popMatrix();
            if (!goldPlayed) {
                goldPlayed = true;
                if (isGold(winner) && (c == null || c.caseOpeningGoldSound)) playGold();
            }
        }
    }

    /** SkyOcean's card: a dark box fading into the rarity colour, a rarity line at the bottom, and the item. */
    private static void card(GuiGraphicsExtractor g, ItemStack item, int colour) {
        int tint = ARGB.color(0x60, colour);
        g.fillGradient(0, 0, CARD_W, CARD_H - 1, 0x80303030, tint);
        g.fill(0, CARD_H - 1, CARD_W, CARD_H, colour);
        g.item(item, CARD_W / 2 - 8, CARD_H / 2 - 8);
    }

    private static int rarityColour(ItemStack stack) {
        SkyJewItemRarity rarity = SkyJewItemBackgrounds.rarity(stack);
        return rarity == null || rarity == SkyJewItemRarity.UNKNOWN ? 0xFFFFFF : rarity.color;
    }

    /** "Gold" like a CS2 knife: Legendary or better. */
    private static boolean isGold(ItemStack stack) {
        SkyJewItemRarity rarity = SkyJewItemBackgrounds.rarity(stack);
        return rarity != null && rarity != SkyJewItemRarity.UNKNOWN && rarity.ordinal() >= SkyJewItemRarity.LEGENDARY.ordinal();
    }

    private static void playGold() {
        Minecraft mc = Minecraft.getInstance();
        boolean custom = mc.getResourceManager().getResource(Identifier.fromNamespaceAndPath("skyjew", "sounds/gold.ogg")).isPresent();
        SoundEvent sound = custom ? SoundEvent.createVariableRangeEvent(GOLD_SOUND) : SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1f, 1f));
    }
}

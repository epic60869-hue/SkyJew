package com.epic60869.skyjew.features.skills;

import com.epic60869.skyjew.ItemPriceResolver;
import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewTabWidgetManager;
import com.epic60869.skyjew.features.combat.CombatFeatures;
import com.epic60869.skyjew.features.core.SkyJewAlerts;
import com.epic60869.skyjew.features.core.SkyJewChat;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sweep display, Alchemy 50 estimate, valuable rune alert, Safari tracker and Hunting Box value. */
public final class SkillFeatures {
    // Sweep and hunting patterns from SkyHanni's repo (MIT) and Skyblocker's HuntingBoxHelper (LGPL-3.0).
    private static final Pattern SWEEP = Pattern.compile("^\\s*Sweep: (?<value>[\\d,.]+)");
    private static final Pattern TOUGHNESS = Pattern.compile("^\\s*(?<tree>[\\S ]+) Tree Toughness: (?<toughness>[\\d,.]+) (?<logs>[\\d,.]+) Logs");
    private static final Pattern ALCHEMY_TAB = Pattern.compile("^\\s*Alchemy (?<level>\\d+): (?:(?<pct>[\\d.]+)%|MAX)");
    private static final Pattern ALCHEMY_BAR = Pattern.compile("\\+[\\d,.]+ Alchemy \\((?<current>[\\d,.]+)/(?<needed>[\\d,.]+[kM]?)\\)");
    private static final Pattern RUNE = Pattern.compile("◆\\s*(?<name>[A-Za-z' ]+?) Rune\\b");
    private static final Pattern CAPTURED = Pattern.compile("Captured Mobs: (?<count>\\d+)");
    private static final Pattern HUNTING_BOX = Pattern.compile("^(?:\\(\\d+/\\d+\\) )?Hunting Box$");
    private static final Pattern OWNED = Pattern.compile("Owned: (?<count>[\\d,]+) Shards?");

    /** XP needed for each skill level 1-50 (standard SkyBlock skill table). */
    private static final long[] SKILL_XP = {
        50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500, 5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000,
        100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000, 1300000,
        1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000, 2300000, 2400000, 2500000,
        2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000};

    private static String sweep;
    private static final Map<String, String> SWEEP_LOGS = new LinkedHashMap<>();
    private static int alchemyLevel = -1;
    private static double alchemyFraction = -1;
    private static int lastCaptured = -1;
    private static final Map<String, Integer> CAPTURES = new LinkedHashMap<>();
    private static int ticks;

    private SkillFeatures() {}

    private static SkyJewConfig config() {
        return SkyJewConfig.current();
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 20 == 0) tick(mc);
        });
        SkyJewChat.onChat(SkillFeatures::onChat);
        SkyJewChat.onActionBar(SkillFeatures::onActionBar);
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof AbstractContainerScreen<?> container && HUNTING_BOX.matcher(screen.getTitle().getString()).matches()) {
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
                    SkyJewConfig c = config();
                    if (c == null || !c.hunting.huntingBoxValue) return;
                    double value = huntingBoxValue(container);
                    graphics.text(Minecraft.getInstance().font,
                        Component.literal("Hunting Box value: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(CombatFeatures.formatCoins(value) + " coins").withStyle(ChatFormatting.GOLD)),
                        4, 4, 0xFFFFFFFF, true);
                });
            }
        });

        SkyJewHuds.register("sweep", "Sweep Display",
            () -> config() != null && config().foraging.sweepDisplay && sweep != null && holdingAxe(),
            SkillFeatures::sweepLines,
            List.of(kv("Sweep: ", "150"), kv("Oak: ", "12 Logs")),
            8, 620);
        SkyJewHuds.register("alchemy", "Alchemy 50 Estimate",
            () -> config() != null && config().alchemy.progressEstimate && alchemyLevel >= 0,
            SkillFeatures::alchemyLines,
            List.of(kv("Alchemy ", "40 (23%)"), kv("To 50: ", "~12,345 potions")),
            8, 660);
        SkyJewHuds.register("safari", "Safari Critter Tracker",
            () -> config() != null && config().hunting.safariTracker && !CAPTURES.isEmpty(),
            SkillFeatures::safariLines,
            List.of(kv("Safari captures", ""), kv("Cave: ", "3"), kv("Forest: ", "5")),
            8, 700);
    }

    private static Component kv(String key, String value) {
        return Component.literal(key).withStyle(ChatFormatting.GRAY).append(Component.literal(value).withStyle(ChatFormatting.GREEN));
    }

    private static boolean holdingAxe() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getMainHandItem().getItem().toString().contains("axe") && !player.getMainHandItem().getItem().toString().contains("pickaxe");
    }

    private static List<Component> sweepLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(kv("Sweep: ", sweep));
        SWEEP_LOGS.forEach((tree, logs) -> lines.add(kv(tree + ": ", logs + " Logs")));
        return lines;
    }

    private static List<Component> alchemyLines() {
        double xpPerPotion = Math.max(1, config().alchemy.xpPerPotion);
        long remaining = 0;
        if (alchemyLevel < 50) {
            double currentLevelLeft = SKILL_XP[alchemyLevel] * (1 - Math.max(0, alchemyFraction));
            remaining = (long) currentLevelLeft;
            for (int level = alchemyLevel + 1; level < 50; level++) remaining += SKILL_XP[level];
        }
        String progress = alchemyFraction >= 0 ? String.format(Locale.US, " (%.1f%%)", alchemyFraction * 100) : "";
        return List.of(
            kv("Alchemy ", alchemyLevel + progress),
            kv("To 50: ", remaining == 0 ? "done" : String.format(Locale.US, "%,d XP, ~%,d potions", remaining, (long) Math.ceil(remaining / xpPerPotion))));
    }

    private static List<Component> safariLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Safari captures").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        CAPTURES.forEach((biome, count) -> lines.add(kv(biome + ": ", String.valueOf(count))));
        return lines;
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || !SkyJewLocation.onSkyblock()) return;
        for (PlayerInfo info : SkyJewTabWidgetManager.players()) {
            if (info.getTabListDisplayName() == null) continue;
            String text = SkyJewLocation.strip(info.getTabListDisplayName().getString());
            Matcher m = SWEEP.matcher(text);
            if (m.find()) sweep = m.group("value");
            m = ALCHEMY_TAB.matcher(text);
            if (m.find()) {
                alchemyLevel = Integer.parseInt(m.group("level"));
                if (m.group("pct") != null) alchemyFraction = Double.parseDouble(m.group("pct")) / 100.0;
            }
        }

        // Sweep Details lines (tree toughness and logs) shown in item lore, e.g. on your axe.
        ItemLore lore = mc.player.getMainHandItem().get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                Matcher m = TOUGHNESS.matcher(SkyJewLocation.strip(line.getString()));
                if (m.find()) SWEEP_LOGS.put(m.group("tree"), m.group("logs"));
            }
        }

        boolean inSafari = false;
        for (String line : SkyJewLocation.scoreboard()) {
            Matcher m = CAPTURED.matcher(line);
            if (!m.find()) continue;
            inSafari = true;
            int count = Integer.parseInt(m.group("count"));
            if (lastCaptured >= 0 && count > lastCaptured) {
                String biome = SkyJewLocation.location().isEmpty() ? "Unknown" : SkyJewLocation.location();
                CAPTURES.merge(biome, count - lastCaptured, Integer::sum);
            }
            lastCaptured = count;
        }
        if (!inSafari) lastCaptured = -1;
    }

    private static void onActionBar(SkyJewChat.Message message) {
        Matcher m = ALCHEMY_BAR.matcher(message.text());
        if (!m.find()) return;
        double current = parse(m.group("current"));
        double needed = parse(m.group("needed"));
        if (needed > 0) alchemyFraction = Math.min(1, current / needed);
    }

    private static void onChat(SkyJewChat.Message message) {
        SkyJewConfig c = config();
        if (c == null || !c.runecrafting.valuableRuneAlert) return;
        Matcher m = RUNE.matcher(message.text());
        if (!m.find()) return;
        String rune = m.group("name").trim();
        boolean wanted = Arrays.stream(c.runecrafting.runes.split(","))
            .map(s -> s.trim().toLowerCase(Locale.ROOT))
            .anyMatch(s -> !s.isEmpty() && rune.toLowerCase(Locale.ROOT).equals(s));
        if (wanted) {
            SkyJewAlerts.title(Component.literal(rune + " Rune!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), Component.empty());
        }
    }

    private static double huntingBoxValue(AbstractContainerScreen<?> screen) {
        double total = 0;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof net.minecraft.world.entity.player.Inventory) continue;
            ItemStack stack = slot.getItem();
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;
            for (Component line : lore.lines()) {
                Matcher m = OWNED.matcher(SkyJewLocation.strip(line.getString()));
                if (!m.find()) continue;
                int owned = Integer.parseInt(m.group("count").replace(",", ""));
                String name = SkyJewLocation.strip(stack.getHoverName().getString()).trim();
                double price = ItemPriceResolver.valueByName(name);
                if (price <= 0) {
                    String base = name.replaceAll("(?i)\\s*shard$", "").trim().toUpperCase(Locale.ROOT).replace(' ', '_');
                    price = ItemPriceResolver.value("SHARD_" + base);
                }
                total += price * owned;
                break;
            }
        }
        return total;
    }

    private static double parse(String value) {
        String v = value.replace(",", "");
        double mult = 1;
        if (v.endsWith("k")) { mult = 1_000; v = v.substring(0, v.length() - 1); }
        else if (v.endsWith("M")) { mult = 1_000_000; v = v.substring(0, v.length() - 1); }
        return Double.parseDouble(v) * mult;
    }
}

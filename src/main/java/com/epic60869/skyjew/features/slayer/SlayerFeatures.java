package com.epic60869.skyjew.features.slayer;

import com.epic60869.skyjew.ItemPriceResolver;
import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.custom.util.Compat;
import com.epic60869.skyjew.features.FeatureConfigs;
import com.epic60869.skyjew.features.combat.CombatFeatures;
import com.epic60869.skyjew.features.core.SkyJewChat;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slayer tracker, boss phase display and profit tracker. Boss XP and spawn costs per tier are
 * from SkyHanni's repo (constants/Slayer.json, MIT).
 */
public final class SlayerFeatures {
    private static final Pattern PROGRESS = Pattern.compile("\\((?<current>[\\d,.]+k?)/(?<max>[\\d,.]+k?)\\) Combat XP");
    private static final Pattern BOSS = Pattern.compile("(?<boss>Revenant Horror|Tarantula Broodfather|Sven Packmaster|Voidgloom Seraph|Inferno Demonlord|Riftstalker Bloodfiend) (?<tier>[IV]+)");
    private static final Pattern LEVEL = Pattern.compile("(?<slayer>\\w+) Slayer LVL (?<level>\\d+) - (?:Next LVL in (?<next>[\\d,]+) XP!|LVL MAXED OUT!)");
    private static final Pattern RARE_DROP = Pattern.compile("^(?:VERY |CRAZY |INSANE |PRAY TO RNGESUS )?RARE DROP!\\s+\\(?(?<item>.+?)\\)?(?:\\s+x(?<amount>[\\d,]+))?(?:\\s+\\(\\+.*Magic Find\\))?\\s*$");

    // Boss XP and spawn cost per tier, from SkyHanni's Slayer.json.
    private static final Map<String, int[]> XP = Map.of(
        "Revenant Horror", new int[]{5, 25, 100, 500, 1500},
        "Tarantula Broodfather", new int[]{5, 25, 100, 500, 1500},
        "Sven Packmaster", new int[]{5, 25, 100, 500},
        "Voidgloom Seraph", new int[]{5, 25, 100, 500},
        "Inferno Demonlord", new int[]{5, 25, 100, 500},
        "Riftstalker Bloodfiend", new int[]{10, 25, 60, 120, 160});
    private static final Map<String, int[]> COST = Map.of(
        "Revenant Horror", new int[]{2000, 7500, 20000, 50000, 100000},
        "Tarantula Broodfather", new int[]{2000, 7500, 20000, 50000, 100000},
        "Sven Packmaster", new int[]{2000, 7500, 20000, 50000},
        "Voidgloom Seraph", new int[]{2000, 7500, 20000, 50000},
        "Inferno Demonlord", new int[]{10000, 25000, 60000, 150000},
        "Riftstalker Bloodfiend", new int[]{2000, 4000, 5000, 7000, 10000});

    private static String boss;
    private static int tier;
    private static double spawnCurrent = -1, spawnMax = -1, lastSpawnDelta;
    private static long nextLevelXp = -1;
    private static int sessionBosses;
    private static long sessionXp;
    private static boolean questActive;
    private static List<Component> bossLines = List.of();

    private static long sessionStart;
    private static double profit;
    private static Map<String, Integer> lastInventory;
    private static int ticks;

    private SlayerFeatures() {}

    private static FeatureConfigs.Slayer config() {
        SkyJewConfig c = SkyJewConfig.current();
        return c == null ? null : c.slayers.huds;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 5 == 0) tick(mc);
        });
        SkyJewChat.onChat(SlayerFeatures::onChat);

        SkyJewHuds.register("slayer_tracker", "Slayer Tracker",
            () -> config() != null && config().tracker && boss != null,
            SlayerFeatures::trackerLines,
            List.of(title("Revenant Horror IV"), kv("Spawn: ", "1,234/2,400 XP (~20 kills)"),
                kv("Next LVL: ", "150,000 XP (~300 bosses)"), kv("Session: ", "12 bosses, +6,000 XP")),
            8, 160);
        SkyJewHuds.register("slayer_phase", "Slayer Boss Phase",
            () -> config() != null && config().phaseDisplay,
            () -> bossLines,
            List.of(Component.literal("☠ Voidgloom Seraph 45M❤").withStyle(ChatFormatting.RED), Component.literal("15 Hits").withStyle(ChatFormatting.LIGHT_PURPLE)),
            8, 220);
        SkyJewHuds.register("slayer_profit", "Slayer Profit",
            () -> config() != null && config().profitTracker && sessionStart > 0,
            SlayerFeatures::profitLines,
            List.of(title("Slayer Profit"), kv("Profit: ", "12.5M"), kv("Per hour: ", "25.1M/h")),
            8, 260);
    }

    private static Component title(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    }

    private static Component kv(String key, String value) {
        return Component.literal(key).withStyle(ChatFormatting.GRAY).append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static List<Component> trackerLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(title(boss + " " + roman(tier)));
        if (spawnMax > 0) {
            String kills = lastSpawnDelta > 0 ? " (~" + (int) Math.ceil((spawnMax - spawnCurrent) / lastSpawnDelta) + " kills)" : "";
            lines.add(kv("Spawn: ", fmt(spawnCurrent) + "/" + fmt(spawnMax) + " XP" + kills));
        } else if (questActive) {
            lines.add(kv("Spawn: ", "Boss spawned"));
        }
        int xpPerBoss = xpPerBoss();
        if (nextLevelXp >= 0 && xpPerBoss > 0) {
            lines.add(kv("Next LVL: ", fmt(nextLevelXp) + " XP (~" + (long) Math.ceil(nextLevelXp / (double) xpPerBoss) + " bosses)"));
        }
        lines.add(kv("Session: ", sessionBosses + " bosses, +" + fmt(sessionXp) + " XP"));
        return lines;
    }

    private static List<Component> profitLines() {
        double hours = Math.max(1 / 60d, (System.currentTimeMillis() - sessionStart) / 3_600_000d);
        return List.of(title("Slayer Profit"),
            kv("Profit: ", CombatFeatures.formatCoins(profit)),
            kv("Per hour: ", CombatFeatures.formatCoins(profit / hours) + "/h"));
    }

    private static int xpPerBoss() {
        int[] xp = boss == null ? null : XP.get(boss);
        return xp == null || tier < 1 || tier > xp.length ? 0 : xp[tier - 1];
    }

    private static int spawnCost() {
        int[] cost = boss == null ? null : COST.get(boss);
        return cost == null || tier < 1 || tier > cost.length ? 0 : cost[tier - 1];
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || !SkyJewLocation.onSkyblock()) return;

        boolean inQuest = false;
        double current = -1, max = -1;
        for (String line : SkyJewLocation.scoreboard()) {
            if (line.equals("Slayer Quest")) inQuest = true;
            Matcher m = BOSS.matcher(line);
            if (m.find()) {
                boss = m.group("boss");
                tier = fromRoman(m.group("tier"));
            }
            m = PROGRESS.matcher(line);
            if (m.find()) {
                current = parse(m.group("current"));
                max = parse(m.group("max"));
            }
        }
        if (current >= 0 && spawnCurrent >= 0 && current > spawnCurrent) lastSpawnDelta = current - spawnCurrent;
        spawnCurrent = current;
        spawnMax = max;
        questActive = inQuest;

        updateBossLines(mc);
        if (inQuest && config() != null && config().profitTracker) trackInventory(mc);
    }

    /** Nametag lines of your own boss: the armor stands stacked above the "Spawned by: you" line. */
    private static void updateBossLines(Minecraft mc) {
        if (!questActive || mc.level == null) {
            bossLines = List.of();
            return;
        }
        String name = mc.player.getGameProfile().name();
        ArmorStand owner = null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand stand && stand.hasCustomName()
                && stand.getCustomName().getString().contains("Spawned by: " + name)) {
                owner = stand;
                break;
            }
        }
        if (owner == null) {
            bossLines = List.of();
            return;
        }
        AABB area = owner.getBoundingBox().inflate(1.5, 3, 1.5);
        List<ArmorStand> stands = mc.level.getEntitiesOfClass(ArmorStand.class, area, s -> s != null && s.hasCustomName());
        stands.sort(Comparator.comparingDouble((ArmorStand stand) -> stand.getY()).reversed());
        List<Component> lines = new ArrayList<>();
        for (ArmorStand stand : stands) {
            String text = stand.getCustomName().getString();
            if (text.contains("Spawned by:") || text.isBlank()) continue;
            lines.add(stand.getCustomName());
        }
        bossLines = lines;
    }

    private static void trackInventory(Minecraft mc) {
        if (mc.gui.screen() != null) {
            lastInventory = null; // moving items around in menus is not loot
            return;
        }
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            String id = Compat.neuName(stack);
            if (!id.isEmpty()) counts.merge(id, stack.getCount(), Integer::sum);
        }
        if (lastInventory != null) {
            for (var entry : counts.entrySet()) {
                int gained = entry.getValue() - lastInventory.getOrDefault(entry.getKey(), 0);
                if (gained > 0) profit += ItemPriceResolver.value(entry.getKey()) * gained;
            }
        }
        lastInventory = counts;
    }

    private static void onChat(SkyJewChat.Message message) {
        String text = message.text();
        if (text.trim().equals("SLAYER QUEST STARTED!")) {
            if (sessionStart == 0) sessionStart = System.currentTimeMillis();
            return;
        }
        if (text.trim().equals("SLAYER QUEST COMPLETE!")) {
            sessionBosses++;
            sessionXp += xpPerBoss();
            profit -= spawnCost();
            if (sessionStart == 0) sessionStart = System.currentTimeMillis();
            return;
        }
        Matcher m = LEVEL.matcher(text);
        if (m.find()) {
            nextLevelXp = m.group("next") == null ? 0 : Long.parseLong(m.group("next").replace(",", ""));
            return;
        }
        // Rare drops are counted by value when they land in the inventory; this catches ones that
        // go straight to sacks or are auto-picked up while a menu is open.
        if (questActive && (m = RARE_DROP.matcher(text)).find() && Minecraft.getInstance().gui.screen() != null) {
            profit += ItemPriceResolver.valueByName(m.group("item").trim());
        }
    }

    private static double parse(String value) {
        String v = value.replace(",", "").toLowerCase(Locale.ROOT);
        double mult = v.endsWith("k") ? 1000 : 1;
        if (v.endsWith("k")) v = v.substring(0, v.length() - 1);
        return Double.parseDouble(v) * mult;
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%,.0f", value);
    }

    private static int fromRoman(String roman) {
        return switch (roman) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            default -> 0;
        };
    }

    private static String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "";
        };
    }
}

package com.epic60869.skyballs.features.slayer;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.core.SkyBallsChat;
import com.epic60869.skyballs.features.core.SkyBallsHuds;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slayer tracker and boss phase display. Boss XP per tier is
 * from SkyHanni's repo (constants/Slayer.json, MIT).
 */
public final class SlayerFeatures {
    private static final Pattern PROGRESS = Pattern.compile("\\((?<current>[\\d,.]+k?)/(?<max>[\\d,.]+k?)\\) Combat XP");
    private static final Pattern BOSS = Pattern.compile("(?<boss>Revenant Horror|Tarantula Broodfather|Sven Packmaster|Voidgloom Seraph|Inferno Demonlord|Riftstalker Bloodfiend) (?<tier>[IV]+)");
    private static final Pattern LEVEL = Pattern.compile("(?<slayer>\\w+) Slayer LVL (?<level>\\d+) - (?:Next LVL in (?<next>[\\d,]+) XP!|LVL MAXED OUT!)");

    // Boss XP per tier, from SkyHanni's Slayer.json.
    private static final Map<String, int[]> XP = Map.of(
        "Revenant Horror", new int[]{5, 25, 100, 500, 1500},
        "Tarantula Broodfather", new int[]{5, 25, 100, 500, 1500},
        "Sven Packmaster", new int[]{5, 25, 100, 500},
        "Voidgloom Seraph", new int[]{5, 25, 100, 500},
        "Inferno Demonlord", new int[]{5, 25, 100, 500},
        "Riftstalker Bloodfiend", new int[]{10, 25, 60, 120, 160});

    private static String boss;
    private static int tier;
    private static double spawnCurrent = -1, spawnMax = -1, lastSpawnDelta;
    private static long nextLevelXp = -1;
    private static int sessionBosses;
    private static long sessionXp;
    private static boolean questActive;
    private static List<Component> bossLines = List.of();

    private static int ticks;

    private SlayerFeatures() {}

    private static FeatureConfigs.Slayer config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.slayers.huds;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 5 == 0) tick(mc);
        });
        SkyBallsChat.onChat(SlayerFeatures::onChat);

        SkyBallsHuds.register("slayer_tracker", "Slayer Tracker",
            () -> config() != null && config().tracker && boss != null,
            SlayerFeatures::trackerLines,
            List.of(title("Revenant Horror IV"), kv("Spawn: ", "1,234/2,400 XP (~20 kills)"),
                kv("Next LVL: ", "150,000 XP (~300 bosses)"), kv("Session: ", "12 bosses, +6,000 XP")),
            8, 160);
        SkyBallsHuds.register("slayer_phase", "Slayer Boss Phase",
            () -> config() != null && config().phaseDisplay,
            () -> bossLines,
            List.of(Component.literal("☠ Voidgloom Seraph 45M❤").withStyle(ChatFormatting.RED), Component.literal("15 Hits").withStyle(ChatFormatting.LIGHT_PURPLE)),
            8, 220);
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

    private static int xpPerBoss() {
        int[] xp = boss == null ? null : XP.get(boss);
        return xp == null || tier < 1 || tier > xp.length ? 0 : xp[tier - 1];
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || !SkyBallsLocation.onSkyblock()) return;

        boolean inQuest = false;
        double current = -1, max = -1;
        for (String raw : SkyBallsLocation.scoreboard()) {
            // Hypixel pads sidebar lines with invisible emoji between the team prefix and suffix.
            String line = clean(raw);
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
    }

    /** Keeps plain text only: drops Hypixel's padding emoji and other invisible characters. */
    private static String clean(String line) {
        return line.replaceAll("[^\\x20-\\x7E]", "").replaceAll("\\s+", " ").trim();
    }

    /** A nametag's text, from an armor stand's custom name or (as Hypixel now uses) a text display. */
    private static Component nametag(Entity entity) {
        if (entity instanceof ArmorStand stand && stand.hasCustomName()) return stand.getCustomName();
        if (entity instanceof Display.TextDisplay display) return display.getText();
        return null;
    }

    /**
     * Nametag lines of your own boss: the nametags stacked above the "Spawned by: you" line. Hypixel draws them
     * with armor stands or text displays, and a text display can hold several lines.
     */
    private static void updateBossLines(Minecraft mc) {
        if (!questActive || mc.level == null) {
            bossLines = List.of();
            return;
        }
        String name = mc.player.getGameProfile().name();
        Entity owner = null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            Component tag = nametag(entity);
            if (tag != null && SkyBallsLocation.strip(tag.getString()).contains("Spawned by: " + name)) {
                owner = entity;
                break;
            }
        }
        if (owner == null) {
            bossLines = List.of();
            return;
        }
        AABB area = owner.getBoundingBox().inflate(1.5, 3, 1.5);
        List<Entity> tags = mc.level.getEntities((Entity) null, area, e -> nametag(e) != null);
        tags.sort(Comparator.comparingDouble((Entity e) -> e.getY()).reversed());
        List<Component> lines = new ArrayList<>();
        for (Entity tagEntity : tags) {
            Component tag = nametag(tagEntity);
            String text = tag.getString();
            if (text.isBlank()) continue;
            if (!text.contains("\n")) {
                if (!text.contains("Spawned by:")) lines.add(tag);
                continue;
            }
            for (String part : text.split("\n")) {
                if (!part.isBlank() && !part.contains("Spawned by:")) lines.add(Component.literal(part));
            }
        }
        bossLines = lines;
    }

    private static void onChat(SkyBallsChat.Message message) {
        String text = message.text();
        if (clean(text).equals("SLAYER QUEST COMPLETE!")) {
            sessionBosses++;
            sessionXp += xpPerBoss();
            return;
        }
        Matcher m = LEVEL.matcher(text);
        if (m.find()) {
            nextLevelXp = m.group("next") == null ? 0 : Long.parseLong(m.group("next").replace(",", ""));
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

package com.epic60869.skyjew.features.dungeons;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.features.FeatureConfigs;
import com.epic60869.skyjew.features.core.SkyJewAlerts;
import com.epic60869.skyjew.features.core.SkyJewChat;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import com.epic60869.skyjew.sb.events.ServerTickCallback;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Dungeon splits, tick timers, mask timers, M7 debuff alert. Routes are in {@link DungeonRoutes}. */
public final class DungeonFeatures {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ----- Splits -----
    private record Split(String name, long time) {}
    private static final List<Split> SPLITS = new ArrayList<>();
    private static long runStart;

    // ----- Tick timers (counted in server ticks from Hypixel's per-tick ping packets) -----
    private static long serverTicks;
    private static long stormStartTick = -1;
    private static long goldorStartTick = -1;

    // ----- Masks -----
    private static long bonzoReady, spiritReady, phoenixReady;

    // ----- Debuffs -----
    private static int lastBreath, iceSpray, lethality;
    private static boolean debuffAlerted;
    private static boolean dragonPhase;

    private DungeonFeatures() {}

    private static FeatureConfigs.Dungeons config() {
        SkyJewConfig c = SkyJewConfig.current();
        return c == null ? null : c.dungeons;
    }

    public static void init(Path configDir) {
        SkyJewChat.onChat(DungeonFeatures::onChat);
        ServerTickCallback.EVENT.register(() -> serverTicks++);
        SkyJewLocation.onAreaChange(area -> {
            if (!area.equals("Catacombs")) resetRun();
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() && dragonPhase) countDebuffUse(player.getItemInHand(hand));
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide() && dragonPhase && entity instanceof EnderDragon && hasLore(player.getMainHandItem(), "Lethality")) {
                lethality++;
                checkDebuffs();
            }
            return InteractionResult.PASS;
        });
        DungeonRoutes.init(configDir);
        StarredMobs.init();

        SkyJewHuds.register("dungeon_splits", "Dungeon Splits",
            () -> config() != null && config().timers.splits && runStart > 0,
            DungeonFeatures::splitLines,
            List.of(title("Splits"), kv("Blood Open: ", "0:24"), kv("Blood Clear: ", "1:10"), kv("Boss Entry: ", "2:31")),
            8, 740);
        SkyJewHuds.register("tick_timers", "Tick Timers",
            () -> config() != null && config().timers.tickTimers && (stormStartTick >= 0 || goldorStartTick >= 0),
            DungeonFeatures::tickLines,
            List.of(kv("Storm pillars: ", "12 ticks")),
            200, 740);
        SkyJewHuds.register("mask_timers", "Mask Timers",
            () -> config() != null && config().timers.maskTimers && SkyJewLocation.inDungeon(),
            DungeonFeatures::maskLines,
            List.of(kv("Bonzo: ", "READY"), kv("Spirit: ", "12s"), kv("Phoenix: ", "READY")),
            200, 780);
    }

    private static Component title(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    }

    private static Component kv(String key, String value) {
        return Component.literal(key).withStyle(ChatFormatting.GRAY).append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static void resetRun() {
        SPLITS.clear();
        runStart = 0;
        stormStartTick = -1;
        goldorStartTick = -1;
        dragonPhase = false;
        resetDebuffs();
    }

    private static void split(String name) {
        if (runStart == 0) return;
        for (Split s : SPLITS) if (s.name().equals(name)) return;
        SPLITS.add(new Split(name, System.currentTimeMillis() - runStart));
    }

    private static String time(long millis) {
        long seconds = millis / 1000;
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }

    private static List<Component> splitLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(title("Splits " + time(System.currentTimeMillis() - runStart)));
        for (Split s : SPLITS) lines.add(kv(s.name() + ": ", time(s.time())));
        return lines;
    }

    private static List<Component> tickLines() {
        List<Component> lines = new ArrayList<>();
        if (stormStartTick >= 0 && goldorStartTick < 0) {
            lines.add(kv("Storm pillars: ", (20 - (serverTicks - stormStartTick) % 20) + " ticks"));
        }
        if (goldorStartTick >= 0) {
            lines.add(kv("Goldor death tick: ", (60 - (serverTicks - goldorStartTick) % 60) + " ticks"));
        }
        return lines;
    }

    private static List<Component> maskLines() {
        var player = Minecraft.getInstance().player;
        if (player == null) return List.of();
        List<Component> lines = new ArrayList<>();
        String helmet = player.getItemBySlot(EquipmentSlot.HEAD).getHoverName().getString();
        if (helmet.contains("Bonzo's Mask")) lines.add(maskLine("Bonzo", bonzoReady));
        if (helmet.contains("Spirit Mask")) lines.add(maskLine("Spirit", spiritReady));
        if (phoenixReady > 0) lines.add(maskLine("Phoenix", phoenixReady));
        return lines;
    }

    private static Component maskLine(String name, long readyAt) {
        long left = readyAt - System.currentTimeMillis();
        return left <= 0
            ? Component.literal(name + ": ").withStyle(ChatFormatting.GRAY).append(Component.literal("READY").withStyle(ChatFormatting.GREEN))
            : Component.literal(name + ": ").withStyle(ChatFormatting.GRAY).append(Component.literal((left / 1000 + 1) + "s").withStyle(ChatFormatting.RED));
    }

    private static void onChat(SkyJewChat.Message message) {
        String text = message.text();
        FeatureConfigs.Dungeons config = config();
        if (config == null) return;

        // Splits and boss phases. Messages from SkyHanni's repo (MIT) and Hypixel's dungeon chat.
        if (text.equals("Starting in 1 second.") || text.startsWith("[NPC] Mort: Here, I found this map")) {
            if (runStart == 0) {
                resetRun();
                runStart = System.currentTimeMillis();
            }
        } else if (text.contains("The BLOOD DOOR has been opened!") || (text.startsWith("[BOSS] The Watcher:") && !text.contains("You have proven yourself"))) {
            split("Blood Open");
        } else if (text.startsWith("[BOSS] The Watcher: You have proven yourself. You may pass.")) {
            split("Blood Clear");
        } else if (text.startsWith("[BOSS] ") && !text.startsWith("[BOSS] The Watcher")) {
            split("Boss Entry");
            if (text.startsWith("[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!")) split("Maxor");
            if (text.startsWith("[BOSS] Storm: Pathetic Maxor, just like expected.")) {
                split("Storm");
                stormStartTick = serverTicks;
            }
            if (text.startsWith("[BOSS] Goldor: Who dares trespass into my domain?")) {
                split("Goldor");
                goldorStartTick = serverTicks;
            }
            if (text.startsWith("[BOSS] Necron: You went further than any human before, congratulations.")) {
                split("Necron");
                goldorStartTick = -1;
            }
            if (text.startsWith("[BOSS] Necron: All this, for nothing...")) {
                split("Dragons");
                dragonPhase = true;
            }
        } else if (text.equals("The Core entrance is opening!")) {
            split("Terminals");
            stormStartTick = -1;
        } else if (text.contains("> EXTRA STATS <")) {
            split("End");
            printSummary();
        }

        // Masks (messages from SkyHanni's repo) with their ability cooldowns.
        long now = System.currentTimeMillis();
        if (text.contains("Your Bonzo's Mask saved your life!")) {
            boolean starred = helmetName().contains("⚚");
            bonzoReady = now + (starred ? 180_000 : 360_000);
            if (config.timers.maskTimers) SkyJewAlerts.title(Component.literal("Bonzo's Mask used!").withStyle(ChatFormatting.RED), Component.empty());
        } else if (text.contains("Second Wind Activated! Your Spirit Mask saved your life!")) {
            spiritReady = now + 30_000;
            if (config.timers.maskTimers) SkyJewAlerts.title(Component.literal("Spirit Mask used!").withStyle(ChatFormatting.RED), Component.empty());
        } else if (text.contains("Your Phoenix Pet saved you from certain death!")) {
            phoenixReady = now + 60_000;
            if (config.timers.maskTimers) SkyJewAlerts.title(Component.literal("Phoenix used!").withStyle(ChatFormatting.RED), Component.empty());
        }

        if (dragonPhase && text.contains("Dragon") && text.contains("spawning")) resetDebuffs();
    }

    private static String helmetName() {
        var player = Minecraft.getInstance().player;
        return player == null ? "" : player.getItemBySlot(EquipmentSlot.HEAD).getHoverName().getString();
    }

    private static void printSummary() {
        long bloodOpen = splitTime("Blood Open"), bloodClear = splitTime("Blood Clear"), boss = splitTime("Boss Entry"), end = splitTime("End");
        StringBuilder out = new StringBuilder("Run: ");
        if (bloodOpen >= 0) out.append("blood rush ").append(bloodOpen / 1000).append("s, ");
        if (boss >= 0) out.append("clear ").append(boss / 1000).append("s, ");
        if (boss >= 0 && end >= 0) out.append("boss ").append((end - boss) / 1000).append("s, ");
        if (bloodClear >= 0 && bloodOpen >= 0) out.append("blood camp ").append((bloodClear - bloodOpen) / 1000).append("s, ");
        if (end >= 0) out.append("total ").append(time(end));
        if (config() != null && config().timers.splits) SkyJewAlerts.chat(Component.literal(out.toString()).withStyle(ChatFormatting.YELLOW));
    }

    private static long splitTime(String name) {
        for (Split s : SPLITS) if (s.name().equals(name)) return s.time();
        return -1;
    }

    private static void countDebuffUse(ItemStack stack) {
        String name = stack.getHoverName().getString();
        if (name.contains("Last Breath")) lastBreath++;
        else if (name.contains("Ice Spray")) iceSpray++;
        else return;
        checkDebuffs();
    }

    private static void checkDebuffs() {
        FeatureConfigs.Dungeons config = config();
        if (config == null || !config.timers.debuffAlert || debuffAlerted) return;
        if (lastBreath >= 5 && iceSpray >= 1 && lethality >= 5) {
            debuffAlerted = true;
            SkyJewAlerts.title(Component.literal("MAX DEBUFF!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                Component.literal("5x Last Breath, Ice Spray, 5x Lethality").withStyle(ChatFormatting.WHITE));
        }
    }

    private static void resetDebuffs() {
        lastBreath = 0;
        iceSpray = 0;
        lethality = 0;
        debuffAlerted = false;
    }

    private static boolean hasLore(ItemStack stack, String text) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (Component line : lore.lines()) if (line.getString().contains(text)) return true;
        return false;
    }
}

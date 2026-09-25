package com.epic60869.skyjew.features.dungeons;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.custom.util.Compat;
import com.epic60869.skyjew.features.FeatureConfigs;
import com.epic60869.skyjew.features.core.SkyJewAlerts;
import com.epic60869.skyjew.features.core.SkyJewChat;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import com.epic60869.skyjew.features.core.SkyJewWorldRender;
import com.epic60869.skyjew.sb.events.ServerTickCallback;
import com.epic60869.skyjew.sb.skyblock.dungeon.secrets.DungeonManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Blood camp helpers, following Odin's BloodCamp (https://github.com/odtheking/Odin, BSD-3-Clause):
 * <ul>
 *     <li>Watcher move prediction: when the Watcher says "Let's see how you can handle this.", predicts when it moves,
 *     shows a Move Timer HUD and a "Kill Mobs" title when it is time to kill the first spawns.</li>
 *     <li>Blood mob kill timers: each blood mob head the Watcher throws gets a box where it lands and a countdown until
 *     the mob spawns there.</li>
 * </ul>
 * Everything is counted in server ticks.
 */
public final class BloodCamp {
    private static final Pattern BLOOD_START = Pattern.compile("^\\[BOSS] The Watcher: (Congratulations, you made it through the Entrance\\.|Ah, you've finally arrived\\.|Ah, we meet again\\.\\.\\.|So you made it this far\\.\\.\\. interesting\\.|You've managed to scratch and claw your way here, eh\\?|I'm starting to get tired of seeing you around here\\.\\.\\.|Oh\\.\\. hello\\?|Things feel a little more roomy now, eh\\?)$");
    private static final Pattern BLOOD_MOVE = Pattern.compile("^\\[BOSS] The Watcher: Let's see how you can handle this\\.$");
    private static final float[] LAND_COLOUR = {1f, 0.33f, 0.33f};
    private static final float[] MOB_COLOUR = {0.33f, 1f, 0.33f};
    /** Mobs spawn about 38 ticks after the head is thrown (37-41), plus 2s for the first wave. */
    private static final int SPAWN_TICK = 38;

    private static final class Head {
        final Vec3 start;
        final long startedMillis;
        final boolean firstSpawns;
        Vec3 last;
        Vec3 totalDelta = Vec3.ZERO;
        Vec3 end;

        Head(Vec3 start, long startedMillis, boolean firstSpawns) {
            this.start = start;
            this.last = start;
            this.end = start;
            this.startedMillis = startedMillis;
            this.firstSpawns = firstSpawns;
        }
    }

    private static final Map<ArmorStand, Head> HEADS = new HashMap<>();
    private static Zombie watcher;
    private static boolean firstSpawns = true;
    /** Server time in ms, advanced 50 per server tick. */
    private static long tickTime;
    private static long startTime = -1;
    private static float moveSeconds = -1;
    private static int killTitleIn = -1;

    private BloodCamp() {}

    private static FeatureConfigs.BloodCamp config() {
        SkyJewConfig c = SkyJewConfig.current();
        return c == null ? null : c.dungeons.bloodCamp;
    }

    private static boolean inClear() {
        return SkyJewLocation.inDungeon() && !DungeonManager.isInBoss();
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> reset());
        SkyJewLocation.onAreaChange(area -> reset());
        ServerTickCallback.EVENT.register(BloodCamp::onServerTick);
        SkyJewChat.onChat(message -> onChat(message.text()));
        SkyJewWorldRender.register(collector -> {
            FeatureConfigs.BloodCamp config = config();
            if (config == null || !config.killTimers || !inClear()) return;
            for (var entry : HEADS.entrySet()) {
                ArmorStand stand = entry.getKey();
                if (!stand.isAlive()) continue;
                Head head = entry.getValue();
                long left = (head.firstSpawns ? 2000 : 0) + SPAWN_TICK * 50L - (tickTime - head.startedMillis);
                float seconds = left / 1000f;
                AABB landing = new AABB(-0.5, 1.5, -0.5, 0.5, 2.5, 0.5).move(head.end);
                collector.submitOutlinedBox(landing, LAND_COLOUR, 2f, false);
                collector.submitOutlinedBox(new AABB(-0.5, 1.5, -0.5, 0.5, 2.5, 0.5).move(stand.position()), MOB_COLOUR, 1.5f, false);
                collector.submitLinesFromPoints(new Vec3[]{stand.position().add(0, 2, 0), head.end.add(0, 2, 0)}, LAND_COLOUR, 1f, 1.5f, false);
                ChatFormatting colour = seconds > 1.5 ? ChatFormatting.GREEN : seconds >= 0.5 ? ChatFormatting.GOLD : seconds >= 0 ? ChatFormatting.RED : ChatFormatting.AQUA;
                collector.submitText(Component.literal(String.format(Locale.US, "%.2fs", seconds)).withStyle(colour), head.end.add(0, 3, 0), 1.5f, true);
            }
        });
        SkyJewHuds.register("blood_move_timer", "Watcher Move Timer",
            () -> config() != null && config().movePrediction && moveSeconds > 0,
            () -> List.of(Component.literal(String.format(Locale.US, "Move Timer: %.2fs", moveSeconds)).withStyle(ChatFormatting.RED)),
            List.of(Component.literal("Move Timer: 0.50s").withStyle(ChatFormatting.RED)),
            200, 700);
    }

    private static void reset() {
        HEADS.clear();
        watcher = null;
        firstSpawns = true;
        tickTime = 0;
        startTime = -1;
        moveSeconds = -1;
        killTitleIn = -1;
    }

    private static void onChat(String text) {
        FeatureConfigs.BloodCamp config = config();
        if (config == null || !inClear()) return;
        if (BLOOD_START.matcher(text).matches()) {
            startTime = tickTime;
        } else if (BLOOD_MOVE.matcher(text).matches()) {
            firstSpawns = false;
            if (startTime < 0 || !config.movePrediction) return;
            long moveTicks = (tickTime - startTime) / 20 / 50;
            int predTicks;
            if (moveTicks >= 31 && moveTicks < 34) predTicks = 36;
            else if (moveTicks >= 28) predTicks = moveTicks < 31 ? 33 : (int) moveTicks + 3;
            else if (moveTicks >= 25) predTicks = 30;
            else if (moveTicks >= 22) predTicks = 27;
            else if (moveTicks >= 1) predTicks = 24;
            else predTicks = (int) moveTicks + 3;
            moveSeconds = predTicks / 20f;
            killTitleIn = predTicks;
            String msg = String.format(Locale.US, "Watcher will move in %.2fs.", moveSeconds);
            if (config.moveMessage) SkyJewAlerts.chat(Component.literal(msg).withStyle(ChatFormatting.RED));
            if (config.partyMoveMessage) {
                var connection = Minecraft.getInstance().getConnection();
                if (connection != null) connection.sendCommand("pc " + msg);
            }
        }
    }

    private static void onServerTick() {
        tickTime += 50;
        if (moveSeconds > 0) moveSeconds = Math.max(0, moveSeconds - 0.05f);
        if (killTitleIn > 0 && --killTitleIn == 0) {
            FeatureConfigs.BloodCamp config = config();
            if (config != null && config.killTitle) SkyJewAlerts.title(Component.literal("Kill Mobs").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), Component.empty());
            moveSeconds = -1;
        }
        FeatureConfigs.BloodCamp config = config();
        if (config == null || !config.killTimers || !inClear()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (watcher != null && !watcher.isAlive()) watcher = null;
        if (watcher == null) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof Zombie zombie && BloodCampSkulls.WATCHER.contains(Compat.getHeadTexture(zombie.getItemBySlot(EquipmentSlot.HEAD)))) {
                    watcher = zombie;
                    break;
                }
            }
            if (watcher == null) return;
        }
        HEADS.keySet().removeIf(stand -> !stand.isAlive());
        for (ArmorStand stand : mc.level.getEntitiesOfClass(ArmorStand.class, watcher.getBoundingBox().inflate(20))) {
            Vec3 pos = stand.position();
            Head head = HEADS.get(stand);
            if (head == null) {
                if (!BloodCampSkulls.MOBS.contains(Compat.getHeadTexture(stand.getItemBySlot(EquipmentSlot.HEAD)))) continue;
                HEADS.put(stand, new Head(pos, tickTime, firstSpawns));
                continue;
            }
            Vec3 delta = pos.subtract(head.last);
            head.last = pos;
            if (delta.lengthSqr() == 0) continue;
            head.totalDelta = head.totalDelta.add(delta);
            // The head flies in a straight line: 16.1 blocks for the first wave, 11.9 after.
            head.end = head.start.add(head.totalDelta.normalize().scale(head.firstSpawns ? 16.1 : 11.9));
        }
    }
}

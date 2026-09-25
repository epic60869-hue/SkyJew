package com.epic60869.skyjew.features.combat;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.features.core.SkyJewChat;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.EnderMan;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Counts Zealots you kill and kills since your last Summoning Eye. Totals are saved between sessions.
 * A kill is counted when the server reports a Zealot's death, or when a nearby Zealot's health hits zero
 * (which also catches several Zealots killed at once by a Hyperion's Wither Impact).
 */
public final class ZealotCounter {
    private static final double KILL_RANGE = 20;
    private static final List<String> END_LOCATIONS = List.of("The End", "Dragon's Nest", "Void Sepulture", "Zealot Bruiser Hideout", "Void Slate");
    /** Entity ids already counted, so a death packet and the health check never count the same Zealot twice. */
    private static final java.util.Set<Integer> COUNTED = new java.util.HashSet<>();
    /** Zealots seen alive nearby, by entity id. */
    private static final java.util.Set<Integer> TRACKED = new java.util.HashSet<>();
    private static final Gson GSON = new Gson();

    private static int sessionKills;
    private static int totalKills;
    private static int sinceEye;
    private static int eyes;
    private static Path file;

    private ZealotCounter() {}

    public static void init(Path configDir) {
        file = configDir.resolve("skyjew-zealots.json");
        load();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> save());
        SkyJewChat.onChat(message -> {
            String text = message.text();
            if (text.contains("RARE DROP!") && text.contains("Summoning Eye")) {
                eyes++;
                sinceEye = 0;
                save();
            }
        });
        SkyJewHuds.register("zealots", "Zealot Counter",
            () -> {
                SkyJewConfig c = SkyJewConfig.current();
                return c != null && c.combat.zealotCounter && inEnd();
            },
            ZealotCounter::lines,
            List.of(kv("Zealots: ", "1,234 (session 56)"), kv("Since eye: ", "321"), kv("Eyes: ", "4")),
            8, 300);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(ZealotCounter::tick);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
            COUNTED.clear();
            TRACKED.clear();
        });
    }

    public static boolean inEnd() {
        if (SkyJewLocation.areaIs("The End")) return true;
        String location = SkyJewLocation.location();
        for (String name : END_LOCATIONS) if (location.contains(name)) return true;
        return false;
    }

    private static boolean isZealot(Minecraft mc, Entity entity) {
        return entity instanceof EnderMan && !mc.level.getEntitiesOfClass(ArmorStand.class, entity.getBoundingBox().inflate(0.5, 3, 0.5),
            stand -> stand.hasCustomName() && stand.getCustomName().getString().contains("Zealot")).isEmpty();
    }

    /** Remembers Zealots near you and counts those whose health drops to zero. */
    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || !inEnd()) return;
        for (EnderMan enderman : mc.level.getEntitiesOfClass(EnderMan.class, mc.player.getBoundingBox().inflate(KILL_RANGE), e -> true)) {
            int id = enderman.getId();
            if (COUNTED.contains(id)) continue;
            if (enderman.isDeadOrDying()) {
                if (TRACKED.remove(id)) count(id);
            } else if (!TRACKED.contains(id) && isZealot(mc, enderman)) {
                TRACKED.add(id);
            }
        }
        if (TRACKED.size() > 512) TRACKED.clear();
    }

    private static void count(int id) {
        if (!COUNTED.add(id)) return;
        if (COUNTED.size() > 4096) COUNTED.clear();
        sessionKills++;
        totalKills++;
        sinceEye++;
        if (totalKills % 25 == 0) save();
    }

    private static Component kv(String key, String value) {
        return Component.literal(key).withStyle(ChatFormatting.GRAY).append(Component.literal(value).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static List<Component> lines() {
        return List.of(
            kv("Zealots: ", String.format(Locale.US, "%,d (session %,d)", totalKills, sessionKills)),
            kv("Since eye: ", String.format(Locale.US, "%,d", sinceEye)),
            kv("Eyes: ", String.valueOf(eyes)));
    }

    /** Called when the server says an entity died. Zealots are Endermen with a "Zealot" nametag stand above them. */
    public static void onEntityDeath(Entity entity) {
        if (!(entity instanceof EnderMan) || !inEnd()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || entity.distanceTo(mc.player) > KILL_RANGE) return;
        if (!TRACKED.remove(entity.getId()) && !isZealot(mc, entity)) return;
        count(entity.getId());
    }

    private static void load() {
        try {
            if (!Files.exists(file)) return;
            JsonObject root = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            totalKills = root.has("totalKills") ? root.get("totalKills").getAsInt() : 0;
            sinceEye = root.has("sinceEye") ? root.get("sinceEye").getAsInt() : 0;
            eyes = root.has("eyes") ? root.get("eyes").getAsInt() : 0;
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to load zealot counter: " + e.getMessage());
        }
    }

    private static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("totalKills", totalKills);
            root.addProperty("sinceEye", sinceEye);
            root.addProperty("eyes", eyes);
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to save zealot counter: " + e.getMessage());
        }
    }
}

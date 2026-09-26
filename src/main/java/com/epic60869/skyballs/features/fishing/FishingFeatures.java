package com.epic60869.skyballs.features.fishing;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsTabWidgetManager;
import com.epic60869.skyballs.custom.RepoItems;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.core.SkyBallsAlerts;
import com.epic60869.skyballs.features.core.SkyBallsChat;
import com.epic60869.skyballs.features.core.SkyBallsHuds;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fishing stat display, hook timer, bait display and rare sea creature alerts. */
public final class FishingFeatures {
    private static final String[] STATS = {"Fishing Speed", "Sea Creature Chance", "Trophy Fish Chance", "Double Hook Chance", "Treasure Chance"};
    // Hook timer and bait patterns from SkyHanni's repo (MIT).
    private static final Pattern HOOK_TIMER = Pattern.compile("^(?<time>\\d+(?:\\.\\d+)?)$|(?<alert>!!!)");
    private static final Pattern BAIT_REMAINING = Pattern.compile("Bait Remaining: (?<amount>[\\d,]+)");
    private static final Pattern BAIT_NAME = Pattern.compile("^(?:Obfuscated.*|.* Bait)$");

    /** Catch message → (name, rarity index), from SkyHanni's constants/SeaCreatures.json. */
    private record SeaCreature(String name, String rarity) {}
    private static final Map<String, SeaCreature> CREATURES = new ConcurrentHashMap<>();

    private static String hookText;
    private static String baitName;
    private static long baitAmount = -1;
    private static int ticks;

    private FishingFeatures() {}

    private static FeatureConfigs.Fishing config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.fishing;
    }

    public static void init() {
        loadSeaCreatures();
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ticks++;
            tickHook(mc);
            if (ticks % 20 == 0) tickBait(mc);
        });
        SkyBallsChat.onChat(FishingFeatures::onChat);

        SkyBallsHuds.register("fishing_stats", "Fishing Stats",
            () -> config() != null && config().statDisplay && holdingRod(),
            FishingFeatures::statLines,
            List.of(kv("Fishing Speed: ", "250"), kv("Sea Creature Chance: ", "28%"), kv("Double Hook Chance: ", "12%")),
            8, 380);
        SkyBallsHuds.register("hook_timer", "Fishing Hook Timer",
            () -> config() != null && config().hookTimer && hookText != null,
            () -> List.of(hookText.equals("!!!")
                ? Component.literal("Hook: BITE!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                : kv("Hook: ", hookText + "s")),
            List.of(kv("Hook: ", "1.5s")),
            8, 440);
        SkyBallsHuds.register("bait", "Bait Display",
            () -> config() != null && config().baitDisplay && baitName != null && holdingRod(),
            () -> List.of(kv("Bait: ", baitName + (baitAmount >= 0 ? " x" + baitAmount : ""))),
            List.of(kv("Bait: ", "Glowy Chum Bait x64")),
            8, 454);
    }

    private static Component kv(String key, String value) {
        return Component.literal(key).withStyle(ChatFormatting.GRAY).append(Component.literal(value).withStyle(ChatFormatting.AQUA));
    }

    private static boolean holdingRod() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getMainHandItem().is(Items.FISHING_ROD) && SkyBallsLocation.onSkyblock();
    }

    private static List<Component> statLines() {
        List<Component> lines = new ArrayList<>();
        for (PlayerInfo info : SkyBallsTabWidgetManager.players()) {
            if (com.epic60869.skyballs.custom.util.Compat.rawTabName(info) == null) continue;
            String text = SkyBallsLocation.strip(com.epic60869.skyballs.custom.util.Compat.rawTabName(info).getString()).trim();
            for (String stat : STATS) {
                if (text.startsWith(stat + ":")) {
                    lines.add(kv(stat + ": ", text.substring(stat.length() + 1).trim()));
                }
            }
        }
        if (lines.isEmpty()) lines.add(Component.literal("Add the Stats widget to your tab list (/tab) to see fishing stats").withStyle(ChatFormatting.GRAY));
        return lines;
    }

    /** Hypixel shows the bite countdown on an armor stand above your bobber. */
    private static void tickHook(Minecraft mc) {
        hookText = null;
        if (mc.player == null || mc.level == null) return;
        FishingHook hook = mc.player.fishing;
        if (hook == null) return;
        for (ArmorStand stand : mc.level.getEntitiesOfClass(ArmorStand.class, hook.getBoundingBox().inflate(0.6, 2.5, 0.6), ArmorStand::hasCustomName)) {
            String name = SkyBallsLocation.strip(stand.getCustomName().getString()).trim();
            Matcher m = HOOK_TIMER.matcher(name);
            if (m.find()) {
                hookText = m.group("alert") != null ? "!!!" : m.group("time");
                return;
            }
        }
    }

    private static void tickBait(Minecraft mc) {
        if (mc.player == null) return;
        String name = null;
        long amount = -1;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;
            boolean isBaitItem = BAIT_NAME.matcher(SkyBallsLocation.strip(com.epic60869.skyballs.custom.util.Compat.realName(stack).getString())).matches();
            for (Component line : lore.lines()) {
                String text = SkyBallsLocation.strip(line.getString()).trim();
                Matcher m = BAIT_REMAINING.matcher(text);
                if (m.find()) amount = Long.parseLong(m.group("amount").replace(",", ""));
                if (name == null && BAIT_NAME.matcher(text).matches()) name = text;
            }
            if (amount >= 0 && name == null && isBaitItem) name = SkyBallsLocation.strip(com.epic60869.skyballs.custom.util.Compat.realName(stack).getString());
            if (amount >= 0 && name != null) break;
        }
        if (name != null) {
            baitName = name;
            baitAmount = amount;
        }
    }

    private static void onChat(SkyBallsChat.Message message) {
        FeatureConfigs.Fishing config = config();
        if (config == null || !config.rareCreatureAlert) return;
        SeaCreature creature = CREATURES.get(message.text().trim());
        if (creature == null) return;
        if (rarityIndex(creature.rarity()) >= config.alertRarity.ordinal()) {
            SkyBallsAlerts.title(Component.literal(creature.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal(creature.rarity()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Index into FeatureConfigs.CreatureRarity (RARE=0 .. MYTHIC=3), or -1 for lower rarities. */
    private static int rarityIndex(String rarity) {
        try {
            return FeatureConfigs.CreatureRarity.valueOf(rarity.toUpperCase()).ordinal();
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    private static void loadSeaCreatures() {
        RepoItems.runAsync(() -> {
            try {
                String url = "https://raw.githubusercontent.com/hannibal002/SkyHanni-REPO/main/constants/SeaCreatures.json";
                var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).header("User-Agent", "SkyBalls/1.0").build();
                String body = java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString()).body();
                JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                for (var category : root.entrySet()) {
                    JsonObject creatures = category.getValue().getAsJsonObject().getAsJsonObject("sea_creatures");
                    if (creatures == null) continue;
                    for (var entry : creatures.entrySet()) {
                        JsonObject creature = entry.getValue().getAsJsonObject();
                        JsonElement chat = creature.get("chat_message");
                        if (chat == null) continue;
                        CREATURES.put(SkyBallsLocation.strip(chat.getAsString()).trim(),
                            new SeaCreature(entry.getKey(), creature.has("rarity") ? creature.get("rarity").getAsString() : "COMMON"));
                    }
                }
            } catch (Exception e) {
                System.err.println("[SkyBalls] Failed to load sea creatures: " + e.getMessage());
            }
        });
    }
}

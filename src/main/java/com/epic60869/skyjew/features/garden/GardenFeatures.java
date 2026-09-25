package com.epic60869.skyjew.features.garden;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewTabWidgetManager;
import com.epic60869.skyjew.features.FeatureConfigs;
import com.epic60869.skyjew.features.core.SkyJewAlerts;
import com.epic60869.skyjew.features.core.SkyJewChat;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Yaw/pitch, pest cooldown, blocks per second and the dye / Ray of Helios animation. */
public final class GardenFeatures {
    // Pest patterns from SkyHanni's repo (MIT).
    private static final Pattern PEST_SPAWN = Pattern.compile("^\\w+! (?:A|\\d) .*Pests? (?:has|have) (?:appeared|spawned) in ");
    private static final Pattern TAB_COOLDOWN = Pattern.compile("^\\s*Cooldown: (?<time>\\d{1,2}[ms](?: \\d{1,2}s?)?)?(?<ready>READY)?(?<max>MAX PESTS)?");
    private static final Pattern SPECIAL_DROP = Pattern.compile("(?<item>[A-Z][\\w' ]* Dye|Ray of Helios)");
    private static final String[] FACINGS = {"South", "South West", "West", "North West", "North", "North East", "East", "South East"};

    private static final Deque<Long> BREAKS = new ArrayDeque<>();
    private static long lastPestSpawn;

    private GardenFeatures() {}

    private static FeatureConfigs.Garden config() {
        SkyJewConfig c = SkyJewConfig.current();
        return c == null ? null : c.farming.garden;
    }

    public static void init() {
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
            synchronized (BREAKS) {
                BREAKS.addLast(System.currentTimeMillis());
            }
        });
        SkyJewChat.onChat(GardenFeatures::onChat);

        SkyJewHuds.register("yaw_pitch", "Yaw and Pitch",
            () -> config() != null && config().yawPitch && SkyJewLocation.inGarden(),
            GardenFeatures::yawPitchLines,
            List.of(kv("Yaw: ", "123.96"), kv("Pitch: ", "0.00"), kv("Facing: ", "West")),
            8, 300);
        SkyJewHuds.register("pest_cooldown", "Pest Cooldown",
            () -> config() != null && config().pestCooldown && SkyJewLocation.inGarden(),
            GardenFeatures::pestLines,
            List.of(kv("Pest cooldown: ", "3:21")),
            8, 340);
        SkyJewHuds.register("bps", "Blocks Per Second",
            () -> config() != null && config().blocksPerSecond && SkyJewLocation.inGarden(),
            GardenFeatures::bpsLines,
            List.of(kv("BPS: ", "19.8")),
            8, 354);
    }

    private static Component kv(String key, String value) {
        return Component.literal(key).withStyle(ChatFormatting.GRAY).append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static List<Component> yawPitchLines() {
        var player = Minecraft.getInstance().player;
        if (player == null) return List.of();
        float yaw = Mth.wrapDegrees(player.getYRot());
        float pitch = player.getXRot();
        int index = Math.floorMod(Math.round(yaw / 45f), 8);
        return List.of(
            kv("Yaw: ", String.format(Locale.US, "%.2f", yaw)),
            kv("Pitch: ", String.format(Locale.US, "%.2f", pitch)),
            kv("Facing: ", FACINGS[index]));
    }

    private static List<Component> pestLines() {
        // Prefer Hypixel's own cooldown from the tab list Pests widget when it is shown.
        for (PlayerInfo info : SkyJewTabWidgetManager.players()) {
            if (info.getTabListDisplayName() == null) continue;
            Matcher m = TAB_COOLDOWN.matcher(SkyJewLocation.strip(info.getTabListDisplayName().getString()));
            if (m.find() && (m.group("time") != null || m.group("ready") != null || m.group("max") != null)) {
                String value = m.group("ready") != null ? "READY" : m.group("max") != null ? "MAX PESTS" : m.group("time");
                return List.of(Component.literal("Pest cooldown: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(value).withStyle(m.group("time") == null ? ChatFormatting.GREEN : ChatFormatting.YELLOW)));
            }
        }
        if (lastPestSpawn == 0) return List.of(kv("Pest cooldown: ", "unknown"));
        long remaining = (long) (config().pestCooldownSeconds * 1000) - (System.currentTimeMillis() - lastPestSpawn);
        if (remaining <= 0) {
            return List.of(Component.literal("Pest cooldown: ").withStyle(ChatFormatting.GRAY).append(Component.literal("READY").withStyle(ChatFormatting.GREEN)));
        }
        long seconds = remaining / 1000;
        return List.of(Component.literal("Pest cooldown: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)).withStyle(ChatFormatting.YELLOW)));
    }

    private static List<Component> bpsLines() {
        long now = System.currentTimeMillis();
        int count;
        synchronized (BREAKS) {
            while (!BREAKS.isEmpty() && now - BREAKS.peekFirst() > 2000) BREAKS.removeFirst();
            count = BREAKS.size();
        }
        return List.of(kv("BPS: ", String.format(Locale.US, "%.1f", count / 2.0)));
    }

    private static void onChat(SkyJewChat.Message message) {
        String text = message.text();
        if (PEST_SPAWN.matcher(text).find()) {
            lastPestSpawn = System.currentTimeMillis();
            return;
        }
        FeatureConfigs.Garden config = config();
        if (config == null || !config.specialDropAnimation || !SkyJewLocation.inGarden()) return;
        boolean dropMessage = text.contains("DROP!") || text.contains("CROP!") || text.startsWith("WOW!") || text.contains(" found ");
        if (!dropMessage) return;
        Matcher m = SPECIAL_DROP.matcher(text);
        if (m.find()) {
            SkyJewAlerts.dropAnimation(Component.literal(m.group("item") + "!").withStyle(ChatFormatting.BOLD), 0xFF55FF);
        }
    }
}

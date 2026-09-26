package com.epic60869.skyjew.features.mining;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.features.FeatureConfigs;
import com.epic60869.skyjew.features.core.SkyJewAlerts;
import com.epic60869.skyjew.features.core.SkyJewChat;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pickaxe ability cooldown HUD (Mining Speed Boost, Pickobulus, Maniac Miner, ...). Everything comes from Hypixel's
 * chat: "You used your X Pickaxe Ability!" starts the cooldown, "This ability is on cooldown for Ns." corrects it, and
 * "X is now available!" marks it ready. The cooldown length is learned from how long that took last time, since it
 * depends on your Heart of the Mountain perks.
 */
public final class PickaxeAbility {
    private static final Pattern USED = Pattern.compile("^You used your (?<ability>.+?) Pickaxe Ability!$");
    private static final Pattern ON_COOLDOWN = Pattern.compile("^(?:This ability is on cooldown for|Your pickaxe ability is on cooldown for) (?<seconds>\\d+)s\\.$");
    private static final Pattern AVAILABLE = Pattern.compile("^(?<ability>.+?) is now available!$");
    private static final int DEFAULT_COOLDOWN = 120;

    private static String ability = "";
    private static long readyAt = -1;
    private static long usedAt = -1;
    private static int cooldownSeconds = DEFAULT_COOLDOWN;
    private static boolean alerted = true;

    private PickaxeAbility() {}

    private static FeatureConfigs.MiningFeatures config() {
        SkyJewConfig c = SkyJewConfig.current();
        return c == null ? null : c.mining.features;
    }

    private static boolean inMiningArea() {
        String area = SkyJewLocation.area();
        return SkyJewLocation.inDwarvenMines() || SkyJewLocation.inCrystalHollows() || area.contains("Mineshaft")
            || area.equals("Gold Mine") || area.equals("Deep Caverns") || area.equals("The End") || area.equals("Crimson Isle");
    }

    public static void init() {
        SkyJewChat.onChat(message -> onChat(message.text().trim()));
        SkyJewHuds.register("pickaxe_ability", "Pickaxe Ability",
            () -> config() != null && config().pickaxeAbilityHud && !ability.isEmpty() && inMiningArea(),
            PickaxeAbility::lines,
            List.of(Component.literal("Mining Speed Boost: ").withStyle(ChatFormatting.GOLD).append(Component.literal("Ready").withStyle(ChatFormatting.GREEN))),
            8, 300);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (alerted || readyAt < 0 || System.currentTimeMillis() < readyAt) return;
            alerted = true;
            FeatureConfigs.MiningFeatures c = config();
            if (c != null && c.pickaxeAbilityAlert && inMiningArea()) {
                SkyJewAlerts.title(Component.literal(ability + " Ready!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), Component.empty());
            }
        });
    }

    private static void onChat(String text) {
        Matcher m;
        long now = System.currentTimeMillis();
        if ((m = USED.matcher(text)).matches()) {
            ability = m.group("ability");
            usedAt = now;
            readyAt = now + cooldownSeconds * 1000L;
            alerted = false;
        } else if ((m = ON_COOLDOWN.matcher(text)).matches()) {
            readyAt = now + Integer.parseInt(m.group("seconds")) * 1000L;
            alerted = false;
        } else if ((m = AVAILABLE.matcher(text)).matches() && isAbility(m.group("ability"))) {
            ability = m.group("ability");
            // Learn the real cooldown (HOTM perks change it).
            if (usedAt > 0) {
                int learned = (int) Math.round((now - usedAt) / 1000.0);
                if (learned >= 10 && learned <= 600) cooldownSeconds = learned;
            }
            usedAt = -1;
            readyAt = now;
        }
    }

    private static boolean isAbility(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.equals("mining speed boost") || n.equals("pickobulus") || n.equals("maniac miner") || n.equals("gemstone infusion")
            || n.equals("sheer force") || n.equals("anomalous desire") || n.equals("vein seeker") || n.equals("hazardous miner")
            || n.equals(ability.toLowerCase(Locale.ROOT));
    }

    private static List<Component> lines() {
        long left = readyAt - System.currentTimeMillis();
        Component value = left <= 0
            ? Component.literal("Ready").withStyle(ChatFormatting.GREEN)
            : Component.literal(String.format(Locale.US, "%.1fs", left / 1000.0)).withStyle(ChatFormatting.RED);
        return List.of(Component.literal(ability + ": ").withStyle(ChatFormatting.GOLD).append(value));
    }
}

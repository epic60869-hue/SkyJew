package com.epic60869.skyjew.features.mining;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewTabWidgetManager;
import com.epic60869.skyjew.features.FeatureConfigs;
import com.epic60869.skyjew.features.core.SkyJewAlerts;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pickaxe ability cooldown HUD (Mining Speed Boost, Pickobulus, ...), read from Hypixel's "Pickaxe Ability" tab list
 * widget like SkyHanni does: " Pickobulus: Available" or " Pickobulus: 1m 5s". Hypixel keeps that line up to date, so
 * it is always right whatever your Heart of the Mountain perks are. Turn the widget on in Hypixel's tab list settings.
 */
public final class PickaxeAbility {
    private static final Pattern LINE = Pattern.compile("^(?<ability>[A-Za-z' ]+?)\\s*:\\s*(?<value>.+)$");
    private static final Pattern MINUTES = Pattern.compile("(\\d+)\\s*m");
    private static final Pattern SECONDS = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*s");

    private static String ability = "";
    /** When the ability is ready (millis), or -1 when it already is. */
    private static long readyAt = -1;
    private static boolean onCooldown;
    private static boolean widgetFound;

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
        SkyJewHuds.register("pickaxe_ability", "Pickaxe Ability",
            () -> config() != null && config().pickaxeAbilityHud && inMiningArea(),
            PickaxeAbility::lines,
            List.of(Component.literal("Mining Speed Boost: ").withStyle(ChatFormatting.GOLD).append(Component.literal("Ready").withStyle(ChatFormatting.GREEN))),
            8, 300);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick());
    }

    private static void tick() {
        if (!SkyJewLocation.onSkyblock()) return;
        SkyJewTabWidgetManager.Widget widget = SkyJewTabWidgetManager.get("Pickaxe Ability");
        List<String> texts = new ArrayList<>();
        if (widget.detail() != null) texts.add(widget.detail().getString());
        for (Component line : widget.lines()) texts.add(line.getString());
        widgetFound = !widget.lines().isEmpty() || (widget.detail() != null && !widget.detail().getString().isBlank());

        for (String raw : texts) {
            String text = SkyJewLocation.strip(raw).trim();
            Matcher m = LINE.matcher(text);
            String name;
            String value;
            if (m.matches()) {
                name = m.group("ability").trim();
                value = m.group("value").trim();
            } else if (!text.isEmpty() && !ability.isEmpty()) {
                // "Pickaxe Ability: Pickobulus" in the header, time on its own line.
                name = ability;
                value = text;
            } else {
                if (!text.isEmpty()) ability = text;
                continue;
            }
            if (name.isEmpty()) continue;
            ability = name;
            long left = parseMillis(value);
            boolean wasOnCooldown = onCooldown;
            if (left > 0) {
                onCooldown = true;
                readyAt = System.currentTimeMillis() + left;
            } else if (isReady(value)) {
                onCooldown = false;
                readyAt = -1;
                if (wasOnCooldown) alert();
            }
            return;
        }
    }

    private static boolean isReady(String value) {
        String v = value.toLowerCase(Locale.ROOT);
        return v.contains("available") || v.contains("ready");
    }

    /** "1m 5s", "45s", "12.3s" -> milliseconds; 0 when there's no time in it. */
    private static long parseMillis(String value) {
        long ms = 0;
        Matcher m = MINUTES.matcher(value);
        if (m.find()) ms += Long.parseLong(m.group(1)) * 60_000L;
        m = SECONDS.matcher(value);
        if (m.find()) ms += (long) (Double.parseDouble(m.group(1)) * 1000);
        return ms;
    }

    private static void alert() {
        FeatureConfigs.MiningFeatures c = config();
        if (c != null && c.pickaxeAbilityAlert && inMiningArea()) {
            SkyJewAlerts.title(Component.literal(ability + " Ready!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), Component.empty());
        }
    }

    private static List<Component> lines() {
        if (!widgetFound || ability.isEmpty()) {
            return List.of(Component.literal("Pickaxe Ability: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("turn on its tab list widget").withStyle(ChatFormatting.GRAY)));
        }
        long left = readyAt - System.currentTimeMillis();
        Component value = !onCooldown || left <= 0
            ? Component.literal("Ready").withStyle(ChatFormatting.GREEN)
            : Component.literal(String.format(Locale.US, "%.1fs", left / 1000.0)).withStyle(ChatFormatting.RED);
        return List.of(Component.literal(ability + ": ").withStyle(ChatFormatting.GOLD).append(value));
    }
}

package com.epic60869.skyjew.features.combat;

import com.epic60869.skyjew.ItemPriceResolver;
import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.features.FeatureConfigs;
import com.epic60869.skyjew.features.core.SkyJewAlerts;
import com.epic60869.skyjew.features.core.SkyJewChat;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Arrow counter, legion display, cocoon alert and rare drop copy/animation. */
public final class CombatFeatures {
    // Quiver patterns from SkyHanni's repo (MIT).
    private static final Pattern ACTIVE_ARROW = Pattern.compile("Active Arrow: (?<type>.*) \\((?<amount>[\\d,]+)\\)");
    private static final Pattern ARROW_SELECT = Pattern.compile("You set your selected arrow type to (?<arrow>.*)!");
    private static final Pattern ARROW_RAN_OUT = Pattern.compile("QUIVER! You have run out of (?<type>.*)s!");
    private static final Pattern COCOON = Pattern.compile("CAUGHT! You cocooned an? (?<name>[\\w ]+)!");
    private static final Pattern COCOON_BOSS = Pattern.compile("\\s*YOU COCOONED YOUR SLAYER BOSS");
    private static final Pattern RARE_DROP = Pattern.compile(
        "^(?<type>(?:VERY |CRAZY |INSANE |PRAY TO RNGESUS )?RARE DROP!|PET DROP!|INSANE DROP!)\\s+\\(?(?<item>.+?)\\)?(?:\\s+x(?<amount>[\\d,]+))?(?:\\s+\\(\\+[\\d,.]+%? ?.*Magic Find\\))?\\s*$");
    private static final double LEGION_RANGE = 30;

    private static String arrowType;
    private static long arrowAmount = -1;
    private static int legionCount;
    private static int ticks;

    private CombatFeatures() {}

    private static FeatureConfigs.Combat config() {
        SkyJewConfig c = SkyJewConfig.current();
        return c == null ? null : c.combat;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 10 == 0) tick(mc);
        });
        SkyJewChat.onChat(CombatFeatures::onChat);

        SkyJewHuds.register("arrows", "Arrow Counter",
            () -> config() != null && config().arrowCounter && SkyJewLocation.onSkyblock(),
            CombatFeatures::arrowLines,
            List.of(line("Arrows: ", "Flint Arrow ", ChatFormatting.WHITE).append(Component.literal("x1,234").withStyle(ChatFormatting.GREEN))),
            8, 120);
        SkyJewHuds.register("legion", "Legion Display",
            () -> config() != null && config().legionDisplay && SkyJewLocation.onSkyblock(),
            () -> List.of(line("Legion: ", legionCount + (legionCount == 1 ? " player" : " players"), ChatFormatting.AQUA)),
            List.of(line("Legion: ", "4 players", ChatFormatting.AQUA)),
            8, 134);
    }

    private static Component header(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    private static net.minecraft.network.chat.MutableComponent line(String label, String value, ChatFormatting color) {
        return Component.literal(label).withStyle(ChatFormatting.GRAY).append(Component.literal(value).withStyle(color));
    }

    private static List<Component> arrowLines() {
        if (arrowType == null) return List.of();
        var value = Component.literal(arrowType + " ").withStyle(ChatFormatting.WHITE);
        if (arrowAmount >= 0) {
            value.append(Component.literal("x" + String.format(Locale.US, "%,d", arrowAmount))
                .withStyle(arrowAmount < 128 ? ChatFormatting.RED : ChatFormatting.GREEN));
        }
        return List.of(header("Arrows: ").copy().append(value));
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        // The quiver's active arrow is shown in the lore of an inventory item (the SkyBlock menu slot while holding a bow).
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;
            for (Component loreLine : lore.lines()) {
                Matcher m = ACTIVE_ARROW.matcher(SkyJewLocation.strip(loreLine.getString()));
                if (m.find()) {
                    arrowType = m.group("type").trim();
                    arrowAmount = Long.parseLong(m.group("amount").replace(",", ""));
                }
            }
        }

        FeatureConfigs.Combat config = config();
        if (config != null && config.legionDisplay) {
            int count = 0;
            for (Player player : mc.level.players()) {
                if (player == mc.player || player.getUUID().version() != 4) continue; // NPCs use v2 UUIDs
                if (player.distanceTo(mc.player) <= LEGION_RANGE) count++;
            }
            legionCount = count;
        }
    }

    private static void onChat(SkyJewChat.Message message) {
        FeatureConfigs.Combat config = config();
        if (config == null) return;
        String text = message.text();

        Matcher m = ARROW_SELECT.matcher(text);
        if (m.find()) {
            arrowType = m.group("arrow").trim();
            arrowAmount = -1;
        } else if ((m = ARROW_RAN_OUT.matcher(text)).find()) {
            arrowType = m.group("type").trim();
            arrowAmount = 0;
        } else if (text.contains("Cleared your quiver!") || text.contains("Your quiver is now completely empty!")) {
            arrowAmount = 0;
        }

        if (config.cocoonAlert.enabled) {
            String mob = null;
            if ((m = COCOON.matcher(text)).find()) mob = m.group("name").trim();
            else if (COCOON_BOSS.matcher(text).matches()) mob = "Slayer Boss";
            if (mob != null && wanted(config.cocoonAlert.mobs, mob)) {
                SkyJewAlerts.title(Component.literal("COCOONED!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    Component.literal(mob).withStyle(ChatFormatting.WHITE));
            }
        }

        if ((m = RARE_DROP.matcher(text)).find()) onRareDrop(config.rareDrops, text, m.group("item").trim());
    }

    private static boolean wanted(String list, String mob) {
        if (list == null || list.isBlank()) return true;
        String lower = mob.toLowerCase(Locale.ROOT);
        return Arrays.stream(list.split(",")).map(s -> s.trim().toLowerCase(Locale.ROOT))
            .anyMatch(s -> !s.isEmpty() && (lower.contains(s) || s.contains(lower)));
    }

    private static void onRareDrop(FeatureConfigs.RareDrops config, String text, String item) {
        Minecraft mc = Minecraft.getInstance();
        if (config.copy) {
            mc.execute(() -> mc.keyboardHandler.setClipboard(text));
        }
        if (config.animation) {
            ItemPriceResolver.valueByNameAsync(item).thenAccept(value -> {
                if (value != null && value >= config.thresholdMillions * 1_000_000d) {
                    SkyJewAlerts.dropAnimation(Component.literal(item + "!").withStyle(ChatFormatting.BOLD), 0xFFAA00);
                    SkyJewAlerts.chat(Component.literal(item + " is worth " + formatCoins(value) + " coins!").withStyle(ChatFormatting.GOLD));
                }
            });
        }
    }

    public static String formatCoins(double value) {
        if (value >= 1_000_000_000) return String.format(Locale.US, "%.2fB", value / 1_000_000_000);
        if (value >= 1_000_000) return String.format(Locale.US, "%.2fM", value / 1_000_000);
        if (value >= 1_000) return String.format(Locale.US, "%.1fk", value / 1_000);
        return String.format(Locale.US, "%.0f", value);
    }

    static List<Component> list(Component... lines) {
        return new ArrayList<>(Arrays.asList(lines));
    }
}

package com.epic60869.skyjew.features.garden;

import com.epic60869.skyjew.ItemPriceResolver;
import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.custom.util.Compat;
import com.epic60869.skyjew.features.FeatureConfigs;
import com.epic60869.skyjew.features.core.SkyJewChat;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Farming profit tracker, ported from Skysoft's Profit Tracker "Farming" preset
 * (https://github.com/Akinsoft/Skysoft, LGPL-3.0).
 * <ul>
 *     <li>Counts farming items you gain in the Garden: new items in your inventory (not while a menu is open, so
 *     moving items around never counts), items added to your sacks (read from the "[Sacks]" message's hover), and
 *     chat drops (rare crops, pest rewards, BLESSED!, OVERFLOW!, pet drops).</li>
 *     <li>Replenish costs: crops replanted by Replenish are taken off again, like Skysoft.</li>
 *     <li>Pest kills, Kernels (valued through Feast I) and coins picked up.</li>
 *     <li>Uptime only runs while you are farming and pauses after a set time without activity; profit per hour
 *     uses that uptime.</li>
 * </ul>
 * Totals are saved per SkyBlock profile; the session resets when the game restarts or with /sj farmingprofit reset.
 */
public final class FarmingProfitTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Skysoft's Farming preset items. */
    private static final Set<String> TRACKED = Set.of(
        "WHEAT", "SEEDS", "CARROT_ITEM", "POTATO_ITEM", "NETHER_STALK", "PUMPKIN", "MELON", "INK_SACK-3", "INK_SACK:3", "SUGAR_CANE",
        "CACTUS", "RED_MUSHROOM", "BROWN_MUSHROOM", "HUGE_MUSHROOM_1", "HUGE_MUSHROOM_2", "DOUBLE_PLANT", "MOONFLOWER", "WILD_ROSE",
        "ENCHANTED_WHEAT", "ENCHANTED_HAY_BALE", "ENCHANTED_GOLDEN_CARROT", "ENCHANTED_BAKED_POTATO", "MUTANT_NETHER_STALK",
        "POLISHED_PUMPKIN", "ENCHANTED_MELON_BLOCK", "ENCHANTED_COOKIE", "ENCHANTED_SUGAR_CANE", "ENCHANTED_CACTUS",
        "ENCHANTED_HUGE_MUSHROOM_1", "ENCHANTED_HUGE_MUSHROOM_2", "COMPACTED_SUNFLOWER", "COMPACTED_MOONFLOWER", "COMPACTED_WILD_ROSE",
        "CROPIE", "SQUASH", "FERMENTO", "HELIANTHUS", "CORNUCOPIA", "CARROT_ZEST", "DEEPFRIES", "AGGOURDIAN", "CANE_KNOT",
        "MELON_JUICE", "CACTUS_FLOWER", "DESIGNER_COFFEE_BEANS", "FEASTFUNGUS", "BOTROOT", "SALTED_SUNFLOWER_SEEDS",
        "CRYSTALIZED_MOONLIGHT", "FLORAL_GELATIN", "RAREFINDER_GARDEN_CHIP", "BURROWING_SPORES", "WARTY", "DYE_WILD_STRAWBERRY",
        "DYE_DUNG", "DYE_COPPER", "TOOL_EXP_CAPSULE", "SQUEAKY_TOY", "SQUEAKY_MOUSEMAT", "BEADY_EYES", "CHIRPING_STEREO",
        "BOOKWORM_BOOK", "LOCUST_LARVA", "ATMOSPHERIC_FILTER", "CLIPPED_WINGS", "WRIGGLING_LARVA", "OVERCLOCKER_3000",
        "MANTID_CLAW", "FIRE_IN_A_BOTTLE", "VERMIN_VAPORIZER_GARDEN_CHIP", "DUNG", "COMPOST", "HONEY_JAR", "PLANT_MATTER",
        "CHEESE_FUEL", "JELLY", "VINYL_BEETLE", "VINYL_CRICKET_CHOIR", "VINYL_EARTHWORM_ENSEMBLE", "VINYL_PRETTY_FLY",
        "VINYL_CICADA_SYMPHONY", "VINYL_DYNAMITES", "VINYL_BUZZIN_BEATS", "VINYL_WINGS_OF_HARMONY", "VINYL_RODENT_REVOLUTION",
        "VINYL_SLOW_AND_GROOVY", "VINYL_PRAY_FOR_ME", "VINYL_FIREFLY", "VINYL_IMAGINE_DRAGONFLIES", "SLUG;3", "SLUG;4", "RAT;4",
        "ATTRIBUTE_SHARD_PEST_LUCK;1", "ENCHANTMENT_PESTERMINATOR_1", "ENCHANTMENT_ULTIMATE_SUNSET_1");

    private static final List<Pattern> DROP_PATTERNS = List.of(
        Pattern.compile("^BLESSED! You found an? (?<item>.+)!$"),
        Pattern.compile("^(?:VERY )?RARE CROP! (?<item>.+?)(?: \\(.*)?$"),
        Pattern.compile("^[\\w ]+! You dropped (?<amount>[\\d,]+)x (?<item>[\\w ]+)!$"),
        Pattern.compile("^ABOUT TIME! You find an? (?<item>.+?) \\(.*\\)!$"),
        Pattern.compile("^OVERFLOW! Your .+ has just dropped an? (?<item>Tool Exp Capsule)!$"),
        Pattern.compile("^(?:RARE|PET) DROP! (?<item>.+?)(?: x(?<amount>\\d+))? \\(.*\\)!?$"));
    private static final Pattern PEST_KILL = Pattern.compile("^You received (?<amount>\\d+)x (?<item>.+) for killing an? (?<pest>.+)!$");
    private static final Pattern SACK_LINE = Pattern.compile("^\\s*(?<amount>[+-][\\d,]+) (?<item>.+) \\((?<sacks>.+)\\)$");
    private static final Pattern PURSE = Pattern.compile("(?:Purse|Piggy): ([\\d,]+(?:\\.\\d+)?)");
    private static final Pattern NPC_SALE = Pattern.compile("^You sold .+ x[\\d,]+ for [\\d,]+ Coins!$");
    private static final String KERNEL_DONATION = "[NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.";

    public static final class Stats {
        public Map<String, Long> items = new LinkedHashMap<>();
        public Map<String, Long> pests = new LinkedHashMap<>();
        public long coins;
        public long kernels;
        public long activeMillis;

        void add(String id, long amount) {
            long updated = Math.max(0, items.getOrDefault(id, 0L) + amount);
            if (updated == 0) items.remove(id);
            else items.put(id, updated);
        }
    }

    private static Path file;
    private static Map<String, Stats> totals = new HashMap<>();
    private static Stats session = new Stats();
    private static String profile = "";

    private static Map<String, Integer> lastInventory;
    private static boolean screenWasOpen;
    private static final Map<String, Integer> suppressedGains = new HashMap<>();
    private static long suppressUntil;
    private static final Map<String, Integer> replenishPending = new HashMap<>();
    private static long lastActivity;
    private static long lastTickMillis;
    private static double lastPurse = -1;
    private static long lastSale;
    private static int saveTicks;

    private FarmingProfitTracker() {}

    private static FeatureConfigs.FarmingProfit config() {
        SkyJewConfig c = SkyJewConfig.current();
        return c == null ? null : c.farming.profitTracker;
    }

    private static boolean active() {
        FeatureConfigs.FarmingProfit config = config();
        return config != null && config.enabled && SkyJewLocation.inGarden();
    }

    private static Stats total() {
        return totals.computeIfAbsent(profile, k -> new Stats());
    }

    public static void init(Path configDir) {
        file = configDir.resolve("skyjew").resolve("farming-profit.json");
        load();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
            lastInventory = null;
            lastPurse = -1;
        });
        SkyJewChat.onChat(message -> onChat(message.component(), message.text()));
        ClientTickEvents.END_CLIENT_TICK.register(FarmingProfitTracker::tick);
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> onBreak(state.getBlock(), level.getGameTime()));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("farmingprofit")
                    .then(ClientCommands.literal("reset").executes(c -> {
                        FeatureConfigs.FarmingProfit config = config();
                        if (config != null && config.display == FeatureConfigs.TrackerDisplay.TOTAL) {
                            totals.put(profile, new Stats());
                            save();
                        } else {
                            session = new Stats();
                        }
                        c.getSource().sendFeedback(Component.literal("Farming profit tracker reset.").withStyle(ChatFormatting.GREEN));
                        return 1;
                    }))));
            }
        });
        SkyJewHuds.register("farming_profit", "Farming Profit Tracker",
            () -> active(),
            FarmingProfitTracker::lines,
            List.of(Component.literal("Farming Profit Tracker").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("§7 12,480x §fEnchanted Hay Bale§7: §616.2M"),
                Component.literal("§7 3x §9Cropie§7: §636.0K"),
                Component.literal("§7Pests killed: §a14"),
                Component.literal("§eProfit: §627.4M §7(§619.1M/h§7)"),
                Component.literal("§7Uptime: §b1h 26m")),
            8, 260);
    }

    // ----- Gains -----

    private static void gain(String id, int amount) {
        if (id == null || amount == 0 || !TRACKED.contains(id)) return;
        if (amount > 0) {
            Integer pending = replenishPending.remove(replenishCostFor(id));
            if (pending != null && pending > 0) record(replenishCostFor(id), -pending);
        }
        record(id, amount);
    }

    private static void record(String id, long amount) {
        if (amount == 0) return;
        session.add(id, amount);
        total().add(id, amount);
        lastActivity = System.currentTimeMillis();
    }

    private static void onChat(Component component, String text) {
        if (!active()) return;
        String clean = text.trim();
        if (NPC_SALE.matcher(clean).matches()) lastSale = System.currentTimeMillis();
        if (clean.equals(KERNEL_DONATION)) {
            session.kernels++;
            total().kernels++;
            lastActivity = System.currentTimeMillis();
            return;
        }
        if (clean.startsWith("[Sacks]")) {
            onSacks(component);
            return;
        }
        Matcher pest = PEST_KILL.matcher(clean);
        if (pest.matches()) {
            String kind = pest.group("pest");
            session.pests.merge(kind, 1L, Long::sum);
            total().pests.merge(kind, 1L, Long::sum);
            chatDrop(pest.group("item"), Integer.parseInt(pest.group("amount")));
            return;
        }
        for (Pattern p : DROP_PATTERNS) {
            Matcher m = p.matcher(clean);
            if (!m.matches()) continue;
            String amount = null;
            try {
                amount = m.group("amount");
            } catch (IllegalArgumentException ignored) {}
            chatDrop(m.group("item"), amount == null ? 1 : Integer.parseInt(amount.replace(",", "")));
            return;
        }
    }

    /** A drop announced in chat also lands in the inventory; that gain is skipped so it only counts once. */
    private static void chatDrop(String name, int amount) {
        String id = ItemPriceResolver.idByName(SkyJewLocation.strip(name).trim());
        if (id == null || !TRACKED.contains(id)) return;
        record(id, amount);
        suppressedGains.merge(id, amount, Integer::sum);
        suppressUntil = System.currentTimeMillis() + 60_000;
    }

    private static void onSacks(Component component) {
        Set<String> hovers = new HashSet<>();
        collectHovers(component, hovers);
        for (String hover : hovers) {
            if (!hover.startsWith("Added items:")) continue;
            for (String line : hover.split("\n")) {
                Matcher m = SACK_LINE.matcher(line);
                if (!m.matches()) continue;
                int amount = Integer.parseInt(m.group("amount").replace(",", "").replace("+", ""));
                String id = ItemPriceResolver.idByName(m.group("item").trim());
                Integer suppressed = id == null ? null : suppressedGains.get(id);
                if (suppressed != null && System.currentTimeMillis() < suppressUntil) {
                    int used = Math.min(suppressed, amount);
                    amount -= used;
                    if (suppressed - used <= 0) suppressedGains.remove(id);
                    else suppressedGains.put(id, suppressed - used);
                }
                gain(id, amount);
            }
        }
    }

    private static void collectHovers(Component component, Set<String> out) {
        HoverEvent hover = component.getStyle().getHoverEvent();
        if (hover instanceof HoverEvent.ShowText text) out.add(SkyJewLocation.strip(text.value().getString()));
        for (Component sibling : component.getSiblings()) collectHovers(sibling, out);
    }

    private static void tick(Minecraft mc) {
        long now = System.currentTimeMillis();
        long delta = lastTickMillis == 0 ? 0 : Math.min(1000, now - lastTickMillis);
        lastTickMillis = now;
        if (!active() || mc.player == null) {
            lastInventory = null;
            return;
        }
        FeatureConfigs.FarmingProfit config = config();

        // Uptime: runs while you've done something farming-related within the pause time.
        if (lastActivity > 0 && now - lastActivity <= config.pauseAfterSeconds * 1000L) {
            session.activeMillis += delta;
            total().activeMillis += delta;
        }

        boolean screenOpen = mc.gui.screen() instanceof AbstractContainerScreen<?>;
        Map<String, Integer> inventory = new HashMap<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = Compat.neuName(stack);
            if (!id.isEmpty()) inventory.merge(id, stack.getCount(), Integer::sum);
        }
        if (lastInventory != null && !screenOpen && !screenWasOpen) {
            for (var entry : inventory.entrySet()) {
                int change = entry.getValue() - lastInventory.getOrDefault(entry.getKey(), 0);
                if (change <= 0) continue;
                Integer suppressed = suppressedGains.get(entry.getKey());
                if (suppressed != null && now < suppressUntil) {
                    int used = Math.min(suppressed, change);
                    change -= used;
                    if (suppressed - used <= 0) suppressedGains.remove(entry.getKey());
                    else suppressedGains.put(entry.getKey(), suppressed - used);
                }
                gain(entry.getKey(), change);
            }
        }
        lastInventory = inventory;
        screenWasOpen = screenOpen;

        // Coins: purse increases while no menu is open and nothing was just sold.
        double purse = purse();
        if (purse >= 0) {
            if (lastPurse >= 0 && purse > lastPurse && !screenOpen && now - lastSale > 3000) {
                long coins = Math.round(purse - lastPurse);
                if (coins < 50_000_000) {
                    session.coins += coins;
                    total().coins += coins;
                }
            }
            lastPurse = purse;
        }

        if (++saveTicks >= 20 * 60) {
            saveTicks = 0;
            save();
        }
    }

    private static double purse() {
        for (String line : SkyJewLocation.scoreboard()) {
            Matcher m = PURSE.matcher(line);
            if (m.find()) {
                try {
                    return Double.parseDouble(m.group(1).replace(",", ""));
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }

    // ----- Replenish (ProfitReplenishCosts) -----

    private static void onBreak(Block block, long dayTime) {
        if (!active() || !isCrop(block)) return;
        lastActivity = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !hasReplenish(mc.player.getMainHandItem())) return;
        String cost = switch (block) {
            case Block b when b == Blocks.WHEAT -> "SEEDS";
            case Block b when b == Blocks.CARROTS -> "CARROT_ITEM";
            case Block b when b == Blocks.POTATOES -> "POTATO_ITEM";
            case Block b when b == Blocks.NETHER_WART -> "NETHER_STALK";
            case Block b when b == Blocks.COCOA -> "INK_SACK:3";
            case Block b when b == Blocks.ROSE_BUSH -> "WILD_ROSE";
            case Block b when b == Blocks.SUNFLOWER -> dayTime % 24_000 >= 12_000 ? "MOONFLOWER" : "DOUBLE_PLANT";
            default -> null;
        };
        if (cost != null) replenishPending.merge(cost, 1, Integer::sum);
    }

    /** The item Replenish uses up when you harvest this item's crop. */
    private static String replenishCostFor(String harvestId) {
        return harvestId.equals("WHEAT") ? "SEEDS" : harvestId.equals("INK_SACK-3") ? "INK_SACK:3" : harvestId;
    }

    private static boolean hasReplenish(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        return data.copyTag().getCompoundOrEmpty("enchantments").contains("replenish");
    }

    private static boolean isCrop(Block block) {
        return block == Blocks.WHEAT || block == Blocks.CARROTS || block == Blocks.POTATOES || block == Blocks.NETHER_WART
            || block == Blocks.PUMPKIN || block == Blocks.CARVED_PUMPKIN || block == Blocks.MELON || block == Blocks.COCOA
            || block == Blocks.SUGAR_CANE || block == Blocks.CACTUS || block == Blocks.RED_MUSHROOM || block == Blocks.BROWN_MUSHROOM
            || block == Blocks.RED_MUSHROOM_BLOCK || block == Blocks.BROWN_MUSHROOM_BLOCK || block == Blocks.SUNFLOWER || block == Blocks.ROSE_BUSH;
    }

    // ----- HUD -----

    private static String displayName(String id) {
        String name = id.replaceAll("[;:-]\\d+$", "").toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder out = new StringBuilder();
        for (String word : name.split(" ")) {
            if (word.isEmpty()) continue;
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return out.toString().trim().replace("Enchanted ", "Ench ").replace("Ink Sack", "Cocoa Beans")
            .replace("Nether Stalk", "Nether Wart").replace("Carrot Item", "Carrot").replace("Potato Item", "Potato");
    }

    private static double price(String id) {
        FeatureConfigs.FarmingProfit config = config();
        String lookup = id.replace("INK_SACK-3", "INK_SACK:3");
        if (config != null && config.npcPrices) {
            double npc = ItemPriceResolver.npcPrice(lookup);
            if (npc > 0) return npc;
        }
        return ItemPriceResolver.value(lookup);
    }

    public static String coins(double value) {
        double abs = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        if (abs >= 1_000_000_000) return sign + String.format(Locale.US, "%.2fB", abs / 1e9);
        if (abs >= 1_000_000) return sign + String.format(Locale.US, "%.1fM", abs / 1e6);
        if (abs >= 1_000) return sign + String.format(Locale.US, "%.1fK", abs / 1e3);
        return sign + String.format(Locale.US, "%.0f", abs);
    }

    private static List<Component> lines() {
        FeatureConfigs.FarmingProfit config = config();
        Stats stats = config != null && config.display == FeatureConfigs.TrackerDisplay.TOTAL ? total() : session;
        List<Component> lines = new ArrayList<>();
        String mode = config != null && config.display == FeatureConfigs.TrackerDisplay.TOTAL ? "Total" : "Session";
        lines.add(Component.literal("Farming Profit Tracker ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(Component.literal("(" + mode + ")").withStyle(ChatFormatting.GRAY)));

        record Row(String id, long amount, double value) {}
        List<Row> rows = new ArrayList<>();
        double profit = 0;
        for (var entry : stats.items.entrySet()) {
            double value = price(entry.getKey()) * entry.getValue();
            profit += value;
            rows.add(new Row(entry.getKey(), entry.getValue(), value));
        }
        rows.sort((a, b) -> Double.compare(b.value(), a.value()));
        int max = config == null ? 12 : config.maxItems;
        for (int i = 0; i < Math.min(max, rows.size()); i++) {
            Row r = rows.get(i);
            lines.add(Component.literal(String.format(Locale.US, "§7 %,dx §f%s§7: §6%s", r.amount(), displayName(r.id()), coins(r.value()))));
        }
        if (rows.size() > max) lines.add(Component.literal("§8 +" + (rows.size() - max) + " more"));

        if (stats.kernels > 0) {
            double feast = ItemPriceResolver.value("ENCHANTMENT_FEAST_1");
            double kernelValue = feast > 0 ? stats.kernels * feast / 25.0 : 0;
            profit += kernelValue;
            lines.add(Component.literal("§7 Kernels: §e" + stats.kernels + (kernelValue > 0 ? " §7(§6" + coins(kernelValue) + "§7)" : "")));
        }
        if (stats.coins > 0) {
            profit += stats.coins;
            lines.add(Component.literal("§7 Coins: §6" + coins(stats.coins)));
        }
        long pests = stats.pests.values().stream().mapToLong(Long::longValue).sum();
        if (pests > 0) lines.add(Component.literal("§7Pests killed: §a" + pests));

        double hours = stats.activeMillis / 3_600_000.0;
        String perHour = hours > 0.01 ? " §7(§6" + coins(profit / hours) + "/h§7)" : "";
        lines.add(Component.literal("§eProfit: §6" + coins(profit) + perHour));
        long minutes = stats.activeMillis / 60_000;
        boolean paused = lastActivity == 0 || System.currentTimeMillis() - lastActivity > (config == null ? 30 : config.pauseAfterSeconds) * 1000L;
        lines.add(Component.literal("§7Uptime: §b" + (minutes >= 60 ? minutes / 60 + "h " : "") + minutes % 60 + "m" + (paused ? " §c(paused)" : "")));
        return lines;
    }

    // ----- Storage -----

    private static void load() {
        try {
            if (!Files.exists(file)) return;
            Map<String, Stats> loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), new TypeToken<Map<String, Stats>>() {}.getType());
            if (loaded != null) totals = new HashMap<>(loaded);
        } catch (Exception e) {
            System.err.println("[SkyJew] Could not read farming profit: " + e.getMessage());
        }
    }

    private static void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(totals), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyJew] Could not save farming profit: " + e.getMessage());
        }
    }

    /** Called with the "Profile ID: ..." chat line so totals are kept per profile. */
    public static void setProfile(String id) {
        profile = id;
    }
}

package com.epic60869.skyjew;

import com.epic60869.skyjew.custom.util.Compat;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds prices to SkyBlock item tooltips, following Skyblocker's AvgBinTooltip, LBinTooltip and
 * NpcPriceTooltip: 3 day average price, lowest BIN and NPC sell price, with the stack total and
 * the price each when there is more than one item. Auction prices come from the same API
 * Skyblocker uses (hysky.de); NPC prices come from the Hypixel items API.
 */
public final class SkyJewPriceTooltip {
    private static final String LOWEST_BINS_URL = "https://hysky.de/api/auctions/lowestbins";
    private static final String AVERAGE_URL = "https://hysky.de/api/auctions/lowestbins/average?days=3";
    private static final long REFRESH_MS = 60_000;
    private static final Pattern PET_LEVEL = Pattern.compile("\\[Lvl (\\d+)]");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private static volatile Map<String, Double> lowestBins = Map.of();
    private static volatile Map<String, Double> threeDayAverage = Map.of();
    private static final AtomicBoolean REFRESHING = new AtomicBoolean();
    private static volatile long lastRefresh;

    private SkyJewPriceTooltip() {}

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            SkyJewConfig config = SkyJewConfig.current();
            if (config == null || !config.misc.priceTooltip.enabled || !Compat.isOnSkyblock()) return;
            addPrices(config.misc.priceTooltip, stack, lines);
        });
    }

    private static void refreshIfStale() {
        if (System.currentTimeMillis() - lastRefresh < REFRESH_MS || !REFRESHING.compareAndSet(false, true)) return;
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Double> bins = fetch(LOWEST_BINS_URL);
                if (!bins.isEmpty()) lowestBins = bins;
                Map<String, Double> average = fetch(AVERAGE_URL);
                if (!average.isEmpty()) threeDayAverage = average;
            } finally {
                lastRefresh = System.currentTimeMillis();
                REFRESHING.set(false);
            }
        });
    }

    private static Map<String, Double> fetch(String url) {
        Map<String, Double> result = new HashMap<>();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                .header("User-Agent", "SkyJew/1.0").GET().build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return result;
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            for (var entry : root.entrySet()) {
                try {
                    result.put(entry.getKey(), entry.getValue().getAsDouble());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[SkyJew] Price download failed for " + url + ": " + e.getMessage());
        }
        return result;
    }

    private static void addPrices(SkyJewConfig.PriceTooltip config, ItemStack stack, List<Component> lines) {
        String id = Compat.neuName(stack);
        if (id.isEmpty()) return;
        refreshIfStale();
        String apiId = apiId(stack, id);
        int count = Math.max(1, stack.getCount());

        if (config.threeDayAverage) {
            Double price = threeDayAverage.get(apiId);
            if (price != null) lines.add(line("3 Day Avg. Price: ", ChatFormatting.GOLD, price, count));
        }
        if (config.lowestBin) {
            Double price = lowestBins.get(apiId);
            if (price != null) lines.add(line("Lowest BIN Price: ", ChatFormatting.GOLD, price, count));
        }
        if (config.npcPrice) {
            double price = ItemPriceResolver.npcPrice(id);
            if (price >= 0) lines.add(line("NPC Sell Price: ", ChatFormatting.YELLOW, price, count));
        }
    }

    /** Skyblocker's coin format: the total, plus the price each when there is more than one. */
    private static Component line(String label, ChatFormatting labelColour, double price, int count) {
        String each = String.format(Locale.ENGLISH, "%,.1f", price);
        MutableComponent line = Component.literal(label).withStyle(labelColour);
        if (count == 1) return line.append(Component.literal(each + " Coins").withStyle(ChatFormatting.DARK_AQUA));
        return line.append(Component.literal(String.format(Locale.ENGLISH, "%,.1f", price * count) + " Coins ").withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("(" + each + " each)").withStyle(ChatFormatting.GRAY));
    }

    /** The key the auction price API uses: the item id, except for pets, runes and enchanted books. */
    private static String apiId(ItemStack stack, String id) {
        CompoundTag data = Compat.getCustomData(stack);
        try {
            switch (id) {
                case "PET" -> {
                    JsonObject pet = JsonParser.parseString(data.getStringOr("petInfo", "{}")).getAsJsonObject();
                    Matcher m = PET_LEVEL.matcher(stack.getHoverName().getString());
                    if (pet.has("type") && pet.has("tier") && m.find()) {
                        return "LVL_" + m.group(1) + "_" + pet.get("tier").getAsString() + "_" + pet.get("type").getAsString();
                    }
                }
                case "RUNE", "UNIQUE_RUNE" -> {
                    CompoundTag runes = data.getCompoundOrEmpty("runes");
                    for (String rune : runes.keySet()) return rune + "_RUNE_" + runes.getIntOr(rune, 1);
                }
                case "ENCHANTED_BOOK" -> {
                    CompoundTag enchants = data.getCompoundOrEmpty("enchantments");
                    if (enchants.size() == 1) {
                        for (String enchant : enchants.keySet()) {
                            return "ENCHANTMENT_" + enchant.toUpperCase(Locale.ROOT) + "_" + enchants.getIntOr(enchant, 1);
                        }
                    }
                }
                default -> {}
            }
        } catch (Exception ignored) {}
        return id;
    }
}

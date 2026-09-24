package com.epic60869.skyjew;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class SkyJewRecipeCommand {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final List<ItemEntry> ITEMS = new ArrayList<>();
    private static volatile boolean loaded;

    private record ItemEntry(String id, String name) {}

    private SkyJewRecipeCommand() {}

    public static void init() {
        CompletableFuture.runAsync(SkyJewRecipeCommand::loadItems);
    }

    private static void loadItems() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.hypixel.net/v2/resources/skyblock/items"))
                .GET().build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("[SkyJew] Failed to load Hypixel item list: HTTP " + response.statusCode());
                return;
            }
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray items = root.getAsJsonArray("items");
            if (items == null) return;

            List<ItemEntry> loadedItems = new ArrayList<>();
            for (JsonElement element : items) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                if (!item.has("id") || !item.has("name")) continue;
                String id = item.get("id").getAsString().trim();
                String name = item.get("name").getAsString().trim();
                if (!id.isEmpty() && !name.isEmpty()) loadedItems.add(new ItemEntry(id, name));
            }
            loadedItems.sort(Comparator.comparing(ItemEntry::name, String.CASE_INSENSITIVE_ORDER));
            synchronized (ITEMS) {
                ITEMS.clear();
                ITEMS.addAll(loadedItems);
            }
            loaded = true;
            System.out.println("[SkyJew] Loaded " + loadedItems.size() + " Hypixel SkyBlock items for /sj recipe autocomplete.");
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to load Hypixel item list: " + e.getMessage());
        }
    }

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("recipe")
            .then(argument("item", StringArgumentType.greedyString())
                .suggests(SkyJewRecipeCommand::suggestItems)
                .executes(context -> run(context)));
    }

    private static CompletableFuture<Suggestions> suggestItems(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase(Locale.ROOT);
        synchronized (ITEMS) {
            for (ItemEntry item : ITEMS) {
                if (item.name().toLowerCase(Locale.ROOT).contains(input) || item.id().toLowerCase(Locale.ROOT).contains(input)) {
                    builder.suggest(item.id(), Component.literal(item.name()));
                }
            }
        }
        return builder.buildFuture();
    }

    private static int run(CommandContext<FabricClientCommandSource> context) {
        String raw = StringArgumentType.getString(context, "item").trim();
        int amount = 1;
        String input = raw;
        String last = raw.substring(raw.lastIndexOf(' ') + 1);
        try {
            if (raw.contains(" ") && Integer.parseInt(last) > 0) {
                amount = Integer.parseInt(last);
                input = raw.substring(0, raw.lastIndexOf(' ')).trim();
            }
        } catch (NumberFormatException ignored) {}
        String id = resolveId(input);
        Minecraft mc = Minecraft.getInstance();
        if (id == null || mc.player == null) {
            if (mc.player != null) mc.player.sendSystemMessage(Component.literal("§c[SkyJew] Unknown SkyBlock item: §f" + input));
            return 0;
        }
        mc.player.connection.sendCommand("viewrecipe " + id);
        if (amount > 1) mc.player.sendSystemMessage(Component.literal("§6[SkyJew] Recipe amount: §f" + amount + "x §7" + displayName(id)));
        return 1;
    }

    private static String resolveId(String input) {
        String normalized = input.toLowerCase(Locale.ROOT).replace(' ', '_');
        synchronized (ITEMS) {
            for (ItemEntry item : ITEMS) if (item.id().equalsIgnoreCase(input)) return item.id();
            for (ItemEntry item : ITEMS) if (item.name().equalsIgnoreCase(input)) return item.id();
            for (ItemEntry item : ITEMS) if (item.id().equalsIgnoreCase(normalized)) return item.id();
        }
        return input.matches("[A-Za-z0-9_:.\\-]+") ? input.toUpperCase(Locale.ROOT) : null;
    }

    private static String displayName(String id) {
        synchronized (ITEMS) {
            for (ItemEntry item : ITEMS) if (item.id().equalsIgnoreCase(id)) return item.name();
        }
        return id;
    }
}

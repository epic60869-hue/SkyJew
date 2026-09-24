package com.epic60869.skyjew;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

/**
 * SkyOcean-style recipe command.
 *
 * Uses the public Hypixel item list for autocomplete and the public
 * NotEnoughUpdates repository for recipe layouts. The recipe is rendered
 * locally instead of sending /viewrecipe to Hypixel, avoiding server GUI
 * crashes and allowing the requested amount to be shown.
 */
public final class SkyJewRecipeCommand {
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(5)).build();
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
                .timeout(java.time.Duration.ofSeconds(10))
                .header("User-Agent", "SkyJew/1.0")
                .GET().build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray items = root.getAsJsonArray("items");
            if (items == null) return;

            List<ItemEntry> next = new ArrayList<>();
            for (JsonElement element : items) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                if (!item.has("id") || !item.has("name")) continue;
                String id = item.get("id").getAsString().trim();
                String name = item.get("name").getAsString().trim();
                if (!id.isEmpty() && !name.isEmpty()) next.add(new ItemEntry(id, name));
            }
            next.sort(Comparator.comparing(ItemEntry::name, String.CASE_INSENSITIVE_ORDER));
            synchronized (ITEMS) {
                ITEMS.clear();
                ITEMS.addAll(next);
            }
            loaded = true;
            System.out.println("[SkyJew] Loaded " + next.size() + " Hypixel items for recipe autocomplete.");
        } catch (Exception e) {
            System.err.println("[SkyJew] Recipe item list load failed: " + e.getMessage());
        }
    }

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("recipe")
            .then(argument("item", StringArgumentType.greedyString())
                .suggests(SkyJewRecipeCommand::suggestItems)
                .executes(SkyJewRecipeCommand::run));
    }

    private static CompletableFuture<Suggestions> suggestItems(
        CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase(Locale.ROOT);
        synchronized (ITEMS) {
            for (ItemEntry item : ITEMS) {
                if (item.name().toLowerCase(Locale.ROOT).contains(input)
                    || item.id().toLowerCase(Locale.ROOT).contains(input)) {
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

        int split = raw.lastIndexOf(' ');
        if (split > 0) {
            try {
                int parsed = Integer.parseInt(raw.substring(split + 1));
                if (parsed > 0) {
                    amount = parsed;
                    input = raw.substring(0, split).trim();
                }
            } catch (NumberFormatException ignored) {}
        }

        String id = resolveId(input);
        Minecraft mc = Minecraft.getInstance();
        if (id == null || mc.player == null) {
            if (mc.player != null) {
                mc.gui.hud.getChat().addClientSystemMessage(Component.literal(
                    "§c[SkyJew] Unknown SkyBlock item: §f" + input));
            }
            return 0;
        }

        final int finalAmount = amount;
        final String finalId = id;
        CompletableFuture.supplyAsync(() -> loadRecipe(finalId))
            .thenAccept(recipe -> mc.execute(() -> {
                if (recipe == null || recipe.isEmpty()) {
                    mc.gui.hud.getChat().addClientSystemMessage(Component.literal(
                        "§c[SkyJew] No recipe data found for §f" + displayName(finalId)
                            + "§c. The item may not be craftable."));
                    return;
                }
                mc.gui.setScreen(new SkyJewRecipeScreen(mc.gui.screen(), finalId, displayName(finalId),
                    finalAmount, recipe));
            }));
        return 1;
    }

    private static RecipeData loadRecipe(String id) {
        try {
            String url = "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/items/"
                + java.net.URLEncoder.encode(id.toUpperCase(Locale.ROOT), java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20") + ".json";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(8)).header("User-Agent", "SkyJew/1.0").GET().build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject recipe = root.getAsJsonObject("recipe");
            if (recipe == null) {
                JsonArray recipes = root.getAsJsonArray("recipes");
                if (recipes != null && !recipes.isEmpty() && recipes.get(0).isJsonObject()) {
                    recipe = recipes.get(0).getAsJsonObject();
                }
            }
            if (recipe == null) return null;

            List<Ingredient> ingredients = new ArrayList<>();
            for (String row : new String[]{"A","B","C"}) {
                for (String col : new String[]{"1","2","3"}) {
                    String key = row + col;
                    if (!recipe.has(key)) continue;
                    String value = recipe.get(key).getAsString().trim();
                    ingredients.add(parseIngredient(key, value));
                }
            }
            return new RecipeData(ingredients);
        } catch (Exception e) {
            System.err.println("[SkyJew] Recipe load failed for " + id + ": " + e.getMessage());
            return null;
        }
    }

    private static Ingredient parseIngredient(String slot, String value) {
        if (value.isBlank()) return new Ingredient(slot, "", 0);
        int split = value.lastIndexOf(':');
        if (split > 0) {
            try {
                return new Ingredient(slot, value.substring(0, split), Integer.parseInt(value.substring(split + 1)));
            } catch (NumberFormatException ignored) {}
        }
        return new Ingredient(slot, value, 1);
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

    public static String displayName(String id) {
        synchronized (ITEMS) {
            for (ItemEntry item : ITEMS) if (item.id().equalsIgnoreCase(id)) return item.name();
        }
        return id.replace('_', ' ');
    }

    public record Ingredient(String slot, String id, int amount) {}
    public record RecipeData(List<Ingredient> ingredients) {
        public boolean isEmpty() {
            return ingredients == null || ingredients.stream().noneMatch(i -> i.amount() > 0);
        }
    }
}

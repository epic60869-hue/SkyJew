package com.epic60869.skyjew.features.misc;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewPriceTooltip;
import com.epic60869.skyjew.custom.RepoItems;
import com.epic60869.skyjew.custom.util.Compat;
import com.epic60869.skyjew.features.core.SkyJewChat;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collection tracker, like SkyHanni's farming weight display but for every collection: whatever you're gathering
 * (mining, farming, foraging, fishing, ...) shows your collection, what you've gained this session and your rank
 * on the Elite (elitebot.dev) collection leaderboard, with how much you need to pass the next player.
 *
 * What you're gathering is the collection item you picked up most recently, from your inventory or from a
 * "[Sacks]" message. The total comes from Elite's copy of the Hypixel API, plus what you've gathered since.
 *
 * The HUD shows two lines: "Collection: 12,345,678" (with the item's icon in front), and the next player above you
 * on the Elite leaderboard with how far ahead of you they are ("Tado 1,500").
 *
 * Shown SkyHanni style: the item's icon and "Cobblestone collection: 12,345,678 +1,234" (the green gain shows for a
 * few seconds after each pickup), then the session gain, and the Elite rank like SkyHanni's farming weight display.
 * /sj trackcollection &lt;item&gt; [goal] pins one collection (with an optional goal), like SkyHanni's /shtrackcollection;
 * /sj trackcollection on its own goes back to following what you gather, and /sj trackcollection stop hides it.
 *
 * Compacted items count too: an Enchanted Cobblestone is 160 Cobblestone, an Enchanted Hay Bale 25,600 Wheat. Each
 * enchanted item is resolved once from its NEU repo recipe (recursively) and cached in
 * config/skyjew/compacted-items.json. The inventory is counted as "base items" per collection, so a compactor turning
 * 160 Cobblestone into one Enchanted Cobblestone is neither a gain nor a loss.
 */
public final class CollectionTracker {
    private static final String API = "https://api.elitebot.dev";
    private static final Pattern SACK_LINE = Pattern.compile("^\\s*\\+([\\d,]+) (.+?) \\(.+\\)$");
    private static final long PROFILE_REFRESH_MS = 10 * 60_000L;
    private static final long RANK_REFRESH_MS = 5 * 60_000L;
    private static final long IDLE_HIDE_MS = 90_000L;

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    /** Elite leaderboard for one collection. */
    private record Board(String id, String itemId, String title) {}

    /** Your rank on one leaderboard, as fetched. */
    private record Rank(int rank, long amount, long minAmount, int upcomingRank, List<Upcoming> upcoming, long fetchedAt) {}

    private record Upcoming(String name, long amount) {}

    /** A compacted item: how many of which collection item it is made from. */
    private record Compact(String base, long amount) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Compact> COMPACT = new ConcurrentHashMap<>();       // item id -> base and amount
    private static final Set<String> NOT_COMPACT = ConcurrentHashMap.newKeySet();         // resolved, not a collection item
    private static final Set<String> RESOLVING = ConcurrentHashMap.newKeySet();
    private static Path compactFile;

    private static final Map<String, Board> BOARDS = new HashMap<>();      // Hypixel item id -> board
    private static final Map<String, String> NAMES = new HashMap<>();      // lower-case item name -> item id
    private static final Map<String, Long> apiAmounts = new HashMap<>();   // item id -> collection from Elite
    private static final Map<String, Long> sinceFetch = new HashMap<>();   // item id -> gathered since that amount
    private static final Map<String, Long> session = new HashMap<>();      // item id -> gathered this session
    private static final Map<String, Long> sessionStart = new HashMap<>(); // item id -> first gain this session
    private static final Map<String, Rank> ranks = new HashMap<>();
    private static final Map<String, Boolean> rankLoading = new HashMap<>();

    /** Collection items in your inventory last tick, or null when there's nothing to compare against. */
    private static Map<String, Long> lastInventory;
    private static boolean boardsLoading;
    private static long boardsRetryAt;
    private static boolean profileLoading;
    private static long profileFetchedAt;
    private static String profileId = "";
    private static String current = "";
    private static long lastGain;
    private static String status = "";
    /** Gain shown as the green "+N" after the total, reset a few seconds after the last pickup. */
    private static long recentGain;
    private static long recentGainAt;
    private static final long RECENT_GAIN_MS = 3_000L;
    private static final Map<String, ItemStack> ICONS = new HashMap<>();

    private CollectionTracker() {}

    private static SkyJewConfig.Misc config() {
        SkyJewConfig c = SkyJewConfig.current();
        return c == null ? null : c.misc;
    }

    private static boolean enabled() {
        SkyJewConfig.Misc c = config();
        return c != null && c.collectionTracker && SkyJewLocation.onSkyblock();
    }

    public static void init(Path configDir) {
        compactFile = configDir.resolve("skyjew").resolve("compacted-items.json");
        loadCompactCache();
        SkyJewHuds.registerCustom("collection_tracker", "Collection Tracker", CollectionTracker::enabled, new Hud(), 8, 150);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("trackcollection")
                    .executes(c -> track(""))
                    .then(ClientCommands.argument("item", StringArgumentType.greedyString())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(suggestions(), b))
                        .executes(c -> track(StringArgumentType.getString(c, "item"))))));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick());
        SkyJewChat.onGameMessage((component, overlay) -> {
            if (!overlay) onSacksMessage(component);
        });
        SkyJewLocation.onAreaChange(area -> lastInventory = null);
    }

    // ---------------------------------------------------------------- what you're gathering

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled() || mc.player == null) {
            lastInventory = null;
            return;
        }
        loadBoards();
        // Only count pickups while no menu is open, so moving items out of chests doesn't count.
        if (mc.gui.screen() != null) {
            lastInventory = null;
            return;
        }
        // Collection items in the inventory, with compacted items counted as the base items they're made of.
        Map<String, Long> now = new HashMap<>();
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            String id = Compat.neuName(stack);
            String collection = collectionOf(id);
            if (collection != null) {
                now.merge(collection, (long) stack.getCount(), Long::sum);
                continue;
            }
            Compact compact = COMPACT.get(id);
            if (compact != null) now.merge(compact.base(), compact.amount() * stack.getCount(), Long::sum);
            else if (isCompactCandidate(id)) resolveLater(id);
        }
        if (lastInventory != null) {
            for (Map.Entry<String, Long> e : now.entrySet()) {
                long gained = e.getValue() - lastInventory.getOrDefault(e.getKey(), 0L);
                if (gained > 0) gain(e.getKey(), gained);
            }
        }
        lastInventory = BOARDS.isEmpty() ? null : now;
        if (!pinned().isEmpty()) current = pinned();
        if (!current.isEmpty()) refresh(current);
    }

    /** "[Sacks] +1,234 items." — the hover lists each item that went into your sacks. */
    private static void onSacksMessage(Component component) {
        if (!enabled() || !component.getString().contains("[Sacks]")) return;
        for (Component part : flatten(component)) {
            if (!(part.getStyle().getHoverEvent() instanceof HoverEvent.ShowText(Component hover))) continue;
            for (String line : ChatFormatting.stripFormatting(hover.getString()).split("\n")) {
                Matcher m = SACK_LINE.matcher(line);
                if (!m.matches()) continue;
                String id = NAMES.get(m.group(2).trim().toLowerCase(Locale.ROOT));
                if (id == null) continue;
                long amount = Long.parseLong(m.group(1).replace(",", ""));
                Compact compact = COMPACT.get(id);
                if (compact != null) gain(compact.base(), amount * compact.amount());
                else if (collectionOf(id) != null) gain(collectionOf(id), amount);
            }
        }
    }

    private static List<Component> flatten(Component component) {
        List<Component> out = new ArrayList<>();
        out.add(component);
        for (Component sibling : component.getSiblings()) out.addAll(flatten(sibling));
        return out;
    }

    private static void gain(String id, long amount) {
        long now = System.currentTimeMillis();
        String pinned = pinned();
        if (!id.equals(current) && !current.isEmpty() && pinned.isEmpty()) status = "";
        if (pinned.isEmpty() || pinned.equals(id)) {
            if (!id.equals(current) || now - recentGainAt > RECENT_GAIN_MS) recentGain = 0;
            current = id;
            lastGain = now;
            recentGain += amount;
            recentGainAt = now;
        }
        sinceFetch.merge(id, amount, Long::sum);
        session.merge(id, amount, Long::sum);
        sessionStart.putIfAbsent(id, now);
        checkGoal(id);
    }

    // ---------------------------------------------------------------- /sj trackcollection

    private static String pinned() {
        SkyJewConfig.Misc c = config();
        return c == null || c.collectionTrackerItem == null ? "" : c.collectionTrackerItem;
    }

    private static long goal() {
        SkyJewConfig.Misc c = config();
        return c == null ? 0 : c.collectionTrackerGoal;
    }

    private static List<String> suggestions() {
        List<String> out = new ArrayList<>(List.of("stop"));
        for (Board board : BOARDS.values()) out.add(shortName(board).replace(' ', '_'));
        return out;
    }

    private static String shortName(Board board) {
        return board.title().endsWith(" Collection") ? board.title().substring(0, board.title().length() - " Collection".length()) : board.title();
    }

    private static int track(String input) {
        SkyJewConfig c = SkyJewConfig.current();
        if (c == null) return 0;
        String text = input.trim();
        if (text.equalsIgnoreCase("stop")) {
            c.misc.collectionTrackerItem = "";
            c.misc.collectionTrackerGoal = 0;
            current = "";
            SkyJewConfig.saveCurrent(c);
            return say(Component.literal("Stopped the collection tracker.").withStyle(ChatFormatting.YELLOW));
        }
        if (text.isEmpty()) {
            c.misc.collectionTrackerItem = "";
            c.misc.collectionTrackerGoal = 0;
            SkyJewConfig.saveCurrent(c);
            return say(Component.literal("The collection tracker follows whatever you gather again.").withStyle(ChatFormatting.YELLOW));
        }
        long goalAmount = 0;
        String[] words = text.split("\\s+");
        String last = words[words.length - 1].replace(",", "").toLowerCase(Locale.ROOT);
        if (words.length > 1 && last.matches("\\d+(\\.\\d+)?[km]?")) {
            double n = Double.parseDouble(last.replaceAll("[km]", ""));
            goalAmount = (long) (n * (last.endsWith("m") ? 1_000_000 : last.endsWith("k") ? 1_000 : 1));
            text = text.substring(0, text.length() - words[words.length - 1].length()).trim();
        }
        if (BOARDS.isEmpty()) return say(Component.literal("Collections are still loading, try again in a moment.").withStyle(ChatFormatting.RED));
        Board board = findBoard(text);
        if (board == null) return say(Component.literal("No collection called \"" + text + "\".").withStyle(ChatFormatting.RED));
        c.misc.collectionTrackerItem = board.itemId();
        c.misc.collectionTrackerGoal = goalAmount;
        SkyJewConfig.saveCurrent(c);
        current = board.itemId();
        lastGain = System.currentTimeMillis();
        MutableComponent msg = Component.literal("Tracking your ").withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(shortName(board)).withStyle(ChatFormatting.GOLD))
            .append(Component.literal(" collection").withStyle(ChatFormatting.YELLOW));
        if (goalAmount > 0) msg.append(Component.literal(" (goal " + fmt(goalAmount) + ")").withStyle(ChatFormatting.AQUA));
        return say(msg.append(Component.literal(".").withStyle(ChatFormatting.YELLOW)));
    }

    /** Matches "cobblestone", "Sugar_Cane", "wart", "lapis", ... to a collection, like SkyHanni's typo fixes. */
    private static Board findBoard(String input) {
        String name = input.toLowerCase(Locale.ROOT).replace('_', ' ').trim();
        name = switch (name) {
            case "carrots" -> "carrot";
            case "melons" -> "melon";
            case "seed" -> "seeds";
            case "iron" -> "iron ingot";
            case "gold" -> "gold ingot";
            case "sugar", "cane" -> "sugar cane";
            case "cocoa", "cocoa beans" -> "cocoa bean";
            case "lapis" -> "lapis lazuli";
            case "cacti" -> "cactus";
            case "pumpkins" -> "pumpkin";
            case "potatoes" -> "potato";
            case "wart", "warts", "nether warts" -> "nether wart";
            case "stone", "cobble" -> "cobblestone";
            case "mushrooms", "red mushroom", "brown mushroom" -> "mushroom";
            case "gemstones", "gems" -> "gemstone";
            case "quartz" -> "nether quartz";
            case "glowstone dust" -> "glowstone";
            case "endstone" -> "end stone";
            case "hardstone" -> "hard stone";
            default -> name;
        };
        for (Board board : BOARDS.values()) if (shortName(board).equalsIgnoreCase(name)) return board;
        for (Board board : BOARDS.values()) if (shortName(board).toLowerCase(Locale.ROOT).startsWith(name)) return board;
        String id = NAMES.get(name);
        return id == null ? null : BOARDS.get(id);
    }

    private static void checkGoal(String id) {
        long goalAmount = goal();
        if (goalAmount <= 0 || !id.equals(pinned())) return;
        Long api = apiAmounts.get(id);
        if (api == null) return;
        long live = api + sinceFetch.getOrDefault(id, 0L);
        if (live < goalAmount) return;
        SkyJewConfig c = SkyJewConfig.current();
        c.misc.collectionTrackerGoal = 0;
        SkyJewConfig.saveCurrent(c);
        Board board = BOARDS.get(id);
        say(Component.literal("Collection goal of ").withStyle(ChatFormatting.GREEN)
            .append(Component.literal(fmt(goalAmount)).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" " + (board == null ? id : shortName(board)) + " reached!").withStyle(ChatFormatting.GREEN)));
    }

    private static int say(Component message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(
                Component.literal("[SJ] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(message));
        });
        return 1;
    }

    // ---------------------------------------------------------------- compacted items

    /**
     * The collection an item counts toward as-is, or null. Most collections are their own item; mushrooms and
     * gemstones share one collection (rough gemstones count, finer ones resolve to rough through their recipes).
     */
    private static String collectionOf(String id) {
        if (BOARDS.containsKey(id)) return id;
        if ((id.equals("RED_MUSHROOM") || id.equals("BROWN_MUSHROOM")) && BOARDS.containsKey("MUSHROOM_COLLECTION")) return "MUSHROOM_COLLECTION";
        if (id.startsWith("ROUGH_") && id.endsWith("_GEM") && BOARDS.containsKey("GEMSTONE_COLLECTION")) return "GEMSTONE_COLLECTION";
        return null;
    }

    private static boolean isCompactCandidate(String id) {
        return !id.isEmpty() && !BOARDS.isEmpty() && !NOT_COMPACT.contains(id)
            && (id.startsWith("ENCHANTED_") || id.equals("HAY_BLOCK") || (id.startsWith("FLAWED_") && id.endsWith("_GEM")));
    }

    /** Works out what an enchanted item is made of, off the render thread. */
    private static void resolveLater(String id) {
        if (!RESOLVING.add(id)) return;
        RepoItems.runAsync(() -> {
            try {
                Compact compact = resolve(id, new HashSet<>(), 0);
                if (compact != null) {
                    COMPACT.put(id, compact);
                    String name = RepoItems.displayName(id);
                    if (name != null) {
                        String key = ChatFormatting.stripFormatting(name).trim().toLowerCase(Locale.ROOT);
                        Minecraft.getInstance().execute(() -> NAMES.put(key, id));
                    }
                } else {
                    NOT_COMPACT.add(id);
                }
                saveCompactCache();
                // Don't count the enchanted items already in the inventory as a gain.
                Minecraft.getInstance().execute(() -> lastInventory = null);
            } catch (Exception e) {
                System.err.println("[SkyJew] Could not resolve " + id + ": " + e.getMessage());
            } finally {
                RESOLVING.remove(id);
            }
        });
    }

    /** The collection item {@code id} is made of and how many, following its NEU recipes; null if it isn't one. */
    private static Compact resolve(String id, Set<String> visiting, int depth) throws Exception {
        String collection = collectionOf(id);
        if (collection != null) return new Compact(collection, 1);
        Compact known = COMPACT.get(id);
        if (known != null) return known;
        if (NOT_COMPACT.contains(id) || depth > 4 || !visiting.add(id)) return null;
        try {
            JsonObject item = JsonParser.parseString(RepoItems.neuRepoFile("items/" + id.replace(':', '-') + ".json")).getAsJsonObject();
            List<JsonObject> recipes = new ArrayList<>();
            if (item.has("recipe") && item.get("recipe").isJsonObject()) recipes.add(item.getAsJsonObject("recipe"));
            if (item.has("recipes") && item.get("recipes").isJsonArray()) {
                for (JsonElement r : item.getAsJsonArray("recipes")) {
                    JsonObject o = r.getAsJsonObject();
                    if (!o.has("type") || "crafting".equals(str(o, "type"))) recipes.add(o);
                }
            }
            for (JsonObject recipe : recipes) {
                Compact compact = resolveRecipe(recipe, visiting, depth);
                if (compact != null) return compact;
            }
            return null;
        } finally {
            visiting.remove(id);
        }
    }

    /** A recipe counts only if every ingredient comes down to the same collection item. */
    private static Compact resolveRecipe(JsonObject recipe, Set<String> visiting, int depth) throws Exception {
        String base = null;
        long total = 0;
        for (String row : new String[]{"A", "B", "C"}) {
            for (int col = 1; col <= 3; col++) {
                String slot = str(recipe, row + col);
                if (slot.isEmpty()) continue;
                int colon = slot.lastIndexOf(':');
                String ingredient = (colon > 0 ? slot.substring(0, colon) : slot).replace('-', ':');
                long count = colon > 0 ? Long.parseLong(slot.substring(colon + 1)) : 1;
                Compact part = resolve(ingredient, visiting, depth + 1);
                if (part == null || (base != null && !base.equals(part.base()))) return null;
                base = part.base();
                total += count * part.amount();
            }
        }
        if (base == null) return null;
        int made = recipe.has("count") ? Math.max(1, recipe.get("count").getAsInt()) : 1;
        if (total % made != 0) return null;
        return new Compact(base, total / made);
    }

    private static void loadCompactCache() {
        try {
            if (!Files.exists(compactFile)) return;
            JsonObject root = JsonParser.parseString(Files.readString(compactFile, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("compacted")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("compacted").entrySet()) {
                    JsonObject o = e.getValue().getAsJsonObject();
                    COMPACT.put(e.getKey(), new Compact(o.get("base").getAsString(), o.get("amount").getAsLong()));
                }
            }
            if (root.has("notCompacted")) {
                for (JsonElement e : root.getAsJsonArray("notCompacted")) NOT_COMPACT.add(e.getAsString());
            }
        } catch (Exception e) {
            System.err.println("[SkyJew] Could not read compacted-items.json: " + e.getMessage());
        }
    }

    private static synchronized void saveCompactCache() {
        try {
            JsonObject root = new JsonObject();
            JsonObject compacted = new JsonObject();
            new TreeMap<>(COMPACT).forEach((id, c) -> {
                JsonObject o = new JsonObject();
                o.addProperty("base", c.base());
                o.addProperty("amount", c.amount());
                compacted.add(id, o);
            });
            root.add("compacted", compacted);
            JsonArray not = new JsonArray();
            new TreeSet<>(NOT_COMPACT).forEach(not::add);
            root.add("notCompacted", not);
            Files.createDirectories(compactFile.getParent());
            Files.writeString(compactFile, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyJew] Could not save compacted-items.json: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- Elite API

    private static void loadBoards() {
        if (!BOARDS.isEmpty() || boardsLoading || System.currentTimeMillis() < boardsRetryAt) return;
        boardsLoading = true;
        get("/leaderboards").thenAccept(json -> {
            JsonObject boards = json.getAsJsonObject().getAsJsonObject("leaderboards");
            Map<String, Board> found = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : boards.entrySet()) {
                JsonObject b = e.getValue().getAsJsonObject();
                if (!"Current".equals(str(b, "intervalType")) || !b.has("itemId")) continue;
                String title = str(b, "title");
                if (!title.endsWith(" Collection") || title.endsWith("Milestone Collection")) continue;
                found.put(str(b, "itemId"), new Board(e.getKey(), str(b, "itemId"), title));
            }
            Minecraft.getInstance().execute(() -> {
                BOARDS.putAll(found);
                for (Board board : found.values()) {
                    NAMES.put(board.title().substring(0, board.title().length() - " Collection".length()).toLowerCase(Locale.ROOT), board.itemId());
                    String name = RepoItems.displayName(board.itemId());
                    if (name != null) NAMES.put(ChatFormatting.stripFormatting(name).trim().toLowerCase(Locale.ROOT), board.itemId());
                }
                for (String id : List.of("RED_MUSHROOM", "BROWN_MUSHROOM")) {
                    String name = RepoItems.displayName(id);
                    if (name != null) NAMES.put(ChatFormatting.stripFormatting(name).trim().toLowerCase(Locale.ROOT), id);
                }
                for (String id : COMPACT.keySet()) {
                    String name = RepoItems.displayName(id);
                    if (name != null) NAMES.put(ChatFormatting.stripFormatting(name).trim().toLowerCase(Locale.ROOT), id);
                }
            });
        }).exceptionally(e -> {
            boardsRetryAt = System.currentTimeMillis() + 60_000;
            boardsLoading = false;
            return null;
        });
    }

    private static void refresh(String id) {
        long now = System.currentTimeMillis();
        String uuid = playerUuid();
        if (!profileLoading && now - profileFetchedAt > PROFILE_REFRESH_MS) {
            profileLoading = true;
            profileFetchedAt = now;
            get("/profile/" + uuid + "/selected").thenAccept(json -> {
                JsonObject profile = json.getAsJsonObject();
                String pid = str(profile, "profileId");
                Map<String, Long> amounts = new HashMap<>();
                if (profile.has("collections") && profile.get("collections").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> e : profile.getAsJsonObject("collections").entrySet()) {
                        amounts.put(e.getKey(), e.getValue().getAsLong());
                    }
                }
                Minecraft.getInstance().execute(() -> {
                    if (!pid.equals(profileId)) ranks.clear();
                    profileId = pid;
                    for (Map.Entry<String, Long> e : amounts.entrySet()) {
                        Long old = apiAmounts.put(e.getKey(), e.getValue());
                        // Hypixel's number moved, so it now includes what we had counted ourselves.
                        if (old == null || !old.equals(e.getValue())) sinceFetch.remove(e.getKey());
                    }
                    status = "";
                    profileLoading = false;
                });
            }).exceptionally(e -> {
                Minecraft.getInstance().execute(() -> {
                    status = "Elite has no data for you yet";
                    profileLoading = false;
                });
                return null;
            });
        }
        SkyJewConfig.Misc c = config();
        Board board = BOARDS.get(id);
        if (c == null || !c.collectionTrackerRank || board == null || profileId.isEmpty()) return;
        Rank rank = ranks.get(id);
        if (Boolean.TRUE.equals(rankLoading.get(id)) || (rank != null && now - rank.fetchedAt() < RANK_REFRESH_MS)) return;
        rankLoading.put(id, true);
        get("/leaderboard/rank/" + board.id() + "/" + uuid + "/" + profileId + "?includeUpcoming=true").thenAccept(json -> {
            JsonObject r = json.getAsJsonObject();
            List<Upcoming> upcoming = new ArrayList<>();
            if (r.has("upcomingPlayers") && r.get("upcomingPlayers").isJsonArray()) {
                JsonArray players = r.getAsJsonArray("upcomingPlayers");
                for (JsonElement p : players) {
                    JsonObject o = p.getAsJsonObject();
                    upcoming.add(new Upcoming(str(o, "ign"), o.has("amount") ? o.get("amount").getAsLong() : 0));
                }
            }
            upcoming.sort((a, b) -> Long.compare(a.amount(), b.amount()));
            Rank result = new Rank(num(r, "rank"), r.has("amount") ? r.get("amount").getAsLong() : 0,
                r.has("minAmount") ? r.get("minAmount").getAsLong() : 0, num(r, "upcomingRank"), upcoming, System.currentTimeMillis());
            Minecraft.getInstance().execute(() -> {
                ranks.put(id, result);
                rankLoading.put(id, false);
            });
        }).exceptionally(e -> {
            Minecraft.getInstance().execute(() -> {
                ranks.put(id, new Rank(-2, 0, 0, -1, List.of(), System.currentTimeMillis()));
                rankLoading.put(id, false);
            });
            return null;
        });
    }

    private static CompletableFuture<JsonElement> get(String path) {
        String version = FabricLoader.getInstance().getModContainer("skyjew")
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + path))
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", "SkyJew/" + version)
            .GET().build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode());
            return JsonParser.parseString(response.body());
        });
    }

    private static String playerUuid() {
        return Minecraft.getInstance().getUser().getProfileId().toString().replace("-", "");
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static int num(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : -1;
    }

    // ---------------------------------------------------------------- HUD

    private static String fmt(long n) {
        return String.format(Locale.US, "%,d", n);
    }

    private static boolean showing() {
        if (!enabled() || current.isEmpty()) return false;
        return !pinned().isEmpty() || System.currentTimeMillis() - lastGain < IDLE_HIDE_MS;
    }

    /** The collection item's icon (mushrooms and gemstones use a red mushroom and a rough ruby). */
    private static ItemStack icon(String id) {
        ItemStack cached = ICONS.get(id);
        if (cached != null) return cached;
        String neuId = switch (id) {
            case "MUSHROOM_COLLECTION" -> "RED_MUSHROOM";
            case "GEMSTONE_COLLECTION" -> "ROUGH_RUBY_GEM";
            default -> id.replace(':', '-');
        };
        ItemStack stack = RepoItems.itemStack(neuId);
        if (RepoItems.itemsLoaded()) ICONS.put(id, stack);
        return stack;
    }

    /**
     * "Collection: 12,345,678", then the next player above you on the Elite leaderboard and how far ahead they are
     * ("Tado 1,500"), counting the players you've passed since the rank was fetched.
     */
    private static List<Component> lines(String id, long live, boolean known) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Collection: ").withStyle(ChatFormatting.GRAY)
            .append(known ? Component.literal(fmt(live)).withStyle(ChatFormatting.YELLOW)
                : Component.literal("loading...").withStyle(ChatFormatting.DARK_GRAY)));

        SkyJewConfig.Misc c = config();
        if (c != null && c.collectionTrackerRank && known) {
            Rank rank = ranks.get(id);
            if (rank == null) {
                lines.add(Component.literal("loading...").withStyle(ChatFormatting.DARK_GRAY));
            } else if (rank.rank() != -2) {
                Upcoming next = null;
                for (Upcoming u : rank.upcoming()) {
                    if (u.amount() >= live) {
                        next = u;
                        break;
                    }
                }
                if (next != null) {
                    lines.add(Component.literal(next.name() + " ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(fmt(next.amount() - live)).withStyle(ChatFormatting.YELLOW)));
                } else if (rank.rank() > 0 && rank.rank() - rank.upcoming().size() <= 1) {
                    // Passed everyone above you (or already first).
                    lines.add(Component.literal("You're #1!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                }
            }
        }
        if (!status.isEmpty()) lines.add(Component.literal(status).withStyle(ChatFormatting.RED));
        return lines;
    }

    private static List<Component> liveLines() {
        Long api = apiAmounts.get(current);
        long live = (api == null ? 0 : api) + sinceFetch.getOrDefault(current, 0L);
        // Once your profile has loaded, a collection Elite has no number for starts at 0.
        return lines(current, live, api != null || (!profileLoading && !profileId.isEmpty()));
    }

    private static final List<Component> PREVIEW = List.of(
        Component.literal("Collection: ").withStyle(ChatFormatting.GRAY).append(Component.literal("12,345,678").withStyle(ChatFormatting.YELLOW)),
        Component.literal("Player ").withStyle(ChatFormatting.AQUA).append(Component.literal("1,500").withStyle(ChatFormatting.YELLOW)));

    /** The two lines, with the item's icon in front of them. */
    private static final class Hud implements SkyJewHuds.CustomHud {
        private static final int ICON = 16;

        private List<Component> shown(boolean preview) {
            if (showing()) return liveLines();
            return preview ? PREVIEW : List.of();
        }

        private String shownId() {
            return showing() ? current : "COBBLESTONE";
        }

        @Override
        public int width() {
            var font = Minecraft.getInstance().font;
            int w = 0;
            for (Component line : shown(true)) w = Math.max(w, font.width(line));
            return w + ICON + 3 + SkyJewHuds.PADDING * 2;
        }

        @Override
        public int height() {
            int n = shown(true).size();
            return SkyJewHuds.PADDING * 2 + Math.max(ICON, n * SkyJewHuds.LINE_HEIGHT - 2);
        }

        @Override
        public boolean visible() {
            return showing();
        }

        @Override
        public void render(GuiGraphicsExtractor g, boolean preview) {
            List<Component> lines = shown(preview);
            if (lines.isEmpty()) return;
            var font = Minecraft.getInstance().font;
            if (SkyJewHuds.placement("collection_tracker").background) g.fill(0, 0, width(), height(), 0x80000000);
            int pad = SkyJewHuds.PADDING;
            int textH = lines.size() * SkyJewHuds.LINE_HEIGHT - 2;
            g.item(icon(shownId()), pad, pad + Math.max(0, (textH - ICON) / 2));
            int y = pad + Math.max(0, (ICON - textH) / 2);
            for (Component line : lines) {
                g.text(font, line, pad + ICON + 3, y, 0xFFFFFFFF, true);
                y += SkyJewHuds.LINE_HEIGHT;
            }
        }
    }
}

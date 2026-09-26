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
 * The HUD is a SkyHanni-style tracker: "Collection Tracker" with the collection you're gathering (and its Elite rank),
 * one row per collection gathered (icon, amount, name, coin value), the total value, and, while an inventory is open,
 * the Display Mode switch (Total / This Session), Reset session, and rows you can click to hide (Ctrl+Click removes).
 * Totals are saved in config/skyjew/collection-tracker.json.
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

    /** One tracked collection: how much was gathered, whether its row is hidden, and when it last went up. */
    private static final class Row {
        long amount;
        boolean hidden;
        transient long lastGain;
    }

    private static final Map<String, Row> TOTAL = new java.util.LinkedHashMap<>();
    private static final Map<String, Row> SESSION = new java.util.LinkedHashMap<>();
    private static boolean showSession;
    private static Path trackerFile;
    private static long lastSave;

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
        trackerFile = configDir.resolve("skyjew").resolve("collection-tracker.json");
        loadCompactCache();
        loadTracker();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> saveTracker(true));
        // While an inventory is open the tracker is drawn over it, with the clickable SkyHanni-style controls.
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)) return;
            net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> renderInInventory(g, mouseX, mouseY));
            net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !clickInInventory(event.x(), event.y()));
        });
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
        for (Map<String, Row> rows : List.of(TOTAL, SESSION)) {
            Row row = rows.computeIfAbsent(id, k -> new Row());
            row.amount += amount;
            row.lastGain = now;
        }
        if (now - lastSave > 30_000) saveTracker();
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
                Component.literal("[SkyJew] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(message));
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

    /** SkyHanni-style lines: the collection line (drawn after the icon), then session and Elite rank. */
    private static List<Component> lines(String id, long live, boolean known, long gained, long elapsed, long recent) {
        Board board = BOARDS.get(id);
        String name = board != null ? shortName(board) : id;
        List<Component> lines = new ArrayList<>();

        MutableComponent first = Component.literal(name).withStyle(ChatFormatting.WHITE)
            .append(Component.literal(" collection: ").withStyle(ChatFormatting.GRAY));
        first.append(known ? Component.literal(fmt(live)).withStyle(ChatFormatting.YELLOW)
            : Component.literal("loading...").withStyle(ChatFormatting.DARK_GRAY));
        long goalAmount = id.equals(pinned()) ? goal() : 0;
        if (goalAmount > 0 && known) {
            double pct = Math.min(100.0, live * 100.0 / goalAmount);
            first.append(Component.literal(" / ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(fmt(goalAmount)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" (").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.format(Locale.US, "%.1f%%", pct)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(")").withStyle(ChatFormatting.WHITE));
        }
        if (recent > 0) first.append(Component.literal(" +" + fmt(recent)).withStyle(ChatFormatting.GREEN));
        lines.add(first);

        MutableComponent sessionLine = Component.literal("Session: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal("+" + fmt(gained)).withStyle(ChatFormatting.GREEN));
        if (elapsed > 30_000 && gained > 0) {
            sessionLine.append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(fmt(gained * 3_600_000L / elapsed) + "/h").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
        }
        lines.add(sessionLine);

        SkyJewConfig.Misc c = config();
        if (c != null && c.collectionTrackerRank) lines.addAll(rankLines(id, live));
        if (!status.isEmpty()) lines.add(Component.literal(status).withStyle(ChatFormatting.RED));
        return lines;
    }

    /** Like SkyHanni's farming weight display: "Elite Rank: #1,234" and "1,500 until #1,233 (Name)". */
    private static List<Component> rankLines(String id, long live) {
        Rank rank = ranks.get(id);
        MutableComponent head = Component.literal("Elite Rank: ").withStyle(ChatFormatting.GOLD);
        if (rank == null) return List.of(head.append(Component.literal("loading...").withStyle(ChatFormatting.DARK_GRAY)));
        if (rank.rank() == -2) return List.of(head.append(Component.literal("unavailable").withStyle(ChatFormatting.DARK_GRAY)));
        // Count the players above you that you've passed since the rank was fetched.
        int passed = 0;
        Upcoming next = null;
        for (Upcoming u : rank.upcoming()) {
            if (u.amount() < live) passed++;
            else if (next == null) next = u;
        }
        boolean ranked = rank.rank() > 0 || passed > 0;
        if (!ranked && live < rank.minAmount()) {
            return List.of(head.append(Component.literal("Unranked").withStyle(ChatFormatting.GRAY)),
                Component.literal(fmt(rank.minAmount() - live)).withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" until ranked").withStyle(ChatFormatting.GRAY)));
        }
        int base = rank.rank() > 0 ? rank.rank() : rank.upcomingRank() + 1;
        int liveRank = Math.max(1, base - passed);
        List<Component> lines = new ArrayList<>();
        lines.add(head.append(Component.literal("#" + fmt(liveRank)).withStyle(ChatFormatting.YELLOW)));
        if (liveRank == 1) {
            lines.add(Component.literal("You're #1!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else if (next != null) {
            lines.add(Component.literal(fmt(next.amount() - live + 1)).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" until ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("#" + fmt(liveRank - 1)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(next.name()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY)));
        }
        return lines;
    }

    private static List<Component> liveLines() {
        long now = System.currentTimeMillis();
        Long api = apiAmounts.get(current);
        long live = (api == null ? 0 : api) + sinceFetch.getOrDefault(current, 0L);
        long gained = session.getOrDefault(current, 0L);
        long elapsed = now - sessionStart.getOrDefault(current, now);
        long recent = now - recentGainAt <= RECENT_GAIN_MS ? recentGain : 0;
        return lines(current, live, api != null || !profileLoading, gained, elapsed, recent);
    }

    // ---------------------------------------------------------------- tracker data

    private static void loadTracker() {
        try {
            if (!Files.exists(trackerFile)) return;
            JsonObject root = JsonParser.parseString(Files.readString(trackerFile, StandardCharsets.UTF_8)).getAsJsonObject();
            showSession = root.has("showSession") && root.get("showSession").getAsBoolean();
            if (root.has("total")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("total").entrySet()) {
                    JsonObject o = e.getValue().getAsJsonObject();
                    Row row = new Row();
                    row.amount = o.get("amount").getAsLong();
                    row.hidden = o.has("hidden") && o.get("hidden").getAsBoolean();
                    TOTAL.put(e.getKey(), row);
                }
            }
        } catch (Exception e) {
            System.err.println("[SkyJew] Could not read collection-tracker.json: " + e.getMessage());
        }
    }

    private static void saveTracker() {
        saveTracker(false);
    }

    private static void saveTracker(boolean now) {
        lastSave = System.currentTimeMillis();
        JsonObject root = new JsonObject();
        root.addProperty("showSession", showSession);
        JsonObject total = new JsonObject();
        TOTAL.forEach((id, row) -> {
            JsonObject o = new JsonObject();
            o.addProperty("amount", row.amount);
            if (row.hidden) o.addProperty("hidden", true);
            total.add(id, o);
        });
        root.add("total", total);
        String json = GSON.toJson(root);
        Runnable write = () -> {
            try {
                Files.createDirectories(trackerFile.getParent());
                Files.writeString(trackerFile, json, StandardCharsets.UTF_8);
            } catch (Exception e) {
                System.err.println("[SkyJew] Could not save collection-tracker.json: " + e.getMessage());
            }
        };
        if (now) write.run();
        else CompletableFuture.runAsync(write);
    }

    private static Map<String, Row> shownRows() {
        return showSession ? SESSION : TOTAL;
    }

    // ---------------------------------------------------------------- the SkyHanni-style panel

    /** One line of the panel: optional icon, text, and what clicking it does (while an inventory is open). */
    private record Line(ItemStack icon, Component text, Runnable onClick) {}

    private static final int ROW = 11;
    private static final float ICON_SCALE = 0.6875f; // 11px icons, like SkyHanni's tracker rows

    private static String coins(double value) {
        if (value >= 1_000_000_000) return String.format(Locale.US, "%.2fB", value / 1_000_000_000);
        if (value >= 1_000_000) return String.format(Locale.US, "%.2fM", value / 1_000_000);
        if (value >= 1_000) return String.format(Locale.US, "%.1fk", value / 1_000);
        return String.format(Locale.US, "%,.0f", value);
    }

    private static String rowName(String id) {
        Board board = BOARDS.get(id);
        return board != null ? shortName(board) : id;
    }

    private static boolean panelVisible() {
        if (!enabled()) return false;
        return showing() || shownRows().values().stream().anyMatch(r -> !r.hidden && r.amount > 0);
    }

    private static List<Line> panel(boolean inventory) {
        List<Line> out = new ArrayList<>();
        out.add(new Line(null, Component.literal("Collection Tracker").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), null));

        // The collection you're gathering now, with its Elite rank.
        if (showing()) {
            List<Component> head = liveLines();
            out.add(new Line(icon(current), head.get(0), null));
            for (int i = 2; i < head.size(); i++) out.add(new Line(null, head.get(i), null));
        }

        // One row per collection, most valuable first.
        long now = System.currentTimeMillis();
        record Entry(String id, Row row, double value) {}
        List<Entry> entries = new ArrayList<>();
        double totalValue = 0;
        for (Map.Entry<String, Row> e : shownRows().entrySet()) {
            Row row = e.getValue();
            if (row.amount <= 0 || (row.hidden && !inventory)) continue;
            double value = row.amount * SkyJewPriceTooltip.unitPrice(e.getKey());
            entries.add(new Entry(e.getKey(), row, value));
            if (!row.hidden) totalValue += value;
        }
        entries.sort((a, b) -> Double.compare(b.value(), a.value()));
        for (Entry entry : entries) {
            Row row = entry.row();
            boolean recent = now - row.lastGain < 10_000;
            MutableComponent text = Component.literal(fmt(row.amount) + "x ")
                .withStyle(recent ? net.minecraft.network.chat.Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true) : net.minecraft.network.chat.Style.EMPTY.withColor(ChatFormatting.GRAY));
            text.append(row.hidden
                ? Component.literal(rowName(entry.id())).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH)
                : Component.literal(rowName(entry.id())).withStyle(ChatFormatting.WHITE));
            if (entry.value() > 0) {
                text.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(coins(entry.value())).withStyle(row.hidden ? ChatFormatting.DARK_GRAY : ChatFormatting.GOLD));
            }
            String id = entry.id();
            out.add(new Line(icon(id), text, () -> {
                if (Minecraft.getInstance().hasControlDown()) {
                    TOTAL.remove(id);
                    SESSION.remove(id);
                    say(Component.literal("Removed " + rowName(id) + " from the Collection Tracker.").withStyle(ChatFormatting.YELLOW));
                } else {
                    for (Map<String, Row> rows : List.of(TOTAL, SESSION)) {
                        Row r = rows.get(id);
                        if (r != null) r.hidden = !r.hidden;
                    }
                }
                saveTracker();
            }));
        }
        if (entries.isEmpty()) out.add(new Line(null, Component.literal("Gather something to start tracking.").withStyle(ChatFormatting.GRAY), null));
        else out.add(new Line(null, Component.literal("Total Profit: ").withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(coins(totalValue) + " coins").withStyle(ChatFormatting.GOLD)), null));

        if (inventory) {
            MutableComponent mode = Component.literal("Display Mode: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[Total]").withStyle(showSession ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW))
                .append(Component.literal(" "))
                .append(Component.literal("[This Session]").withStyle(showSession ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
            out.add(new Line(null, mode, () -> {
                showSession = !showSession;
                saveTracker();
            }));
            if (showSession) {
                out.add(new Line(null, Component.literal("Reset session!").withStyle(ChatFormatting.RED), () -> {
                    SESSION.clear();
                    session.clear();
                    sessionStart.clear();
                    say(Component.literal("Reset this session of the Collection Tracker!").withStyle(ChatFormatting.YELLOW));
                }));
            }
            if (!entries.isEmpty()) out.add(new Line(null, Component.literal("Click a row to hide it, Ctrl+Click to remove it.").withStyle(ChatFormatting.DARK_GRAY), null));
        }
        return out;
    }

    private static int panelWidth(List<Line> lines) {
        var font = Minecraft.getInstance().font;
        int w = 0;
        for (Line line : lines) w = Math.max(w, font.width(line.text()) + (line.icon() != null ? ROW + 2 : 0));
        return w + SkyJewHuds.PADDING * 2;
    }

    private static int panelHeight(List<Line> lines) {
        return SkyJewHuds.PADDING * 2 + lines.size() * ROW - 1;
    }

    private static void drawPanel(GuiGraphicsExtractor g, List<Line> lines, boolean background) {
        var font = Minecraft.getInstance().font;
        if (background) g.fill(0, 0, panelWidth(lines), panelHeight(lines), 0x80000000);
        int y = SkyJewHuds.PADDING;
        for (Line line : lines) {
            int x = SkyJewHuds.PADDING;
            if (line.icon() != null) {
                g.pose().pushMatrix();
                g.pose().translate(x, y - 1);
                g.pose().scale(ICON_SCALE, ICON_SCALE);
                g.item(line.icon(), 0, 0);
                g.pose().popMatrix();
                x += ROW + 2;
            }
            g.text(font, line.text(), x, y + 1, 0xFFFFFFFF, true);
            y += ROW;
        }
    }

    private static final List<Line> PREVIEW = List.of(
        new Line(null, Component.literal("Collection Tracker").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), null),
        new Line(null, Component.literal("Cobblestone").withStyle(ChatFormatting.WHITE)
            .append(Component.literal(" collection: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("12,345,678").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" +160").withStyle(ChatFormatting.GREEN)), null),
        new Line(null, Component.literal("Elite Rank: ").withStyle(ChatFormatting.GOLD).append(Component.literal("#1,234").withStyle(ChatFormatting.YELLOW)), null),
        new Line(null, Component.literal("4,321x ").withStyle(ChatFormatting.GRAY).append(Component.literal("Cobblestone").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(": ").withStyle(ChatFormatting.GRAY)).append(Component.literal("12.1k").withStyle(ChatFormatting.GOLD)), null),
        new Line(null, Component.literal("Total Profit: ").withStyle(ChatFormatting.YELLOW).append(Component.literal("12.1k coins").withStyle(ChatFormatting.GOLD)), null));

    /** The panel in the normal HUD (hidden while an inventory is open, where it is drawn over the inventory instead). */
    private static final class Hud implements SkyJewHuds.CustomHud {
        private List<Line> shown(boolean preview) {
            if (panelVisible()) return panel(false);
            return preview ? PREVIEW : List.of();
        }

        @Override
        public int width() {
            return panelWidth(shown(true));
        }

        @Override
        public int height() {
            return panelHeight(shown(true));
        }

        @Override
        public boolean visible() {
            return panelVisible() && !(Minecraft.getInstance().gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>);
        }

        @Override
        public void render(GuiGraphicsExtractor g, boolean preview) {
            if (!preview && !visible()) return;
            List<Line> lines = shown(preview);
            if (lines.isEmpty()) return;
            drawPanel(g, lines, SkyJewHuds.placement("collection_tracker").background);
        }
    }

    /** Top-left corner and scale of the panel on screen, matching where the HUD draws it. */
    private static float[] panelPosition(List<Line> lines) {
        SkyJewHuds.Placement p = SkyJewHuds.placement("collection_tracker");
        int x = SkyJewHuds.mapX(p.x, Math.round(panelWidth(lines) * p.scale));
        int y = SkyJewHuds.mapY(p.y, Math.round(panelHeight(lines) * p.scale));
        return new float[]{x, y, p.scale};
    }

    private static void renderInInventory(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!panelVisible()) return;
        List<Line> lines = panel(true);
        float[] pos = panelPosition(lines);
        g.pose().pushMatrix();
        g.pose().translate(pos[0], pos[1]);
        g.pose().scale(pos[2], pos[2]);
        // Highlight the clickable line under the mouse.
        int hovered = lineAt(lines, pos, mouseX, mouseY);
        if (hovered >= 0 && lines.get(hovered).onClick() != null) {
            int top = SkyJewHuds.PADDING + hovered * ROW - 1;
            g.fill(0, top, panelWidth(lines), top + ROW, 0x30FFFFFF);
        }
        drawPanel(g, lines, SkyJewHuds.placement("collection_tracker").background);
        g.pose().popMatrix();
    }

    private static int lineAt(List<Line> lines, float[] pos, double mouseX, double mouseY) {
        double lx = (mouseX - pos[0]) / pos[2];
        double ly = (mouseY - pos[1]) / pos[2] - SkyJewHuds.PADDING + 1;
        if (lx < 0 || lx > panelWidth(lines) || ly < 0) return -1;
        int index = (int) (ly / ROW);
        return index < lines.size() ? index : -1;
    }

    /** Runs the clicked line's action; true if the click was used. */
    private static boolean clickInInventory(double mouseX, double mouseY) {
        if (!panelVisible()) return false;
        List<Line> lines = panel(true);
        int index = lineAt(lines, panelPosition(lines), mouseX, mouseY);
        if (index < 0 || lines.get(index).onClick() == null) return false;
        lines.get(index).onClick().run();
        return true;
    }
}

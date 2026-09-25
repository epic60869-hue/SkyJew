package com.epic60869.skyjew;

import com.epic60869.skyjew.custom.RepoItems;
import com.epic60869.skyjew.custom.util.Compat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /sj recipe craft helper, ported from SkyOcean's CraftHelper (SkyOcean code is MIT licensed):
 * the selected item is expanded into a recipe tree (crafting and Forge recipes from the NEU repo,
 * with leftovers carried over between branches), and every node shows how many you have versus need,
 * counting your inventory and cached storage. Shown next to any inventory screen.
 */
public final class SkyJewCraftHelper {
    private static final int MAX_DEPTH = 10;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 5;
    private static final long REFRESH_MS = 500;

    // ----- Recipes from the NEU repo -----
    public record Input(String id, int amount) {}
    public record Recipe(String type, List<Input> inputs, int outputCount) {}
    private static final Map<String, Optional<Recipe>> RECIPES = new ConcurrentHashMap<>();

    // ----- Tree (SkyOcean's CraftHelperTree / CraftHelperRecipeNode / CraftHelperLeafNode) -----
    private static final class Node {
        final String id;
        final Recipe recipe;
        final int required;      // amount of this item needed after carry-over
        final int carriedOver;   // leftovers from earlier crafts used here
        final List<Node> children = new ArrayList<>();
        int totalChildren;

        Node(String id, Recipe recipe, int required, int carriedOver) {
            this.id = id;
            this.recipe = recipe;
            this.required = required;
            this.carriedOver = carriedOver;
        }
    }

    // ----- Evaluated state (SkyOcean's CraftHelperState) -----
    private record Row(String prefix, String id, int available, int needed, boolean done, boolean childrenDone,
                       int fromInventory, int fromStorage, int throughParents, int carryOver, String recipeType, boolean leaf) {}

    private static String selectedId;
    private static int selectedAmount = 1;
    private static Node tree;
    private static boolean building;
    private static List<Row> rows = List.of();
    private static long lastRefresh;
    private static Map<String, Integer> storageCounts = Map.of();
    private static long lastStorageRefresh;
    private static int scroll;
    private static Path file;

    private SkyJewCraftHelper() {}

    public static void init(Path configDir) {
        // SkyOcean's CraftHelperOverlay: "64x Item" and the raw list of base ingredients, movable in /sj gui.
        com.epic60869.skyjew.features.core.SkyJewHuds.register("craft_helper", "Recipe (Craft Helper)",
            () -> {
                SkyJewConfig config = SkyJewConfig.current();
                return config != null && config.misc.recipeHud && selectedId != null && Compat.isOnSkyblock();
            },
            SkyJewCraftHelper::hudLines,
            List.of(Component.literal("64x Hyperion").withStyle(ChatFormatting.GOLD),
                Component.literal("✔ 8/8 Necron's Handle").withStyle(ChatFormatting.GREEN),
                Component.literal("✖ 120/350 Wither Catalyst").withStyle(ChatFormatting.RED)),
            8, 200);
        file = configDir.resolve("skyjew-crafthelper.json");
        try {
            if (Files.exists(file)) {
                JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.has("id")) {
                    select(root.get("id").getAsString(), root.has("amount") ? root.get("amount").getAsInt() : 1, false);
                }
            }
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to load craft helper: " + e.getMessage());
        }
    }

    public static boolean active() {
        return selectedId != null;
    }

    public static String selectedId() {
        return selectedId;
    }

    public static int selectedAmount() {
        return selectedAmount;
    }

    public static void clear() {
        selectedId = null;
        tree = null;
        rows = List.of();
        save();
    }

    /** Selects an item and builds its recipe tree in the background. */
    public static void select(String id, int amount, boolean announce) {
        selectedId = id;
        selectedAmount = Math.max(1, amount);
        tree = null;
        rows = List.of();
        scroll = 0;
        building = true;
        save();
        CompletableFuture.supplyAsync(() -> buildTree(id, selectedAmount)).thenAccept(built -> Minecraft.getInstance().execute(() -> {
            if (!id.equals(selectedId)) return;
            building = false;
            tree = built;
            lastRefresh = 0;
            if (announce) {
                if (built.recipe == null) {
                    message(Component.literal("No recipe found for " + SkyJewRecipeCommand.displayName(id) + "; tracking the item only.").withStyle(ChatFormatting.YELLOW));
                } else {
                    message(Component.literal("Set current recipe to ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(built.required + "x ").withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(SkyJewRecipeCommand.displayName(id)).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("!").withStyle(ChatFormatting.GRAY)));
                }
            }
        }));
    }

    public static void setAmount(int amount) {
        if (selectedId != null) select(selectedId, amount, true);
    }

    private static int perCraft() {
        return tree == null || tree.recipe == null ? 1 : Math.max(1, tree.recipe.outputCount());
    }

    /** Changes the amount by whole crafts: 1, or 10 with Shift, or 64 with Ctrl (SkyOcean's -/+ buttons). */
    private static void stepAmount(int direction) {
        if (selectedId == null) return;
        var window = Minecraft.getInstance().getWindow();
        boolean ctrl = org.lwjgl.glfw.GLFW.glfwGetKey(window.handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
            || org.lwjgl.glfw.GLFW.glfwGetKey(window.handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        boolean shift = org.lwjgl.glfw.GLFW.glfwGetKey(window.handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
            || org.lwjgl.glfw.GLFW.glfwGetKey(window.handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        int step = ctrl ? 64 : shift ? 10 : 1;
        int per = perCraft();
        int crafts = Math.max(1, selectedAmount / per + direction * step);
        select(selectedId, crafts * per, false);
    }

    /** HUD lines: title and the merged base ingredients, like SkyOcean's raw formatter. */
    private static List<Component> hudLines() {
        refresh();
        List<Component> lines = new ArrayList<>();
        if (selectedId == null) return lines;
        lines.add(Component.literal(selectedAmount + "x ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(SkyJewRecipeCommand.displayName(selectedId)).withStyle(ChatFormatting.GOLD)));
        if (building) {
            lines.add(Component.literal("Loading recipes...").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        java.util.LinkedHashMap<String, int[]> merged = new java.util.LinkedHashMap<>();
        for (Row row : rows) {
            if (!row.leaf() || row == rows.getFirst()) continue;
            int[] totals = merged.computeIfAbsent(row.id(), k -> new int[2]);
            totals[0] += row.available();
            totals[1] += row.needed();
        }
        SkyJewConfig config = SkyJewConfig.current();
        boolean hideDone = config != null && config.misc.recipeHudHideCompleted;
        int shown = 0;
        for (var entry : merged.entrySet()) {
            int have = entry.getValue()[0], need = entry.getValue()[1];
            boolean done = have >= need;
            if (done && hideDone) continue;
            float progress = need <= 0 ? 1f : Math.min(1f, have / (float) need);
            int colour = ARGB.srgbLerp(progress, 0xFF5555, 0x55FF55);
            lines.add(Component.literal(done ? "✔ " : "✖ ").withStyle(done ? ChatFormatting.GREEN : ChatFormatting.RED)
                .append(Component.literal(String.format(Locale.US, "%,d/%,d ", Math.min(have, need), need)).withColor(colour))
                .append(Component.literal(SkyJewRecipeCommand.displayName(entry.getKey())).withStyle(ChatFormatting.WHITE)));
            if (++shown >= 20) break;
        }
        if (shown == 0) lines.add(Component.literal("You have everything!").withStyle(ChatFormatting.GREEN));
        return lines;
    }

    private static void message(Component text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(Compat.PREFIX.get().append(text));
    }

    private static void save() {
        if (file == null) return;
        try {
            JsonObject root = new JsonObject();
            if (selectedId != null) {
                root.addProperty("id", selectedId);
                root.addProperty("amount", selectedAmount);
            }
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to save craft helper: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------- recipes

    /** The best recipe for an item (crafting before Forge), fetched from the NEU repo and cached. Blocking. */
    static Recipe recipeOf(String id) {
        return RECIPES.computeIfAbsent(id, key -> Optional.ofNullable(withoutUncompacting(key, rawRecipeOf(key)))).orElse(null);
    }

    private static final Map<String, Optional<Recipe>> RAW_RECIPES = new ConcurrentHashMap<>();

    private static Recipe rawRecipeOf(String id) {
        return RAW_RECIPES.computeIfAbsent(id, key -> Optional.ofNullable(fetchRecipe(key))).orElse(null);
    }

    /**
     * Drops "uncompacting" recipes such as Diamond Block → 9 Diamonds: a single-input recipe whose
     * input is itself crafted from this item. Those items are treated as base items instead of
     * looping back up the compaction chain.
     */
    private static Recipe withoutUncompacting(String id, Recipe recipe) {
        if (recipe == null || recipe.inputs().size() != 1) return recipe;
        Recipe inputRecipe = rawRecipeOf(recipe.inputs().getFirst().id());
        if (inputRecipe == null) return recipe;
        for (Input input : inputRecipe.inputs()) {
            if (input.id().equalsIgnoreCase(id)) return null;
        }
        return recipe;
    }

    private static Recipe fetchRecipe(String id) {
        try {
            String path = "items/" + java.net.URLEncoder.encode(neuId(id), StandardCharsets.UTF_8).replace("+", "%20") + ".json";
            JsonObject root = JsonParser.parseString(RepoItems.neuRepoFile(path)).getAsJsonObject();
            Recipe crafting = null;
            Recipe forge = null;
            if (root.has("recipe") && root.get("recipe").isJsonObject()) {
                crafting = craftingRecipe(root.getAsJsonObject("recipe"));
            }
            if (root.has("recipes") && root.get("recipes").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("recipes")) {
                    if (!element.isJsonObject()) continue;
                    JsonObject recipe = element.getAsJsonObject();
                    String type = recipe.has("type") ? recipe.get("type").getAsString() : "crafting";
                    if (recipe.has("overrideOutputId") && !recipe.get("overrideOutputId").getAsString().equalsIgnoreCase(neuId(id))) continue;
                    if (crafting == null && type.equals("crafting")) crafting = craftingRecipe(recipe);
                    if (forge == null && type.equals("forge") && recipe.has("inputs")) {
                        forge = new Recipe("forge", merge(parseInputs(recipe.getAsJsonArray("inputs"))), count(recipe));
                    }
                }
            }
            return crafting != null ? crafting : forge;
        } catch (Exception e) {
            return null; // no repo entry: a base item
        }
    }

    private static Recipe craftingRecipe(JsonObject recipe) {
        List<Input> inputs = new ArrayList<>();
        for (String row : new String[]{"A", "B", "C"}) {
            for (String col : new String[]{"1", "2", "3"}) {
                String key = row + col;
                if (!recipe.has(key)) continue;
                Input input = parseInput(recipe.get(key).getAsString());
                if (input != null) inputs.add(input);
            }
        }
        return inputs.isEmpty() ? null : new Recipe("crafting", merge(inputs), count(recipe));
    }

    private static int count(JsonObject recipe) {
        return recipe.has("count") ? Math.max(1, (int) Math.round(recipe.get("count").getAsDouble())) : 1;
    }

    private static List<Input> parseInputs(JsonArray array) {
        List<Input> inputs = new ArrayList<>();
        for (JsonElement element : array) {
            Input input = parseInput(element.getAsString());
            if (input != null) inputs.add(input);
        }
        return inputs;
    }

    private static Input parseInput(String value) {
        if (value == null || value.isBlank()) return null;
        int split = value.lastIndexOf(':');
        if (split > 0) {
            try {
                return new Input(value.substring(0, split), (int) Math.ceil(Double.parseDouble(value.substring(split + 1))));
            } catch (NumberFormatException ignored) {}
        }
        return new Input(value, 1);
    }

    private static List<Input> merge(List<Input> inputs) {
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (Input input : inputs) merged.merge(input.id(), input.amount(), Integer::sum);
        List<Input> result = new ArrayList<>();
        merged.forEach((id, amount) -> result.add(new Input(id, amount)));
        return result;
    }

    /** NEU repo ids use "-" for item variants where SkyBlock ids use ":". */
    private static String neuId(String id) {
        return id.toUpperCase(Locale.ROOT).replace(':', '-');
    }

    // -------------------------------------------------------------------- tree

    private static Node buildTree(String id, int amount) {
        Recipe recipe = recipeOf(id);
        int perCraft = recipe == null ? 1 : recipe.outputCount();
        int total = (int) Math.ceil(amount / (double) perCraft) * perCraft; // round up to whole crafts
        Node root = new Node(id, recipe, total, 0);
        if (recipe != null) {
            Set<String> visited = new HashSet<>();
            visited.add(id);
            evaluateChildren(root, recipe, total / perCraft, new HashMap<>(), visited, 0);
        }
        return root;
    }

    /** SkyOcean's CraftHelperParentNode.evaluateChildren. */
    private static void evaluateChildren(Node parent, Recipe recipe, int crafts, Map<String, Integer> remainder, Set<String> visited, int depth) {
        for (Input input : recipe.inputs()) {
            Recipe childRecipe = depth >= MAX_DEPTH || visited.contains(input.id()) ? null : recipeOf(input.id());
            int perCraft = childRecipe == null ? 1 : childRecipe.outputCount();
            int totalRequired = Math.multiplyExact(input.amount(), crafts);
            int available = remainder.getOrDefault(input.id(), 0);
            int carriedOver = Math.min(available, totalRequired);
            int requiredAmount = totalRequired - carriedOver;
            int leftoverOfLeftover = available - carriedOver;
            int craftsRequired = Math.ceilDiv(requiredAmount, perCraft);
            int newRemainder = (int) ((long) craftsRequired * perCraft - requiredAmount);
            remainder.put(input.id(), newRemainder + leftoverOfLeftover);

            Node child = new Node(input.id(), childRecipe, requiredAmount, carriedOver);
            if (childRecipe != null) {
                Set<String> nextVisited = new HashSet<>(visited);
                nextVisited.add(input.id());
                evaluateChildren(child, childRecipe, craftsRequired, remainder, nextVisited, depth + 1);
            }
            parent.children.add(child);
        }
        parent.children.sort((a, b) -> Integer.compare(a.totalChildren, b.totalChildren));
        parent.totalChildren = parent.children.size() + parent.children.stream().mapToInt(n -> n.totalChildren).sum();
    }

    // -------------------------------------------------------------- evaluation

    /** What you own, preferring the inventory over storage (SkyOcean's ItemTracker priority). */
    private static final class Tracker {
        final Map<String, Integer> inventory = new HashMap<>();
        final Map<String, Integer> storage;

        Tracker(Map<String, Integer> storage) {
            this.storage = new HashMap<>(storage);
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                String id = Compat.neuName(stack);
                if (!id.isEmpty()) inventory.merge(id, stack.getCount(), Integer::sum);
            }
        }

        /** Takes up to {@code amount}; returns {fromInventory, fromStorage}. */
        int[] take(String id, int amount) {
            if (amount <= 0) return new int[]{0, 0};
            int inv = Math.min(amount, inventory.getOrDefault(id, 0));
            inventory.merge(id, -inv, Integer::sum);
            int sto = Math.min(amount - inv, storage.getOrDefault(id, 0));
            storage.merge(id, -sto, Integer::sum);
            return new int[]{inv, sto};
        }
    }

    private record State(int amount, int required, int carryOver, int throughParents, boolean done, boolean childrenDone) {
        int totalAmount() {
            return amount + carryOver + throughParents;
        }
    }

    private static void refresh() {
        long now = System.currentTimeMillis();
        if (tree == null || now - lastRefresh < REFRESH_MS) return;
        lastRefresh = now;
        if (now - lastStorageRefresh > 5000) {
            lastStorageRefresh = now;
            storageCounts = SkyJewStorageSearch.storedItemCounts();
        }
        List<Row> out = new ArrayList<>();
        evaluate(tree, null, 0, new Tracker(storageCounts), "", true, out);
        rows = out;
    }

    /** SkyOcean's CraftHelperContext.toState plus TreeFormatter, producing display rows depth first. */
    private static State evaluate(Node node, State parent, int parentRequired, Tracker tracker, String prefix, boolean root, List<Row> out) {
        int required = node.required;
        int throughParents = 0;
        if (parent != null && parentRequired > 0) {
            throughParents = (int) Math.floor(required * (parent.totalAmount() / (float) parentRequired));
            throughParents = Math.max(0, Math.min(required, throughParents));
        }
        int[] taken = tracker.take(node.id, required - throughParents);
        int amount = taken[0] + taken[1];
        int stateRequired = required - throughParents;
        boolean done = amount + node.carriedOver >= stateRequired;

        int rowIndex = out.size();
        out.add(null); // placeholder until children are evaluated
        State self = new State(amount, stateRequired, node.carriedOver, throughParents, done, true);

        List<Node> children = new ArrayList<>(node.children);
        java.util.Collections.reverse(children);
        boolean childrenDone = !children.isEmpty();
        for (int i = 0; i < children.size(); i++) {
            boolean last = i == children.size() - 1;
            String childPrefix = root ? (last ? "└ " : "├ ") : prefix.replace("├", "│").replace("└", "  ") + (last ? "└ " : "├ ");
            State child = evaluate(children.get(i), self, required, tracker, childPrefix, false, out);
            childrenDone &= child.done() || child.childrenDone();
        }
        String type = node.recipe == null ? "none" : node.recipe.type();
        out.set(rowIndex, new Row(prefix, node.id, amount + node.carriedOver, stateRequired, done, childrenDone,
            taken[0], taken[1], throughParents, node.carriedOver, type, node.children.isEmpty()));
        return new State(amount, stateRequired, node.carriedOver, throughParents, done, childrenDone);
    }

    // ----------------------------------------------------------------- overlay

    private static Component rowText(Row row) {
        MutableComponent text = Component.literal(row.prefix()).withStyle(ChatFormatting.DARK_GRAY);
        if (row.done()) text.append(Component.literal("✔ ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        else if (row.childrenDone()) text.append(Component.literal("⚠ ").withStyle(ChatFormatting.YELLOW));
        else text.append(Component.literal("✖ ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        float progress = row.needed() <= 0 ? 1f : Math.min(1f, row.available() / (float) row.needed());
        int colour = ARGB.srgbLerp(progress, 0xFF5555, 0x55FF55);
        text.append(Component.literal(String.format(Locale.US, "%,d", row.available())).withColor(colour))
            .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.format(Locale.US, "%,d", row.needed())).withColor(colour))
            .append(Component.literal(" " + SkyJewRecipeCommand.displayName(row.id())).withStyle(ChatFormatting.WHITE));
        return text;
    }

    private static int[] bounds(AbstractContainerScreen<?> screen) {
        var accessor = (com.epic60869.skyjew.mixin.SkyJewContainerScreenAccessor) screen;
        int left = accessor.skyjew$getLeftPos();
        int width = Math.max(120, Math.min(260, left - 12));
        int x = left - width - 6;
        if (x < 4) x = 4; // narrow window: overlap rather than go off screen
        int y = Math.max(6, accessor.skyjew$getTopPos());
        int height = Math.min(screen.height - y - 6, 36 + rows.size() * LINE_HEIGHT);
        return new int[]{x, y, width, Math.max(40, height)};
    }

    public static void render(GuiGraphicsExtractor g, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (selectedId == null) return;
        refresh();
        var font = Minecraft.getInstance().font;
        int[] b = bounds(screen);
        int x = b[0], y = b[1], w = b[2], h = b[3];

        g.fill(x, y, x + w, y + h, 0xE0101420);
        g.fill(x, y, x + w, y + 1, 0xFF9A6CFF);
        g.item(RepoItems.itemStack(selectedId), x + PADDING, y + PADDING);
        String title = SkyJewRecipeCommand.displayName(selectedId);
        g.text(font, font.plainSubstrByWidth(title, w - 44), x + PADDING + 20, y + PADDING - 1, 0xFFFFD34D, true);
        int ax = x + PADDING + 20, ay = y + PADDING + 9;
        String amountText = " " + selectedAmount + " ";
        g.text(font, "-", ax, ay, 0xFFFF5555, true);
        g.text(font, amountText, ax + 6, ay, 0xFFAAAAAA, true);
        g.text(font, "+", ax + 6 + font.width(amountText), ay, 0xFF55FF55, true);
        boolean overMinus = mouseX >= ax - 2 && mouseX < ax + 6 && mouseY >= ay - 1 && mouseY < ay + 9;
        boolean overPlus = mouseX >= ax + 4 + font.width(amountText) && mouseX < ax + 12 + font.width(amountText) && mouseY >= ay - 1 && mouseY < ay + 9;
        if (overMinus || overPlus) {
            String verb = overMinus ? "decrease" : "increase";
            g.setTooltipForNextFrame(font, List.of(
                Component.literal("Click to " + verb + " by 1").withStyle(ChatFormatting.GRAY),
                Component.literal("Shift + Click to " + verb + " by 10").withStyle(ChatFormatting.GRAY),
                Component.literal("Ctrl + Click to " + verb + " by 64").withStyle(ChatFormatting.GRAY)), java.util.Optional.empty(), mouseX, mouseY);
        }
        g.fill(x + w - 16, y + 4, x + w - 4, y + 16, 0xFF8B1E2D);
        g.text(font, "×", x + w - 12, y + 6, 0xFFFFFFFF, true);

        int listTop = y + 28;
        int visible = Math.max(1, (y + h - listTop - 4) / LINE_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        if (building) {
            g.text(font, "Loading recipes...", x + PADDING, listTop, 0xFFAAAAAA, false);
            return;
        }
        Row hovered = null;
        for (int i = 0; i < visible && i + scroll < rows.size(); i++) {
            Row row = rows.get(i + scroll);
            int rowY = listTop + i * LINE_HEIGHT;
            g.pose().pushMatrix();
            g.enableScissor(x + PADDING, rowY, x + w - PADDING, rowY + LINE_HEIGHT);
            g.text(font, rowText(row), x + PADDING, rowY, 0xFFFFFFFF, false);
            g.disableScissor();
            g.pose().popMatrix();
            if (mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + LINE_HEIGHT) hovered = row;
        }
        if (rows.size() > visible) {
            g.text(font, (scroll + 1) + "-" + Math.min(rows.size(), scroll + visible) + " / " + rows.size() + " (scroll)", x + PADDING, y + h - 10, 0xFF8794A8, false);
        }
        if (hovered != null) g.setTooltipForNextFrame(font, tooltip(hovered), java.util.Optional.empty(), mouseX, mouseY);
    }

    private static List<Component> tooltip(Row row) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(SkyJewRecipeCommand.displayName(row.id())).withStyle(ChatFormatting.WHITE));
        if (row.throughParents() > 0) lines.add(Component.literal("Covered by crafted parents: " + row.throughParents()).withStyle(ChatFormatting.GRAY));
        if (row.carryOver() > 0) lines.add(Component.literal("Leftover from another craft: " + row.carryOver()).withStyle(ChatFormatting.GRAY));
        if (row.fromInventory() > 0) lines.add(Component.literal("Inventory: " + row.fromInventory()).withStyle(ChatFormatting.GRAY));
        if (row.fromStorage() > 0) lines.add(Component.literal("Storage: " + row.fromStorage()).withStyle(ChatFormatting.GRAY));
        if (!row.recipeType().equals("none")) lines.add(Component.literal("Click to open recipe!").withStyle(ChatFormatting.YELLOW));
        return lines;
    }

    public static boolean mouseClicked(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        if (selectedId == null || button != 0) return false;
        int[] b = bounds(screen);
        int x = b[0], y = b[1], w = b[2], h = b[3];
        if (mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + h) return false;
        if (mouseX >= x + w - 16 && mouseY < y + 18) {
            clear();
            return true;
        }
        var font = Minecraft.getInstance().font;
        int ax = x + PADDING + 20, ay = y + PADDING + 9;
        int amountWidth = font.width(" " + selectedAmount + " ");
        if (mouseY >= ay - 1 && mouseY < ay + 9) {
            if (mouseX >= ax - 2 && mouseX < ax + 6) {
                stepAmount(-1);
                return true;
            }
            if (mouseX >= ax + 4 + amountWidth && mouseX < ax + 12 + amountWidth) {
                stepAmount(1);
                return true;
            }
        }
        int index = (int) ((mouseY - (y + 28)) / LINE_HEIGHT) + scroll;
        if (mouseY >= y + 28 && index >= 0 && index < rows.size() && !rows.get(index).recipeType().equals("none")) {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) connection.sendCommand("viewrecipe " + rows.get(index).id());
        }
        return true;
    }

    public static boolean mouseScrolled(AbstractContainerScreen<?> screen, double mouseX, double mouseY, double amount) {
        if (selectedId == null) return false;
        int[] b = bounds(screen);
        if (mouseX < b[0] || mouseX >= b[0] + b[2] || mouseY < b[1] || mouseY >= b[1] + b[3]) return false;
        scroll -= (int) Math.signum(amount) * 3;
        return true;
    }
}

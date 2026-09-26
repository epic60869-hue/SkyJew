package com.epic60869.skyballs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import com.mojang.blaze3d.platform.InputConstants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Global Hypixel SkyBlock storage search.
 *
 * It learns Ender Chest and Backpack pages as the player opens them, keeps the
 * last known contents locally, and provides a fast searchable index. It never
 * moves or clicks items by itself.
 */
public final class SkyBallsStorageSearch {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "skyballs-storage-search.json";

    private static final Pattern ENDER_CHEST = Pattern.compile("(?i)ender\\s+chest.*?(?:#|\\(|\\s)(\\d+)(?:\\)|\\s|$)");
    private static final Pattern BACKPACK = Pattern.compile("(?i)(?:small|medium|large|greater|jumbo)?\\s*backpack.*?(?:#|\\(|\\s)(\\d+)(?:\\)|\\s|$)");

    private static final long CAPTURE_INTERVAL_MS = 400L;
    private static final long SAVE_INTERVAL_MS = 1200L;
    private static final long INVENTORY_CAPTURE_INTERVAL_MS = 500L;

    private static final Map<String, Page> pages = new LinkedHashMap<>();
    private static final Map<String, Page> inventoryPages = new LinkedHashMap<>();
    private static Path configDir;
    private static boolean initialized;
    private static long lastCapture;
    private static long lastInventoryCapture;
    private static long lastSave;
    private static boolean dirty;
    private static boolean previousOpenKey;
    private static Result pendingHighlight;

    private record Page(String type, int number, String label, String blob, long updatedMs) {}

    public record Result(ItemStack stack, String name, String id, String lore,
                         String location, String key, String type, int number, int slot) {}

    /** Current SkyBlock profile id (from "Profile ID: ..."), or "" until Hypixel has told us. */
    private static String profile = "";
    private static final java.util.regex.Pattern PROFILE_ID = java.util.regex.Pattern.compile("^Profile ID: (?<id>[0-9a-fA-F-]+)$");

    private SkyBallsStorageSearch() {}

    public static void init(Path dir) {
        configDir = dir;
        load();
        initialized = true;
        // Pages are stored per SkyBlock profile so different profiles (e.g. an ironman) never share storage.
        com.epic60869.skyballs.features.core.SkyBallsChat.onChat(message -> {
            java.util.regex.Matcher m = PROFILE_ID.matcher(message.text().trim());
            if (m.matches()) profile = m.group("id").toLowerCase(Locale.ROOT);
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> profile = "");
    }

    /** Key prefix for the current server and profile. Keys are "server|profile|type|number". */
    private static String profilePrefix(Minecraft mc) {
        String server = "unknown";
        try {
            if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
                server = mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
            }
        } catch (Throwable ignored) {}
        return server + "|" + profile + "|";
    }

    private static boolean currentProfile(String key) {
        return !profile.isEmpty() && key.startsWith(profilePrefix(Minecraft.getInstance()));
    }

    public static void tick(Minecraft mc) {
        if (!initialized || mc.player == null) return;

        captureOpenStorage(mc);
        capturePlayerInventory(mc);
        applyPendingHighlight(mc);

        boolean ctrl = InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_RCONTROL);
        boolean f = InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_F);
        boolean open = ctrl && f;

        if (open && !previousOpenKey && mc.gui.screen() == null && isHypixel(mc)) {
            open(mc, "");
        }
        previousOpenKey = open;

        if (dirty && System.currentTimeMillis() - lastSave >= SAVE_INTERVAL_MS) {
            save();
        }
    }

    public static void open(Minecraft mc, String query) {
        if (!isHypixel(mc)) return;
        Screen parent = mc.gui.screen();
        mc.gui.setScreen(new SkyBallsStorageSearchScreen(parent, query == null ? "" : query));
    }

    public static boolean isHypixel(Minecraft mc) {
        try {
            if (mc.getCurrentServer() == null || mc.getCurrentServer().ip == null) return false;
            String ip = mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
            return ip.equals("hypixel.net") || ip.endsWith(".hypixel.net");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static List<Result> search(Minecraft mc, String query, boolean lore, boolean inventory) {
        List<Result> results = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        for (Map.Entry<String, Page> entry : pages.entrySet()) {
            if (!currentProfile(entry.getKey())) continue;
            Page page = entry.getValue();
            List<ItemStack> contents = decode(page.blob());
            if (contents == null) continue;

            for (int i = 0; i < contents.size(); i++) {
                ItemStack stack = contents.get(i);
                if (stack == null || stack.isEmpty() || !isSearchableStorageItem(stack)) continue;

                SearchText text = searchable(stack);
                if (!q.isEmpty()
                        && !text.name().contains(q)
                        && !text.id().contains(q)
                        && (!lore || !text.lore().contains(q))) {
                    continue;
                }

                int row = i / 9 + 1;
                int col = i % 9 + 1;
                String location = page.label() + " · slot " + (i + 1) + " (r" + row + " c" + col + ")";
                results.add(new Result(stack.copy(), text.displayName(), text.id(), text.lore(),
                        location, entry.getKey(), page.type(), page.number(), i));
            }
        }

        if (inventory) {
            for (Map.Entry<String, Page> entry : inventoryPages.entrySet()) {
                if (!currentProfile(entry.getKey())) continue;
                Page page = entry.getValue();
                List<ItemStack> contents = decode(page.blob());
                if (contents == null) continue;
                for (int i = 0; i < contents.size(); i++) {
                    ItemStack stack = contents.get(i);
                    if (stack == null || stack.isEmpty()) continue;
                    SearchText text = searchable(stack);
                    if (!q.isEmpty()
                            && !text.name().contains(q)
                            && !text.id().contains(q)
                            && (!lore || !text.lore().contains(q))) continue;
                    results.add(new Result(stack.copy(), text.displayName(), text.id(), text.lore(),
                            page.label() + " · " + inventoryLocation(i), entry.getKey(), "INVENTORY", 0, i));
                }
            }
        }

        results.sort(Comparator
                .comparing((Result r) -> r.name().toLowerCase(Locale.ROOT))
                .thenComparing(Result::location));
        return results;
    }

    public static int cachedStorageCount() {
        return (int) pages.keySet().stream().filter(SkyBallsStorageSearch::currentProfile).count();
    }

    public static long oldestCacheAgeMs() {
        long oldest = Long.MAX_VALUE;
        for (Map.Entry<String, Page> entry : pages.entrySet()) {
            if (currentProfile(entry.getKey())) oldest = Math.min(oldest, entry.getValue().updatedMs());
        }
        if (oldest == Long.MAX_VALUE) return -1L;
        return Math.max(0L, System.currentTimeMillis() - oldest);
    }

    public static void openResult(Minecraft mc, Result result) {
        pendingHighlight = result;
        if ("INVENTORY".equals(result.type())) {
            mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            return;
        }
        if (result.type().equals("ENDER_CHEST") && result.number() > 0) {
            mc.gui.setScreen(null);
            if (mc.player != null && mc.player.connection != null) {
                mc.player.connection.sendCommand("ec " + result.number());
            } else {
                pendingHighlight = null;
            }
        } else if (result.type().equals("BACKPACK") && result.number() > 0) {
            mc.gui.setScreen(null);
            if (mc.player != null && mc.player.connection != null) {
                mc.player.connection.sendCommand("bp " + result.number());
            } else {
                pendingHighlight = null;
            }
        }
    }

    public static boolean shouldHighlight(ItemStack stack) {
        if (pendingHighlight == null || stack == null || stack.isEmpty()) return false;
        return sameSearchItem(stack, pendingHighlight.stack());
    }

    public static void consumeHighlight() {
        pendingHighlight = null;
    }

    private static boolean sameSearchItem(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return false;
        SearchText aa = searchable(a);
        SearchText bb = searchable(b);
        return aa.id().equals(bb.id())
                && aa.name().equals(bb.name())
                && aa.lore().equals(bb.lore());
    }

    private static void applyPendingHighlight(Minecraft mc) {
        // Do not move the native GLFW cursor here. On 26.2 that can race the
        // container's input/render path and crash when another mod replaces the
        // screen during an /ec or /bp command. The result remains selected by
        // the search UI and the opened container is left untouched.
        if (pendingHighlight == null || mc.gui.screen() == null) return;
        Result result = pendingHighlight;
        if ("INVENTORY".equals(result.type())) {
            return;
        }
        if (!(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) return;
        String title = cleanTitle(screen.getTitle().getString());
        boolean matching = result.type().equals("ENDER_CHEST")
            ? title.contains("ender chest")
            : title.contains("backpack");
        if (matching) pendingHighlight = null;
    }

    private static String cleanTitle(String title) {
        return title.replaceAll("§[0-9A-FK-ORa-fk-or]", "")
            .replaceAll("\\s+", " ")
            .trim().toLowerCase(Locale.ROOT);
    }

    private static void capturePlayerInventory(Minecraft mc) {
        if (!isHypixel(mc) || mc.player == null || profile.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - lastInventoryCapture < INVENTORY_CAPTURE_INTERVAL_MS) return;
        lastInventoryCapture = now;
        List<ItemStack> contents = new ArrayList<>(mc.player.getInventory().getContainerSize());
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            contents.add(stack == null ? ItemStack.EMPTY : stack.copy());
        }
        String key = inventoryCacheKey(mc);
        String blob = encode(contents);
        if (blob == null) return;
        Page old = inventoryPages.get(key);
        if (old == null || !old.blob().equals(blob)) {
            inventoryPages.put(key, new Page("INVENTORY", 0, "Your Inventory", blob, now));
            dirty = true;
        }
    }

    private static String inventoryCacheKey(Minecraft mc) {
        String uuid = mc.player == null ? "unknown" : mc.player.getUUID().toString();
        return profilePrefix(mc) + "INVENTORY|" + uuid;
    }

    private static void captureOpenStorage(Minecraft mc) {
        if (!isHypixel(mc) || profile.isEmpty()) return;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> container)) return;
        if (container.getMenu().slots.size() <= 36) return;

        long now = System.currentTimeMillis();
        if (now - lastCapture < CAPTURE_INTERVAL_MS) return;
        lastCapture = now;

        StorageTarget target = identify(cleanTitle(container.getTitle().getString()));
        if (target == null) return;

        List<Slot> slots = container.getMenu().slots;
        int count = Math.max(0, slots.size() - 36);
        if (count == 0) count = Math.min(54, slots.size());
        if (count <= 0) return;

        List<ItemStack> contents = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = slots.get(i).getItem();
            // Persist only real storage contents. GUI filler/navigation controls are
            // deliberately written as empty slots so they can never pollute search.
            contents.add(isSearchableStorageItem(stack) ? stack.copy() : ItemStack.EMPTY);
        }

        String key = cacheKey(mc, target.type(), target.number());
        Page old = pages.get(key);
        String blob = encode(contents);
        if (blob == null) return;

        if (old == null || !old.blob().equals(blob) || !old.label().equals(target.label())) {
            pages.put(key, new Page(target.type(), target.number(), target.label(), blob, now));
            dirty = true;
        }
    }

    private static StorageTarget identify(String title) {
        String normalized = title == null ? "" : title.trim().replaceAll("\\s+", " ");
        Matcher ender = ENDER_CHEST.matcher(normalized);
        if (ender.find()) {
            int number = parseNumber(ender.group(1));
            return new StorageTarget("ENDER_CHEST", number, number > 0 ? "Ender Chest #" + number : "Ender Chest");
        }
        Matcher backpack = BACKPACK.matcher(normalized);
        if (backpack.find()) {
            int number = parseNumber(backpack.group(1));
            return new StorageTarget("BACKPACK", number, number > 0 ? "Backpack #" + number : "Backpack");
        }

        // Hypixel has changed the visible title formatting several times.
        // Never let a title variation prevent the cache from learning a page.
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("ender chest")) {
            int number = firstNumber(normalized);
            return new StorageTarget("ENDER_CHEST", number, number > 0 ? "Ender Chest #" + number : "Ender Chest");
        }
        if (lower.contains("backpack")) {
            int number = firstNumber(normalized);
            return new StorageTarget("BACKPACK", number, number > 0 ? "Backpack #" + number : "Backpack");
        }
        return null;
    }

    private static int firstNumber(String text) {
        Matcher m = Pattern.compile("\\d+").matcher(text);
        return m.find() ? parseNumber(m.group()) : 0;
    }

    private record StorageTarget(String type, int number, String label) {}

    private record SearchText(String displayName, String name, String id, String lore) {}

    private static SearchText searchable(ItemStack stack) {
        String display = stack.getHoverName().getString();
        String name = display.toLowerCase(Locale.ROOT);

        String id = "";
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            try {
                id = customData.copyTag().getStringOr("id", "").toLowerCase(Locale.ROOT);
            } catch (Throwable ignored) {}
        }

        StringBuilder lore = new StringBuilder();
        ItemLore itemLore = stack.get(DataComponents.LORE);
        if (itemLore != null) {
            for (Component line : itemLore.lines()) {
                lore.append(line.getString()).append('\n');
            }
        }

        return new SearchText(display, name, id, lore.toString().toLowerCase(Locale.ROOT));
    }

    private static String inventoryLocation(int slot) {
        if (slot < 9) return "Inventory · Hotbar slot " + (slot + 1);
        if (slot < 36) {
            int row = (slot - 9) / 9 + 1;
            int col = (slot - 9) % 9 + 1;
            return "Inventory · r" + row + " c" + col;
        }
        if (slot == 40) return "Inventory · Offhand";
        if (slot >= 36 && slot <= 39) {
            return switch (slot) {
                case 36 -> "Inventory · Boots";
                case 37 -> "Inventory · Leggings";
                case 38 -> "Inventory · Chestplate";
                default -> "Inventory · Helmet";
            };
        }
        return "Inventory · slot " + (slot + 1);
    }

    private static boolean isSearchableStorageItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        String name = stack.getHoverName().getString()
                .replaceAll("§[0-9A-FK-ORa-fk-or]", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        String id = "";
        try {
            id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {}

        // SkyBlock storage GUIs use these as navigation/decorative controls.
        if (id.endsWith("stained_glass_pane") || id.equals("barrier")) return false;

        // Hypixel's page arrows are plain vanilla arrows without a SkyBlock item id.
        // Real SkyBlock arrows always carry one, so this never hides stored items.
        if (id.equals("arrow") && searchable(stack).id().isEmpty()) return false;

        // Names may be decorated with arrows or page counters ("» Next Page (2/9)").
        String words = name.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (words.contains("next page") || words.contains("previous page") || words.contains("prev page")
                || words.contains("first page") || words.contains("last page")) {
            return false;
        }

        return !(name.equals("go back")
                || name.equals("back")
                || name.equals("close")
                || name.equals("exit")
                || name.equals("first page") || name.equals("last page") || name.equals("previous page")
                || name.equals("next page")
                || name.equals("previous")
                || name.equals("next")
                || name.startsWith("first page") || name.startsWith("last page") || name.startsWith("previous page")
                || name.startsWith("next page")
                || name.startsWith("page ")
                || name.matches("page\\s*\\d+")
                || name.matches("[<>]\\s*page\\s*\\d*")
                || name.contains("click to go back")
                || name.contains("click to close")
                || name.contains("click to view")
                || name.contains("open first") || name.contains("open last") || name.contains("open previous")
                || name.contains("open next"));
    }

    private static String cacheKey(Minecraft mc, String type, int number) {
        return profilePrefix(mc) + type + "|" + number;
    }

    private static void load() {
        pages.clear();
        if (configDir == null) return;

        Path file = configDir.resolve(FILE_NAME);
        if (!Files.exists(file)) return;

        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            inventoryPages.clear();
            for (String key : root.keySet()) {
                JsonObject obj = root.getAsJsonObject(key);
                if (obj == null || !obj.has("blob")) continue;
                String type = obj.has("type") ? obj.get("type").getAsString() : "";
                int number = obj.has("number") ? obj.get("number").getAsInt() : 0;
                String label = obj.has("label") ? obj.get("label").getAsString() : type + " #" + number;
                long updated = obj.has("updated") ? obj.get("updated").getAsLong() : 0L;
                if (key.split("\\|", -1).length < 4) continue; // pre-profile cache entry (mixed profiles)
                Page page = new Page(type, number, label, obj.get("blob").getAsString(), updated);
                if ("INVENTORY".equals(type)) inventoryPages.put(key, page);
                else pages.put(key, page);
            }
        } catch (Exception e) {
            System.err.println("[SkyBalls] Failed to load storage search cache: " + e.getMessage());
            pages.clear();
        }
    }

    private static void save() {
        if (configDir == null) return;
        try {
            Files.createDirectories(configDir);
            JsonObject root = new JsonObject();
            for (Map.Entry<String, Page> entry : pages.entrySet()) {
                Page page = entry.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("type", page.type());
                obj.addProperty("number", page.number());
                obj.addProperty("label", page.label());
                obj.addProperty("updated", page.updatedMs());
                obj.addProperty("blob", page.blob());
                root.add(entry.getKey(), obj);
            }
            for (Map.Entry<String, Page> entry : inventoryPages.entrySet()) {
                Page page = entry.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("type", page.type());
                obj.addProperty("number", page.number());
                obj.addProperty("label", page.label());
                obj.addProperty("updated", page.updatedMs());
                obj.addProperty("blob", page.blob());
                root.add(entry.getKey(), obj);
            }
            Files.writeString(configDir.resolve(FILE_NAME), GSON.toJson(root), StandardCharsets.UTF_8);
            dirty = false;
            lastSave = System.currentTimeMillis();
        } catch (IOException e) {
            System.err.println("[SkyBalls] Failed to save storage search cache: " + e.getMessage());
        }
    }

    private static String encode(List<ItemStack> stacks) {
        RegistryAccess registryAccess = registryAccess();
        if (registryAccess == null) return null;

        try {
            HolderLookup.Provider provider = registryAccess;
            var ops = provider.createSerializationContext(NbtOps.INSTANCE);
            ListTag list = new ListTag();

            for (ItemStack stack : stacks) {
                CompoundTag tag = new CompoundTag();
                if (stack != null && !stack.isEmpty()) {
                    Tag encoded = ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);
                    if (encoded instanceof CompoundTag compound) {
                        tag = compound;
                    }
                }
                list.add(tag);
            }

            CompoundTag root = new CompoundTag();
            root.put("items", list);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            NbtIo.writeCompressed(root, out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    /** A saved Ender Chest page or backpack: its items are in menu slot order (navigation slots are empty). */
    public record StoragePage(String type, int number, String label, List<ItemStack> items) {}

    /** Every saved page on the current profile: Ender Chest pages first, then backpacks, by number. */
    public static List<StoragePage> storagePages() {
        List<StoragePage> out = new ArrayList<>();
        for (Map.Entry<String, Page> entry : new ArrayList<>(pages.entrySet())) {
            if (!currentProfile(entry.getKey())) continue;
            Page page = entry.getValue();
            List<ItemStack> items = decode(page.blob());
            if (items != null) out.add(new StoragePage(page.type(), page.number(), page.label(), items));
        }
        out.sort((x, y) -> x.type().equals(y.type()) ? Integer.compare(x.number(), y.number())
            : x.type().equals("ENDER_CHEST") ? -1 : 1);
        return out;
    }

    /** "ENDER_CHEST:3" / "BACKPACK:12" for a storage page menu title, or null. */
    public static String pageKey(String title) {
        StorageTarget target = identify(cleanTitle(title));
        return target == null ? null : target.type() + ":" + target.number();
    }

    /** Item counts by SkyBlock id across the cached Ender Chest and Backpack pages (used by the craft helper). */
    /** Every item on the current profile's saved Ender Chest and backpack pages. */
    public static List<ItemStack> storedStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<String, Page> entry : new ArrayList<>(pages.entrySet())) {
            if (!currentProfile(entry.getKey())) continue;
            List<ItemStack> contents = decode(entry.getValue().blob());
            if (contents == null) continue;
            for (ItemStack stack : contents) if (stack != null && !stack.isEmpty()) stacks.add(stack);
        }
        return stacks;
    }

    public static Map<String, Integer> storedItemCounts() {
        Map<String, Integer> counts = new java.util.HashMap<>();
        for (Map.Entry<String, Page> entry : new ArrayList<>(pages.entrySet())) {
            if (!currentProfile(entry.getKey())) continue;
            Page page = entry.getValue();
            List<ItemStack> contents = decode(page.blob());
            if (contents == null) continue;
            for (ItemStack stack : contents) {
                if (stack == null || stack.isEmpty()) continue;
                String id = com.epic60869.skyballs.custom.util.Compat.neuName(stack);
                if (!id.isEmpty()) counts.merge(id, stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    private static List<ItemStack> decode(String blob) {
        if (blob == null || blob.isEmpty()) return null;
        RegistryAccess registryAccess = registryAccess();
        if (registryAccess == null) return null;

        try {
            HolderLookup.Provider provider = registryAccess;
            var ops = provider.createSerializationContext(NbtOps.INSTANCE);
            byte[] bytes = Base64.getDecoder().decode(blob);
            CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap());
            ListTag list = root.getListOrEmpty("items");
            List<ItemStack> result = new ArrayList<>(list.size());

            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompoundOrEmpty(i);
                result.add(tag.isEmpty()
                        ? ItemStack.EMPTY
                        : ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY));
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static RegistryAccess registryAccess() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) return mc.level.registryAccess();
        return mc.getConnection() != null ? mc.getConnection().registryAccess() : null;
    }

    private static int parseNumber(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}

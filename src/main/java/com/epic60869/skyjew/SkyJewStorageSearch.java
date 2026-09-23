package com.epic60869.skyjew;

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
import org.lwjgl.glfw.GLFW;

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
public final class SkyJewStorageSearch {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "skyjew-storage-search.json";

    private static final Pattern ENDER_CHEST = Pattern.compile("(?i)ender chest\\s*#?\\s*(\\d+)");
    private static final Pattern BACKPACK = Pattern.compile("(?i)backpack\\s*#?\\s*(\\d+)");

    private static final long CAPTURE_INTERVAL_MS = 400L;
    private static final long SAVE_INTERVAL_MS = 1200L;

    private static final Map<String, Page> pages = new LinkedHashMap<>();
    private static Path configDir;
    private static boolean initialized;
    private static long lastCapture;
    private static long lastSave;
    private static boolean dirty;
    private static boolean previousOpenKey;
    private static Result pendingHighlight;

    private record Page(String type, int number, String label, String blob, long updatedMs) {}

    public record Result(ItemStack stack, String name, String id, String lore,
                         String location, String key, String type, int number, int slot) {}

    private SkyJewStorageSearch() {}

    public static void init(Path dir) {
        configDir = dir;
        load();
        initialized = true;
    }

    public static void tick(Minecraft mc) {
        if (!initialized || mc.player == null) return;

        captureOpenStorage(mc);
        applyPendingHighlight(mc);

        boolean ctrl = GLFW.glfwGetKey(mc.getWindow().handle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(mc.getWindow().handle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean f = GLFW.glfwGetKey(mc.getWindow().handle(), GLFW.GLFW_KEY_F) == GLFW.GLFW_PRESS;
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
        mc.gui.setScreen(new SkyJewStorageSearchScreen(parent, query == null ? "" : query));
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

        if (inventory && mc.player != null) {
            var inv = mc.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack == null || stack.isEmpty()) continue;

                SearchText text = searchable(stack);
                if (!q.isEmpty()
                        && !text.name().contains(q)
                        && !text.id().contains(q)
                        && (!lore || !text.lore().contains(q))) {
                    continue;
                }

                results.add(new Result(stack.copy(), text.displayName(), text.id(), text.lore(),
                        inventoryLocation(i), "inventory", "INVENTORY", 0, i));
            }
        }

        results.sort(Comparator
                .comparing((Result r) -> r.name().toLowerCase(Locale.ROOT))
                .thenComparing(Result::location));
        return results;
    }

    public static int cachedStorageCount() {
        return pages.size();
    }

    public static long oldestCacheAgeMs() {
        if (pages.isEmpty()) return -1L;
        long oldest = Long.MAX_VALUE;
        for (Page page : pages.values()) {
            oldest = Math.min(oldest, page.updatedMs());
        }
        return Math.max(0L, System.currentTimeMillis() - oldest);
    }

    public static void openResult(Minecraft mc, Result result) {
        pendingHighlight = result;
        if ("INVENTORY".equals(result.type())) {
            mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            return;
        }
        if (result.type().equals("ENDER_CHEST")) {
            mc.gui.setScreen(null);
            if (mc.player != null && mc.player.connection != null) {
                mc.player.connection.sendCommand("enderchest " + result.number());
            }
        } else if (result.type().equals("BACKPACK")) {
            mc.gui.setScreen(null);
            if (mc.player != null && mc.player.connection != null) {
                mc.player.connection.sendCommand("backpack " + result.number());
            }
        }
    }

    private static void applyPendingHighlight(Minecraft mc) {
        Result result = pendingHighlight;
        if (result == null || mc.gui.screen() == null) return;

        if ("INVENTORY".equals(result.type())) {
            if (!(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) return;
            Slot target = null;
            for (Slot slot : screen.getMenu().slots) {
                if (slot.getContainerSlot() == result.slot()) {
                    target = slot;
                    break;
                }
            }
            if (target == null) return;
            moveCursorToSlot(mc, screen, target);
            pendingHighlight = null;
            return;
        }

        if (!(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) return;
        String title = cleanTitle(screen.getTitle().getString());
        boolean matching = result.type().equals("ENDER_CHEST")
            ? title.contains("ender chest")
            : title.contains("backpack");
        if (!matching) return;

        if (result.slot() >= 0 && result.slot() < screen.getMenu().slots.size()) {
            moveCursorToSlot(mc, screen, screen.getMenu().slots.get(result.slot()));
            pendingHighlight = null;
        }
    }

    private static void moveCursorToSlot(Minecraft mc,
                                         net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen,
                                         Slot slot) {
        double scale = mc.getWindow().getGuiScale();
        double x = (screen.getGuiLeft() + slot.x + 8) * scale;
        double y = (screen.getGuiTop() + slot.y + 8) * scale;
        GLFW.glfwSetCursorPos(mc.getWindow().handle(), x, y);
    }

    private static String cleanTitle(String title) {
        return title.replaceAll("§[0-9A-FK-ORa-fk-or]", "")
            .replaceAll("\\s+", " ")
            .trim().toLowerCase(Locale.ROOT);
    }

    private static void captureOpenStorage(Minecraft mc) {
        if (!isHypixel(mc)) return;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> container)) return;
        if (!(container.getMenu() instanceof ChestMenu)) return;

        long now = System.currentTimeMillis();
        if (now - lastCapture < CAPTURE_INTERVAL_MS) return;
        lastCapture = now;

        StorageTarget target = identify(cleanTitle(container.getTitle().getString()));
        if (target == null) return;

        List<Slot> slots = container.getMenu().slots;
        int count = Math.max(0, slots.size() - 36);
        if (count <= 0) return;

        List<ItemStack> contents = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = slots.get(i).getItem();
            contents.add(stack == null ? ItemStack.EMPTY : stack.copy());
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
        String normalized = title == null ? "" : title.trim();
        Matcher ender = ENDER_CHEST.matcher(normalized);
        if (ender.find()) {
            int number = parseNumber(ender.group(1));
            return number > 0 ? new StorageTarget("ENDER_CHEST", number, "Ender Chest #" + number) : null;
        }

        Matcher backpack = BACKPACK.matcher(normalized);
        if (backpack.find()) {
            int number = parseNumber(backpack.group(1));
            return number > 0 ? new StorageTarget("BACKPACK", number, "Backpack #" + number) : null;
        }
        return null;
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

    private static String cacheKey(Minecraft mc, String type, int number) {
        String server = "unknown";
        try {
            if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
                server = mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
            }
        } catch (Throwable ignored) {}
        return server + "|" + type + "|" + number;
    }

    private static void load() {
        pages.clear();
        if (configDir == null) return;

        Path file = configDir.resolve(FILE_NAME);
        if (!Files.exists(file)) return;

        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : root.keySet()) {
                JsonObject obj = root.getAsJsonObject(key);
                if (obj == null || !obj.has("blob")) continue;
                String type = obj.has("type") ? obj.get("type").getAsString() : "";
                int number = obj.has("number") ? obj.get("number").getAsInt() : 0;
                String label = obj.has("label") ? obj.get("label").getAsString() : type + " #" + number;
                long updated = obj.has("updated") ? obj.get("updated").getAsLong() : 0L;
                pages.put(key, new Page(type, number, label, obj.get("blob").getAsString(), updated));
            }
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to load storage search cache: " + e.getMessage());
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
            Files.writeString(configDir.resolve(FILE_NAME), GSON.toJson(root), StandardCharsets.UTF_8);
            dirty = false;
            lastSave = System.currentTimeMillis();
        } catch (IOException e) {
            System.err.println("[SkyJew] Failed to save storage search cache: " + e.getMessage());
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

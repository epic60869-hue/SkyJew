package com.epic60869.skyjew;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * SkyJew implementation of Skyblocker's /skyblocker custom item/armour
 * customization features. Everything is client-side and keyed to the item's
 * Hypixel UUID, so the server item itself is never modified.
 */
public final class SkyJewCustom {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "skyjew-custom.json";

    private static final Map<String, String> ITEM_NAMES = new LinkedHashMap<>();
    private static final Map<String, Integer> DYE_COLORS = new LinkedHashMap<>();
    private static final Map<String, TrimId> ARMOR_TRIMS = new LinkedHashMap<>();
    private static final Map<String, AnimatedDye> ANIMATED_DYES = new LinkedHashMap<>();
    private static final Map<String, Integer> HYPIXEL_STATIC_DYES = new LinkedHashMap<>();
    private static final Map<String, List<Integer>> HYPIXEL_ANIMATED_DYES = new LinkedHashMap<>();
    private static volatile boolean dyeDataLoaded;
    private static final Map<String, String> ITEM_MODELS = new LinkedHashMap<>();
    private static final Map<String, Boolean> ITEM_GLINTS = new LinkedHashMap<>();
    private static final Map<String, String> HELMET_SKINS = new LinkedHashMap<>();
    private static final List<HelmetSkin> AVAILABLE_HELMET_SKINS = new ArrayList<>();
    private static final Map<String, net.minecraft.world.item.component.ResolvableProfile> HELMET_PROFILE_CACHE = new LinkedHashMap<>();
    private static volatile boolean helmetSkinDataLoaded;

    private static Path configDir;
    private static boolean initialized;
    private static long animationTicks;

    private SkyJewCustom() {}

    public record TrimId(String material, String pattern) {}
    public record Keyframe(int color, float time) {}
    public record AnimatedDye(List<Keyframe> keyframes, boolean cycleBack, float duration, float delay) {}
    public record HelmetSkin(String id, String name, String texture) {}

    public static void init(Path dir) {
        configDir = dir;
        load();
        loadHypixelDyes();
        loadHelmetSkins();
        initialized = true;
    }

    public static void tick(Minecraft mc) {
        if (!initialized) return;
        animationTicks++;
    }

    /** Opens SkyJew's standalone port of the Skyblocker customisation workflow. */
    public static void open(Minecraft mc, Screen parent) {
        mc.execute(() -> mc.gui.setScreen(new SkyJewCustomScreen(parent)));
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

    public static ItemStack held(Minecraft mc) {
        return mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
    }

    public static String uuid(ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) return "";

            // Hypixel stores the item UUID in the item's CustomData component.
            // Keep this lookup compatible with the mappings used by this build.
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null) {
                String legacy = data.copyTag().getStringOr("uuid", "");
                if (!legacy.isBlank()) return legacy;
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    public static boolean hasUuid(ItemStack stack) {
        return !uuid(stack).isBlank();
    }

    public static void setName(ItemStack stack, String name) {
        String id = uuid(stack);
        if (id.isBlank()) return;
        if (name == null || name.isBlank()) ITEM_NAMES.remove(id);
        else ITEM_NAMES.put(id, name);
        save();
    }

    public static String getName(ItemStack stack) {
        return ITEM_NAMES.get(uuid(stack));
    }

    public static void setDye(ItemStack stack, Integer color) {
        String id = uuid(stack);
        if (id.isBlank()) return;
        if (color == null) DYE_COLORS.remove(id);
        else DYE_COLORS.put(id, color & 0xFFFFFF);
        save();
    }

    public static Integer getDye(ItemStack stack) {
        return DYE_COLORS.get(uuid(stack));
    }

    public static void setTrim(ItemStack stack, String material, String pattern) {
        String id = uuid(stack);
        if (id.isBlank()) return;
        if (material == null || material.isBlank() || pattern == null || pattern.isBlank()) {
            ARMOR_TRIMS.remove(id);
        } else {
            ARMOR_TRIMS.put(id, new TrimId(normalizeId(material), normalizeId(pattern)));
        }
        save();
    }

    public static TrimId getTrim(ItemStack stack) {
        return ARMOR_TRIMS.get(uuid(stack));
    }

    public static void setAnimatedDye(ItemStack stack, Integer color1, Integer color2,
                                       float duration, boolean cycleBack, float delay) {
        String id = uuid(stack);
        if (id.isBlank()) return;
        if (color1 == null || color2 == null) {
            ANIMATED_DYES.remove(id);
        } else {
            setAnimatedDye(stack, List.of(color1, color2), duration, cycleBack, delay);
            return;
        }
        save();
    }

    public static AnimatedDye getAnimatedDye(ItemStack stack) {
        return ANIMATED_DYES.get(uuid(stack));
    }

    public static void setAnimatedDye(ItemStack stack, List<Integer> colors, float duration, boolean cycleBack, float delay) {
        String id = uuid(stack);
        if (id.isBlank()) return;
        if (colors == null || colors.size() < 2) {
            ANIMATED_DYES.remove(id);
        } else {
            List<Keyframe> frames = new ArrayList<>(colors.size());
            int max = cycleBack && colors.size() % 2 == 0 ? colors.size() / 2 : colors.size() - 1;
            max = Math.max(1, max);
            for (int i = 0; i < colors.size(); i++) {
                if (cycleBack && colors.size() % 2 == 0 && i > max) break;
                float time = i == max ? 1f : Math.min(1f, (float) i / max);
                frames.add(new Keyframe(colors.get(i) & 0xFFFFFF, time));
            }
            if (frames.size() < 2) frames.add(new Keyframe(colors.getLast() & 0xFFFFFF, 1f));
            ANIMATED_DYES.put(id, new AnimatedDye(List.copyOf(frames), cycleBack,
                Math.max(0.1f, duration), Math.max(0f, delay)));
        }
        save();
    }

    public static void setAnimatedDyeKeyframes(ItemStack stack, List<Keyframe> keyframes, float duration, boolean cycleBack, float delay) {
        String id = uuid(stack);
        if (id.isBlank()) return;
        if (keyframes == null || keyframes.size() < 2) {
            ANIMATED_DYES.remove(id);
        } else {
            List<Keyframe> copy = new ArrayList<>(keyframes);
            copy.sort(java.util.Comparator.comparingDouble(Keyframe::time));
            ANIMATED_DYES.put(id, new AnimatedDye(List.copyOf(copy), cycleBack, Math.max(0.1f, duration), Math.max(0f, delay)));
        }
        save();
    }

    public static Map<String, Integer> hypixelStaticDyes() {
        return HYPIXEL_STATIC_DYES;
    }

    public static Map<String, List<Integer>> hypixelAnimatedDyes() {
        return HYPIXEL_ANIMATED_DYES;
    }

    public static boolean dyeDataLoaded() {
        return dyeDataLoaded;
    }

    private static void loadHypixelDyes() {
        CompletableFuture.runAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/dyes.json"))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .header("User-Agent", "SkyJew/1.0")
                    .GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return;
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

                Map<String, Integer> statics = new LinkedHashMap<>();
                JsonObject staticObject = root.getAsJsonObject("static");
                if (staticObject != null) {
                    staticObject.entrySet().forEach(e -> statics.put(e.getKey(), parseHex(e.getValue().getAsString())));
                }

                Map<String, List<Integer>> animated = new LinkedHashMap<>();
                JsonObject animatedObject = root.getAsJsonObject("animated");
                if (animatedObject != null) {
                    animatedObject.entrySet().forEach(e -> {
                        if (!e.getValue().isJsonArray()) return;
                        List<Integer> colors = new ArrayList<>();
                        e.getValue().getAsJsonArray().forEach(v -> colors.add(parseHex(v.getAsString())));
                        if (colors.size() >= 2) animated.put(e.getKey(), List.copyOf(colors));
                    });
                }

                synchronized (HYPIXEL_STATIC_DYES) {
                    HYPIXEL_STATIC_DYES.clear();
                    HYPIXEL_STATIC_DYES.putAll(statics);
                }
                synchronized (HYPIXEL_ANIMATED_DYES) {
                    HYPIXEL_ANIMATED_DYES.clear();
                    HYPIXEL_ANIMATED_DYES.putAll(animated);
                }
                dyeDataLoaded = true;
            } catch (Exception e) {
                System.err.println("[SkyJew] Failed to load Hypixel dye data: " + e.getMessage());
            }
        });
    }

    public static String dyeDisplayName(String id) {
        String name = id.replace('_', ' ');
        if (name.startsWith("DYE ")) name = name.substring(4);
        if (name.startsWith("TENTACLE ")) name = name.substring(0, 1) + name.substring(1).toLowerCase(Locale.ROOT);
        String[] words = name.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    public static void setItemModel(ItemStack stack, String model) {
        String id = uuid(stack);
        if (id.isBlank()) return;
        if (model == null || model.isBlank()) ITEM_MODELS.remove(id);
        else ITEM_MODELS.put(id, model.trim());
        save();
    }

    public static String getItemModel(ItemStack stack) {
        return ITEM_MODELS.get(uuid(stack));
    }

    public static void setItemIcon(ItemStack stack, String itemId) {
        String id = uuid(stack);
        if (id.isBlank()) return;
        if (itemId == null || itemId.isBlank()) ITEM_MODELS.remove(id);
        else ITEM_MODELS.put(id, itemId.trim().toLowerCase(Locale.ROOT));
        save();
    }

    public static Identifier getItemIcon(ItemStack stack) {
        String value = getItemModel(stack);
        if (value == null || value.isBlank()) return null;
        try {
            return Identifier.parse(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void setGlint(ItemStack stack, Boolean enabled) {
        String id = uuid(stack);
        if (id.isBlank()) return;
        if (enabled == null) ITEM_GLINTS.remove(id);
        else ITEM_GLINTS.put(id, enabled);
        save();
    }

    public static Boolean getGlint(ItemStack stack) {
        return ITEM_GLINTS.get(uuid(stack));
    }

    public static boolean customGlint(ItemStack stack, boolean original) {
        Boolean value = getGlint(stack);
        return value == null ? original : value;
    }

    public static Component customName(ItemStack stack, Component original) {
        String value = getName(stack);
        if (value == null || value.isBlank()) return original;
        try {
            if (value.trim().startsWith("{")) {
                try {
                    Component parsed = net.minecraft.network.chat.ComponentSerialization.CODEC
                            .parse(com.mojang.serialization.JsonOps.INSTANCE, JsonParser.parseString(value))
                            .result().orElse(null);
                    if (parsed != null) return parsed;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return Component.literal(value);
    }

    public static int customDye(ItemStack stack, int original) {
        AnimatedDye animated = getAnimatedDye(stack);
        if (animated != null) return 0xFF000000 | (animate(animated) & 0xFFFFFF);
        Integer color = getDye(stack);
        return color == null ? original : 0xFF000000 | (color & 0xFFFFFF);
    }

    public static void setHelmetSkin(ItemStack stack, String texture) {
        String id = uuid(stack);
        if (id.isBlank()) return;
        if (texture == null || texture.isBlank()) HELMET_SKINS.remove(id);
        else HELMET_SKINS.put(id, texture);
        save();
    }

    public static String getHelmetSkin(ItemStack stack) {
        return HELMET_SKINS.get(uuid(stack));
    }

    public static boolean helmetSkinDataLoaded() {
        return helmetSkinDataLoaded;
    }

    public static List<HelmetSkin> helmetSkins() {
        synchronized (AVAILABLE_HELMET_SKINS) {
            return List.copyOf(AVAILABLE_HELMET_SKINS);
        }
    }

    public static net.minecraft.world.item.component.ResolvableProfile helmetSkinProfile(ItemStack stack) {
        return helmetSkinProfile(getHelmetSkin(stack));
    }

    public static net.minecraft.world.item.component.ResolvableProfile helmetSkinProfile(String texture) {
        if (texture == null || texture.isBlank()) return null;
        synchronized (HELMET_PROFILE_CACHE) {
            net.minecraft.world.item.component.ResolvableProfile cached = HELMET_PROFILE_CACHE.get(texture);
            if (cached != null) return cached;
            try {
                com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(
                    UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8)), "skyjew",
                    net.minecraft.util.ExtraCodecs.PROPERTY_MAP.parse(
                        com.mojang.serialization.JsonOps.INSTANCE,
                        JsonParser.parseString("[{\"name\":\"textures\",\"value\":\"" + texture + "\"}]")
                    ).getOrThrow());
                net.minecraft.world.item.component.ResolvableProfile resolved =
                    net.minecraft.world.item.component.ResolvableProfile.createResolved(profile);
                HELMET_PROFILE_CACHE.put(texture, resolved);
                return resolved;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    public static ItemStack createHelmetSkinStack(String texture) {
        ItemStack stack = new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
        net.minecraft.world.item.component.ResolvableProfile profile = helmetSkinProfile(texture);
        if (profile != null) stack.set(DataComponents.PROFILE, profile);
        return stack;
    }

    private static void loadHelmetSkins() {
        CompletableFuture.runAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.hypixel.net/v2/resources/skyblock/items"))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .header("User-Agent", "SkyJew/1.0 (helmet skin selector)")
                    .GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return;

                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (!root.has("items") || !root.get("items").isJsonArray()) return;

                Map<String, HelmetSkin> found = new LinkedHashMap<>();
                root.getAsJsonArray("items").forEach(element -> {
                    if (!element.isJsonObject()) return;
                    JsonObject item = element.getAsJsonObject();
                    String material = item.has("material") ? item.get("material").getAsString() : "";
                    String texture = item.has("skin") ? item.get("skin").getAsString() : "";
                    String category = item.has("category") ? item.get("category").getAsString() : "";
                    String id = item.has("id") ? item.get("id").getAsString() : "";
                    String name = item.has("name") ? item.get("name").getAsString() : id;

                    boolean skull = material.equalsIgnoreCase("SKULL_ITEM")
                        || material.equalsIgnoreCase("PLAYER_HEAD");
                    String upperCategory = category.toUpperCase(Locale.ROOT);
                    boolean helmetLike = upperCategory.contains("HELMET")
                        || upperCategory.equals("HAT")
                        || upperCategory.equals("MASK")
                        || upperCategory.equals("HEAD");
                    if (!skull || texture.isBlank() || !helmetLike) return;

                    found.putIfAbsent(texture, new HelmetSkin(id, name, texture));
                });

                List<HelmetSkin> sorted = new ArrayList<>(found.values());
                sorted.sort(java.util.Comparator.comparing(HelmetSkin::name, String.CASE_INSENSITIVE_ORDER));
                synchronized (AVAILABLE_HELMET_SKINS) {
                    AVAILABLE_HELMET_SKINS.clear();
                    AVAILABLE_HELMET_SKINS.addAll(sorted);
                }
                helmetSkinDataLoaded = true;
            } catch (Exception e) {
                System.err.println("[SkyJew] Failed to load Hypixel helmet skins: " + e.getMessage());
            }
        });
    }

    public static ArmorTrim customTrim(ItemStack stack, ArmorTrim original) {
        TrimId id = getTrim(stack);
        if (id == null) return original;
        ArmorTrim trim = resolveTrim(id);
        return trim == null ? original : trim;
    }

    private static ArmorTrim resolveTrim(TrimId id) {
        Minecraft mc = Minecraft.getInstance();
        RegistryAccess access = mc.level != null ? mc.level.registryAccess()
                : mc.getConnection() != null ? mc.getConnection().registryAccess() : null;
        if (access == null) return null;

        try {
            HolderLookup.RegistryLookup<TrimMaterial> materials = access.lookupOrThrow(Registries.TRIM_MATERIAL);
            HolderLookup.RegistryLookup<TrimPattern> patterns = access.lookupOrThrow(Registries.TRIM_PATTERN);

            Identifier materialId = Identifier.parse(id.material());
            Identifier patternId = Identifier.parse(id.pattern());

            Holder<TrimMaterial> material = materials.get(ResourceKey.create(Registries.TRIM_MATERIAL, materialId)).orElse(null);
            Holder<TrimPattern> pattern = patterns.get(ResourceKey.create(Registries.TRIM_PATTERN, patternId)).orElse(null);
            if (material == null || pattern == null) return null;
            return new ArmorTrim(material, pattern);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int animate(AnimatedDye dye) {
        double seconds = animationTicks / 20.0;
        double delay = dye.delay();
        double duration = Math.max(0.1, dye.duration());

        double t;
        if (seconds < delay) {
            t = 0;
        } else {
            double progress = (seconds - delay) / duration;
            if (dye.cycleBack()) {
                double cycle = progress % 2.0;
                t = cycle <= 1.0 ? cycle : 2.0 - cycle;
            } else {
                t = progress % 1.0;
            }
        }

        List<Keyframe> frames = dye.keyframes();
        if (frames.size() < 2) return frames.isEmpty() ? 0 : frames.getFirst().color();
        Keyframe current = frames.getFirst();
        Keyframe next = frames.getLast();
        for (int i = 0; i < frames.size() - 1; i++) {
            if (t >= frames.get(i).time() && t <= frames.get(i + 1).time()) {
                current = frames.get(i);
                next = frames.get(i + 1);
                break;
            }
        }
        float local = next.time() <= current.time() ? 0f :
            (float) ((t - current.time()) / (next.time() - current.time()));
        return interpolateOkLab(current.color(), next.color(), Math.max(0f, Math.min(1f, local)));
    }

    // Skyblocker's animated dye uses perceptual OKLab interpolation rather than
    // a simple RGB blend. This compact conversion keeps the same visual effect.
    private static int interpolateOkLab(int a, int b, float t) {
        double[] la = rgbToLab(a);
        double[] lb = rgbToLab(b);
        double l = la[0] + (lb[0] - la[0]) * t;
        double aa = la[1] + (lb[1] - la[1]) * t;
        double bb = la[2] + (lb[2] - la[2]) * t;
        return labToRgb(l, aa, bb);
    }

    private static double[] rgbToLab(int rgb) {
        double r = pivot(((rgb >> 16) & 255) / 255.0);
        double g = pivot(((rgb >> 8) & 255) / 255.0);
        double b = pivot((rgb & 255) / 255.0);
        double x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047;
        double y = (r * 0.2126 + g * 0.7152 + b * 0.0722);
        double z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883;
        x = labPivot(x); y = labPivot(y); z = labPivot(z);
        return new double[]{116 * y - 16, 500 * (x - y), 200 * (y - z)};
    }

    private static int labToRgb(double l, double a, double b) {
        double y = (l + 16) / 116.0;
        double x = a / 500.0 + y;
        double z = y - b / 200.0;
        x = labInverse(x) * 0.95047;
        y = labInverse(y);
        z = labInverse(z) * 1.08883;
        double r = x * 3.2406 + y * -1.5372 + z * -0.4986;
        double g = x * -0.9689 + y * 1.8758 + z * 0.0415;
        double bl = x * 0.0557 + y * -0.2040 + z * 1.0570;
        r = gamma(r); g = gamma(g); bl = gamma(bl);
        return ((int)(clamp(r) * 255) << 16) | ((int)(clamp(g) * 255) << 8) | (int)(clamp(bl) * 255);
    }

    private static double pivot(double n) { return n > 0.04045 ? Math.pow((n + 0.055) / 1.055, 2.4) : n / 12.92; }
    private static double gamma(double n) { return n <= 0.0031308 ? 12.92 * n : 1.055 * Math.pow(Math.max(0, n), 1 / 2.4) - 0.055; }
    private static double labPivot(double n) { return n > 0.008856 ? Math.cbrt(n) : 7.787 * n + 16.0 / 116.0; }
    private static double labInverse(double n) { double n3=n*n*n; return n3 > 0.008856 ? n3 : (n - 16.0/116.0)/7.787; }
    private static double clamp(double n) { return Math.max(0, Math.min(1, n)); }

    private static String normalizeId(String id) {
        id = id.trim().toLowerCase(Locale.ROOT);
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static void load() {
        ITEM_NAMES.clear(); DYE_COLORS.clear(); ARMOR_TRIMS.clear(); ANIMATED_DYES.clear(); ITEM_MODELS.clear(); ITEM_GLINTS.clear(); HELMET_SKINS.clear();
        if (configDir == null) return;
        Path file = configDir.resolve(FILE_NAME);
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();

            if (root.has("itemNames")) root.getAsJsonObject("itemNames").entrySet().forEach(e -> ITEM_NAMES.put(e.getKey(), e.getValue().getAsString()));
            if (root.has("dyeColors")) root.getAsJsonObject("dyeColors").entrySet().forEach(e -> DYE_COLORS.put(e.getKey(), e.getValue().getAsInt()));
            if (root.has("armorTrims")) root.getAsJsonObject("armorTrims").entrySet().forEach(e -> {
                JsonObject v = e.getValue().getAsJsonObject();
                ARMOR_TRIMS.put(e.getKey(), new TrimId(v.get("material").getAsString(), v.get("pattern").getAsString()));
            });
            if (root.has("itemModels")) root.getAsJsonObject("itemModels").entrySet().forEach(e -> ITEM_MODELS.put(e.getKey(), e.getValue().getAsString()));
            if (root.has("itemGlints")) root.getAsJsonObject("itemGlints").entrySet().forEach(e -> ITEM_GLINTS.put(e.getKey(), e.getValue().getAsBoolean()));
            if (root.has("helmetSkins")) root.getAsJsonObject("helmetSkins").entrySet().forEach(e -> HELMET_SKINS.put(e.getKey(), e.getValue().getAsString()));
            if (root.has("animatedDyes")) root.getAsJsonObject("animatedDyes").entrySet().forEach(e -> {
                JsonObject v = e.getValue().getAsJsonObject();
                List<Keyframe> frames = new ArrayList<>();
                if (v.has("keyframes") && v.get("keyframes").isJsonArray()) {
                    v.getAsJsonArray("keyframes").forEach(frame -> {
                        JsonObject f = frame.getAsJsonObject();
                        frames.add(new Keyframe(f.get("color").getAsInt(), f.get("time").getAsFloat()));
                    });
                } else if (v.has("first") && v.has("second")) {
                    JsonObject a = v.getAsJsonObject("first");
                    JsonObject b = v.getAsJsonObject("second");
                    frames.add(new Keyframe(a.get("color").getAsInt(), a.get("time").getAsFloat()));
                    frames.add(new Keyframe(b.get("color").getAsInt(), b.get("time").getAsFloat()));
                }
                if (frames.size() >= 2) {
                    ANIMATED_DYES.put(e.getKey(), new AnimatedDye(List.copyOf(frames),
                        v.get("cycleBack").getAsBoolean(),
                        v.get("duration").getAsFloat(),
                        v.get("delay").getAsFloat()));
                }
            });
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to load custom item config: " + e.getMessage());
        }
    }

    public static void save() {
        if (configDir == null) return;
        try {
            Files.createDirectories(configDir);
            JsonObject root = new JsonObject();
            JsonObject names = new JsonObject();
            ITEM_NAMES.forEach(names::addProperty);
            root.add("itemNames", names);
            JsonObject dyes = new JsonObject();
            DYE_COLORS.forEach(dyes::addProperty);
            root.add("dyeColors", dyes);
            JsonObject trims = new JsonObject();
            ARMOR_TRIMS.forEach((k,v) -> {
                JsonObject o = new JsonObject();
                o.addProperty("material", v.material());
                o.addProperty("pattern", v.pattern());
                trims.add(k,o);
            });
            root.add("armorTrims", trims);
            JsonObject animated = new JsonObject();
            ANIMATED_DYES.forEach((k,v) -> {
                JsonObject o = new JsonObject();
                com.google.gson.JsonArray frames = new com.google.gson.JsonArray();
                v.keyframes().forEach(frame -> {
                    JsonObject f = new JsonObject();
                    f.addProperty("color", frame.color());
                    f.addProperty("time", frame.time());
                    frames.add(f);
                });
                o.add("keyframes", frames);
                o.addProperty("cycleBack",v.cycleBack());
                o.addProperty("duration",v.duration());
                o.addProperty("delay",v.delay());
                animated.add(k,o);
            });
            root.add("animatedDyes", animated);
            JsonObject models = new JsonObject();
            ITEM_MODELS.forEach(models::addProperty);
            root.add("itemModels", models);
            JsonObject glints = new JsonObject();
            ITEM_GLINTS.forEach(glints::addProperty);
            root.add("itemGlints", glints);
            JsonObject helmetSkins = new JsonObject();
            HELMET_SKINS.forEach(helmetSkins::addProperty);
            root.add("helmetSkins", helmetSkins);
            Files.writeString(configDir.resolve(FILE_NAME), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[SkyJew] Failed to save custom config: " + e.getMessage());
        }
    }

    public static int parseHex(String value) {
        String s = value.trim().replace("#", "");
        if (s.length() != 6) throw new IllegalArgumentException("HEX colours must be 6 digits.");
        return Integer.parseInt(s, 16);
    }

    public static String describe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "Nothing held";
        return stack.getHoverName().getString() + "  [" + uuid(stack) + "]";
    }

    public static void clearAll(ItemStack stack) {
        String id = uuid(stack);
        if (id.isBlank()) return;
        ITEM_NAMES.remove(id); DYE_COLORS.remove(id); ARMOR_TRIMS.remove(id); ANIMATED_DYES.remove(id); ITEM_GLINTS.remove(id); ITEM_MODELS.remove(id); HELMET_SKINS.remove(id);
        save();
    }
}

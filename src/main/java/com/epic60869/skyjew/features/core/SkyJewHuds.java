package com.epic60869.skyjew.features.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Registry for SkyJew's text HUD elements. Each element supplies its lines; this class draws
 * them with a fitted background and stores position and scale in {@code skyjew-huds.json}.
 * Every registered element appears in the /sj gui editor.
 */
public final class SkyJewHuds {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Identifier ID = Identifier.fromNamespaceAndPath("skyjew", "feature_huds");
    public static final int PADDING = 3;
    public static final int LINE_HEIGHT = 10;

    private static final Map<String, Element> ELEMENTS = new LinkedHashMap<>();
    private static final Map<String, Placement> PLACEMENTS = new LinkedHashMap<>();
    private static Path file;
    private static final String BACKGROUNDS_OFF = "#backgroundsOff";

    public static final class Placement {
        public int x;
        public int y;
        public float scale = 1f;
        /** Draw the dark background behind the HUD. Toggled by right-clicking it in /sj gui. */
        public boolean background = false;
    }

    /** A HUD element that draws itself instead of drawing text lines. Sizes are unscaled. */
    public interface CustomHud {
        int width();
        int height();
        /** Whether there is anything to show outside the editor. */
        boolean visible();
        void render(GuiGraphicsExtractor graphics, boolean preview);
    }

    public record Element(String id, String name, BooleanSupplier enabled, Supplier<List<Component>> lines,
                          List<Component> preview, int defaultX, int defaultY, CustomHud custom) {}

    private SkyJewHuds() {}

    public static void init(Path configDir) {
        file = configDir.resolve("skyjew-huds.json");
        load();
        HudElementRegistry.addLast(ID, (graphics, delta) -> renderAll(graphics));
    }

    /**
     * Registers a text HUD.
     *
     * @param lines   current lines; an empty list hides the element
     * @param preview sample lines shown in the /sj gui editor when there is no live data
     */
    public static void register(String id, String name, BooleanSupplier enabled, Supplier<List<Component>> lines,
                                List<Component> preview, int defaultX, int defaultY) {
        ELEMENTS.put(id, new Element(id, name, enabled, lines, preview, defaultX, defaultY, null));
        defaultPlacement(id, defaultX, defaultY);
    }

    /** Registers a HUD element that draws itself. */
    public static void registerCustom(String id, String name, BooleanSupplier enabled, CustomHud hud, int defaultX, int defaultY) {
        ELEMENTS.put(id, new Element(id, name, enabled, List::of, List.of(), defaultX, defaultY, hud));
        defaultPlacement(id, defaultX, defaultY);
    }

    private static void defaultPlacement(String id, int defaultX, int defaultY) {
        PLACEMENTS.computeIfAbsent(id, k -> {
            Placement p = new Placement();
            p.x = defaultX;
            p.y = defaultY;
            return p;
        });
    }

    public static List<Element> elements() {
        return new ArrayList<>(ELEMENTS.values());
    }

    public static Placement placement(String id) {
        return PLACEMENTS.computeIfAbsent(id, k -> new Placement());
    }

    private static void renderAll(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (Element element : ELEMENTS.values()) {
            if (!safe(element.enabled())) continue;
            if (element.custom() != null) {
                try {
                    if (element.custom().visible()) renderCustom(graphics, element, false);
                } catch (Exception e) {
                    System.err.println("[SkyJew] HUD " + element.id() + " failed: " + e);
                }
                continue;
            }
            List<Component> lines = safeLines(element);
            if (lines.isEmpty()) continue;
            Placement p = placement(element.id());
            render(graphics, lines, clampX(p.x, width(lines, p.scale)), clampY(p.y, height(lines, p.scale)), p.scale, p.background);
        }
    }

    private static boolean safe(BooleanSupplier supplier) {
        try {
            return supplier.getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private static List<Component> safeLines(Element element) {
        try {
            List<Component> lines = element.lines().get();
            return lines == null ? List.of() : lines;
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Where a text HUD with these lines is drawn on screen, after keeping it on screen: {x, y}. */
    public static int[] screenPosition(String id, List<Component> lines) {
        Placement p = placement(id);
        return new int[]{clampX(p.x, width(lines, p.scale)), clampY(p.y, height(lines, p.scale))};
    }

    /** Keeps a HUD on screen even if its saved position is past the edge (e.g. after a window or GUI scale change). */
    private static int clampX(int x, int w) {
        return Math.max(0, Math.min(x, Minecraft.getInstance().getWindow().getGuiScaledWidth() - w));
    }

    private static int clampY(int y, int h) {
        return Math.max(0, Math.min(y, Minecraft.getInstance().getWindow().getGuiScaledHeight() - h));
    }

    public static void renderCustom(GuiGraphicsExtractor graphics, Element element, boolean preview) {
        Placement p = placement(element.id());
        int x = clampX(p.x, Math.round(element.custom().width() * p.scale));
        int y = clampY(p.y, Math.round(element.custom().height() * p.scale));
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().scale(p.scale, p.scale);
        element.custom().render(graphics, preview);
        graphics.pose().popMatrix();
    }

    /** Scaled on-screen width of an element as shown in the editor. */
    public static int editorWidth(Element element) {
        float scale = placement(element.id()).scale;
        if (element.custom() != null) return Math.round(element.custom().width() * scale);
        return width(editorLines(element), scale);
    }

    /** Scaled on-screen height of an element as shown in the editor. */
    public static int editorHeight(Element element) {
        float scale = placement(element.id()).scale;
        if (element.custom() != null) return Math.round(element.custom().height() * scale);
        return height(editorLines(element), scale);
    }

    /** Lines to use for sizing and previewing in the editor. */
    public static List<Component> editorLines(Element element) {
        List<Component> live = safeLines(element);
        return live.isEmpty() ? element.preview() : live;
    }

    public static int width(List<Component> lines, float scale) {
        int w = 0;
        for (Component line : lines) w = Math.max(w, Minecraft.getInstance().font.width(line));
        return Math.round((w + PADDING * 2) * scale);
    }

    public static int height(List<Component> lines, float scale) {
        return Math.round((lines.size() * LINE_HEIGHT + PADDING * 2 - 2) * scale);
    }

    public static void render(GuiGraphicsExtractor graphics, List<Component> lines, int x, int y, float scale) {
        render(graphics, lines, x, y, scale, true);
    }

    public static void render(GuiGraphicsExtractor graphics, List<Component> lines, int x, int y, float scale, boolean background) {
        var font = Minecraft.getInstance().font;
        int w = 0;
        for (Component line : lines) w = Math.max(w, font.width(line));
        int h = lines.size() * LINE_HEIGHT + PADDING * 2 - 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().scale(scale, scale);
        if (background) graphics.fill(0, 0, w + PADDING * 2, h, 0x80000000);
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(font, lines.get(i), PADDING, PADDING + i * LINE_HEIGHT, 0xFFFFFFFF, true);
        }
        graphics.pose().popMatrix();
    }

    public static void toggleBackground(String id) {
        Placement p = placement(id);
        p.background = !p.background;
    }

    public static void changeScale(String id, float delta) {
        Placement p = placement(id);
        p.scale = Math.max(0.5f, Math.min(3f, Math.round((p.scale + delta) * 10f) / 10f));
    }

    private static void load() {
        if (file == null || !Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : root.entrySet()) {
                if (entry.getKey().startsWith("#")) continue;
                PLACEMENTS.put(entry.getKey(), GSON.fromJson(entry.getValue(), Placement.class));
            }
            // Backgrounds became opt-in; turn off the ones saved before that, once.
            if (!root.has(BACKGROUNDS_OFF)) {
                for (Placement p : PLACEMENTS.values()) p.background = false;
                com.epic60869.skyjew.SkyJewConfig config = com.epic60869.skyjew.SkyJewConfig.current();
                if (config != null) {
                    config.mining.commissions.background = false;
                    config.farming.rng.background = false;
                    com.epic60869.skyjew.SkyJewConfig.saveCurrent(config);
                }
                save();
            }
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to load HUD positions: " + e.getMessage());
        }
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = GSON.toJsonTree(PLACEMENTS).getAsJsonObject();
            root.addProperty(BACKGROUNDS_OFF, true);
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to save HUD positions: " + e.getMessage());
        }
    }
}

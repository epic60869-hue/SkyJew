package com.epic60869.skyjew.features.combat;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.features.core.SkyJewChat;
import com.epic60869.skyjew.features.core.SkyJewHuds;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Zealot tracker in the style of SkyHanni's trackers: kills and Summoning Eyes, shown either for
 * this session or in total. The mode is switched by clicking it while an inventory is open.
 *
 * <p>Only your own kills count. A Zealot counts if you hit it in the last few seconds: melee hits,
 * your own arrows, or being near where your Wither Impact (Hyperion and the other wither blades) landed.
 */
public final class ZealotCounter {
    private static final List<String> END_LOCATIONS = List.of("The End", "Dragon's Nest", "Void Sepulture", "Zealot Bruiser Hideout", "Void Slate");
    private static final List<String> WITHER_BLADES = List.of("Hyperion", "Astraea", "Scylla", "Valkyrie", "Necron's Blade");
    private static final long HIT_MEMORY_MS = 5000;
    private static final double WITHER_IMPACT_RANGE = 10;
    private static final Gson GSON = new Gson();

    /** Entity id → when you last hit it. */
    private static final Map<Integer, Long> HIT_BY_YOU = new HashMap<>();
    /** Entity ids already counted, so the death packet and the health check never count one Zealot twice. */
    private static final Set<Integer> COUNTED = new HashSet<>();
    private static int witherImpactTicks;

    private static int totalKills, totalEyes, sinceEye;
    private static int sessionKills, sessionEyes;
    private static boolean showSession;
    private static Path file;

    private ZealotCounter() {}

    private static boolean enabled() {
        SkyJewConfig c = SkyJewConfig.current();
        return c != null && c.combat.zealotCounter && inEnd();
    }

    public static void init(Path configDir) {
        file = configDir.resolve("skyjew-zealots.json");
        load();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> save());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
            COUNTED.clear();
            HIT_BY_YOU.clear();
        });
        SkyJewChat.onChat(message -> {
            String text = message.text();
            if (text.contains("RARE DROP!") && text.contains("Summoning Eye")) {
                totalEyes++;
                sessionEyes++;
                sinceEye = 0;
                save();
            }
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide() && entity instanceof EnderMan) HIT_BY_YOU.put(entity.getId(), System.currentTimeMillis());
            return InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() && inEnd()) {
                String held = SkyJewLocation.strip(player.getItemInHand(hand).getHoverName().getString());
                // The teleport lands a tick or two later, so mark Zealots around you for a few ticks.
                if (WITHER_BLADES.stream().anyMatch(held::contains)) witherImpactTicks = 4;
            }
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(ZealotCounter::tick);

        SkyJewHuds.register("zealots", "Zealot Tracker", ZealotCounter::enabled,
            // While an inventory is open the tracker is drawn over it instead, with the clickable mode switch.
            () -> Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> ? List.of() : lines(false),
            List.of(title(), kv("Kills: ", "1,234"), kv("Summoning Eyes: ", "4"), kv("Since last eye: ", "321")),
            8, 300);

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) return;
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> renderInInventory(graphics));
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !clickModeSwitch(event.x(), event.y()));
        });
    }

    public static boolean inEnd() {
        if (SkyJewLocation.areaIs("The End")) return true;
        String location = SkyJewLocation.location();
        for (String name : END_LOCATIONS) if (location.contains(name)) return true;
        return false;
    }

    // ----- Display -----

    private static Component title() {
        return Component.literal("Zealot Tracker").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
    }

    private static Component kv(String key, String value) {
        return Component.literal(key).withStyle(ChatFormatting.GRAY).append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static String fmt(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    private static List<Component> lines(boolean withModeSwitch) {
        List<Component> lines = new ArrayList<>();
        lines.add(title());
        lines.add(kv("Kills: ", fmt(showSession ? sessionKills : totalKills)));
        lines.add(kv("Summoning Eyes: ", fmt(showSession ? sessionEyes : totalEyes)));
        lines.add(kv("Since last eye: ", fmt(sinceEye)));
        if (withModeSwitch) lines.add(modeLine());
        return lines;
    }

    private static final String MODE_LABEL = "Display Mode: ";
    private static final String TOTAL = "[Total]";
    private static final String SESSION = "[This Session]";

    private static Component modeLine() {
        MutableComponent line = Component.literal(MODE_LABEL).withStyle(ChatFormatting.GRAY);
        line.append(Component.literal(TOTAL).withStyle(showSession ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW));
        line.append(Component.literal(" "));
        line.append(Component.literal(SESSION).withStyle(showSession ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
        return line;
    }

    private static void renderInInventory(GuiGraphicsExtractor graphics) {
        if (!enabled()) return;
        List<Component> lines = lines(true);
        int[] pos = SkyJewHuds.screenPosition("zealots", lines);
        var placement = SkyJewHuds.placement("zealots");
        SkyJewHuds.render(graphics, lines, pos[0], pos[1], placement.scale, placement.background);
    }

    /** Switches between total and session when the matching "[...]" on the mode line is clicked. */
    private static boolean clickModeSwitch(double mouseX, double mouseY) {
        if (!enabled()) return false;
        List<Component> lines = lines(true);
        int[] pos = SkyJewHuds.screenPosition("zealots", lines);
        float scale = SkyJewHuds.placement("zealots").scale;
        double localX = (mouseX - pos[0]) / scale - SkyJewHuds.PADDING;
        double localY = (mouseY - pos[1]) / scale - SkyJewHuds.PADDING;
        int modeRow = lines.size() - 1;
        if (localY < modeRow * SkyJewHuds.LINE_HEIGHT || localY >= modeRow * SkyJewHuds.LINE_HEIGHT + 9) return false;
        var font = Minecraft.getInstance().font;
        int totalStart = font.width(MODE_LABEL);
        int totalEnd = totalStart + font.width(TOTAL);
        int sessionStart = totalEnd + font.width(" ");
        int sessionEnd = sessionStart + font.width(SESSION);
        if (localX >= totalStart && localX < totalEnd) showSession = false;
        else if (localX >= sessionStart && localX < sessionEnd) showSession = true;
        else return false;
        save();
        return true;
    }

    // ----- Kill detection -----

    private static boolean isZealot(Minecraft mc, Entity entity) {
        return entity instanceof EnderMan && !mc.level.getEntitiesOfClass(ArmorStand.class, entity.getBoundingBox().inflate(0.5, 3, 0.5),
            stand -> stand.hasCustomName() && stand.getCustomName().getString().contains("Zealot")).isEmpty();
    }

    private static boolean hitByYou(Entity entity) {
        Long at = HIT_BY_YOU.get(entity.getId());
        return at != null && System.currentTimeMillis() - at <= HIT_MEMORY_MS;
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || !inEnd()) return;
        long now = System.currentTimeMillis();

        if (witherImpactTicks > 0) {
            witherImpactTicks--;
            for (EnderMan enderman : mc.level.getEntitiesOfClass(EnderMan.class, mc.player.getBoundingBox().inflate(WITHER_IMPACT_RANGE), e -> true)) {
                HIT_BY_YOU.put(enderman.getId(), now);
            }
        }
        // Your arrows mark the Zealots they reach.
        for (AbstractArrow arrow : mc.level.getEntitiesOfClass(AbstractArrow.class, mc.player.getBoundingBox().inflate(64),
                a -> a.getOwner() == mc.player)) {
            for (EnderMan enderman : mc.level.getEntitiesOfClass(EnderMan.class, arrow.getBoundingBox().inflate(1.5), e -> true)) {
                HIT_BY_YOU.put(enderman.getId(), now);
            }
        }
        // Zealots whose health reached zero; this also catches several killed at once.
        for (EnderMan enderman : mc.level.getEntitiesOfClass(EnderMan.class, mc.player.getBoundingBox().inflate(32), e -> true)) {
            if (enderman.isDeadOrDying()) countIfYours(mc, enderman);
        }
        HIT_BY_YOU.values().removeIf(at -> now - at > HIT_MEMORY_MS);
    }

    /** Called when the server says an entity died. */
    public static void onEntityDeath(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (!(entity instanceof EnderMan) || mc.player == null || mc.level == null || !inEnd()) return;
        countIfYours(mc, entity);
    }

    private static void countIfYours(Minecraft mc, Entity entity) {
        if (COUNTED.contains(entity.getId()) || !hitByYou(entity) || !isZealot(mc, entity)) return;
        COUNTED.add(entity.getId());
        if (COUNTED.size() > 4096) COUNTED.clear();
        totalKills++;
        sessionKills++;
        sinceEye++;
        if (totalKills % 25 == 0) save();
    }

    // ----- Storage -----

    private static void load() {
        try {
            if (!Files.exists(file)) return;
            JsonObject root = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            totalKills = root.has("totalKills") ? root.get("totalKills").getAsInt() : 0;
            sinceEye = root.has("sinceEye") ? root.get("sinceEye").getAsInt() : 0;
            totalEyes = root.has("eyes") ? root.get("eyes").getAsInt() : 0;
            showSession = root.has("showSession") && root.get("showSession").getAsBoolean();
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to load zealot tracker: " + e.getMessage());
        }
    }

    private static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("totalKills", totalKills);
            root.addProperty("sinceEye", sinceEye);
            root.addProperty("eyes", totalEyes);
            root.addProperty("showSession", showSession);
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to save zealot tracker: " + e.getMessage());
        }
    }
}

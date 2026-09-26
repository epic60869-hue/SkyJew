package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.features.core.SkyBallsAlerts;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * First-person held item model: moves, rotates and scales the item in your hand and changes the swing speed,
 * like Skysoft's Held Item feature. The Misc > Held Item Model settings apply to every item; /sj helditem save
 * stores the current settings for the SkyBlock item you are holding, which then uses its own transform.
 */
public final class HeldItemModel {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class Transform {
        public float x, y, z, scale = 1f, rotationX, rotationY, rotationZ, swingSpeed = 1f;
    }

    private static Map<String, Transform> perItem = new LinkedHashMap<>();
    private static Path file;

    private HeldItemModel() {}

    private static SkyBallsConfig.HeldItemModel config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.misc.heldItemModel;
    }

    public static void init(Path configDir) {
        file = configDir.resolve("skyballs-held-items.json");
        try {
            if (Files.exists(file)) {
                Map<String, Transform> loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), new TypeToken<Map<String, Transform>>() {}.getType());
                if (loaded != null) perItem = new LinkedHashMap<>(loaded);
            }
        } catch (Exception e) {
            System.err.println("[SkyBalls] Could not read held item transforms: " + e);
        }
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("helditem")
                    .executes(c -> say("/sb helditem save | remove | list. Save stores the current Held Item Model settings for the item in your hand.", ChatFormatting.YELLOW))
                    .then(ClientCommands.literal("save").executes(c -> saveHeld()))
                    .then(ClientCommands.literal("remove").executes(c -> removeHeld()))
                    .then(ClientCommands.literal("list").executes(c -> say(perItem.isEmpty() ? "No per-item transforms." : "Per-item transforms: " + String.join(", ", perItem.keySet()), ChatFormatting.YELLOW)))));
            }
        });
    }

    /** The transform for this stack, or null when the feature is off. */
    public static Transform transform(ItemStack stack) {
        SkyBallsConfig.HeldItemModel config = config();
        if (config == null || !config.enabled) return null;
        String id = stack == null ? "" : Compat.neuName(stack);
        Transform item = id.isEmpty() ? null : perItem.get(id);
        if (item != null) return item;
        Transform global = new Transform();
        global.x = config.x;
        global.y = config.y;
        global.z = config.z;
        global.scale = config.scale;
        global.rotationX = config.rotationX;
        global.rotationY = config.rotationY;
        global.rotationZ = config.rotationZ;
        global.swingSpeed = config.swingSpeed;
        return global;
    }

    /** Applies the transform to the first-person item pose. */
    public static void apply(PoseStack pose, Transform t, boolean leftHand) {
        float side = leftHand ? -1f : 1f;
        pose.translate(t.x * side, t.y, t.z);
        if (t.rotationX != 0) pose.mulPose(Axis.XP.rotationDegrees(t.rotationX));
        if (t.rotationY != 0) pose.mulPose(Axis.YP.rotationDegrees(t.rotationY * side));
        if (t.rotationZ != 0) pose.mulPose(Axis.ZP.rotationDegrees(t.rotationZ * side));
        if (t.scale != 1f) pose.scale(t.scale, t.scale, t.scale);
    }

    /** Misc > Held Item Model > No Swing Animation. */
    public static boolean noSwing() {
        SkyBallsConfig.HeldItemModel config = config();
        return config != null && config.noSwing;
    }

    /** Swing duration in ticks for your own hand. */
    public static int swingDuration(int vanilla) {
        SkyBallsConfig.HeldItemModel config = config();
        if (config == null || !config.enabled) return vanilla;
        var player = Minecraft.getInstance().player;
        Transform t = transform(player == null ? null : player.getMainHandItem());
        int base = config.ignoreMiningEffects ? 6 : vanilla;
        float speed = t == null ? 1f : Math.max(0.05f, t.swingSpeed);
        return Math.max(1, Math.round(base / speed));
    }

    private static int saveHeld() {
        var player = Minecraft.getInstance().player;
        SkyBallsConfig.HeldItemModel config = config();
        if (player == null || config == null) return 0;
        String id = Compat.neuName(player.getMainHandItem());
        if (id.isEmpty()) return say("Hold a SkyBlock item first.", ChatFormatting.RED);
        perItem.remove(id);
        Transform t = transform(null);
        if (t == null) return say("Turn on Misc > Held Item Model first.", ChatFormatting.RED);
        perItem.put(id, t);
        save();
        return say("Saved the current held item settings for " + id + ".", ChatFormatting.GREEN);
    }

    private static int removeHeld() {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        String id = Compat.neuName(player.getMainHandItem());
        if (perItem.remove(id) == null) return say("The held item has no transform of its own.", ChatFormatting.RED);
        save();
        return say("Removed the transform for " + id + ". It uses the global settings again.", ChatFormatting.YELLOW);
    }

    private static void save() {
        try {
            Files.writeString(file, GSON.toJson(perItem), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyBalls] Could not save held item transforms: " + e);
        }
    }

    private static int say(String text, ChatFormatting colour) {
        SkyBallsAlerts.chat(Component.literal(text).withStyle(colour));
        return 1;
    }
}

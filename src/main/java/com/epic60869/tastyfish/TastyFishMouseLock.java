package com.epic60869.tastyfish;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public final class TastyFishMouseLock {
    private static final Set<String> FARMING_TOOLS = Set.of(
        "THEORETICAL_HOE_WHEAT_1","THEORETICAL_HOE_WHEAT_2","THEORETICAL_HOE_WHEAT_3",
        "THEORETICAL_HOE_CARROT_1","THEORETICAL_HOE_CARROT_2","THEORETICAL_HOE_CARROT_3",
        "THEORETICAL_HOE_POTATO_1","THEORETICAL_HOE_POTATO_2","THEORETICAL_HOE_POTATO_3",
        "THEORETICAL_HOE_CANE_1","THEORETICAL_HOE_CANE_2","THEORETICAL_HOE_CANE_3",
        "THEORETICAL_HOE_SUNFLOWER_1","THEORETICAL_HOE_SUNFLOWER_2","THEORETICAL_HOE_SUNFLOWER_3",
        "THEORETICAL_HOE_WILD_ROSE_1","THEORETICAL_HOE_WILD_ROSE_2","THEORETICAL_HOE_WILD_ROSE_3",
        "THEORETICAL_HOE_WARTS_1","THEORETICAL_HOE_WARTS_2","THEORETICAL_HOE_WARTS_3",
        "FUNGI_CUTTER","FUNGI_CUTTER_2","FUNGI_CUTTER_3",
        "CACTUS_KNIFE","CACTUS_KNIFE_2","CACTUS_KNIFE_3",
        "MELON_DICER","MELON_DICER_2","MELON_DICER_3",
        "PUMPKIN_DICER","PUMPKIN_DICER_2","PUMPKIN_DICER_3",
        "COCO_CHOPPER","COCO_CHOPPER_2","COCO_CHOPPER_3",
        "BASIC_GARDENING_HOE","ADVANCED_GARDENING_HOE",
        "BASIC_GARDENING_AXE","ADVANCED_GARDENING_AXE","BINGHOE"
    );

    private static TastyFishConfig config;
    private static boolean locked;

    private TastyFishMouseLock() {}

    public static void init(TastyFishConfig cfg) {
        config = cfg;
    }

    public static void tick(Minecraft mc) {
        boolean shouldLock = config != null && config.mouseLockEnabled && mc.player != null
            && (!config.mouseLockGroundOnly || mc.player.onGround())
            && isFarmingTool(mc.player.getMainHandItem());

        locked = shouldLock;
    }

    public static boolean isLocked() {
        return locked;
    }

    private static boolean isFarmingTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = stack.getHoverName().getString();
        String lower = id.toLowerCase();
        return FARMING_TOOLS.stream().anyMatch(tool -> lower.contains(tool.toLowerCase().replace("_", " ")))
            || lower.contains("theoretical hoe")
            || lower.contains("fungi cutter")
            || lower.contains("cactus knife")
            || lower.contains("melon dicer")
            || lower.contains("pumpkin dicer")
            || lower.contains("coco chopper")
            || lower.contains("gardening hoe")
            || lower.contains("gardening axe");
    }
}

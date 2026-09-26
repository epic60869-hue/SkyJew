package com.epic60869.skyballs.features.dungeons;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import com.epic60869.skyballs.features.core.SkyBallsWorldRender;
import com.epic60869.skyballs.sb.skyblock.dungeon.DungeonClass;
import com.epic60869.skyballs.sb.skyblock.dungeon.secrets.DungeonPlayerManager;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 * Dungeons > Platform Highlight (3x3): one box over the 3x3 floor at x 53-55, z 113-115 (y 63) on floor 7, shown from
 * when Goldor starts. The spot, options and default colours match NoFrills' Platform Highlight.
 */
public final class ThreeByThree {
    private static final AABB BOX = AABB.encapsulatingFullBlocks(new BlockPos(53, 63, 113), new BlockPos(55, 63, 115));

    private ThreeByThree() {}

    public static void init() {
        SkyBallsWorldRender.register(collector -> {
            SkyBallsConfig config = SkyBallsConfig.current();
            Minecraft mc = Minecraft.getInstance();
            if (config == null || mc.player == null) return;
            FeatureConfigs.PlatformHighlight settings = config.dungeons.platformHighlight;
            if (!settings.enabled || !DungeonFeatures.goldorReached()
                || !SkyBallsLocation.inDungeon() || !SkyBallsLocation.dungeonFloor().endsWith("7")) return;
            if (settings.healerOnly && DungeonPlayerManager.getClassFromPlayer(mc.player) != DungeonClass.HEALER) return;
            int outline = colour(settings.outlineColor);
            int fill = colour(settings.fillColor);
            if (settings.style != FeatureConfigs.BoxStyle.OUTLINE) {
                collector.submitFilledBox(BOX, rgb(fill), ((fill >>> 24) & 255) / 255f, false);
            }
            if (settings.style != FeatureConfigs.BoxStyle.FILLED) {
                collector.submitOutlinedBox(BOX, rgb(outline), ((outline >>> 24) & 255) / 255f, 3f, false);
            }
        });
    }

    private static int colour(String value) {
        try {
            return ChromaColour.Companion.specialToChromaRGB(value);
        } catch (Exception e) {
            return 0xFF55FF55;
        }
    }

    private static float[] rgb(int argb) {
        return new float[]{((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f};
    }
}

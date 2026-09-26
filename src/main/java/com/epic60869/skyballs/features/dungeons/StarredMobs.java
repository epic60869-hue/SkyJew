package com.epic60869.skyballs.features.dungeons;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import com.epic60869.skyballs.features.core.SkyBallsWorldRender;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Boxes starred dungeon mobs. Hypixel marks them with a "✯" in the nametag armor stand above
 * the mob, so the mob is the closest living entity just below a starred stand. The mobs are
 * found once per tick; the boxes are drawn every frame at the mob's interpolated position.
 */
public final class StarredMobs {
    private static List<LivingEntity> visible = List.of();

    private StarredMobs() {}

    private static FeatureConfigs.DungeonMobs config() {
        SkyBallsConfig config = SkyBallsConfig.current();
        return config == null ? null : config.dungeons.mobs;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            FeatureConfigs.DungeonMobs config = config();
            if (config == null || !config.starredMobs || !SkyBallsLocation.inDungeon() || mc.level == null || mc.player == null) {
                visible = List.of();
                return;
            }
            List<LivingEntity> found = new ArrayList<>();
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof ArmorStand stand) || !stand.hasCustomName()
                    || !stand.getCustomName().getString().contains("✯")) continue;
                LivingEntity mob = mobBelow(mc, stand);
                // Depth-tested and only for mobs you can see, so nothing shows through walls.
                if (mob != null && mc.player.hasLineOfSight(mob)) found.add(mob);
            }
            visible = found;
        });
        SkyBallsWorldRender.register(collector -> {
            FeatureConfigs.DungeonMobs config = config();
            if (config == null || !config.starredMobs || visible.isEmpty()) return;
            float[] colour = colour(config.starredColor);
            float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            for (LivingEntity mob : visible) {
                if (!mob.isAlive()) continue;
                Vec3 offset = mob.getPosition(partial).subtract(mob.position());
                AABB box = mob.getBoundingBox().move(offset);
                if (config.starredFill > 0f) collector.submitFilledBox(box, colour, config.starredFill * colour[3], false);
                collector.submitOutlinedBox(box, colour, colour[3], config.starredLineWidth, false);
            }
        });
    }

    private static float[] colour(String value) {
        try {
            int argb = ChromaColour.Companion.specialToChromaRGB(value);
            return new float[]{((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, ((argb >>> 24) & 0xFF) / 255f};
        } catch (Exception e) {
            return new float[]{1f, 0.85f, 0.2f, 1f};
        }
    }

    private static LivingEntity mobBelow(Minecraft mc, ArmorStand stand) {
        AABB search = new AABB(stand.getX() - 1, stand.getY() - 4, stand.getZ() - 1, stand.getX() + 1, stand.getY() + 0.5, stand.getZ() + 1);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity mob : mc.level.getEntitiesOfClass(LivingEntity.class, search,
                e -> !(e instanceof ArmorStand) && e != mc.player && e.isAlive())) {
            double distance = mob.distanceToSqr(stand.getX(), stand.getY(), stand.getZ());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = mob;
            }
        }
        return best;
    }
}

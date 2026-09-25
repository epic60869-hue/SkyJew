package com.epic60869.skyjew.features.dungeons;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import com.epic60869.skyjew.features.core.SkyJewWorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

/**
 * Boxes starred dungeon mobs. Hypixel marks them with a "✯" in the nametag armor stand above
 * the mob, so the mob is the closest living entity just below a starred stand.
 */
public final class StarredMobs {
    private static final float[] COLOUR = {1f, 0.85f, 0.2f};

    private StarredMobs() {}

    public static void init() {
        SkyJewWorldRender.register(collector -> {
            SkyJewConfig config = SkyJewConfig.current();
            Minecraft mc = Minecraft.getInstance();
            if (config == null || !config.dungeons.mobs.starredMobs || !SkyJewLocation.inDungeon() || mc.level == null) return;
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof ArmorStand stand) || !stand.hasCustomName()
                    || !stand.getCustomName().getString().contains("✯")) continue;
                LivingEntity mob = mobBelow(mc, stand);
                if (mob != null) collector.submitOutlinedBox(mob.getBoundingBox(), COLOUR, 2f, true);
            }
        });
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

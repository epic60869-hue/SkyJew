package com.epic60869.skyjew.features.dungeons;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewTabWidgetManager;
import com.epic60869.skyjew.features.FeatureConfigs;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import com.epic60869.skyjew.sb.skyblock.dungeon.DungeonClass;
import com.epic60869.skyjew.sb.skyblock.dungeon.secrets.DungeonPlayerManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Your own dungeon class. Read from any tab list row with your name and "(Class Level)", falling back to
 * Skyblocker's player list, and remembered for the rest of the run so it survives being a ghost. The
 * "Your Class" option overrides it.
 */
public final class SelfClass {
    private static final Pattern CLASS = Pattern.compile("\\((Healer|Mage|Berserk|Archer|Tank)\\b");
    private static DungeonClass cached = DungeonClass.UNKNOWN;
    private static int ticks;

    private SelfClass() {}

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> cached = DungeonClass.UNKNOWN);
        SkyJewLocation.onAreaChange(area -> cached = DungeonClass.UNKNOWN);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 20 != 0 || mc.player == null || !SkyJewLocation.inDungeon()) return;
            DungeonClass found = fromTab(mc);
            if (found == DungeonClass.UNKNOWN) found = DungeonPlayerManager.getClassFromPlayer(mc.player);
            if (found != DungeonClass.UNKNOWN) cached = found;
        });
    }

    private static DungeonClass fromTab(Minecraft mc) {
        String name = mc.player.getGameProfile().name();
        for (PlayerInfo info : SkyJewTabWidgetManager.players()) {
            Component display = info.getTabListDisplayName();
            if (display == null) continue;
            String line = SkyJewLocation.strip(display.getString());
            if (!line.contains(" " + name + " ") && !line.startsWith(name + " ")) continue;
            Matcher m = CLASS.matcher(line);
            if (m.find()) return DungeonClass.from(m.group(1));
        }
        return DungeonClass.UNKNOWN;
    }

    /** Your class this run, or UNKNOWN before it has been seen. */
    public static DungeonClass get() {
        SkyJewConfig config = SkyJewConfig.current();
        if (config != null) {
            FeatureConfigs.ClassOverride override = config.dungeons.positionalMessages.classOverride;
            if (override != FeatureConfigs.ClassOverride.AUTO) return DungeonClass.from(override.toString());
        }
        return cached;
    }
}

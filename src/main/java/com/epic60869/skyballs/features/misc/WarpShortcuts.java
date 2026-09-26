package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Warp shortcuts: /dhub runs /warp dhub, /garden runs /warp garden, and so on for every name in Misc > Warp Shortcuts.
 * The list is read when you join a server, so changes apply the next time you connect.
 */
public final class WarpShortcuts {
    /** Names that are already commands (vanilla or SkyBalls), which would clash. */
    private static final Set<String> RESERVED = Set.of("sb", "skyballs", "sbc", "warp", "msg", "tell", "w", "me", "help", "trigger",
        "teammsg", "tm", "say", "list", "seed", "home", "is", "hub", "lobby", "l", "party", "p", "pc", "ac", "gc", "oc", "cc");

    private WarpShortcuts() {}

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            SkyBallsConfig config = SkyBallsConfig.current();
            if (config == null || !config.misc.warpShortcuts) return;
            for (String name : names(config.misc.warpShortcutList)) {
                dispatcher.register(ClientCommands.literal(name).executes(c -> {
                    var connection = Minecraft.getInstance().getConnection();
                    if (connection != null) connection.sendCommand("warp " + name);
                    return 1;
                }));
            }
        });
    }

    private static Set<String> names(String list) {
        Set<String> names = new LinkedHashSet<>();
        if (list == null) return names;
        for (String part : list.split("[,\\s]+")) {
            String name = part.trim().toLowerCase(Locale.ROOT).replace("/", "");
            if (name.matches("[a-z0-9_]{1,20}") && !RESERVED.contains(name)) names.add(name);
        }
        return names;
    }
}

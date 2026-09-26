package com.epic60869.skyballs;

import com.epic60869.skyballs.custom.util.Compat;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * /sj log: what was added and fixed in each update, read from the CHANGELOG.md bundled in the jar.
 * /sj log opens {@link SkyBallsChangelogScreen} on the version you have installed; /sj log &lt;version&gt; on another one.
 */
public final class SkyBallsChangelog {
    record Section(String title, List<String> entries) {}

    record Version(String name, String date, List<Section> sections) {}

    private static List<Version> versions;

    private SkyBallsChangelog() {}

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("log")
                    .executes(c -> show(null))
                    .then(ClientCommands.argument("version", StringArgumentType.greedyString())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(versions().stream().map(Version::name).toList(), b))
                        .executes(c -> show(StringArgumentType.getString(c, "version"))))));
            }
        });
    }

    private static List<Version> versions() {
        if (versions != null && !versions.isEmpty()) return versions;
        List<Version> parsed = new ArrayList<>();
        String text = readChangelog();
        if (text != null) {
            Version version = null;
            Section section = null;
            for (String raw : text.split("\\r?\\n")) {
                String line = raw.strip();
                if (line.startsWith("## ")) {
                    String[] parts = line.substring(3).split("\\s+[\u2014-]\\s+", 2);
                    version = new Version(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "", new ArrayList<>());
                    parsed.add(version);
                    section = null;
                } else if (line.startsWith("### ") && version != null) {
                    section = new Section(line.substring(4).trim(), new ArrayList<>());
                    version.sections().add(section);
                } else if (line.startsWith("- ") && section != null) {
                    section.entries().add(line.substring(2).replace("`", "").replace("**", ""));
                }
            }
        }
        // Only remember a successful read, so a failed one can be retried.
        if (!parsed.isEmpty()) versions = parsed;
        return parsed;
    }

    /**
     * The CHANGELOG.md bundled in the jar. Read through Fabric's view of the mod's files (class resource lookups
     * don't find non-class files in a real game), then Minecraft's resource manager, then the class path.
     */
    private static String readChangelog() {
        try {
            var mod = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("skyballs");
            if (mod.isPresent()) {
                var path = mod.get().findPath("assets/skyballs/CHANGELOG.md");
                if (path.isPresent()) return java.nio.file.Files.readString(path.get(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            System.err.println("[SkyBalls] Could not read the changelog from the mod jar: " + e.getMessage());
        }
        try {
            var resource = Minecraft.getInstance().getResourceManager()
                .getResource(net.minecraft.resources.Identifier.fromNamespaceAndPath("skyballs", "CHANGELOG.md"));
            if (resource.isPresent()) {
                try (InputStream in = resource.get().open()) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            System.err.println("[SkyBalls] Could not read the changelog resource: " + e.getMessage());
        }
        try (InputStream in = SkyBallsChangelog.class.getResourceAsStream("/assets/skyballs/CHANGELOG.md")) {
            if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyBalls] Could not read the changelog: " + e.getMessage());
        }
        return null;
    }

    /** The installed SkyBalls version, e.g. "1.2.3". */
    private static String installedVersion() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("skyballs")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("");
    }

    private static int show(String wanted) {
        List<Version> all = versions();
        String installed = installedVersion();
        // Default: the changes in the version you have installed (or the newest one).
        String target = wanted != null ? wanted.trim() : installed;
        int index = -1;
        for (int i = 0; i < all.size(); i++) if (all.get(i).name().equalsIgnoreCase(target)) index = i;
        if (index < 0 && wanted != null && !all.isEmpty()) {
            return send(Component.literal("[SB] No changelog for \"" + wanted + "\".").withStyle(ChatFormatting.RED));
        }
        return Compat.queueOpenScreen(new SkyBallsChangelogScreen(all, Math.max(0, index), installed));
    }

    private static int send(Component message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(message);
        });
        return 1;
    }
}

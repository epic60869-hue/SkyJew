package com.epic60869.skyjew;

import com.epic60869.skyjew.custom.util.Compat;
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
 * /sj log opens {@link SkyJewChangelogScreen} on the version you have installed; /sj log &lt;version&gt; on another one.
 */
public final class SkyJewChangelog {
    record Section(String title, List<String> entries) {}

    record Version(String name, String date, List<Section> sections) {}

    private static List<Version> versions;

    private SkyJewChangelog() {}

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
        if (versions != null) return versions;
        List<Version> parsed = new ArrayList<>();
        try (InputStream in = SkyJewChangelog.class.getResourceAsStream("/assets/skyjew/CHANGELOG.md")) {
            if (in != null) {
                Version version = null;
                Section section = null;
                for (String raw : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\r?\n")) {
                    String line = raw.strip();
                    if (line.startsWith("## ")) {
                        String[] parts = line.substring(3).split("\\s+[—-]\\s+", 2);
                        version = new Version(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "", new ArrayList<>());
                        parsed.add(version);
                        section = null;
                    } else if (line.startsWith("### ") && version != null) {
                        section = new Section(line.substring(4).trim(), new ArrayList<>());
                        version.sections().add(section);
                    } else if (line.startsWith("- ") && section != null) {
                        section.entries().add(line.substring(2).replace("`", ""));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SkyJew] Could not read the changelog: " + e.getMessage());
        }
        versions = parsed;
        return parsed;
    }

    /** The installed SkyJew version, e.g. "1.2.3". */
    private static String installedVersion() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("skyjew")
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
            return send(Component.literal("[SkyJew] No changelog for \"" + wanted + "\".").withStyle(ChatFormatting.RED));
        }
        return Compat.queueOpenScreen(new SkyJewChangelogScreen(all, Math.max(0, index), installed));
    }

    private static int send(Component message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(message);
        });
        return 1;
    }
}

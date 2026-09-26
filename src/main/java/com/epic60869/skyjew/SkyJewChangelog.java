package com.epic60869.skyjew;

import com.epic60869.skyjew.custom.util.Compat;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /sj log: what was added and fixed in each update, read from the CHANGELOG.md bundled in the jar.
 * /sj log shows the changes in the version you have installed; /sj log &lt;version&gt; shows another one.
 * Each entry shows its first sentence; hover it for the full text.
 */
public final class SkyJewChangelog {
    private record Section(String title, List<String> entries) {}

    private record Version(String name, String date, List<Section> sections) {}

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
        if (all.isEmpty()) return send(Component.literal("[SkyJew] The changelog isn't available in this build.").withStyle(ChatFormatting.RED));
        // Default: the changes in the version you have installed.
        String target = wanted != null ? wanted.trim() : installedVersion();
        Version version = null;
        for (Version v : all) if (v.name().equalsIgnoreCase(target)) version = v;
        if (version == null && wanted == null) version = all.getFirst();
        if (version == null) return send(Component.literal("[SkyJew] No changelog for \"" + wanted + "\".").withStyle(ChatFormatting.RED));

        MutableComponent out = Component.literal("SkyJew " + version.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        if (!version.date().isEmpty()) out.append(Component.literal("  " + version.date()).withStyle(ChatFormatting.GRAY));
        for (Section section : version.sections()) {
            ChatFormatting colour = switch (section.title().toLowerCase(Locale.ROOT)) {
                case "added" -> ChatFormatting.GREEN;
                case "fixed" -> ChatFormatting.AQUA;
                case "removed" -> ChatFormatting.RED;
                default -> ChatFormatting.YELLOW;
            };
            out.append(Component.literal("\n" + section.title() + " (" + section.entries().size() + ")").withStyle(colour, ChatFormatting.BOLD));
            for (String entry : section.entries()) {
                out.append(Component.literal("\n • ").withStyle(colour))
                    .append(Component.literal(summary(entry)).withStyle(style -> style.withColor(ChatFormatting.WHITE)
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(entry)))));
            }
        }
        out.append(Component.literal("\nHover an entry for the full text.").withStyle(ChatFormatting.DARK_GRAY));
        return send(out);
    }

    /** First sentence of an entry, so the list stays short. */
    private static String summary(String entry) {
        int end = entry.indexOf(". ");
        String first = end > 0 ? entry.substring(0, end + 1) : entry;
        return first.length() > 140 ? first.substring(0, 137) + "..." : first;
    }

    private static int send(Component message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(message);
        });
        return 1;
    }
}

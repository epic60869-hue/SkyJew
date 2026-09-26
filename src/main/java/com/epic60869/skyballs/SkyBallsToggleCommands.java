package com.epic60869.skyballs;

import com.epic60869.skyballs.custom.util.Compat;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Commands for switching things on and off without opening the menu:
 * <ul>
 *   <li>/sb toggle &lt;setting&gt;: flips any on/off setting (e.g. /sb toggle dungeons.caseOpening), with suggestions.</li>
 *   <li>/sb togglenick &lt;player&gt;: hides or shows that player's nickname for you (your own name toggles yours).</li>
 *   <li>/sb disableall: turns every feature off, after you click to confirm (like /skyblocker disableall).</li>
 * </ul>
 */
public final class SkyBallsToggleCommands {
    private static long disableAllAsked;

    private SkyBallsToggleCommands() {}

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root)
                    .then(ClientCommands.literal("toggle")
                        .then(ClientCommands.argument("setting", StringArgumentType.greedyString())
                            .suggests((c, b) -> SharedSuggestionProvider.suggest(toggles().keySet(), b))
                            .executes(c -> toggle(StringArgumentType.getString(c, "setting")))))
                    .then(ClientCommands.literal("togglenick")
                        .then(ClientCommands.argument("player", StringArgumentType.word())
                            .suggests((c, b) -> SharedSuggestionProvider.suggest(SkyBallsNick.nickedPlayers(), b))
                            .executes(c -> {
                                SkyBallsNick.toggleFor(StringArgumentType.getString(c, "player"));
                                return 1;
                            })))
                    .then(ClientCommands.literal("who").executes(c -> {
                        SkyBallsGlobalChat.requestWho();
                        return 1;
                    }))
                    .then(ClientCommands.literal("disableall")
                        .executes(c -> askDisableAll())
                        .then(ClientCommands.literal("confirm").executes(c -> disableAll()))));
            }
            dispatcher.register(ClientCommands.literal("sbdisableall")
                .executes(c -> askDisableAll())
                .then(ClientCommands.literal("confirm").executes(c -> disableAll())));
        });
    }

    /** A toggle: the object holding the field, the field, and the name shown in the menu. */
    private record Toggle(Object owner, Field field, String name) {}

    /** Every on/off setting in the config, keyed "category.field" (or "category.section.field"). */
    private static Map<String, Toggle> toggles() {
        Map<String, Toggle> out = new LinkedHashMap<>();
        SkyBallsConfig config = SkyBallsConfig.current();
        if (config != null) collect(config, "", out, 0);
        return out;
    }

    private static void collect(Object obj, String prefix, Map<String, Toggle> out, int depth) {
        if (obj == null || depth > 4) return;
        for (Field field : obj.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            try {
                Object value = field.get(obj);
                String path = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
                if (field.getType() == boolean.class && field.isAnnotationPresent(ConfigEditorBoolean.class)) {
                    ConfigOption option = field.getAnnotation(ConfigOption.class);
                    out.put(path, new Toggle(obj, field, option == null ? field.getName() : option.name()));
                } else if (value != null && value.getClass().getName().startsWith("com.epic60869.skyballs")
                    && !value.getClass().isEnum() && !(value instanceof Runnable)) {
                    collect(value, path, out, depth + 1);
                }
            } catch (IllegalAccessException ignored) {}
        }
    }

    private static int toggle(String input) {
        String wanted = input.trim();
        Map<String, Toggle> all = toggles();
        Toggle toggle = all.get(wanted);
        if (toggle == null) {
            // Also accept the name from the menu, or just the last part ("caseOpening").
            List<Map.Entry<String, Toggle>> matches = new ArrayList<>();
            for (Map.Entry<String, Toggle> e : all.entrySet()) {
                String last = e.getKey().substring(e.getKey().lastIndexOf('.') + 1);
                if (last.equalsIgnoreCase(wanted) || e.getValue().name().equalsIgnoreCase(wanted)) matches.add(e);
            }
            if (matches.size() == 1) toggle = matches.getFirst().getValue();
            else if (matches.size() > 1) return say(Component.literal("More than one setting is called that; use the full name, e.g. " + matches.getFirst().getKey()).withStyle(ChatFormatting.YELLOW));
        }
        if (toggle == null) return say(Component.literal("No setting called \"" + wanted + "\". Press Tab after /sb toggle to see them.").withStyle(ChatFormatting.RED));
        try {
            boolean now = !toggle.field().getBoolean(toggle.owner());
            toggle.field().setBoolean(toggle.owner(), now);
            SkyBallsConfig.saveCurrent(SkyBallsConfig.current());
            return say(Component.literal(toggle.name() + " is now ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(now ? "ON" : "OFF").withStyle(now ? ChatFormatting.GREEN : ChatFormatting.RED)));
        } catch (IllegalAccessException e) {
            return say(Component.literal("Couldn't change that setting.").withStyle(ChatFormatting.RED));
        }
    }

    private static int askDisableAll() {
        disableAllAsked = System.currentTimeMillis();
        return say(Component.literal("This turns off every SkyBalls feature. ").withStyle(ChatFormatting.RED)
            .append(Component.literal("[Click to confirm]").withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/sb disableall confirm"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Turn off every feature (you can turn them back on in /sb)"))))));
    }

    private static int disableAll() {
        // Only right after /sb disableall, so it can't happen by accident.
        if (System.currentTimeMillis() - disableAllAsked > 60_000) return askDisableAll();
        disableAllAsked = 0;
        int count = 0;
        for (Toggle t : toggles().values()) {
            try {
                if (t.field().getBoolean(t.owner())) {
                    t.field().setBoolean(t.owner(), false);
                    count++;
                }
            } catch (IllegalAccessException ignored) {}
        }
        SkyBallsConfig.saveCurrent(SkyBallsConfig.current());
        return say(Component.literal("Turned off " + count + " settings. Turn features back on in /sb.").withStyle(ChatFormatting.YELLOW));
    }

    private static int say(Component message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(
                Component.literal("[SB] ").withStyle(ChatFormatting.DARK_GREEN).append(message));
        });
        return 1;
    }

    @SuppressWarnings("unused")
    private static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}

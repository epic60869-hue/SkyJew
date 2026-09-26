package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.core.SkyBallsChat;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto Welcome: when a player on your list comes online ("Guild > Name joined." or "Friend > Name joined."), SkyBalls
 * welcomes them in guild chat or with /msg, whichever you picked. It can also welcome new guild members
 * ("[MVP+] Name joined the guild!"). Each player is welcomed at most once per cooldown, after a short random delay.
 *
 * /sj welcome add|remove &lt;name&gt;, /sj welcome list, or edit the list in Misc > Auto Welcome.
 */
public final class AutoWelcome {
    private static final Pattern CAME_ONLINE = Pattern.compile("^(?:Guild|Friend) > (?<name>\\w{1,16}) joined\\.$");
    private static final Pattern NEW_MEMBER = Pattern.compile("^(?:\\[[^]]+] )?(?<name>\\w{1,16}) joined the guild!$");

    private static final Map<String, Long> lastWelcomed = new HashMap<>();

    private AutoWelcome() {}

    private static FeatureConfigs.AutoWelcome config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.misc.autoWelcome;
    }

    public static void init() {
        SkyBallsChat.onChat(message -> onChat(message.text().trim()));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("welcome")
                    .executes(c -> list())
                    .then(ClientCommands.literal("list").executes(c -> list()))
                    .then(ClientCommands.literal("add").then(ClientCommands.argument("name", StringArgumentType.word())
                        .executes(c -> add(StringArgumentType.getString(c, "name")))))
                    .then(ClientCommands.literal("remove").then(ClientCommands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(names(), b))
                        .executes(c -> remove(StringArgumentType.getString(c, "name")))))));
            }
        });
    }

    private static void onChat(String text) {
        FeatureConfigs.AutoWelcome c = config();
        if (c == null || !c.enabled) return;
        Matcher m;
        if ((m = CAME_ONLINE.matcher(text)).matches()) {
            String name = m.group("name");
            if (isListed(name)) welcome(name, c.message, c.destination, c);
        } else if (c.welcomeNewMembers && (m = NEW_MEMBER.matcher(text)).matches()) {
            // New members are always welcomed in guild chat: they may not accept /msg from strangers yet.
            welcome(m.group("name"), c.newMemberMessage, FeatureConfigs.AutoWelcome.Destination.GUILD, c);
        }
    }

    private static void welcome(String name, String template, FeatureConfigs.AutoWelcome.Destination destination, FeatureConfigs.AutoWelcome c) {
        Minecraft mc = Minecraft.getInstance();
        if (name.equalsIgnoreCase(mc.getUser().getName())) return;
        String key = name.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        Long last = lastWelcomed.get(key);
        if (last != null && now - last < c.cooldownMinutes * 60_000L) return;
        lastWelcomed.put(key, now);

        String text = (template == null || template.isBlank() ? "Welcome {name}!" : template).replace("{name}", name).trim();
        if (text.length() > 200) text = text.substring(0, 200);
        String command = destination == FeatureConfigs.AutoWelcome.Destination.MESSAGE ? "msg " + name + " " + text : "gc " + text;
        // A short, random delay so it doesn't look instant.
        long delay = ThreadLocalRandom.current().nextLong(1_000, 2_500);
        CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute(() -> mc.execute(() -> {
            if (mc.player != null && mc.player.connection != null) mc.player.connection.sendCommand(command);
        }));
    }

    // ---------------------------------------------------------------- the list

    static List<String> names() {
        FeatureConfigs.AutoWelcome c = config();
        List<String> out = new ArrayList<>();
        if (c == null || c.names == null) return out;
        for (String part : c.names.split("[,\\s]+")) {
            String name = part.trim();
            if (name.matches("\\w{1,16}")) out.add(name);
        }
        return out;
    }

    private static boolean isListed(String name) {
        for (String n : names()) if (n.equalsIgnoreCase(name)) return true;
        return false;
    }

    private static void setNames(List<String> list) {
        SkyBallsConfig c = SkyBallsConfig.current();
        if (c == null) return;
        c.misc.autoWelcome.names = String.join(", ", list);
        SkyBallsConfig.saveCurrent(c);
    }

    private static int add(String name) {
        if (!name.matches("\\w{1,16}")) return say(Component.literal("\"" + name + "\" isn't a Minecraft name.").withStyle(ChatFormatting.RED));
        Set<String> list = new LinkedHashSet<>(names());
        if (isListed(name)) return say(Component.literal(name + " is already on your welcome list.").withStyle(ChatFormatting.YELLOW));
        list.add(name);
        setNames(new ArrayList<>(list));
        FeatureConfigs.AutoWelcome c = config();
        String where = c != null && c.destination == FeatureConfigs.AutoWelcome.Destination.MESSAGE ? "with /msg" : "in guild chat";
        String off = c != null && !c.enabled ? " (Auto Welcome is off: turn it on in /sb > Misc > Auto Welcome)" : "";
        return say(Component.literal("Added " + name + " to your welcome list. They'll be welcomed " + where + " when they come online." + off)
            .withStyle(ChatFormatting.GREEN));
    }

    private static int remove(String name) {
        List<String> list = names();
        if (!list.removeIf(n -> n.equalsIgnoreCase(name))) {
            return say(Component.literal(name + " isn't on your welcome list.").withStyle(ChatFormatting.YELLOW));
        }
        setNames(list);
        return say(Component.literal("Removed " + name + " from your welcome list.").withStyle(ChatFormatting.GREEN));
    }

    private static int list() {
        List<String> list = names();
        if (list.isEmpty()) {
            return say(Component.literal("Your welcome list is empty. Add someone with /sb welcome add <name>.").withStyle(ChatFormatting.YELLOW));
        }
        return say(Component.literal("Welcome list (" + list.size() + "): ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(String.join(", ", list)).withStyle(ChatFormatting.WHITE)));
    }

    private static int say(Component message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(
                Component.literal("[SB] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(message));
        });
        return 1;
    }
}

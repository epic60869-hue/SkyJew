package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.core.SkyBallsChat;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lets party members run !warp, !allinvite, !pt and !promote while you are party leader.
 * Leadership is tracked from Hypixel's party messages.
 */
public final class PartyCommands {
    private static final Pattern PARTY_CHAT = Pattern.compile("^Party > (?:\\[[^]]+] )?(?<name>\\w+)[^:]*: !(?<command>\\w+)");
    private static final Pattern INVITED = Pattern.compile("^(?:\\[[^]]+] )?(?<me>\\w+) invited (?:\\[[^]]+] )?\\w+ to the party!");
    private static final Pattern TRANSFERRED = Pattern.compile("^The party was transferred to (?:\\[[^]]+] )?(?<name>\\w+)");
    private static final Pattern JOINED_OTHER = Pattern.compile("^You have joined (?:\\[[^]]+] )?(?<name>\\w+)'s? party!");
    private static final Pattern LEADER_LIST = Pattern.compile("^Party Leader: (?:\\[[^]]+] )?(?<name>\\w+)");

    private static String leader;
    private static long lastCommand;

    private PartyCommands() {}

    public static void init() {
        SkyBallsChat.onChat(PartyCommands::onChat);
    }

    private static String me() {
        return Minecraft.getInstance().getUser().getName();
    }

    private static void onChat(SkyBallsChat.Message message) {
        String text = message.text();
        Matcher m;
        if ((m = INVITED.matcher(text)).find() && m.group("me").equalsIgnoreCase(me())) leader = me();
        else if ((m = TRANSFERRED.matcher(text)).find()) leader = m.group("name");
        else if ((m = JOINED_OTHER.matcher(text)).find()) leader = m.group("name");
        else if ((m = LEADER_LIST.matcher(text)).find()) leader = m.group("name");
        else if (text.equals("You left the party.") || text.endsWith("has disbanded the party!")
            || text.startsWith("You have been kicked from the party") || text.equals("The party was disbanded because all invites expired and the party was empty.")) {
            leader = null;
        }

        SkyBallsConfig c = SkyBallsConfig.current();
        if (c == null) return;
        FeatureConfigs.PartyCommands config = c.misc.partyCommands;
        if (!config.enabled || leader == null || !leader.equalsIgnoreCase(me())) return;
        if ((m = PARTY_CHAT.matcher(text)).find()) {
            String sender = m.group("name");
            if (sender.equalsIgnoreCase(me())) return;
            String command = m.group("command").toLowerCase(Locale.ROOT);
            String toRun = switch (command) {
                case "warp" -> config.warp ? "party warp" : null;
                case "allinvite", "allinv" -> config.allInvite ? "party settings allinvite" : null;
                case "pt", "transfer", "ptme" -> config.transfer ? "party transfer " + sender : null;
                case "promote" -> config.promote ? "party promote " + sender : null;
                default -> null;
            };
            // Small cooldown so a spammed command is only run once.
            if (toRun != null && System.currentTimeMillis() - lastCommand > 1500) {
                lastCommand = System.currentTimeMillis();
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    if (mc.getConnection() != null) mc.getConnection().sendCommand(toRun);
                });
            }
        }
    }
}

package com.epic60869.skyjew.features.core;

import com.epic60869.skyjew.SkyJewTabWidgetManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where the player is in SkyBlock, read from the sidebar scoreboard and the tab list.
 * Updated twice a second; listeners are told when the tab-list area changes.
 */
public final class SkyJewLocation {
    private static final Pattern FLOOR = Pattern.compile("The Catacombs \\((?<floor>[FM]\\d|E)\\)");
    private static final Pattern GLACITE = Pattern.compile("Glacite Tunnels|Dwarven Base Camp|Great Glacite Lake|Fossil Research Center");
    private static final List<Consumer<String>> AREA_LISTENERS = new CopyOnWriteArrayList<>();

    private static List<String> scoreboard = List.of();
    private static String scoreboardTitle = "";
    private static String area = "";
    private static String location = "";
    private static String floor = "";
    private static boolean onSkyblock;
    private static int ticks;

    private SkyJewLocation() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 10 == 0) update(mc);
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> reset());
    }

    /** Called when the tab-list area changes (e.g. "Garden", "Catacombs", "Crystal Hollows"). */
    public static void onAreaChange(Consumer<String> listener) {
        AREA_LISTENERS.add(listener);
    }

    private static void reset() {
        scoreboard = List.of();
        scoreboardTitle = "";
        location = "";
        floor = "";
        onSkyblock = false;
        setArea("");
    }

    private static void update(Minecraft mc) {
        if (mc.level == null) {
            if (onSkyblock || !area.isEmpty()) reset();
            return;
        }

        Scoreboard board = mc.level.getScoreboard();
        Objective objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);
        List<String> lines = new ArrayList<>();
        if (objective != null) {
            scoreboardTitle = strip(objective.getDisplayName().getString());
            List<PlayerScoreEntry> entries = new ArrayList<>(board.listPlayerScores(objective));
            entries.removeIf(PlayerScoreEntry::isHidden);
            entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner));
            for (PlayerScoreEntry entry : entries) {
                PlayerTeam team = board.getPlayersTeam(entry.owner());
                lines.add(strip(PlayerTeam.formatNameForTeam(team, entry.ownerName()).getString()).trim());
            }
        } else {
            scoreboardTitle = "";
        }
        scoreboard = List.copyOf(lines);
        onSkyblock = scoreboardTitle.contains("SKYBLOCK") || scoreboardTitle.contains("SKIBLOCK");

        String newLocation = "";
        String newFloor = "";
        for (String line : lines) {
            int symbol = Math.max(line.indexOf('⏣'), line.indexOf('ф'));
            if (symbol >= 0) newLocation = line.substring(symbol + 1).trim();
            Matcher m = FLOOR.matcher(line);
            if (m.find()) newFloor = m.group("floor");
        }
        location = newLocation;
        floor = newFloor;

        String newArea = "";
        for (PlayerInfo info : SkyJewTabWidgetManager.players()) {
            Component name = info.getTabListDisplayName();
            if (name == null) continue;
            String text = strip(name.getString()).trim();
            if (text.startsWith("Area: ")) { newArea = text.substring(6).trim(); break; }
            if (text.startsWith("Dungeon: ")) { newArea = text.substring(9).trim(); break; }
        }
        setArea(newArea);
    }

    private static void setArea(String newArea) {
        if (newArea.equals(area)) return;
        area = newArea;
        for (Consumer<String> listener : AREA_LISTENERS) {
            try {
                listener.accept(newArea);
            } catch (Exception ignored) {}
        }
    }

    public static String strip(String text) {
        String stripped = ChatFormatting.stripFormatting(text);
        return stripped == null ? "" : stripped;
    }

    public static boolean onSkyblock() { return onSkyblock; }
    public static String area() { return area; }
    public static String location() { return location; }
    public static List<String> scoreboard() { return scoreboard; }
    /** Floor such as "F7" or "M7", or "" outside dungeons. */
    public static String dungeonFloor() { return floor; }

    public static boolean inDungeon() {
        return area.equals("Catacombs") || !floor.isEmpty();
    }

    public static boolean inGarden() { return area.equals("Garden"); }
    public static boolean inCrystalHollows() { return area.equals("Crystal Hollows"); }
    public static boolean inDwarvenMines() { return area.equals("Dwarven Mines"); }
    public static boolean inMineshaft() { return area.equals("Mineshaft"); }
    public static boolean inGlaciteTunnels() { return inDwarvenMines() && GLACITE.matcher(location).find(); }
    public static boolean inMiningIsland() { return inDwarvenMines() || inCrystalHollows() || inMineshaft(); }

    public static boolean areaIs(String name) {
        return area.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT));
    }

    /** First scoreboard line that starts with {@code prefix}, without the prefix, or null. */
    public static String scoreboardValue(String prefix) {
        for (String line : scoreboard) {
            if (line.startsWith(prefix)) return line.substring(prefix.length()).trim();
        }
        return null;
    }
}
